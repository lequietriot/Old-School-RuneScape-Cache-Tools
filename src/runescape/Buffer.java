package runescape;

import java.math.BigInteger;

public class Buffer extends Node {
	static int[] crc32Table;
	static long[] crc64Table;
	static final char[] cp1252AsciiExtension;
	public byte[] array;
	public int offset;

	static {
		crc32Table = new int[256]; // L: 16
		cp1252AsciiExtension = new char[]{'€', '\u0000', '‚', 'ƒ', '„', '…', '†', '‡', 'ˆ', '‰', 'Š', '‹', 'Œ', '\u0000', 'Ž', '\u0000', '\u0000', '‘', '’', '“', '”', '•', '–', '—', '˜', '™', 'š', '›', 'œ', '\u0000', 'ž', 'Ÿ'}; // L: 4

		int var2;
		for (int var1 = 0; var1 < 256; ++var1) { // L: 21
			int var4 = var1; // L: 22

			for (var2 = 0; var2 < 8; ++var2) { // L: 23
				if ((var4 & 1) == 1) { // L: 24
					var4 = var4 >>> 1 ^ -306674912;
				} else {
					var4 >>>= 1; // L: 25
				}
			}

			crc32Table[var1] = var4; // L: 27
		}

		crc64Table = new long[256]; // L: 31

		for (var2 = 0; var2 < 256; ++var2) { // L: 36
			long var0 = (long)var2; // L: 37

			for (int var3 = 0; var3 < 8; ++var3) { // L: 38
				if ((var0 & 1L) == 1L) { // L: 39
					var0 = var0 >>> 1 ^ -3932672073523589310L;
				} else {
					var0 >>>= 1; // L: 40
				}
			}

			crc64Table[var2] = var0; // L: 42
		}

	} // L: 44

	public Buffer(int var1) {
		this.array = new byte[var1]; // L: 56
		this.offset = 0; // L: 57
	} // L: 58

	public Buffer(byte[] var1) {
		this.array = var1; // L: 61
		this.offset = 0; // L: 62
	} // L: 63

	public void writeByte(int var1) {
		this.array[++this.offset - 1] = (byte)var1; // L: 71
	} // L: 72

	public void writeShort(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 75
		this.array[++this.offset - 1] = (byte)var1; // L: 76
	} // L: 77

