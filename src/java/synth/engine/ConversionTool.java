/*
 * Based on Tritonus: http://www.tritonus.org/
 */

/*
 *  Copyright (c) 2000,2004 by Florian Bomers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 |<---            this code is formatted to fit into 80 columns             --->|
 */

//package org.tritonus.share.sampled;
package synth.engine;

import javax.sound.sampled.AudioFormat;
import java.util.List;
import java.util.Random;

/**
 * Utility functions for handling data in normalized double arrays. Each sample
 * is linear in the range of [-1.0, +1.0].
 * <p>
 * Currently, the following bit sizes are supported:
 * <ul>
 * <li>8-bit
 * <li>16-bit
 * <li>packed 24-bit (stored in 3 bytes)
 * <li>32-bit
 * </ul>
 * 8-bit data can be unsigned or signed. All other data is only supported in
 * signed encoding.
 * 
 * @see AudioBuffer
 * @author Florian Bomers
 */
public class ConversionTool {

	/** default number of bits to be dithered: 0.7f */
	public static final double DEFAULT_DITHER_BITS = 0.7;

	private static Random random = null;

	// sample width (must be in order !)
	static final int F_8 = 1;

	static final int F_16 = 2;

	static final int F_24_3 = 3;

	static final int F_24_4 = 4;

	static final int F_32 = 5;

	static final int F_SAMPLE_WIDTH_MASK = F_8 | F_16 | F_24_3 | F_24_4 | F_32;

	// format bit-flags
	static final int F_SIGNED = 16;

	static final int F_BIGENDIAN = 32;

	// supported formats
	static final int CT_8S = F_8 | F_SIGNED;

	static final int CT_8U = F_8;

	static final int CT_16SB = F_16 | F_SIGNED | F_BIGENDIAN;

	static final int CT_16SL = F_16 | F_SIGNED;

	static final int CT_24_3SB = F_24_3 | F_SIGNED | F_BIGENDIAN;

	static final int CT_24_3SL = F_24_3 | F_SIGNED;

	static final int CT_24_4SB = F_24_4 | F_SIGNED | F_BIGENDIAN;

	static final int CT_24_4SL = F_24_4 | F_SIGNED;

	static final int CT_32SB = F_32 | F_SIGNED | F_BIGENDIAN;

	static final int CT_32SL = F_32 | F_SIGNED;

	// ////////////////////////////// initialization
	// /////////////////////////////// //

	/** prevent instanciation */
	private ConversionTool() {
	}

	// /////////////////// FORMAT / FORMAT TYPE
	// /////////////////////////////////// //

	/**
	 * only allow "packed" samples -- currently no support for 18, 20
	 * bits -- except 24 bits stored in 4 bytes.
	 * 
	 * @throws IllegalArgumentException
	 */
	static void checkSupportedSampleSize(int ssib, int channels, int frameSize) {
		if (ssib == 24 && frameSize == 4 * channels) {
			// 24 bits stored in 4 bytes is OK (24_4)
			return;
		}
		if ((ssib * channels) != frameSize * 8) {
			throw new IllegalArgumentException("unsupported sample size: "
					+ ssib + " bits stored in " + (frameSize / channels) + " bytes.");
		}
	}

	/**
	 * Get the formatType code from the given format.
	 * 
	 * @throws IllegalArgumentException
	 */
	static int getFormatType(AudioFormat format) {
		boolean signed =
				format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
		if (!signed
				&& !format.getEncoding().equals(
						AudioFormat.Encoding.PCM_UNSIGNED)) {
			throw new IllegalArgumentException(
					"unsupported encoding: only PCM encoding supported.");
		}
		if (!signed && format.getSampleSizeInBits() != 8) {
			throw new IllegalArgumentException(
					"unsupported encoding: only 8-bit can be unsigned");
		}
		checkSupportedSampleSize(format.getSampleSizeInBits(),
				format.getChannels(), format.getFrameSize());

		int formatType =
				getFormatType(format.getSampleSizeInBits(), 
						format.getFrameSize() / format.getChannels(), signed,
						format.isBigEndian());
		return formatType;
	}

