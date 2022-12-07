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
package synth.test;

import synth.engine.*;
import synth.modules.*;
import synth.soundfont2.SoundFontSoundbank;
import synth.utils.AudioUtils;
import synth.utils.MidiUtils;

import javax.sound.sampled.AudioFormat;
import java.io.File;

/**
 * A console synth implementation that uses a SoundFont 2 soundbank.
 * 
 * @author florian
 */

public class SoundFont2Synth {

	public static final double DEFAULT_LATENCY = 100.0;
	public static final double DEFAULT_SLICE_TIME = 1.0;

	// fields needed in the timeOutThread
	private static boolean running = false;
	private static final Object runLock = new Object();
	private static final int timeOutSeconds = 0; // no timeOut
	private static Synthesizer synth = null;
	private static AudioSink sink = null;
	private static double latencyMillis = DEFAULT_LATENCY;
	private static double volumeDB = 0;

	// needed by play method
	private static SMFMidiIn smfPlayer = null;
	private static MaintenanceThread maintenance = null;

	private static final MemoryAllocator memAllocator = null;

	public static void start(String customSoundBankPath, byte[] midiData, int musicVolume) throws Exception {
		String soundbankFile = customSoundBankPath;
		int[] midiDevs = new int[30];
		int midiDevCount = 0;
		String[] dmidiDevs = new String[30];
		int dmidiDevCount = 0;
		int audioDev = -2; // use default device
		String directAudioDev = "";
		String eventronAudioDev = "";
		double sliceTimeMillis = DEFAULT_SLICE_TIME;
		String outputFile = "";
		double sampleRate = 44100.0;
		int channels = 2;
		int bitsPerSample = 16;
		int threadCount = -1;
		boolean useJavaSoundMidiPlayback = true;
		boolean benchmarkMode = false;
		boolean lowlatencyMode = false;
		boolean interactive = true;
		boolean errorOccured = false;
		int noteDispatcherMode = Synthesizer.NOTE_DISPATCHER_REQUEST_ASYNCHRONOUS;
		double playWaitTime = 0.0;
		boolean preload = true;

		// verify parameters
		File sbFile = new File(soundbankFile);
		if (sbFile.isDirectory() || !sbFile.exists()) {
			System.out.println("Invalid soundfont file: " + sbFile);
			System.out.println("");
		}
		File wavFile = null;
		byte[] mFile = null;
		if (midiData != null) {
			mFile = midiData;
			if (mFile == null) {
				System.out.println("Invalid MIDI input file: " + mFile);
			}
		}
		if (((audioDev >= -1 ? 1 : 0) + (directAudioDev != "" ? 1 : 0) + (eventronAudioDev != "" ? 1
				: 0)) > 1) {
			System.out.println("Error: can only select one audio device!");
			System.out.println("");
		}
		// use Java Sound's default device if neither -a nor -da is specified
		if (audioDev == -2 && directAudioDev == "" && eventronAudioDev == "") {
			audioDev = -1;
		}
		// non-interactive only possible with duration
		if (!interactive && timeOutSeconds == 0) {
			System.out.println("Refuse to run in non-interactive mode without timeout.");
			interactive = true;
		}
		if (lowlatencyMode && latencyMillis > 2.0) {
			System.out.println("WARNING: using low latency mode with larger buffer size will increase jitter!");
		}

		AudioFormat format = new AudioFormat((float) sampleRate, bitsPerSample,
				channels, true, false);
		DiskWriterSink waveSink = null;
		AudioPullThread pullThread = null;
		DirectMidiIn[] dmidis = null;
		JavaSoundMidiIn[] midis = null;

		// STARTUP ENGINE
		try {
			System.out.println("Loading SoundFont " + sbFile.getName() + "...");
			SoundFontSoundbank soundFont = new SoundFontSoundbank(sbFile);

			// set up mixer
			AudioMixer mixer = new AudioMixer();

			// slice time should always be <= latency
			if (sliceTimeMillis > latencyMillis) {
				sliceTimeMillis = latencyMillis;
			}
			// create the pull thread
			pullThread = new AudioPullThread();
			pullThread.setSliceTimeMillis(sliceTimeMillis);
			pullThread.setInput(mixer);

			// set up soundcard (sink)
			int bufferSizeSamples = pullThread.getPreferredSinkBufferSizeSamples(
					latencyMillis, format.getSampleRate());
			if (audioDev >= -1) {
				System.out.println("creating JavaSoundSink...");
				JavaSoundSink jsSink = new JavaSoundSink();
				// open the sink
				jsSink.open(audioDev, format, bufferSizeSamples);
				System.out.println(jsSink.getName());
				sink = jsSink;
				format = jsSink.getFormat();
			} else if (directAudioDev != "") {
				System.out.println("creating DirectAudioSink: " + directAudioDev);
				DirectAudioSink daSink = new DirectAudioSink();
				daSink.open(directAudioDev, format, bufferSizeSamples);
				sink = daSink;
				format = daSink.getFormat();
				System.out.println("- slice: "
						+ pullThread.getSliceTimeSamples(format.getSampleRate())
						+ " samples = "
						+ (pullThread.getSliceTime() * 1000.0)
						+ "ms, period: "
						+ daSink.getPeriodSizeSamples() + " samples = "
						+ (daSink.getPeriodTimeNanos() / 1000000.0)
						+ "ms, ALSA buffer: "
						+ daSink.getALSABufferSizeSamples() + " samples = "
						+ (daSink.getALSABufferTimeNanos() / 1000000.0)
						+ "ms");

			}
			System.out.println("- audio format: " + format.getChannels() + " channels, "
					+ format.getSampleSizeInBits() + " bits, "
					+ format.getFrameSize() + " bytes per frame, "
					+ (format.getSampleRate()) + " Hz, "
					+ (format.isBigEndian() ? "big endian" : "little endian"));

			// use effective latency
			latencyMillis = AudioUtils.samples2nanos(sink.getBufferSize(),
					sampleRate) / 1000000.0;
			pullThread.setSink(sink);
			// set up Synthesizer
			System.out.println("creating Synthesizer: ");
			if (synth == null) {
				synth = new Synthesizer(soundFont, mixer);
				synth.setFixedDelayNanos((long) (((2.0 * latencyMillis)) * 1000000.0));
				synth.setMasterClock(sink);
				synth.getParams().setMasterTuning(443);
				synth.setNoteDispatcherMode(noteDispatcherMode);
				System.out.println((synth.getParams().getMasterTuning())
						+ "Hz tuning, ");
				if (benchmarkMode) {
					synth.setBenchmarkMode(true);
					if (volumeDB == 0.0) {
						volumeDB = AudioUtils.linear2decibel(4.0);
					}
				}
				if (lowlatencyMode) {
					synth.setSchedulingOfRealtimeEvents(false);
				}
				synth.getParams().setMasterVolume(
						AudioUtils.decibel2linear(volumeDB));
				if (volumeDB != 0.0) {
					System.out.println("volume: "
							+ (AudioUtils.linear2decibel(synth.getParams().getMasterVolume()))
							+ "dB (linear: "
							+ (synth.getParams().getMasterVolume()) + "), ");
				}
				pullThread.addListener(synth);
				if (threadCount >= 0) {
					synth.setRenderThreadCount(threadCount);
				}
				if (preload) {
					System.out.println("preloading, ");
					synth.preLoad();
				}
				setVolume(musicVolume / 100.0);
				synth.start();
				if (synth.getRenderThreadCount() > 1) {
					System.out.println("" + synth.getRenderThreadCount()
							+ " render threads");
				} else {
					System.out.println("rendering from mixer thread");
				}
			}

			// set up wave output
			if (wavFile != null) {
				System.out.println("setting up wave file output to file "+wavFile);
				waveSink = new DiskWriterSink();
				waveSink.open(wavFile, format);
				pullThread.setSlaveSink(waveSink);
			}
			
			System.out.println("MIDI ports:");

			// open Java Sound MIDI ports?
			midis = new JavaSoundMidiIn[midiDevCount];
			midiDevCount = 0;
			for (int i = 0; i < midis.length; i++) {
				if (midiDevs[i] < JavaSoundMidiIn.getMidiDeviceCount()) {
					System.out.println(" Java Sound ");
					JavaSoundMidiIn midi = new JavaSoundMidiIn(i);
					midis[midiDevCount] = midi;
					midi.addListener(synth);
					try {
						midi.open(midiDevs[i]);
						System.out.println(midi.getName());
						if (lowlatencyMode) {
							midi.setTimestamping(false);
						}
						midiDevCount++;
					} catch (Exception e) {
						System.out.println("ERROR:");
						System.out.println(" no MIDI: " + e.toString());
					}
				}
			}
			// open Direct MIDI ports?
			dmidis = new DirectMidiIn[dmidiDevCount];
			dmidiDevCount = 0;
			for (int i = 0; i < dmidis.length; i++) {
				System.out.println(" Direct " + dmidiDevs[i]);
				DirectMidiIn dmidi = new DirectMidiIn(i);
				dmidis[dmidiDevCount] = dmidi;
				dmidi.addListener(synth);
				try {
					dmidi.open(dmidiDevs[i]);
					if (lowlatencyMode) {
						dmidi.setTimestamping(false);
					}
					dmidiDevCount++;
				} catch (Exception e) {
					System.out.println("ERROR:");
					System.out.println(" no MIDI: " + e.toString());
				}
			}

			if (midiDevCount + dmidiDevCount == 0) {
				System.out.println("none");
			} else {
				// new line
				System.out.println("");
			}

			// create the maintenance thread
			maintenance = new MaintenanceThread();

			maintenance.addServiceable(mixer);
			for (int i = 0; i < midiDevCount; i++) {
				maintenance.addAdjustableClock(midis[i]);
			}
			for (int i = 0; i < dmidiDevCount; i++) {
				maintenance.addAdjustableClock(dmidis[i]);
			}
			maintenance.setMasterClock(sink);

			// push the MIDI input file to the synthesizer (at once)
			if (mFile != null) {
				if (useJavaSoundMidiPlayback) {
					playMidiFileWithJavaSound(mFile);
				} else {
					System.out.println("Playing MIDI File");
					SMFPusher pusher = new SMFPusher();
					pusher.open(mFile);
					int events = pusher.pushToSynth(synth);
					System.out.println(", duration:" + (pusher.getDurationInSeconds())
							+ "s, " + events + " notes.");
				}
			}

			// start memory allocator
			if (memAllocator != null) {
				System.out.println("Starting memory allocator");
				memAllocator.setEnabled(true);
			}
			// start the pull thread -- from now on, the mixer is polled
			// for new data
			pullThread.start();
			// start the maintenance thread
			maintenance.start();

			if (ThreadFactory.hasRealtimeThread()) {
				if (ThreadFactory.couldSetRealtimeThreadPriority()) {
					System.out.println("Using realtime threads with high priority");
				} else {
					System.out.println("Using realtime threads with default priority");
				}
			} else {
				System.out.println("Realtime threads are not enabled.");
			}

			if (smfPlayer != null) {
				// grace period for playback start to warm up engine
				try {
					Thread.sleep((int) (playWaitTime * 1000.0));
				} catch (InterruptedException ie) {
				}
				smfPlayer.start();
			}
			maintenance.synchronizeClocks(true);

			running = true;

			// if we're using a time out, start a thread to get stdin
			if (interactive && timeOutSeconds > 0) {
				Thread t = new Thread(new Runnable() {
					public void run() {
						interactiveKeyHandling();
					}
				});
				t.setDaemon(true);
				t.start();
			}
			if (timeOutSeconds == 0) {
				interactiveKeyHandling();
			} else {
				long startTime = System.nanoTime();
				while (running) {
					try {
						synchronized (runLock) {
							runLock.wait(1000);
						}
						if (timeOutSeconds > 0) {
							long elapsedSeconds = (System.nanoTime() - startTime) / 1000000000L;
							if (elapsedSeconds >= timeOutSeconds) {
								System.out.println("Timeout reached. Stopping...");
								running = false;
								break;
							}
						}
					} catch (Exception e) {
						System.out.println(String.valueOf(e));
					}
				}
			}
		} catch (Throwable t) {
			System.out.println(String.valueOf(t));
		}

		// clean-up
		if (memAllocator != null) {
			memAllocator.setEnabled(false);
		}

		if (smfPlayer != null) {
			try {
				smfPlayer.close();
			} catch (Exception e) {
				System.out.println(String.valueOf(e));
			}
		}
		if (waveSink != null) {
			waveSink.close();
		}
		if (maintenance != null) {
			maintenance.stop();
		}
		if (pullThread != null) {
			pullThread.stop();
			System.out.println("Resynchronizations of audio device: " + pullThread.getResynchCounter());
		}
		for (int i = 0; i < midiDevCount; i++) {
			if (midis[i] != null) {
				midis[i].close();
			}
		}
		for (int i = 0; i < dmidiDevCount; i++) {
			if (dmidis[i] != null) {
				dmidis[i].close();
			}
		}
		if (synth != null) {
			synth.close();
		}
		if (sink != null) {
			sink.close();
			if (sink instanceof DirectAudioSink) {
				System.out.println("Underruns of the audio device     : " + ((DirectAudioSink) sink).getUnderRunCount());
			}
		}

		// done
		System.out.println("Done.");
	}

