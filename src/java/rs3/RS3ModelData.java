package rs3;

import com.displee.cache.CacheLibrary;
import lombok.Getter;
import lombok.Setter;
import osrs.Buffer;
import rs3.model.EffectiveVertex;
import rs3.model.EmissiveTriangle;
import rs3.model.FaceBillboard;
import rs3.model.Particle;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.io.InputStream;

/**
 * A class representing the model definition in RS3 format.
 * @author Displee
 */
@Getter
@Setter
public class RS3ModelData {

	private final int id;

	private int anInt1942 = 12;
	private int numVertices = 0;
	private int maxIndex = 0;
	private int numFaces = 0;
	private byte priority = 0;
	private int numTextures = 0;
	private byte[] faceMappings;
	private int anInt1951;
	private int[] verticesX;
	private int[] verticesY;
	private int[] verticesZ;
	private short[] faceIndicesA;
	private short[] faceIndicesB;
	private short[] faceIndicesC;
	private int[] vertexSkins;
	private byte[] faceTypes;
	private byte[] facePriorities;
	private byte[] faceAlphas;
	private int[] faceSkins;
	private short[] faceMaterials;
	private short[] faceColors;
	private short[] faceTextures;
	private short[] textureMappingP;
	private short[] textureMappingM;
	private short[] textureMappingN;
	private int[] textureScaleX;
	private int[] textureScaleY;
	private int[] textureScaleZ;
	private byte[] textureRotation;
	private byte[] textureDirection;
	private int[] textureSpeed;
	private int[] textureTransU;
	private int[] textureTransV;
	private int[] anIntArray1923;
	private byte[] aByteArray1933;
	private byte[] aByteArray1934;
	private byte[] aByteArray1952;
	private float[] aFloatArray1914;
	private float[] aFloatArray1928;
	private EmissiveTriangle[] emitters;
	private EffectiveVertex[] effectors;
	private FaceBillboard[] billboards;

	private TextureData[] textures;
	private float[][] texturedUCoordinates;
	private float[][] texturedVCoordinates;

	/**
	 * Constructs a new {@code RS3ModelData} {@code Object}.
	 * @param id The model id.
	 */
	public RS3ModelData(int id) {
		this.id = id;
	}