	/**
	 * @throws IllegalArgumentException
	 */
	static int getFormatType(int ssib, int bytesPerSample, boolean signed,
			boolean bigEndian) {
		int res = 0;
		if (ssib == 24 || (bytesPerSample == ssib / 8)) {
			if (ssib == 8) {
				res = F_8;
			} else if (ssib == 16) {
				res = F_16;
			} else if (ssib == 24) {
				if (bytesPerSample == 3) {
					res = F_24_3;
				} else if (bytesPerSample == 4) {
					res = F_24_4;
				}
			} else if (ssib == 32) {
				res = F_32;
			}
		}
		if (res == 0) {
			throw new IllegalArgumentException(
					"ConversionTool: unsupported sample size of " + ssib
							+ " bits per sample in "+bytesPerSample+" bytes.");
		}
		if (!signed && bytesPerSample > 1) {
			throw new IllegalArgumentException(
					"ConversionTool: unsigned samples larger than "
							+ "8 bit are not supported");
		}
		if (signed) {
			res |= F_SIGNED;
		}
		if (bigEndian && (ssib != 8)) {
			res |= F_BIGENDIAN;
		}
		return res;
	}

	/**
	 * @return the storage size in bytes for the format type
	 */
	static int getSampleSize(int formatType) {
		switch (formatType & F_SAMPLE_WIDTH_MASK) {
		case F_8:
			return 1;
		case F_16:
			return 2;
		case F_24_3:
			return 3;
		case F_24_4:
			return 4;
		case F_32:
			return 4;
		}
		return 0;
	}

	/**
	 * Return a string representation of this format
	 */
	static String formatType2Str(int formatType) {
		String res = "" + formatType + ": ";
		switch (formatType & F_SAMPLE_WIDTH_MASK) {
		case F_8:
			res += "8bit";
			break;
		case F_16:
			res += "16bit";
			break;
		case F_24_3:
			res += "24_3bit";
			break;
		case F_24_4:
			res += "24_4bit";
			break;
		case F_32:
			res += "32bit";
			break;
		}
		res += ((formatType & F_SIGNED) == F_SIGNED) ? " signed" : " unsigned";
		if ((formatType & F_SAMPLE_WIDTH_MASK) != F_8) {
			res +=
					((formatType & F_BIGENDIAN) == F_BIGENDIAN) ? " big endian"
							: " little endian";
		}
		return res;
	}

	// /////////////////// BYTE 2 double /////////////////////////////////// //

	private static final double twoPower7 = 128.0;

	private static final double twoPower15 = 32768.0;

	private static final double twoPower23 = 8388608.0;

	private static final double twoPower31 = 2147483648.0;

	private static final double invTwoPower7 = 1 / twoPower7;

	private static final double invTwoPower15 = 1 / twoPower15;

	private static final double invTwoPower23 = 1 / twoPower23;

	private static final double invTwoPower31 = 1 / twoPower31;

	/**
	 * @see #byte2double(byte[] input, int inByteOffset, Object[] output,
	 *      int outOffset, int frameCount, AudioFormat format, boolean
	 *      allowAddChannel)
	 */
	public static void byte2double(byte[] input, int inByteOffset,
			List<double[]> output, int outOffset, int frameCount,
			AudioFormat format) {

		byte2double(input, inByteOffset, output, outOffset, frameCount, format,
				true);
	}

	/**
	 * @param output an array of double[] arrays
	 * @throws ArrayIndexOutOfBoundsException if output does not
	 *             format.getChannels() elements
	 * @see #byte2double(byte[] input, int inByteOffset, Object[] output,
	 *      int outOffset, int frameCount, AudioFormat format, boolean
	 *      allowAddChannel)
	 */
	public static void byte2double(byte[] input, int inByteOffset,
			Object[] output, int outOffset, int frameCount, AudioFormat format) {

		byte2double(input, inByteOffset, output, outOffset, frameCount, format,
				true);
	}

	/**
	 * @param output an array of double[] arrays
	 * @param allowAddChannel if true, and output has fewer channels than
	 *            format, then only output.length channels are filled
	 * @throws ArrayIndexOutOfBoundsException if output does not
	 *             format.getChannels() elements
	 * @see #byte2double(byte[] input, int inByteOffset, Object[] output,
	 *      int outOffset, int frameCount, AudioFormat format, boolean
	 *      allowAddChannel)
	 */
	public static void byte2double(byte[] input, int inByteOffset,
			Object[] output, int outOffset, int frameCount, AudioFormat format,
			boolean allowAddChannel) {

		int channels = format.getChannels();
		if (!allowAddChannel && channels > output.length) {
			channels = output.length;
		}
		if (output.length < channels) {
			throw new ArrayIndexOutOfBoundsException(
					"too few channel output array");
		}
		for (int channel = 0; channel < channels; channel++) {
			double[] data = (double[]) output[channel];
			if (data.length < frameCount + outOffset) {
				data = new double[frameCount + outOffset];
				output[channel] = data;
			}

			byte2doubleGeneric(input, inByteOffset, format.getFrameSize(),
					data, outOffset, frameCount, format);
			inByteOffset += format.getFrameSize() / format.getChannels();
		}
	}

