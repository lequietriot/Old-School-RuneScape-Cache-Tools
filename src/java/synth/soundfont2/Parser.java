/*
 * Soundfont 2 File Parser
 * based in small parts on WaveAudioFileReader.java of the tritonus
 * project: www.tritonus.org
 */

/*
 *  Copyright (c) 1999,2000 by Florian Bomers
 *  Copyright (c) 1999 by Matthias Pfisterer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package synth.soundfont2;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Class to parse a SoundFont 2 file.
 * 
 * @author florian
 * 
 */

public class Parser {

	public static boolean TRACE = false;
	public static boolean TRACE_INFO = false;
	public static boolean TRACE_RIFF = false;
	public static boolean TRACE_RIFF_MORE = false;
	public static boolean TRACE_PRESET = false;
	public static boolean TRACE_PROCESSOR = false;
	public static boolean TRACE_GENERATORS = false;
	public static boolean TRACE_MODULATORS = false;
	public static boolean TRACE_SAMPLELINKS = false;

	public static boolean DEBUG_SAMPLELINKS = false;

	private static final boolean ENFORCE_CONFORMANCE = false;

	public static final int FOURCC_RIFF = 0x52494646;
	public static final int FOURCC_sfbk = 0x7366626B;
	public static final int FOURCC_LIST = 0x4C495354;
	public static final int FOURCC_INFO = 0x494E464F;
	public static final int FOURCC_sdta = 0x73647461;
	public static final int FOURCC_pdta = 0x70647461;
	public static final int FOURCC_ifil = 0x6966696C;
	public static final int FOURCC_isng = 0x69736E67;
	public static final int FOURCC_INAM = 0x494E414D;
	public static final int FOURCC_irom = 0x69726F6D;
	public static final int FOURCC_iver = 0x69766572;
	public static final int FOURCC_ICRD = 0x49435244;
	public static final int FOURCC_IENG = 0x49454E47;
	public static final int FOURCC_IPRD = 0x49505244;
	public static final int FOURCC_ICOP = 0x49434F50;
	public static final int FOURCC_ICMT = 0x49434D54;
	public static final int FOURCC_ISFT = 0x49534654;
	public static final int FOURCC_smpl = 0x736D706C;
	public static final int FOURCC_phdr = 0x70686472;
	public static final int FOURCC_pbag = 0x70626167;
	public static final int FOURCC_pmod = 0x706D6F64;
	public static final int FOURCC_pgen = 0x7067656E;
	public static final int FOURCC_inst = 0x696E7374;
	public static final int FOURCC_ibag = 0x69626167;
	public static final int FOURCC_imod = 0x696D6F64;
	public static final int FOURCC_igen = 0x6967656E;
	public static final int FOURCC_shdr = 0x73686472;

	// special, internal FOURCC's:
	public static final int FOURCC_OUTERCHUNK = 0x00000000;
	public static final int FOURCC_IGNORED = 0x00000001;

	/**
	 * The stream to read from. It should be buffered and should not be accessed
	 * directly -- only through this classes' readXXX methods.
	 */
	private InputStream inputStream;

	/**
	 * The current read position in dis.
	 */
	private long readPos = 0;

	/**
	 * A temporary class used during reading to temporarily store all data as
	 * found in the file.
	 */
	private PresetTempData tempData;

	/**
	 * Meta data found in the soundfont
	 */
	private SoundFontInfo infoData = null;

	/**
	 * The actual audio samples.
	 */
	private SoundFontSampleData sampleData = null;

	/**
	 * The list of banks, containing the presets.
	 */
	private List<SoundFontBank> banks = null;

	/**
	 * Create a SoundFont 2 parser
	 */
	public Parser() {
	}

	/**
	 * @return Returns the SoundFont meta data (only valid after load).
	 */
	public SoundFontInfo getInfo() {
		return infoData;
	}

	/**
	 * @return Returns the presets in array of banks. Each bank contains an
	 *         array of 128 presets.
	 */
	public List<SoundFontBank> getPresetBanks() {
		return banks;
	}

	/**
	 * @return Returns the sampleData.
	 */
	public SoundFontSampleData getSampleData() {
		return sampleData;
	}

	/**
	 * Actually read the soundbank from the stream and parse it into the
	 * infoData, sampleData, and presetData fields.
	 * 
	 * @param in the input stream to read from -- preferably a buffered stream.
	 * @throws IOException on stream read error or premature end of stream
	 * @throws SoundFont2ParserException if the stream is not a well-structured
	 *             SoundFont 2 file.
	 */
	public void load(InputStream in) throws IOException,
			SoundFont2ParserException {
		infoData = null;
		sampleData = null;
		banks = null;
		this.inputStream = in;
		tempData = new PresetTempData();
		if (TRACE) System.out.println("Parsing...");
		readChunks(0xFFFFFFFFFFFFFFFL, FOURCC_OUTERCHUNK);
		in.close();
		if (TRACE) System.out.println("Processing...");
		tempData.process();
		tempData = null;
		if (TRACE) System.out.println("end parsing soundfont.");
	}

	/**
	 * Read a little endian 32-bit int from the stream. Advance readPos by 4.
	 * 
	 * @return the 32-bit integer value
	 * @throws IOException
	 */
	public int readIntLE() throws IOException {
		assert (scheduledSkip == 0);
		int b0 = inputStream.read();
		int b1 = inputStream.read();
		int b2 = inputStream.read();
		int b3 = inputStream.read();
		if ((b0 | b1 | b2 | b3) < 0) {
			throw new EOFException();
		}
		readPos += 4;
		return (b3 << 24) + (b2 << 16) + (b1 << 8) + (b0 << 0);
	}

