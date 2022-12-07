package rs3.model;

public class EffectiveVertex {

	private int particleId;
	private int vertex;

	public EffectiveVertex(int i, int i_0_) {
		particleId = i;
		vertex = i_0_;
	}

	public EffectiveVertex copy(int i) {
		return new EffectiveVertex(particleId, i);
	}

	public int getParticleId() {
		return particleId;
	}

	public int getVertex() {
		return vertex;
	}

}