	public void writeMedium(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 >> 16); // L: 80
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 81
		this.array[++this.offset - 1] = (byte)var1; // L: 82
	} // L: 83

	public void writeInt(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 >> 24); // L: 86
		this.array[++this.offset - 1] = (byte)(var1 >> 16); // L: 87
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 88
		this.array[++this.offset - 1] = (byte)var1; // L: 89
	} // L: 90

	public void writeLongMedium(long var1) {
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 40)); // L: 93
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 32)); // L: 94
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 24)); // L: 95
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 16)); // L: 96
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 8)); // L: 97
		this.array[++this.offset - 1] = (byte)((int)var1); // L: 98
	} // L: 99

	public void writeLong(long var1) {
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 56)); // L: 102
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 48)); // L: 103
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 40)); // L: 104
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 32)); // L: 105
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 24)); // L: 106
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 16)); // L: 107
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 8)); // L: 108
		this.array[++this.offset - 1] = (byte)((int)var1); // L: 109
	} // L: 110

	public void writeBoolean(boolean var1) {
		this.writeByte(var1 ? 1 : 0); // L: 113
	} // L: 114

	public void writeBytes(byte[] var1, int var2, int var3) {
		for (int var4 = var2; var4 < var3 + var2; ++var4) { // L: 155
			this.array[++this.offset - 1] = var1[var4];
		}

	} // L: 156

	public void method7668(Buffer var1) {
		this.writeBytes(var1.array, 0, var1.offset); // L: 159
	} // L: 160

	public void writeLengthInt(int var1) {
		if (var1 < 0) { // L: 163
			throw new IllegalArgumentException(); // L: 164
		} else {
			this.array[this.offset - var1 - 4] = (byte)(var1 >> 24); // L: 166
			this.array[this.offset - var1 - 3] = (byte)(var1 >> 16); // L: 167
			this.array[this.offset - var1 - 2] = (byte)(var1 >> 8); // L: 168
			this.array[this.offset - var1 - 1] = (byte)var1; // L: 169
		}
	} // L: 170

	public void writeLengthShort(int var1) {
		if (var1 >= 0 && var1 <= 65535) { // L: 173
			this.array[this.offset - var1 - 2] = (byte)(var1 >> 8); // L: 176
			this.array[this.offset - var1 - 1] = (byte)var1; // L: 177
		} else {
			throw new IllegalArgumentException(); // L: 174
		}
	} // L: 178

	public void method7774(int var1) {
		if (var1 >= 0 && var1 <= 255) { // L: 181
			this.array[this.offset - var1 - 1] = (byte)var1; // L: 184
		} else {
			throw new IllegalArgumentException(); // L: 182
		}
	} // L: 185

	public void writeSmartByteShort(int var1) {
		if (var1 >= 0 && var1 < 128) { // L: 188
			this.writeByte(var1); // L: 189
		} else if (var1 >= 0 && var1 < 32768) { // L: 192
			this.writeShort(var1 + 32768); // L: 193
		} else {
			throw new IllegalArgumentException(); // L: 196
		}
	} // L: 190 194

	public void writeVarInt(int var1) {
		if ((var1 & -128) != 0) { // L: 200
			if ((var1 & -16384) != 0) { // L: 201
				if ((var1 & -2097152) != 0) { // L: 202
					if ((var1 & -268435456) != 0) { // L: 203
						this.writeByte(var1 >>> 28 | 128);
					}

					this.writeByte(var1 >>> 21 | 128); // L: 204
				}

				this.writeByte(var1 >>> 14 | 128); // L: 206
			}

			this.writeByte(var1 >>> 7 | 128); // L: 208
		}

		this.writeByte(var1 & 127); // L: 210
	} // L: 211

	public int readUnsignedByte() {
		return this.array[++this.offset - 1] & 255; // L: 214
	}

	public byte readByte() {
		return this.array[++this.offset - 1]; // L: 218
	}

	public int readUnsignedShort() {
		this.offset += 2; // L: 222
		return (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 223
	}

	public int readShort() {
		this.offset += 2; // L: 227
		int var1 = (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 228
		if (var1 > 32767) { // L: 229
			var1 -= 65536;
		}

		return var1; // L: 230
	}

	public int readMedium() {
		this.offset += 3; // L: 234
		return ((this.array[this.offset - 3] & 255) << 16) + (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 235
	}

	public int readInt() {
		this.offset += 4; // L: 239
		return ((this.array[this.offset - 3] & 255) << 16) + (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8) + ((this.array[this.offset - 4] & 255) << 24); // L: 240
	}

	public long readLong() {
		long var1 = (long)this.readInt() & 4294967295L; // L: 244
		long var3 = (long)this.readInt() & 4294967295L; // L: 245
		return var3 + (var1 << 32); // L: 246
	}

	public float method7681() {
		return Float.intBitsToFloat(this.readInt()); // L: 250
	}

	public boolean readBoolean() {
		return (this.readUnsignedByte() & 1) == 1; // L: 254
	}

	public String readStringCp1252NullTerminatedOrNull() {
		if (this.array[this.offset] == 0) { // L: 258
			++this.offset; // L: 259
			return null; // L: 260
		} else {
			return this.readStringCp1252NullTerminated(); // L: 262
		}
	}

	public String readStringCp1252NullTerminated() {
		int var1 = this.offset; // L: 266

		while (this.array[++this.offset - 1] != 0) { // L: 267
		}

		int var2 = this.offset - var1 - 1; // L: 268
		return var2 == 0 ? "" : decodeStringCp1252(this.array, var1, var2); // L: 269 270
	}

	public static String decodeStringCp1252(byte[] var0, int var1, int var2) {
		char[] var3 = new char[var2]; // L: 132
		int var4 = 0; // L: 133

		for (int var5 = 0; var5 < var2; ++var5) { // L: 134
			int var6 = var0[var5 + var1] & 255; // L: 135
			if (var6 != 0) { // L: 136
				if (var6 >= 128 && var6 < 160) { // L: 137
					char var7 = cp1252AsciiExtension[var6 - 128]; // L: 138
					if (var7 == 0) { // L: 139
						var7 = '?';
					}

					var6 = var7; // L: 140
				}

				var3[var4++] = (char)var6; // L: 142
			}
		}

		return new String(var3, 0, var4); // L: 144
	}

	public String readCESU8() {
		byte var1 = this.array[++this.offset - 1]; // L: 284
		if (var1 != 0) { // L: 285
			throw new IllegalStateException("");
		} else {
			int var2 = this.readVarInt(); // L: 286
			if (var2 + this.offset > this.array.length) { // L: 287
				throw new IllegalStateException("");
			} else {
				byte[] var4 = this.array; // L: 289
				int var5 = this.offset; // L: 290
				char[] var6 = new char[var2]; // L: 292
				int var7 = 0; // L: 293
				int var8 = var5; // L: 294

				int var11;
				for (int var9 = var5 + var2; var8 < var9; var6[var7++] = (char)var11) { // L: 295 296 327
					int var10 = var4[var8++] & 255; // L: 297
					if (var10 < 128) { // L: 299
						if (var10 == 0) { // L: 300
							var11 = 65533;
						} else {
							var11 = var10; // L: 301
						}
					} else if (var10 < 192) { // L: 303
						var11 = 65533;
					} else if (var10 < 224) { // L: 304
						if (var8 < var9 && (var4[var8] & 192) == 128) { // L: 305
							var11 = (var10 & 31) << 6 | var4[var8++] & 63; // L: 306
							if (var11 < 128) { // L: 307
								var11 = 65533;
							}
						} else {
							var11 = 65533; // L: 309
						}
					} else if (var10 < 240) { // L: 311
						if (var8 + 1 < var9 && (var4[var8] & 192) == 128 && (var4[var8 + 1] & 192) == 128) { // L: 312
							var11 = (var10 & 15) << 12 | (var4[var8++] & 63) << 6 | var4[var8++] & 63; // L: 313
							if (var11 < 2048) { // L: 314
								var11 = 65533;
							}
						} else {
							var11 = 65533; // L: 316
						}
					} else if (var10 < 248) { // L: 318
						if (var8 + 2 < var9 && (var4[var8] & 192) == 128 && (var4[var8 + 1] & 192) == 128 && (var4[var8 + 2] & 192) == 128) { // L: 319
							var11 = (var10 & 7) << 18 | (var4[var8++] & 63) << 12 | (var4[var8++] & 63) << 6 | var4[var8++] & 63; // L: 320
							if (var11 >= 65536 && var11 <= 1114111) { // L: 321
								var11 = 65533; // L: 322
							} else {
								var11 = 65533;
							}
						} else {
							var11 = 65533; // L: 324
						}
					} else {
						var11 = 65533; // L: 326
					}
				}

				String var3 = new String(var6, 0, var7); // L: 329
				this.offset += var2; // L: 332
				return var3; // L: 333
			}
		}
	}

	public void readBytes(byte[] var1, int var2, int var3) {
		for (int var4 = var2; var4 < var3 + var2; ++var4) {
			var1[var4] = this.array[++this.offset - 1]; // L: 337
		}

	} // L: 338

	public int readShortSmart() {
		int var1 = this.array[this.offset] & 255; // L: 341
		return var1 < 128 ? this.readUnsignedByte() - 64 : this.readUnsignedShort() - 49152; // L: 342 343
	}

	public int readUShortSmart() {
		int var1 = this.array[this.offset] & 255; // L: 347
		return var1 < 128 ? this.readUnsignedByte() : this.readUnsignedShort() - 32768; // L: 348 349
	}

	public int method7743() {
		int var1 = 0; // L: 353

		int var2;
		for (var2 = this.readUShortSmart(); var2 == 32767; var2 = this.readUShortSmart()) { // L: 354 355 357
			var1 += 32767; // L: 356
		}

		var1 += var2; // L: 359
		return var1; // L: 360
	}

	public int method7758() {
		return this.array[this.offset] < 0 ? this.readInt() & Integer.MAX_VALUE : this.readUnsignedShort(); // L: 364 365
	}

	public int method7692() {
		if (this.array[this.offset] < 0) { // L: 369
			return this.readInt() & Integer.MAX_VALUE;
		} else {
			int var1 = this.readUnsignedShort(); // L: 370
			return var1 == 32767 ? -1 : var1; // L: 371
		}
	}

	public int readVarInt() {
		byte var1 = this.array[++this.offset - 1]; // L: 376

		int var2;
		for (var2 = 0; var1 < 0; var1 = this.array[++this.offset - 1]) { // L: 377 378 380
			var2 = (var2 | var1 & 127) << 7; // L: 379
		}

		return var2 | var1; // L: 382
	}

	public void xteaEncryptAll(int[] var1) {
		int var2 = this.offset / 8; // L: 386
		this.offset = 0; // L: 387

		for (int var3 = 0; var3 < var2; ++var3) { // L: 388
			int var4 = this.readInt(); // L: 389
			int var5 = this.readInt(); // L: 390
			int var6 = 0; // L: 391
			int var7 = -1640531527; // L: 392

			for (int var8 = 32; var8-- > 0; var5 += var4 + (var4 << 4 ^ var4 >>> 5) ^ var1[var6 >>> 11 & 3] + var6) { // L: 393 394 397
				var4 += var5 + (var5 << 4 ^ var5 >>> 5) ^ var6 + var1[var6 & 3]; // L: 395
				var6 += var7; // L: 396
			}

			this.offset -= 8; // L: 399
			this.writeInt(var4); // L: 400
			this.writeInt(var5); // L: 401
		}

	} // L: 403

	public void xteaDecryptAll(int[] var1) {
		int var2 = this.offset / 8; // L: 406
		this.offset = 0; // L: 407

		for (int var3 = 0; var3 < var2; ++var3) { // L: 408
			int var4 = this.readInt(); // L: 409
			int var5 = this.readInt(); // L: 410
			int var6 = -957401312; // L: 411
			int var7 = -1640531527; // L: 412

			for (int var8 = 32; var8-- > 0; var4 -= var5 + (var5 << 4 ^ var5 >>> 5) ^ var6 + var1[var6 & 3]) { // L: 413 414 417
				var5 -= var4 + (var4 << 4 ^ var4 >>> 5) ^ var1[var6 >>> 11 & 3] + var6; // L: 415
				var6 -= var7; // L: 416
			}

			this.offset -= 8; // L: 419
			this.writeInt(var4); // L: 420
			this.writeInt(var5); // L: 421
		}

	} // L: 423

	public void xteaEncrypt(int[] var1, int var2, int var3) {
		int var4 = this.offset; // L: 426
		this.offset = var2; // L: 427
		int var5 = (var3 - var2) / 8; // L: 428

		for (int var6 = 0; var6 < var5; ++var6) { // L: 429
			int var7 = this.readInt(); // L: 430
			int var8 = this.readInt(); // L: 431
			int var9 = 0; // L: 432
			int var10 = -1640531527; // L: 433

			for (int var11 = 32; var11-- > 0; var8 += var7 + (var7 << 4 ^ var7 >>> 5) ^ var1[var9 >>> 11 & 3] + var9) { // L: 434 435 438
				var7 += var8 + (var8 << 4 ^ var8 >>> 5) ^ var9 + var1[var9 & 3]; // L: 436
				var9 += var10; // L: 437
			}

			this.offset -= 8; // L: 440
			this.writeInt(var7); // L: 441
			this.writeInt(var8); // L: 442
		}

		this.offset = var4; // L: 444
	} // L: 445

	public void xteaDecrypt(int[] var1, int var2, int var3) {
		int var4 = this.offset; // L: 448
		this.offset = var2; // L: 449
		int var5 = (var3 - var2) / 8; // L: 450

		for (int var6 = 0; var6 < var5; ++var6) { // L: 451
			int var7 = this.readInt(); // L: 452
			int var8 = this.readInt(); // L: 453
			int var9 = -957401312; // L: 454
			int var10 = -1640531527; // L: 455

			for (int var11 = 32; var11-- > 0; var7 -= var8 + (var8 << 4 ^ var8 >>> 5) ^ var9 + var1[var9 & 3]) { // L: 456 457 460
				var8 -= var7 + (var7 << 4 ^ var7 >>> 5) ^ var1[var9 >>> 11 & 3] + var9; // L: 458
				var9 -= var10; // L: 459
			}

			this.offset -= 8; // L: 462
			this.writeInt(var7); // L: 463
			this.writeInt(var8); // L: 464
		}

		this.offset = var4; // L: 466
	} // L: 467

	public void encryptRsa(BigInteger var1, BigInteger var2) {
		int var3 = this.offset; // L: 470
		this.offset = 0; // L: 471
		byte[] var4 = new byte[var3]; // L: 472
		this.readBytes(var4, 0, var3); // L: 473
		BigInteger var5 = new BigInteger(var4); // L: 474
		BigInteger var6 = var5.modPow(var1, var2); // L: 475
		byte[] var7 = var6.toByteArray(); // L: 476
		this.offset = 0; // L: 477
		this.writeShort(var7.length); // L: 478
		this.writeBytes(var7, 0, var7.length); // L: 479
	} // L: 480

	public int writeCrc(int var1) {
		byte[] var3 = this.array; // L: 484
		int var4 = this.offset; // L: 485
		int var5 = -1; // L: 487

		for (int var6 = var1; var6 < var4; ++var6) { // L: 488
			var5 = var5 >>> 8 ^ crc32Table[(var5 ^ var3[var6]) & 255]; // L: 489
		}

		var5 = ~var5; // L: 491
		this.writeInt(var5); // L: 495
		return var5; // L: 496
	}

	public void method7701(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 + 128); // L: 508
	} // L: 509

	public void method7804(int var1) {
		this.array[++this.offset - 1] = (byte)(0 - var1); // L: 512
	} // L: 513

	public void method7703(int var1) {
		this.array[++this.offset - 1] = (byte)(128 - var1); // L: 516
	} // L: 517

	public int method7875() {
		return this.array[++this.offset - 1] - 128 & 255; // L: 520
	}

	public int method7773() {
		return 0 - this.array[++this.offset - 1] & 255; // L: 524
	}

	public int method7742() {
		return 128 - this.array[++this.offset - 1] & 255; // L: 528
	}

	public byte method7707() {
		return (byte)(this.array[++this.offset - 1] - 128); // L: 532
	}

	public byte method7708() {
		return (byte)(0 - this.array[++this.offset - 1]); // L: 536
	}

	public byte method7885() {
		return (byte)(128 - this.array[++this.offset - 1]); // L: 540
	}

	public void method7710(int var1) {
		this.array[++this.offset - 1] = (byte)var1; // L: 544
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 545
	} // L: 546

	public void method7711(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 549
		this.array[++this.offset - 1] = (byte)(var1 + 128); // L: 550
	} // L: 551

	public void method7712(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 + 128); // L: 554
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 555
	} // L: 556

	public int method7713() {
		this.offset += 2; // L: 559
		return ((this.array[this.offset - 1] & 255) << 8) + (this.array[this.offset - 2] & 255); // L: 560
	}

	public int method7714() {
		this.offset += 2; // L: 564
		return (this.array[this.offset - 1] - 128 & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 565
	}

	public int method7715() {
		this.offset += 2; // L: 569
		return ((this.array[this.offset - 1] & 255) << 8) + (this.array[this.offset - 2] - 128 & 255); // L: 570
	}

	public int method7716() {
		this.offset += 2; // L: 574
		int var1 = ((this.array[this.offset - 1] & 255) << 8) + (this.array[this.offset - 2] & 255); // L: 575
		if (var1 > 32767) { // L: 576
			var1 -= 65536;
		}

		return var1; // L: 577
	}

	public int method7717() {
		this.offset += 2; // L: 581
		int var1 = (this.array[this.offset - 1] - 128 & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 582
		if (var1 > 32767) { // L: 583
			var1 -= 65536;
		}

		return var1; // L: 584
	}

	public void method7680(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 588
		this.array[++this.offset - 1] = (byte)(var1 >> 16); // L: 589
		this.array[++this.offset - 1] = (byte)var1; // L: 590
	} // L: 591

	public int method7752() {
		this.offset += 3; // L: 594
		return (this.array[this.offset - 3] & 255) + ((this.array[this.offset - 2] & 255) << 8) + ((this.array[this.offset - 1] & 255) << 16); // L: 595
	}

	public int method7793() {
		this.offset += 3; // L: 599
		return (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 3] & 255) << 8) + ((this.array[this.offset - 2] & 255) << 16); // L: 600
	}

	public void method7721(int var1) {
		this.array[++this.offset - 1] = (byte)var1; // L: 604
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 605
		this.array[++this.offset - 1] = (byte)(var1 >> 16); // L: 606
		this.array[++this.offset - 1] = (byte)(var1 >> 24); // L: 607
	} // L: 608

	public void writeIntME(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 611
		this.array[++this.offset - 1] = (byte)var1; // L: 612
		this.array[++this.offset - 1] = (byte)(var1 >> 24); // L: 613
		this.array[++this.offset - 1] = (byte)(var1 >> 16); // L: 614
	} // L: 615

	public void method7723(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 >> 16); // L: 618
		this.array[++this.offset - 1] = (byte)(var1 >> 24); // L: 619
		this.array[++this.offset - 1] = (byte)var1; // L: 620
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 621
	} // L: 622

	public int method7705() {
		this.offset += 4; // L: 625
		return (this.array[this.offset - 4] & 255) + ((this.array[this.offset - 3] & 255) << 8) + ((this.array[this.offset - 2] & 255) << 16) + ((this.array[this.offset - 1] & 255) << 24); // L: 626
	}

	public int method7827() {
		this.offset += 4; // L: 630
		return ((this.array[this.offset - 2] & 255) << 24) + ((this.array[this.offset - 4] & 255) << 8) + (this.array[this.offset - 3] & 255) + ((this.array[this.offset - 1] & 255) << 16); // L: 631
	}

	public int method7837() {
		this.offset += 4; // L: 635
		return ((this.array[this.offset - 1] & 255) << 8) + ((this.array[this.offset - 4] & 255) << 16) + (this.array[this.offset - 2] & 255) + ((this.array[this.offset - 3] & 255) << 24); // L: 636
	}

	public void method7814(byte[] var1, int var2, int var3) {
		for (int var4 = var3 + var2 - 1; var4 >= var2; --var4) { // L: 640
			var1[var4] = this.array[++this.offset - 1];
		}

	} // L: 641
}
