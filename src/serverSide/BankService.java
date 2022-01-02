package serverSide;

/**
 * BankService is meant to be the API with the credit card company,
 * in this project the payment always successful.
 * therefore we return true.
 * */
public class BankService {
	public Boolean handlePayment(Payment p) {
		return true;
	}
}
