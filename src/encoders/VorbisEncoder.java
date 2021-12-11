package encoders;

import application.GUI;
import org.displee.CacheLibrary;
import org.gagravarr.vorbis.VorbisAudioData;
import org.gagravarr.vorbis.VorbisFile;

import javax.swing.*;
import java.io.*;
import java.net.URISyntaxException;

public class VorbisEncoder {

    GUI gui;

    public VorbisEncoder(GUI currentGUI) {
        gui = currentGUI;
        JFileChooser chooseOgg = new JFileChooser();
        chooseOgg.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooseOgg.showOpenDialog(gui) == JFileChooser.APPROVE_OPTION) {
            File selected = chooseOgg.getSelectedFile();
            if (selected.getName().endsWith(".ogg")) {
                encode(selected);
            }
        }
    }

    public void encode(File selected) {
        try {
            VorbisFile oggFile = new VorbisFile(selected);
            File outputFilePath = new File(gui.cacheLibrary.getPath() + File.separator + "Encoded Data" + File.separator + "Vorbis");
            boolean madeDirectory = outputFilePath.mkdirs();
            if (madeDirectory) {
                JOptionPane.showMessageDialog(gui.getContentPane(), selected.getName() + " was encoded successfully.\nIt can be found in the newly created path: " + outputFilePath.getPath());
            } else {
                JOptionPane.showMessageDialog(gui.getContentPane(), selected.getName() + " was encoded successfully.\nIt can be found in the following path: " + outputFilePath.getPath());
            }
            File outputFile = new File(outputFilePath + File.separator + selected.getName().replace(".ogg", ".dat").trim());
            DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(outputFile));

            if (oggFile.getTags().getComments("Sample Rate").size() != 0) {
                dataOutputStream.writeInt(Integer.parseInt(oggFile.getTags().getComments("Sample Rate").get(0))); //Sample Rate
            } else {
                dataOutputStream.writeInt(oggFile.getInfo().getSampleRate());
                System.out.println("Using default Sample Rate of " + oggFile.getInfo().getSampleRate() + ".");
            }

            if (oggFile.getTags().getComments("Sample Size").size() != 0) {
                dataOutputStream.writeInt(Integer.parseInt(oggFile.getTags().getComments("Sample Size").get(0))); //Audio Sample Size
            } else {
                dataOutputStream.writeInt(0);
                System.out.println("WARNING: No comment tag in OGG file for Sample size/length!");
            }

            if (oggFile.getTags().getComments("Loop Start").size() != 0) {
                dataOutputStream.writeInt(Integer.parseInt(oggFile.getTags().getComments("Loop Start").get(0))); //Sample Loop Start
            } else {
                dataOutputStream.writeInt(0);
                System.out.println("WARNING: No comment tag in OGG file for Sample Loop Start position!");
            }

            if (oggFile.getTags().getComments("Loop End").size() != 0) {
                dataOutputStream.writeInt(Integer.parseInt(oggFile.getTags().getComments("Loop End").get(0))); //Sample Loop End
            } else {
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
            dataOutputStream.flush();
            dataOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
