package osrs;

import com.displee.cache.index.Index;

import java.util.Arrays;
import java.util.Objects;

public class NPCComposition extends DualNode {

	public static EvictingDualNodeHashTable NpcDefinition_cached;
	public static EvictingDualNodeHashTable NpcDefinition_cachedModels;
	public int id;
	public String name;
	public int size;
	int[] models;
	int[] field1957;
	public int idleSequence;
	public int turnLeftSequence;
	public int turnRightSequence;
	public int walkSequence;
	public int walkBackSequence;
	public int walkLeftSequence;
	public int walkRightSequence;
	public int field1959;
	public int field1960;
	public int field1961;
	public int field1946;
	public int field1985;
	public int field1964;
	public int field1956;
	public int field1966;
	short[] recolorFrom;
	short[] recolorTo;
	short[] retextureFrom;
	short[] retextureTo;
	public String[] actions;
	public boolean drawMapDot;
	public int combatLevel;
	int widthScale;
	int heightScale;
	public boolean isVisible;
	int ambient;
	int contrast;
	public int headIconPrayer;
	public int rotation;
	public int[] transforms;
	int transformVarbit;
	int transformVarp;
	public boolean isInteractable;
	public boolean isClickable;
	public boolean isFollower;
	IterableNodeHashTable params;

	static {
		NpcDefinition_cached = new EvictingDualNodeHashTable(64); // L: 18
		NpcDefinition_cachedModels = new EvictingDualNodeHashTable(50); // L: 19
	}

	public NPCComposition() {
		this.name = "null"; // L: 21
		this.size = 1; // L: 22
		this.idleSequence = -1; // L: 25
		this.turnLeftSequence = -1; // L: 26
		this.turnRightSequence = -1; // L: 27
		this.walkSequence = -1; // L: 28
		this.walkBackSequence = -1; // L: 29
		this.walkLeftSequence = -1; // L: 30
		this.walkRightSequence = -1; // L: 31
		this.field1959 = -1; // L: 32
		this.field1960 = -1; // L: 33
		this.field1961 = -1; // L: 34
		this.field1946 = -1; // L: 35
		this.field1985 = -1; // L: 36
		this.field1964 = -1; // L: 37
		this.field1956 = -1; // L: 38
		this.field1966 = -1; // L: 39
		this.actions = new String[5]; // L: 44
		this.drawMapDot = true; // L: 45
		this.combatLevel = -1; // L: 46
		this.widthScale = 128; // L: 47
		this.heightScale = 128; // L: 48
		this.isVisible = false; // L: 49
		this.ambient = 0; // L: 50
		this.contrast = 0; // L: 51
		this.headIconPrayer = -1; // L: 52
		this.rotation = 32; // L: 53
		this.transformVarbit = -1; // L: 55
		this.transformVarp = -1; // L: 56
		this.isInteractable = true; // L: 57
		this.isClickable = true; // L: 58
		this.isFollower = false; // L: 59
	} // L: 62

	public void getNpcDefinition(Index index, int archiveId, int fileId) {
		NPCComposition var1 = (NPCComposition)NPCComposition.NpcDefinition_cached.get((long)fileId); // L: 65
		if (var1 != null) { // L: 66
		} else {
			byte[] var2 = Objects.requireNonNull(Objects.requireNonNull(index.archive(archiveId)).file(fileId)).getData(); // L: 67
			var1 = new NPCComposition(); // L: 68
			var1.id = fileId; // L: 69
			if (var2 != null) { // L: 70
				var1.decode(new Buffer(var2));
			}

			var1.postDecode(); // L: 71
			NPCComposition.NpcDefinition_cached.put(var1, (long)fileId); // L: 72
			System.out.println("Name: " + name);
			System.out.println("Size: " + size);
			System.out.println("Idle Sequence: " + idleSequence);
			System.out.println("Walk Sequence: " + walkSequence);
			System.out.println("Walk Back Sequence: " + walkBackSequence);
			System.out.println("Walk Left Sequence: " + walkLeftSequence);
			System.out.println("Walk Right Sequence: " + walkRightSequence);
			System.out.println("Actions: " + Arrays.toString(actions));
			System.out.println("Size: " + size);
			System.out.println("Size: " + size);
			System.out.println("Size: " + size);
			System.out.println("Size: " + size);
			System.out.println("Size: " + size);
			System.out.println("Size: " + size);
			System.out.println("Size: " + size);
		}
	}

	void postDecode() {
	} // L: 76

	void decode(Buffer var1) {
		while (true) {
			int var2 = var1.readUnsignedByte(); // L: 80
			if (var2 == 0) { // L: 81
				return; // L: 84
			}

			this.decodeNext(var1, var2); // L: 82
		}
	}

