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
 * Class to hold all data for a Preset in a soundfont file.
 * In MIDI terms, a preset is an instrument, patch, or program.
 * 
 * @author florian
 * 
 */
public class SoundFontPreset implements Soundbank.Instrument {
	private String name;
	private int program;
	private int bank;

	private SoundFontPresetZone[] zones;

	public SoundFontPreset(String name, int program, int bank) {
		this.name = name;
		this.program = program;
		this.bank = bank;
	}

	/**
	 * @return Returns the bank.
	 */
	public int getBank() {
		return bank;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Returns the program number.
	 */
	public int getMidiNumber() {
		return program;
	}

	/**
	 * @return Returns the preset zones.
	 */
	public SoundFontPresetZone[] getZones() {
		return zones;
	}

	/**
	 * @param zones The preset zones to set.
	 */
	protected void setZones(SoundFontPresetZone[] zones) {
		this.zones = zones;
	}

	/**
	 * @return the matching zones, or null if none found. This method does not
	 *         return zones without attached instruments.
	 */
	public List<SoundFontPresetZone> getZones(int note, int vel) {
		List<SoundFontPresetZone> result = null;
		for (SoundFontPresetZone zone : zones) {
			if (zone.getInstrument()!=null && zone.matches(note, vel)) {
				if (result == null) {
					result = new ArrayList<SoundFontPresetZone>(2);
				}
				result.add(zone);
			}
		}
		return result;
	}

	/**
	 * @return the global zone of this preset, or null if none exists
	 */
	public SoundFontPresetZone getGlobalZone() {
		if (zones.length > 0 && zones[0].isGlobalZone()) {
			return zones[0];
		}
		return null;
	}

	public String toString() {
		String ret = "Preset: name=" + name + " program=" + program + " bank="
				+ bank;
		if (zones != null) {
			ret += " with " + zones.length + " zones";
		}
		return ret;
	}
}
