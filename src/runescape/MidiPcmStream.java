package runescape;

import com.sun.media.sound.SF2Soundbank;
import org.displee.cache.index.Index;

public class MidiPcmStream extends PcmStream {

	NodeHashTable musicPatches;
	int volume;
	int field2931;
	int[] field2956;
	int[] field2933;
	int[] field2934;
	int[] field2941;
	int[] field2936;
	int[] field2937;
	int[] field2938;
	int[] field2939;
	int[] field2940;
	int[] field2944;
	int[] field2942;
	int[] field2943;
	int[] field2932;
	int[] field2945;
	int[] field2946;
	MusicPatchNode[][] field2952;
	MusicPatchNode[][] field2949;
	public MidiFileReader midiFile;
	boolean field2950;
	int track;
	int trackLength;
	long field2953;
	long field2954;
	MusicPatchPcmStream patchStream;

	public MidiPcmStream() {
		this.volume = 256;
		this.field2931 = 1000000;
		this.field2956 = new int[16];
		this.field2933 = new int[16];
		this.field2934 = new int[16];
		this.field2941 = new int[16];
		this.field2936 = new int[16];
		this.field2937 = new int[16];
		this.field2938 = new int[16];
		this.field2939 = new int[16];
		this.field2940 = new int[16];
		this.field2944 = new int[16];
		this.field2942 = new int[16];
		this.field2943 = new int[16];
		this.field2932 = new int[16];
		this.field2945 = new int[16];
		this.field2946 = new int[16];
		this.field2952 = new MusicPatchNode[16][128];
		this.field2949 = new MusicPatchNode[16][128];
		this.midiFile = new MidiFileReader();
		this.patchStream = new MusicPatchPcmStream(this);
		this.musicPatches = new NodeHashTable(128);
		this.method4773();
	}

	public synchronized void setPcmStreamVolume(int volumeLevel) {
		this.volume = volumeLevel;
	}

	int getPcmStreamVolume() {
		return this.volume;
	}

	public synchronized boolean loadMusicTrack(MusicTrack var1, Index var2, SoundCache var3, int var4) {
		var1.method4981();
		boolean var5 = true;
		int[] var6 = null;
		if (var4 > 0) {
			var6 = new int[]{var4};
		}

		for (ByteArrayNode var7 = (ByteArrayNode)var1.table.first(); var7 != null; var7 = (ByteArrayNode)var1.table.next()) {
			int var8 = (int)var7.key;
			MusicPatch var9 = (MusicPatch)this.musicPatches.get((long)var8);
			if (var9 == null) {
				byte[] var11 = var2.getArchive(var8).getFile(0).getData();
				MusicPatch var10;
				if (var11 == null) {
					var10 = null;
				} else {
					var10 = new MusicPatch(var11);
				}

				var9 = var10;
				if (var10 == null) {
					var5 = false;
					continue;
				}

				this.musicPatches.put(var10, (long)var8);
			}

			if (!var9.method4945(var3, var7.byteArray, var6)) {
				var5 = false;
			}
		}

		if (var5) {
			var1.clear();
		}

		return var5;
	}

	public synchronized void clearAll() {
		for (MusicPatch var1 = (MusicPatch) this.musicPatches.first(); var1 != null; var1 = (MusicPatch) this.musicPatches.next()) {
			var1.clear();
		}

	}

	synchronized void removeAll() {
		for (MusicPatch var1 = (MusicPatch)this.musicPatches.first(); var1 != null; var1 = (MusicPatch)this.musicPatches.next()) {
			var1.remove();
		}

	}

	protected synchronized PcmStream firstSubStream() {
		return this.patchStream;
	}

	protected synchronized PcmStream nextSubStream() {
		return null;
	}

	protected synchronized int vmethod4958() {
		return 0;
	}

