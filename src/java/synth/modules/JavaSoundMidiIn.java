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
import synth.engine.MidiIn;

import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for handling incoming MIDI events from a Java Sound MIDI device. This
 * class can only open one MIDI IN device, because it provides a clock, and
 * multiple MIDI IN devices would cause clock confusion.
 * <p>
 * In order to open several MIDI IN ports, create multiple instances of this
 * class.
 * 
 * @author florian
 */

public class JavaSoundMidiIn implements MidiIn {

	public static boolean DEBUG_JSMIDIIN = false;

	/**
	 * The instance of the selected device
	 */
	private MidiDevice midiDev;

	/**
	 * The Transmitter retrieved from the MIDI device.
	 */
	private Transmitter midiInTransmitter;

	/**
	 * The Receiver to use to dispatch the messages received from the
	 * Transmitter
	 */
	private JavaSoundReceiver receiver;

	/**
	 * List of usable MIDI devices, i.e. they provide a MIDI IN port.
	 */
	private static List<DevInfo> devList;

	/**
	 * the offset of the clock in nanoseconds
	 */
	private long clockOffset = 0;

	/**
	 * The index of this instance -- an arbitrary value to be used by other
	 * classes
	 */
	private int instanceIndex;

	/**
	 * Create a Java Sound MidiIn instance.
	 */
	public JavaSoundMidiIn() {
		this(0);
	}

	/**
	 * Create a Java Sound MidiIn instance with the given device index.
	 */
	public JavaSoundMidiIn(int instanceIndex) {
		this.instanceIndex = instanceIndex;
		setupMidiDevices();
		receiver = new JavaSoundReceiver(this);
	}

	public void addListener(Listener L) {
		receiver.addListener(L);
	}

	public void removeListener(Listener L) {
		receiver.removeListener(L);
	}

	/** if false, all MIDI events will have time stamp 0 */
	public void setTimestamping(boolean value) {
		receiver.setRemoveTimestamps(!value);
	}

	/** @return the current status of time stamping MIDI events */
	public boolean isTimestamping() {
		return !receiver.isRemovingTimestamps();
	}

	public synchronized void open(int devIndex) throws Exception {
		if (devList.size() == 0) {
			throw new Exception("no MIDI IN devices available!");
		}
		if (devIndex < 0 || devIndex >= devList.size()) {
			throw new Exception("selected MIDI IN device ID out of range");
		}
		DevInfo info = devList.get(devIndex);
		if (midiDev != null) {
			midiDev.close();
			midiDev = null;
		}
		midiDev = MidiSystem.getMidiDevice(info.info);
		if (!midiDev.isOpen()) {
			midiDev.open();
		}
		midiInTransmitter = midiDev.getTransmitter();
		// connect the device with this instance as Receiver
		receiver.setID(devIndex);
		midiInTransmitter.setReceiver(receiver);
		if (DEBUG_JSMIDIIN) {
			System.out.println("Opened MIDI IN device '" + info.info + "'");
		}
	}

	public synchronized void close() {
		if (midiInTransmitter != null) {
			midiInTransmitter.setReceiver(null);
		}
		if (midiDev != null) {
			DevInfo devInfo = null;
			for (DevInfo devInfo2 : devList) {
				if (devInfo2.info == midiDev.getDeviceInfo()) {
					devInfo = devInfo2;
					break;
				}
			}
			if (DEBUG_JSMIDIIN) {
				System.out.println("Closing MIDI IN device '" + devInfo + "'");
			}
			try {
				midiDev.close();
			} catch (Exception e) {
				System.out.println(String.valueOf(e));
			}
			midiDev = null;
		}
	}

	public synchronized boolean isOpen() {
		return (midiDev != null) && (midiDev.isOpen());
	}

	// interface AudioClock
	public synchronized AudioTime getAudioTime() {
		if (midiDev != null) {
			assert (midiDev.getMicrosecondPosition() != -1);
			return new AudioTime((midiDev.getMicrosecondPosition() * 1000L)
					+ clockOffset);
		}
		return new AudioTime(0);
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

	public String toString() {
		if (isOpen()) {
			return "JSMidiIn " + midiDev.getDeviceInfo().getName();
		}
		return "JSMidiIn";
	}

	// static portion to maintain a list of MIDI devices

	public static List<DevInfo> getDeviceList() {
		setupMidiDevices();
		return devList;
	}

	private static void setupMidiDevices() {
		if (DEBUG_JSMIDIIN) System.out.println("Gathering MIDI devices...");
		if (devList == null) {
			devList = new ArrayList<DevInfo>();
			MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
			// go through all MIDI devices and see if they are MIDI IN
			for (MidiDevice.Info info : infos) {
				try {
					MidiDevice dev = MidiSystem.getMidiDevice(info);
					if (!(dev instanceof Sequencer)
							&& !(dev instanceof Synthesizer)
							&& (dev.getMaxTransmitters() != 0)) {
						devList.add(new DevInfo(info));
					}
				} catch (MidiUnavailableException mue) {
					System.out.println(String.valueOf(mue));
				}
			}
		}
		if (DEBUG_JSMIDIIN) {
			System.out.println("done (" + devList.size() + " devices available).");
		}
	}

	public static int getMidiDeviceCount() {
		setupMidiDevices();
		return devList.size();
	}

	/**
	 * @return the name of the open device, or a generic name if not open
	 */
	public String getName() {
		if (isOpen()) {
			return midiDev.getDeviceInfo().getName();
		}
		return "JavaSoundMidiIn";
	}

	public int getInstanceIndex() {
		return instanceIndex;
	}

	/**
	 * A wrapper for the Java Sound MidiDevice.Info object.
	 * 
	 * @author florian
	 */
	private static class DevInfo {
		MidiDevice.Info info;

		public DevInfo(MidiDevice.Info info) {
			this.info = info;
		}

		public String toString() {
			return info.toString();
		}
	}

}
