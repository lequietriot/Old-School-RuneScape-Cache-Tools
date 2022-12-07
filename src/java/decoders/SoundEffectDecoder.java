package decoders;

import com.application.GUI;
import com.displee.cache.CacheLibrary;
import osrs.SoundEffect;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

public class SoundEffectDecoder {

    GUI gui;
    CacheLibrary cacheLibrary;

    public SoundEffectDecoder(GUI currentGUI) {
        gui = currentGUI;
        cacheLibrary = GUI.cacheLibrary;
        int index = currentGUI.selectedIndex;
        int archive = currentGUI.selectedArchive;
        int file = currentGUI.selectedFile;

        try {

            File outputFilePath = new File(GUI.cacheLibrary.getPath() + File.separator + "Decoded Data" + File.separator + "Sound Effects");

            boolean madeDirectory = outputFilePath.mkdirs();
            if (madeDirectory) {
                GUI.cacheOperationInfo.setText("Archive " + archive + " was decoded successfully. New folder created in cache directory.");
            } else {
                GUI.cacheOperationInfo.setText("Archive " + archive + " was decoded successfully. It is in the cache directory.");
            }
            File outputFile = new File(outputFilePath + File.separator + archive + ".wav");
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            if (cacheLibrary.index(index).archive(archive) != null) {
                byte[] audioData = Objects.requireNonNull(SoundEffect.readSoundEffect(cacheLibrary.index(index), archive, file)).toRawSound().audioData;
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
                AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, new AudioFormat(22050, 8, 1, true, false), audioData.length);
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, fileOutputStream);
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
