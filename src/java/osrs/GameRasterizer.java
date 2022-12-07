package osrs;

public class GameRasterizer extends GameRaster {

	private static GameRasterizer instance;

	public static GameRasterizer getInstance() {
		return instance;
	}

	public static void setInstance(GameRasterizer rasterizer) {
		instance = rasterizer;
	}

	public boolean[] aBooleanArray1663 = new boolean[4096];
	 public boolean[] cullFaces = new boolean[4096];
	 public boolean[] cullFacesOther = new boolean[4096];
	 public int[] vertexScreenX = new int[4096];
	 public int[] vertexScreenY = new int[4096];
	 public int[] vertexScreenZ = new int[4096];
	 public int[] camera_vertex_x = new int[4096];
	 public int[] camera_vertex_y = new int[4096];
	 public int[] camera_vertex_z = new int[4096];
	 public int[] depthListIndices = new int[1500];
	 public int[] anIntArray1673 = new int[12];
	 public int[] anIntArray1675 = new int[2000];
	 public int[] anIntArray1676 = new int[2000];
	 public int[] anIntArray1677 = new int[12];
	 public int[] anIntArray1678 = new int[10];
	 public int[] anIntArray1679 = new int[10];
	 public int[] anIntArray1680 = new int[10];
	 public int[][] faceList = new int[1500][512];
	 public int[][] anIntArrayArray1674 = new int[12][2000];

	public boolean restrictEdges;
	public int anInt1481;
	public int[] anIntArray1480 = new int[50];
	public int[] colourPalette = new int[0x10000];
	public boolean approximateAlphaBlending = true;
	public int currentAlpha;
	public Point2D viewCenter;
	public int[] scanOffsets;
	boolean currentTextureTransparent;
	int anInt1477;


	public void dispose() {
		scanOffsets = null;
		anIntArray1480 = null;
		colourPalette = null;
	}

	public void drawLine(int[] pixels, int i, int j, int k, int startX, int endX, int j1, int k1) {
		if (approximateAlphaBlending) {
			int l1;
			if (restrictEdges) {
				if (endX - startX > 3) {
					l1 = (k1 - j1) / (endX - startX);
				} else {
					l1 = 0;
				}
				if (endX > this.maxRight) {
					endX = this.maxRight;
				}
				if (startX < 0) {
					j1 -= startX * l1;
					startX = 0;
				}
				if (startX >= endX)
					return;
				i += startX;
				k = endX - startX >> 2;
				l1 <<= 2;
			} else {
				if (startX >= endX)
					return;
				i += startX;
				k = endX - startX >> 2;
				if (k > 0) {
					l1 = (k1 - j1) * GraphicConstants.SHADOW_DECAY[k] >> 15;
				} else {
					l1 = 0;
				}
			}
			if (currentAlpha == 0) {
				while (--k >= 0) {
					j = colourPalette[j1 >> 8];
					j1 += l1;
					pixels[i++] = j;
					pixels[i++] = j;
					pixels[i++] = j;
					pixels[i++] = j;
				}
				k = endX - startX & 3;
				if (k > 0) {
					j = colourPalette[j1 >> 8];
					do {
						pixels[i++] = j;
					} while (--k > 0);
					return;
				}
			} else {
				int j2 = currentAlpha;
				int l2 = 256 - currentAlpha;
				while (--k >= 0) {
					j = colourPalette[j1 >> 8];
					j1 += l1;
					j = ((j & 0xff00ff) * l2 >> 8 & 0xff00ff) + ((j & 0xff00) * l2 >> 8 & 0xff00);
					pixels[i++] = j + ((pixels[i] & 0xff00ff) * j2 >> 8 & 0xff00ff)
							+ ((pixels[i] & 0xff00) * j2 >> 8 & 0xff00);
					pixels[i++] = j + ((pixels[i] & 0xff00ff) * j2 >> 8 & 0xff00ff)
							+ ((pixels[i] & 0xff00) * j2 >> 8 & 0xff00);
					pixels[i++] = j + ((pixels[i] & 0xff00ff) * j2 >> 8 & 0xff00ff)
							+ ((pixels[i] & 0xff00) * j2 >> 8 & 0xff00);
					pixels[i++] = j + ((pixels[i] & 0xff00ff) * j2 >> 8 & 0xff00ff)
							+ ((pixels[i] & 0xff00) * j2 >> 8 & 0xff00);
				}
				k = endX - startX & 3;
				if (k > 0) {
					j = colourPalette[j1 >> 8];
					j = ((j & 0xff00ff) * l2 >> 8 & 0xff00ff) + ((j & 0xff00) * l2 >> 8 & 0xff00);
					do {
						pixels[i++] = j + ((pixels[i] & 0xff00ff) * j2 >> 8 & 0xff00ff)
								+ ((pixels[i] & 0xff00) * j2 >> 8 & 0xff00);
					} while (--k > 0);
				}
			}
			return;
		}
		if (startX >= endX)
			return;
		int i2 = (k1 - j1) / (endX - startX);
		if (restrictEdges) {
			if (endX > this.maxRight) {
				endX = this.maxRight;
			}
			if (startX < 0) {
				j1 -= startX * i2;
				startX = 0;
			}
			if (startX >= endX)
				return;
		}
		i += startX;
		k = endX - startX;
		if (currentAlpha == 0) {
			do {
				pixels[i++] = colourPalette[j1 >> 8];
				j1 += i2;
			} while (--k > 0);
			return;
		}
		int k2 = currentAlpha;
		int i3 = 256 - currentAlpha;
		do {
			j = colourPalette[j1 >> 8];
			j1 += i2;
			j = ((j & 0xff00ff) * i3 >> 8 & 0xff00ff) + ((j & 0xff00) * i3 >> 8 & 0xff00);
			pixels[i++] = j + ((pixels[i] & 0xff00ff) * k2 >> 8 & 0xff00ff) + ((pixels[i] & 0xff00) * k2 >> 8 & 0xff00);
		} while (--k > 0);
	}

