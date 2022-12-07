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
 * Runtime Articulation data for a note. It keeps track of the various state
 * variables.
 * 
 * @author florian
 * 
 */
public abstract class Articulation {

	/**
	 * a local reference to the channel used for this Note's articulation
	 */
	private MidiChannel channel;

	/**
	 * a local reference to the patch used for this Note's articulation
	 */
	private Patch patch;

	/**
	 * An offset, in semitones, added to the pitch of the note. This comes from
	 * the soundbank's instrument definitions.
	 */
	private double initialPitchOffset = 0.0;

	/**
	 * The combined panorama and volume. Call calcVolumeFactor to set it from
	 * internal values
	 */
	protected double[] effectiveLinearVolume = new double[2];

	/**
	 * Should be called by the constructors of subclasses.
	 * 
	 * @param patch
	 * @param channel
	 */
	protected void init(AudioTime time, Patch patch, MidiChannel channel) {
		this.patch = patch;
		this.channel = channel;
	}

	/**
	 * Called by NoteInput's constructor
	 * 
	 * @param note
	 * @param vel
	 */
	public void setup(int note, int vel) {
		calcEffectiveVolumeFactor();
	}

	/**
	 * Calculate the new volumeFactor and sampleRateFactor.
	 * 
	 * @param time
	 */
	public abstract void calculate(AudioTime time);

	/**
	 * Apply further processing to the rendered buffer (like filters).
	 * @param buffer the sample buffer
	 */
	public abstract void process(AudioBuffer buffer);

	/**
	 * Must be called when releasing the note.
	 * 
	 * @param time
	 */
	public abstract void release(AudioTime time);

	/**
	 * @return Returns the channel.
	 */
	public MidiChannel getChannel() {
		return channel;
	}

	/**
	 * @return Returns the patch.
	 */
	public Patch getPatch() {
		return patch;
	}

	/**
	 * The initial pitch offset includes the instrument definition's fine tune
	 * and note-to-pitch calculation. It does not include runtime pitch offset
	 * like pitch wheel, LFO and/or envelope pitch modifications.
	 * 
	 * @return Returns the initial pitch offset in semitones.
	 */
	public double getInitialPitchOffset() {
		return initialPitchOffset;
	}

	/**
	 * @param pitchOffset The pitchOffset to set, in semitones.
	 */
	public void setInitialPitchOffset(double pitchOffset) {
		this.initialPitchOffset = pitchOffset;
	}

	/**
	 * @return Returns the effective volumeFactor -- includes initial and
	 *         runtime volume.
	 */
	public double getEffectiveVolumeFactor(int channel) {
		if (channel < effectiveLinearVolume.length) {
			return effectiveLinearVolume[channel];
		}
		return 0.0;
	}

	/**
	 * Calculate the relative note offset to the base note, including pitch wheel, pitch eg,
	 * pitch modulation, vibrato, etc. 
	 * The parameter <code>relativeNote</code> is the number of semitones
	 * above (is positive) or below (if negative) the root key of the playing
	 * note. Usually, the relative note will just be added to the returned
	 * value. For different tuning schemes, it can be altered. 
	 * I.e. if no articulation module alters the pitch, then this method
	 * just returns <code>relativeNote</code> for a tempered tuning.
	 * 
	 * @param relativeNote the number of semitones to be played above the root key
	 * @return the offset, in semitones, to add to the note's root key
	 */
	public double getEffectivePitchOffset(double relativeNote) {
		return channel.getPitchWheelSemitones() + initialPitchOffset
				+ getRuntimePitchOffset() + relativeNote;
	}

	/**
	 * Return the pitch offset of the specific implementation, like EG and LFO,
	 * but excluding the static fine tune (pitchOffset) and excluding the pitch
	 * wheel.
	 * 
	 * @return the cumulated pitch deviation for this instance
	 */
	protected abstract double getRuntimePitchOffset();

	/**
	 * This method determines if this note has finished playing (e.g. when a
	 * release segment of a volume envelope reaches -96dB).
	 * 
	 * @return true if this note is finished with playback
	 */
	public abstract boolean endReached();

	/**
	 * Should be overriden in descendant classes to set meaningful values in
	 * effectiveLinearVolume
	 */
	protected abstract void calcEffectiveVolumeFactor();

	/**
	 * Is called in response to a change of a MIDI controller. Should be
	 * overriden in descendant classes to update internal variables.
	 * 
	 * @param controller the controller number that changed
	 * @param value the new value of the controller
	 */
	public abstract void controlChange(int controller, int value);

	/**
	 * Is called in response to a change of a MIDI pitch bend change. Should be
	 * overriden in descendant classes to update internal variables.
	 */
	public abstract void pitchWheelChange();
}
