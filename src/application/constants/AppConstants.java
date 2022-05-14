package application.constants;

import runescape.PcmPlayerProvider;

import java.io.File;

public class AppConstants {

    public static int sampleRate = 44100; //Original: 22050
    public static int volumeLevel = 127; //Original: 255
    public static boolean stereo = true;
    public static boolean shuffle = false;
    public static boolean usingSoundFont = false;

    public static PcmPlayerProvider pcmPlayerProvider;

    public static byte[] midiMusicFileBytes;
    public static byte[] nextMidiFileBytes;
    public static String currentSongName = "";
    public static String nextSongName = "";
    public static int currentMusicIndex = 6;
    public static String customSoundFontPath = System.getProperty("user.home") + File.separator + "Downloads" + File.separator + "RS2.sf2";
    public static String customSoundFontsPath = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "Custom Sounds";
    public static File[] currentMusicFiles;
    public static String customCache = System.getProperty("user.home") + File.separator + "Custom Cache";
}
