package modelviewer.scene;

import com.application.AppConstants;
import com.application.GUI;
import javafx.animation.Animation;
import javafx.animation.TranslateTransition;
import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import lombok.Getter;
import modelviewer.model.Vector3i;
import modelviewer.util.ColorUtils;
import net.runelite.cache.TextureManager;
import net.runelite.cache.definitions.FrameDefinition;
import net.runelite.cache.definitions.FramemapDefinition;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.SpriteDefinition;
import net.runelite.cache.definitions.loaders.FrameLoader;
import net.runelite.cache.definitions.loaders.FramemapLoader;
import net.runelite.cache.definitions.loaders.SpriteLoader;
import net.runelite.cache.fs.Store;
import rshd.TextureLoaderHD;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class RSMeshGroup {
    private final ModelDefinition model;
    private final List<MeshView> meshes = new ArrayList<>();
    public final float MODEL_SCALE = 0.03f;

    private TextureManager textureManager;
    private TextureLoaderHD textureLoaderHD;

    public MeshView view;
    public TriangleMesh mesh;

    public RSMeshGroup(ModelDefinition model) {
        this.model = model;
    }

    private Image texture;

    public void buildMeshes() throws IOException {
        if (AppConstants.cacheType.equals("RuneScape 2")) {

        }
        if (AppConstants.cacheType.equals("Old School RuneScape")) {
            Store store = new Store(new File(GUI.cacheLibrary.getPath()));
            store.load();
            textureManager = new TextureManager(store);
            textureManager.load();
        }
        if (AppConstants.cacheType.equals("RuneScape High Definition")) {
            textureLoaderHD = new TextureLoaderHD();
        }
        model.computeTextureUVCoordinates();
        for (int face = 0; face < model.faceCount; face++) {
            mesh = new TriangleMesh();
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
            view = new MeshView(mesh);
            view.getTransforms().add(new Scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE));
            if (textured) {
                PhongMaterial mat = new PhongMaterial();
                if (model.faceTextures != null && model.faceTextures[face] != -1 && AppConstants.cacheType.equals("Old School RuneScape")) {
                    texture = exportToImage(textureManager.findTexture(model.faceTextures[face]).getFileIds()[0]);
                    mat.setDiffuseMap(texture);
                    mat.setDiffuseColor(ColorUtils.rs2HSLToColor(model.faceColors[face], model.faceTransparencies == null ? 0 : model.faceTransparencies[face]));
                    view.setMaterial(mat);
                }
                if (model.faceTextures != null && model.faceTextures[face] != -1 && AppConstants.cacheType.equals("RuneScape High Definition")) {
                    texture = exportToImageHD(model.faceTextures[face]);
                    mat.setDiffuseMap(texture);
                    mat.setDiffuseColor(ColorUtils.rs2HSLToColor(model.faceColors[face], model.faceTransparencies == null ? 0 : model.faceTransparencies[face]));
                    view.setMaterial(mat);
                }
                else {
                    if (model.faceTextures != null && model.faceTextures[face] == -1) {
                        view.setMaterial(new PhongMaterial(ColorUtils.rs2HSLToColor(model.faceColors[face], model.faceTransparencies == null ? 0 : model.faceTransparencies[face])));
                    }
                }
            } else {
                view.setMaterial(new PhongMaterial(ColorUtils.rs2HSLToColor(model.faceColors[face], model.faceTransparencies == null ? 0 : model.faceTransparencies[face])));
            }
            meshes.add(view);
        }
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

    public Image exportToImageHD(int id) {
        SpriteLoader spriteLoader = new SpriteLoader();
        SpriteDefinition spriteDefinition = spriteLoader.load(0, GUI.cacheLibrary.data(8, 0, 0))[0];
        BufferedImage image = export(spriteDefinition);
        try {
            ImageIO.write(image, "png", new FileOutputStream(new File(System.getProperty("user.home") + "/Documents/Cache/0.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] data = GUI.cacheLibrary.data(9, id, 0);
        if (data != null) {
            if (data[1] == "P".getBytes()[0]) {
                return new Image(new ByteArrayInputStream(data));
            }
        }
        /*
        try {
            ImageIndexLoader imageIndexLoader = new ImageIndexLoader(GUI.cacheLibrary.index(26), GUI.cacheLibrary.index(9), GUI.cacheLibrary.index(8));
            int[] pixels = imageIndexLoader.renderMaterialPixelsI(id, 256, 256);
            BufferedImage bi = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            bi.setRGB(0, 0, 256, 256, pixels, 0, 256);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", byteArrayOutputStream);
            return new Image(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
         */
        return null;
    }

    public void animateModel(int sequenceID, ModelDefinition model) {
        FramemapLoader framemapLoader = new FramemapLoader();
        FramemapDefinition framemapDefinition = framemapLoader.load(sequenceID, GUI.cacheLibrary.data(1, sequenceID, 0));
        FrameLoader frameLoader = new FrameLoader();
        FrameDefinition frameDefinition = frameLoader.load(framemapDefinition, sequenceID, GUI.cacheLibrary.data(0, sequenceID, 0));

        if (model.origVX == null)
        {
            model.origVX = Arrays.copyOf(model.vertexX, model.vertexX.length);
            model.origVY = Arrays.copyOf(model.vertexY, model.vertexY.length);
            model.origVZ = Arrays.copyOf(model.vertexZ, model.vertexZ.length);
        }

        final int[] verticesX = model.vertexX;
        final int[] verticesY = model.vertexY;
        final int[] verticesZ = model.vertexZ;
        int var6 = frameDefinition.translatorCount;
        int var8;
        int var11;
        int var12;
        model.animOffsetX = 0;
        model.animOffsetY = 0;
        model.animOffsetZ = 0;

        for (var8 = 0; var8 < var6; ++var8)
        {
            int var9 = frameDefinition.indexFrameIds[var8];
            if (var9 < model.vertexGroups.length)
            {
                int[] var10 = model.vertexGroups[var9];

                for (var11 = 0; var11 < var10.length; ++var11)
                {
                    var12 = var10[var11];
                    model.animOffsetX += verticesX[var12];
                    model.animOffsetY += verticesY[var12];
                    model.animOffsetZ += verticesZ[var12];
                }
            }
        }

        TranslateTransition translateTransition = new TranslateTransition();
        for (int index = 0; index < frameDefinition.translatorCount; index++) {
            translateTransition.setDuration(Duration.millis(1000));
            translateTransition.setFromX(model.getOrigVX()[index]);
            translateTransition.setToX(model.getAnimOffsetX());
            translateTransition.setFromX(model.getOrigVY()[index]);
            translateTransition.setToX(model.getAnimOffsetY());
            translateTransition.setFromX(model.getOrigVZ()[index]);
            translateTransition.setToX(model.getAnimOffsetZ());
            translateTransition.setCycleCount(Animation.INDEFINITE);
            translateTransition.setNode(view);
            translateTransition.play();

            System.out.println("playing");
        }
    }
}
