package modelviewer.controllers;

import com.application.AppConstants;
import com.application.GUI;
import javafx.animation.Animation;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import modelviewer.ModelViewer;
import modelviewer.model.Grid3D;
import modelviewer.scene.RSMeshGroup;
import modelviewer.scene.camera.OrbitCamera;
import net.runelite.cache.definitions.FrameDefinition;
import net.runelite.cache.definitions.FramemapDefinition;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.FrameLoader;
import net.runelite.cache.definitions.loaders.FramemapLoader;
import net.runelite.cache.definitions.loaders.ModelLoader;
import rs3.RS3ModelData;
import rshd.ModelData;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.ResourceBundle;

public class ModelController implements Initializable {

    @FXML
    private AnchorPane modelPane;

    private RSMeshGroup meshGroup;
    private Group scene;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ModelViewer.modelController = this;
        updateModel(GUI.selectedIndex, GUI.selectedArchive, GUI.selectedFile);
    }

    public void initScene(ModelDefinition model) throws IOException {
        meshGroup = new RSMeshGroup(model);
        meshGroup.buildMeshes();
        scene = buildScene();
        Group grid = new Grid3D().create(48f, 1.25f);
        //scene.getChildren().add(grid);
        SubScene subScene = create3DScene();
        scene.getChildren().add(new AmbientLight(Color.WHITE));
        modelPane.getChildren().addAll(subScene);
    }

    private Group buildScene() {
        Group group = new Group();
        group.getChildren().addAll(meshGroup.getMeshes());
        return group;
    }

    private SubScene create3DScene() {
        SubScene scene3D = new SubScene(scene, modelPane.getPrefWidth(), modelPane.getPrefHeight(), true, SceneAntialiasing.BALANCED);
        scene3D.setFill(Color.rgb(30, 30, 30));
        new OrbitCamera(scene3D, scene);
        return scene3D;
    }

    public void updateModel(int selectedIndex, int selectedArchive, int selectedFile) {
        if (AppConstants.cacheType.equals("RuneScape 2")) {
            ModelLoader modelLoader = new ModelLoader();
            ModelDefinition model;
            if (selectedIndex == 1) {
                model = modelLoader.load(0, Objects.requireNonNull(GUI.cacheLibrary.data(selectedIndex, selectedArchive, selectedFile)));
            } else {
                model = modelLoader.load(0, Objects.requireNonNull(GUI.cacheLibrary.data(1, 1, 0)));
            }
            if (model != null) {
                try {
                    initScene(model);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (AppConstants.cacheType.equals("Old School RuneScape")) {
            ModelLoader modelLoader = new ModelLoader();
            ModelDefinition model;
            if (selectedIndex == 7) {
                model = modelLoader.load(0, Objects.requireNonNull(GUI.cacheLibrary.data(selectedIndex, selectedArchive, selectedFile)));
            } else {
                model = modelLoader.load(0, Objects.requireNonNull(GUI.cacheLibrary.data(7, 1, 0)));
            }
            if (model != null) {
                try {
                    initScene(model);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (AppConstants.cacheType.equals("RuneScape High Definition")) {
            ModelData modelData = new ModelData();
            ModelDefinition model;
            if (selectedIndex == 7) {
                model = modelData.load(0, Objects.requireNonNull(GUI.cacheLibrary.data(selectedIndex, selectedArchive, selectedFile)));
            } else {
                model = modelData.load(0, Objects.requireNonNull(GUI.cacheLibrary.data(7, 1, 0)));
            }
            if (model != null) {
                try {
                    initScene(model);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (AppConstants.cacheType.equals("RuneScape 3")) {
            RS3ModelData modelData = new RS3ModelData(0);
            ModelDefinition model;
            if (selectedIndex == 7) {
                model = modelData.load(0, Objects.requireNonNull(GUI.cacheLibrary.data(selectedIndex, selectedArchive, selectedFile)), GUI.cacheLibrary);
            } else {
                model = modelData.load(0, Objects.requireNonNull(GUI.cacheLibrary.data(7, 1, 0)), GUI.cacheLibrary);
            }
            if (model != null) {
                try {
                    initScene(model);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
            translateTransition.setNode(meshGroup.getView());
            translateTransition.play();

            System.out.println("playing");
        }
    }
}

