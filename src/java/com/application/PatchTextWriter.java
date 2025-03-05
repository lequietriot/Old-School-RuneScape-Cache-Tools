package com.application;

import com.sun.media.sound.SF2Sample;
import com.sun.media.sound.SF2Soundbank;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PatchTextWriter {

    public static void main(String[] args) throws IOException {
        SF2Soundbank sf2Soundbank = new SF2Soundbank(new File(AppConstants.customSoundFontPath));
        for (SF2Sample sf2Sample : sf2Soundbank.getSamples()) {
            File loopFile = new File("Loops" + File.separator + sf2Sample.getName() + ".dat");
            FileOutputStream fileOutputStream = new FileOutputStream(loopFile);
            DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
            dataOutputStream.writeLong(sf2Sample.getSampleRate());
            dataOutputStream.writeLong(sf2Sample.getStartLoop());
            dataOutputStream.writeLong(sf2Sample.getEndLoop());
        }
    }
}
