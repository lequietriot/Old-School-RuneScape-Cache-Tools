/*
 * FloatSampleTools.java
 *
 * Based on Tritonus: http://www.tritonus.org/
 *
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
package synth.simple;

import javax.sound.sampled.AudioFormat;
import java.util.List;
import java.util.Random;


/**
 * Utility functions for handling data in normalized float arrays.
 * Each sample is linear in the range of [-1.0f, +1.0f].
 * <p>
 * Currently, the following bit sizes are supported:
 * <ul>
 * <li>8-bit
 * <li>16-bit
 * <li>packed 24-bit (stored in 3 bytes)
 * <li>32-bit
 * </ul>
 * 8-bit data can be unsigned or signed. All other data is only
 * supported in signed encoding.
 *
 * @see FloatSampleBuffer
 * @author Florian Bomers
 */

public class FloatSampleTools {

	/** default number of bits to be dithered: 0.7f */
	public static final float DEFAULT_DITHER_BITS = 0.7f;

	private static Random random = null;

	// sample width (must be in order !)
	static final int F_8=1;
	static final int F_16=2;
	static final int F_24=3;
	static final int F_32=4;
	static final int F_SAMPLE_WIDTH_MASK=F_8 | F_16 | F_24 | F_32;

	// format bit-flags
	static final int F_SIGNED=8;
	static final int F_BIGENDIAN=16;

	// supported formats
	static final int CT_8S=F_8 | F_SIGNED;
	static final int CT_8U=F_8;
	static final int CT_16SB=F_16 | F_SIGNED | F_BIGENDIAN;
	static final int CT_16SL=F_16 | F_SIGNED;
	static final int CT_24SB=F_24 | F_SIGNED | F_BIGENDIAN;
	static final int CT_24SL=F_24 | F_SIGNED;
	static final int CT_32SB=F_32 | F_SIGNED | F_BIGENDIAN;
	static final int CT_32SL=F_32 | F_SIGNED;

	// ////////////////////////////// initialization /////////////////////////////// //

	/** prevent instanciation */
	private FloatSampleTools() {
	}


	// /////////////////// FORMAT / FORMAT TYPE /////////////////////////////////// //

	/**
	 * only allow "packed" samples -- currently no support for 18, 20, 24_32 bits.
	 * @throws IllegalArgumentException
	 */
	static void checkSupportedSampleSize(int ssib, int channels, int frameSize) {
		if ((ssib*channels) != frameSize * 8) {
			throw new IllegalArgumentException("unsupported sample size: "+ssib
			                                   +" stored in "+(frameSize/channels)+" bytes.");
		}
	}


	/**
	 * Get the formatType code from the given format.
	 * @throws IllegalArgumentException
	 */
	static int getFormatType(AudioFormat format) {
		boolean signed = format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
		if (!signed &&
		    !format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
			throw new IllegalArgumentException
			("unsupported encoding: only PCM encoding supported.");
		}
		if (!signed && format.getSampleSizeInBits() != 8) {
			throw new IllegalArgumentException
			("unsupported encoding: only 8-bit can be unsigned");
		}
		checkSupportedSampleSize(format.getSampleSizeInBits(),
		                         format.getChannels(),
		                         format.getFrameSize());

		int formatType = getFormatType(format.getSampleSizeInBits(),
		                               signed, format.isBigEndian());
		return formatType;
	}

	/**
	 * @throws IllegalArgumentException
	 */
	static int getFormatType(int ssib, boolean signed, boolean bigEndian) {
		int bytesPerSample=ssib/8;
		int res=0;
		if (ssib==8) {
			res=F_8;
		} else if (ssib==16) {
			res=F_16;
		} else if (ssib==24) {
			res=F_24;
		} else if (ssib==32) {
			res=F_32;
		}
		if (res==0) {
			throw new IllegalArgumentException
			("FloatSampleBuffer: unsupported sample size of "
			 +ssib+" bits per sample.");
		}
		if (!signed && bytesPerSample>1) {
			throw new IllegalArgumentException
			("FloatSampleBuffer: unsigned samples larger than "
			 +"8 bit are not supported");
		}
		if (signed) {
			res|=F_SIGNED;
		}
		if (bigEndian && (ssib!=8)) {
			res|=F_BIGENDIAN;
		}
		return res;
	}

	static int getSampleSize(int formatType) {
		switch (formatType &  F_SAMPLE_WIDTH_MASK) {
		case F_8: return 1;
		case F_16: return 2;
		case F_24: return 3;
		case F_32: return 4;
		}
		return 0;
	}

