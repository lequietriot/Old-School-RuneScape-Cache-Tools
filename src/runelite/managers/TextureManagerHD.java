package runelite.managers;

import application.constants.OSRSCacheIndexConstants;
import org.displee.CacheLibrary;
import org.displee.cache.index.Index;
import org.displee.cache.index.archive.Archive;
import runelite.definitions.TextureDefinition;
import runelite.loaders.TextureLoader;
import runelite.providers.TextureProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TextureManagerHD implements TextureProvider {

    private final CacheLibrary store;
    private final List<TextureDefinition> textures = new ArrayList<>();

    public TextureManagerHD(CacheLibrary store)
    {
        this.store = store;
    }

    public void load() throws IOException
    {
        Index index = store.getIndex(OSRSCacheIndexConstants.TEXTURES);
        Archive[] archives = index.getArchives();

        TextureLoader loader = new TextureLoader();

        for (Archive archive : archives)
        {
            TextureDefinition texture = loader.load(archive.getId(), archive.getFile(0).getData());
            textures.add(texture);
        }
    }

    public List<TextureDefinition> getTextures()
    {
        return textures;
    }

    public TextureDefinition findTexture(int id)
    {
        for (TextureDefinition td : textures)
        {
            if (td.getId() == id)
            {
                return td;
            }
        }
        return null;
    }

    @Override
    public TextureDefinition[] provide()
    {
        return textures.toArray(new TextureDefinition[textures.size()]);
    }
}
