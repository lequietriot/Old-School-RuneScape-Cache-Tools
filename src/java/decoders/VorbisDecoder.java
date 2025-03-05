package decoders;

import com.application.GUI;
import com.displee.cache.CacheLibrary;
import org.gagravarr.ogg.OggFile;
import org.gagravarr.ogg.OggPacket;
import org.gagravarr.ogg.OggPacketWriter;
import org.gagravarr.vorbis.VorbisComments;
import org.gagravarr.vorbis.VorbisFile;
import org.gagravarr.vorbis.VorbisInfo;
import org.gagravarr.vorbis.VorbisSetup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class VorbisDecoder {

    GUI currentGUI;
    CacheLibrary cacheLibrary;

    public VorbisDecoder(GUI currentGUI) {
        this.currentGUI = currentGUI;
        cacheLibrary = GUI.cacheLibrary;
        int index = currentGUI.selectedIndex;
        int archive = currentGUI.selectedArchive;
        int file = currentGUI.selectedFile;
        try {
            for (archive = 1; archive < 100000; archive++) {
                if (cacheLibrary.index(14).archive(archive) != null) {
                    decodeAsOGG(Objects.requireNonNull(cacheLibrary.index(14).archive(archive)).file(0).getData(), archive);
                }
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public VorbisDecoder(byte[] data, int id) {
        try {
            decodeAsOGG(data, id);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void decodeAsOGG(byte[] data, int archiveID) throws IOException, URISyntaxException {
        File outputFilePath = new File(GUI.cacheLibrary.getPath() + File.separator + "Decoded Data" + File.separator + "OGG Vorbis");
        boolean madeDirectory = outputFilePath.mkdirs();
        if (madeDirectory) {
            //currentGUI.cacheOperationInfo.setText("Archive " + archiveID + " was decoded successfully. New folder created in cache directory.");
        } else {
            //currentGUI.cacheOperationInfo.setText("Archive " + archiveID + " was decoded successfully. It is in the cache directory.");
        }
        File outputFile = new File(outputFilePath + File.separator + archiveID + ".ogg");
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        OggFile newOggFile = new OggFile(fileOutputStream);
        OggPacketWriter oggPacketWriter = newOggFile.getPacketWriter();

        VorbisFile exampleOgg = new VorbisFile(new OggFile(VorbisDecoder.class.getClassLoader().getResourceAsStream("example.ogg")));

        ByteBuffer buffer = ByteBuffer.wrap(data);
        int samplingRate = buffer.getInt();
        int sampleSize = buffer.getInt();
        int loopStart = buffer.getInt();
        int loopEnd = buffer.getInt();
        int packetCount = buffer.getInt();

        VorbisInfo vorbisInfo = exampleOgg.getInfo();
        vorbisInfo.setRate(samplingRate);
        oggPacketWriter.bufferPacket(vorbisInfo.write(), true);

        VorbisComments vorbisComments = exampleOgg.getComment();
        vorbisComments.addComment("Sample Rate", String.valueOf(samplingRate));
        vorbisComments.addComment("Sample Size", String.valueOf(sampleSize));
        vorbisComments.addComment("Loop Start", String.valueOf(loopStart));
        vorbisComments.addComment("Loop End", String.valueOf(loopEnd));

        oggPacketWriter.bufferPacket(vorbisComments.write(), true);

        VorbisSetup vorbisSetup = exampleOgg.getSetup();
        oggPacketWriter.bufferPacket(new OggPacket(vorbisSetup.getData()), true);

        for(int packet = 0; packet < packetCount; ++packet) {
            int size = 0;

            int offset;
            do {
                offset = buffer.get() & 0xFF;
                size += offset;
            } while(offset >= 255);

            byte[] packetData = new byte[size];
            buffer.get(packetData, 0, size);

            OggPacket oggPacket = new OggPacket(packetData);
            oggPacketWriter.setGranulePosition(packet + 1);
            oggPacketWriter.bufferPacket(oggPacket, true);
        }
        fileOutputStream.flush();
        fileOutputStream.close();
    }

}
