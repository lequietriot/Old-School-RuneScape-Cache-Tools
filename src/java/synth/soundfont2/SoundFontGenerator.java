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
package synth.soundfont2;

/**
 * Class to manage a generator on preset level
 * 
 * @author florian
 * 
 */
public class SoundFontGenerator {

	// index generators
	public final static int INSTRUMENT = 41;
	public final static int SAMPLE_ID = 53;

	// "normal" generators
	public final static int START_ADDRS_OFFSET = 0;
	public final static int END_ADDRS_OFFSET = 1;
	public final static int START_LOOP_ADDRS_OFFSET = 2;
	public final static int END_LOOP_ADDRS_OFFSET = 3;
	public final static int START_ADDRS_COARSE_OFFSET = 4;
	public final static int MODLFOTO_PITCH = 5;
	public final static int VIBLFOTO_PITCH = 6;
	public final static int MODENVTO_PITCH = 7;
	public final static int INITIALFILTER_FC = 8;
	public final static int INITIALFILTER_Q = 9;
	public final static int MODLFOTOFILTER_FC = 10;
	public final static int MODENVTOFILTER_FC = 11;
	public final static int ENDADDRSCOARSE_OFFSET = 12;
	public final static int MOD_LFO_TO_VOLUME = 13;
	public final static int CHORUS_EFFECTS_SEND = 15;
	public final static int REVERB_EFFECTS_SEND = 16;
	public final static int PAN = 17;
	public final static int DELAY_MOD_LFO = 21;
	public final static int FREQ_MOD_LFO = 22;
	public final static int DELAY_VIB_LFO = 23;
	public final static int FREQ_VIB_LFO = 24;
	public final static int DELAY_MOD_ENV = 25;
	public final static int ATTACK_MOD_ENV = 26;
	public final static int HOLD_MOD_ENV = 27;
	public final static int DECAY_MOD_ENV = 28;
	public final static int SUSTAIN_MOD_ENV = 29;
	public final static int RELEASE_MOD_ENV = 30;
	public final static int KEYNUM_TO_MOD_ENV_HOLD = 31;
	public final static int KEYNUM_TO_MOD_ENV_DECAY = 32;
	public final static int DELAY_VOL_ENV = 33;
	public final static int ATTACK_VOL_ENV = 34;
	public final static int HOLD_VOL_ENV = 35;
	public final static int DECAY_VOL_ENV = 36;
	public final static int SUSTAIN_VOL_ENV = 37;
	public final static int RELEASE_VOL_ENV = 38;
	public final static int KEYNUM_TO_VOL_ENV_HOLD = 39;
	public final static int KEYNUM_TO_VOL_ENV_DECAY = 40;
	public final static int KEY_RANGE = 43;
	public final static int VEL_RANGE = 44;
	public final static int START_LOOP_ADDRS_COARSE_OFFSET = 45;
	public final static int KEYNUM = 46;
	public final static int VELOCITY = 47;
	public final static int INITIAL_ATTENUATION = 48;
	public final static int END_LOOP_ADDRS_COARSE_OFFSET = 50;
	public final static int COARSE_TUNE = 51;
	public final static int FINE_TUNE = 52;
	public final static int SAMPLE_MODES = 54;
	public final static int SCALE_TUNING = 56;
	public final static int EXCLUSIVE_CLASS = 57;
	public final static int OVERRIDING_ROOTKEY = 58;

	private int op;
	private short amount;

	public SoundFontGenerator(int op, short amount) {
		super();
		this.op = op;
		this.amount = amount;
	}

	/**
	 * @return Returns the amount.
	 */
	public short getAmount() {
		return amount;
	}

	/**
	 * @return Returns the op.
	 */
	public int getOp() {
		return op;
	}

