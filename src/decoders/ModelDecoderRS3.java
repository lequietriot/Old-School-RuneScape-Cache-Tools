package decoders;

import application.GUI;
import com.displee.cache.CacheLibrary;
import rs3.RS3ModelData;
import runelite.definitions.ModelDefinition;
import runelite.models.ObjExporterRS3;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ModelDecoderRS3 {

    CacheLibrary cacheLibrary;

    public ModelDecoderRS3(GUI currentGUI) {
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

            int id = 0;
            RS3ModelData loader = new RS3ModelData(id);
            ModelDefinition modelDefinition = loader.load(archive, Files.readAllBytes(Paths.get("" + id)), cacheLibrary);
            modelDefinition.resize(32, 32, 32);

            ObjExporterRS3 exporter = new ObjExporterRS3(null, modelDefinition, id);
            try (PrintWriter objWriter = new PrintWriter(new FileWriter(outputFilePath + File.separator + id + ".obj"));
                 PrintWriter mtlWriter = new PrintWriter(new FileWriter(outputFilePath + File.separator + id + ".mtl")))
            {
                exporter.export(objWriter, mtlWriter);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
