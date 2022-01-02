package util;

public class MustFeature {
	private int price;
	private String name;
	private String id;

	public MustFeature(int price, String name, String id) {
		this.price = price;
		this.name = name;
		this.id = id;
	}

	public String getName() {
		return name;
	}
	
	public int getPrice() {
		return price;
	}
	
	public String getID() {
		return id;
	}

}
