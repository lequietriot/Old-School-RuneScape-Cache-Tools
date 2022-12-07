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

/**
 * Master interface for all classes providing audio data.
 * 
 * @author florian
 */
public interface AudioInput {

	/**
	 * Method 1 to fill the buffer. By definition, the read method will <b>add</b> the data
	 * to buffer, i.e. each sample value is added to any existing value.
	 * 
	 * @param buffer - the buffer to be added to
	 * @param time - the start playback time of this buffer
	 * @param offset - the offset in buffer where to start writing samples
	 * @param count - how many samples to read to the buffer
	 */
	public void read(AudioTime time, AudioBuffer buffer, int offset, int count);

	/**
	 * Method 2 to fill a buffer. This method returns a buffer instance. 
	 * Ownership of the returned buffer is passed away.
	 * If this AudioInput cannot provide <code>sampleCount</code> samples,
	 * the returned buffer will have its sample count set to a lower
	 * number. This method must never return a buffer with more than
	 * <code>sampleCount</code> samples. The returned buffer must have exactly
	 * <code>channelCount</code> channels and a sample rate of <code>sampleRate</code>.
	 * 
	 * @param time - the start playback time of this buffer
	 * @param sampleCount  - how many samples the returned buffer must have
	 * @param channelCount - the number of channels for the returned buffer
	 * @param sampleRate - the sample rate of the returned buffer 
	 * @return an AudioBuffer instance with the read audio data
	 */
	public AudioBuffer read(AudioTime time, int sampleCount, int channelCount, double sampleRate);

	/**
	 * Returns true if this input stream has finished rendering data and further
	 * calls to read would just return silence.
	 */
	public boolean done();
}
