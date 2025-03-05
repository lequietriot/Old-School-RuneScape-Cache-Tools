package decoders;

import com.application.GUI;
import com.displee.cache.CacheLibrary;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.models.ObjExporterRS3;
import rs3.RS3ModelData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

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

            RS3ModelData loader = new RS3ModelData(archive);
            ModelDefinition modelDefinition = loader.load(archive, cacheLibrary.data(index, archive, file), cacheLibrary);

            ObjExporterRS3 exporter = new ObjExporterRS3(null, modelDefinition, archive);
            try (PrintWriter objWriter = new PrintWriter(new FileWriter(outputFilePath + File.separator + archive + ".obj"));
                 PrintWriter mtlWriter = new PrintWriter(new FileWriter(outputFilePath + File.separator + archive + ".mtl"))) {
                exporter.export(objWriter, mtlWriter);
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