	/**
	 * Decode this model.
	 * @param library The cache library.
	 */
	public void decode(ModelDefinition modelDefinition, byte[] modelData, CacheLibrary library) {
		Buffer first = new Buffer(modelData);
		Buffer second = new Buffer(modelData);
		Buffer third = new Buffer(modelData);
		Buffer fourth = new Buffer(modelData);
		Buffer fifth = new Buffer(modelData);
		Buffer sixth = new Buffer(modelData);
		Buffer seventh = new Buffer(modelData);
		int var9 = first.readUnsignedByte();
		if (var9 != 1) {
		} else {
			first.readUnsignedByte();
			this.anInt1942 = first.readUnsignedByte();
			assert modelData != null;
			first.offset = modelData.length - 26;
			modelDefinition.vertexCount = first.readUnsignedShort();
			modelDefinition.faceCount = first.readUnsignedShort();
			modelDefinition.numTextureFaces = first.readUnsignedShort();
			int footerFlags = first.readUnsignedByte();
			boolean hasFaceTypes = (footerFlags & 1) == 1;
			boolean hasParticleEffects = (footerFlags & 2) == 2;
			boolean hasBillboards = (footerFlags & 4) == 4;
			boolean hasExtendedVertexSkins = (footerFlags & 16) == 16;
			boolean hasExtendedTriangleSkins = (footerFlags & 32) == 32;
			boolean hasExtendedBillboards = (footerFlags & 64) == 64;
			boolean var17 = (footerFlags & 128) == 128;
			int modelPriority = first.readUnsignedByte();
			int hasFaceAlpha = first.readUnsignedByte();
			int hasFaceSkins = first.readUnsignedByte();
			int hasFaceTextures = first.readUnsignedByte();
			int hasVertexSkins = first.readUnsignedByte();
			int modelVerticesX = first.readUnsignedShort();
			int modelVerticesY = first.readUnsignedShort();
			int modelVerticesZ = first.readUnsignedShort();
			int faceIndices = first.readUnsignedShort();
			int textureIndices = first.readUnsignedShort();
			int numVertexSkins = first.readUnsignedShort();
			int numFaceSkins = first.readUnsignedShort();
			if (!hasExtendedVertexSkins) {
				if (hasVertexSkins == 1) {
					numVertexSkins = modelDefinition.vertexCount;
				} else {
					numVertexSkins = 0;
				}
			}

			if (!hasExtendedTriangleSkins) {
				if (hasFaceSkins == 1) {
					numFaceSkins = modelDefinition.faceCount;
				} else {
					numFaceSkins = 0;
				}
			}

			int simpleTextureFaceCount = 0;
			int complexTextureFaceCount = 0;
			int cubeTextureFaceCount = 0;
			int offset;
			if (modelDefinition.numTextureFaces > 0) {
				modelDefinition.textureRenderTypes = new byte[modelDefinition.numTextureFaces];
				first.offset = 3;

				for (offset = 0; offset < modelDefinition.numTextureFaces; ++offset) {
					byte type = modelDefinition.textureRenderTypes[offset] = first.readByte();
					if (type == 0) {
						++simpleTextureFaceCount;
					}

					if (type >= 1 && type <= 3) {
						++complexTextureFaceCount;
					}

					if (type == 2) {
						++cubeTextureFaceCount;
					}
				}
			}

			offset = 3 + modelDefinition.numTextureFaces;
			int vertexFlagsOffset = offset;
			offset += modelDefinition.vertexCount;
			int faceTypesOffset = offset;
			if (hasFaceTypes) {
				offset += modelDefinition.faceCount;
			}

			int facesCompressTypeOffset = offset;
			offset += modelDefinition.faceCount;
			int facePrioritiesOffset = offset;
			if (modelPriority == 255) {
				offset += modelDefinition.faceCount;
			}

			int faceSkinsOffset = offset;
			offset += numFaceSkins;
			int vertexSkinsOffset = offset;
			offset += numVertexSkins;
			int faceAlphasOffset = offset;
			if (hasFaceAlpha == 1) {
				offset += modelDefinition.faceCount;
			}

			int faceIndicesOffset = offset;
			offset += faceIndices;
			int faceMaterialsOffset = offset;
			if (hasFaceTextures == 1) {
				offset += modelDefinition.faceCount * 2;
			}

			int faceTextureIndicesOffset = offset;
			offset += textureIndices;
			int faceColorsOffset = offset;
			offset += modelDefinition.faceCount * 2;
			int vertexXOffsetOffset = offset;
			offset += modelVerticesX;
			int vertexYOffsetOffset = offset;
			offset += modelVerticesY;
			int vertexZOffsetOffset = offset;
			offset += modelVerticesZ;
			int simpleTexturesOffset = offset;
			offset += simpleTextureFaceCount * 6;
			int complexTexturesOffset = offset;
			offset += complexTextureFaceCount * 6;
			byte textureBytes = 6;
			if (this.anInt1942 == 14) {
				textureBytes = 7;
			} else if (this.anInt1942 >= 15) {
				textureBytes = 9;
			}

			int texturesScaleOffset = offset;
			offset += complexTextureFaceCount * textureBytes;
			int texturesRotationOffset = offset;
			offset += complexTextureFaceCount;
			int texturesDirectionOffset = offset;
			offset += complexTextureFaceCount;
			int texturesTranslationOffset = offset;
			offset += complexTextureFaceCount + cubeTextureFaceCount * 2;
			int modelDataLength1 = modelData.length;
			int modelDataLength2 = modelData.length;
			int modelDataLength3 = modelData.length;
			int modelDataLength4 = modelData.length;
			int baseY;
			int baseZ;
			if (var17) {
				InputStream var60 = new InputStream(modelData);
				var60.setOffset(modelData.length - 26);
				var60.setOffset(modelData[var60.getOffset() - 1]);
				this.anInt1951 = var60.readUnsignedShort();
				baseY = var60.readUnsignedShort();
				baseZ = var60.readUnsignedShort();
				modelDataLength1 = offset + baseY;
				modelDataLength2 = modelDataLength1 + baseZ;
				modelDataLength3 = modelDataLength2 + modelDefinition.vertexCount;
				modelDataLength4 = modelDataLength3 + this.anInt1951 * 2;
			}

			modelDefinition.vertexX = new int[modelDefinition.vertexCount];
			modelDefinition.vertexY = new int[modelDefinition.vertexCount];
			modelDefinition.vertexZ = new int[modelDefinition.vertexCount];
			modelDefinition.faceIndices1 = new int[modelDefinition.faceCount];
			modelDefinition.faceIndices2 = new int[modelDefinition.faceCount];
			modelDefinition.faceIndices3 = new int[modelDefinition.faceCount];
			if (hasVertexSkins == 1) {
				this.vertexSkins = new int[modelDefinition.vertexCount];
			}

			if (hasFaceTypes) {
				this.faceTypes = new byte[modelDefinition.faceCount];
			}

			if (modelPriority == 255) {
				this.facePriorities = new byte[modelDefinition.faceCount];
			} else {
				this.priority = (byte) modelPriority;
			}

			if (hasFaceAlpha == 1) {
				this.faceAlphas = new byte[modelDefinition.faceCount];
			}

			if (hasFaceSkins == 1) {
				this.faceSkins = new int[modelDefinition.faceCount];
			}

			if (hasFaceTextures == 1) {
				this.faceMaterials = new short[modelDefinition.faceCount];
			}

			if (hasFaceTextures == 1 && (modelDefinition.numTextureFaces > 0 || this.anInt1951 > 0)) {
				modelDefinition.faceTextures = new short[modelDefinition.faceCount];
			}

			modelDefinition.faceColors = new short[modelDefinition.faceCount];
			if (modelDefinition.numTextureFaces > 0) {
				this.textureMappingP = new short[modelDefinition.numTextureFaces];
				this.textureMappingM = new short[modelDefinition.numTextureFaces];
				this.textureMappingN = new short[modelDefinition.numTextureFaces];
				if (complexTextureFaceCount > 0) {
					this.textureScaleX = new int[complexTextureFaceCount];
					this.textureScaleY = new int[complexTextureFaceCount];
					this.textureScaleZ = new int[complexTextureFaceCount];
					this.textureRotation = new byte[complexTextureFaceCount];
					this.textureDirection = new byte[complexTextureFaceCount];
					this.textureSpeed = new int[complexTextureFaceCount];
				}

				if (cubeTextureFaceCount > 0) {
					this.textureTransU = new int[cubeTextureFaceCount];
					this.textureTransV = new int[cubeTextureFaceCount];
				}
			}

			first.offset = vertexFlagsOffset;
			second.offset = vertexXOffsetOffset;
			third.offset = vertexYOffsetOffset;
			fourth.offset = vertexZOffsetOffset;
			fifth.offset = vertexSkinsOffset;
			int baseX = 0;
			baseY = 0;
			baseZ = 0;

			int vertexCount;
			int pflag;
			int xOffset;
			int yOffset;
			int zOffset;
			for (vertexCount = 0; vertexCount < modelDefinition.vertexCount; ++vertexCount) {
				pflag = first.readUnsignedByte();
				xOffset = 0;
				if ((pflag & 1) != 0) {
					xOffset = second.readUnsignedSmart();
				}

				yOffset = 0;
				if ((pflag & 2) != 0) {
					yOffset = third.readUnsignedSmart();
				}

				zOffset = 0;
				if ((pflag & 4) != 0) {
					zOffset = fourth.readUnsignedSmart();
				}

				modelDefinition.vertexX[vertexCount] = baseX + xOffset;
				modelDefinition.vertexY[vertexCount] = baseY + yOffset;
				modelDefinition.vertexZ[vertexCount] = baseZ + zOffset;
				baseX = modelDefinition.vertexX[vertexCount];
				baseY = modelDefinition.vertexY[vertexCount];
				baseZ = modelDefinition.vertexZ[vertexCount];
				if (hasVertexSkins == 1) {
					if (hasExtendedVertexSkins) {
						this.vertexSkins[vertexCount] = fifth.readShortSmart() - 1;
					} else {
						this.vertexSkins[vertexCount] = fifth.readUnsignedByte();
						if (this.vertexSkins[vertexCount] == 255) {
							this.vertexSkins[vertexCount] = -1;
						}
					}
				}
			}

			if (this.anInt1951 > 0) {
				/*
				first.offset = modelDataLength2;
				second.offset = modelDataLength3;
				third.offset = modelDataLength4;
				this.anIntArray1923 = new int[modelDefinition.vertexCount];
				vertexCount = 0;

				for (pflag = 0; vertexCount < modelDefinition.vertexCount; ++vertexCount) {
					this.anIntArray1923[vertexCount] = pflag;
					pflag += first.readUnsignedByte();
				}

				this.aByteArray1933 = new byte[modelDefinition.faceCount];
				this.aByteArray1934 = new byte[modelDefinition.faceCount];
				this.aByteArray1952 = new byte[modelDefinition.faceCount];
				this.aFloatArray1914 = new float[this.anInt1951];
				this.aFloatArray1928 = new float[this.anInt1951];

				for (vertexCount = 0; vertexCount < this.anInt1951; ++vertexCount) {
					this.aFloatArray1914[vertexCount] = (float) second.readShort() / 4096.0F;
					this.aFloatArray1928[vertexCount] = (float) third.readShort() / 4096.0F;
				}
				 */
			}

			first.offset = (faceColorsOffset);
			second.offset = (faceTypesOffset);
			third.offset = (facePrioritiesOffset);
			fourth.offset = (faceAlphasOffset);
			fifth.offset = (faceSkinsOffset);
			sixth.offset = (faceMaterialsOffset);
			seventh.offset = (faceTextureIndicesOffset);

			for (vertexCount = 0; vertexCount < modelDefinition.faceCount; ++vertexCount) {
				modelDefinition.faceColors[vertexCount] = (short) first.readUnsignedShort();
				if (hasFaceTypes) {
					this.faceTypes[vertexCount] = second.readByte();
				}

				if (modelPriority == 255) {
					this.facePriorities[vertexCount] = third.readByte();
				}

				if (hasFaceAlpha == 1) {
					this.faceAlphas[vertexCount] = fourth.readByte();
				}

				if (hasFaceSkins == 1) {
					if (hasExtendedTriangleSkins) {
						this.faceSkins[vertexCount] = fifth.readShortSmart() - 1;
					} else {
						this.faceSkins[vertexCount] = fifth.readUnsignedByte();
						if (this.faceSkins[vertexCount] == 255) {
							this.faceSkins[vertexCount] = -1;
						}
					}
				}

				if (hasFaceTextures == 1) {
					this.faceMaterials[vertexCount] = (short) (sixth.readUnsignedShort() - 1);
				}

				if (modelDefinition.faceTextures != null) {
					if (this.faceMaterials[vertexCount] != -1) {
						if (this.anInt1942 >= 16) {
							modelDefinition.faceTextures[vertexCount] = (short) (seventh.readShortSmart() - 1);
						} else {
							modelDefinition.faceTextures[vertexCount] = (short) (seventh.readUnsignedByte() - 1);
						}
					} else {
						modelDefinition.faceTextures[vertexCount] = -1;
					}
				}
			}

			this.maxIndex = -1;
			first.offset = (faceIndicesOffset);
			second.offset = (facesCompressTypeOffset);
			third.offset = (modelDataLength1);
			this.decodeIndices(modelDefinition, first, second, third);
			first.offset = (simpleTexturesOffset);
			second.offset = (complexTexturesOffset);
			third.offset = (texturesScaleOffset);
			fourth.offset = (texturesRotationOffset);
			fifth.offset = (texturesDirectionOffset);
			sixth.offset = (texturesTranslationOffset);
			this.decodeMapping(modelDefinition, first, second, third, fourth, fifth, sixth);
			first.offset = (offset);
			if (hasParticleEffects) {
				vertexCount = first.readUnsignedByte();
				if (vertexCount > 0) {
					this.emitters = new EmissiveTriangle[vertexCount];

					for (pflag = 0; pflag < vertexCount; ++pflag) {
						xOffset = first.readUnsignedShort();
						yOffset = first.readUnsignedShort();
						byte pri;
						if (modelPriority == 255) {
							pri = this.facePriorities[yOffset];
						} else {
							pri = (byte) modelPriority;
						}

						this.emitters[pflag] = new EmissiveTriangle(xOffset, yOffset, modelDefinition.faceIndices1[yOffset], modelDefinition.faceIndices2[yOffset], modelDefinition.faceIndices3[yOffset], pri);
					}
				}
				pflag = first.readUnsignedByte();
				if (pflag > 0) {
					this.effectors = new EffectiveVertex[pflag];

					for (xOffset = 0; xOffset < pflag; ++xOffset) {
						yOffset = first.readUnsignedShort();
						zOffset = first.readUnsignedShort();
						this.effectors[xOffset] = new EffectiveVertex(yOffset, zOffset);
					}
				}
			}

			if (hasBillboards) {
				vertexCount = first.readUnsignedByte();
				if (vertexCount > 0) {
					this.billboards = new FaceBillboard[vertexCount];
					for (pflag = 0; pflag < vertexCount; ++pflag) {
						xOffset = first.readUnsignedShort();
						yOffset = first.readUnsignedShort();
						if (hasExtendedBillboards) {
							zOffset = first.readShortSmart() -1;
						} else {
							zOffset = first.readUnsignedByte();
							if (zOffset == 255) {
								zOffset = -1;
							}
						}
						byte distance = (byte) first.readByte();
						this.billboards[pflag] = new FaceBillboard(xOffset, yOffset, zOffset, distance);
					}
				}
			}
		}

		if (faceMaterials == null) {
			return;
		}

		/*
		//custom
		Map<Short, TextureData> map = new HashMap<>();
		textures = new TextureData[faceMaterials.length];
		for (int i = 0; i < faceMaterials.length; i++) {
			short textureId = faceMaterials[i];
			if (textureId == -1) {
				continue;
			}
			TextureData texture = map.get(textureId);
			if (texture == null) {
				texture = new TextureData(textureId);
				texture.decode(library);
				map.put(textureId, texture);
			}
			textures[i] = texture;
		}
		map.clear();
		 */
	}


