package net.runelite.cache.loaders;

import com.displee.cache.index.Index;
import net.runelite.cache.definitions.TextureDetails;
import osrs.Buffer;

import java.util.Objects;

public class ImageIndexLoader implements ImageLoader {

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
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (stream.readUnsignedByte() == 1) {
                textures[i_5] = new TextureDetails();
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].isGroundMesh = stream.readUnsignedByte() == 0;
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].isHalfSize = stream.readUnsignedByte() == 1;
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].skipTriangles = stream.readUnsignedByte() == 1;
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].brightness = stream.readByte();
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].shadowFactor = stream.readByte();
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].effectId = stream.readByte();
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].effectParam1 = stream.readByte();
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].color = (short) stream.readUnsignedShort();
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].textureSpeedU = stream.readByte();
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].textureSpeedV = stream.readByte();
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].aBool2087 = stream.readUnsignedByte() == 1;
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].isBrickTile = stream.readUnsignedByte() == 1;
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].useMipmaps = stream.readByte();
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].repeatS = stream.readUnsignedByte() == 1;
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].repeatT = stream.readUnsignedByte() == 1;
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].hdr = stream.readUnsignedByte() == 1;
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].combineMode = stream.readUnsignedByte();
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].effectParam2 = stream.readInt();
            }
        }
        for (i_5 = 0; i_5 < textureDefSize; i_5++) {
            if (textures[i_5] != null) {
                textures[i_5].blendType = stream.readUnsignedByte();
            }
        }
    }

    @Override
    public int method84() {
        return textureDefSize;
    }

    @Override
    public TextureDetails getTextureDetails(int i_1) {
        textures[i_1].id = i_1;
        return textures[i_1];
    }

    @Override
    public void method161() {
    }

}
