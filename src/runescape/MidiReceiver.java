package runescape;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

public class MidiReceiver implements Receiver {

    MidiPcmStream midiPcmStream;

    public MidiReceiver(MidiPcmStream currentMidiStream) {
        midiPcmStream = currentMidiStream;
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (message != null) {
            if (message instanceof ShortMessage) {
                ShortMessage shortMessage = (ShortMessage) message;
                int var2 = shortMessage.getCommand();
                int var3 = shortMessage.getChannel();
                int var4 = shortMessage.getData1();
                int var5 = shortMessage.getData2();
                if (var2 == 128) {
                    midiPcmStream.noteOff(var3, var4, var5);
                } else if (var2 == 144) {
                    if (var5 > 0) {
                        midiPcmStream.noteOn(var3, var4, var5);
                    } else {
                        midiPcmStream.noteOff(var3, var4, 64);
                    }

                } else if (var2 == 160) {
                    midiPcmStream.polyPressure(var3, var4, var5);
                } else if (var2 == 176) {
                    if (var4 == 0) {
                        midiPcmStream.bankControls[var3] = (var5 << 14) + (midiPcmStream.bankControls[var3] & -2080769);
                    }

                    if (var4 == 32) {
                        midiPcmStream.bankControls[var3] = (var5 << 7) + (midiPcmStream.bankControls[var3] & -16257);
                    }

                    if (var4 == 1) {
                        midiPcmStream.modulationControls[var3] = (var5 << 7) + (midiPcmStream.modulationControls[var3] & -16257);
                    }

                    if (var4 == 33) {
                        midiPcmStream.modulationControls[var3] = var5 + (midiPcmStream.modulationControls[var3] & -128);
                    }

                    if (var4 == 5) {
                        midiPcmStream.portamentoTimeControls[var3] = (var5 << 7) + (midiPcmStream.portamentoTimeControls[var3] & -16257);
                    }

                    if (var4 == 37) {
                        midiPcmStream.portamentoTimeControls[var3] = var5 + (midiPcmStream.portamentoTimeControls[var3] & -128);
                    }

                    if (var4 == 7) {
                        midiPcmStream.volumeControls[var3] = (var5 << 7) + (midiPcmStream.volumeControls[var3] & -16257);
                    }

                    if (var4 == 39) {
                        midiPcmStream.volumeControls[var3] = var5 + (midiPcmStream.volumeControls[var3] & -128);
                    }

                    if (var4 == 10) {
                        midiPcmStream.panControls[var3] = (var5 << 7) + (midiPcmStream.panControls[var3] & -16257);
                    }

                    if (var4 == 42) {
                        midiPcmStream.panControls[var3] = var5 + (midiPcmStream.panControls[var3] & -128);
                    }

                    if (var4 == 11) {
                        midiPcmStream.expressionControls[var3] = (var5 << 7) + (midiPcmStream.expressionControls[var3] & -16257);
                    }

                    if (var4 == 43) {
                        midiPcmStream.expressionControls[var3] = var5 + (midiPcmStream.expressionControls[var3] & -128);
                    }

                    int[] var10000;
                    if (var4 == 64) {
                        var10000 = midiPcmStream.switchControls;
                        if (var5 >= 64) {
                            var10000[var3] |= 1;
                        } else {
                            var10000[var3] &= -2;
                        }
                    }

                    if (var4 == 65) {
                        if (var5 >= 64) {
                            var10000 = midiPcmStream.switchControls;
                            var10000[var3] |= 2;
                        } else {
                            midiPcmStream.setReverb(var3);
                            var10000 = midiPcmStream.switchControls;
                            var10000[var3] &= -3;
                        }
                    }

                    if (var4 == 99) {
                        midiPcmStream.dataEntriesMSB[var3] = (var5 << 7) + (midiPcmStream.dataEntriesMSB[var3] & 127);
                    }

                    if (var4 == 98) {
                        midiPcmStream.dataEntriesMSB[var3] = (midiPcmStream.dataEntriesMSB[var3] & 16256) + var5;
                    }

                    if (var4 == 101) {
                        midiPcmStream.dataEntriesMSB[var3] = (var5 << 7) + (midiPcmStream.dataEntriesMSB[var3] & 127) + 16384;
                    }

                    if (var4 == 100) {
                        midiPcmStream.dataEntriesMSB[var3] = (midiPcmStream.dataEntriesMSB[var3] & 16256) + var5 + 16384;
                    }

                    if (var4 == 120) {
                        midiPcmStream.allSoundOff(var3);
                    }

                    if (var4 == 121) {
                        midiPcmStream.resetAllControllers(var3);
                    }

                    if (var4 == 123) {
                        midiPcmStream.allNotesOff(var3);
                    }

                    int var6;
                    if (var4 == 6) {
                        var6 = midiPcmStream.dataEntriesMSB[var3];
                        if (var6 == 16384) {
                            midiPcmStream.dataEntriesLSB[var3] = (var5 << 7) + (midiPcmStream.dataEntriesLSB[var3] & -16257);
                        }
                    }

                    if (var4 == 38) {
                        var6 = midiPcmStream.dataEntriesMSB[var3];
                        if (var6 == 16384) {
                            midiPcmStream.dataEntriesLSB[var3] = var5 + (midiPcmStream.dataEntriesLSB[var3] & -128);
                        }
                    }

                    if (var4 == 16) {
                        midiPcmStream.sampleLoopControls[var3] = (var5 << 7) + (midiPcmStream.sampleLoopControls[var3] & -16257);
                    }

                    if (var4 == 48) {
                        midiPcmStream.sampleLoopControls[var3] = var5 + (midiPcmStream.sampleLoopControls[var3] & -128);
                    }

                    if (var4 == 81) {
                        if (var5 >= 64) {
                            var10000 = midiPcmStream.switchControls;
                            var10000[var3] |= 4;
                        } else {
                            midiPcmStream.method4775(var3);
                            var10000 = midiPcmStream.switchControls;
                            var10000[var3] &= -5;
                        }
                    }

                    if (var4 == 17) {
                        midiPcmStream.retrigger(var3, (var5 << 7) + (midiPcmStream.retriggerControls[var3] & -16257));
                    }

                    if (var4 == 49) {
                        midiPcmStream.retrigger(var3, var5 + (midiPcmStream.retriggerControls[var3] & -128));
                    }

                } else if (var2 == 192) {
                    midiPcmStream.programChange(var3, var4 + midiPcmStream.bankControls[var3]);
                } else if (var2 == 208) {
                    midiPcmStream.channelPressure(var3, var4);
                } else if (var2 == 224) {
                    midiPcmStream.pitchBend(var3, (var4 + var5) * 128);
                } else {
                    if (var2 == 255) {
                        midiPcmStream.systemReset();
                    }
                }
            }
        }
        else {
            System.out.println("message null");
        }
    }

    @Override
    public void close() {
        if (midiPcmStream != null) {
            midiPcmStream = null;
        }
    }
}
