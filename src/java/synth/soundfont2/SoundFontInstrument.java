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

import java.util.ArrayList;
import java.util.List;

/**
 * Container for a SoundFont instrument with its zones.
 * 
 * @author florian
 * 
 */
public class SoundFontInstrument {
	private String name;

	private SoundFontInstrumentZone[] zones;

	public SoundFontInstrument(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Returns the zones.
	 */
	public SoundFontInstrumentZone[] getZones() {
		return zones;
	}

	/**
	 * @param zones The zones to set.
	 */
	protected void setZones(SoundFontInstrumentZone[] zones) {
		this.zones = zones;
	}

	/**
	 * @return a matching zone, or null if none found. This method does not return zones without a sample.
	 */
	public List<SoundFontInstrumentZone> getZones(int note, int vel) {
		List<SoundFontInstrumentZone> result = null;
		for (SoundFontInstrumentZone zone : zones) {
			if (zone.getSample()!=null && zone.matches(note, vel)) {
				if (result == null) {
					result = new ArrayList<SoundFontInstrumentZone>(2);
				}
				result.add(zone);
			}
		}
		return result;
	}
	
	/**
	 * @return the global zone of this instrument, or null if none exists
	 */
	public SoundFontInstrumentZone getGlobalZone() {
		if (zones.length > 0 && zones[0].isGlobalZone()) {
			return zones[0];
		}
		return null;
	}

	public String toString() {
		return "Instrument: name=" + name + " with " + zones.length + " zones.";
	}
}
