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
 * Master interface for all classes providing a final place for audio samples.
 * It provides a clock so that the audio time can serve as clock provider.
 * 
 * @author florian
 */
public interface AudioSink extends AdjustableAudioClock {

	/**
	 * write audio data to this audio sink. This method is blocking, i.e. it
	 * only returns when all data is written or this sink is closed.
	 * 
	 * @param buffer
	 */
	public void write(AudioBuffer buffer);

	/**
	 * @return true if the sink is open and ready to accept audio data
	 */
	public boolean isOpen();

	/**
	 * Close the device
	 */
	public void close();
	
	/**
	 * @return number of audio channels of this sink
	 */
	public int getChannels();

	/**
	 * @return the number of samples that this devices preferably writes at once
	 */
	public int getBufferSize();

	/**
	 * @return the sample rate at which this sink expects audio data be written
	 */
	public double getSampleRate();

}