	public void drawShadedTriangle(int i, int j, int k, int l, int i1, int j1, int r, int g, int b) {
		int j2 = 0;
		int k2 = 0;
		if (j != i) {
			j2 = (i1 - l << 16) / (j - i);
			k2 = (g - r << 15) / (j - i);
		}

		int l2 = 0;
		int i3 = 0;
		if (k != j) {
			l2 = (j1 - i1 << 16) / (k - j);
			i3 = (b - g << 15) / (k - j);
		}

		int j3 = 0;
		int k3 = 0;
		if (k != i) {
			j3 = (l - j1 << 16) / (i - k);
			k3 = (r - b << 15) / (i - k);
		}

		if (i <= j && i <= k) {
			if (i >= this.getClipTop())
				return;
			if (j > this.getClipTop()) {
				j = this.getClipTop();
			}
			if (k > this.getClipTop()) {
				k = this.getClipTop();
			}
			if (j < k) {
				j1 = l <<= 16;
				b = r <<= 15;
				if (i < 0) {
					j1 -= j3 * i;
					l -= j2 * i;
					b -= k3 * i;
					r -= k2 * i;
					i = 0;
				}
				i1 <<= 16;
				g <<= 15;
				if (j < 0) {
					i1 -= l2 * j;
					g -= i3 * j;
					j = 0;
				}
				if (i != j && j3 < j2 || i == j && j3 > l2) {
					k -= j;
					j -= i;
					for (i = scanOffsets[i]; --j >= 0; i += this.width) {
						drawLine(this.raster, i, 0, 0, j1 >> 16, l >> 16, b >> 7, r >> 7);
						j1 += j3;
						l += j2;
						b += k3;
						r += k2;
					}

					while (--k >= 0) {
						drawLine(this.raster, i, 0, 0, j1 >> 16, i1 >> 16, b >> 7, g >> 7);
						j1 += j3;
						i1 += l2;
						b += k3;
						g += i3;
						i += this.width;
					}
					return;
				}
				k -= j;
				j -= i;
				for (i = scanOffsets[i]; --j >= 0; i += this.width) {
					drawLine(this.raster, i, 0, 0, l >> 16, j1 >> 16, r >> 7, b >> 7);
					j1 += j3;
					l += j2;
					b += k3;
					r += k2;
				}

				while (--k >= 0) {
					drawLine(this.raster, i, 0, 0, i1 >> 16, j1 >> 16, g >> 7, b >> 7);
					j1 += j3;
					i1 += l2;
					b += k3;
					g += i3;
					i += this.width;
				}
				return;
			}
			i1 = l <<= 16;
			g = r <<= 15;
			if (i < 0) {
				i1 -= j3 * i;
				l -= j2 * i;
				g -= k3 * i;
				r -= k2 * i;
				i = 0;
			}
			j1 <<= 16;
			b <<= 15;
			if (k < 0) {
				j1 -= l2 * k;
				b -= i3 * k;
				k = 0;
			}
			if (i != k && j3 < j2 || i == k && l2 > j2) {
				j -= k;
				k -= i;
				for (i = scanOffsets[i]; --k >= 0; i += this.width) {
					drawLine(this.raster, i, 0, 0, i1 >> 16, l >> 16, g >> 7, r >> 7);
					i1 += j3;
					l += j2;
					g += k3;
					r += k2;
				}

				while (--j >= 0) {
					drawLine(this.raster, i, 0, 0, j1 >> 16, l >> 16, b >> 7, r >> 7);
					j1 += l2;
					l += j2;
					b += i3;
					r += k2;
					i += this.width;
				}
				return;
			}
			j -= k;
			k -= i;
			for (i = scanOffsets[i]; --k >= 0; i += this.width) {
				drawLine(this.raster, i, 0, 0, l >> 16, i1 >> 16, r >> 7, g >> 7);
				i1 += j3;
				l += j2;
				g += k3;
				r += k2;
			}

			while (--j >= 0) {
				drawLine(this.raster, i, 0, 0, l >> 16, j1 >> 16, r >> 7, b >> 7);
				j1 += l2;
				l += j2;
				b += i3;
				r += k2;
				i += this.width;
			}
			return;
		}
		if (j <= k) {
			if (j >= this.getClipTop())
				return;
			if (k > this.getClipTop()) {
				k = this.getClipTop();
			}
			if (i > this.getClipTop()) {
				i = this.getClipTop();
			}
			if (k < i) {
				l = i1 <<= 16;
				r = g <<= 15;
				if (j < 0) {
					l -= j2 * j;
					i1 -= l2 * j;
					r -= k2 * j;
					g -= i3 * j;
					j = 0;
				}
				j1 <<= 16;
				b <<= 15;
				if (k < 0) {
					j1 -= j3 * k;
					b -= k3 * k;
					k = 0;
				}
				if (j != k && j2 < l2 || j == k && j2 > j3) {
					i -= k;
					k -= j;
					for (j = scanOffsets[j]; --k >= 0; j += this.width) {
						drawLine(this.raster, j, 0, 0, l >> 16, i1 >> 16, r >> 7, g >> 7);
						l += j2;
						i1 += l2;
						r += k2;
						g += i3;
					}

					while (--i >= 0) {
						drawLine(this.raster, j, 0, 0, l >> 16, j1 >> 16, r >> 7, b >> 7);
						l += j2;
						j1 += j3;
						r += k2;
						b += k3;
						j += this.width;
					}
					return;
				}
				i -= k;
				k -= j;
				for (j = scanOffsets[j]; --k >= 0; j += this.width) {
					drawLine(this.raster, j, 0, 0, i1 >> 16, l >> 16, g >> 7, r >> 7);
					l += j2;
					i1 += l2;
					r += k2;
					g += i3;
				}

				while (--i >= 0) {
					drawLine(this.raster, j, 0, 0, j1 >> 16, l >> 16, b >> 7, r >> 7);
					l += j2;
					j1 += j3;
					r += k2;
					b += k3;
					j += this.width;
				}
				return;
			}
			j1 = i1 <<= 16;
			b = g <<= 15;
			if (j < 0) {
				j1 -= j2 * j;
				i1 -= l2 * j;
				b -= k2 * j;
				g -= i3 * j;
				j = 0;
			}
			l <<= 16;
			r <<= 15;
			if (i < 0) {
				l -= j3 * i;
				r -= k3 * i;
				i = 0;
			}
			if (j2 < l2) {
				k -= i;
				i -= j;
				for (j = scanOffsets[j]; --i >= 0; j += this.width) {
					drawLine(this.raster, j, 0, 0, j1 >> 16, i1 >> 16, b >> 7, g >> 7);
					j1 += j2;
					i1 += l2;
					b += k2;
					g += i3;
				}

				while (--k >= 0) {
					drawLine(this.raster, j, 0, 0, l >> 16, i1 >> 16, r >> 7, g >> 7);
					l += j3;
					i1 += l2;
					r += k3;
					g += i3;
					j += this.width;
				}
				return;
			}
			k -= i;
			i -= j;
			for (j = scanOffsets[j]; --i >= 0; j += this.width) {
				drawLine(this.raster, j, 0, 0, i1 >> 16, j1 >> 16, g >> 7, b >> 7);
				j1 += j2;
				i1 += l2;
				b += k2;
				g += i3;
			}

			while (--k >= 0) {
				drawLine(this.raster, j, 0, 0, i1 >> 16, l >> 16, g >> 7, r >> 7);
				l += j3;
				i1 += l2;
				r += k3;
				g += i3;
				j += this.width;
			}
			return;
		}
		if (k >= this.getClipTop())
			return;
		if (i > this.getClipTop()) {
			i = this.getClipTop();
		}
		if (j > this.getClipTop()) {
			j = this.getClipTop();
		}
		if (i < j) {
			i1 = j1 <<= 16;
			g = b <<= 15;
			if (k < 0) {
				i1 -= l2 * k;
				j1 -= j3 * k;
				g -= i3 * k;
				b -= k3 * k;
				k = 0;
			}
			l <<= 16;
			r <<= 15;
			if (i < 0) {
				l -= j2 * i;
				r -= k2 * i;
				i = 0;
			}
			if (l2 < j3) {
				j -= i;
				i -= k;
				for (k = scanOffsets[k]; --i >= 0; k += this.width) {
					drawLine(this.raster, k, 0, 0, i1 >> 16, j1 >> 16, g >> 7, b >> 7);
					i1 += l2;
					j1 += j3;
					g += i3;
					b += k3;
				}

				while (--j >= 0) {
					drawLine(this.raster, k, 0, 0, i1 >> 16, l >> 16, g >> 7, r >> 7);
					i1 += l2;
					l += j2;
					g += i3;
					r += k2;
					k += this.width;
				}
				return;
			}
			j -= i;
			i -= k;
			for (k = scanOffsets[k]; --i >= 0; k += this.width) {
				drawLine(this.raster, k, 0, 0, j1 >> 16, i1 >> 16, b >> 7, g >> 7);
				i1 += l2;
				j1 += j3;
				g += i3;
				b += k3;
			}

			while (--j >= 0) {
				drawLine(this.raster, k, 0, 0, l >> 16, i1 >> 16, r >> 7, g >> 7);
				i1 += l2;
				l += j2;
				g += i3;
				r += k2;
				k += this.width;
			}
			return;
		}
		l = j1 <<= 16;
		r = b <<= 15;
		if (k < 0) {
			l -= l2 * k;
			j1 -= j3 * k;
			r -= i3 * k;
			b -= k3 * k;
			k = 0;
		}
		i1 <<= 16;
		g <<= 15;
		if (j < 0) {
			i1 -= j2 * j;
			g -= k2 * j;
			j = 0;
		}
		if (l2 < j3) {
			i -= j;
			j -= k;
			for (k = scanOffsets[k]; --j >= 0; k += this.width) {
				drawLine(this.raster, k, 0, 0, l >> 16, j1 >> 16, r >> 7, b >> 7);
				l += l2;
				j1 += j3;
				r += i3;
				b += k3;
			}

			while (--i >= 0) {
				drawLine(this.raster, k, 0, 0, i1 >> 16, j1 >> 16, g >> 7, b >> 7);
				i1 += j2;
				j1 += j3;
				g += k2;
				b += k3;
				k += this.width;
			}
			return;
		}
		i -= j;
		j -= k;
		for (k = scanOffsets[k]; --j >= 0; k += this.width) {
			drawLine(this.raster, k, 0, 0, j1 >> 16, l >> 16, b >> 7, r >> 7);
			l += l2;
			j1 += j3;
			r += i3;
			b += k3;
		}

		while (--i >= 0) {
			drawLine(this.raster, k, 0, 0, j1 >> 16, i1 >> 16, b >> 7, g >> 7);
			i1 += j2;
			j1 += j3;
			g += k2;
			b += k3;
			k += this.width;
		}
	}

