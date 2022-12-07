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

import synth.engine.AudioTime;

import static synth.soundfont2.SoundFontUtils.*;

// TODO: verify relative values (add*() methods) work correctly.

/**
 * A low frequency oscillator for the SoundFont synth.
 *
 * @author florian
 */

public class SoundFontLFO {

	public static boolean DEBUG_LFO = false;

	// for debugging
	String name;

	// setup variables

	/**
	 * How much this LFO will act on pitch, in semitones. A value of 1 means
	 * that this LFO will vary pitch in between -1...+1 semitones.
	 */
	private double pitch = 0.0;

	/**
	 * This static value is always added to the pitch at runtime.
	 */
	private double runtimePitchOffset = 0.0;

	/**
	 * How much this LFO will act on volume, in dB. A value of 1 means that this
	 * LFO will vary volume in between -1dB...+1dB.
	 */
	private double volume = 0.0;

	/**
	 * How much this LFO will act on filter cut off, in semitones. A value of 12
	 * means that this LFO will vary cutoff frequency in between -1...+1 octaves
	 * (i.e. half frequency to double frequency).
	 */
	private double cutoff = 0.0;

	/**
	 * The delay, in seconds
	 */
	private double delay = DEFAULT_ARTICULATION_DELAY;

	/**
	 * The frequency, in hertz.
	 */
	private double frequency = cents2hertzLFO(0);

	// runtime variables

	/**
	 * The start time, in microseconds, of this LFO. It is initialized in the
	 * setup() method.
	 */
	private long startTime;

	/**
	 * The period of one entire "run" of the oscillator, in microseconds.
	 */
	private double period;

	/**
	 * The current value, [-1.0 ... 0.0 ... +1.0]
	 */
	private double value;

	/**
	 * Create an LFO without any influence on pitch, volume, or cutoff.
	 */
	public SoundFontLFO(AudioTime time) {
		startTime = time.getMicroTime();
	}

	/**
	 * Initialize internal runtime variables. Must be called after pitch,
	 * volume, cutoff, delay, and frequency are finalized.
	 */
	public void setup() {
		// add the delay to startTime
		startTime += (long) (delay * 1000000.0);
		if (frequency != 0.0) {
			period = 1000000.0 / (double) frequency;
		} else {
			period = 1000000.0;
		}
		value = 0.0; // start value
	}

	private final double getRemainder(double v) {
		return v - ((int) v);
	}

	/**
	 * Calculate the current value for the given time. Use getCurrent*() to
	 * retrieve the current LFO values in the respective unit.
	 */
	public void calculate(AudioTime time) {
		double curr = (double) (time.getMicroTime() - startTime);
		if (curr < 0.0) {
			value = 0.0;
		} else {
			// a triangle that "starts" 1/4th of a period before the start time.
			double periodFraction =
					getRemainder((curr + (period / 4.0)) / period);
			if (periodFraction < 0.5) {
				// first phase: "uphill"
				value = (periodFraction - 0.25) * 4.0;
			} else {
				// second phase: "downhill"
				value = (0.75 - periodFraction) * 4.0;
			}
		}
	}

	/**
	 * @return Returns the current cutoff in relative "semitones".
	 */
	public double getCurrentCutoff() {
		return cutoff * value;
	}

	/**
	 * @param cutoff The cutoff to set.
	 */
	void setCutoff(double cutoff) {
		this.cutoff = cutoff;
		if (DEBUG_LFO) {
			System.out.println(" " + name + ": set cutoff to " + (this.cutoff)
					+ " semitones");
		}
	}

	/**
	 * @param cutoff The cutoff to add.
	 */
	void addCutoff(double cutoff) {
		this.cutoff += cutoff;
		if (DEBUG_LFO) {
			System.out.println(" " + name + ": add cutoff by " + (cutoff)
					+ ", new value: " + (this.cutoff) + " semitones");
		}
	}

	/**
	 * @return Returns the delay.
	 */
	public double getDelay() {
		return delay;
	}

	/**
	 * @param timecents The delay to set in time cents.
	 */
	void setDelayTimeCents(int timecents) {
		this.delay = timecents2seconds(timecents);
		if (DEBUG_LFO) {
			System.out.println(" " + name + ": set delay to " + timecents + " cents, -> "
					+ (delay) + "s");
		}
	}

	/**
	 * @param timecents The delay to add in time cents.
	 */
	void addDelayTimeCents(int timecents) {
		this.delay *= timecents2seconds(timecents);
		if (DEBUG_LFO) {
			System.out.println(" " + name + ": add " + timecents + " cents, new delay: "
					+ (this.delay) + "s");
		}
	}

	/**
	 * @return Returns the frequency.
	 */
	public double getFrequency() {
		return frequency;
	}

	/**
	 * @param cents The frequency to set in cents [-32768...+32767]
	 */
	void setFrequencyCents(int cents) {
		this.frequency = cents2hertzLFO(cents);
		if (DEBUG_LFO) {
			System.out.println(" " + name + ": set freq to " + cents + " cents, -> "
					+ (this.frequency) + " Hz");
		}
	}

	/**
	 * @param cents The frequency to add in cents [-32768...+32767]
	 */
	void addFrequencyCents(int cents) {
		this.frequency *= cents2hertzLFO(cents);
		if (DEBUG_LFO) {
			System.out.println(" " + name + ": add freq by " + cents + " cents, new value: "
					+ (this.frequency) + " Hz");
		}
	}

	/**
	 * @return Returns the current pitch offset in semitones.
	 */
	public double getCurrentPitch() {
		return (pitch + runtimePitchOffset) * value;
	}

	/**
	 * @param pitch The pitch to set.
	 */
	void setPitch(double pitch) {
		this.pitch = pitch;
		if (DEBUG_LFO) {
			System.out.println(" " + name + ": set pitch to " + (this.pitch)
					+ " semitones");
		}
	}

	/**
	 * @param pitch The pitch to add.
	 */
	void addPitch(double pitch) {
		this.pitch += pitch;
		if (DEBUG_LFO) {
			System.out.println(" " + name + ": add pitch by " + (pitch)
					+ ", new value: " + (this.pitch) + " semitones");
		}
	}

	/**
	 * @return Returns the pitch.
	 */
	public double getPitch() {
		return pitch;
	}

	/**
	 * @param runtimePitch The pitch offset to set.
	 */
	public void setRuntimePitchOffset(double runtimePitch) {
		this.runtimePitchOffset = runtimePitch;
	}

	/**
	 * @return the current runtime pitch offset
	 */
	public double getRuntimePitchOffset() {
		return runtimePitchOffset;
	}

	/**
	 * @return Returns the current volume offset in deciBel.
	 */
	public double getCurrentVolume() {
		return volume * value;
	}

	/**
	 * @param volume The volume to set.
	 */
	void setVolume(double volume) {
		this.volume = volume;
		if (DEBUG_LFO) {
			System.out.println(" " + name + ": set volume to " + (this.volume)
					+ " dB");
		}
	}

	/**
	 * @param volume The volume to add.
	 */
	void addVolume(double volume) {
		this.volume += volume;
		if (DEBUG_LFO) {
			System.out.println(name + ": add volume by " + (volume) + ", new value: "
					+ (this.volume) + " dB");
		}
	}

	/**
	 * @return Returns the volume.
	 */
	public double getVolume() {
		return volume;
	}

	public String toString() {
		return name + ": pitch=" + (this.pitch) + " semitones, vol="
				+ (this.volume) + "dB, cutoff=" + (this.cutoff)
				+ " semitones, delay=" + (delay) + "s, freq="
				+ (this.frequency) + " Hz.";
	}

}
