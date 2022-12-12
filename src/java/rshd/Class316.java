package rshd;

import osrs.NodeHashTable;

public class Class316 {

	public static int[] anIntArray3677;
	public static int[] anIntArray3675;
	public static int[] anIntArray3672;
	public static int anInt3669;
	public static int anInt3670;
	public static int anInt3678;
	public static int[] anIntArray3668;
	public static int anInt3673;
	public static int anInt3671;
	static NodeHashTable aClass223_3679 = new NodeHashTable(16);

	public static void method5586() {
		if (anIntArray3677 == null || anIntArray3675 == null) {
			anIntArray3677 = new int[256];
			anIntArray3675 = new int[256];
			for (int i_1 = 0; i_1 < 256; i_1++) {
				double d_2 = 6.283185307179586D * (i_1 / 255.0D);
				anIntArray3677[i_1] = (int) (Math.sin(d_2) * 4096.0D);
				anIntArray3675[i_1] = (int) (Math.cos(d_2) * 4096.0D);
			}
		}
	}

	Class316() throws Throwable {
		throw new Error();
	}
}