	/**
	 * Conversion function to convert an interleaved byte array to a List of
	 * interleaved double arrays. The double arrays will contain normalized
	 * samples in the range [-1.0, +1.0]. The input array provides bytes in the
	 * format specified in <code>format</code>.
	 * <p>
	 * Only PCM formats are accepted. The method will convert all byte values
	 * from <code>input[inByteOffset]</code> to
	 * <code>input[inByteOffset + (frameCount * format.getFrameSize()) - 1]</code>
	 * to doubles from <code>output(n)[outOffset]</code> to
	 * <code>output(n)[outOffset + frameCount - 1]</code>
	 * 
	 * @param input the audio data in an byte array
	 * @param inByteOffset index in input where to start the conversion
	 * @param output list of double[] arrays which receive the converted audio
	 *            data. if the list does not contain enough elements, or
	 *            individual double arrays are not large enough, they are
	 *            created.
	 * @param outOffset the start offset in <code>output</code>
	 * @param frameCount number of frames to be converted
	 * @param format the input format. Only packed PCM is allowed
	 * @param allowAddChannel if true, channels may be added to
	 *            <code>output</code> to match the number of input channels,
	 *            otherwise, only the first output.size() channels of input data
	 *            are converted.
	 * @throws IllegalArgumentException if one of the parameters is out of
	 *             bounds
	 * @see #byte2doubleInterleaved(byte[],int,double[],int,int,AudioFormat)
	 */
	public static void byte2double(byte[] input, int inByteOffset,
			List<double[]> output, int outOffset, int frameCount,
			AudioFormat format, boolean allowAddChannel) {

		int channels = format.getChannels();
		if (!allowAddChannel && channels > output.size()) {
			channels = output.size();
		}
		for (int channel = 0; channel < channels; channel++) {
			double[] data;
			if (output.size() < channel) {
				data = new double[frameCount + outOffset];
				output.add(data);
			} else {
				data = output.get(channel);
				if (data.length < frameCount + outOffset) {
					data = new double[frameCount + outOffset];
					output.set(channel, data);
				}
			}

			byte2doubleGeneric(input, inByteOffset, format.getFrameSize(),
					data, outOffset, frameCount, format);
			inByteOffset += format.getFrameSize() / format.getChannels();
		}
	}

	/**
	 * Conversion function to convert an interleaved byte array to an
	 * interleaved double array. The double array will contain normalized
	 * samples in the range [-1.0, +1.0]. The input array provides bytes in the
	 * format specified in <code>format</code>.
	 * <p>
	 * Only PCM formats are accepted. The method will convert all byte values
	 * from <code>input[inByteOffset]</code> to
	 * <code>input[inByteOffset + (frameCount * format.getFrameSize()) - 1]</code>
	 * to doubles from <code>output[outOffset]</code> to
	 * <code>output[outOffset + (frameCount * format.getChannels()) - 1]</code>
	 * 
	 * @param input the audio data in an byte array
	 * @param inByteOffset index in input where to start the conversion
	 * @param output the double array that receives the converted audio data
	 * @param outOffset the start offset in <code>output</code>
	 * @param frameCount number of frames to be converted
	 * @param format the input format. Only packed PCM is allowed
	 * @throws IllegalArgumentException if one of the parameters is out of
	 *             bounds
	 * @see #byte2double(byte[],int,List,int,int,AudioFormat)
	 */
	public static void byte2doubleInterleaved(byte[] input, int inByteOffset,
			double[] output, int outOffset, int frameCount, AudioFormat format) {

		byte2doubleGeneric(input, inByteOffset, format.getFrameSize()
				/ format.getChannels(), output, outOffset, frameCount
				* format.getChannels(), format);
	}

	/**
	 * Generic conversion function to convert a byte array to a double array.
	 * <p>
	 * Only PCM formats are accepted. The method will convert all bytes from
	 * <code>input[inByteOffset]</code> to
	 * <code>input[inByteOffset + (sampleCount * (inByteStep - 1)]</code> to
	 * samples from <code>output[outOffset]</code> to
	 * <code>output[outOffset+sampleCount-1]</code>.
	 * <p>
	 * The <code>format</code>'s channel count is ignored.
	 * <p>
	 * For mono data, set <code>inByteOffset</code> to
	 * <code>format.getFrameSize()</code>.<br>
	 * For converting interleaved input data, multiply <code>sampleCount</code>
	 * by the number of channels and set inByteStep to
	 * <code>format.getFrameSize() / format.getChannels()</code>.
	 * 
	 * @param sampleCount number of samples to be written to output
	 * @param inByteStep how many bytes advance for each output sample in
	 *            <code>output</code>.
	 * @throws IllegalArgumentException if one of the parameters is out of
	 *             bounds
	 * @see #byte2doubleInterleaved(byte[],int,double[],int,int,AudioFormat)
	 * @see #byte2double(byte[],int,List,int,int,AudioFormat)
	 */
	static void byte2doubleGeneric(byte[] input, int inByteOffset,
			int inByteStep, double[] output, int outOffset, int sampleCount,
			AudioFormat format) {
		int formatType = getFormatType(format);

		byte2doubleGeneric(input, inByteOffset, inByteStep, output, outOffset,
				sampleCount, formatType);
	}

