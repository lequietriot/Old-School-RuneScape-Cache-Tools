package decoders;

import com.application.GUI;
import com.displee.cache.CacheLibrary;
import net.runelite.cache.TextureManager;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.fs.Store;
import net.runelite.cache.loaders.ModelLoader;
import net.runelite.cache.models.ObjExporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;

public class ModelDecoderRS2 {

    GUI gui;
    CacheLibrary cacheLibrary;

    public ModelDecoderRS2(GUI currentGUI) {
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

            TextureManager tm = new TextureManager(new Store(new File(cacheLibrary.getPath())));

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
