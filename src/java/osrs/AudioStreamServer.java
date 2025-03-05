package osrs;

import javax.sound.sampled.*;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class AudioStreamServer {

    public static void main(String[] args) throws Exception {
        final AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Server started, waiting for connection...");
            try (Socket clientSocket = serverSocket.accept()) {
                System.out.println("Client connected");
                OutputStream out = clientSocket.getOutputStream();

                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                    line.open(format);
                    line.start();

                    byte[] buffer = new byte[4096];
                    while (true) {
                        int bytesRead = line.write(buffer, 0, buffer.length);
                        if (bytesRead > 0) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        }
    }
}