	/**
	 * Read a signed little endian 16-bit short from the stream. Advance readPos
	 * by 2.
	 * 
	 * @return the 16-bit short value
	 * @throws IOException
	 */
	public short readShortLE() throws IOException {
		assert (scheduledSkip == 0);
		int b0 = inputStream.read();
		int b1 = inputStream.read();
		if ((b0 | b1) < 0) {
			throw new EOFException();
		}
		readPos += 2;
		return (short) ((b1 << 8) + (b0 << 0));
	}

	/**
	 * Read an unsigned little endian 16-bit short from the stream. Advance
	 * readPos by 2.
	 * 
	 * @return the 16-bit short value
	 * @throws IOException
	 */
	public int readWordLE() throws IOException {
		assert (scheduledSkip == 0);
		int b0 = inputStream.read();
		int b1 = inputStream.read();
		if ((b0 | b1) < 0) {
			throw new EOFException();
		}
		readPos += 2;
		return ((b1 << 8) + (b0 << 0));
	}

	/**
	 * Read a big endian 32-bit int from the stream. Advance readPos by 4.
	 * 
	 * @return the 32-bit integer value
	 * @throws IOException
	 */
	public int readIntBE() throws IOException {
		assert (scheduledSkip == 0);
		int b3 = inputStream.read();
		int b2 = inputStream.read();
		int b1 = inputStream.read();
		int b0 = inputStream.read();
		if ((b0 | b1 | b2 | b3) < 0) {
			throw new EOFException();
		}
		readPos += 4;
		return (b3 << 24) + (b2 << 16) + (b1 << 8) + (b0 << 0);
	}

	/**
	 * Read a big endian 16-bit short from the stream. Advance readPos by 2.
	 * 
	 * @return the 16-bit short value
	 * @throws IOException
	 */
	public short readShortBE() throws IOException {
		assert (scheduledSkip == 0);
		int b1 = inputStream.read();
		int b0 = inputStream.read();
		if ((b0 | b1) < 0) {
			throw new EOFException();
		}
		readPos += 2;
		return (short) ((b1 << 8) + (b0 << 0));
	}

	/**
	 * Read an unsigned Byte from the stream
	 */
	public int readUnsignedByte() throws IOException {
		int ret = inputStream.read();
		if (ret < 0) {
			throw new EOFException();
		}
		readPos++;
		return ret;
	}

	/**
	 * Read a signed Byte from the stream
	 */
	public byte readSignedByte() throws IOException {
		int ret = inputStream.read();
		if (ret < 0) {
			throw new EOFException();
		}
		readPos++;
		return (byte) ret;
	}

	public void readFully(byte bytes[]) throws IOException {
		readFully(bytes, 0, bytes.length);
	}

	public void readFully(byte bytes[], int offset, int length)
			throws IOException {
		readPos += length;
		while (length > 0) {
			int read = inputStream.read(bytes, offset, length);
			if (read > 0) {
				length -= read;
				offset += read;
			} else if (read == 0) {
				Thread.yield();
			} else {
				throw new EOFException();
			}
		}
	}

	public String readString(long chunkLength) throws IOException,
			SoundFont2ParserException {
		// sanity
		if (chunkLength > 100000) {
			throw new SoundFont2ParserException(
					"corrupt soundfont: string subchunk>100000 bytes");
		}
		byte[] bytes = new byte[(int) chunkLength];
		readFully(bytes);
		// find terminator
		while (chunkLength > 0 && bytes[(byte) chunkLength - 1] == 0) {
			chunkLength--;
		}
		return new String(bytes, 0, (byte) chunkLength);
	}

	public String readFixedLengthString(int length) throws IOException,
			SoundFont2ParserException {
		byte[] bytes = new byte[length];
		readFully(bytes);
		// find terminator
		for (int i = 0; i < length; i++) {
			if (bytes[i] == 0) {
				length = i;
				break;
			}
		}
		return new String(bytes, 0, length);
	}

	private long scheduledSkip = 0;

	protected void scheduleAdvanceChunk(long chunkStart, long chunkLength)
			throws IOException {
		long chunkRead = readPos - chunkStart;
		if (chunkLength > 0) {
			long add = ((chunkLength + 1) & 0xFFFFFFFE) - chunkRead;
			scheduledSkip += add;
			readPos += add;
		}
	}

	protected void commitAdvanceChunk() throws IOException {
		if (scheduledSkip > 0) {
			skip(scheduledSkip);
			// compensate readPos (because it was already increased in
			// scheduleAdvanceChunk)
			readPos -= scheduledSkip;
		}
		scheduledSkip = 0;
	}

	protected void skip(long bytes) throws IOException {
		while (bytes > 0) {
			long skipped = inputStream.skip(bytes);
			if (skipped > 0) {
				bytes -= skipped;
				readPos += skipped;
			}
		}
	}

	protected String key2string(int key) {
		if (key == FOURCC_IGNORED) {
			return "IGNORED_LIST";
		} else if (key == FOURCC_OUTERCHUNK) {
			return "OUTER";
		}
		byte[] bytes = new byte[4];
		bytes[3] = (byte) (key & 0xFF);
		bytes[2] = (byte) ((key & 0xFF00) >> 8);
		bytes[1] = (byte) ((key & 0xFF0000) >> 16);
		bytes[0] = (byte) ((key & 0xFF000000) >> 24);
		return new String(bytes);
	}

