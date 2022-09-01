package runelite.definitions;

import lombok.Data;
import osrs.GameRasterizer;
import osrs.GraphicConstants;
import runelite.models.CircularAngle;
import runelite.models.FaceNormal;
import runelite.models.VertexNormal;

import java.util.Arrays;

@Data
public class ModelDefinition
{
	public int id;
	public byte[] modelData;

	public int vertexCount = 0;
	public int[] vertexX;
	public int[] vertexY;
	public int[] vertexZ;
	public transient VertexNormal[] vertexNormals;

	public int faceCount;
	public int[] faceIndices1;
	public int[] faceIndices2;
	public int[] faceIndices3;
	public byte[] faceTransparencies;
	public short[] faceColors;
	public byte[] faceRenderPriorities;
	public byte[] faceRenderTypes;
	public transient FaceNormal[] faceNormals;

	public int numTextureFaces;
	public short[] texIndices1;
	public short[] texIndices2;
	public short[] texIndices3;
	public transient float[][] faceTextureUCoordinates;
	public transient float[][] faceTextureVCoordinates;
	public short[] texturePrimaryColors;
	public short[] faceTextures;
	public byte[] textureCoords;
	public byte[] textureRenderTypes;

	public int[] packedVertexGroups;
	public int[] packedTransparencyVertexGroups;

	public byte priority;

	private transient int[][] vertexGroups;
	public int[][] animayaGroups;
	public int[][] animayaScales;

	private transient int[] origVX;
	private transient int[] origVY;
	private transient int[] origVZ;

	public transient int maxPriority;

	public static transient int animOffsetX, animOffsetY, animOffsetZ;

	public void computeNormals()
	{
		if (this.vertexNormals != null)
		{
			return;
		}

		this.vertexNormals = new VertexNormal[this.vertexCount];

		int var1;
		for (var1 = 0; var1 < this.vertexCount; ++var1)
		{
			this.vertexNormals[var1] = new VertexNormal();
		}

		for (var1 = 0; var1 < this.faceCount; ++var1)
		{
			int vertexA = this.faceIndices1[var1];
			int vertexB = this.faceIndices2[var1];
			int vertexC = this.faceIndices3[var1];

			int xA = this.vertexX[vertexB] - this.vertexX[vertexA];
			int yA = this.vertexY[vertexB] - this.vertexY[vertexA];
			int zA = this.vertexZ[vertexB] - this.vertexZ[vertexA];

			int xB = this.vertexX[vertexC] - this.vertexX[vertexA];
			int yB = this.vertexY[vertexC] - this.vertexY[vertexA];
			int zB = this.vertexZ[vertexC] - this.vertexZ[vertexA];

			// Compute cross product
			int var11 = yA * zB - yB * zA;
			int var12 = zA * xB - zB * xA;
			int var13 = xA * yB - xB * yA;

			while (var11 > 8192 || var12 > 8192 || var13 > 8192 || var11 < -8192 || var12 < -8192 || var13 < -8192)
			{
				var11 >>= 1;
				var12 >>= 1;
				var13 >>= 1;
			}

			int length = (int) Math.sqrt((double) (var11 * var11 + var12 * var12 + var13 * var13));
			if (length <= 0)
			{
				length = 1;
			}

			var11 = var11 * 256 / length;
			var12 = var12 * 256 / length;
			var13 = var13 * 256 / length;

			byte var15;
			if (this.faceRenderTypes == null)
			{
				var15 = 0;
			}
			else
			{
				var15 = this.faceRenderTypes[var1];
			}

			if (var15 == 0)
			{
				VertexNormal var16 = this.vertexNormals[vertexA];
				var16.x += var11;
				var16.y += var12;
				var16.z += var13;
				++var16.magnitude;

				var16 = this.vertexNormals[vertexB];
				var16.x += var11;
				var16.y += var12;
				var16.z += var13;
				++var16.magnitude;

				var16 = this.vertexNormals[vertexC];
				var16.x += var11;
				var16.y += var12;
				var16.z += var13;
				++var16.magnitude;
			}
			else if (var15 == 1)
			{
				if (this.faceNormals == null)
				{
					this.faceNormals = new FaceNormal[this.faceCount];
				}

				FaceNormal var17 = this.faceNormals[var1] = new FaceNormal();
				var17.x = var11;
				var17.y = var12;
				var17.z = var13;
			}
		}
	}

