package util;

public class OptionalFeature {
	private int price;
	private String name;
	private String id;
	
	public OptionalFeature(int price, String name, String id) {
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

	public Object getID() {
		return id;
	}
}