	/**
	 * Adds a preset to the ordered list of banks. If necessary, a new
	 * SoundFontBank object is created and added to the banks list.
	 * 
	 * @param preset the preset object to be added to the hierarchical
	 *            bank/preset list
	 * @param bank the numerical bank of the preset
	 * @param program the program number of this preset
	 */
	private void addPresetToBanks(SoundFontPreset preset, int bank, int program) {
		SoundFontBank bankObject = null;
		if (banks == null) {
			banks = new ArrayList<SoundFontBank>();
		} else {
			int i = SoundFontBank.findBank(banks, bank);
			if (i >= 0) {
				bankObject = banks.get(i);
			}
		}
		if (bankObject == null) {
			// need to create a new bank object
			bankObject = new SoundFontBank(bank);
			banks.add(bankObject);
			Collections.sort(banks);
		}
		bankObject.setPreset(program, preset);
	}

	private void requireChunk(int chunkID, int fourcc)
			throws SoundFont2ParserException {
		if (chunkID != fourcc) {
			throw new SoundFont2ParserException(
					"corrupt soundfont file: found chunk "
							+ key2string(chunkID) + " but expected "
							+ key2string(fourcc));
		}
	}

	/**
	 * 
	 * @throws SoundFont2ParserException
	 * @throws IOException
	 */
	private void readChunks(long outerChunkLength, int listChunk)
			throws SoundFont2ParserException, IOException {
		long endPos = readPos + outerChunkLength;
		while (readPos < endPos) {
			if (TRACE_RIFF_MORE) {
				System.out.println("readPos=0x" + Long.toHexString(readPos) + " endPos=0x"
						+ Long.toHexString(endPos));
			}
			// commit skipped bytes
			commitAdvanceChunk();
			long chunkLength;
			int thisChunk;
			try {
				thisChunk = readIntBE();
				chunkLength = readIntLE() & 0xFFFFFFFF; // unsigned
			} catch (IOException e) {
				// when we come here, we skipped past the end of the file
				throw new SoundFont2ParserException(
						"corrupt soundfont: premature end of file.", e);
			}
			long chunkStart = readPos;
			if (TRACE_RIFF)
				System.out.println("offset 0x"
						+ Long.toHexString(chunkStart - 8)
						+ "("
						+ key2string(listChunk)
						+ ")"
						+ ": chunk "
						+ key2string(thisChunk)
						+ " length "
						+ chunkLength
						+ " bytes (next chunk at 0x"
						+ Long.toHexString((chunkStart + chunkLength + 1) & 0xFFFFFFFE)
						+ ")");

			// depending on level, parse and read the different chunk types
			switch (listChunk) {
			case FOURCC_OUTERCHUNK:
				// special list type: for the outer level
				requireChunk(thisChunk, FOURCC_RIFF);
				int header = readIntBE();
				requireChunk(header, FOURCC_sfbk);
				readChunks(chunkLength - 4, header);
				break;
			case FOURCC_sfbk:
				// soundfonts only have LIST chunks on 1st level
				requireChunk(thisChunk, FOURCC_LIST);
				int listType = readIntBE();
				if (TRACE_RIFF) System.out.println("\t\tLIST type " + key2string(listType));
				switch (listType) {
				case FOURCC_INFO:
					if (infoData != null) {
						System.out.println("Ignored repeated INFO chunk");
					} else if (banks != null) {
						System.out.println("Ignored INFO chunk after pdta chunk");
					} else if (sampleData != null) {
						System.out.println("Ignored INFO chunk after sdta chunk");
					} else {
						readChunks(chunkLength - 4, listType);
						// consistency check
						if (infoData == null) {
							throw new SoundFont2ParserException(
									"corrupt soundfont: missing INFO data");
						}
						if (infoData.getVersionMajor() == 0
								&& infoData.getVersionMinor() == 0) {
							if (ENFORCE_CONFORMANCE) {
								throw new SoundFont2ParserException(
										"corrupt soundfont: missing version "
												+ "information (iver chunk)");
							}
							System.out.println(" -weak conformance: missing version, assuming 2.0");
							infoData.setVersion(2, 0);
						}
						// compatibility check
						if (infoData.getVersionMajor() != 2) {
							throw new SoundFont2ParserException(
									"incompatible soundfont (version "
											+ infoData.getVersionMajor() + "."
											+ infoData.getVersionMinor()
											+ "): only version 2.xx supported.");
						}
					}
					break;
				case FOURCC_sdta:
					if (infoData == null) {
						throw new SoundFont2ParserException(
								"illegal sdta chunk before INFO chunk");
					}
					if (banks != null) {
						System.out.println("Ignored sdta chunk after pdta chunk");
					} else if (sampleData != null) {
						System.out.println("Ignored repeated sdta chunk");
					} else {
						readChunks(chunkLength - 4, listType);
					}
					break;
				case FOURCC_pdta:
					if (sampleData == null) {
						throw new SoundFont2ParserException(
								"illegal pdta chunk before sdta chunk");
					}
					if (banks != null) {
						System.out.println("Ignored repeated pdta chunk");
					} else {
						readChunks(chunkLength - 4, listType);
						// consistency check
						if (pdtaChunksRead != 9) {
							throw new SoundFont2ParserException(
									"corrupt soundfont: pdta chunk does not "
											+ "contain the 9 mandatory sub-chunks");
						}
					}
					break;
				default:
					System.out.println("Ignored list chunk " + key2string(listType));
					// only for debugging
					readChunks(chunkLength - 4, FOURCC_IGNORED);
					break;
				}

				break;
			case FOURCC_INFO:
				readINFO(thisChunk, chunkLength);
				break;
			case FOURCC_sdta:
				readSampleData(thisChunk, chunkLength);
				break;
			case FOURCC_pdta:
				readPresetData(thisChunk, chunkLength);
				break;
			case FOURCC_IGNORED:
				// only for debugging purposes
				System.out.println("Ignored LIST element: " + key2string(thisChunk)
						+ " with size " + chunkLength + " bytes");
				break;
			default:
				break;
			}
			// don't advance immediately:
			// some RIFF files truncate the end of the last chunks.
			// if we'd try to skip to the end of the last chunk, an EOFException
			// would be thrown, although the file is not corrupt.
			scheduleAdvanceChunk(chunkStart, chunkLength);
			if (listChunk == FOURCC_OUTERCHUNK) {
				// the outermost chunk can only have one element
				break;
			}
		}
		// another consistency check
		if (readPos > endPos) {
			throw new SoundFont2ParserException(
					"corrupt soundfont: inner chunk is declared larger than fits "
							+ "into outer chunk");
		}
	}