	/**
	 * Return a string representation of this format
	 */
	static String formatType2Str(int formatType) {
		String res=""+formatType+": ";
		switch (formatType &  F_SAMPLE_WIDTH_MASK) {
		case F_8:
			res+="8bit";
			break;
		case F_16:
			res+="16bit";
			break;
		case F_24:
			res+="24bit";
			break;
		case F_32:
			res+="32bit";
			break;
		}
		res+=((formatType & F_SIGNED)==F_SIGNED)?" signed":" unsigned";
		if ((formatType &  F_SAMPLE_WIDTH_MASK)!=F_8) {
			res+=((formatType & F_BIGENDIAN)==F_BIGENDIAN)?
			     " big endian":" little endian";
		}
		return res;
	}


	// /////////////////// BYTE 2 FLOAT /////////////////////////////////// //

	private static final float twoPower7=128.0f;
	private static final float twoPower15=32768.0f;
	private static final float twoPower23=8388608.0f;
	private static final float twoPower31=2147483648.0f;

	private static final float invTwoPower7=1/twoPower7;
	private static final float invTwoPower15=1/twoPower15;
	private static final float invTwoPower23=1/twoPower23;
	private static final float invTwoPower31=1/twoPower31;


	/**
	 * Conversion function to convert an interleaved byte array to
	 * a List of interleaved float arrays. The float arrays will contain normalized
	 * samples in the range [-1.0f, +1.0f]. The input array
	 * provides bytes in the format specified in <code>format</code>.
	 * <p>
	 * Only PCM formats are accepted. The method will convert all
	 * byte values from
	 * <code>input[inByteOffset]</code> to
	 * <code>input[inByteOffset + (frameCount * format.getFrameSize()) - 1]</code>
	 * to floats from
	 * <code>output(n)[outOffset]</code> to
	 * <code>output(n)[outOffset + frameCount - 1]</code>
	 *
	 * @param input the audio data in an byte array
	 * @param inByteOffset index in input where to start the conversion
	 * @param output list of float[] arrays which receive the converted audio data.
	 *        if the list does not contain enough elements, or individual float arrays
	 *        are not large enough, they are created.
	 * @param outOffset the start offset in <code>output</code>
	 * @param frameCount number of frames to be converted
	 * @param format the input format. Only packed PCM is allowed
	 * @throws IllegalArgumentException if one of the parameters is out of bounds
	 *
	 * @see #byte2floatInterleaved(byte[],int,float[],int,int,AudioFormat)
	 */
	public static void byte2float(byte[] input, int inByteOffset,
	                              List<float[]> output, int outOffset, int frameCount,
	                              //List output, int outOffset, int frameCount,
	                              AudioFormat format) {
		for (int channel = 0; channel < format.getChannels(); channel++) {
			float[] data;
			if (output.size() < channel) {
				data = new float[frameCount + outOffset];
				output.add(data);
			} else {
				data = output.get(channel);
				if (data.length < frameCount + outOffset) {
					data = new float[frameCount + outOffset];
					output.set(channel, data);
				}
			}

			byte2floatGeneric(input, inByteOffset, format.getFrameSize(),
			                  data, outOffset,
			                  frameCount, format);
			inByteOffset += format.getFrameSize() / format.getChannels();
		}
	}


	/**
	 * Conversion function to convert an interleaved byte array to
	 * an interleaved float array. The float array will contain normalized
	 * samples in the range [-1.0f, +1.0f]. The input array
	 * provides bytes in the format specified in <code>format</code>.
	 * <p>
	 * Only PCM formats are accepted. The method will convert all
	 * byte values from
	 * <code>input[inByteOffset]</code> to
	 * <code>input[inByteOffset + (frameCount * format.getFrameSize()) - 1]</code>
	 * to floats from
	 * <code>output[outOffset]</code> to
	 * <code>output[outOffset + (frameCount * format.getChannels()) - 1]</code>
	 *
	 * @param input the audio data in an byte array
	 * @param inByteOffset index in input where to start the conversion
	 * @param output the float array that receives the converted audio data
	 * @param outOffset the start offset in <code>output</code>
	 * @param frameCount number of frames to be converted
	 * @param format the input format. Only packed PCM is allowed
	 * @throws IllegalArgumentException if one of the parameters is out of bounds
	 *
	 * @see #byte2float(byte[],int,List,int,int,AudioFormat)
	 */
	public static void byte2floatInterleaved(byte[] input, int inByteOffset,
	                                         float[] output, int outOffset, int frameCount,
	                                         AudioFormat format) {

		byte2floatGeneric(input, inByteOffset, format.getFrameSize() / format.getChannels(),
	                          output, outOffset, frameCount * format.getChannels(),
	                          format);
	}