	public void drawShadedTriangle(int i, int j, int k, int l, int i1, int j1, int k1) {
		int l1 = 0;
		if (j != i) {
			l1 = (i1 - l << 16) / (j - i);
		}
		int i2 = 0;
		if (k != j) {
			i2 = (j1 - i1 << 16) / (k - j);
		}
		int j2 = 0;
		if (k != i) {
			j2 = (l - j1 << 16) / (i - k);
		}
		if (i <= j && i <= k) {
			if (i >= this.getClipTop())
				return;
			if (j > this.getClipTop()) {
				j = this.getClipTop();
			}
			if (k > this.getClipTop()) {
				k = this.getClipTop();
			}
			if (j < k) {
				j1 = l <<= 16;
				if (i < 0) {
					j1 -= j2 * i;
					l -= l1 * i;
					i = 0;
				}
				i1 <<= 16;
				if (j < 0) {
					i1 -= i2 * j;
					j = 0;
				}
				if (i != j && j2 < l1 || i == j && j2 > i2) {
					k -= j;
					j -= i;
					for (i = scanOffsets[i]; --j >= 0; i += this.width) {
						method377(this.raster, i, k1, 0, j1 >> 16, l >> 16);
						j1 += j2;
						l += l1;
					}

					while (--k >= 0) {
						method377(this.raster, i, k1, 0, j1 >> 16, i1 >> 16);
						j1 += j2;
						i1 += i2;
						i += this.width;
					}
					return;
				}
				k -= j;
				j -= i;
				for (i = scanOffsets[i]; --j >= 0; i += this.width) {
					method377(this.raster, i, k1, 0, l >> 16, j1 >> 16);
					j1 += j2;
					l += l1;
				}

				while (--k >= 0) {
					method377(this.raster, i, k1, 0, i1 >> 16, j1 >> 16);
					j1 += j2;
					i1 += i2;
					i += this.width;
				}
				return;
			}
			i1 = l <<= 16;
			if (i < 0) {
				i1 -= j2 * i;
				l -= l1 * i;
				i = 0;
			}
			j1 <<= 16;
			if (k < 0) {
				j1 -= i2 * k;
				k = 0;
			}
			if (i != k && j2 < l1 || i == k && i2 > l1) {
				j -= k;
				k -= i;
				for (i = scanOffsets[i]; --k >= 0; i += this.width) {
					method377(this.raster, i, k1, 0, i1 >> 16, l >> 16);
					i1 += j2;
					l += l1;
				}

				while (--j >= 0) {
					method377(this.raster, i, k1, 0, j1 >> 16, l >> 16);
					j1 += i2;
					l += l1;
					i += this.width;
				}
				return;
			}
			j -= k;
			k -= i;
			for (i = scanOffsets[i]; --k >= 0; i += this.width) {
				method377(this.raster, i, k1, 0, l >> 16, i1 >> 16);
				i1 += j2;
				l += l1;
			}

			while (--j >= 0) {
				method377(this.raster, i, k1, 0, l >> 16, j1 >> 16);
				j1 += i2;
				l += l1;
				i += this.width;
			}
			return;
		}
		if (j <= k) {
			if (j >= this.getClipTop())
				return;
			if (k > this.getClipTop()) {
				k = this.getClipTop();
			}
			if (i > this.getClipTop()) {
				i = this.getClipTop();
			}
			if (k < i) {
				l = i1 <<= 16;
				if (j < 0) {
					l -= l1 * j;
					i1 -= i2 * j;
					j = 0;
				}
				j1 <<= 16;
				if (k < 0) {
					j1 -= j2 * k;
					k = 0;
				}
				if (j != k && l1 < i2 || j == k && l1 > j2) {
					i -= k;
					k -= j;
					for (j = scanOffsets[j]; --k >= 0; j += this.width) {
						method377(this.raster, j, k1, 0, l >> 16, i1 >> 16);
						l += l1;
						i1 += i2;
					}

					while (--i >= 0) {
						method377(this.raster, j, k1, 0, l >> 16, j1 >> 16);
						l += l1;
						j1 += j2;
						j += this.width;
					}
					return;
				}
				i -= k;
				k -= j;
				for (j = scanOffsets[j]; --k >= 0; j += this.width) {
					method377(this.raster, j, k1, 0, i1 >> 16, l >> 16);
					l += l1;
					i1 += i2;
				}

				while (--i >= 0) {
					method377(this.raster, j, k1, 0, j1 >> 16, l >> 16);
					l += l1;
					j1 += j2;
					j += this.width;
				}
				return;
			}
			j1 = i1 <<= 16;
			if (j < 0) {
				j1 -= l1 * j;
				i1 -= i2 * j;
				j = 0;
			}
			l <<= 16;
			if (i < 0) {
				l -= j2 * i;
				i = 0;
			}
			if (l1 < i2) {
				k -= i;
				i -= j;
				for (j = scanOffsets[j]; --i >= 0; j += this.width) {
					method377(this.raster, j, k1, 0, j1 >> 16, i1 >> 16);
					j1 += l1;
					i1 += i2;
				}

				while (--k >= 0) {
					method377(this.raster, j, k1, 0, l >> 16, i1 >> 16);
					l += j2;
					i1 += i2;
					j += this.width;
				}
				return;
			}
			k -= i;
			i -= j;
			for (j = scanOffsets[j]; --i >= 0; j += this.width) {
				method377(this.raster, j, k1, 0, i1 >> 16, j1 >> 16);
				j1 += l1;
				i1 += i2;
			}

			while (--k >= 0) {
				method377(this.raster, j, k1, 0, i1 >> 16, l >> 16);
				l += j2;
				i1 += i2;
				j += this.width;
			}
			return;
		}
		if (k >= this.getClipTop())
			return;
		if (i > this.getClipTop()) {
			i = this.getClipTop();
		}
		if (j > this.getClipTop()) {
			j = this.getClipTop();
		}
		if (i < j) {
			i1 = j1 <<= 16;
			if (k < 0) {
				i1 -= i2 * k;
				j1 -= j2 * k;
				k = 0;
			}
			l <<= 16;
			if (i < 0) {
				l -= l1 * i;
				i = 0;
			}
			if (i2 < j2) {
				j -= i;
				i -= k;
				for (k = scanOffsets[k]; --i >= 0; k += this.width) {
					method377(this.raster, k, k1, 0, i1 >> 16, j1 >> 16);
					i1 += i2;
					j1 += j2;
				}

				while (--j >= 0) {
					method377(this.raster, k, k1, 0, i1 >> 16, l >> 16);
					i1 += i2;
					l += l1;
					k += this.width;
				}
				return;
			}
			j -= i;
			i -= k;
			for (k = scanOffsets[k]; --i >= 0; k += this.width) {
				method377(this.raster, k, k1, 0, j1 >> 16, i1 >> 16);
				i1 += i2;
				j1 += j2;
			}

			while (--j >= 0) {
				method377(this.raster, k, k1, 0, l >> 16, i1 >> 16);
				i1 += i2;
				l += l1;
				k += this.width;
			}
			return;
		}
		l = j1 <<= 16;
		if (k < 0) {
			l -= i2 * k;
			j1 -= j2 * k;
			k = 0;
		}
		i1 <<= 16;
		if (j < 0) {
			i1 -= l1 * j;
			j = 0;
		}
		if (i2 < j2) {
			i -= j;
			j -= k;
			for (k = scanOffsets[k]; --j >= 0; k += this.width) {
				method377(this.raster, k, k1, 0, l >> 16, j1 >> 16);
				l += i2;
				j1 += j2;
			}

			while (--i >= 0) {
				method377(this.raster, k, k1, 0, i1 >> 16, j1 >> 16);
				i1 += l1;
				j1 += j2;
				k += this.width;
			}
			return;
		}
		i -= j;
		j -= k;
		for (k = scanOffsets[k]; --j >= 0; k += this.width) {
			method377(this.raster, k, k1, 0, j1 >> 16, l >> 16);
			l += i2;
			j1 += j2;
		}

		while (--i >= 0) {
			method377(this.raster, k, k1, 0, j1 >> 16, i1 >> 16);
			i1 += l1;
			j1 += j2;
			k += this.width;
		}
	}


