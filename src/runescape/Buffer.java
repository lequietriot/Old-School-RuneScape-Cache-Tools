package runescape;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Buffer extends Node {

	static int[] crc32Table;
	static long[] crc64Table;

	public byte[] array;
	public int offset;

	private static final char[] cp1252AsciiExtension;

	private static final int DEFAULT_BUFFER_SIZE = 8192;

	private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

	static {

		cp1252AsciiExtension = new char[]{'€', '\u0000', '‚', 'ƒ', '„', '…', '†', '‡', 'ˆ', '‰', 'Š', '‹', 'Œ', '\u0000', 'Ž', '\u0000', '\u0000', '‘', '’', '“', '”', '•', '–', '—', '˜', '™', 'š', '›', 'œ', '\u0000', 'ž', 'Ÿ'}; // L: 4

		crc32Table = new int[256]; // L: 16

		int var2;
		for (int var1 = 0; var1 < 256; ++var1) { // L: 21
			int var4 = var1; // L: 22

			for (var2 = 0; var2 < 8; ++var2) { // L: 23
				if ((var4 & 1) == 1) { // L: 24
					var4 = var4 >>> 1 ^ -306674912;
				} else {
					var4 >>>= 1;
				}
			}

			crc32Table[var1] = var4;
		}

		crc64Table = new long[256];

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

	public Buffer(byte[] var1) {
		this.array = var1; // L: 74
		this.offset = 0; // L: 75
	} // L: 76

	public static byte[] readAllBytes(InputStream inputStream) throws IOException {
		return readNBytes(inputStream, Integer.MAX_VALUE);
	}

	public static byte[] readNBytes(InputStream inputStream, int len) throws IOException {
		if (len < 0) {
			throw new IllegalArgumentException("len < 0");
		}

		List<byte[]> bufs = null;
		byte[] result = null;
		int total = 0;
		int remaining = len;
		int n;
		do {
			byte[] buf = new byte[Math.min(remaining, DEFAULT_BUFFER_SIZE)];
			int nread = 0;

			// read to EOF which may read more or less than buffer size
			while ((n = inputStream.read(buf, nread,
					Math.min(buf.length - nread, remaining))) > 0) {
				nread += n;
				remaining -= n;
			}

			if (nread > 0) {
				if (MAX_BUFFER_SIZE - total < nread) {
					throw new OutOfMemoryError("Required array size too large");
				}
				if (nread < buf.length) {
					buf = Arrays.copyOfRange(buf, 0, nread);
				}
				total += nread;
				if (result == null) {
					result = buf;
				} else {
					if (bufs == null) {
						bufs = new ArrayList<>();
						bufs.add(result);
					}
					bufs.add(buf);
				}
			}
			// if the last call to read returned -1 or the number of bytes
			// requested have been read then break
		} while (n >= 0 && remaining > 0);

		if (bufs == null) {
			if (result == null) {
				return new byte[0];
			}
			return result.length == total ?
					result : Arrays.copyOf(result, total);
		}

		result = new byte[total];
		int offset = 0;
		remaining = total;
		for (byte[] b : bufs) {
			int count = Math.min(b.length, remaining);
			System.arraycopy(b, 0, result, offset, count);
			offset += count;
			remaining -= count;
		}

		return result;
	}

	public void writeByte(int var1) {
		this.array[++this.offset - 1] = (byte)var1; // L: 84
	} // L: 85

	public void writeShort(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 88
		this.array[++this.offset - 1] = (byte)var1; // L: 89
	} // L: 90

	public void writeMedium(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 >> 16); // L: 93
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 94
		this.array[++this.offset - 1] = (byte)var1; // L: 95
	} // L: 96

	public void writeInt(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 >> 24); // L: 99
		this.array[++this.offset - 1] = (byte)(var1 >> 16); // L: 100
		this.array[++this.offset - 1] = (byte)(var1 >> 8); // L: 101
		this.array[++this.offset - 1] = (byte)var1; // L: 102
	} // L: 103

	public void writeLongMedium(long var1) {
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 40)); // L: 106
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 32)); // L: 107
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 24)); // L: 108
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 16)); // L: 109
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 8)); // L: 110
		this.array[++this.offset - 1] = (byte)((int)var1); // L: 111
	} // L: 112

	public void writeLong(long var1) {
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 56)); // L: 115
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 48)); // L: 116
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 40)); // L: 117
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 32)); // L: 118
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 24)); // L: 119
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 16)); // L: 120
		this.array[++this.offset - 1] = (byte)((int)(var1 >> 8)); // L: 121
		this.array[++this.offset - 1] = (byte)((int)var1); // L: 122
	} // L: 123

	public void writeBoolean(boolean var1) {
		this.writeByte(var1 ? 1 : 0); // L: 126
	} // L: 127

	public void writeCESU8(CharSequence var1) {
		int var3 = var1.length(); // L: 151
		int var4 = 0; // L: 152

		int var5;
		for (var5 = 0; var5 < var3; ++var5) { // L: 153
			char var12 = var1.charAt(var5); // L: 154
			if (var12 <= 127) { // L: 155
				++var4;
			} else if (var12 <= 2047) { // L: 156
				var4 += 2;
			} else {
				var4 += 3; // L: 157
			}
		}

		this.array[++this.offset - 1] = 0; // L: 162
		this.writeVarInt(var4); // L: 163
		var4 = this.offset * -288034005; // L: 164
		byte[] var6 = this.array; // L: 166
		int var7 = this.offset; // L: 167
		int var8 = var1.length(); // L: 169
		int var9 = var7; // L: 170

		for (int var10 = 0; var10 < var8; ++var10) { // L: 171
			char var11 = var1.charAt(var10); // L: 172
			if (var11 <= 127) { // L: 173
				var6[var9++] = (byte)var11; // L: 174
			} else if (var11 <= 2047) { // L: 176
				var6[var9++] = (byte)(192 | var11 >> 6); // L: 177
				var6[var9++] = (byte)(128 | var11 & '?'); // L: 178
			} else {
				var6[var9++] = (byte)(224 | var11 >> '\f'); // L: 181
				var6[var9++] = (byte)(128 | var11 >> 6 & 63); // L: 182
				var6[var9++] = (byte)(128 | var11 & '?'); // L: 183
			}
		}

		var5 = var9 - var7; // L: 186
		this.offset = (var5 * -288034005 + var4) * 1120023427; // L: 188
	} // L: 189

	public void writeBytes(byte[] var1, int var2, int var3) {
		for (int var4 = var2; var4 < var3 + var2; ++var4) { // L: 192
			this.array[++this.offset - 1] = var1[var4];
		}

	} // L: 193

	public void method6960(Buffer var1) {
		this.writeBytes(var1.array, 0, var1.offset); // L: 196
	} // L: 197

	public void writeLengthInt(int var1) {
		if (var1 < 0) { // L: 200
			throw new IllegalArgumentException(); // L: 201
		} else {
			this.array[this.offset - var1 - 4] = (byte)(var1 >> 24); // L: 203
			this.array[this.offset - var1 - 3] = (byte)(var1 >> 16); // L: 204
			this.array[this.offset - var1 - 2] = (byte)(var1 >> 8); // L: 205
			this.array[this.offset - var1 - 1] = (byte)var1; // L: 206
		}
	} // L: 207

	public void writeLengthShort(int var1) {
		if (var1 >= 0 && var1 <= 65535) { // L: 210
			this.array[this.offset - var1 - 2] = (byte)(var1 >> 8); // L: 213
			this.array[this.offset - var1 - 1] = (byte)var1; // L: 214
		} else {
			throw new IllegalArgumentException(); // L: 211
		}
	} // L: 215

	public void method6963(int var1) {
		if (var1 >= 0 && var1 <= 255) { // L: 218
			this.array[this.offset - var1 - 1] = (byte)var1; // L: 221
		} else {
			throw new IllegalArgumentException(); // L: 219
		}
	} // L: 222

	public void writeSmartByteShort(int var1) {
		if (var1 >= 0 && var1 < 128) { // L: 225
			this.writeByte(var1); // L: 226
		} else if (var1 >= 0 && var1 < 32768) { // L: 229
			this.writeShort(var1 + 32768); // L: 230
		} else {
			throw new IllegalArgumentException(); // L: 233
		}
	} // L: 227 231

	public void writeVarInt(int var1) {
		if ((var1 & -128) != 0) { // L: 237
			if ((var1 & -16384) != 0) { // L: 238
				if ((var1 & -2097152) != 0) { // L: 239
					if ((var1 & -268435456) != 0) { // L: 240
						this.writeByte(var1 >>> 28 | 128);
					}

					this.writeByte(var1 >>> 21 | 128); // L: 241
				}

				this.writeByte(var1 >>> 14 | 128); // L: 243
			}

			this.writeByte(var1 >>> 7 | 128); // L: 245
		}

		this.writeByte(var1 & 127); // L: 247
	} // L: 248

	public int readUnsignedByte() {
		return this.array[++this.offset - 1] & 255; // L: 251
	}

	public byte readByte() {
		return this.array[++this.offset - 1]; // L: 255
	}

	public int readUnsignedShort() {
		this.offset += 2; // L: 259
		return (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 260
	}

	public int readShort() {
		this.offset += 2; // L: 264
		int var1 = (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 265
		if (var1 > 32767) { // L: 266
			var1 -= 65536;
		}

		return var1; // L: 267
	}

	public int readMedium() {
		this.offset += 3; // L: 271
		return ((this.array[this.offset - 3] & 255) << 16) + (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8); // L: 272
	}

	public int readInt() {
		this.offset += 4; // L: 276
		return ((this.array[this.offset - 3] & 255) << 16) + (this.array[this.offset - 1] & 255) + ((this.array[this.offset - 2] & 255) << 8) + ((this.array[this.offset - 4] & 255) << 24); // L: 277
	}

	public long readLong() {
		long var1 = (long)this.readInt() & 4294967295L; // L: 281
		long var3 = (long)this.readInt() & 4294967295L; // L: 282
		return var3 + (var1 << 32); // L: 283
	}

	public boolean readBoolean() {
		return (this.readUnsignedByte() & 1) == 1; // L: 287
	}

	public String readCESU8() {
		byte var1 = this.array[++this.offset - 1]; // L: 317
		if (var1 != 0) { // L: 318
			throw new IllegalStateException("");
		} else {
			int var2 = this.readVarInt(); // L: 319
			if (var2 + this.offset > this.array.length) { // L: 320
				throw new IllegalStateException("");
			} else {
				byte[] var4 = this.array; // L: 322
				int var5 = this.offset; // L: 323
				char[] var6 = new char[var2]; // L: 325
				int var7 = 0; // L: 326
				int var8 = var5; // L: 327

				int var11;
				for (int var9 = var2 + var5; var8 < var9; var6[var7++] = (char)var11) { // L: 328 329 360
					int var10 = var4[var8++] & 255; // L: 330
					if (var10 < 128) { // L: 332
						if (var10 == 0) { // L: 333
							var11 = 65533;
						} else {
							var11 = var10; // L: 334
						}
					} else if (var10 < 192) { // L: 336
						var11 = 65533;
					} else if (var10 < 224) { // L: 337
						if (var8 < var9 && (var4[var8] & 192) == 128) { // L: 338
							var11 = (var10 & 31) << 6 | var4[var8++] & 63; // L: 339
							if (var11 < 128) { // L: 340
								var11 = 65533;
							}
						} else {
							var11 = 65533; // L: 342
						}
					} else if (var10 < 240) { // L: 344
						if (var8 + 1 < var9 && (var4[var8] & 192) == 128 && (var4[var8 + 1] & 192) == 128) { // L: 345
							var11 = (var10 & 15) << 12 | (var4[var8++] & 63) << 6 | var4[var8++] & 63; // L: 346
							if (var11 < 2048) { // L: 347
								var11 = 65533;
							}
						} else {
							var11 = 65533; // L: 349
						}
					} else if (var10 < 248) { // L: 351
						if (var8 + 2 < var9 && (var4[var8] & 192) == 128 && (var4[var8 + 1] & 192) == 128 && (var4[var8 + 2] & 192) == 128) { // L: 352
							var11 = (var10 & 7) << 18 | (var4[var8++] & 63) << 12 | (var4[var8++] & 63) << 6 | var4[var8++] & 63; // L: 353
							if (var11 >= 65536 && var11 <= 1114111) { // L: 354
								var11 = 65533; // L: 355
							} else {
								var11 = 65533;
							}
						} else {
							var11 = 65533; // L: 357
						}
					} else {
						var11 = 65533; // L: 359
					}
				}

				String var3 = new String(var6, 0, var7); // L: 362
				this.offset += var2; // L: 365
				return var3; // L: 366
			}
		}
	}

	public void readBytes(byte[] var1, int var2, int var3) {
		for (int var4 = var2; var4 < var3 + var2; ++var4) {
			var1[var4] = this.array[++this.offset - 1]; // L: 370
		}

	} // L: 371

	public int readShortSmart() {
		int var1 = this.array[this.offset] & 255; // L: 374
		return var1 < 128 ? this.readUnsignedByte() - 64 : this.readUnsignedShort() - 49152; // L: 375 376
	}

	public int readUShortSmart() {
		int var1 = this.array[this.offset] & 255; // L: 380
		return var1 < 128 ? this.readUnsignedByte() : this.readUnsignedShort() - 32768; // L: 381 382
	}

	public int method6981() {
		int var1 = 0; // L: 386

		int var2;
		for (var2 = this.readUShortSmart(); var2 == 32767; var2 = this.readUShortSmart()) { // L: 387 388 390
			var1 += 32767; // L: 389
		}

		var1 += var2; // L: 392
		return var1; // L: 393
	}

	public int method6982() {
		return this.array[this.offset] < 0 ? this.readInt() & Integer.MAX_VALUE : this.readUnsignedShort(); // L: 397 398
	}

	public int method6946() {
		if (this.array[this.offset] < 0) { // L: 402
			return this.readInt() & Integer.MAX_VALUE;
		} else {
			int var1 = this.readUnsignedShort(); // L: 403
			return var1 == 32767 ? -1 : var1; // L: 404
		}
	}

	public int readVarInt() {
		byte var1 = this.array[++this.offset - 1]; // L: 409

		int var2;
		for (var2 = 0; var1 < 0; var1 = this.array[++this.offset - 1]) { // L: 410 411 413
			var2 = (var2 | var1 & 127) << 7; // L: 412
		}

		return var2 | var1; // L: 415
	}

	public void xteaEncryptAll(int[] var1) {
		int var2 = this.offset / 8; // L: 419
		this.offset = 0; // L: 420

		for (int var3 = 0; var3 < var2; ++var3) { // L: 421
			int var4 = this.readInt(); // L: 422
			int var5 = this.readInt(); // L: 423
			int var6 = 0; // L: 424
			int var7 = -1640531527; // L: 425

			for (int var8 = 32; var8-- > 0; var5 += var4 + (var4 << 4 ^ var4 >>> 5) ^ var1[var6 >>> 11 & 3] + var6) { // L: 426 427 430
				var4 += var5 + (var5 << 4 ^ var5 >>> 5) ^ var6 + var1[var6 & 3]; // L: 428
				var6 += var7; // L: 429
			}

			this.offset -= 8; // L: 432
			this.writeInt(var4); // L: 433
			this.writeInt(var5); // L: 434
		}

	} // L: 436

	public void xteaDecryptAll(int[] var1) {
		int var2 = this.offset / 8; // L: 439
		this.offset = 0; // L: 440

		for (int var3 = 0; var3 < var2; ++var3) { // L: 441
			int var4 = this.readInt(); // L: 442
			int var5 = this.readInt(); // L: 443
			int var6 = -957401312; // L: 444
			int var7 = -1640531527; // L: 445

			for (int var8 = 32; var8-- > 0; var4 -= var5 + (var5 << 4 ^ var5 >>> 5) ^ var6 + var1[var6 & 3]) { // L: 446 447 450
				var5 -= var4 + (var4 << 4 ^ var4 >>> 5) ^ var1[var6 >>> 11 & 3] + var6; // L: 448
				var6 -= var7; // L: 449
			}

			this.offset -= 8; // L: 452
			this.writeInt(var4); // L: 453
			this.writeInt(var5); // L: 454
		}

	} // L: 456

	public void xteaEncrypt(int[] var1, int var2, int var3) {
		int var4 = this.offset; // L: 459
		this.offset = var2; // L: 460
		int var5 = (var3 - var2) / 8; // L: 461

		for (int var6 = 0; var6 < var5; ++var6) { // L: 462
			int var7 = this.readInt(); // L: 463
			int var8 = this.readInt(); // L: 464
			int var9 = 0; // L: 465
			int var10 = -1640531527; // L: 466

			for (int var11 = 32; var11-- > 0; var8 += var7 + (var7 << 4 ^ var7 >>> 5) ^ var1[var9 >>> 11 & 3] + var9) { // L: 467 468 471
				var7 += var8 + (var8 << 4 ^ var8 >>> 5) ^ var9 + var1[var9 & 3]; // L: 469
				var9 += var10; // L: 470
			}

			this.offset -= 8; // L: 473
			this.writeInt(var7); // L: 474
			this.writeInt(var8); // L: 475
		}

		this.offset = var4; // L: 477
	} // L: 478

	public void xteaDecrypt(int[] var1, int var2, int var3) {
		int var4 = this.offset; // L: 481
		this.offset = var2; // L: 482
		int var5 = (var3 - var2) / 8; // L: 483

		for (int var6 = 0; var6 < var5; ++var6) { // L: 484
			int var7 = this.readInt(); // L: 485
			int var8 = this.readInt(); // L: 486
			int var9 = -957401312; // L: 487
			int var10 = -1640531527; // L: 488

			for (int var11 = 32; var11-- > 0; var7 -= var8 + (var8 << 4 ^ var8 >>> 5) ^ var9 + var1[var9 & 3]) { // L: 489 490 493
				var8 -= var7 + (var7 << 4 ^ var7 >>> 5) ^ var1[var9 >>> 11 & 3] + var9; // L: 491
				var9 -= var10; // L: 492
			}

			this.offset -= 8; // L: 495
			this.writeInt(var7); // L: 496
			this.writeInt(var8); // L: 497
		}

		this.offset = var4; // L: 499
	} // L: 500

	public void encryptRsa(BigInteger var1, BigInteger var2) {
		int var3 = this.offset; // L: 503
		this.offset = 0; // L: 504
		byte[] var4 = new byte[var3]; // L: 505
		this.readBytes(var4, 0, var3); // L: 506
		BigInteger var5 = new BigInteger(var4); // L: 507
		BigInteger var6 = var5.modPow(var1, var2); // L: 508
		byte[] var7 = var6.toByteArray(); // L: 509
		this.offset = 0; // L: 510
		this.writeShort(var7.length); // L: 511
		this.writeBytes(var7, 0, var7.length); // L: 512
	} // L: 513

	public String readStringCp1252NullTerminated() {
		int var1 = this.offset; // L: 299

		while (this.array[++this.offset - 1] != 0) { // L: 300
		}

		int var2 = this.offset - var1 - 1; // L: 301
		return var2 == 0 ? "" : decodeStringCp1252(this.array, var1, var2); // L: 302 303
	}

	public static String decodeStringCp1252(byte[] var0, int var1, int var2) {
		char[] var3 = new char[var2]; // L: 111
		int var4 = 0; // L: 112

		for (int var5 = 0; var5 < var2; ++var5) { // L: 113
			int var6 = var0[var5 + var1] & 255; // L: 114
			if (var6 != 0) { // L: 115
				if (var6 >= 128 && var6 < 160) { // L: 116
					char var7 = cp1252AsciiExtension[var6 - 128]; // L: 117
					if (var7 == 0) { // L: 118
						var7 = '?';
					}

					var6 = var7; // L: 119
				}

				var3[var4++] = (char)var6; // L: 121
			}
		}

		return new String(var3, 0, var4); // L: 123
	}

	public void method7171(int var1) {
		this.array[++this.offset - 1] = (byte)(var1 + 128); // L: 530
	} // L: 531

	public void method6993(int var1) {
		this.array[++this.offset - 1] = (byte)(0 - var1); // L: 534
	} // L: 535

	public void method6947(int var1) {
		this.array[++this.offset - 1] = (byte)(128 - var1); // L: 538
	} // L: 539

	public int method6995() {
		return this.array[++this.offset - 1] - 128 & 255; // L: 542
	}

	public int method6996() {
		return 0 - this.array[++this.offset - 1] & 255; // L: 546
	}

	public int method6997() {
		return 128 - this.array[++this.offset - 1] & 255; // L: 550
	}

	public byte method6973() {
		return (byte)(this.array[++this.offset - 1] - 128); // L: 554
	}

	public byte method6999() {
		return (byte)(0 - this.array[++this.offset - 1]); // L: 558
	}

	public byte method7128() {
		return (byte)(128 - this.array[++this.offset - 1]); // L: 562
	}

}
