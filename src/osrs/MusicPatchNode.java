package osrs;

public class MusicPatchNode extends Node {

	int midiChannel;
	MusicPatchNode2 musicPatchNode2;
	MusicPatch patch;
	AudioDataSource audioDataSource;
	int loopType;
	int midiNote;
	int midiNoteVolume;
	int field2992;
	int soundTransposition;
	int field2997;
	int field2998;
	int field2986;
	int volumeEnvelopePosition;
	int positionOffset;
	int releasePosition;
	int releaseOffset;
	int field3001;
	int field3002;
	RawPcmStream stream;
	int field2995;
	int field3003;

	MusicPatchNode() {

	}

	void reset() {
		this.patch = null;
		this.audioDataSource = null;
		this.musicPatchNode2 = null;
		this.stream = null;
	}
}