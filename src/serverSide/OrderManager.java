package serverSide;

import org.json.simple.JSONObject;

public class OrderManager {

	private BankService bank = new BankService();
	private DataBase db;

	public OrderManager(DataBase db) {
		this.db = db;
	}
	
	public void updateOrder() {
		
	}
}
