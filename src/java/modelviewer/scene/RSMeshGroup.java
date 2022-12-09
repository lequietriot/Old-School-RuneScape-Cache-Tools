package modelviewer.scene;

import com.application.AppConstants;
import com.application.GUI;
import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Scale;
import lombok.Getter;
import modelviewer.model.Vector3i;
import modelviewer.util.ColorUtils;
import net.runelite.cache.TextureManager;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.SpriteDefinition;
import net.runelite.cache.definitions.loaders.SpriteLoader;
import net.runelite.cache.fs.Store;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
public class RSMeshGroup {
    private final ModelDefinition model;
    private final List<MeshView> meshes = new ArrayList<>();
    public final float MODEL_SCALE = 0.03f;

    private TextureManager textureManager;

    public RSMeshGroup(ModelDefinition model) {
        this.model = model;
    }

    private Image texture;

    public void buildMeshes() throws IOException {
        if (AppConstants.cacheType.equals("Old School RuneScape")) {
            Store store = new Store(new File(GUI.cacheLibrary.getPath()));
            store.load();
            textureManager = new TextureManager(store);
            textureManager.load();
        }
        model.computeTextureUVCoordinates();
        for (int face = 0; face < model.faceCount; face++) {
            TriangleMesh mesh = new TriangleMesh();
            int faceA = model.faceIndices1[face];
            int faceB = model.faceIndices2[face];
            int faceC = model.faceIndices3[face];

            Vector3i v1 = new Vector3i(model.vertexX[faceA], model.vertexY[faceA], model.vertexZ[faceA]);
            Vector3i v2 = new Vector3i(model.vertexX[faceB], model.vertexY[faceB], model.vertexZ[faceB]);
            Vector3i v3 = new Vector3i(model.vertexX[faceC], model.vertexY[faceC], model.vertexZ[faceC]);

            mesh.getPoints()
                    .addAll(v1.x(), v1.y(), v1.z(), v2.x(), v2.y(), v2.z(), v3.x(), v3.y(), v3.z());

            mesh.getFaces().addAll(
                    0, 0, 1, 1, 2, 2
            );
            boolean textured = model.faceTextures != null;
            if (textured) {
                mesh.getTexCoords()
                        .addAll(model.faceTextureUCoordinates[face][0], model.faceTextureVCoordinates[face][0]);
                mesh.getTexCoords()
                        .addAll(model.faceTextureUCoordinates[face][1], model.faceTextureVCoordinates[face][1]);
                mesh.getTexCoords()
                        .addAll(model.faceTextureUCoordinates[face][2], model.faceTextureVCoordinates[face][2]);
            } else {
                mesh.getTexCoords().addAll(0f, 0f, 1f, 0f, 0f, 1f);
            }
            MeshView view = new MeshView(mesh);
            view.getTransforms().add(new Scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE));
            if (textured) {
                PhongMaterial mat = new PhongMaterial();
                if (model.faceTextures != null && model.faceTextures[face] != -1 && AppConstants.cacheType.equals("Old School RuneScape")) {
                    texture = exportToImage(textureManager.findTexture(model.faceTextures[face]).getFileIds()[0]);
                    mat.setDiffuseMap(texture);
                    view.setMaterial(mat);
                }
                else {
                    view.setMaterial(new PhongMaterial(ColorUtils.rs2HSLToColor(model.faceColors[face], model.faceTransparencies == null ? 0 : model.faceTransparencies[face])));
                }
            } else {
                view.setMaterial(new PhongMaterial(ColorUtils.rs2HSLToColor(model.faceColors[face], model.faceTransparencies == null ? 0 : model.faceTransparencies[face])));
            }
            initListeners(view);
            meshes.add(view);
        }
    }

    private void initListeners(MeshView view) {
        view.setOnMouseClicked(event -> {
            paint(view);
        });
        view.setOnMouseEntered(event -> {
            if (event.isAltDown()) {
                paint(view);
            }
        });
    }

    private void paint(MeshView view) {
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseMap(texture);
        view.setMaterial(mat);
    }

    public BufferedImage export(SpriteDefinition spriteDefinition)
    {
        BufferedImage bi = new BufferedImage(spriteDefinition.getWidth(), spriteDefinition.getHeight(), BufferedImage.TYPE_INT_ARGB);
        bi.setRGB(0, 0, spriteDefinition.getWidth(), spriteDefinition.getHeight(), spriteDefinition.getPixels(), 0, spriteDefinition.getWidth());
        return bi;
    }

    public Image exportToImage(int spriteID) throws IOException
    {
        SpriteLoader spriteLoader = new SpriteLoader();
        SpriteDefinition spriteDefinition = spriteLoader.load(spriteID, GUI.cacheLibrary.data(8, spriteID, 0))[0];
        BufferedImage image = export(spriteDefinition);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", byteArrayOutputStream);
        return new Image(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
    }
}
