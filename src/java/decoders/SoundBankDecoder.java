package decoders;

import com.application.GUI;
import com.displee.cache.CacheLibrary;
import com.sun.media.sound.SF2Instrument;
import com.sun.media.sound.SF2Soundbank;
import osrs.MusicPatch;
import osrs.SoundCache;

import javax.sound.midi.SoundbankResource;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.Objects;

public class SoundBankDecoder {

    GUI gui;
    CacheLibrary cacheLibrary;
    SF2Soundbank sf2Soundbank;

    public SoundBankDecoder(GUI currentGUI) {
        gui = currentGUI;
        cacheLibrary = GUI.cacheLibrary;
        sf2Soundbank = new SF2Soundbank();
        for (int id = 0; id < 4000; id++) {
            if (cacheLibrary.index(15).archive(id) != null) {
                SoundCache soundCache = new SoundCache(cacheLibrary.index(4), cacheLibrary.index(14));
                MusicPatch musicPatch = new MusicPatch(Objects.requireNonNull(Objects.requireNonNull(cacheLibrary.index(15).archive(id)).file(0)).getData());
                try {
                    decodeAsSoundFont2(musicPatch, soundCache, id);
                } catch (UnsupportedAudioFileException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            sf2Soundbank.save("./RuneScape.sf2");
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