	protected synchronized void fill(int[] var1, int var2, int var3) {
		if (this.midiFile.isReady()) {
			int tempo = this.midiFile.division * this.field2931 / SoundConstants.sampleRate;

			do {
				long var5 = this.field2953 + (long) tempo * (long)var3;
				if (this.field2954 - var5 >= 0L) {
					this.field2953 = var5;
					break;
				}

				int var7 = (int)((this.field2954 - this.field2953 + (long) tempo - 1L) / (long) tempo);
				this.field2953 += (long)var7 * (long)tempo;
				this.patchStream.fill(var1, var2, var7);
				var2 += var7;
				var3 -= var7;
				this.method4758();
			} while(this.midiFile.isReady());
		}

		this.patchStream.fill(var1, var2, var3);
	}

	public synchronized void setMusicTrack(MusicTrack var1, boolean var2) {
		this.clear();
		this.midiFile.parse(var1.midi);
		this.field2950 = var2;
		this.field2953 = 0L;
		int var3 = this.midiFile.trackCount();

		for (int var4 = 0; var4 < var3; ++var4) {
			this.midiFile.gotoTrack(var4);
			this.midiFile.readTrackLength(var4);
			this.midiFile.markTrackPosition(var4);
		}

		this.track = this.midiFile.getPrioritizedTrack();
		this.trackLength = this.midiFile.trackLengths[this.track];
		this.field2954 = this.midiFile.method4934(this.trackLength);
	}

	protected synchronized void skip(int var1) {
		if (this.midiFile.isReady()) {
			int var2 = this.midiFile.division * this.field2931 / SoundConstants.sampleRate;

			do {
				long var3 = (long)var1 * (long)var2 + this.field2953;
				if (this.field2954 - var3 >= 0L) {
					this.field2953 = var3;
					break;
				}

				int var5 = (int)(((long)var2 + (this.field2954 - this.field2953) - 1L) / (long)var2);
				this.field2953 += (long)var5 * (long)var2;
				this.patchStream.skip(var5);
				var1 -= var5;
				this.method4758();
			} while(this.midiFile.isReady());
		}

		this.patchStream.skip(var1);
	}

	public synchronized void clear() {
		this.midiFile.clear();
		this.method4773();
	}

	public synchronized boolean isReady() {
		return this.midiFile.isReady();
	}

	public synchronized void method4761(int var1, int var2) {
		this.method4826(var1, var2);
	}

	void method4826(int var1, int var2) {
		this.field2941[var1] = var2;
		this.field2937[var1] = var2 & -128;
		this.method4863(var1, var2);
	}

	void method4863(int var1, int var2) {
		if (var2 != this.field2936[var1]) {
			this.field2936[var1] = var2;

			for (int var3 = 0; var3 < 128; ++var3) {
				this.field2949[var1][var3] = null;
			}
		}

	}

	void method4764(int var1, int var2, int var3) {
		this.method4847(var1, var2, 64);
		if ((this.field2944[var1] & 2) != 0) {
			for (MusicPatchNode var4 = (MusicPatchNode)this.patchStream.queue.first(); var4 != null; var4 = (MusicPatchNode)this.patchStream.queue.next()) {
				if (var4.midiChannel == var1 && var4.field2999 < 0) {
					this.field2952[var1][var4.midiNote] = null;
					this.field2952[var1][var2] = var4;
					int var8 = (var4.field2998 * var4.field2997 >> 12) + var4.soundTransposition;
					var4.soundTransposition += var2 - var4.midiNote << 8;
					var4.field2997 = var8 - var4.soundTransposition;
					var4.field2998 = 4096;
					var4.midiNote = var2;
					return;
				}
			}
		}

		MusicPatch var9 = (MusicPatch)this.musicPatches.get(this.field2936[var1]);
		if (var9 != null) {
			RawSound var5 = var9.rawSounds[var2];
			if (var5 != null) {
				MusicPatchNode var6 = new MusicPatchNode();
				var6.midiChannel = var1;
				var6.patch = var9;
				var6.rawSound = var5;
				var6.field2988 = var9.field2976[var2];
				var6.field2989 = var9.field2977[var2];
				var6.midiNote = var2;
				var6.midiNoteVolume = var3 * var3 * var9.field2974[var2] * var9.field2973 + 1024 >> 11;
				var6.field2992 = var9.field2971[var2] & 255;
				var6.soundTransposition = (var2 << 8) - (var9.pitchOffset[var2] & 32767);
				var6.field2986 = 0;
				var6.field3004 = 0;
				var6.field2994 = 0;
				var6.field2999 = -1;
				var6.field3000 = 0;
				if (this.field2932[var1] == 0) {
					var6.stream = RawPcmStream.method817(var5, this.calculatePitch(var6), this.calculateVolume(var6), this.calculatePanning(var6));
				} else {
					var6.stream = RawPcmStream.method817(var5, this.calculatePitch(var6), 0, this.calculatePanning(var6));
					this.method4765(var6, var9.pitchOffset[var2] < 0);
				}

				if (var9.pitchOffset[var2] < 0) {
					if (var6.stream != null) {
						var6.stream.setNumLoops(-1);
					}
				}

				if (var6.field2989 >= 0) {
					MusicPatchNode var7 = this.field2949[var1][var6.field2989];
					if (var7 != null && var7.field2999 < 0) {
						this.field2952[var1][var7.midiNote] = null;
						var7.field2999 = 0;
					}

					this.field2949[var1][var6.field2989] = var6;
				}

				this.patchStream.queue.addFirst(var6);
				this.field2952[var1][var2] = var6;
			}
		}
	}

