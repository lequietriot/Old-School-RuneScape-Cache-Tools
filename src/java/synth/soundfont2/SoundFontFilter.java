package synth.soundfont2;

import synth.engine.AudioBuffer;
import synth.engine.MidiChannel;
import synth.utils.AudioUtils;

import static synth.soundfont2.SoundFontUtils.cents2hertzCutoff;

/**
 * A class implementing a low pass filter for SoundFont instruments.
 * <p>
 * The lowpass filter implementation is from this public domain source:
 * http://www.musicdsp.org/showone.php?id=27 <br>
 * References : Posted by Olli Niemitalo
 *
 * @author florian
 *
 */

public class SoundFontFilter {
	public static boolean DEBUG_LP = false;
	public static boolean DEBUG_LP_IO = false;

	/**
	 * The normalized cutoff value over which the filter is disabled to save
	 * processor usage. For larger sample rate conversion ratios, the filter is
	 * not switched off to act as an aliasing filter. It's maxed at this value.
	 */
	private final static double FLAT_CUTOFF = 0.97;

	/**
	 * A scaling factor for the RESONANCE MIDI controller values to relative
	 * resonance gain in dB. A value of 40/64 means that turning the resonance
	 * to the max(min), the resonance level will be increased(decreased) by
	 * 40dB.
	 */
	private final static double RESONANCE_MIDI_SENSITIVITY = 40.0 / 64.0;

	/**
	 * A scaling factor for the CUTOFF MIDI controller values to a relative
	 * cutoff frequency offset in semitones. A value of 12/64 means that turning
	 * the cutoff controller to the max(min), the cutoff level will be
	 * increased(decreased) by 12 semitones = 1 octave.
	 */
	private final static double CUTOFF_MIDI_SENSITIVITY = 64.0 / 64.0;

	/**
	 * Factor for the resonance in dB before converting to linear factor to
	 * convert to the useful resonance range.
	 */
	//private final static double DB_TO_RESONANCE_SCALING = 0.2; // 0.5;

	/**
	 * The owning articulation instance. It is used to access the MidiChannel
	 * object, etc.
	 */
	private SoundFontArticulation owner;

	/**
	 * If true, the filter is currently enabled. This will be determined by
	 * examining the effective cutoff and comparing to the FLAT_CUTOFF constant.
	 */
	private boolean enabled = false;

	/**
	 * The initial cutoff in absolute cents
	 */
	private int initialCutoffCents = 13500;

	/**
	 * The initial normalized cutoff [0..1] (derived from initialCutoffCents
	 * using the sample rate).
	 */
	private double initialNormalizedCutoff = 1.0;

	/**
	 * Offset to the cutoff, in semitones, originating from the cutoff MIDI
	 * controller.
	 */
	private double cutoffController = 0.0;

	/**
	 * The initial resonance in centibel [0....960cB]
	 */
	private int initialResonanceCB;

	/**
	 * The initial normalized resonance [0..1]
	 */
	private double initialNormalizedResonance;

	/**
	 * Offset to the resonance, in dB, originating from the resonance MIDI
	 * controller.
	 */
	private double resonanceController = 0.0;

	/**
	 * the sample rate that was used for calculation of cutoff
	 */
	private double currentSampleRate;

	/**
	 * the cutoff offset from last call to process()
	 */
	private double currentCutOffOffset;

	public SoundFontFilter(SoundFontArticulation owner) {
		this.owner = owner;
	}

	/**
	 * Must be called after initializing the initial cutoff and resonance.
	 */
	public void setup(int note, int vel) {
		updateResonanceController(false);
		updateCutoffController(false);
		calcResonance();
		// force recalculation of the filter
		currentCutOffOffset = -1;
		calculate(0);
		// force a call to setSampleRate in the next call to process()
		currentSampleRate = 0.0;
	}

	/**
	 * Called to calculate internal variables.
	 * 
	 * @param cutOffOffset - an increase/decrease of the initial cutoff in
	 *            semitones
	 */
	public void calculate(double cutOffOffset) {
		// check if the cutoff offset is different than last time
		if (cutOffOffset != currentCutOffOffset) {
			currentCutOffOffset = cutOffOffset;
			calcFilter();
		}
	}

