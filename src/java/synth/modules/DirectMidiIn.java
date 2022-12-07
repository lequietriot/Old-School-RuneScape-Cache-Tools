/*
 * (C) Copyright IBM Corp. 2005, 2008
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package synth.modules;

import synth.engine.AudioTime;
import synth.engine.MidiEvent;
import synth.engine.MidiIn;
import synth.engine.ThreadFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A proprietary implementation of the MidiIn interface for receiving MIDI
 * messages with the lowest possible delay.
 * <p>
 * The ALSA API does not make it easy to implement low latency MIDI input: in
 * blocking mode we do not need to rely on sleep or similar (who knows how ALSA
 * implements blocking though), and I assume that blocking mode will yield lower
 * delay. However, short MIDI messages can be as little as 1 byte, so it is only
 * practical to read byte by byte in blocking mode, otherwise we may
 * inadvertently read several messages at once (and wait for the second
 * message). Note that transmitting 1 byte over a MIDI cable already takes 300
 * microseconds. Of course one can argue that if it takes 300us per byte, then
 * we cannot cause bad timing by reading byte by byte. This will become a
 * problem when alternative MIDI transports are used, e.g. USB, Firewire,
 * virtual MIDI devices (like loopbacks). Querying the MIDI device how many
 * bytes are available for reading without blocking before each call will
 * probably just add overhead without much of a benefit.
 * <p>
 * If non-blocking mode is used, we do not gain much, because the length of
 * messages depends on the (first) status byte, and for sys ex messages, the
 * length is arbitrary and cannot be known in advance.
 * <p>
 * Therefore, it seems the best approach is to use blocking read, byte by byte.
 * <p>
 * Another problem is the time stamp: retrieving a short message from native is
 * easy, because it can be entirely returned in an <code>int</code>. However,
 * that removes the possibility to include the time code in the returned value.
 * A short MIDI message can have at most 8+7+7 bits, 22 bits. If a
 * <code>long</code> is used as return code, we could use 64-22=42 bits for
 * the time stamp, yielding 50 days before the time stamp wraps around, if a
 * microsecond time stamp is used. An error can be signaled by setting the high
 * order bit of the lowest order byte (the status byte) to 0 -- status bytes
 * always must have set the high order bit.
 * <p>
 * When does the timestamp need to be taken?
 * <ul>
 * <li>At arrival of the first byte:<br>
 * This would match expectations for timing that the first byte represents the
 * actual trigger time. However, a 2-byte message would then have a timestamp
 * 300usecs in the past, and a 3-byte message would have the timestamp 600usecs
 * in the past.
 * <li>At arrival of the last byte of the message:<br>
 * This would solve the problem of past time stamps, but it would be inaccurate,
 * because the timestamp would be delayed non-uniformly by the length of the
 * message.
 * </ul>
 * The current solution is to take the time of arrival of the first byte of a
 * message, but always add 600usecs to it to make the implicit delay imposed by
 * possibly following bytes a uniform delay. For messages with 1 or 2 bytes, we
 * add slightly more delay than necessary, but we'll hopefully be able to
 * minimize jitter.
 * <p>
 * <b>Blocking read</b>: According to ALSA developers, a blocking read in
 * ALSA's rawmidi subsystem cannot be interrupted, unless a MIDI byte arrives.
 * So for low latency, a blocking read is great, but it cannot be unblocked once
 * you want to close the device. So an unblocking read call must be used. Using
 * poll() was suggested.
 * <p>
 * Handling <b>running status</b>: the native layer probably needs to undo
 * running status to correctly interprete running status messages.
 * 
 * @author florian
 */

public class DirectMidiIn implements Runnable, MidiIn {

	public static boolean DEBUG_DIRECTMIDIIN = false;

	/**
	 * The priority of the read thread, on a scale 0...28
	 */
	public static final int MIDIIN_THREAD_PRIORITY = 27;

	private static boolean libAvailable = false;

	static {
		try {
			System.loadLibrary("directmidiin");
			libAvailable = true;
		} catch (UnsatisfiedLinkError ule) {
			if (DEBUG_DIRECTMIDIIN) {
				System.out.println("DirectMidiIn not available (failed to load native library)");
				// Debug.debug(ule);
				System.out.println("java.library.path="
						+ System.getProperty("java.library.path"));
			}
		}
	}

	/**
	 * the offset of the clock in nanoseconds
	 */
	private long clockOffset = 0;

	/**
	 * The index of this instance
	 */
	private int instanceIndex;

	/**
	 * Native handle to the device
	 */
	private long handle = 0;

	/**
	 * The listeners that will receive incoming MIDI messages.
	 */
	private List<Listener> listeners = new ArrayList<Listener>();

