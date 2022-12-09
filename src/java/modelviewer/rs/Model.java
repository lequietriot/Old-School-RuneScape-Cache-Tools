package modelviewer.rs;

import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import lombok.SneakyThrows;
import modelviewer.rs.buffer.Buffer;

public class Model {

    public static Color rs2HSBToColor(short hsb, int alpha) {

        int transparency = alpha;
        if (transparency <= 0) {
            transparency = 255;
        }

        int hue = hsb >> 10 & 0x3f;
        int sat = hsb >> 7 & 0x07;
        int bri = hsb & 0x7f;
        java.awt.Color awtCol = java.awt.Color.getHSBColor((float) hue / 63, (float) sat / 7, (float) bri / 127);
        double r = awtCol.getRed() / 255.0;
        double g = awtCol.getGreen() / 255.0;
        double b = awtCol.getBlue() / 255.0;
        return Color.color(r, g, b, transparency / 255.0);
    }

    public boolean uvBetween(int face, double uLower, double uUpper, double vLower, double vUpper) {
        float u1 = textureUCoordinates[face][0];
        float u2 = textureUCoordinates[face][1];
        float u3 = textureUCoordinates[face][2];

        float v1 = textureVCoordinates[face][0];
        float v2 = textureVCoordinates[face][1];
        float v3 = textureVCoordinates[face][2];
        return (u1 >= uLower && u1 <= uUpper) && (u2 >= uLower && u2 <= uUpper) && (u3 >= uLower && u3 <= uUpper) && (v1 >= vLower && v1 <= vUpper) && (v2 >= vLower && v2 <= vUpper) && (v3 >= vLower && v3 <= vUpper);
    }