	/**
	 * Central conversion function from a byte array to a normalized double
	 * array. In order to accomodate interleaved and non-interleaved samples,
	 * this method takes inByteStep as parameter which can be used to flexibly
	 * convert the data.
	 * <p>
	 * E.g.:<br>
	 * mono->mono: inByteStep=format.getFrameSize()<br>
	 * interleaved_stereo->interleaved_stereo:
	 * inByteStep=format.getFrameSize()/2, sampleCount*2<br>
	 * stereo->2 mono arrays:<br>
	 * ---inByteOffset=0, outOffset=0, inByteStep=format.getFrameSize()<br>
	 * ---inByteOffset=format.getFrameSize()/2, outOffset=1,
	 * inByteStep=format.getFrameSize()<br>
	 */
	static void byte2doubleGeneric(byte[] input, int inByteOffset,
			int inByteStep, double[] output, int outOffset, int sampleCount,
			int formatType) {
		// if (TDebug.TraceAudioConverter) {
		// TDebug.out("ConversionTool.byte2doubleGeneric, formatType="
		// +formatType2Str(formatType));
		// }
		int endCount = outOffset + sampleCount;
		int inIndex = inByteOffset;
		for (int outIndex = outOffset; outIndex < endCount; outIndex++, inIndex +=
				inByteStep) {
			// do conversion
			switch (formatType) {
			case CT_8S:
				output[outIndex] = ((double) input[inIndex]) * invTwoPower7;
				break;
			case CT_8U:
				output[outIndex] =
						((double) ((input[inIndex] & 0xFF) - 128))
								* invTwoPower7;
				break;
			case CT_16SB:
				output[outIndex] =
						((double) ((input[inIndex] << 8) | (input[inIndex + 1] & 0xFF)))
								* invTwoPower15;
				break;
			case CT_16SL:
				output[outIndex] =
						((double) ((input[inIndex + 1] << 8) | (input[inIndex] & 0xFF)))
								* invTwoPower15;
				break;
			case CT_24_3SB:
				output[outIndex] =
						((double) ((input[inIndex] << 16)
								| ((input[inIndex + 1] & 0xFF) << 8) | (input[inIndex + 2] & 0xFF)))
								* invTwoPower23;
				break;
			case CT_24_3SL:
				output[outIndex] =
						((double) ((input[inIndex + 2] << 16)
								| ((input[inIndex + 1] & 0xFF) << 8) | (input[inIndex] & 0xFF)))
								* invTwoPower23;
				break;
			case CT_24_4SB:
				output[outIndex] =
						((double) ((input[inIndex + 1] << 16)
								| ((input[inIndex + 2] & 0xFF) << 8) | (input[inIndex + 3] & 0xFF)))
								* invTwoPower23;
				break;
			case CT_24_4SL:
				// TODO: verify the indexes
				output[outIndex] =
						((double) ((input[inIndex + 3] << 16)
								| ((input[inIndex + 2] & 0xFF) << 8) | (input[inIndex + 1] & 0xFF)))
								* invTwoPower23;
				break;
			case CT_32SB:
				output[outIndex] =
						((double) ((input[inIndex] << 24)
								| ((input[inIndex + 1] & 0xFF) << 16)
								| ((input[inIndex + 2] & 0xFF) << 8) | (input[inIndex + 3] & 0xFF)))
								* invTwoPower31;
				break;
			case CT_32SL:
				output[outIndex] =
						((double) ((input[inIndex + 3] << 24)
								| ((input[inIndex + 2] & 0xFF) << 16)
								| ((input[inIndex + 1] & 0xFF) << 8) | (input[inIndex] & 0xFF)))
								* invTwoPower31;
				break;
			default:
				throw new IllegalArgumentException("unsupported format="
						+ formatType2Str(formatType));
			}
		}
	}

