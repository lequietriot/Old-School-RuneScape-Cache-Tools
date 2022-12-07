/*
 * From jsresources.org:
 *
 * Copyright (c) 2001, 2004, 2005 by Florian Bomers
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

//package org.jsresources.utils.audio;
package synth.utils;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteOrder;

public class AudioUtils {

	private final static double EPSILON = 1.0e-9;
	private final static double MINUS_INFINITY = -1.0 / EPSILON;
	
	// TABLE-BASED CONVERSION
	
	private final static boolean USE_TABLES = true;
	private final static double DECIBEL_TABLE_MAX_BANDWITH = 120; // in dB
	
	/** how many entries in the table per decibel */
	private final static double DECIBEL_TABLE_SCALING_FACTOR = 32;
	
	private final static double NOTE_TABLE_MIN = -80; // in semitones
	private final static double NOTE_TABLE_MAX = 80; // in semitones
	
	/** how many entries in the table per semitone */
	private final static double NOTE_TABLE_SCALING_FACTOR = 64;

	static {
		if (USE_TABLES) {
			generateTables();
		}
	}

	
	// CONVERSION TIME <-> BYTES
	
	public final static long bytes2millis(long bytes, AudioFormat format) {
		return (long) (((double) bytes) / ((double) format.getFrameRate())
				* 1000.0 / ((double) format.getFrameSize()));
	}

	public final static long millis2bytes(long ms, AudioFormat format) {
		return ((long) (((double) ms) * ((double) format.getFrameRate()) / 1000.0))
				* ((long) format.getFrameSize());
	}

	public final static int bytes2millis(int bytes, AudioFormat format) {
		return (int) (((double) bytes) / ((double) format.getFrameRate())
				* 1000.0 / ((double) format.getFrameSize()));
	}

	public final static int millis2bytes(int ms, AudioFormat format) {
		return ((int) (((double) ms) * ((double) format.getFrameRate()) / 1000.0))
				* format.getFrameSize();
	}

	// CONVERSION TIME <-> SAMPLES
	
	public final static long samples2millis(long samples, double sampleRate) {
		return (long) (samples / sampleRate * 1000.0);
	}

	public final static long millis2samples(long ms, double sampleRate) {
		return (long) (((double) ms) * 0.001 * sampleRate);
	}

	public final static double samples2seconds(long samples, double sampleRate) {
		return (double) samples / sampleRate;
	}

	public final static long seconds2samples(double seconds, double sampleRate) {
		return (long) (seconds * sampleRate);
	}

	public final static long samples2nanos(long samples, double sampleRate) {
		return (long) (((double) samples) / sampleRate * 1000000000.0);
	}

	public final static long nanos2samples(long nanos, double sampleRate) {
		return (long) (nanos * 0.000000001 * sampleRate);
	}

	public final static long samples2micros(long samples, double sampleRate) {
		return (long) (((double) samples) / sampleRate * 1000000.0);
	}

	public final static long micros2samples(long nanos, double sampleRate) {
		return (long) (nanos * 0.000001 * sampleRate);
	}

	// AUDIO FORMAT UTILITIES
	
	public final static boolean isPCM(AudioFormat.Encoding enc) {
		return enc.equals(AudioFormat.Encoding.PCM_SIGNED)
				|| enc.equals(AudioFormat.Encoding.PCM_UNSIGNED);
	}

	public final static boolean isPCM(AudioFormat af) {
		return isPCM(af.getEncoding());
	}

	public final static boolean isAlmost(double a, double b) {
		return Math.abs(a - b) < EPSILON;
	}
	
	public final static boolean isSystemBigEndian() {
		return ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);
	}

	// CONVERSION DECIBEL <-> LINEAR FACTOR 
	
	/**
	 * Get the linear factor corresponding to the given decibel value.
	 * 
	 * @param decibels the gain in dB, [-inf...0...inf]
	 * @return the corresponding value as a linear factor [0...1...inf]
	 */
	public final static double decibel2linear(double decibels) {
		if (USE_TABLES) {
			if (decibels == 0.0) {
				return 1.0;
			} else if (decibels < 0) {
				if (decibels <= -DECIBEL_TABLE_MAX_BANDWITH) {
					return 0.0;
				}
				return decibel2table_table[(int) (decibels * -DECIBEL_TABLE_SCALING_FACTOR)];
			} else {
				if (decibels >= DECIBEL_TABLE_MAX_BANDWITH) {
					decibels = DECIBEL_TABLE_MAX_BANDWITH;
				}
				return 1.0/decibel2table_table[(int) (decibels * DECIBEL_TABLE_SCALING_FACTOR)];
			}
		} else {
			if (decibels <= MINUS_INFINITY) {
				return 0.0;
			}
			return calcDecibel2linear(decibels);
		}
	}

	/**
	 * Calculate the linear factor corresponding to the specified decibel level.
	 * @param decibels [-inf...0...inf]
	 * @return linear factor [0...1...inf]
	 */
	private final static double calcDecibel2linear(double decibels) {
		return Math.pow(10.0, decibels * 0.05);
	}

	/**
	 * Get decibel from a linear factor.
	 * 
	 * @param linearFactor 0..1..inf
	 * @return the converted decibel (-inf...0...inf)
	 */
	public final static double linear2decibel(double linearFactor) {
		if (isAlmost(linearFactor, 0.0)) {
			return MINUS_INFINITY;
		}
		// currently no need to optimize with tables, since not used
		return calcLinear2decibel(linearFactor);
	}

	/**
	 * Calculate the decibel level from the linear factor 
	 * @param linearFactor
	 * @return decibels
	 */
	private final static double calcLinear2decibel(double linearFactor) {
		return Math.log10(linearFactor) * 20.0;
	}

	// CONVERSION SEMITONE -> SAMPLERATE FACTOR

	/**
	 * Get the samplerate factor from the relative note. For example, if
	 * relative note is 12 (one octave higer), the sample rate factor is 2.0,
	 * because the samples have to be played twice as fast to achieve an
	 * increase of one octave.
	 * 
	 * @param relativeNote the note difference, in semitones
	 */
	public final static double getSamplerateFactorFromRelativeNote(
			double relativeNote) {
		
		if (USE_TABLES) {
			if (relativeNote < NOTE_TABLE_MIN) {
				relativeNote = NOTE_TABLE_MIN;
			} else if (relativeNote > NOTE_TABLE_MAX) {
				relativeNote = NOTE_TABLE_MAX;
			}
			return note2factor_table[(int) ((relativeNote - NOTE_TABLE_MIN) * NOTE_TABLE_SCALING_FACTOR)];
		} else {
			return calcSamplerateFactorFromRelativeNote(relativeNote);
		}
	}
	
	private final static double calcSamplerateFactorFromRelativeNote(
			double relativeNote) {
		// optimize by substituting a division by a multiplication
		// = relativeNote / 12.0
		return Math.pow(2.0, relativeNote * 0.0833333333333333333333);
	}

	// TABLE SUPPORT
	
	private static double[] decibel2table_table;
	private static double[] note2factor_table;
	
	/**
	 * Creates tables for linear<->decibel and relativeNote->samplerateFactor conversion 
	 */
	private static void generateTables() {
		// generate decibel2linear table
		int max = (int) (DECIBEL_TABLE_MAX_BANDWITH * DECIBEL_TABLE_SCALING_FACTOR);
		decibel2table_table = new double[max + 1];
		for (int i = 0; i <= max; i++) {
			double dB = ((double) i) / -DECIBEL_TABLE_SCALING_FACTOR;
			decibel2table_table[i] = calcDecibel2linear(dB);
		}
		// generate note2factor table
		int min = (int) (NOTE_TABLE_MIN * NOTE_TABLE_SCALING_FACTOR);
		max = (int) (NOTE_TABLE_MAX * NOTE_TABLE_SCALING_FACTOR);
		note2factor_table = new double[max - min + 1];
		for (int i = min; i <= max; i++) {
			double note = ((double) i) / NOTE_TABLE_SCALING_FACTOR;
			note2factor_table[i - min] = calcSamplerateFactorFromRelativeNote(note);
		}
		
	}

}
