package rshd;

public class Class320 {

	public static int[] VARC_INT;
	public boolean aBool3722;
	int[][] anIntArrayArray3717;
	int anInt3714;
	int anInt3718 = -1;
	int anInt3715;

	int anInt3716;

	Class320(int i_1, int i_2, int i_3) {
		anInt3715 = i_2;
		anInt3716 = i_1;
		anIntArrayArray3717 = new int[anInt3716][i_3];
	}

	void clear() {
		for (int i_2 = 0; i_2 < anInt3716; i_2++)
			anIntArrayArray3717[i_2] = null;
		anIntArrayArray3717 = null;
	}

}