	private static void interactiveKeyHandling() {
		int note = -1;
		int ignoreNext = 0;
		try {
			while (true) {
				int c = (int) ((char) System.in.read());

				if (ignoreNext > 0 && (c == 10 || c == 13)) {
					ignoreNext--;
					continue;
				}

				if (c != 10 && c != 13) {
					ignoreNext = 2;
				}

				// finish a previously played note
				if (note >= 0) {
					synth.midiInReceived(new MidiEvent(null,
							sink.getAudioTime(), 0, 0x90, note, 0));
					note = -1;
				}
				if (c == 'q') {
					System.out.println("stopping...");
					break;
				}
				if (c == 'p') {
					System.out.println("Reset.");
					synth.reset();
				}
				if (c == 32 || c == 10 || c == 13) {
					// generate a random Note On event on a white key
					do {
						note = ((int) (Math.random() * 40)) + 40;
					} while (!MidiUtils.isWhiteKey(note));
					int vel = ((int) (Math.random() * 40)) + 87;
					System.out.println("Playing: NoteOn " + note + "," + vel);
					synth.midiInReceived(new MidiEvent(null,
							sink.getAudioTime(), 0, 0x90, note, vel));
					if (c != 32) ignoreNext++;
				}
			}
		} catch (Exception e) {
			System.out.println(String.valueOf(e));
		}
		running = false;
		synchronized (runLock) {
			runLock.notifyAll();
		}
	}

