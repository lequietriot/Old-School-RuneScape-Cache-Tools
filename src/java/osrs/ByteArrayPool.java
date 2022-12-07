package osrs;

import java.util.ArrayList;
import java.util.HashMap;

public class ByteArrayPool {
	static int ByteArrayPool_smallCount;
	static int ByteArrayPool_mediumCount;
	static int ByteArrayPool_largeCount;
	static int field4217;
	static int field4210;
	static int field4219;
	static int field4220;
	static int field4221;
	static byte[][] ByteArrayPool_small;
	static byte[][] ByteArrayPool_medium;
	static byte[][] ByteArrayPool_large;
	static byte[][] field4225;
	static ArrayList field4212;

	static {
		ByteArrayPool_smallCount = 0; // L: 13
		ByteArrayPool_mediumCount = 0; // L: 14
		ByteArrayPool_largeCount = 0; // L: 15
		field4217 = 0; // L: 16
		field4210 = 1000; // L: 17
		field4219 = 250; // L: 18
		field4220 = 100; // L: 19
		field4221 = 50; // L: 20
		ByteArrayPool_small = new byte[1000][]; // L: 21
		ByteArrayPool_medium = new byte[250][]; // L: 22
		ByteArrayPool_large = new byte[100][]; // L: 23
		field4225 = new byte[50][];
		field4212 = new ArrayList(); // L: 28
		new HashMap();
	} // L: 36

	public static float method6364(int var0) {
		var0 &= 16383; // L: 24
		return (float)(6.283185307179586D * (double)((float)var0 / 16384.0F)); // L: 25
	}

	static synchronized byte[] ByteArrayPool_getArrayBool(int var0, boolean var1) {
		byte[] var4;
		if (var0 != 100) { // L: 70
			if (var0 < 100) {
			}
		} else if (ByteArrayPool_smallCount > 0) {
			var4 = ByteArrayPool_small[--ByteArrayPool_smallCount]; // L: 71
			ByteArrayPool_small[ByteArrayPool_smallCount] = null; // L: 72
			return var4; // L: 73
		}

		if (var0 != 5000) { // L: 75
			if (var0 < 5000) {
			}
		} else if (ByteArrayPool_mediumCount > 0) {
			var4 = ByteArrayPool_medium[--ByteArrayPool_mediumCount]; // L: 76
			ByteArrayPool_medium[ByteArrayPool_mediumCount] = null; // L: 77
			return var4; // L: 78
		}

		if (var0 != 10000) { // L: 80
			if (var0 < 10000) {
			}
		} else if (ByteArrayPool_largeCount > 0) {
			var4 = ByteArrayPool_large[--ByteArrayPool_largeCount]; // L: 81
			ByteArrayPool_large[ByteArrayPool_largeCount] = null; // L: 82
			return var4; // L: 83
		}

		if (var0 != 30000) { // L: 85
			if (var0 < 30000) {
			}
		} else if (field4217 > 0) {
			var4 = field4225[--field4217]; // L: 86
			field4225[field4217] = null; // L: 87
			return var4; // L: 88
		}

		return new byte[var0]; // L: 108
	}

}
