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

import synth.engine.Soundbank;

import java.util.ArrayList;
import java.util.List;

/**
 * A representation of a bank. A bank has a bank ID and 128 programs (presets).
 * 
 * @author florian
 * 
 */
public class SoundFontBank implements Comparable<SoundFontBank>, Soundbank.Bank {

	private int bank;
	private SoundFontPreset[] presets;

	public SoundFontBank(int bank) {
		this.bank = bank;
	}

	/**
	 * @return Returns the bank number.
	 */
	public int getMidiNumber() {
		return bank;
	}

	/**
	 * @return Returns an array of 128 presets (aka instruments). Some elements may be null.
	 */
	public SoundFontPreset[] getPresets() {
		if (presets == null) {
			presets = new SoundFontPreset[128];
		}
		return presets;
	}
	
	/**
	 * @return the list of all existing presets in this bank, up to 128.
	 */
	public List<Soundbank.Instrument> getInstruments() {
		List<Soundbank.Instrument> list = new ArrayList<Soundbank.Instrument>(128);
		SoundFontPreset[] ps = getPresets();
		for (SoundFontPreset p : ps) {
			if (p != null) {
				list.add(p);
			}
		}
		return list;
	}

	/**
	 * @return Returns the specified preset, or null if the preset does not exist in this bank.
	 */
	public SoundFontPreset getPreset(int index) {
		return getPresets()[index];
	}

	/**
	 * Set a specified preset.
	 * @param index the index of the preset [0..127] 
	 */
	public void setPreset(int index, SoundFontPreset preset) {
		getPresets()[index] = preset;
	}

	/**
	 * From an ordered list of SoundFontBanks, find the one identified by thisBank.
	 * 
	 * @return the index of the found SoundFontBank, or -1 if not found
	 */
	public static int findBank(List<SoundFontBank> banks, int thisBank) {
		// TODO: if more than 5 banks, use binary search
		return banks.indexOf(new SoundFontBank(thisBank));
	}

	public boolean equals(SoundFontBank sfb) {
		return (bank == sfb.getMidiNumber());
	}

	public boolean equals(Object o) {
		if (o instanceof SoundFontBank) {
			return (((SoundFontBank) o).getMidiNumber() == bank);
		} else if (o instanceof Integer) {
			return (((Integer) o).intValue() == bank);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(SoundFontBank arg0) {
		return bank - arg0.getMidiNumber();
	}

}