	/**
	 * Decode the face indices.
	 * @param first The first stream.
	 * @param second The second stream.
	 * @param third The third stream.
	 */
	private void decodeIndices(ModelDefinition modelDefinition, Buffer first, Buffer second, Buffer third) {
		short var4 = 0;
		short var5 = 0;
		short var6 = 0;
		short var7 = 0;

		for (int var8 = 0; var8 < modelDefinition.faceCount; ++var8) {
			int var9 = second.readUnsignedByte();
			int var10 = var9 & 7;
			if (var10 == 1) {
				modelDefinition.faceIndices1[var8] = var4 = (short) (first.readUnsignedSmart() + var7);
				modelDefinition.faceIndices2[var8] = var5 = (short) (first.readUnsignedSmart() + var4);
				modelDefinition.faceIndices3[var8] = var6 = (short) (first.readUnsignedSmart() + var5);
				var7 = var6;
				if (var4 > this.maxIndex) {
					this.maxIndex = var4;
				}

				if (var5 > this.maxIndex) {
					this.maxIndex = var5;
				}

				if (var6 > this.maxIndex) {
					this.maxIndex = var6;
				}
			}

			if (var10 == 2) {
				var5 = var6;
				var6 = (short) (first.readUnsignedSmart() + var7);
				var7 = var6;
				modelDefinition.faceIndices1[var8] = var4;
				modelDefinition.faceIndices2[var8] = var5;
				modelDefinition.faceIndices3[var8] = var6;
				if (var6 > this.maxIndex) {
					this.maxIndex = var6;
				}
			}

			if (var10 == 3) {
				var4 = var6;
				var6 = (short) (first.readUnsignedSmart() + var7);
				var7 = var6;
				modelDefinition.faceIndices1[var8] = var4;
				modelDefinition.faceIndices2[var8] = var5;
				modelDefinition.faceIndices3[var8] = var6;
				if (var6 > this.maxIndex) {
					this.maxIndex = var6;
				}
			}

			if (var10 == 4) {
				short var11 = var4;
				var4 = var5;
				var5 = var11;
				var6 = (short) (first.readUnsignedSmart() + var7);
				var7 = var6;
				modelDefinition.faceIndices1[var8] = var4;
				modelDefinition.faceIndices2[var8] = var11;
				modelDefinition.faceIndices3[var8] = var6;
				if (var6 > this.maxIndex) {
					this.maxIndex = var6;
				}
			}

			/*
			if (this.anInt1951 > 0 && (var9 & 8) != 0) {
				this.aByteArray1933[var8] = (byte) third.readUnsignedByte();
				this.aByteArray1934[var8] = (byte) third.readUnsignedByte();
				this.aByteArray1952[var8] = (byte) third.readUnsignedByte();
			}
			 */
		}

		++this.maxIndex;
	}

