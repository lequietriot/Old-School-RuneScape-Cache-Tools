package osrs;

public class SoundEnvelope {

	int segments;
	int[] durations;
	int[] phases;
	int start;
	int end;
	int form;
	int ticks;
	int phaseIndex;
	int step;
	int amplitude;
	int max;

	SoundEnvelope() {
		this.segments = 2;
		this.durations = new int[2];
		this.phases = new int[2];
		this.durations[1] = 65535;
		this.phases[1] = 65535;
	}

	final void decode(Buffer buffer) {
		this.form = buffer.readUnsignedByte();
		this.start = buffer.readInt();
		this.end = buffer.readInt();
		this.decodeSegments(buffer);
	}

	final void decodeSegments(Buffer buffer) {
		this.segments = buffer.readUnsignedByte();
		this.durations = new int[this.segments];
		this.phases = new int[this.segments];

		for (int segment = 0; segment < this.segments; ++segment) {
			this.durations[segment] = buffer.readUnsignedShort();
			this.phases[segment] = buffer.readUnsignedShort();
		}

	}

	final void reset() {
		this.ticks = 0;
		this.phaseIndex = 0;
		this.step = 0;
		this.amplitude = 0;
		this.max = 0;
	}

	final int doStep(int period) {
		if (this.max >= this.ticks) {
			this.amplitude = this.phases[this.phaseIndex++] << 15;
			if (this.phaseIndex >= this.segments) {
				this.phaseIndex = this.segments - 1;
			}

			this.ticks = (int)((double)this.durations[this.phaseIndex] / 65536.0D * (double) period);
			if (this.ticks > this.max) {
				this.step = ((this.phases[this.phaseIndex] << 15) - this.amplitude) / (this.ticks - this.max);
			}
		}

		this.amplitude += this.step;
		++this.max;
		return this.amplitude - this.step >> 15;
	}
}