	public void method377(int[] ai, int i, int j, int k, int l, int i1) {
		if (restrictEdges) {
			if (i1 > this.maxRight) {
				i1 = this.maxRight;
			}
			if (l < 0) {
				l = 0;
			}
		}
		if (l >= i1)
			return;
		i += l;
		k = i1 - l >> 2;
		if (currentAlpha == 0) {
			while (--k >= 0) {
				ai[i++] = j;
				ai[i++] = j;
				ai[i++] = j;
				ai[i++] = j;
			}
			for (k = i1 - l & 3; --k >= 0;) {
				ai[i++] = j;
			}

			return;
		}
		int j1 = currentAlpha;
		int k1 = 256 - currentAlpha;
		j = ((j & 0xff00ff) * k1 >> 8 & 0xff00ff) + ((j & 0xff00) * k1 >> 8 & 0xff00);
		while (--k >= 0) {
			ai[i++] = j + ((ai[i] & 0xff00ff) * j1 >> 8 & 0xff00ff) + ((ai[i] & 0xff00) * j1 >> 8 & 0xff00);
			ai[i++] = j + ((ai[i] & 0xff00ff) * j1 >> 8 & 0xff00ff) + ((ai[i] & 0xff00) * j1 >> 8 & 0xff00);
			ai[i++] = j + ((ai[i] & 0xff00ff) * j1 >> 8 & 0xff00ff) + ((ai[i] & 0xff00) * j1 >> 8 & 0xff00);
			ai[i++] = j + ((ai[i] & 0xff00ff) * j1 >> 8 & 0xff00ff) + ((ai[i] & 0xff00) * j1 >> 8 & 0xff00);
		}
		for (k = i1 - l & 3; --k >= 0;) {
			ai[i++] = j + ((ai[i] & 0xff00ff) * j1 >> 8 & 0xff00ff) + ((ai[i] & 0xff00) * j1 >> 8 & 0xff00);
		}

	}

