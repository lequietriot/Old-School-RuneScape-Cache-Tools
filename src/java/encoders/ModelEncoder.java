package encoders;

import com.application.GUI;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.models.VertexNormal;
import osrs.ByteBufferUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ModelEncoder {

    private final GUI gui;

    public ModelEncoder(GUI selectedGUI) {
        gui = selectedGUI;
        JFileChooser chooseModel = new JFileChooser();
        chooseModel.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooseModel.setFileFilter(new FileNameExtensionFilter("Model Object", "obj"));
        chooseModel.setMultiSelectionEnabled(true);
        if (chooseModel.showOpenDialog(gui) == JFileChooser.APPROVE_OPTION) {
            File[] files = chooseModel.getSelectedFiles();
            for (File selected : files) {
                if (selected.getName().endsWith(".obj")) {
                    try {
                        encode(selected);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void encode(File selected) throws IOException {
        Path materialPath = Paths.get(selected.getPath().replace(".obj", ".mtl"));
        List<String> materialLines = Files.readAllLines(materialPath);
        List<String> modelLines = Files.readAllLines(selected.toPath());
        ModelDefinition modelDefinition = new ModelDefinition();

        int vCount = 0;
        int vtCount = 0;
        int fCount = 0;
        int fPosition = 0;
        List<Integer> vertexXList = new ArrayList<>();
        List<Integer> vertexYList = new ArrayList<>();
        List<Integer> vertexZList = new ArrayList<>();
        List<VertexNormal> vertexNormals = new ArrayList<>();
        List<Integer> faceXList = new ArrayList<>();
        List<Integer> faceYList = new ArrayList<>();
        List<Integer> faceZList = new ArrayList<>();
        List<Short> materialList = new ArrayList<>();
        List<Short> textureList = new ArrayList<>();
        List<Integer> textureXList = new ArrayList<>();
        List<Integer> textureYList = new ArrayList<>();
        List<Integer> textureZList = new ArrayList<>();

        for (int line = 0; line < modelLines.size(); line++) {

            if (modelLines.get(line).contains("v ")) {
                String[] values = modelLines.get(line).split(" ");
                vertexXList.add((int) Double.parseDouble(values[1]));
                vertexYList.add((int) (Double.parseDouble(values[2]) * -1));
                vertexZList.add((int) (Double.parseDouble(values[3]) * -1));
                vCount++;
            }

            else if (modelLines.get(line).contains("vn ")) {
                String[] values = modelLines.get(line).split(" ");
                VertexNormal vertexNormal = new VertexNormal();
                vertexNormal.x = (int) Double.parseDouble(values[1]);
                vertexNormal.y = (int) Double.parseDouble(values[2]);
                vertexNormal.z = (int) Double.parseDouble(values[3]);
                vertexNormals.add(vertexNormal);
            }
            else if (modelLines.get(line).contains("vt ")) {
                vtCount++;
            }

            else if (modelLines.get(line).contains("f ")) {
                String[] values = modelLines.get(line).split(" ");
                if (values[1].contains("/")) {
                    String[] values1 = values[1].split("/");
                    faceXList.add(Integer.parseInt(values1[1]));

                    String[] values2 = values[2].split("/");
                    faceYList.add(Integer.parseInt(values2[1]));

                    String[] values3 = values[3].split("/");
                    faceZList.add(Integer.parseInt(values3[1]));
                }
                else {
                    faceXList.add(Integer.parseInt(values[1]) - 1);
                    faceYList.add(Integer.parseInt(values[2]) - 1);
                    faceZList.add(Integer.parseInt(values[3]) - 1);
                }
                fCount++;
            }
        }

        for (int line = 0; line < materialLines.size(); line++) {
            if (materialLines.get(line).startsWith("Kd")) {
                String[] values = materialLines.get(line).split(" ");
                int r = (int) (Float.parseFloat(values[1]) * 255.0);
                int g = (int) (Float.parseFloat(values[2]) * 255.0);
                int b = (int) (Float.parseFloat(values[3]) * 255.0);

                float[] hsbColor = Color.RGBtoHSB(r, g, b, null);
                float hue = (hsbColor[0]);
                float saturation = (hsbColor[1]);
                float brightness = (hsbColor[2]);
                int encode_hue = (int) (hue * 63);
                int encode_saturation = (int) (saturation * 7);
                int encode_brightness = (int) (brightness * 80);
                short color = (short) ((encode_hue << 10) + (encode_saturation << 7) + (encode_brightness));
                materialList.add(color);
                textureList.add((short) -1);
                textureXList.add(-1);
                textureYList.add(-1);
                textureZList.add(-1);
                fPosition++;
            }

            /*
            if (materialLines.get(line).startsWith("map_Kd")) {
                String[] values = materialLines.get(line).split(" ");
                if (values[1].contains(".png")) {
                    values[1] = values[1].replace(".png", "");
                }
                textureList.add(Short.parseShort(values[1]));
                textureXList.add(faceXList.get(fPosition));
                textureYList.add(faceYList.get(fPosition));
                textureZList.add(faceZList.get(fPosition));
                materialList.add((short) -1);
                fPosition++;
            }
             */
        }

        modelDefinition.priority = (byte) 0xFF;
        modelDefinition.vertexCount = vCount;
        modelDefinition.vertexX = new int[vCount];
        modelDefinition.vertexY = new int[vCount];
        modelDefinition.vertexZ = new int[vCount];

        for (int index = 0; index < vCount; index++) {
            modelDefinition.vertexX[index] = vertexXList.get(index);
            modelDefinition.vertexY[index] = vertexYList.get(index);
            modelDefinition.vertexZ[index] = vertexZList.get(index);
        }

        modelDefinition.faceCount = fCount;
        modelDefinition.faceColors = new short[fCount];
        if (materialList.size() == fCount) {
            for (int index = 0; index < fCount; index++) {
                modelDefinition.faceColors[index] = materialList.get(index);
            }
        }
        VertexNormal[] vNormals = new VertexNormal[vertexNormals.size()];
        for (int index = 0; index < vNormals.length; index++) {
            vNormals[index] = vertexNormals.get(index);
        }

        modelDefinition.vertexNormals = vNormals;

        modelDefinition.faceTextures = new short[fCount];
        if (textureList.size() == fCount) {
            for (int index = 0; index < fCount; index++) {
                modelDefinition.faceTextures[index] = textureList.get(index);
            }
        }

        modelDefinition.faceIndices1 = new int[fCount];
        modelDefinition.faceIndices2 = new int[fCount];
        modelDefinition.faceIndices3 = new int[fCount];
        for (int index = 0; index < fCount; index++) {
            modelDefinition.faceIndices1[index] = faceXList.get(index);
            modelDefinition.faceIndices2[index] = faceYList.get(index);
            modelDefinition.faceIndices3[index] = faceZList.get(index);
        }

        modelDefinition.texIndices1 = new short[textureXList.size()];
        modelDefinition.texIndices2 = new short[textureYList.size()];
        modelDefinition.texIndices3 = new short[textureZList.size()];
        for (int texture = 0; texture < vtCount / 3; texture++) {
            modelDefinition.texIndices1[texture] = (short) (modelDefinition.faceIndices1[texture]);
            modelDefinition.texIndices2[texture] = (short) (modelDefinition.faceIndices2[texture]);
            modelDefinition.texIndices3[texture] = (short) (modelDefinition.faceIndices3[texture]);
        }

        modelDefinition.numTextureFaces = vtCount / 3;

        File outputFilePath = new File(GUI.cacheLibrary.getPath() + File.separator + "Encoded Data" + File.separator + "Models");
        boolean madeDirectory = outputFilePath.mkdirs();
        if (madeDirectory) {
            JOptionPane.showMessageDialog(gui.getContentPane(), selected.getName() + " was encoded successfully.\nIt can be found in the newly created path: " + outputFilePath.getPath());
        }
        else {
            JOptionPane.showMessageDialog(gui.getContentPane(), selected.getName() + " was encoded successfully.\nIt can be found in the following path: " + outputFilePath.getPath());
        }
        File outputFile = new File(outputFilePath + File.separator + selected.getName().replace(".obj", ".dat").trim());
        DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(outputFile));

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

        modelDefinition.resize(32, 32, 32);

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
                faceTypesBuffer.writeByte(modelDefinition.faceRenderTypes[face]);
            }

            if (hasTrianglePriorities) {
                trianglePrioritiesBuffer.writeByte(modelDefinition.faceRenderPriorities[face]);
            }

            if (hasTriangleAlpha) {
                faceAlphasBuffer.writeByte(modelDefinition.faceTransparencies[face]);
            }

            if (hasTriangleSkins) {
                int weight = modelDefinition.packedVertexGroups[face];
                faceSkinsBuffer.writeByte(weight);
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
                    texturePointerBuffer.writeShort(modelDefinition.texIndices1[face]);
                    texturePointerBuffer.writeShort(modelDefinition.texIndices2[face]);
                    texturePointerBuffer.writeShort(modelDefinition.texIndices3[face]);
                }
            }
        }

        footerBuffer.writeShort(modelDefinition.vertexCount);
        footerBuffer.writeShort(modelDefinition.faceCount);
        footerBuffer.writeByte(modelDefinition.numTextureFaces);
        footerBuffer.writeByte(0);
        footerBuffer.writeByte(10);
        footerBuffer.writeByte(modelDefinition.faceTextures != null ? 1 : 0);
        footerBuffer.writeByte(hasTrianglePriorities ? -1 : modelDefinition.priority);
        footerBuffer.writeBoolean(hasTriangleAlpha);
        footerBuffer.writeBoolean(hasTriangleSkins);
        footerBuffer.writeBoolean(hasVertexLabels);

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
