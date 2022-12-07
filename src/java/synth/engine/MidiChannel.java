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
package synth.engine;

/**
 * All parameters of a MIDI channel.
 *
 * @author florian
 *
 */
public class MidiChannel {

	public static boolean DEBUG_MIDICHANNEL = false;

	public final static int BANK_SELECT_MSB = 0;
	public final static int MODULATION = 1;
	public final static int PORTAMENTO_TIME = 5;
	public final static int DATA_ENTRY = 6;
	public final static int VOLUME = 7;
	public final static int PAN = 10;
	public final static int EXPRESSION = 11;
	public final static int BANK_SELECT_LSB = 32;
	public final static int SUSTAIN_PEDAL = 64;
	public final static int PORTAMENTO = 65;
	public final static int SOSTENUTO_PEDAL = 66;
	public final static int SOFT = 67;
	public final static int RESONANCE = 71;
	public final static int RELEASE_TIME = 72;
	public final static int ATTACK_TIME = 73;
	public final static int CUTOFF = 74;
	public final static int DECAY_TIME = 75;
	public final static int VIBRATO_RATE = 76;
	public final static int VIBRATO_DEPTH = 77;
	public final static int VIBRATO_DELAY = 78;
	public final static int REVERB_LEVEL = 91;
	public final static int CHORUS_LEVEL = 93;
	public final static int RPN_LSB = 100;
	public final static int RPN_MSB = 101;
	public final static int ALL_SOUND_OFF = 120;
	public final static int RESET_ALL_CONTROLLERS = 121;
	public final static int ALL_NOTES_OFF = 123;
	public final static int OMNI_MODE_OFF = 124;
	public final static int OMNI_MODE_ON = 125;
	public final static int MONO_MODE = 126;
	public final static int POLY_MODE = 127;

	private final static int[][] DEFAULT_CONTROLLERS = new int[][] {
		{7,100}, // volume
		{10,64}, // pan
		{11,127}, // expression
		{71,64}, // resonance
		{72,64}, // release time
		{73,64}, // attack time
		{74,64}, // cutoff
		{75,64}, // decay time
		{76,64}, // vibrato rate
		{77,64}, // vibrato depth
		{78,64}, // vibrato delay
		{91,40}, // reverb send
	};

	private final static int[][] RESET_CONTROLLERS = new int[][] {
		{1,0}, // modulation
		{11,127}, // expression
		{64,0}, // hold/damper
		{65,0}, // portamento
		{66,0}, // sostenuto
		{67,0}, // soft
		{100,0x7F}, // RPN LSB
		{101,0x7F}, // RPN MSB
	};

	/**
	 * The numeric channel of this MidiChannel object
	 */
	private int channelNum;

	private int[] controllers = new int[128]; // values of all controllers

	/**
	 * The current instantaneous pitch wheel value [-8192...+8191], as set with
	 * MIDI status 0xE0.
	 */
	private int pitchWheel;

	/**
	 * How much will the pitch wheel change the pitch in semitones for one
	 * direction. This can be changed with the RPN.
	 */
	private double pitchWheelSensitivity = 2.0;

	/**
	 * The current channel pressure
	 */
	private int channelPressure;

	/**
	 * bank changes are only committed when a program change is received. So
	 * incoming bank change messages are queued. The LSB is stored in this
	 * variable.
	 */
	private int queuedBankLSB = -1;

	/**
	 * bank changes are only committed when a program change is received. So
	 * incoming bank change messages are queued. The MSB is stored in this
	 * variable.
	 */
	private int queuedBankMSB = -1;

	/**
	 * The current program, [0..127], as set with MIDI status 0xC0.
	 */
	private int program;

	/**
	 * Create a MidiChannel instance with some default values.
	 *
	 * @param channelNum the numeric MIDI channel (e.g. 0..15)
	 */
	public MidiChannel(int channelNum) {
		this.channelNum = channelNum;
		init();
	}

	/**
	 * Sets all controllers to 0 or a defined default value.
	 */
	public void init() {
		// first init everything with 0
		for (int i = 0; i < controllers.length; i++) {
			controllers[i] = 0;
		}
		// then set some default values
		for (int i = 0; i < DEFAULT_CONTROLLERS.length; i++) {
			controllers[DEFAULT_CONTROLLERS[i][0]] = DEFAULT_CONTROLLERS[i][1];
		}
		pitchWheel = 0;
		queuedBankLSB = -1;
		queuedBankMSB = -1;
		program = 0;

		// select drum channel
		if (channelNum == 9) {
			controllers[BANK_SELECT_MSB] = 1;
		}
	}

	/**
	 * Sets some controllers to a defined reset value.
	 */
	public void reset() {
		// set some reset values
		for (int i = 0; i < RESET_CONTROLLERS.length; i++) {
			controllers[RESET_CONTROLLERS[i][0]] = RESET_CONTROLLERS[i][1];
		}
		pitchWheel = 0;
		channelPressure = 0;
	}

