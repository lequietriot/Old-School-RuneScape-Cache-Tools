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

import synth.engine.*;
import synth.utils.AudioUtils;

/**
 * The master class for all articulation (runtime parameters) of a playing
 * SoundFont voice.
 * 
 * @author florian
 */

public class SoundFontArticulation extends Articulation {

	public static boolean DEBUG_ART_VOLUME = false;
	public static boolean DEBUG_ART = false;

	/**
	 * Pitch/Filter changes are only computed in this interval, in nanoseconds
	 */
	private final static int PITCH_CHANGE_INTERVAL = 10000000;

	/**
	 * name for debugging
	 */
	private String name = "";

	/**
	 * The initial linear panorama of this note. Need to be stored in order to
	 * track zone precedence.
	 * <ul>
	 * <li>-1.0: 100% sent to the left channel
	 * <li>0.0: send 100% to left and right
	 * <li>1.0: send 100% to right only
	 */
	private double linearPan = 0.0;

	/**
	 * The initial linear volume factor. Need to be stored in order to track
	 * zone precedence.
	 */
	private double linearVolume = 1.0;

	/**
	 * The accumulated initial attenuation in decibel. Need to be stored in
	 * order to track zone precedence. [0..144]
	 */
	private double initialAttenuation = 0.0;

	/**
	 * Initial volume factor -- incorporates
	 * <ul>
	 * <li>note on velocity
	 * <li>initial pan
	 * <li>other volume adjustments in the instrument's zones
	 * </ul>
	 */
	private double[] initialVolumeFactor = new double[2];

	/**
	 * runtime attenuation -- incorporates
	 * <ul>
	 * <li>MIDI channel volume -- controller 7
	 * <li>MIDI channel pan -- controller 10
	 * <li>MIDI channel expression -- controller 11
	 * </ul>
	 */
	private double[] runtimeVolumeFactor = new double[2];
	
	/**
	 * Use a distinct variable for fine tune to support the set/add concept
	 * of SF2's generators. Value is in semitones.
	 */
	private double fineTune;

	/**
	 * The first LFO: for vibrato (pitch) only.
	 */
	private SoundFontLFO lfo1;

	/**
	 * The second LFO: applies to pitch, filter cutoff, and/or amplitude.
	 */
	private SoundFontLFO lfo2;

	/**
	 * The first envelope generator, for volume.
	 */
	private SoundFontEnvelope eg1;

	/**
	 * The second envelope generator, for modulation and filter cutoff
	 */
	private SoundFontEnvelope eg2;

	/**
	 * A cache for the volume factor of combined EG and LFO volume
	 */
	private double LFO_EG_VolumeFactor = 1.0;

	/**
	 * The amount, in semitones, by how much a full excursion of the modulation
	 * wheel will offset the pitch of the vibrato LFO.
	 */
	private double modulationToPitchLFO = 0.5; // 50 cents default

	/**
	 * The amount, in semitones, by how much a full channel pressure will offset
	 * the pitch of the vibrato LFO.
	 */
	private double channelPressureToPitchLFO = 0.5; // 50 cents default

	/**
	 * The low pass filter
	 */
	private SoundFontFilter lowPass;

	/**
	 * For optimization, only compute a new pitch and filter values every
	 * PITCH_CHANGE_INTERVAL nanoseconds. This is the next audio time at which a
	 * pitch change needs to be computed.
	 */
	private long nextPitchChange = 0;

	/**
	 * The value of chorus effects send [0..1]
	 */
	private double chorusSend = 0.0;

	/**
	 * The value of reverb effects send [0..1]
	 */
	private double reverbSend = 0.0;

	/**
	 * The scale tuning value, normally 1 semitone
	 */
	private double scaleTuning = 1.0;

