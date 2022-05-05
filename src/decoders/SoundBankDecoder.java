package decoders;

import application.GUI;
import com.sun.media.sound.SF2Instrument;
import com.sun.media.sound.SF2Soundbank;
import org.displee.CacheLibrary;
import runescape.MusicPatch;
import runescape.SoundCache;

import javax.sound.midi.SoundbankResource;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;

public class SoundBankDecoder {

    GUI gui;
    CacheLibrary cacheLibrary;
    SF2Soundbank sf2Soundbank;

    public SoundBankDecoder(GUI currentGUI) {
        gui = currentGUI;
        cacheLibrary = currentGUI.cacheLibrary;
        sf2Soundbank = new SF2Soundbank();
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
        try {
            sf2Soundbank.save("./SoundFont/RuneScape.sf2");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void decodeAsSoundFont2(MusicPatch musicPatch, SoundCache soundCache, int id) throws IOException, UnsupportedAudioFileException {
        SF2Soundbank sf2 = new SF2Soundbank();
        sf2 = musicPatch.decodeMusicPatch(soundCache, sf2, id);
        for (SoundbankResource soundbankResource : sf2.getResources()) {
            sf2Soundbank.addResource(soundbankResource);
        }
        for (SF2Instrument sf2Instrument : sf2.getInstruments()) {
            sf2Soundbank.addInstrument(sf2Instrument);
        }
    }
}
