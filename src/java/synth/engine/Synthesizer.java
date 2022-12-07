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

import synth.utils.AsynchExec;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/*
 * TODO: Note On events (and controller events?) should be dispatched *before*
 * their correct insertion time has passed. Note Off events should be dispatched
 * after their correct insertion time. How about other events? If it's only note
 * offs, maybe generally dispatch events before their actual insertion time and
 * do special handling for Note Off, just as it's done for Note On? -- for now,
 * also send note Off too early (i.e. at the beginning of the slice where it'll
 * be occurring). Since note off will only cause release(), its timing will be
 * adjusted in the articulation modules anyway.
 */

/*
 * TODO: "intermediate sustain": releasing the sustain pedal a little bit, will
 * soften the sustained notes.
 */

/**
 * The main class for rendering MIDI notes. Dispatches incoming notes to the
 * connected Mixer using the connected soundbank.
 * <p>
 * <b>Multi-threaded rendering:</b><br>
 * This synthesizer implementation allows multi-threaded rendering by using the
 * class AsynchronousRenderer. Recommended is to use the number of threads
 * corresponding to the number of processor cores/hyperthreaded cores.
 * <p>
 * <b>Asynchronous execution works like this:</b><br>
 * The audio pull thread signals the start of a new audio slice by calling its
 * list of AudioRendererListeners. First, the synth dispatches all MIDI events
 * for that slice (in the AudioRendererListener thread). Once that is done, it
 * signals the AsynchronousRenderer thread the beginning of a new slice. From
 * now on, all threads of the AsynchronousRenderer simultaneously try to render
 * the Renderable lines of the mixer, in the same order. Meanwhile, the
 * AudioPullThread will finish serving AudioRendererListeners and then call the
 * mixer's read() method. The mixer is unaware of any AsynchronousRenderer and
 * will read all AudioInput lines in order. So, all threads, plus the thread in
 * mixer.read() compete for rendering the line's slices.
 * <p>
 * When the mixer calls a line's read() method (in the thread of
 * AudioPullThread), the following alternatives may happen:
 * <ol>
 * <li>This line is not yet rendered by an asynch thread. This will cause the
 * read() method to call the render() method, so that effectively this line is
 * rendered in the AudioPullThread thread. For an optimal resource usage, this
 * is OK.</li>
 * <li>This line is already fully rendered by an asynch thread. The read()
 * method will notice that and just copy the pre-rendered buffer to the mix
 * buffer.</li>
 * <li>This line is currently being rendered by an asynchronous
 * AsynchronousRenderer thread. This causes the entry of AudioInput.read() to be
 * blocked, because read() and render() are synchronized to the line's instance.
 * If there are enough AsynchronousRenderer threads this blocking is no waste,
 * since the asychronous threads will sufficiently exploit all processor cores.</li>
 * </ul>
 * The scenario 1 above is particularly efficient since all threads start
 * working on the lines in the same order. That means that statistically render
 * threads may render higher numbered lines, while the read() method already
 * mixes the beginning lines.
 * <p>
 * <b>Bottlenecks that could be further optimized:</b><br>
 * <ul>
 * <li>Render threads must not start a slice before all events for the slice
 * are dispatched. So the dispatching action currently forces the render threads
 * to wait.</li>
 * <li>Once AudioPullThread writes to the AudioSink, it needs to wait until the
 * sink returns from writing to the device. During that time, the render threads
 * are idle, although they could already start rendering the next slice. This is
 * a question of priority: to maintain lowest latency, any rendered buffer needs
 * to be passed on to the audio sink as fast as possible. Now if the next slice
 * is started before writing the last buffer to the sink, the event dispatching
 * may delay the writing unnecessarily and in an uncontrolled way. If the next
 * slice is started after writing, we miss the opportunity to use the blocking
 * time of the sink for rendering.</li>
 * 
 * @author florian
 */

