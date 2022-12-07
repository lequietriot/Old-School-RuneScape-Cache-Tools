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

import synth.engine.MidiChannel;
import synth.engine.Oscillator;

import static synth.soundfont2.SoundFontGenerator.*;

/**
 * Base class for instrument and preset zones.
 * 
 * @author florian
 * 
 */
public abstract class SoundFontZone {
	private SoundFontGenerator[] generators;
	private SoundFontModulator[] modulators;

	/** low boundary of key range */
	protected int keyMin = 0;
	/** high boundary of key range */
	protected int keyMax = 127;
	/** low boundary of velocity range */
	protected int velMin = 0;
	/** high boundary of velocity range */
	protected int velMax = 127;

	protected SoundFontZone(SoundFontGenerator[] generators,
			SoundFontModulator[] modulators) {
		this.generators = generators;
		this.modulators = modulators;
		parseStaticFields();
	}

	/**
	 * @return Returns the generators.
	 */
	public SoundFontGenerator[] getGenerators() {
		return generators;
	}

	/**
	 * @return Returns the modulators.
	 */
	public SoundFontModulator[] getModulators() {
		return modulators;
	}

	/**
	 * From the Generators and Modulators, derive static fields such as keyRange
	 * and velocityRange to speed up the choice of zone at real time.
	 */
	final void parseStaticFields() {
		for (SoundFontGenerator gen : generators) {
			parseStaticFieldFromGenerator(gen);
		}
		for (SoundFontModulator mod : modulators) {
			parseStaticFieldFromModulator(mod);
		}
	}

	protected void parseStaticFieldFromGenerator(SoundFontGenerator gen) {
		switch (gen.getOp()) {
		case KEY_RANGE:
			keyMin = gen.getAmount() & 0xFF;
			keyMax = (gen.getAmount() >> 8) & 0xFF;
			break;
		case VEL_RANGE:
			velMin = gen.getAmount() & 0xFF;
			velMax = (gen.getAmount() >> 8) & 0xFF;
			break;
		}
	}

	protected void parseStaticFieldFromModulator(SoundFontModulator mod) {
		// nothing to do
	}

	public final boolean matches(int note, int vel) {
		return (note >= keyMin) && (note <= keyMax) && (vel >= velMin)
				&& (vel <= velMax);
	}

	public final boolean matchesKeyRegion(SoundFontZone zone) {
		return (zone.keyMin == this.keyMin) && (zone.keyMin == this.keyMin)
				&& (zone.velMin == this.velMin) && (zone.velMax == this.velMax);
	}

	/**
	 * Invalidate this zone so that it will not be used at runtime anymore. This
	 * is currently achieved by setting the keyMin/keyMax values to impossible
	 * values.
	 * <p>
	 * This is used for incomplete zone definitions (e.g. missing sample) or
	 * inconsistent sample links.
	 */
	void makeInaccessible() {
		keyMin = -1;
		keyMax = -1;
	}

	/**
	 * Returns if this zone is a global zone. This can only be true for the
	 * first zone in the list of preset or instrument zones.
	 * 
	 * @return true if this is a global zone (pending that it is the first zone
	 *         of a preset/inst).
	 */
	public abstract boolean isGlobalZone();

	/**
	 * Execute a relative generator and apply it to <code>art</code>. This is
	 * a convenient method for use with modulators.
	 * 
	 * @param generator the generator index to modify
	 * @param amount the amount of the generator to be added
	 * @param art the articulation object to apply the generator to
	 */
	public static final void executeRelativeGenerator(int generator, int amount, 
			SoundFontArticulation art) { 
		executeGenerator(generator, amount, true, art, null, null); 
	}

