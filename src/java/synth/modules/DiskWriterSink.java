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

import synth.engine.AudioBuffer;
import synth.engine.AudioSink;
import synth.engine.AudioTime;

import javax.sound.sampled.AudioFormat;
import java.io.*;

/**
 * An AudioSink implementation that writes its output to a file on disk.
 * 
 * @author florian
 */

public class DiskWriterSink implements AudioSink {

	private static boolean DEBUG_WAVESINK = false;

	/**
	 * The file to write
	 */
	private OutputStream output;

	private AudioFormat format;

	private File file;

	/**
	 * Support for the AdjustableAudioTime interface
	 */
	private AudioTime timeOffset;

	/**
	 * A temporary byte buffer for conversion to the native format
	 */
	private byte[] byteBuffer;

	/**
	 * A buffer for the WAVE header
	 */
	private byte[] header = {
			/* 0 */0x52, 0x49, 0x46, 0x46, // RIFF
			/* 4 */0x00, 0x00, 0x00, 0x00, // RIFF Size
			/* 8 */0x57, 0x41, 0x56, 0x45, // WAVE
			/* 12 */0x66, 0x6D, 0x74, 0x20, // "fmt "
			/* 16 */0x12, 0x00, 0x00, 0x00, // fmt size
			/* 20 */0x01, 0x00, // format code
			/* 22 */0x00, 0x00, // channels
			/* 24 */0x00, 0x00, 0x00, 0x00, // samples per second
			/* 28 */0x00, 0x00, 0x00, 0x00, // avg bytes per second
			/* 32 */0x00, 0x00, // block align
			/* 34 */0x00, 0x00, // bits per sample
			/* 36 */0x00, 0x00, // extra data size
			/* 38 */0x64, 0x61, 0x74, 0x61, // "data"
			/* 42 */0x00, 0x00, 0x00, 0x00
	// data chunk size
			};

	/**
	 * how many bytes written to the file
	 */
	private long writtenBytes;

	private boolean open;

	/**
	 * Constructor for this sink (empty)
	 */
	public DiskWriterSink() {
	}

	/**
	 * Open the file with the specified format
	 */
	public synchronized void open(File file, AudioFormat format)
			throws Exception {
		if (output != null) {
			close();
		}
		output = new FileOutputStream(file);
		this.format = format;
		if (!format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)
				&& !format.getEncoding().equals(
						AudioFormat.Encoding.PCM_UNSIGNED)) {
			throw new Exception("Unsupported format for wave writing: "
					+ format);
		}
		this.file = file;
		writtenBytes = 0;
		patchHeader();
		output.write(header);
		open = true;
		if (DEBUG_WAVESINK) {
			System.out.println("DiskWriterSink: opened output file " + file);
		}
	}

	public synchronized void close() {
		try {
			if (output != null) {
				output.close();
				output = null;
				// patch header
				patchHeader();
				RandomAccessFile ra = new RandomAccessFile(file, "rw");
				try {
					ra.write(header);
					if (DEBUG_WAVESINK) {
						System.out.println("DiskWriterSink: patched WAVE header");
					}
				} finally {
					ra.close();
				}
			}
		} catch (IOException ioe) {
			System.out.println(String.valueOf(ioe));
		}
		open = false;
		if (DEBUG_WAVESINK) {
			System.out.println("DiskWriterSink: closed output file, wrote "
					+ (writtenBytes / format.getFrameSize()) + " samples");
		}
	}

	public boolean isOpen() {
		return open;
	}

	public AudioFormat getFormat() {
		return format;
	}

	private void setHeaderField32(int offset, int val) {
		header[offset++] = (byte) (val & 0xFF);
		header[offset++] = (byte) ((val >> 8) & 0xFF);
		header[offset++] = (byte) ((val >> 16) & 0xFF);
		header[offset] = (byte) ((val >> 24) & 0xFF);
	}

	private void setHeaderField16(int offset, short val) {
		header[offset++] = (byte) (val & 0xFF);
		header[offset] = (byte) ((val >> 8) & 0xFF);
	}

	private void patchHeader() {
		if (writtenBytes == 0) {
			// size not known
			setHeaderField32(4, -1);
			setHeaderField32(42, -1);
		} else {
			setHeaderField32(4, (int) writtenBytes + header.length - 8);
			setHeaderField32(42, (int) writtenBytes);
		}
		setHeaderField16(22, (short) format.getChannels());
		setHeaderField32(24, (int) format.getSampleRate());
		setHeaderField32(28, ((int) format.getSampleRate())
				* format.getFrameSize());
		setHeaderField16(32, (short) format.getFrameSize());
		setHeaderField16(34, (short) format.getSampleSizeInBits());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.AudioSink#write(synth.engine.AudioBuffer)
	 */
	public synchronized void write(AudioBuffer buffer) {
		// set up the temporary buffer that receives the converted
		// samples in bytes
		if (output != null) {
			int requiredSize = buffer.getByteArrayBufferSize(getFormat());
			if (byteBuffer == null || byteBuffer.length < requiredSize) {
				byteBuffer = new byte[requiredSize];
			}
			buffer.convertToByteArray(byteBuffer, 0, getFormat());
			try {
				output.write(byteBuffer, 0, requiredSize);
				writtenBytes += requiredSize;
				if (DEBUG_WAVESINK) {
					System.out.println("WaveSink: Wrote "+requiredSize+" bytes -> "+(requiredSize / getFormat().getFrameSize())+" samples");
				}
			} catch (IOException ioe) {
				System.out.println(String.valueOf(ioe));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.AudioClock#getClockTime()
	 */
	public AudioTime getAudioTime() {
		AudioTime ret;
		if (getFormat() == null) {
			ret = new AudioTime(0);
		} else {
			ret = new AudioTime(writtenBytes / getFormat().getFrameSize(),
							getFormat().getSampleRate());
			if (timeOffset != null) {
				return ret.add(timeOffset);
			}
		}
		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.AdjustableAudioClock#getTimeOffset()
	 */
	public AudioTime getTimeOffset() {
		if (timeOffset == null) {
			return new AudioTime(0);
		}
		return timeOffset;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.AdjustableAudioClock#setTimeOffset(synth.engine.AudioTime)
	 */
	public void setTimeOffset(AudioTime offset) {
		if (offset.getNanoTime() == 0) {
			timeOffset = null;
		} else {
			timeOffset = offset;
		}
	}

	/*
	 * (non-Javadoc) @return the buffer size of this sink in samples
	 * 
	 * @see synth.engine.AudioSink#getBufferSize()
	 */
	public int getBufferSize() {
		return 100; // something arbitrary
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.AudioSink#getChannels()
	 */
	public int getChannels() {
		return getFormat().getChannels();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see synth.engine.AudioSink#getSampleRate()
	 */
	public double getSampleRate() {
		return (double) getFormat().getSampleRate();
	}
}
