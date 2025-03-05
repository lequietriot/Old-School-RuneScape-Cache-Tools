package osrs;

import com.displee.cache.index.Index;

import java.util.Objects;

public class SoundCache {

	Index soundEffectIndex;
	Index musicSampleIndex;
	NodeHashTable musicSamples;
	NodeHashTable rawSounds;

	public SoundCache(Index var1, Index var2) {
		this.musicSamples = new NodeHashTable(256);
		this.rawSounds = new NodeHashTable(256);
		this.soundEffectIndex = var1;
		this.musicSampleIndex = var2;
	}

	AudioDataSource getSoundEffect0(int var1, int var2, int[] var3) {
		int var4 = var2 ^ (var1 << 4 & 65535 | var1 >>> 12);
		var4 |= var1 << 16;
		long var5 = var4;
		AudioDataSource var7 = (AudioDataSource)this.rawSounds.get(var5);
		if (var7 != null) {
			return var7;
		} else if (var3 != null && var3[0] <= 0) {
			return null;
		} else {
			SoundEffect var8 = SoundEffect.readSoundEffect(this.soundEffectIndex, var1, var2);
			if (var8 == null) {
				return null;
			} else {
				var7 = var8.toRawSound();
				this.rawSounds.put(var7, var5);
				if (var3 != null) {
					var3[0] -= var7.audioData.length;
				}

				return var7;
			}
		}
	}

	AudioDataSource getMusicSample0(int var1, int var2, int[] var3) {
		int var4 = var2 ^ (var1 << 4 & 65535 | var1 >>> 12);
		var4 |= var1 << 16;
		long var5 = (long)var4 ^ 4294967296L;
		AudioDataSource var7 = (AudioDataSource)this.rawSounds.get(var5);
		if (var7 != null) {
			return var7;
		} else if (var3 != null && var3[0] <= 0) {
			return null;
		} else {
			VorbisSample var8 = (VorbisSample)this.musicSamples.get(var5);
			if (var8 == null) {
				var8 = VorbisSample.readVorbisSample(this.musicSampleIndex, var1, var2);
				if (var8 == null) {
					return null;
				}

				this.musicSamples.put(var8, var5);
			}

			var7 = var8.toRawSound(var3);
			if (var7 == null) {
				return null;
			} else {
				var8.remove();
				this.rawSounds.put(var7, var5);
				return var7;
			}
		}
	}

	public AudioDataSource getSoundEffect(int var1, int[] var2) {
		if (this.soundEffectIndex.archives().length == 1) {
			return this.getSoundEffect0(0, var1, var2);
		} else if (Objects.requireNonNull(this.soundEffectIndex.archive(var1)).files().length == 1) {
			return this.getSoundEffect0(var1, 0, var2);
		} else {
			throw new RuntimeException();
		}
	}

	public AudioDataSource getMusicSample(int var1, int[] var2) {
		if (musicSampleIndex.archive(var1) != null) {
			if (this.musicSampleIndex.archives().length == 1) {
				return this.getMusicSample0(0, var1, var2);
			} else if (Objects.requireNonNull(this.musicSampleIndex.archive(var1)).files().length == 1) {
				return this.getMusicSample0(var1, 0, var2);
			} else {
				throw new RuntimeException();
			}
		}
		return new AudioDataSource(0, new byte[0], 0, 0);
	}

}
