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
                    midiPcmStream.method4847(var3, var4, var5);
                } else if (var2 == 144) {
                    if (var5 > 0) {
                        midiPcmStream.method4764(var3, var4, var5);
                    } else {
                        midiPcmStream.method4847(var3, var4, 64);
                    }

                } else if (var2 == 160) {
                    midiPcmStream.method4853(var3, var4, var5);
                } else if (var2 == 176) {
                    if (var4 == 0) {
                        midiPcmStream.field2937[var3] = (var5 << 14) + (midiPcmStream.field2937[var3] & -2080769);
                    }

                    if (var4 == 32) {
                        midiPcmStream.field2937[var3] = (var5 << 7) + (midiPcmStream.field2937[var3] & -16257);
                    }

                    if (var4 == 1) {
                        midiPcmStream.field2939[var3] = (var5 << 7) + (midiPcmStream.field2939[var3] & -16257);
                    }

                    if (var4 == 33) {
                        midiPcmStream.field2939[var3] = var5 + (midiPcmStream.field2939[var3] & -128);
                    }

                    if (var4 == 5) {
                        midiPcmStream.field2940[var3] = (var5 << 7) + (midiPcmStream.field2940[var3] & -16257);
                    }

                    if (var4 == 37) {
                        midiPcmStream.field2940[var3] = var5 + (midiPcmStream.field2940[var3] & -128);
                    }

                    if (var4 == 7) {
                        midiPcmStream.field2956[var3] = (var5 << 7) + (midiPcmStream.field2956[var3] & -16257);
                    }

                    if (var4 == 39) {
                        midiPcmStream.field2956[var3] = var5 + (midiPcmStream.field2956[var3] & -128);
                    }

                    if (var4 == 10) {
                        midiPcmStream.field2933[var3] = (var5 << 7) + (midiPcmStream.field2933[var3] & -16257);
                    }

                    if (var4 == 42) {
                        midiPcmStream.field2933[var3] = var5 + (midiPcmStream.field2933[var3] & -128);
                    }

                    if (var4 == 11) {
                        midiPcmStream.field2934[var3] = (var5 << 7) + (midiPcmStream.field2934[var3] & -16257);
                    }

                    if (var4 == 43) {
                        midiPcmStream.field2934[var3] = var5 + (midiPcmStream.field2934[var3] & -128);
                    }

                    int[] var10000;
                    if (var4 == 64) {
                        var10000 = midiPcmStream.field2944;
                        if (var5 >= 64) {
                            var10000[var3] |= 1;
                        } else {
                            var10000[var3] &= -2;
                        }
                    }

                    if (var4 == 65) {
                        if (var5 >= 64) {
                            var10000 = midiPcmStream.field2944;
                            var10000[var3] |= 2;
                        } else {
                            midiPcmStream.method4774(var3);
                            var10000 = midiPcmStream.field2944;
                            var10000[var3] &= -3;
                        }
                    }

                    if (var4 == 99) {
                        midiPcmStream.field2942[var3] = (var5 << 7) + (midiPcmStream.field2942[var3] & 127);
                    }

                    if (var4 == 98) {
                        midiPcmStream.field2942[var3] = (midiPcmStream.field2942[var3] & 16256) + var5;
                    }

                    if (var4 == 101) {
                        midiPcmStream.field2942[var3] = (var5 << 7) + (midiPcmStream.field2942[var3] & 127) + 16384;
                    }

                    if (var4 == 100) {
                        midiPcmStream.field2942[var3] = (midiPcmStream.field2942[var3] & 16256) + var5 + 16384;
                    }

                    if (var4 == 120) {
                        midiPcmStream.method4770(var3);
                    }

                    if (var4 == 121) {
                        midiPcmStream.method4771(var3);
                    }

                    if (var4 == 123) {
                        midiPcmStream.method4772(var3);
                    }

                    int var6;
                    if (var4 == 6) {
                        var6 = midiPcmStream.field2942[var3];
                        if (var6 == 16384) {
                            midiPcmStream.field2943[var3] = (var5 << 7) + (midiPcmStream.field2943[var3] & -16257);
                        }
                    }

                    if (var4 == 38) {
                        var6 = midiPcmStream.field2942[var3];
                        if (var6 == 16384) {
                            midiPcmStream.field2943[var3] = var5 + (midiPcmStream.field2943[var3] & -128);
                        }
                    }

                    if (var4 == 16) {
                        midiPcmStream.field2932[var3] = (var5 << 7) + (midiPcmStream.field2932[var3] & -16257);
                    }

                    if (var4 == 48) {
                        midiPcmStream.field2932[var3] = var5 + (midiPcmStream.field2932[var3] & -128);
                    }

                    if (var4 == 81) {
                        if (var5 >= 64) {
                            var10000 = midiPcmStream.field2944;
                            var10000[var3] |= 4;
                        } else {
                            midiPcmStream.method4775(var3);
                            var10000 = midiPcmStream.field2944;
                            var10000[var3] &= -5;
                        }
                    }

                    if (var4 == 17) {
                        midiPcmStream.method4777(var3, (var5 << 7) + (midiPcmStream.field2945[var3] & -16257));
                    }

                    if (var4 == 49) {
                        midiPcmStream.method4777(var3, var5 + (midiPcmStream.field2945[var3] & -128));
                    }

                } else if (var2 == 192) {
                    midiPcmStream.method4863(var3, var4 + midiPcmStream.field2937[var3]);
                } else if (var2 == 208) {
                    midiPcmStream.method4768(var3, var4);
                } else if (var2 == 224) {
                    midiPcmStream.method4769(var3, var4);
                } else {
                    if (var2 == 255) {
                        midiPcmStream.method4773();
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
