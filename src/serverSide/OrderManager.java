package serverSide;

public class OrderManager {

	private BankService bank = new BankService();
	private DataBase db;

	public OrderManager(DataBase db) {
		this.db = db;
	}
	
	public void updateOrder() {
		
	}
}
