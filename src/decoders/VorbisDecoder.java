package decoders;

import application.GUI;
import org.displee.CacheLibrary;
import org.gagravarr.ogg.OggFile;
import org.gagravarr.ogg.OggPacket;
import org.gagravarr.ogg.OggPacketWriter;
import org.gagravarr.vorbis.VorbisComments;
import org.gagravarr.vorbis.VorbisFile;
import org.gagravarr.vorbis.VorbisInfo;
import org.gagravarr.vorbis.VorbisSetup;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

public class VorbisDecoder {

    GUI gui;
    CacheLibrary cacheLibrary;

    public VorbisDecoder(GUI currentGUI) {
        gui = currentGUI;
        cacheLibrary = currentGUI.cacheLibrary;
        int index = currentGUI.selectedIndex;
        int archive = currentGUI.selectedArchive;
        int file = currentGUI.selectedFile;
        try {
            decodeAsOGG(cacheLibrary.getIndex(index).getArchive(archive).getFile(file).getData(), archive);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void decodeAsOGG(byte[] data, int archiveID) throws IOException, URISyntaxException {
        File outputFilePath = new File(gui.cacheLibrary.getPath() + File.separator + "Decoded Data" + File.separator + "OGG Vorbis");
        boolean madeDirectory = outputFilePath.mkdirs();
        if (madeDirectory) {
            JOptionPane.showMessageDialog(gui.getContentPane(), "Archive " + archiveID + " was decoded successfully.\nIt can be found in the newly created path: " + outputFilePath.getPath());
        } else {
            JOptionPane.showMessageDialog(gui.getContentPane(), "Archive " + archiveID + " was decoded successfully.\nIt can be found in the following path: " + outputFilePath.getPath());
        }
        File outputFile = new File(outputFilePath + File.separator + archiveID + ".ogg");
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        OggFile newOggFile = new OggFile(fileOutputStream);
        OggPacketWriter oggPacketWriter = newOggFile.getPacketWriter();

        VorbisFile exampleOgg = new VorbisFile(new OggFile(getClass().getClassLoader().getResourceAsStream("example.ogg")));

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

        oggPacketWriter.flush();
        oggPacketWriter.close();
    }

}
