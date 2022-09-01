package runelite.models;

public class VertexNormal
{
	public int x;
	public int y;
	public int z;
	public int magnitude;

	public VertexNormal() {

	}

	public VertexNormal(int xVertex, int yVertex, int zVertex, int magnitudeValue) {
		x = xVertex;
		y = yVertex;
		z = zVertex;
		magnitude = magnitudeValue;
	}

	public VertexNormal(int xVertex, int yVertex, int zVertex) {
		x = xVertex;
		y = yVertex;
		z = zVertex;
	}

    public Vector3f normalize()
	{
		Vector3f v = new Vector3f();

		int length = (int) Math.sqrt((double) (x * x + y * y + z * z));
		if (length == 0)
		{
			length = 1;
		}

		v.x = (float) x / length;
		v.y = (float) y / length;
		v.z = (float) z / length;

		assert v.x >= -1f && v.x <= 1f;
		assert v.y >= -1f && v.y <= 1f;
		assert v.z >= -1f && v.z <= 1f;

		return v;
	}

	public VertexNormal method1459(int yVertex) {
		return new VertexNormal(x, yVertex, z, magnitude);
	}

}
