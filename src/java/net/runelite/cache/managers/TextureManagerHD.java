package net.runelite.cache.managers;

import com.displee.cache.CacheLibrary;
import com.displee.cache.index.Index;
import com.displee.cache.index.archive.Archive;
import rshd.ImageIndexLoader;
import net.runelite.cache.definitions.TextureDefinition;
import net.runelite.cache.definitions.TextureDetails;
import net.runelite.cache.providers.TextureProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TextureManagerHD implements TextureProvider {

    private final CacheLibrary store;
    private final List<TextureDetails> textures = new ArrayList<>();

    public TextureManagerHD(CacheLibrary store)
    {
        this.store = store;
    }

    public void load() throws IOException
    {
        Index textureIndex = store.index(9);
        Index textureDefinitionsIndex = store.index(26);
        Index spriteIndex = store.index(8);

        ImageIndexLoader imageIndexLoader = new ImageIndexLoader(textureDefinitionsIndex, textureIndex, spriteIndex);
        for (Archive archive : textureIndex.archives()) {
            textures.add(imageIndexLoader.getTextureDetails(archive.getId()));
        }
    }

    public List<TextureDetails> getTextures()
    {
        return textures;
    }

    public TextureDetails findTexture(int id)
    {
        for (TextureDetails td : textures)
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