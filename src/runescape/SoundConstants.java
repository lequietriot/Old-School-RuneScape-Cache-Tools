package runescape;

public class SoundConstants {

    public static int sampleRate = 44100; //Original: 22050
    public static int volumeLevel = 255; //Original: 255
    public static boolean stereo = true;
    public static boolean shuffle = false;

    public static PcmPlayerProvider pcmPlayerProvider;

    public static byte[] midiMusicFileBytes;
    public static String currentSongName = "";
    public static int currentMusicIndex = 6;
}
