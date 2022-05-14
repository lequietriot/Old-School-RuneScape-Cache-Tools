package runelite.managers;

import application.constants.RSHDCacheIndexConstants;
import org.displee.CacheLibrary;
import org.displee.cache.index.Index;
import org.displee.cache.index.archive.Archive;
import runelite.definitions.TextureDefinition;
import runelite.definitions.TextureDetails;
import runelite.loaders.ImageIndexLoader;
import runelite.providers.TextureProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TextureManagerHD implements TextureProvider {

    private final CacheLibrary store;
    private final List<TextureDetails> textures = new ArrayList<TextureDetails>();

    public TextureManagerHD(CacheLibrary store)
    {
        this.store = store;
    }

    public void load() throws IOException
    {
        Index textureIndex = store.getIndex(RSHDCacheIndexConstants.TEXTURES);
        Index textureDefinitionsIndex = store.getIndex(RSHDCacheIndexConstants.TEXTURE_DEFINITIONS);
        Index spriteIndex = store.getIndex(RSHDCacheIndexConstants.SPRITES);

        ImageIndexLoader imageIndexLoader = new ImageIndexLoader(textureDefinitionsIndex, textureIndex, spriteIndex);
        for (Archive archive : textureIndex.getArchives()) {
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
