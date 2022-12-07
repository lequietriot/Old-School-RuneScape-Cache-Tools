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

import synth.engine.*;
import synth.utils.AudioUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Soundbank that reads from Creative Lab's SoundFont 2 files.
 * 
 * @author florian
 * 
 */

public class SoundFontSoundbank implements Soundbank {

	public static boolean TRACE_SB2SB = false;

	/**
	 * The audio sample data
	 */
	private SoundFontSampleData sampleData;

	/**
	 * The SoundFont's info data
	 */
	private SoundFontInfo info;

	/**
	 * The hierarchical Preset list, grouped in banks
	 */
	private List<SoundFontBank> banks;

	/**
	 * Create a new instance of SoundFont2Soundbank by parsing the specified
	 * file.
	 * 
	 * @param inputFile
	 * @throws IOException
	 * @throws Parser.SoundFont2ParserException
	 */
	public SoundFontSoundbank(File inputFile) throws IOException,
			Parser.SoundFont2ParserException {
		Parser parser = new Parser();
		parser.load(new FileInputStream(inputFile));
		sampleData = parser.getSampleData();
		info = parser.getInfo();
		banks = parser.getPresetBanks();
	}
	
	/**
	 * @param sampleData
	 * @param info
	 * @param banks
	 */
	public SoundFontSoundbank(SoundFontSampleData sampleData,
			SoundFontInfo info, List<SoundFontBank> banks) {
		super();
		this.sampleData = sampleData;
		this.info = info;
		this.banks = banks;
	}


	public String getName() {
		return info.getName();
	}

	public List<Bank> getBanks() {
		return new ArrayList<Bank>(banks);
	}

