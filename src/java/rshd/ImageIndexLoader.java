package rshd;

import com.displee.cache.index.Index;
import net.runelite.cache.definitions.TextureDetails;
import osrs.Buffer;
import osrs.Node;
import osrs.NodeHashTable;

import java.util.Objects;

public class ImageIndexLoader implements ImageLoader {

    NodeHashTable aClass223_3754 = new NodeHashTable(256);

    Index textureIndex;

    Index spriteIndex;

    int textureDefSize;

    TextureDetails[] textures;

    public ImageIndexLoader(Index textureDefIndex, Index textureIndex, Index spriteIndex) {
        this.textureIndex = textureIndex;
        this.spriteIndex = spriteIndex;
        Buffer stream = new Buffer(Objects.requireNonNull(Objects.requireNonNull(textureDefIndex.archive(0)).file(0)).getData());
        textureDefSize = stream.readUnsignedShort();
        textures = new TextureDetails[textureDefSize];
        int i_5;
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (stream.readUnsignedByte() == 1)
                textures[i_5] = new TextureDetails();
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].isGroundMesh = stream.readUnsignedByte() == 0;
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].isHalfSize = stream.readUnsignedByte() == 1;
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].skipTriangles = stream.readUnsignedByte() == 1;
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].brightness = stream.readByte();
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].shadowFactor = stream.readByte();
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].effectId = stream.readByte();
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].effectParam1 = stream.readByte();
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].color = (short) stream.readUnsignedShort();
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].textureSpeedU = stream.readByte();
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].textureSpeedV = stream.readByte();
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].aBool2087 = stream.readUnsignedByte() == 1;
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].isBrickTile = stream.readUnsignedByte() == 1;
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].useMipmaps = stream.readByte();
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].repeatS = stream.readUnsignedByte() == 1;
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].repeatT = stream.readUnsignedByte() == 1;
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].hdr = stream.readUnsignedByte() == 1;
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].combineMode = stream.readUnsignedByte();
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].effectParam2 = stream.readInt();
        for (i_5 = 0; i_5 < textureDefSize; i_5++)
            if (textures[i_5] != null)
                textures[i_5].blendType = stream.readUnsignedByte();
    }

    MaterialDefinitions getMaterialDefinitions(int textureId) {
        Node cacheablenode_3 = aClass223_3754.get(textureId);
        if (cacheablenode_3 != null) {
            return (MaterialDefinitions) cacheablenode_3;
        }
        byte[] bytes_4 = Objects.requireNonNull(Objects.requireNonNull(textureIndex.archive(textureId)).file(0)).getData();
        if (bytes_4 == null)
            return null;
        else {
            MaterialDefinitions texturedefinition_5 = new MaterialDefinitions(new Buffer(bytes_4));
            aClass223_3754.put(texturedefinition_5, textureId);
            return texturedefinition_5;
        }
    }

    @Override
    public TextureDetails getTextureDetails(int i_1) {
        return textures[i_1];
    }

    @Override
    public boolean loadTexture(int var1) {
        return true;
    }

    @Override
    public void method161() {
    }

    @Override
    public int method84() {
        return textureDefSize;
    }

    @Override
    public float[] renderMaterialPixelsF(int i_1, int i_3, int i_4) {
        float[] floats_7 = getMaterialDefinitions(i_1).renderFloatPixels(spriteIndex, this, i_3, i_4, textures[i_1].isBrickTile);
        return floats_7;
    }

    @Override
    public int[] renderMaterialPixelsI(int textureId, int width, int height) {
        int[] ints_7 = getMaterialDefinitions(textureId).renderIntPixels(spriteIndex, this, (float) 0.7, width, height, textures[textureId].isBrickTile);
        return ints_7;
    }

    @Override
    public int[] renderTexturePixels(int i_1, float f_2, int i_3, int i_4, boolean bool_5) {
        int[] pixels = getMaterialDefinitions(i_1).method14718(spriteIndex, this, f_2, i_3, i_4, bool_5, textures[i_1].isBrickTile);
        return pixels;
    }

}