	void method4765(MusicPatchNode var1, boolean var2) {
		int var3 = var1.rawSound.samples.length;
		int var4;
		if (var2 && var1.rawSound.field238) {
			int var5 = var3 + var3 - var1.rawSound.start;
			var4 = (int)((long)this.field2932[var1.midiChannel] * (long)var5 >> 6);
			var3 <<= 8;
			if (var4 >= var3) {
				var4 = var3 + var3 - 1 - var4;
				var1.stream.method922();
			}
		} else {
			var4 = (int)((long)var3 * (long)this.field2932[var1.midiChannel] >> 6);
		}

		var1.stream.method825(var4);
	}

	void method4847(int var1, int var2, int var3) {
		MusicPatchNode var4 = this.field2952[var1][var2];
		if (var4 != null) {
			this.field2952[var1][var2] = null;
			if ((this.field2944[var1] & 2) != 0) {
				for (MusicPatchNode var5 = (MusicPatchNode)this.patchStream.queue.last(); var5 != null; var5 = (MusicPatchNode)this.patchStream.queue.previous()) {
					if (var4.midiChannel == var5.midiChannel && var5.field2999 < 0 && var4 != var5) {
						var4.field2999 = 0;
						break;
					}
				}
			} else {
				var4.field2999 = 0;
			}

		}
	}

	void method4853(int var1, int var2, int var3) {
	}

	void method4768(int var1, int var2) {
	}

	void method4769(int var1, int var2) {
		this.field2938[var1] = var2;
	}

	void method4770(int var1) {
		for (MusicPatchNode var2 = (MusicPatchNode)this.patchStream.queue.last(); var2 != null; var2 = (MusicPatchNode)this.patchStream.queue.previous()) {
			if (var1 < 0 || var2.midiChannel == var1) {
				if (var2.stream != null) {
					var2.stream.method830(SoundConstants.sampleRate / 100);
					if (var2.stream.method834()) {
						this.patchStream.mixer.addSubStream(var2.stream);
					}

					var2.method4992();
				}

				if (var2.field2999 < 0) {
					this.field2952[var2.midiChannel][var2.midiNote] = null;
				}

				var2.remove();
			}
		}

	}

	void method4771(int var1) {
		if (var1 >= 0) {
			this.field2956[var1] = 12800;
			this.field2933[var1] = 8192;
			this.field2934[var1] = 16383;
			this.field2938[var1] = 8192;
			this.field2939[var1] = 0;
			this.field2940[var1] = 8192;
			this.method4774(var1);
			this.method4775(var1);
			this.field2944[var1] = 0;
			this.field2942[var1] = 32767;
			this.field2943[var1] = 256;
			this.field2932[var1] = 0;
			this.method4777(var1, 8192);
		} else {
			for (var1 = 0; var1 < 16; ++var1) {
				this.method4771(var1);
			}

		}
	}