	/**
	 * Generic conversion function to convert a byte array to
	 * a float array.
	 * <p>
	 * Only PCM formats are accepted. The method will convert all
	 * bytes from
	 * <code>input[inByteOffset]</code> to
	 * <code>input[inByteOffset + (sampleCount * (inByteStep - 1)]</code>
	 * to samples from
	 * <code>output[outOffset]</code> to
	 * <code>output[outOffset+sampleCount-1]</code>.
	 * <p>
	 * The <code>format</code>'s channel count is ignored.
	 * <p>
	 * For mono data, set <code>inByteOffset</code> to <code>format.getFrameSize()</code>.<br>
	 * For converting interleaved input data, multiply <code>sampleCount</code>
	 *    by the number of channels and set inByteStep to
	 *    <code>format.getFrameSize() / format.getChannels()</code>.
	 *
	 * @param sampleCount number of samples to be written to output
	 * @param inByteStep how many bytes advance for each output sample in <code>output</code>.
	 * @throws IllegalArgumentException if one of the parameters is out of bounds
	 *
	 * @see #byte2floatInterleaved(byte[],int,float[],int,int,AudioFormat)
	 * @see #byte2float(byte[],int,List,int,int,AudioFormat)
	 */
	static void byte2floatGeneric(byte[] input, int inByteOffset, int inByteStep,
	                              float[] output, int outOffset, int sampleCount,
	                              AudioFormat format) {
		int formatType = getFormatType(format);

		byte2floatGeneric(input, inByteOffset, inByteStep,
	                          output, outOffset, sampleCount,
	                          formatType);
	}


	/**
	 * Central conversion function from
	 * a byte array to a normalized float array. In order to accomodate
	 * interleaved and non-interleaved
	 * samples, this method takes inByteStep as parameter which
	 * can be used to flexibly convert the data.
	 * <p>
	 * E.g.:<br>
	 * mono->mono: inByteStep=format.getFrameSize()<br>
	 * interleaved_stereo->interleaved_stereo: inByteStep=format.getFrameSize()/2,
	 *          sampleCount*2<br>
	 * stereo->2 mono arrays:<br>
	 * ---inByteOffset=0, outOffset=0, inByteStep=format.getFrameSize()<br>
	 * ---inByteOffset=format.getFrameSize()/2, outOffset=1, inByteStep=format.getFrameSize()<br>
	 */
	static void byte2floatGeneric(byte[] input, int inByteOffset, int inByteStep,
	                              float[] output, int outOffset, int sampleCount,
	                              int formatType) {
		//if (TDebug.TraceAudioConverter) {
		//    TDebug.out("FloatSampleTools.byte2floatGeneric, formatType="
		//           +formatType2Str(formatType));
		//}
		int endCount = outOffset + sampleCount;
		int inIndex = inByteOffset;
		for (int outIndex = outOffset; outIndex < endCount; outIndex++, inIndex+=inByteStep) {
			// do conversion
			switch (formatType) {
			case CT_8S:
				output[outIndex]=
				    ((float) input[inIndex])*invTwoPower7;
				break;
			case CT_8U:
				output[outIndex]=
				    ((float) ((input[inIndex] & 0xFF)-128))*invTwoPower7;
				break;
			case CT_16SB:
				output[outIndex]=
				    ((float) ((input[inIndex]<<8)
				              | (input[inIndex+1] & 0xFF)))*invTwoPower15;
				break;
			case CT_16SL:
				output[outIndex]=
				    ((float) ((input[inIndex+1]<<8)
				              | (input[inIndex] & 0xFF)))*invTwoPower15;
				break;
			case CT_24SB:
				output[outIndex]=
				    ((float) ((input[inIndex]<<16)
				              | ((input[inIndex+1] & 0xFF)<<8)
				              | (input[inIndex+2] & 0xFF)))*invTwoPower23;
				break;
			case CT_24SL:
				output[outIndex]=
				    ((float) ((input[inIndex+2]<<16)
				              | ((input[inIndex+1] & 0xFF)<<8)
				              | (input[inIndex] & 0xFF)))*invTwoPower23;
				break;
			case CT_32SB:
				output[outIndex]=
				    ((float) ((input[inIndex]<<24)
				              | ((input[inIndex+1] & 0xFF)<<16)
				              | ((input[inIndex+2] & 0xFF)<<8)
				              | (input[inIndex+3] & 0xFF)))*invTwoPower31;
				break;
			case CT_32SL:
				output[outIndex]=
				    ((float) ((input[inIndex+3]<<24)
				              | ((input[inIndex+2] & 0xFF)<<16)
				              | ((input[inIndex+1] & 0xFF)<<8)
				              | (input[inIndex] & 0xFF)))*invTwoPower31;
				break;
			default:
				throw new IllegalArgumentException
				("unsupported format="+formatType2Str(formatType));
			}
		}
	}