public class Synthesizer implements MidiIn.Listener, AudioRendererListener,
		AsynchExec.Listener<MidiEvent> {

	public static boolean DEBUG_SYNTH = false;
	public static boolean DEBUG_SYNTH_IO = false;
	public static boolean DEBUG_SYNTH_TIMING = false;

	public static int NOTE_DISPATCHER_PRIORITY = 27;

	/**
	 * Only if the latency (i.e. the soundcard's buffer size * 2) is equal or
	 * below this threshold in nanoseconds, will the asynchronous renderer be
	 * used in auto mode.
	 */
	public static final long NOTE_DISPATCHER_LATENCY_THRESHOLD_NANOS = 2 * 2700000L;

	/** for debugging: if > 0 show audio time, decreasing by one */
	private int debugShowAudioTime = 0;

	/**
	 * Only if the mixer has more than this number of streams, asynchronous
	 * rendering threads are used for rendering. Otherwise, the overhead and
	 * synchronization is too expensive to justify usage of the other threads.
	 */
	public static int ASYNCH_RENDER_STREAM_THRESHOLD = 10;

	/**
	 * Mode for setNoteDispatcherMode: do not use a separate thread
	 */
	public static final int NOTE_DISPATCHER_SYNCHRONOUS = 0;

	/**
	 * Mode for setNoteDispatcherMode: request a separate thread, only use it if
	 * fixedLatency is smaller than NOTE_DISPATCHER_LATENCY_THRESHOLD_NANOS.
	 */
	public static final int NOTE_DISPATCHER_REQUEST_ASYNCHRONOUS = 1;

	/**
	 * Mode for setNoteDispatcherMode: force usage of a separate thread
	 */
	public static final int NOTE_DISPATCHER_FORCE_ASYNCHRONOUS = 2;

	/**
	 * The list of non-NoteOn events for correct scheduling
	 */
	private EventQueue eventQueue;

	/**
	 * The array of MIDI channels
	 */
	private MidiChannel[] channels;

	/**
	 * The soundbank to retrieve the patches from
	 */
	private Soundbank soundbank;

	/**
	 * The mixer that receives the notes.
	 */
	private AudioMixer aMixer;

	/**
	 * An arbitrary delay that is imposed on all input events.
	 */
	private long fixedDelayNanos;

	private Params params;

	/**
	 * The end time of the last rendered audio slice i.e. the start time of the
	 * next slice to be rendered. So this time has already passed and all events
	 * to be scheduled before this time must be immediately processed
	 */
	private volatile AudioTime nextAudioSliceTime = new AudioTime(0);

	/**
	 * Keep track of the duration of the last audio slice in order to
	 * extrapolate the duration of the next audio slice. This is used in the
	 * asynchronous event dispatcher.
	 */
	private AudioTime nextAudioSliceDuration = new AudioTime(0);

	/**
	 * An optional pointer to the audio clock of the rendering device, with
	 * higher resolution than the nextAudioSliceTime.
	 */
	private AudioClock masterClock;

	/**
	 * The list of listeners
	 */
	private List<SynthesizerListener> listeners;

	/**
	 * The ListenerHandler dispatches the events to registered
	 * Synthesizer.Listener instances.
	 */
	private AsynchExec<MidiEvent> listenerHandler;

	/**
	 * Instance of an AsynchronousRenderer if multi-threaded rendering is used.
	 */
	private AsynchronousRenderer asynchRenderer = null;

	/**
	 * if yes, asynchronous dispatching is requested
	 */
	private int noteDispatcherMode = NOTE_DISPATCHER_SYNCHRONOUS;

	/**
	 * the asynchronous note dispatcher thread
	 */
	private NoteDispatcher noteDispatcher = null;

	/**
	 * Benchmark mode: used for measuring timing. Will cause any events to be
	 * mapped to channel 10, to have a sharp attack of the played notes.
	 */
	private boolean benchmarkMode = false;

	/**
	 * If this flag is not set, incoming MIDI events with 0 time will not be
	 * scheduled but inserted into the stream as soon as possible.
	 */
	private boolean schedulingOfRealtimeEvents = true;

	/**
	 * the MIDI note number mapped in benchmark mode. This should be a drum note
	 * with sharp attack.
	 */
	private static final int BENCHMARK_NOTE = 32;

	private int threadCount = 0;

	private boolean started = false;

	/**
	 * Constructor without parameters. This will create a default mixer to be
	 * used.
	 */
	public Synthesizer() {
		this(null, new AudioMixer());
	}

	/**
	 * Constructor without parameters. This will create a default mixer to be
	 * used.
	 * 
	 * @param sb the initial soundbank
	 */
	public Synthesizer(Soundbank sb) {
		this(sb, new AudioMixer());
	}

	/**
	 * Constructor with parameters.
	 * 
	 * @param sb the initial soundbank
	 * @param m the initial mixer
	 */
	public Synthesizer(Soundbank sb, AudioMixer m) {
		init();
		setSoundbank(sb);
		setMixer(m);
	}

	/** initialization tasks common to all constructors */
	private final void init() {
		channels = new MidiChannel[16];
		for (int i = 0; i < channels.length; i++) {
			channels[i] = new MidiChannel(i);
		}
		params = new Params();
		eventQueue = new EventQueue();
		listeners = new ArrayList<SynthesizerListener>(1);
		listenerHandler = new AsynchExec<MidiEvent>(this,
				"Synthesizer listener handler");
		// set default render thread count
		threadCount = AsynchronousRenderer.getDefaultThreadCount();
		if (threadCount < 2) {
			threadCount = 0;
		}
		asynchRenderer = new AsynchronousRenderer(0);
	}

	/**
	 * start the synthesizer
	 */
	public void start() {
		started = true;
		listenerHandler.start();
		// start asynchronous renderer
		setRenderThreadCount(threadCount);
		// start the note dispatcher if requested, and if latency is small
		// enough
		verifyNoteDispatcher();
	}

	private boolean isStarted() {
		return started;
	}

	/**
	 * close down this synthesizer
	 */
	public void close() {
		started = false;
		eventQueue.close();
		listenerHandler.stop();
		if (asynchRenderer != null) {
			asynchRenderer.stop();
		}
		stopNoteDispatcher();
	}

	/**
	 * @return Returns the mixer.
	 */
	// sync note: must be synchronized to flush old versions on a different
	// processor
	public synchronized AudioMixer getMixer() {
		return aMixer;
	}

	/**
	 * @param mixer The mixer that this Synthesizer will be rendering to.
	 * @exception IllegalArgumentException if mixer is null
	 */
	// sync note: must be synchronized to flush old versions on a different
	// processor
	public synchronized void setMixer(AudioMixer mixer) {
		if (mixer == null) {
			throw new IllegalArgumentException(
					"may not set the synth's mixer to null");
		}
		this.aMixer = mixer;
	}

	/**
	 * @return Returns the soundbank.
	 */
	// sync note: must be synchronized to flush old versions on a different
	// processor
	public synchronized Soundbank getSoundbank() {
		return soundbank;
	}

	/**
	 * @param soundbank The soundbank that this mixer will use for rendering.
	 */
	// sync note: must be synchronized to flush old versions on a different
	// processor
	public synchronized void setSoundbank(Soundbank soundbank) {
		this.soundbank = soundbank;
	}

	/**
	 * Set the number of simultaneous threads that are used for rendering the
	 * notes of the attached mixer. To turn off asynchronous rendering, set this
	 * to 0.
	 */
	public synchronized void setRenderThreadCount(int count) {
		threadCount = count;
		if (!isStarted()) {
			return;
		}
		if (count == 0) {
			if (asynchRenderer != null) {
				asynchRenderer.stop();
			}
		} else {
			if (asynchRenderer == null) {
				asynchRenderer = new AsynchronousRenderer(count);
			} else {
				asynchRenderer.setThreadCount(count);
			}
			asynchRenderer.start();
		}
		if (DEBUG_SYNTH) {
			System.out.println("Set asynchronous render threads to " + count
					+ " threads -> " + asynchRenderer.getActiveCount()
					+ " active threads");
		}
	}

	/**
	 * Get the number of currently running render threads
	 * 
	 * @return the number of threads used for rendering, or 0 if rendering is
	 *         done exclusively from PullThread.
	 */
	public synchronized int getRenderThreadCount() {
		return (asynchRenderer == null || !asynchRenderer.isStarted()) ? 0
				: asynchRenderer.getThreadCount();
	}

	/**
	 * Either enable the asynchronous note dispatcher or disable it. If it is
	 * disabled, note dispatching is done from the newAudioSlice callback.
	 * 
	 * @param mode one of the NOTE_DISPATCHER_* flags
	 */
	public void setNoteDispatcherMode(int mode) {
		if (mode < 0) {
			mode = 0;
		}
		if (mode > NOTE_DISPATCHER_FORCE_ASYNCHRONOUS) {
			mode = NOTE_DISPATCHER_FORCE_ASYNCHRONOUS;
		}
		this.noteDispatcherMode = mode;
		verifyNoteDispatcher();
	}

	public int getNoteDispatcherMode() {
		return noteDispatcherMode;
	}

	/**
	 * @return true if note dispatching is done asynchronously
	 */
	// sync note: do not synchronize
	public boolean isNoteDispatcherRunning() {
		return (noteDispatcher != null);
	}

	/**
	 * Stop the note dispatcher thread. This method should only be called from a
	 * non-synchronized context.
	 */
	private void stopNoteDispatcher() {
		NoteDispatcher stopDispatcher = null;
		synchronized (this) {
			stopDispatcher = noteDispatcher;
			noteDispatcher = null;
		}
		// stop the dispatcher outside the synchronized block
		if (stopDispatcher != null) {
			stopDispatcher.stop();
		}
	}

	/**
	 * If asynchronous dispatching is requested, and the latency is small
	 * enough, start the asynchronous note dispatcher thread. Otherwise, stop
	 * the dispatcher.
	 */
	private void verifyNoteDispatcher() {
		boolean stop = false;
		synchronized (this) {
			boolean canDispatch = (noteDispatcherMode == NOTE_DISPATCHER_FORCE_ASYNCHRONOUS)
					|| ((noteDispatcherMode == NOTE_DISPATCHER_REQUEST_ASYNCHRONOUS) && (fixedDelayNanos <= NOTE_DISPATCHER_LATENCY_THRESHOLD_NANOS));
			if (canDispatch != isNoteDispatcherRunning()) {
				if (canDispatch) {
					noteDispatcher = new NoteDispatcher();
				} else {
					// stop the dispatcher if actually running
					stop = (noteDispatcher != null);
				}
			}
		}
		// stop the dispatcher outside the synchronized block
		if (stop) {
			stopNoteDispatcher();
		}
	}

	/**
	 * Retrieve the MidiChannel object for the specified numbered channel.
	 * 
	 * @param channel 0..15 the MIDI channel number
	 * @return the associated MidiChannel instance
	 */
	public MidiChannel getChannel(int channel) {
		return channels[channel];
	}

	/**
	 * @return the set of all channels
	 */
	public List<MidiChannel> getChannels() {
		List<MidiChannel> res = new ArrayList<MidiChannel>(channels.length);
		for (int i = 0; i < channels.length; i++) {
			res.add(getChannel(i));
		}
		return res;
	}

	public long getFixedDelayNanos() {
		return fixedDelayNanos;
	}

	/**
	 * @param fixedDelayNanos The delay that is imposed on all input events.
	 */
	public void setFixedDelayNanos(long fixedDelayNanos) {
		this.fixedDelayNanos = fixedDelayNanos;
		// may need to start or to stop the asynchronous note dispatcher
		verifyNoteDispatcher();
	}

	/**
	 * @return Returns the synthesizer params.
	 */
	public Params getParams() {
		return params;
	}

	/**
	 * Register this listener to receive played MIDI events
	 */
	public void addListener(SynthesizerListener L) {
		listeners.add(L);
		if (!listenerHandler.isStarted()) {
			listenerHandler.start();
		}
	}

	/**
	 * Remove this instance from the list of notified listeners
	 */
	public void removeListener(SynthesizerListener L) {
		listeners.remove(L);
		if (listeners.size() == 0) {
			listenerHandler.stop();
		}
	}

	/**
	 * @return Returns the masterAudioClock.
	 */
	public AudioClock getMasterClock() {
		return masterClock;
	}

	/**
	 * Set a new master clock.
	 * 
	 * @param masterClock The master clock to set.
	 */
	public void setMasterClock(AudioClock masterClock) {
		this.masterClock = masterClock;
	}

	/**
	 * @return the benchmarkMode
	 */
	public boolean isBenchmarkMode() {
		return benchmarkMode;
	}

	/**
	 * @param benchmarkMode the benchmarkMode to set
	 */
	public void setBenchmarkMode(boolean benchmarkMode) {
		this.benchmarkMode = benchmarkMode;
	}

	/**
	 * if not enabled, incoming realtime events (time==0) will be inserted at
	 * the beginning of the next buffer
	 */
	public boolean isSchedulingRealtimeEvents() {
		return schedulingOfRealtimeEvents;
	}

	/**
	 * set the mode for realtime events. Should be disabled only for very small
	 * buffer sizes
	 */
	public void setSchedulingOfRealtimeEvents(boolean value) {
		this.schedulingOfRealtimeEvents = value;
	}

	// ////////////////////////////////////////////////////

	/**
	 * Reset the entire synth, remove all lines from the mixer regardless of
	 * clicks, etc. Otherwise like reset().
	 */
	public void hardReset() {
		AudioMixer localMixer = getMixer();
		if (localMixer == null) return;
		AudioInput[] lines = localMixer.getAudioStreamsArray();
		for (AudioInput ai : lines) {
			if (ai instanceof NoteInput) {
				localMixer.removeAudioStream(ai);
			}
		}
		reset();
		nextAudioSliceTime = new AudioTime(0);
	}

	/**
	 * Reset the entire synth:
	 * <ul>
	 * <li>Stop all playing notes</li>
	 * <li>Reset all controllers, programs, etc. to initial state</li>
	 * </ul>
	 */
	public void reset() {
		AudioMixer localMixer = getMixer();
		if (localMixer == null) return;
		AudioInput[] lines = localMixer.getAudioStreamsArray();
		for (AudioInput ai : lines) {
			if (ai instanceof NoteInput) {
				((NoteInput) ai).stopAsap();
			}
		}
		for (MidiChannel channel : channels) {
			if (channel != null) {
				channel.init();
			}
		}
		eventQueue.clear();
	}

	/**
	 * pre-conditions:
	 * <li>localMixer!=null
	 */
	private NoteInput getNoteFromMixer(AudioMixer localMixer,
			MidiChannel channel, int note) {
		AudioInput[] lines = localMixer.getAudioStreamsArray();
		for (AudioInput ai : lines) {
			if (ai instanceof NoteInput) {
				NoteInput ni = (NoteInput) ai;
				if (ni.getMidiChannel() == channel
						&& ni.getTriggerNote() == note && !ni.done()) {
					return ni;
				}
			}
		}
		return null;
	}

	/**
	 * pre-conditions:
	 * <li>exclusiveLevel != 0
	 * <li>localMixer!=null
	 * 
	 * @param exclusiveLevel
	 * @param program
	 * @param bank
	 * @return
	 */
	private NoteInput stopExclusiveNotes(AudioMixer localMixer,
			int exclusiveLevel, int program, int bank) {
		AudioInput[] lines = localMixer.getAudioStreamsArray();
		for (AudioInput ai : lines) {
			if (ai instanceof NoteInput) {
				NoteInput ni = (NoteInput) ai;
				Patch patch = ni.getPatch();
				if (patch.getExclusiveLevel() == exclusiveLevel
						&& patch.getBank() == bank
						&& patch.getProgram() == program && !ni.done()) {
					if (DEBUG_SYNTH) {
						System.out.println("Synth " + nextAudioSliceTime.getMillisTime()
								+ ": stopping exclusive note, level="
								+ patch.getExclusiveLevel() + " note:" + ni);
					}
					stopAsap(ni);
				}
			}
		}
		return null;
	}

	/**
	 * pre-conditions:
	 * <li>localMixer!=null
	 * 
	 * @param channel
	 * @param time
	 */
	private void releaseSustainedNotes(AudioMixer localMixer,
			MidiChannel channel, AudioTime time) {
		AudioInput[] lines = localMixer.getAudioStreamsArray();
		for (AudioInput ai : lines) {
			if (ai instanceof NoteInput) {
				NoteInput ni = (NoteInput) ai;
				if (ni.getMidiChannel() == channel && ni.isReleaseInhibited()
						&& !ni.done()) {
					// no need to check linked notes
					ni.release(time);
					if (DEBUG_SYNTH) {
						System.out.println("Synth " + nextAudioSliceTime.getMillisTime()
								+ ": releasing sustained note:" + ni);
					}
				}
			}
		}
	}

	/**
	 * pre-conditions:
	 * <li>localMixer!=null
	 * 
	 * @param channel
	 * @param time
	 * @param active
	 */
	private void handleSostenuto(AudioMixer localMixer, MidiChannel channel,
			AudioTime time, boolean active) {
		AudioInput[] lines = localMixer.getAudioStreamsArray();
		for (AudioInput ai : lines) {
			if (ai instanceof NoteInput) {
				NoteInput ni = (NoteInput) ai;
				if (ni.getMidiChannel() == channel && !ni.done()) {
					// no need to check linked notes
					ni.setSostenuto(time, active);
					if (DEBUG_SYNTH) {
						System.out.println("Synth " + nextAudioSliceTime.getMillisTime()
								+ ": " + "setting sostenuto to " + active
								+ ": note:" + ni);
					}
				}
			}
		}
	}

	/**
	 * pre-conditions:
	 * <li>localMixer!=null
	 * 
	 * @param channel
	 */
	private void handleAllSoundOff(AudioMixer localMixer, MidiChannel channel) {
		if (DEBUG_SYNTH) {
			System.out.println("Synth " + nextAudioSliceTime.getMillisTime()
					+ ": all sound off channel " + channel);
		}
		AudioInput[] lines = localMixer.getAudioStreamsArray();
		for (AudioInput ai : lines) {
			if (ai instanceof NoteInput) {
				NoteInput ni = (NoteInput) ai;
				if (ni.getMidiChannel() == channel) {
					// no need to check linked notes
					ni.stopAsap();
				}
			}
		}
	}

	/**
	 * pre-conditions:
	 * <li>localMixer!=null
	 * 
	 * @param channel
	 * @param time
	 */
	private void handleAllNotesOff(AudioMixer localMixer, MidiChannel channel,
			AudioTime time) {
		if (DEBUG_SYNTH) {
			System.out.println("Synth " + nextAudioSliceTime.getMillisTime()
					+ ": all notes off channel " + channel);
		}
		AudioInput[] lines = localMixer.getAudioStreamsArray();
		for (AudioInput ai : lines) {
			if (ai instanceof NoteInput) {
				NoteInput ni = (NoteInput) ai;
				if (ni.getMidiChannel() == channel && !ni.done()) {
					// no need to check linked notes
					ni.release(time);
				}
			}
		}
	}

	private void stopAsap(NoteInput ni) {
		NoteInput firstNI = ni;
		do {
			ni.stopAsap();
			ni = ni.getLinkedNoteInput();
		} while (ni != null && ni != firstNI);
	}

	private void noteOn(AudioTime time, MidiChannel channel, int note, int vel) {
		Soundbank localSoundbank = getSoundbank();
		AudioMixer localMixer = getMixer();
		if (localSoundbank == null || localMixer == null) return;

		NoteInput firstNoteStream = localSoundbank.createNoteInput(params,
				time, channel, note, vel);
		NoteInput thisNoteStream = firstNoteStream;

		if (firstNoteStream == null) {
			System.out.println("Synth: cannot find sample for channel=" + channel
					+ ", bank=" + channel.getBank() + ", program="
					+ channel.getProgram() + ", note=" + note + " vel="
					+ vel);
		} else {
			Patch patch = thisNoteStream.getPatch();
			if (patch.isSelfExclusive()) {
				NoteInput ni = getNoteFromMixer(localMixer, channel, note);
				if (ni != null) {
					if (DEBUG_SYNTH) {
						System.out.println("Synth " + nextAudioSliceTime.getMillisTime()
								+ ": " + "stopping self-exclusive note " + ni);
					}
					stopAsap(ni);
				}
			}

			if (patch.getExclusiveLevel() != 0) {
				if (DEBUG_SYNTH) {
					System.out.println("Synth " + nextAudioSliceTime.getMillisTime() + ": "
							+ "playing exclusive note, level="
							+ patch.getExclusiveLevel());
				}
				stopExclusiveNotes(localMixer, patch.getExclusiveLevel(),
						channel.getProgram(), channel.getBank());
			}
			// loop through all linked streams
			do {
				// allow identification of this NoteInput with the corresponding
				// NoteOff message (NoteInput may internally store a different
				// note number):
				thisNoteStream.setTriggerNote(note);
				localMixer.addAudioStream(thisNoteStream);
				if (DEBUG_SYNTH_TIMING) {
					if (thisNoteStream != firstNoteStream) {
						System.out.println("Synth NoteOn: nextAudioSlice="
								+ nextAudioSliceTime.getMillisTime() + "ms. "
								+ "adding linked instrument with "
								+ time.subtract(nextAudioSliceTime)
								+ " delay: " + thisNoteStream);
					} else {
						System.out.println("Synth NoteOn: nextAudioSlice="
								+ nextAudioSliceTime.getMillisTime() + "ms. "
								+ "adding instrument with "
								+ time.subtract(nextAudioSliceTime)
								+ " delay: " + thisNoteStream);
					}
				}
				thisNoteStream = thisNoteStream.getLinkedNoteInput();
				// the loop ends if the linked field is null, or points back to
				// the first NoteInput instance.
			} while (thisNoteStream != null
					&& thisNoteStream != firstNoteStream);
			if (DEBUG_SYNTH) debugShowAudioTime += 4;
		}
	}

	private void noteOff(AudioTime time, MidiChannel channel, int note) {
		AudioMixer localMixer = getMixer();
		if (localMixer == null) return;

		// Note: there may be several NoteInput's for one key, so we need to
		// iterate through all lines
		AudioInput[] lines = localMixer.getAudioStreamsArray();
		for (AudioInput ai : lines) {
			if (ai instanceof NoteInput) {
				NoteInput ni = (NoteInput) ai;
				if (ni.getMidiChannel() == channel
						&& ni.getTriggerNote() == note) {
					if (DEBUG_SYNTH) {
						if (!ni.done()) {
							System.out.println("Synth: Note Off: releasing note " + ni);
						} else {
							System.out.println("Synth: Note Off: note " + ni
									+ " already done playing.");
						}
					}
					if (!ni.done()) {
						ni.release(time);
					}
				}
			}
		}
	}

	/**
	 * The main MIDI event parsing and dispatching method
	 * 
	 * @param event the event to get dispatched
	 */
	private final void dispatchEvent(MidiEvent event) {
		if (DEBUG_SYNTH_IO) {
			System.out.println("Synth: Dispatching MIDI event " + event);
		}
		MidiChannel channel = getChannel(event.getChannel());
		switch (event.getStatus()) {
		case 0x80: // NOTE OFF
			noteOff(event.getTime(), channel, event.getData1());
			break;
		case 0x90: // NOTE ON
			if (event.getData2() == 0) {
				// NOTE ON with velocity=0 is equivalent to NOTE OFF
				noteOff(event.getTime(), channel, event.getData1());
			} else {
				noteOn(event.getTime(), channel, event.getData1(),
						event.getData2());
			}
			break;
		case 0xB0: // Controller Change
			channel.parseController(event.getData1(), event.getData2());
			handleControlChange(event.getTime(), channel, event.getData1(),
					event.getData2());
			break;
		case 0xC0: // Program Change
			channel.parseProgramChange(event.getData1());
			break;
		case 0xD0: // Channel Pressure
			channel.parseChannelPressure(event.getData1());
			break;
		case 0xE0: // Pitch Wheel
			channel.setPitchWheel(event.getData2(), event.getData1());
			handlePitchWheel(channel);
			break;
		}
		if (listeners.size() > 0) {
			// send this event to all listeners (asynchronously)
			listenerHandler.invokeLater(event);
		}
	}

	private void handleControlChange(AudioTime time, MidiChannel channel,
			int num, int data) {
		AudioMixer localMixer = getMixer();
		if (localMixer == null) return;

		switch (num) {
		case MidiChannel.SUSTAIN_PEDAL:
			if (data < 64) {
				releaseSustainedNotes(localMixer, channel, time);
			}
			break;
		case MidiChannel.SOSTENUTO_PEDAL:
			handleSostenuto(localMixer, channel, time, data >= 64);
			break;
		case MidiChannel.ALL_SOUND_OFF:
			handleAllSoundOff(localMixer, channel);
			break;
		case MidiChannel.RESET_ALL_CONTROLLERS:
			// TODO: notify the articulation objects individually of the changed
			// controllers?
			break;
		case MidiChannel.ALL_NOTES_OFF:
			handleAllNotesOff(localMixer, channel, time);
			break;
		}
		// notify the articulation modules of all playing notes on this channel
		AudioInput[] lines = localMixer.getAudioStreamsArray();
		for (AudioInput ai : lines) {
			if (ai instanceof NoteInput) {
				NoteInput ni = (NoteInput) ai;
				if (ni.getMidiChannel() == channel && !ni.done()) {
					// no need to check linked notes
					ni.getArticulation().controlChange(num, data);
				}
			}
		}
	}

	/**
	 * notify all active NoteInput voices of the change in pitch wheel.
	 */
	private void handlePitchWheel(MidiChannel channel) {
		AudioMixer localMixer = getMixer();
		if (localMixer == null) return;
		AudioInput[] lines = localMixer.getAudioStreamsArray();
		for (AudioInput ai : lines) {
			if (ai instanceof NoteInput) {
				NoteInput ni = (NoteInput) ai;
				if (ni.getMidiChannel() == channel && !ni.done()) {
					// no need to check linked notes
					ni.getArticulation().pitchWheelChange();
				}
			}
		}
	}

	/**
	 * Receive an event. The event's time should be aligned with the time of the
	 * Mixer, i.e. with the time passed to newAudioSlice().
	 * <p>
	 * The event's time (plus the FixedDelayOffset) is compared to the last
	 * audio slice's time. If it is smaller, it should have already been played
	 * and it is dispatched and parsed immediately. All other events (except
	 * long events and real time events) are added to the scheduler queue which
	 * is processed in newAudioSlice().
	 */
	// note: this method must not be synchronized, otherwise deadlock with
	// newAudioSlice()!
	public void midiInReceived(MidiEvent event) {

		// add the fixed delay to the event's time
		AudioTime eventTime;
		if (event.getTime().getNanoTime() == 0) {
			// time==0 means to schedule immediately
			if (masterClock != null && schedulingOfRealtimeEvents) {
				eventTime = masterClock.getAudioTime().add(fixedDelayNanos);
			} else {
				// just insert it at the beginning of the next buffer
				eventTime = event.getTime();
			}
		} else {
			eventTime = event.getTime().add(fixedDelayNanos);
		}
		if (DEBUG_SYNTH_IO) {
			// if (event.getStatus() == 0x90 && startDebugTime == 0) {
			// startDebugTime = System.nanoTime();
			// }
			// System.out.println("Synth "+((System.nanoTime() - startDebugTime)/1000000)+":
			// Incoming MIDI event " + event);
			System.out.println("Synth: Incoming MIDI event: " + event);
		}
		// patch the event so that it has the corrected time
		if (benchmarkMode) {
			// in benchmark mode, set channel to 10, note to 32 and velocity to
			// 127 */
			int status = event.getStatus() & 0xF0;
			if (status == 0x90 && event.getData2() > 0) {
				event = new MidiEvent(event.getSource(), eventTime,
						9 /* 0-based */, event.getStatus(), BENCHMARK_NOTE,
						0x7F);
			} else {
				// just change channel to 10
				event = event.cloneNewTimeChannel(eventTime, 9 /* 0-based */);
			}
		} else {
			event = event.clone(eventTime);
		}
		if (DEBUG_SYNTH_TIMING) {
			String add = "";
			if (masterClock != null) {
				long master = masterClock.getAudioTime().getMillisTime();
				long masterSliceDiff = nextAudioSliceTime.getMillisTime()
						- master;
				add = " | master=" + master + "ms, masterSliceDiff="
						+ masterSliceDiff + "ms";
				if (masterSliceDiff > (fixedDelayNanos / 1000000L)) {
					add += " ## > fixedDelay=" + (fixedDelayNanos / 1000000L)
							+ "ms!";
				}
			}
			long eventSliceDiff = eventTime.getMillisTime()
					- nextAudioSliceTime.getMillisTime();
			System.out.println("Synth: Incoming: " + "adjustedEventTime="
					+ eventTime.getMillisTime() + "ms, " + "nextSlice="
					+ nextAudioSliceTime.getMillisTime() + "ms, "
					+ "eventSliceDiff=" + eventSliceDiff + "ms" + add);
			if (DEBUG_SYNTH) debugShowAudioTime += 2;
		}

		// all events are enqueued

		// $$fb do not use this "direct path": it will not be processed
		// before the next audio slice anyway, in which case it will
		// be retrieved from the queue anyway. Enqueuing all events
		// increases predictability.
		// // don't queue if the event is already too late
		// if (nextAudioSliceTime.laterOrEqualThan(eventTime)) {
		// if (DEBUG_SYNTH) {
		// System.out.println("## Synth: event comes too late for correct scheduling! "
		// + "event=" + event);
		// }
		// dispatchEvent(event);
		// } else {
		if (!event.isLong() && !event.isRealtimeEvent()) {
			// schedule this event for usage by newAudioSlice callback
			eventQueue.offer(event);
		}
		// }
	}

	// listener AudioRendererListener
	/**
	 * goes through the queued MIDI events and dispatch/execute them. If an
	 * asynchronous renderer is used, start rendering the new slice.
	 */
	public final void newAudioSlice(AudioTime time, AudioTime duration) {
		AudioTime nextNextAudioSliceTime = time.add(duration);
		nextAudioSliceDuration = duration;

		if (DEBUG_SYNTH) {
			if (debugShowAudioTime > 0) {
				System.out.println("Synth.newAudioSlice: Audio Time: "
						+ time.getMillisTime() + "ms, queue size="
						+ eventQueue.size());
				if (masterClock != null) {
					long master = masterClock.getAudioTime().getMillisTime();
					long diff = (time.getMillisTime() - master);
					System.out.println("           master:" + master
							+ "ms, masterSliceDiff=" + diff + "ms.");
					if (diff > (fixedDelayNanos / 1000000)) {
						System.out.println(" ## > fixedDelay=" + (fixedDelayNanos / 1000000)
								+ "ms!");
					}
				}
				debugShowAudioTime--;
			}
		}

		if (!isNoteDispatcherRunning()) {
			synchronized (eventQueue) {
				while (eventQueue.lastIsEarlier(nextNextAudioSliceTime)) {
					dispatchEvent(eventQueue.poll());
				}
			}
		}

		if (asynchRenderer != null) {
			AudioMixer localMixer = getMixer();
			if (localMixer.getCount() > ASYNCH_RENDER_STREAM_THRESHOLD) {
				asynchRenderer.dispatch(time, localMixer.getRenderables());
			}
		}

		// eventually, commit the new nextAudioSlice time
		nextAudioSliceTime = nextNextAudioSliceTime;
	}

	// listener EventDispatcher
	public void onAsynchronousExecution(MidiEvent me) {
		int size = listeners.size();
		MidiChannel mc = getChannel(me.getChannel());
		AudioTime time = me.getTime();
		if (size == 1) {
			listeners.get(0).midiEventPlayed(time, mc,
					me.getStatus(), me.getData1(), me.getData2());
		} else {
			for (SynthesizerListener L : listeners) {
				L.midiEventPlayed(time, mc, me.getStatus(),
						me.getData1(), me.getData2());
			}
		}
	}

	/**
	 * do a small dry run in order to load class files (and possibly initialize
	 * the JIT) before the first actual note. Only call this function BEFORE
	 * engaging in actual I/O! A mixer must have be set before calling this
	 * method.
	 */
	public void preLoad() {
		// a buffer with arbitrary format for reading from the mixer
		AudioBuffer b = new AudioBuffer(2, 44, 44100.0);
		AudioTime time = new AudioTime(0);
		AudioTime duration = new AudioTime(b.getSampleCount(),
				b.getSampleRate());
		// play 20 notes
		for (int note = 0; note < 20; note++) {
			// "play" the note
			noteOn(new AudioTime(0), getChannel(0), 30 + (note * 2), 120);
			// "render" the note for half a second
			for (int i = 0; i < 500; i++) {
				getMixer().read(time, b, 0, b.getSampleCount());
				time = time.add(duration);
			}
		}
		// clean up and load java.util.Iterator in cleanUp()
		getMixer().cleanUp();
		getMixer().clear();
		new MidiEvent(null, 0, 0x90, 0, 0, 0);
		// preload some classes that will be needed when the first "real" note
		// is played
		try {
			Class.forName("java.util.EventObject");
			Class.forName("javax.sound.sampled.LineEvent");
		} catch (Throwable t) {
			System.out.println(String.valueOf(t));
		}

	}

	/**
	 * Global parameters like master volume, master tuning, etc. that are used
	 * by the individual note generators.
	 * 
	 * @author florian
	 */
	public static class Params {

		/**
		 * Factor by which the "external" volume is multiplied so that a more
		 * intuitive scale is achieved
		 */
		private static final double MASTER_VOLUME_FACTOR_EXTERNAL = 0.3;

		/**
		 * default master volume
		 */
		private double masterVolume = MASTER_VOLUME_FACTOR_EXTERNAL;

		private double masterTuningFactor = 1.0f;

		double getMasterVolumeInternal() {
			return masterVolume;
		}

		/**
		 * Set the master volume. Internally, the volume is applied with a
		 * factor to achieve a default attenuation to prevent clipping.
		 * 
		 * @return the master volume on a linear scale
		 */
		public double getMasterVolume() {
			return masterVolume / MASTER_VOLUME_FACTOR_EXTERNAL;
		}

		/**
		 * Set the master volume. Internally, this volume is attenuated by a
		 * constant factor to prevent clipping.
		 * 
		 * @param vol the master volume to set 0:silence] 1.0: 0dB
		 */
		public void setMasterVolume(double vol) {
			if (vol < 0.0) {
				masterVolume = 0.0;
			} else {
				masterVolume = vol * MASTER_VOLUME_FACTOR_EXTERNAL;
			}
		}

		/**
		 * master tuning in Hertz
		 */
		public void setMasterTuning(double masterFreq) {
			masterTuningFactor = masterFreq / 440.0;
		}

		/**
		 * @return master tuning in Hertz, rounded to 5 decimals
		 */
		public double getMasterTuning() {
			return Math.round(masterTuningFactor * 4400000.0) / 10000.0;
		}

		/**
		 * @return master tuning as a linear factor
		 */
		public double getMasterTuningFactor() {
			return masterTuningFactor;
		}
	}

	/**
	 * A queue implementation to maintain a sorted FIFO list of MidiEvents. The
	 * MidiEvents are inserted at the beginning of the queue. If the Comparable
	 * of the inserted event is higher than the first element, it is inserted at
	 * the next index, etc.
	 * <p>
	 * An own class for this functionality is necessary in order to maintain
	 * FIFO sorting of elements with the same time (i.e. Comparable.compareTo()
	 * returns 0). Also, this class can use an optimal implementation for the
	 * only 2 used methods: offer() (sorted insert at the beginning) and poll()
	 * (remove last element). Internally, it uses a double-linked list to assure
	 * that.
	 * <p>
	 * Java's PriorityQueue and PriorityBlockingQueue could not be used for
	 * this, because the ordering is not maintained, causing e.g. program change
	 * messages to be dispatched after the note on message if the program change
	 * and the note on message were delivered with the same time stamp, but the
	 * program change message was delivered first.
	 * <p>
	 * This implementation is synchronized.
	 * 
	 * @author florian
	 */
	private static class EventQueue {

		private LinkedList<MidiEvent> list;

		private volatile boolean closed = false;

		public EventQueue() {
			list = new LinkedList<MidiEvent>();
		}

		/**
		 * close operation on this queue, so any thread waiting in
		 * waitForEvent() will be released
		 */
		public synchronized void close() {
			closed = true;
			this.notifyAll();
		}

		/**
		 * Inserts the MidiEvent into the queue. It is sorted in a correct
		 * interpretation of MIDI timing.
		 * 
		 * @param me the element to insert
		 */
		public synchronized void offer(MidiEvent me) {
			if (!list.isEmpty()) {
				// go through all elements until it finds one with higher time
				ListIterator<MidiEvent> it = list.listIterator(0);
				long nanoTime = me.getTime().getNanoTime();
				while (it.hasNext()) {
					MidiEvent lme = it.next();
					if (lme.getTime().getNanoTime() <= nanoTime) {
						it.previous();
						// out("inserting at index "+it.nextIndex()+": "+me);
						it.add(me);
						// for (int i=0; i<list.size(); i++) {
						// out(" "+i+": "+list.get(i)); }
						this.notifyAll();
						return;
					}
				}
			}
			// if the list is empty, or it could not be inserted in the
			// loop above, insert at the end
			// out("inserting at index "+list.size()+": "+me);
			list.add(me);
			this.notifyAll();
			// for (int i=0; i<list.size(); i++) {
			// out(" "+i+": "+list.get(i)); }
		}

		/**
		 * Returns true if the queue has at least one element, and if the last
		 * element is earlier than the specified time.
		 * 
		 * @param time the time to test the last element for
		 * @return true if the last element exists and is earlier than
		 *         <code>time</code>
		 */
		public synchronized boolean lastIsEarlier(AudioTime time) {
			return ((list.size() > 0) && (list.getLast().getTime().earlierThan(time)));
		}

		/**
		 * @return Retrieves and removes the head of this queue, or null if this
		 *         queue is empty.
		 */
		public synchronized MidiEvent poll() {
			return list.removeLast();
		}

		/**
		 * @return Retrieves and removes the head of this queue if its time is
		 *         earlier than the given time, or null otherwise
		 */
		public synchronized MidiEvent pollIfEarlier(AudioTime time) {
			if (list.size() > 0) {
				MidiEvent event = list.getLast();
				if ((event.getTime().earlierThan(time))) {
					return list.removeLast();
				}
			}
			return null;
		}

		/**
		 * @return Retrieves and removes the head of this queue if its time is
		 *         earlier than the given time, or null otherwise
		 */
		public synchronized MidiEvent pollIfEarlier(long nanoTime) {
			if (list.size() > 0) {
				MidiEvent event = list.getLast();
				if ((event.getTime().getNanoTime() < nanoTime)) {
					return list.removeLast();
				}
			}
			return null;
		}

		/**
		 * Cleans this queue.
		 */
		public synchronized void clear() {
			list.clear();
		}

		/**
		 * @return the size of the list
		 */
		public synchronized int size() {
			return list.size();
		}

		/**
		 * Returns true if there are no events in the queue.
		 * 
		 * @return true if the list is empty
		 */
		public synchronized boolean isEmpty() {
			return (list.size() > 0);
		}

		/**
		 * Wait until an event is added to the queue.
		 */
		public synchronized void waitForEvent() {
			if (!closed) {
				try {
					while (list.size() == 0) {
						this.wait();
					}
				} catch (InterruptedException ie) {
				}
			}
		}
	}

	private class NoteDispatcher implements Runnable {

		private final static int SLEEP_MICROSECONDS = 75;

		private Thread thread;
		private volatile boolean doStop = false;

		/**
		 * create a thread and start it
		 */
		public NoteDispatcher() {
			thread = ThreadFactory.createThread(this,
					"Asynchronous Note Dispatcher", NOTE_DISPATCHER_PRIORITY);
		}

		/**
		 * a blocking stop
		 */
		public void stop() {
			doStop = true;
			synchronized (this) {
				try {
					thread.join(2000);
				} catch (InterruptedException ie) {
				}
			}

		}

		public void run() {
			if (DEBUG_SYNTH) {
				System.out.println("Synthesizer: starting asynchronous note dispatcher");
			}
			MidiEvent event = null;
			while (!doStop) {
				try {
					// 2 loops to not enter the try block for every loop
					while (!doStop) {
						// asynchronous note dispatcher needs to insert up to
						// one buffer after "nextAudioSliceTime"!
						// also start events a little ahead so that they can't
						// come too late
						event = eventQueue.pollIfEarlier(nextAudioSliceTime.getNanoTime()
								+ nextAudioSliceDuration.getNanoTime());
						if (event != null) {
							dispatchEvent(event);
						} else {
							if (eventQueue.isEmpty()) {
								eventQueue.waitForEvent();
							} else {
								Thread.sleep(0, SLEEP_MICROSECONDS * 1000);
							}
						}
					}
				} catch (Exception e) {
					System.out.println(String.valueOf(e));
				}
			}
			if (DEBUG_SYNTH) {
				System.out.println("Synthesizer: stopped asynchronous note dispatcher");
			}
		}
	}

}