	/**
	 * flag to notify the thread to stop execution
	 */
	private volatile boolean stopRequested;

	/**
	 * The thread object for reading MIDI data
	 */
	private Object runner;

	/**
	 * flag that is true while the thread is running
	 */
	private volatile boolean inThread = false;

	/**
	 * Set when the listener or other important setting changes
	 */
	private volatile boolean configChange = false;

	/** name of the open device */
	private String openDevName;
	
	/** current value if timestamping is enabled */
	private boolean timestamping = true;

	/**
	 * Create a DirectMidiIn instance.
	 */
	public DirectMidiIn() {
		this(0);
	}

	/**
	 * Create a DirectMidiIn instance, initialized with specified instance
	 * index.
	 * 
	 * @param instanceIndex the device instance
	 */
	public DirectMidiIn(int instanceIndex) {
		this.instanceIndex = instanceIndex;
	}

	/**
	 * Open the MIDI IN device. The audio time (regardless of the time offset)
	 * will be reset to 0.
	 */
	public void open(String devName) throws Exception {
		synchronized (this) {
			handle = nOpen(devName);
		}
		if (handle != 0) {
			try {
				startThread();
			} catch (RuntimeException re) {
				close();
				throw re;
			}
			openDevName = devName;
		} else {
			throw new Exception("Cannot open MIDI device: " + devName);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.MidiIn#close()
	 */
	public void close() {
		stopRequested = true;
		synchronized (this) {
			if (handle != 0) {
				int maxTrials = 50;
				try {
					while (nClose(handle) == 2 && maxTrials-- > 0) {
						this.wait(20);
					}
				} catch (InterruptedException ie) {
				}
				handle = 0;
			}
		}
	}

	private void startThread() {
		runner = ThreadFactory.createThread(this, getClass().getSimpleName()
				+ " read thread", MIDIIN_THREAD_PRIORITY);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.MidiIn#getDeviceIndex()
	 */
	public int getInstanceIndex() {
		return instanceIndex;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.MidiIn#addListener(synth.engine.MidiIn.Listener)
	 */
	public void addListener(Listener L) {
		synchronized (listeners) {
			this.listeners.add(L);
		}
		configChange = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.MidiIn#removeListener(synth.engine.MidiIn.Listener)
	 */
	public void removeListener(Listener L) {
		synchronized (listeners) {
			this.listeners.remove(L);
		}
		configChange = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.AudioClock#getAudioTime()
	 */
	public AudioTime getAudioTime() {
		if (handle != 0) {
			return new AudioTime(nGetTimeStamp(handle) + clockOffset);
		} else {
			return new AudioTime(clockOffset);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.AdjustableAudioClock#getTimeOffset()
	 */
	public AudioTime getTimeOffset() {
		return new AudioTime(clockOffset);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.AdjustableAudioClock#setTimeOffset(synth.engine.AudioTime)
	 */
	public void setTimeOffset(AudioTime offset) {
		this.clockOffset = offset.getNanoTime();
	}

	/**
	 * @return true if the direct MIDI device can be used
	 */
	public static boolean isAvailable() {
		return libAvailable;
	}

	/** if false, all MIDI events will have time stamp 0 */
	public void setTimestamping(boolean value) {
		timestamping = value;
	}

	/** @return the current status of time stamping MIDI events */
	public boolean isTimestamping() {
		return timestamping;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.MidiIn#isOpen()
	 */
	public boolean isOpen() {
		return handle != 0;
	}

	public String toString() {
		if (handle != 0) {
			return "DirectMidiIn: " + openDevName;
		}
		return "DirectMidiIn";
	}

	/**
	 * Returns a list of available devices. It starts with the device identifier
	 * followed by an optional description. The description is separated by the
	 * bar | character.
	 * 
	 * @return a list of available devices which should not be modified.
	 */
	public static List<String> getDeviceList() {
		if (!isAvailable()) {
			return new ArrayList<String>(0);
		}
		List<DirectMidiInDeviceEntry> list = getDeviceEntryList();
		List<String> ret = new ArrayList<String>(list.size());
		for (int i = 0; i < list.size(); i++) {
			ret.add(list.get(i).toString());
		}
		return ret;
	}

	public static int getMidiDeviceCount() {
		return getDeviceEntryList().size();
	}

	/**
	 * Remember the device listing for a certain time to optimize repeated calls
	 */
	private static List<DirectMidiInDeviceEntry> deviceListCache = null;

	/**
	 * the time in millis when the deviceCache was updated
	 */
	private static long deviceListCacheTime = 0;

	public static List<DirectMidiInDeviceEntry> getDeviceEntryList() {
		if (!isAvailable()) {
			return new ArrayList<DirectMidiInDeviceEntry>(0);
		}
		if (deviceListCache == null
				|| deviceListCacheTime + 2000 < System.currentTimeMillis()) {
			deviceListCache = new ArrayList<DirectMidiInDeviceEntry>(20);
			nFillDeviceNames(deviceListCache);
		}
		return deviceListCache;
	}

	/**
	 * The actual loop of reading from the MIDI device
	 */
	public void run() {
		if (DEBUG_DIRECTMIDIIN) {
			System.out.println("DirectMidiIn: in reading thread");
		}
		inThread = true;
		configChange = true;
		try {
			stopRequested = false;
			Listener l = null;
			Listener[] ls = null;
			while (!stopRequested) {
				try {
					if (configChange) {
						configChange = false;
						if (listeners.size() == 1) {
							l = listeners.get(0);
						} else {
							l = null;
							ls = (Listener[]) listeners.toArray(new Listener[listeners.size()]);
						}
					}

					long ret = nReadShort(handle);
					if (stopRequested) {
						break;
					}
					int status = (int) (ret & 0xFF);
					long time = timestamping?(((ret >> 22) << 10) + clockOffset):0;

					if (ret == 0) {
						Thread.sleep(0, 100);
					} else if (status < 0x80) {
						System.out.println("DirectMidiIn: read returned error code "
								+ status);
					} else if (status == 0xF0) {
						// long event
						int byteLength = nGetLongMessageLength(handle);
						byte[] data = new byte[byteLength + 1]; // plus status
						data[0] = (byte) status;
						int longRet = nReadLong(handle, data, 1, byteLength);
						if (longRet < 0) {
							System.out.println("DirectMidiIn: readLong returned error code "
									+ longRet);
						} else {
							MidiEvent me = new MidiEvent(this, time, data);
							if (l != null) {
								l.midiInReceived(me);
							} else {
								for (Listener li : ls) {
									li.midiInReceived(me);
								}
							}
						}
					} else {
						// short event
						MidiEvent me = new MidiEvent(this, time, status & 0x0F,
								status > 0xF0 ? status : status & 0xF0,
								(int) ((ret >> 8) & 0x7F),
								(int) ((ret >> 15) & 0x7F));
						if (l != null) {
							l.midiInReceived(me);
						} else {
							for (Listener li : ls) {
								li.midiInReceived(me);
							}
						}
					}
				} catch (Throwable t) {
					System.out.println(String.valueOf(t));
				}
			}
		} finally {
			inThread = false;
		}
	}

	// ------------ NATIVE METHODS

	/**
	 * Returns Handle to device
	 * 
	 * @param devName the hardware name of the device
	 */
	private native static long nOpen(String devName);

	/*
	 * Close the device. @return 1 for success, 0 for error, 2 for wait until
	 * read finished
	 */
	private native static int nClose(long handle);

	/**
	 * Read a short MIDI message from the MIDI device. The status is returned in
	 * the low byte, param1 occupies the following 7 bits, and param2 the
	 * following 7 bits. The remaining bits are the low 42 bits of the time
	 * stamp. The time stamp is in nanoseconds right-shifted by 10.
	 * <p>
	 * If the status byte is 0xF7, it means that there is long data to be read
	 * with nRead().
	 * <p>
	 * On error, the status byte is lower than 0x80. The following values are
	 * defined: <br>
	 * 1: error<br>
	 * 2: data without status byte received<br>
	 * 3: device closed
	 * <p>
	 * Internally, this method will read byte by byte. If the read byte is a
	 * non-status byte, it is added to the sys ex queue. Only if a status byte
	 * is encountered, the remainder of the message (0 to 2 data bytes) will be
	 * read, and the short message will be returned. If the status is F7,
	 * pending sys ex data should be read via nRead().
	 * 
	 * @return the packed MIDI message plus time stamp, or an error code in the
	 *         lowest byte.
	 */
	private native static long nReadShort(long handle);

	/**
	 * @return number of bytes to be read for long MIDI data, excluding the
	 *         status byte
	 */
	private native static int nGetLongMessageLength(long handle);

	/**
	 * Read (remaining) sys ex bytes from MIDI device. This method will read at
	 * maximum len bytes, or up to the end of the sys ex message (i.e. F7
	 * received).
	 * 
	 * @param byteArray the array to fill with read audio data
	 * @param offset where to fill the byte array
	 * @param len the maximum number of bytes to read
	 * @return number of bytes read, or a negative error code
	 */
	private native static int nReadLong(long handle, Object byteArray,
			int offset, int len);

	/**
	 * @return the current device time stamp, in nanoseconds
	 */
	private native static long nGetTimeStamp(long handle);

	/**
	 * Fill a list with device description objects.
	 */
	private native static void nFillDeviceNames(
			List<DirectMidiInDeviceEntry> list);

}
