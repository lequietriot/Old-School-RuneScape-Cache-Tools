package runescape;

import application.constants.AppConstants;
import com.sun.media.sound.SF2Soundbank;
import org.displee.cache.index.Index;

import java.io.File;
import java.io.IOException;

public class MidiPcmStream extends PcmStream {

	NodeHashTable musicPatches;
	int volume;
	public int tempo;
	int[] volumeControls;
	int[] panControls;
	int[] expressionControls;
	int[] programConstants;
	int[] patch;
	int[] bankControls;
	int[] pitchBendControls;
	int[] modulationControls;
	int[] portamentoTimeControls;
	int[] switchControls;
	int[] dataEntriesMSB;
	int[] dataEntriesLSB;
	int[] sampleLoopControls;
	int[] retriggerControls;
	int[] retriggerEffects;
	MusicPatchNode[][] loopedVoices;
	MusicPatchNode[][] oneShotVoices;
	public MidiFileReader midiFile;
	boolean isLooping;
	int track;
	int trackLength;
	public long microsecondLength;
	public long microsecondPosition;
	MusicPatchPcmStream patchStream;

	public MidiPcmStream() {
		this.volume = 256;
		this.tempo = 1000000;
		this.volumeControls = new int[16];
		this.panControls = new int[16];
		this.expressionControls = new int[16];
		this.programConstants = new int[16];
		this.patch = new int[16];
		this.bankControls = new int[16];
		this.pitchBendControls = new int[16];
		this.modulationControls = new int[16];
		this.portamentoTimeControls = new int[16];
		this.switchControls = new int[16];
		this.dataEntriesMSB = new int[16];
		this.dataEntriesLSB = new int[16];
		this.sampleLoopControls = new int[16];
		this.retriggerControls = new int[16];
		this.retriggerEffects = new int[16];
		this.loopedVoices = new MusicPatchNode[16][128];
		this.oneShotVoices = new MusicPatchNode[16][128];
		this.midiFile = new MidiFileReader();
		this.patchStream = new MusicPatchPcmStream(this);
		this.musicPatches = new NodeHashTable(128);
		this.systemReset();
	}

	public synchronized void setPcmStreamVolume(int volumeLevel) {
		this.volume = volumeLevel;
	}

	public int getPcmStreamVolume() {
		return this.volume;
	}

	public synchronized boolean loadMusicTrack(MusicTrack musicTrack, Index patchIndex, SoundCache soundCache, int sampleRate) {
		musicTrack.readMidiTrack();
		boolean reset = true;
		int[] sampleRates = null;
		if (sampleRate > 0) {
			sampleRates = new int[]{sampleRate};
		}

		for (ByteArrayNode var7 = (ByteArrayNode)musicTrack.table.first(); var7 != null; var7 = (ByteArrayNode)musicTrack.table.next()) {
			int var8 = (int)var7.key;
			MusicPatch musicPatch = (MusicPatch)this.musicPatches.get(var8);
			if (musicPatch == null) {
				if (patchIndex.getArchive(var8) != null) {
					byte[] var11 = patchIndex.getArchive(var8).getFile(0).getData();
					MusicPatch var10;
					if (var11 == null) {
						var10 = null;
					} else {
						var10 = new MusicPatch(var11);
					}

					musicPatch = var10;
					if (var10 == null) {
						reset = false;
						continue;
					}

					this.musicPatches.put(var10, var8);

					if (!musicPatch.method4945(soundCache, var7.byteArray, sampleRates)) {
						reset = false;
					}
				}
			}
		}

		if (reset) {
			musicTrack.clear();
		}

		return reset;
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
			int beatsPerMinute = this.midiFile.resolution * this.tempo / AppConstants.sampleRate;

			do {
				long var5 = this.microsecondLength + (long) beatsPerMinute * (long)var3;
				if (this.microsecondPosition - var5 >= 0L) {
					this.microsecondLength = var5;
					break;
				}

				int var7 = (int)((this.microsecondPosition - this.microsecondLength + (long) beatsPerMinute - 1L) / (long) beatsPerMinute);
				this.microsecondLength += (long)var7 * (long) beatsPerMinute;
				this.patchStream.fill(var1, var2, var7);
				var2 += var7;
				var3 -= var7;
				this.method4758();
			} while(this.midiFile.isReady());
		}