	private static final double convertOneSample(byte[] input, int inIndex,
			int formatType) {
		// do conversion
		switch (formatType) {
		case CT_8S:
			return ((double) input[inIndex]) * invTwoPower7;
		case CT_8U:
			return ((double) ((input[inIndex] & 0xFF) - 128)) * invTwoPower7;
		case CT_16SB:
			return ((double) ((input[inIndex] << 8) | (input[inIndex + 1] & 0xFF)))
					* invTwoPower15;
		case CT_16SL:
			return ((double) ((input[inIndex + 1] << 8) | (input[inIndex] & 0xFF)))
					* invTwoPower15;
		case CT_24_3SB:
			return ((double) ((input[inIndex] << 16)
					| ((input[inIndex + 1] & 0xFF) << 8) | (input[inIndex + 2] & 0xFF)))
					* invTwoPower23;
		case CT_24_3SL:
			return ((double) ((input[inIndex + 2] << 16)
					| ((input[inIndex + 1] & 0xFF) << 8) | (input[inIndex] & 0xFF)))
					* invTwoPower23;
		case CT_24_4SB:
			// TODO: verify
			return ((double) ((input[inIndex + 1] << 16)
					| ((input[inIndex + 2] & 0xFF) << 8) | (input[inIndex + 3] & 0xFF)))
					* invTwoPower23;
		case CT_24_4SL:
			// TODO: verify
			return ((double) ((input[inIndex + 3] << 16)
					| ((input[inIndex + 2] & 0xFF) << 8) | (input[inIndex + 1] & 0xFF)))
					* invTwoPower23;
		case CT_32SB:
			return ((double) ((input[inIndex] << 24)
					| ((input[inIndex + 1] & 0xFF) << 16)
					| ((input[inIndex + 2] & 0xFF) << 8) | (input[inIndex + 3] & 0xFF)))
					* invTwoPower31;
		case CT_32SL:
			return ((double) ((input[inIndex + 3] << 24)
					| ((input[inIndex + 2] & 0xFF) << 16)
					| ((input[inIndex + 1] & 0xFF) << 8) | (input[inIndex] & 0xFF)))
					* invTwoPower31;
		default:
			throw new IllegalArgumentException("unsupported format="
					+ formatType2Str(formatType));
		}
	}

	/**
	 * Sample rate conversion with linear interpolation.
	 * <p>
	 * Pre-Conditions:
	 * <ul>
	 * <li>input has enough bytes to generate sampleCount output samples
	 * <li>output has enough room to write sampleCount samples
	 * <li>formatType is a correct format
	 * </ul>
	 * 
	 * @param input the input array
	 * @param inByteOffset an initial offset to the input array
	 * @param inSampleToByteFactor how many bytes per sample in input array
	 * @param inSamplePos the floating point position in the input array. This
	 *            is not a byte array index, but a sample number. The first
	 *            array element to be read from the input array is
	 *            (Math.floor(inSamplePos)*inSampleToByteFactor)+inByteOffset
	 * @param inSampleStep how much increase inSamplePos for each output sample.
	 *            This is the inverse of the effective sample rate factor.
	 * @param output the output array to be filled
	 * @param outOffset where to start writing to output[]
	 * @param sampleCount the number of samples to generate
	 * @param formatType the format of the input data
	 */
	public static void byte2doubleGenericLSRC(byte[] input, int inByteOffset,
			int inSampleToByteFactor, double inSamplePos, double inSampleStep,
			double[] output, int outOffset, int sampleCount, int formatType) {
		int endCount = outOffset + sampleCount;
		double sample1 = 0.0;
		double sample2 = 0.0;
		int index1 = -2; // impossible value to make sure both initial
		// samples are calculated

		for (int outIndex = outOffset; outIndex < endCount; outIndex++, inSamplePos +=
				inSampleStep) {
			// calculate index1 and index2: sample position of the input data
			int newIndex1 = (int) inSamplePos;

			// calculate sample1 and sample2.
			// shortcut: if we had the same ones in the last iteration, don't
			// need to recalculate the sample values
			if (newIndex1 != index1) {
				int byteArrayIndex1 =
						(newIndex1 * inSampleToByteFactor) + inByteOffset;
				if ((index1 + 1) == newIndex1) {
					// last second index is this round's first index
					// -> we only need to calculate the second sample
					sample1 = sample2;
				} else {
					sample1 =
							convertOneSample(input, byteArrayIndex1, formatType);
				}
				sample2 =
						convertOneSample(input, byteArrayIndex1
								+ inSampleToByteFactor, formatType);
			}
			// remember index1 for next iteration
			index1 = newIndex1;

			double factor2 = inSamplePos - index1;
			double factor1 = 1.0 - factor2;
			output[outIndex] = ((factor1 * sample1) + (factor2 * sample2));
		}
	}

	// /////////////////// double 2 BYTE /////////////////////////////////// //

