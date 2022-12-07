package decoders;

import com.application.GUI;
import com.displee.cache.CacheLibrary;
import osrs.MusicTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

public class MidiDecoder {

    GUI gui;
    CacheLibrary cacheLibrary;

    public MidiDecoder(GUI currentGUI) {
        gui = currentGUI;
        cacheLibrary = GUI.cacheLibrary;
        int index = currentGUI.selectedIndex;
        int archive = currentGUI.selectedArchive;
        int file = currentGUI.selectedFile;

        try {
            File outputFilePath = null;

            if (index == 6) {
                outputFilePath = new File(GUI.cacheLibrary.getPath() + File.separator + "Decoded Data" + File.separator + "MIDI Music");
            }
            if (index == 11) {
                outputFilePath = new File(GUI.cacheLibrary.getPath() + File.separator + "Decoded Data" + File.separator + "MIDI Jingles");
            }
            if (outputFilePath != null) {
                boolean madeDirectory = outputFilePath.mkdirs();
                if (madeDirectory) {
                    GUI.cacheOperationInfo.setText("Archive " + archive + " was decoded successfully. New folder created in cache directory.");
                } else {
                    GUI.cacheOperationInfo.setText("Archive " + archive + " was decoded successfully. It is in the cache directory.");
                }
                File outputFile = new File(outputFilePath + File.separator + archive + ".mid");
                FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                if (cacheLibrary.index(index).archive(archive) != null) {
                    fileOutputStream.write(fixMidiFile(Objects.requireNonNull(MusicTrack.readTrack(cacheLibrary.index(index), archive, file)).midi));
                }
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private byte[] fixMidiFile(byte[] midi) {
        return midi;
    }

}