	/**
	 * 
	 * @param chunkID
	 * @param chunkLength
	 * @param blockSize
	 * @param minNumBlocks
	 * @param maxNumBlocks
	 * @return the number of blocks in this chunk
	 * @throws SoundFont2ParserException
	 */
	private int checkSize(int chunkID, long chunkLength, int blockSize,
			int minNumBlocks, int maxNumBlocks)
			throws SoundFont2ParserException {
		long expectedMinSize = (long) blockSize * minNumBlocks;
		long expectedMaxSize = (long) blockSize * maxNumBlocks;
		int blockCount = (int) (chunkLength / blockSize);
		// first check that chunkLength is a multiple of blockSize
		if ((blockSize * blockCount) != chunkLength) {
			throw new SoundFont2ParserException("corrupt soundfont: chunk "
					+ key2string(chunkID) + "'s length of " + chunkLength
					+ " is not a multiple of the block size " + blockSize);
		}
		if (chunkLength < expectedMinSize) {
			throw new SoundFont2ParserException("corrupt soundfont: chunk "
					+ key2string(chunkID) + "'s length of " + chunkLength
					+ " is too small to accomodate at least " + minNumBlocks
					+ " blocks");
		}
		if (maxNumBlocks > minNumBlocks && chunkLength > expectedMaxSize) {
			throw new SoundFont2ParserException("corrupt soundfont: chunk "
					+ key2string(chunkID) + "'s length of " + chunkLength
					+ " is too large to accomodate at most " + maxNumBlocks
					+ " blocks");
		}
		return blockCount;
	}

	private void readINFO(int chunkID, long chunkLength) throws IOException,
			SoundFont2ParserException {
		if (infoData == null) {
			infoData = new SoundFontInfo();
		}
		switch (chunkID) {
		case FOURCC_ifil:
			checkSize(chunkID, chunkLength, 4, 1, 1);
			infoData.setVersion(readShortLE(), readShortLE());
			if (TRACE) {
				System.out.println(" read version " + infoData.getVersionMajor() + "."
						+ infoData.getVersionMinor());
			}
			break;

		case FOURCC_isng:
			infoData.setSoundEngine(readString(chunkLength));
			if (TRACE_INFO)
				System.out.println(" read sound engine: " + infoData.getSoundEngine());
			break;

		case FOURCC_INAM:
			infoData.setName(readString(chunkLength));
			if (TRACE) System.out.println(" read soundfont name: " + infoData.getName());
			break;

		case FOURCC_irom:
			infoData.setRomName(readString(chunkLength));
			if (TRACE_INFO) System.out.println(" read ROM name: " + infoData.getRomName());
			break;

		case FOURCC_iver:
			checkSize(chunkID, chunkLength, 4, 1, 1);
			infoData.setROMVersion(readShortLE(), readShortLE());
			if (TRACE_INFO) {
				System.out.println(" read ROM version " + infoData.getROMVersionMajor()
						+ "." + infoData.getROMVersionMinor());
			}
			break;

		case FOURCC_ICRD:
			infoData.setCreationDate(readString(chunkLength));
			if (TRACE_INFO)
				System.out.println(" read creation date name: " + infoData.getCreationDate());
			break;

		case FOURCC_IENG:
			infoData.setEngineer(readString(chunkLength));
			if (TRACE_INFO)
				System.out.println(" read engineer name: " + infoData.getEngineer());
			break;

		case FOURCC_IPRD:
			infoData.setProduct(readString(chunkLength));
			if (TRACE_INFO)
				System.out.println(" read product name: " + infoData.getProduct());
			break;

		case FOURCC_ICOP:
			infoData.setCopyright(readString(chunkLength));
			if (TRACE_INFO)
				System.out.println(" read copyright: " + infoData.getCopyright());
			break;

		case FOURCC_ICMT:
			infoData.setComment(readString(chunkLength));
			if (TRACE_INFO) System.out.println(" read comment: " + infoData.getComment());
			break;

		case FOURCC_ISFT:
			infoData.setSoftware(readString(chunkLength));
			if (TRACE_INFO)
				System.out.println(" read software package: " + infoData.getSoftware());
			break;
		default:
			System.out.println(" ignored INFO element " + key2string(chunkID)
					+ " with size " + chunkLength + " bytes");
		}

	}

