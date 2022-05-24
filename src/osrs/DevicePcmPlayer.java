package osrs;

import application.constants.AppConstants;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DevicePcmPlayer extends PcmPlayer {

	public AudioFormat format;
	SourceDataLine line;
	int capacity2;
	byte[] byteSamples;
	public ByteArrayOutputStream byteArrayOutputStream;

	public void init() {
		this.format = new AudioFormat((float) AppConstants.sampleRate, 16, AppConstants.stereo ? 2 : 1, true, false);
		this.byteSamples = new byte[256 << (AppConstants.stereo ? 2 : 1)];
		this.byteArrayOutputStream = new ByteArrayOutputStream();
	}

	public void open(int var1) throws LineUnavailableException {
		try {
			Info var2 = new Info(SourceDataLine.class, this.format, var1 << (AppConstants.stereo ? 2 : 1));
			this.line = (SourceDataLine)AudioSystem.getLine(var2);
			this.line.open();
			this.line.start();
			this.capacity2 = var1;
		} catch (LineUnavailableException var5) {
			if (method4199(var1) != 1) {
				int var4 = var1 - 1;
				var4 |= var4 >>> 1;
				var4 |= var4 >>> 2;
				var4 |= var4 >>> 4;
				var4 |= var4 >>> 8;
				var4 |= var4 >>> 16;
				int var3 = var4 + 1;
				this.open(var3);
			} else {
				this.line = null;
				throw var5;
			}
		}
	}

	public static int method4199(int var0) {
		var0 = (var0 & 1431655765) + (var0 >>> 1 & 1431655765);
		var0 = (var0 >>> 2 & 858993459) + (var0 & 858993459);
		var0 = var0 + (var0 >>> 4) & 252645135;
		var0 += var0 >>> 8;
		var0 += var0 >>> 16;
		return var0 & 255;
	}

	protected int position() {
		return this.capacity2 - (this.line.available() >> (AppConstants.stereo ? 2 : 1));
	}

	public void write() {
		int var1 = 256;
		if (AppConstants.stereo) {
			var1 <<= 1;
		}

		for (int var2 = 0; var2 < var1; ++var2) {
			int var3 = super.samples[var2];
			if ((var3 + 8388608 & -16777216) != 0) {
				var3 = 8388607 ^ var3 >> 31;
			}

			this.byteSamples[var2 * 2] = (byte)(var3 >> 8);
			this.byteSamples[var2 * 2 + 1] = (byte)(var3 >> 16);
		}
		this.line.write(this.byteSamples, 0, var1 << 1);
	}

	public void writeToBuffer() {
		int var1 = 256;
		if (AppConstants.stereo) {
			var1 <<= 1;
		}

		for (int var2 = 0; var2 < var1; ++var2) {
			int var3 = super.samples[var2];
			if ((var3 + 8388608 & -16777216) != 0) {
				var3 = 8388607 ^ var3 >> 31;
			}

			this.byteSamples[var2 * 2] = (byte)(var3 >> 8 >> 1);
			this.byteSamples[var2 * 2 + 1] = (byte)(var3 >> 16 >> 1);
		}
		try {
			this.byteArrayOutputStream.write(this.byteSamples);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void close() {
		if (this.line != null) {
			this.line.close();
			this.line = null;
		}

	}

	protected void discard() {
		this.line.flush();
	}
}
