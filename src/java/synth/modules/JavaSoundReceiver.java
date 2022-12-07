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

import synth.engine.MidiEvent;
import synth.engine.MidiIn;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.ArrayList;
import java.util.List;


public class JavaSoundReceiver implements Receiver {

	public static boolean DEBUG_JSMIDIIN_IO = false;
	
	/**
	 * The listeners that will receive incoming MIDI messages.
	 */
	private List<MidiIn.Listener> listeners = new ArrayList<MidiIn.Listener>();

	// private int ID;

	private MidiIn owner;

	/**
	 * If false, use the time of the incoming MIDI events, otherwise push MIDI
	 * events to the listener with time 0, i.e. "now"
	 */
	private boolean removeTimestamps = false;

	public JavaSoundReceiver(MidiIn owner) {
		this.owner = owner;
	}

	public void addListener(MidiIn.Listener L) {
		synchronized (listeners) {
			this.listeners.add(L);
		}
	}

	public void removeListener(MidiIn.Listener L) {
		synchronized (listeners) {
			this.listeners.remove(L);
		}
	}

	public void setID(int ID) {
		// this.ID = ID;
	}

	/**
	 * @return Returns the value of removeTimestamps.
	 */
	public boolean isRemovingTimestamps() {
		return removeTimestamps;
	}

	/**
	 * @param removeTimestamps
	 */
	public void setRemoveTimestamps(boolean removeTimestamps) {
		this.removeTimestamps = removeTimestamps;
	}

	/**
	 * Send a short message to the registered listeners.
	 * 
	 * @param microTime the original device time of the event in microseconds
	 * @param status MIDI status byte
	 * @param data1 1st MIDI data byte
	 * @param data2 2nd MIDI data byte
	 */
	private void dispatchMessage(long microTime, int status, int data1,
			int data2) {
		
		int channel;
		if (status < 0xF0) {
			// normal channel messages
			channel = status & 0x0F;
			status &= 0xF0;
		} else {
			// real time/system messages
			channel = 0;
		}
		long nanoTime;
		if (microTime == -1 || removeTimestamps) {
			if (removeTimestamps) {
				// let the receiver schedule!
				nanoTime = 0;
			} else {
				nanoTime = owner.getAudioTime().getNanoTime();
			}
		} else {
			nanoTime =
					(microTime * 1000L) + owner.getTimeOffset().getNanoTime();
		}
		synchronized (listeners) {
			for (MidiIn.Listener listener : listeners) {
				listener.midiInReceived(new MidiEvent(owner, nanoTime, channel,
						status, data1, data2));
			}
		}
	}

	/**
	 * Send a long message to the registered listeners.
	 * 
	 * @param microTime the original device time of the event in microseconds
	 * @param msg the actual message
	 */
	private void dispatchMessage(long microTime, byte[] msg) {
		if (DEBUG_JSMIDIIN_IO) {
			System.out.println("JavaSoundReceiver: time="+(microTime/1000)+"ms "
					+" long msg, length="+msg.length);
		}
		long nanoTime;
		if (microTime == -1 || removeTimestamps) {
			if (removeTimestamps) {
				// let the receiver schedule!
				nanoTime = 0;
			} else {
				nanoTime = owner.getAudioTime().getNanoTime();
			}
		} else {
			nanoTime =
					(microTime * 1000L) + owner.getTimeOffset().getNanoTime();
		}
		synchronized (listeners) {
			for (MidiIn.Listener listener : listeners) {
				listener.midiInReceived(new MidiEvent(owner, nanoTime, msg));
			}
		}
	}

	public void send(MidiMessage message, long timeStamp) {
		// timestamp should be in microseconds
		if (message.getLength() <= 3) {
			if (message instanceof ShortMessage) {
				ShortMessage sm = (ShortMessage) message;
				dispatchMessage(timeStamp, sm.getStatus(), sm.getData1(),
						sm.getData2());
			} else {
				int data1 = 0;
				int data2 = 0;
				if (message.getLength() > 1) {
					byte[] msg = message.getMessage();
					data1 = msg[1] & 0xFF;
					if (message.getLength() > 2) {
						data2 = msg[2] & 0xFF;
					}
				}
				dispatchMessage(timeStamp, message.getStatus(), data1, data2);
			}
		} else {
			dispatchMessage(timeStamp, message.getMessage());
		}
	}

	public void close() {
		// nothing to do?
	}

}