	public static String generator2String(int op) {
		String s;
		switch (op) {

		case INSTRUMENT:
			s = "INSTRUMENT";
			break;

		case SAMPLE_ID:
			s = "SAMPLE_ID";
			break;

		case START_ADDRS_OFFSET:
			s = "START_ADDRS_OFFSET";
			break;

		case END_ADDRS_OFFSET:
			s = "END_ADDRS_OFFSET";
			break;

		case START_LOOP_ADDRS_OFFSET:
			s = "START_LOOP_ADDRS_OFFSET";
			break;

		case END_LOOP_ADDRS_OFFSET:
			s = "END_LOOP_ADDRS_OFFSET";
			break;

		case START_ADDRS_COARSE_OFFSET:
			s = "START_ADDRS_COARSE_OFFSET";
			break;

		case MODLFOTO_PITCH:
			s = "MODLFOTO_PITCH";
			break;

		case VIBLFOTO_PITCH:
			s = "VIBLFOTO_PITCH";
			break;

		case MODENVTO_PITCH:
			s = "MODENVTO_PITCH";
			break;

		case INITIALFILTER_FC:
			s = "INITIALFILTER_FC";
			break;

		case INITIALFILTER_Q:
			s = "INITIALFILTER_Q";
			break;

		case MODLFOTOFILTER_FC:
			s = "MODLFOTOFILTER_FC";
			break;

		case MODENVTOFILTER_FC:
			s = "MODENVTOFILTER_FC";
			break;

		case ENDADDRSCOARSE_OFFSET:
			s = "ENDADDRSCOARSE_OFFSET";
			break;

		case MOD_LFO_TO_VOLUME:
			s = "MOD_LFO_TO_VOLUME";
			break;

		case CHORUS_EFFECTS_SEND:
			s = "CHORUS_EFFECTS_SEND";
			break;

		case REVERB_EFFECTS_SEND:
			s = "REVERB_EFFECTS_SEND";
			break;

		case PAN:
			s = "PAN";
			break;

		case DELAY_MOD_LFO:
			s = "DELAY_MOD_LFO";
			break;

		case FREQ_MOD_LFO:
			s = "FREQ_MOD_LFO";
			break;

		case DELAY_VIB_LFO:
			s = "DELAY_VIB_LFO";
			break;

		case FREQ_VIB_LFO:
			s = "FREQ_VIB_LFO";
			break;

		case DELAY_MOD_ENV:
			s = "DELAY_MOD_ENV";
			break;

		case ATTACK_MOD_ENV:
			s = "ATTACK_MOD_ENV";
			break;

		case HOLD_MOD_ENV:
			s = "HOLD_MOD_ENV";
			break;

		case DECAY_MOD_ENV:
			s = "DECAY_MOD_ENV";
			break;

		case SUSTAIN_MOD_ENV:
			s = "SUSTAIN_MOD_ENV";
			break;

		case RELEASE_MOD_ENV:
			s = "RELEASE_MOD_ENV";
			break;

		case KEYNUM_TO_MOD_ENV_HOLD:
			s = "KEYNUM_TO_MOD_ENV_HOLD";
			break;

		case KEYNUM_TO_MOD_ENV_DECAY:
			s = "KEYNUM_TO_MOD_ENV_DECAY";
			break;

		case DELAY_VOL_ENV:
			s = "DELAY_VOL_ENV";
			break;

		case ATTACK_VOL_ENV:
			s = "ATTACK_VOL_ENV";
			break;

		case HOLD_VOL_ENV:
			s = "HOLD_VOL_ENV";
			break;

		case DECAY_VOL_ENV:
			s = "DECAY_VOL_ENV";
			break;

		case SUSTAIN_VOL_ENV:
			s = "SUSTAIN_VOL_ENV";
			break;

		case RELEASE_VOL_ENV:
			s = "RELEASE_VOL_ENV";
			break;

		case KEYNUM_TO_VOL_ENV_HOLD:
			s = "KEYNUM_TO_VOL_ENV_HOLD";
			break;

		case KEYNUM_TO_VOL_ENV_DECAY:
			s = "KEYNUM_TO_VOL_ENV_DECAY";
			break;

		case KEY_RANGE:
			s = "KEY_RANGE";
			break;

		case VEL_RANGE:
			s = "VEL_RANGE";
			break;

		case START_LOOP_ADDRS_COARSE_OFFSET:
			s = "START_LOOP_ADDRS_COARSE_OFFSET";
			break;

		case KEYNUM:
			s = "KEYNUM";
			break;

		case VELOCITY:
			s = "VELOCITY";
			break;

		case INITIAL_ATTENUATION:
			s = "INITIAL_ATTENUATION";
			break;

		case END_LOOP_ADDRS_COARSE_OFFSET:
			s = "END_LOOP_ADDRS_COARSE_OFFSET";
			break;

		case COARSE_TUNE:
			s = "COARSE_TUNE";
			break;

		case FINE_TUNE:
			s = "FINE_TUNE";
			break;

		case SAMPLE_MODES:
			s = "SAMPLE_MODES";
			break;

		case SCALE_TUNING:
			s = "SCALE_TUNING";
			break;

		case EXCLUSIVE_CLASS:
			s = "EXCLUSIVE_CLASS";
			break;

		case OVERRIDING_ROOTKEY:
			s = "OVERRIDING_ROOTKEY";
			break;

		default:
			s = "(unknown " + op + ")";

		}
		return s;
	}

	public String toString() {
		return "Generator: op=" + generator2String(op) + " amount=" + amount;
	}

}
