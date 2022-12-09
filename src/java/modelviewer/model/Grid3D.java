package modelviewer.model;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import modelviewer.polygonalmesh.PolygonMesh;
import modelviewer.polygonalmesh.PolygonMeshView;

public class Grid3D {

    public Group create(float size, float delta) {
        if (delta < 0.01f) {
            delta = 0.01f;
        }

        final PolygonMesh plane2 = createQuadrilateralMesh(size, size, (int) (size / delta), (int) (size / delta));

        PolygonMeshView meshViewXZ2 = new PolygonMeshView(plane2);
        meshViewXZ2.setDrawMode(DrawMode.LINE);
        meshViewXZ2.setCullFace(CullFace.NONE);
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(Color.BLACK);
        meshViewXZ2.setMaterial(mat);
        meshViewXZ2.getTransforms().add(new Translate(size / 1000f, size / 1000f, 0));
        meshViewXZ2.getTransforms().add(new Rotate(90, Rotate.X_AXIS));

        return new Group(meshViewXZ2);
    }

    private PolygonMesh createQuadrilateralMesh(float width, float height, int subDivX, int subDivY) {
        final float minX = -width / 2f;
        final float minY = -height / 2f;
        final float maxX = width / 2f;
        final float maxY = height / 2f;

        final int pointSize = 3;
        final int texCoordSize = 2;
        // 4 point indices and 4 texCoord indices per face
        final int faceSize = 8;
        int numDivX = subDivX + 1;
        int numVerts = (subDivY + 1) * numDivX;
        float[] points = new float[numVerts * pointSize];
        float[] texCoords = new float[numVerts * texCoordSize];
        int faceCount = subDivX * subDivY;
        int[][] faces = new int[faceCount][faceSize];

        // Create points and texCoords
        for (int y = 0; y <= subDivY; y++) {
            float dy = (float) y / subDivY;
            double fy = (1 - dy) * minY + dy * maxY;

            for (int x = 0; x <= subDivX; x++) {
                float dx = (float) x / subDivX;
                double fx = (1 - dx) * minX + dx * maxX;

                int index = y * numDivX * pointSize + (x * pointSize);
                points[index] = (float) fx;
                points[index + 1] = (float) fy;
                points[index + 2] = 0.0f;

                index = y * numDivX * texCoordSize + (x * texCoordSize);
                texCoords[index] = dx;
                texCoords[index + 1] = dy;
            }
        }

        // Create faces
        int index = 0;
        for (int y = 0; y < subDivY; y++) {
            for (int x = 0; x < subDivX; x++) {
                int p00 = y * numDivX + x;
                int p01 = p00 + 1;
                int p10 = p00 + numDivX;
                int p11 = p10 + 1;
                int tc00 = y * numDivX + x;
                int tc01 = tc00 + 1;
                int tc10 = tc00 + numDivX;
                int tc11 = tc10 + 1;

                faces[index][0] = p00;
                faces[index][1] = tc00;
                faces[index][2] = p10;
                faces[index][3] = tc10;
                faces[index][4] = p11;
                faces[index][5] = tc11;
                faces[index][6] = p01;
                faces[index++][7] = tc01;
            }
        }

        int[] smooth = new int[faceCount];

        PolygonMesh mesh = new PolygonMesh(points, texCoords, faces);
        mesh.getFaceSmoothingGroups().addAll(smooth);
        return mesh;
    }
}
