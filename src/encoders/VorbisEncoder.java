package encoders;

import org.gagravarr.vorbis.VorbisAudioData;
import org.gagravarr.vorbis.VorbisFile;

import java.io.*;

public class VorbisEncoder {

    public static void encode(VorbisFile oggFile, int archiveID) throws IOException {
        //Write the setup file first (To be stored in Index 14, Archive 0)
        File setupFile = new File("./0.dat/");
        FileOutputStream setupFileOutputStream = new FileOutputStream(setupFile);
        byte[] setupData = oggFile.getSetup().getData();
        byte[] rsSetupData = new byte[setupData.length - 6];
        rsSetupData[0] = (byte) 170;
        int newIndex = 1;
        for (int index = 7; index < rsSetupData.length; index++) {
            rsSetupData[newIndex] = setupData[index];
            newIndex++;
        }
        //Writes the new OGG setup data, with the " vorbis" header replaced
        setupFileOutputStream.write(rsSetupData);

        //Now, read the input OGG file and convert to RuneScape's format
        File file = new File("./" + archiveID + ".dat/");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);

        if (oggFile.getTags().getComments("Sample Rate").size() != 0) {
            dataOutputStream.writeInt(Integer.parseInt(oggFile.getTags().getComments("Sample Rate").get(0))); //Sample Rate
        }
        else {
            dataOutputStream.writeInt(oggFile.getInfo().getSampleRate());
            System.out.println("Using default Sample Rate of " + oggFile.getInfo().getSampleRate() + ".");
        }

        if (oggFile.getTags().getComments("Sample Size").size() != 0) {
            dataOutputStream.writeInt(Integer.parseInt(oggFile.getTags().getComments("Sample Size").get(0))); //Audio Sample Size
        }
        else {
            dataOutputStream.writeInt(0);
            System.out.println("WARNING: No comment tag in OGG file for Sample size/length!");
        }

        if (oggFile.getTags().getComments("Loop Start").size() != 0) {
            dataOutputStream.writeInt(Integer.parseInt(oggFile.getTags().getComments("Loop Start").get(0))); //Sample Loop Start
        }
        else {
            dataOutputStream.writeInt(0);
            System.out.println("WARNING: No comment tag in OGG file for Sample Loop Start position!");
        }

        if (oggFile.getTags().getComments("Loop End").size() != 0) {
            dataOutputStream.writeInt(Integer.parseInt(oggFile.getTags().getComments("Loop End").get(0))); //Sample Loop End
        }
        else {
            dataOutputStream.writeInt(0);
            System.out.println("WARNING: No comment tag in OGG file for Sample Loop End position!");
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        VorbisAudioData vorbisAudioData = oggFile.getNextAudioPacket();

        int packetAmount = 0;
        while (vorbisAudioData != null) {
            //Get the length of the audio packet
            int dataLength = vorbisAudioData.getData().length;
            //If the audio packet size is greater than 255, split the size over various bytes as needed
            while (dataLength > 255) {
                byteArrayOutputStream.write(255);
                dataLength = dataLength - 256;
            }
            //Write the length of the audio packet
            byteArrayOutputStream.write(dataLength);
            //Next, write the audio packet data after the length
            for (int index = 0; index < vorbisAudioData.getData().length; index++) {
                byteArrayOutputStream.write(vorbisAudioData.getData()[index]);
            }
            packetAmount++;
            vorbisAudioData = oggFile.getNextAudioPacket();
            //Repeat until all packets are written to the data file
        }
        //Finally, write the amount of packets in the output, as well as the data from the byte array.
        dataOutputStream.writeInt(packetAmount);
        dataOutputStream.write(byteArrayOutputStream.toByteArray());
        System.out.println("Successfully encoded ID: " + archiveID + " to the RuneScape format!");
    }

}