	/**
	 * Decode the texture mappings.
	 *  @param var1 The first stream.
	 * @param var2 The second stream.
	 * @param var3 The third stream.
	 * @param var4 The fourth stream.
	 * @param var5 The fifth stream.
	 * @param var6 The sixth stream.
	 */
	private void decodeMapping(ModelDefinition modelDefinition, Buffer var1, Buffer var2, Buffer var3, Buffer var4, Buffer var5, Buffer var6) {
		for (int var7 = 0; var7 < modelDefinition.numTextureFaces; ++var7) {
			int var8 = modelDefinition.textureRenderTypes[var7] & 255;
			if (var8 == 0) {
				this.textureMappingP[var7] = (short) var1.readUnsignedShort();
				this.textureMappingM[var7] = (short) var1.readUnsignedShort();
				this.textureMappingN[var7] = (short) var1.readUnsignedShort();
			}

			if (var8 == 1) {
				this.textureMappingP[var7] = (short) var2.readUnsignedShort();
				this.textureMappingM[var7] = (short) var2.readUnsignedShort();
				this.textureMappingN[var7] = (short) var2.readUnsignedShort();
				if (this.anInt1942 < 15) {
					this.textureScaleX[var7] = var3.readUnsignedShort();
					if (this.anInt1942 < 14) {
						this.textureScaleY[var7] = var3.readUnsignedShort();
					} else {
						this.textureScaleY[var7] = var3.read24BitUnsignedInteger();
					}

					this.textureScaleZ[var7] = var3.readUnsignedShort();
				} else {
					this.textureScaleX[var7] = var3.read24BitUnsignedInteger();
					this.textureScaleY[var7] = var3.read24BitUnsignedInteger();
					this.textureScaleZ[var7] = var3.read24BitUnsignedInteger();
				}

				this.textureRotation[var7] = var4.readByte();
				this.textureDirection[var7] = var5.readByte();
				this.textureSpeed[var7] = var6.readByte();
			}

			if (var8 == 2) {
				this.textureMappingP[var7] = (short) var2.readUnsignedShort();
				this.textureMappingM[var7] = (short) var2.readUnsignedShort();
				this.textureMappingN[var7] = (short) var2.readUnsignedShort();
				if (this.anInt1942 < 15) {
					this.textureScaleX[var7] = var3.readUnsignedShort();
					if (this.anInt1942 < 14) {
						this.textureScaleY[var7] = var3.readUnsignedShort();
					} else {
						this.textureScaleY[var7] = var3.read24BitUnsignedInteger();
					}

					this.textureScaleZ[var7] = var3.readUnsignedShort();
				} else {
					this.textureScaleX[var7] = var3.read24BitUnsignedInteger();
					this.textureScaleY[var7] = var3.read24BitUnsignedInteger();
					this.textureScaleZ[var7] = var3.read24BitUnsignedInteger();
				}

				this.textureRotation[var7] = var4.readByte();
				this.textureDirection[var7] = var5.readByte();
				this.textureSpeed[var7] = var6.readByte();
				this.textureTransU[var7] = var6.readByte();
				this.textureTransV[var7] = var6.readByte();
			}

			if (var8 == 3) {
				this.textureMappingP[var7] = (short) var2.readUnsignedShort();
				this.textureMappingM[var7] = (short) var2.readUnsignedShort();
				this.textureMappingN[var7] = (short) var2.readUnsignedShort();
				if (this.anInt1942 < 15) {
					this.textureScaleX[var7] = var3.readUnsignedShort();
					if (this.anInt1942 < 14) {
						this.textureScaleY[var7] = var3.readUnsignedShort();
					} else {
						this.textureScaleY[var7] = var3.read24BitUnsignedInteger();
					}

					this.textureScaleZ[var7] = var3.readUnsignedShort();
				} else {
					this.textureScaleX[var7] = var3.read24BitUnsignedInteger();
					this.textureScaleY[var7] = var3.read24BitUnsignedInteger();
					this.textureScaleZ[var7] = var3.read24BitUnsignedInteger();
				}

				this.textureRotation[var7] = var4.readByte();
				this.textureDirection[var7] = var5.readByte();
				this.textureSpeed[var7] = var6.readByte();
			}
		}
	}