	void method4772(int var1) {
		for (MusicPatchNode var2 = (MusicPatchNode)this.patchStream.queue.last(); var2 != null; var2 = (MusicPatchNode)this.patchStream.queue.previous()) {
			if ((var1 < 0 || var2.midiChannel == var1) && var2.field2999 < 0) {
				this.field2952[var2.midiChannel][var2.midiNote] = null;
				var2.field2999 = 0;
			}
		}

	}

	public void method4773() {
		this.method4770(-1);
		this.method4771(-1);

		int var1;
		for (var1 = 0; var1 < 16; ++var1) {
			this.field2936[var1] = this.field2941[var1];
		}

		for (var1 = 0; var1 < 16; ++var1) {
			this.field2937[var1] = this.field2941[var1] & -128;
		}

	}

	void method4774(int var1) {
		if ((this.field2944[var1] & 2) != 0) {
			for (MusicPatchNode var2 = (MusicPatchNode)this.patchStream.queue.last(); var2 != null; var2 = (MusicPatchNode)this.patchStream.queue.previous()) {
				if (var2.midiChannel == var1 && this.field2952[var1][var2.midiNote] == null && var2.field2999 < 0) {
					var2.field2999 = 0;
				}
			}
		}

	}

	void method4775(int var1) {
		if ((this.field2944[var1] & 4) != 0) {
			for (MusicPatchNode var2 = (MusicPatchNode) this.patchStream.queue.last(); var2 != null; var2 = (MusicPatchNode) this.patchStream.queue.previous()) {
				if (var2.midiChannel == var1) {
					var2.field3003 = 0;
				}
			}
		}

	}

