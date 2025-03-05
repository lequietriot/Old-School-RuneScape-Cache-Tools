package osrs;

public abstract class AbstractRasterProvider {

	public int[] pixels;

	public int width;

	public int height;

	protected float[] field4252;

	protected AbstractRasterProvider() {
	}

	public abstract void drawFull(int var1, int var2);

	public abstract void draw(int var1, int var2, int var3, int var4);

	public void apply() {
		Rasterizer2D.method2659(this.pixels, this.width, this.height, this.field4252);
	}

	public final void method2685(boolean var1) {
		this.field4252 = var1 ? new float[this.width * this.height + 1] : null;
	}
}