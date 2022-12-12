package rshd;

import osrs.Node;

public class Class308 {

	public boolean aBool3619;
	int[][][] anIntArrayArrayArray3614;
	LinkedNodeList aClass473_3612 = new LinkedNodeList();
	int anInt3617;
	int anInt3616 = -1;
	int anInt3611;
	Node[] aNode_Sub16Array3615;
	int anInt3613;

	Class308(int i_1, int i_2, int i_3) {
		anInt3611 = i_2;
		anInt3613 = i_1;
		anIntArrayArrayArray3614 = new int[anInt3613][3][i_3];
	}

	void clear() {
		for (int i_2 = 0; i_2 < anInt3613; i_2++) {
			anIntArrayArrayArray3614[i_2][0] = null;
			anIntArrayArrayArray3614[i_2][1] = null;
			anIntArrayArrayArray3614[i_2][2] = null;
			anIntArrayArrayArray3614[i_2] = null;
		}
		anIntArrayArrayArray3614 = null;
		aClass473_3612.clear();
		aClass473_3612 = null;
	}

	public int[][] method5463(int i_1) {
		if (anInt3611 == anInt3613) {
			aBool3619 = true;
			return anIntArrayArrayArray3614[i_1];
		}
		if (anInt3613 != 1) {
			Node class282_sub16_3 = aNode_Sub16Array3615[i_1];
			aClass473_3612.insertFront(class282_sub16_3);
			return anIntArrayArrayArray3614[(int) class282_sub16_3.key];
		} else {
			aBool3619 = anInt3616 != i_1;
			anInt3616 = i_1;
			return anIntArrayArrayArray3614[0];
		}
	}

}
