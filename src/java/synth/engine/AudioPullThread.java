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

import java.util.ArrayList;
import java.util.List;

import static synth.utils.AudioUtils.*;


/**
 * The AudioPullThread reads data from an AudioInput and writes it to an
 * AudioSink. By way of the implemented AudioClock interface, you can query the
 * start time of the currently filled audio buffer -- this is in steps of the
 * buffer size of the audio sink.
 * 
 * @author florian
 */

public class AudioPullThread implements Runnable, AudioClock {

	public static boolean DEBUG_PULLTHREAD = false;

	/**
	 * The priority of the pull thread, on a scale 0...28
	 */
	public static final int PULLTHREAD_PRIORITY = 28;

	/**
	 * Flag to choose if READ buffers should be recycled for the next slice.
	 */
	public static boolean REUSE_AUDIO_BUFFERS = true;

	/**
	 * Flag to choose the read method of the AudioInput: if <code>true</code>,
	 * type 1 is used, otherwise type 2.
	 * 
	 * @see AudioInput
	 */
	public static boolean DEBUG_USE_FUNCTIONAL_READ = false;

	// default 1 millisecond slicetime
	private final static double DEFAULT_SLICE_TIME = 0.001;
	private volatile AudioInput input;

	private AudioSink sink;

	/**
	 * A secondary sink that receives everything that the master sink gets.
	 */
	private AudioSink slaveSink;

	private List<AudioRendererListener> listeners = new ArrayList<AudioRendererListener>();

	private double sliceTime = DEFAULT_SLICE_TIME;

	/**
	 * flag to notify the write thread to stop execution
	 */
	private volatile boolean stopped;

	/**
	 * flag that is true while the thread is running
	 */
	private volatile boolean inThread = false;

	/**
	 * The thread for the actual writing
	 */
	private Object runner;

	/**
	 * The number of samples already written to the audio sink.
	 * 
	 * @see #getAudioTime()
	 */
	private long nextBufferSamples;

	/**
	 * Keep track of the number of resynchronizations
	 */
	private long resynchCounter = 0;

	public AudioPullThread() {
		// nothing
	}

	public AudioPullThread(AudioInput input, AudioSink sink) {
		setInput(input);
		setSink(sink);
	}

	public void addListener(AudioRendererListener L) {
		listeners.add(L);
	}

	public void removeListener(AudioRendererListener L) {
		listeners.remove(L);
	}

	/**
	 * @return Returns the input.
	 */
	public synchronized AudioInput getInput() {
		return input;
	}

	/**
	 * @param input The input to set.
	 */
	public synchronized void setInput(AudioInput input) {
		this.input = input;
	}

	/**
	 * @return Returns the sink.
	 */
	public synchronized AudioSink getSink() {
		return sink;
	}

	/**
	 * @param sink The sink to set.
	 */
	public synchronized void setSink(AudioSink sink) {
		this.sink = sink;
	}

	/**
	 * @return Returns the second sink.
	 */
	public synchronized AudioSink getSlaveSink() {
		return slaveSink;
	}

	/**
	 * @param sink The second sink to set.
	 */
	public synchronized void setSlaveSink(AudioSink sink) {
		this.slaveSink = sink;
	}

	/**
	 * @param seconds the new slice time in seconds
	 */
	public void setSliceTime(double seconds) {
		this.sliceTime = seconds;
	}

	/**
	 * @return the current slice time in seconds
	 */
	public double getSliceTime() {
		return sliceTime;
	}

	/**
	 * @param sliceTimeMillis Set the time in milliseconds
	 */
	public void setSliceTimeMillis(double sliceTimeMillis) {
		setSliceTime(sliceTimeMillis / 1000.0);
	}

	/**
	 * @return Returns the sliceTime in milliseconds
	 */
	public double getSliceTimeMillis() {
		return getSliceTime() * 1000.0;
	}

	/**
	 * align the buffer size to an integral number of slices and slice sample
	 * count must be a multiple of 4
	 */
	public int getSliceTimeSamples(double sampleRate) {
		return (int) seconds2samples(sliceTime, sampleRate);
	}

