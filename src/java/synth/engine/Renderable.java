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
 * An interface for objects that can render audio data. The objects
 * must maintain themselves the buffer, sample rate and number of samples
 * to be rendered in each render() call.
 * @author florian
 *
 */
public interface Renderable {

	/**
	 * Render the next buffer for the passed time. The method
	 * should keep track on its own of how many samples to render. 
	 * @param time the time of the buffer to be rendered.
	 * @return true if the method actually rendered. False if the Renderable
	 * has already rendered the buffer for the time, or if the Renderable is 
	 * already done
	 */
	public boolean render(AudioTime time);

	/**
	 * Checks if this Renderable has already rendered a buffer for the 
	 * specified time. This method should allow for 125microseconds grace time
	 * to compensate rounding errors (125us is 1 sample at 8000Hz).
	 * 
	 * @param currTime the time to be tested
	 * @return true if this Renderable has already rendered a block of audio 
	 * data for the specified time.
	 */
	public boolean alreadyRendered(AudioTime currTime);

}