	// /////////////////// FLOAT 2 BYTE /////////////////////////////////// //

	private static byte quantize8(float sample, float ditherBits) {
		if (ditherBits!=0) {
			sample+=random.nextFloat()*ditherBits;
		}
		if (sample>=127.0f) {
			return (byte) 127;
		} else if (sample<=-128.0f) {
			return (byte) -128;
		} else {
			return (byte) (sample<0?(sample-0.5f):(sample+0.5f));
		}
	}

	private static int quantize16(float sample, float ditherBits) {
		if (ditherBits!=0) {
			sample+=random.nextFloat()*ditherBits;
		}
		if (sample>=32767.0f) {
			return 32767;
		} else if (sample<=-32768.0f) {
			return -32768;
		} else {
			return (int) (sample<0?(sample-0.5f):(sample+0.5f));
		}
	}

	private static int quantize24(float sample, float ditherBits) {
		if (ditherBits!=0) {
			sample+=random.nextFloat()*ditherBits;
		}
		if (sample>=8388607.0f) {
			return 8388607;
		} else if (sample<=-8388608.0f) {
			return -8388608;
		} else {
			return (int) (sample<0?(sample-0.5f):(sample+0.5f));
		}
	}

	private static int quantize32(float sample, float ditherBits) {
		if (ditherBits!=0) {
			sample+=random.nextFloat()*ditherBits;
		}
		if (sample>=2147483647.0f) {
			return 2147483647;
		} else if (sample<=-2147483648.0f) {
			return -2147483648;
		} else {
			return (int) (sample<0?(sample-0.5f):(sample+0.5f));
		}
	}


	/**
	 * Conversion function to convert a non-interleaved float audio data to
	 * an interleaved byte array. The float arrays contains normalized
	 * samples in the range [-1.0f, +1.0f]. The output array
	 * will receive bytes in the format specified in <code>format</code>.
	 * Exactly <code>format.getChannels()</code> channels are converted
	 * regardless of the number of elements in <code>input</code>. If <code>input</code>
	 * does not provide enough channels, an </code>IllegalArgumentException<code> is thrown.
	 * <p>
	 * Only PCM formats are accepted. The method will convert all
	 * samples from <code>input(n)[inOffset]</code> to
	 * <code>input(n)[inOffset + frameCount - 1]</code>
	 * to byte values from <code>output[outByteOffset]</code> to
	 * <code>output[outByteOffset + (frameCount * format.getFrameSize()) - 1]</code>
	 * <p>
	 * Dithering should be used when the output resolution is significantly
	 * lower than the original resolution. This includes if the original
	 * data was 16-bit and it is now converted to 8-bit, or if the
	 * data was generated in the float domain. No dithering need to be used
	 * if the original sample data was in e.g. 8-bit and the resulting output
	 * data has a higher resolution. If dithering is used, a sensitive value
	 * is DEFAULT_DITHER_BITS.
	 *
	 * @param input a List of float arrays with the input audio data
	 * @param inOffset index in the input arrays where to start the conversion
	 * @param output the byte array that receives the converted audio data
	 * @param outByteOffset the start offset in <code>output</code>
	 * @param frameCount number of frames to be converted.
	 * @param format the output format. Only packed PCM is allowed
	 * @param ditherBits if 0, do not dither. Otherwise the number of bits to be dithered
	 * @throws IllegalArgumentException if one of the parameters is out of bounds
	 *
	 * @see #DEFAULT_DITHER_BITS
	 * @see #float2byteInterleaved(float[],int,byte[],int,int,AudioFormat,float)
	 */
	//public static void float2byte(List<float[]> input, int inOffset,
	@SuppressWarnings("unchecked")
	public static void float2byte(List input, int inOffset,
	                              byte[] output, int outByteOffset,
	                              int frameCount,
	                              AudioFormat format, float ditherBits) {
		for (int channel = 0; channel < format.getChannels(); channel++) {
			float[] data = (float[]) input.get(channel);
			float2byteGeneric(data, inOffset,
	                                  output, outByteOffset, format.getFrameSize(),
	                                  frameCount, format, ditherBits);
			outByteOffset += format.getFrameSize() / format.getChannels();
		}
	}

