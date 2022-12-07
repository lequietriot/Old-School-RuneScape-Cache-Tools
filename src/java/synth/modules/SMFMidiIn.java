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
import java.io.ByteArrayInputStream;

/**
 * A class to provide MIDI Input from a Standard MIDI File (SMF)
 * 
 * @author florian
 */

public class SMFMidiIn implements MidiIn, MetaEventListener {

	public static boolean DEBUG_SMF_MIDI_IN = false;

	/**
	 * The Sequencer to use for dispatching
	 */
	private Sequencer sequencer;

	/**
	 * The Sequence containing the MIDI file
	 */
	private Sequence sequence;

	/**
	 * The currently open file
	 */
	private byte[] file;

	/**
	 * The Transmitter retrieved from the sequencer.
	 */
	private Transmitter seqTransmitter;

	/**
	 * The Receiver to use to dispatch the messages received from the
	 * Transmitter
	 */
	private JavaSoundReceiver receiver;

	/**
	 * the offset of the clock in nanoseconds (interface AdjustableAudioClock)
	 */
	private long clockOffset = 0;

	/**
	 * A listener to receive an event when playback stops
	 */
	private SMFMidiInListener stopListener;
	
	/**
	 * the device index
	 */
	private int devIndex = 0;

	/**
	 * Create an SMF MidiIn instance.
	 */
	public SMFMidiIn() {
		receiver = new JavaSoundReceiver(this);
		// do not use the overhead of using this clock.
		receiver.setRemoveTimestamps(true);
	}

	public void addListener(Listener L) {
		receiver.addListener(L);
	}

	public void removeListener(Listener L) {
		receiver.removeListener(L);
	}

	/**
	 * @return Returns the stopListener.
	 */
	public SMFMidiInListener getStopListener() {
		return stopListener;
	}

	/**
	 * @param stopListener The stopListener to set.
	 */
	public void setStopListener(SMFMidiInListener stopListener) {
		this.stopListener = stopListener;
	}

	/** if false, all MIDI events will have time stamp 0 */
	public void setTimestamping(boolean value) {
		receiver.setRemoveTimestamps(!value);
	}

	/** @return the current status of time stamping MIDI events */
	public boolean isTimestamping() {
		return !receiver.isRemovingTimestamps();
	}

	/**
	 * @return the currently loaded file, or <code>null</code>
	 */
	public byte[] getFile() {
		return file;
	}

	public synchronized void open(byte[] file) throws Exception {
		close();
		if (sequencer == null) {
			sequencer = MidiSystem.getSequencer(false);
			if (sequencer.getMaxTransmitters() == 0) {
				throw new Exception(
						"Cannot use system sequencer: does not provide Transmitters!");
			}
		}
		if (DEBUG_SMF_MIDI_IN) {
			System.out.println("Using sequencer: " + sequencer.getDeviceInfo().getName());
		}
		if (DEBUG_SMF_MIDI_IN) {
			System.out.println("Opening MIDI file: " + file);
		}
		sequence = MidiSystem.getSequence(new ByteArrayInputStream(file));
		if (DEBUG_SMF_MIDI_IN) {
			System.out.println("Got MIDI sequence with " + sequence.getTracks().length
					+ " tracks. Duration: "
					+ (sequence.getMicrosecondLength() / 1000000.0)
					+ " seconds.");
		}
		seqTransmitter = sequencer.getTransmitter();
		seqTransmitter.setReceiver(receiver);
		sequencer.setSequence(sequence);
		// register a Meta Event listener that reacts on META event 47: End Of
		// File.
		sequencer.addMetaEventListener(this);
		sequencer.open();
		this.file = file;
		if (DEBUG_SMF_MIDI_IN) {
			System.out.println("Sequencer opened and connected.");
		}
	}

	public synchronized void close() {
		if (seqTransmitter != null) {
			seqTransmitter.setReceiver(null);
		}
		if (sequence != null) {
			sequence = null;
		}
		if (sequencer != null) {
			sequencer.close();
			if (DEBUG_SMF_MIDI_IN) {
				System.out.println("Closed Sequencer.");
			}
		}
		file = null;
	}

