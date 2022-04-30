package decoders;

import application.GUI;
import org.displee.CacheLibrary;
import runelite.definitions.ModelDefinition;
import runelite.loaders.ModelLoader;
import runelite.managers.TextureManager;
import runelite.models.ObjExporter;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ModelDecoderHD {


    GUI gui;
    CacheLibrary cacheLibrary;

    public ModelDecoderHD(GUI currentGUI) {
        gui = currentGUI;
        cacheLibrary = currentGUI.cacheLibrary;
        int index = currentGUI.selectedIndex;
        int archive = currentGUI.selectedArchive;
        int file = currentGUI.selectedFile;
        try {
            File outputFilePath = new File(gui.cacheLibrary.getPath() + File.separator + "Decoded Data" + File.separator + "Models");
            if (outputFilePath != null) {
                boolean madeDirectory = outputFilePath.mkdirs();
                if (madeDirectory) {
                    JOptionPane.showMessageDialog(gui.getContentPane(), "Archive " + archive + " was decoded successfully.\nIt can be found in the newly created path: " + outputFilePath.getPath());
                } else {
                    JOptionPane.showMessageDialog(gui.getContentPane(), "Archive " + archive + " was decoded successfully.\nIt can be found in the following path: " + outputFilePath.getPath());
                }

                TextureManager tm = new TextureManager(cacheLibrary);
                tm.load();

                ModelLoader loader = new ModelLoader();
                ModelDefinition model = loader.load(archive, cacheLibrary.getIndex(index).getArchive(archive).getFile(file).getData());

                ObjExporter exporter = new ObjExporter(tm, model);
                try (PrintWriter objWriter = new PrintWriter(new FileWriter(outputFilePath + File.separator + archive + ".obj"));
                     PrintWriter mtlWriter = new PrintWriter(new FileWriter(outputFilePath + File.separator + archive + ".mtl")))
                {
                    exporter.export(objWriter, mtlWriter);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
