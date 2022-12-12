package modelviewer.controllers;

import com.application.AppConstants;
import com.application.GUI;
import com.displee.cache.CacheLibrary;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import modelviewer.model.Grid3D;
import modelviewer.scene.RSMeshGroup;
import modelviewer.scene.camera.OrbitCamera;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.ModelLoader;
import rs3.RS3ModelData;
import rshd.ModelData;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class ModelController implements Initializable {

    @FXML
    private AnchorPane modelPane;

    private RSMeshGroup meshGroup;
    private Group scene;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateModel(GUI.cacheLibrary, GUI.selectedIndex, GUI.selectedArchive, GUI.selectedFile);
    }

    public void initScene(ModelDefinition model) throws IOException {
        meshGroup = new RSMeshGroup(model);
        meshGroup.buildMeshes();
        scene = buildScene();
        Group grid = new Grid3D().create(48f, 1.25f);
        scene.getChildren().add(grid);
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

    public void updateModel(CacheLibrary cacheLibrary, int selectedIndex, int selectedArchive, int selectedFile) {

        if (AppConstants.cacheType.equals("RuneScape 2")) {
            ModelLoader modelLoader = new ModelLoader();
            ModelDefinition model;
            if (selectedIndex == 1) {
                model = modelLoader.load(0, Objects.requireNonNull(cacheLibrary.data(selectedIndex, selectedArchive, selectedFile)));
            } else {
                model = modelLoader.load(0, Objects.requireNonNull(cacheLibrary.data(1, 1, 0)));
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
                model = modelLoader.load(0, Objects.requireNonNull(cacheLibrary.data(selectedIndex, selectedArchive, selectedFile)));
            } else {
                model = modelLoader.load(0, Objects.requireNonNull(cacheLibrary.data(7, 1, 0)));
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
                    model = modelData.load(0, Objects.requireNonNull(cacheLibrary.data(selectedIndex, selectedArchive, selectedFile)));
                } else {
                    model = modelData.load(0, Objects.requireNonNull(cacheLibrary.data(7, 1, 0)));
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
                    model = modelData.load(0, Objects.requireNonNull(cacheLibrary.data(selectedIndex, selectedArchive, selectedFile)), cacheLibrary);
                } else {
                    model = modelData.load(0, Objects.requireNonNull(cacheLibrary.data(7, 1, 0)), cacheLibrary);
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
    }

