package decoders;

import com.application.GUI;
import com.displee.cache.CacheLibrary;
import net.runelite.cache.InterfaceManager;
import net.runelite.cache.definitions.InterfaceDefinition;
import net.runelite.cache.definitions.loaders.InterfaceLoader;
import net.runelite.cache.fs.Store;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class InterfaceDecoder {

    GUI gui;
    CacheLibrary cacheLibrary;

    public InterfaceDecoder(GUI currentGUI) {
        try {
            gui = currentGUI;
            cacheLibrary = GUI.cacheLibrary;
            int index = currentGUI.selectedIndex;
            int archive = currentGUI.selectedArchive;
            int file = currentGUI.selectedFile;

            InterfaceDefinition interfaceDefinition = new InterfaceLoader().load(archive, Objects.requireNonNull(cacheLibrary.data(index, archive, file)));
            InterfaceDefinition[] interfaceDefinitions = new InterfaceManager(new Store(new File(cacheLibrary.getPath()))).getIntefaceGroup(archive);

            JFrame interfaceNew = new JFrame();
            interfaceNew.add(new OSRSInterface(interfaceDefinitions));
            interfaceNew.setVisible(true);
            interfaceNew.revalidate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class OSRSInterface extends Canvas {

        InterfaceDefinition interfaceDefinition;

        public OSRSInterface(InterfaceDefinition definition) {
            interfaceDefinition = definition;
        }

        public OSRSInterface(InterfaceDefinition[] definitions) {

        }
    }
}