	private static byte quantize8(double sample, double ditherBits) {
		if (ditherBits != 0) {
			sample += random.nextDouble() * ditherBits;
		}
		if (sample >= 127.0) {
			return (byte) 127;
		} else if (sample <= -128.0) {
			return (byte) -128;
		} else {
			return (byte) (sample < 0 ? (sample - 0.5) : (sample + 0.5));
		}
	}

	private static int quantize16(double sample, double ditherBits) {
		if (ditherBits != 0) {
			sample += random.nextDouble() * ditherBits;
		}
		if (sample >= 32767.0) {
			return 32767;
		} else if (sample <= -32768.0) {
			return -32768;
		} else {
			return (int) (sample < 0 ? (sample - 0.5) : (sample + 0.5));
		}
	}

	private static int quantize24(double sample, double ditherBits) {
		if (ditherBits != 0) {
			sample += random.nextDouble() * ditherBits;
		}
		if (sample >= 8388607.0) {
			return 8388607;
		} else if (sample <= -8388608.0) {
			return -8388608;
		} else {
			return (int) (sample < 0 ? (sample - 0.5) : (sample + 0.5));
		}
	}

	private static int quantize32(double sample, double ditherBits) {
		if (ditherBits != 0) {
			sample += random.nextDouble() * ditherBits;
		}
		if (sample >= 2147483647.0) {
			return 2147483647;
		} else if (sample <= -2147483648.0) {
			return -2147483648;
		} else {
			return (int) (sample < 0 ? (sample - 0.5) : (sample + 0.5));
		}
	}

	/**
	 * Conversion function to convert a non-interleaved double audio data to an
	 * interleaved byte array. The double arrays contains normalized samples in
	 * the range [-1.0, +1.0]. The output array will receive bytes in the format
	 * specified in <code>format</code>. Exactly
	 * <code>format.getChannels()</code> channels are converted regardless of
	 * the number of elements in <code>input</code>. If <code>input</code>
	 * does not provide enough channels, an </code>IllegalArgumentException<code>
	 * is thrown.
	 * <p>
	 * Only PCM formats are accepted. The method will convert all samples from
	 * <code>input(n)[inOffset]</code> to
	 * <code>input(n)[inOffset + frameCount - 1]</code> to byte values from
	 * <code>output[outByteOffset]</code> to
	 * <code>output[outByteOffset + (frameCount * format.getFrameSize()) - 1]</code>
	 * <p>
	 * Dithering should be used when the output resolution is significantly
	 * lower than the original resolution. This includes if the original data
	 * was 16-bit and it is now converted to 8-bit, or if the data was generated
	 * in the double domain. No dithering need to be used if the original sample
	 * data was in e.g. 8-bit and the resulting output data has a higher
	 * resolution. If dithering is used, a sensitive value is
	 * DEFAULT_DITHER_BITS.
	 * 
	 * @param input a List of double arrays with the input audio data
	 * @param inOffset index in the input arrays where to start the conversion
	 * @param output the byte array that receives the converted audio data
	 * @param outByteOffset the start offset in <code>output</code>
	 * @param frameCount number of frames to be converted.
	 * @param format the output format. Only packed PCM is allowed
	 * @param ditherBits if 0, do not dither. Otherwise the number of bits to be
	 *            dithered
	 * @throws IllegalArgumentException if one of the parameters is out of
	 *             bounds
	 * @see #DEFAULT_DITHER_BITS
	 * @see #double2byteInterleaved(double[],int,byte[],int,int,AudioFormat,double)
	 */
	public static void double2byte(List<double[]> input, int inOffset,
			byte[] output, int outByteOffset, int frameCount,
			AudioFormat format, double ditherBits) {
		for (int channel = 0; channel < format.getChannels(); channel++) {
			double[] data = (double[]) input.get(channel);
			double2byteGeneric(data, inOffset, output, outByteOffset,
					format.getFrameSize(), frameCount, format, ditherBits);
			outByteOffset += format.getFrameSize() / format.getChannels();
		}
	}

	/**
	 * @param input an array of double[] arrays
	 * @throws ArrayIndexOutOfBoundsException if one of the parameters is out of
	 *             bounds
	 * @see #double2byte(Object[] input, int inOffset, byte[] output, int
	 *      outByteOffset, int frameCount, AudioFormat format, double
	 *      ditherBits)
	 */
	public static void double2byte(Object[] input, int inOffset, byte[] output,
			int outByteOffset, int frameCount, AudioFormat format,
			double ditherBits) {
		int channels = format.getChannels();
		for (int channel = 0; channel < channels; channel++) {
			double[] data = (double[]) input[channel];
			double2byteGeneric(data, inOffset, output, outByteOffset,
					format.getFrameSize(), frameCount, format, ditherBits);
			outByteOffset += format.getFrameSize() / format.getChannels();
		}
	}

