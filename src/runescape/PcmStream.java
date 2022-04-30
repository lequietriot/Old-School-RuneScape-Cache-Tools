package runescape;

public abstract class PcmStream extends Node {

	volatile boolean active;
	PcmStream after;
	int field320;
	AbstractSound sound;

	protected PcmStream() {
		this.active = true;
	}

	protected abstract PcmStream firstSubStream();

	protected abstract PcmStream nextSubStream();

	protected abstract int vmethod4958();

	protected abstract void fill(int[] var1, int var2, int var3);

	public abstract void skip(int var1);

	int vmethod974() {
		return 255;
	}

	final void update(int[] var1, int var2, int var3) {
		if (this.active) {
			this.fill(var1, var2, var3);
		} else {
			this.skip(var3);
		}

	}

	static final void PcmStream_disable(PcmStream var0) {
		var0.active = false;
		if (var0.sound != null) {
			var0.sound.position = 0;
		}

		for (PcmStream var1 = var0.firstSubStream(); var1 != null; var1 = var0.nextSubStream()) {
			PcmStream_disable(var1);
		}

	}
}
