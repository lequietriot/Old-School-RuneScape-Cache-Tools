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
 * Container for a sample, the lowest element in the SoundFont2 synthesis
 * hierarchy.
 * 
 * @author florian
 * 
 */
public class SoundFontSample {

	public final static int MONO_SAMPLE = 1;
	public final static int RIGHT_SAMPLE = 2;
	public final static int LEFT_SAMPLE = 4;
	public final static int LINKED_SAMPLE = 8;
	public final static int ROM_SAMPLE_FLAG = 0x8000;

	public final static int SAMPLETYPE_MASK =
			(MONO_SAMPLE | RIGHT_SAMPLE | LEFT_SAMPLE | LINKED_SAMPLE);

	private String name;
	private int start;
	private int end;
	private int startLoop;
	private int endLoop;
	private double sampleRate;
	private int originalPitch;
	private int pitchCorrection;
	private int sampleLinkIndex;
	// private SoundFontSample sampleLink;
	private int sampleType;

	public SoundFontSample(String name, int start, int end, int startLoop,
			int endLoop, double sampleRate, int originalPitch,
			int pitchCorrection, int sampleLinkIndex, int sampleType) {
		this.name = name;
		this.start = start;
		this.end = end;
		this.startLoop = startLoop;
		this.endLoop = endLoop;
		this.sampleRate = sampleRate;
		this.originalPitch = originalPitch;
		this.pitchCorrection = pitchCorrection;
		this.sampleLinkIndex = sampleLinkIndex;
		this.sampleType = sampleType;
	}

	/**
	 * @return Returns the end sample point.
	 */
	public int getEnd() {
		return end;
	}

	/**
	 * @return Returns the endLoop sample point.
	 */
	public int getEndLoop() {
		return endLoop;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Returns the originalPitch.
	 */
	public int getOriginalPitch() {
		return originalPitch;
	}

	/**
	 * @return Returns the pitchCorrection in cents.
	 */
	public int getPitchCorrection() {
		return pitchCorrection;
	}

	/**
	 * Package private field, since not used at run-time.
	 * 
	 * @return Returns the sampleLinkIndex index.
	 */
	int getSampleLinkIndex() {
		return sampleLinkIndex;
	}

	/**
	 * @return Returns the sampleRate.
	 */
	public double getSampleRate() {
		return sampleRate;
	}

	/**
	 * @return Returns the sampleType.
	 */
	public int getSampleType() {
		return sampleType;
	}

	/**
	 * @return Returns the start sample point.
	 */
	public int getStart() {
		return start;
	}

	/**
	 * @return Returns the startLoop sample point.
	 */
	public int getStartLoop() {
		return startLoop;
	}

	/**
	 * @return if the values of this sample are consistent with the number of
	 *         available sample data points and with the number of samples
	 */
	public boolean isConsistent(int numSampleDataPoints, int numSamples) {
		return (start >= 0 && start < numSampleDataPoints)
				&& (end >= start && end < numSampleDataPoints)
				&& (startLoop >= start && endLoop >= startLoop && endLoop <= end)
				&& (sampleRate > 0)
				&& (((sampleType & MONO_SAMPLE) == MONO_SAMPLE) || (sampleLinkIndex >= 0 && sampleLinkIndex < numSamples))
				&& ((sampleType & (SAMPLETYPE_MASK ^ 0xFFFFFFFF)) == 0);
	}

	public static String sampleType2string(int sampleType) {
		String sampleTypeText;
		switch (sampleType & 0x7FFF) {
		case MONO_SAMPLE:
			sampleTypeText = "MONO";
			break;
		case RIGHT_SAMPLE:
			sampleTypeText = "RIGHT";
			break;
		case LEFT_SAMPLE:
			sampleTypeText = "LEFT";
			break;
		case LINKED_SAMPLE:
			sampleTypeText = "LEFT";
			break;
		default:
			sampleTypeText = "" + sampleType;
		}
		if ((sampleType & ROM_SAMPLE_FLAG) != 0) {
			sampleTypeText = "ROM " + sampleTypeText;
		}
		return sampleTypeText;
	}

	public String toString() {
		String sampleLinkText;
		// if (sampleLink != null) {
		// sampleLinkText="sampleLink='"+sampleLink.getName()+"'";
		// } else {
		sampleLinkText = "sampleLinkIndex=" + sampleLinkIndex;
		// }

		return "Sample: name=" + name + " start=" + start + " end=" + end
				+ " startLoop=" + startLoop + " endLoop=" + endLoop
				+ " sampleRate=" + sampleRate + " originalPitch="
				+ originalPitch + " pitchCorrection=" + pitchCorrection + " "
				+ sampleLinkText + " type=" + sampleType2string(sampleType);
	}

}