	/**
	 * Conversion function to convert an interleaved double array to an
	 * interleaved byte array. The double array contains normalized samples in
	 * the range [-1.0, +1.0]. The output array will receive bytes in the format
	 * specified in <code>format</code>.
	 * <p>
	 * Only PCM formats are accepted. The method will convert all samples from
	 * <code>input[inOffset]</code> to
	 * <code>input[inOffset + (frameCount * format.getChannels()) - 1]</code>
	 * to byte values from <code>output[outByteOffset]</code> to
	 * <code>output[outByteOffset + (frameCount * format.getFrameSize()) - 1]</code>
	 * <p>
	 * Dithering should be used when the output resolution is significantly
	 * lower than the original resolution. This includes if the original data
	 * was 16-bit and it is now converted to 8-bit, or if the data was generated
	 * in the double domain. No dithering need to be used if the original sample
	 * data was in e.g. 8-bit and the resulting output data has a higher
	 * resolution. If dithering is used, a sensitive value is
	 * DEFAULT_DITHER_BITS.
	 * 
	 * @param input the audio data in normalized samples
	 * @param inOffset index in input where to start the conversion
	 * @param output the byte array that receives the converted audio data
	 * @param outByteOffset the start offset in <code>output</code>
	 * @param frameCount number of frames to be converted.
	 * @param format the output format. Only packed PCM is allowed
	 * @param ditherBits if 0, do not dither. Otherwise the number of bits to be
	 *            dithered
	 * @throws IllegalArgumentException if one of the parameters is out of
	 *             bounds
	 * @see #DEFAULT_DITHER_BITS
	 * @see #double2byte(List,int,byte[],int,int,AudioFormat,double)
	 */
	public static void double2byteInterleaved(double[] input, int inOffset,
			byte[] output, int outByteOffset, int frameCount,
			AudioFormat format, double ditherBits) {
		double2byteGeneric(input, inOffset, output, outByteOffset,
				format.getFrameSize() / format.getChannels(), frameCount
						* format.getChannels(), format, ditherBits);
	}

	/**
	 * Generic conversion function to convert a double array to a byte array.
	 * <p>
	 * Only PCM formats are accepted. The method will convert all samples from
	 * <code>input[inOffset]</code> to
	 * <code>input[inOffset+sampleCount-1]</code> to byte values from
	 * <code>output[outByteOffset]</code> to
	 * <code>output[outByteOffset + (sampleCount * (outByteStep - 1)]</code>.
	 * <p>
	 * The <code>format</code>'s channel count is ignored.
	 * <p>
	 * For mono data, set <code>outByteOffset</code> to
	 * <code>format.getFrameSize()</code>.<br>
	 * For converting interleaved input data, multiply <code>sampleCount</code>
	 * by the number of channels and set outByteStep to
	 * <code>format.getFrameSize() / format.getChannels()</code>.
	 * 
	 * @param sampleCount number of samples in input to be converted.
	 * @param outByteStep how many bytes advance for each input sample in
	 *            <code>input</code>.
	 * @throws IllegalArgumentException if one of the parameters is out of
	 *             bounds
	 * @see #double2byteInterleaved(double[],int,byte[],int,int,AudioFormat,double)
	 * @see #double2byte(List,int,byte[],int,int,AudioFormat,double)
	 */
	static void double2byteGeneric(double[] input, int inOffset, byte[] output,
			int outByteOffset, int outByteStep, int sampleCount,
			AudioFormat format, double ditherBits) {
		int formatType = getFormatType(format);

		double2byteGeneric(input, inOffset, output, outByteOffset, outByteStep,
				sampleCount, formatType, ditherBits);
	}

