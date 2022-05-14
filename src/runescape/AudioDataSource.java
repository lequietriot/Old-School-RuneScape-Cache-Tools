package runescape;

/**
 * This class holds all the sample data and information.
 * Source Code: https://github.com/bome/java-wavetable-synth
 * @author Rodolfo Ruiz-Velasco (https://github.com/lequietriot)
 */
public class AudioDataSource extends AudioDataPosition {

	/**
	 * The Sample Rate of the audioData
	 */
	public int sampleRate;
	/**
	 * The audio data
	 */
	public byte[] audioData;
	/**
	 * The loop start position of the audioData
	 */
	public int loopStart;
	/**
	 * The loop end position of the audioData
	 */
	public int loopEnd;

	/**
	 * Determines whether the audioData is looping or not
	 */
	public boolean isLooping;

	/**
	 * Constructs a new Audio Sample which does not loop by default.
	 * This is mainly used for Sound Effects.
	 * @param rate the Sample Rate of the Audio Data
	 * @param data the Audio Data
	 * @param start the sample Loop Start position
	 * @param end the sample Loop End position
	 */
	AudioDataSource(int rate, byte[] data, int start, int end) {
		this.sampleRate = rate;
		this.audioData = data;
		this.loopStart = start;
		this.loopEnd = end;
	}

	/**
	 * Constructs a new Audio Sample which may or may not loop by default.
	 * This is mainly used for the Vorbis Samples.
	 * @param rate the Sample Rate of the Audio Data
	 * @param data the Audio Data
	 * @param start the sample Loop Start position
	 * @param end the sample Loop End position
	 * @param loopEnabled if the sample loops or not
	 */
	AudioDataSource(int rate, byte[] data, int start, int end, boolean loopEnabled) {
		this.sampleRate = rate;
		this.audioData = data;
		this.loopStart = start;
		this.loopEnd = end;
		this.isLooping = loopEnabled;
	}

	/**
	 * Resamples the Audio Data to match the output Sample Rate
	 * @param resampler the resampler
	 * @return returns the new Audio Data
	 */
	@Deprecated
	public AudioDataSource resample(Resampler resampler) {
		this.audioData = resampler.resample(this.audioData);
		this.sampleRate = resampler.scaleRate(this.sampleRate);
		if (this.loopStart == this.loopEnd) {
			this.loopStart = this.loopEnd = resampler.scalePosition(this.loopStart);
		} else {
			this.loopStart = resampler.scalePosition(this.loopStart);
			this.loopEnd = resampler.scalePosition(this.loopEnd);
			if (this.loopStart == this.loopEnd) {
				--this.loopStart;
			}
		}
		return this;
	}
}
