package decoders;

import application.GUI;
import com.sun.media.sound.SF2Soundbank;
import org.displee.CacheLibrary;
import runescape.MusicPatch;
import runescape.SoundCache;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;

public class SoundBankDecoder {

    GUI gui;
    CacheLibrary cacheLibrary;

    public SoundBankDecoder(GUI currentGUI) {
        gui = currentGUI;
        cacheLibrary = currentGUI.cacheLibrary;
        for (int id = 0; id < 4000; id++) {
            if (cacheLibrary.getIndex(15).getArchive(id) != null) {
                SoundCache soundCache = new SoundCache(cacheLibrary.getIndex(4), cacheLibrary.getIndex(14));
                MusicPatch musicPatch = new MusicPatch(cacheLibrary.getIndex(15).getArchive(id).getFile(0).getData());
                try {
                    decodeAsSoundFont2(musicPatch, soundCache, id);

                } catch (UnsupportedAudioFileException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void decodeAsSoundFont2(MusicPatch musicPatch, SoundCache soundCache, int id) throws IOException, UnsupportedAudioFileException {
        SF2Soundbank sf2 = new SF2Soundbank();
        musicPatch.decodeMusicPatch(soundCache, sf2, id).save("./SoundFont/" + id + ".sf2");
    }
}
