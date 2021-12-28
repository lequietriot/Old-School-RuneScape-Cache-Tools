package decoders;

import application.GUI;
import org.displee.CacheLibrary;
import runescape.MusicTrack;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

public class MidiDecoder {

    GUI gui;
    CacheLibrary cacheLibrary;

    public MidiDecoder(GUI currentGUI) {
        gui = currentGUI;
        cacheLibrary = currentGUI.cacheLibrary;
        int index = currentGUI.selectedIndex;
        int archive = currentGUI.selectedArchive;
        int file = currentGUI.selectedFile;

        try {
            File outputFilePath = null;

            if (index == 6) {
                outputFilePath = new File(gui.cacheLibrary.getPath() + File.separator + "Decoded Data" + File.separator + "MIDI Music");
            }
            if (index == 11) {
                outputFilePath = new File(gui.cacheLibrary.getPath() + File.separator + "Decoded Data" + File.separator + "MIDI Jingles");
            }
            if (outputFilePath != null) {
                boolean madeDirectory = outputFilePath.mkdirs();
                if (madeDirectory) {
                    JOptionPane.showMessageDialog(gui.getContentPane(), "Archive " + archive + " was decoded successfully.\nIt can be found in the newly created path: " + outputFilePath.getPath());
                } else {
                    JOptionPane.showMessageDialog(gui.getContentPane(), "Archive " + archive + " was decoded successfully.\nIt can be found in the following path: " + outputFilePath.getPath());
                }
                File outputFile = new File(outputFilePath + File.separator + archive + ".mid");
                FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                fileOutputStream.write(Objects.requireNonNull(MusicTrack.readTrack(cacheLibrary.getIndex(index), archive, file)).midi);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
