package osrs;

import java.math.BigInteger;

public class Buffer extends Node {

	static int[] crc32Table;
	static long[] crc64Table;
	public byte[] array;
	public int offset;

	static {
		crc32Table = new int[256]; // L: 16

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
				if (1L == (var0 & 1L)) { // L: 39
					var0 = var0 >>> 1 ^ -3932672073523589310L;
				} else {
					var0 >>>= 1; // L: 40
				}
			}

			crc64Table[var2] = var0; // L: 42
		}

	} // L: 44

	public Buffer(int var1) {
		this.array = ByteArrayPool_getArray(var1); // L: 60
		this.offset = 0; // L: 61
	} // L: 62

    public Buffer() {

    }

	public static synchronized byte[] ByteArrayPool_getArray(int var0) {
		return ByteArrayPool.ByteArrayPool_getArrayBool(var0, false); // L: 112
	}

	public Buffer(byte[] var1) {
		this.array = var1; // L: 65
		this.offset = 0; // L: 66
	} // L: 67

	static final IterableNodeHashTable readStringIntParameters(Buffer var0, IterableNodeHashTable var1) {
		int var2 = var0.readUnsignedByte(); // L: 16
		int var3;
		if (var1 == null) { // L: 17
			var3 = method7228(var2); // L: 18
			var1 = new IterableNodeHashTable(var3); // L: 19
		}

		for (var3 = 0; var3 < var2; ++var3) { // L: 21
			boolean var4 = var0.readUnsignedByte() == 1; // L: 22
			int var5 = var0.readMedium(); // L: 23
			Object var6;
			if (var4) {
				var6 = new ObjectNode(var0.readStringCp1252NullTerminated()); // L: 25
			} else {
				var6 = new IntegerNode(var0.readInt()); // L: 26
			}

			var1.put((Node)var6, (long)var5);
		}

		return var1;
	}

	public static int method7228(int var0) {
		--var0; // L: 75
		var0 |= var0 >>> 1; // L: 76
		var0 |= var0 >>> 2; // L: 77
		var0 |= var0 >>> 4; // L: 78
		var0 |= var0 >>> 8; // L: 79
		var0 |= var0 >>> 16; // L: 80
		return var0 + 1; // L: 81
	}

	public void releaseArray() {
		if (this.array != null) { // L: 70
			ByteArrayPool_release(this.array);
		}

		this.array = null; // L: 71
	} // L: 72


	public static synchronized void ByteArrayPool_release(byte[] var0) {
		if (var0.length == 100 && ByteArrayPool.ByteArrayPool_smallCount < ByteArrayPool.field4210) { // L: 116
			ByteArrayPool.ByteArrayPool_small[++ByteArrayPool.ByteArrayPool_smallCount - 1] = var0; // L: 117
		} else if (var0.length == 5000 && ByteArrayPool.ByteArrayPool_mediumCount < ByteArrayPool.field4219) { // L: 120
			ByteArrayPool.ByteArrayPool_medium[++ByteArrayPool.ByteArrayPool_mediumCount - 1] = var0; // L: 121
		} else if (var0.length == 10000 && ByteArrayPool.ByteArrayPool_largeCount < ByteArrayPool.field4220) { // L: 124
			ByteArrayPool.ByteArrayPool_large[++ByteArrayPool.ByteArrayPool_largeCount - 1] = var0; // L: 125
		} else if (var0.length == 30000 && ByteArrayPool.field4217 < ByteArrayPool.field4221) { // L: 128
			ByteArrayPool.field4225[++ByteArrayPool.field4217 - 1] = var0; // L: 129
		}
	} // L: 118 122 126 130 140

	public void writeByte(int var1) {
		this.array[++this.offset - 1] = (byte)var1; // L: 75
	} // L: 76

	public void writeShort(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 79
		this.array[++this.offset - 1] = (byte)var1; // L: 80
	} // L: 81

	public void writeMedium(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 >> 16); // L: 84
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 85
		this.array[++this.offset - 1] = (byte)var1; // L: 86
	} // L: 87

	public void writeInt(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 >> 24); // L: 90
		this.array[++this.offset - 1] = (byte)(var1 >> 16); // L: 91
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 92
		this.array[++this.offset - 1] = (byte)var1; // L: 93
	} // L: 94

	public void writeLongMedium(long var1) {
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 40)); // L: 97
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 32)); // L: 98
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 24)); // L: 99
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 16)); // L: 100
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 8)); // L: 101
		this.array[++this.offset - 1] = (byte)((int)var1); // L: 102
	} // L: 103

	public void writeLong(long var1) {
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 56)); // L: 106
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 48)); // L: 107
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 40)); // L: 108
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 32)); // L: 109
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 24)); // L: 110
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 16)); // L: 111
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 8)); // L: 112
		this.array[++this.offset - 1] = (byte)((int)var1); // L: 113
	} // L: 114

	public void writeBoolean(boolean var1) {
		this.writeByte(var1 ? 1 : 0); // L: 117
	} // L: 118

	public void writeCESU8(CharSequence var1) {
		int var3 = var1.length(); // L: 142
		int var4 = 0; // L: 143

		int var5;
		for (var5 = 0; var5 < var3; ++var5) { // L: 144
			char var12 = var1.charAt(var5); // L: 145
			if (var12 <= 127) { // L: 146
				++var4;
			} else if (var12 <= 2047) { // L: 147
				var4 += 2;
			} else {
				var4 += 3; // L: 148
			}
		}

		this.array[++this.offset - 1] = 0; // L: 153
		this.writeVarInt(var4); // L: 154
		var4 = this.offset * -2117273951; // L: 155
		byte[] var6 = this.array; // L: 157
		int var7 = this.offset; // L: 158
		int var8 = var1.length(); // L: 160
		int var9 = var7; // L: 161

		for (int var10 = 0; var10 < var8; ++var10) { // L: 162
			char var11 = var1.charAt(var10); // L: 163
			if (var11 <= 127) { // L: 164
				var6[var9++] = (byte)var11; // L: 165
			} else if (var11 <= 2047) { // L: 167
				var6[var9++] = (byte)(192 | var11 >> 6); // L: 168
				var6[var9++] = (byte)(128 | var11 & '?'); // L: 169
			} else {
				var6[var9++] = (byte)(224 | var11 >> '\f'); // L: 172
				var6[var9++] = (byte)(128 | var11 >> 6 & 63); // L: 173
				var6[var9++] = (byte)(128 | var11 & '?'); // L: 174
			}
		}

		var5 = var9 - var7; // L: 177
		this.offset = (var5 * -2117273951 + var4) * -271291039; // L: 179
	} // L: 180

	public void writeBytes(byte[] var1, int var2, int var3) {
		for (int var4 = var2; var4 < var3 + var2; ++var4) { // L: 183
			this.array[++this.offset - 1] = var1[var4];
		}

	} // L: 184

	public void method7530(Buffer var1) {
		this.writeBytes(var1.array, 0, var1.offset); // L: 187
	} // L: 188

	public void writeLengthInt(int var1) {
		if (var1 < 0) { // L: 191
			throw new IllegalArgumentException(); // L: 192
		} else {
			this.array[this.offset - var1 - 4] = (byte)(var1 >> 24); // L: 194
			this.array[this.offset - var1 - 3] = (byte)(var1 >> 16); // L: 195
			this.array[this.offset - var1 - 2] = (byte)(var1 >> 8); // L: 196
			this.array[this.offset - var1 - 1] = (byte)var1; // L: 197
		}
	} // L: 198

	public void writeLengthShort(int var1) {
		if (var1 >= 0 && var1 <= 65535) { // L: 201
			this.array[this.offset - var1 - 2] = (byte)(var1 >> 8); // L: 204
			this.array[this.offset - var1 - 1] = (byte)var1; // L: 205
		} else {
			throw new IllegalArgumentException(); // L: 202
		}
	} // L: 206

	public void method7740(int var1) {
		if (var1 >= 0 && var1 <= 255) { // L: 209
			this.array[this.offset - var1 - 1] = (byte)var1; // L: 212
		} else {
			throw new IllegalArgumentException(); // L: 210
		}
	} // L: 213

	public void writeSmartByteShort(int var1) {
		if (var1 >= 0 && var1 < 128) { // L: 216
			this.writeByte(var1); // L: 217
		} else if (var1 >= 0 && var1 < 32768) { // L: 220
			this.writeShort(var1 + 32768); // L: 221
		} else {
			throw new IllegalArgumentException(); // L: 224
		}
	} // L: 218 222

	public void writeVarInt(int var1) {
		if ((var1 & -128) != 0) { // L: 228
			if ((var1 & -16384) != 0) { // L: 229
				if ((var1 & -2097152) != 0) { // L: 230
					if ((var1 & -268435456) != 0) { // L: 231
						this.writeByte(var1 >>> 28 | 128);
					}

					this.writeByte(var1 >>> 21 | 128); // L: 232
				}

				this.writeByte(var1 >>> 14 | 128); // L: 234
			}

			this.writeByte(var1 >>> 7 | 128); // L: 236
		}

		this.writeByte(var1 & 127); // L: 238
	} // L: 239

	public int readUnsignedByte() {
		return this.array[++this.offset - 1] & 255; // L: 242
	}

	public byte readByte() {
		return this.array[++this.offset - 1]; // L: 246
	}

	public int readUnsignedShort() {
		this.offset += 2; // L: 250
		return (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 251
	}

	public int readShort() {
		this.offset += 2; // L: 255
		int var1 = (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 256
		if (var1 > 32767) { // L: 257
			var1 -= 65536;
		}

		return var1; // L: 258
	}

	public int readMedium() {
		this.offset += 3; // L: 262
		return ((this.array[this.offset - 3] & 255) << 16) + (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 263
	}

	public int readInt() {
		this.offset += 4; // L: 267
		return ((this.array[this.offset - 3] & 255) << 16) + (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8) + ((this.array[this.offset - 4] & 255) << 24); // L: 268
	}

	public long readLong() {
		long var1 = (long)this.readInt() & 4294967295L; // L: 272
		long var3 = (long)this.readInt() & 4294967295L; // L: 273
		return (var1 << 32) + var3; // L: 274
	}

	public float method7570() {
		return Float.intBitsToFloat(this.readInt()); // L: 278
	}

	public boolean readBoolean() {
		return (this.readUnsignedByte() & 1) == 1; // L: 282
	}

	public String readStringCp1252NullTerminatedOrNull() {
		if (this.array[this.offset] == 0) { // L: 286
			++this.offset; // L: 287
			return null; // L: 288
		} else {
			return this.readStringCp1252NullTerminated(); // L: 290
		}
	}

	public String readStringCp1252NullTerminated() {
		int var1 = this.offset; // L: 294

		while (this.array[++this.offset - 1] != 0) { // L: 295
		}

		int var2 = this.offset - var1 - 1; // L: 296
		return var2 == 0 ? "" : EnumComposition.decodeStringCp1252(this.array, var1, var2); // L: 297 298
	}

	public String readStringCp1252NullCircumfixed() {
		byte var1 = this.array[++this.offset - 1]; // L: 302
		if (var1 != 0) { // L: 303
			throw new IllegalStateException("");
		} else {
			int var2 = this.offset; // L: 304

			while (this.array[++this.offset - 1] != 0) { // L: 305
			}

			int var3 = this.offset - var2 - 1; // L: 306
			return var3 == 0 ? "" : EnumComposition.decodeStringCp1252(this.array, var2, var3); // L: 307 308
		}
	}

	public String readCESU8() {
		byte var1 = this.array[++this.offset - 1]; // L: 312
		if (var1 != 0) { // L: 313
			throw new IllegalStateException("");
		} else {
			int var2 = this.readVarInt(); // L: 314
			if (var2 + this.offset > this.array.length) { // L: 315
				throw new IllegalStateException("");
			} else {
				byte[] var4 = this.array; // L: 317
				int var5 = this.offset; // L: 318
				char[] var6 = new char[var2]; // L: 320
				int var7 = 0; // L: 321
				int var8 = var5; // L: 322

				int var11;
				for (int var9 = var5 + var2; var8 < var9; var6[var7++] = (char)var11) { // L: 323 324 355
					int var10 = var4[var8++] & 255; // L: 325
					if (var10 < 128) { // L: 327
						if (var10 == 0) { // L: 328
							var11 = 65533;
						} else {
							var11 = var10; // L: 329
						}
					} else if (var10 < 192) { // L: 331
						var11 = 65533;
					} else if (var10 < 224) { // L: 332
						if (var8 < var9 && (var4[var8] & 192) == 128) { // L: 333
							var11 = (var10 & 31) << 6 | var4[var8++] & 63; // L: 334
							if (var11 < 128) { // L: 335
								var11 = 65533;
							}
						} else {
							var11 = 65533; // L: 337
						}
					} else if (var10 < 240) { // L: 339
						if (var8 + 1 < var9 && (var4[var8] & 192) == 128 && (var4[var8 + 1] & 192) == 128) { // L: 340
							var11 = (var10 & 15) << 12 | (var4[var8++] & 63) << 6 | var4[var8++] & 63; // L: 341
							if (var11 < 2048) { // L: 342
								var11 = 65533;
							}
						} else {
							var11 = 65533; // L: 344
						}
					} else if (var10 < 248) { // L: 346
						if (var8 + 2 < var9 && (var4[var8] & 192) == 128 && (var4[var8 + 1] & 192) == 128 && (var4[var8 + 2] & 192) == 128) { // L: 347
							var11 = (var10 & 7) << 18 | (var4[var8++] & 63) << 12 | (var4[var8++] & 63) << 6 | var4[var8++] & 63; // L: 348
							if (var11 >= 65536 && var11 <= 1114111) { // L: 349
								var11 = 65533; // L: 350
							} else {
								var11 = 65533;
							}
						} else {
							var11 = 65533; // L: 352
						}
					} else {
						var11 = 65533; // L: 354
					}
				}

				String var3 = new String(var6, 0, var7); // L: 357
				this.offset += var2; // L: 360
				return var3; // L: 361
			}
		}
	}

	public void readBytes(byte[] var1, int var2, int var3) {
		for (int var4 = var2; var4 < var3 + var2; ++var4) {
			var1[var4] = this.array[++this.offset - 1]; // L: 365
		}

	} // L: 366

	public int readShortSmart() {
		int var1 = this.array[this.offset] & 255; // L: 369
		return var1 < 128 ? this.readUnsignedByte() - 64 : this.readUnsignedShort() - 49152; // L: 370 371
	}

	public int readUShortSmart() {
		int var1 = this.array[this.offset] & 255; // L: 375
		return var1 < 128 ? this.readUnsignedByte() : this.readUnsignedShort() - 32768; // L: 376 377
	}

	public int method7531() {
		int var1 = 0; // L: 381

		int var2;
		for (var2 = this.readUShortSmart(); var2 == 32767; var2 = this.readUShortSmart()) { // L: 382 383 385
			var1 += 32767; // L: 384
		}

		var1 += var2; // L: 387
		return var1; // L: 388
	}

	public int method7627() {
		return this.array[this.offset] < 0 ? this.readInt() & Integer.MAX_VALUE : this.readUnsignedShort(); // L: 392 393
	}

	public int method7532() {
		if (this.array[this.offset] < 0) { // L: 397
			return this.readInt() & Integer.MAX_VALUE;
		} else {
			int var1 = this.readUnsignedShort(); // L: 398
			return var1 == 32767 ? -1 : var1; // L: 399
		}
	}

	public int readVarInt() {
		byte var1 = this.array[++this.offset - 1]; // L: 404

		int var2;
		for (var2 = 0; var1 < 0; var1 = this.array[++this.offset - 1]) { // L: 405 406 408
			var2 = (var2 | var1 & 127) << 7; // L: 407
		}

		return var2 | var1; // L: 410
	}

	public void xteaEncryptAll(int[] var1) {
		int var2 = this.offset / 8; // L: 414
		this.offset = 0; // L: 415

		for (int var3 = 0; var3 < var2; ++var3) { // L: 416
			int var4 = this.readInt(); // L: 417
			int var5 = this.readInt(); // L: 418
			int var6 = 0; // L: 419
			int var7 = -1640531527; // L: 420

			for (int var8 = 32; var8-- > 0; var5 += var4 + (var4 << 4 ^ var4 >>> 5) ^ var1[var6 >>> 11 & 3] + var6) { // L: 421 422 425
				var4 += var5 + (var5 << 4 ^ var5 >>> 5) ^ var6 + var1[var6 & 3]; // L: 423
				var6 += var7; // L: 424
			}

			this.offset -= 8; // L: 427
			this.writeInt(var4); // L: 428
			this.writeInt(var5); // L: 429
		}

	} // L: 431

	public void xteaDecryptAll(int[] var1) {
		int var2 = this.offset / 8; // L: 434
		this.offset = 0; // L: 435

		for (int var3 = 0; var3 < var2; ++var3) { // L: 436
			int var4 = this.readInt(); // L: 437
			int var5 = this.readInt(); // L: 438
			int var6 = -957401312; // L: 439
			int var7 = -1640531527; // L: 440

			for (int var8 = 32; var8-- > 0; var4 -= var5 + (var5 << 4 ^ var5 >>> 5) ^ var6 + var1[var6 & 3]) { // L: 441 442 445
				var5 -= var4 + (var4 << 4 ^ var4 >>> 5) ^ var1[var6 >>> 11 & 3] + var6; // L: 443
				var6 -= var7; // L: 444
			}

			this.offset -= 8; // L: 447
			this.writeInt(var4); // L: 448
			this.writeInt(var5); // L: 449
		}

	} // L: 451

	public void xteaEncrypt(int[] var1, int var2, int var3) {
		int var4 = this.offset; // L: 454
		this.offset = var2; // L: 455
		int var5 = (var3 - var2) / 8; // L: 456

		for (int var6 = 0; var6 < var5; ++var6) { // L: 457
			int var7 = this.readInt(); // L: 458
			int var8 = this.readInt(); // L: 459
			int var9 = 0; // L: 460
			int var10 = -1640531527; // L: 461

			for (int var11 = 32; var11-- > 0; var8 += var7 + (var7 << 4 ^ var7 >>> 5) ^ var1[var9 >>> 11 & 3] + var9) { // L: 462 463 466
				var7 += var8 + (var8 << 4 ^ var8 >>> 5) ^ var9 + var1[var9 & 3]; // L: 464
				var9 += var10; // L: 465
			}

			this.offset -= 8; // L: 468
			this.writeInt(var7); // L: 469
			this.writeInt(var8); // L: 470
		}

		this.offset = var4; // L: 472
	} // L: 473

	public void xteaDecrypt(int[] var1, int var2, int var3) {
		int var4 = this.offset; // L: 476
		this.offset = var2; // L: 477
		int var5 = (var3 - var2) / 8; // L: 478

		for (int var6 = 0; var6 < var5; ++var6) { // L: 479
			int var7 = this.readInt(); // L: 480
			int var8 = this.readInt(); // L: 481
			int var9 = -957401312; // L: 482
			int var10 = -1640531527; // L: 483

			for (int var11 = 32; var11-- > 0; var7 -= var8 + (var8 << 4 ^ var8 >>> 5) ^ var9 + var1[var9 & 3]) { // L: 484 485 488
				var8 -= var7 + (var7 << 4 ^ var7 >>> 5) ^ var1[var9 >>> 11 & 3] + var9; // L: 486
				var9 -= var10; // L: 487
			}

			this.offset -= 8; // L: 490
			this.writeInt(var7); // L: 491
			this.writeInt(var8); // L: 492
		}

		this.offset = var4; // L: 494
	} // L: 495

	public void encryptRsa(BigInteger var1, BigInteger var2) {
		int var3 = this.offset; // L: 498
		this.offset = 0; // L: 499
		byte[] var4 = new byte[var3]; // L: 500
		this.readBytes(var4, 0, var3); // L: 501
		BigInteger var5 = new BigInteger(var4); // L: 502
		BigInteger var6 = var5.modPow(var1, var2); // L: 503
		byte[] var7 = var6.toByteArray(); // L: 504
		this.offset = 0; // L: 505
		this.writeShort(var7.length); // L: 506
		this.writeBytes(var7, 0, var7.length); // L: 507
	} // L: 508

	public void method7687(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 + 128); // L: 525
	} // L: 526

	public void method7542(int var1) {
		this.array[++this.offset - 1] = (byte)(0 - var1); // L: 529
	} // L: 530

	public void method7596(int var1) {
		this.array[++this.offset - 1] = (byte)(128 - var1); // L: 533
	} // L: 534

	public int method7593() {
		return this.array[++this.offset - 1] - 128 & 255; // L: 537
	}

	public int method7545() {
		return 0 - this.array[++this.offset - 1] & 255; // L: 541
	}

	public int method7546() {
		return 128 - this.array[++this.offset - 1] & 255; // L: 545
	}

	public byte method7547() {
		return (byte)(this.array[++this.offset - 1] - 128); // L: 549
	}

	public byte method7548() {
		return (byte)(0 - this.array[++this.offset - 1]); // L: 553
	}

	public byte method7549() {
		return (byte)(128 - this.array[++this.offset - 1]); // L: 557
	}

	public void method7550(int var1) {
		this.array[++this.offset - 1] = (byte)var1; // L: 561
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 562
	} // L: 563

	public void method7551(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 566
		this.array[++this.offset - 1] = (byte)(var1 + 128); // L: 567
	} // L: 568

	public void method7641(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 + 128); // L: 571
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 572
	} // L: 573

	public int method7716() {
		this.offset += 2; // L: 576
		return ((this.array[this.offset - 1] & 255) << 8) + (this.array[this.offset - 2] & 255); // L: 577
	}

	public int method7554() {
		this.offset += 2; // L: 581
		return (this.array[this.offset - 1] - 128 & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 582
	}

	public int method7576() {
		this.offset += 2; // L: 586
		return ((this.array[this.offset - 1] & 255) << 8) + (this.array[this.offset - 2] - 128 & 255); // L: 587
	}

	public int method7556() {
		this.offset += 2; // L: 591
		int var1 = ((this.array[this.offset - 1] & 255) << 8) + (this.array[this.offset - 2] & 255); // L: 592
		if (var1 > 32767) { // L: 593
			var1 -= 65536;
		}

		return var1; // L: 594
	}

	public int method7522() {
		this.offset += 2; // L: 598
		int var1 = (this.array[this.offset - 1] - 128 & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 599
		if (var1 > 32767) { // L: 600
			var1 -= 65536;
		}

		return var1; // L: 601
	}

	public int method7558() {
		this.offset += 2; // L: 605
		int var1 = ((this.array[this.offset - 1] & 255) << 8) + (this.array[this.offset - 2] - 128 & 255); // L: 606
		if (var1 > 32767) { // L: 607
			var1 -= 65536;
		}

		return var1; // L: 608
	}

	public void method7559(int var1) {
		this.array[++this.offset - 1] = (byte)var1; // L: 612
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 613
		this.array[++this.offset - 1] = (byte)(var1 >> 16); // L: 614
	} // L: 615

	public int method7544() {
		this.offset += 3; // L: 618
		return (this.array[this.offset - 3] & 255) + ((this.array[this.offset - 2] & 255) << 8) + ((this.array[this.offset - 1] & 255) << 16); // L: 619
	}

	public int method7561() {
		this.offset += 3; // L: 623
		return ((this.array[this.offset - 1] & 255) << 8) + ((this.array[this.offset - 3] & 255) << 16) + (this.array[this.offset - 2] & 255); // L: 624
	}

	public int method7503() {
		this.offset += 3; // L: 628
		return (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 3] & 255) << 8) + ((this.array[this.offset - 2] & 255) << 16); // L: 629
	}

	public void method7563(int var1) {
		this.array[++this.offset - 1] = (byte)var1; // L: 633
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 634
		this.array[++this.offset - 1] = (byte)(var1 >> 16); // L: 635
		this.array[++this.offset - 1] = (byte)(var1 >> 24); // L: 636
	} // L: 637

	public void writeIntME(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 640
		this.array[++this.offset - 1] = (byte)var1; // L: 641
		this.array[++this.offset - 1] = (byte)(var1 >> 24); // L: 642
		this.array[++this.offset - 1] = (byte)(var1 >> 16); // L: 643
	} // L: 644

	public void method7565(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 >> 16); // L: 647
		this.array[++this.offset - 1] = (byte)(var1 >> 24); // L: 648
		this.array[++this.offset - 1] = (byte)var1; // L: 649
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 650
	} // L: 651

	public int method7701() {
		this.offset += 4; // L: 654
		return (this.array[this.offset - 4] & 255) + ((this.array[this.offset - 3] & 255) << 8) + ((this.array[this.offset - 2] & 255) << 16) + ((this.array[this.offset - 1] & 255) << 24); // L: 655
	}

	public int method7567() {
		this.offset += 4; // L: 659
		return ((this.array[this.offset - 2] & 255) << 24) + ((this.array[this.offset - 4] & 255) << 8) + (this.array[this.offset - 3] & 255) + ((this.array[this.offset - 1] & 255) << 16); // L: 660
	}

	public int method7568() {
		this.offset += 4; // L: 664
		return ((this.array[this.offset - 1] & 255) << 8) + ((this.array[this.offset - 4] & 255) << 16) + (this.array[this.offset - 2] & 255) + ((this.array[this.offset - 3] & 255) << 24); // L: 665
	}

	public void method7569(byte[] var1, int var2, int var3) {
		for (int var4 = var3 + var2 - 1; var4 >= var2; --var4) {
			var1[var4] = this.array[++this.offset - 1]; // L: 669
		}

	} // L: 670

	public int readUnsignedSmart() {
		int i_2 = array[offset] & 0xff;
		return i_2 < 128 ? readUnsignedByte() - 64 : readUnsignedShort() - 49152;
	}

	public int read24BitUnsignedInteger() {
		offset += 3;
		return ((array[offset - 3] & 0xff) << 16) + (array[offset - 1] & 0xff) + ((array[offset - 2] & 0xff) << 8);
	}
}
