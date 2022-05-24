package encoders;

import application.GUI;
import com.displee.cache.CacheLibrary;
import rshd.ModelData;
import runelite.definitions.ModelDefinition;
import osrs.ByteBufferUtils;

import java.io.*;
import java.util.Objects;

public class ModelEncoder {

    private final GUI gui;
    private final CacheLibrary cacheLibrary;

    public ModelEncoder(GUI selectedGUI) {
        gui = selectedGUI;
        cacheLibrary = GUI.cacheLibrary;
        int index = selectedGUI.selectedIndex;
        int archive = selectedGUI.selectedArchive;
        int file = selectedGUI.selectedFile;

        for (int archiveid = 0; archiveid < cacheLibrary.index(7).archives().length; archiveid++) {
            System.out.println("encoding " + archiveid);
            try {
                ModelData loader = new ModelData();
                ModelDefinition model = loader.load(archive, Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(cacheLibrary.index(7).archive(archiveid)).file(0)).getData()));
                encode(model, archiveid);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void encode(ModelDefinition modelDefinition, int id) throws IOException {

        modelDefinition.computeNormals();
        modelDefinition.computeTextureUVCoordinates();
        modelDefinition.computeAnimationTables();
        modelDefinition.computeMaxPriority();

        File file = new File("");
        if (!file.exists()) {
            if (file.mkdirs()) {
                System.out.println("made file");
            }
        }
        DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file + File.separator + id + File.separator + "0.dat"));

        ByteArrayOutputStream vertexFlagsBufferStream = new ByteArrayOutputStream();
        ByteArrayOutputStream faceTypesBufferStream = new ByteArrayOutputStream();
        ByteArrayOutputStream faceIndexTypesBufferStream = new ByteArrayOutputStream();
        ByteArrayOutputStream trianglePrioritiesBufferStream = new ByteArrayOutputStream();
        ByteArrayOutputStream faceSkinsBufferStream = new ByteArrayOutputStream();
        ByteArrayOutputStream vertexSkinsBufferStream = new ByteArrayOutputStream();
        ByteArrayOutputStream faceAlphasBufferStream = new ByteArrayOutputStream();
        ByteArrayOutputStream triangleIndicesBufferStream = new ByteArrayOutputStream();
        ByteArrayOutputStream faceColorsBufferStream = new ByteArrayOutputStream();
        ByteArrayOutputStream verticesXBufferStream = new ByteArrayOutputStream();
        ByteArrayOutputStream verticesYBufferStream = new ByteArrayOutputStream();
        ByteArrayOutputStream verticesZBufferStream = new ByteArrayOutputStream();
        ByteArrayOutputStream texturesBufferStream = new ByteArrayOutputStream();
        ByteArrayOutputStream footerBufferStream = new ByteArrayOutputStream();
        
        DataOutputStream vertexFlagsBuffer = new DataOutputStream(vertexFlagsBufferStream);
        DataOutputStream faceTypesBuffer = new DataOutputStream(faceTypesBufferStream);
        DataOutputStream faceIndexTypesBuffer = new DataOutputStream(faceIndexTypesBufferStream);
        DataOutputStream trianglePrioritiesBuffer = new DataOutputStream(trianglePrioritiesBufferStream);
        DataOutputStream faceSkinsBuffer = new DataOutputStream(faceSkinsBufferStream);
        DataOutputStream vertexSkinsBuffer = new DataOutputStream(vertexSkinsBufferStream);
        DataOutputStream faceAlphasBuffer = new DataOutputStream(faceAlphasBufferStream);
        DataOutputStream triangleIndicesBuffer = new DataOutputStream(triangleIndicesBufferStream);
        DataOutputStream faceColorsBuffer = new DataOutputStream(faceColorsBufferStream);
        DataOutputStream verticesXBuffer = new DataOutputStream(verticesXBufferStream);
        DataOutputStream verticesYBuffer = new DataOutputStream(verticesYBufferStream);
        DataOutputStream verticesZBuffer = new DataOutputStream(verticesZBufferStream);
        DataOutputStream texturesBuffer = new DataOutputStream(texturesBufferStream);
        DataOutputStream footerBuffer = new DataOutputStream(footerBufferStream);

