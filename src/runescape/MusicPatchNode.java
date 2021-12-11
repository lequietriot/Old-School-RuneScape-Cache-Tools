package runescape;

public class MusicPatchNode extends Node {

	int midiChannel;
	MusicPatchNode2 field2988;
	MusicPatch patch;
	RawSound rawSound;
	int field2989;
	int midiNote;
	int midiNoteVolume;
	int field2992;
	int soundTransposition;
	int field2997;
	int field2998;
	int field2986;
	int field3004;
	int field2994;
	int field2999;
	int field3000;
	int field3001;
	int field3002;
	RawPcmStream stream;
	int field2995;
	int field3003;

	MusicPatchNode() {

	}

	void method4992() {
		this.patch = null;
		this.rawSound = null;
		this.field2988 = null;
		this.stream = null;
	}
}