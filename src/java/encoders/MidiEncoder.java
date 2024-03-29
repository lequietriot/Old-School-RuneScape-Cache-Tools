package encoders;

import com.application.GUI;
import osrs.ByteBufferUtils;

import javax.sound.midi.*;
import javax.swing.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MidiEncoder {

    private static final int NOTE_OFF = 0x80;
    private static final int NOTE_ON = 0x90;
    private static final int KEY_AFTER_TOUCH = 0xA0;
    private static final int CONTROL_CHANGE = 0xB0;
    private static final int PROGRAM_CHANGE = 0xC0;
    private static final int CHANNEL_AFTER_TOUCH = 0xD0;
    private static final int PITCH_WHEEL_CHANGE = 0xE0;

    private static final int END_OF_TRACK = 0x2F;
    private static final int SET_TEMPO = 0x51;

    private final GUI gui;

    public MidiEncoder(GUI selectedGUI) {
        gui = selectedGUI;
        JFileChooser chooseMidi = new JFileChooser();
        chooseMidi.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooseMidi.setMultiSelectionEnabled(true);
        if (chooseMidi.showOpenDialog(gui) == JFileChooser.APPROVE_OPTION) {
            File[] files = chooseMidi.getSelectedFiles();
            for (File selected : files) {
                if (selected.getName().endsWith(".mid")) {
                    encode(selected);
                }
            }
        }
    }

    private void encode(File selected) {
        try {
            Sequence sequence = MidiSystem.getSequence(selected);
            File outputFilePath = new File(gui.cacheLibrary.getPath() + File.separator + "Encoded Data" + File.separator + "MIDI");
            boolean madeDirectory = outputFilePath.mkdirs();
            if (madeDirectory) {
                JOptionPane.showMessageDialog(gui.getContentPane(), selected.getName() + " was encoded successfully.\nIt can be found in the newly created path: " + outputFilePath.getPath());
            }
            else {
                JOptionPane.showMessageDialog(gui.getContentPane(), selected.getName() + " was encoded successfully.\nIt can be found in the following path: " + outputFilePath.getPath());
            }
            File outputFile = new File(outputFilePath + File.separator + selected.getName().replace(".mid", ".dat").trim());
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(outputFile));

            for (Track track : sequence.getTracks()) {
                int prevChannel = 0;
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        int ch = (sm.getChannel() ^ prevChannel) << 4;
                        switch (sm.getCommand()) {
                            case NOTE_OFF:
                                dos.write(1 | ch);
                                prevChannel = sm.getChannel();
                                break;
                            case NOTE_ON:
                                dos.write(ch);
                                prevChannel = sm.getChannel();
                                break;
                            case KEY_AFTER_TOUCH:
                                dos.write(5 | ch);
                                prevChannel = sm.getChannel();
                                break;
                            case CONTROL_CHANGE:
                                dos.write(2 | ch);
                                prevChannel = sm.getChannel();
                                break;
                            case PROGRAM_CHANGE:
                                dos.write(6 | ch);
                                prevChannel = sm.getChannel();
                                break;
                            case CHANNEL_AFTER_TOUCH:
                                dos.write(4 | ch);
                                prevChannel = sm.getChannel();
                                break;
                            case PITCH_WHEEL_CHANGE:
                                dos.write(3 | ch);
                                prevChannel = sm.getChannel();
                                break;
                        }
                    } else if (message instanceof MetaMessage) {
                        MetaMessage mm = (MetaMessage) message;
                        switch (mm.getType()) {
                            case END_OF_TRACK:
                                dos.write(7);
                                break;
                            case SET_TEMPO:
                                dos.write(23);
                                break;
                            default:
                                //OTHER META EVENTS ARE IGNORED
                                break;
                        }

                    }

                }

            }

            //write event timestamp for used opcodes
            for (Track track : sequence.getTracks()) {
                int lastTick = 0;
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        switch (sm.getCommand()) {
                            case NOTE_OFF:
                            case NOTE_ON:
                            case KEY_AFTER_TOUCH:
                            case CONTROL_CHANGE:
                            case PROGRAM_CHANGE:
                            case CHANNEL_AFTER_TOUCH:
                            case PITCH_WHEEL_CHANGE:
                                ByteBufferUtils.putVarIntDos(dos, (int) event.getTick() - lastTick);
                                lastTick = (int) event.getTick();
                                break;
                        }
                    } else if (message instanceof MetaMessage) {
                        MetaMessage mm = (MetaMessage) message;
                        switch (mm.getType()) {
                            case END_OF_TRACK:
                            case SET_TEMPO:
                                ByteBufferUtils.putVarIntDos(dos, (int) event.getTick() - lastTick);
                                lastTick = (int) event.getTick();
                                break;
                        }
                    }
                }
            }

            //jagex works with offset from the last one because this is usually 0 and gives better compression rates
            int lastController = 0;
            int lastNote = 0;
            int lastNoteOnVelocity = 0;
            int lastNoteOffVelocity = 0;
            int lastWheelChangeT = 0;
            int lastWheelChangeB = 0;
            int lastChannelAfterTouch = 0;
            int lastKeyAfterTouchVelocity = 0;

            //write controller number changes
            int[] lastControllerValue = new int[128];
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == CONTROL_CHANGE) {
                            dos.write(sm.getData1() - lastController);
                            lastController = sm.getData1();
                        }
                    }
                }
            }

            //controller 64 65 120 121 123 changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == CONTROL_CHANGE && (sm.getData1() == 64 || sm.getData1() == 65 || sm.getData1() == 120 || sm.getData1() == 121 || sm.getData1() == 123)) {
                            dos.write(sm.getData2() - lastControllerValue[sm.getData1()]);
                            lastControllerValue[sm.getData1()] = sm.getData2();
                        }
                    }
                }
            }
            //key after touch velocity changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == KEY_AFTER_TOUCH) {
                            dos.write(sm.getData2() - lastKeyAfterTouchVelocity);
                            lastKeyAfterTouchVelocity = sm.getData2();
                        }
                    }
                }
            }
            //channel after touch channel changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == CHANNEL_AFTER_TOUCH) {
                            dos.write(sm.getData1() - lastChannelAfterTouch);
                            lastChannelAfterTouch = sm.getData1();
                        }
                    }
                }
            }
            //pitch bend top values
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == PITCH_WHEEL_CHANGE) {
                            dos.write(sm.getData2() - lastWheelChangeT);
                            lastWheelChangeT = sm.getData2();
                        }
                    }
                }
            }
            //controller 1 changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == CONTROL_CHANGE && sm.getData1() == 1) {
                            dos.write(sm.getData2() - lastControllerValue[sm.getData1()]);
                            lastControllerValue[sm.getData1()] = sm.getData2();
                        }
                    }
                }
            }
            //controller 7 changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == CONTROL_CHANGE && sm.getData1() == 7) {
                            dos.write(sm.getData2() - lastControllerValue[sm.getData1()]);
                            lastControllerValue[sm.getData1()] = sm.getData2();
                        }
                    }
                }
            }
            //controller 10 changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == CONTROL_CHANGE && sm.getData1() == 10) {
                            dos.write(sm.getData2() - lastControllerValue[sm.getData1()]);
                            lastControllerValue[sm.getData1()] = sm.getData2();
                        }
                    }
                }
            }
            //note changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == NOTE_OFF || sm.getCommand() == NOTE_ON || sm.getCommand() == KEY_AFTER_TOUCH) {
                            dos.write(sm.getData1() - lastNote);
                            lastNote = sm.getData1();
                        }
                    }
                }
            }
            //note on velocity changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == NOTE_ON) {
                            dos.write(sm.getData2() - lastNoteOnVelocity);
                            lastNoteOnVelocity = sm.getData2();
                        }
                    }
                }
            }
            //all unlisted controller changes (controllers are probably grouped like this because it gives an even better compression)
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == CONTROL_CHANGE && !(sm.getData1() == 64 || sm.getData1() == 65 || sm.getData1() == 120 || sm.getData1() == 121 || sm.getData1() == 123 || sm.getData1() == 0 || sm.getData1() == 32 || sm.getData1() == 1 || sm.getData1() == 33 || sm.getData1() == 7 || sm.getData1() == 39 || sm.getData1() == 10 || sm.getData1() == 42 || sm.getData1() == 99 || sm.getData1() == 98 || sm.getData1() == 101 || sm.getData1() == 100)) {
                            dos.write(sm.getData2() - lastControllerValue[sm.getData1()]);
                            lastControllerValue[sm.getData1()] = sm.getData2();
                        }
                    }
                }
            }
            //note off velocity changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == NOTE_OFF) {
                            dos.write(sm.getData2() - lastNoteOffVelocity);
                            lastNoteOffVelocity = sm.getData2();
                        }
                    }
                }
            }
            //controller 33 changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == CONTROL_CHANGE && sm.getData1() == 33) {
                            dos.write(sm.getData2() - lastControllerValue[sm.getData1()]);
                            lastControllerValue[sm.getData1()] = sm.getData2();
                        }
                    }
                }
            }
            //controller 39 changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == CONTROL_CHANGE && sm.getData1() == 39) {
                            dos.write(sm.getData2() - lastControllerValue[sm.getData1()]);
                            lastControllerValue[sm.getData1()] = sm.getData2();
                        }
                    }
                }
            }
            //controller 42 changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == CONTROL_CHANGE && sm.getData1() == 42) {
                            dos.write(sm.getData2() - lastControllerValue[sm.getData1()]);
                            lastControllerValue[sm.getData1()] = sm.getData2();
                        }
                    }
                }
            }
            //controller 0, 32 and program changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == CONTROL_CHANGE && (sm.getData1() == 0 || sm.getData1() == 32)) {
                            dos.write(sm.getData2() - lastControllerValue[sm.getData1()]);
                            lastControllerValue[sm.getData1()] = sm.getData2();
                        } else if (sm.getCommand() == PROGRAM_CHANGE) {
                            dos.write(sm.getData1());
                        }
                    }
                }
            }
            //pitch bend bottom changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == PITCH_WHEEL_CHANGE) {
                            dos.write(sm.getData1() - lastWheelChangeB);
                            lastWheelChangeB = sm.getData1();
                        }
                    }
                }
            }
            //controller 99 changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == CONTROL_CHANGE && sm.getData1() == 99) {
                            dos.write(sm.getData2() - lastControllerValue[sm.getData1()]);
                            lastControllerValue[sm.getData1()] = sm.getData2();
                        }
                    }
                }
            }
            //controller 98 changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == CONTROL_CHANGE && sm.getData1() == 98) {
                            dos.write(sm.getData2() - lastControllerValue[sm.getData1()]);
                            lastControllerValue[sm.getData1()] = sm.getData2();
                        }
                    }
                }
            }
            //controller 101 changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == CONTROL_CHANGE && sm.getData1() == 101) {
                            dos.write(sm.getData2() - lastControllerValue[sm.getData1()]);
                            lastControllerValue[sm.getData1()] = sm.getData2();
                        }
                    }
                }
            }
            //controller 100 changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) message;
                        if (sm.getCommand() == CONTROL_CHANGE && sm.getData1() == 100) {
                            dos.write(sm.getData2() - lastControllerValue[sm.getData1()]);
                            lastControllerValue[sm.getData1()] = sm.getData2();
                        }
                    }
                }
            }
            //tempo changes
            for (Track track : sequence.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent event = track.get(i);
                    MidiMessage message = event.getMessage();
                    if (message instanceof MetaMessage) {
                        MetaMessage mm = (MetaMessage) message;
                        if (mm.getType() == SET_TEMPO) {
                            dos.write(mm.getData());
                        }
                    }
                }
            }
            //write footer
            dos.write(sequence.getTracks().length);
            dos.writeShort(sequence.getResolution());

            dos.flush();
            dos.close();
        } catch (InvalidMidiDataException | IOException e) {
            e.printStackTrace();
        }
    }
}
