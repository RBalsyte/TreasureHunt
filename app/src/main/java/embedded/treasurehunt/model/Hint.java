package embedded.treasurehunt.model;

import java.io.Serializable;

public class Hint implements Serializable {
	private int id;
	private String instructions;
	private GestureType gestureType;
	private Treasure treasure;
	private byte[] image;
	
	private double longitude;
	private double latitude;
	
	public Hint(){
		
	}
	
	public Hint(Treasure treasure, String instructions, double latitude, double longitude){
		this.treasure = treasure;
		this.instructions = instructions;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	public Treasure getTreasure() {
		return treasure;
	}

	public void setTreasure(Treasure treasure) {
		this.treasure = treasure;
	}

	public String getInstructions() {
		return instructions;
	}
	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

	public GestureType getGestureType() {
		return gestureType;
	}
	public void setGestureType(GestureType gestureType) {
		this.gestureType = gestureType;
	}

	public byte[] getImage() {
		return image;
	}
	public void setImage(byte[] image) {
		this.image = image;
	}
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	
}