	private void readSampleData(int chunkID, long chunkLength)
			throws IOException, SoundFont2ParserException {
		if (sampleData == null) {
			sampleData = new SoundFontSampleData();
		}
		switch (chunkID) {
		case FOURCC_smpl:
			// check consistency
			if (sampleData.getData() != null) {
				throw new SoundFont2ParserException(
						"corrupt soundfont: multiple smpl chunk");
			}
			checkSize(chunkID, chunkLength, 2, 1, -1);
			// read data
			byte[] data = new byte[(int) chunkLength];
			readFully(data);
			sampleData.setData(data);
			if (TRACE) System.out.println(" read " + chunkLength + " bytes of audio data");
			break;
		default:
			System.out.println("ignored sdta element " + key2string(chunkID) + " with size "
					+ chunkLength + " bytes");
		}
	}

	private int pdtaChunksRead = 0;

	private void checkPresetDataOrder(int order, int chunkID)
			throws SoundFont2ParserException {
		if (pdtaChunksRead != order) {
			throw new SoundFont2ParserException(
					"corrupt soundfont: multiple or out of order "
							+ key2string(chunkID) + " chunk");
		}
		pdtaChunksRead++;
	}

	private void readPresetData(int chunkID, long chunkLength)
			throws IOException, SoundFont2ParserException {
		int blockCount;

		switch (chunkID) {
		case FOURCC_phdr:
			checkPresetDataOrder(0, chunkID);
			blockCount = checkSize(chunkID, chunkLength, 38, 2, -1);
			tempData.presetData = new SoundFontPreset[blockCount - 1];
			tempData.readPresetHeader(blockCount);
			break;

		case FOURCC_pbag:
			checkPresetDataOrder(1, chunkID);
			blockCount = checkSize(chunkID, chunkLength, 4, 2, -1);
			tempData.readPresetZones(blockCount);
			break;

		case FOURCC_pmod:
			checkPresetDataOrder(2, chunkID);
			blockCount = checkSize(chunkID, chunkLength, 10, 1, -1);
			tempData.readPresetModulators(blockCount);
			break;

		case FOURCC_pgen:
			checkPresetDataOrder(3, chunkID);
			blockCount = checkSize(chunkID, chunkLength, 4, 1, -1);
			tempData.readPresetGenerators(blockCount);
			break;

		case FOURCC_inst:
			checkPresetDataOrder(4, chunkID);
			blockCount = checkSize(chunkID, chunkLength, 22, 2, -1);
			tempData.readInstruments(blockCount);
			break;

		case FOURCC_ibag:
			checkPresetDataOrder(5, chunkID);
			blockCount = checkSize(chunkID, chunkLength, 4, 2, -1);
			tempData.readInstZones(blockCount);
			break;

		case FOURCC_imod:
			checkPresetDataOrder(6, chunkID);
			blockCount = checkSize(chunkID, chunkLength, 10, 1, -1);
			tempData.readInstModulators(blockCount);
			break;

		case FOURCC_igen:
			checkPresetDataOrder(7, chunkID);
			blockCount = checkSize(chunkID, chunkLength, 4, 1, -1);
			tempData.readInstGenerators(blockCount);
			break;

		case FOURCC_shdr:
			checkPresetDataOrder(8, chunkID);
			blockCount = checkSize(chunkID, chunkLength, 46, 1, -1);
			tempData.readSampleHeaders(blockCount);
			break;

		default:
			System.out.println("ignored pdta element " + key2string(chunkID) + " with size "
					+ chunkLength + " bytes");
		}
	}

	public static class SoundFont2ParserException extends Exception {
		public static final long serialVersionUID = 0;

		public SoundFont2ParserException(String msg) {
			super(msg);
		}

		public SoundFont2ParserException(String msg, Throwable t) {
			super(msg, t);
		}
	}

	private class PresetTempData {
		/**
		 * Array of all presets.
		 */
		private SoundFontPreset[] presetData;

		private SoundFontInstrument[] instData;
		private SoundFontSample[] samples;

		private int[] presetZoneIndexes;
		private int[] presetGeneratorIndexes;
		private int[] presetModulatorIndexes;
		private SoundFontModulator[] presetModulators;
		private SoundFontGenerator[] presetGenerators;

		private int[] instZoneIndexes;
		private int[] instGeneratorIndexes;
		private int[] instModulatorIndexes;
		private SoundFontModulator[] instModulators;
		private SoundFontGenerator[] instGenerators;

		private SoundFontModulator readModulator() throws IOException {
			int srcOp = readWordLE();
			int dstOp = readWordLE();
			short amount = readShortLE();
			int srcAmount = readWordLE();
			int transform = readWordLE();
			return new SoundFontModulator(srcOp, dstOp, amount, srcAmount,
					transform);
		}

		private SoundFontGenerator readGenerator() throws IOException {
			int op = readWordLE();
			short amount = readShortLE();
			return new SoundFontGenerator(op, amount);
		}

		private SoundFontSample readSampleHeader() throws IOException,
				SoundFont2ParserException {
			String name = readFixedLengthString(20);
			int start = readIntLE();
			int end = readIntLE();
			int startLoop = readIntLE();
			int endLoop = readIntLE();
			double sampleRate = (double) readIntLE();
			int originalPitch = readUnsignedByte();
			int pitchCorrection = readSignedByte();
			int sampleLink = readWordLE();
			int sampleType = readWordLE();

			return new SoundFontSample(name, start, end, startLoop, endLoop,
					sampleRate, originalPitch, pitchCorrection, sampleLink,
					sampleType);
		}

