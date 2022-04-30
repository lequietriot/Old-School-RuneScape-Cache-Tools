package decoders;

import application.GUI;
import org.displee.CacheLibrary;

public class FileInfoDetector {

    GUI gui;
    CacheLibrary cacheLibrary;

    public FileInfoDetector(GUI currentGUI) {
        gui = currentGUI;
        cacheLibrary = currentGUI.cacheLibrary;
        int index = currentGUI.selectedIndex;
        int archive = currentGUI.selectedArchive;
        int file = currentGUI.selectedFile;
    }
}
