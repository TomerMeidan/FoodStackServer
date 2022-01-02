package serverSide;

public class Payment {

	private double amount;

	public Payment(double amount) {
		this.amount = amount;
	}

	public String toString() {
		return "Payment amount is : " + amount;
	}
}