	/**
	 * @return Returns the controller value.
	 */
	public int getController(int num) {
		return controllers[num];
	}

	/**
	 * @param num The controller to set.
	 * @param value The value to set it to.
	 */
	public void setControllers(int num, int value) {
		assert (value >= 0 && value < 128);
		this.controllers[num] = value;
	}

	/**
	 * Get the value of a 14-bit controller
	 *
	 * @param num 0..31 for the MSB of the controller to get
	 * @return the controller's value 0..16383
	 */
	public int getController14bit(int num) {
		assert (num >= 0 && num < 32);
		if (controllers[num + 32] >= 0) {
			return (controllers[num] << 7) | controllers[num + 32];
		} else {
			return (controllers[num] << 7);
		}
	}

	/**
	 * @param num 0..127 for the controller number to get
	 * @return the controller's value, normalized 0..1
	 */
	public double getNormalizedController(int num) {
		assert (num >= 0 && num < 128);
		if (num < 32) {
			// simplified calculation cuts the range:
			// if the MSB is 127, the LSB is ignored.
			// But better than not using the full range if the LSB is not used.
			if (controllers[num] == 127) {
				return 1.0;
			}
			return getController14bit(num) / 16256.0; // = 127.0*128.0
		} else {
			return controllers[num] / 127.0;
		}
	}

	/**
	 * @return Returns the current pitch wheel value, -8192..0..+8191.
	 */
	public int getPitchWheel() {
		return pitchWheel;
	}

	/**
	 * @param pitch change the pitch wheel value, -8192..0..+8191.
	 */
	public void setPitchWheel(int pitch) {
		this.pitchWheel = pitch;
	}

	/**
	 * Set the pitch, as two 7-bit values
	 * @param msb the most significant 7-bit byte
	 * @param lsb the most significant 7-bit byte
	 */
	public void setPitchWheel(int msb, int lsb) {
		this.pitchWheel = ((msb << 7) | (lsb & 0x7F)) - 8192;
		if (DEBUG_MIDICHANNEL) {
			System.out.println("set pitch to " + pitchWheel + " = "
					+ getNormalizedPitchWheel());
		}
	}

	/**
	 * @return the current pitch as normalized double value, -1...0...+1
	 */
	public double getNormalizedPitchWheel() {
		return (pitchWheel < 0) ? pitchWheel / 8192.0 : pitchWheel / 8191.0;
	}

	/**
	 * @return How much will the pitch wheel change the pitch in semitones for
	 *         one direction.
	 */
	public double getPitchWheelSensitivity() {
		return pitchWheelSensitivity;
	}

	/**
	 * 
	 */
	public void setPitchWheelSensitivity(double value) {
		pitchWheelSensitivity = value;
	}

	/**
	 * @return the current actual pitch wheel pitch change in semitones
	 */
	public double getPitchWheelSemitones() {
		return getNormalizedPitchWheel() * getPitchWheelSensitivity();
	}

	/**
	 * @return the bank number 0..16383
	 */
	public int getBank() {
		return getController14bit(0);
	}

	/**
	 * @return the current program [0..127]
	 */
	public int getProgram() {
		return program;
	}

	/**
	 * @return the current channel pressure [0..127]
	 */
	public int getChannelPressure() {
		return channelPressure;
	}

	/**
	 * @return the current channel pressure [0..1]
	 */
	public double getNormalizedChannelPressure() {
		return channelPressure / 127.0;
	}

	/**
	 * For incoming MIDI change controller messages, parse and store internally.
	 * Special handling of bank select messages, which only get into effect with
	 * the next program change message.
	 *
	 * @param data1 the first data byte of the CC message (i.e. the controller
	 *            number)
	 * @param data2 the second data byte of the CC message (i.e. the controller
	 *            value)
	 */
	public void parseController(int data1, int data2) {
		if (data1 == BANK_SELECT_MSB) {
			queuedBankMSB = data2;
		} else if (data1 == BANK_SELECT_LSB) {
			queuedBankLSB = data2;
		} else {
			controllers[data1] = data2;
		}
		switch (data1) {
		case MidiChannel.RESET_ALL_CONTROLLERS:
			reset();
			break;
		}
	}

	public void parseProgramChange(int program) {
		this.program = program;
		if (queuedBankLSB != -1 || queuedBankMSB != -1) {
			if (queuedBankLSB != -1 && queuedBankMSB != -1) {
				// commit bank change
				controllers[BANK_SELECT_MSB] = queuedBankMSB;
				controllers[BANK_SELECT_LSB] = queuedBankLSB;
			}
			// reset queued bank change commands
			queuedBankMSB = -1;
			queuedBankLSB = -1;
		}
	}

	public void parseChannelPressure(int pressure) {
		this.channelPressure = pressure;
	}

	public boolean sustainDown() {
		return controllers[SUSTAIN_PEDAL] >= 64;
	}

	public int getChannelNum() {
		return channelNum;
	}

	public String toString() {
		return "MidiChannel " + (channelNum+1);
	}

}