	public NoteInput createNoteInput(Synthesizer.Params params, AudioTime time,
			MidiChannel channel, int note, int vel) {
		if (TRACE_SB2SB) {
			System.out.println("createNoteInput: look for sample for note=" + note
					+ ", vel=" + vel);
		}
		NoteInput result = null;

		// first find a fitting bank
		int index = SoundFontBank.findBank(banks, channel.getBank());
		if (index >= 0) {
			SoundFontBank bank = banks.get(index);
			SoundFontPreset preset = bank.getPreset(channel.getProgram());
			if (preset != null) {
				// we found a preset! now find a fitting zone
				if (TRACE_SB2SB) {
					System.out.println("-matching preset: " + preset);
				}
				List<SoundFontPresetZone> pZones = preset.getZones(note, vel);
				if (pZones != null) {
					for (SoundFontPresetZone pZone : pZones) {
						if (TRACE_SB2SB) {
							System.out.println(" -matching preset zone: " + pZone);
						}
						SoundFontPresetZone pZoneGlobal =
								preset.getGlobalZone();
						if (TRACE_SB2SB) {
							if (pZoneGlobal != null) {
								System.out.println(" -matching global preset zone: "
										+ pZoneGlobal);
							}
						}

						SoundFontInstrument inst = pZone.getInstrument();
						if (TRACE_SB2SB) {
							System.out.println("  -matching inst: " + inst);
						}
						List<SoundFontInstrumentZone> iZones =
								inst.getZones(note, vel);
						if (iZones != null) {
							for (SoundFontInstrumentZone iZone : iZones) {
								if (TRACE_SB2SB) {
									System.out.println("   -matching inst Zone: " + iZone);
								}
								NoteInput ni =
										createNoteInput(params, time, channel,
												note, vel, preset, pZone,
												pZoneGlobal, inst, iZone);
								result = addNoteInputToResult(result, ni);
								if (ni != null) {
									// do we need to set up a linked sample?
									SoundFontInstrumentZone iZoneLink =
											iZone.getZoneLink();
									if (iZoneLink != null) {
										if (TRACE_SB2SB) {
											System.out.println("    -creating phase-locked NoteInput with "
													+ "inst Zone: " + iZoneLink);
										}
										NoteInput linkedNI =
												createNoteInput(params, time,
														channel, note, vel,
														preset, pZone,
														pZoneGlobal, inst,
														iZoneLink);
										result =
												addNoteInputToResult(result,
														linkedNI);
									}
								}
							}
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * If there are several instruments in a preset, or several waves in an
	 * instrument, or phase-locked samples, the new NoteInput will be added to
	 * the result by way of the linkedNoteInput field in NoteInput.
	 * 
	 * @param prevResult the result variable (or null, if none created yet)
	 * @param newNoteInput the new NoteInput instance
	 * @return the result NoteInput instance
	 */
	private final NoteInput addNoteInputToResult(NoteInput prevResult,
			NoteInput newNoteInput) {
		if (newNoteInput != null) {
			if (prevResult == null) {
				prevResult = newNoteInput;
			} else {
				NoteInput thisNoteInput = prevResult;
				while (thisNoteInput.getLinkedNoteInput() != null
						&& thisNoteInput.getLinkedNoteInput() != prevResult) {
					thisNoteInput = thisNoteInput.getLinkedNoteInput();
				}
				thisNoteInput.setLinkedNoteInput(newNoteInput);
				if (TRACE_SB2SB) {
					System.out.println("   -added linked NoteInput " + newNoteInput);
				}
			}
		}
		return prevResult;
	}

	private final NoteInput createNoteInput(Synthesizer.Params params,
			AudioTime time, MidiChannel channel, int note, int vel,
			SoundFontPreset preset, SoundFontPresetZone pZone,
			SoundFontPresetZone pZoneGlobal, SoundFontInstrument inst,
			SoundFontInstrumentZone iZone) {

		SoundFontInstrumentZone iZoneGlobal = inst.getGlobalZone();
		if (TRACE_SB2SB) {
			if (iZoneGlobal != null) {
				System.out.println("     -matching global inst Zone: " + iZoneGlobal);
			}
		}

		SoundFontSample sample = iZone.getSample();
		if (TRACE_SB2SB) {
			System.out.println("     -matching sample: " + sample);
		}
		SoundFontPatch patch =
				new SoundFontPatch(note, vel, channel.getBank(),
						channel.getProgram(), sample);
		SoundFontArticulation art =
				new SoundFontArticulation(time, patch, channel);
		art.setName(preset.getName() + "." + inst.getName());
		SoundFontOscillator osc = new SoundFontOscillator(sample, sampleData);

		// GENERATORS

		// the generators may change the values in Patch, also the note!

		// first parse the instrument zone for absolute values
		if (iZoneGlobal != null) {
			// don't need to specify a forbidden list, since the global values
			// will be overriden anyway by the local zone
			iZoneGlobal.parseZone(false, patch, art, osc, null);
		}
		iZone.parseZone(false, patch, art, osc, null);

		// then parse the preset zone for relative values
		if (pZoneGlobal != null) {
			pZoneGlobal.parseZone(true, patch, art, osc, pZone);
		}
		pZone.parseZone(true, patch, art, osc, null);

		// MODULATORS

		// use "patch.getNote()", because the generators may change the note in Patch.
		note = patch.getNote();
		vel = patch.getVelocity();
		// first parse the instrument zone for absolute values
		if (iZoneGlobal != null) {
			iZoneGlobal.parseModulators(note, vel, channel, art, iZone);
		}
		iZone.parseModulators(note, vel, channel, art, null);

		// then parse the preset zone's modulators
		if (pZoneGlobal != null) {
			pZoneGlobal.parseModulators(note, vel, channel, art, pZone);
		}
		pZone.parseModulators(note, vel, channel, art, null);
		
		// eventually, execute the default modulators (if not overriden by 
		// zone modulators)
		executeDefaultModulators(note, vel, channel, art);

		// add sample's pitch correction
		int pitchCorrection = sample.getPitchCorrection();
		if (pitchCorrection != 0) {
			art.setInitialPitchOffset(art.getInitialPitchOffset()
					+ (pitchCorrection / 100.0));
		}

		// initialize the oscillator
		osc.init();

		return new NoteInput(params, time, channel, patch, osc, art, note, vel);
	}

	private final void executeDefaultModulators(int note, int vel, 
			MidiChannel channel, SoundFontArticulation art) {

		// 8.4.1: velocity to attenuation: negative concave unipolar transform
		double velDB =
				SoundFontUtils.transform(vel / 127.0, true,
						true, false)
						* -96.0;
		double linear = AudioUtils.decibel2linear(velDB);
		art.addLinearInitialAttenuation(linear);
		if (TRACE_SB2SB) {
			System.out.println("      -velocity " + vel
					+ " is mapped to attenuation=" + (velDB) + "dB");
		}
		
		// 8.4.2: key number to filter cutoff
		// this modulator only works for velocity >=64
		
		// sounds bad if only for velocities > 64, so enable for all velocities
		// TODO: what to do, leave it in state?
		//if (vel >= 64) {
		//if (false) {
			int offset = -2400 * (127 - note) / 128;
			art.getLowPassFilter().addCutoffCents(offset);
			if (TRACE_SB2SB) {
				System.out.println("      -key is mapped to filter cutoff: " + offset
						+ " cents");
			}
		//}
		
		// 8.4.3: channel pressure to LFO pitch depth: in SoundFontArticulation
		
		// 8.4.4: mod wheel to LFO pitch depth: in SoundFontArticulation
		
		
	}
}
