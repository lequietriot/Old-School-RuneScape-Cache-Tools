package runelite.loaders;

import org.displee.cache.index.Index;
import runelite.definitions.TextureDetails;
import runescape.Buffer;

public class ImageIndexLoader implements ImageLoader {

    Index textureIndex;

    Index spriteIndex;

    int textureDefSize;

    TextureDetails[] textures;

    public ImageIndexLoader(Index textureDefIndex, Index textureIndex, Index spriteIndex) {
        this.textureIndex = textureIndex;
        this.spriteIndex = spriteIndex;
        Buffer stream = new Buffer(textureDefIndex.getArchive(0).getFile(0).getData());
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

    public static int method5773(int i_0, int i_1) {
        int i_3 = i_1 >>> 24;
        int i_4 = 255 - i_3;
        i_1 = (i_3 * (i_1 & 0xff00ff) & -16711936 | i_3 * (i_1 & 0xff00) & 0xff0000) >>> 8;
        return ((i_4 * (i_0 & 0xff00ff) & -16711936 | (i_0 & 0xff00) * i_4 & 0xff0000) >>> 8) + i_1;
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
