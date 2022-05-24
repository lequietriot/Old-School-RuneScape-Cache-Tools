package osrs;

import java.util.Random;

public class SoundTone {

	static int[] toneSamples;
	static int[] toneNoise;
	static int[] toneSine;
	static int[] tonePhases;
	static int[] toneDelays;
	static int[] toneVolumeSteps;
	static int[] tonePitchSteps;
	static int[] tonePitchBaseSteps;
	SoundEnvelope pitch;
	SoundEnvelope volume;
	SoundEnvelope pitchModifier;
	SoundEnvelope pitchModifierAmplitude;
	SoundEnvelope volumeMultiplier;
	SoundEnvelope volumeMultiplierAmplitude;
	SoundEnvelope release;
	SoundEnvelope attack;
	int[] oscillatorVolume;
	int[] oscillatorPitch;
	int[] oscillatorDelays;
	int delayTime;
	int delayDecay;
	SoundFilter filter;
	SoundEnvelope filterEnvelope;
	public int duration;
	public int offset;

	static {
		toneNoise = new int[32768];
		Random randomID = new Random(0L);

		int toneID;
		for (toneID = 0; toneID < 32768; ++toneID) {
			toneNoise[toneID] = (randomID.nextInt() & 2) - 1;
		}

		toneSine = new int[32768];

		for (toneID = 0; toneID < 32768; ++toneID) {
			toneSine[toneID] = (int)(Math.sin((double) toneID / 5215.1903D) * 16384.0D);
		}

		toneSamples = new int[220500];
		tonePhases = new int[5];
		toneDelays = new int[5];
		toneVolumeSteps = new int[5];
		tonePitchSteps = new int[5];
		tonePitchBaseSteps = new int[5];
	}

	public SoundTone() {
		this.oscillatorVolume = new int[]{0, 0, 0, 0, 0};
		this.oscillatorPitch = new int[]{0, 0, 0, 0, 0};
		this.oscillatorDelays = new int[]{0, 0, 0, 0, 0};
		this.delayTime = 0;
		this.delayDecay = 100;
		this.duration = 500;
		this.offset = 0;
	}