	/**
	 * Get the preferred buffer size for the given buffer size. This method may
	 * adjust the slice time for very small buffer sizes, so it is good practice
	 * to always call setSliceTime() before calling this method.
	 * 
	 * @param bufferSizeMillis the requested buffer size in milliseconds
	 * @param sampleRate the sample rate used
	 * @return the buffer size in samples that should be used for the audio
	 *         buffer
	 */
	public int getPreferredSinkBufferSizeSamples(double bufferSizeMillis,
			double sampleRate) {
		double bufferSizeSecs = (double) (bufferSizeMillis / 1000.0);
		int bufferSamples = (int) seconds2samples(bufferSizeSecs, sampleRate);
		if (bufferSizeSecs < sliceTime * 2) {
			// set the slice time to the buffer size for very small buffers
			setSliceTime(samples2seconds(bufferSamples, sampleRate));
			return bufferSamples;
		}
		return alignSinkBufferSizeToSliceTime(bufferSamples,
				getSliceTimeSamples(sampleRate));
	}

	private static int alignSinkBufferSizeToSliceTime(int bufferSampleCount,
			int sliceTimeSamples) {
		if (sliceTimeSamples > bufferSampleCount) {
			return sliceTimeSamples;
		}
		return (bufferSampleCount / sliceTimeSamples) * sliceTimeSamples;
	}

	/**
	 * @return the resynchCounter
	 */
	public long getResynchCounter() {
		return resynchCounter;
	}

	/**
	 * @param resynchCounter the resynchCounter to set
	 */
	public void setResynchCounter(long resynchCounter) {
		this.resynchCounter = resynchCounter;
	}

	public void start() {
		runner = ThreadFactory.createThread(this, getClass().getSimpleName()
				+ " mixing thread", PULLTHREAD_PRIORITY);
	}

	public void stop() {
		if (!stopped) {
			stopped = true;
			if (runner != null) {
				try {
					while (inThread && stopped) {
						Thread.sleep(1);
					}
				} catch (InterruptedException ie) {
				}
			}
		}
	}

	/**
	 * This method will return the start time of the next buffer that is written
	 * to the audio sink. Because buffers are always written with the size of
	 * the audio sink's buffer, the returned time is not continuos, but jumps in
	 * steps of the sink's buffer size. Since this is the start time of the next
	 * buffer, the returned time is usually a future reference.
	 * 
	 * @return the next buffer's time
	 * @see AudioClock#getAudioTime()
	 */
	public final AudioTime getAudioTime() {
		if (sink == null) {
			return new AudioTime(0);
		}
		return new AudioTime(nextBufferSamples, sink.getSampleRate());
	}