        boolean hasVertexLabels = modelDefinition.packedVertexGroups != null;

        int baseX = 0;
        int baseY = 0;
        int baseZ = 0;

        for (int vertex = 0; vertex < modelDefinition.vertexCount; vertex++) {
            int x = modelDefinition.vertexX[vertex];
            int y = modelDefinition.vertexY[vertex];
            int z = modelDefinition.vertexZ[vertex];
            int xOffset = x - baseX;
            int yOffset = y - baseY;
            int zOffset = z - baseZ;
            int flag = 0;
            if (xOffset != 0) {
                ByteBufferUtils.writeUnsignedSmart(xOffset, verticesXBuffer);
                flag |= 0x1;
            }
            if (yOffset != 0) {
                ByteBufferUtils.writeUnsignedSmart(yOffset, verticesYBuffer);
                flag |= 0x2;
            }
            if (zOffset != 0) {
                ByteBufferUtils.writeUnsignedSmart(zOffset, verticesZBuffer);
                flag |= 0x4;
            }

            vertexFlagsBuffer.writeByte(flag & 0xFF);

            modelDefinition.vertexX[vertex] = baseX + xOffset;
            modelDefinition.vertexY[vertex] = baseY + yOffset;
            modelDefinition.vertexZ[vertex] = baseZ + zOffset;
            baseX = modelDefinition.vertexX[vertex];
            baseY = modelDefinition.vertexY[vertex];
            baseZ = modelDefinition.vertexZ[vertex];
            if (hasVertexLabels) {
                int weight = modelDefinition.packedVertexGroups[vertex];
                vertexSkinsBuffer.writeByte(weight & 0xFF);
            }
        }


        boolean hasTriangleInfo = modelDefinition.faceRenderTypes != null;
        boolean hasTrianglePriorities = modelDefinition.faceRenderPriorities != null;
        boolean hasTriangleAlpha = modelDefinition.faceTransparencies != null;
        boolean hasTriangleSkins = modelDefinition.packedTransparencyVertexGroups != null;

        for (int face = 0; face < modelDefinition.faceCount; face++) {

            faceColorsBuffer.writeShort(modelDefinition.faceColors[face] & 0xFFFF);

            if (hasTriangleInfo) {
                faceTypesBuffer.writeByte(modelDefinition.faceRenderTypes[face] & 0xFF);
            }

            if (hasTrianglePriorities) {
                trianglePrioritiesBuffer.writeByte(modelDefinition.faceRenderPriorities[face] & 0xFF);
            }
            if (hasTriangleAlpha) {
                faceAlphasBuffer.writeByte(modelDefinition.faceTransparencies[face] & 0xFF);
            }

            if (hasTriangleSkins) {
                int weight = modelDefinition.packedTransparencyVertexGroups[face];
                faceSkinsBuffer.writeByte(weight & 0xFF);
            }
        }

        int lastA = 0;
        int lastB = 0;
        int lastC = 0;
        int pAcc = 0;

        for (int face = 0; face < modelDefinition.faceCount; face++) {
            int currentA = modelDefinition.faceIndices1[face];
            int currentB = modelDefinition.faceIndices2[face];
            int currentC = modelDefinition.faceIndices3[face];
            if (currentA == lastB && currentB == lastA && currentC != lastC) {
                faceIndexTypesBuffer.writeByte(4 & 0xFF);
                ByteBufferUtils.writeUnsignedSmart(currentC - pAcc, triangleIndicesBuffer);
                int back = lastA;
                lastA = lastB;
                lastB = back;
                pAcc = lastC = currentC;
            } else if (currentA == lastC && currentB == lastB && currentC != lastC) {
                faceIndexTypesBuffer.writeByte(3 & 0xFF);
                ByteBufferUtils.writeUnsignedSmart(currentC - pAcc, triangleIndicesBuffer);
                lastA = lastC;
                pAcc = lastC = currentC;
            } else if (currentA == lastA && currentB == lastC && currentC != lastC) {
                faceIndexTypesBuffer.writeByte(2 & 0xFF);
                ByteBufferUtils.writeUnsignedSmart(currentC - pAcc, triangleIndicesBuffer);
                lastB = lastC;
                pAcc = lastC = currentC;
            } else {
                faceIndexTypesBuffer.writeByte(1 & 0xFF);
                ByteBufferUtils.writeUnsignedSmart(currentA - pAcc, triangleIndicesBuffer);
                ByteBufferUtils.writeUnsignedSmart(currentB  - currentA, triangleIndicesBuffer);
                ByteBufferUtils.writeUnsignedSmart(currentC - currentB, triangleIndicesBuffer);
                lastA = currentA;
                lastB = currentB;
                pAcc = lastC = currentC;
            }
        }