	/**
	 * Central conversion function from normalized double array to a byte array.
	 * In order to accomodate interleaved and non-interleaved samples, this
	 * method takes outByteStep as parameter which can be used to flexibly
	 * convert the data.
	 * <p>
	 * E.g.:<br>
	 * mono->mono: outByteStep=format.getFrameSize()<br>
	 * interleaved stereo->interleaved stereo:
	 * outByteStep=format.getFrameSize()/2, sampleCount*2<br>
	 * 2 mono arrays->stereo:<br>
	 * ---inOffset=0, outByteOffset=0, outByteStep=format.getFrameSize()<br>
	 * ---inOffset=1, outByteOffset=format.getFrameSize()/2,
	 * outByteStep=format.getFrameSize()<br>
	 */
	static void double2byteGeneric(double[] input, int inOffset, byte[] output,
			int outByteOffset, int outByteStep, int sampleCount,
			int formatType, double ditherBits) {
		// if (TDebug.TraceAudioConverter) {
		// TDebug.out("ConversionTool.double2byteGeneric, formatType="
		// +"formatType2Str(formatType));
		// }

		if (inOffset < 0 || inOffset + sampleCount > input.length
				|| sampleCount < 0) {
			throw new IllegalArgumentException("invalid input index: "
					+ "input.length=" + input.length + " inOffset=" + inOffset
					+ " sampleCount=" + sampleCount);
		}
		if (outByteOffset < 0
				|| outByteOffset + (sampleCount * outByteStep) >= (output.length + outByteStep)
				|| outByteStep < getSampleSize(formatType)) {
			throw new IllegalArgumentException("invalid output index: "
					+ "output.length=" + output.length + " outByteOffset="
					+ outByteOffset + " outByteStep=" + outByteStep
					+ " sampleCount=" + sampleCount + " format="
					+ formatType2Str(formatType));
		}

		if (ditherBits != 0.0 && random == null) {
			// create the random number generator for dithering
			random = new Random();
		}
		int endSample = inOffset + sampleCount;
		int iSample;
		int outIndex = outByteOffset;
		for (int inIndex = inOffset; inIndex < endSample; inIndex++, outIndex +=
				outByteStep) {
			// do conversion
			switch (formatType) {
			case CT_8S:
				output[outIndex] =
						quantize8(input[inIndex] * twoPower7, ditherBits);
				break;
			case CT_8U:
				output[outIndex] =
						(byte) (quantize8((input[inIndex] * twoPower7),
								ditherBits) + 128);
				break;
			case CT_16SB:
				iSample = quantize16(input[inIndex] * twoPower15, ditherBits);
				output[outIndex] = (byte) (iSample >> 8);
				output[outIndex + 1] = (byte) (iSample & 0xFF);
				break;
			case CT_16SL:
				iSample = quantize16(input[inIndex] * twoPower15, ditherBits);
				output[outIndex + 1] = (byte) (iSample >> 8);
				output[outIndex] = (byte) (iSample & 0xFF);
				break;
			case CT_24_3SB:
				iSample = quantize24(input[inIndex] * twoPower23, ditherBits);
				output[outIndex] = (byte) (iSample >> 16);
				output[outIndex + 1] = (byte) ((iSample >>> 8) & 0xFF);
				output[outIndex + 2] = (byte) (iSample & 0xFF);
				break;
			case CT_24_3SL:
				iSample = quantize24(input[inIndex] * twoPower23, ditherBits);
				output[outIndex + 2] = (byte) (iSample >> 16);
				output[outIndex + 1] = (byte) ((iSample >>> 8) & 0xFF);
				output[outIndex] = (byte) (iSample & 0xFF);
				break;
			case CT_24_4SB:
				// TODO: verify
				iSample = quantize24(input[inIndex] * twoPower23, ditherBits);
				output[outIndex + 0] = 0;
				output[outIndex + 1] = (byte) (iSample >> 16);
				output[outIndex + 2] = (byte) ((iSample >>> 8) & 0xFF);
				output[outIndex + 3] = (byte) (iSample & 0xFF);
				break;
			case CT_24_4SL:
				// TODO: verify
				iSample = quantize24(input[inIndex] * twoPower23, ditherBits);
				output[outIndex + 3] = (byte) (iSample >> 16);
				output[outIndex + 2] = (byte) ((iSample >>> 8) & 0xFF);
				output[outIndex + 1] = (byte) (iSample & 0xFF);
				output[outIndex + 0] = 0;
				break;
			case CT_32SB:
				iSample = quantize32(input[inIndex] * twoPower31, ditherBits);
				output[outIndex] = (byte) (iSample >> 24);
				output[outIndex + 1] = (byte) ((iSample >>> 16) & 0xFF);
				output[outIndex + 2] = (byte) ((iSample >>> 8) & 0xFF);
				output[outIndex + 3] = (byte) (iSample & 0xFF);
				break;
			case CT_32SL:
				iSample = quantize32(input[inIndex] * twoPower31, ditherBits);
				output[outIndex + 3] = (byte) (iSample >> 24);
				output[outIndex + 2] = (byte) ((iSample >>> 16) & 0xFF);
				output[outIndex + 1] = (byte) ((iSample >>> 8) & 0xFF);
				output[outIndex] = (byte) (iSample & 0xFF);
				break;
			default:
				throw new IllegalArgumentException("unsupported format="
						+ formatType2Str(formatType));
			}
		}
	}
}