	/**
	 * If the sink's time is not aligning with the calculated time, need to
	 * change the sink's time offset. This should typically only happen upon
	 * buffer underruns, because this thread is aligned to the sink's time by
	 * way of the blocking write() method.
	 * 
	 * @param samples
	 * @param bufferSampleCount
	 * @param reset
	 */
	private final void synchronizeSink(long samples, int bufferSampleCount,
			boolean reset) {
		long sinkSamplesTime = sink.getAudioTime().getSamplesTime(
				sink.getSampleRate());
		if (reset) {
			long adjust = samples - sinkSamplesTime;
			if (adjust != 0) {
				AudioTime oldOffset = sink.getTimeOffset();
				AudioTime adjustTime = new AudioTime(adjust,
						sink.getSampleRate());
				sink.setTimeOffset(oldOffset.add(adjustTime));

				if (DEBUG_PULLTHREAD) {
					System.out.println("AudioPullThread: synchronization reset: sink is "
							+ (samples - sinkSamplesTime)
							+ " samples off, adjusting its time offset by "
							+ adjust + " samples. OldOffset="
							+ oldOffset.getMillisTime() + "ms, new Offset="
							+ sink.getTimeOffset().getMillisTime() + "ms");
				}
			}
		}
		// If an adjustment is necessary, only adjust by 1/16 of the buffer size
		// to gradually adjust to the right adjustment amount (in case
		// of unprecise measurement. These numbers are found by trial and error
		// and seem to work well
		if (samples - sinkSamplesTime > bufferSampleCount) {
			// by default, adjust by 1/16 of a buffer
			long adjust = bufferSampleCount / 16;
			if ((samples - sinkSamplesTime - bufferSampleCount) > bufferSampleCount / 2) {
				// if there is a large offset (more than half a buffer),
				// adjust to the full amount
				adjust = (samples - sinkSamplesTime - bufferSampleCount);
			}
			AudioTime oldOffset = sink.getTimeOffset();
			if (adjust != 0) {
				if (!reset) resynchCounter++;
				AudioTime adjustTime = new AudioTime(adjust,
						sink.getSampleRate());
				sink.setTimeOffset(oldOffset.add(adjustTime));

				if (DEBUG_PULLTHREAD) {
					System.out.println("AudioPullThread: sink is lagging "
							+ (samples - sinkSamplesTime - bufferSampleCount)
							+ " samples, adjusting its time offset by "
							+ adjust + " samples. OldOffset="
							+ oldOffset.getMillisTime() + "ms, new Offset="
							+ sink.getTimeOffset().getMillisTime() + "ms");
				}
			}
		} else if (samples - sinkSamplesTime < 0) {
			// by default, adjust by 1/16 of a buffer
			long adjust = bufferSampleCount / 16;
			if ((sinkSamplesTime - samples) > bufferSampleCount / 2) {
				// if there is a large offset (more than half a buffer),
				// adjust to the full amount
				adjust = (sinkSamplesTime - samples);
			}
			if (adjust != 0) {
				if (!reset) resynchCounter++;
				AudioTime adjustTime = new AudioTime(adjust,
						sink.getSampleRate());
				if (DEBUG_PULLTHREAD) {
					System.out.println("AudioPullThread: sink is "
							+ (sinkSamplesTime - samples)
							+ " samples ahead, adjusting it by " + adjust
							+ " samples.");
				}
				sink.setTimeOffset(sink.getTimeOffset().subtract(adjustTime));
			}
		}
	}

	private void waitForSinkToBeOpen() {
		// use 0 values to signal that no synchronizeSink() is wished
		waitForSinkToBeOpen(0, 0);
	}

	private void waitForSinkToBeOpen(long samples, int bufferSampleCount) {
		// wait for sink to be ready
		while (sink == null || !sink.isOpen()) {
			if (DEBUG_PULLTHREAD) {
				System.out.println("AudioPullThread: wait for soundsink to be opened...");
			}
			try {
				Thread.sleep(5);
			} catch (InterruptedException ie) {
			}
			if (stopped) {
				break;
			}
			if (sink != null && sink.isOpen()) {
				if (DEBUG_PULLTHREAD) {
					System.out.println("AudioPullThread: soundsink is ready to be written to.");
				}
				if (bufferSampleCount > 0) {
					synchronizeSink(samples, bufferSampleCount, true);
				}
			}
		}
	}