	/**
	 * Create an articulation object for SoundFont2 voices
	 * 
	 * @param patch
	 * @param channel
	 */
	public SoundFontArticulation(AudioTime time, Patch patch,
			MidiChannel channel) {
		init(time, patch, channel);
		lfo1 = new SoundFontLFO(time);
		lfo2 = new SoundFontLFO(time);
		eg1 = new SoundFontEnvelope(time);
		eg2 = new SoundFontEnvelope(time);
		// the lowpass filter could also belong to NoteInput, but all
		// the modifiers for the lowpass are calculated in the Articulation
		// objects, so it is owned by Articulation.
		lowPass = new SoundFontFilter(this);
		lfo1.name = "LFO 1"; // vibrato LFO: only pitch
		lfo2.name = "LFO 2"; // modulation LFO: pitch, volume, cutoff
		eg1.name = "EG 1"; // volume envelope
		eg2.name = "EG 2"; // modulation envelope: pitch and/or cutoff
		// set eg1 as the volume envelope.
		eg1.setVolume(96); // 96dB is the maximum attenuation
	}

	// TODO: move the concept of initial and runtime factors to the base class.
	public void setup(int note, int vel) {
		super.setup(note, vel);
		updateRuntimeVibratoLFO();
		// first initialize LFO's, before calculating volume factors: the volume
		// factors need current LFO values.
		eg1.setup(note, vel);
		eg2.setup(note, vel);
		lfo1.setup();
		lfo2.setup();
		lowPass.setup(note, vel);
		calcInitialVolumeFactor();
		calcRuntimeVolumeFactor();
		calcLFO_EG_VolumeFactor();

		if (DEBUG_ART) {
			System.out.println(name + "initial pitch offset=" + (getInitialPitchOffset()+fineTune)
					+ " semitones.");
			System.out.println(lfo1.toString());
			System.out.println(lfo2.toString());
			System.out.println(eg1.toString());
			System.out.println(eg2.toString());
		}
	}

	/**
	 * Override from super class: calculate envelopes and LFO's.
	 */
	public void calculate(AudioTime time) {
		long nanoTime = time.getNanoTime();
		if (nanoTime >= nextPitchChange) {
			// calculate pitch/filter only
			lfo1.calculate(time);
			eg2.calculate(time);
			lowPass.calculate(lfo2.getCurrentCutoff() + eg2.getCurrentCutoff());
			nextPitchChange = nanoTime + PITCH_CHANGE_INTERVAL;
		}
		eg1.calculate(time);
		lfo2.calculate(time);
		calcLFO_EG_VolumeFactor();
	}

	public void process(AudioBuffer buffer) {
		lowPass.process(buffer);
	}

	protected double getRuntimePitchOffset() {
		return lfo1.getCurrentPitch() + lfo2.getCurrentPitch()
				+ eg2.getCurrentPitch();
	}

	/**
	 * This method is overridden in order to account for scale tuning.
	 */
	public double getEffectivePitchOffset(double relativeNote) {
		// TODO: make pitch wheel response parametrizable by way of the 
		// modulation blocks of the soundfont according to section 8.4.10
		double res = super.getEffectivePitchOffset(relativeNote) + fineTune;
		if (scaleTuning != 1.0) {
			// scale tuning applies to the played note
			res -= relativeNote - (relativeNote * scaleTuning);
		}
		return res;
	}

	private double getPanValue(double pan) {
		if (pan < 0.0) return 1.0;
		if (pan >= 1.0) return 0.0;
		return 1.0 - pan;
	}

	private void calcInitialVolumeFactor() {
		if (initialAttenuation != 0.0) {
			linearVolume *=
					SoundFontUtils.attenuation2linear(initialAttenuation);
			if (DEBUG_ART_VOLUME) {
				System.out.println(" "
						+ name
						+ "push initial attenuation to linear factor: was="
						+ (initialAttenuation)
						+ "dB -> factor="
						+ (SoundFontUtils.attenuation2linear(initialAttenuation) * 100)
						+ "%");
			}
			initialAttenuation = 0.0;
		}
		initialVolumeFactor[0] = getPanValue(linearPan) * linearVolume;
		initialVolumeFactor[1] = getPanValue(-linearPan) * linearVolume;
		calcEffectiveVolumeFactor();
	}