	void decodeNext(Buffer var1, int var2) {
		int var3;
		int var4;
		if (var2 == 1) { // L: 87
			var3 = var1.readUnsignedByte(); // L: 88
			this.models = new int[var3]; // L: 89

			for (var4 = 0; var4 < var3; ++var4) { // L: 90
				this.models[var4] = var1.readUnsignedShort();
			}
		} else if (var2 == 2) { // L: 92
			this.name = var1.readStringCp1252NullTerminated();
		} else if (var2 == 12) { // L: 93
			this.size = var1.readUnsignedByte();
		} else if (var2 == 13) { // L: 94
			this.idleSequence = var1.readUnsignedShort();
		} else if (var2 == 14) { // L: 95
			this.walkSequence = var1.readUnsignedShort();
		} else if (var2 == 15) { // L: 96
			this.turnLeftSequence = var1.readUnsignedShort();
		} else if (var2 == 16) { // L: 97
			this.turnRightSequence = var1.readUnsignedShort();
		} else if (var2 == 17) { // L: 98
			this.walkSequence = var1.readUnsignedShort(); // L: 99
			this.walkBackSequence = var1.readUnsignedShort(); // L: 100
			this.walkLeftSequence = var1.readUnsignedShort(); // L: 101
			this.walkRightSequence = var1.readUnsignedShort(); // L: 102
		} else if (var2 == 18) { // L: 104
			var1.readUnsignedShort(); // L: 105
		} else if (var2 >= 30 && var2 < 35) { // L: 107
			this.actions[var2 - 30] = var1.readStringCp1252NullTerminated(); // L: 108
			if (this.actions[var2 - 30].equalsIgnoreCase("Hidden")) { // L: 109
				this.actions[var2 - 30] = null;
			}
		} else if (var2 == 40) { // L: 111
			var3 = var1.readUnsignedByte(); // L: 112
			this.recolorFrom = new short[var3]; // L: 113
			this.recolorTo = new short[var3]; // L: 114

			for (var4 = 0; var4 < var3; ++var4) { // L: 115
				this.recolorFrom[var4] = (short)var1.readUnsignedShort(); // L: 116
				this.recolorTo[var4] = (short)var1.readUnsignedShort(); // L: 117
			}
		} else if (var2 == 41) { // L: 120
			var3 = var1.readUnsignedByte(); // L: 121
			this.retextureFrom = new short[var3]; // L: 122
			this.retextureTo = new short[var3]; // L: 123

			for (var4 = 0; var4 < var3; ++var4) { // L: 124
				this.retextureFrom[var4] = (short)var1.readUnsignedShort(); // L: 125
				this.retextureTo[var4] = (short)var1.readUnsignedShort(); // L: 126
			}
		} else if (var2 == 60) { // L: 129
			var3 = var1.readUnsignedByte(); // L: 130
			this.field1957 = new int[var3]; // L: 131

			for (var4 = 0; var4 < var3; ++var4) { // L: 132
				this.field1957[var4] = var1.readUnsignedShort();
			}
		} else if (var2 == 93) { // L: 134
			this.drawMapDot = false;
		} else if (var2 == 95) { // L: 135
			this.combatLevel = var1.readUnsignedShort();
		} else if (var2 == 97) { // L: 136
			this.widthScale = var1.readUnsignedShort();
		} else if (var2 == 98) { // L: 137
			this.heightScale = var1.readUnsignedShort();
		} else if (var2 == 99) { // L: 138
			this.isVisible = true;
		} else if (var2 == 100) { // L: 139
			this.ambient = var1.readByte();
		} else if (var2 == 101) { // L: 140
			this.contrast = var1.readByte() * 5;
		} else if (var2 == 102) { // L: 141
			this.headIconPrayer = var1.readUnsignedShort();
		} else if (var2 == 103) { // L: 142
			this.rotation = var1.readUnsignedShort();
		} else if (var2 != 106 && var2 != 118) { // L: 143
			if (var2 == 107) { // L: 161
				this.isInteractable = false;
			} else if (var2 == 109) { // L: 162
				this.isClickable = false;
			} else if (var2 == 111) { // L: 163
				this.isFollower = true;
			} else if (var2 == 114) { // L: 164
				this.field1959 = var1.readUnsignedShort();
			} else if (var2 == 115) { // L: 165
				this.field1959 = var1.readUnsignedShort(); // L: 166
				this.field1960 = var1.readUnsignedShort(); // L: 167
				this.field1961 = var1.readUnsignedShort(); // L: 168
				this.field1946 = var1.readUnsignedShort(); // L: 169
			} else if (var2 == 116) {
				this.field1985 = var1.readUnsignedShort(); // L: 171
			} else if (var2 == 117) { // L: 172
				this.field1985 = var1.readUnsignedShort(); // L: 173
				this.field1964 = var1.readUnsignedShort(); // L: 174
				this.field1956 = var1.readUnsignedShort(); // L: 175
				this.field1966 = var1.readUnsignedShort(); // L: 176
			} else if (var2 == 249) { // L: 178
				this.params = Buffer.readStringIntParameters(var1, this.params);
			}
		} else {
			this.transformVarbit = var1.readUnsignedShort(); // L: 144
			if (this.transformVarbit == 65535) { // L: 145
				this.transformVarbit = -1;
			}

			this.transformVarp = var1.readUnsignedShort(); // L: 146
			if (this.transformVarp == 65535) { // L: 147
				this.transformVarp = -1;
			}

			var3 = -1; // L: 148
			if (var2 == 118) { // L: 149
				var3 = var1.readUnsignedShort(); // L: 150
				if (var3 == 65535) { // L: 151
					var3 = -1;
				}
			}

			var4 = var1.readUnsignedByte(); // L: 153
			this.transforms = new int[var4 + 2]; // L: 154

			for (int var5 = 0; var5 <= var4; ++var5) { // L: 155
				this.transforms[var5] = var1.readUnsignedShort(); // L: 156
				if (this.transforms[var5] == 65535) { // L: 157
					this.transforms[var5] = -1;
				}
			}

			this.transforms[var4 + 1] = var3; // L: 159
		}

	} // L: 180

}