	/**
	 * Computes the UV coordinates for every three-vertex face that has a
	 * texture.
	 */
	public void computeTextureUVCoordinates()
	{
		this.faceTextureUCoordinates = new float[faceCount][];
		this.faceTextureVCoordinates = new float[faceCount][];

		for (int i = 0; i < faceCount; i++)
		{
			int textureCoordinate;
			if (textureCoords == null)
			{
				textureCoordinate = -1;
			}
			else
			{
				textureCoordinate = textureCoords[i];
			}

			int textureIdx;
			if (faceTextures == null)
			{
				textureIdx = -1;
			}
			else
			{
				textureIdx = faceTextures[i] & 0xFFFF;
			}

			if (textureIdx != -1)
			{
				float[] u = new float[3];
				float[] v = new float[3];

				if (textureCoordinate == -1)
				{
					u[0] = 0.0F;
					v[0] = 1.0F;

					u[1] = 1.0F;
					v[1] = 1.0F;

					u[2] = 0.0F;
					v[2] = 0.0F;
				}
				else
				{
					textureCoordinate &= 0xFF;

					byte textureRenderType = 0;
					if (textureRenderTypes != null)
					{
						textureRenderType = textureRenderTypes[textureCoordinate];
					}

					if (textureRenderType == 0)
					{
						int faceVertexIdx1 = faceIndices1[i];
						int faceVertexIdx2 = faceIndices2[i];
						int faceVertexIdx3 = faceIndices3[i];

						short triangleVertexIdx1 = texIndices1[textureCoordinate];
						short triangleVertexIdx2 = texIndices2[textureCoordinate];
						short triangleVertexIdx3 = texIndices3[textureCoordinate];

						float triangleX = (float) vertexX[triangleVertexIdx1];
						float triangleY = (float) vertexY[triangleVertexIdx1];
						float triangleZ = (float) vertexZ[triangleVertexIdx1];

						float f_882_ = (float) vertexX[triangleVertexIdx2] - triangleX;
						float f_883_ = (float) vertexY[triangleVertexIdx2] - triangleY;
						float f_884_ = (float) vertexZ[triangleVertexIdx2] - triangleZ;
						float f_885_ = (float) vertexX[triangleVertexIdx3] - triangleX;
						float f_886_ = (float) vertexY[triangleVertexIdx3] - triangleY;
						float f_887_ = (float) vertexZ[triangleVertexIdx3] - triangleZ;
						float f_888_ = (float) vertexX[faceVertexIdx1] - triangleX;
						float f_889_ = (float) vertexY[faceVertexIdx1] - triangleY;
						float f_890_ = (float) vertexZ[faceVertexIdx1] - triangleZ;
						float f_891_ = (float) vertexX[faceVertexIdx2] - triangleX;
						float f_892_ = (float) vertexY[faceVertexIdx2] - triangleY;
						float f_893_ = (float) vertexZ[faceVertexIdx2] - triangleZ;
						float f_894_ = (float) vertexX[faceVertexIdx3] - triangleX;
						float f_895_ = (float) vertexY[faceVertexIdx3] - triangleY;
						float f_896_ = (float) vertexZ[faceVertexIdx3] - triangleZ;

						float f_897_ = f_883_ * f_887_ - f_884_ * f_886_;
						float f_898_ = f_884_ * f_885_ - f_882_ * f_887_;
						float f_899_ = f_882_ * f_886_ - f_883_ * f_885_;
						float f_900_ = f_886_ * f_899_ - f_887_ * f_898_;
						float f_901_ = f_887_ * f_897_ - f_885_ * f_899_;
						float f_902_ = f_885_ * f_898_ - f_886_ * f_897_;
						float f_903_ = 1.0F / (f_900_ * f_882_ + f_901_ * f_883_ + f_902_ * f_884_);

						u[0] = (f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_) * f_903_;
						u[1] = (f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_) * f_903_;
						u[2] = (f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_) * f_903_;

						f_900_ = f_883_ * f_899_ - f_884_ * f_898_;
						f_901_ = f_884_ * f_897_ - f_882_ * f_899_;
						f_902_ = f_882_ * f_898_ - f_883_ * f_897_;
						f_903_ = 1.0F / (f_900_ * f_885_ + f_901_ * f_886_ + f_902_ * f_887_);

						v[0] = (f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_) * f_903_;
						v[1] = (f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_) * f_903_;
						v[2] = (f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_) * f_903_;
					}
				}

				this.faceTextureUCoordinates[i] = u;
				this.faceTextureVCoordinates[i] = v;
			}
		}
	}

	public void computeAnimationTables()
	{
		if (this.packedVertexGroups != null)
		{
			int[] groupCounts = new int[256];
			int numGroups = 0;
			int var3, var4;

			for (var3 = 0; var3 < this.vertexCount; ++var3)
			{
				var4 = this.packedVertexGroups[var3];
				++groupCounts[var4];
				if (var4 > numGroups)
				{
					numGroups = var4;
				}
			}

			this.vertexGroups = new int[numGroups + 1][];

			for (var3 = 0; var3 <= numGroups; ++var3)
			{
				this.vertexGroups[var3] = new int[groupCounts[var3]];
				groupCounts[var3] = 0;
			}

			for (var3 = 0; var3 < this.vertexCount; this.vertexGroups[var4][groupCounts[var4]++] = var3++)
			{
				var4 = this.packedVertexGroups[var3];
			}

			this.packedVertexGroups = null;
		}

		// triangleSkinValues is here
	}

	public void rotate(int orientation)
	{
		int sin = CircularAngle.SINE[orientation];
		int cos = CircularAngle.COSINE[orientation];

		assert vertexX.length == vertexY.length;
		assert vertexY.length == vertexZ.length;

		for (int i = 0; i < vertexX.length; ++i)
		{
			vertexX[i] = vertexX[i] * cos + vertexZ[i] * sin >> 16;
			vertexZ[i] = vertexZ[i] * cos - vertexX[i] * sin >> 16;
		}

		reset();
	}

	public void resetAnim()
	{
		if (origVX == null)
		{
			return;
		}

		System.arraycopy(origVX, 0, vertexX, 0, origVX.length);
		System.arraycopy(origVY, 0, vertexY, 0, origVY.length);
		System.arraycopy(origVZ, 0, vertexZ, 0, origVZ.length);
	}