	public void computeTextureCoordinates(ModelDefinition modelDefinition) {
		int[] indices = new int[numFaces];
		for (int i = 0; i < numFaces; i++) {
			indices[i] = i;
		}
		Particle particle = getParticle(modelDefinition, indices, numFaces);

		float[] fs = new float[2];

		texturedUCoordinates = new float[numFaces][];
		texturedVCoordinates = new float[numFaces][];
		for (int i = 0; i < numFaces; i++) {
			int faceMaterialId = faceMaterials[i];
			int faceTextureId = faceTextures[i];
			if (faceMaterialId != -1) {
				float[] uCoordinates = new float[3];
				float[] vCoordinates = new float[3];
				if (faceTextureId == -1) {
					uCoordinates[0] = 0.0F;
					vCoordinates[0] = 1.0F;
					uCoordinates[1] = 1.0F;
					vCoordinates[1] = 1.0F;
					uCoordinates[2] = 0.0F;
					vCoordinates[2] = 0.0F;
				} else {
					faceTextureId &= 0xff;
					byte i_28_ = faceMappings[faceTextureId];
					if (i_28_ == 0) {
						short i_29_ = faceIndicesA[i];
						short i_30_ = faceIndicesB[i];
						short i_31_ = faceIndicesC[i];
						short i_32_ = textureMappingP[faceTextureId];
						short i_33_ = textureMappingM[faceTextureId];
						short i_34_ = textureMappingN[faceTextureId];
						float f = (float) (modelDefinition.vertexX[i_32_]);
						float f_35_ = (float) (modelDefinition.vertexY[i_32_]);
						float f_36_ = (float) (modelDefinition.vertexZ[i_32_]);
						float f_37_ = ((float) (modelDefinition.vertexX[i_33_]) - f);
						float f_38_ = ((float) (modelDefinition.vertexY[i_33_]) - f_35_);
						float f_39_ = ((float) (modelDefinition.vertexZ[i_33_]) - f_36_);
						float f_40_ = ((float) (modelDefinition.vertexX[i_34_]) - f);
						float f_41_ = ((float) (modelDefinition.vertexY[i_34_]) - f_35_);
						float f_42_ = ((float) (modelDefinition.vertexZ[i_34_]) - f_36_);
						float f_43_ = ((float) (modelDefinition.vertexX[i_29_]) - f);
						float f_44_ = ((float) (modelDefinition.vertexY[i_29_]) - f_35_);
						float f_45_ = ((float) (modelDefinition.vertexZ[i_29_]) - f_36_);
						float f_46_ = ((float) (modelDefinition.vertexX[i_30_]) - f);
						float f_47_ = ((float) (modelDefinition.vertexY[i_30_]) - f_35_);
						float f_48_ = ((float) (modelDefinition.vertexZ[i_30_]) - f_36_);
						float f_49_ = ((float) (modelDefinition.vertexX[i_31_]) - f);
						float f_50_ = ((float) (modelDefinition.vertexY[i_31_]) - f_35_);
						float f_51_ = ((float) (modelDefinition.vertexZ[i_31_]) - f_36_);
						float f_52_ = f_38_ * f_42_ - f_39_ * f_41_;
						float f_53_ = f_39_ * f_40_ - f_37_ * f_42_;
						float f_54_ = f_37_ * f_41_ - f_38_ * f_40_;
						float f_55_ = f_41_ * f_54_ - f_42_ * f_53_;
						float f_56_ = f_42_ * f_52_ - f_40_ * f_54_;
						float f_57_ = f_40_ * f_53_ - f_41_ * f_52_;
						float f_58_ = 1.0F / (f_55_ * f_37_ + f_56_ * f_38_ + f_57_ * f_39_);
						uCoordinates[0] = (f_55_ * f_43_ + f_56_ * f_44_ + f_57_ * f_45_) * f_58_;
						uCoordinates[1] = (f_55_ * f_46_ + f_56_ * f_47_ + f_57_ * f_48_) * f_58_;
						uCoordinates[2] = (f_55_ * f_49_ + f_56_ * f_50_ + f_57_ * f_51_) * f_58_;
						f_55_ = f_38_ * f_54_ - f_39_ * f_53_;
						f_56_ = f_39_ * f_52_ - f_37_ * f_54_;
						f_57_ = f_37_ * f_53_ - f_38_ * f_52_;
						f_58_ = 1.0F / (f_55_ * f_40_ + f_56_ * f_41_ + f_57_ * f_42_);
						vCoordinates[0] = (f_55_ * f_43_ + f_56_ * f_44_ + f_57_ * f_45_) * f_58_;
						vCoordinates[1] = (f_55_ * f_46_ + f_56_ * f_47_ + f_57_ * f_48_) * f_58_;
						vCoordinates[2] = (f_55_ * f_49_ + f_56_ * f_50_ + f_57_ * f_51_) * f_58_;
					} else {
						short i_59_ = faceIndicesA[i];
						short i_60_ = faceIndicesB[i];
						short i_61_ = faceIndicesC[i];
						int i_62_ = particle.verticesX[faceTextureId];
						int i_63_ = particle.verticesY[faceTextureId];
						int i_64_ = particle.verticesZ[faceTextureId];
						float[] fs_65_ = particle.coordinates[faceTextureId];
						byte i_66_ = textureDirection[faceTextureId];
						float f = (float) textureSpeed[faceTextureId] / 256.0F;
						if (i_28_ == 1) {
							float f_67_ = ((float) textureScaleZ[faceTextureId] / 1024.0F);
							method6904(modelDefinition.vertexX[i_59_], modelDefinition.vertexY[i_59_], modelDefinition.vertexZ[i_59_], i_62_, i_63_, i_64_, fs_65_, f_67_, i_66_, f, fs);
							uCoordinates[0] = fs[0];
							vCoordinates[0] = fs[1];
							method6904(modelDefinition.vertexX[i_60_], modelDefinition.vertexY[i_60_], modelDefinition.vertexZ[i_60_], i_62_, i_63_, i_64_, fs_65_, f_67_, i_66_, f, fs);
							uCoordinates[1] = fs[0];
							vCoordinates[1] = fs[1];
							method6904(modelDefinition.vertexX[i_61_], modelDefinition.vertexY[i_61_], modelDefinition.vertexZ[i_61_], i_62_, i_63_, i_64_, fs_65_, f_67_, i_66_, f, fs);
							uCoordinates[2] = fs[0];
							vCoordinates[2] = fs[1];
							float f_68_ = f_67_ / 2.0F;
							if ((i_66_ & 0x1) == 0) {
								if (uCoordinates[1] - uCoordinates[0] > f_68_) {
									uCoordinates[1] -= f_67_;
								} else if (uCoordinates[0] - uCoordinates[1] > f_68_) {
									uCoordinates[1] += f_67_;
								}
								if (uCoordinates[2] - uCoordinates[0] > f_68_) {
									uCoordinates[2] -= f_67_;
								} else if (uCoordinates[0] - uCoordinates[2] > f_68_) {
									uCoordinates[2] += f_67_;
								}
							} else {
								if (vCoordinates[1] - vCoordinates[0] > f_68_) {
									vCoordinates[1] -= f_67_;
								} else if (vCoordinates[0] - vCoordinates[1] > f_68_) {
									vCoordinates[1] += f_67_;
								}
								if (vCoordinates[2] - vCoordinates[0] > f_68_) {
									vCoordinates[2] -= f_67_;
								} else if (vCoordinates[0] - vCoordinates[2] > f_68_) {
									vCoordinates[2] += f_67_;
								}
							}
						} else if (i_28_ == 2) {
							float f_69_ = ((float) textureTransU[faceTextureId] / 256.0F);
							float f_70_ = ((float) textureTransV[faceTextureId] / 256.0F);
							int i_71_ = (modelDefinition.vertexX[i_60_] - (modelDefinition.vertexX[i_59_]));
							int i_72_ = (modelDefinition.vertexY[i_60_] - (modelDefinition.vertexY[i_59_]));
							int i_73_ = (modelDefinition.vertexZ[i_60_] - (modelDefinition.vertexZ[i_59_]));
							int i_74_ = (modelDefinition.vertexX[i_61_] - (modelDefinition.vertexX[i_59_]));
							int i_75_ = (modelDefinition.vertexY[i_61_] - (modelDefinition.vertexY[i_59_]));
							int i_76_ = (modelDefinition.vertexZ[i_61_] - (modelDefinition.vertexZ[i_59_]));
							int i_77_ = i_72_ * i_76_ - i_75_ * i_73_;
							int i_78_ = i_73_ * i_74_ - i_76_ * i_71_;
							int i_79_ = i_71_ * i_75_ - i_74_ * i_72_;
							float f_80_ = (64.0F / (float) textureScaleX[faceTextureId]);
							float f_81_ = (64.0F / (float) textureScaleY[faceTextureId]);
							float f_82_ = (64.0F / (float) textureScaleZ[faceTextureId]);
							float f_83_ = (((float) i_77_ * fs_65_[0] + (float) i_78_ * fs_65_[1] + (float) i_79_ * fs_65_[2]) / f_80_);
							float f_84_ = (((float) i_77_ * fs_65_[3] + (float) i_78_ * fs_65_[4] + (float) i_79_ * fs_65_[5]) / f_81_);
							float f_85_ = (((float) i_77_ * fs_65_[6] + (float) i_78_ * fs_65_[7] + (float) i_79_ * fs_65_[8]) / f_82_);
							int i_86_ = method6936(f_83_, f_84_, f_85_);
							method6939(modelDefinition.vertexX[i_59_], modelDefinition.vertexY[i_59_], modelDefinition.vertexZ[i_59_], i_62_, i_63_, i_64_, i_86_, fs_65_, i_66_, f, f_69_, f_70_, fs);
							uCoordinates[0] = fs[0];
							vCoordinates[0] = fs[1];
							method6939(modelDefinition.vertexX[i_60_], modelDefinition.vertexY[i_60_], modelDefinition.vertexZ[i_60_], i_62_, i_63_, i_64_, i_86_, fs_65_, i_66_, f, f_69_, f_70_, fs);
							uCoordinates[1] = fs[0];
							vCoordinates[1] = fs[1];
							method6939(modelDefinition.vertexX[i_61_], modelDefinition.vertexY[i_61_], modelDefinition.vertexZ[i_61_], i_62_, i_63_, i_64_, i_86_, fs_65_, i_66_, f, f_69_, f_70_, fs);
							uCoordinates[2] = fs[0];
							vCoordinates[2] = fs[1];
						} else if (i_28_ == 3) {
							method6903(modelDefinition.vertexX[i_59_], modelDefinition.vertexY[i_59_], modelDefinition.vertexZ[i_59_], i_62_, i_63_, i_64_, fs_65_, i_66_, f, fs);
							uCoordinates[0] = fs[0];
							vCoordinates[0] = fs[1];
							method6903(modelDefinition.vertexX[i_60_], modelDefinition.vertexY[i_60_], modelDefinition.vertexZ[i_60_], i_62_, i_63_, i_64_, fs_65_, i_66_, f, fs);
							uCoordinates[1] = fs[0];
							vCoordinates[1] = fs[1];
							method6903(modelDefinition.vertexX[i_61_], modelDefinition.vertexY[i_61_], modelDefinition.vertexZ[i_61_], i_62_, i_63_, i_64_, fs_65_, i_66_, f, fs);
							uCoordinates[2] = fs[0];
							vCoordinates[2] = fs[1];
							if ((i_66_ & 0x1) == 0) {
								if (uCoordinates[1] - uCoordinates[0] > 0.5F) {
									uCoordinates[1]--;
								} else if (uCoordinates[0] - uCoordinates[1] > 0.5F) {
									uCoordinates[1]++;
								}
								if (uCoordinates[2] - uCoordinates[0] > 0.5F) {
									uCoordinates[2]--;
								} else if (uCoordinates[0] - uCoordinates[2] > 0.5F) {
									uCoordinates[2]++;
								}
							} else {
								if (vCoordinates[1] - vCoordinates[0] > 0.5F) {
									vCoordinates[1]--;
								} else if (vCoordinates[0] - vCoordinates[1] > 0.5F) {
									vCoordinates[1]++;
								}
								if (vCoordinates[2] - vCoordinates[0] > 0.5F) {
									vCoordinates[2]--;
								} else if (vCoordinates[0] - vCoordinates[2] > 0.5F) {
									vCoordinates[2]++;
								}
							}
						}
					}
				}
				texturedUCoordinates[i] = uCoordinates;
				texturedVCoordinates[i] = vCoordinates;
			}
		}
	}

