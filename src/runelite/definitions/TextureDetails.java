package runelite.definitions;

import lombok.Data;

@Data
public class TextureDetails {

    public boolean isGroundMesh;
    public boolean repeatT;
    public byte effectId;
    public boolean isHalfSize;
    public boolean skipTriangles;
    public short color;
    public int blendType;
    public byte effectParam1;
    public int effectParam2;
    public byte shadowFactor;
    public byte brightness;
    public byte textureSpeedU;
    public boolean isBrickTile;
    public boolean repeatS;
    public boolean aBool2087;
    public byte useMipmaps;
    public boolean hdr;
    public byte textureSpeedV;
    public int combineMode;

    public int id;

    public int getId() {
        return id;
    }
}
