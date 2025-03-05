package osrs;

import java.awt.*;
import java.awt.image.*;
import java.util.Hashtable;

public final class RasterProvider extends AbstractRasterProvider {

	public Component component;

	Image image;

	public int width;

	public int height;

	public int[] pixels;

	public ColorModel colorModel;

	public WritableRaster writableRaster;

	public DirectColorModel directColorModel;

	public DataBufferInt dataBufferInt;

	public RasterProvider(int var1, int var2, Component var3) {
		width = var1;
		height = var2;
		pixels = new int[var2 * var1 + 1];
		dataBufferInt = new DataBufferInt(pixels, pixels.length);
		directColorModel = new DirectColorModel(32, 16711680, 65280, 255);
		writableRaster = Raster.createWritableRaster(directColorModel.createCompatibleSampleModel(width, height), dataBufferInt, null);
		this.image = new BufferedImage(directColorModel, writableRaster, false, new Hashtable<>());
		this.setComponent(var3);
		this.apply();
	}

	final void setComponent(Component var1) {
		this.component = var1;
	}

	public final void drawFull(int var1, int var2) {
		this.drawFull0(this.component.getGraphics(), var1, var2);
	}

	public final void draw(int var1, int var2, int var3, int var4) {
		this.draw0(this.component.getGraphics(), var1, var2, var3, var4);
	}

	final void drawFull0(Graphics var1, int var2, int var3) {
		try {
			var1.drawImage(this.image, var2, var3, this.component);
		} catch (Exception var5) {
			this.component.repaint();
		}

	}

	final void draw0(Graphics var1, int var2, int var3, int var4, int var5) {
		try {
			Shape var6 = var1.getClip();
			var1.clipRect(var2, var3, var4, var5);
			var1.drawImage(this.image, 0, 0, this.component);
			var1.setClip(var6);
		} catch (Exception var7) {
			this.component.repaint();
		}

	}

}