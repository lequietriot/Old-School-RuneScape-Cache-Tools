package osrs;

import com.sun.media.sound.*;

import javax.sound.midi.Patch;
import javax.sound.sampled.AudioInputStream;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MusicPatch extends Node {

	public int volume;
	public AudioDataSource[] audioDataSources;
	public short[] pitchOffset;
	public byte[] volumeOffset;
	public byte[] panOffset;
	public MusicPatchNode2[] musicPatchParameters;
	public byte[] loopOffset;
	public int[] sampleOffset;

	private static final int DEFAULT_BUFFER_SIZE = 8192;
	private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

	public MusicPatch(byte[] var1) {
		this.audioDataSources = new AudioDataSource[128];
		this.pitchOffset = new short[128];
		this.volumeOffset = new byte[128];
		this.panOffset = new byte[128];
		this.musicPatchParameters = new MusicPatchNode2[128];
		this.loopOffset = new byte[128];
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

		MusicPatchNode2[] musicPatchParamsFinal = new MusicPatchNode2[var12];

		MusicPatchNode2 var15;
		for (var14 = 0; var14 < musicPatchParamsFinal.length; ++var14) {
			var15 = musicPatchParamsFinal[var14] = new MusicPatchNode2();
			int envelopeArrayLength = buffer.get() & 0xFF;
			if (envelopeArrayLength > 0) {
				var15.attackEnvelope = new byte[envelopeArrayLength * 2];
			}

			envelopeArrayLength = buffer.get() & 0xFF;
			if (envelopeArrayLength > 0) {
				var15.decayEnvelope = new byte[envelopeArrayLength * 2 + 2];
				var15.decayEnvelope[1] = 64;
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

				this.loopOffset[var24] = (byte) var23;
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

				this.panOffset[var25] = (byte) var24;
				--var20;
			}
		}

		var20 = 0;
		var21 = 0;
		MusicPatchNode2 musicPatchParamsToCopy = null;

		int var26;
		for (var26 = 0; var26 < 128; ++var26) {
			if (this.sampleOffset[var26] != 0) {
				if (var20 == 0) {
					musicPatchParamsToCopy = musicPatchParamsFinal[var38[var21]];
					if (var21 < var10.length) {
						var20 = var10[var21++];
					} else {
						var20 = -1;
					}
				}

				this.musicPatchParameters[var26] = musicPatchParamsToCopy;
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

			this.volumeOffset[var27] = (byte) var26;
			--var20;
		}

		this.volume = (buffer.get() & 0xFF) + 1;

		int var29;
		MusicPatchNode2 var41;
		for (var27 = 0; var27 < var12; ++var27) {
			var41 = musicPatchParamsFinal[var27];
			if (var41.attackEnvelope != null) {
				for (var29 = 1; var29 < var41.attackEnvelope.length; var29 += 2) {
					var41.attackEnvelope[var29] = buffer.get();
				}
			}

			if (var41.decayEnvelope != null) {
				for (var29 = 3; var29 < var41.decayEnvelope.length - 2; var29 += 2) {
					var41.decayEnvelope[var29] = buffer.get();
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
			var41 = musicPatchParamsFinal[var27];
			if (var41.decayEnvelope != null) {
				var19 = 0;

				for (var29 = 2; var29 < var41.decayEnvelope.length; var29 += 2) {
					var19 = var19 + 1 + buffer.get() & 0xFF;
					var41.decayEnvelope[var29] = (byte) var19;
				}
			}
		}

		for (var27 = 0; var27 < var12; ++var27) {
			var41 = musicPatchParamsFinal[var27];
			if (var41.attackEnvelope != null) {
				var19 = 0;

				for (var29 = 2; var29 < var41.attackEnvelope.length; var29 += 2) {
					var19 = 1 + var19 + buffer.get() & 0xFF;
					var41.attackEnvelope[var29] = (byte) var19;
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
				this.volumeOffset[var29] = (byte) (var28 * this.volumeOffset[var29] + 32 >> 6);
			}

			for (var29 = 2; var29 < var44.length; var29 += 2) {
				var30 = var44[var29];
				byte var31 = var44[var29 + 1];
				var32 = var28 * (var30 - var49) + (var30 - var49) / 2;

				for (var33 = var49; var33 < var30; ++var33) {
					var35 = var30 - var49;
					var36 = var32 >>> 31;
					var34 = (var32 + var36) / var35 - var36;
					this.volumeOffset[var33] = (byte) (var34 * this.volumeOffset[var33] + 32 >> 6);
					var32 += var31 - var28;
				}

				var49 = var30;
				var28 = var31;
			}

			for (var47 = var49; var47 < 128; ++var47) {
				this.volumeOffset[var47] = (byte) (var28 * this.volumeOffset[var47] + 32 >> 6);
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
				var47 = var46 + (this.panOffset[var29] & 255);
				if (var47 < 0) {
					var47 = 0;
				}

				if (var47 > 128) {
					var47 = 128;
				}

				this.panOffset[var29] = (byte) var47;
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
					int var37 = var34 + (this.panOffset[var33] & 255);
					if (var37 < 0) {
						var37 = 0;
					}

					if (var37 > 128) {
						var37 = 128;
					}

					this.panOffset[var33] = (byte) var37;
					var32 += var48 - var46;
				}

				var49 = var30;
				var46 = var48;
			}

			for (var47 = var49; var47 < 128; ++var47) {
				var48 = var46 + (this.panOffset[var47] & 255);
				if (var48 < 0) {
					var48 = 0;
				}

				if (var48 > 128) {
					var48 = 128;
				}

				this.panOffset[var47] = (byte) var48;
			}

		}

		for (var27 = 0; var27 < var12; ++var27) {
			musicPatchParamsFinal[var27].sustain = buffer.get() & 0xFF;
		}

		for (var27 = 0; var27 < var12; ++var27) {
			var41 = musicPatchParamsFinal[var27];
			if (var41.attackEnvelope != null) {
				var41.field2918 = buffer.get() & 0xFF;
			}

			if (var41.decayEnvelope != null) {
				var41.release = buffer.get() & 0xFF;
			}

			if (var41.sustain > 0) {
				var41.field2912 = buffer.get() & 0xFF;
			}
		}

		for (var27 = 0; var27 < var12; ++var27) {
			musicPatchParamsFinal[var27].vibratoFrequencyHertz = buffer.get() & 0xFF;
		}

		for (var27 = 0; var27 < var12; ++var27) {
			var41 = musicPatchParamsFinal[var27];
			if (var41.vibratoFrequencyHertz > 0) {
				var41.vibratoPitchModulatorCents = buffer.get() & 0xFF;
			}
		}

		for (var27 = 0; var27 < var12; ++var27) {
			var41 = musicPatchParamsFinal[var27];
			if (var41.vibratoPitchModulatorCents > 0) {
				var41.vibratoDelayMilliseconds = buffer.get() & 0xFF;
			}
		}

	}

	public void copyParametersTo(byte[] from, byte[] to, File path) throws FileNotFoundException {
		this.audioDataSources = new AudioDataSource[128];
		this.pitchOffset = new short[128];
		this.volumeOffset = new byte[128];
		this.panOffset = new byte[128];
		this.musicPatchParameters = new MusicPatchNode2[128];
		this.loopOffset = new byte[128];
		this.sampleOffset = new int[128];
		ByteBuffer buffer = ByteBuffer.wrap(from);
		ByteBuffer buffer2 = ByteBuffer.wrap(to);
		DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(path));

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

		MusicPatchNode2[] musicPatchParamsFinal = new MusicPatchNode2[var12];

		MusicPatchNode2 var15;
		for (var14 = 0; var14 < musicPatchParamsFinal.length; ++var14) {
			var15 = musicPatchParamsFinal[var14] = new MusicPatchNode2();
			int envelopeArrayLength = buffer.get() & 0xFF;
			if (envelopeArrayLength > 0) {
				var15.attackEnvelope = new byte[envelopeArrayLength * 2];
			}

			envelopeArrayLength = buffer.get() & 0xFF;
			if (envelopeArrayLength > 0) {
				var15.decayEnvelope = new byte[envelopeArrayLength * 2 + 2];
				var15.decayEnvelope[1] = 64;
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

				this.loopOffset[var24] = (byte) var23;
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

				this.panOffset[var25] = (byte) var24;
				--var20;
			}
		}

		var20 = 0;
		var21 = 0;
		MusicPatchNode2 musicPatchParamsToCopy = null;

		int var26;
		for (var26 = 0; var26 < 128; ++var26) {
			if (this.sampleOffset[var26] != 0) {
				if (var20 == 0) {
					musicPatchParamsToCopy = musicPatchParamsFinal[var38[var21]];
					if (var21 < var10.length) {
						var20 = var10[var21++];
					} else {
						var20 = -1;
					}
				}

				this.musicPatchParameters[var26] = musicPatchParamsToCopy;
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

			this.volumeOffset[var27] = (byte) var26;
			--var20;
		}

		this.volume = (buffer.get() & 0xFF) + 1;

		int var29;
		MusicPatchNode2 var41;
		for (var27 = 0; var27 < var12; ++var27) {
			var41 = musicPatchParamsFinal[var27];
			if (var41.attackEnvelope != null) {
				for (var29 = 1; var29 < var41.attackEnvelope.length; var29 += 2) {
					var41.attackEnvelope[var29] = buffer.get();
				}
			}

			if (var41.decayEnvelope != null) {
				for (var29 = 3; var29 < var41.decayEnvelope.length - 2; var29 += 2) {
					var41.decayEnvelope[var29] = buffer.get();
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
			var41 = musicPatchParamsFinal[var27];
			if (var41.decayEnvelope != null) {
				var19 = 0;

				for (var29 = 2; var29 < var41.decayEnvelope.length; var29 += 2) {
					var19 = var19 + 1 + buffer.get() & 0xFF;
					var41.decayEnvelope[var29] = (byte) var19;
				}
			}
		}

		for (var27 = 0; var27 < var12; ++var27) {
			var41 = musicPatchParamsFinal[var27];
			if (var41.attackEnvelope != null) {
				var19 = 0;

				for (var29 = 2; var29 < var41.attackEnvelope.length; var29 += 2) {
					var19 = 1 + var19 + buffer.get() & 0xFF;
					var41.attackEnvelope[var29] = (byte) var19;
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
				this.volumeOffset[var29] = (byte) (var28 * this.volumeOffset[var29] + 32 >> 6);
			}

			for (var29 = 2; var29 < var44.length; var29 += 2) {
				var30 = var44[var29];
				byte var31 = var44[var29 + 1];
				var32 = var28 * (var30 - var49) + (var30 - var49) / 2;

				for (var33 = var49; var33 < var30; ++var33) {
					var35 = var30 - var49;
					var36 = var32 >>> 31;
					var34 = (var32 + var36) / var35 - var36;
					this.volumeOffset[var33] = (byte) (var34 * this.volumeOffset[var33] + 32 >> 6);
					var32 += var31 - var28;
				}

				var49 = var30;
				var28 = var31;
			}

			for (var47 = var49; var47 < 128; ++var47) {
				this.volumeOffset[var47] = (byte) (var28 * this.volumeOffset[var47] + 32 >> 6);
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
				var47 = var46 + (this.panOffset[var29] & 255);
				if (var47 < 0) {
					var47 = 0;
				}

				if (var47 > 128) {
					var47 = 128;
				}

				this.panOffset[var29] = (byte) var47;
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
					int var37 = var34 + (this.panOffset[var33] & 255);
					if (var37 < 0) {
						var37 = 0;
					}

					if (var37 > 128) {
						var37 = 128;
					}

					this.panOffset[var33] = (byte) var37;
					var32 += var48 - var46;
				}

				var49 = var30;
				var46 = var48;
			}

			for (var47 = var49; var47 < 128; ++var47) {
				var48 = var46 + (this.panOffset[var47] & 255);
				if (var48 < 0) {
					var48 = 0;
				}

				if (var48 > 128) {
					var48 = 128;
				}

				this.panOffset[var47] = (byte) var48;
			}

		}

		for (var27 = 0; var27 < var12; ++var27) {
			musicPatchParamsFinal[var27].sustain = buffer.get() & 0xFF;
		}

		for (var27 = 0; var27 < var12; ++var27) {
			var41 = musicPatchParamsFinal[var27];
			if (var41.attackEnvelope != null) {
				var41.field2918 = buffer.get() & 0xFF;
			}

			if (var41.decayEnvelope != null) {
				var41.release = buffer.get() & 0xFF;
			}

			if (var41.sustain > 0) {
				var41.field2912 = buffer.get() & 0xFF;
			}
		}

		for (var27 = 0; var27 < var12; ++var27) {
			musicPatchParamsFinal[var27].vibratoFrequencyHertz = buffer.get() & 0xFF;
		}

		for (var27 = 0; var27 < var12; ++var27) {
			var41 = musicPatchParamsFinal[var27];
			if (var41.vibratoFrequencyHertz > 0) {
				var41.vibratoPitchModulatorCents = buffer.get() & 0xFF;
			}
		}

		for (var27 = 0; var27 < var12; ++var27) {
			var41 = musicPatchParamsFinal[var27];
			if (var41.vibratoPitchModulatorCents > 0) {
				var41.vibratoDelayMilliseconds = buffer.get() & 0xFF;
			}
		}

	}

	boolean method4945(SoundCache var1, byte[] var2, int[] var3) {
		boolean var4 = true;
		int var5 = 0;
		AudioDataSource var6 = null;

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
						this.audioDataSources[var7] = var6;
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

	public SF2Soundbank decodeMusicPatch(SoundCache soundCache, SF2Soundbank sf2Soundbank, int id) {
		int var5 = 0;
		int lowNote = 0;
		AudioDataSource var6 = null;
		SF2Sample sf2Sample = null;
		SF2Layer sf2Layer = null;
		SF2LayerRegion sf2LayerRegion = null;
		SF2Instrument sf2Instrument = null;
		SF2InstrumentRegion sf2InstrumentRegion = null;

		for (int var7 = 0; var7 < 128; ++var7) {
			int var8 = this.sampleOffset[var7];
			if (var8 != 0) {
				if (var5 != var8) {
					var5 = var8--;
					if ((var8 & 1) == 0) {
						var6 = soundCache.getSoundEffect(var8 >> 2, null);
					} else {
						var6 = soundCache.getMusicSample(var8 >> 2, null);
					}
					if (var6 != null) {
						byte[] sample = new byte[var6.audioData.length * 2];
						for (int index = 0; index < sample.length; index++) {
							sample[index] = var6.audioData[index / 2];
						}
						sf2Sample = new SF2Sample(sf2Soundbank);
						sf2Sample.setName(String.valueOf(var8 >> 2));
						sf2Sample.setData(sample);
						sf2Sample.setSampleRate(var6.sampleRate);
						sf2Sample.setStartLoop(var6.loopStart);
						sf2Sample.setEndLoop(var6.loopEnd);
						sf2Sample.setSampleType(1);
						sf2Sample.setSampleLink(-1);

						sf2Soundbank.addResource(sf2Sample);
						lowNote = var7;
					}
				}

				if (var6 != null) {

					if (sf2Layer == null) {
						sf2Layer = new SF2Layer();
						sf2Layer.setName("Patch " + id);
					}

					sf2LayerRegion = new SF2LayerRegion();

					if (sf2Instrument == null) {
						sf2Instrument = new SF2Instrument();
						sf2Instrument.setName("Patch " + id);
						int bank = 0;
						while (id > 127) {
							id = id - 128;
							bank++;
							if (id < 128) {
								bank = bank * 128;
							}
						}
						sf2Instrument.setPatch(new Patch(bank, id));
					}

					if (sf2InstrumentRegion == null) {
						sf2InstrumentRegion = new SF2InstrumentRegion();
					}

					byte[] noteRanges;
					if (var7 >= lowNote) {
						noteRanges = new byte[]{(byte) lowNote, (byte) var7};
					}
					else {
						noteRanges = new byte[]{(byte) var7, (byte) var7};
					}

					assert musicPatchParameters[var7] != null;
					if (!sf2Sample.getName().equals(String.valueOf(var8 >> 2))) {
						if ((var7 + 1) != 128) {
							if (pitchOffset[var7] != pitchOffset[var7 + 1]) {
								addSamplesToBank(sf2Soundbank, sf2Instrument, sf2InstrumentRegion, sf2Layer, sf2LayerRegion, sf2Sample, noteRanges, var7, true);
							}
						}
						if (var7 == 127) {
							addSamplesToBank(sf2Soundbank, sf2Instrument, sf2InstrumentRegion, sf2Layer, sf2LayerRegion, sf2Sample, noteRanges, var7, true);
						}
					}
					else {
						if ((var7 + 1) != 128) {
							if (pitchOffset[var7] != pitchOffset[var7 + 1]) {
								addSamplesToBank(sf2Soundbank, sf2Instrument, sf2InstrumentRegion, sf2Layer, sf2LayerRegion, sf2Sample, noteRanges, var7, false);
							}
						}
						if (var7 == 127) {
							addSamplesToBank(sf2Soundbank, sf2Instrument, sf2InstrumentRegion, sf2Layer, sf2LayerRegion, sf2Sample, noteRanges, var7, false);
						}
					}

					sampleOffset[var7] = 0;
				}
			}
		}
		return sf2Soundbank;
	}

	public void addSamplesToBank(SF2Soundbank sf2Soundbank, SF2Instrument sf2Instrument, SF2InstrumentRegion sf2InstrumentRegion, SF2Layer sf2Layer, SF2LayerRegion sf2LayerRegion, SF2Sample sf2Sample, byte[] noteRanges, int currentNote, boolean addRange) {

		byte lowNote = noteRanges[0];
		byte highNote = noteRanges[1];

		if (addRange) {
			noteRanges = new byte[] {lowNote, highNote};
		}

		else {
			noteRanges = new byte[] {(byte) currentNote, (byte) currentNote};
		}

		if (sf2Soundbank.getSamples() != null) {
			for (SF2Sample sampleSf2 : sf2Soundbank.getSamples()) {
				if (sampleSf2 == sf2Sample) {
					sf2Soundbank.addResource(sf2Sample);
					sf2Soundbank.removeResource(sampleSf2);
				}
			}
		}

		sf2LayerRegion.setSample(sf2Sample);
		short pitchOffset = (short) ((this.pitchOffset[currentNote] / 256) + 128);
		short pitchCorrection = this.pitchOffset[currentNote];

		while (pitchOffset >= 128) {
			pitchOffset = (short) (pitchOffset - 128);
		}

		while (pitchCorrection <= -256) {
			pitchCorrection = (short) (pitchCorrection + 256);
		}

		while (pitchCorrection >= 256) {
			pitchCorrection = (short) (pitchCorrection - 256);
		}

		pitchCorrection = (short) ((pitchCorrection) / -2.56);

		int loopMode = 0;
		if (sf2Sample != null && sf2Sample.getStartLoop() != 0) {
			loopMode = 1;
		}

		int volume = volumeOffset[currentNote] + this.volume;
		double attenuation = (volume / 128.0) * 48.16;
		double maxEightBits = -48.16;

		sf2LayerRegion.putInteger(SF2Region.GENERATOR_SAMPLEMODES, loopMode);
		if (sf2Sample != null) {
			sf2Sample.setOriginalPitch(pitchOffset);
		}
		sf2LayerRegion.putBytes(SF2Region.GENERATOR_KEYRANGE, noteRanges);
		if (pitchOffset != (this.pitchOffset[currentNote] / 256) + 128) {
			sf2LayerRegion.putShort(SF2Region.GENERATOR_OVERRIDINGROOTKEY, pitchOffset);
		}
		sf2LayerRegion.putShort(SF2Region.GENERATOR_FINETUNE, pitchCorrection);
		sf2LayerRegion.putShort(SF2Region.GENERATOR_INITIALATTENUATION, (short) ((maxEightBits + attenuation) * -4));
		sf2LayerRegion.putShort(SF2Region.GENERATOR_PAN, (short) (((panOffset[currentNote] / 1.28) - 50) * 10));

		/*
		try {
			SF2Soundbank sf2Copy = new SF2Soundbank(new File(AppConstants.customSoundFontPath));
			if (sf2Copy.getInstrument(sf2Instrument.getPatch()) != null) {
				int attack = ((SF2Instrument) sf2Copy.getInstrument(sf2Instrument.getPatch())).getRegions().get(0).getLayer().getGlobalRegion().getInteger(SF2Region.GENERATOR_ATTACKVOLENV);
				int decay = ((SF2Instrument) sf2Copy.getInstrument(sf2Instrument.getPatch())).getRegions().get(0).getLayer().getGlobalRegion().getInteger(SF2Region.GENERATOR_DECAYVOLENV);
				int sustain = ((SF2Instrument) sf2Copy.getInstrument(sf2Instrument.getPatch())).getRegions().get(0).getLayer().getGlobalRegion().getInteger(SF2Region.GENERATOR_SUSTAINVOLENV);
				int release = ((SF2Instrument) sf2Copy.getInstrument(sf2Instrument.getPatch())).getRegions().get(0).getLayer().getGlobalRegion().getInteger(SF2Region.GENERATOR_RELEASEVOLENV);
				sf2LayerRegion.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, attack);
				sf2LayerRegion.putInteger(SF2Region.GENERATOR_DECAYVOLENV, decay);
				sf2LayerRegion.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, sustain);
				sf2LayerRegion.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, release);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (musicPatchParameters[currentNote] != null) {
			if (musicPatchParameters[currentNote].attackEnvelope != null) {
				int attackValue = 0;
				for (int attackIndex = 0; attackIndex < musicPatchParameters[currentNote].attackEnvelope.length; attackIndex++) {
					attackValue = attackValue + musicPatchParameters[currentNote].attackEnvelope[attackIndex];
				}
				//sf2LayerRegion.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, SF2Region.getDefaultValue(SF2Region.GENERATOR_ATTACKVOLENV) + musicPatchParameters[currentNote].field2915 * 100);
			}
			//sf2LayerRegion.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, (int) (144.0 * 10));
			if (musicPatchParameters[currentNote].decayEnvelope != null) {
				int decayValue = 0;
				for (int decayIndex = 0; decayIndex < musicPatchParameters[currentNote].decayEnvelope.length; decayIndex++) {
					decayValue = decayValue + musicPatchParameters[currentNote].decayEnvelope[decayIndex];
				}
				//sf2LayerRegion.putInteger(SF2Region.GENERATOR_DECAYVOLENV, decayValue);
			}
			//sf2LayerRegion.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 0);
		}
		 */

		sf2Layer.getRegions().add(sf2LayerRegion);

		sf2InstrumentRegion.setLayer(sf2Layer);

		if (sf2Soundbank.getInstrument(sf2Instrument.getPatch()) == null) {
			sf2Soundbank.addResource(sf2Layer);
			sf2Instrument.getRegions().add(sf2InstrumentRegion);
			sf2Soundbank.addInstrument(sf2Instrument);
		}
	}

	public void mapSoundFontSamples(int key, int channel, SF2Soundbank sf2Soundbank) {

		if (channel == 0) {
			Arrays.fill(panOffset, (byte) 0);
		}

		if (channel == 1) {
			Arrays.fill(panOffset, (byte) 127);
		}

		Patch patch;

		int bank = 0;
		int patchNumber = key;

		while (patchNumber > 127) {
			patchNumber = patchNumber - 128;
			bank++;
		}

		bank = bank * 128;

		patch = new Patch(bank, patchNumber);

		if (sf2Soundbank.getInstrument(patch) != null) {
			for (SF2InstrumentRegion sf2InstrumentRegion : ((SF2Instrument) sf2Soundbank.getInstrument(patch)).getRegions()) {
				 for (SF2LayerRegion sf2LayerRegion : sf2InstrumentRegion.getLayer().getRegions()) {
					 SF2Sample sf2Sample = sf2LayerRegion.getSample();
					 try {
						 byte[] sampleData = readAllBytes((AudioInputStream) sf2Sample.getData());
						 byte[] noteRange = sf2LayerRegion.getBytes(SF2Region.GENERATOR_KEYRANGE);
						 int loopModes = sf2LayerRegion.getInteger(SF2Region.GENERATOR_SAMPLEMODES);
						 int loopModeGlobal = sf2InstrumentRegion.getLayer().getGlobalRegion().getInteger(SF2Region.GENERATOR_SAMPLEMODES);
						 int pitchCorrection = sf2Sample.getPitchCorrection();
						 byte[] overridingNote = sf2LayerRegion.getBytes(SF2Region.GENERATOR_OVERRIDINGROOTKEY);
						 int fineTune = sf2LayerRegion.getInteger(SF2Region.GENERATOR_FINETUNE) * 256 / 100;
						 int coarseTune = sf2LayerRegion.getInteger(SF2Region.GENERATOR_COARSETUNE) * 256 / 100;

						 if (noteRange[0] == noteRange[1]) {
							 noteRange[1]++;
						 }

						 if (loopModeGlobal != 0) {
							 loopModes = loopModeGlobal;
						 }

						 for (int note = noteRange[0]; note < noteRange[1] + 1; note++) {
							 if (loopModes == 1) {
								 audioDataSources[note] = new AudioDataSource((int) sf2Sample.getSampleRate(), getEightBitData(sampleData), (int) sf2Sample.getStartLoop(), (int) sf2Sample.getEndLoop());
								 if (overridingNote != null && overridingNote[0] != -1) {
									 this.pitchOffset[note] = (short) ((overridingNote[0] * 256) - ((pitchCorrection + fineTune + coarseTune)));
								 } else {
									 this.pitchOffset[note] = (short) (((sf2Sample.getOriginalPitch() * 256) - ((pitchCorrection + fineTune + coarseTune))));
								 }

								 this.pitchOffset[note] = (short) (this.pitchOffset[note] - 32768);

							 } else {
								 audioDataSources[note] = new AudioDataSource((int) sf2Sample.getSampleRate(), getEightBitData(sampleData), 0, 0);
								 if (overridingNote != null && overridingNote[0] != -1) {
									 this.pitchOffset[note] = (short) ((overridingNote[0] * 256) + ((pitchCorrection + fineTune + coarseTune)));
								 } else {
									 this.pitchOffset[note] = (short) (((sf2Sample.getOriginalPitch() * 256) + ((pitchCorrection + fineTune + coarseTune))));
								 }
							 }
						 }
					 } catch (IOException e) {
						 e.printStackTrace();
					 }
				 }
			}
		}
	}
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

	public byte[] getEightBitData(byte[] sampleBytes) {

		byte[] compressedData = new byte[sampleBytes.length / 2];

		for (int index = 0; index < compressedData.length; index++) {
			compressedData[index] = sampleBytes[index * 2 + 1];
		}

		return compressedData;
	}

}