	public void animate(int type, int[] frameMap, int dx, int dy, int dz)
	{
		if (origVX == null)
		{
			origVX = Arrays.copyOf(vertexX, vertexX.length);
			origVY = Arrays.copyOf(vertexY, vertexY.length);
			origVZ = Arrays.copyOf(vertexZ, vertexZ.length);
		}

		final int[] verticesX = vertexX;
		final int[] verticesY = vertexY;
		final int[] verticesZ = vertexZ;
		int var6 = frameMap.length;
		int var7;
		int var8;
		int var11;
		int var12;
		if (type == 0)
		{
			var7 = 0;
			animOffsetX = 0;
			animOffsetY = 0;
			animOffsetZ = 0;

			for (var8 = 0; var8 < var6; ++var8)
			{
				int var9 = frameMap[var8];
				if (var9 < this.vertexGroups.length)
				{
					int[] var10 = this.vertexGroups[var9];

					for (var11 = 0; var11 < var10.length; ++var11)
					{
						var12 = var10[var11];
						animOffsetX += verticesX[var12];
						animOffsetY += verticesY[var12];
						animOffsetZ += verticesZ[var12];
						++var7;
					}
				}
			}

			if (var7 > 0)
			{
				animOffsetX = dx + animOffsetX / var7;
				animOffsetY = dy + animOffsetY / var7;
				animOffsetZ = dz + animOffsetZ / var7;
			}
			else
			{
				animOffsetX = dx;
				animOffsetY = dy;
				animOffsetZ = dz;
			}

		}
		else
		{
			int[] var18;
			int var19;
			if (type == 1)
			{
				for (var7 = 0; var7 < var6; ++var7)
				{
					var8 = frameMap[var7];
					if (var8 < this.vertexGroups.length)
					{
						var18 = this.vertexGroups[var8];

						for (var19 = 0; var19 < var18.length; ++var19)
						{
							var11 = var18[var19];
							verticesX[var11] += dx;
							verticesY[var11] += dy;
							verticesZ[var11] += dz;
						}
					}
				}

			}
			else if (type == 2)
			{
				for (var7 = 0; var7 < var6; ++var7)
				{
					var8 = frameMap[var7];
					if (var8 < this.vertexGroups.length)
					{
						var18 = this.vertexGroups[var8];

						for (var19 = 0; var19 < var18.length; ++var19)
						{
							var11 = var18[var19];
							verticesX[var11] -= animOffsetX;
							verticesY[var11] -= animOffsetY;
							verticesZ[var11] -= animOffsetZ;
							var12 = (dx & 255) * 8;
							int var13 = (dy & 255) * 8;
							int var14 = (dz & 255) * 8;
							int var15;
							int var16;
							int var17;
							if (var14 != 0)
							{
								var15 = CircularAngle.SINE[var14];
								var16 = CircularAngle.COSINE[var14];
								var17 = var15 * verticesY[var11] + var16 * verticesX[var11] >> 16;
								verticesY[var11] = var16 * verticesY[var11] - var15 * verticesX[var11] >> 16;
								verticesX[var11] = var17;
							}

							if (var12 != 0)
							{
								var15 = CircularAngle.SINE[var12];
								var16 = CircularAngle.COSINE[var12];
								var17 = var16 * verticesY[var11] - var15 * verticesZ[var11] >> 16;
								verticesZ[var11] = var15 * verticesY[var11] + var16 * verticesZ[var11] >> 16;
								verticesY[var11] = var17;
							}

							if (var13 != 0)
							{
								var15 = CircularAngle.SINE[var13];
								var16 = CircularAngle.COSINE[var13];
								var17 = var15 * verticesZ[var11] + var16 * verticesX[var11] >> 16;
								verticesZ[var11] = var16 * verticesZ[var11] - var15 * verticesX[var11] >> 16;
								verticesX[var11] = var17;
							}

							verticesX[var11] += animOffsetX;
							verticesY[var11] += animOffsetY;
							verticesZ[var11] += animOffsetZ;
						}
					}
				}

			}
			else if (type == 3)
			{
				for (var7 = 0; var7 < var6; ++var7)
				{
					var8 = frameMap[var7];
					if (var8 < this.vertexGroups.length)
					{
						var18 = this.vertexGroups[var8];

						for (var19 = 0; var19 < var18.length; ++var19)
						{
							var11 = var18[var19];
							verticesX[var11] -= animOffsetX;
							verticesY[var11] -= animOffsetY;
							verticesZ[var11] -= animOffsetZ;
							verticesX[var11] = dx * verticesX[var11] / 128;
							verticesY[var11] = dy * verticesY[var11] / 128;
							verticesZ[var11] = dz * verticesZ[var11] / 128;
							verticesX[var11] += animOffsetX;
							verticesY[var11] += animOffsetY;
							verticesZ[var11] += animOffsetZ;
						}
					}
				}

			}
			else if (type == 5)
			{
				// alpha animation
			}
		}
	}

	public void method1493()
	{
		int var1;
		for (var1 = 0; var1 < this.vertexCount; ++var1)
		{
			this.vertexZ[var1] = -this.vertexZ[var1];
		}

		for (var1 = 0; var1 < this.faceCount; ++var1)
		{
			int var2 = this.faceIndices1[var1];
			this.faceIndices1[var1] = this.faceIndices3[var1];
			this.faceIndices3[var1] = var2;
		}

		reset();
	}

	public void rotate1()
	{
		for (int var1 = 0; var1 < this.vertexCount; ++var1)
		{
			int var2 = this.vertexX[var1];
			this.vertexX[var1] = this.vertexZ[var1];
			this.vertexZ[var1] = -var2;
		}

		reset();
	}

	public void rotate2()
	{
		for (int var1 = 0; var1 < this.vertexCount; ++var1)
		{
			this.vertexX[var1] = -this.vertexX[var1];
			this.vertexZ[var1] = -this.vertexZ[var1];
		}

		reset();
	}

	public void rotate3()
	{
		for (int var1 = 0; var1 < this.vertexCount; ++var1)
		{
			int var2 = this.vertexZ[var1];
			this.vertexZ[var1] = this.vertexX[var1];
			this.vertexX[var1] = -var2;
		}

		reset();
	}

	private void reset()
	{
		vertexNormals = null;
		faceNormals = null;
		faceTextureUCoordinates = faceTextureVCoordinates = null;
	}

	public void resize(int var1, int var2, int var3)
	{
		for (int var4 = 0; var4 < this.vertexCount; ++var4)
		{
			this.vertexX[var4] = this.vertexX[var4] * var1 / 128;
			this.vertexY[var4] = var2 * this.vertexY[var4] / 128;
			this.vertexZ[var4] = var3 * this.vertexZ[var4] / 128;
		}

		reset();
	}

	public void recolor(short var1, short var2)
	{
		for (int var3 = 0; var3 < this.faceCount; ++var3)
		{
			if (this.faceColors[var3] == var1)
			{
				this.faceColors[var3] = var2;
			}
		}

	}

	public void retexture(short var1, short var2)
	{
		if (this.faceTextures != null)
		{
			for (int var3 = 0; var3 < this.faceCount; ++var3)
			{
				if (this.faceTextures[var3] == var1)
				{
					this.faceTextures[var3] = var2;
				}
			}

		}
	}