		private void readPresetHeader(int blockCount) throws IOException,
				SoundFont2ParserException {
			presetZoneIndexes = new int[blockCount];
			for (int i = 0; i < blockCount; i++) {
				String name = readFixedLengthString(20);
				int preset = readWordLE();
				int bank = readWordLE();
				int presetZoneIndex = readWordLE();
				skip(12);

				if (i > 0) {
					// check consistency. Using "<" allows the same index to be
					// repeated (for what it's worth). Creative Lab's 8M
					// soundbank has duplicated zone indexes.
					if (presetZoneIndex < presetZoneIndexes[i - 1]) {
						throw new SoundFont2ParserException(
								"corrupt soundfont: presetZoneIndex is not monotonic. Current:"
										+ presetZoneIndex + ", last:"
										+ presetZoneIndexes[i - 1]);
					}
				}

				presetZoneIndexes[i] = presetZoneIndex;
				if (i < blockCount - 1) {
					SoundFontPreset presetObject = new SoundFontPreset(name,
							preset, bank);
					presetData[i] = presetObject;
					addPresetToBanks(presetObject, bank, preset);
					if (TRACE_PRESET) {
						System.out.println(" Read preset " + i + ": " + name + " program="
								+ preset + " bank=" + bank);
					}
				}
			}
			if (!TRACE_PRESET && TRACE) {
				System.out.println(" read " + (blockCount - 1) + " preset definitions.");
			}
		}

		private void readPresetZones(int blockCount) throws IOException,
				SoundFont2ParserException {
			// check consistency: pbag must equal the last presetZoneIndex
			// blocks
			if (blockCount - 1 != presetZoneIndexes[presetZoneIndexes.length - 1]) {
				throw new SoundFont2ParserException(
						"corrupt soundfont: pbag must accomodate exactly the last "
								+ "presetZoneIndex+1 records");
			}
			presetGeneratorIndexes = new int[blockCount];
			presetModulatorIndexes = new int[blockCount];
			for (int i = 0; i < blockCount; i++) {
				presetGeneratorIndexes[i] = readWordLE();
				presetModulatorIndexes[i] = readWordLE();
			}
			if (TRACE_PRESET) {
				System.out.println(" read " + (blockCount - 1)
						+ " preset generator indexes and modulator indexes");
			}
		}

		private void readPresetModulators(int blockCount) throws IOException,
				SoundFont2ParserException {
			// check consistency: pmod must equal the last presetModulatorIndex
			// blocks
			if (blockCount - 1 != presetModulatorIndexes[presetModulatorIndexes.length - 1]) {
				throw new SoundFont2ParserException(
						"corrupt soundfont: pmod must accomodate exactly the last "
								+ "presetModulatorIndex+1 records");
			}
			presetModulators = new SoundFontModulator[blockCount - 1];
			for (int i = 0; i < blockCount - 1; i++) {
				presetModulators[i] = readModulator();
			}
			// skip the terminating modulator
			skip(10);
			if (TRACE_PRESET) {
				System.out.println(" read " + (blockCount - 1) + " preset modulators");
			}
		}

		private void readPresetGenerators(int blockCount) throws IOException,
				SoundFont2ParserException {
			// check consistency: pgen must equal the last presetGeneratorIndex
			// blocks
			if (blockCount - 1 != presetGeneratorIndexes[presetGeneratorIndexes.length - 1]) {
				throw new SoundFont2ParserException(
						"corrupt soundfont: pgen must accomodate exactly the last "
								+ "presetGeneratorIndex+1 records");
			}
			presetGenerators = new SoundFontGenerator[blockCount - 1];
			for (int i = 0; i < blockCount - 1; i++) {
				presetGenerators[i] = readGenerator();
			}
			// skip the terminating generator
			skip(4);
			if (TRACE_PRESET) {
				System.out.println(" read " + (blockCount - 1) + " preset generators");
			}
		}

		private void readInstruments(int blockCount) throws IOException,
				SoundFont2ParserException {
			instData = new SoundFontInstrument[blockCount - 1];
			instZoneIndexes = new int[blockCount];
			for (int i = 0; i < blockCount; i++) {
				String name = readFixedLengthString(20);
				int instZoneIndex = readWordLE();
				if (i > 0) {
					// check consistency
					if (instZoneIndex <= instZoneIndexes[i - 1]) {
						throw new SoundFont2ParserException(
								"corrupt soundfont: instZoneIndex is not monotonic");
					}
				}

				instZoneIndexes[i] = instZoneIndex;
				if (i < blockCount - 1) {
					instData[i] = new SoundFontInstrument(name);
					if (TRACE_PRESET) {
						System.out.println(" read inst " + i + ": " + name);
					}
				}
			}
			if (!TRACE_PRESET && TRACE) {
				System.out.println(" read " + (blockCount - 1) + " instruments.");
			}
		}

		private void readInstZones(int blockCount) throws IOException,
				SoundFont2ParserException {
			// check consistency: ibag must equal the last instZoneIndex blocks
			if (blockCount - 1 != instZoneIndexes[instZoneIndexes.length - 1]) {
				throw new SoundFont2ParserException(
						"corrupt soundfont: ibag must accomodate exactly the last "
								+ "instZoneIndex+1 records");
			}
			instGeneratorIndexes = new int[blockCount];
			instModulatorIndexes = new int[blockCount];
			for (int i = 0; i < blockCount; i++) {
				instGeneratorIndexes[i] = readWordLE();
				instModulatorIndexes[i] = readWordLE();
			}
			if (TRACE_PRESET) {
				System.out.println(" read " + (blockCount - 1)
						+ " instrument generator indexes and modulator indexes");
			}
		}

