package rs3.model;

public class FaceBillboard {

	private int distance;
	private int face;
	private int skin;
	private int billboardId;

	public FaceBillboard(int i, int i_1_, int i_2_, int i_3_) {
		billboardId = i;
		face = i_1_;
		skin = i_2_;
		distance = i_3_;
	}

	public int getId() {
		return billboardId;
	}

	public int getFace() {
		return face;
	}

	public int getSkin() {
		return skin;
	}

	public int getDistance() {
		return distance;
	}

	public FaceBillboard copy(int i) {
		return new FaceBillboard(billboardId, i, skin, distance);
	}

	@Override
	public String toString() {
		return "FaceBillboard[billboard=" + billboardId + ", face=" + face + ", skin=" + skin + ", distance=" + distance + "]";
	}

}