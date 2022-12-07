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
 * Generic interface for classes providing MIDI input. It provides a clock so
 * that the MIDI time can serve as clock provider.
 * 
 * @author florian
 * 
 */
public interface MidiIn extends AdjustableAudioClock {

	/**
	 * Set the listener
	 */
	public void addListener(Listener L);

	/**
	 * remove the listener
	 */
	public void removeListener(Listener L);
	
	/**
	 * @return the Index of this MIDI IN instance
	 */
	public int getInstanceIndex();

	/**
	 * @return true if the device is currently open
	 */
	public boolean isOpen();
	
	/**
	 * Close the device.
	 */
	public void close();

	/** if false, all MIDI events will have time stamp 0 */
	public void setTimestamping(boolean value);

	/** @return the current status of time stamping MIDI events */
	public boolean isTimestamping();

	/**
	 * The listener interface that needs to be implemented by classes that want
	 * to receive MIDI messages.
	 */
	public interface Listener {
		// have channel a single parameter to allow more than 16 channels
		/**
		 * Sent to the listener upon incoming MIDI data.
		 * 
		 * @param event the received event 
		 */
		public void midiInReceived(MidiEvent event);
	}
}