	/**
	 * Conversion function to convert an interleaved float array to
	 * an interleaved byte array. The float array contains normalized
	 * samples in the range [-1.0f, +1.0f]. The output array
	 * will receive bytes in the format specified in <code>format</code>.
	 * <p>
	 * Only PCM formats are accepted. The method will convert all
	 * samples from <code>input[inOffset]</code> to
	 * <code>input[inOffset + (frameCount * format.getChannels()) - 1]</code>
	 * to byte values from <code>output[outByteOffset]</code> to
	 * <code>output[outByteOffset + (frameCount * format.getFrameSize()) - 1]</code>
	 * <p>
	 * Dithering should be used when the output resolution is significantly
	 * lower than the original resolution. This includes if the original
	 * data was 16-bit and it is now converted to 8-bit, or if the
	 * data was generated in the float domain. No dithering need to be used
	 * if the original sample data was in e.g. 8-bit and the resulting output
	 * data has a higher resolution. If dithering is used, a sensitive value
	 * is DEFAULT_DITHER_BITS.
	 *
	 * @param input the audio data in normalized samples
	 * @param inOffset index in input where to start the conversion
	 * @param output the byte array that receives the converted audio data
	 * @param outByteOffset the start offset in <code>output</code>
	 * @param frameCount number of frames to be converted.
	 * @param format the output format. Only packed PCM is allowed
	 * @param ditherBits if 0, do not dither. Otherwise the number of bits to be dithered
	 * @throws IllegalArgumentException if one of the parameters is out of bounds
	 *
	 * @see #DEFAULT_DITHER_BITS
	 * @see #float2byte(List,int,byte[],int,int,AudioFormat,float)
	 */
	public static void float2byteInterleaved(float[] input, int inOffset,
	                                         byte[] output, int outByteOffset,
	                                         int frameCount,
	                                         AudioFormat format, float ditherBits) {
		float2byteGeneric(input, inOffset,
	                          output, outByteOffset, format.getFrameSize() / format.getChannels(),
	                          frameCount * format.getChannels(),
	                          format, ditherBits);
	}



	/**
	 * Generic conversion function to convert a float array to
	 * a byte array.
	 * <p>
	 * Only PCM formats are accepted. The method will convert all
	 * samples from <code>input[inOffset]</code> to
	 * <code>input[inOffset+sampleCount-1]</code>
	 * to byte values from <code>output[outByteOffset]</code> to
	 * <code>output[outByteOffset + (sampleCount * (outByteStep - 1)]</code>.
	 * <p>
	 * The <code>format</code>'s channel count is ignored.
	 * <p>
	 * For mono data, set <code>outByteOffset</code> to <code>format.getFrameSize()</code>.<br>
	 * For converting interleaved input data, multiply <code>sampleCount</code>
	 *    by the number of channels and set outByteStep to
	 *    <code>format.getFrameSize() / format.getChannels()</code>.
	 *
	 * @param sampleCount number of samples in input to be converted.
	 * @param outByteStep how many bytes advance for each input sample in <code>input</code>.
	 * @throws IllegalArgumentException if one of the parameters is out of bounds
	 *
	 * @see #float2byteInterleaved(float[],int,byte[],int,int,AudioFormat,float)
	 * @see #float2byte(List,int,byte[],int,int,AudioFormat,float)
	 */
	static void float2byteGeneric(float[] input, int inOffset,
	                              byte[] output, int outByteOffset, int outByteStep,
	                              int sampleCount,
	                              AudioFormat format, float ditherBits) {
		int formatType = getFormatType(format);

		float2byteGeneric(input, inOffset,
	                           output, outByteOffset, outByteStep,
	                           sampleCount,
	                           formatType, ditherBits);
	}