	/**
	 * Executes this generator and sets or adds the corresponding value
	 * in <code>art</code>,<code>osc</code>, or <code>patch</code>.  
	 * @param generator the numerical generator to set/add
	 * @param amount the value to set/add
	 * @param isRelative if true, the value is added to the generator. Otherwise 
	 *   the generator's value is overriden
	 * @param art the articulation object to set the generator values
	 * @param osc the oscillator to set the values. Only needed if isRelative=false
	 * @param patch the patch to set the values. Only needed if isRelative=false 
	 */
	public static final void executeGenerator(int generator, int amount, 
			boolean isRelative, SoundFontArticulation art, 
			SoundFontOscillator osc, SoundFontPatch patch) {
		switch (generator) {
		case START_ADDRS_OFFSET:
			if (!isRelative) {
				// only valid on instrument level, is always relative
				// (offset)
				osc.addNativeSamplesStartPos(amount);
			}
			break;

		case END_ADDRS_OFFSET:
			if (!isRelative) {
				// only valid on instrument level, is always relative
				// (offset)
				osc.addNativeSamplesEndPos(amount);
			}
			break;

		case START_LOOP_ADDRS_OFFSET:
			if (!isRelative) {
				// only valid on instrument level, is always relative
				// (offset)
				osc.addLoopStart(amount);
			}
			break;

		case END_LOOP_ADDRS_OFFSET:
			if (!isRelative) {
				// only valid on instrument level, is always relative
				// (offset)
				osc.addLoopEnd(amount);
			}
			break;

		case START_ADDRS_COARSE_OFFSET:
			if (!isRelative) {
				// only valid on instrument level, is always relative
				// (offset)
				osc.addNativeSamplesStartPos(amount << 15);
			}
			break;

		case MODLFOTO_PITCH:
			if (isRelative) {
				art.getModulationLFO().addPitch(amount / 100.0);
			} else {
				art.getModulationLFO().setPitch(amount / 100.0);
			}
			break;

		case VIBLFOTO_PITCH:
			if (isRelative) {
				art.getVibratoLFO().addPitch(amount / 100.0);
			} else {
				art.getVibratoLFO().setPitch(amount / 100.0);
			}
			break;

		case MODENVTO_PITCH:
			if (isRelative) {
				art.getModulationEnvelope().addPitch(amount / 100.0);
			} else {
				art.getModulationEnvelope().setPitch(amount / 100.0);
			}
			break;

		case INITIALFILTER_FC:
			if (isRelative) {
				art.getLowPassFilter().addCutoffCents(amount);
			} else {
				art.getLowPassFilter().setCutoffCents(amount);
			}
			break;

		case INITIALFILTER_Q:
			if (isRelative) {
				art.getLowPassFilter().addResonanceCB(amount);
			} else {
				art.getLowPassFilter().setResonanceCB(amount);
			}
			break;

		case MODLFOTOFILTER_FC:
			if (isRelative) {
				art.getModulationLFO().addCutoff(amount / 100.0);
			} else {
				art.getModulationLFO().setCutoff(amount / 100.0);
			}
			break;

		case MODENVTOFILTER_FC:
			if (isRelative) {
				art.getModulationEnvelope().addCutoff(amount / 100.0);
			} else {
				art.getModulationEnvelope().setCutoff(amount / 100.0);
			}
			break;

		case ENDADDRSCOARSE_OFFSET:
			if (!isRelative) {
				// only valid on instrument level, is always relative
				// (offset)
				osc.addNativeSamplesEndPos(amount << 15);
			}
			break;

		case MOD_LFO_TO_VOLUME:
			if (isRelative) {
				art.getModulationLFO().addVolume(amount / 10.0);
			} else {
				art.getModulationLFO().setVolume(amount / 10.0);
			}
			break;

		case CHORUS_EFFECTS_SEND:
			if (isRelative) {
				art.addChorusSend(amount / 1000.0);
			} else {
				art.setChorusSend(amount / 1000.0);
			}
			break;

		case REVERB_EFFECTS_SEND:
			if (isRelative) {
				art.addReverbSend(amount / 1000.0);
			} else {
				art.setReverbSend(amount / 1000.0);
			}
			break;

		case PAN:
			if (isRelative) {
				art.addLinearPan(((double) amount) / 500.0);
			} else {
				art.setLinearPan(((double) amount) / 500.0);
			}
			break;

		case DELAY_MOD_LFO:
			if (isRelative) {
				art.getModulationLFO().addDelayTimeCents(amount);
			} else {
				art.getModulationLFO().setDelayTimeCents(amount);
			}
			break;

		case FREQ_MOD_LFO:
			if (isRelative) {
				art.getModulationLFO().addFrequencyCents(amount);
			} else {
				art.getModulationLFO().setFrequencyCents(amount);
			}
			break;

		case DELAY_VIB_LFO:
			if (isRelative) {
				art.getVibratoLFO().addDelayTimeCents(amount);
			} else {
				art.getVibratoLFO().setDelayTimeCents(amount);
			}
			break;

		case FREQ_VIB_LFO:
			if (isRelative) {
				art.getVibratoLFO().addFrequencyCents(amount);
			} else {
				art.getVibratoLFO().setFrequencyCents(amount);
			}
			break;

		case DELAY_MOD_ENV:
			art.getModulationEnvelope().setSegmentTimeCents(
					SoundFontEnvelope.DELAY, isRelative, amount);
			break;

		case ATTACK_MOD_ENV:
			art.getModulationEnvelope().setSegmentTimeCents(
					SoundFontEnvelope.ATTACK, isRelative, amount);
			break;

		case HOLD_MOD_ENV:
			art.getModulationEnvelope().setSegmentTimeCents(
					SoundFontEnvelope.HOLD, isRelative, amount);
			break;

		case DECAY_MOD_ENV:
			art.getModulationEnvelope().setSegmentTimeCents(
					SoundFontEnvelope.DECAY, isRelative, amount);
			break;

		case SUSTAIN_MOD_ENV:
			if (isRelative) {
				art.getModulationEnvelope().addSustainLevel(amount / 1000.0);
			} else {
				art.getModulationEnvelope().setSustainLevel(amount / 1000.0);
			}
			break;

		case RELEASE_MOD_ENV:
			art.getModulationEnvelope().setSegmentTimeCents(
					SoundFontEnvelope.RELEASE, isRelative, amount);
			break;

		case KEYNUM_TO_MOD_ENV_HOLD:
			if (isRelative) {
				art.getModulationEnvelope().addKeyNumToHold(amount);
			} else {
				art.getModulationEnvelope().setKeyNumToHold(amount);
			}
			break;

		case KEYNUM_TO_MOD_ENV_DECAY:
			if (isRelative) {
				art.getModulationEnvelope().addKeyNumToDecay(amount);
			} else {
				art.getModulationEnvelope().setKeyNumToDecay(amount);
			}
			break;

		case DELAY_VOL_ENV:
			art.getVolumeEnvelope().setSegmentTimeCents(
					SoundFontEnvelope.DELAY, isRelative, amount);
			break;

		case ATTACK_VOL_ENV:
			art.getVolumeEnvelope().setSegmentTimeCents(
					SoundFontEnvelope.ATTACK, isRelative, amount);
			break;

		case HOLD_VOL_ENV:
			art.getVolumeEnvelope().setSegmentTimeCents(
					SoundFontEnvelope.HOLD, isRelative, amount);
			break;

		case DECAY_VOL_ENV:
			art.getVolumeEnvelope().setSegmentTimeCents(
					SoundFontEnvelope.DECAY, isRelative, amount);
			break;

		case SUSTAIN_VOL_ENV:
			if (isRelative) {
				art.getVolumeEnvelope().addSustainLevel(amount / 1000.0);
			} else {
				art.getVolumeEnvelope().setSustainLevel(amount / 1000.0);
			}
			break;

		case RELEASE_VOL_ENV:
			art.getVolumeEnvelope().setSegmentTimeCents(
					SoundFontEnvelope.RELEASE, isRelative, amount);
			break;

		case KEYNUM_TO_VOL_ENV_HOLD:
			if (isRelative) {
				art.getVolumeEnvelope().addKeyNumToHold(amount);
			} else {
				art.getVolumeEnvelope().setKeyNumToHold(amount);
			}
			break;

		case KEYNUM_TO_VOL_ENV_DECAY:
			if (isRelative) {
				art.getVolumeEnvelope().addKeyNumToDecay(amount);
			} else {
				art.getVolumeEnvelope().setKeyNumToDecay(amount);
			}
			break;

		case START_LOOP_ADDRS_COARSE_OFFSET:
			if (!isRelative) {
				// only valid on instrument level, is always relative
				// (offset)
				osc.addLoopStart(amount << 15);
			}
			break;

		case KEYNUM:
			if (!isRelative) {
				// only valid on instrument level
				patch.setNote(amount);
			}
			break;

		case VELOCITY:
			if (!isRelative) {
				// only valid on instrument level
				patch.setVelocity(amount);
			}
			break;

		case INITIAL_ATTENUATION:
			if (isRelative) {
				art.addInitialAttenuation(((double) amount) / 10.0);
			} else {
				art.setInitialAttenuation(((double) amount) / 10.0);
			}
			break;

		case END_LOOP_ADDRS_COARSE_OFFSET:
			if (!isRelative) {
				// only valid on instrument level, is always relative
				// (offset)
				osc.addLoopEnd(amount << 15);
			}
			break;

		case COARSE_TUNE:
			if (isRelative) {
				art.addCoarseTune(amount);
			} else {
				art.setCoarseTune(amount);
			}
			break;

		case FINE_TUNE:
			if (isRelative) {
				art.addFineTune(amount / 100.0);
			} else {
				art.setFineTune(amount / 100.0);
			}
			break;

		case SAMPLE_MODES:
			if (!isRelative) {
				// only valid on instrument level, not valid in global zone
				switch (amount & 0x03) {
				case 0:
					osc.setLoopMode(Oscillator.LOOPMODE_NONE);
					break;
				case 1:
					osc.setLoopMode(Oscillator.LOOPMODE_CONTINOUSLY);
					break;
				case 2:
					osc.setLoopMode(Oscillator.LOOPMODE_NONE);
					break;
				case 3:
					osc.setLoopMode(Oscillator.LOOPMODE_UNTIL_RELEASE);
					break;
				}
			}
			break;

		case SCALE_TUNING:
			if (isRelative) {
				// Creative Labs' synths do not take into account
				// a scale tuning at preset level, so we ignore it, too.
				// art.addScaleTuning(((double) amount) / 100.0);
			} else {
				art.setScaleTuning(((double) amount) / 100.0);
			}
			break;

		case EXCLUSIVE_CLASS:
			if (!isRelative) {
				// only valid on instrument level
				patch.setExclusiveLevel(amount);
			}
			break;

		case OVERRIDING_ROOTKEY:
			if (!isRelative) {
				if (amount >= 0 && amount <= 127) {
					patch.setRootKey(amount);
				}
			}
			break;
		}
	}
		
