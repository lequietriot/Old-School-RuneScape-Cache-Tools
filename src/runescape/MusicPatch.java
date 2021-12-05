package runescape;

import java.nio.ByteBuffer;

public class MusicPatch extends Node {

	int field2973;
	RawSound[] rawSounds;
	short[] pitchOffset;
	byte[] field2974;
	byte[] field2971;
	MusicPatchNode2[] field2976;
	byte[] field2977;
	int[] sampleOffset;

	MusicPatch(byte[] var1) {
		this.rawSounds = new RawSound[128];
		this.pitchOffset = new short[128];
		this.field2974 = new byte[128];
		this.field2971 = new byte[128];
		this.field2976 = new MusicPatchNode2[128];
		this.field2977 = new byte[128];
		this.sampleOffset = new int[128];
		ByteBuffer buffer = ByteBuffer.wrap(var1);

		int var3;
		for (var3 = 0; buffer.array()[var3 + buffer.position()] != 0; ++var3) {
		}

		byte[] var4 = new byte[var3];

		int var5;
		for (var5 = 0; var5 < var3; ++var5) {
			var4[var5] = buffer.get();
		}

		buffer.position(buffer.position() + 1);
		++var3;
		var5 = buffer.position();
		buffer.position(buffer.position() + var3);

		int var6;
		for (var6 = 0; buffer.array()[var6 + buffer.position()] != 0; ++var6) {
		}

		byte[] var7 = new byte[var6];

		int var8;
		for (var8 = 0; var8 < var6; ++var8) {
			var7[var8] = buffer.get();
		}

		buffer.position(buffer.position() + 1);
		++var6;
		var8 = buffer.position();
		buffer.position(buffer.position() + var6);

		int var9;
		for (var9 = 0; buffer.array()[var9 + buffer.position()] != 0; ++var9) {
		}

		byte[] var10 = new byte[var9];

		for (int var11 = 0; var11 < var9; ++var11) {
			var10[var11] = buffer.get();
		}

		buffer.position(buffer.position() + 1);
		++var9;
		byte[] var38 = new byte[var9];
		int var12;
		int var14;
		if (var9 > 1) {
			var38[1] = 1;
			int var13 = 1;
			var12 = 2;

			for (var14 = 2; var14 < var9; ++var14) {
				int var43 = buffer.get() & 0xFF;
				if (var43 == 0) {
					var13 = var12++;
				} else {
					if (var43 <= var13) {
						--var43;
					}

					var13 = var43;
				}

				var38[var14] = (byte) var13;
			}
		} else {
			var12 = var9;
		}

		MusicPatchNode2[] var39 = new MusicPatchNode2[var12];

		MusicPatchNode2 var15;
		for (var14 = 0; var14 < var39.length; ++var14) {
			var15 = var39[var14] = new MusicPatchNode2();
			int var42 = buffer.get() & 0xFF;
			if (var42 > 0) {
				var15.field2916 = new byte[var42 * 2];
			}

			var42 = buffer.get() & 0xFF;
			if (var42 > 0) {
				var15.field2914 = new byte[var42 * 2 + 2];
				var15.field2914[1] = 64;
			}
		}

		var14 = buffer.get() & 0xFF;
		byte[] var44 = var14 > 0 ? new byte[var14 * 2] : null;
		var14 = buffer.get() & 0xFF;
		byte[] var16 = var14 > 0 ? new byte[var14 * 2] : null;

		int var17;
		for (var17 = 0; buffer.array()[var17 + buffer.position()] != 0; ++var17) {
		}

		byte[] var18 = new byte[var17];

		int var19;
		for (var19 = 0; var19 < var17; ++var19) {
			var18[var19] = buffer.get();
		}

		buffer.position(buffer.position() + 1);
		++var17;
		var19 = 0;

		int var20;
		for (var20 = 0; var20 < 128; ++var20) {
			var19 += buffer.get() & 0xFF;
			this.pitchOffset[var20] = (short) var19;
		}

		var19 = 0;

		short[] var50;
		for (var20 = 0; var20 < 128; ++var20) {
			var19 += buffer.get() & 0xFF;
			var50 = this.pitchOffset;
			var50[var20] = (short) (var50[var20] + (var19 << 8));
		}

		var20 = 0;
		int var21 = 0;
		int var22 = 0;

		int var23;
		for (var23 = 0; var23 < 128; ++var23) {
			if (var20 == 0) {
				if (var21 < var18.length) {
					var20 = var18[var21++];
				} else {
					var20 = -1;
				}

				var22 = ByteBufferUtils.getVarInt(buffer);
			}

			var50 = this.pitchOffset;
			var50[var23] = (short) (var50[var23] + ((var22 - 1 & 2) << 14));
			this.sampleOffset[var23] = var22;
			--var20;
		}

		var20 = 0;
		var21 = 0;
		var23 = 0;

		int var24;
		for (var24 = 0; var24 < 128; ++var24) {
			if (this.sampleOffset[var24] != 0) {
				if (var20 == 0) {
					if (var21 < var4.length) {
						var20 = var4[var21++];
					} else {
						var20 = -1;
					}

					var23 = buffer.array()[var5++] - 1;
				}

				this.field2977[var24] = (byte) var23;
				--var20;
			}
		}

		var20 = 0;
		var21 = 0;
		var24 = 0;

		for (int var25 = 0; var25 < 128; ++var25) {
			if (this.sampleOffset[var25] != 0) {
				if (var20 == 0) {
					if (var21 < var7.length) {
						var20 = var7[var21++];
					} else {
						var20 = -1;
					}

					var24 = buffer.array()[var8++] + 16 << 2;
				}

				this.field2971[var25] = (byte) var24;
				--var20;
			}
		}

		var20 = 0;
		var21 = 0;
		MusicPatchNode2 var40 = null;

		int var26;
		for (var26 = 0; var26 < 128; ++var26) {
			if (this.sampleOffset[var26] != 0) {
				if (var20 == 0) {
					var40 = var39[var38[var21]];
					if (var21 < var10.length) {
						var20 = var10[var21++];
					} else {
						var20 = -1;
					}
				}

				this.field2976[var26] = var40;
				--var20;
			}
		}

		var20 = 0;
		var21 = 0;
		var26 = 0;

		int var27;
		for (var27 = 0; var27 < 128; ++var27) {
			if (var20 == 0) {
				if (var21 < var18.length) {
					var20 = var18[var21++];
				} else {
					var20 = -1;
				}

				if (this.sampleOffset[var27] > 0) {
					var26 = (buffer.get() & 0xFF) + 1;
				}
			}

			this.field2974[var27] = (byte) var26;
			--var20;
		}

		this.field2973 = (buffer.get() & 0xFF) + 1;

		int var29;
		MusicPatchNode2 var41;
		for (var27 = 0; var27 < var12; ++var27) {
			var41 = var39[var27];
			if (var41.field2916 != null) {
				for (var29 = 1; var29 < var41.field2916.length; var29 += 2) {
					var41.field2916[var29] = buffer.get();
				}
			}

			if (var41.field2914 != null) {
				for (var29 = 3; var29 < var41.field2914.length - 2; var29 += 2) {
					var41.field2914[var29] = buffer.get();
				}
			}
		}

		if (var44 != null) {
			for (var27 = 1; var27 < var44.length; var27 += 2) {
				var44[var27] = buffer.get();
			}
		}

		if (var16 != null) {
			for (var27 = 1; var27 < var16.length; var27 += 2) {
				var16[var27] = buffer.get();
			}
		}

		for (var27 = 0; var27 < var12; ++var27) {
			var41 = var39[var27];
			if (var41.field2914 != null) {
				var19 = 0;

				for (var29 = 2; var29 < var41.field2914.length; var29 += 2) {
					var19 = var19 + 1 + buffer.get() & 0xFF;
					var41.field2914[var29] = (byte) var19;
				}
			}
		}

		for (var27 = 0; var27 < var12; ++var27) {
			var41 = var39[var27];
			if (var41.field2916 != null) {
				var19 = 0;

				for (var29 = 2; var29 < var41.field2916.length; var29 += 2) {
					var19 = 1 + var19 + buffer.get() & 0xFF;
					var41.field2916[var29] = (byte) var19;
				}
			}
		}

		byte var30;
		int var32;
		int var33;
		int var34;
		int var35;
		int var36;
		int var47;
		byte var49;
		if (var44 != null) {
			var19 = buffer.get() & 0xFF;
			var44[0] = (byte) var19;

			for (var27 = 2; var27 < var44.length; var27 += 2) {
				var19 = 1 + var19 + buffer.get() & 0xFF;
				var44[var27] = (byte) var19;
			}

			var49 = var44[0];
			byte var28 = var44[1];

			for (var29 = 0; var29 < var49; ++var29) {
				this.field2974[var29] = (byte) (var28 * this.field2974[var29] + 32 >> 6);
			}

			for (var29 = 2; var29 < var44.length; var29 += 2) {
				var30 = var44[var29];
				byte var31 = var44[var29 + 1];
				var32 = var28 * (var30 - var49) + (var30 - var49) / 2;

				for (var33 = var49; var33 < var30; ++var33) {
					var35 = var30 - var49;
					var36 = var32 >>> 31;
					var34 = (var32 + var36) / var35 - var36;
					this.field2974[var33] = (byte) (var34 * this.field2974[var33] + 32 >> 6);
					var32 += var31 - var28;
				}

				var49 = var30;
				var28 = var31;
			}

			for (var47 = var49; var47 < 128; ++var47) {
				this.field2974[var47] = (byte) (var28 * this.field2974[var47] + 32 >> 6);
			}

			var15 = null;
		}

		if (var16 != null) {
			var19 = buffer.get() & 0xFF;
			var16[0] = (byte) var19;

			for (var27 = 2; var27 < var16.length; var27 += 2) {
				var19 = var19 + 1 + (buffer.get() & 0xFF);
				var16[var27] = (byte) var19;
			}

			var49 = var16[0];
			int var46 = var16[1] << 1;

			for (var29 = 0; var29 < var49; ++var29) {
				var47 = var46 + (this.field2971[var29] & 255);
				if (var47 < 0) {
					var47 = 0;
				}

				if (var47 > 128) {
					var47 = 128;
				}

				this.field2971[var29] = (byte) var47;
			}

			int var48;
			for (var29 = 2; var29 < var16.length; var29 += 2) {
				var30 = var16[var29];
				var48 = var16[var29 + 1] << 1;
				var32 = var46 * (var30 - var49) + (var30 - var49) / 2;

				for (var33 = var49; var33 < var30; ++var33) {
					var35 = var30 - var49;
					var36 = var32 >>> 31;
					var34 = (var36 + var32) / var35 - var36;
					int var37 = var34 + (this.field2971[var33] & 255);
					if (var37 < 0) {
						var37 = 0;
					}

					if (var37 > 128) {
						var37 = 128;
					}

					this.field2971[var33] = (byte) var37;
					var32 += var48 - var46;
				}

				var49 = var30;
				var46 = var48;
			}

			for (var47 = var49; var47 < 128; ++var47) {
				var48 = var46 + (this.field2971[var47] & 255);
				if (var48 < 0) {
					var48 = 0;
				}

				if (var48 > 128) {
					var48 = 128;
				}

				this.field2971[var47] = (byte) var48;
			}

			Object var45 = null;
		}

		for (var27 = 0; var27 < var12; ++var27) {
			var39[var27].field2913 = buffer.get() & 0xFF;
		}

		for (var27 = 0; var27 < var12; ++var27) {
			var41 = var39[var27];
			if (var41.field2916 != null) {
				var41.field2918 = buffer.get() & 0xFF;
			}

			if (var41.field2914 != null) {
				var41.field2915 = buffer.get() & 0xFF;
			}

			if (var41.field2913 > 0) {
				var41.field2912 = buffer.get() & 0xFF;
			}
		}

		for (var27 = 0; var27 < var12; ++var27) {
			var39[var27].field2911 = buffer.get() & 0xFF;
		}

		for (var27 = 0; var27 < var12; ++var27) {
			var41 = var39[var27];
			if (var41.field2911 > 0) {
				var41.field2917 = buffer.get() & 0xFF;
			}
		}

		for (var27 = 0; var27 < var12; ++var27) {
			var41 = var39[var27];
			if (var41.field2917 > 0) {
				var41.field2919 = buffer.get() & 0xFF;
			}
		}
	}

	boolean method4945(SoundCache var1, byte[] var2, int[] var3) {
		boolean var4 = true;
		int var5 = 0;
		RawSound var6 = null;

		for (int var7 = 0; var7 < 128; ++var7) {
			if (var2 == null || var2[var7] != 0) {
				int var8 = this.sampleOffset[var7];
				if (var8 != 0) {
					if (var5 != var8) {
						var5 = var8--;
						if ((var8 & 1) == 0) {
							var6 = var1.getSoundEffect(var8 >> 2, var3);
						} else {
							var6 = var1.getMusicSample(var8 >> 2, var3);
						}

						if (var6 == null) {
							var4 = false;
						}
					}

					if (var6 != null) {
						this.rawSounds[var7] = var6;
						this.sampleOffset[var7] = 0;
					}
				}
			}
		}

		return var4;
	}

	void clear() {
		this.sampleOffset = null;
	}

}
