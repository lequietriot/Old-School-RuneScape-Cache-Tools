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

import static synth.utils.AudioUtils.nanos2samples;
import static synth.utils.AudioUtils.samples2nanos;

/**
 * An abstraction of a time value.
 * 
 * @author florian
 */
public class AudioTime implements Comparable<AudioTime> {
	private long nanosecond;

	public AudioTime(long nanosecond) {
		this.nanosecond = nanosecond;
	}

	public AudioTime(long samples, double sampleRate) {
		this.nanosecond = samples2nanos(samples, sampleRate);
	}

	public long getNanoTime() {
		return nanosecond;
	}

	public long getMicroTime() {
		return nanosecond / 1000L;
	}

	public long getMillisTime() {
		return nanosecond / 1000000L;
	}

	public double getSecondsTime() {
		return nanosecond / 1000000000.0;
	}

	public long getSamplesTime(double sampleRate) {
		return nanos2samples(nanosecond, sampleRate);
	}

	public AudioTime add(long nanos) {
		return new AudioTime(getNanoTime() + nanos);
	}

	public AudioTime add(AudioTime at) {
		return new AudioTime(getNanoTime() + at.getNanoTime());
	}

	/**
	 * @return this - nanos
	 */
	public AudioTime subtract(long nanos) {
		return new AudioTime(getNanoTime() - nanos);
	}

	/**
	 * @return this time - at
	 */
	public AudioTime subtract(AudioTime at) {
		return new AudioTime(getNanoTime() - at.getNanoTime());
	}

	public boolean earlierThan(AudioTime at) {
		return compareTo(at) < 0;
	}

	public boolean earlierOrEqualThan(AudioTime at) {
		return compareTo(at) <= 0;
	}

	public boolean laterThan(AudioTime at) {
		return compareTo(at) > 0;
	}

	public boolean laterOrEqualThan(AudioTime at) {
		return compareTo(at) >= 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(AudioTime at) {
		long comp = getNanoTime() - at.getNanoTime();
		return (comp < 0) ? -1 : ((comp == 0) ? 0 : 1);
	}

	/** display this audio time in microseconds */
	public String toString() {
		StringBuffer sb = new StringBuffer(Long.toString(getNanoTime() / 1000));
		int length = sb.length();
		int cmp = 0;
		if (sb.charAt(0) == '-') {
			cmp = 1;
		}
		char sep = '.';
		if (length > 3 + cmp) {
			sb.insert(length - 3, sep);
			if (length > 6 + cmp) {
				sb.insert(length - 6, sep);
				// if (length > 9 + cmp) {
				// sb.insert(length-9, sep);
				// }
			}
		}
		sb.append("us");
		return sb.toString();
	}

}