	private static void playMidiFileWithJavaSound(byte[] file) {
		try {
			smfPlayer = new SMFMidiIn();
			smfPlayer.open(file);

			// MIDI devices
			smfPlayer.addListener(synth);
			maintenance.addAdjustableClock(smfPlayer);

			// make sure the clocks are synchronized
			maintenance.synchronizeClocks(true);
			System.out.println("MIDI file playing with Java Sound: " + file);

		} catch (Exception e) {
			System.out.println("Cannot load MIDI file: " + file);
			System.out.println(String.valueOf(e));
		}
	}

	public static double getVolume() {
		if (synth != null) {
			return synth.getParams().getMasterVolume();
		}
		return volumeDB;
	}

	public static void setVolume(double volume) {
		if (synth != null) {
			synth.getParams().setMasterVolume(volume);
		}
	}

	public static void fadeOut() {
		new Thread(() -> {
			try {
				if (synth != null) {
					double volume = synth.getParams().getMasterVolume();
					while (volume != 0.0) {
						volume = -0.01;
						Thread.sleep(10);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

	public static void stop() {
		if (smfPlayer != null) {
			smfPlayer.stop();
		}
	}

	public static void close() {
		if (smfPlayer != null) {
			smfPlayer.close();
			smfPlayer = null;
		}
	}
}
