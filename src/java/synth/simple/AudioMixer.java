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
package synth.simple;

import java.util.ArrayList;
import java.util.List;

/**
 * An object that takes an arbitrary number of
 * input audio streams and renders them to the
 * output buffer.
 */
public class AudioMixer implements AudioInput {

	/**
	 * Collection of currently active input streams
	 */
	private List<AudioInput> streams;
	
	/**
	 * Create an instance of a mixer
	 */
	public AudioMixer(int channels, float sampleRate) {
		streams = new ArrayList<AudioInput>();
	}

	/**
	 * The temporary buffer that is used to read the individual input streams.
	 */
	FloatSampleBuffer tempBuffer = new FloatSampleBuffer(2, 0, 44100.0f);
			

	/**
	 * The actual mixing function
	 */
	public void read(FloatSampleBuffer buffer) {
		// get a local copy of the input streams
		AudioInput[] localStreams;
		synchronized(streams) {
			localStreams = streams.toArray(new AudioInput[0]);
		}
		// setup the temporary buffer
		tempBuffer.init(buffer.getChannelCount(), buffer.getSampleCount(), buffer.getSampleRate());
		
		// empty the buffer passed in
		buffer.makeSilence();
		// iterate over all registered input streams
		for (AudioInput stream : localStreams) {
			// read from this source stream
			stream.read(tempBuffer);
			// add/mix to mix buffer
			for (int channel = 0; channel < buffer.getChannelCount(); channel++) {
				float[] data = buffer.getChannel(channel);
				float[] tempData = tempBuffer.getChannel(channel);
				for (int i=0; i<buffer.getSampleCount(); i++) {
					data[i] += tempData[i];
				}
			}
		}
	}
	
	public boolean done() {
		return false;
	}
	
	public void addAudioStream(AudioInput stream) {
		synchronized(streams) {
			streams.add(stream);
		}
		//Debug.debug("Mixer: added audio stream -- now "+streams.size()+" streams.");
	}

	public void removeAudioStream(AudioInput stream) {
		synchronized(streams) {
			streams.remove(stream);
		}
		//Debug.debug("Mixer: removed audio stream -- now "+streams.size()+" streams.");
	}
	
	public List<AudioInput> getAudioStreams() {
		// TODO: should really return a copy
		return streams;
	}

}