    public static Model decode(byte[] data, int id) {
        Model model = new Model();
        try {
            if (data[data.length - 1] == -1 && data[data.length - 2] == -1) {
                model.decodeNew(data, id);
            } else {
                model.decode317(data, id);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
       /* System.out.println(Arrays.toString(model.verticesXCoordinate));
        System.out.println(Arrays.toString(model.verticesYCoordinate));
        System.out.println(Arrays.toString(model.verticesZCoordinate));
        System.out.println("-".repeat(250));
        System.out.println(Arrays.toString(model.faceIndicesA));
        System.out.println(Arrays.toString(model.faceIndicesB));
        System.out.println(Arrays.toString(model.faceIndicesC));*/
        return model;
    }

    public void decodeNew(byte[] data, int id) {
        System.out.println("Decoded new " + id + " | " + data.length);
        modelId = id;
        Buffer first = new Buffer(data);
        Buffer second = new Buffer(data);
        Buffer third = new Buffer(data);
        Buffer fourth = new Buffer(data);
        Buffer fifth = new Buffer(data);
        Buffer sixth = new Buffer(data);
        Buffer seventh = new Buffer(data);

        first.pos = data.length - 23;
        vertices = first.getUnsignedShort();
        triangleCount = first.getUnsignedShort();
        texturedFaces = first.getUnsignedByte();

        System.out.println("Vertices: " + vertices);
        System.out.println("Triangle count: " + triangleCount);
        System.out.println("Textured faces: " + texturedFaces);


        // System.err.println("Vertices: " + vertices + " | Faces: " + faces + " | Texture faces: " + texture_faces);

        int flag = first.getUnsignedByte();//texture flag 00 false, 01+ true
        boolean hasFaceTypes = (flag & 0x1) == 1;
        boolean hasParticleEffects = (flag & 0x2) == 2;
        boolean hasBillboards = (flag & 0x4) == 4;
        boolean hasVersion = (flag & 0x8) == 8;
        if (hasVersion) {
            first.pos -= 7;
            first.pos += 6;
        }
        int model_priority_opcode = first.getUnsignedByte();
        int model_alpha_opcode = first.getUnsignedByte();
        int model_muscle_opcode = first.getUnsignedByte();
        int model_texture_opcode = first.getUnsignedByte();
        int model_bones_opcode = first.getUnsignedByte();
        System.out.println("---------------> data here");
        //619, 1244, 0, true, false, true, true, true
        System.out.printf("%d, %d, %d, %b, %b, %b, %b, %b\n", vertices, triangleCount, texturedFaces, flag == 1, model_priority_opcode == 1, model_alpha_opcode == 1, model_muscle_opcode == 1, model_bones_opcode == 1);
        int model_vertex_x = first.getUnsignedShort();
        int model_vertex_y = first.getUnsignedShort();
        int model_vertex_z = first.getUnsignedShort();
        int model_vertex_points = first.getUnsignedShort();
        System.out.println("new model = " + vertices + ", " + triangleCount + ", " + texturedFaces + ", " + flag + ", " + model_priority_opcode + ", " + model_alpha_opcode + ", " + model_muscle_opcode + ", " + model_bones_opcode + ", " + model_vertex_x + ", " + model_vertex_y + ", " + model_vertex_z + ", " + model_vertex_points);
        int model_texture_indices = first.getUnsignedShort();
        int texture_id_simple = 0;
        int texture_id_complex = 0;
        int texture_id_cube = 0;
        int face;
        System.out.println("Tex faces: " + texturedFaces);
        if (texturedFaces > 0) {
            textureMap = new short[texturedFaces];
            first.pos = 0;
            for (face = 0; face < texturedFaces; face++) {
                short opcode = textureMap[face] = first.getSignedByte();
                if (opcode == 0) {
                    texture_id_simple++;
                }
                if (opcode >= 1 && opcode <= 3) {
                    texture_id_complex++;
                }
                if (opcode == 2) {
                    texture_id_cube++;
                }

            }
        }
        int pos = texturedFaces;

        int model_vertex_offset = pos;
        pos += vertices;

        int model_render_type_offset = pos;
        if (flag == 1)
            pos += triangleCount;

        int model_face_offset = pos;
        pos += triangleCount;

        int model_face_priorities_offset = pos;
        if (model_priority_opcode == 255)
            pos += triangleCount;

        int model_muscle_offset = pos;
        if (model_muscle_opcode == 1)
            pos += triangleCount;

        int model_bones_offset = pos;
        if (model_bones_opcode == 1)
            pos += vertices;

        int model_alpha_offset = pos;
        if (model_alpha_opcode == 1)
            pos += triangleCount;

        int model_points_offset = pos;
        pos += model_vertex_points;

        int model_texture_id = pos;
        if (model_texture_opcode == 1)
            pos += triangleCount * 2;

        int model_texture_coordinate_offset = pos;
        pos += model_texture_indices;

        int model_color_offset = pos;
        pos += triangleCount * 2;

        int model_vertex_x_offset = pos;
        pos += model_vertex_x;

        int model_vertex_y_offset = pos;
        pos += model_vertex_y;

        int model_vertex_z_offset = pos;
        pos += model_vertex_z;

        int model_simple_texture_offset = pos;
        pos += texture_id_simple * 6;

        int model_complex_texture_offset = pos;
        pos += texture_id_complex * 6;

        int model_texture_scale_offset = pos;
        pos += texture_id_complex * 6;

        int model_texture_rotation_offset = pos;
        pos += texture_id_complex * 2;

        int model_texture_direction_offset = pos;
        pos += texture_id_complex;

        int model_texture_translate_offset = pos;
        pos += texture_id_complex * 2 + texture_id_cube * 2;

        verticesXCoordinate = new int[vertices];
        verticesYCoordinate = new int[vertices];
        verticesZCoordinate = new int[vertices];
        faceIndicesA = new int[triangleCount];
        faceIndicesB = new int[triangleCount];
        faceIndicesC = new int[triangleCount];
        if (model_bones_opcode == 1)
            vertexWeights = new int[vertices];

        if (flag == 1)
            triangleInfo = new int[triangleCount];

        if (model_priority_opcode == 255)
            trianglePriorities = new byte[triangleCount];
        else
            modelPriority = (byte) model_priority_opcode;

        if (model_alpha_opcode == 1)
            faceAlpha = new int[triangleCount];

        if (model_muscle_opcode == 1)
            triangleSkin = new int[triangleCount];

        if (model_texture_opcode == 1)
            faceMaterial = new short[triangleCount];

        if (model_texture_opcode == 1 && texturedFaces > 0)
            faceTexture = new short[triangleCount];

        triangleColors = new short[triangleCount];
        if (texturedFaces > 0) {
            textureVertexA = new short[texturedFaces];
            textureVertexB = new short[texturedFaces];
            textureVertexC = new short[texturedFaces];
        }
        first.pos = model_vertex_offset;
        second.pos = model_vertex_x_offset;
        third.pos = model_vertex_y_offset;
        fourth.pos = model_vertex_z_offset;
        fifth.pos = model_bones_offset;
        int start_x = 0;
        int start_y = 0;

        int start_z = 0;
        for (int point = 0; point < vertices; point++) {
            int position_mask = first.getUnsignedByte();
            int x = 0;
            if ((position_mask & 1) != 0) {
                x = second.getSignedSmart();
            }
            int y = 0;
            if ((position_mask & 2) != 0) {
                y = third.getSignedSmart();
            }
            int z = 0;
            if ((position_mask & 4) != 0) {
                z = fourth.getSignedSmart();
            }
            verticesXCoordinate[point] = start_x + x;
            verticesYCoordinate[point] = start_y + y;
            verticesZCoordinate[point] = start_z + z;
            start_x = verticesXCoordinate[point];
            start_y = verticesYCoordinate[point];
            start_z = verticesZCoordinate[point];
            if (vertexWeights != null)
                vertexWeights[point] = fifth.getUnsignedByte();

        }
        first.pos = model_color_offset;
        second.pos = model_render_type_offset;
        third.pos = model_face_priorities_offset;
        fourth.pos = model_alpha_offset;
        fifth.pos = model_muscle_offset;
        sixth.pos = model_texture_id;
        seventh.pos = model_texture_coordinate_offset;
        for (face = 0; face < triangleCount; face++) {
            triangleColors[face] = (short) (first.getUnsignedShort() & 0xFFFF);
            //   System.out.println("Read face color: " + triangleColors[face]);
            if (flag == 1) {
                triangleInfo[face] = second.getSignedByte();
            }
            if (model_priority_opcode == 255) {
                trianglePriorities[face] = third.getSignedByte();
            }
            if (model_alpha_opcode == 1) {
                faceAlpha[face] = fourth.getSignedByte();
                if (faceAlpha[face] < 0)
                    faceAlpha[face] = (256 + faceAlpha[face]);

            }
            if (model_muscle_opcode == 1)
                triangleSkin[face] = fifth.getUnsignedByte();

            if (model_texture_opcode == 1) {
                //System.out.println("Started reading face material at pos " + sixth.pos);
                faceMaterial[face] = (short) (sixth.getUnsignedShort() - 1);
                if (faceMaterial[face] >= 0) {
                    if (triangleInfo != null) {
                        if (triangleInfo[face] < 2
                                && triangleColors[face] != 127
                                && triangleColors[face] != -27075
                                && triangleColors[face] != 8128
                                && triangleColors[face] != 7510) {
                            faceMaterial[face] = -1;
                        }
                    }
                }


                if (faceMaterial[face] != -1 && faceMaterial[face] >= 0 && faceMaterial[face] <= 85)
                    triangleColors[face] = 127;

            }

            //System.out.println(Arrays.toString(triangleColors));
            if (faceTexture != null && faceMaterial[face] != -1) {
                faceTexture[face] = (byte) (seventh.getUnsignedByte() - 1);
                //  System.out.println(faceTexture[face] + " ->>>>>>");
            }
        }
        first.pos = model_points_offset;
        second.pos = model_face_offset;
        int a = 0;
        int b = 0;
        int c = 0;
        int last_coordinate = 0;
        for (face = 0; face < triangleCount; face++) {
            int opcode = second.getUnsignedByte();
            if (opcode == 1) {
                a = first.getSignedSmart() + last_coordinate;
                last_coordinate = a;
                b = first.getSignedSmart() + last_coordinate;
                last_coordinate = b;
                c = first.getSignedSmart() + last_coordinate;
                last_coordinate = c;
                faceIndicesA[face] = a;
                faceIndicesB[face] = b;
                faceIndicesC[face] = c;
            }
            if (opcode == 2) {
                b = c;
                c = first.getSignedSmart() + last_coordinate;
                last_coordinate = c;
                faceIndicesA[face] = a;
                faceIndicesB[face] = b;
                faceIndicesC[face] = c;
            }
            if (opcode == 3) {
                a = c;
                c = first.getSignedSmart() + last_coordinate;
                last_coordinate = c;
                faceIndicesA[face] = a;
                faceIndicesB[face] = b;
                faceIndicesC[face] = c;
            }
            if (opcode == 4) {
                int l14 = a;
                a = b;
                b = l14;
                c = first.getSignedSmart() + last_coordinate;
                last_coordinate = c;
                faceIndicesA[face] = a;
                faceIndicesB[face] = b;
                faceIndicesC[face] = c;
            }
        }
        first.pos = model_simple_texture_offset;
        second.pos = model_complex_texture_offset;
        third.pos = model_texture_scale_offset;
        fourth.pos = model_texture_rotation_offset;
        fifth.pos = model_texture_direction_offset;
        sixth.pos = model_texture_translate_offset;
        for (face = 0; face < texturedFaces; face++) {
            int opcode = textureMap[face] & 0xff;
            if (opcode == 0) {
                textureVertexA[face] = (short) first.getUnsignedShort();
                textureVertexB[face] = (short) first.getUnsignedShort();
                textureVertexC[face] = (short) first.getUnsignedShort();
            }
            if (opcode == 1) {
                textureVertexA[face] = (short) second.getUnsignedShort();
                textureVertexB[face] = (short) second.getUnsignedShort();
                textureVertexC[face] = (short) second.getUnsignedShort();
            }
            if (opcode == 2) {
                textureVertexA[face] = (short) second.getUnsignedShort();
                textureVertexB[face] = (short) second.getUnsignedShort();
                textureVertexC[face] = (short) second.getUnsignedShort();
            }
            if (opcode == 3) {
                textureVertexA[face] = (short) second.getUnsignedShort();
                textureVertexB[face] = (short) second.getUnsignedShort();
                textureVertexC[face] = (short) second.getUnsignedShort();
            }
        }

    }

    //*Added*//
    public short[] faceMaterial;
    public short[] faceTexture;
    public short[] textureMap;


    public void computeUVCoordinates() {
        if (texturedFaces == 0) {
            return;
        }

        textureUCoordinates = new float[triangleCount][];
        textureVCoordinates = new float[triangleCount][];

        for (int i = 0; i < triangleCount; i++) {
            int coordinate = triangleInfo == null ? -1 : triangleInfo[i] >> 2;
            int textureIdx;
            if (triangleInfo == null || triangleInfo[i] < 2) {
                textureIdx = -1;
            } else {
                textureIdx = triangleColors[i] & 0xFFFF;
            }

            if (textureIdx != -1) {
                float[] u = new float[3];
                float[] v = new float[3];

                if (coordinate == -1) {
                    u[0] = 0.0F;
                    v[0] = 1.0F;

                    u[1] = 1.0F;
                    v[1] = 1.0F;

                    u[2] = 0.0F;
                    v[2] = 0.0F;
                } else {
                    coordinate &= 0xFF;
                    int faceA = faceIndicesA[i];
                    int faceB = faceIndicesB[i];
                    int faceC = faceIndicesC[i];

                    Point3D a = new Point3D(verticesXCoordinate[faceA], verticesYCoordinate[faceA], verticesZCoordinate[faceA]);
                    Point3D b = new Point3D(verticesXCoordinate[faceB], verticesYCoordinate[faceB], verticesZCoordinate[faceB]);
                    Point3D c = new Point3D(verticesXCoordinate[faceC], verticesYCoordinate[faceC], verticesZCoordinate[faceC]);

                    Point3D p = new Point3D(verticesXCoordinate[textureVertexA[coordinate]], verticesYCoordinate[textureVertexA[coordinate]], verticesZCoordinate[textureVertexA[coordinate]]);
                    Point3D m = new Point3D(verticesXCoordinate[textureVertexB[coordinate]], verticesYCoordinate[textureVertexB[coordinate]], verticesZCoordinate[textureVertexB[coordinate]]);
                    Point3D n = new Point3D(verticesXCoordinate[textureVertexC[coordinate]], verticesYCoordinate[textureVertexC[coordinate]], verticesZCoordinate[textureVertexC[coordinate]]);

                    Point3D pM = m.subtract(p);
                    Point3D pN = n.subtract(p);
                    Point3D pA = a.subtract(p);
                    Point3D pB = b.subtract(p);
                    Point3D pC = c.subtract(p);

                    Point3D pMxPn = pM.crossProduct(pN);

                    Point3D uCoordinate = pN.crossProduct(pMxPn);
                    double mU = 1.0F / uCoordinate.dotProduct(pM);

                    double uA = uCoordinate.dotProduct(pA) * mU;
                    double uB = uCoordinate.dotProduct(pB) * mU;
                    double uC = uCoordinate.dotProduct(pC) * mU;

                    Point3D vCoordinate = pM.crossProduct(pMxPn);
                    double mV = 1.0 / vCoordinate.dotProduct(pN);
                    double vA = vCoordinate.dotProduct(pA) * mV;
                    double vB = vCoordinate.dotProduct(pB) * mV;
                    double vC = vCoordinate.dotProduct(pC) * mV;

                    u[0] = (float) uA;
                    u[1] = (float) uB;
                    u[2] = (float) uC;

                    v[0] = (float) vA;
                    v[1] = (float) vB;
                    v[2] = (float) vC;
                }
                this.textureUCoordinates[i] = u;
                this.textureVCoordinates[i] = v;
            }
        }
    }

    public float[][] textureUCoordinates;
    public float[][] textureVCoordinates;

    @SneakyThrows(Exception.class)
    public void decode317(byte[] data, int id) {
        System.out.println("Decoded old model " + id + " | " + data.length);
        modelId = id;
        Buffer first = new Buffer(data);
        Buffer second = new Buffer(data);
        Buffer third = new Buffer(data);
        Buffer fourth = new Buffer(data);
        Buffer fifth = new Buffer(data);
        first.pos = data.length - 18;
        vertices = first.getUnsignedShort();
        triangleCount = first.getUnsignedShort();
        texturedFaces = first.getUnsignedByte();


        int renderTypeOpcode = first.getUnsignedByte();
        int renderPriorityOpcode = first.getUnsignedByte();
        int triangleAlphaOpcode = first.getUnsignedByte();
        int triangleSkinOpcode = first.getUnsignedByte();
        int vertexLabelOpcode = first.getUnsignedByte();
        int verticesXCoordinateOffset = first.getUnsignedShort();

        int verticesYCoordinateOffset = first.getUnsignedShort();
        int verticesZCoordinateOffset = first.getUnsignedShort();
        int triangleIndicesOffset = first.getUnsignedShort();

        int pos = 0;

        int vertexFlagOffset = pos;
        pos += vertices;

        int triangleCompressTypeOffset = pos;
        pos += triangleCount;

        int facePriorityOffset = pos;
        if (renderPriorityOpcode == 255) {
            pos += triangleCount;
        }

        int triangleSkinOffset = pos;
        if (triangleSkinOpcode == 1) {
            pos += triangleCount;
        }

        int renderTypeOffset = pos;
        if (renderTypeOpcode == 1) {
            pos += triangleCount;
        }

        int vertexLabelsOffset = pos;
        if (vertexLabelOpcode == 1) {
            pos += vertices;
        }

        int triangleAlphaOffset = pos;
        if (triangleAlphaOpcode == 1) {
            pos += triangleCount;
        }

        int indicesOffset = pos;
        pos += triangleIndicesOffset;

        int triangleColorOffset = pos;
        pos += triangleCount * 2;

        int textureOffset = pos;
        pos += texturedFaces * 6;

        int xOffset = pos;
        pos += verticesXCoordinateOffset;

        int yOffset = pos;
        pos += verticesYCoordinateOffset;

        int zOffset = pos;

        verticesXCoordinate = new int[vertices];
        verticesYCoordinate = new int[vertices];
        verticesZCoordinate = new int[vertices];
        faceIndicesA = new int[triangleCount];
        faceIndicesB = new int[triangleCount];
        faceIndicesC = new int[triangleCount];
        if (texturedFaces > 0) {
            textureVertexA = new short[texturedFaces];
            textureVertexB = new short[texturedFaces];
            textureVertexC = new short[texturedFaces];
        }

        if (vertexLabelOpcode == 1)
            vertexWeights = new int[vertices];


        if (renderTypeOpcode == 1) {
            triangleInfo = new int[triangleCount];
        }

        if (renderPriorityOpcode == 255)
            trianglePriorities = new byte[triangleCount];
        else
            modelPriority = (byte) renderPriorityOpcode;

        if (triangleAlphaOpcode == 1)
            faceAlpha = new int[triangleCount];

        if (triangleSkinOpcode == 1)
            triangleSkin = new int[triangleCount];

        triangleColors = new short[triangleCount];
        first.pos = vertexFlagOffset;
        second.pos = xOffset;
        third.pos = yOffset;
        fourth.pos = zOffset;
        fifth.pos = vertexLabelsOffset; // 18 +
        int baseX = 0;
        int baseY = 0;
        int baseZ = 0;

        for (int point = 0; point < vertices; point++) {
            int flag = first.getUnsignedByte();

            int x = 0;
            if ((flag & 0x1) != 0) {
                x = second.getSignedSmart();
            }

            int y = 0;
            if ((flag & 0x2) != 0) {
                y = third.getSignedSmart();
            }
            int z = 0;
            if ((flag & 0x4) != 0) {
                z = fourth.getSignedSmart();
            }

            verticesXCoordinate[point] = baseX + x;
            verticesYCoordinate[point] = baseY + y;
            verticesZCoordinate[point] = baseZ + z;
            baseX = verticesXCoordinate[point];
            baseY = verticesYCoordinate[point];
            baseZ = verticesZCoordinate[point];
            if (vertexLabelOpcode == 1) {
                vertexWeights[point] = fifth.getUnsignedByte();
            }
        }


        first.pos = triangleColorOffset;
        second.pos = renderTypeOffset;
        third.pos = facePriorityOffset;
        fourth.pos = triangleAlphaOffset;
        fifth.pos = triangleSkinOffset;

        for (int face = 0; face < triangleCount; face++) {
            int color = first.getUnsignedShort();
            triangleColors[face] = (short) color;

            if (renderTypeOpcode == 1) {
                triangleInfo[face] = second.getUnsignedByte();
            }
            if (renderPriorityOpcode == 255) {
                trianglePriorities[face] = third.getSignedByte();
            }

            if (triangleAlphaOpcode == 1) {
                faceAlpha[face] = fourth.getSignedByte();
                if (faceAlpha[face] < 0) {
                    faceAlpha[face] = (256 + faceAlpha[face]);
                }

            }
            if (triangleSkinOpcode == 1) {
                triangleSkin[face] = fifth.getUnsignedByte();
            }

        }
        first.pos = indicesOffset;
        second.pos = triangleCompressTypeOffset;
        int a = 0;
        int b = 0;
        int c = 0;
        int offset = 0;
        int coordinate;

        for (int face = 0; face < triangleCount; face++) {
            int opcode = second.getUnsignedByte();


            if (opcode == 1) {
                a = (first.getSignedSmart() + offset);
                offset = a;
                b = (first.getSignedSmart() + offset);
                offset = b;
                c = (first.getSignedSmart() + offset);
                offset = c;
                faceIndicesA[face] = a;
                faceIndicesB[face] = b;
                faceIndicesC[face] = c;

            }
            if (opcode == 2) {
                b = c;
                c = (first.getSignedSmart() + offset);
                offset = c;
                faceIndicesA[face] = a;
                faceIndicesB[face] = b;
                faceIndicesC[face] = c;
            }
            if (opcode == 3) {
                a = c;
                c = (first.getSignedSmart() + offset);
                offset = c;
                faceIndicesA[face] = a;
                faceIndicesB[face] = b;
                faceIndicesC[face] = c;
            }
            if (opcode == 4) {
                coordinate = a;
                a = b;
                b = coordinate;
                c = (first.getSignedSmart() + offset);
                offset = c;
                faceIndicesA[face] = a;
                faceIndicesB[face] = b;
                faceIndicesC[face] = c;
            }

        }
        first.pos = textureOffset;

        for (int face = 0; face < texturedFaces; face++) {
            textureVertexA[face] = (short) first.getUnsignedShort();
            textureVertexB[face] = (short) first.getUnsignedShort();
            textureVertexC[face] = (short) first.getUnsignedShort();
        }

        if (triangleInfo == null) {
            triangleInfo = new int[triangleCount];
        }
        // System.out.println("Tri info = " + Arrays.toString(triangleInfo));
    }

    private int modelId;

    public int getModelId() {
        return modelId;
    }

    public int hsbToRGB(int hsb) {
        float h = hsb >> 10 & 0x3f;
        float s = hsb >> 7 & 0x07;
        float b = hsb & 0x7f;
        return java.awt.Color.HSBtoRGB(h / 63, s / 7, b / 127);
    }


    public int vertices;
    public int triangleCount;
    public int[] verticesXCoordinate;
    public int[] verticesYCoordinate;
    public int[] verticesZCoordinate;
    public int[] faceIndicesA;
    public int[] faceIndicesB;
    public int[] faceIndicesC;
    public int[] triangleInfo;
    public byte[] trianglePriorities;
    public int[] faceAlpha;
    public short[] triangleColors;
    public byte modelPriority = 0;
    public int texturedFaces;
    public short[] textureVertexA;
    public short[] textureVertexB;
    public short[] textureVertexC;
    public int[] vertexWeights;
    public int[] triangleSkin;


}
