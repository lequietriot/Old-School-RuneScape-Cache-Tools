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

import static synth.soundfont2.SoundFontUtils.DEFAULT_ARTICULATION_DELAY;
import static synth.soundfont2.SoundFontUtils.timecents2seconds;

/**
 * The envelope generator for SoundFont voices.
 *
 * @author florian
 *
 */

public class SoundFontEnvelope {

	public static boolean DEBUG_EG = false;

	// the different segments
	public final static int DELAY = 0;
	public final static int ATTACK = 1;
	public final static int HOLD = 2;
	public final static int DECAY = 3;
	public final static int SUSTAIN = 4;
	public final static int RELEASE = 5;
	
	/**
	 * Ensure that the release time is at least this value, in seconds
	 * to prevent a sharp drop to zero resulting in a click.
	 */
	private final static double MIN_RELEASE_TIME = 0.003; 

	// for debugging
	String name;

	// setup variables

	/**
	 * How much this envelope will act on pitch, in semitones. A value of 1
	 * means that this envelope will vary the pitch between 0...+1 semitones.
	 */
	private double pitch = 0.0;

	/**
	 * How much this envelope will act on volume, in dB. A value of 1 means that
	 * this envelope will vary volume in between 0dB...-1dB.
	 */
	private double volume = 0.0;

	/**
	 * How much this envelope will act on filter cut off, in semitones. A value
	 * of 12 means that this envelope will vary cutoff frequency between 0...+1
	 * octaves (i.e. current frequency to double frequency).
	 */
	private double cutoff = 0.0;

	/**
	 * The segment values. segmentValue[SUSTAIN] is a percentage, all other
	 * values are time in seconds.
	 */
	private double[] segmentValue = new double[6];

	/**
	 * The keyNumTo*EnvHold generator, used to apply a key-depending factor to
	 * the hold time.
	 */
	private int keyNumToHoldTimeCents = 0;

	// the envelope phases

	/**
	 * The keyNumTo*EnvDecay generator, used to apply a key-depending factor to
	 * the decay time.
	 */
	private int keyNumToDecayTimeCents = 0;

	// runtime variables

	/**
	 * The played key/note
	 */
	private int key;

	/**
	 * The start time of this instrument in nanoseconds
	 */
	private long startTime;

	/**
	 * The start time, in seconds, of the current segment (relative to
	 * startTime).
	 */
	private double segmentStartTime;

	/**
	 * The start time, in seconds, of the next segment (relative to startTime).
	 */
	private double nextSegmentStartTime;

	/**
	 * The current value, [0.0 ... +1.0]
	 */
	private double value;

	/**
	 * The current value at the time of releasing the key.
	 */
	private double releaseLevel;

	/**
	 * The current segment (DELAY, ATTACK, etc.)
	 */
	private int segment;

