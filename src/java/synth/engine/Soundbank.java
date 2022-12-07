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

import java.util.List;

/**
 * Master interface for soundbank implementations.
 * 
 * @author florian
 * 
 */
public interface Soundbank {
	
	/**
	 * Return the name of this soundbank
	 */
	public String getName();
	
	/**
	 * This method is called when a note is to be played. The implementation
	 * needs to find the correct patch from the soundbank for the channel, given
	 * in the descriptor, matching the given note and velocity.
	 * <p>
	 * The soundbank can also create linked instances of NoteInput in this method.
	 * 
	 * @param note the note to be played (0..127)
	 * @param vel the velocity at which the note is played (1..127)
	 * @return a (possibly linked) instance of NoteInput, or null if no instrument exists for
	 *    this channel's program/bank, this note, or this velocity.
	 */
	public NoteInput createNoteInput(Synthesizer.Params params,
			AudioTime time, MidiChannel channel, int note, int vel);
	
	/**
	 * Returns a list of the existing Banks
	 */
	public List<Bank> getBanks();
	
	/**
	 * A descriptor of a Bank in a soundbank
	 */
	public interface Bank {
		/**
		 * @return the MIDI bank number of this bank [0..16383]
		 */
		public int getMidiNumber();
		/**
		 * @return the list of instruments in this bank. Up to 128 instruments.
		 */
		public List<Instrument> getInstruments();
	}

	/**
	 * A descriptor of an Instrument in a Bank
	 */
	public interface Instrument {
		/**
		 * @return the MIDI program number of this instrument [0..127]
		 */
		public int getMidiNumber();
		/**
		 * @return the name of this instrument
		 */
		public String getName();
	}
}
