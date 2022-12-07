package rs3.model;

public class Particle {

	public float[][] coordinates;
	public int[] verticesZ;
	public int[] verticesX;
	public int[] verticesY;

	public Particle(int[] verticesX, int[] verticesY, int[] verticesZ, float[][] coordinates) {
		this.verticesX = verticesX;
		this.verticesY = verticesY;
		this.verticesZ = verticesZ;
		this.coordinates = coordinates;
	}

}