	private Particle getParticle(ModelDefinition modelDefinition, int[] is, int i) {
		int[] verticesX = null;
		int[] verticesY = null;
		int[] verticesZ = null;
		float[][] coordinates = null;
		if (faceTextures != null) {
			int i_33_ = numTextures;
			int[] is_34_ = new int[i_33_];
			int[] is_35_ = new int[i_33_];
			int[] is_36_ = new int[i_33_];
			int[] is_37_ = new int[i_33_];
			int[] is_38_ = new int[i_33_];
			int[] is_39_ = new int[i_33_];
			for (int i_40_ = 0; i_40_ < i_33_; i_40_++) {
				is_34_[i_40_] = 2147483647;
				is_35_[i_40_] = -2147483647;
				is_36_[i_40_] = 2147483647;
				is_37_[i_40_] = -2147483647;
				is_38_[i_40_] = 2147483647;
				is_39_[i_40_] = -2147483647;
			}
			for (int i_41_ = 0; i_41_ < i; i_41_++) {
				int i_42_ = is[i_41_];
				if (faceTextures[i_42_] != -1) {
					int i_43_ = faceTextures[i_42_] & 0xff;
					for (int i_44_ = 0; i_44_ < 3; i_44_++) {
						short i_45_;
						if (i_44_ == 0) {
							i_45_ = faceIndicesA[i_42_];
						} else if (i_44_ == 1) {
							i_45_ = faceIndicesB[i_42_];
						} else {
							i_45_ = faceIndicesC[i_42_];
						}
						int i_46_ = modelDefinition.vertexX[i_45_];
						int i_47_ = modelDefinition.vertexY[i_45_];
						int i_48_ = modelDefinition.vertexZ[i_45_];
						if (i_46_ < is_34_[i_43_]) {
							is_34_[i_43_] = i_46_;
						}
						if (i_46_ > is_35_[i_43_]) {
							is_35_[i_43_] = i_46_;
						}
						if (i_47_ < is_36_[i_43_]) {
							is_36_[i_43_] = i_47_;
						}
						if (i_47_ > is_37_[i_43_]) {
							is_37_[i_43_] = i_47_;
						}
						if (i_48_ < is_38_[i_43_]) {
							is_38_[i_43_] = i_48_;
						}
						if (i_48_ > is_39_[i_43_]) {
							is_39_[i_43_] = i_48_;
						}
					}
				}
			}
			verticesX = new int[i_33_];
			verticesY = new int[i_33_];
			verticesZ = new int[i_33_];
			coordinates = new float[i_33_][];
			for (int i_49_ = 0; i_49_ < i_33_; i_49_++) {
				byte i_50_ = faceMappings[i_49_];
				if (i_50_ > 0) {
					verticesX[i_49_] = (is_34_[i_49_] + is_35_[i_49_]) / 2;
					verticesY[i_49_] = (is_36_[i_49_] + is_37_[i_49_]) / 2;
					verticesZ[i_49_] = (is_38_[i_49_] + is_39_[i_49_]) / 2;
					float f;
					float f_51_;
					float f_52_;
					if (i_50_ == 1) {
						int i_53_ = textureScaleX[i_49_];
						if (i_53_ == 0) {
							f = 1.0F;
							f_52_ = 1.0F;
						} else if (i_53_ > 0) {
							f = 1.0F;
							f_52_ = (float) i_53_ / 1024.0F;
						} else {
							f_52_ = 1.0F;
							f = (float) -i_53_ / 1024.0F;
						}
						f_51_ = 64.0F / (float) textureScaleY[i_49_];
					} else if (i_50_ == 2) {
						f = 64.0F / (float) textureScaleX[i_49_];
						f_51_ = 64.0F / (float) textureScaleY[i_49_];
						f_52_ = 64.0F / (float) textureScaleZ[i_49_];
					} else {
						f = (float) textureScaleX[i_49_] / 1024.0F;
						f_51_ = (float) textureScaleY[i_49_] / 1024.0F;
						f_52_ = (float) textureScaleZ[i_49_] / 1024.0F;
					}
					coordinates[i_49_] = method6907(textureMappingP[i_49_], textureMappingM[i_49_], textureMappingN[i_49_], textureRotation[i_49_] & 0xff, f, f_51_, f_52_);
				}
			}
		}
		return new Particle(verticesX, verticesY, verticesZ, coordinates);
	}