	void method4776(int var1) {
		int var2 = var1 & 240;
		int var3;
		int var4;
		int var5;
		if (var2 == 128) {
			var3 = var1 & 15;
			var4 = var1 >> 8 & 127;
			var5 = var1 >> 16 & 127;
			this.method4847(var3, var4, var5);
		} else if (var2 == 144) {
			var3 = var1 & 15;
			var4 = var1 >> 8 & 127;
			var5 = var1 >> 16 & 127;
			if (var5 > 0) {
				this.method4764(var3, var4, var5);
			} else {
				this.method4847(var3, var4, 64);
			}

		} else if (var2 == 160) {
			var3 = var1 & 15;
			var4 = var1 >> 8 & 127;
			var5 = var1 >> 16 & 127;
			this.method4853(var3, var4, var5);
		} else if (var2 == 176) {
			var3 = var1 & 15;
			var4 = var1 >> 8 & 127;
			var5 = var1 >> 16 & 127;
			if (var4 == 0) {
				this.field2937[var3] = (var5 << 14) + (this.field2937[var3] & -2080769);
			}

			if (var4 == 32) {
				this.field2937[var3] = (var5 << 7) + (this.field2937[var3] & -16257);
			}

			if (var4 == 1) {
				this.field2939[var3] = (var5 << 7) + (this.field2939[var3] & -16257);
			}

			if (var4 == 33) {
				this.field2939[var3] = var5 + (this.field2939[var3] & -128);
			}

			if (var4 == 5) {
				this.field2940[var3] = (var5 << 7) + (this.field2940[var3] & -16257);
			}

			if (var4 == 37) {
				this.field2940[var3] = var5 + (this.field2940[var3] & -128);
			}

			if (var4 == 7) {
				this.field2956[var3] = (var5 << 7) + (this.field2956[var3] & -16257);
			}

			if (var4 == 39) {
				this.field2956[var3] = var5 + (this.field2956[var3] & -128);
			}

			if (var4 == 10) {
				this.field2933[var3] = (var5 << 7) + (this.field2933[var3] & -16257);
			}

			if (var4 == 42) {
				this.field2933[var3] = var5 + (this.field2933[var3] & -128);
			}

			if (var4 == 11) {
				this.field2934[var3] = (var5 << 7) + (this.field2934[var3] & -16257);
			}

			if (var4 == 43) {
				this.field2934[var3] = var5 + (this.field2934[var3] & -128);
			}

			int[] var10000;
			if (var4 == 64) {
				if (var5 >= 64) {
					var10000 = this.field2944;
					var10000[var3] |= 1;
				} else {
					var10000 = this.field2944;
					var10000[var3] &= -2;
				}
			}

			if (var4 == 65) {
				if (var5 >= 64) {
					var10000 = this.field2944;
					var10000[var3] |= 2;
				} else {
					this.method4774(var3);
					var10000 = this.field2944;
					var10000[var3] &= -3;
				}
			}

			if (var4 == 99) {
				this.field2942[var3] = (var5 << 7) + (this.field2942[var3] & 127);
			}

			if (var4 == 98) {
				this.field2942[var3] = (this.field2942[var3] & 16256) + var5;
			}

			if (var4 == 101) {
				this.field2942[var3] = (var5 << 7) + (this.field2942[var3] & 127) + 16384;
			}

			if (var4 == 100) {
				this.field2942[var3] = (this.field2942[var3] & 16256) + var5 + 16384;
			}

			if (var4 == 120) {
				this.method4770(var3);
			}

			if (var4 == 121) {
				this.method4771(var3);
			}

			if (var4 == 123) {
				this.method4772(var3);
			}

			int var6;
			if (var4 == 6) {
				var6 = this.field2942[var3];
				if (var6 == 16384) {
					this.field2943[var3] = (var5 << 7) + (this.field2943[var3] & -16257);
				}
			}

			if (var4 == 38) {
				var6 = this.field2942[var3];
				if (var6 == 16384) {
					this.field2943[var3] = var5 + (this.field2943[var3] & -128);
				}
			}

			if (var4 == 16) {
				this.field2932[var3] = (var5 << 7) + (this.field2932[var3] & -16257);
			}

			if (var4 == 48) {
				this.field2932[var3] = var5 + (this.field2932[var3] & -128);
			}

			if (var4 == 81) {
				if (var5 >= 64) {
					var10000 = this.field2944;
					var10000[var3] |= 4;
				} else {
					this.method4775(var3);
					var10000 = this.field2944;
					var10000[var3] &= -5;
				}
			}

			if (var4 == 17) {
				this.method4777(var3, (var5 << 7) + (this.field2945[var3] & -16257));
			}

			if (var4 == 49) {
				this.method4777(var3, var5 + (this.field2945[var3] & -128));
			}

		} else if (var2 == 192) {
			var3 = var1 & 15;
			var4 = var1 >> 8 & 127;
			this.method4863(var3, var4 + this.field2937[var3]);
		} else if (var2 == 208) {
			var3 = var1 & 15;
			var4 = var1 >> 8 & 127;
			this.method4768(var3, var4);
		} else if (var2 == 224) {
			var3 = var1 & 15;
			var4 = (var1 >> 8 & 127) + (var1 >> 9 & 16256);
			this.method4769(var3, var4);
		} else {
			var2 = var1 & 255;
			if (var2 == 255) {
				this.method4773();
			}
		}
	}

	void method4777(int var1, int var2) {
		this.field2945[var1] = var2;
		this.field2946[var1] = (int)(2097152.0D * Math.pow(2.0D, 5.4931640625E-4D * (double)var2) + 0.5D);
	}

	int calculatePitch(MusicPatchNode var1) {
		int var2 = (var1.field2998 * var1.field2997 >> 12) + var1.soundTransposition;
		var2 += (this.field2938[var1.midiChannel] - 8192) * this.field2943[var1.midiChannel] >> 12;
		MusicPatchNode2 var3 = var1.field2988;
		int var4;
		if (var3.field2911 > 0 && (var3.field2917 > 0 || this.field2939[var1.midiChannel] > 0)) {
			var4 = var3.field2917 << 2;
			int var5 = var3.field2919 << 1;
			if (var1.field3001 < var5) {
				var4 = var4 * var1.field3001 / var5;
			}

			var4 += this.field2939[var1.midiChannel] >> 7;
			double var6 = Math.sin(0.01227184630308513D * (double)(var1.field3002 & 511));
			var2 += (int)(var6 * (double)var4);
		}

		var4 = (int)((double)(var1.rawSound.sampleRate * 256) * Math.pow(2.0D, (double)var2 * 3.255208333333333E-4D) / (double)SoundConstants.sampleRate + 0.5D);
		return var4 < 1 ? 1 : var4;
	}

