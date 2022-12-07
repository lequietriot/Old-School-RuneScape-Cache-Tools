/*
 * (C) Copyright IBM Corp. 2005, 2008
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package synth.engine;

import javax.sound.sampled.AudioFormat;

import static synth.utils.AudioUtils.isAlmost;


/**
 * Class for creating audio samples from the instrument data. The oscillator
 * takes care of sample rate conversion and looping.
 * <p>
 * The oscillator is per definition a mono audio source. If the native audio
 * data exists in stereo format, 2 oscillators need to be created that provides
 * mono data each.
 *
 * @author florian
 *
 */

public abstract class Oscillator {

	public static boolean DEBUG_OSC = false;

	/**
	 * Value for loopMode: Ignore loopStart and loopEnd and do not perform any
	 * looping
	 */
	public final static int LOOPMODE_NONE = 0;

	/**
	 * Value for loopMode: Everytime when playback reaches loopEnd, jump back to
	 * loopStart.
	 */
	public final static int LOOPMODE_CONTINOUSLY = 1;

	/**
	 * Value for loopMode: Everytime when playback reaches loopEnd, jump back to
	 * loopStart, until the release segment is entered, in which case the loop
	 * is continued to the end of the loop, and then the remainder of the
	 * waveform is played until nativeSamplesEndPos.
	 */
	public final static int LOOPMODE_UNTIL_RELEASE = 2;

	/**
	 * The current read position in the native samples array nativeSamples. This
	 * is a sample position, not an array index.
	 */
	protected double nativePos;

	/**
	 * how much nativePos must be increased per outgoing sample
	 */
	protected double nativePosDelta;

	/**
	 * The sample rate of the native audio data in nativeSamples.
	 */
	protected double nativeSampleRate;

	/**
	 * input frame Size, including number of channels, i.e. 4 for 16-bit stereo
	 */
	protected int nativeSampleSize;

	/**
	 * how many channels in native data
	 */
	protected int nativeChannels;

	/**
	 * code for conversion tool
	 */
	protected int nativeFormatCode;

	/**
	 * The actual audio data to read from
	 */
	protected byte[] nativeSamples;

	/**
	 * The first sample that can be read in the native array This is a sample
	 * position, not an array index.
	 */
	protected int nativeSamplesStartPos;

	/**
	 * One after last sample that can be read in the native array This is a
	 * sample position, not an array index.
	 */
	protected int nativeSamplesEndPos;

	/**
	 * The sample rate of the out buffer as provided by the caller in convert()
	 */
	protected double outSampleRate;

	/**
	 * The sample rate factor as provided by the caller in convert()
	 */
	protected double sampleRateFactor;

	/**
	 * The internal sample rate factor that combines nativeSampleRate,
	 * outSampleRate, and samplerateFactor
	 */
	protected double effectiveSampleRateFactor;

	/**
	 * The loop mode (see the LOOPMODE_ constants above). This field is modified
	 * when playback reaches the release segment: if loopMode is
	 * LOOPMODE_UNTIL_RELEASE, it will be changed to LOOPMODE_NONE.
	 */
	protected int loopMode = LOOPMODE_NONE;

	/**
	 * The loop start index into the nativeSamples array. This is a sample
	 * position, not an array index.
	 */
	protected double loopStart;

	/**
	 * The loop end index into the nativeSamples array. This index is one after
	 * the last sample to be played. This is a sample position, not an array
	 * index.
	 */
	protected double loopEnd;

	/**
	 * Protected default constructor so that descending classes can instantiate
	 * an instance without using the public simple constructor below.
	 */
	protected Oscillator() {
		// nothing
	}

	protected void setNativeAudioFormat(AudioFormat format) {
		this.nativeSampleRate = format.getSampleRate();
		this.nativeFormatCode = ConversionTool.getFormatType(format);
		this.nativeSampleSize = format.getFrameSize();
		this.nativeChannels = format.getChannels();
	}

	protected void setNativeAudioFormat(double sampleRate, int bitsPerSample,
			int bytesPerSample, int channels, boolean signed, boolean bigEndian) {
		this.nativeSampleRate = sampleRate;
		this.nativeFormatCode =
				ConversionTool.getFormatType(bitsPerSample, bytesPerSample, signed, bigEndian);
		this.nativeSampleSize = ((bitsPerSample + 7) / 8) * channels;
		this.nativeChannels = channels;
	}

	/**
	 * Initialization of state variables. Should be called when setting the
	 * parameters is done.
	 */
	public void init() {
		this.nativePos = nativeSamplesStartPos;
	}

