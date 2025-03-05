package osrs;

public class Clips {

	public boolean field2278;

	public boolean field2289;

	public int field2282;

	public int field2290;

	public TextureLoader Rasterizer3D_textureLoader;

	public int field2284;

	public int field2287;

	public int field2285;

	public int field2281;

	public int clipNegativeMidX;

	public int field2286;

	public int clipNegativeMidY;

	public int field2279;

	public int[] Rasterizer3D_rowOffsets;

	Clips() {
		this.field2278 = false;
		this.field2289 = true;
		this.field2282 = 0;
		this.field2290 = 512;
		this.Rasterizer3D_rowOffsets = new int[1024];
	}


	void method1400() {
		this.field2284 = this.field2285 / 2;
		this.field2287 = this.field2281 / 2;
		this.clipNegativeMidX = -this.field2284;
		this.field2286 = this.field2285 - this.field2284;
		this.clipNegativeMidY = -this.field2287;
		this.field2279 = this.field2281 - this.field2287;
	}


	void method1401(int var1, int var2, int var3, int var4) {
		this.field2284 = var1 - var2;
		this.field2287 = var3 - var4;
		this.clipNegativeMidX = -this.field2284;
		this.field2286 = this.field2285 - this.field2284;
		this.clipNegativeMidY = -this.field2287;
		this.field2279 = this.field2281 - this.field2287;
	}


	void method1402(int var1, int var2, int var3) {
		this.field2278 = var1 < 0 || var1 > this.field2285 || var2 < 0 || var2 > this.field2285 || var3 < 0 || var3 > this.field2285;
	}
}