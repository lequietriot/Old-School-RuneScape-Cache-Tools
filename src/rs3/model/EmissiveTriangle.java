package rs3.model;

public class EmissiveTriangle {

	private int particleId;
	private int face;
	private int faceA;
	private int faceC;
	private int faceB;
	private byte priority;

	public EmissiveTriangle(int i, int i_2_, int i_3_, int i_4_, int i_5_, byte i_6_) {
		particleId = i;
		face = i_2_;
		faceA = i_3_;
		faceB = i_4_;
		faceC = i_5_;
		priority = i_6_;
	}

	public EmissiveTriangle copy(int i, int i_0_, int i_1_) {
		return new EmissiveTriangle(this.particleId, face, i, i_0_, i_1_, priority);
	}

	public int getParticleId() {
		return particleId;
	}

	public int getFace() {
		return face;
	}

	public int getFaceA() {
		return faceA;
	}

	public int getFaceC() {
		return faceC;
	}

	public int getFaceB() {
		return faceB;
	}

	public byte getPriority() {
		return priority;
	}

}