	int calculateVolume(MusicPatchNode var1) {
		MusicPatchNode2 var2 = var1.field2988;
		int var3 = this.field2934[var1.midiChannel] * this.field2956[var1.midiChannel] + 4096 >> 13;
		var3 = var3 * var3 + 16384 >> 15;
		var3 = var3 * var1.midiNoteVolume + 16384 >> 15;
		var3 = var3 * this.volume + 128 >> 8;
		if (var2.field2913 > 0) {
			var3 = (int)((double)var3 * Math.pow(0.5D, (double)var2.field2913 * (double)var1.field2986 * 1.953125E-5D) + 0.5D);
		}

		int var4;
		int var5;
		int var6;
		int var7;
		if (var2.field2916 != null) {
			var4 = var1.field3004;
			var5 = var2.field2916[var1.field2994 + 1];
			if (var1.field2994 < var2.field2916.length - 2) {
				var6 = (var2.field2916[var1.field2994] & 255) << 8;
				var7 = (var2.field2916[var1.field2994 + 2] & 255) << 8;
				var5 += (var4 - var6) * (var2.field2916[var1.field2994 + 3] - var5) / (var7 - var6);
			}

			var3 = var3 * var5 + 32 >> 6;
		}

		if (var1.field2999 > 0 && var2.field2914 != null) {
			var4 = var1.field2999;
			var5 = var2.field2914[var1.field3000 + 1];
			if (var1.field3000 < var2.field2914.length - 2) {
				var6 = (var2.field2914[var1.field3000] & 255) << 8;
				var7 = (var2.field2914[var1.field3000 + 2] & 255) << 8;
				var5 += (var2.field2914[var1.field3000 + 3] - var5) * (var4 - var6) / (var7 - var6);
			}

			var3 = var5 * var3 + 32 >> 6;
		}

		return var3;
	}

	int calculatePanning(MusicPatchNode var1) {
		int var2 = this.field2933[var1.midiChannel];
		return var2 < 8192 ? var2 * var1.field2992 + 32 >> 6 : 16384 - ((128 - var1.field2992) * (16384 - var2) + 32 >> 6);
	}

	void method4758() {
		int var1 = this.track;
		int var2 = this.trackLength;

		long var3;
		for (var3 = this.field2954; var2 == this.trackLength; var3 = this.midiFile.method4934(var2)) {
			while (var2 == this.midiFile.trackLengths[var1]) {
				this.midiFile.gotoTrack(var1);
				int var5 = this.midiFile.readMessage(var1);
				if (var5 == 1) {
					this.midiFile.setTrackDone();
					this.midiFile.markTrackPosition(var1);
					if (this.midiFile.isDone()) {
						if (!this.field2950 || var2 == 0) {
							this.method4773();
							this.midiFile.clear();
							return;
						}

						this.midiFile.reset(var3);
					}
					break;
				}

				if ((var5 & 128) != 0) {
					this.method4776(var5);
				}

				this.midiFile.readTrackLength(var1);
				this.midiFile.markTrackPosition(var1);
			}

			var1 = this.midiFile.getPrioritizedTrack();
			var2 = this.midiFile.trackLengths[var1];
		}

		this.track = var1;
		this.trackLength = var2;
		this.field2954 = var3;
	}

	boolean method4787(MusicPatchNode var1) {
		if (var1.stream == null) {
			if (var1.field2999 >= 0) {
				var1.remove();
				if (var1.field2989 > 0 && var1 == this.field2949[var1.midiChannel][var1.field2989]) {
					this.field2949[var1.midiChannel][var1.field2989] = null;
				}
			}

			return true;
		} else {
			return false;
		}
	}

