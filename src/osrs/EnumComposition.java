package osrs;

public class EnumComposition extends DualNode {

	static EvictingDualNodeHashTable EnumDefinition_cached;
	static char[] cp1252AsciiExtension;
	public char inputType;
	public char outputType;
	public String defaultStr;
	public int defaultInt;
	public int outputCount;
	public int[] keys;
	public int[] intVals;
	public String[] strVals;

	static {
		cp1252AsciiExtension = new char[]{'€', '\u0000', '‚', 'ƒ', '„', '…', '†', '‡', 'ˆ', '‰', 'Š', '‹', 'Œ', '\u0000', 'Ž', '\u0000', '\u0000', '‘', '’', '“', '”', '•', '–', '—', '˜', '™', 'š', '›', 'œ', '\u0000', 'ž', 'Ÿ'}; // L: 4
		EnumDefinition_cached = new EvictingDualNodeHashTable(64); // L: 12
	}

	public EnumComposition() {
		this.defaultStr = "null"; // L: 15
		this.outputCount = 0; // L: 17
	} // L: 22

	public void decode(Buffer var1) {
		while (true) {
			int var2 = var1.readUnsignedByte(); // L: 36
			if (var2 == 0) { // L: 37
				return; // L: 40
			}

			this.decodeNext(var1, var2); // L: 38
		}
	}

	void decodeNext(Buffer var1, int var2) {
		if (var2 == 1) { // L: 43
			this.inputType = (char)var1.readUnsignedByte();
		} else if (var2 == 2) { // L: 44
			this.outputType = (char)var1.readUnsignedByte();
		} else if (var2 == 3) { // L: 45
			this.defaultStr = var1.readStringCp1252NullTerminated();
		} else if (var2 == 4) { // L: 46
			this.defaultInt = var1.readInt();
		} else {
			int var3;
			if (var2 == 5) { // L: 47
				this.outputCount = var1.readUnsignedShort(); // L: 48
				this.keys = new int[this.outputCount]; // L: 49
				this.strVals = new String[this.outputCount]; // L: 50

				for (var3 = 0; var3 < this.outputCount; ++var3) { // L: 51
					this.keys[var3] = var1.readInt(); // L: 52
					this.strVals[var3] = var1.readStringCp1252NullTerminated(); // L: 53
				}
			} else if (var2 == 6) { // L: 56
				this.outputCount = var1.readUnsignedShort(); // L: 57
				this.keys = new int[this.outputCount]; // L: 58
				this.intVals = new int[this.outputCount]; // L: 59

				for (var3 = 0; var3 < this.outputCount; ++var3) { // L: 60
					this.keys[var3] = var1.readInt(); // L: 61
					this.intVals[var3] = var1.readInt(); // L: 62
				}
			}
		}

	} // L: 66

	public int size() {
		return this.outputCount; // L: 69
	}

	public static String decodeStringCp1252(byte[] var0, int var1, int var2) {
		char[] var3 = new char[var2]; // L: 82
		int var4 = 0; // L: 83

		for (int var5 = 0; var5 < var2; ++var5) { // L: 84
			int var6 = var0[var5 + var1] & 255; // L: 85
			if (var6 != 0) { // L: 86
				if (var6 >= 128 && var6 < 160) { // L: 87
					char var7 = cp1252AsciiExtension[var6 - 128]; // L: 88
					if (var7 == 0) { // L: 89
						var7 = '?';
					}

					var6 = var7; // L: 90
				}

				var3[var4++] = (char)var6; // L: 92
			}
		}

		return new String(var3, 0, var4); // L: 94
	}

}