	private float[] method6907(int i, int i_54_, int i_55_, int i_56_, float f, float f_57_, float f_58_) {
		float[] fs = new float[9];
		float[] fs_59_ = new float[9];
		float f_60_ = (float) Math.cos((double) ((float) i_56_ * 0.024543693F));
		float f_61_ = (float) Math.sin((double) ((float) i_56_ * 0.024543693F));
		float f_62_ = 1.0F - f_60_;
		fs[0] = f_60_;
		fs[1] = 0.0F;
		fs[2] = f_61_;
		fs[3] = 0.0F;
		fs[4] = 1.0F;
		fs[5] = 0.0F;
		fs[6] = -f_61_;
		fs[7] = 0.0F;
		fs[8] = f_60_;
		float[] fs_63_ = new float[9];
		float f_64_ = 1.0F;
		float f_65_ = 0.0F;
		f_60_ = (float) i_54_ / 32767.0F;
		f_61_ = -(float) Math.sqrt((double) (1.0F - f_60_ * f_60_));
		f_62_ = 1.0F - f_60_;
		float f_66_ = (float) Math.sqrt((double) (i * i + i_55_ * i_55_));
		if (f_66_ == 0.0F && f_60_ == 0.0F) {
			fs_59_ = fs;
		} else {
			if (f_66_ != 0.0F) {
				f_64_ = (float) -i_55_ / f_66_;
				f_65_ = (float) i / f_66_;
			}
			fs_63_[0] = f_60_ + f_64_ * f_64_ * f_62_;
			fs_63_[1] = f_65_ * f_61_;
			fs_63_[2] = f_65_ * f_64_ * f_62_;
			fs_63_[3] = -f_65_ * f_61_;
			fs_63_[4] = f_60_;
			fs_63_[5] = f_64_ * f_61_;
			fs_63_[6] = f_64_ * f_65_ * f_62_;
			fs_63_[7] = -f_64_ * f_61_;
			fs_63_[8] = f_60_ + f_65_ * f_65_ * f_62_;
			fs_59_[0] = fs[0] * fs_63_[0] + fs[1] * fs_63_[3] + fs[2] * fs_63_[6];
			fs_59_[1] = fs[0] * fs_63_[1] + fs[1] * fs_63_[4] + fs[2] * fs_63_[7];
			fs_59_[2] = fs[0] * fs_63_[2] + fs[1] * fs_63_[5] + fs[2] * fs_63_[8];
			fs_59_[3] = fs[3] * fs_63_[0] + fs[4] * fs_63_[3] + fs[5] * fs_63_[6];
			fs_59_[4] = fs[3] * fs_63_[1] + fs[4] * fs_63_[4] + fs[5] * fs_63_[7];
			fs_59_[5] = fs[3] * fs_63_[2] + fs[4] * fs_63_[5] + fs[5] * fs_63_[8];
			fs_59_[6] = fs[6] * fs_63_[0] + fs[7] * fs_63_[3] + fs[8] * fs_63_[6];
			fs_59_[7] = fs[6] * fs_63_[1] + fs[7] * fs_63_[4] + fs[8] * fs_63_[7];
			fs_59_[8] = fs[6] * fs_63_[2] + fs[7] * fs_63_[5] + fs[8] * fs_63_[8];
		}
		fs_59_[0] *= f;
		fs_59_[1] *= f;
		fs_59_[2] *= f;
		fs_59_[3] *= f_57_;
		fs_59_[4] *= f_57_;
		fs_59_[5] *= f_57_;
		fs_59_[6] *= f_58_;
		fs_59_[7] *= f_58_;
		fs_59_[8] *= f_58_;
		return fs_59_;
	}

