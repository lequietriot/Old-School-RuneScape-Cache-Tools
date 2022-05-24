package runelite.loaders;

import runelite.definitions.ParticleEmitterConfig;
import runelite.definitions.SurfaceSkin;
import runelite.definitions.Trig;
import runelite.models.FaceNormal;
import runelite.models.VertexNormal;
import osrs.Buffer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ModelLoaderHD {

    public int version = 12;
    public int vertexCount;
    public int maxDepth;
    public int faceCount;
    public byte priority;
    public int texturedFaceCount;
    public byte[] textureRenderTypes;
    public int[] vertexX;
    public int[] vertexY;
    public int[] vertexZ;
    public short[] triangleX;
    public short[] triangleY;
    public short[] triangleZ;
    public int[] vertexSkins;
    public byte[] faceType;
    public byte[] facePriorities;
    public byte[] faceAlphas;
    public int[] textureSkins;
    public short[] faceTextures;
    public short[] faceColor;
    public byte[] texturePos;
    public short[] texTriX;
    public short[] texTriY;
    public short[] texTriZ;
    public int[] particleDirectionX;
    public int[] particleDirectionY;
    public int[] particleDirectionZ;
    public byte[] particleLifespanX;
    public byte[] particleLifespanY;
    public int[] particleLifespanZ;
    public int[] texturePrimaryColor;
    public int[] textureSecondaryColor;
    public ParticleEmitterConfig[] particleConfig;
    public SurfaceSkin[] surfaceSkins;
    public VertexNormal[] isolatedVertexNormals;
    public short[] aShortArray1980;
    public short[] aShortArray1981;
    
    public float[][] faceTextureUCoordinates;
    public float[][] faceTextureVCoordinates;
    private int[] textureCoords;

    private FaceNormal[] faceNormals;

    public ModelLoaderHD(byte[] data) {
        if (data[data.length - 1] == -1 && data[data.length - 2] == -1) {
            decodeNewFormat(data);
        } else {
            decodeOldFormat(data);
        }
    }

    public void computeNormals()
    {
        if (this.isolatedVertexNormals != null)
        {
            return;
        }

        this.isolatedVertexNormals = new VertexNormal[this.vertexCount];

        int var1;
        for (var1 = 0; var1 < this.vertexCount; ++var1)
        {
            this.isolatedVertexNormals[var1] = new VertexNormal();
        }

        for (var1 = 0; var1 < this.faceCount; ++var1)
        {
            int vertexA = this.triangleX[var1];
            int vertexB = this.triangleY[var1];
            int vertexC = this.triangleZ[var1];

            int xA = this.vertexX[vertexB] - this.vertexX[vertexA];
            int yA = this.vertexY[vertexB] - this.vertexY[vertexA];
            int zA = this.vertexZ[vertexB] - this.vertexZ[vertexA];

            int xB = this.vertexX[vertexC] - this.vertexX[vertexA];
            int yB = this.vertexY[vertexC] - this.vertexY[vertexA];
            int zB = this.vertexZ[vertexC] - this.vertexZ[vertexA];

            // Compute cross product
            int var11 = yA * zB - yB * zA;
            int var12 = zA * xB - zB * xA;
            int var13 = xA * yB - xB * yA;

            while (var11 > 8192 || var12 > 8192 || var13 > 8192 || var11 < -8192 || var12 < -8192 || var13 < -8192)
            {
                var11 >>= 1;
                var12 >>= 1;
                var13 >>= 1;
            }

            int length = (int) Math.sqrt((double) (var11 * var11 + var12 * var12 + var13 * var13));
            if (length <= 0)
            {
                length = 1;
            }

            var11 = var11 * 256 / length;
            var12 = var12 * 256 / length;
            var13 = var13 * 256 / length;

            byte var15;
            if (this.faceType == null)
            {
                var15 = 0;
            }
            else
            {
                var15 = this.faceType[var1];
            }

            if (var15 == 0)
            {
                VertexNormal var16 = this.isolatedVertexNormals[vertexA];
                var16.x += var11;
                var16.y += var12;
                var16.z += var13;
                ++var16.magnitude;

                var16 = this.isolatedVertexNormals[vertexB];
                var16.x += var11;
                var16.y += var12;
                var16.z += var13;
                ++var16.magnitude;

                var16 = this.isolatedVertexNormals[vertexC];
                var16.x += var11;
                var16.y += var12;
                var16.z += var13;
                ++var16.magnitude;
            }
            else if (var15 == 1)
            {
                if (this.faceNormals == null)
                {
                    this.faceNormals = new FaceNormal[this.faceCount];
                }

                FaceNormal var17 = this.faceNormals[var1] = new FaceNormal();
                var17.x = var11;
                var17.y = var12;
                var17.z = var13;
            }
            else {
                if (this.faceNormals == null)
                {
                    this.faceNormals = new FaceNormal[this.faceCount];
                }

                FaceNormal var17 = this.faceNormals[var1] = new FaceNormal();
                var17.x = var11;
                var17.y = var12;
                var17.z = var13;
            }
        }
    }

    public void computeTextureUVCoordinates()
    {
        this.faceTextureUCoordinates = new float[faceCount][];
        this.faceTextureVCoordinates = new float[faceCount][];

        for (int i = 0; i < faceCount; i++)
        {
            int textureCoordinate;
            if (textureCoords == null)
            {
                textureCoordinate = -1;
            }
            else
            {
                textureCoordinate = textureCoords[i];
            }

            int textureIdx;
            if (faceTextures == null)
            {
                textureIdx = -1;
            }
            else
            {
                textureIdx = faceTextures[i] & 0xFFFF;
            }

            if (textureIdx != -1)
            {
                float[] u = new float[3];
                float[] v = new float[3];

                if (textureCoordinate == -1)
                {
                    u[0] = 0.0F;
                    v[0] = 1.0F;

                    u[1] = 1.0F;
                    v[1] = 1.0F;

                    u[2] = 0.0F;
                    v[2] = 0.0F;
                }
                else
                {
                    textureCoordinate &= 0xFF;

                    byte textureRenderType = 0;
                    if (textureRenderTypes != null)
                    {
                        textureRenderType = textureRenderTypes[textureCoordinate];
                    }

                    if (textureRenderType == 0)
                    {
                        int faceVertexIdx1 = texTriX[i];
                        int faceVertexIdx2 = texTriY[i];
                        int faceVertexIdx3 = texTriZ[i];

                        short triangleVertexIdx1 = triangleX[textureCoordinate];
                        short triangleVertexIdx2 = triangleY[textureCoordinate];
                        short triangleVertexIdx3 = triangleZ[textureCoordinate];

                        float triangleX = (float) vertexX[triangleVertexIdx1];
                        float triangleY = (float) vertexY[triangleVertexIdx1];
                        float triangleZ = (float) vertexZ[triangleVertexIdx1];

                        float f_882_ = (float) vertexX[triangleVertexIdx2] - triangleX;
                        float f_883_ = (float) vertexY[triangleVertexIdx2] - triangleY;
                        float f_884_ = (float) vertexZ[triangleVertexIdx2] - triangleZ;
                        float f_885_ = (float) vertexX[triangleVertexIdx3] - triangleX;
                        float f_886_ = (float) vertexY[triangleVertexIdx3] - triangleY;
                        float f_887_ = (float) vertexZ[triangleVertexIdx3] - triangleZ;
                        float f_888_ = (float) vertexX[faceVertexIdx1] - triangleX;
                        float f_889_ = (float) vertexY[faceVertexIdx1] - triangleY;
                        float f_890_ = (float) vertexZ[faceVertexIdx1] - triangleZ;
                        float f_891_ = (float) vertexX[faceVertexIdx2] - triangleX;
                        float f_892_ = (float) vertexY[faceVertexIdx2] - triangleY;
                        float f_893_ = (float) vertexZ[faceVertexIdx2] - triangleZ;
                        float f_894_ = (float) vertexX[faceVertexIdx3] - triangleX;
                        float f_895_ = (float) vertexY[faceVertexIdx3] - triangleY;
                        float f_896_ = (float) vertexZ[faceVertexIdx3] - triangleZ;

                        float f_897_ = f_883_ * f_887_ - f_884_ * f_886_;
                        float f_898_ = f_884_ * f_885_ - f_882_ * f_887_;
                        float f_899_ = f_882_ * f_886_ - f_883_ * f_885_;
                        float f_900_ = f_886_ * f_899_ - f_887_ * f_898_;
                        float f_901_ = f_887_ * f_897_ - f_885_ * f_899_;
                        float f_902_ = f_885_ * f_898_ - f_886_ * f_897_;
                        float f_903_ = 1.0F / (f_900_ * f_882_ + f_901_ * f_883_ + f_902_ * f_884_);

                        u[0] = (f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_) * f_903_;
                        u[1] = (f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_) * f_903_;
                        u[2] = (f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_) * f_903_;

                        f_900_ = f_883_ * f_899_ - f_884_ * f_898_;
                        f_901_ = f_884_ * f_897_ - f_882_ * f_899_;
                        f_902_ = f_882_ * f_898_ - f_883_ * f_897_;
                        f_903_ = 1.0F / (f_900_ * f_885_ + f_901_ * f_886_ + f_902_ * f_887_);

                        v[0] = (f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_) * f_903_;
                        v[1] = (f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_) * f_903_;
                        v[2] = (f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_) * f_903_;
                    }
                }

                this.faceTextureUCoordinates[i] = u;
                this.faceTextureVCoordinates[i] = v;
            }
        }
    }

    public ModelLoaderHD(ModelLoaderHD[] arr_1, int i_2) {
        vertexCount = 0;
        faceCount = 0;
        texturedFaceCount = 0;
        int i_3 = 0;
        int i_4 = 0;
        int i_5 = 0;
        boolean bool_6 = false;
        boolean bool_7 = false;
        boolean bool_8 = false;
        boolean bool_9 = false;
        boolean bool_10 = false;
        boolean bool_11 = false;
        priority = -1;

        int i_12;
        for (i_12 = 0; i_12 < i_2; i_12++) {
            ModelLoaderHD rsmesh_22 = arr_1[i_12];
            if (rsmesh_22 != null) {
                vertexCount += rsmesh_22.vertexCount;
                faceCount += rsmesh_22.faceCount;
                texturedFaceCount += rsmesh_22.texturedFaceCount;
                if (rsmesh_22.particleConfig != null) {
                    i_3 += rsmesh_22.particleConfig.length;
                }

                if (rsmesh_22.surfaceSkins != null) {
                    i_4 += rsmesh_22.surfaceSkins.length;
                }

                if (rsmesh_22.isolatedVertexNormals != null) {
                    i_5 += rsmesh_22.isolatedVertexNormals.length;
                }

                bool_6 |= rsmesh_22.faceType != null;
                if (rsmesh_22.facePriorities != null) {
                    bool_7 = true;
                } else {
                    if (priority == -1) {
                        priority = rsmesh_22.priority;
                    }

                    if (priority != rsmesh_22.priority) {
                        bool_7 = true;
                    }
                }

                bool_8 |= rsmesh_22.faceAlphas != null;
                bool_9 |= rsmesh_22.texturePos != null;
                bool_10 |= rsmesh_22.faceTextures != null;
                bool_11 |= rsmesh_22.textureSkins != null;
            }
        }

        vertexX = new int[vertexCount];
        vertexY = new int[vertexCount];
        vertexZ = new int[vertexCount];
        vertexSkins = new int[vertexCount];
        aShortArray1980 = new short[vertexCount];
        triangleX = new short[faceCount];
        triangleY = new short[faceCount];
        triangleZ = new short[faceCount];
        if (bool_6) {
            faceType = new byte[faceCount];
        }

        if (bool_7) {
            facePriorities = new byte[faceCount];
        }

        if (bool_8) {
            faceAlphas = new byte[faceCount];
        }

        if (bool_9) {
            texturePos = new byte[faceCount];
        }

        faceColor = new short[faceCount];
        if (bool_10) {
            faceTextures = new short[faceCount];
        }

        if (bool_11) {
            textureSkins = new int[faceCount];
        }

        aShortArray1981 = new short[faceCount];
        if (texturedFaceCount > 0) {
            textureRenderTypes = new byte[texturedFaceCount];
            texTriX = new short[texturedFaceCount];
            texTriY = new short[texturedFaceCount];
            texTriZ = new short[texturedFaceCount];
            particleDirectionX = new int[texturedFaceCount];
            particleDirectionY = new int[texturedFaceCount];
            particleDirectionZ = new int[texturedFaceCount];
            particleLifespanX = new byte[texturedFaceCount];
            particleLifespanY = new byte[texturedFaceCount];
            particleLifespanZ = new int[texturedFaceCount];
            texturePrimaryColor = new int[texturedFaceCount];
            textureSecondaryColor = new int[texturedFaceCount];
        }

        if (i_5 > 0) {
            isolatedVertexNormals = new VertexNormal[i_5];
        }

        if (i_3 > 0) {
            particleConfig = new ParticleEmitterConfig[i_3];
        }

        if (i_4 > 0) {
            surfaceSkins = new SurfaceSkin[i_4];
        }

        vertexCount = 0;
        faceCount = 0;
        texturedFaceCount = 0;
        i_3 = 0;
        i_4 = 0;
        i_5 = 0;

        int i_16;
        for (i_12 = 0; i_12 < i_2; i_12++) {
            short s_13 = (short) (1 << i_12);
            ModelLoaderHD rsmesh_14 = arr_1[i_12];
            if (rsmesh_14 != null) {
                int i_15;
                if (rsmesh_14.isolatedVertexNormals != null) {
                    for (i_15 = 0; i_15 < rsmesh_14.isolatedVertexNormals.length; i_15++) {
                        VertexNormal class84_21 = rsmesh_14.isolatedVertexNormals[i_15];
                        isolatedVertexNormals[i_5++] = class84_21.method1459(class84_21.y + faceCount);
                    }
                }

                for (i_15 = 0; i_15 < rsmesh_14.faceCount; i_15++) {
                    if (bool_6 && rsmesh_14.faceType != null) {
                        faceType[faceCount] = rsmesh_14.faceType[i_15];
                    }

                    if (bool_7) {
                        if (rsmesh_14.facePriorities != null) {
                            facePriorities[faceCount] = rsmesh_14.facePriorities[i_15];
                        } else {
                            facePriorities[faceCount] = rsmesh_14.priority;
                        }
                    }

                    if (bool_8 && rsmesh_14.faceAlphas != null) {
                        faceAlphas[faceCount] = rsmesh_14.faceAlphas[i_15];
                    }

                    if (bool_10) {
                        if (rsmesh_14.faceTextures != null) {
                            faceTextures[faceCount] = rsmesh_14.faceTextures[i_15];
                        } else {
                            faceTextures[faceCount] = -1;
                        }
                    }

                    if (bool_11) {
                        if (rsmesh_14.textureSkins != null) {
                            textureSkins[faceCount] = rsmesh_14.textureSkins[i_15];
                        } else {
                            textureSkins[faceCount] = -1;
                        }
                    }

                    triangleX[faceCount] = (short) method2657(rsmesh_14, rsmesh_14.triangleX[i_15], s_13);
                    triangleY[faceCount] = (short) method2657(rsmesh_14, rsmesh_14.triangleY[i_15], s_13);
                    triangleZ[faceCount] = (short) method2657(rsmesh_14, rsmesh_14.triangleZ[i_15], s_13);
                    aShortArray1981[faceCount] = s_13;
                    faceColor[faceCount] = rsmesh_14.faceColor[i_15];
                    ++faceCount;
                }

                if (rsmesh_14.particleConfig != null) {
                    for (i_15 = 0; i_15 < rsmesh_14.particleConfig.length; i_15++) {
                        i_16 = method2657(rsmesh_14, rsmesh_14.particleConfig[i_15].faceX, s_13);
                        int i_17 = method2657(rsmesh_14, rsmesh_14.particleConfig[i_15].faceY, s_13);
                        int i_18 = method2657(rsmesh_14, rsmesh_14.particleConfig[i_15].faceZ, s_13);
                        particleConfig[i_3] = rsmesh_14.particleConfig[i_15].method1488(i_16, i_17, i_18);
                        ++i_3;
                    }
                }

                if (rsmesh_14.surfaceSkins != null) {
                    for (i_15 = 0; i_15 < rsmesh_14.surfaceSkins.length; i_15++) {
                        i_16 = method2657(rsmesh_14, rsmesh_14.surfaceSkins[i_15].anInt2119, s_13);
                        surfaceSkins[i_4] = rsmesh_14.surfaceSkins[i_15].method2911(i_16);
                        ++i_4;
                    }
                }
            }
        }

        i_12 = 0;
        maxDepth = vertexCount;

        for (int i_23 = 0; i_23 < i_2; i_23++) {
            short s_19 = (short) (1 << i_23);
            ModelLoaderHD rsmesh_20 = arr_1[i_23];
            if (rsmesh_20 != null) {
                for (i_16 = 0; i_16 < rsmesh_20.faceCount; i_16++) {
                    if (bool_9) {
                        texturePos[i_12++] = (byte) (rsmesh_20.texturePos != null && rsmesh_20.texturePos[i_16] != -1 ? texturedFaceCount + rsmesh_20.texturePos[i_16] : -1);
                    }
                }

                for (i_16 = 0; i_16 < rsmesh_20.texturedFaceCount; i_16++) {
                    byte b_24 = textureRenderTypes[texturedFaceCount] = rsmesh_20.textureRenderTypes[i_16];
                    if (b_24 == 0) {
                        texTriX[texturedFaceCount] = (short) method2657(rsmesh_20, rsmesh_20.texTriX[i_16], s_19);
                        texTriY[texturedFaceCount] = (short) method2657(rsmesh_20, rsmesh_20.texTriY[i_16], s_19);
                        texTriZ[texturedFaceCount] = (short) method2657(rsmesh_20, rsmesh_20.texTriZ[i_16], s_19);
                    }

                    if (b_24 >= 1 && b_24 <= 3) {
                        texTriX[texturedFaceCount] = rsmesh_20.texTriX[i_16];
                        texTriY[texturedFaceCount] = rsmesh_20.texTriY[i_16];
                        texTriZ[texturedFaceCount] = rsmesh_20.texTriZ[i_16];
                        particleDirectionX[texturedFaceCount] = rsmesh_20.particleDirectionX[i_16];
                        particleDirectionY[texturedFaceCount] = rsmesh_20.particleDirectionY[i_16];
                        particleDirectionZ[texturedFaceCount] = rsmesh_20.particleDirectionZ[i_16];
                        particleLifespanX[texturedFaceCount] = rsmesh_20.particleLifespanX[i_16];
                        particleLifespanY[texturedFaceCount] = rsmesh_20.particleLifespanY[i_16];
                        particleLifespanZ[texturedFaceCount] = rsmesh_20.particleLifespanZ[i_16];
                    }

                    if (b_24 == 2) {
                        texturePrimaryColor[texturedFaceCount] = rsmesh_20.texturePrimaryColor[i_16];
                        textureSecondaryColor[texturedFaceCount] = rsmesh_20.textureSecondaryColor[i_16];
                    }

                    ++texturedFaceCount;
                }
            }
        }

    }

    public ModelLoaderHD(int i_1, int i_2, int i_3) {
        vertexX = new int[i_1];
        vertexY = new int[i_1];
        vertexZ = new int[i_1];
        vertexSkins = new int[i_1];
        triangleX = new short[i_2];
        triangleY = new short[i_2];
        triangleZ = new short[i_2];
        faceType = new byte[i_2];
        facePriorities = new byte[i_2];
        faceAlphas = new byte[i_2];
        texturePos = new byte[i_2];
        faceColor = new short[i_2];
        faceTextures = new short[i_2];
        textureSkins = new int[i_2];
        if (i_3 > 0) {
            textureRenderTypes = new byte[i_3];
            texTriX = new short[i_3];
            texTriY = new short[i_3];
            texTriZ = new short[i_3];
            particleDirectionX = new int[i_3];
            particleDirectionY = new int[i_3];
            particleDirectionZ = new int[i_3];
            particleLifespanX = new byte[i_3];
            particleLifespanY = new byte[i_3];
            particleLifespanZ = new int[i_3];
            texturePrimaryColor = new int[i_3];
            textureSecondaryColor = new int[i_3];
        }

    }

    int method2657(ModelLoaderHD rsmesh_1, int i_2, short s_3) {
        int i_4 = rsmesh_1.vertexX[i_2];
        int i_5 = rsmesh_1.vertexY[i_2];
        int i_6 = rsmesh_1.vertexZ[i_2];

        for (int i_7 = 0; i_7 < vertexCount; i_7++) {
            if (i_4 == vertexX[i_7] && i_5 == vertexY[i_7] && i_6 == vertexZ[i_7]) {
                aShortArray1980[i_7] |= s_3;
                return i_7;
            }
        }

        vertexX[vertexCount] = i_4;
        vertexY[vertexCount] = i_5;
        vertexZ[vertexCount] = i_6;
        aShortArray1980[vertexCount] = s_3;
        vertexSkins[vertexCount] = rsmesh_1.vertexSkins != null ? rsmesh_1.vertexSkins[i_2] : -1;
        return vertexCount++;
    }

    void decodeNewFormat(byte[] data) {
        Buffer first = new Buffer(data);
        Buffer second = new Buffer(data);
        Buffer third = new Buffer(data);
        Buffer fourth = new Buffer(data);
        Buffer fifth = new Buffer(data);
        Buffer sixth = new Buffer(data);
        Buffer seventh = new Buffer(data);
        first.offset = data.length - 23;
        vertexCount = first.readUnsignedShort();
        faceCount = first.readUnsignedShort();
        texturedFaceCount = first.readUnsignedByte();
        int i_9 = first.readUnsignedByte();
        boolean hasFaceRenderTypes = (i_9 & 0x1) == 1;
        boolean hasParticleEffects = (i_9 & 0x2) == 2;
        boolean hasBillboards = (i_9 & 0x4) == 4;
        boolean hasVersion = (i_9 & 0x8) == 8;
        if (hasVersion) {
            first.offset -= 7;
            version = first.readUnsignedByte();
            first.offset += 6;
        }

        int modelPriority = first.readUnsignedByte();
        int hasFaceAlpha = first.readUnsignedByte();
        int hasFaceSkins = first.readUnsignedByte();
        int hasFaceTextures = first.readUnsignedByte();
        int hasVertexSkins = first.readUnsignedByte();
        int modelVerticesX = first.readUnsignedShort();
        int modelVerticesY = first.readUnsignedShort();
        int modelVerticesZ = first.readUnsignedShort();
        int faceIndices = first.readUnsignedShort();
        int textureIndices = first.readUnsignedShort();
        int numVertexSkins = 0;
        int i_25 = 0;
        int i_26 = 0;
        if (texturedFaceCount > 0) {
            textureRenderTypes = new byte[texturedFaceCount];
            first.offset = 0;

            for (int i = 0; i < texturedFaceCount; i++) {
                byte b_28 = textureRenderTypes[i] = first.readByte();
                if (b_28 == 0) {
                    ++numVertexSkins;
                }

                if (b_28 >= 1 && b_28 <= 3) {
                    ++i_25;
                }

                if (b_28 == 2) {
                    ++i_26;
                }
            }
        }

        int totalFaces = texturedFaceCount;
        int flagBufferOffset = totalFaces;
        totalFaces += vertexCount;
        int i_29 = totalFaces;
        if (hasFaceRenderTypes) {
            totalFaces += faceCount;
        }

        int i_30 = totalFaces;
        totalFaces += faceCount;
        int i_31 = totalFaces;
        if (modelPriority == 255) {
            totalFaces += faceCount;
        }

        int i_32 = totalFaces;
        if (hasFaceSkins == 1) {
            totalFaces += faceCount;
        }

        int vertSkinsBufferOffset = totalFaces;
        if (hasVertexSkins == 1) {
            totalFaces += vertexCount;
        }

        int i_34 = totalFaces;
        if (hasFaceAlpha == 1) {
            totalFaces += faceCount;
        }

        int i_35 = totalFaces;
        totalFaces += faceIndices;
        int i_36 = totalFaces;
        if (hasFaceTextures == 1) {
            totalFaces += faceCount * 2;
        }

        int i_37 = totalFaces;
        totalFaces += textureIndices;
        int i_38 = totalFaces;
        totalFaces += faceCount * 2;
        int vertXBufferOffset = totalFaces;
        totalFaces += modelVerticesX;
        int vertYBufferOffset = totalFaces;
        totalFaces += modelVerticesY;
        int vertZBufferOffset = totalFaces;
        totalFaces += modelVerticesZ;
        int simple_tex_pmn_offset = totalFaces;
        totalFaces += numVertexSkins * 6;
        int i_43 = totalFaces;
        totalFaces += i_25 * 6;
        byte b_44 = 6;
        if (version == 14) {
            b_44 = 7;
        } else if (version >= 15) {
            b_44 = 9;
        }

        int i_45 = totalFaces;
        totalFaces += i_25 * b_44;
        int i_46 = totalFaces;
        totalFaces += i_25;
        int i_47 = totalFaces;
        totalFaces += i_25;
        int i_48 = totalFaces;
        totalFaces = i_26 * 2 + totalFaces + i_25;
        vertexX = new int[vertexCount];
        vertexY = new int[vertexCount];
        vertexZ = new int[vertexCount];
        triangleX = new short[faceCount];
        triangleY = new short[faceCount];
        triangleZ = new short[faceCount];
        if (hasVertexSkins == 1) {
            vertexSkins = new int[vertexCount];
        }

        if (hasFaceRenderTypes) {
            faceType = new byte[faceCount];
        }

        if (modelPriority == 255) {
            facePriorities = new byte[faceCount];
        } else {
            priority = (byte) modelPriority;
        }

        if (hasFaceAlpha == 1) {
            faceAlphas = new byte[faceCount];
        }

        if (hasFaceSkins == 1) {
            textureSkins = new int[faceCount];
        }

        if (hasFaceTextures == 1) {
            faceTextures = new short[faceCount];
        }

        if (hasFaceTextures == 1 && texturedFaceCount > 0) {
            texturePos = new byte[faceCount];
        }

        faceColor = new short[faceCount];
        if (texturedFaceCount > 0) {
            texTriX = new short[texturedFaceCount];
            texTriY = new short[texturedFaceCount];
            texTriZ = new short[texturedFaceCount];
            if (i_25 > 0) {
                particleDirectionX = new int[i_25];
                particleDirectionY = new int[i_25];
                particleDirectionZ = new int[i_25];
                particleLifespanX = new byte[i_25];
                particleLifespanY = new byte[i_25];
                particleLifespanZ = new int[i_25];
            }

            if (i_26 > 0) {
                texturePrimaryColor = new int[i_26];
                textureSecondaryColor = new int[i_26];
            }
        }

        first.offset = flagBufferOffset;
        second.offset = vertXBufferOffset;
        third.offset = vertYBufferOffset;
        fourth.offset = vertZBufferOffset;
        fifth.offset = vertSkinsBufferOffset;
        int baseX = 0;
        int baseY = 0;
        int baseZ = 0;

        for (int vertex = 0; vertex < vertexCount; vertex++) {
            int offsetFlags = first.readUnsignedByte();
            int vertextOffsetX = 0;
            if ((offsetFlags & 0x1) != 0) {
                vertextOffsetX = second.readUnsignedSmart();
            }

            int vertextOffsetY = 0;
            if ((offsetFlags & 0x2) != 0) {
                vertextOffsetY = third.readUnsignedSmart();
            }

            int vertetxOffsetZ = 0;
            if ((offsetFlags & 0x4) != 0) {
                vertetxOffsetZ = fourth.readUnsignedSmart();
            }

            vertexX[vertex] = baseX + vertextOffsetX;
            vertexY[vertex] = baseY + vertextOffsetY;
            vertexZ[vertex] = baseZ + vertetxOffsetZ;
            baseX = vertexX[vertex];
            baseY = vertexY[vertex];
            baseZ = vertexZ[vertex];
            if (hasVertexSkins == 1) {
                vertexSkins[vertex] = fifth.readUnsignedByte();
            }
        }

        first.offset = i_38;
        second.offset = i_29;
        third.offset = i_31;
        fourth.offset = i_34;
        fifth.offset = i_32;
        sixth.offset = i_36;
        seventh.offset = i_37;

        for (int i_53 = 0; i_53 < faceCount; i_53++) {
            faceColor[i_53] = (short) first.readUnsignedShort();
            if (hasFaceRenderTypes) {
                faceType[i_53] = second.readByte();
            }

            if (modelPriority == 255) {
                facePriorities[i_53] = third.readByte();
            }

            if (hasFaceAlpha == 1) {
                faceAlphas[i_53] = fourth.readByte();
            }

            if (hasFaceSkins == 1) {
                textureSkins[i_53] = fifth.readUnsignedByte();
            }

            if (hasFaceTextures == 1) {
                faceTextures[i_53] = (short) (sixth.readUnsignedShort() - 1);
            }

            if (texturePos != null) {
                if (faceTextures[i_53] != -1) {
                    texturePos[i_53] = (byte) (seventh.readUnsignedByte() - 1);
                } else {
                    texturePos[i_53] = -1;
                }
            }
        }

        maxDepth = -1;
        first.offset = i_35;
        second.offset = i_30;
        calculateMaxDepth(first, second);
        first.offset = simple_tex_pmn_offset;
        second.offset = i_43;
        third.offset = i_45;
        fourth.offset = i_46;
        fifth.offset = i_47;
        sixth.offset = i_48;
        decodeTexturedTriangles(first, second, third, fourth, fifth, sixth);
        first.offset = totalFaces;
        if (hasParticleEffects) {
            int emitterCount = first.readUnsignedByte();
            if (emitterCount > 0) {
                particleConfig = new ParticleEmitterConfig[emitterCount];

                for (int i = 0; i < emitterCount; i++) {
                    int particleId = first.readUnsignedShort();
                    int faceIdx = first.readUnsignedShort();
                    byte b_60;
                    if (modelPriority == 255) {
                        b_60 = facePriorities[faceIdx];
                    } else {
                        b_60 = (byte) modelPriority;
                    }

                    particleConfig[i] = new ParticleEmitterConfig(particleId, triangleX[faceIdx], triangleY[faceIdx], triangleZ[faceIdx], b_60);
                }
            }

            int surfaceSkinCount = first.readUnsignedByte();
            if (surfaceSkinCount > 0) {
                surfaceSkins = new SurfaceSkin[surfaceSkinCount];

                for (int i = 0; i < surfaceSkinCount; i++) {
                    int x = first.readUnsignedShort();
                    int y = first.readUnsignedShort();
                    surfaceSkins[i] = new SurfaceSkin(x, y);
                }
            }
        }

        if (hasBillboards) {
            int i_53 = first.readUnsignedByte();
            if (i_53 > 0) {
                isolatedVertexNormals = new VertexNormal[i_53];

                for (int i = 0; i < i_53; i++) {
                    int vertextOffsetX = first.readUnsignedShort();
                    int vertextOffsetY = first.readUnsignedShort();
                    int vertetxOffsetZ = first.readUnsignedByte();
                    byte b_58 = first.readByte();
                    isolatedVertexNormals[i] = new VertexNormal(vertextOffsetX, vertextOffsetY, vertetxOffsetZ, b_58);
                }
            }
        }

    }

    void calculateMaxDepth(Buffer rsbytebuffer_1, Buffer rsbytebuffer_2) {
        short s_3 = 0;
        short s_4 = 0;
        short s_5 = 0;
        short s_6 = 0;

        for (int i_7 = 0; i_7 < faceCount; i_7++) {
            int i_8 = rsbytebuffer_2.readUnsignedByte();
            if (i_8 == 1) {
                s_3 = (short) (rsbytebuffer_1.readUnsignedSmart() + s_6);
                s_4 = (short) (rsbytebuffer_1.readUnsignedSmart() + s_3);
                s_5 = (short) (rsbytebuffer_1.readUnsignedSmart() + s_4);
                s_6 = s_5;
                triangleX[i_7] = s_3;
                triangleY[i_7] = s_4;
                triangleZ[i_7] = s_5;
                if (s_3 > maxDepth) {
                    maxDepth = s_3;
                }

                if (s_4 > maxDepth) {
                    maxDepth = s_4;
                }

                if (s_5 > maxDepth) {
                    maxDepth = s_5;
                }
            }

            if (i_8 == 2) {
                s_4 = s_5;
                s_5 = (short) (rsbytebuffer_1.readUnsignedSmart() + s_6);
                s_6 = s_5;
                triangleX[i_7] = s_3;
                triangleY[i_7] = s_4;
                triangleZ[i_7] = s_5;
                if (s_5 > maxDepth) {
                    maxDepth = s_5;
                }
            }

            if (i_8 == 3) {
                s_3 = s_5;
                s_5 = (short) (rsbytebuffer_1.readUnsignedSmart() + s_6);
                s_6 = s_5;
                triangleX[i_7] = s_3;
                triangleY[i_7] = s_4;
                triangleZ[i_7] = s_5;
                if (s_5 > maxDepth) {
                    maxDepth = s_5;
                }
            }

            if (i_8 == 4) {
                short s_9 = s_3;
                s_3 = s_4;
                s_4 = s_9;
                s_5 = (short) (rsbytebuffer_1.readUnsignedSmart() + s_6);
                s_6 = s_5;
                triangleX[i_7] = s_3;
                triangleY[i_7] = s_9;
                triangleZ[i_7] = s_5;
                if (s_5 > maxDepth) {
                    maxDepth = s_5;
                }
            }
        }

        ++maxDepth;
    }

    void decodeTexturedTriangles(Buffer rsbytebuffer_1, Buffer rsbytebuffer_2, Buffer rsbytebuffer_3, Buffer rsbytebuffer_4, Buffer rsbytebuffer_5, Buffer rsbytebuffer_6) {
        for (int i_7 = 0; i_7 < texturedFaceCount; i_7++) {
            int i_8 = textureRenderTypes[i_7] & 0xff;
            if (i_8 == 0) {
                texTriX[i_7] = (short) rsbytebuffer_1.readUnsignedShort();
                texTriY[i_7] = (short) rsbytebuffer_1.readUnsignedShort();
                texTriZ[i_7] = (short) rsbytebuffer_1.readUnsignedShort();
            }

            if (i_8 == 1) {
                texTriX[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                texTriY[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                texTriZ[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                if (version < 15) {
                    particleDirectionX[i_7] = rsbytebuffer_3.readUnsignedShort();
                    if (version < 14) {
                        particleDirectionY[i_7] = rsbytebuffer_3.readUnsignedShort();
                    } else {
                        particleDirectionY[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    }

                    particleDirectionZ[i_7] = rsbytebuffer_3.readUnsignedShort();
                } else {
                    particleDirectionX[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    particleDirectionY[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    particleDirectionZ[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                }

                particleLifespanX[i_7] = rsbytebuffer_4.readByte();
                particleLifespanY[i_7] = rsbytebuffer_5.readByte();
                particleLifespanZ[i_7] = rsbytebuffer_6.readByte();
            }

            if (i_8 == 2) {
                texTriX[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                texTriY[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                texTriZ[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                if (version < 15) {
                    particleDirectionX[i_7] = rsbytebuffer_3.readUnsignedShort();
                    if (version < 14) {
                        particleDirectionY[i_7] = rsbytebuffer_3.readUnsignedShort();
                    } else {
                        particleDirectionY[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    }

                    particleDirectionZ[i_7] = rsbytebuffer_3.readUnsignedShort();
                } else {
                    particleDirectionX[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    particleDirectionY[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    particleDirectionZ[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                }

                particleLifespanX[i_7] = rsbytebuffer_4.readByte();
                particleLifespanY[i_7] = rsbytebuffer_5.readByte();
                particleLifespanZ[i_7] = rsbytebuffer_6.readByte();
                texturePrimaryColor[i_7] = rsbytebuffer_6.readByte();
                textureSecondaryColor[i_7] = rsbytebuffer_6.readByte();
            }

            if (i_8 == 3) {
                texTriX[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                texTriY[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                texTriZ[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                if (version < 15) {
                    particleDirectionX[i_7] = rsbytebuffer_3.readUnsignedShort();
                    if (version < 14) {
                        particleDirectionY[i_7] = rsbytebuffer_3.readUnsignedShort();
                    } else {
                        particleDirectionY[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    }

                    particleDirectionZ[i_7] = rsbytebuffer_3.readUnsignedShort();
                } else {
                    particleDirectionX[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    particleDirectionY[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    particleDirectionZ[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                }

                particleLifespanX[i_7] = rsbytebuffer_4.readByte();
                particleLifespanY[i_7] = rsbytebuffer_5.readByte();
                particleLifespanZ[i_7] = rsbytebuffer_6.readByte();
            }
        }

    }

    public int method2662(int i_1, int i_2, int i_3) {
        for (int i_4 = 0; i_4 < vertexCount; i_4++) {
            if (vertexX[i_4] == i_1 && i_2 == vertexY[i_4] && i_3 == vertexZ[i_4]) {
                return i_4;
            }
        }

        vertexX[vertexCount] = i_1;
        vertexY[vertexCount] = i_2;
        vertexZ[vertexCount] = i_3;
        maxDepth = vertexCount + 1;
        return vertexCount++;
    }

    public int method2663(int i_1, int i_2, int i_3, byte b_4, byte b_5, short s_6, byte b_7, short s_8) {
        triangleX[faceCount] = (short) i_1;
        triangleY[faceCount] = (short) i_2;
        triangleZ[faceCount] = (short) i_3;
        faceType[faceCount] = b_4;
        texturePos[faceCount] = b_5;
        faceColor[faceCount] = s_6;
        faceAlphas[faceCount] = b_7;
        faceTextures[faceCount] = s_8;
        return faceCount++;
    }

    public byte method2664() {
        if (texturedFaceCount >= 255) {
            throw new IllegalStateException();
        } else {
            textureRenderTypes[texturedFaceCount] = 3;
            texTriX[texturedFaceCount] = 0;
            texTriY[texturedFaceCount] = 32767;
            texTriZ[texturedFaceCount] = 0;
            particleDirectionX[texturedFaceCount] = 1024;
            particleDirectionY[texturedFaceCount] = 1024;
            particleDirectionZ[texturedFaceCount] = 1024;
            particleLifespanX[texturedFaceCount] = 0;
            particleLifespanY[texturedFaceCount] = 0;
            particleLifespanZ[texturedFaceCount] = 0;
            return (byte) (texturedFaceCount++);
        }
    }

    public int[][] getBones(boolean bool_1) {
        int[] ints_2 = new int[256];
        int i_3 = 0;
        int i_4 = bool_1 ? vertexCount : maxDepth;

        int i_6;
        for (int i_5 = 0; i_5 < i_4; i_5++) {
            i_6 = vertexSkins[i_5];
            if (i_6 >= 0) {
                ++ints_2[i_6];
                if (i_6 > i_3) {
                    i_3 = i_6;
                }
            }
        }

        int[][] ints_8 = new int[i_3 + 1][];

        for (i_6 = 0; i_6 <= i_3; i_6++) {
            ints_8[i_6] = new int[ints_2[i_6]];
            ints_2[i_6] = 0;
        }

        for (i_6 = 0; i_6 < i_4; i_6++) {
            int i_7 = vertexSkins[i_6];
            if (i_7 >= 0) {
                ints_8[i_7][ints_2[i_7]++] = i_6;
            }
        }

        return ints_8;
    }

    public int[][] method2666() {
        int[] ints_1 = new int[256];
        int i_2 = 0;

        int i_4;
        for (int i_3 = 0; i_3 < faceCount; i_3++) {
            i_4 = textureSkins[i_3];
            if (i_4 >= 0) {
                ++ints_1[i_4];
                if (i_4 > i_2) {
                    i_2 = i_4;
                }
            }
        }

        int[][] ints_6 = new int[i_2 + 1][];

        for (i_4 = 0; i_4 <= i_2; i_4++) {
            ints_6[i_4] = new int[ints_1[i_4]];
            ints_1[i_4] = 0;
        }

        for (i_4 = 0; i_4 < faceCount; i_4++) {
            int i_5 = textureSkins[i_4];
            if (i_5 >= 0) {
                ints_6[i_5][ints_1[i_5]++] = i_4;
            }
        }

        return ints_6;
    }

    public int[][] method2667() {
        int[] ints_1 = new int[256];
        int i_2 = 0;

        int i_4;
        for (int i_3 = 0; i_3 < isolatedVertexNormals.length; i_3++) {
            i_4 = isolatedVertexNormals[i_3].z;
            if (i_4 >= 0) {
                ++ints_1[i_4];
                if (i_4 > i_2) {
                    i_2 = i_4;
                }
            }
        }

        int[][] ints_6 = new int[i_2 + 1][];

        for (i_4 = 0; i_4 <= i_2; i_4++) {
            ints_6[i_4] = new int[ints_1[i_4]];
            ints_1[i_4] = 0;
        }

        for (i_4 = 0; i_4 < isolatedVertexNormals.length; i_4++) {
            int i_5 = isolatedVertexNormals[i_4].z;
            if (i_5 >= 0) {
                ints_6[i_5][ints_1[i_5]++] = i_4;
            }
        }

        return ints_6;
    }

    public void recolor(short s_1, short s_2) {
        for (int i_3 = 0; i_3 < faceCount; i_3++) {
            if (faceColor[i_3] == s_1) {
                faceColor[i_3] = s_2;
            }
        }

    }

    public void retexture(short s_1, short s_2) {
        if (faceTextures != null) {
            for (int i_3 = 0; i_3 < faceCount; i_3++) {
                if (faceTextures[i_3] == s_1) {
                    faceTextures[i_3] = s_2;
                }
            }
        }

    }

    public void method2671(int i_1, int i_2, int i_3) {
        int i_4;
        int i_5;
        int i_6;
        int i_7;
        if (i_3 != 0) {
            i_4 = Trig.SINE[i_3];
            i_5 = Trig.COSINE[i_3];

            for (i_6 = 0; i_6 < vertexCount; i_6++) {
                i_7 = i_4 * vertexY[i_6] + i_5 * vertexX[i_6] >> 14;
                vertexY[i_6] = i_5 * vertexY[i_6] - i_4 * vertexX[i_6] >> 14;
                vertexX[i_6] = i_7;
            }
        }

        if (i_1 != 0) {
            i_4 = Trig.SINE[i_1];
            i_5 = Trig.COSINE[i_1];

            for (i_6 = 0; i_6 < vertexCount; i_6++) {
                i_7 = i_5 * vertexY[i_6] - i_4 * vertexZ[i_6] >> 14;
                vertexZ[i_6] = i_4 * vertexY[i_6] + i_5 * vertexZ[i_6] >> 14;
                vertexY[i_6] = i_7;
            }
        }

        if (i_2 != 0) {
            i_4 = Trig.SINE[i_2];
            i_5 = Trig.COSINE[i_2];

            for (i_6 = 0; i_6 < vertexCount; i_6++) {
                i_7 = i_4 * vertexZ[i_6] + i_5 * vertexX[i_6] >> 14;
                vertexZ[i_6] = i_5 * vertexZ[i_6] - i_4 * vertexX[i_6] >> 14;
                vertexX[i_6] = i_7;
            }
        }

    }

    void decodeOldFormat(byte[] bytes_1) {
        boolean bool_2 = false;
        boolean bool_3 = false;
        Buffer rsbytebuffer_4 = new Buffer(bytes_1);
        Buffer rsbytebuffer_5 = new Buffer(bytes_1);
        Buffer rsbytebuffer_6 = new Buffer(bytes_1);
        Buffer rsbytebuffer_7 = new Buffer(bytes_1);
        Buffer rsbytebuffer_8 = new Buffer(bytes_1);
        rsbytebuffer_4.offset = bytes_1.length - 18;
        vertexCount = rsbytebuffer_4.readUnsignedShort();
        faceCount = rsbytebuffer_4.readUnsignedShort();
        texturedFaceCount = rsbytebuffer_4.readUnsignedByte();
        int i_9 = rsbytebuffer_4.readUnsignedByte();
        int i_10 = rsbytebuffer_4.readUnsignedByte();
        int i_11 = rsbytebuffer_4.readUnsignedByte();
        int i_12 = rsbytebuffer_4.readUnsignedByte();
        int i_13 = rsbytebuffer_4.readUnsignedByte();
        int i_14 = rsbytebuffer_4.readUnsignedShort();
        int i_15 = rsbytebuffer_4.readUnsignedShort();
        int i_16 = rsbytebuffer_4.readUnsignedShort();
        int i_17 = rsbytebuffer_4.readUnsignedShort();
        byte b_18 = 0;
        int i_42 = b_18 + vertexCount;
        int i_20 = i_42;
        i_42 += faceCount;
        int i_21 = i_42;
        if (i_10 == 255) {
            i_42 += faceCount;
        }

        int i_22 = i_42;
        if (i_12 == 1) {
            i_42 += faceCount;
        }

        int i_23 = i_42;
        if (i_9 == 1) {
            i_42 += faceCount;
        }

        int i_24 = i_42;
        if (i_13 == 1) {
            i_42 += vertexCount;
        }

        int i_25 = i_42;
        if (i_11 == 1) {
            i_42 += faceCount;
        }

        int i_26 = i_42;
        i_42 += i_17;
        int i_27 = i_42;
        i_42 += faceCount * 2;
        int i_28 = i_42;
        i_42 += texturedFaceCount * 6;
        int i_29 = i_42;
        i_42 += i_14;
        int i_30 = i_42;
        i_42 += i_15;
        int i_10000 = i_42 + i_16;
        vertexX = new int[vertexCount];
        vertexY = new int[vertexCount];
        vertexZ = new int[vertexCount];
        triangleX = new short[faceCount];
        triangleY = new short[faceCount];
        triangleZ = new short[faceCount];
        if (texturedFaceCount > 0) {
            textureRenderTypes = new byte[texturedFaceCount];
            texTriX = new short[texturedFaceCount];
            texTriY = new short[texturedFaceCount];
            texTriZ = new short[texturedFaceCount];
        }

        if (i_13 == 1) {
            vertexSkins = new int[vertexCount];
        }

        if (i_9 == 1) {
            faceType = new byte[faceCount];
            texturePos = new byte[faceCount];
            faceTextures = new short[faceCount];
        }

        if (i_10 == 255) {
            facePriorities = new byte[faceCount];
        } else {
            priority = (byte) i_10;
        }

        if (i_11 == 1) {
            faceAlphas = new byte[faceCount];
        }

        if (i_12 == 1) {
            textureSkins = new int[faceCount];
        }

        faceColor = new short[faceCount];
        rsbytebuffer_4.offset = b_18;
        rsbytebuffer_5.offset = i_29;
        rsbytebuffer_6.offset = i_30;
        rsbytebuffer_7.offset = i_42;
        rsbytebuffer_8.offset = i_24;
        int i_32 = 0;
        int i_33 = 0;
        int i_34 = 0;

        int i_35;
        int i_36;
        int i_39;
        for (i_35 = 0; i_35 < vertexCount; i_35++) {
            i_36 = rsbytebuffer_4.readUnsignedByte();
            int i_37 = 0;
            if ((i_36 & 0x1) != 0) {
                i_37 = rsbytebuffer_5.readUnsignedSmart();
            }

            int i_38 = 0;
            if ((i_36 & 0x2) != 0) {
                i_38 = rsbytebuffer_6.readUnsignedSmart();
            }

            i_39 = 0;
            if ((i_36 & 0x4) != 0) {
                i_39 = rsbytebuffer_7.readUnsignedSmart();
            }

            vertexX[i_35] = i_32 + i_37;
            vertexY[i_35] = i_33 + i_38;
            vertexZ[i_35] = i_34 + i_39;
            i_32 = vertexX[i_35];
            i_33 = vertexY[i_35];
            i_34 = vertexZ[i_35];
            if (i_13 == 1) {
                vertexSkins[i_35] = rsbytebuffer_8.readUnsignedByte();
            }
        }

        rsbytebuffer_4.offset = i_27;
        rsbytebuffer_5.offset = i_23;
        rsbytebuffer_6.offset = i_21;
        rsbytebuffer_7.offset = i_25;
        rsbytebuffer_8.offset = i_22;

        for (i_35 = 0; i_35 < faceCount; i_35++) {
            faceColor[i_35] = (short) rsbytebuffer_4.readUnsignedShort();
            if (i_9 == 1) {
                i_36 = rsbytebuffer_5.readUnsignedByte();
                if ((i_36 & 0x1) == 1) {
                    faceType[i_35] = 1;
                    bool_2 = true;
                } else {
                    faceType[i_35] = 0;
                }

                if ((i_36 & 0x2) == 2) {
                    texturePos[i_35] = (byte) (i_36 >> 2);
                    faceTextures[i_35] = faceColor[i_35];
                    faceColor[i_35] = 127;
                    if (faceTextures[i_35] != -1) {
                        bool_3 = true;
                    }
                } else {
                    texturePos[i_35] = -1;
                    faceTextures[i_35] = -1;
                }
            }

            if (i_10 == 255) {
                facePriorities[i_35] = rsbytebuffer_6.readByte();
            }

            if (i_11 == 1) {
                faceAlphas[i_35] = rsbytebuffer_7.readByte();
            }

            if (i_12 == 1) {
                textureSkins[i_35] = rsbytebuffer_8.readUnsignedByte();
            }
        }

        maxDepth = -1;
        rsbytebuffer_4.offset = i_26;
        rsbytebuffer_5.offset = i_20;
        short s_43 = 0;
        short s_44 = 0;
        short s_45 = 0;
        short s_46 = 0;

        int i_40;
        for (i_39 = 0; i_39 < faceCount; i_39++) {
            i_40 = rsbytebuffer_5.readUnsignedByte();
            if (i_40 == 1) {
                s_43 = (short) (rsbytebuffer_4.readUnsignedSmart() + s_46);
                s_44 = (short) (rsbytebuffer_4.readUnsignedSmart() + s_43);
                s_45 = (short) (rsbytebuffer_4.readUnsignedSmart() + s_44);
                s_46 = s_45;
                triangleX[i_39] = s_43;
                triangleY[i_39] = s_44;
                triangleZ[i_39] = s_45;
                if (s_43 > maxDepth) {
                    maxDepth = s_43;
                }

                if (s_44 > maxDepth) {
                    maxDepth = s_44;
                }

                if (s_45 > maxDepth) {
                    maxDepth = s_45;
                }
            }

            if (i_40 == 2) {
                s_44 = s_45;
                s_45 = (short) (rsbytebuffer_4.readUnsignedSmart() + s_46);
                s_46 = s_45;
                triangleX[i_39] = s_43;
                triangleY[i_39] = s_44;
                triangleZ[i_39] = s_45;
                if (s_45 > maxDepth) {
                    maxDepth = s_45;
                }
            }

            if (i_40 == 3) {
                s_43 = s_45;
                s_45 = (short) (rsbytebuffer_4.readUnsignedSmart() + s_46);
                s_46 = s_45;
                triangleX[i_39] = s_43;
                triangleY[i_39] = s_44;
                triangleZ[i_39] = s_45;
                if (s_45 > maxDepth) {
                    maxDepth = s_45;
                }
            }

            if (i_40 == 4) {
                short s_41 = s_43;
                s_43 = s_44;
                s_44 = s_41;
                s_45 = (short) (rsbytebuffer_4.readUnsignedSmart() + s_46);
                s_46 = s_45;
                triangleX[i_39] = s_43;
                triangleY[i_39] = s_41;
                triangleZ[i_39] = s_45;
                if (s_45 > maxDepth) {
                    maxDepth = s_45;
                }
            }
        }

        ++maxDepth;
        rsbytebuffer_4.offset = i_28;

        for (i_39 = 0; i_39 < texturedFaceCount; i_39++) {
            textureRenderTypes[i_39] = 0;
            texTriX[i_39] = (short) rsbytebuffer_4.readUnsignedShort();
            texTriY[i_39] = (short) rsbytebuffer_4.readUnsignedShort();
            texTriZ[i_39] = (short) rsbytebuffer_4.readUnsignedShort();
        }

        if (texturePos != null) {
            boolean bool_47 = false;

            for (i_40 = 0; i_40 < faceCount; i_40++) {
                int i_48 = texturePos[i_40] & 0xff;
                if (i_48 != 255) {
                    if (triangleX[i_40] == (texTriX[i_48] & 0xffff) && triangleY[i_40] == (texTriY[i_48] & 0xffff) && triangleZ[i_40] == (texTriZ[i_48] & 0xffff)) {
                        texturePos[i_40] = -1;
                    } else {
                        bool_47 = true;
                    }
                }
            }

            if (!bool_47) {
                texturePos = null;
            }
        }

        if (!bool_3) {
            faceTextures = null;
        }

        if (!bool_2) {
            faceType = null;
        }

    }

    public void upscale() {
        int i_2;
        for (i_2 = 0; i_2 < vertexCount; i_2++) {
            vertexX[i_2] <<= 2;
            vertexY[i_2] <<= 2;
            vertexZ[i_2] <<= 2;
        }

        if (texturedFaceCount > 0 && particleDirectionX != null) {
            for (i_2 = 0; i_2 < particleDirectionX.length; i_2++) {
                particleDirectionX[i_2] <<= 2;
                particleDirectionY[i_2] <<= 2;
                if (textureRenderTypes[i_2] != 1) {
                    particleDirectionZ[i_2] <<= 2;
                }
            }
        }

    }

    public void translate(int dx, int dy, int dz) {
        for (int i = 0; i < vertexCount; i++) {
            vertexX[i] += dx;
            vertexY[i] += dy;
            vertexZ[i] += dz;
        }

    }

    public byte[] convertToOldModel() {
        try {
            ByteArrayOutputStream masterBuffer = new ByteArrayOutputStream();
            DataOutputStream vertexFlagsBuffer = new DataOutputStream(masterBuffer);
            DataOutputStream faceTypesBuffer = new DataOutputStream(masterBuffer);
            DataOutputStream faceIndexTypesBuffer = new DataOutputStream(masterBuffer);
            DataOutputStream trianglePrioritiesBuffer = new DataOutputStream(masterBuffer);
            DataOutputStream faceSkinsBuffer = new DataOutputStream(masterBuffer);
            DataOutputStream vertexSkinsBuffer = new DataOutputStream(masterBuffer);
            DataOutputStream faceAlphasBuffer = new DataOutputStream(masterBuffer);
            DataOutputStream triangleIndicesBuffer = new DataOutputStream(masterBuffer);
            DataOutputStream faceColorsBuffer = new DataOutputStream(masterBuffer);
            DataOutputStream verticesXBuffer = new DataOutputStream(masterBuffer);
            DataOutputStream verticesYBuffer = new DataOutputStream(masterBuffer);
            DataOutputStream verticesZBuffer = new DataOutputStream(masterBuffer);
            DataOutputStream texturesBuffer = new DataOutputStream(masterBuffer);
            DataOutputStream footerBuffer = new DataOutputStream(masterBuffer);

            boolean hasVertexLabels = vertexSkins != null;

            int baseX = 0;
            int baseY = 0;
            int baseZ = 0;

            for (int vertex = 0; vertex < vertexCount; vertex++) {
                int x = vertexX[vertex];
                int y = vertexY[vertex];
                int z = vertexZ[vertex];
                int xOffset = x - baseX;
                int yOffset = y - baseY;
                int zOffset = z - baseZ;
                int flag = 0;
                if (xOffset != 0) {
                    verticesXBuffer.write(xOffset);
                    flag |= 0x1;
                }
                if (yOffset != 0) {
                    verticesYBuffer.write(yOffset);
                    flag |= 0x2;
                }
                if (zOffset != 0) {
                    verticesZBuffer.write(zOffset);
                    flag |= 0x4;
                }

                vertexFlagsBuffer.writeByte(flag);

                vertexX[vertex] = baseX + xOffset;
                vertexY[vertex] = baseY + yOffset;
                vertexZ[vertex] = baseZ + zOffset;
                baseX = vertexX[vertex];
                baseY = vertexY[vertex];
                baseZ = vertexZ[vertex];
                if (hasVertexLabels) {
                    int weight = vertexSkins[vertex];
                    vertexSkinsBuffer.writeByte(weight);
                }
            }


            boolean hasTriangleInfo = faceType != null;
            boolean hasTrianglePriorities = facePriorities != null;
            boolean hasTriangleAlpha = faceAlphas != null;
            boolean hasTriangleSkins = textureSkins != null;

            for (int face = 0; face < faceCount; face++) {
                faceColorsBuffer.writeShort(faceColor[face]);

                if (hasTriangleInfo) {
                    faceTypesBuffer.writeByte(faceType[face]);
                }

                if (hasTrianglePriorities) {
                    trianglePrioritiesBuffer.writeByte(facePriorities[face]);
                }
                if (hasTriangleAlpha) {
                    faceAlphasBuffer.writeByte(faceAlphas[face]);
                }

                if (hasTriangleSkins) {
                    int weight = textureSkins[face];
                    faceSkinsBuffer.writeByte(weight);
                }
            }

            int lastA = 0;
            int lastB = 0;
            int lastC = 0;
            int pAcc = 0;

            // share edge info to save space
            for (int face = 0; face < faceCount; face++) {
                int currentA = triangleX[face];
                int currentB = triangleY[face];
                int currentC = triangleZ[face];
                if (currentA == lastB && currentB == lastA && currentC != lastC) {
                    faceIndexTypesBuffer.writeByte(4);
                    triangleIndicesBuffer.write(currentC - pAcc);
                    int back = lastA;
                    lastA = lastB;
                    lastB = back;
                    pAcc = lastC = currentC;
                } else if (currentA == lastC && currentB == lastB && currentC != lastC) {
                    faceIndexTypesBuffer.writeByte(3);
                    triangleIndicesBuffer.write(currentC - pAcc);
                    lastA = lastC;
                    pAcc = lastC = currentC;
                } else if (currentA == lastA && currentB == lastC && currentC != lastC) {
                    faceIndexTypesBuffer.writeByte(2);
                    triangleIndicesBuffer.write(currentC - pAcc);
                    lastB = lastC;
                    pAcc = lastC = currentC;
                } else {
                    faceIndexTypesBuffer.writeByte(1);
                    triangleIndicesBuffer.write(currentA - pAcc);
                    triangleIndicesBuffer.write(currentB - currentA);
                    triangleIndicesBuffer.write(currentC - currentB);
                    lastA = currentA;
                    lastB = currentB;
                    pAcc = lastC = currentC;
                }
            }

            for (int face = 0; face < texturedFaceCount; face++) {
                texturesBuffer.writeShort(texTriX[face]);
                texturesBuffer.writeShort(texTriY[face]);
                texturesBuffer.writeShort(texTriZ[face]);
            }

            footerBuffer.writeShort(vertexCount);
            footerBuffer.writeShort(faceCount);
            footerBuffer.writeByte(texturedFaceCount);

            footerBuffer.writeByte(hasTriangleInfo ? 1 : 0);
            footerBuffer.writeByte(hasTrianglePriorities ? -1 : priority);
            footerBuffer.writeBoolean(hasTriangleAlpha);
            footerBuffer.writeBoolean(hasTriangleSkins);
            footerBuffer.writeBoolean(hasVertexLabels);

            footerBuffer.writeShort(vertexX.length);
            footerBuffer.writeShort(vertexY.length);
            footerBuffer.writeShort(vertexZ.length);
            footerBuffer.writeShort(triangleX.length);

            return masterBuffer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