	public synchronized boolean isOpen() {
		return (sequencer != null) && (sequencer.isOpen());
	}

	public synchronized boolean isStarted() {
		return isOpen() && sequencer.isRunning();
	}

	public synchronized void start() {
		if (isOpen()) {
			// if at the end, rewind
			if (sequencer.getTickPosition() >= sequencer.getTickLength()) {
				rewind();
			}
			sequencer.start();
			if (DEBUG_SMF_MIDI_IN) {
				System.out.println("Started sequencer. Current position: "
						+ (sequencer.getMicrosecondPosition() / 1000000.0));
			}
		}
	}

	public synchronized void stop() {
		if (isOpen()) {
			sequencer.stop();
			if (DEBUG_SMF_MIDI_IN) {
				System.out.println("Stopped sequencer. Current position: "
						+ (sequencer.getMicrosecondPosition() / 1000000.0));
			}
		}
	}

	public synchronized void rewind() {
		if (isOpen()) {
			sequencer.setTickPosition(0);
		}
	}

	public synchronized void windSeconds(double seconds) {
		if (isOpen()) {
			long newMicroPos = sequencer.getMicrosecondPosition() + ((long) (seconds * 1000000.0));
			if (newMicroPos < 0) {
				newMicroPos = 0;
			} else if (newMicroPos > sequencer.getMicrosecondLength()) {
				newMicroPos = sequencer.getMicrosecondLength();
			}
			sequencer.setMicrosecondPosition(newMicroPos);
		}
	}

	/**
	 * Set the playback position to the percentage
	 * @param percent 0..100
	 */
	public synchronized void setPositionPercent(double percent) {
		if (isOpen()) {
			long tickLen = sequencer.getTickLength();
			long newTickPos = (long) (tickLen * percent / 100.0);
			if (newTickPos < 0) {
				newTickPos = 0;
			} else if (newTickPos > tickLen) {
				newTickPos = tickLen-1;
			}
			sequencer.setTickPosition(newTickPos);
		}
	}

	/**
	 * Get the playback position expressed as a percentage
	 * @return percent 0..100
	 */
	public synchronized double getPositionPercent() {
		if (isOpen()) {
			double tickLen = (double) sequencer.getTickLength();
			double tickPos = (double) sequencer.getTickPosition();
			double percent = (long) (tickPos * 100.0 / tickLen);
			return percent;
		}
		return 0.0;
	}

	public synchronized long getPlaybackPosMillis() {
		if (isOpen()) {
			return sequencer.getMicrosecondPosition() / 1000;
		}
		return 0;
	}

	public synchronized String getPlaybackPosBars() {
		if (isOpen()) {
			long tickPos = sequencer.getTickPosition(); 
			// last number is "frames"
			int frames = (int) tickPos % sequence.getResolution();
			// align frames to a 12 scale
			frames = ((frames * 12) / sequence.getResolution())+1;
			String sFrames;
			if (frames < 10) {
				sFrames = "0"+frames;
			} else {
				sFrames = Integer.toString(frames);
			}
			tickPos /= sequence.getResolution();
			// second number is beats
			int beat = (int) ((tickPos % 4)+1);
			// first number is bars, assume a 4/4 signature
			long bars = (tickPos / 4)+1;
			return Long.toString(bars)+":"+beat+"."+sFrames;
		}
		return "";
	}

	// interface MetaEventListener
	public void meta(MetaMessage event) {
		if (event.getType() == 47) {
			if (stopListener != null) {
				stopListener.onMidiPlaybackStop();
			}
		}
	}

	// interface AudioClock
	public AudioTime getAudioTime() {
		if (sequencer != null) {
			return new AudioTime((sequencer.getMicrosecondPosition() * 1000L)
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

	public int getInstanceIndex() {
		return devIndex;
	}
	
	public void setDeviceIndex(int index) {
		this.devIndex = index;
	}

	public String toString() {
		if (isOpen()) {
			return "SMFMidiIn " + file;
		}
		return "SMFMidiIn";
	}


	
}