	/**
	 * Create an envelope without any influence on pitch, volume, or cutoff.
	 */
	public SoundFontEnvelope(AudioTime time) {
		startTime = time.getNanoTime();
		segmentStartTime = 0.0;
		// set default values
		for (int i = 0; i < segmentValue.length; i++) {
			segmentValue[i] = DEFAULT_ARTICULATION_DELAY;
		}
		// sustain value has a different default
		segmentValue[SUSTAIN] = 0.0;
		// make it snappier by setting attack and delay to 0 by default. This
		// violates the spec slightly, but for a good cause :)
		segmentValue[DELAY] = 0.0;
		segmentValue[ATTACK] = 0.0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.Envelope#setup()
	 */
	public void setup(int note, int vel) {
		this.key = note;
		advanceSegment(DELAY, 0.0);
		if (DEBUG_EG) {
			// process this segment's start in case delay==0
			calculate(new AudioTime(startTime));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.Envelope#calculate(synth.engine.AudioTime)
	 */
	public void calculate(AudioTime time) {
		if (segment > RELEASE) {
			return;
		}
		double thisTime = getRelativeTime(time);
		// first check if we need to go to next segment
		while (segment != SUSTAIN && thisTime >= nextSegmentStartTime) {
			// advance to next segment
			advanceSegment(segment + 1, nextSegmentStartTime);
			if (segment > RELEASE) {
				value = 0.0;
				return;
			}
		}
		double segmentDone =
				(thisTime - segmentStartTime)
						/ (nextSegmentStartTime - segmentStartTime);
		switch (segment) {
		case DELAY:
			value = 0.0;
			break;
		case ATTACK:
			// apply positive unipolar convex transform
			value = SoundFontUtils.transform(segmentDone, false, true, true);
			break;
		case HOLD:
			value = 1.0;
			break;
		case DECAY:
			// segmentValue[SUSTAIN] is percent down from maximum value
			value = 1.0 - (segmentValue[SUSTAIN] * segmentDone);
			break;
		case SUSTAIN:
			value = 1.0 - segmentValue[SUSTAIN];
			break;
		case RELEASE:
			value = releaseLevel * (1.0 - segmentDone);
			break;
		}
	}

	/**
	 * Return the effective duration of the specified segment. For most
	 * segments, this is the value of segmentValue[seg], but the DECAY and
	 * RELEASE segment need to be scaled by the sustain level or the current
	 * value, respectively.
	 * 
	 * @param seg the segment of which to get the duration
	 * @return the effective duration of the given segment in seconds
	 */
	private final double getEffectiveSegmentTime(int seg) {
		double segValue = segmentValue[seg];
		if (seg == HOLD) {
			double keyNumToHold =
					timecents2seconds(keyNumToHoldTimeCents * (60 - key));
			segValue *= keyNumToHold;
		} else if (seg == DECAY) {
			double keyNumToDecay =
					timecents2seconds(keyNumToDecayTimeCents * (60 - key));
			// the decay segment time is given in the time for a 100% decay,
			// but if the sustain level is not 100%, the decay needs to be
			// scaled, too.
			segValue *= segmentValue[SUSTAIN] * keyNumToDecay;
		} else if (seg == RELEASE) {
			// the value specified for RELEASE is the time to fully decay 100%.
			// Now if the current value is not 100% when the release phase is
			// entered, the release time is shortened proportionally
			segValue *= value;
			if (segValue < MIN_RELEASE_TIME) {
				segValue = MIN_RELEASE_TIME;
				if (DEBUG_EG) {
					System.out.println(name + ": lengthened RELEASE segment to prevent click.");
				}
			}
		}
		return segValue;
	}

	/**
	 * Calculate the duration from start of this voice until the specified time.
	 * 
	 * @param time the time to convert to relative time
	 * @return the time, in seconds, that have passed since the start time of
	 *         this instrument
	 */
	private final double getRelativeTime(AudioTime time) {
		return (time.getNanoTime() - startTime) / 1000000000.0;
	}

	/**
	 * Advance the current segment to newSegment, and use newStartTime as the
	 * new value for segmentStartTime. This function calculates
	 * nextSegmentStartTime.
	 * 
	 * @param newSegment the new segment
	 * @param newStartTime the start time, in seconds, of the new segment
	 */
	private final void advanceSegment(int newSegment, double newStartTime) {
		segment = newSegment;
		// if the decay segment is finished, and the sustain level is 0.0,
		// then we're done, too. Note that sustain value is "from top to
		// bottom".
		if (segment == SUSTAIN && segmentValue[SUSTAIN] >= 1.0) {
			segment = RELEASE + 1;
			if (DEBUG_EG) {
				System.out.println(name + ": at time " + (newStartTime)
						+ "s: decay to 0, envelope finished.");
			}
			return;
		} else if (segment > RELEASE) {
			if (DEBUG_EG) {
				System.out.println(name + ": at time " + (newStartTime)
						+ "s: finished.");
			}
			return;
		}
		segmentStartTime = newStartTime;
		nextSegmentStartTime =
				segmentStartTime + getEffectiveSegmentTime(segment);
		if (DEBUG_EG) {
			System.out.println(name + " at time " + (newStartTime) + "s: value="
					+ (value) + ", start of segment "
					+ segment2string(segment) + " at "
					+ (segmentStartTime) + "s until "
					+ (nextSegmentStartTime) + "s (length="
					+ (nextSegmentStartTime - segmentStartTime) + "s)");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.Envelope#endReached()
	 */
	public boolean endReached() {
		return segment > RELEASE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.Envelope#release(synth.engine.AudioTime)
	 */
	public void release(AudioTime time) {
		if (segment < RELEASE) {
			calculate(time);
			releaseLevel = value;
			advanceSegment(RELEASE, getRelativeTime(time));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.Envelope#getCurrentValue()
	 */
	public double getCurrentValue() {
		return value;
	}

	// SETUP of the segment parameters

	/**
	 * @param seg the segment (DELAY, ATTACK,...) to set the time
	 * @param relative if true, add to the corresponding time
	 * @param timecents The time in cents to set
	 */
	void setSegmentTimeCents(int seg, boolean relative, int timecents) {
		double seconds = timecents2seconds(timecents);
		if (relative) {
			this.segmentValue[seg] *= seconds;
			if (DEBUG_EG) {
				System.out.println(" " + name + ": add " + timecents + " cents (="
						+ (seconds) + "s), new " + segment2string(seg)
						+ ": " + (segmentValue[seg]) + "s");
			}
		} else {
			this.segmentValue[seg] = seconds;
			if (DEBUG_EG) {
				System.out.println(" " + name + ": set " + segment2string(seg) + " to "
						+ (segmentValue[seg]) + "s");
			}
		}
	}

	private static final String segment2string(int seg) {
		switch (seg) {
		case DELAY:
			return "delay";
		case ATTACK:
			return "attack";
		case HOLD:
			return "hold";
		case DECAY:
			return "decay";
		case SUSTAIN:
			return "sustain";
		case RELEASE:
			return "release";
		}
		return "(none)";
	}

	/**
	 * Set the sustain level as it is specified in the sf2 file: as the value
	 * which is below maximum. So for a sustain at peak level, level is set to
	 * 0. According to the spec, the level value is set to 0 if it is below 0,
	 * and clipped at 1 if greater than 1.
	 * <p>
	 * For the volume envelope, the value is in 1/100 of decibel, but as a
	 * convention, the range goes from [0..1] meaning [0dB..100dB] attenuation.
	 * 
	 * @param level [0..1] corresponds to peak level...minimum level
	 */
	public void setSustainLevel(double level) {
		// if (volume != 0.0) {
		// // for volume envelope, the sustain range is 0..144. Scale down...
		// level /= 1.44;
		// }
		if (level < 0.0) {
			level = 0.0;
		} else if (level > 1.0) {
			level = 1.0;
		}
		this.segmentValue[SUSTAIN] = level;
		if (DEBUG_EG) {
			if (volume != 0.0) {
				System.out.println(" " + name + ": set sustain to "
						+ ((1.0 - level) * 100.0) + "dB");
			} else {
				System.out.println(" " + name + ": set sustain to "
						+ ((1.0 - level) * 100.0) + "%");
			}
		}
	}

	/**
	 * Add this value to the sustain level.
	 * 
	 * @param level the level to add to the (inverted) sustain level, [-1...+1]
	 */
	public void addSustainLevel(double level) {
		if (DEBUG_EG) {
			// display absolute value
			if (volume != 0.0) {
				System.out.println(" " + name + ": add " + (level * 100.0)
						+ "dB to sustain: ");
			} else {
				System.out.println(" " + name + ": add " + (level * 100.0)
						+ "% to sustain: ");
			}
		}
		setSustainLevel(this.segmentValue[SUSTAIN] + level);
	}

	// Filter support

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
		if (DEBUG_EG) {
			System.out.println(" " + name + ": set cutoff to " + (this.cutoff)
					+ " semitones");
		}
	}

	/**
	 * @param cutoff The cutoff to add.
	 */
	void addCutoff(double cutoff) {
		this.cutoff += cutoff;
		if (DEBUG_EG) {
			System.out.println(" " + name + ": add cutoff by " + (cutoff)
					+ ", new value: " + (this.cutoff) + " semitones");
		}
	}

	// Pitch support

	/**
	 * @return Returns the current pitch offset in semitones.
	 */
	public double getCurrentPitch() {
		return pitch * value;
	}

	/**
	 * @param pitch The pitch to set.
	 */
	void setPitch(double pitch) {
		this.pitch = pitch;
		if (DEBUG_EG) {
			System.out.println(" " + name + ": set pitch to " + (this.pitch)
					+ " semitones");
		}
	}

	/**
	 * @param pitch The pitch to add.
	 */
	void addPitch(double pitch) {
		this.pitch += pitch;
		if (DEBUG_EG) {
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

	// volume support

	/**
	 * @return Returns the current volume offset in deciBel. Since the envelope
	 *         only attenuates the signal, the return value is always
	 *         non-positive.
	 */
	public double getCurrentVolume() {
		return volume * (value - 1.0);
	}

	/**
	 * @param volume The volume to set.
	 */
	void setVolume(double volume) {
		this.volume = volume;
		if (DEBUG_EG) {
			System.out.println(" " + name + ": set maximum attenuation to "
					+ (this.volume) + "dB");
		}
	}

	/**
	 * @return Returns the volume.
	 */
	public double getVolume() {
		return volume;
	}

	// MODULATOR support

	/**
	 * @param keyNumToDecayTimeCents The keyNumToDecay in time cents to set.
	 */
	public void setKeyNumToDecay(int keyNumToDecayTimeCents) {
		this.keyNumToDecayTimeCents = keyNumToDecayTimeCents;
	}

	/**
	 * @param keyNumToHoldTimeCents The keyNumToHold in time cents to set.
	 */
	public void setKeyNumToHold(int keyNumToHoldTimeCents) {
		this.keyNumToHoldTimeCents = keyNumToHoldTimeCents;
	}

	/**
	 * @param keyNumToDecayTimeCents The keyNumToDecay in time cents to add.
	 */
	public void addKeyNumToDecay(int keyNumToDecayTimeCents) {
		this.keyNumToDecayTimeCents += keyNumToDecayTimeCents;
	}

	/**
	 * @param keyNumToHoldTimeCents The keyNumToHold in time cents to add.
	 */
	public void addKeyNumToHold(int keyNumToHoldTimeCents) {
		this.keyNumToHoldTimeCents += keyNumToHoldTimeCents;
	}

	// DEBUGGING

	public String toString() {
		return name + ": pitch=" + (this.pitch) + " semitones, vol="
				+ (this.volume) + "dB, cutoff=" + (this.cutoff)
				+ " semitones," + " key2hold="
				+ (timecents2seconds(keyNumToHoldTimeCents * 12))
				+ " key2decay="
				+ (timecents2seconds(keyNumToDecayTimeCents * 12))
				+ " dl=" + (segmentValue[DELAY]) + " a="
				+ (segmentValue[ATTACK]) + " h="
				+ (segmentValue[HOLD]) + " (eff.h="
				+ (getEffectiveSegmentTime(HOLD)) + ") dc="
				+ (segmentValue[DECAY]) + " (eff.dc="
				+ (getEffectiveSegmentTime(DECAY)) + ") s="
				+ ((1.0 - segmentValue[SUSTAIN]) * 100.0) + "% r="
				+ (segmentValue[RELEASE]);
	}
}