	boolean method4788(MusicPatchNode var1, int[] var2, int var3, int var4) {
		var1.field2995 = SoundConstants.sampleRate / 100;
		if (var1.field2999 < 0 || var1.stream != null && !var1.stream.method833()) {
			int var5 = var1.field2998;
			if (var5 > 0) {
				var5 -= (int)(16.0D * Math.pow(2.0D, 4.921259842519685E-4D * (double)this.field2940[var1.midiChannel]) + 0.5D);
				if (var5 < 0) {
					var5 = 0;
				}

				var1.field2998 = var5;
			}

			var1.stream.method912(this.calculatePitch(var1));
			MusicPatchNode2 var6 = var1.field2988;
			boolean var7 = false;
			++var1.field3001;
			var1.field3002 += var6.field2911;
			double var8 = 5.086263020833333E-6D * (double)((var1.midiNote - 60 << 8) + (var1.field2997 * var1.field2998 >> 12));
			if (var6.field2913 > 0) {
				if (var6.field2912 > 0) {
					var1.field2986 += (int)(128.0D * Math.pow(2.0D, (double)var6.field2912 * var8) + 0.5D);
				} else {
					var1.field2986 += 128;
				}
			}

			if (var6.field2916 != null) {
				if (var6.field2918 > 0) {
					var1.field3004 += (int)(128.0D * Math.pow(2.0D, var8 * (double)var6.field2918) + 0.5D);
				} else {
					var1.field3004 += 128;
				}

				while (var1.field2994 < var6.field2916.length - 2 && var1.field3004 > (var6.field2916[var1.field2994 + 2] & 255) << 8) {
					var1.field2994 += 2;
				}

				if (var6.field2916.length - 2 == var1.field2994 && var6.field2916[var1.field2994 + 1] == 0) {
					var7 = true;
				}
			}

			if (var1.field2999 >= 0 && var6.field2914 != null && (this.field2944[var1.midiChannel] & 1) == 0 && (var1.field2989 < 0 || var1 != this.field2949[var1.midiChannel][var1.field2989])) {
				if (var6.field2915 > 0) {
					var1.field2999 += (int)(128.0D * Math.pow(2.0D, var8 * (double)var6.field2915) + 0.5D);
				} else {
					var1.field2999 += 128;
				}

				while (var1.field3000 < var6.field2914.length - 2 && var1.field2999 > (var6.field2914[var1.field3000 + 2] & 255) << 8) {
					var1.field3000 += 2;
				}

				if (var6.field2914.length - 2 == var1.field3000) {
					var7 = true;
				}
			}

			if (var7) {
				var1.stream.method830(var1.field2995);
				if (var2 != null) {
					var1.stream.fill(var2, var3, var4);
				} else {
					var1.stream.skip(var4);
				}

				if (var1.stream.method834()) {
					this.patchStream.mixer.addSubStream(var1.stream);
				}

				var1.method4992();
				if (var1.field2999 >= 0) {
					var1.remove();
					if (var1.field2989 > 0 && var1 == this.field2949[var1.midiChannel][var1.field2989]) {
						this.field2949[var1.midiChannel][var1.field2989] = null;
					}
				}

				return true;
			} else {
				var1.stream.method829(var1.field2995, this.calculateVolume(var1), this.calculatePanning(var1));
				return false;
			}
		} else {
			var1.method4992();
			var1.remove();
			if (var1.field2989 > 0 && var1 == this.field2949[var1.midiChannel][var1.field2989]) {
				this.field2949[var1.midiChannel][var1.field2989] = null;
			}

			return true;
		}
	}

	public void loadSoundFont(SF2Soundbank sf2Soundbank, int channel) {
		if (musicPatches != null) {
			for (MusicPatch musicPatch = (MusicPatch) musicPatches.first(); musicPatch != null; musicPatch = (MusicPatch) musicPatches.next()) {
				musicPatch.mapSoundFontSamples((int) musicPatch.key, channel, sf2Soundbank);
			}
		}
	}
}