	/**
	 * Parse the generators in this zone and set (isRelative=false) or add
	 * (isRelative=true) the values to the respective data in patch, art, and
	 * osc.
	 * <p>
	 * Precondition: all parameters are non-null.
	 * 
	 * @param isRelative if true, the values are treated as summands, otherwise
	 *            as absolute values to set.
	 * @param patch the patch to modify
	 * @param art the articulation module to modify
	 * @param osc the oscillators whose parameters are to be modified
	 * @param forbiddenZone a (local) zone which contains generators and
	 *            modulators which take precedence if the same
	 *            generator/modulator is defined in this zone. This is used for
	 *            the global zones. See Parser.java for an idea how to overcome
	 *            this performance stopper. Set to null for local zones.
	 */
	public final void parseZone(boolean isRelative, SoundFontPatch patch,
			SoundFontArticulation art, SoundFontOscillator osc,
			SoundFontZone forbiddenZone) {

		SoundFontGenerator[] forbiddenGenerators = null;
		if (forbiddenZone != null) {
			forbiddenGenerators = forbiddenZone.getGenerators();
		}

		for (SoundFontGenerator gen : generators) {
			short amount = gen.getAmount();

			// do not use any of the forbidden generators, if any.
			if (forbiddenGenerators != null) {
				boolean isForbidden = false;
				for (SoundFontGenerator forbidden : forbiddenGenerators) {
					if (forbidden.getOp() == gen.getOp()) {
						isForbidden = true;
						break;
					}
				}
				if (isForbidden) {
					// do not consider this generator because there exists one
					// in the forbidden zone.
					continue;
				}
			}
			
			executeGenerator(gen.getOp(), amount, isRelative, art, osc, patch);
		}
	}
	
	/**
	 * Go through the list of modulators and execute them.
	 * @param note the effective note (after executing the generators)
	 * @param vel the effective velocity
	 * @param channel the MIDI channel
	 * @param art the articulation object for this note
	 * @param forbiddenZone all the modulators in the forbiddenZone must be 
	 *  ignored for this zone. This is used to implement the override scheme
	 *  of local/global zones
	 */
	public final void parseModulators(int note, int vel, MidiChannel channel, SoundFontArticulation art, 
			SoundFontZone forbiddenZone) {
		// scheme:
		// iterate through all modulators
		// for each modulator, if it's not in the forbidden list,
		// execute it 
		
		// problem: how to handle the modulators in real time?
		// the only solution I can find right now is to track the 
		// previous controller value and accordingly apply the modulators
		// only on the difference value of the controller.
		
		// problem: how to detect/override the default modulators?
		
		// problem: how to prevent iterating through all modulators
		// when a controller is changed?
	}

	public String toString() {
		return "Zone with " + generators.length + " generators and "
				+ modulators.length + " modulators (key: " + keyMin + "-"
				+ keyMax + ", vel: " + velMin + "-" + velMax + ")";
	}
}