	private int method6936(float f, float f_239_, float f_240_) {
		float f_241_ = f < 0.0F ? -f : f;
		float f_242_ = f_239_ < 0.0F ? -f_239_ : f_239_;
		float f_243_ = f_240_ < 0.0F ? -f_240_ : f_240_;
		if (f_242_ > f_241_ && f_242_ > f_243_) {
			if (f_239_ > 0.0F) {
				return 0;
			}
			return 1;
		}
		if (f_243_ > f_241_ && f_243_ > f_242_) {
			if (f_240_ > 0.0F) {
				return 2;
			}
			return 3;
		}
		if (f > 0.0F) {
			return 4;
		}
		return 5;
	}

	private void method6939(int i, int i_271_, int i_272_, int i_273_, int i_274_, int i_275_, int i_276_, float[] fs, int i_277_, float f, float f_278_, float f_279_, float[] fs_280_) {
		i -= i_273_;
		i_271_ -= i_274_;
		i_272_ -= i_275_;
		float f_281_ = ((float) i * fs[0] + (float) i_271_ * fs[1] + (float) i_272_ * fs[2]);
		float f_282_ = ((float) i * fs[3] + (float) i_271_ * fs[4] + (float) i_272_ * fs[5]);
		float f_283_ = ((float) i * fs[6] + (float) i_271_ * fs[7] + (float) i_272_ * fs[8]);
		float f_284_;
		float f_285_;
		if (i_276_ == 0) {
			f_284_ = f_281_ + f + 0.5F;
			f_285_ = -f_283_ + f_279_ + 0.5F;
		} else if (i_276_ == 1) {
			f_284_ = f_281_ + f + 0.5F;
			f_285_ = f_283_ + f_279_ + 0.5F;
		} else if (i_276_ == 2) {
			f_284_ = -f_281_ + f + 0.5F;
			f_285_ = -f_282_ + f_278_ + 0.5F;
		} else if (i_276_ == 3) {
			f_284_ = f_281_ + f + 0.5F;
			f_285_ = -f_282_ + f_278_ + 0.5F;
		} else if (i_276_ == 4) {
			f_284_ = f_283_ + f_279_ + 0.5F;
			f_285_ = -f_282_ + f_278_ + 0.5F;
		} else {
			f_284_ = -f_283_ + f_279_ + 0.5F;
			f_285_ = -f_282_ + f_278_ + 0.5F;
		}
		if (i_277_ == 1) {
			float f_286_ = f_284_;
			f_284_ = -f_285_;
			f_285_ = f_286_;
		} else if (i_277_ == 2) {
			f_284_ = -f_284_;
			f_285_ = -f_285_;
		} else if (i_277_ == 3) {
			float f_287_ = f_284_;
			f_284_ = f_285_;
			f_285_ = -f_287_;
		}
		fs_280_[0] = f_284_;
		fs_280_[1] = f_285_;
	}

	private void method6903(int i, int i_0_, int i_1_, int i_2_, int i_3_, int i_4_, float[] fs, int i_5_, float f, float[] fs_6_) {
		i -= i_2_;
		i_0_ -= i_3_;
		i_1_ -= i_4_;
		float f_7_ = (float) i * fs[0] + (float) i_0_ * fs[1] + (float) i_1_ * fs[2];
		float f_8_ = (float) i * fs[3] + (float) i_0_ * fs[4] + (float) i_1_ * fs[5];
		float f_9_ = (float) i * fs[6] + (float) i_0_ * fs[7] + (float) i_1_ * fs[8];
		float f_10_ = (float) Math.sqrt((double) (f_7_ * f_7_ + f_8_ * f_8_ + f_9_ * f_9_));
		float f_11_ = ((float) Math.atan2((double) f_7_, (double) f_9_) / 6.2831855F + 0.5F);
		float f_12_ = ((float) Math.asin((double) (f_8_ / f_10_)) / 3.1415927F + 0.5F + f);
		if (i_5_ == 1) {
			float f_13_ = f_11_;
			f_11_ = -f_12_;
			f_12_ = f_13_;
		} else if (i_5_ == 2) {
			f_11_ = -f_11_;
			f_12_ = -f_12_;
		} else if (i_5_ == 3) {
			float f_14_ = f_11_;
			f_11_ = f_12_;
			f_12_ = -f_14_;
		}
		fs_6_[0] = f_11_;
		fs_6_[1] = f_12_;
	}

	private void method6904(int i, int i_15_, int i_16_, int i_17_, int i_18_, int i_19_, float[] fs, float f, int i_20_, float f_21_, float[] fs_22_) {
		i -= i_17_;
		i_15_ -= i_18_;
		i_16_ -= i_19_;
		float f_23_ = ((float) i * fs[0] + (float) i_15_ * fs[1] + (float) i_16_ * fs[2]);
		float f_24_ = ((float) i * fs[3] + (float) i_15_ * fs[4] + (float) i_16_ * fs[5]);
		float f_25_ = ((float) i * fs[6] + (float) i_15_ * fs[7] + (float) i_16_ * fs[8]);
		float f_26_ = ((float) Math.atan2((double) f_23_, (double) f_25_) / 6.2831855F + 0.5F);
		if (f != 1.0F) {
			f_26_ *= f;
		}
		float f_27_ = f_24_ + 0.5F + f_21_;
		if (i_20_ == 1) {
			float f_28_ = f_26_;
			f_26_ = -f_27_;
			f_27_ = f_28_;
		} else if (i_20_ == 2) {
			f_26_ = -f_26_;
			f_27_ = -f_27_;
		} else if (i_20_ == 3) {
			float f_29_ = f_26_;
			f_26_ = f_27_;
			f_27_ = -f_29_;
		}
		fs_22_[0] = f_26_;
		fs_22_[1] = f_27_;
	}

	@Override
	public String toString() {
		return "Model " + id;
	}

	public ModelDefinition load(int archive, byte[] data, CacheLibrary cacheLibrary) {

		ModelDefinition modelDefinition = new ModelDefinition();
		modelDefinition.id = archive;
		modelDefinition.modelData = data;

		decode(modelDefinition, data, cacheLibrary);

		//modelDefinition.resize(32, 32, 32);

		return modelDefinition;
	}
}