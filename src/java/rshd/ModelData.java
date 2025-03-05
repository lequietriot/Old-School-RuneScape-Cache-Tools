package rshd;

import osrs.Buffer;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.ParticleEmitterConfig;
import net.runelite.cache.definitions.SurfaceSkin;
import net.runelite.cache.models.VertexNormal;

public class ModelData {

    public int version;
    public int verticesCount;
    public int maxDepth;
    public int faceCount;
    public byte priority;
    public int textureTriangleCount;
    public byte[] textureRenderTypes;
    public int[] verticesX;
    public int[] verticesY;
    public int[] verticesZ;
    public short[] indices1;
    public short[] indices2;
    public short[] indices3;
    public int[] vertexSkins;
    public byte[] faceRenderTypes;
    public byte[] faceRenderPriorities;
    public byte[] faceAlphas;
    public int[] faceSkins;
    public short[] faceTextures;
    public short[] faceColors;
    public byte[] textureCoords;
    public short[] texTriangleX;
    public short[] texTriangleY;
    public short[] texTriangleZ;
    public int[] textureScaleX;
    public int[] textureScaleY;
    public int[] textureScaleZ;
    public byte[] textureRotation;
    public byte[] textureDirection;
    public int[] textureSpeed;
    public int[] textureTransU;
    public int[] textureTransV;
    public ParticleEmitterConfig[] particleConfig;
    public SurfaceSkin[] surfaceSkins;
    public VertexNormal[] isolatedVertexNormals;
    public short[] aShortArray1980;
    public short[] aShortArray1981;

    public ModelDefinition load(int modelId, byte[] data) {

        ModelDefinition modelDefinition = new ModelDefinition();
        modelDefinition.id = modelId;
        modelDefinition.modelData = data;

        if (data[data.length - 1] == -1 && data[data.length - 2] == -1) {
            decodeNewFormat(modelDefinition, data);
            modelDefinition.modelData = (data);
        }
        /*
        if (data[data.length - 1] == -1 && data[data.length - 2] == -1 && data[0] == 1) {
            ByteBuffer dataBuffer = ByteBuffer.allocate(data.length - 1);
            for (int index = 1; index < data.length; index++) {
                dataBuffer.put(data[index]);
            }
            dataBuffer.flip();
            decodeNewFormat(modelDefinition, dataBuffer.array());
            modelDefinition.resize(32, 32, 32);
        }
         */
        else {
            decodeOldFormat(modelDefinition, data);
        }

        return modelDefinition;
    }