		this.patchStream.fill(var1, var2, var3);
	}

	public synchronized void setMusicTrack(MusicTrack musicTrack, boolean musicLoop) {
		this.clear();
		this.midiFile.parse(musicTrack.midi);
		this.isLooping = musicLoop;
		this.microsecondLength = 0L;
		int var3 = this.midiFile.trackCount();

		for (int var4 = 0; var4 < var3; ++var4) {
			this.midiFile.gotoTrack(var4);
			this.midiFile.readTrackLength(var4);
			this.midiFile.markTrackPosition(var4);
		}

		this.track = this.midiFile.getPrioritizedTrack();
		this.trackLength = this.midiFile.trackLengths[this.track];
		this.microsecondPosition = this.midiFile.method4934(this.trackLength);
	}

	public synchronized void skip(int var1) {
		if (this.midiFile.isReady()) {
			int var2 = this.midiFile.resolution * this.tempo / AppConstants.sampleRate;

			do {
				long var3 = (long)var1 * (long)var2 + this.microsecondLength;
				if (this.microsecondPosition - var3 >= 0L) {
					this.microsecondLength = var3;
					break;
				}

				int var5 = (int)(((long)var2 + (this.microsecondPosition - this.microsecondLength) - 1L) / (long)var2);
				this.microsecondLength += (long)var5 * (long)var2;
				this.patchStream.skip(var5);
				var1 -= var5;
				this.method4758();
			} while(this.midiFile.isReady());
		}

		this.patchStream.skip(var1);
	}

	public synchronized void clear() {
		this.midiFile.clear();
		this.systemReset();
	}

	public synchronized boolean isReady() {
		return this.midiFile.isReady();
	}

	public synchronized void setInitialPatch(int channel, int patch) {
		this.setPatch(channel, patch);
	}

	void setPatch(int channel, int patch) {
		this.programConstants[channel] = patch;
		this.bankControls[channel] = patch & -128;
		this.programChange(channel, patch);
	}

	void programChange(int channel, int program) {
		if (program != this.patch[channel]) {
			this.patch[channel] = program;

			for (int note = 0; note < 128; ++note) {
				this.oneShotVoices[channel][note] = null;
			}
		}

	}

	void noteOn(int channel, int data1, int data2) {
		this.noteOff(channel, data1, 64);
		if ((this.switchControls[channel] & 2) != 0) {
			for (MusicPatchNode var4 = (MusicPatchNode)this.patchStream.queue.first(); var4 != null; var4 = (MusicPatchNode)this.patchStream.queue.next()) {
				if (var4.midiChannel == channel && var4.releasePosition < 0) {
					this.loopedVoices[channel][var4.midiNote] = null;
					this.loopedVoices[channel][data1] = var4;
					int var8 = (var4.field2998 * var4.field2997 >> 12) + var4.soundTransposition;
					var4.soundTransposition += data1 - var4.midiNote << 8;
					var4.field2997 = var8 - var4.soundTransposition;
					var4.field2998 = 4096;
					var4.midiNote = data1;
					return;
				}
			}
		}

		MusicPatch var9 = (MusicPatch)this.musicPatches.get(this.patch[channel]);
		if (var9 != null) {
			AudioDataSource var5 = var9.audioDataSources[data1];
			if (var5 != null) {
				MusicPatchNode var6 = new MusicPatchNode();
				var6.midiChannel = channel;
				var6.patch = var9;
				var6.audioDataSource = var5;
				var6.musicPatchNode2 = var9.musicPatchParameters[data1];
				var6.loopType = var9.loopOffset[data1];
				var6.midiNote = data1;
				var6.midiNoteVolume = data2 * data2 * var9.volumeOffset[data1] * var9.volume + 1024 >> 11;
				var6.field2992 = var9.panOffset[data1] & 255;
				var6.soundTransposition = (data1 << 8) - (var9.pitchOffset[data1] & 32767);
				var6.field2986 = 0;
				var6.volumeEnvelopePosition = 0;
				var6.positionOffset = 0;
				var6.releasePosition = -1;
				var6.releaseOffset = 0;
				if (this.sampleLoopControls[channel] == 0) {
					var6.stream = RawPcmStream.createSampledRawPcmStream(var5, this.calculatePitch(var6), this.calculateVolume(var6), this.calculatePanning(var6));
				} else {
					var6.stream = RawPcmStream.createSampledRawPcmStream(var5, this.calculatePitch(var6), 0, this.calculatePanning(var6));
					this.method4765(var6, var9.pitchOffset[data1] < 0);
				}

				if (var9.pitchOffset[data1] < 0) {
					if (var6.stream != null) {
						var6.stream.setNumLoops(-1);
					}
				}

				if (var6.loopType >= 0) {
					MusicPatchNode var7 = this.oneShotVoices[channel][var6.loopType];
					if (var7 != null && var7.releasePosition < 0) {
						this.loopedVoices[channel][var7.midiNote] = null;
						var7.releasePosition = 0;
					}

					this.oneShotVoices[channel][var6.loopType] = var6;
				}

				this.patchStream.queue.addFirst(var6);
				this.loopedVoices[channel][data1] = var6;
			}
		}
	}

	void method4765(MusicPatchNode musicPatchNode, boolean validPitch) {
		int var3 = musicPatchNode.audioDataSource.audioData.length;
		int var4;
		if (validPitch && musicPatchNode.audioDataSource.isLooping) {
			int var5 = var3 + var3 - musicPatchNode.audioDataSource.loopStart;
			var4 = (int)((long)this.sampleLoopControls[musicPatchNode.midiChannel] * (long)var5 >> 6);
			var3 <<= 8;
			if (var4 >= var3) {
				var4 = var3 + var3 - 1 - var4;
				musicPatchNode.stream.method922();
			}
		} else {
			var4 = (int)((long)var3 * (long)this.sampleLoopControls[musicPatchNode.midiChannel] >> 6);
		}

		musicPatchNode.stream.method825(var4);
	}

	void noteOff(int channel, int data1, int data2) {
		MusicPatchNode var4 = this.loopedVoices[channel][data1];
		if (var4 != null) {
			this.loopedVoices[channel][data1] = null;
			if ((this.switchControls[channel] & 2) != 0) {
				for (MusicPatchNode var5 = (MusicPatchNode)this.patchStream.queue.last(); var5 != null; var5 = (MusicPatchNode)this.patchStream.queue.previous()) {
					if (var4.midiChannel == var5.midiChannel && var5.releasePosition < 0 && var4 != var5) {
						var4.releasePosition = 0;
						break;
					}
				}
			} else {
				var4.releasePosition = 0;
			}

		}
	}

	void polyPressure(int var1, int var2, int var3) {
	}

	void channelPressure(int var1, int var2) {
	}

	void pitchBend(int var1, int var2) {
		this.pitchBendControls[var1] = var2;
	}

	void allSoundOff(int var1) {
		for (MusicPatchNode var2 = (MusicPatchNode)this.patchStream.queue.last(); var2 != null; var2 = (MusicPatchNode)this.patchStream.queue.previous()) {
			if (var1 < 0 || var2.midiChannel == var1) {
				if (var2.stream != null) {
					var2.stream.method830(AppConstants.sampleRate / 100);
					if (var2.stream.method834()) {
						this.patchStream.mixer.addSubStream(var2.stream);
					}

					var2.reset();
				}

				if (var2.releasePosition < 0) {
					this.loopedVoices[var2.midiChannel][var2.midiNote] = null;
				}

				var2.remove();
			}
		}

	}

	void resetAllControllers(int channel) {
		if (channel >= 0) {
			this.volumeControls[channel] = 12800;
			this.panControls[channel] = 8192;
			this.expressionControls[channel] = 16383;
			this.pitchBendControls[channel] = 8192;
			this.modulationControls[channel] = 0;
			this.portamentoTimeControls[channel] = 8192;
			this.setReverb(channel);
			this.method4775(channel);
			this.switchControls[channel] = 0;
			this.dataEntriesMSB[channel] = 32767;
			this.dataEntriesLSB[channel] = 256;
			this.sampleLoopControls[channel] = 0;
			this.retrigger(channel, 8192);
		} else {
			for (channel = 0; channel < 16; ++channel) {
				this.resetAllControllers(channel);
			}

		}
	}

	void allNotesOff(int var1) {
		for (MusicPatchNode var2 = (MusicPatchNode)this.patchStream.queue.last(); var2 != null; var2 = (MusicPatchNode)this.patchStream.queue.previous()) {
			if ((var1 < 0 || var2.midiChannel == var1) && var2.releasePosition < 0) {
				this.loopedVoices[var2.midiChannel][var2.midiNote] = null;
				var2.releasePosition = 0;
			}
		}

	}

	public void systemReset() {
		this.allSoundOff(-1);
		this.resetAllControllers(-1);

		int var1;
		for (var1 = 0; var1 < 16; ++var1) {
			this.patch[var1] = this.programConstants[var1];
		}

		for (var1 = 0; var1 < 16; ++var1) {
			this.bankControls[var1] = this.programConstants[var1] & -128;
		}

	}

	void setReverb(int var1) {
		if ((this.switchControls[var1] & 2) != 0) {
			for (MusicPatchNode var2 = (MusicPatchNode)this.patchStream.queue.last(); var2 != null; var2 = (MusicPatchNode)this.patchStream.queue.previous()) {
				if (var2.midiChannel == var1 && this.loopedVoices[var1][var2.midiNote] == null && var2.releasePosition < 0) {
					var2.releasePosition = 0;
				}
			}
		}

	}

	void method4775(int var1) {
		if ((this.switchControls[var1] & 4) != 0) {
			for (MusicPatchNode var2 = (MusicPatchNode) this.patchStream.queue.last(); var2 != null; var2 = (MusicPatchNode) this.patchStream.queue.previous()) {
				if (var2.midiChannel == var1) {
					var2.field3003 = 0;
				}
			}
		}

	}

	void dispatchEvent(int message) {
		int command = message & 240;
		int channel;
		int data1;
		int data2;
		if (command == 128) {
			channel = message & 15;
			data1 = message >> 8 & 127;
			data2 = message >> 16 & 127;
			this.noteOff(channel, data1, data2);
		} else if (command == 144) {
			channel = message & 15;
			data1 = message >> 8 & 127;
			data2 = message >> 16 & 127;
			if (data2 > 0) {
				this.noteOn(channel, data1, data2);
			} else {
				this.noteOff(channel, data1, 64);
			}

		} else if (command == 160) {
			channel = message & 15;
			data1 = message >> 8 & 127;
			data2 = message >> 16 & 127;
			this.polyPressure(channel, data1, data2);
		} else if (command == 176) {
			channel = message & 15;
			data1 = message >> 8 & 127;
			data2 = message >> 16 & 127;
			if (data1 == 0) {
				this.bankControls[channel] = (data2 << 14) + (this.bankControls[channel] & -2080769);
			}

			if (data1 == 32) {
				this.bankControls[channel] = (data2 << 7) + (this.bankControls[channel] & -16257);
			}

			if (data1 == 1) {
				this.modulationControls[channel] = (data2 << 7) + (this.modulationControls[channel] & -16257);
			}

			if (data1 == 33) {
				this.modulationControls[channel] = data2 + (this.modulationControls[channel] & -128);
			}

			if (data1 == 5) {
				this.portamentoTimeControls[channel] = (data2 << 7) + (this.portamentoTimeControls[channel] & -16257);
			}

			if (data1 == 37) {
				this.portamentoTimeControls[channel] = data2 + (this.portamentoTimeControls[channel] & -128);
			}

			if (data1 == 7) {
				this.volumeControls[channel] = (data2 << 7) + (this.volumeControls[channel] & -16257);
			}

			if (data1 == 39) {
				this.volumeControls[channel] = data2 + (this.volumeControls[channel] & -128);
			}

			if (data1 == 10) {
				this.panControls[channel] = (data2 << 7) + (this.panControls[channel] & -16257);
			}

			if (data1 == 42) {
				this.panControls[channel] = data2 + (this.panControls[channel] & -128);
			}

			if (data1 == 11) {
				this.expressionControls[channel] = (data2 << 7) + (this.expressionControls[channel] & -16257);
			}

			if (data1 == 43) {
				this.expressionControls[channel] = data2 + (this.expressionControls[channel] & -128);
			}

			int[] values;
			if (data1 == 64) {
				if (data2 >= 64) {
					values = this.switchControls;
					values[channel] |= 1;
				} else {
					values = this.switchControls;
					values[channel] &= -2;
				}
			}

			if (data1 == 65) {
				if (data2 >= 64) {
					values = this.switchControls;
					values[channel] |= 2;
				} else {
					this.setReverb(channel);
					values = this.switchControls;
					values[channel] &= -3;
				}
			}

			if (data1 == 99) {
				this.dataEntriesMSB[channel] = (data2 << 7) + (this.dataEntriesMSB[channel] & 127);
			}

			if (data1 == 98) {
				this.dataEntriesMSB[channel] = (this.dataEntriesMSB[channel] & 16256) + data2;
			}

			if (data1 == 101) {
				this.dataEntriesMSB[channel] = (data2 << 7) + (this.dataEntriesMSB[channel] & 127) + 16384;
			}

			if (data1 == 100) {
				this.dataEntriesMSB[channel] = (this.dataEntriesMSB[channel] & 16256) + data2 + 16384;
			}

			if (data1 == 120) {
				this.allSoundOff(channel);
			}

			if (data1 == 121) {
				this.resetAllControllers(channel);
			}

			if (data1 == 123) {
				this.allNotesOff(channel);
			}

			int dataEntry;
			if (data1 == 6) {
				dataEntry = this.dataEntriesMSB[channel];
				if (dataEntry == 16384) {
					this.dataEntriesLSB[channel] = (data2 << 7) + (this.dataEntriesLSB[channel] & -16257);
				}
			}

			if (data1 == 38) {
				dataEntry = this.dataEntriesMSB[channel];
				if (dataEntry == 16384) {
					this.dataEntriesLSB[channel] = data2 + (this.dataEntriesLSB[channel] & -128);
				}
			}

			if (data1 == 16) {
				this.sampleLoopControls[channel] = (data2 << 7) + (this.sampleLoopControls[channel] & -16257);
			}

			if (data1 == 48) {
				this.sampleLoopControls[channel] = data2 + (this.sampleLoopControls[channel] & -128);
			}

			if (data1 == 81) {
				if (data2 >= 64) {
					values = this.switchControls;
					values[channel] |= 4;
				} else {
					this.method4775(channel);
					values = this.switchControls;
					values[channel] &= -5;
				}
			}

			if (data1 == 17) {
				this.retrigger(channel, (data2 << 7) + (this.retriggerControls[channel] & -16257));
			}

			if (data1 == 49) {
				this.retrigger(channel, data2 + (this.retriggerControls[channel] & -128));
			}

		} else if (command == 192) {
			channel = message & 15;
			data1 = message >> 8 & 127;
			this.programChange(channel, data1 + this.bankControls[channel]);
		} else if (command == 208) {
			channel = message & 15;
			data1 = message >> 8 & 127;
			this.channelPressure(channel, data1);
		} else if (command == 224) {
			channel = message & 15;
			data1 = (message >> 8 & 127) + (message >> 9 & 16256);
			this.pitchBend(channel, data1);
		} else {
			command = message & 255;
			if (command == 255) {
				this.systemReset();
			}
		}
	}

	void retrigger(int channel, int data) {
		this.retriggerControls[channel] = data;
		this.retriggerEffects[channel] = (int) (2097152.0D * Math.pow(2.0D, 5.4931640625E-4D * (double) data) + 0.5D);
	}

	int calculatePitch(MusicPatchNode var1) {
		int var2 = (var1.field2998 * var1.field2997 >> 12) + var1.soundTransposition;
		var2 += (this.pitchBendControls[var1.midiChannel] - 8192) * this.dataEntriesLSB[var1.midiChannel] >> 12;
		MusicPatchNode2 var3 = var1.musicPatchNode2;
		int var4;
		if (var3.vibratoFrequencyHertz > 0 && (var3.vibratoPitchModulatorCents > 0 || this.modulationControls[var1.midiChannel] > 0)) {
			var4 = var3.vibratoPitchModulatorCents << 2;
			int var5 = var3.vibratoDelayMilliseconds << 1;
			if (var1.field3001 < var5) {
				var4 = var4 * var1.field3001 / var5;
			}

			var4 += this.modulationControls[var1.midiChannel] >> 7;
			double var6 = Math.sin(0.01227184630308513D * (double)(var1.field3002 & 511));
			var2 += (int)(var6 * (double)var4);
		}

		var4 = (int)((double)(var1.audioDataSource.sampleRate * 256) * Math.pow(2.0D, (double)var2 * 3.255208333333333E-4D) / (double) AppConstants.sampleRate + 0.5D);
		return Math.max(var4, 1);
	}

	int calculateVolume(MusicPatchNode var1) {
		MusicPatchNode2 var2 = var1.musicPatchNode2;
		int var3 = this.expressionControls[var1.midiChannel] * this.volumeControls[var1.midiChannel] + 4096 >> 13;
		var3 = var3 * var3 + 16384 >> 15;
		var3 = var3 * var1.midiNoteVolume + 16384 >> 15;
		var3 = var3 * this.volume + 128 >> 8;
		if (var2.sustain > 0) {
			var3 = (int)((double)var3 * Math.pow(0.5D, (double)var2.sustain * (double)var1.field2986 * 1.953125E-5D) + 0.5D);
		}

		int var4;
		int var5;
		int var6;
		int var7;
		if (var2.attackEnvelope != null) {
			var4 = var1.volumeEnvelopePosition;
			var5 = var2.attackEnvelope[var1.positionOffset + 1];
			if (var1.positionOffset < var2.attackEnvelope.length - 2) {
				var6 = (var2.attackEnvelope[var1.positionOffset] & 255) << 8;
				var7 = (var2.attackEnvelope[var1.positionOffset + 2] & 255) << 8;
				var5 += (var4 - var6) * (var2.attackEnvelope[var1.positionOffset + 3] - var5) / (var7 - var6);
			}

			var3 = var3 * var5 + 32 >> 6;
		}

		if (var1.releasePosition > 0 && var2.decayEnvelope != null) {
			var4 = var1.releasePosition;
			var5 = var2.decayEnvelope[var1.releaseOffset + 1];
			if (var1.releaseOffset < var2.decayEnvelope.length - 2) {
				var6 = (var2.decayEnvelope[var1.releaseOffset] & 255) << 8;
				var7 = (var2.decayEnvelope[var1.releaseOffset + 2] & 255) << 8;
				var5 += (var2.decayEnvelope[var1.releaseOffset + 3] - var5) * (var4 - var6) / (var7 - var6);
			}

			var3 = var5 * var3 + 32 >> 6;
		}

		return var3;
	}

	int calculatePanning(MusicPatchNode var1) {
		int var2 = this.panControls[var1.midiChannel];
		return var2 < 8192 ? var2 * var1.field2992 + 32 >> 6 : 16384 - ((128 - var1.field2992) * (16384 - var2) + 32 >> 6);
	}

	void method4758() {
		int trackNumber = this.track;
		int trackCount = this.trackLength;

		long position;
		for (position = this.microsecondPosition; trackCount == this.trackLength; position = this.midiFile.method4934(trackCount)) {
			while (trackCount == this.midiFile.trackLengths[trackNumber]) {
				this.midiFile.gotoTrack(trackNumber);
				int midiMessage = this.midiFile.readMessage(trackNumber);
				if (midiMessage == 1) {
					this.midiFile.setTrackDone();
					this.midiFile.markTrackPosition(trackNumber);
					if (this.midiFile.isDone()) {
						if (!this.isLooping || trackCount == 0) {
							this.systemReset();
							this.midiFile.clear();
							return;
						}

						this.midiFile.reset(position);
					}
					break;
				}

				if ((midiMessage & 128) != 0) {
					this.dispatchEvent(midiMessage);
				}

				this.midiFile.readTrackLength(trackNumber);
				this.midiFile.markTrackPosition(trackNumber);
			}

			trackNumber = this.midiFile.getPrioritizedTrack();
			trackCount = this.midiFile.trackLengths[trackNumber];
		}

		this.track = trackNumber;
		this.trackLength = trackCount;
		this.microsecondPosition = position;
	}

	boolean method4787(MusicPatchNode var1) {
		if (var1.stream == null) {
			if (var1.releasePosition >= 0) {
				var1.remove();
				if (var1.loopType > 0 && var1 == this.oneShotVoices[var1.midiChannel][var1.loopType]) {
					this.oneShotVoices[var1.midiChannel][var1.loopType] = null;
				}
			}

			return true;
		} else {
			return false;
		}
	}

	boolean method4788(MusicPatchNode musicPatchNode, int[] var2, int var3, int var4) {
		musicPatchNode.field2995 = AppConstants.sampleRate / 100;
		if (musicPatchNode.releasePosition < 0 || musicPatchNode.stream != null && !musicPatchNode.stream.method833()) {
			int var5 = musicPatchNode.field2998;
			if (var5 > 0) {
				var5 -= (int)(16.0D * Math.pow(2.0D, 4.921259842519685E-4D * (double)this.portamentoTimeControls[musicPatchNode.midiChannel]) + 0.5D);
				if (var5 < 0) {
					var5 = 0;
				}

				musicPatchNode.field2998 = var5;
			}

			musicPatchNode.stream.method912(this.calculatePitch(musicPatchNode));
			MusicPatchNode2 var6 = musicPatchNode.musicPatchNode2;
			boolean var7 = false;
			++musicPatchNode.field3001;
			musicPatchNode.field3002 += var6.vibratoFrequencyHertz;
			double var8 = 5.086263020833333E-6D * (double)((musicPatchNode.midiNote - 60 << 8) + (musicPatchNode.field2997 * musicPatchNode.field2998 >> 12));
			if (var6.sustain > 0) {
				if (var6.field2912 > 0) {
					musicPatchNode.field2986 += (int)(128.0D * Math.pow(2.0D, (double)var6.field2912 * var8) + 0.5D);
				} else {
					musicPatchNode.field2986 += 128;
				}
			}

			if (var6.attackEnvelope != null) {
				if (var6.field2918 > 0) {
					musicPatchNode.volumeEnvelopePosition += (int)(128.0D * Math.pow(2.0D, var8 * (double)var6.field2918) + 0.5D);
				} else {
					musicPatchNode.volumeEnvelopePosition += 128;
				}

				while (musicPatchNode.positionOffset < var6.attackEnvelope.length - 2 && musicPatchNode.volumeEnvelopePosition > (var6.attackEnvelope[musicPatchNode.positionOffset + 2] & 255) << 8) {
					musicPatchNode.positionOffset += 2;
				}

				if (var6.attackEnvelope.length - 2 == musicPatchNode.positionOffset && var6.attackEnvelope[musicPatchNode.positionOffset + 1] == 0) {
					var7 = true;
				}
			}

			if (musicPatchNode.releasePosition >= 0 && var6.decayEnvelope != null && (this.switchControls[musicPatchNode.midiChannel] & 1) == 0 && (musicPatchNode.loopType < 0 || musicPatchNode != this.oneShotVoices[musicPatchNode.midiChannel][musicPatchNode.loopType])) {
				if (var6.release > 0) {
					musicPatchNode.releasePosition += (int)(128.0D * Math.pow(2.0D, var8 * (double)var6.release) + 0.5D);
				} else {
					musicPatchNode.releasePosition += 128;
				}

				while (musicPatchNode.releaseOffset < var6.decayEnvelope.length - 2 && musicPatchNode.releasePosition > (var6.decayEnvelope[musicPatchNode.releaseOffset + 2] & 255) << 8) {
					musicPatchNode.releaseOffset += 2;
				}

				if (var6.decayEnvelope.length - 2 == musicPatchNode.releaseOffset) {
					var7 = true;
				}
			}

			if (var7) {
				musicPatchNode.stream.method830(musicPatchNode.field2995);
				if (var2 != null) {
					musicPatchNode.stream.fill(var2, var3, var4);
				} else {
					musicPatchNode.stream.skip(var4);
				}

				if (musicPatchNode.stream.method834()) {
					this.patchStream.mixer.addSubStream(musicPatchNode.stream);
				}

				musicPatchNode.reset();
				if (musicPatchNode.releasePosition >= 0) {
					musicPatchNode.remove();
					if (musicPatchNode.loopType > 0 && musicPatchNode == this.oneShotVoices[musicPatchNode.midiChannel][musicPatchNode.loopType]) {
						this.oneShotVoices[musicPatchNode.midiChannel][musicPatchNode.loopType] = null;
					}
				}

				return true;
			} else {
				musicPatchNode.stream.method829(musicPatchNode.field2995, this.calculateVolume(musicPatchNode), this.calculatePanning(musicPatchNode));
				return false;
			}
		} else {
			musicPatchNode.reset();
			musicPatchNode.remove();
			if (musicPatchNode.loopType > 0 && musicPatchNode == this.oneShotVoices[musicPatchNode.midiChannel][musicPatchNode.loopType]) {
				this.oneShotVoices[musicPatchNode.midiChannel][musicPatchNode.loopType] = null;
			}

			return true;
		}
	}

	public void loadSoundFonts(String soundFontPath, int channel) {
		if (musicPatches != null) {
			for (MusicPatch musicPatch = (MusicPatch) musicPatches.first(); musicPatch != null; musicPatch = (MusicPatch) musicPatches.next()) {
				try {
					if (new File(soundFontPath + File.separator + musicPatch.key + ".sf2").exists()) {
						musicPatch.mapSoundFontSamples((int) musicPatch.key, channel, new SF2Soundbank(new File(soundFontPath + File.separator + musicPatch.key + ".sf2")));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
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
