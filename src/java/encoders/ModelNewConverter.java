package encoders;

import com.application.AppConstants;
import com.application.GUI;
import com.displee.cache.CacheLibrary;
import net.runelite.cache.fs.Store;
import osrs.ByteBufferUtils;
import rshd.ModelData;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.loaders.ModelLoader;
import net.runelite.cache.managers.TextureManager;

import java.io.*;
import java.util.Objects;

public class ModelNewConverter {

    private final GUI gui;
    private final CacheLibrary cacheLibrary;

    public ModelNewConverter(GUI selectedGUI) {
        gui = selectedGUI;
        cacheLibrary = GUI.cacheLibrary;
        int index = selectedGUI.selectedIndex;
        int archive = selectedGUI.selectedArchive;
        int file = selectedGUI.selectedFile;

        System.out.println("encoding " + archive);
        try {
            if (AppConstants.cacheType.equals("RuneScape High Definition")) {
                ModelData loader = new ModelData();
                if (cacheLibrary.index(7).archive(archive) != null) {
                    ModelDefinition model = loader.load(archive, Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(cacheLibrary.index(7).archive(archive)).file(0)).getData()));
                    encode(model, archive);
                }
            } else {
                TextureManager tm = new TextureManager(new Store(new File(cacheLibrary.getPath())));
                tm.load();

                ModelLoader loader = new ModelLoader();
                ModelDefinition model = loader.load(archive, Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(cacheLibrary.index(index).archive(archive)).file(file)).getData()));
                encode(model, archive);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void encode(ModelDefinition modelDefinition, int id) throws IOException {

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

        ByteArrayOutputStream face_mappings_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream vertex_flags_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream face_types_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream face_index_types_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream face_priorities_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream face_skins_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream vertex_skins_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream face_alphas_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream face_indices_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream face_materials_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream face_textures_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream face_colors_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream vertex_x_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream vertex_y_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream vertex_z_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream simple_textures_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream complex_textures_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream texture_scale_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream texture_rotation_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream texture_direction_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream texture_translation_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream particle_effects_buffer = new ByteArrayOutputStream();
        ByteArrayOutputStream footer_buffer = new ByteArrayOutputStream();

        DataOutputStream faceMappings = new DataOutputStream(face_mappings_buffer);
        DataOutputStream vertexFlags = new DataOutputStream(vertex_flags_buffer);
        DataOutputStream faceTypes = new DataOutputStream(face_types_buffer);
        DataOutputStream faceIndexTypes = new DataOutputStream(face_index_types_buffer);
        DataOutputStream facePriorities = new DataOutputStream(face_priorities_buffer);
        DataOutputStream faceSkins = new DataOutputStream(face_skins_buffer);
        DataOutputStream vertexSkins = new DataOutputStream(vertex_skins_buffer);
        DataOutputStream faceAlphas = new DataOutputStream(face_alphas_buffer);
        DataOutputStream faceIndices = new DataOutputStream(face_indices_buffer);
        DataOutputStream faceMaterials = new DataOutputStream(face_materials_buffer);
        DataOutputStream faceTextures = new DataOutputStream(face_textures_buffer);
        DataOutputStream faceColors = new DataOutputStream(face_colors_buffer);
        DataOutputStream verticesX = new DataOutputStream(vertex_x_buffer);
        DataOutputStream verticesY = new DataOutputStream(vertex_y_buffer);
        DataOutputStream verticesZ = new DataOutputStream(vertex_z_buffer);
        DataOutputStream simpleTextures = new DataOutputStream(simple_textures_buffer);
        DataOutputStream complexTextures = new DataOutputStream(complex_textures_buffer);
        DataOutputStream textureScales = new DataOutputStream(texture_scale_buffer);
        DataOutputStream textureRotations = new DataOutputStream(texture_rotation_buffer);
        DataOutputStream textureDirections = new DataOutputStream(texture_direction_buffer);
        DataOutputStream textureTranslations = new DataOutputStream(texture_translation_buffer);
        DataOutputStream particleEffects = new DataOutputStream(particle_effects_buffer);
        DataOutputStream footer = new DataOutputStream(footer_buffer);

        if (modelDefinition.numTextureFaces > 0) {
            for (int face = 0; face < modelDefinition.numTextureFaces; face++) {
                faceMappings.writeByte(modelDefinition.textureRenderTypes[face]);
            }
        }

        boolean hasVertexSkins = modelDefinition.packedVertexGroups != null;
        boolean hasExtendedVertexSkins = false;

        int baseX = 0;
        int baseY = 0;
        int baseZ = 0;

        for (int vertex = 0; vertex < modelDefinition.vertexCount; vertex++) {
            int x = modelDefinition.vertexX[vertex];
            int y = modelDefinition.vertexY[vertex];
            int z = modelDefinition.vertexZ[vertex];
            int xoff = x - baseX;
            int yoff = y - baseY;
            int zoff = z - baseZ;
            int flag = 0;
            if (xoff != 0) {
                ByteBufferUtils.writeUnsignedSmart(xoff, verticesX);
                flag |= 0x1;
            }
            if (yoff != 0) {
                ByteBufferUtils.writeUnsignedSmart(yoff, verticesY);
                flag |= 0x2;
            }
            if (zoff != 0) {
                ByteBufferUtils.writeUnsignedSmart(zoff, verticesZ);
                flag |= 0x4;
            }
            vertexFlags.writeByte(flag);
            modelDefinition.vertexX[vertex] = baseX + xoff;
            modelDefinition.vertexY[vertex] = baseY + yoff;
            modelDefinition.vertexZ[vertex] = baseZ + zoff;
            baseX = modelDefinition.vertexX[vertex];
            baseY = modelDefinition.vertexY[vertex];
            baseZ = modelDefinition.vertexZ[vertex];
            if (hasVertexSkins) {
                int weight = modelDefinition.packedVertexGroups[vertex];
                if (weight >= -1 && weight <= 254) {
                    vertexSkins.writeByte(weight);
                } else {
                    ByteBufferUtils.writeSmart(weight + 1, vertexSkins);
                    hasExtendedVertexSkins = true;
                }
            }
        }
        boolean hasFaceTypes = modelDefinition.faceRenderTypes != null;
        boolean hasFacePriorities = modelDefinition.faceRenderPriorities != null;
        boolean hasFaceAlpha = modelDefinition.faceTransparencies != null;
        boolean hasFaceSkins = modelDefinition.packedVertexGroups != null;
        boolean hasExtendedFaceSkins = false;
        boolean hasFaceTextures = modelDefinition.faceTextures != null;

        for (int face = 0; face < modelDefinition.faceCount; face++) {

            if (modelDefinition.faceTextures[face] == 923) {
                modelDefinition.faceTextures[face] = 7;
            }
            if (modelDefinition.faceTextures[face] == 951) {
                modelDefinition.faceTextures[face] = 8;
            }
            if (modelDefinition.faceTextures[face] == 952) {
                modelDefinition.faceTextures[face] = 8;
            }
            if (modelDefinition.faceTextures[face] == 953) {
                modelDefinition.faceTextures[face] = 8;
            }
            if (modelDefinition.faceTextures[face] == 956) {
                modelDefinition.faceTextures[face] = 8;
            }

            faceColors.writeShort(modelDefinition.faceColors[face]);
            if (hasFaceTypes) {
                faceTypes.writeByte(modelDefinition.faceRenderTypes[face]);
            }
            if (hasFacePriorities) {
                facePriorities.writeByte(modelDefinition.faceRenderPriorities[face]);
            }
            if (hasFaceAlpha) {
                faceAlphas.writeByte(modelDefinition.faceTransparencies[face]);
            }
            if (hasFaceSkins) {
                int weight = modelDefinition.packedVertexGroups[face];
                if (weight >= -1 && weight <= 254) {
                    faceSkins.writeByte(weight);
                } else {
                    ByteBufferUtils.writeSmart(weight + 1, faceSkins);
                    hasExtendedFaceSkins = true;
                }
            }
            if (hasFaceTextures) {
                faceMaterials.writeShort(modelDefinition.faceTextures[face] + 1);
            }
            if (modelDefinition.faceTextures != null) {
                if (modelDefinition.faceTextures[face] != -1) {
                    faceTextures.writeByte(modelDefinition.faceTextures[face] + 1);
                }
            }
        }

        encodeIndices(modelDefinition, faceIndices, faceIndexTypes);
        encodeMapping(modelDefinition, simpleTextures, complexTextures, textureScales, textureRotations, textureDirections, textureTranslations);

        footer.writeShort(modelDefinition.vertexCount);
        footer.writeShort(modelDefinition.faceCount);
        footer.writeByte(modelDefinition.numTextureFaces);
        int flags = 0;
        if (hasFaceTypes) {
            flags |= 0x1;
        }
        if (hasExtendedVertexSkins) {
            flags |= 0x10;
        }
        if (hasExtendedFaceSkins) {
            flags |= 0x20;
        }
        footer.writeByte(flags);
        footer.writeByte(hasFacePriorities ? -1 : modelDefinition.priority);
        footer.writeBoolean(hasFaceAlpha);
        footer.writeBoolean(hasFaceSkins);
        footer.writeBoolean(hasFaceTextures);
        footer.writeBoolean(hasVertexSkins);
        footer.writeShort(vertex_x_buffer.size());
        footer.writeShort(vertex_y_buffer.size());
        footer.writeShort(vertex_z_buffer.size());
        footer.writeShort(face_indices_buffer.size());
        footer.writeShort(face_textures_buffer.size());
        if (hasExtendedVertexSkins) {
            footer.writeShort(vertex_skins_buffer.size());
        }
        if (hasExtendedFaceSkins) {
            footer.writeShort(face_skins_buffer.size());
        }
        footer.writeByte(255);
        footer.writeByte(255);

        dataOutputStream.write(face_mappings_buffer.toByteArray());
        dataOutputStream.write(vertex_flags_buffer.toByteArray());
        dataOutputStream.write(face_types_buffer.toByteArray());
        dataOutputStream.write(face_index_types_buffer.toByteArray());
        dataOutputStream.write(face_priorities_buffer.toByteArray());
        dataOutputStream.write(face_skins_buffer.toByteArray());
        dataOutputStream.write(vertex_skins_buffer.toByteArray());
        dataOutputStream.write(face_alphas_buffer.toByteArray());
        dataOutputStream.write(face_indices_buffer.toByteArray());
        dataOutputStream.write(face_materials_buffer.toByteArray());
        dataOutputStream.write(face_textures_buffer.toByteArray());
        dataOutputStream.write(face_colors_buffer.toByteArray());
        dataOutputStream.write(vertex_x_buffer.toByteArray());
        dataOutputStream.write(vertex_y_buffer.toByteArray());
        dataOutputStream.write(vertex_z_buffer.toByteArray());
        dataOutputStream.write(simple_textures_buffer.toByteArray());
        dataOutputStream.write(complex_textures_buffer.toByteArray());
        dataOutputStream.write(texture_scale_buffer.toByteArray());
        dataOutputStream.write(texture_rotation_buffer.toByteArray());
        dataOutputStream.write(texture_direction_buffer.toByteArray());
        dataOutputStream.write(texture_translation_buffer.toByteArray());
        dataOutputStream.write(particle_effects_buffer.toByteArray());
        dataOutputStream.write(footer_buffer.toByteArray());

        faceMappings.flush();
        faceMappings.close();

        vertexFlags.flush();
        vertexFlags.close();

        faceTypes.flush();
        faceTypes.close();

        faceIndexTypes.flush();
        faceIndexTypes.close();

        facePriorities.flush();
        facePriorities.close();

        faceSkins.flush();
        faceSkins.close();

        vertexSkins.flush();
        vertexSkins.close();

        faceAlphas.flush();
        faceAlphas.close();

        faceIndices.flush();
        faceIndices.close();

        faceMaterials.flush();
        faceMaterials.close();

        faceTextures.flush();
        faceTextures.close();

        faceColors.flush();
        faceColors.close();

        verticesX.flush();
        verticesX.close();

        verticesY.flush();
        verticesY.close();

        verticesZ.flush();
        verticesZ.close();

        simpleTextures.flush();
        simpleTextures.close();

        complexTextures.flush();
        complexTextures.close();

        textureScales.flush();
        textureScales.close();

        textureRotations.flush();
        textureRotations.close();

        textureDirections.flush();
        textureDirections.close();

        textureTranslations.flush();
        textureTranslations.close();

        particleEffects.flush();
        particleEffects.close();

        footer.flush();
        footer.close();

        dataOutputStream.flush();
        dataOutputStream.close();
    }

    private void encodeMapping(ModelDefinition modelDefinition, DataOutputStream simple, DataOutputStream complex, DataOutputStream scale, DataOutputStream rotation, DataOutputStream direction, DataOutputStream translation) throws IOException {
        for (int face = 0; face < modelDefinition.numTextureFaces; face++) {
            int type = modelDefinition.textureRenderTypes[face] & 0xff;
            if (type == 0) {
                simple.writeShort(modelDefinition.texIndices1[face]);
                simple.writeShort(modelDefinition.texIndices2[face]);
                simple.writeShort(modelDefinition.texIndices3[face]);
            } else {
                int scaleX = modelDefinition.textureScaleX[face];
                int scaleY = modelDefinition.textureScaleY[face];
                int scaleZ = modelDefinition.textureScaleZ[face];
                if (type == 1) {
                    complex.writeShort(modelDefinition.texIndices1[face]);
                    complex.writeShort(modelDefinition.texIndices2[face]);
                    complex.writeShort(modelDefinition.texIndices3[face]);
                    if (modelDefinition.version >= 15 || scaleX > 0xffff || scaleZ > 0xffff) {
                        if (modelDefinition.version < 15) {
                            modelDefinition.version = 15;
                        }
                        ByteBufferUtils.writeTriByte(scaleX, scale);
                        ByteBufferUtils.writeTriByte(scaleY, scale);
                        ByteBufferUtils.writeTriByte(scaleZ, scale);
                    } else {
                        scale.writeShort(scaleX);
                        if (modelDefinition.version < 14 && scaleY > 0xffff) {
                            modelDefinition.version = 14;
                        }
                        if (modelDefinition.version < 14) {
                            scale.writeShort(scaleY);
                        } else {
                            ByteBufferUtils.writeTriByte(scaleY, scale);
                        }
                        scale.writeShort(scaleZ);
                    }
                    rotation.writeByte(modelDefinition.textureRotation[face]);
                    direction.writeByte(modelDefinition.textureDirection[face]);
                    translation.writeByte(modelDefinition.textureSpeed[face]);
                } else if (type == 2) {
                    complex.writeShort(modelDefinition.texIndices1[face]);
                    complex.writeShort(modelDefinition.texIndices2[face]);
                    complex.writeShort(modelDefinition.texIndices3[face]);
                    if (modelDefinition.version >= 15 || scaleX > 0xffff || scaleZ > 0xffff) {
                        if (modelDefinition.version < 15) {
                            modelDefinition.version = 15;
                        }
                        ByteBufferUtils.writeTriByte(scaleX, scale);
                        ByteBufferUtils.writeTriByte(scaleY, scale);
                        ByteBufferUtils.writeTriByte(scaleZ, scale);
                    } else {
                        scale.writeShort(scaleX);
                        if (modelDefinition.version < 14 && scaleY > 0xffff) {
                            modelDefinition.version = 14;
                        }
                        if (modelDefinition.version < 14) {
                            scale.writeShort(scaleY);
                        } else {
                            ByteBufferUtils.writeTriByte(scaleY, scale);
                        }
                        scale.writeShort(scaleZ);
                    }
                    rotation.writeByte(modelDefinition.textureRotation[face]);
                    direction.writeByte(modelDefinition.textureDirection[face]);
                    translation.writeByte(modelDefinition.textureSpeed[face]);
                    translation.writeByte(modelDefinition.textureTransU[face]);
                    translation.writeByte(modelDefinition.textureTransV[face]);
                } else if (type == 3) {
                    complex.writeShort(modelDefinition.texIndices1[face]);
                    complex.writeShort(modelDefinition.texIndices2[face]);
                    complex.writeShort(modelDefinition.texIndices3[face]);


                    if (modelDefinition.version >= 15 || scaleX > 0xffff || scaleZ > 0xffff) {
                        if (modelDefinition.version < 15) {
                            modelDefinition.version = 15;
                        }
                        ByteBufferUtils.writeTriByte(scaleX, scale);
                        ByteBufferUtils.writeTriByte(scaleY, scale);
                        ByteBufferUtils.writeTriByte(scaleZ, scale);
                    } else {
                        scale.writeShort(scaleX);
                        if (modelDefinition.version < 14 && scaleY > 0xffff) {
                            modelDefinition.version = 14;
                        }
                        if (modelDefinition.version < 14) {
                            scale.writeShort(scaleY);
                        } else {
                            ByteBufferUtils.writeTriByte(scaleY, scale);
                        }
                        scale.writeShort(scaleZ);
                    }
                    rotation.writeByte(modelDefinition.textureRotation[face]);
                    direction.writeByte(modelDefinition.textureDirection[face]);
                    translation.writeByte(modelDefinition.textureSpeed[face]);
                }
            }
        }
    }

    private void encodeIndices(ModelDefinition modelDefinition, DataOutputStream ibuffer, DataOutputStream tbuffer) throws IOException {
        short lasta = 0;
        short lastb = 0;
        short lastc = 0;
        int pacc = 0;
        for (int fndex = 0; fndex < modelDefinition.faceCount; fndex++) {
            short cura = (short) modelDefinition.faceIndices1[fndex];
            short curb = (short) modelDefinition.faceIndices2[fndex];
            short curc = (short) modelDefinition.faceIndices3[fndex];
            if (cura == lastb && curb == lasta && curc != lastc) {
                tbuffer.writeByte(4);
                ByteBufferUtils.writeUnsignedSmart(curc - pacc, ibuffer);
                short back = lasta;
                lasta = lastb;
                lastb = back;
                pacc = lastc = curc;
            } else if (cura == lastc && curb == lastb && curc != lastc) {
                tbuffer.writeByte(3);
                ByteBufferUtils.writeUnsignedSmart(curc - pacc, ibuffer);
                lasta = lastc;
                pacc = lastc = curc;
            } else if (cura == lasta && curb == lastc && curc != lastc) {
                tbuffer.writeByte(2);
                ByteBufferUtils.writeUnsignedSmart(curc - pacc, ibuffer);
                lastb = lastc;
                pacc = lastc = curc;
            } else {
                tbuffer.writeByte(1);
                ByteBufferUtils.writeUnsignedSmart(curc - pacc, ibuffer);
                ByteBufferUtils.writeUnsignedSmart(curb - cura, ibuffer);
                ByteBufferUtils.writeUnsignedSmart(curc - curb, ibuffer);
                lasta = cura;
                lastb = curb;
                pacc = lastc = curc;
            }


        }
    }

}