    void decodeNewFormat(ModelDefinition modelDefinition, byte[] data) {
        Buffer textureBuffer = new Buffer(data);
        Buffer first = new Buffer(data);
        Buffer second = new Buffer(data);
        Buffer third = new Buffer(data);
        Buffer fourth = new Buffer(data);
        Buffer fifth = new Buffer(data);
        Buffer sixth = new Buffer(data);
        Buffer seventh = new Buffer(data);
        first.offset = data.length - 23;
        modelDefinition.vertexCount = first.readUnsignedShort();
        modelDefinition.faceCount = first.readUnsignedShort();
        modelDefinition.numTextureFaces = first.readUnsignedByte();
        int i_9 = first.readUnsignedByte();
        boolean hasFaceRenderTypes = (i_9 & 0x1) == 1;
        boolean hasParticleEffects = (i_9 & 0x2) == 2;
        boolean hasBillboards = (i_9 & 0x4) == 4;
        boolean hasVersion = (i_9 & 0x8) == 8;
        if (hasVersion) {
            first.offset -= 7;
            modelDefinition.version = first.readUnsignedByte();
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
        modelDefinition.faceTextureFlags = new int[modelDefinition.faceCount];
        if (modelDefinition.numTextureFaces > 0) {
            modelDefinition.textureRenderTypes = new byte[modelDefinition.numTextureFaces];
            first.offset = 0;

            for (int i = 0; i < modelDefinition.numTextureFaces; i++) {
                byte b_28 = modelDefinition.textureRenderTypes[i] = first.readByte();
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

        int totalFaces = modelDefinition.numTextureFaces;
        int flagBufferOffset = totalFaces;
        totalFaces += modelDefinition.vertexCount;
        int i_29 = totalFaces;
        if (hasFaceRenderTypes) {
            totalFaces += modelDefinition.faceCount;
        }

        int i_30 = totalFaces;
        totalFaces += modelDefinition.faceCount;
        int i_31 = totalFaces;
        if (modelPriority == 255) {
            totalFaces += modelDefinition.faceCount;
        }

        int i_32 = totalFaces;
        if (hasFaceSkins == 1) {
            totalFaces += modelDefinition.faceCount;
        }

        int vertSkinsBufferOffset = totalFaces;
        if (hasVertexSkins == 1) {
            totalFaces += modelDefinition.vertexCount;
        }

        int i_34 = totalFaces;
        if (hasFaceAlpha == 1) {
            totalFaces += modelDefinition.faceCount;
        }

        int i_35 = totalFaces;
        totalFaces += faceIndices;
        int i_36 = totalFaces;
        if (hasFaceTextures == 1) {
            totalFaces += modelDefinition.faceCount * 2;
        }
        int i_37 = totalFaces;
        totalFaces += textureIndices;
        int i_38 = totalFaces;
        totalFaces += modelDefinition.faceCount * 2;
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
        if (modelDefinition.version == 14) {
            b_44 = 7;
        } else if (modelDefinition.version >= 15) {
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
        modelDefinition.vertexX = new int[modelDefinition.vertexCount];
        modelDefinition.vertexY = new int[modelDefinition.vertexCount];
        modelDefinition.vertexZ = new int[modelDefinition.vertexCount];
        modelDefinition.faceIndices1 = new int[modelDefinition.faceCount];
        modelDefinition.faceIndices2 = new int[modelDefinition.faceCount];
        modelDefinition.faceIndices3 = new int[modelDefinition.faceCount];
        if (hasVertexSkins == 1) {
            modelDefinition.packedVertexGroups = new int[modelDefinition.vertexCount];
        }

        if (hasFaceRenderTypes) {
            modelDefinition.faceRenderTypes = new byte[modelDefinition.faceCount];
        }

        if (modelPriority == 255) {
            modelDefinition.faceRenderPriorities = new byte[modelDefinition.faceCount];
        } else {
            modelDefinition.priority = (byte) modelPriority;
        }

        if (hasFaceAlpha == 1) {
            modelDefinition.faceTransparencies = new byte[modelDefinition.faceCount];
        }

        if (hasFaceSkins == 1) {
            modelDefinition.packedTransparencyVertexGroups = new int[modelDefinition.faceCount];
        }

        if (hasFaceTextures == 1) {
            modelDefinition.faceTextures = new short[modelDefinition.faceCount];
        }

        if (hasFaceTextures == 1 && modelDefinition.numTextureFaces > 0) {
            modelDefinition.textureCoordinates = new byte[modelDefinition.faceCount];
        }

        modelDefinition.faceColors = new short[modelDefinition.faceCount];
        if (modelDefinition.numTextureFaces > 0) {
            modelDefinition.texIndices1 = new short[modelDefinition.numTextureFaces];
            modelDefinition.texIndices2 = new short[modelDefinition.numTextureFaces];
            modelDefinition.texIndices3 = new short[modelDefinition.numTextureFaces];
            if (i_25 > 0) {
                modelDefinition.textureScaleX = new int[i_25];
                modelDefinition.textureScaleY = new int[i_25];
                modelDefinition.textureScaleZ = new int[i_25];
                modelDefinition.textureRotation = new byte[i_25];
                modelDefinition.textureDirection = new byte[i_25];
                modelDefinition.textureSpeed = new int[i_25];
            }

            if (i_26 > 0) {
                modelDefinition.textureTransU = new int[i_26];
                modelDefinition.textureTransV = new int[i_26];
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

        for (int vertex = 0; vertex < modelDefinition.vertexCount; vertex++) {
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

            modelDefinition.vertexX[vertex] = baseX + vertextOffsetX;
            modelDefinition.vertexY[vertex] = baseY + vertextOffsetY;
            modelDefinition.vertexZ[vertex] = baseZ + vertetxOffsetZ;
            baseX = modelDefinition.vertexX[vertex];
            baseY = modelDefinition.vertexY[vertex];
            baseZ = modelDefinition.vertexZ[vertex];
            if (hasVertexSkins == 1) {
                modelDefinition.packedVertexGroups[vertex] = fifth.readUnsignedByte();
            }
        }

        first.offset = i_38;
        second.offset = i_29;
        third.offset = i_31;
        fourth.offset = i_34;
        fifth.offset = i_32;
        sixth.offset = i_36;
        seventh.offset = i_37;

        for (int i_53 = 0; i_53 < modelDefinition.faceCount; i_53++) {
            modelDefinition.faceColors[i_53] = (short) first.readUnsignedShort();
            if (hasFaceRenderTypes) {
                modelDefinition.faceRenderTypes[i_53] = second.readByte();
            }

            if (modelPriority == 255) {
                modelDefinition.faceRenderPriorities[i_53] = third.readByte();
            }

            if (hasFaceAlpha == 1) {
                modelDefinition.faceTransparencies[i_53] = fourth.readByte();
            }

            if (hasFaceSkins == 1) {
                modelDefinition.packedTransparencyVertexGroups[i_53] = fifth.readUnsignedByte();
            }

            if (hasFaceTextures == 1) {
                modelDefinition.faceTextures[i_53] = (short) (sixth.readUnsignedShort() - 1);
            }

            if (modelDefinition.textureCoordinates != null) {
                if (modelDefinition.faceTextures[i_53] != -1) {
                    modelDefinition.textureCoordinates[i_53] = (byte) (seventh.readUnsignedByte() - 1);
                    modelDefinition.faceTextureFlags[i_53] = 1;
                } else {
                    modelDefinition.textureCoordinates[i_53] = -1;
                    modelDefinition.faceTextureFlags[i_53] = -1;
                }
            }

        }

        maxDepth = -1;
        first.offset = i_35;
        second.offset = i_30;
        calculateMaxDepth(modelDefinition, first, second);
        first.offset = simple_tex_pmn_offset;
        second.offset = i_43;
        third.offset = i_45;
        fourth.offset = i_46;
        fifth.offset = i_47;
        sixth.offset = i_48;
        //decodeTexturedTriangles(modelDefinition, first, second, third, fourth, fifth, sixth);
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
                        b_60 = modelDefinition.faceRenderPriorities[faceIdx];
                    } else {
                        b_60 = (byte) modelPriority;
                    }

                    //particleConfig[i] = new ParticleEmitterConfig(particleId, modelDefinition.vertexX[faceIdx], modelDefinition.vertexY[faceIdx], modelDefinition.vertexZ[faceIdx], b_60);
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
                modelDefinition.vertexNormals = new VertexNormal[i_53];

                for (int i = 0; i < i_53; i++) {
                    int vertextOffsetX = first.readUnsignedShort();
                    int vertextOffsetY = first.readUnsignedShort();
                    int vertetxOffsetZ = first.readUnsignedByte();
                    byte b_58 = first.readByte();
                    modelDefinition.vertexNormals[i] = new VertexNormal(vertextOffsetX, vertextOffsetY, vertetxOffsetZ, b_58);
                }
            }
        }

    }

    void calculateMaxDepth(ModelDefinition modelDefinition, Buffer rsbytebuffer_1, Buffer rsbytebuffer_2) {
        short s_3 = 0;
        short s_4 = 0;
        short s_5 = 0;
        short s_6 = 0;

        for (int i_7 = 0; i_7 < modelDefinition.faceCount; i_7++) {
            int i_8 = rsbytebuffer_2.readUnsignedByte();
            if (i_8 == 1) {
                s_3 = (short) (rsbytebuffer_1.readUnsignedSmart() + s_6);
                s_4 = (short) (rsbytebuffer_1.readUnsignedSmart() + s_3);
                s_5 = (short) (rsbytebuffer_1.readUnsignedSmart() + s_4);
                s_6 = s_5;
                modelDefinition.faceIndices1[i_7] = s_3;
                modelDefinition.faceIndices2[i_7] = s_4;
                modelDefinition.faceIndices3[i_7] = s_5;
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
                modelDefinition.faceIndices1[i_7] = s_3;
                modelDefinition.faceIndices2[i_7] = s_4;
                modelDefinition.faceIndices3[i_7] = s_5;
                if (s_5 > maxDepth) {
                    maxDepth = s_5;
                }
            }

            if (i_8 == 3) {
                s_3 = s_5;
                s_5 = (short) (rsbytebuffer_1.readUnsignedSmart() + s_6);
                s_6 = s_5;
                modelDefinition.faceIndices1[i_7] = s_3;
                modelDefinition.faceIndices2[i_7] = s_4;
                modelDefinition.faceIndices3[i_7] = s_5;
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
                modelDefinition.faceIndices1[i_7] = s_3;
                modelDefinition.faceIndices2[i_7] = s_9;
                modelDefinition.faceIndices3[i_7] = s_5;
                if (s_5 > maxDepth) {
                    maxDepth = s_5;
                }
            }
        }

        ++maxDepth;
    }

    void decodeTexturedTriangles(ModelDefinition modelDefinition, Buffer rsbytebuffer_1, Buffer rsbytebuffer_2, Buffer rsbytebuffer_3, Buffer rsbytebuffer_4, Buffer rsbytebuffer_5, Buffer rsbytebuffer_6) {
        for (int i_7 = 0; i_7 < modelDefinition.numTextureFaces; i_7++) {
            int i_8 = modelDefinition.textureRenderTypes[i_7] & 0xff;
            if (i_8 == 0) {
                modelDefinition.texIndices1[i_7] = (short) rsbytebuffer_1.readUnsignedShort();
                modelDefinition.texIndices2[i_7] = (short) rsbytebuffer_1.readUnsignedShort();
                modelDefinition.texIndices3[i_7] = (short) rsbytebuffer_1.readUnsignedShort();
            }

            if (i_8 == 1) {
                modelDefinition.texIndices1[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                modelDefinition.texIndices2[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                modelDefinition.texIndices3[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                if (modelDefinition.version < 15) {
                    modelDefinition.textureScaleX[i_7] = rsbytebuffer_3.readUnsignedShort();
                    if (modelDefinition.version < 14) {
                        modelDefinition.textureScaleY[i_7] = rsbytebuffer_3.readUnsignedShort();
                    } else {
                        modelDefinition.textureScaleY[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    }

                    modelDefinition.textureScaleZ[i_7] = rsbytebuffer_3.readUnsignedShort();
                } else {
                    modelDefinition.textureScaleX[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    modelDefinition.textureScaleY[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    modelDefinition.textureScaleZ[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                }

                modelDefinition.textureRotation[i_7] = rsbytebuffer_4.readByte();
                modelDefinition.textureDirection[i_7] = rsbytebuffer_5.readByte();
                modelDefinition.textureSpeed[i_7] = rsbytebuffer_6.readByte();
            }

            if (i_8 == 2) {
                modelDefinition.texIndices1[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                modelDefinition.texIndices2[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                modelDefinition.texIndices3[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                if (modelDefinition.version < 15) {
                    modelDefinition.textureScaleX[i_7] = rsbytebuffer_3.readUnsignedShort();
                    if (modelDefinition.version < 14) {
                        modelDefinition.textureScaleY[i_7] = rsbytebuffer_3.readUnsignedShort();
                    } else {
                        modelDefinition.textureScaleY[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    }

                    modelDefinition.textureScaleZ[i_7] = rsbytebuffer_3.readUnsignedShort();
                } else {
                    modelDefinition.textureScaleX[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    modelDefinition.textureScaleY[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    modelDefinition.textureScaleZ[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                }

                modelDefinition.textureRotation[i_7] = rsbytebuffer_4.readByte();
                modelDefinition.textureDirection[i_7] = rsbytebuffer_5.readByte();
                modelDefinition.textureSpeed[i_7] = rsbytebuffer_6.readByte();
                modelDefinition.textureTransU[i_7] = rsbytebuffer_6.readByte();
                modelDefinition.textureTransV[i_7] = rsbytebuffer_6.readByte();
            }

            if (i_8 == 3) {
                modelDefinition.texIndices1[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                modelDefinition.texIndices2[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                modelDefinition.texIndices3[i_7] = (short) rsbytebuffer_2.readUnsignedShort();
                if (modelDefinition.version < 15) {
                    modelDefinition.textureScaleX[i_7] = rsbytebuffer_3.readUnsignedShort();
                    if (modelDefinition.version < 14) {
                        modelDefinition.textureScaleY[i_7] = rsbytebuffer_3.readUnsignedShort();
                    } else {
                        modelDefinition.textureScaleY[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    }

                    modelDefinition.textureScaleZ[i_7] = rsbytebuffer_3.readUnsignedShort();
                } else {
                    modelDefinition.textureScaleX[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    modelDefinition.textureScaleY[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                    modelDefinition.textureScaleZ[i_7] = rsbytebuffer_3.read24BitUnsignedInteger();
                }

                modelDefinition.textureRotation[i_7] = rsbytebuffer_4.readByte();
                modelDefinition.textureDirection[i_7] = rsbytebuffer_5.readByte();
                modelDefinition.textureSpeed[i_7] = rsbytebuffer_6.readByte();
            }
        }

    }

    void decodeOldFormat(ModelDefinition modelDefinition, byte[] bytes_1) {
        boolean bool_2 = false;
        boolean bool_3 = false;
        Buffer rsbytebuffer_4 = new Buffer(bytes_1);
        Buffer rsbytebuffer_5 = new Buffer(bytes_1);
        Buffer rsbytebuffer_6 = new Buffer(bytes_1);
        Buffer rsbytebuffer_7 = new Buffer(bytes_1);
        Buffer rsbytebuffer_8 = new Buffer(bytes_1);
        rsbytebuffer_4.offset = bytes_1.length - 18;
        modelDefinition.vertexCount = rsbytebuffer_4.readUnsignedShort();
        modelDefinition.faceCount = rsbytebuffer_4.readUnsignedShort();
        modelDefinition.numTextureFaces = rsbytebuffer_4.readUnsignedByte();
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
        int i_42 = b_18 + modelDefinition.vertexCount;
        int i_20 = i_42;
        i_42 += modelDefinition.faceCount;
        int i_21 = i_42;
        if (i_10 == 255) {
            i_42 += modelDefinition.faceCount;
        }

        int i_22 = i_42;
        if (i_12 == 1) {
            i_42 += modelDefinition.faceCount;
        }

        int i_23 = i_42;
        if (i_9 == 1) {
            i_42 += modelDefinition.faceCount;
        }

        int i_24 = i_42;
        if (i_13 == 1) {
            i_42 += modelDefinition.vertexCount;
        }

        int i_25 = i_42;
        if (i_11 == 1) {
            i_42 += modelDefinition.faceCount;
        }

        int i_26 = i_42;
        i_42 += i_17;
        int i_27 = i_42;
        i_42 += modelDefinition.faceCount * 2;
        int i_28 = i_42;
        i_42 += modelDefinition.numTextureFaces * 6;
        int i_29 = i_42;
        i_42 += i_14;
        int i_30 = i_42;
        i_42 += i_15;
        int i_10000 = i_42 + i_16;
        modelDefinition.vertexX = new int[modelDefinition.vertexCount];
        modelDefinition.vertexY = new int[modelDefinition.vertexCount];
        modelDefinition.vertexZ = new int[modelDefinition.vertexCount];
        modelDefinition.faceIndices1 = new int[modelDefinition.faceCount];
        modelDefinition.faceIndices2 = new int[modelDefinition.faceCount];
        modelDefinition.faceIndices3 = new int[modelDefinition.faceCount];
        if (modelDefinition.numTextureFaces > 0) {
            modelDefinition.textureRenderTypes = new byte[modelDefinition.numTextureFaces];
            modelDefinition.texIndices1 = new short[modelDefinition.numTextureFaces];
            modelDefinition.texIndices2 = new short[modelDefinition.numTextureFaces];
            modelDefinition.texIndices3 = new short[modelDefinition.numTextureFaces];
        }

        if (i_13 == 1) {
            modelDefinition.packedVertexGroups = new int[modelDefinition.vertexCount];
        }

        if (i_9 == 1) {
            modelDefinition.faceRenderTypes = new byte[modelDefinition.faceCount];
            modelDefinition.textureCoordinates = new byte[modelDefinition.faceCount];
            modelDefinition.faceTextures = new short[modelDefinition.faceCount];
        }

        if (i_10 == 255) {
            modelDefinition.faceRenderPriorities = new byte[modelDefinition.faceCount];
        } else {
            modelDefinition.priority = (byte) i_10;
        }

        if (i_11 == 1) {
            modelDefinition.faceTransparencies = new byte[modelDefinition.faceCount];
        }

        if (i_12 == 1) {
            modelDefinition.packedTransparencyVertexGroups = new int[modelDefinition.faceCount];
        }

        modelDefinition.faceColors = new short[modelDefinition.faceCount];
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
        for (i_35 = 0; i_35 < modelDefinition.vertexCount; i_35++) {
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

            modelDefinition.vertexX[i_35] = i_32 + i_37;
            modelDefinition.vertexY[i_35] = i_33 + i_38;
            modelDefinition.vertexZ[i_35] = i_34 + i_39;
            i_32 = modelDefinition.vertexX[i_35];
            i_33 = modelDefinition.vertexY[i_35];
            i_34 = modelDefinition.vertexZ[i_35];
            if (i_13 == 1) {
                modelDefinition.packedVertexGroups[i_35] = rsbytebuffer_8.readUnsignedByte();
            }
        }

        rsbytebuffer_4.offset = i_27;
        rsbytebuffer_5.offset = i_23;
        rsbytebuffer_6.offset = i_21;
        rsbytebuffer_7.offset = i_25;
        rsbytebuffer_8.offset = i_22;

        modelDefinition.faceTextureFlags = new int[modelDefinition.faceCount];
        for (i_35 = 0; i_35 < modelDefinition.faceCount; i_35++) {
            modelDefinition.faceColors[i_35] = (short) rsbytebuffer_4.readUnsignedShort();
            if (i_9 == 1) {
                i_36 = rsbytebuffer_5.readUnsignedByte();
                modelDefinition.faceTextureFlags[i_35] = i_36;
                if ((i_36 & 0x1) == 1) {
                    modelDefinition.faceRenderTypes[i_35] = 1;
                    bool_2 = true;
                } else {
                    modelDefinition.faceRenderTypes[i_35] = 0;
                }

                if ((i_36 & 0x2) == 2) {
                    modelDefinition.textureCoordinates[i_35] = (byte) (i_36 >> 2);
                    modelDefinition.faceTextures[i_35] = modelDefinition.faceColors[i_35];
                    modelDefinition.faceColors[i_35] = 127;
                    if (modelDefinition.faceTextures[i_35] != -1) {
                        bool_3 = true;
                    }
                } else {
                    modelDefinition.textureCoordinates[i_35] = -1;
                    modelDefinition.faceTextures[i_35] = -1;
                }
            }

            if (i_10 == 255) {
                modelDefinition.faceRenderPriorities[i_35] = rsbytebuffer_6.readByte();
            }

            if (i_11 == 1) {
                modelDefinition.faceTransparencies[i_35] = rsbytebuffer_7.readByte();
            }

            if (i_12 == 1) {
                modelDefinition.packedTransparencyVertexGroups[i_35] = rsbytebuffer_8.readUnsignedByte();
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
        for (i_39 = 0; i_39 < modelDefinition.faceCount; i_39++) {
            i_40 = rsbytebuffer_5.readUnsignedByte();
            if (i_40 == 1) {
                s_43 = (short) (rsbytebuffer_4.readUnsignedSmart() + s_46);
                s_44 = (short) (rsbytebuffer_4.readUnsignedSmart() + s_43);
                s_45 = (short) (rsbytebuffer_4.readUnsignedSmart() + s_44);
                s_46 = s_45;
                modelDefinition.faceIndices1[i_39] = s_43;
                modelDefinition.faceIndices2[i_39] = s_44;
                modelDefinition.faceIndices3[i_39] = s_45;
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
                modelDefinition.faceIndices1[i_39] = s_43;
                modelDefinition.faceIndices2[i_39] = s_44;
                modelDefinition.faceIndices3[i_39] = s_45;
                if (s_45 > maxDepth) {
                    maxDepth = s_45;
                }
            }

            if (i_40 == 3) {
                s_43 = s_45;
                s_45 = (short) (rsbytebuffer_4.readUnsignedSmart() + s_46);
                s_46 = s_45;
                modelDefinition.faceIndices1[i_39] = s_43;
                modelDefinition.faceIndices2[i_39] = s_44;
                modelDefinition.faceIndices3[i_39] = s_45;
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
                modelDefinition.faceIndices1[i_39] = s_43;
                modelDefinition.faceIndices2[i_39] = s_41;
                modelDefinition.faceIndices3[i_39] = s_45;
                if (s_45 > maxDepth) {
                    maxDepth = s_45;
                }
            }
        }

        ++maxDepth;
        rsbytebuffer_4.offset = i_28;

        for (i_39 = 0; i_39 < modelDefinition.numTextureFaces; i_39++) {
            modelDefinition.textureRenderTypes[i_39] = 0;
            modelDefinition.texIndices1[i_39] = (short) rsbytebuffer_4.readUnsignedShort();
            modelDefinition.texIndices2[i_39] = (short) rsbytebuffer_4.readUnsignedShort();
            modelDefinition.texIndices3[i_39] = (short) rsbytebuffer_4.readUnsignedShort();
        }

        if (modelDefinition.textureCoordinates != null) {
            boolean bool_47 = false;

            for (i_40 = 0; i_40 < modelDefinition.faceCount; i_40++) {
                int i_48 = modelDefinition.textureCoordinates[i_40] & 0xff;
                if (i_48 != 255) {
                    if (modelDefinition.faceIndices1[i_40] == (modelDefinition.texIndices1[i_48] & 0xffff) && modelDefinition.faceIndices2[i_40] == (modelDefinition.texIndices2[i_48] & 0xffff) && modelDefinition.faceIndices3[i_40] == (modelDefinition.texIndices3[i_48] & 0xffff)) {
                        modelDefinition.textureCoordinates[i_40] = -1;
                    } else {
                        bool_47 = true;
                    }
                }
            }

            if (!bool_47) {
                modelDefinition.textureCoordinates = null;
            }
        }

        if (!bool_3) {
            modelDefinition.faceTextures = null;
        }

        if (!bool_2) {
            modelDefinition.faceRenderTypes = null;
        }

    }

}
