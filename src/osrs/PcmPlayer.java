package osrs;

import application.constants.AppConstants;

public class PcmPlayer {

	public int field272;
	public int[] samples;
	public PcmStream stream;
	int field254;
	long timeMs;
	public int capacity;
	public int field264;
	int field258;
	long field259;
	int field251;
	int field253;
	int field261;
	long field263;
	boolean field257;
	int field267;
	PcmStream[] field268;
	PcmStream[] field269;

	protected PcmPlayer() {
		this.field254 = 32;
		this.timeMs = System.currentTimeMillis();
		this.field259 = 0L;
		this.field251 = 0;
		this.field253 = 0;
		this.field261 = 0;
		this.field263 = 0L;
		this.field257 = true;
		this.field267 = 0;
		this.field268 = new PcmStream[8];
		this.field269 = new PcmStream[8];
	}

	protected void init() throws Exception {
	}

	protected void open(int var1) throws Exception {
	}

	protected int position() throws Exception {
		return this.capacity;
	}

	protected void write() throws Exception {
	}

	protected void close() {
	}

	protected void discard() throws Exception {
	}

	public final synchronized void setStream(PcmStream var1) {
		this.stream = var1;
	}

	public final synchronized void run() {
		if (this.samples != null) {
			long var1 = System.currentTimeMillis();

			try {
				if (this.field259 != 0L) {
					if (var1 < this.field259) {
						return;
					}

					this.open(this.capacity);
					this.field259 = 0L;
					this.field257 = true;
				}

				int var3 = this.position();
				if (this.field261 - var3 > this.field251) {
					this.field251 = this.field261 - var3;
				}

				int var4 = this.field258 + this.field264;
				if (var4 + 256 > 16384) {
					var4 = 16128;
				}

				if (var4 + 256 > this.capacity) {
					this.capacity += 1024;
					if (this.capacity > 16384) {
						this.capacity = 16384;
					}

					this.close();
					this.open(this.capacity);
					var3 = 0;
					this.field257 = true;
					if (var4 + 256 > this.capacity) {
						var4 = this.capacity - 256;
						this.field258 = var4 - this.field264;
					}
				}

				while (var3 < var4) {
					this.fill(this.samples, 256);
					this.write();
					var3 += 256;
				}

				if (var1 > this.field263) {
					if (!this.field257) {
						if (this.field251 == 0 && this.field253 == 0) {
							this.close();
							this.field259 = var1 + 2000L;
							return;
						}

						this.field258 = Math.min(this.field253, this.field251);
						this.field253 = this.field251;
					} else {
						this.field257 = false;
					}

					this.field251 = 0;
					this.field263 = var1 + 2000L;
				}

				this.field261 = var3;
			} catch (Exception var7) {
				this.close();
				this.field259 = var1 + 2000L;
			}

			try {
				if (var1 > this.timeMs + 500000L) {
					var1 = this.timeMs;
				}

				while (var1 > this.timeMs + 5000L) {
					this.skip(256);
					this.timeMs += 256000 / AppConstants.sampleRate;
				}
			} catch (Exception var6) {
				this.timeMs = var1;
			}

		}
	}

	public final void method750() {
		this.field257 = true;
	}

	public final synchronized void tryDiscard() {
		this.field257 = true;

		try {
			this.discard();
		} catch (Exception var2) {
			this.close();
			this.field259 = System.currentTimeMillis() + 2000L;
		}

	}

	final void skip(int var1) {
		this.field267 -= var1;
		if (this.field267 < 0) {
			this.field267 = 0;
		}

		if (this.stream != null) {
			this.stream.skip(var1);
		}

	}

	public final void fill(int[] var1, int var2) {
		int var3 = var2;
		if (AppConstants.stereo) {
			var3 = var2 << 1;
		}

		ByteBufferUtils.clearIntArray(var1, 0, var3);
		this.field267 -= var2; // L: 181
		if (this.stream != null && this.field267 <= 0) { // L: 182
			this.field267 += AppConstants.sampleRate >> 4; // L: 183
			PcmStream.PcmStream_disable(this.stream); // L: 184
			this.method719(this.stream, this.stream.vmethod974()); // L: 185
			int var4 = 0; // L: 186
			int var5 = 255; // L: 187

			int var6;
			PcmStream var10;
			label108:
			for (var6 = 7; var5 != 0; --var6) { // L: 188
				int var7;
				int var8;
				if (var6 < 0) { // L: 191
					var7 = var6 & 3; // L: 192
					var8 = -(var6 >> 2); // L: 193
				} else {
					var7 = var6; // L: 196
					var8 = 0; // L: 197
				}

				for (int var9 = var5 >>> var7 & 286331153; var9 != 0; var9 >>>= 4) { // L: 199 201
					if ((var9 & 1) != 0) { // L: 204
						var5 &= ~(1 << var7); // L: 207
						var10 = null; // L: 208
						PcmStream var11 = this.field268[var7]; // L: 209

						label102:
						while (true) {
							while (true) {
								if (var11 == null) { // L: 210
									break label102;
								}

								AudioDataPosition var12 = var11.sound; // L: 211
								if (var12 != null && var12.position > var8) { // L: 212
									var5 |= 1 << var7; // L: 213
									var10 = var11; // L: 214
									var11 = var11.after; // L: 215
								} else {
									var11.active = true; // L: 218
									int var13 = var11.vmethod4958(); // L: 219
									var4 += var13; // L: 220
									if (var12 != null) {
										var12.position += var13; // L: 221
									}

									if (var4 >= this.field254) { // L: 222
										break label108;
									}

									PcmStream var14 = var11.firstSubStream(); // L: 223
									if (var14 != null) { // L: 224
										for (int var15 = var11.field320; var14 != null; var14 = var11.nextSubStream()) { // L: 225 226 228
											this.method719(var14, var15 * var14.vmethod974() >> 8); // L: 227
										}
									}

									PcmStream var18 = var11.after; // L: 231
									var11.after = null; // L: 232
									if (var10 == null) { // L: 233
										this.field268[var7] = var18;
									} else {
										var10.after = var18; // L: 234
									}

									if (var18 == null) { // L: 235
										this.field269[var7] = var10;
									}

									var11 = var18; // L: 236
								}
							}
						}
					}

					var7 += 4; // L: 200
					++var8;
				}
			}

			for (var6 = 0; var6 < 8; ++var6) { // L: 240
				PcmStream var16 = this.field268[var6]; // L: 241
				PcmStream[] var17 = this.field268; // L: 242
				this.field269[var6] = null; // L: 244

				for (var17[var6] = null; var16 != null; var16 = var10) { // L: 245 246 249
					var10 = var16.after; // L: 247
					var16.after = null; // L: 248
				}
			}
		}

		if (this.field267 < 0) { // L: 253
			this.field267 = 0;
		}

		if (this.stream != null) {
			this.stream.fill(var1, 0, var2); // L: 254
		}

		this.timeMs = System.currentTimeMillis(); // L: 255
	}

	final void method719(PcmStream var1, int var2) {
		int var3 = var2 >> 5; // L: 265
		PcmStream var4 = this.field269[var3]; // L: 266
		if (var4 == null) { // L: 267
			this.field268[var3] = var1;
		} else {
			var4.after = var1; // L: 268
		}

		this.field269[var3] = var1; // L: 269
		var1.field320 = var2; // L: 270
	} // L: 271

}
