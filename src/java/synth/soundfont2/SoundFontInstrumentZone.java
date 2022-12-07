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
 * A zone on instrument level. This zone typically defines the sample to be used
 * by this instrument in this zone.
 * 
 * @author florian
 * 
 */
public class SoundFontInstrumentZone extends SoundFontZone {

	/**
	 * The associated sample for this zone. Will be null for global zones or
	 * preset zones.
	 */
	private SoundFontSample sample;
	
	/**
	 * The associated zone. If this zone is the master, the linked
	 * zone is the master and vice versa.
	 */
	private SoundFontInstrumentZone zoneLink;

	/**
	 * Constructor for an instrument zone
	 * 
	 * @param generators
	 * @param modulators
	 * @param sample
	 */
	public SoundFontInstrumentZone(SoundFontGenerator[] generators,
			SoundFontModulator[] modulators, SoundFontSample sample) {
		super(generators, modulators);
		this.sample = sample;
	}

	/**
	 * @return Returns the sample.
	 */
	public SoundFontSample getSample() {
		return sample;
	}
	
	/**
	 * @return Returns the zoneLink.
	 */
	public SoundFontInstrumentZone getZoneLink() {
		return zoneLink;
	}

	/**
	 * Set this zone as being the master zone in a linked
	 * list of zones (currently only supported for stereo links).
	 * Calling this method will invalidate the other zone so that it
	 * will not be used directly anymore.
	 */
	public void setMasterZone(SoundFontInstrumentZone slaveZone) {
		zoneLink = slaveZone;
		slaveZone.zoneLink = this;
		slaveZone.makeInaccessible();
	}
	
	public boolean isValid() {
		return (sample != null) && (keyMin>=0);
	}

	public boolean isGlobalZone() {
		return (keyMin >= 0) && (sample == null);
	}

	public String toString() {
		String ret = super.toString();
		if (sample != null) {
			ret += " using sample: " + sample.getName();
		}
		return ret;
	}

}
