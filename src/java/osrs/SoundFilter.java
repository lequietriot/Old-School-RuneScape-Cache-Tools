package osrs;

public class SoundFilter {

	static float[][] minimizedCoefficients;
	static int[][] coefficients;
	static float forwardMinimizedCoefficientMultiplier;
	static int forwardMultiplier;
	int[] pairs;
	int[][][] phases;
	int[][][] magnitudes;
	int[] unity;

	static {
		minimizedCoefficients = new float[2][8];
		coefficients = new int[2][8];
	}

	SoundFilter() {
		this.pairs = new int[2];
		this.phases = new int[2][2][4];
		this.magnitudes = new int[2][2][4];
		this.unity = new int[2];
	}

	float adaptMagnitude(int direction, int i, float f) {
		float alpha = (float) this.magnitudes[direction][0][i] + f * (float)(this.magnitudes[direction][1][i] - this.magnitudes[direction][0][i]);
		alpha *= 0.0015258789F;
		return 1.0F - (float) Math.pow(10.0D, -alpha / 20.0F);
	}

	float adaptPhase(int direction, int i, float f) {
		float alpha = (float) this.phases[direction][0][i] + f * (float)(this.phases[direction][1][i] - this.phases[direction][0][i]);
		alpha *= 1.2207031E-4F;
		return normalize(alpha);
	}

	int compute(int direction, float f) {
		float magnitude;
		if (direction == 0) {
			magnitude = (float)this.unity[0] + (float)(this.unity[1] - this.unity[0]) * f;
			magnitude *= 0.0030517578F;
			forwardMinimizedCoefficientMultiplier = (float)Math.pow(0.1D, magnitude / 20.0F);
			forwardMultiplier = (int)(forwardMinimizedCoefficientMultiplier * 65536.0F);
		}

		if (this.pairs[direction] == 0) {
			return 0;
		} else {
			magnitude = this.adaptMagnitude(direction, 0, f);
			minimizedCoefficients[direction][0] = -2.0F * magnitude * (float) Math.cos(this.adaptPhase(direction, 0, f));
			minimizedCoefficients[direction][1] = magnitude * magnitude;

			float[] coefficientFloatArray;
			int pair;
			for (pair = 1; pair < this.pairs[direction]; ++pair) {
				magnitude = this.adaptMagnitude(direction, pair, f);
				float phase = -2.0F * magnitude * (float) Math.cos(this.adaptPhase(direction, pair, f));
				float coefficient = magnitude * magnitude;
				minimizedCoefficients[direction][pair * 2 + 1] = minimizedCoefficients[direction][pair * 2 - 1] * coefficient;
				minimizedCoefficients[direction][pair * 2] = minimizedCoefficients[direction][pair * 2 - 1] * phase + minimizedCoefficients[direction][pair * 2 - 2] * coefficient;

				for (int pair2 = pair * 2 - 1; pair2 >= 2; --pair2) {
					coefficientFloatArray = minimizedCoefficients[direction];
					coefficientFloatArray[pair2] += minimizedCoefficients[direction][pair2 - 1] * phase + minimizedCoefficients[direction][pair2 - 2] * coefficient;
				}

				coefficientFloatArray = minimizedCoefficients[direction];
				coefficientFloatArray[1] += minimizedCoefficients[direction][0] * phase + coefficient;
				coefficientFloatArray = minimizedCoefficients[direction];
				coefficientFloatArray[0] += phase;
			}

			if (direction == 0) {
				for (pair = 0; pair < this.pairs[0] * 2; ++pair) {
					coefficientFloatArray = minimizedCoefficients[0];
					coefficientFloatArray[pair] *= forwardMinimizedCoefficientMultiplier;
				}
			}

			for (pair = 0; pair < this.pairs[direction] * 2; ++pair) {
				coefficients[direction][pair] = (int)(minimizedCoefficients[direction][pair] * 65536.0F);
			}

			return this.pairs[direction] * 2;
		}
	}

	final void decode(Buffer buffer, SoundEnvelope envelope) {
		int count = buffer.readUnsignedByte();
		this.pairs[0] = count >> 4;
		this.pairs[1] = count & 15;
		if (count != 0) {
			this.unity[0] = buffer.readUnsignedShort();
			this.unity[1] = buffer.readUnsignedShort();
			int migrated = buffer.readUnsignedByte();

			int direction;
			int pair;
			for (direction = 0; direction < 2; ++direction) {
				for (pair = 0; pair < this.pairs[direction]; ++pair) {
					this.phases[direction][0][pair] = buffer.readUnsignedShort();
					this.magnitudes[direction][0][pair] = buffer.readUnsignedShort();
				}
			}

			for (direction = 0; direction < 2; ++direction) {
				for (pair = 0; pair < this.pairs[direction]; ++pair) {
					if ((migrated & 1 << direction * 4 << pair) != 0) {
						this.phases[direction][1][pair] = buffer.readUnsignedShort();
						this.magnitudes[direction][1][pair] = buffer.readUnsignedShort();
					} else {
						this.phases[direction][1][pair] = this.phases[direction][0][pair];
						this.magnitudes[direction][1][pair] = this.magnitudes[direction][0][pair];
					}
				}
			}

			if (migrated != 0 || this.unity[1] != this.unity[0]) {
				envelope.decodeSegments(buffer);
			}
		} else {
			int[] unityArray = this.unity;
			this.unity[1] = 0;
			unityArray[0] = 0;
		}

	}

	static float normalize(float alpha) {
		float f = 32.703197F * (float) Math.pow(2.0D, alpha);
		return f * 3.1415927F / 11025.0F;
	}
}