	/**
	 * Response to controllers 7, 10 and 11.
	 */
	protected void calcRuntimeVolumeFactor() {
		// TODO: allow override of default controller response
		MidiChannel channel = getChannel();
		// controller 7: channel volume
		// this is the default ctrl 7 response, from section 8.4.5
		double vol = channel.getNormalizedController(MidiChannel.VOLUME);
		// controller 10: channel pan. Use 14-bit integer to get "true center"
		// this is the default ctrl 10 response, from section 8.4.6
		int pan = channel.getController14bit(MidiChannel.PAN);
		// controller 11: channel expression
		// this is the default ctrl 11 response, from section 8.4.7
		double expr = channel.getNormalizedController(MidiChannel.EXPRESSION);

		double decibel =
				SoundFontUtils.transform(vol * expr, true, true, false) * 96.0;
		double linear = AudioUtils.decibel2linear(-decibel);

		// use new pan "style" with 64 as the middle (=64*128 with 14-bit
		// controllers)
		// should PAN really be linear?
		double panRight;

		if (pan <= 128) {
			panRight = 0.0;
		} else if (pan >= 16256) { // 127*128
			panRight = 1.0;
		} else {
			panRight = ((pan - 128) / 16128.0); // 126*128 = 16128
		}
		runtimeVolumeFactor[0] = linear * (1.0 - panRight);
		runtimeVolumeFactor[1] = linear * panRight;
		calcEffectiveVolumeFactor();
	}

	protected void calcLFO_EG_VolumeFactor() {
		double lfoEg = lfo2.getCurrentVolume() + eg1.getCurrentVolume();
		double newLFO_EG_VolumeFactor;
		if (lfoEg != 0.0) {
			newLFO_EG_VolumeFactor = AudioUtils.decibel2linear(lfoEg);
		} else {
			newLFO_EG_VolumeFactor = 1.0;
		}
		if (newLFO_EG_VolumeFactor != LFO_EG_VolumeFactor) {
			if (DEBUG_ART_VOLUME) {
				double dB = eg1.getCurrentVolume();
				System.out.println(eg1.name + ": value="
						+ (eg1.getCurrentValue() * 100) + "%, vol="
						+ (dB) + "dB -> "
						+ (AudioUtils.decibel2linear(dB) * 100.0) + "%");
			}
			LFO_EG_VolumeFactor = newLFO_EG_VolumeFactor;
			calcEffectiveVolumeFactor();
		}
	}

	protected void calcEffectiveVolumeFactor() {
		effectiveLinearVolume[0] =
				initialVolumeFactor[0] * runtimeVolumeFactor[0]
						* LFO_EG_VolumeFactor;
		effectiveLinearVolume[1] =
				initialVolumeFactor[1] * runtimeVolumeFactor[1]
						* LFO_EG_VolumeFactor;
		if (DEBUG_ART_VOLUME) {
			System.out.println(name + "new volume factors: " + " effective factor: " + "L="
					+ (effectiveLinearVolume[0] * 100) + "% " + "R="
					+ (effectiveLinearVolume[1] * 100) + "%. "
					+ "initial: " + "L="
					+ (initialVolumeFactor[0] * 100) + "% " + "R="
					+ (initialVolumeFactor[1] * 100) + "% "
					+ "runtime: " + "L="
					+ (runtimeVolumeFactor[0] * 100) + "% " + "R="
					+ (runtimeVolumeFactor[1] * 100) + "% " + "LFO/EG: "
					+ (LFO_EG_VolumeFactor * 100) + "% ");
		}
	}

	/**
	 * Set the fineTune.
	 * @param fineTune the fine tune in semitones
	 */
	public void setFineTune(double fineTune) {
		this.fineTune = fineTune;
		if (DEBUG_ART) {
			System.out.println(" " + name + "set fine tune to " + (fineTune)
					+ " semitones");
		}
	}