		private void readInstModulators(int blockCount) throws IOException,
				SoundFont2ParserException {
			// check consistency: imod must equal the last instModulatorIndex
			// blocks
			if (blockCount - 1 != instModulatorIndexes[instModulatorIndexes.length - 1]) {
				throw new SoundFont2ParserException(
						"corrupt soundfont: imod must accomodate exactly the last "
								+ "instModulatorIndex+1 records");
			}
			instModulators = new SoundFontModulator[blockCount - 1];
			for (int i = 0; i < blockCount - 1; i++) {
				instModulators[i] = readModulator();
			}
			// skip the terminating modulator
			skip(10);
			if (TRACE_PRESET) {
				System.out.println(" read " + (blockCount - 1) + " instrument modulators");
			}
		}

		private void readInstGenerators(int blockCount) throws IOException,
				SoundFont2ParserException {
			// check consistency: igen must equal the last instGeneratorIndex
			// blocks
			if (blockCount - 1 != instGeneratorIndexes[instGeneratorIndexes.length - 1]) {
				throw new SoundFont2ParserException(
						"corrupt soundfont: igen must accomodate exactly the last "
								+ "instGeneratorIndex+1 records");
			}
			instGenerators = new SoundFontGenerator[blockCount - 1];
			for (int i = 0; i < blockCount - 1; i++) {
				instGenerators[i] = readGenerator();
			}
			// skip the terminating generator
			skip(4);
			if (TRACE_PRESET) {
				System.out.println(" read " + (blockCount - 1) + " instrument generators");
			}
		}

		private void readSampleHeaders(int blockCount) throws IOException,
				SoundFont2ParserException {
			samples = new SoundFontSample[blockCount - 1];
			SoundFontSample sample;
			for (int i = 0; i < blockCount - 1; i++) {
				sample = readSampleHeader();
				samples[i] = sample;
				if (TRACE_PRESET) System.out.println(" read " + i + ": " + sample);
				if ((sample.getSampleType() & (SoundFontSample.LINKED_SAMPLE | SoundFontSample.ROM_SAMPLE_FLAG)) != 0) {
					throw new SoundFont2ParserException(
							"unsupported soundfont: linked samples and ROM samples not supported");
				}
				if (!sample.isConsistent(sampleData.getSampleCount(),
						samples.length)) {
					throw new SoundFont2ParserException("corrupt soundfont: "
							+ sample.getName() + " not consistent");
				}
			}
			// skip the terminating generator
			skip(46);
			if (!TRACE_PRESET && TRACE) {
				System.out.println(" read " + (blockCount - 1) + " sample headers");
			}
		}