	/**
	 * Apply the low pass filter to the specified buffer. If the effective cut
	 * off frequency is near the nyquist frequency, no processing is done.
	 * 
	 * @param buffer - the audio buffer to filter
	 */
	public void process(AudioBuffer buffer) {
		// check if the samplerate has changed
		if (buffer.getSampleRate() != currentSampleRate) {
			setSampleRate(buffer.getSampleRate());
			calcFilter();
		}
		// only filter if cutoff is "audible"
		if (enabled) {
			// this filter only operates on one channel, so only use the first
			// one
			process(buffer.getChannel(0), buffer.getSampleCount());
		}
	}

	private MidiChannel getChannel() {
		return owner.getChannel();
	}

	/**
	 * @param cents The initial cutoff to set, in cents
	 */
	void setCutoffCents(int cents) {
		this.initialCutoffCents = cents;
		if (DEBUG_LP) {
			System.out.println(" Low Pass: set cutoff to "
					+ (cents2hertzCutoff(initialCutoffCents)) + "Hz");
		}
	}

	/**
	 * @param cents The value to add to initial cutoff, in cents
	 */
	void addCutoffCents(int cents) {
		this.initialCutoffCents += cents;
		if (DEBUG_LP) {
			System.out.println(" Low Pass: add " + cents + " cents to cutoff, new value: "
					+ (cents2hertzCutoff(initialCutoffCents)) + "Hz");
		}
	}

	/**
	 * @param cB The initial resonance to set, in centibel
	 */
	void setResonanceCB(int cB) {
		this.initialResonanceCB = cB;
		if (DEBUG_LP) {
			System.out.println(" Low Pass: set resonance to "
					+ (initialResonanceCB / 10.0) + "dB");
		}
	}

	/**
	 * @param cB The value to add to initial resonance, in centibel
	 */
	void addResonanceCB(int cB) {
		this.initialResonanceCB += cB;
		if (DEBUG_LP) {
			System.out.println(" Low Pass: add " + (cB / 10.0)
					+ "dB to resonance, new value: "
					+ (initialResonanceCB / 10.0) + "dB");
		}
	}

	public void setSampleRate(double sampleRate) {
		currentSampleRate = sampleRate;
		double initialCutOffHertz =
				cents2hertzCutoff(initialCutoffCents);
		// calculate the cut off as a factor
		initialNormalizedCutoff = 2.0 * initialCutOffHertz / sampleRate;
		if (DEBUG_LP) {
			System.out.println(" Low Pass: set sample rate to " + (sampleRate)
					+ "Hz, initial cutoff=" + (initialCutOffHertz)
					+ "Hz " + "normalized cutoff factor: "
					+ (initialNormalizedCutoff));
		}
		lastCutoffOffset = -100000.0; // impossible value to force init
	}

	private void updateResonanceController(boolean doCalcFilter) {
		int ctrlValue = getChannel().getController(MidiChannel.RESONANCE) - 64;
		resonanceController = ctrlValue * RESONANCE_MIDI_SENSITIVITY;
		if (doCalcFilter) {
			calcFilter();
		}
	}

	private void updateCutoffController(boolean doCalcFilter) {
		int ctrlValue = getChannel().getController(MidiChannel.CUTOFF) - 64;
		cutoffController = ctrlValue * CUTOFF_MIDI_SENSITIVITY;
		if (doCalcFilter) {
			calcFilter();
		}
	}

	/**
	 * Is called in response to a change of a MIDI controller. Controller that
	 * modify the filter like resonance and cutoff will cause respective changes
	 * of the filter.
	 * 
	 * @param controller the controller number that changed
	 * @param value the new value of the controller [0..127]
	 */
	public void controlChange(int controller, int value) {
		switch (controller) {
		case MidiChannel.RESONANCE:
			updateResonanceController(true);
			break;
		case MidiChannel.CUTOFF:
			updateCutoffController(true);
			break;
		}
	}

	/**
	 * Calculate the normalized resonance from the resonance in cB.
	 */
	private void calcResonance() {
		// resInDB is approx. 0...64dB. The linear resonance should
		// be in between 0...1. So we can just use a direct translation
		// from decibel to linear with a factor 1.5. The filter becomes unstable
		// if the linear resonance is lower than 1, so limit it.
		double resInDB = initialResonanceCB / 10.0;
		if (resInDB < 0) {
			resInDB = 0.0;
		}
		this.initialNormalizedResonance =
				AudioUtils.decibel2linear(/*DB_TO_RESONANCE_SCALING * */ -resInDB);
		if (DEBUG_LP) {
			System.out.println(" Low Pass: resInDB="+(resInDB)+" -> calculated normalized initial resonance: "
					+ (initialNormalizedResonance));
		}
		lastResonanceOffset = -100000.0; // impossible value to force init
	}

