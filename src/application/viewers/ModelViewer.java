package application.viewers;

import application.GUI;
import application.constants.AppConstants;
import com.displee.cache.CacheLibrary;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import osrs.RotationControl;
import rshd.ModelData;
import runelite.definitions.ModelDefinition;
import runelite.loaders.ModelLoader;
import runelite.managers.TextureManager;

import javax.swing.*;
import java.io.IOException;
import java.util.Objects;

public class ModelViewer extends JPanel {

    private final CacheLibrary cacheLibrary;
    private ModelDefinition model;
    private RotationControl rotationControl;

    public ModelViewer(JFXPanel modelPanel) {

        modelPanel.setVisible(true);

        cacheLibrary = GUI.cacheLibrary;

        int index = 7;
        int archive = 0;
        int file = 0;

        try {
            if (AppConstants.cacheType.equals("RuneScape High Definition")) {
                ModelData loader = new ModelData();
                if (cacheLibrary.index(7).archive(archive) != null) {
                    model = loader.load(archive, Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(cacheLibrary.index(7).archive(archive)).file(0)).getData()));
                }
            } else {
                TextureManager tm = new TextureManager(cacheLibrary);
                tm.load();

                ModelLoader loader = new ModelLoader();
                model = loader.load(archive, Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(cacheLibrary.index(index).archive(archive)).file(file)).getData()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        model.computeNormals();

        Scene scene = modelPanel.getScene();

    }
}