	/**
	 * The actual loop of reading from the AudioInput and writing it to the
	 * soundcard
	 */
	public void run() {
		if (DEBUG_PULLTHREAD) {
			System.out.println("AudioPullThread: in soundcard writing thread");
		}
		inThread = true;
		try {

			stopped = false;
			waitForSinkToBeOpen();

			double sampleRate = sink.getSampleRate();
			// force re-setup of buffer
			double localSliceTime = -1.0;
			int localBufferSampleCount = -1;
			int bufferSampleCount = 0;
			int sliceSampleCount = 0;
			AudioBuffer buffer = null;
			AudioTime duration = null;
			int offset = 0;
			long samples = 0;
			int synchronizeCounter = 0;
			while (!stopped) {
				try {
					// need to re-setup temp buffer?
					// TODO: some global way to signal a change of parameter
					// (also for, e.g. stopped flag)
					if (localSliceTime != sliceTime
							|| localBufferSampleCount != sink.getBufferSize()) {
						// set up the temporary buffer for reading
						localBufferSampleCount = sink.getBufferSize();
						localSliceTime = sliceTime;

						sliceSampleCount = getSliceTimeSamples(sampleRate);
						if (sliceSampleCount > localBufferSampleCount) {
							sliceSampleCount = localBufferSampleCount;
						}

						bufferSampleCount = alignSinkBufferSizeToSliceTime(
								localBufferSampleCount, sliceSampleCount);
						if (DEBUG_PULLTHREAD) {
							System.out.println("AudioPullThread: Slice size: "
									+ ((Math.round(localSliceTime * 10000.0)) / 10.0)
									+ "ms = "
									+ sliceSampleCount
									+ " samples"
									+ ", buffer size: "
									+ ((Math.round(samples2nanos(
											bufferSampleCount, sampleRate) / 100000.0)) / 10.0)
									+ "ms = " + bufferSampleCount + " samples"
									+ ", sink buffer size = "
									+ sink.getBufferSize() + " samples.");
						}
						buffer = new AudioBuffer(sink.getChannels(),
								bufferSampleCount, sampleRate);
						duration = new AudioTime(sliceSampleCount, sampleRate);
						synchronizeSink(samples, bufferSampleCount, true);
						if (offset > localBufferSampleCount) {
							// adapt offset if in the middle of filling a buffer
							offset = localBufferSampleCount - sliceSampleCount;
						}
					}

					AudioTime time = new AudioTime(samples, sampleRate);

					for (AudioRendererListener arl : listeners) {
						arl.newAudioSlice(time, duration);
					}

					// wait for input to become ready
					while (input == null) {
						if (DEBUG_PULLTHREAD) {
							System.out.println("AudioPullThread: wait for input to be opened...");
						}
						try {
							Thread.sleep(10);
						} catch (InterruptedException ie) {
						}
						if (stopped) {
							break;
						}
						if (DEBUG_PULLTHREAD) {
							if (input != null) {
								System.out.println("AudioPullThread: input is ready to be read from.");
							}
						}
					}
					if (stopped) {
						break;
					}
					// read audio data from input
					if (!DEBUG_USE_FUNCTIONAL_READ) {
						// this call will take some time
						input.read(time, buffer, offset, sliceSampleCount);
					} else {
						AudioBuffer thisBuffer = input.read(time,
								sliceSampleCount, buffer.getChannelCount(),
								sampleRate);
						// if we're doing synchronous read/write, just exchange
						// the buffer
						if (sliceSampleCount == bufferSampleCount) {
							buffer = thisBuffer;
						} else {
							thisBuffer.copyTo(buffer, offset, sliceSampleCount);
						}
					}

					samples += sliceSampleCount;
					offset += sliceSampleCount;
					if (offset >= bufferSampleCount) {
						// wait for sink to be ready
						if (sink == null || !sink.isOpen()) {
							waitForSinkToBeOpen(samples, bufferSampleCount);
						}
						// write the audio data
						if (!stopped) {

							// this is a blocking call
							sink.write(buffer);

							// since we always write a full buffer to the sink,
							// we can assume that the sink's time must be
							// somewhere at the beginning of the last buffer's
							// time. If not, we'll need to moderately adjust the
							// next buffer's time synchronize every 16th time
							if ((++synchronizeCounter & 15) == 0) {
								synchronizeSink(samples, bufferSampleCount,
										false);
							}
							if (slaveSink != null) {
								slaveSink.write(buffer);
							}
							offset = 0;
							if (REUSE_AUDIO_BUFFERS) {
								if (!DEBUG_USE_FUNCTIONAL_READ) {
									// functional read will always overwrite
									buffer.makeSilence();
								}
							} else {
								buffer = new AudioBuffer(
										buffer.getChannelCount(),
										bufferSampleCount, sampleRate);
							}
							nextBufferSamples = samples;
						}
					}
				} catch (Throwable t) {
					System.out.println(String.valueOf(t));
				}
			}
		} finally {
			inThread = false;
		}
		if (DEBUG_PULLTHREAD) {
			System.out.println("AudioPullThread: quit soundcard writing thread");
		}
	}

}
