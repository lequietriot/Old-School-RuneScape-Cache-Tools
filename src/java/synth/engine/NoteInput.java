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

import synth.engine.Synthesizer.Params;

import static synth.utils.AudioUtils.getSamplerateFactorFromRelativeNote;
import static synth.utils.AudioUtils.nanos2samples;


/**
 * A class for feeding the mixer with a single played note.
 * <p>
 * One NoteInput instance provides mono data by way of an attached mono
 * Oscillator. For stereo or multi-channel, instanciate several NoteInput
 * instances with corresponding panorama settings.
 * <p>
 * NoteInput instances can be linked together to form a unit, notably for stereo
 * and multi-channel instruments. For this, the LinkedNoteInput field points to
 * the next linked instance, which can again point to the next linked instance,
 * and so on. The last instance points back to the first instance. E.g. for
 * stereo, the left NoteInput instance point to the right instance, and vice
 * versa. If an action need to be done on all instances, this action needs to be
 * manually executed on all instances. The methods in NoteInput only act on
 * itself.
 * <p>
 * This class provides the following MIDI services:
 * <ul>
 * <li>keep track of the played note (i.e. which note is heard)
 * <li>keep track of the triggered ntoe (i.e. which key has triggered this note --
 * important for matching Note Off)
 * <li>note on and note off
 * <li>hold controller
 * <li>sostenuto controller
 * </ul>
 * 
 * @author florian
 */

public class NoteInput implements AudioInput, Renderable {

	public static boolean DEBUG_NOTEINPUT = false;
	public static boolean DEBUG_NOTEINPUT_IO = false;

	/**
	 * How many output channels will be ever present in this system
	 */
	private final static int MAX_OUTPUT_CHANNELS = 16;

	/**
	 * Pitch changes are only recognized in this interval, in nanoseconds
	 */
	private final static int PITCH_CHANGE_INTERVAL = 10000000;

	/**
	 * The note that this NoteInput object plays.
	 */
	private int note;

	/**
	 * The note that triggered this NoteInput, e.g. this is the key number
	 * played on a MIDI instrument.
	 */
	private int triggerNote;

	/**
	 * The channel parameters for this note. (read-only)
	 */
	private MidiChannel channel;

	/**
	 * runtime articulation data
	 */
	private Articulation art;

	/**
	 * The global synthesizer params
	 */
	private Params synthParams;

	/**
	 * Read-only patch information. The Patch object provides access to the
	 * complete instrument in a byte array
	 */
	private Patch patch;

	/**
	 * The temporary rendering buffer
	 */
	private AudioBuffer tempBuffer = new AudioBuffer(1, 0, 44100.0);

	/**
	 * The oscillator for rendering
	 */
	private Oscillator osc;

	/**
	 * the offset to copy tempBuffer to
	 */
	private int outSampleOffset = 0;

	/**
	 * the time of the rendered tempBuffer
	 */
	private volatile long lastRenderTime = -1;

	/**
	 * flag that is set in the rendering method to notify when this line is done
	 * with playback
	 */
	private boolean eofReached = false;

	/**
	 * flag to signal the renderer to fade out the note in the next rendering
	 * block. After that block is rendered, eofReached will be set to true.
	 */
	private boolean doFadeOut = false;

	/**
	 * when to start playing this note, in nanoseconds
	 */
	private long insertionTime;

	/**
	 * Initial rendering volume of the buffer. Each value is for an output
	 * channel. For stereo, the first element is for the left channel, and the
	 * second element is for the right channel.
	 */
	private double[] initialVolFactor = new double[MAX_OUTPUT_CHANNELS];

	/**
	 * Final rendering volume of the current buffer. Each value is for an output
	 * channel. For stereo, the first element is for the left channel, and the
	 * second element is for the right channel.
	 */
	private double[] finalVolFactor = new double[MAX_OUTPUT_CHANNELS];

	/**
	 * The linked instance (see class description for more details).
	 */
	private NoteInput linkedNoteInput;

	/**
	 * If set, then the note is not released because of the sustain or sostenuto
	 * pedal.
	 */
	private boolean inhibitedRelease;

	/**
	 * If set, then the note is in sostenuto state.
	 */
	private boolean sostenuto;

	/**
	 * For optimization, cache the last relative pitch value
	 */
	private double lastRelativePitch = -100000.0; // impossible value to
	// initialize
	/**
	 * For optimization, cache the last sample rate factor corresponding to the
	 * last relative pitch value
	 */
	private double lastSampleRateFactor = 0.0;