	/**
	 * Writes count samples to the buffer at position offset. Samples in buffer
	 * are always overwritten. If there are not enough native samples to fill
	 * the buffer, the return value is lower than count.
	 * 
	 * @param buffer the buffer to be written to
	 * @param offset the offset in samples where to write to buffer
	 * @param count the number of samples to write to buffer
	 * @param sampleRateFactor the factor to be applied in addition to the
	 *            conversion from nativeSampleRate to buffer.getSampleRate().
	 * @return the number of samples written to buffer
	 */
	public int convert(AudioBuffer buffer, int offset, int count,
			double sampleRateFactor) {
		// first check if the output sample rate has changed. If so, init
		if (buffer.getSampleRate() != outSampleRate
				|| sampleRateFactor != this.sampleRateFactor) {
			initSampleRate(buffer.getSampleRate(), sampleRateFactor);
		}
		if (sampleRateFactor == 0.0 || count == 0) {
			return 0;
		}

		// how many samples are actually converted
		int converted = 0;
		int endPos = nativeSamplesEndPos;
		if (loopMode != LOOPMODE_NONE) {
			endPos = (int) loopEnd;
		}
		do {
			// how many samples can be converted?
			// note: for the linear converter, always one more sample needs to
			// be read!
			int thisCount = count - converted;
			double nextBufferNativePos =
					nativePos + (thisCount * nativePosDelta);

			if ((((int) nextBufferNativePos) /* + 1 */) >= endPos) {
				thisCount =
						(int) ((((double) endPos /*- 1*/) - nativePos) / nativePosDelta);

				if (loopMode == LOOPMODE_NONE) {
					// set to an impossible high value so that next calls to
					// convert()
					// will not try to read more samples
					nextBufferNativePos = endPos + 1000;
					count = thisCount;

					if (thisCount <= 0) {
						nativePos = nextBufferNativePos;
						return 0;
					}
				} else {
					// looping back
					nextBufferNativePos =
							nativePos + (thisCount * nativePosDelta)
									- (loopEnd - loopStart);
					if (DEBUG_OSC) {
						System.out.println("loop: nativePos="
								+ (nativePos - nativeSamplesStartPos)
								+ " thisCount="
								+ thisCount
								+ " nextNativePos="
								+ ((nativePos + (thisCount * nativePosDelta)) - nativeSamplesStartPos)
								+ " loopStart="
								+ (loopStart - nativeSamplesStartPos)
								+ " loopEnd="
								+ (loopEnd - nativeSamplesStartPos)
								+ " nextLoopStart="
								+ (nextBufferNativePos - nativeSamplesStartPos));
					}
				}
			} else {
				if (DEBUG_OSC) {
					System.out.println("regular: nativePos="
							+ (nativePos - nativeSamplesStartPos)
							+ " thisCount=" + thisCount + " nextNativePos="
							+ (nextBufferNativePos - nativeSamplesStartPos)
							+ " loopStart="
							+ (loopStart - nativeSamplesStartPos) + " loopEnd="
							+ (loopEnd - nativeSamplesStartPos));
				}
			}

			// do the actual conversion
			convertOneBlock(buffer, offset, thisCount);
			converted += thisCount;
			offset += thisCount;
			if (DEBUG_OSC) {
				float sample1 =
						(float) ((int) (buffer.getChannel(0)[0] * 1000.0)) / 1000.0f;
				float sample2 =
						(float) ((int) (buffer.getChannel(0)[thisCount - 1] * 1000.0)) / 1000.0f;
				System.out.println("   after conversion: first sample=" + sample1
						+ " last sample = " + sample2);
			}

			// advance to the next native read position
			nativePos = nextBufferNativePos;
		} while (converted < count);

		return count;
	}

	/**
	 * Convert and write into buffer <code>count</code> output samples. The
	 * caller has already calculated how many samples can be read from the
	 * source audio data, so no further checks regarding <code>count</code>
	 * should be necessary.
	 * 
	 * @param buffer The buffer to which to write the samples
	 * @param offset at which sample position to write the samples
	 * @param count how many samples to write to buffer
	 */
	protected abstract void convertOneBlock(AudioBuffer buffer, int offset,
			int count);

	private void initSampleRate(double newSampleRate, double newSampleRateFactor) {
		if (isAlmost(newSampleRate, 0.0) || isAlmost(newSampleRateFactor, 0.0)) {
			sampleRateFactor = 0.0;
			outSampleRate = 0.0;
		} else {
			sampleRateFactor = newSampleRateFactor;
			outSampleRate = newSampleRate;
			// effectiveSampleRateFactor = newSampleRate / nativeSampleRate
			// * newSampleRateFactor;
			// nativePosDelta = 1.0 / effectiveSampleRateFactor;
			effectiveSampleRateFactor =
					nativeSampleRate / newSampleRate * newSampleRateFactor;
			nativePosDelta = effectiveSampleRateFactor;
		}
	}

	public void release(AudioTime time) {
		if (loopMode == LOOPMODE_UNTIL_RELEASE) {
			loopMode = LOOPMODE_NONE;
		}
	}

	public boolean endReached() {
		return (nativePos >= nativeSamplesEndPos);
	}

}
