package decoders;

import com.application.GUI;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.runelite.cache.ConfigType;
import net.runelite.cache.IndexType;
import net.runelite.cache.definitions.EnumDefinition;
import net.runelite.cache.definitions.loaders.EnumLoader;
import net.runelite.cache.fs.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class EnumDecoder {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public EnumDecoder()
    {
        try {
            dump();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void dump() throws IOException
    {
        File dumpDir = new File(GUI.cacheLibrary.getPath() + File.separator + "Decoded Data" + File.separator + "Configurations" + File.separator + "Enums");

        boolean madeDirectory = dumpDir.mkdirs();
        if (madeDirectory) {
            GUI.cacheOperationInfo.setText("New folder created in cache directory.");
        } else {
            GUI.cacheOperationInfo.setText("It is in the cache directory.");
        }


        int count = 0;

        Store store = new Store(new File(GUI.cacheLibrary.getPath()));
        try
        {
            store.load();

            Storage storage = store.getStorage();
            Index index = store.getIndex(IndexType.CONFIGS);
            Archive archive = index.getArchive(ConfigType.ENUM.getId());

            byte[] archiveData = storage.loadArchive(archive);
            ArchiveFiles files = archive.getFiles(archiveData);

            EnumLoader loader = new EnumLoader();

            for (FSFile file : files.getFiles())
            {
                byte[] b = file.getContents();

                EnumDefinition def = loader.load(file.getFileId(), b);

                if (def != null)
                {
                    Files.asCharSink(new File(dumpDir, file.getFileId() + ".json"), Charset.defaultCharset()).write(gson.toJson(def));
                    ++count;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Dumped " + count + " enums to " + dumpDir.getPath());
    }
}