	/**
	 * For optimization, only compute a new pitch every PITCH_CHANGE_INTERVAL
	 * nanoseconds. This is the next audio time at which a pitch change needs to
	 * be computed.
	 */
	private long nextPitchChange = 0;

	/**
	 * The number of samples rendered in the render method.
	 */
	private int renderSampleCount = 0;

	/**
	 * Create a NoteInput stream.
	 *
	 * @param params
	 * @param channel
	 * @param patch
	 * @param osc
	 * @param art
	 * @param note -- the effective note playing (not necessarily the note that
	 *            triggered this instrument)
	 */
	public NoteInput(Params params, AudioTime time,
                     MidiChannel channel, Patch patch, Oscillator osc, Articulation art,
                     int note, int vel) {
		this.synthParams = params;
		this.channel = channel;
		this.patch = patch;
		this.osc = osc;
		this.art = art;
		this.note = note;
		this.insertionTime = time.getNanoTime();
		art.setup(note, vel);
		// setup initial volume. Use the "finalVolFactor" values
		// because that will become the initialVolFactor after rendering
		// the first buffer
		double masterVolume = synthParams.getMasterVolumeInternal();
		finalVolFactor[0] = masterVolume * art.getEffectiveVolumeFactor(0);
		finalVolFactor[1] = masterVolume * art.getEffectiveVolumeFactor(1);
	}

	/**
	 * @return Returns the patch.
	 */
	public Patch getPatch() {
		return patch;
	}

	/**
	 * @return Returns the articulation data.
	 */
	public Articulation getArticulation() {
		return art;
	}

	/**
	 * @return Returns the MIDI channel object.
	 */
	public MidiChannel getMidiChannel() {
		return channel;
	}

	/**
	 * @return Returns the note that this NoteInput plays.
	 */
	public int getNote() {
		return note;
	}

	/**
	 * @return Returns the note that this NoteInput was triggered with.
	 */
	public int getTriggerNote() {
		return triggerNote;
	}

	/**
	 * @return Returns the oscillator.
	 */
	public Oscillator getOscillator() {
		return osc;
	}

	/**
	 * @return Returns the synthParams.
	 */
	public Params getSynthParams() {
		return synthParams;
	}

	/**
	 * package private method to set the trigger note. This will be used by the
	 * synthesizer to be able to match a Note-Off event to this NoteInput
	 * object.
	 * 
	 * @param triggerNote
	 */
	void setTriggerNote(int triggerNote) {
		this.triggerNote = triggerNote;
	}

	/**
	 * @return Returns the linkedNoteInput.
	 */
	public NoteInput getLinkedNoteInput() {
		return linkedNoteInput;
	}

	/**
	 * @param linkedNoteInput The linkedNoteInput to set.
	 */
	public void setLinkedNoteInput(NoteInput linkedNoteInput) {
		this.linkedNoteInput = linkedNoteInput;
	}

	/**
	 * Read a rendered buffer (type 1).
	 */
	public synchronized final void read(AudioTime time, AudioBuffer buffer,
			int offset, int count) {

		if (done()) return;

		// store current sample rate and sample count for the render() method
		tempBuffer.setSampleRate(buffer.getSampleRate());
		this.renderSampleCount = count;

		// on-demand rendering
		if (lastRenderTime + 125000 < time.getNanoTime()) {
			renderImpl(time, count);
		}

		// use the temp buffer's sample count, in case it rendered fewer samples
		int thisCount = tempBuffer.getSampleCount();
		offset += outSampleOffset;

		if (thisCount == 0) return;
		if (thisCount + outSampleOffset > count) {
			thisCount = count - outSampleOffset;
		}

		assert (tempBuffer.getChannelCount() == 1);
		assert (thisCount + offset <= buffer.getSampleCount());

		double[] tempSamples = tempBuffer.getChannel(0);
		// add the rendered buffer to this buffer

		// for the stereo case, an optimized version:
		if (buffer.getChannelCount() == 2) {
			double[] samples1 = buffer.getChannel(0);
			double[] samples2 = buffer.getChannel(1);
			double volFactor1 = initialVolFactor[0];
			double volFactorInc1 = (finalVolFactor[0] - volFactor1) / thisCount;
			double volFactor2 = initialVolFactor[1];
			double volFactorInc2 = (finalVolFactor[1] - volFactor2) / thisCount;
			int index = offset;
			// $$fb added this extra check -- happened apparently in a 
			// race condition when lowering the buffer size 
			if (thisCount > samples1.length - offset) {
				thisCount = samples1.length - offset;
			}
			for (int i = 0; i < thisCount; i++) {
				double sample = tempSamples[i];
				samples1[index] += (volFactor1 * sample);
				samples2[index++] += (volFactor2 * sample);
				volFactor1 += volFactorInc1;
				volFactor2 += volFactorInc2;
			}
		} else {
			// ... or the generic version
			for (int c = 0; c < buffer.getChannelCount(); c++) {
				double[] samples = buffer.getChannel(c);
				double volFactor = initialVolFactor[c];
				double volFactorInc =
						(finalVolFactor[c] - volFactor) / thisCount;
				for (int i = 0; i < thisCount; i++) {
					samples[i + offset] += (volFactor * tempSamples[i]);
					volFactor += volFactorInc;
				}
			}
		}
		// stop note generation if this last slice was a fade out
		if (doFadeOut) {
			eofReached = true;
		}
	}

