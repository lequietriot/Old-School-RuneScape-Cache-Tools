package application.viewers;

import application.GUI;
import com.displee.cache.CacheLibrary;
import osrs.GameRasterizer;
import osrs.RotationControl;
import osrs.ProducingGraphicsBuffer;
import runelite.definitions.ModelDefinition;
import runelite.loaders.ModelLoader;
import runelite.models.JagexColor;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class ModelViewer extends JPanel {

    private final GameRasterizer modelRasterizer;
    private final ProducingGraphicsBuffer buffer;
    private final CacheLibrary cacheLibrary;
    private ModelDefinition model;
    private RotationControl rotationControl;

    public ModelViewer(JPanel modelPanel) {

        modelPanel.setVisible(true);

        cacheLibrary = GUI.cacheLibrary;

        int index = 7;
        int archive = 0;
        int file = 0;
        
        modelRasterizer = new GameRasterizer();
        modelRasterizer.setBrightness(JagexColor.BRIGHTNESS_MAX);

        buffer = new ProducingGraphicsBuffer(modelPanel, 400, 400);
        buffer.initializeRasterizer();

        modelRasterizer.useViewport();
        modelRasterizer.setDefaultBounds();
        modelRasterizer.init(modelPanel.getHeight(), modelPanel.getWidth(), buffer.getRaster().getRaster());

        rotationControl = new RotationControl();

        ModelLoader loader = new ModelLoader();
        model = loader.load(archive, Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(cacheLibrary.index(index).archive(archive)).file(file)).getData()));
        
        renderModel();
    }

    private void renderModel() {

        int roll = (int) Math.toDegrees(Math.toRadians(rotationControl.getRotateX().getAngle()) * 5.65) & 0x7ff;
        int yaw = (int) Math.toDegrees(Math.toRadians(rotationControl.getRotateY().getAngle()) * 5.65) & 0x7ff;
        int pitch = (int) Math.toDegrees(Math.toRadians(rotationControl.getRotateZ().getAngle()) * 5.65) & 0x7ff;

        model.render(modelRasterizer, 0, 0, 0, 0, 1, 1, 1);
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(buffer.getImage(), 0, 0, null);
    }
}
