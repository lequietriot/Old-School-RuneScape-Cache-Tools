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
package synth.modules;

import synth.engine.AdjustableAudioClock;
import synth.engine.AudioBuffer;
import synth.engine.AudioSink;
import synth.engine.AudioTime;
import synth.utils.AudioUtils;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

/**
 * An AudioSink that writes to a Java Sound SourceDataLine.
 * 
 * @author florian
 */

public class JavaSoundSink implements AudioSink, AdjustableAudioClock {

	public static boolean DEBUG_SINK = false;

	/**
	 * The SourceDataLine used to access the soundcard
	 */
	private SourceDataLine sdl;

	/**
	 * the name of the open device
	 */
	private String devName;

	/**
	 * The current (or previous) sample rate.
	 */
	private double sampleRate = 44100.0;

	/**
	 * List of usable audio devices, i.e. they provide a SourceDataLine (line
	 * out/speaker).
	 */
	private static List<Mixer.Info> devList;

	/**
	 * A temporary byte buffer for conversion to the native format
	 */
	private byte[] byteBuffer;

	/**
	 * Flag to track if the line was started
	 */
	private boolean started = false;

	/**
	 * The current offset of the audio clock (interface AdjustableAudioClock) in
	 * samples.
	 */
	private long clockOffsetSamples = 0;

	/**
	 * Constructor for this sink
	 */
	public JavaSoundSink() {
		setupAudioDevices();
	}

	/**
	 * Open the soundcard with the given format and buffer size in milliseconds.
	 * The audio time (regardless of the time offset) will be reset to 0.
	 */
	public synchronized void open(int devIndex, int bufferSizeInMillis,
			AudioFormat format) throws LineUnavailableException, Exception {
		open(devIndex, format, (int) AudioUtils.millis2samples(
				bufferSizeInMillis, format.getSampleRate()));
	}

	/**
	 * Open the soundcard with the given format. The audio time (regardless of
	 * the time offset) will be reset to 0.
	 */
	public synchronized void open(int devIndex, AudioFormat format,
			int bufferSizeInSamples) throws LineUnavailableException, Exception {
		if (devIndex < -1 || devIndex >= devList.size()) {
			throw new Exception("illegal audio device index: " + devIndex);
		}
		if (devIndex < 0) {
			if (DEBUG_SINK) System.out.println("Opening default soundcard...");
			devName = "(default)";
			sdl = AudioSystem.getSourceDataLine(format);
			if (DEBUG_SINK) System.out.println(sdl.getClass().getName());
		} else {
			Mixer.Info info = devList.get(devIndex);
			devName = info.getName();
			if (DEBUG_SINK) {
				System.out.println("Opening audio out device: " + devName + " ("
						+ info.getDescription() + ")");
			}
			sdl = AudioSystem.getSourceDataLine(format, info);
		}
		sdl.open(format, bufferSizeInSamples * format.getFrameSize());

		sampleRate = (double) format.getSampleRate();
		if (DEBUG_SINK) {
			System.out.println("Buffer size = "
					+ sdl.getBufferSize()
					+ " bytes = "
					+ (sdl.getBufferSize() / format.getFrameSize())
					+ " samples = "
					+ (AudioUtils.samples2micros(sdl.getBufferSize()
							/ format.getFrameSize(), format.getSampleRate()) / 1000.0)
					+ " millis");
		}
	}

	public synchronized void close() {
		started = false;
		if (sdl != null) {
			sdl.close();
		}
		if (DEBUG_SINK) System.out.println("closed soundcard: "+devName);
	}

	public boolean isOpen() {
		return (sdl != null && sdl.isOpen());
	}

	public AudioFormat getFormat() {
		return sdl.getFormat();
	}

	public static List<Mixer.Info> getDeviceList() {
		setupAudioDevices();
		return devList;
	}

	public static boolean isJavaSoundEngine(Mixer.Info info) {
		return info.getName().indexOf("Java Sound Audio Engine") >= 0;
	}

	private static void setupAudioDevices() {
		if (devList == null) {
			if (DEBUG_SINK) System.out.println("Gathering Audio devices...");
			devList = new ArrayList<Mixer.Info>();
			Mixer.Info[] infos = AudioSystem.getMixerInfo();
			// go through all audio devices and see if they provide input
			// line(s)
			for (Mixer.Info info : infos) {
				Mixer m = AudioSystem.getMixer(info);
				Line.Info[] lineInfos = m.getSourceLineInfo();
				for (Line.Info lineInfo : lineInfos) {
					if (lineInfo instanceof DataLine.Info) {
						// we found a source data line, so we can add this mixer
						// to the list of supported devices
						devList.add(info);
						break;
					}
				}
			}
			if (DEBUG_SINK) {
				System.out.println("done (" + devList.size() + " devices available).");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.AudioSink#write(synth.engine.AudioBuffer)
	 */
	public synchronized void write(AudioBuffer buffer) {
		if (!isOpen()) {
			return;
		}
		// if the device is not started, start it
		if (!started || !sdl.isActive()) {
			sdl.start();
			started = true;
		}
		// set up the temporary buffer that receives the converted
		// samples in bytes
		int requiredSize = buffer.getByteArrayBufferSize(getFormat());
		if (byteBuffer == null || byteBuffer.length < requiredSize) {
			byteBuffer = new byte[requiredSize];
		}

		int length = buffer.convertToByteArray(byteBuffer, 0, getFormat());
		sdl.write(byteBuffer, 0, length);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.AudioClock#getClockTime()
	 */
	public AudioTime getAudioTime() {
		if (sdl != null) {
			return new AudioTime(sdl.getLongFramePosition()
					+ clockOffsetSamples, getSampleRate());
		} else {
			return new AudioTime(clockOffsetSamples, getSampleRate());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.AdjustableAudioClock#getTimeOffset()
	 */
	public AudioTime getTimeOffset() {
		return new AudioTime(clockOffsetSamples, getSampleRate());
	}

	/**
	 * Set the clock offset. This offset is internally stored in samples, so you
	 * should only call it when this sink is open, or has already been open with
	 * the correct sample rate.
	 * 
	 * @see AdjustableAudioClock#setTimeOffset(AudioTime)
	 */
	public void setTimeOffset(AudioTime offset) {
		this.clockOffsetSamples = offset.getSamplesTime(getSampleRate());
	}

	/*
	 * (non-Javadoc) @return the buffer size of this sink in samples
	 * 
	 * @see synth.engine.AudioSink#getBufferSize()
	 */
	public int getBufferSize() {
		if (sdl != null) {
			return sdl.getBufferSize() / sdl.getFormat().getFrameSize();
		} else {
			return 1024; // something arbitrary
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.AudioSink#getChannels()
	 */
	public int getChannels() {
		if (sdl != null) {
			return sdl.getFormat().getChannels();
		} else {
			return 2; // something arbitrary
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.AudioSink#getSampleRate()
	 */
	public double getSampleRate() {
		return sampleRate;
	}

	/**
	 * @return the name of the open device, or a generic name if not open
	 */
	public String getName() {
		if (isOpen()) {
			return devName;
		}
		return "JavaSoundSink";
	}
}