        for (int face = 0; face < modelDefinition.numTextureFaces; face++) {
            texturesBuffer.writeShort(modelDefinition.texIndices1[face] & 0xFFFF);
            texturesBuffer.writeShort(modelDefinition.texIndices2[face] & 0xFFFF);
            texturesBuffer.writeShort(modelDefinition.texIndices3[face] & 0xFFFF);
        }

        footerBuffer.writeShort(modelDefinition.vertexCount & 0xFFFF);
        footerBuffer.writeShort(modelDefinition.faceCount & 0xFFFF);
        footerBuffer.writeByte(modelDefinition.numTextureFaces & 0xFF);

        footerBuffer.writeByte((hasTriangleInfo ? 1 : 0) & 0xFF);
        footerBuffer.writeByte((hasTrianglePriorities ? -1 : modelDefinition.priority) & 0xFF);
        footerBuffer.writeBoolean(hasTriangleAlpha);
        footerBuffer.writeBoolean(hasTriangleSkins);
        footerBuffer.writeBoolean(hasVertexLabels);

        footerBuffer.writeShort(verticesXBuffer.size() & 0xFFFF);
        footerBuffer.writeShort(verticesYBuffer.size() & 0xFFFF);
        footerBuffer.writeShort(verticesZBuffer.size() & 0xFFFF);
        footerBuffer.writeShort(triangleIndicesBuffer.size() & 0xFFFF);

        dataOutputStream.write(vertexFlagsBufferStream.toByteArray());
        dataOutputStream.write(faceIndexTypesBufferStream.toByteArray());
        dataOutputStream.write(trianglePrioritiesBufferStream.toByteArray());
        dataOutputStream.write(faceSkinsBufferStream.toByteArray());
        dataOutputStream.write(faceTypesBufferStream.toByteArray());
        dataOutputStream.write(vertexSkinsBufferStream.toByteArray());
        dataOutputStream.write(faceAlphasBufferStream.toByteArray());
        dataOutputStream.write(triangleIndicesBufferStream.toByteArray());
        dataOutputStream.write(faceColorsBufferStream.toByteArray());
        dataOutputStream.write(texturesBufferStream.toByteArray());
        dataOutputStream.write(verticesXBufferStream.toByteArray());
        dataOutputStream.write(verticesYBufferStream.toByteArray());
        dataOutputStream.write(verticesZBufferStream.toByteArray());
        dataOutputStream.write(footerBufferStream.toByteArray());

        dataOutputStream.flush();
        dataOutputStream.close();

        vertexFlagsBuffer.flush();
        vertexFlagsBuffer.close();

        faceTypesBuffer.flush();
        faceTypesBuffer.close();

        faceIndexTypesBuffer.flush();
        faceIndexTypesBuffer.close();

        trianglePrioritiesBuffer.flush();
        trianglePrioritiesBuffer.close();

        faceSkinsBuffer.flush();
        faceSkinsBuffer.close();

        vertexSkinsBuffer.flush();
        vertexSkinsBuffer.close();

        faceAlphasBuffer.flush();
        faceAlphasBuffer.close();

        triangleIndicesBuffer.flush();
        triangleIndicesBuffer.close();

        faceColorsBuffer.flush();
        faceColorsBuffer.close();

        verticesXBuffer.flush();
        verticesXBuffer.close();

        verticesYBuffer.flush();
        verticesYBuffer.close();

        verticesZBuffer.flush();
        verticesZBuffer.close();

        texturesBuffer.flush();
        texturesBuffer.close();

        footerBuffer.flush();
        footerBuffer.close();
    }

}
