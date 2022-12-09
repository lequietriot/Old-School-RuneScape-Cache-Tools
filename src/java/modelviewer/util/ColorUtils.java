package modelviewer.util;

import javafx.scene.paint.Color;

public final class ColorUtils {

    public static Color rs2HSLToColor(short hsl, int alpha) {
        int transparency = alpha;
        if (transparency <= 0) {
            transparency = 255;
        }

        int hue = hsl >> 10 & 0x3f;
        int sat = hsl >> 7 & 0x07;
        int bri = hsl & 0x7f;
        java.awt.Color awtCol = java.awt.Color.getHSBColor((float) hue / 63, (float) sat / 7, (float) bri / 127);
        double r = awtCol.getRed() / 255.0;
        double g = awtCol.getGreen() / 255.0;
        double b = awtCol.getBlue() / 255.0;
        return Color.color(r, g, b, transparency / 255.0);
    }
}