	/**
	 * Read a rendered buffer (type 2).
	 */
	public AudioBuffer read(AudioTime time, int sampleCount, int channelCount, double sampleRate) {
		AudioBuffer returnBuffer = new AudioBuffer(channelCount, sampleCount, sampleRate);
		read(time, returnBuffer, 0, sampleCount);
		return returnBuffer;
	}

	/**
	 * This method must not be synchronized!
	 * 
	 * @param currTime the time to be tested
	 * @return returns true if the block starting at time currTime is already
	 *         rendered
	 */
	public final boolean alreadyRendered(AudioTime currTime) {
		// add 125 microseconds (1 sample at 8000Hz) to account for rounding
		// errors
		return lastRenderTime + 125000 >= currTime.getNanoTime();
	}

	/**
	 * Render a block of audio data for the given time. Internally, this will
	 * fill a temporary buffer. This method is called from the read() method, or
	 * from a separate rendering thread for asynchronous rendering.
	 * <p>
	 * This method will not (and must not) set the eofReached flag. Instead, it
	 * can set the doFadeOut flag to cause the read() method to set eofReached.
	 * 
	 * @param time the start time of the next buffer to be filled
	 */
	public synchronized final boolean render(AudioTime time) {
		// do not render if already done or if no render sample count is set
		if (done() || renderSampleCount <= 0) {
			tempBuffer.changeSampleCount(0, false);
			return false;
		}

		// account for race conditions (when a render thread blocks because
		// the read() method is calling this render instance)
		if (lastRenderTime + 125000 >= time.getNanoTime()) {
			// buffer is already rendered
			return false;
		}
		return renderImpl(time, renderSampleCount);
	}

