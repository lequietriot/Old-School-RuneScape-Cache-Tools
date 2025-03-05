package osrs;

public class VorbisMapping {

	int submaps;
	int mappingMux;
	int[] submapFloor;
	int[] submapResidue;

	VorbisMapping() {
		VorbisSample.readBits(16);
		this.submaps = VorbisSample.readBit() != 0 ? VorbisSample.readBits(4) + 1 : 1;
		if (VorbisSample.readBit() != 0) {
			VorbisSample.readBits(8);
		}

		VorbisSample.readBits(2);
		if (this.submaps > 1) {
			this.mappingMux = VorbisSample.readBits(4);
		}

		this.submapFloor = new int[this.submaps];
		this.submapResidue = new int[this.submaps];

		for (int submap = 0; submap < this.submaps; ++submap) {
			VorbisSample.readBits(8);
			this.submapFloor[submap] = VorbisSample.readBits(8);
			this.submapResidue[submap] = VorbisSample.readBits(8);
		}

	}
}