	/**
	 * Add to the fine tune.
	 * @param value the fine tune to add in semitones
	 */
	public void addFineTune(double value) {
		if (DEBUG_ART) {
			System.out.println(" " + name + "add " + (value)
					+ "semitones to fine tune:");
		}
		setFineTune(fineTune+value);
	}

	/**
	 * Set the fine tune.
	 * @param coarseTune the coarse tune in semitones
	 */
	public void setCoarseTune(double coarseTune) {
		setInitialPitchOffset(coarseTune);
		if (DEBUG_ART) {
			System.out.println(" " + name + "set coarse tune to " + (getInitialPitchOffset())
					+ " semitones");
		}
	}

	/**
	 * Add to the coarse tune.
	 * @param value the coarse tune to add in semitones
	 */
	public void addCoarseTune(double value) {
		if (DEBUG_ART) {
			System.out.println(" " + name + "add " + (value)
					+ "semitones to coarse tune:");
		}
		setCoarseTune(getInitialPitchOffset()+value);
	}

	/**
	 * @return Returns the linearPan.
	 */
	public double getLinearPan() {
		return linearPan;
	}

	/**
	 * Set initial pan.
	 * 
	 * @param linearPan The initial linearPan to set.
	 */
	public void setLinearPan(double linearPan) {
		this.linearPan = linearPan;
		if (DEBUG_ART) {
			System.out.println(" " + name + "set pan to " + (linearPan * 100)
					+ "% (scale -100..0..+100)");
		}
	}

	/**
	 * Add to initial pan.
	 * 
	 * @param value The linear pan to add
	 */
	public void addLinearPan(double value) {
		if (DEBUG_ART) {
			System.out.println(" " + name + "add " + (value * 100)
					+ "% to pan. ");
		}
		setLinearPan(linearPan + value);
	}

	/**
	 * Set initial attenuation. This method must only be used for the zone
	 * generators in the soundfont file, because a different scheme is used to
	 * convert these decibel values to linear factor (see
	 * calcInitialVolumeFactor).
	 * 
	 * @param decibel the attenuation [0..144]
	 */
	public void setInitialAttenuation(double decibel) {
		initialAttenuation = decibel;
		if (DEBUG_ART) {
			System.out.println(" " + name + "set initial attenuation to " + (decibel)
					+ "dB");
		}
	}

	/**
	 * Add to initial attenuation. This method must only be used for the zone
	 * generators in the soundfont file, because a different scheme is used to
	 * convert these decibel values to linear factor (see
	 * calcInitialVolumeFactor).
	 * 
	 * @param decibel additive attenuation 0..144
	 */
	public void addInitialAttenuation(double decibel) {
		if (DEBUG_ART) {
			System.out.println(" " + name + "add " + (decibel) + "dB:");
		}
		setInitialAttenuation(initialAttenuation + decibel);
	}

	/**
	 * Add this factor to the linear initial attenuation. Other than
	 * setInitialAttenuation and addInitialAttenuation, this value cannot be
	 * changed later on.
	 */
	public void addLinearInitialAttenuation(double factor) {
		if (DEBUG_ART) {
			System.out.println(" " + name + "add " + (factor * 100)
					+ "% to initial volume factor.");
		}
		linearVolume *= factor;
	}

	/**
	 * @return Returns the first LFO, the vibrato LFO.
	 */
	public SoundFontLFO getVibratoLFO() {
		return lfo1;
	}

	/**
	 * @return Returns the second LFO, the modulation LFO.
	 */
	public SoundFontLFO getModulationLFO() {
		return lfo2;
	}

	/**
	 * @return Returns the first envelope generator, the volume envelope.
	 */
	public SoundFontEnvelope getVolumeEnvelope() {
		return eg1;
	}

	/**
	 * @return Returns the second envelope, the modulation envelope.
	 */
	public SoundFontEnvelope getModulationEnvelope() {
		return eg2;
	}

	/**
	 * @return the low pass filter instance
	 */
	public SoundFontFilter getLowPassFilter() {
		return lowPass;
	}