	/**
	 * Central conversion function from normalized float array to
	 * a byte array. In order to accomodate interleaved and non-interleaved
	 * samples, this method takes outByteStep as parameter which
	 * can be used to flexibly convert the data.
	 * <p>
	 * E.g.:<br>
	 * mono->mono: outByteStep=format.getFrameSize()<br>
	 * interleaved stereo->interleaved stereo: outByteStep=format.getFrameSize()/2, sampleCount*2<br>
	 * 2 mono arrays->stereo:<br>
	 * ---inOffset=0, outByteOffset=0, outByteStep=format.getFrameSize()<br>
	 * ---inOffset=1, outByteOffset=format.getFrameSize()/2, outByteStep=format.getFrameSize()<br>
	 */
	static void float2byteGeneric(float[] input, int inOffset,
	                              byte[] output, int outByteOffset, int outByteStep,
	                              int sampleCount, int formatType, float ditherBits) {
		//if (TDebug.TraceAudioConverter) {
		//    TDebug.out("FloatSampleBuffer.float2byteGeneric, formatType="
		//               +"formatType2Str(formatType));
		//}

		if (inOffset < 0
		    || inOffset + sampleCount > input.length
		    || sampleCount < 0) {
		    	throw new IllegalArgumentException("invalid input index: "
		    	                                   +"input.length="+input.length
		    	                                   +" inOffset="+inOffset
		    	                                   +" sampleCount="+sampleCount);
		}
		if (outByteOffset < 0
		    || outByteOffset + (sampleCount * outByteStep) >= (output.length + outByteStep)
		    || outByteStep < getSampleSize(formatType)) {
		    	throw new IllegalArgumentException("invalid output index: "
		    	                                   +"output.length="+output.length
		    	                                   +" outByteOffset="+outByteOffset
		    	                                   +" outByteStep="+outByteStep
		    	                                   +" sampleCount="+sampleCount
		    	                                   +" format="+formatType2Str(formatType));
		}

		if (ditherBits!=0.0f && random==null) {
			// create the random number generator for dithering
			random=new Random();
		}
		int endSample = inOffset + sampleCount;
		int iSample;
		int outIndex = outByteOffset;
		for (int inIndex = inOffset;
		     inIndex < endSample;
		     inIndex++, outIndex+=outByteStep) {
			// do conversion
			switch (formatType) {
			case CT_8S:
				output[outIndex]=quantize8(input[inIndex]*twoPower7, ditherBits);
				break;
			case CT_8U:
				output[outIndex]=(byte) (quantize8((input[inIndex]*twoPower7), ditherBits)+128);
				break;
			case CT_16SB:
				iSample=quantize16(input[inIndex]*twoPower15, ditherBits);
				output[outIndex]=(byte) (iSample >> 8);
				output[outIndex+1]=(byte) (iSample & 0xFF);
				break;
			case CT_16SL:
				iSample=quantize16(input[inIndex]*twoPower15, ditherBits);
				output[outIndex+1]=(byte) (iSample >> 8);
				output[outIndex]=(byte) (iSample & 0xFF);
				break;
			case CT_24SB:
				iSample=quantize24(input[inIndex]*twoPower23, ditherBits);
				output[outIndex]=(byte) (iSample >> 16);
				output[outIndex+1]=(byte) ((iSample >>> 8) & 0xFF);
				output[outIndex+2]=(byte) (iSample & 0xFF);
				break;
			case CT_24SL:
				iSample=quantize24(input[inIndex]*twoPower23, ditherBits);
				output[outIndex+2]=(byte) (iSample >> 16);
				output[outIndex+1]=(byte) ((iSample >>> 8) & 0xFF);
				output[outIndex]=(byte) (iSample & 0xFF);
				break;
			case CT_32SB:
				iSample=quantize32(input[inIndex]*twoPower31, ditherBits);
				output[outIndex]=(byte) (iSample >> 24);
				output[outIndex+1]=(byte) ((iSample >>> 16) & 0xFF);
				output[outIndex+2]=(byte) ((iSample >>> 8) & 0xFF);
				output[outIndex+3]=(byte) (iSample & 0xFF);
				break;
			case CT_32SL:
				iSample=quantize32(input[inIndex]*twoPower31, ditherBits);
				output[outIndex+3]=(byte) (iSample >> 24);
				output[outIndex+2]=(byte) ((iSample >>> 16) & 0xFF);
				output[outIndex+1]=(byte) ((iSample >>> 8) & 0xFF);
				output[outIndex]=(byte) (iSample & 0xFF);
				break;
			default:
				throw new IllegalArgumentException
				("unsupported format="+formatType2Str(formatType));
			}
		}
	}
}
