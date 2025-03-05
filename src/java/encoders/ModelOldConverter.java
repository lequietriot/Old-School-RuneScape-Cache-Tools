package encoders;

import com.application.AppConstants;
import com.application.GUI;
import com.displee.cache.CacheLibrary;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.loaders.ModelLoader;
import osrs.ByteBufferUtils;
import rs3.RS3ModelData;
import rshd.ModelData;

import java.io.*;
import java.util.Objects;

public class ModelOldConverter {

    private final GUI gui;
    private final CacheLibrary cacheLibrary;

    public ModelOldConverter(GUI selectedGUI) {
        gui = selectedGUI;
        cacheLibrary = GUI.cacheLibrary;
        int index = selectedGUI.selectedIndex;
        int archive = selectedGUI.selectedArchive;
        int file = selectedGUI.selectedFile;

        System.out.println("encoding " + archive);
        try {
            if (AppConstants.cacheType.equals("RuneScape High Definition")) {
                ModelData loader = new ModelData();
                for (archive = 0; archive < cacheLibrary.index(7).archives().length; archive++) {
                    if (cacheLibrary.index(7).archive(archive) != null) {
                        ModelDefinition model = loader.load(archive, Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(cacheLibrary.index(7).archive(archive)).file(0)).getData()));
                        encode(model, archive);
                    }
                }
            }
            if (AppConstants.cacheType.equals("RuneScape 3")) {
                RS3ModelData loader = new RS3ModelData(archive);
                if (cacheLibrary.index(7).archive(archive) != null) {
                    ModelDefinition model = loader.load(archive, Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(cacheLibrary.index(7).archive(archive)).file(0)).getData()), cacheLibrary);
                    encode(model, archive);
                }
            }
            else {
                //Store store = new Store(new File(cacheLibrary.getPath()));
                //store.load();
                //TextureManager tm = new TextureManager(store);
                //tm.load();

                ModelLoader loader = new ModelLoader();
                ModelDefinition model = loader.load(archive, Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(cacheLibrary.index(index).archive(archive)).file(file)).getData()));
                encode(model, archive);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void encode(ModelDefinition modelDefinition, int id) throws IOException {

        //modelDefinition.resize(16, 16, 16);

        File file = new File(GUI.cacheLibrary.getPath() + File.separator + "Encoded Data" + File.separator + "Models");
        if (!file.exists()) {
            if (file.mkdirs()) {
                System.out.println("made file");
            }
        }

        modelDefinition.computeTextureUVCoordinates();
        modelDefinition.computeNormals();
        modelDefinition.computeMaxPriority();
        modelDefinition.computeAnimationTables();

        DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file + File.separator + id + ".dat"));

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
        ByteArrayOutputStream texturePointerBufferStream = new ByteArrayOutputStream();
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
        DataOutputStream texturePointerBuffer = new DataOutputStream(texturePointerBufferStream);
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

            vertexFlagsBuffer.writeByte(flag);

            modelDefinition.vertexX[vertex] = baseX + xOffset;
            modelDefinition.vertexY[vertex] = baseY + yOffset;
            modelDefinition.vertexZ[vertex] = baseZ + zOffset;
            baseX = modelDefinition.vertexX[vertex];
            baseY = modelDefinition.vertexY[vertex];
            baseZ = modelDefinition.vertexZ[vertex];
            if (hasVertexLabels) {
                int weight = modelDefinition.packedVertexGroups[vertex];
                vertexSkinsBuffer.writeByte(weight);
            }
        }

        boolean hasTriangleInfo = modelDefinition.faceRenderTypes != null;
        boolean hasTrianglePriorities = modelDefinition.faceRenderPriorities != null;
        boolean hasTriangleAlpha = modelDefinition.faceTransparencies != null;
        boolean hasTriangleSkins = modelDefinition.packedVertexGroups != null;

        for (int face = 0; face < modelDefinition.faceCount; face++) {
            faceColorsBuffer.writeShort(modelDefinition.faceColors[face]);
            /*
            if (modelDefinition.faceColors != null) {
                if (modelDefinition.faceTextures != null) {
                    if (modelDefinition.faceTextures[face] == 923) {
                        modelDefinition.faceTextures[face] = -1;
                    }
                    else {
                        modelDefinition.faceTextures[face] = 60;
                    }
                    faceColorsBuffer.writeShort(modelDefinition.faceTextures[face]);
                    if (modelDefinition.faceTextureFlags != null) {
                        texturesBuffer.writeByte(modelDefinition.faceTextureFlags[face] & 0xFF);
                    }
                    else {
                        texturesBuffer.writeByte(0xFFFF);
                    }
                }
                else {
                    faceColorsBuffer.writeShort(modelDefinition.faceColors[face]);
                }
            }
             */

            if (hasTriangleInfo) {
                //faceTypesBuffer.writeByte(modelDefinition.faceRenderTypes[face]);
            }

            if (hasTrianglePriorities) {
                //trianglePrioritiesBuffer.writeByte(modelDefinition.faceRenderPriorities[face]);
            }

            if (hasTriangleAlpha) {
                //faceAlphasBuffer.writeByte(modelDefinition.faceTransparencies[face]);
            }

            if (hasTriangleSkins) {
                int weight = modelDefinition.packedVertexGroups[face];
                //faceSkinsBuffer.writeByte(weight);
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
                faceIndexTypesBuffer.writeByte(4);
                ByteBufferUtils.writeUnsignedSmart(currentC - pAcc, triangleIndicesBuffer);
                int back = lastA;
                lastA = lastB;
                lastB = back;
                pAcc = lastC = currentC;
            } else if (currentA == lastC && currentB == lastB && currentC != lastC) {
                faceIndexTypesBuffer.writeByte(3);
                ByteBufferUtils.writeUnsignedSmart(currentC - pAcc, triangleIndicesBuffer);
                lastA = lastC;
                pAcc = lastC = currentC;
            } else if (currentA == lastA && currentB == lastC && currentC != lastC) {
                faceIndexTypesBuffer.writeByte(2);
                ByteBufferUtils.writeUnsignedSmart(currentC - pAcc, triangleIndicesBuffer);
                lastB = lastC;
                pAcc = lastC = currentC;
            } else {
                faceIndexTypesBuffer.writeByte(1);
                ByteBufferUtils.writeUnsignedSmart(currentA - pAcc, triangleIndicesBuffer);
                ByteBufferUtils.writeUnsignedSmart(currentB  - currentA, triangleIndicesBuffer);
                ByteBufferUtils.writeUnsignedSmart(currentC - currentB, triangleIndicesBuffer);
                lastA = currentA;
                lastB = currentB;
                pAcc = lastC = currentC;
            }

        }

        for (int face = 0; face < modelDefinition.numTextureFaces; face++) {
            if (modelDefinition.faceTextures != null) {
                if (modelDefinition.texIndices1 != null && modelDefinition.texIndices2 != null && modelDefinition.texIndices3 != null) {
                    //texturePointerBuffer.writeShort(modelDefinition.texIndices1[face]);
                    //texturePointerBuffer.writeShort(modelDefinition.texIndices2[face]);
                    //texturePointerBuffer.writeShort(modelDefinition.texIndices3[face]);
                }
            }
        }

        footerBuffer.writeShort(modelDefinition.vertexCount);
        footerBuffer.writeShort(modelDefinition.faceCount);
        footerBuffer.writeByte(0);
        //footerBuffer.writeByte(modelDefinition.numTextureFaces);
        footerBuffer.writeByte(0);
        footerBuffer.writeByte(10);
        //footerBuffer.writeByte(modelDefinition.faceTextures != null ? 1 : 0);
        //footerBuffer.writeByte(hasTrianglePriorities ? -1 : modelDefinition.priority);
        //footerBuffer.writeBoolean(hasTriangleAlpha);
        //footerBuffer.writeBoolean(hasTriangleSkins);
        //footerBuffer.writeBoolean(hasVertexLabels);
        footerBuffer.writeByte(0);
        footerBuffer.writeByte(0);
        footerBuffer.writeByte(0);

        footerBuffer.writeShort(verticesXBufferStream.toByteArray().length);
        footerBuffer.writeShort(verticesYBufferStream.toByteArray().length);
        footerBuffer.writeShort(verticesZBufferStream.toByteArray().length);
        footerBuffer.writeShort(triangleIndicesBufferStream.toByteArray().length);

        dataOutputStream.write(vertexFlagsBufferStream.toByteArray());
        dataOutputStream.write(faceIndexTypesBufferStream.toByteArray());
        dataOutputStream.write(trianglePrioritiesBufferStream.toByteArray());
        dataOutputStream.write(texturesBufferStream.toByteArray());
        dataOutputStream.write(faceSkinsBufferStream.toByteArray());
        dataOutputStream.write(faceTypesBufferStream.toByteArray());
        dataOutputStream.write(vertexSkinsBufferStream.toByteArray());
        dataOutputStream.write(faceAlphasBufferStream.toByteArray());
        dataOutputStream.write(triangleIndicesBufferStream.toByteArray());
        dataOutputStream.write(faceColorsBufferStream.toByteArray());
        dataOutputStream.write(texturePointerBufferStream.toByteArray());
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

        texturesBuffer.flush();
        texturesBuffer.close();

        texturePointerBuffer.flush();
        texturePointerBuffer.close();

        verticesXBuffer.flush();
        verticesXBuffer.close();

        verticesYBuffer.flush();
        verticesYBuffer.close();

        verticesZBuffer.flush();
        verticesZBuffer.close();

        footerBuffer.flush();
        footerBuffer.close();
    }

}