	private void updateRuntimeVibratoLFO() {
		// TODO: allow override by way of the modulation blocks of the soundfont
		// this is the default channel pressure response, from section 8.4.3
		double cp = getChannel().getNormalizedChannelPressure();
		// this is the default modulation wheel response, from section 8.4.4
		double mod =
				getChannel().getNormalizedController(MidiChannel.MODULATION);
		lfo1.setRuntimePitchOffset((mod * modulationToPitchLFO)
				+ (cp * channelPressureToPitchLFO));
	}

	/**
	 * Is called in response to a change of a MIDI controller.
	 * 
	 * @param controller the controller number that changed
	 * @param value the new value of the controller [0..127]
	 */
	public void controlChange(int controller, int value) {
		switch (controller) {
		case MidiChannel.VOLUME: // fall through
		case MidiChannel.PAN: // fall through
		case MidiChannel.EXPRESSION:
			calcRuntimeVolumeFactor();
			break;
		case MidiChannel.MODULATION:
			updateRuntimeVibratoLFO();
			break;
		}
		lowPass.controlChange(controller, value);
	}

	/**
	 * Is called in response to a change of a MIDI pitch bend change.
	 */
	public void pitchWheelChange() {
		// nothing?
	}

	public void release(AudioTime time) {
		eg1.release(time);
		eg2.release(time);
	}

	public boolean endReached() {
		// if the volume envelope is done, we're done, too
		return eg1.endReached();
	}

	/**
	 * Set the initial linear chorus send.
	 * 
	 * @param value chorus send, 0..1
	 */
	public void setChorusSend(double value) {
		if (value < 0.0) {
			value = 0.0;
		} else if (value > 1.0) {
			value = 1.0;
		}
		chorusSend = value;
		if (DEBUG_ART) {
			System.out.println(" " + name + "set Chorus send to " + (value * 100)
					+ "%");
		}
	}

	/**
	 * Add this value to the initial linear chorus send.
	 * 
	 * @param value chorus send, 0..1
	 */
	public void addChorusSend(double value) {
		if (DEBUG_ART) {
			System.out.println(" " + name + "add " + (value * 100.0)
					+ "% to Chorus send:");
		}
		setChorusSend(value + chorusSend);
	}

	/**
	 * Set the initial linear reverb send.
	 * 
	 * @param value reverb send, 0..1
	 */
	public void setReverbSend(double value) {
		if (value < 0.0) {
			value = 0.0;
		} else if (value > 1.0) {
			value = 1.0;
		}
		reverbSend = value;
		if (DEBUG_ART) {
			System.out.println(" " + name + "set Reverb send to " + (value * 100)
					+ "%");
		}
	}

	/**
	 * Add this value to the initial linear reverb send.
	 * 
	 * @param value reverb send, 0..1
	 */
	public void addReverbSend(double value) {
		if (DEBUG_ART) {
			System.out.println(" " + name + "add " + (value * 100.0)
					+ "% to Reverb send:");
		}
		setReverbSend(value + reverbSend);
	}

	/**
	 * Set the scale tuning from the sf2 file zones.
	 * 
	 * @param value the scale tuning in semi tunes, 0...12
	 */
	public void setScaleTuning(double value) {
		scaleTuning = value;
		if (DEBUG_ART) {
			System.out.println(" " + name + "set scale tuning to " + (value)
					+ " semitones.");
		}
	}

	/**
	 * Add to the scale tuning from the sf2 file zones.
	 * 
	 * @param value the scale tuning in semi tunes, 0...12
	 */
	public void addScaleTuning(double value) {
		if (DEBUG_ART) {
			System.out.println(" " + name + "add " + (value)
					+ " semitones to scale tuning:");
		}
		setScaleTuning(scaleTuning + value);
	}

	/**
	 * Function for debugging
	 * 
	 * @param s the name to give this instance
	 */
	void setName(String s) {
		name = s + ":";
		eg1.name = s + "." + eg1.name;
		eg2.name = s + "." + eg2.name;
		lfo1.name = s + "." + lfo1.name;
		lfo2.name = s + "." + lfo2.name;
	}

}