	// the following code is derived from a 3rd party implementation, see class
	// description for details.

	/**
	 * state variables of the filter
	 */
	/** frequency coefficient */
	private double F1;
	/** Q coefficient */
	private double Q1;
	
	/** history value: last low pass sample */
	private double lastLP;
	/** history value: last band pass sample */
	private double lastBP;
	
	// caches for optimization
	private double lastCutoffOffset = -100000.0; // impossible value to force
													// init
	private double lastCutoff = 1.0;
	private double lastResonanceOffset = -100000.0; // impossible value to force
													// init
	private double lastResonance = 0.0;

	/**
	 * Calculate the state variables of the filter
	 */
	private void calcFilter() {
		// first calculate the effective cutoff: convert the cutoff offset (in
		// semitones) to a factor.
		double cutoff;
		double cutoffOffset = currentCutOffOffset + cutoffController;
		if (cutoffOffset != lastCutoffOffset) {
			double cutoffOffsetFactor =
					AudioUtils.getSamplerateFactorFromRelativeNote(cutoffOffset);
			// the effective cutoff is the product
			cutoff = initialNormalizedCutoff * cutoffOffsetFactor;
			
			// the algorithm has a slow start, so double the linear cutoff
			cutoff *= 2;
			
			// clip the cutoff to act as an anti-aliasing filter
			if (cutoff > FLAT_CUTOFF) {
				cutoff = FLAT_CUTOFF;
			}
			lastCutoff = cutoff;
			if (DEBUG_LP_IO) {
				System.out.println(" Low Pass: offset=" + (currentCutOffOffset)
						+ " semitones + controller cutoff offset="
						+ (cutoffController)
						+ " semitones -> cutoffFactor="
						+ (cutoffOffsetFactor)
						+ ", calculated normalized effective cutoff: "
						+ (cutoff) + " -> "
						+ (currentSampleRate / 2.0 * cutoff) + "Hz");
			}
			// enabled = (cutoff < FLAT_CUTOFF);
			enabled = true;
		} else {
			cutoff = lastCutoff;
		}

		double resonance;
		if (lastResonanceOffset != resonanceController) {
			double resonanceOffsetFactor =
					AudioUtils.decibel2linear(/*DB_TO_RESONANCE_SCALING * */
							-resonanceController);
			resonance = initialNormalizedResonance * resonanceOffsetFactor;
			//if (resonance < 1.0) {
			//	resonance = 1.0;
			//}
			lastResonance = resonance;
		} else {
			resonance = lastResonance;
		}

		if (!enabled) {
			if (DEBUG_LP_IO) {
				System.out.println(" Low Pass: cutoff is too high, disable lowpass filter. ");
			}
			// reset state vars to not cause a glitch when the filter is
			// initialized next time
			F1 = 0.0; Q1 = 0.0; lastBP = 0.0; lastLP = 0.0;
		} else {
			F1 = 2 * Math.sin(Math.PI * cutoff * 0.25);

			// scale to 0...1
			double res = resonance;
			if (res > 1.0) {
				res = 1.0;
			} else if (res < 0) {
				res = 0;
			}
			Q1 = res * 0.66 + 0.04;
			
			if (DEBUG_LP_IO) {
				System.out.println(" Recalculated: resonance="+resonance+"  res = "+res+"  Q1="+Q1);
			}
		}
	}

	private void process(double[] samples, int count) {
		// use local variables for more efficient access
		double localLastLP = lastLP;
		double localLastBP = lastBP;
		double localF1 = F1;
		double localQ1 = Q1;
		
		for (int i = 0; i < count; i++) {
			localLastBP += localF1 * (samples[i] - localLastLP - localQ1 * localLastBP);
			localLastLP += localF1 * localLastBP;
			samples[i] = (float) localLastLP;
		}
		
		lastLP = localLastLP;
		lastBP = localLastBP;
	}
}
