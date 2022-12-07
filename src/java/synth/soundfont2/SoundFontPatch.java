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
package synth.soundfont2;

import synth.engine.Patch;

public class SoundFontPatch extends Patch {
	
	private int note;
	private int velocity;

	public SoundFontPatch(int note, int velocity, int bank, int program, SoundFontSample sample) {
		this.note = note;
		this.velocity = velocity;
		this.bank = bank;
		this.program = program;
		this.rootKey = sample.getOriginalPitch();
	}
	
	/**
	 * Set the note number. This is used to move a keynum generator override
	 * to the NoteInput instance. In that case, triggerNote and note are different.
	 * @param note
	 */
	void setNote(int note) {
		this.note = note;
	}
	
	int getNote() {
		return note;
	}

	/**
	 * Set the velocity. See setNote for explanation.
	 * @param vel
	 */
	void setVelocity(int vel) {
		this.velocity = vel;
	}
	
	int getVelocity() {
		return velocity;
	}
	
	void setRootKey(int rootKey) {
		this.rootKey = rootKey;
	}

	/**
	 * @param exclusiveLevel The exclusiveLevel to set.
	 */
	void setExclusiveLevel(int exclusiveLevel) {
		this.exclusiveLevel = exclusiveLevel;
	}
}
