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
package synth.utils;

import static synth.engine.MidiChannel.*;

/**
 * Utility methods for MIDI.
 * @author florian
 *
 */
public class MidiUtils {
	
	/**
	 * @param note [0..127] the MIDI note number
	 * @return the octave number of the given MIDI note number, [-1..9]
	 */
	public final static int getOctaveFromNoteNumber(int note) {
		return (note/12)-1;
	}

	/**
	 * @param note [0..127] the MIDI note number
	 * @return the note part inside an octave [0..11]
	 */
	public final static int getNoteFromNoteNumber(int note) {
		return (note % 12);
	}
	
	/**
	 * @param octave the octave of the specified note [-1..9]
	 * @param note the note part of the specified note [0..11]
	 * @return the MIDI note number, [0..127]
	 */
	public final static int getNoteNumber(int octave, int note) {
		return ((octave+1)*12) + note;
	}

	private final static boolean[] iswhite = {
		// C,   C#,   D,    D#,    E,    F,    F#,     
		true, false, true, false, true, true, false, 
		// G,  G#,    A,    A#,    B
		true, false, true, false, true
	};
	
	public final static boolean isWhiteKey(int note) {
		return (note>=0) && (note<128) && iswhite[note % 12];
	}
	
	public final static boolean isBlackKey(int note) {
		return (note>=0) && (note<128) && !iswhite[note % 12];
	}
	
	public final static String getControllerName(int ctrl) {
		switch (ctrl) {
		case BANK_SELECT_MSB: return "Bank Select MSB";
		case MODULATION: return "Modulation";
		case PORTAMENTO_TIME: return "Portamento Time";
		case DATA_ENTRY: return "Data Entry";
		case VOLUME: return "Channel Volume";
		case PAN: return "Panorama";
		case EXPRESSION: return "Expression";
		case BANK_SELECT_LSB: return "Bank Select LSB";
		case SUSTAIN_PEDAL: return "Sustain Switch";
		case PORTAMENTO: return "Portamento Switch";
		case SOSTENUTO_PEDAL: return "Sostenuto Switch";
		case SOFT: return "Soft";
		case RESONANCE: return "Resonance";
		case RELEASE_TIME: return "Release Time";
		case ATTACK_TIME: return "Attack Time";
		case CUTOFF: return "Cutoff";
		case DECAY_TIME: return "Decay Time";
		case VIBRATO_RATE: return "Vibrato Rate";
		case VIBRATO_DEPTH: return "Vibrato Depth";
		case VIBRATO_DELAY: return "Vibrato Delay";
		case REVERB_LEVEL: return "Reverb Level";
		case CHORUS_LEVEL: return "Chorus Level";
		case RPN_LSB : return "RPN LSB";
		case RPN_MSB: return "RPN MSB";
		case ALL_SOUND_OFF: return "All Sound Off";
		case RESET_ALL_CONTROLLERS: return "Reset All Controllers";
		case ALL_NOTES_OFF: return "All Notes Off";
		case OMNI_MODE_OFF: return "Omni Mode Off";
		case OMNI_MODE_ON: return "Omni Mode On";
		case MONO_MODE: return "Mono Mode";
		case POLY_MODE: return "Poly Mode";
		}
		return "(unknown "+ctrl+")";
	}

}