	public final int[] synthesize(int steps, int tones) {
		ByteBufferUtils.clearIntArray(toneSamples, 0, steps);
		if (tones >= 10) {
			double duration = (double) steps / ((double) tones + 0.0D);
			this.pitch.reset();
			this.volume.reset();
			int pitchModulationStep = 0;
			int pitchModulationBaseStep = 0;
			int pitchModulationPhase = 0;
			if (this.pitchModifier != null) {
				this.pitchModifier.reset();
				this.pitchModifierAmplitude.reset();
				pitchModulationStep = (int) ((double) (this.pitchModifier.end - this.pitchModifier.start) * 32.768D / duration);
				pitchModulationBaseStep = (int) ((double) this.pitchModifier.start * 32.768D / duration);
			}

			int volumeModulationStep = 0;
			int volumeModulationBaseStep = 0;
			int volumeModulationPhase = 0;
			if (this.volumeMultiplier != null) {
				this.volumeMultiplier.reset();
				this.volumeMultiplierAmplitude.reset();
				volumeModulationStep = (int) ((double) (this.volumeMultiplier.end - this.volumeMultiplier.start) * 32.768D / duration);
				volumeModulationBaseStep = (int) ((double) this.volumeMultiplier.start * 32.768D / duration);
			}

			int step;
			for (step = 0; step < 5; ++step) {
				if (this.oscillatorVolume[step] != 0) {
					tonePhases[step] = 0;
					toneDelays[step] = (int) ((double) this.oscillatorDelays[step] * duration);
					toneVolumeSteps[step] = (this.oscillatorVolume[step] << 14) / 100;
					tonePitchSteps[step] = (int) ((double) (this.pitch.end - this.pitch.start) * 32.768D * Math.pow(1.0057929410678534D, this.oscillatorPitch[step]) / duration);
					tonePitchBaseSteps[step] = (int) ((double) this.pitch.start * 32.768D / duration);
				}
			}

			int pitchChange;
			int volumeChange;
			int volumeMultiplierChange;
			int volumeMultiplierAmplitudeChange;
			int[] samples;
			for (step = 0; step < steps; ++step) {
				pitchChange = this.pitch.doStep(steps);
				volumeChange = this.volume.doStep(steps);
				if (this.pitchModifier != null) {
					volumeMultiplierChange = this.pitchModifier.doStep(steps);
					volumeMultiplierAmplitudeChange = this.pitchModifierAmplitude.doStep(steps);
					pitchChange += this.evaluateWave(pitchModulationPhase, volumeMultiplierAmplitudeChange, this.pitchModifier.form) >> 1;
					pitchModulationPhase = pitchModulationPhase + pitchModulationBaseStep + (volumeMultiplierChange * pitchModulationStep >> 16);
				}

				if (this.volumeMultiplier != null) {
					volumeMultiplierChange = this.volumeMultiplier.doStep(steps);
					volumeMultiplierAmplitudeChange = this.volumeMultiplierAmplitude.doStep(steps);
					volumeChange = volumeChange * ((this.evaluateWave(volumeModulationPhase, volumeMultiplierAmplitudeChange, this.volumeMultiplier.form) >> 1) + 32768) >> 15;
					volumeModulationPhase = volumeModulationPhase + volumeModulationBaseStep + (volumeMultiplierChange * volumeModulationStep >> 16);
				}

				for (volumeMultiplierChange = 0; volumeMultiplierChange < 5; ++volumeMultiplierChange) {
					if (this.oscillatorVolume[volumeMultiplierChange] != 0) {
						volumeMultiplierAmplitudeChange = toneDelays[volumeMultiplierChange] + step;
						if (volumeMultiplierAmplitudeChange < steps) {
							samples = toneSamples;
							samples[volumeMultiplierAmplitudeChange] += this.evaluateWave(tonePhases[volumeMultiplierChange], volumeChange * toneVolumeSteps[volumeMultiplierChange] >> 15, this.pitch.form);
							samples = tonePhases;
							samples[volumeMultiplierChange] += (pitchChange * tonePitchSteps[volumeMultiplierChange] >> 16) + tonePitchBaseSteps[volumeMultiplierChange];
						}
					}
				}
			}

			int volumeAttackAmplitudeChange;
			if (this.release != null) {
				this.release.reset();
				this.attack.reset();
				step = 0;
				boolean muted = true;

				for (volumeMultiplierChange = 0; volumeMultiplierChange < steps; ++volumeMultiplierChange) {
					volumeMultiplierAmplitudeChange = this.release.doStep(steps);
					volumeAttackAmplitudeChange = this.attack.doStep(steps);
					if (muted) {
						pitchChange = (volumeMultiplierAmplitudeChange * (this.release.end - this.release.start) >> 8) + this.release.start;
					} else {
						pitchChange = (volumeAttackAmplitudeChange * (this.release.end - this.release.start) >> 8) + this.release.start;
					}

					step += 256;
					if (step >= pitchChange) {
						step = 0;
						muted = !muted;
					}

					if (muted) {
						toneSamples[volumeMultiplierChange] = 0;
					}
				}
			}

			if (this.delayTime > 0 && this.delayDecay > 0) {
				step = (int) ((double) this.delayTime * duration);

				for (pitchChange = step; pitchChange < steps; ++pitchChange) {
					samples = toneSamples;
					samples[pitchChange] += toneSamples[pitchChange - step] * this.delayDecay / 100;
				}
			}

			if (this.filter.pairs[0] > 0 || this.filter.pairs[1] > 0) {
				this.filterEnvelope.reset();
				step = this.filterEnvelope.doStep(steps + 1);
				pitchChange = this.filter.compute(0, (float) step / 65536.0F);
				volumeChange = this.filter.compute(1, (float) step / 65536.0F);
				if (steps >= pitchChange + volumeChange) {
					volumeMultiplierChange = 0;
					volumeMultiplierAmplitudeChange = Math.min(volumeChange, steps - pitchChange);
					int var17;
					while (volumeMultiplierChange < volumeMultiplierAmplitudeChange) {
						volumeAttackAmplitudeChange = (int) ((long) toneSamples[volumeMultiplierChange + pitchChange] * (long) SoundFilter.forwardMultiplier >> 16);

						for (var17 = 0; var17 < pitchChange; ++var17) {
							volumeAttackAmplitudeChange += (int) ((long) toneSamples[volumeMultiplierChange + pitchChange - 1 - var17] * (long) SoundFilter.coefficients[0][var17] >> 16);
						}

						for (var17 = 0; var17 < volumeMultiplierChange; ++var17) {
							volumeAttackAmplitudeChange -= (int) ((long) toneSamples[volumeMultiplierChange - 1 - var17] * (long) SoundFilter.coefficients[1][var17] >> 16);
						}

						toneSamples[volumeMultiplierChange] = volumeAttackAmplitudeChange;
						step = this.filterEnvelope.doStep(steps + 1);
						++volumeMultiplierChange;
					}

					volumeMultiplierAmplitudeChange = 128;

					while (true) {
						if (volumeMultiplierAmplitudeChange > steps - pitchChange) {
							volumeMultiplierAmplitudeChange = steps - pitchChange;
						}

						int var18;
						while (volumeMultiplierChange < volumeMultiplierAmplitudeChange) {
							var17 = (int) ((long) toneSamples[volumeMultiplierChange + pitchChange] * (long) SoundFilter.forwardMultiplier >> 16);

							for (var18 = 0; var18 < pitchChange; ++var18) {
								var17 += (int) ((long) toneSamples[volumeMultiplierChange + pitchChange - 1 - var18] * (long) SoundFilter.coefficients[0][var18] >> 16);
							}

							for (var18 = 0; var18 < volumeChange; ++var18) {
								var17 -= (int) ((long) toneSamples[volumeMultiplierChange - 1 - var18] * (long) SoundFilter.coefficients[1][var18] >> 16);
							}

							toneSamples[volumeMultiplierChange] = var17;
							step = this.filterEnvelope.doStep(steps + 1);
							++volumeMultiplierChange;
						}

						if (volumeMultiplierChange >= steps - pitchChange) {
							while (volumeMultiplierChange < steps) {
								var17 = 0;

								for (var18 = volumeMultiplierChange + pitchChange - steps; var18 < pitchChange; ++var18) {
									var17 += (int) ((long) toneSamples[volumeMultiplierChange + pitchChange - 1 - var18] * (long) SoundFilter.coefficients[0][var18] >> 16);
								}

								for (var18 = 0; var18 < volumeChange; ++var18) {
									var17 -= (int) ((long) toneSamples[volumeMultiplierChange - 1 - var18] * (long) SoundFilter.coefficients[1][var18] >> 16);
								}

								toneSamples[volumeMultiplierChange] = var17;
								this.filterEnvelope.doStep(steps + 1);
								++volumeMultiplierChange;
							}
							break;
						}

						pitchChange = this.filter.compute(0, (float) step / 65536.0F);
						volumeChange = this.filter.compute(1, (float) step / 65536.0F);
						volumeMultiplierAmplitudeChange += 128;
					}
				}
			}

			for (step = 0; step < steps; ++step) {
				if (toneSamples[step] < -32768) {
					toneSamples[step] = -32768;
				}

				if (toneSamples[step] > 32767) {
					toneSamples[step] = 32767;
				}
			}

		}
		return toneSamples;
	}

