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
package synth.test;

import synth.engine.MidiEvent;
import synth.engine.Synthesizer;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import java.io.ByteArrayInputStream;

/**
 * Load a MIDI file and push its events directly to a synthesizer's queue.
 * 
 * @author florian
 */

public class SMFPusher {
	
	private final static boolean DEBUG_SMF_PUSHER = false;

	/**
	 * The Sequence containing the MIDI file
	 */
	private Sequence sequence;
	
	private double durationSeconds = 0.0;

	public void open(byte[] file) throws Exception {
		sequence = MidiSystem.getSequence(new ByteArrayInputStream(file));
		if (DEBUG_SMF_PUSHER) {
			System.out.println("Got MIDI sequence with " + sequence.getTracks().length
					+ " tracks. Duration: "
					+ (sequence.getMicrosecondLength() / 1000000.0)
					+ " seconds.");
		}
	}

	/**
	 * Send all events to the synth and let it schedule them.
	 * We do not need to order the tracks' events, since the 
	 * synth should do it on its own. This method plays the file 
	 * at 120bpm and ignores any tempo change events. 
	 * @return the number of events scheduled to the synth
	 */
	public int pushToSynth(Synthesizer synth) {
		if (DEBUG_SMF_PUSHER) {
			System.out.println("Pushing the MIDI file to the synth...");
		}
		durationSeconds = 0.0;
		int events = 0;
		// TODO: keep track of tempo
		double tempo = 120.0;
		double tickFactor = 60.0/(tempo*((double)sequence.getResolution()));
		Track[] tracks = sequence.getTracks();
		for (Track track:tracks) {
			int trackSize = track.size();
			double timeSeconds = 0.0;
			for (int i = 0; i<trackSize; i++) {
				javax.sound.midi.MidiEvent event = track.get(i);
				timeSeconds = event.getTick() * tickFactor;
				long nanoTime = (long) (timeSeconds * 1000000000.0);
				byte[] msg = event.getMessage().getMessage();
				if (msg.length>1 && msg.length<=3) {
					int data2 = 0;
					if (msg.length==3) {
						data2 = (int) msg[2];
					}
					int channel = msg[0] & 0xF;
					int status = msg[0] & 0xF0;
					if (synth!=null) {
						MidiEvent me = new MidiEvent(null, nanoTime, channel, status, msg[1], data2);
						synth.midiInReceived(me);
					}
					if (status == 0x90 && data2 > 0) {
						events++;
					}
				}
			}
			if (timeSeconds > durationSeconds) {
				durationSeconds = timeSeconds;
			}
		}
		if (DEBUG_SMF_PUSHER) {
			System.out.println("...done: scheduled "+events+" Note On events to synth.");
		}
		return events;
	}
	
	public double getDurationInSeconds() {
		return durationSeconds;
	}
	
}