	public void move(int xOffset, int yOffset, int zOffset)
	{
		for (int i = 0; i < this.vertexCount; i++)
		{
			this.vertexX[i] += xOffset;
			this.vertexY[i] += yOffset;
			this.vertexZ[i] += zOffset;
		}
		this.reset();
	}

	public void computeMaxPriority()
	{
		if (faceRenderPriorities == null)
		{
			return;
		}

		for (int i = 0; i < faceCount; ++i)
		{
			if (faceRenderPriorities[i] > maxPriority)
			{
				maxPriority = faceRenderPriorities[i];
			}
		}
	}


	public void render(GameRasterizer rasterizer, int rotationX, int roll, int yaw, int pitch, int transX, int transY, int transZ) {
		int viewX = 0;//rasterizer.viewCenter.getX();
		int viewY = 0;//rasterizer.viewCenter.getY();
		int j2 = GraphicConstants.SINE[rotationX];
		int k2 = GraphicConstants.COSINE[rotationX];
		int l2 = GraphicConstants.SINE[roll];
		int i3 = GraphicConstants.COSINE[roll];
		int j3 = GraphicConstants.SINE[yaw];
		int k3 = GraphicConstants.COSINE[yaw];
		int sinXWorld = GraphicConstants.SINE[pitch];
		int cosXWorld = GraphicConstants.COSINE[pitch];
		int j4 = transY * sinXWorld + transZ * cosXWorld >> 16;
		for (int k4 = 0; k4 < vertexCount; k4++) {
			int x = vertexX[k4];
			int y = vertexY[k4];
			int z = vertexZ[k4];
			if (yaw != 0) {
				int k5 = y * j3 + x * k3 >> 16;
				y = y * k3 - x * j3 >> 16;
				x = k5;
			}
			if (rotationX != 0) {
				int l5 = y * k2 - z * j2 >> 16;
				z = y * j2 + z * k2 >> 16;
				y = l5;
			}
			if (roll != 0) {
				int i6 = z * l2 + x * i3 >> 16;
				z = z * i3 - x * l2 >> 16;
				x = i6;
			}
			x += transX;
			y += transY;
			z += transZ;
			int j6 = y * cosXWorld - z * sinXWorld >> 16;
			z = y * sinXWorld + z * cosXWorld >> 16;
			y = j6;
			rasterizer.vertexScreenZ[k4] = z - j4;
			rasterizer.vertexScreenX[k4] = viewX + (x << 9) / z;
			rasterizer.vertexScreenY[k4] = viewY + (y << 9) / z;
			if (numTextureFaces > 0) {
				rasterizer.camera_vertex_x[k4] = x;
				rasterizer.camera_vertex_y[k4] = y;
				rasterizer.camera_vertex_z[k4] = z;
			}
		}

		try {
			renderFaces(rasterizer, false, false, null, 0);
		} catch (Exception _ex) {
			_ex.printStackTrace();
		}
	}
	private void renderFaces(GameRasterizer rasterizer, boolean flag, boolean multiTileFlag, Object key, int z) {
		int boundingSphereRadius = 1500;
		for (int j = 0; j < boundingSphereRadius; j++) {
			rasterizer.depthListIndices[j] = 0;
		}

		for (int face = 0; face < faceCount; face++) {
			if (faceRenderTypes == null || faceRenderTypes[face] != -1) {
				int indexX = faceIndices1[face];
				int indexY = faceIndices2[face];
				int indexZ = faceIndices3[face];
				int i3 = rasterizer.vertexScreenX[indexX];
				int l3 = rasterizer.vertexScreenX[indexY];
				int k4 = rasterizer.vertexScreenX[indexZ];

				int boundingCylinderRadius = 100000;
				if (flag && (i3 == -5000 || l3 == -5000 || k4 == -5000)) {
					rasterizer.cullFacesOther[face] = true;
					int j5 = (rasterizer.vertexScreenZ[indexX] + rasterizer.vertexScreenZ[indexY] + rasterizer.vertexScreenZ[indexZ]) / 3
							+ boundingCylinderRadius;
					rasterizer.faceList[j5][rasterizer.depthListIndices[j5]++] = face;
				} else {
					if (key != null && multiTileFlag) {
						int mouseX = 0;
						int mouseY = 0;
						if (insideTriangle(mouseX, mouseY, rasterizer.vertexScreenY[indexX], rasterizer.vertexScreenY[indexY], rasterizer.vertexScreenY[indexZ], i3, l3, k4)) {
							multiTileFlag = false;
						}
					}

					if ((i3 - l3) * (rasterizer.vertexScreenY[indexZ] - rasterizer.vertexScreenY[indexY])
							- (rasterizer.vertexScreenY[indexX] - rasterizer.vertexScreenY[indexY]) * (k4 - l3) > 0) {
						rasterizer.cullFacesOther[face] = false;
						rasterizer.cullFaces[face] = i3 < 0 || l3 < 0 || k4 < 0 || i3 > rasterizer.getMaxRight() || l3 > rasterizer.getMaxRight()
								|| k4 > rasterizer.getMaxRight();
						int k5 = (rasterizer.vertexScreenZ[indexX] + rasterizer.vertexScreenZ[indexY] + rasterizer.vertexScreenZ[indexZ]) / 3
								+ boundingCylinderRadius;
						if(k5 >= 0 && k5 < rasterizer.faceList.length)
							rasterizer.faceList[k5][rasterizer.depthListIndices[k5]++] = face;
					}
				}
			}
		}

		if (faceRenderPriorities == null) {
			for (int i1 = boundingSphereRadius - 1; i1 >= 0; i1--) {
				int l1 = rasterizer.depthListIndices[i1];
				if (l1 > 0) {
					int[] ai = rasterizer.faceList[i1];
					for (int j3 = 0; j3 < l1; j3++) {
						renderFace(rasterizer, ai[j3]);
					}
				}
			}

			return;
		}
		for (int j1 = 0; j1 < 12; j1++) {
			rasterizer.anIntArray1673[j1] = 0;
			rasterizer.anIntArray1677[j1] = 0;
		}

		for (int i2 = boundingSphereRadius - 1; i2 >= 0; i2--) {
			int k2 = rasterizer.depthListIndices[i2];
			if (k2 > 0) {
				int[] ai1 = rasterizer.faceList[i2];
				for (int i4 = 0; i4 < k2; i4++) {
					int l4 = ai1[i4];
					int l5 = faceRenderPriorities[l4];
					int j6 = rasterizer.anIntArray1673[l5]++;
					rasterizer.anIntArrayArray1674[l5][j6] = l4;
					if (l5 < 10) {
						rasterizer.anIntArray1677[l5] += i2;
					} else if (l5 == 10) {
						rasterizer.anIntArray1675[j6] = i2;
					} else {
						rasterizer.anIntArray1676[j6] = i2;
					}
				}

			}
		}

		int l2 = 0;
		if (rasterizer.anIntArray1673[1] > 0 || rasterizer.anIntArray1673[2] > 0) {
			l2 = (rasterizer.anIntArray1677[1] + rasterizer.anIntArray1677[2]) / (rasterizer.anIntArray1673[1] + rasterizer.anIntArray1673[2]);
		}
		int k3 = 0;
		if (rasterizer.anIntArray1673[3] > 0 || rasterizer.anIntArray1673[4] > 0) {
			k3 = (rasterizer.anIntArray1677[3] + rasterizer.anIntArray1677[4]) / (rasterizer.anIntArray1673[3] + rasterizer.anIntArray1673[4]);
		}
		int j4 = 0;
		if (rasterizer.anIntArray1673[6] > 0 || rasterizer.anIntArray1673[8] > 0) {
			j4 = (rasterizer.anIntArray1677[6] + rasterizer.anIntArray1677[8]) / (rasterizer.anIntArray1673[6] + rasterizer.anIntArray1673[8]);
		}
		int i6 = 0;
		int k6 = rasterizer.anIntArray1673[10];
		int[] ai2 = rasterizer.anIntArrayArray1674[10];
		int[] ai3 = rasterizer.anIntArray1675;
		if (i6 == k6) {
			i6 = 0;
			k6 = rasterizer.anIntArray1673[11];
			ai2 = rasterizer.anIntArrayArray1674[11];
			ai3 = rasterizer.anIntArray1676;
		}
		int i5;
		if (i6 < k6) {
			i5 = ai3[i6];
		} else {
			i5 = -1000;
		}
		for (int l6 = 0; l6 < 10; l6++) {
			while (l6 == 0 && i5 > l2) {
				renderFace(rasterizer, ai2[i6++]);
				if (i6 == k6 && ai2 != rasterizer.anIntArrayArray1674[11]) {
					i6 = 0;
					k6 = rasterizer.anIntArray1673[11];
					ai2 = rasterizer.anIntArrayArray1674[11];
					ai3 = rasterizer.anIntArray1676;
				}
				if (i6 < k6) {
					i5 = ai3[i6];
				} else {
					i5 = -1000;
				}
			}
			while (l6 == 3 && i5 > k3) {
				renderFace(rasterizer, ai2[i6++]);
				if (i6 == k6 && ai2 != rasterizer.anIntArrayArray1674[11]) {
					i6 = 0;
					k6 = rasterizer.anIntArray1673[11];
					ai2 = rasterizer.anIntArrayArray1674[11];
					ai3 = rasterizer.anIntArray1676;
				}
				if (i6 < k6) {
					i5 = ai3[i6];
				} else {
					i5 = -1000;
				}
			}
			while (l6 == 5 && i5 > j4) {
				renderFace(rasterizer, ai2[i6++]);
				if (i6 == k6 && ai2 != rasterizer.anIntArrayArray1674[11]) {
					i6 = 0;
					k6 = rasterizer.anIntArray1673[11];
					ai2 = rasterizer.anIntArrayArray1674[11];
					ai3 = rasterizer.anIntArray1676;
				}
				if (i6 < k6) {
					i5 = ai3[i6];
				} else {
					i5 = -1000;
				}
			}
			int i7 = rasterizer.anIntArray1673[l6];
			int[] ai4 = rasterizer.anIntArrayArray1674[l6];
			for (int j7 = 0; j7 < i7; j7++) {
				renderFace(rasterizer, ai4[j7]);
			}

		}

		while (i5 != -1000) {
			renderFace(rasterizer, ai2[i6++]);
			if (i6 == k6 && ai2 != rasterizer.anIntArrayArray1674[11]) {
				i6 = 0;
				ai2 = rasterizer.anIntArrayArray1674[11];
				k6 = rasterizer.anIntArray1673[11];
				ai3 = rasterizer.anIntArray1676;
			}
			if (i6 < k6) {
				i5 = ai3[i6];
			} else {
				i5 = -1000;
			}
		}
	}

