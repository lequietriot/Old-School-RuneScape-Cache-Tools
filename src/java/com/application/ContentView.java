package com.application;

import osrs.RasterProvider;
import osrs.Rasterizer2D;

import javax.swing.*;

public class ContentView extends JPanel {

    public RasterProvider rasterProvider;

    public ContentView() {
        setVisible(true);
        rasterProvider = new RasterProvider(800, 800, new JPanel());
        Rasterizer2D.Rasterizer2D_fillRectangle(0, 0, 0, 0, 0);
        Rasterizer2D.Rasterizer2D_drawRectangle(0, 0, 0, 0, 16777215);
        rasterProvider.drawFull(0, 0);
        revalidate();
    }

}
