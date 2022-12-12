package rshd;

import osrs.Buffer;
import osrs.Node;

public class MaterialProperty extends Node {

	protected Class308 aClass308_7670;
	protected Class320 aClass320_7667;

	MaterialProperty() {

	}

	static MaterialProperty decode(Buffer buffer) {
		buffer.readUnsignedByte();
		int opIndex = buffer.readUnsignedByte();
		int numReads = buffer.readUnsignedByte();
		for (int i = 0; i < numReads; i++) {
			int opcode = buffer.readUnsignedByte();
		}
		return null;
	}
	static MaterialProperty getById(int i_0) {
		return null;
	}
	protected boolean noPalette;

	int anInt7668;

	MaterialProperty[] params;

	MaterialProperty(int numParams, boolean bool_2) {
		noPalette = bool_2;
		params = new MaterialProperty[numParams];
	}

	void decode(int i_1, Buffer rsbytebuffer_2) {
	}

	int[][] getPixels(int i_1) {
		return null;
	}

	int getSpriteId() {
		return -1;
	}

	int getTextureId() {
		return -1;
	}

	void method12315(int i_1, int i_2) {
		int i_4 = anInt7668 == 255 ? i_2 : anInt7668;
	}

	int[] method12317(int i_1, int i_2) {
		return !params[i_1].noPalette ? params[i_1].getPixels(i_2)[0] : params[i_1].method12319(i_2);
	}

	int[] method12319(int i_1) {
		return null;
	}

	void method12321() {
	}

	int[][] method12333(int i_1, int i_2) {
		if (params[i_1].noPalette) {
			int[] ints_4 = params[i_1].method12319(i_2);
			int[][] ints_5 = {ints_4, ints_4, ints_4};
			return ints_5;
		}
		return params[i_1].getPixels(i_2);
	}

	void reset() {
	}
}