	public void reposition(int height, int width) {
		System.out.println("Reposition " + width + ", " + height);
		scanOffsets = new int[height];

		for (int x = 0; x < height; x++) {
			scanOffsets[x] = width * x;
		}

		viewCenter = new Point2D(width / 2, height / 2);
	}

	public void setBrightness(double exponent) {
		//exponent += Math.random() * 0.03 - 0.015;
		int j = 0;

		for (int k = 0; k < 512; k++) {
			double d1 = k / 8 / 64D + 0.0078125D;
			double d2 = (k & 7) / 8D + 0.0625D;

			for (int k1 = 0; k1 < 128; k1++) {
				double initial = k1 / 128D;
				double r = initial;
				double g = initial;
				double b = initial;

				if (d2 != 0.0D) {
					double d7;
					if (initial < 0.5D) {
						d7 = initial * (1.0D + d2);
					} else {
						d7 = initial + d2 - initial * d2;
					}

					double d8 = 2D * initial - d7;
					double d9 = d1 + 0.33333333333333331D;
					if (d9 > 1.0D) {
						d9--;
					}

					double d10 = d1;
					double d11 = d1 - 0.33333333333333331D;
					if (d11 < 0.0D) {
						d11++;
					}

					if (6D * d9 < 1.0D) {
						r = d8 + (d7 - d8) * 6D * d9;
					} else if (2D * d9 < 1.0D) {
						r = d7;
					} else if (3D * d9 < 2D) {
						r = d8 + (d7 - d8) * (0.66666666666666663D - d9) * 6D;
					} else {
						r = d8;
					}

					if (6D * d10 < 1.0D) {
						g = d8 + (d7 - d8) * 6D * d10;
					} else if (2D * d10 < 1.0D) {
						g = d7;
					} else if (3D * d10 < 2D) {
						g = d8 + (d7 - d8) * (0.66666666666666663D - d10) * 6D;
					} else {
						g = d8;
					}

					if (6D * d11 < 1.0D) {
						b = d8 + (d7 - d8) * 6D * d11;
					} else if (2D * d11 < 1.0D) {
						b = d7;
					} else if (3D * d11 < 2D) {
						b = d8 + (d7 - d8) * (0.66666666666666663D - d11) * 6D;
					} else {
						b = d8;
					}
				}
				int newR = (int) (r * 256D);
				int newG = (int) (g * 256D);
				int newB = (int) (b * 256D);
				int colour = (newR << 16) + (newG << 8) + newB;

				colour = 0; //ColourUtils.exponent(colour, exponent);
				if (colour == 0) {
					colour = 1;
				}

				colourPalette[j++] = colour;
			}
		}
	}

	public void useViewport() {
		scanOffsets = new int[this.height];
		for (int j = 0; j < this.height; j++) {
			scanOffsets[j] = this.width * j;
		}

		viewCenter = new Point2D(this.width / 2, this.height / 2);
	}

}