	final int evaluateWave(int var1, int var2, int var3) {
		if (var3 == 1) {
			return (var1 & 32767) < 16384 ? var2 : -var2;
		} else if (var3 == 2) {
			return toneSine[var1 & 32767] * var2 >> 14;
		} else if (var3 == 3) {
			return (var2 * (var1 & 32767) >> 14) - var2;
		} else {
			return var3 == 4 ? var2 * toneNoise[var1 / 2607 & 32767] : 0;
		}
	}

	public final void decode(Buffer var1) {
		this.pitch = new SoundEnvelope();
		this.pitch.decode(var1);
		this.volume = new SoundEnvelope();
		this.volume.decode(var1);
		int var2 = var1.readUnsignedByte();
		if (var2 != 0) {
			--var1.offset;
			this.pitchModifier = new SoundEnvelope();
			this.pitchModifier.decode(var1);
			this.pitchModifierAmplitude = new SoundEnvelope();
			this.pitchModifierAmplitude.decode(var1);
		}

		var2 = var1.readUnsignedByte();
		if (var2 != 0) {
			--var1.offset;
			this.volumeMultiplier = new SoundEnvelope();
			this.volumeMultiplier.decode(var1);
			this.volumeMultiplierAmplitude = new SoundEnvelope();
			this.volumeMultiplierAmplitude.decode(var1);
		}

		var2 = var1.readUnsignedByte();
		if (var2 != 0) {
			--var1.offset;
			this.release = new SoundEnvelope();
			this.release.decode(var1);
			this.attack = new SoundEnvelope();
			this.attack.decode(var1);
		}

		for (int var3 = 0; var3 < 10; ++var3) {
			int var4 = var1.readUShortSmart();
			if (var4 == 0) {
				break;
			}

			this.oscillatorVolume[var3] = var4;
			this.oscillatorPitch[var3] = var1.readShortSmart();
			this.oscillatorDelays[var3] = var1.readUShortSmart();
		}

		this.delayTime = var1.readUShortSmart();
		this.delayDecay = var1.readUShortSmart();
		this.duration = var1.readUnsignedShort();
		this.offset = var1.readUnsignedShort();
		this.filter = new SoundFilter();
		this.filterEnvelope = new SoundEnvelope();
		this.filter.decode(var1, this.filterEnvelope);
	}
}