		/**
		 * Parse all temporary data and fill the presets with the corresponding
		 * classes.
		 */
		private void process() throws SoundFont2ParserException {
			if (TRACE) System.out.println(" |creating preset zones...");
			// IDEA: remove the global zones by flattening their generators and
			// modulators into the zones' generators and modulators arrays. This
			// will remove the need for the extended effort to prevent
			// application of a relative global preset generator *and* a local
			// preset generator. Also it can serve to remove the eventually
			// useless range and sampleID/instID generators.
			for (int i = 0; i < presetData.length; i++) {
				int zoneStart = presetZoneIndexes[i];
				int nextZone = presetZoneIndexes[i + 1];
				SoundFontPresetZone[] zones = new SoundFontPresetZone[nextZone
						- zoneStart];
				if (TRACE_PROCESSOR) {
					System.out.println("  preset " + i + ": " + presetData[i] + " with "
							+ zones.length + " zones.");
				}
				for (int z = zoneStart; z < nextZone; z++) {
					// generators in this zone...
					int genStart = presetGeneratorIndexes[z];
					int nextGen = presetGeneratorIndexes[z + 1];
					SoundFontGenerator[] gens = new SoundFontGenerator[nextGen
							- genStart];
					for (int ii = genStart; ii < nextGen; ii++) {
						gens[ii - genStart] = presetGenerators[ii];
					}
					SoundFontInstrument inst = null;
					if (gens.length > 0) {
						SoundFontGenerator lastGen = gens[nextGen - genStart
								- 1];
						if (lastGen.getOp() == SoundFontGenerator.INSTRUMENT) {
							// found an instrument definition for this zone
							inst = instData[lastGen.getAmount()];
						}
					}
					// modulators in this zone...
					int modStart = presetModulatorIndexes[z];
					int nextMod = presetModulatorIndexes[z + 1];
					SoundFontModulator[] mods = new SoundFontModulator[nextMod
							- modStart];
					for (int ii = modStart; ii < nextMod; ii++) {
						mods[ii - modStart] = presetModulators[ii];
					}
					zones[z - zoneStart] = new SoundFontPresetZone(gens, mods,
							inst);
					if (TRACE_PROCESSOR) {
						System.out.println("   preset zone " + z + ": "
								+ zones[z - zoneStart]);
						if (TRACE_GENERATORS) {
							for (int g = 0; g < gens.length; g++) {
								System.out.println("    -" + gens[g]);
							}
						}
						if (TRACE_MODULATORS) {
							for (int m = 0; m < mods.length; m++) {
								System.out.println("    -" + mods[m]);
							}
						}
					}
				}
				presetData[i].setZones(zones);
			}

			if (TRACE) System.out.println(" |creating instrument zones...");
			for (int i = 0; i < instData.length; i++) {
				int zoneStart = instZoneIndexes[i];
				int nextZone = instZoneIndexes[i + 1];
				SoundFontInstrumentZone[] zones = new SoundFontInstrumentZone[nextZone
						- zoneStart];
				if (TRACE_PROCESSOR) {
					System.out.println("  inst " + i + ": " + zones.length + " zones ("
							+ instData[i].getName() + ").");
				}
				for (int z = zoneStart; z < nextZone; z++) {
					// generators in this zone...
					int genStart = instGeneratorIndexes[z];
					int nextGen = instGeneratorIndexes[z + 1];
					SoundFontGenerator[] gens = new SoundFontGenerator[nextGen
							- genStart];
					for (int ii = genStart; ii < nextGen; ii++) {
						gens[ii - genStart] = instGenerators[ii];
					}
					SoundFontSample sample = null;
					if (gens.length > 0) {
						SoundFontGenerator lastGen = gens[nextGen - genStart
								- 1];
						if (lastGen.getOp() == SoundFontGenerator.SAMPLE_ID) {
							// found a sample definition for this zone
							sample = samples[lastGen.getAmount()];
						}
					}

					// modulators in this zone...
					int modStart = instModulatorIndexes[z];
					int nextMod = instModulatorIndexes[z + 1];
					SoundFontModulator[] mods = new SoundFontModulator[nextMod
							- modStart];
					for (int ii = modStart; ii < nextMod; ii++) {
						mods[ii - modStart] = instModulators[ii];
					}
					zones[z - zoneStart] = new SoundFontInstrumentZone(gens,
							mods, sample);
					if (TRACE_PROCESSOR) {
						System.out.println("   inst zone " + z + ": " + zones[z - zoneStart]);
						if (TRACE_GENERATORS) {
							for (int g = 0; g < gens.length; g++) {
								System.out.println("    -" + gens[g]);
							}
						}
						if (TRACE_MODULATORS) {
							for (int m = 0; m < mods.length; m++) {
								System.out.println("    -" + mods[m]);
							}
						}
					}
					if (sample == null && (z > zoneStart)) {
						// disable this zone
						zones[z - zoneStart].makeInaccessible();
						if (TRACE_PROCESSOR) {
							System.out.println("##inconsistent soundfont: inst " + i
									+ " zone " + (z - zoneStart)
									+ " does not provide a sample");
						}
					}
				}
				instData[i].setZones(zones);
			}
			if (TRACE) System.out.println(" |linking stereo samples...");
			for (int i = 0; i < instData.length; i++) {
				SoundFontInstrument inst = instData[i];
				SoundFontInstrumentZone[] zones = inst.getZones();
				for (int z = 0; z < zones.length; z++) {
					SoundFontInstrumentZone zone = zones[z];
					SoundFontSample sample = zone.getSample();
					if (zone.isValid()) {
						boolean mustGetLinked = ((sample.getSampleType() == SoundFontSample.LEFT_SAMPLE) || (sample.getSampleType() == SoundFontSample.RIGHT_SAMPLE))
								&& (zone.getZoneLink() == null);
						if ((sample.getSampleLinkIndex() >= 0)
								&& (sample.getSampleLinkIndex() < samples.length)
								&& mustGetLinked) {
							SoundFontSample sampleLink = samples[sample.getSampleLinkIndex()];
							int sampleLinkType;
							if (sample.getSampleType() == SoundFontSample.LEFT_SAMPLE) {
								sampleLinkType = SoundFontSample.RIGHT_SAMPLE;
							} else {
								sampleLinkType = SoundFontSample.LEFT_SAMPLE;
							}

							// search a following zone with the same key/vel
							// range
							for (int zz = z + 1; zz < zones.length; zz++) {
								SoundFontInstrumentZone zone2 = zones[zz];
								if (zone2.matchesKeyRegion(zone)
										&& (zone2.getSample() == sampleLink)
										&& (zone2.getSample().getSampleType() == sampleLinkType)) {
									// we found the corresponding zone!
									// the right sample is the master zone
									if (sample.getSampleType() == SoundFontSample.RIGHT_SAMPLE) {
										if (TRACE_SAMPLELINKS) {
											System.out.println(" -inst " + i + ", zone " + z
													+ ": making it master: "
													+ zone);
											System.out.println("                     slave zone "
													+ zz + ": " + zone2);
										}
										zone.setMasterZone(zone2);
									} else {
										if (TRACE_SAMPLELINKS) {
											System.out.println(" -inst " + i + ", zone "
													+ zz
													+ ": making it master: "
													+ zone2);
											System.out.println("                     slave zone "
													+ z + ": " + zone);
										}
										zone2.setMasterZone(zone);
									}
									break;
								} else {
									if (DEBUG_SAMPLELINKS) {
										System.out.println("  ## inst " + i
												+ " could not match zone " + zz
												+ ": ");
										if (!zone2.matchesKeyRegion(zone)) {
											System.out.println("   ## key region does not match");
										}
										if (zone2.getSample() != sampleLink) {
											System.out.println("   ## samples don't match: sample1 = "
													+ sampleLink);
											System.out.println("                          sample2 = "
													+ zone2.getSample());
										}
										if (zone2.getSample().getSampleType() != sampleLinkType) {
											System.out.println("   ## wrong sample type: expected="
													+ SoundFontSample.sampleType2string(sampleLinkType)
													+ " actual="
													+ SoundFontSample.sampleType2string(zone2.getSample().getSampleType()));
										}
									}
								}
							}
						}
						// check if successful
						if ((zone.getZoneLink() == null) && mustGetLinked) {
							if (TRACE_PROCESSOR) {
								System.out.println("## inconsistent soundfont: inst zone does not provide a linked sample");
								System.out.println("##   instrument " + i + ", zone " + z
										+ ": " + zone);
								System.out.println("##   " + zone.getSample());
							}
							zone.makeInaccessible();
						}
					}
				}
			}
		}

	}

}