	private void renderFace(GameRasterizer rasterizer, int index) {
		if (rasterizer.cullFacesOther[index]) {
			method485(rasterizer, index);
			return;
		}
		int faceX = faceIndices1[index];
		int faceY = faceIndices2[index];
		int faceZ = faceIndices3[index];
		rasterizer.restrictEdges = rasterizer.cullFaces[index];
		if (faceTransparencies == null) {
			rasterizer.currentAlpha = 0;
		} else {
			rasterizer.currentAlpha = faceTransparencies[index];
		}
		int type;
		if (faceRenderTypes == null) {
			type = faceTextures != null && faceTextures[index] != -1 ? 2 : 0;
		} else {
			type = faceRenderTypes[index] & 3;

		}

		if (type == 0) {
			rasterizer.drawShadedTriangle(rasterizer.vertexScreenY[faceX], rasterizer.vertexScreenY[faceY], rasterizer.vertexScreenY[faceZ], rasterizer.vertexScreenX[faceX],
					rasterizer.vertexScreenX[faceY], rasterizer.vertexScreenX[faceZ], faceColors[index], faceColors[index], faceColors[index]);
		} else if (type == 1) {
			int colour = rasterizer.colourPalette[faceColors[index]];
			rasterizer.drawShadedTriangle(rasterizer.vertexScreenY[faceX], rasterizer.vertexScreenY[faceY], rasterizer.vertexScreenY[faceZ], rasterizer.vertexScreenX[faceX],
					rasterizer.vertexScreenX[faceY], rasterizer.vertexScreenX[faceZ], colour);
		} else {
			int texFaceX = 0, texFaceY = 0, texFaceZ = 0;
			try {
				if(textureCoords != null &&  textureCoords[index] != -1) {
					int k1 = textureCoords[index] & 0xFF;
					texFaceX = texIndices1[k1];
					texFaceY = texIndices2[k1];
					texFaceZ = texIndices3[k1];
				} else {
					texFaceX = faceX;
					texFaceY = faceY;
					texFaceZ = faceZ;

				}

				if(texFaceX >= 4096  || texFaceY >= 4096 || texFaceZ >= 4096 ){
					texFaceX = faceX;
					texFaceY = faceY;
					texFaceZ = faceZ;
				}

				int colourX = faceColors[index];
				int colourY = faceColors[index];
				int colourZ = faceColors[index];

				if(type == 2) {
					colourY = faceColors[index];
					colourZ = faceColors[index];
				}

				int texId = faceTextures[index];
				rasterizer.drawTexturedTriangle(
						rasterizer.vertexScreenY[faceX],
						rasterizer.vertexScreenY[faceY],
						rasterizer.vertexScreenY[faceZ],
						rasterizer.vertexScreenX[faceX],
						rasterizer.vertexScreenX[faceY],
						rasterizer.vertexScreenX[faceZ],
						colourX, colourY, colourZ,
						rasterizer.camera_vertex_x[texFaceX],
						rasterizer.camera_vertex_x[texFaceY],
						rasterizer.camera_vertex_x[texFaceZ],
						rasterizer.camera_vertex_y[texFaceX],
						rasterizer.camera_vertex_y[texFaceY],
						rasterizer.camera_vertex_y[texFaceZ],
						rasterizer.camera_vertex_z[texFaceX],
						rasterizer.camera_vertex_z[texFaceY],
						rasterizer.camera_vertex_z[texFaceZ],
						texId);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void method485(GameRasterizer rasterizer, int index) {

		int viewX = rasterizer.viewCenter.getX();
		int viewY = rasterizer.viewCenter.getY();
		int l = 0;
		int faceX = faceIndices1[index];
		int faceY = faceIndices2[index];
		int faceZ = faceIndices3[index];
		int l1 = rasterizer.camera_vertex_z[faceX];
		int i2 = rasterizer.camera_vertex_z[faceY];
		int j2 = rasterizer.camera_vertex_z[faceZ];
		if (l1 >= 50) {
			rasterizer.anIntArray1678[l] = rasterizer.vertexScreenX[faceX];
			rasterizer.anIntArray1679[l] = rasterizer.vertexScreenY[faceX];
			rasterizer.anIntArray1680[l++] = faceColors[index];
		} else {
			int k2 = rasterizer.camera_vertex_x[faceX];
			int k3 = rasterizer.camera_vertex_y[faceX];
			int k4 = faceColors[index];
			if (j2 >= 50) {
				int k5 = (50 - l1) * GraphicConstants.LIGHT_DECAY[j2 - l1];
				rasterizer.anIntArray1678[l] = viewX + (k2 + ((rasterizer.camera_vertex_x[faceZ] - k2) * k5 >> 16) << 9) / 50;
				rasterizer.anIntArray1679[l] = viewY + (k3 + ((rasterizer.camera_vertex_y[faceZ] - k3) * k5 >> 16) << 9) / 50;
				rasterizer.anIntArray1680[l++] = k4 + ((faceColors[index] - k4) * k5 >> 16);
			}
			if (i2 >= 50) {
				int l5 = (50 - l1) * GraphicConstants.LIGHT_DECAY[i2 - l1];
				rasterizer.anIntArray1678[l] = viewX + (k2 + ((rasterizer.camera_vertex_x[faceY] - k2) * l5 >> 16) << 9) / 50;
				rasterizer.anIntArray1679[l] = viewY + (k3 + ((rasterizer.camera_vertex_y[faceY] - k3) * l5 >> 16) << 9) / 50;
				rasterizer.anIntArray1680[l++] = k4 + ((faceColors[index] - k4) * l5 >> 16);
			}
		}
		if (i2 >= 50) {
			rasterizer.anIntArray1678[l] = rasterizer.vertexScreenX[faceY];
			rasterizer.anIntArray1679[l] = rasterizer.vertexScreenY[faceY];
			rasterizer.anIntArray1680[l++] = faceColors[index];
		} else {
			int l2 = rasterizer.camera_vertex_x[faceY];
			int l3 = rasterizer.camera_vertex_y[faceY];
			int l4 = faceColors[index];
			if (l1 >= 50) {
				int i6 = (50 - i2) * GraphicConstants.LIGHT_DECAY[l1 - i2];
				rasterizer.anIntArray1678[l] = viewX + (l2 + ((rasterizer.camera_vertex_x[faceX] - l2) * i6 >> 16) << 9) / 50;
				rasterizer.anIntArray1679[l] = viewY + (l3 + ((rasterizer.camera_vertex_y[faceX] - l3) * i6 >> 16) << 9) / 50;
				rasterizer.anIntArray1680[l++] = l4 + ((faceColors[index] - l4) * i6 >> 16);
			}
			if (j2 >= 50) {
				int j6 = (50 - i2) * GraphicConstants.LIGHT_DECAY[j2 - i2];
				rasterizer.anIntArray1678[l] = viewX + (l2 + ((rasterizer.camera_vertex_x[faceZ] - l2) * j6 >> 16) << 9) / 50;
				rasterizer.anIntArray1679[l] = viewY + (l3 + ((rasterizer.camera_vertex_y[faceZ] - l3) * j6 >> 16) << 9) / 50;
				rasterizer.anIntArray1680[l++] = l4 + ((faceColors[index] - l4) * j6 >> 16);
			}
		}
		if (j2 >= 50) {
			rasterizer.anIntArray1678[l] = rasterizer.vertexScreenX[faceZ];
			rasterizer.anIntArray1679[l] = rasterizer.vertexScreenY[faceZ];
			rasterizer.anIntArray1680[l++] = faceColors[index];
		} else {
			int i3 = rasterizer.camera_vertex_x[faceZ];
			int i4 = rasterizer.camera_vertex_y[faceZ];
			int i5 = faceColors[index];
			if (i2 >= 50) {
				int k6 = (50 - j2) * GraphicConstants.LIGHT_DECAY[i2 - j2];
				rasterizer.anIntArray1678[l] = viewX + (i3 + ((rasterizer.camera_vertex_x[faceY] - i3) * k6 >> 16) << 9) / 50;
				rasterizer.anIntArray1679[l] = viewY + (i4 + ((rasterizer.camera_vertex_y[faceY] - i4) * k6 >> 16) << 9) / 50;
				rasterizer.anIntArray1680[l++] = i5 + ((faceColors[index] - i5) * k6 >> 16);
			}
			if (l1 >= 50) {
				int l6 = (50 - j2) * GraphicConstants.LIGHT_DECAY[l1 - j2];
				rasterizer.anIntArray1678[l] = viewX + (i3 + ((rasterizer.camera_vertex_x[faceX] - i3) * l6 >> 16) << 9) / 50;
				rasterizer.anIntArray1679[l] = viewY + (i4 + ((rasterizer.camera_vertex_y[faceX] - i4) * l6 >> 16) << 9) / 50;
				rasterizer.anIntArray1680[l++] = i5 + ((faceColors[index] - i5) * l6 >> 16);
			}
		}
		int j3 = rasterizer.anIntArray1678[0];
		int j4 = rasterizer.anIntArray1678[1];
		int j5 = rasterizer.anIntArray1678[2];
		int i7 = rasterizer.anIntArray1679[0];
		int j7 = rasterizer.anIntArray1679[1];
		int k7 = rasterizer.anIntArray1679[2];
		if ((j3 - j4) * (k7 - j7) - (i7 - j7) * (j5 - j4) > 0) {
			rasterizer.restrictEdges = false;
			if (l == 3) {
				if (j3 < 0 || j4 < 0 || j5 < 0 || j3 > rasterizer.getMaxRight() || j4 > rasterizer.getMaxRight()
						|| j5 > rasterizer.getMaxRight()) {
					rasterizer.restrictEdges = true;
				}
				int type;
				if (faceRenderTypes == null) {
					type = faceTextures != null && faceTextures[index] != -1 ? 2 : 0;
				} else {
					type = faceRenderTypes[index] & 3;
				}

				if (type == 0) {
					rasterizer.drawShadedTriangle(i7, j7, k7, j3, j4, j5, rasterizer.anIntArray1680[0], rasterizer.anIntArray1680[1],
							rasterizer.anIntArray1680[2]);
				} else if (type == 1) {
					rasterizer.drawShadedTriangle(i7, j7, k7, j3, j4, j5,
							rasterizer.colourPalette[faceColors[index]]);
				} else if (type == 2) {
					int texFaceX, texFaceY, texFaceZ;
					if(textureCoords != null && textureCoords[index] != -1) {
						int texFaceIndex = textureCoords[index] & 0xFF;
						texFaceX = texIndices1[texFaceIndex];
						texFaceY = texIndices2[texFaceIndex];
						texFaceZ = texIndices3[texFaceIndex];
					} else {
						texFaceX = faceX;
						texFaceY = faceY;
						texFaceZ = faceZ;
					}


					if(texFaceX >= 4096  || texFaceY >= 4096 || texFaceZ >= 4096 ){
						texFaceX = faceX;
						texFaceY = faceY;
						texFaceZ = faceZ;
					}

					rasterizer.drawTexturedTriangle(i7, j7, k7, j3, j4, j5, rasterizer.anIntArray1680[0], rasterizer.anIntArray1680[1],
							rasterizer.anIntArray1680[2], rasterizer.camera_vertex_x[texFaceX], rasterizer.camera_vertex_x[texFaceY], rasterizer.camera_vertex_x[texFaceZ],
							rasterizer.camera_vertex_y[texFaceX], rasterizer.camera_vertex_y[texFaceY], rasterizer.camera_vertex_y[texFaceZ], rasterizer.camera_vertex_z[texFaceX],
							rasterizer.camera_vertex_z[texFaceY], rasterizer.camera_vertex_z[texFaceZ], faceTextures[index]);
				} else if (type == 3) {
					int texFaceX, texFaceY, texFaceZ;
					if(textureCoords != null && textureCoords[index] != -1) {
						int texFaceIndex = textureCoords[index] & 0xFF;
						texFaceX = texIndices1[texFaceIndex];
						texFaceY = texIndices2[texFaceIndex];
						texFaceZ = texIndices3[texFaceIndex];
					} else {
						texFaceX = faceX;
						texFaceY = faceY;
						texFaceZ = faceZ;
					}


					if(texFaceX >= 4096  || texFaceY >= 4096 || texFaceZ >= 4096 ){
						texFaceX = faceX;
						texFaceY = faceY;
						texFaceZ = faceZ;
					}

					rasterizer.drawTexturedTriangle(i7, j7, k7, j3, j4, j5, faceColors[index], faceColors[index],
							faceColors[index], rasterizer.camera_vertex_x[texFaceX], rasterizer.camera_vertex_x[texFaceY], rasterizer.camera_vertex_x[texFaceZ],
							rasterizer.camera_vertex_y[texFaceX], rasterizer.camera_vertex_y[texFaceY], rasterizer.camera_vertex_y[texFaceZ], rasterizer.camera_vertex_z[texFaceX],
							rasterizer.camera_vertex_z[texFaceY], rasterizer.camera_vertex_z[texFaceZ], faceTextures[index]);
				}
			}
			if (l == 4) {
				if (j3 < 0 || j4 < 0 || j5 < 0 || j3 > rasterizer.getMaxRight() || j4 > rasterizer.getMaxRight() || j5 > rasterizer.getMaxRight()
						|| rasterizer.anIntArray1678[3] < 0 || rasterizer.anIntArray1678[3] > rasterizer.getMaxRight()) {
					rasterizer.restrictEdges = true;
				}
				int type;
				if (faceRenderTypes == null) {
					type = faceTextures != null && faceTextures[index] != -1 ? 2 : 0;
				} else {
					type = faceRenderTypes[index] & 3;
				}
				if (type == 0) {
					rasterizer.drawShadedTriangle(i7, j7, k7, j3, j4, j5, rasterizer.anIntArray1680[0], rasterizer.anIntArray1680[1],
							rasterizer.anIntArray1680[2]);
					rasterizer.drawShadedTriangle(i7, k7, rasterizer.anIntArray1679[3], j3, j5, rasterizer.anIntArray1678[3],
							rasterizer.anIntArray1680[0], rasterizer.anIntArray1680[2], rasterizer.anIntArray1680[3]);
					return;
				} else if (type == 1) {
					int l8 =  rasterizer.colourPalette[faceColors[index]];
					rasterizer.drawShadedTriangle(i7, j7, k7, j3, j4, j5, l8);
					rasterizer.drawShadedTriangle(i7, k7, rasterizer.anIntArray1679[3], j3, j5, rasterizer.anIntArray1678[3], l8);
					return;
				} else if (type == 2) {
					int texFaceX, texFaceY, texFaceZ;
					if(textureCoords != null && textureCoords[index] != -1) {
						int texFaceIndex = textureCoords[index] & 0xFF;
						texFaceX = texIndices1[texFaceIndex];
						texFaceY = texIndices2[texFaceIndex];
						texFaceZ = texIndices3[texFaceIndex];
					} else {
						texFaceX = faceX;
						texFaceY = faceY;
						texFaceZ = faceZ;
					}


					if(texFaceX >= 4096  || texFaceY >= 4096 || texFaceZ >= 4096 ){
						texFaceX = faceX;
						texFaceY = faceY;
						texFaceZ = faceZ;
					}

					rasterizer.drawTexturedTriangle(i7, j7, k7, j3, j4, j5, rasterizer.anIntArray1680[0], rasterizer.anIntArray1680[1],
							rasterizer.anIntArray1680[2], rasterizer.camera_vertex_x[texFaceX], rasterizer.camera_vertex_x[texFaceY], rasterizer.camera_vertex_x[texFaceZ],
							rasterizer.camera_vertex_y[texFaceX], rasterizer.camera_vertex_y[texFaceY], rasterizer.camera_vertex_y[texFaceZ], rasterizer.camera_vertex_z[texFaceX],
							rasterizer.camera_vertex_z[texFaceY], rasterizer.camera_vertex_z[texFaceZ], faceTextures[index]);
					rasterizer.drawTexturedTriangle(i7, k7, rasterizer.anIntArray1679[3], j3, j5, rasterizer.anIntArray1678[3],
							rasterizer.anIntArray1680[0], rasterizer.anIntArray1680[2], rasterizer.anIntArray1680[3], rasterizer.camera_vertex_x[texFaceX],
							rasterizer.camera_vertex_x[texFaceY], rasterizer.camera_vertex_x[texFaceZ], rasterizer.camera_vertex_y[texFaceX], rasterizer.camera_vertex_y[texFaceY],
							rasterizer.camera_vertex_y[texFaceZ], rasterizer.camera_vertex_z[texFaceX], rasterizer.camera_vertex_z[texFaceY], rasterizer.camera_vertex_z[texFaceZ],
							faceTextures[index]);
					return;
				} else if (type == 3) {
					int texFaceX, texFaceY, texFaceZ;
					if(textureCoords != null && textureCoords[index] != -1) {
						int texFaceIndex = textureCoords[index] & 0xFF;
						texFaceX = texIndices1[texFaceIndex];
						texFaceY = texIndices2[texFaceIndex];
						texFaceZ = texIndices3[texFaceIndex];
					} else {
						texFaceX = faceX;
						texFaceY = faceY;
						texFaceZ = faceZ;
					}


					if(texFaceX >= 4096  || texFaceY >= 4096 || texFaceZ >= 4096 ){
						texFaceX = faceX;
						texFaceY = faceY;
						texFaceZ = faceZ;
					}

					rasterizer.drawTexturedTriangle(i7, j7, k7, j3, j4, j5, faceColors[index], faceColors[index],
							faceColors[index], rasterizer.camera_vertex_x[texFaceX], rasterizer.camera_vertex_x[texFaceY], rasterizer.camera_vertex_x[texFaceZ],
							rasterizer.camera_vertex_y[texFaceX], rasterizer.camera_vertex_y[texFaceY], rasterizer.camera_vertex_y[texFaceZ], rasterizer.camera_vertex_z[texFaceX],
							rasterizer.camera_vertex_z[texFaceY], rasterizer.camera_vertex_z[texFaceZ], faceTextures[index]);
					rasterizer.drawTexturedTriangle(i7, k7, rasterizer.anIntArray1679[3], j3, j5, rasterizer.anIntArray1678[3],
							faceColors[index], faceColors[index], faceColors[index], rasterizer.camera_vertex_x[texFaceX],
							rasterizer.camera_vertex_x[texFaceY], rasterizer.camera_vertex_x[texFaceZ], rasterizer.camera_vertex_y[texFaceX], rasterizer.camera_vertex_y[texFaceY],
							rasterizer.camera_vertex_y[texFaceZ], rasterizer.camera_vertex_z[texFaceX], rasterizer.camera_vertex_z[texFaceY], rasterizer.camera_vertex_z[texFaceZ],
							faceTextures[index]);
				}
			}
		}
	}

	private final static boolean insideTriangle(int x, int y, int k, int l, int i1, int j1, int k1, int l1) {
		if (y < k && y < l && y < i1)
			return false;
		if (y > k && y > l && y > i1)
			return false;
		if (x < j1 && x < k1 && x < l1)
			return false;
		return x <= j1 || x <= k1 || x <= l1;
	}

}
