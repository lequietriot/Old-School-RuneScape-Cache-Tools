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

/**
 * A zone on preset level. The preset zones typically define the instrument to
 * be used in this preset for this zone.
 * 
 * @author florian
 * 
 */
public class SoundFontPresetZone extends SoundFontZone {

	/**
	 * The associated instrument for this zone. Will be null for global zones or
	 * instrument zones.
	 */
	private SoundFontInstrument instrument;

	/**
	 * Constructor for a preset zone
	 * 
	 * @param generators
	 * @param modulators
	 * @param instrument
	 */
	public SoundFontPresetZone(SoundFontGenerator[] generators,
			SoundFontModulator[] modulators, SoundFontInstrument instrument) {
		super(generators, modulators);
		this.instrument = instrument;
	}

	/**
	 * @return Returns the instrument.
	 */
	public SoundFontInstrument getInstrument() {
		return instrument;
	}

	public boolean isGlobalZone() {
		return (keyMin >= 0) && (instrument == null);
	}

	public String toString() {
		String ret = super.toString();
		if (instrument != null) {
			ret += " using inst: " + instrument.getName();
		}
		return ret;
	}
}
