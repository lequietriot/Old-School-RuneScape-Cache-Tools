package runelite.loaders;

import runelite.definitions.ModelDefinition;
import runelite.io.InputStream;
import runelite.models.VertexNormal;
import runescape.Buffer;

public class ModelLoaderHD {

    public ModelDefinition load(int modelId, byte[] b)
    {
        ModelDefinition def = new ModelDefinition();
        def.id = modelId;

        if (b[b.length - 1] == -1 && b[b.length - 2] == -1)
        {
            decodeNewFormat(def, b);
        }
        else
        {
            decodeOldFormat(def, b);
        }

        def.computeNormals();
        def.computeTextureUVCoordinates();
        def.computeAnimationTables();

        return def;
    }

    private void decodeNewFormat(ModelDefinition def, byte[] data) {
        Buffer first = new Buffer(data);
        Buffer second = new Buffer(data);
        Buffer third = new Buffer(data);
        Buffer fourth = new Buffer(data);
        Buffer fifth = new Buffer(data);
        Buffer sixth = new Buffer(data);
        Buffer seventh = new Buffer(data);
        first.offset = data.length - 23;
        def.vertexCount = first.readUnsignedShort();
        def.faceCount = first.readUnsignedShort();
        def.numTextureFaces = first.readUnsignedByte();
        int i_9 = first.readUnsignedByte();
        boolean hasFaceRenderTypes = (i_9 & 0x1) == 1;
        boolean hasParticleEffects = (i_9 & 0x2) == 2;
        boolean hasBillboards = (i_9 & 0x4) == 4;
        boolean hasVersion = (i_9 & 0x8) == 8;
        int version = 12;
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
        if (def.numTextureFaces > 0) {
            def.textureRenderTypes = new byte[def.numTextureFaces];
            first.offset = 0;

            for (int i = 0; i < def.numTextureFaces; i++) {
                byte b_28 = def.textureRenderTypes[i] = first.readByte();
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

        int totalFaces = def.numTextureFaces;
        int flagBufferOffset = totalFaces;
        totalFaces += def.vertexCount;
        int i_29 = totalFaces;
        if (hasFaceRenderTypes) {
            totalFaces += def.faceCount;
        }

        int i_30 = totalFaces;
        totalFaces += def.faceCount;
        int i_31 = totalFaces;
        if (modelPriority == 255) {
            totalFaces += def.faceCount;
        }

        int i_32 = totalFaces;
        if (hasFaceSkins == 1) {
            totalFaces += def.faceCount;
        }

        int vertSkinsBufferOffset = totalFaces;
        if (hasVertexSkins == 1) {
            totalFaces += def.vertexCount;
        }

        int i_34 = totalFaces;
        if (hasFaceAlpha == 1) {
            totalFaces += def.faceCount;
        }

        int i_35 = totalFaces;
        totalFaces += faceIndices;
        int i_36 = totalFaces;
        if (hasFaceTextures == 1) {
            totalFaces += def.faceCount * 2;
        }

        int i_37 = totalFaces;
        totalFaces += textureIndices;
        int i_38 = totalFaces;
        totalFaces += def.faceCount * 2;
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
        def.vertexX = new int[def.vertexCount];
        def.vertexY = new int[def.vertexCount];
        def.vertexZ = new int[def.vertexCount];
        def.faceIndices1 = new int[def.faceCount];
        def.faceIndices2 = new int[def.faceCount];
        def.faceIndices3 = new int[def.faceCount];
        if (hasVertexSkins == 1) {
            def.packedVertexGroups = new int[def.vertexCount];
        }

        if (hasFaceRenderTypes) {
            def.faceRenderTypes = new byte[def.faceCount];
        }

        if (modelPriority == 255) {
            def.faceRenderPriorities = new byte[def.faceCount];
        } else {
            def.priority = (byte) modelPriority;
        }

        if (hasFaceAlpha == 1) {
            def.faceTransparencies = new byte[def.faceCount];
        }

        if (hasFaceSkins == 1) {
            def.packedTransparencyVertexGroups = new int[def.faceCount];
        }

        if (hasFaceTextures == 1) {
            def.faceTextures = new short[def.faceCount];
        }

        if (hasFaceTextures == 1 && def.numTextureFaces > 0) {
            def.textureCoords = new byte[def.faceCount];
        }

        def.faceColors = new short[def.faceCount];
        if (def.numTextureFaces > 0) {
            def.texIndices1 = new short[def.numTextureFaces];
            def.texIndices2 = new short[def.numTextureFaces];
            def.texIndices3 = new short[def.numTextureFaces];
            if (i_25 > 0) {
                //particleDirectionX = new int[i_25];
                //particleDirectionY = new int[i_25];
                //particleDirectionZ = new int[i_25];
                //particleLifespanX = new byte[i_25];
                //particleLifespanY = new byte[i_25];
                //particleLifespanZ = new int[i_25];
            }

            if (i_26 > 0) {
                def.texturePrimaryColors = new short[i_26];
                //textureSecondaryColor = new int[i_26];
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

        for (int vertex = 0; vertex < def.vertexCount; vertex++) {
            int offsetFlags = first.readUnsignedByte();
            int vertextOffsetX = 0;
            if ((offsetFlags & 0x1) != 0) {
                vertextOffsetX = second.readUShortSmart();
            }

            int vertextOffsetY = 0;
            if ((offsetFlags & 0x2) != 0) {
                vertextOffsetY = third.readUShortSmart();
            }

            int vertetxOffsetZ = 0;
            if ((offsetFlags & 0x4) != 0) {
                vertetxOffsetZ = fourth.readUShortSmart();
            }

            def.vertexX[vertex] = baseX + vertextOffsetX;
            def.vertexY[vertex] = baseY + vertextOffsetY;
            def.vertexZ[vertex] = baseZ + vertetxOffsetZ;
            baseX = def.vertexX[vertex];
            baseY = def.vertexY[vertex];
            baseZ = def.vertexZ[vertex];
            if (hasVertexSkins == 1) {
                def.packedVertexGroups[vertex] = fifth.readUnsignedByte();
            }
        }

        first.offset = i_38;
        second.offset = i_29;
        third.offset = i_31;
        fourth.offset = i_34;
        fifth.offset = i_32;
        sixth.offset = i_36;
        seventh.offset = i_37;

        for (int i_53 = 0; i_53 < def.faceCount; i_53++) {
            def.faceColors[i_53] = (short) first.readUnsignedShort();
            if (hasFaceRenderTypes) {
                def.faceRenderTypes[i_53] = second.readByte();
            }

            if (modelPriority == 255) {
                def.faceRenderPriorities[i_53] = third.readByte();
            }

            if (hasFaceAlpha == 1) {
                def.faceTransparencies[i_53] = fourth.readByte();
            }

            if (hasFaceSkins == 1) {
                def.packedTransparencyVertexGroups[i_53] = fifth.readUnsignedByte();
            }

            if (hasFaceTextures == 1) {
                def.faceTextures[i_53] = (short) (sixth.readUnsignedShort() - 1);
            }

            if (def.textureCoords != null) {
                if (def.faceTextures[i_53] != -1) {
                    def.textureCoords[i_53] = (byte) (seventh.readUnsignedByte() - 1);
                } else {
                    def.textureCoords[i_53] = -1;
                }
            }
        }

        //maxDepth = -1;
        first.offset = i_35;
        second.offset = i_30;
        //calculateMaxDepth(first, second);
        first.offset = simple_tex_pmn_offset;
        second.offset = i_43;
        third.offset = i_45;
        fourth.offset = i_46;
        fifth.offset = i_47;
        sixth.offset = i_48;
        //decodeTexturedTriangles(first, second, third, fourth, fifth, sixth);
        first.offset = totalFaces;
        if (hasParticleEffects) {
            int emitterCount = first.readUnsignedByte();
            if (emitterCount > 0) {
                //particleConfig = new ParticleEmitterConfig[emitterCount];

                for (int i = 0; i < emitterCount; i++) {
                    int particleId = first.readUnsignedShort();
                    int faceIdx = first.readUnsignedShort();
                    byte b_60;
                    if (modelPriority == 255) {
                        b_60 = def.faceRenderPriorities[faceIdx];
                    } else {
                        b_60 = (byte) modelPriority;
                    }

                    //particleConfig[i] = new ParticleEmitterConfig(particleId, triangleX[faceIdx], triangleY[faceIdx], triangleZ[faceIdx], b_60);
                }
            }

            int surfaceSkinCount = first.readUnsignedByte();
            if (surfaceSkinCount > 0) {
                //surfaceSkins = new SurfaceSkin[surfaceSkinCount];

                for (int i = 0; i < surfaceSkinCount; i++) {
                    int x = first.readUnsignedShort();
                    int y = first.readUnsignedShort();
                    //surfaceSkins[i] = new SurfaceSkin(x, y);
                }
            }
        }

        if (hasBillboards) {
            int i_53 = first.readUnsignedByte();
            if (i_53 > 0) {
                def.vertexNormals = new VertexNormal[i_53];

                for (int i = 0; i < i_53; i++) {
                    int vertexOffsetX = first.readUnsignedShort();
                    int vertexOffsetY = first.readUnsignedShort();
                    int vertexOffsetZ = first.readUnsignedByte();
                    byte b_58 = first.readByte();
                    def.vertexNormals[i] = new VertexNormal();
                    def.vertexNormals[i].x = vertexOffsetX;
                    def.vertexNormals[i].y = vertexOffsetY;
                    def.vertexNormals[i].z = vertexOffsetZ;
                    def.vertexNormals[i].magnitude = b_58;
                }
            }
        }
    }

    void decodeOldFormat(ModelDefinition def, byte[] inputData)
    {
        boolean usesFaceRenderTypes = false;
        boolean usesFaceTextures = false;
        InputStream stream1 = new InputStream(inputData);
        InputStream stream2 = new InputStream(inputData);
        InputStream stream3 = new InputStream(inputData);
        InputStream stream4 = new InputStream(inputData);
        InputStream stream5 = new InputStream(inputData);
        stream1.setOffset(inputData.length - 18);
        int vertexCount = stream1.readUnsignedShort();
        int faceCount = stream1.readUnsignedShort();
        int textureCount = stream1.readUnsignedByte();
        int isTextured = stream1.readUnsignedByte();
        int faceRenderPriority = stream1.readUnsignedByte();
        int hasFaceTransparencies = stream1.readUnsignedByte();
        int hasPackedTransparencyVertexGroups = stream1.readUnsignedByte();
        int hasPackedVertexGroups = stream1.readUnsignedByte();
        int vertexXDataByteCount = stream1.readUnsignedShort();
        int vertexYDataByteCount = stream1.readUnsignedShort();
        int vertezZDataByteCount = stream1.readUnsignedShort();
        int faceIndexDataByteCount = stream1.readUnsignedShort();
        byte offsetOfVertexFlags = 0;
        int dataOffset = offsetOfVertexFlags + vertexCount;
        int offsetOfFaceIndexCompressionTypes = dataOffset;
        dataOffset += faceCount;
        int offsetOfFaceRenderPriorities = dataOffset;
        if (faceRenderPriority == 255)
        {
            dataOffset += faceCount;
        }

        int offsetOfPackedTransparencyVertexGroups = dataOffset;
        if (hasPackedTransparencyVertexGroups == 1)
        {
            dataOffset += faceCount;
        }

        int offsetOfFaceTextureFlags = dataOffset;
        if (isTextured == 1)
        {
            dataOffset += faceCount;
        }

        int offsetOfPackedVertexGroups = dataOffset;
        if (hasPackedVertexGroups == 1)
        {
            dataOffset += vertexCount;
        }

        int offsetOfFaceTransparencies = dataOffset;
        if (hasFaceTransparencies == 1)
        {
            dataOffset += faceCount;
        }

        int offsetOfFaceIndexData = dataOffset;
        dataOffset += faceIndexDataByteCount;
        int offsetOfFaceColorsOrFaceTextures = dataOffset;
        dataOffset += faceCount * 2;
        int offsetOfTextureIndices = dataOffset;
        dataOffset += textureCount * 6;
        int offsetOfVertexXData = dataOffset;
        dataOffset += vertexXDataByteCount;
        int offsetOfVertexYData = dataOffset;
        dataOffset += vertexYDataByteCount;
        int offsetOfVertexZData = dataOffset;
        def.vertexCount = vertexCount;
        def.faceCount = faceCount;
        def.numTextureFaces = textureCount;
        def.vertexX = new int[vertexCount];
        def.vertexY = new int[vertexCount];
        def.vertexZ = new int[vertexCount];
        def.faceIndices1 = new int[faceCount];
        def.faceIndices2 = new int[faceCount];
        def.faceIndices3 = new int[faceCount];
        if (textureCount > 0)
        {
            def.textureRenderTypes = new byte[textureCount];
            def.texIndices1 = new short[textureCount];
            def.texIndices2 = new short[textureCount];
            def.texIndices3 = new short[textureCount];
        }

        if (hasPackedVertexGroups == 1)
        {
            def.packedVertexGroups = new int[vertexCount];
        }

        if (isTextured == 1)
        {
            def.faceRenderTypes = new byte[faceCount];
            def.textureCoords = new byte[faceCount];
            def.faceTextures = new short[faceCount];
        }

        if (faceRenderPriority == 255)
        {
            def.faceRenderPriorities = new byte[faceCount];
        }
        else
        {
            def.priority = (byte) faceRenderPriority;
        }

        if (hasFaceTransparencies == 1)
        {
            def.faceTransparencies = new byte[faceCount];
        }

        if (hasPackedTransparencyVertexGroups == 1)
        {
            def.packedTransparencyVertexGroups = new int[faceCount];
        }

        def.faceColors = new short[faceCount];
        stream1.setOffset(offsetOfVertexFlags);
        stream2.setOffset(offsetOfVertexXData);
        stream3.setOffset(offsetOfVertexYData);
        stream4.setOffset(offsetOfVertexZData);
        stream5.setOffset(offsetOfPackedVertexGroups);
        int previousVertexX = 0;
        int previousVertexY = 0;
        int previousVertexZ = 0;

        for (int i = 0; i < vertexCount; ++i)
        {
            int vertexFlags = stream1.readUnsignedByte();
            int deltaX = 0;
            if ((vertexFlags & 1) != 0)
            {
                deltaX = stream2.readShortSmart();
            }

            int deltaY = 0;
            if ((vertexFlags & 2) != 0)
            {
                deltaY = stream3.readShortSmart();
            }

            int deltaZ = 0;
            if ((vertexFlags & 4) != 0)
            {
                deltaZ = stream4.readShortSmart();
            }

            def.vertexX[i] = previousVertexX + deltaX;
            def.vertexY[i] = previousVertexY + deltaY;
            def.vertexZ[i] = previousVertexZ + deltaZ;
            previousVertexX = def.vertexX[i];
            previousVertexY = def.vertexY[i];
            previousVertexZ = def.vertexZ[i];
            if (hasPackedVertexGroups == 1)
            {
                def.packedVertexGroups[i] = stream5.readUnsignedByte();
            }
        }

        stream1.setOffset(offsetOfFaceColorsOrFaceTextures);
        stream2.setOffset(offsetOfFaceTextureFlags);
        stream3.setOffset(offsetOfFaceRenderPriorities);
        stream4.setOffset(offsetOfFaceTransparencies);
        stream5.setOffset(offsetOfPackedTransparencyVertexGroups);

        for (int i = 0; i < faceCount; ++i)
        {
            def.faceColors[i] = (short) stream1.readUnsignedShort();
            if (isTextured == 1)
            {
                int faceTextureFlags = stream2.readUnsignedByte();
                if ((faceTextureFlags & 1) == 1)
                {
                    def.faceRenderTypes[i] = 1;
                    usesFaceRenderTypes = true;
                }
                else
                {
                    def.faceRenderTypes[i] = 0;
                }

                if ((faceTextureFlags & 2) == 2)
                {
                    def.textureCoords[i] = (byte) (faceTextureFlags >> 2);
                    def.faceTextures[i] = def.faceColors[i];
                    def.faceColors[i] = 127;
                    if (def.faceTextures[i] != -1)
                    {
                        usesFaceTextures = true;
                    }
                }
                else
                {
                    def.textureCoords[i] = -1;
                    def.faceTextures[i] = -1;
                }
            }

            if (faceRenderPriority == 255)
            {
                def.faceRenderPriorities[i] = stream3.readByte();
            }

            if (hasFaceTransparencies == 1)
            {
                def.faceTransparencies[i] = stream4.readByte();
            }

            if (hasPackedTransparencyVertexGroups == 1)
            {
                def.packedTransparencyVertexGroups[i] = stream5.readUnsignedByte();
            }
        }

        stream1.setOffset(offsetOfFaceIndexData);
        stream2.setOffset(offsetOfFaceIndexCompressionTypes);
        int previousIndex1 = 0;
        int previousIndex2 = 0;
        int previousIndex3 = 0;

        for (int i = 0; i < faceCount; ++i)
        {
            int faceIndexCompressionType = stream2.readUnsignedByte();
            if (faceIndexCompressionType == 1)
            {
                previousIndex1 = stream1.readShortSmart() + previousIndex3;
                previousIndex2 = stream1.readShortSmart() + previousIndex1;
                previousIndex3 = stream1.readShortSmart() + previousIndex2;
                def.faceIndices1[i] = previousIndex1;
                def.faceIndices2[i] = previousIndex2;
                def.faceIndices3[i] = previousIndex3;
            }

            if (faceIndexCompressionType == 2)
            {
                previousIndex2 = previousIndex3;
                previousIndex3 = stream1.readShortSmart() + previousIndex3;
                def.faceIndices1[i] = previousIndex1;
                def.faceIndices2[i] = previousIndex2;
                def.faceIndices3[i] = previousIndex3;
            }

            if (faceIndexCompressionType == 3)
            {
                previousIndex1 = previousIndex3;
                previousIndex3 = stream1.readShortSmart() + previousIndex3;
                def.faceIndices1[i] = previousIndex1;
                def.faceIndices2[i] = previousIndex2;
                def.faceIndices3[i] = previousIndex3;
            }

            if (faceIndexCompressionType == 4)
            {
                int swap = previousIndex1;
                previousIndex1 = previousIndex2;
                previousIndex2 = swap;
                previousIndex3 = stream1.readShortSmart() + previousIndex3;
                def.faceIndices1[i] = previousIndex1;
                def.faceIndices2[i] = previousIndex2;
                def.faceIndices3[i] = previousIndex3;
            }
        }

        stream1.setOffset(offsetOfTextureIndices);

        for (int i = 0; i < textureCount; ++i)
        {
            def.textureRenderTypes[i] = 0;
            def.texIndices1[i] = (short) stream1.readUnsignedShort();
            def.texIndices2[i] = (short) stream1.readUnsignedShort();
            def.texIndices3[i] = (short) stream1.readUnsignedShort();
        }

        if (def.textureCoords != null)
        {
            boolean usesTextureCoords = false;

            for (int i = 0; i < faceCount; ++i)
            {
                int coord = def.textureCoords[i] & 255;
                if (coord != 255)
                {
                    if (def.faceIndices1[i] == (def.texIndices1[coord] & '\uffff') && def.faceIndices2[i] == (def.texIndices2[coord] & '\uffff') && def.faceIndices3[i] == (def.texIndices3[coord] & '\uffff'))
                    {
                        def.textureCoords[i] = -1;
                    }
                    else
                    {
                        usesTextureCoords = true;
                    }
                }
            }

            if (!usesTextureCoords)
            {
                def.textureCoords = null;
            }
        }

        if (!usesFaceTextures)
        {
            def.faceTextures = null;
        }

        if (!usesFaceRenderTypes)
        {
            def.faceRenderTypes = null;
        }

    }
}
