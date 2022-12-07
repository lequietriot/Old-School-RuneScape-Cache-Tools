package rs3;

import com.displee.cache.CacheLibrary;
import lombok.Getter;
import lombok.Setter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

@Getter
@Setter
public class TextureData {

	private int id;
	private byte[] data;
	private int[] pixels;
	private byte[] imageData;
	private BufferedImage image;

	public TextureData(int id) {
		this.id = id;
	}

	public void decode(CacheLibrary library) {
		data = library.index(43).archive(id).file(0).getData();
		final byte format = (byte) (data[0] & 0xFF);
		try {
			switch (format) {
				case 1:
					byte[] tempPixels = new byte[data.length - 5];
					System.arraycopy(data, 5, tempPixels, 0, data.length - 5);
					pixels = method2671(tempPixels, false);
					break;
				case 6:
					int[] var14 = null;
					int offset = 1;
					for (int i = 0; i < 6; ++i) {
						int someLength = (data[offset] & 255) << 24 | (data[1 + offset] & 255) << 16 | (data[offset + 2] & 255) << 8 | data[3 + offset] & 255;
						byte[] var11 = new byte[someLength];
						System.arraycopy(data, offset + 4, var11, 0, someLength);
						int[] var12 = method2671(var11, false);
						if (i == 0) {
							var14 = new int[var12.length * 6];
						}
						System.arraycopy(var12, 0, var14, var12.length * i, var12.length);
						offset += 4 + someLength;
					}
					pixels = var14;
					break;
				default:
					throw new IllegalStateException("Unknown format=" + format);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/*private int[] getPixelsMethod1(CacheLibrary library, Class595 var1, int var2, double var3) {
		byte[] var5 = library.getIndex(43).getArchive(var2).getFile(0).getData();//this.method2672(var1, var2);
		if (null == var5) {
			return null;
		} else {
			try {
				byte var6 = (byte) (var5[0] & 255);
				if (true) {
					if (6 != var6) {
						return null;
					} else {
						int[] var14 = null;
						int var15 = 1;

						for (int var9 = 0; var9 < 6; ++var9) {
							int var10 = (var5[var15] & 255) << 24 | (var5[1 + var15] & 255) << 16 | (var5[var15 + 2] & 255) << 8 | var5[3 + var15] & 255;
							byte[] var11 = new byte[var10];
							System.arraycopy(var5, var15 + 4, var11, 0, var10);
							int[] var12 = this.method2671(var11, false);
							if (0 == var9) {
								var14 = new int[var12.length * 6];
							}
							System.arraycopy(var12, 0, var14, var12.length * var9, var12.length);
							var15 += 4 + var10;
						}

						if (1.0D != var3) {
							//IsaacCipher.method612(var14, var3);
						}

						return var14;
					}
				} else if (var6 != 1) {
					return null;
				} else {
					byte[] var7 = new byte[var5.length - 5];
					System.arraycopy(var5, 5, var7, 0, var5.length - 5);
					int[] var8 = this.method2671(var7, false);
					if (null == var8) {
						return null;
					} else {
						if (var3 != 1.0D) {
							//IsaacCipher.method612(var8, var3);
						}

						return var8;
					}
				}
			} catch (IOException var13) {
				return null;
			}
		}
	}*/

	private int[] method2671(byte[] imageData, boolean var2) throws IOException {
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
		this.imageData = imageData;
		this.image = image;
		if (image == null) {
			return null;
		} else {
			int[] var5 = getRGB(image);
			if (var2) {
				for (int var6 = image.getHeight() - 1; var6 >= 0; --var6) {
					int var7 = var6 * image.getWidth();
					for (int var8 = (var6 + 1) * image.getWidth(); var7 < var8; ++var7) {
						--var8;
						int var9 = var5[var7];
						var5[var7] = var5[var8];
						var5[var8] = var9;
					}
				}
			}
			return var5;
		}
	}

	private static int[] getRGB(BufferedImage bufferedimage) {
		if (bufferedimage.getType() == 10 || bufferedimage.getType() == 0) {
			int[] is = null;
			is = bufferedimage.getRaster().getPixels(0, 0, bufferedimage.getWidth(), bufferedimage.getHeight(), is);
			int[] is_6_ = (new int[bufferedimage.getWidth() * bufferedimage.getHeight()]);
			if (bufferedimage.getType() == 10) {
				for (int i_7_ = 0; i_7_ < is_6_.length; i_7_++) {
					is_6_[i_7_] = is[i_7_] + ((is[i_7_] << 16) + (is[i_7_] << 8)) + -16777216;
				}
			} else {
				for (int i_8_ = 0; i_8_ < is_6_.length; i_8_++) {
					int i_9_ = 2 * i_8_;
					is_6_[i_8_] = ((is[i_9_ + 1] << 24) + is[i_9_] + ((is[i_9_] << 16) + (is[i_9_] << 8)));
				}
			}
			return is_6_;
		}
		return bufferedimage.getRGB(0, 0, bufferedimage.getWidth(), bufferedimage.getHeight(), null, 0, bufferedimage.getWidth());
	}

	/**
	 * Convert the pixels of this texture to a {@code ByteBuffer} {@code Object}.
	 * @return The byte buffer.
	 */
	public ByteBuffer toByteBuffer() {
		ByteBuffer buffer = ByteBuffer.allocateDirect(pixels.length * 4);
		for (int pixel : pixels) {
			buffer.put((byte) ((pixel >> 16) & 0xFF));
			buffer.put((byte) ((pixel >> 8) & 0xFF));
			buffer.put((byte) (pixel & 0xFF));
			buffer.put((byte) (pixel >> 24));
		}
		buffer.flip();
		return buffer;
	}

}