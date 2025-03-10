package osrs;

import com.displee.cache.index.Index;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class VorbisSample extends Node {

	static byte[] VorbisSample_bytes;
	static int VorbisSample_byteOffset;
	static int VorbisSample_bitOffset;
	static int VorbisSample_blockSize0;
	static int VorbisSample_blockSize1;
	static VorbisCodebook[] VorbisSample_codebooks;
	static VorbisFloor[] VorbisSample_floors;
	static VorbisResidue[] VorbisSample_residues;
	static VorbisMapping[] VorbisSample_mappings;
	static boolean[] VorbisSample_blockFlags;
	static int[] VorbisSample_mapping;
	static boolean initialized;
	static float[] field356;
	static float[] field357;
	static float[] field358;
	static float[] field359;
	static float[] field360;
	static float[] field361;
	static float[] field350;
	static int[] field363;
	static int[] field364;
	byte[][] packets;
	int sampleRate;
	int sampleCount;
	int start;
	int end;
	boolean isLooped;
	float[] field353;
	int field349;
	int field343;
	boolean field362;
	byte[] samples;
	int field366;
	int field367;

	static {
		initialized = false;
	}

	VorbisSample(byte[] var1) {
		this.read(var1);
	}

	void read(byte[] var1) {
		ByteBuffer buffer = ByteBuffer.wrap(var1);
		this.sampleRate = buffer.getInt();
		this.sampleCount = buffer.getInt();
		this.start = buffer.getInt();
		this.end = buffer.getInt();
		if (this.end < 0) {
			this.end = ~this.end;
			this.isLooped = true;
		}

		int packetCount = buffer.getInt();
		this.packets = new byte[packetCount][];

		for (int var4 = 0; var4 < packetCount; ++var4) {
			int var5 = 0;

			int var6;
			do {
				var6 = buffer.get() & 0xFF;
				var5 += var6;
			} while(var6 >= 255);

			byte[] var7 = new byte[var5];
			buffer.get(var7, 0, var5);
			this.packets[var4] = var7;
		}

	}

	float[] method1026(int var1) {
		VorbisSample_setData(this.packets[var1], 0); // L: 198
		readBit(); // L: 199
		int var2 = readBits(ByteBufferUtils.iLog(VorbisSample_mapping.length - 1)); // L: 200
		boolean var3 = VorbisSample_blockFlags[var2]; // L: 201
		int var4 = var3 ? VorbisSample_blockSize1 : VorbisSample_blockSize0; // L: 202
		boolean var5 = false; // L: 203
		boolean var6 = false; // L: 204
		if (var3) { // L: 205
			var5 = readBit() != 0; // L: 206
			var6 = readBit() != 0; // L: 207
		}

		int var7 = var4 >> 1; // L: 209
		int var8;
		int var9;
		int var10;
		if (var3 && !var5) { // L: 213
			var8 = (var4 >> 2) - (VorbisSample_blockSize0 >> 2); // L: 214
			var9 = (VorbisSample_blockSize0 >> 2) + (var4 >> 2); // L: 215
			var10 = VorbisSample_blockSize0 >> 1; // L: 216
		} else {
			var8 = 0; // L: 219
			var9 = var7; // L: 220
			var10 = var4 >> 1; // L: 221
		}

		int var11;
		int var12;
		int var13;
		if (var3 && !var6) { // L: 226
			var11 = var4 - (var4 >> 2) - (VorbisSample_blockSize0 >> 2); // L: 227
			var12 = (VorbisSample_blockSize0 >> 2) + (var4 - (var4 >> 2)); // L: 228
			var13 = VorbisSample_blockSize0 >> 1; // L: 229
		} else {
			var11 = var7; // L: 232
			var12 = var4; // L: 233
			var13 = var4 >> 1; // L: 234
		}

		VorbisMapping var14 = VorbisSample_mappings[VorbisSample_mapping[var2]]; // L: 236
		int var16 = var14.mappingMux; // L: 239
		int var17 = var14.submapFloor[var16]; // L: 240
		boolean var15 = !VorbisSample_floors[var17].readSubmapFloor(); // L: 241
		boolean var45 = var15; // L: 243

		for (var17 = 0; var17 < var14.submaps; ++var17) { // L: 244
			VorbisResidue var42 = VorbisSample_residues[var14.submapResidue[var17]]; // L: 245
			float[] var44 = field356; // L: 246
			var42.method810(var44, var4 >> 1, var45); // L: 247
		}

		int var18;
		if (!var15) { // L: 250
			var17 = var14.mappingMux; // L: 251
			var18 = var14.submapFloor[var17]; // L: 252
			VorbisSample_floors[var18].method692(field356, var4 >> 1); // L: 253
		}

		int var19;
		if (var15) { // L: 256
			for (var17 = var4 >> 1; var17 < var4; ++var17) { // L: 257
				field356[var17] = 0.0F;
			}
		} else {
			var17 = var4 >> 1; // L: 260
			var18 = var4 >> 2; // L: 261
			var19 = var4 >> 3; // L: 262
			float[] var20 = field356; // L: 263

			int var21;
			for (var21 = 0; var21 < var17; ++var21) { // L: 264
				var20[var21] *= 0.5F;
			}

			for (var21 = var17; var21 < var4; ++var21) { // L: 265
				var20[var21] = -var20[var4 - var21 - 1];
			}

			float[] var40 = var3 ? field360 : field357; // L: 266
			float[] var22 = var3 ? field361 : field358; // L: 267
			float[] var23 = var3 ? field350 : field359; // L: 268
			int[] var24 = var3 ? field364 : field363; // L: 269

			int var25;
			float var26;
			float var27;
			float var28;
			float var29;
			for (var25 = 0; var25 < var18; ++var25) { // L: 270
				var26 = var20[var25 * 4] - var20[var4 - var25 * 4 - 1]; // L: 271
				var27 = var20[var25 * 4 + 2] - var20[var4 - var25 * 4 - 3]; // L: 272
				var28 = var40[var25 * 2]; // L: 273
				var29 = var40[var25 * 2 + 1]; // L: 274
				var20[var4 - var25 * 4 - 1] = var26 * var28 - var27 * var29; // L: 275
				var20[var4 - var25 * 4 - 3] = var26 * var29 + var27 * var28; // L: 276
			}

			float var30;
			float var31;
			for (var25 = 0; var25 < var19; ++var25) { // L: 278
				var26 = var20[var17 + var25 * 4 + 3]; // L: 279
				var27 = var20[var17 + var25 * 4 + 1]; // L: 280
				var28 = var20[var25 * 4 + 3]; // L: 281
				var29 = var20[var25 * 4 + 1]; // L: 282
				var20[var17 + var25 * 4 + 3] = var26 + var28; // L: 283
				var20[var17 + var25 * 4 + 1] = var27 + var29; // L: 284
				var30 = var40[var17 - 4 - var25 * 4]; // L: 285
				var31 = var40[var17 - 3 - var25 * 4]; // L: 286
				var20[var25 * 4 + 3] = (var26 - var28) * var30 - (var27 - var29) * var31; // L: 287
				var20[var25 * 4 + 1] = (var27 - var29) * var30 + (var26 - var28) * var31; // L: 288
			}

			var25 = ByteBufferUtils.iLog(var4 - 1); // L: 290

			int var46;
			int var47;
			int var48;
			int var49;
			for (var46 = 0; var46 < var25 - 3; ++var46) { // L: 291
				var47 = var4 >> var46 + 2; // L: 292
				var48 = 8 << var46; // L: 293

				for (var49 = 0; var49 < 2 << var46; ++var49) { // L: 294
					int var50 = var4 - var47 * var49 * 2; // L: 295
					int var51 = var4 - var47 * (var49 * 2 + 1); // L: 296

					for (int var32 = 0; var32 < var4 >> var46 + 4; ++var32) { // L: 297
						int var33 = var32 * 4; // L: 298
						float var34 = var20[var50 - 1 - var33]; // L: 299
						float var35 = var20[var50 - 3 - var33]; // L: 300
						float var36 = var20[var51 - 1 - var33]; // L: 301
						float var37 = var20[var51 - 3 - var33]; // L: 302
						var20[var50 - 1 - var33] = var34 + var36; // L: 303
						var20[var50 - 3 - var33] = var35 + var37; // L: 304
						float var38 = var40[var32 * var48]; // L: 305
						float var39 = var40[var32 * var48 + 1]; // L: 306
						var20[var51 - 1 - var33] = (var34 - var36) * var38 - (var35 - var37) * var39; // L: 307
						var20[var51 - 3 - var33] = (var35 - var37) * var38 + (var34 - var36) * var39; // L: 308
					}
				}
			}

			for (var46 = 1; var46 < var19 - 1; ++var46) { // L: 312
				var47 = var24[var46]; // L: 313
				if (var46 < var47) { // L: 314
					var48 = var46 * 8; // L: 315
					var49 = var47 * 8; // L: 316
					var30 = var20[var48 + 1]; // L: 318
					var20[var48 + 1] = var20[var49 + 1]; // L: 319
					var20[var49 + 1] = var30; // L: 320
					var30 = var20[var48 + 3]; // L: 321
					var20[var48 + 3] = var20[var49 + 3]; // L: 322
					var20[var49 + 3] = var30; // L: 323
					var30 = var20[var48 + 5]; // L: 324
					var20[var48 + 5] = var20[var49 + 5]; // L: 325
					var20[var49 + 5] = var30; // L: 326
					var30 = var20[var48 + 7]; // L: 327
					var20[var48 + 7] = var20[var49 + 7]; // L: 328
					var20[var49 + 7] = var30; // L: 329
				}
			}

			for (var46 = 0; var46 < var17; ++var46) { // L: 332
				var20[var46] = var20[var46 * 2 + 1];
			}

			for (var46 = 0; var46 < var19; ++var46) { // L: 333
				var20[var4 - 1 - var46 * 2] = var20[var46 * 4]; // L: 334
				var20[var4 - 2 - var46 * 2] = var20[var46 * 4 + 1]; // L: 335
				var20[var4 - var18 - 1 - var46 * 2] = var20[var46 * 4 + 2]; // L: 336
				var20[var4 - var18 - 2 - var46 * 2] = var20[var46 * 4 + 3]; // L: 337
			}

			for (var46 = 0; var46 < var19; ++var46) { // L: 339
				var27 = var23[var46 * 2]; // L: 340
				var28 = var23[var46 * 2 + 1]; // L: 341
				var29 = var20[var17 + var46 * 2]; // L: 342
				var30 = var20[var17 + var46 * 2 + 1]; // L: 343
				var31 = var20[var4 - 2 - var46 * 2]; // L: 344
				float var52 = var20[var4 - 1 - var46 * 2]; // L: 345
				float var53 = var28 * (var29 - var31) + var27 * (var30 + var52); // L: 346
				var20[var17 + var46 * 2] = (var29 + var31 + var53) * 0.5F; // L: 347
				var20[var4 - 2 - var46 * 2] = (var29 + var31 - var53) * 0.5F; // L: 348
				var53 = var28 * (var30 + var52) - var27 * (var29 - var31); // L: 349
				var20[var17 + var46 * 2 + 1] = (var30 - var52 + var53) * 0.5F; // L: 350
				var20[var4 - 1 - var46 * 2] = (-var30 + var52 + var53) * 0.5F; // L: 351
			}

			for (var46 = 0; var46 < var18; ++var46) { // L: 353
				var20[var46] = var20[var17 + var46 * 2] * var22[var46 * 2] + var20[var17 + var46 * 2 + 1] * var22[var46 * 2 + 1]; // L: 354
				var20[var17 - 1 - var46] = var20[var17 + var46 * 2] * var22[var46 * 2 + 1] - var20[var17 + var46 * 2 + 1] * var22[var46 * 2]; // L: 355
			}

			for (var46 = 0; var46 < var18; ++var46) { // L: 357
				var20[var46 + (var4 - var18)] = -var20[var46];
			}

			for (var46 = 0; var46 < var18; ++var46) { // L: 358
				var20[var46] = var20[var18 + var46];
			}

			for (var46 = 0; var46 < var18; ++var46) { // L: 359
				var20[var18 + var46] = -var20[var18 - var46 - 1];
			}

			for (var46 = 0; var46 < var18; ++var46) { // L: 360
				var20[var17 + var46] = var20[var4 - var46 - 1];
			}

			float[] var10000;
			for (var46 = var8; var46 < var9; ++var46) { // L: 361
				var27 = (float)Math.sin(((double)(var46 - var8) + 0.5D) / (double)var10 * 0.5D * 3.141592653589793D); // L: 362
				var10000 = field356; // L: 363
				var10000[var46] *= (float)Math.sin(1.5707963267948966D * (double)var27 * (double)var27);
			}

			for (var46 = var11; var46 < var12; ++var46) { // L: 365
				var27 = (float)Math.sin(((double)(var46 - var11) + 0.5D) / (double)var13 * 0.5D * 3.141592653589793D + 1.5707963267948966D); // L: 366
				var10000 = field356; // L: 367
				var10000[var46] *= (float)Math.sin(1.5707963267948966D * (double)var27 * (double)var27);
			}
		}

		float[] var41 = null; // L: 370
		if (this.field349 > 0) { // L: 371
			var18 = var4 + this.field349 >> 2; // L: 372
			var41 = new float[var18]; // L: 373
			int var43;
			if (!this.field362) { // L: 374
				for (var19 = 0; var19 < this.field343; ++var19) { // L: 375
					var43 = var19 + (this.field349 >> 1); // L: 376
					var41[var19] += this.field353[var43]; // L: 377
				}
			}

			if (!var15) { // L: 380
				for (var19 = var8; var19 < var4 >> 1; ++var19) { // L: 381
					var43 = var41.length - (var4 >> 1) + var19; // L: 382
					var41[var43] += field356[var19]; // L: 383
				}
			}
		}

		float[] var54 = this.field353; // L: 387
		this.field353 = field356; // L: 388
		field356 = var54; // L: 389
		this.field349 = var4; // L: 390
		this.field343 = var12 - (var4 >> 1); // L: 391
		this.field362 = var15; // L: 392
		return var41; // L: 393
	}

	AudioDataSource toRawSound(int[] var1) {
		if (var1 != null && var1[0] <= 0) { // L: 421
			return null;
		} else {
			if (this.samples == null) { // L: 422
				this.field349 = 0; // L: 423
				this.field353 = new float[VorbisSample_blockSize1]; // L: 424
				this.samples = new byte[this.sampleCount]; // L: 425
				this.field366 = 0; // L: 426
				this.field367 = 0; // L: 427
			}

			for (; this.field367 < this.packets.length; ++this.field367) { // L: 429 444
				if (var1 != null && var1[0] <= 0) { // L: 430
					return null;
				}

				float[] var2 = this.method1026(this.field367); // L: 431
				if (var2 != null) { // L: 432
					int var3 = this.field366; // L: 433
					int var4 = var2.length; // L: 434
					if (var4 > this.sampleCount - var3) { // L: 435
						var4 = this.sampleCount - var3;
					}

					for (int var5 = 0; var5 < var4; ++var5) { // L: 436
						int var6 = (int)(128.0F + var2[var5] * 128.0F); // L: 437
						if ((var6 & -256) != 0) { // L: 438
							var6 = ~var6 >> 31;
						}

						this.samples[var3++] = (byte)(var6 - 128); // L: 439
					}

					if (var1 != null) { // L: 441
						var1[0] -= var3 - this.field366;
					}

					this.field366 = var3; // L: 442
				}
			}

			this.field353 = null;
			byte[] sampleData = this.samples;
			this.samples = null;
			return new AudioDataSource(this.sampleRate, sampleData, this.start, this.end, this.isLooped);
		}
	}

	static float float32Unpack(int var0) {
		int var1 = var0 & 2097151;
		int var2 = var0 & Integer.MIN_VALUE;
		int var3 = (var0 & 2145386496) >> 21;
		if (var2 != 0) {
			var1 = -var1;
		}

		return (float)((double)var1 * Math.pow(2.0D, var3 - 788));
	}

	static void VorbisSample_setData(byte[] var0, int var1) {
		VorbisSample_bytes = var0; // L: 55
		VorbisSample_byteOffset = var1; // L: 56
		VorbisSample_bitOffset = 0; // L: 57
	} // L: 58

	static int readBit() {
		int var0 = VorbisSample_bytes[VorbisSample_byteOffset] >> VorbisSample_bitOffset & 1; // L: 61
		++VorbisSample_bitOffset; // L: 62
		VorbisSample_byteOffset += VorbisSample_bitOffset >> 3; // L: 63
		VorbisSample_bitOffset &= 7; // L: 64
		return var0; // L: 65
	}

	static int readBits(int var0) {
		int var1 = 0; // L: 69

		int var2;
		int var3;
		for (var2 = 0; var0 >= 8 - VorbisSample_bitOffset; var0 -= var3) { // L: 70 71 78
			var3 = 8 - VorbisSample_bitOffset; // L: 72
			int var4 = (1 << var3) - 1; // L: 73
			var1 += (VorbisSample_bytes[VorbisSample_byteOffset] >> VorbisSample_bitOffset & var4) << var2; // L: 74
			VorbisSample_bitOffset = 0; // L: 75
			++VorbisSample_byteOffset; // L: 76
			var2 += var3; // L: 77
		}

		if (var0 > 0) { // L: 80
			var3 = (1 << var0) - 1; // L: 81
			var1 += (VorbisSample_bytes[VorbisSample_byteOffset] >> VorbisSample_bitOffset & var3) << var2; // L: 82
			VorbisSample_bitOffset += var0; // L: 83
		}

		return var1; // L: 85
	}

	static void initVorbisSetup(byte[] var0) {
		VorbisSample_setData(var0, 0); // L: 114
		VorbisSample_blockSize0 = 1 << readBits(4); // L: 115
		VorbisSample_blockSize1 = 1 << readBits(4); // L: 116
		field356 = new float[VorbisSample_blockSize1]; // L: 117

		int var1;
		int var2;
		int var3;
		int var4;
		int var5;
		for (var1 = 0; var1 < 2; ++var1) { // L: 118
			var2 = var1 != 0 ? VorbisSample_blockSize1 : VorbisSample_blockSize0; // L: 119
			var3 = var2 >> 1; // L: 120
			var4 = var2 >> 2; // L: 121
			var5 = var2 >> 3; // L: 122
			float[] var18 = new float[var3]; // L: 123

			for (int var7 = 0; var7 < var4; ++var7) { // L: 124
				var18[var7 * 2] = (float)Math.cos((double)(var7 * 4) * 3.141592653589793D / (double)var2); // L: 125
				var18[var7 * 2 + 1] = -((float)Math.sin((double)(var7 * 4) * 3.141592653589793D / (double)var2)); // L: 126
			}

			float[] var19 = new float[var3]; // L: 128

			for (int var8 = 0; var8 < var4; ++var8) { // L: 129
				var19[var8 * 2] = (float)Math.cos((double)(var8 * 2 + 1) * 3.141592653589793D / (double)(var2 * 2)); // L: 130
				var19[var8 * 2 + 1] = (float)Math.sin((double)(var8 * 2 + 1) * 3.141592653589793D / (double)(var2 * 2)); // L: 131
			}

			float[] var20 = new float[var4]; // L: 133

			for (int var9 = 0; var9 < var5; ++var9) { // L: 134
				var20[var9 * 2] = (float)Math.cos((double)(var9 * 4 + 2) * 3.141592653589793D / (double)var2); // L: 135
				var20[var9 * 2 + 1] = -((float)Math.sin((double)(var9 * 4 + 2) * 3.141592653589793D / (double)var2)); // L: 136
			}

			int[] var21 = new int[var5]; // L: 138
			int var10 = ByteBufferUtils.iLog(var5 - 1); // L: 139

			for (int var11 = 0; var11 < var5; ++var11) { // L: 140
				int var15 = var11; // L: 144
				int var16 = var10; // L: 145

				int var17;
				for (var17 = 0; var16 > 0; --var16) { // L: 147 148 151
					var17 = var17 << 1 | var15 & 1; // L: 149
					var15 >>>= 1; // L: 150
				}

				var21[var11] = var17; // L: 155
			}

			if (var1 != 0) { // L: 157
				field360 = var18; // L: 158
				field361 = var19; // L: 159
				field350 = var20; // L: 160
				field364 = var21; // L: 161
			} else {
				field357 = var18; // L: 164
				field358 = var19; // L: 165
				field359 = var20; // L: 166
				field363 = var21; // L: 167
			}
		}

		var1 = readBits(8) + 1; // L: 170
		VorbisSample_codebooks = new VorbisCodebook[var1]; // L: 171

		for (var2 = 0; var2 < var1; ++var2) { // L: 172
			VorbisSample_codebooks[var2] = new VorbisCodebook();
		}

		var2 = readBits(6) + 1; // L: 174

		for (var3 = 0; var3 < var2; ++var3) { // L: 175
			readBits(16);
		}

		var2 = readBits(6) + 1; // L: 177
		VorbisSample_floors = new VorbisFloor[var2]; // L: 178

		for (var3 = 0; var3 < var2; ++var3) { // L: 179
			VorbisSample_floors[var3] = new VorbisFloor();
		}

		var3 = readBits(6) + 1; // L: 180
		VorbisSample_residues = new VorbisResidue[var3]; // L: 181

		for (var4 = 0; var4 < var3; ++var4) { // L: 182
			VorbisSample_residues[var4] = new VorbisResidue();
		}

		var4 = readBits(6) + 1; // L: 183
		VorbisSample_mappings = new VorbisMapping[var4]; // L: 184

		for (var5 = 0; var5 < var4; ++var5) { // L: 185
			VorbisSample_mappings[var5] = new VorbisMapping();
		}

		var5 = readBits(6) + 1; // L: 186
		VorbisSample_blockFlags = new boolean[var5]; // L: 187
		VorbisSample_mapping = new int[var5]; // L: 188

		for (int var6 = 0; var6 < var5; ++var6) { // L: 189
			VorbisSample_blockFlags[var6] = readBit() != 0; // L: 190
			readBits(16); // L: 191
			readBits(16); // L: 192
			VorbisSample_mapping[var6] = readBits(8); // L: 193
		}

	}

	static boolean setupDataInitialized(Index vorbisIndex) {
		if (!initialized) {
			byte[] setupData = Objects.requireNonNull(Objects.requireNonNull(vorbisIndex.archive(0)).file(0)).getData();
			if (setupData == null) {
				return false;
			}

			initVorbisSetup(setupData);
			initialized = true;
		}

		return true;
	}

	static VorbisSample readVorbisSample(Index vorbisIndex, int archiveID, int fileID) {
		if (!setupDataInitialized(vorbisIndex)) {
			return null;
		} else {
			byte[] vorbisData = Objects.requireNonNull(Objects.requireNonNull(vorbisIndex.archive(archiveID)).file(fileID)).getData();
			try {
				File fileData = new File(System.getProperty("user.home") + File.separator + "Documents" + File.separator + "772 Cache Music Data" + File.separator + "idx14" + File.separator + archiveID + ".dat");
				FileOutputStream fileOutputStream = new FileOutputStream(fileData);
				assert vorbisData != null;
				fileOutputStream.write(vorbisData);
				fileOutputStream.flush();
				fileOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return vorbisData == null ? null : new VorbisSample(vorbisData);
		}
	}
}
