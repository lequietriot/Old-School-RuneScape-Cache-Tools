package decoders;

import application.GUI;
import com.displee.cache.CacheLibrary;
import runelite.definitions.ModelDefinition;
import runelite.loaders.ModelLoader;
import runelite.managers.TextureManager;
import runelite.models.ObjExporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

public class ModelDecoderOS {

    GUI gui;
    CacheLibrary cacheLibrary;

    public ModelDecoderOS(GUI currentGUI) {
        gui = currentGUI;
        cacheLibrary = GUI.cacheLibrary;
        int index = currentGUI.selectedIndex;
        int archive = currentGUI.selectedArchive;
        int file = currentGUI.selectedFile;
        try {
            File outputFilePath = new File(GUI.cacheLibrary.getPath() + File.separator + "Decoded Data" + File.separator + "Models");
            boolean madeDirectory = outputFilePath.mkdirs();
            if (madeDirectory) {
                currentGUI.cacheOperationInfo.setText("Archive " + archive + " was decoded successfully. New folder created in cache directory.");
            } else {
                currentGUI.cacheOperationInfo.setText("Archive " + archive + " was decoded successfully. It is in the cache directory.");
            }

            TextureManager tm = new TextureManager(cacheLibrary);
            tm.load();

            ModelLoader loader = new ModelLoader();
            ModelDefinition model = loader.load(archive, Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(cacheLibrary.index(index).archive(archive)).file(file)).getData()));

            ObjExporter exporter = new ObjExporter(tm, model);
            try (PrintWriter objWriter = new PrintWriter(new FileWriter(outputFilePath + File.separator + archive + ".obj"));
                 PrintWriter mtlWriter = new PrintWriter(new FileWriter(outputFilePath + File.separator + archive + ".mtl")))
            {
                exporter.export(objWriter, mtlWriter);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