	/**
	 * Private implementation of render() which does not check if this buffer is
	 * already rendered.
	 * 
	 * @param time
	 * @return
	 */
	private final boolean renderImpl(AudioTime time, int count) {
		long nanoTime = time.getNanoTime();
		// set the lastRenderTime as fast as possible:
		// method read() will block anyway until termination of this method,
		// and other threads accessing alreadyRendered() should have the most
		// accurate value to prevent as much as possible redundant calls to
		// render().
		lastRenderTime = nanoTime;

		// adjust insertion point
		outSampleOffset = 0;
		if (insertionTime > 0) {
			// calculate the sample position
			long nanoOffset = (insertionTime - nanoTime);
			outSampleOffset =
					(int) nanos2samples(nanoOffset, tempBuffer.getSampleRate());
			if (outSampleOffset < count) {
				// OK, we can (and should) start inserting the instrument in
				// *this* buffer
				if (outSampleOffset < 0) {
					if (DEBUG_NOTEINPUT) {
						System.out.println("NoteInput: time=" + time.getMicroTime()
						+ "us, insertion time=" + (insertionTime)
						+ "us, offset=" + (nanoOffset / 1000L)
						+ "us, outSampleOffset=" + outSampleOffset
						+ "samples, count=" + count
						+ "samples");
						System.out.println("   -> rendering start of note comes "
								+ (outSampleOffset * -1) + " samples too late!");
					}
					// rendering comes too late! insert at beginning of buffer
					outSampleOffset = 0;
				} else {
					count -= outSampleOffset;
					if (DEBUG_NOTEINPUT_IO) {
						System.out.println("NoteInput: Insert Note at time "+time+" with offset "+outSampleOffset+" samples.");
					}
				}
				insertionTime = 0;
				// force calculation of pitch
				lastRelativePitch = -100000.0;
				nextPitchChange = 0;
			} else {
				// if (DEBUG_NOTEINPUT) {
				// debug("MidiInput: time=" + time.getMicroTime()
				// + "us, insertion time=" + (insertionTime)
				// + "us, offset=" + (nanoOffset / 1000L)
				// + "us, outSampleOffset=" + outSampleOffset
				// + "samples, count=" + count
				// + "samples -> wait until next buffer.");
				// }
				// need to wait this buffer, insertion is scheduled for later
				count = 0;
			}
		}

		// initialize the temporary render buffer
		tempBuffer.changeSampleCount(count, false);
		if (count > 0) {
			// calculate articulation data
			// calculate volume level
			// calculate sample rate factor
			art.calculate(time);

			// sampleRateFactor does not include master tuning
			double sampleRateFactor;

			if (nanoTime >= nextPitchChange) {
				// retrieve instantaneous pitch for this note and calculate the
				// resulting sample rate factor
				double relativePitch =
						art.getEffectivePitchOffset(note - patch.getRootKey());
				if (relativePitch != lastRelativePitch) {
					lastRelativePitch = relativePitch;
					sampleRateFactor =
							getSamplerateFactorFromRelativeNote(relativePitch);
					lastSampleRateFactor = sampleRateFactor;
				} else {
					sampleRateFactor = lastSampleRateFactor;
				}
				nextPitchChange = nanoTime + PITCH_CHANGE_INTERVAL;
			} else {
				sampleRateFactor = lastSampleRateFactor;
			}

			// convert from native applying these things at once:
			// - convert from native format
			// - convert sample-rate (if necessary)
			// - apply any further processing like filters
			int newCount =
					osc.convert(tempBuffer, 0, count, sampleRateFactor
							* synthParams.getMasterTuningFactor());
			if (newCount < count) {
				tempBuffer.changeSampleCount(count, true);
			}

			// let the articulation block further process this buffer (filters,
			// ...).
			art.process(tempBuffer);

			// take care of volume changes.
			// for now: just stereo
			double masterVolume = synthParams.getMasterVolumeInternal();
			initialVolFactor[0] = finalVolFactor[0];
			finalVolFactor[0] = masterVolume * art.getEffectiveVolumeFactor(0);
			initialVolFactor[1] = finalVolFactor[1];
			finalVolFactor[1] = masterVolume * art.getEffectiveVolumeFactor(1);
			if (doFadeOut) {
				// just set final volume to 0
				finalVolFactor[0] = 0.0;
				finalVolFactor[1] = 0.0;
			}
		}
		return true;
	}

	/**
	 * Enters the release segment of this note. Time is the current time, equal
	 * to the time when the release segment is being entered.
	 */
	public void release(AudioTime time) {
		if (channel.sustainDown() || sostenuto) {
			inhibitedRelease = true;
		} else {
			art.release(time);
			osc.release(time);
		}
	}

	/**
	 * @return true if the note should have already released, but sustain or
	 *         sostenuto prevented it.
	 */
	public boolean isReleaseInhibited() {
		return inhibitedRelease;
	}

	/**
	 * @return true if the note is in sostenuto state
	 */
	public boolean isSostenuto() {
		return sostenuto;
	}

	/**
	 * Activate or end sostenuto state. If active is false, this note will be
	 * released if it has already received the NOTE OFF event (unless the
	 * sustain pedal is down).
	 */
	public void setSostenuto(AudioTime time, boolean active) {
		sostenuto = active;
		if (!active) {
			release(time);
		}
	}

	/**
	 * Stops this note as soon as possible without a click. This is done by
	 * fading it out duering the next slice.
	 */
	public void stopAsap() {
		doFadeOut = true;
	}

	/**
	 * Returns true if this input stream has finished rendering data and further
	 * calls to read would just return silence.
	 */
	public boolean done() {
		if (!eofReached) {
			eofReached = osc.endReached() || art.endReached();
		}
		return eofReached;
	}

	public String toString() {
		return "NoteInput: note " + getNote();
	}
}
