package controllers;

import java.io.IOException;

import org.json.simple.JSONObject;

import ocsf.server.ConnectionToClient;
import serverSide.DataBase;
import common.Logger;
import common.Parser;
import common.Logger.Level;
import util.Message;

@SuppressWarnings("unchecked")
public class CustomerPortalViewController implements PortalViewController {

	private DataBase db;
	private ComController com;
	private ConnectionToClient connection;

	private String userID;
	private String employerID;
	private String employerW4C;
	private String employerName;

	public CustomerPortalViewController(DataBase db, ComController com, ConnectionToClient connection) {
		this.db = db;
		this.com = com;
		this.connection = connection;
		System.out.println("connection address:" + connection.getInetAddress());
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleCommandFromClient(JSONObject json) {
		switch (Message.getValueString(json, "command")) {
		case "Order button was clicked":
			handleOrderButtonClicked();
			break;
		case "Order window is displayed":
			handleOrderWindowDisplayed();
			break;
		case "Combo box option was selected":
			handleComboBox();
			break;
		case "Item type was selected":
			handleItemTypeSelected(json);
			break;
		case "Restaurant was selected":
			handleRestaurantSelected(json);
			break;
		case "Add meal button was clicked":
			handleAddMealButtonClicked();
			break;
		case "Check out button was clicked":
			handleCheckOutClicked();
			break;
		case "Delivery method was selected":
			handleDeliveryMethodSelected(json);
			break;
		case "Payment window is displayed":
			handlePaymentWindowDisplayed(json);
			break;
		case "Confirm button was clicked":
			handleConfirmButtonClicked(json);
			break;
		case "Back to homepage button was clicked":
			handleBackToHomePageButtonClicked();
			break;
		case "View Order button was clicked":
			handleViewOrderButtonClicked();
			break;
		case "View Order window is displayed":
			handleViewOrderWindowDisplayed();
			break;
		case "View button clicked":
			handleViewButtonClicked();
			break;
		case "Approve Reception button clicked":
			handleApproveReceptionButtonClicked(json);
			break;
		case "Scan button was clicked":
			handleScanButtonClicked();
			break;
		case "Log out":
			handleLogOutClicked();
			break;
		case "Order is ready":
			handleOrderReady();
			break;
		case "Clicked yes after order failed":
			handleClickedYesAfterOrderFail(Message.getValueJObject(json, "order"));
		default:
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: Unknown command : " + json.get("command"));
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: Unknown command : " + json.get("command"));
			break;
		}
	}

	private void handleClickedYesAfterOrderFail(JSONObject order) {
		JSONObject msgForClient = new JSONObject();
		msgForClient.put("command", "update");
		order.put("userID", userID);
		boolean fail = false;
		try {
			if (db.zeroBusinessCustomerBalance(order)) {
				msgForClient.put("update", "Order was successfuly added");
				ConnectionToClient supplierCon = com.findConnection(Message.getValueString(order, "supplierID"));
				if (supplierCon != null)
					supplierCon.sendToClient(Parser.encode(msgForClient));
			}
			else
				fail = true;
			if (fail) {
				msgForClient.put("update", "Show pop up: failed order");
				msgForClient.put("reason", "Error in system");
			}
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException Clicked yes after order failed");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException Clicked yes after order failed");
		}

	}

	private void handleOrderReady() {
		JSONObject msgForClient = new JSONObject();
		msgForClient = db.getOrders(userID);
		msgForClient.put("update", "Refresh View Window");
		try {
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in Order is ready");
			System.out.println("CustomerPortalViewController: handleCommandFromClient: IOException in Order is ready");
		}
	}

	private void handleOrderWindowDisplayed() {
		JSONObject msgForClient = new JSONObject();
		msgForClient = db.getRestaurants();
		try {
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in Order window is displayed");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException in Order window is displayed");
		}
	}

	private void handleViewOrderWindowDisplayed() {
		JSONObject msgForClient = new JSONObject();
		try {
			msgForClient = db.getOrders(userID);
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in View Order window is displayed");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException in View Order window is displayed");
		}
	}

	private void handlePaymentWindowDisplayed(JSONObject json) {
		JSONObject msgForClient = new JSONObject();
		json.put("userID", userID);
		msgForClient = db.getRefundBalance(json);
		try {
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in Payment window is displayed");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException in Payment window is displayed");
		}
	}

	private void handleOrderButtonClicked() {
		JSONObject msgForClient = new JSONObject();
		msgForClient.put("command", "update");
		msgForClient.put("update", "Show order window");
		try {
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in Order button was clicked");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException in Order button was clicked");
		}
	}

	private void handleComboBox() {
		JSONObject msgForClient = new JSONObject();
		msgForClient.put("command", "update");
		msgForClient.put("update", "Show restaurant list");
		try {
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in Combo box option was selected");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException in Combo box option was selected");
		}
	}

	private void handleRestaurantSelected(JSONObject json) {
		JSONObject msgForClient = new JSONObject();
		msgForClient = db.getRestaurantMenu((JSONObject) json.get("restaurantInfo"));
		try {
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in Restaurant was selected");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException in Restaurant was selected");
		}
	}

	private void handleItemTypeSelected(JSONObject json) {
		JSONObject msgForClient = new JSONObject();
		msgForClient = json;
		msgForClient.put("command", "update");
		msgForClient.put("update", "Show meals by type");
		try {
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in Item types are displayed");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException in Item types are displayed");
		}
	}

	private void handleCheckOutClicked() {
		JSONObject msgForClient = new JSONObject();
		msgForClient.put("command", "update");
		msgForClient.put("update", "Show delivery window");
		try {
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in Check out button was clicked");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException in Check out button was clicked");
		}
	}

	private void handleDeliveryMethodSelected(JSONObject json) {
		JSONObject msgForClient = new JSONObject();
		JSONObject o = Message.getValueJObject(json, "order");
		o.put("employerID", employerID);
		o.put("employerW4C", employerW4C);
		o.put("employerName", employerName);
		msgForClient.put("command", "update");
		msgForClient.put("update", "Show payment window");
		msgForClient.put("order", o);
		try {
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in Delivery method was selected");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException in Delivery method was selected");
		}
	}

	private void handleConfirmButtonClicked(JSONObject json) {
		JSONObject msgForClient = new JSONObject();
		try {
			JSONObject order = Message.getValueJObject(json, "order");
			order.put("userID", userID);
			msgForClient = db.addOrder(order);
			if (Message.getValueString(msgForClient, "update").equals("Order was successfuly added")) {
				ConnectionToClient supplierCon = com.findConnection(Message.getValueString(order, "supplierID"));
				if (supplierCon != null)
					supplierCon.sendToClient(Parser.encode(msgForClient));
			} else {
				msgForClient.put("update", "Show pop up: failed order");
			}
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in Confirm button was clicked");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException in Confirm button was clicked");
		}
	}

	private void handleBackToHomePageButtonClicked() {
		JSONObject msgForClient = new JSONObject();
		msgForClient.put("command", "update");
		msgForClient.put("update", "Go back to homepage");
		try {
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in Order button was clicked");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException in Order button was clicked");
		}
	}

	private void handleViewOrderButtonClicked() {
		JSONObject msgForClient = new JSONObject();
		try {
			msgForClient.put("command", "update");
			msgForClient.put("update", "Show View Order window");
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in View Order button was clicked");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException in View Order button was clicked");
		}
	}

	private void handleViewButtonClicked() {
		JSONObject msgForClient = new JSONObject();
		try {
			msgForClient.put("command", "update");
			msgForClient.put("update", "Show order details");
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			System.out.println(e);
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in View button clicked");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException in View button clicked");
		}
	}

	private void handleAddMealButtonClicked() {
		JSONObject msgForClient = new JSONObject();
		msgForClient.put("command", "update");
		msgForClient.put("update", "Show item types list");
		try {
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in Add meal button was clicked");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException in Add meal button was clicked");
		}
	}

	private void handleApproveReceptionButtonClicked(JSONObject json) {
		JSONObject msgForClient = new JSONObject();
		try {
			json.put("userID", userID);
			msgForClient = db.updateOrderRecieved(json);
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in Approve Reception button clicked");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException in Approve Reception button clicked");
		}
	}

	private void handleScanButtonClicked() {
		JSONObject msgForClient = new JSONObject();
		try {
			msgForClient = db.fetchW4CDetails(userID);
			employerID = Message.getValueString(msgForClient, "employerID");
			employerW4C = Message.getValueString(msgForClient, "employerW4C");
			employerName = Message.getValueString(msgForClient, "employerName");
			connection.sendToClient(Parser.encode(msgForClient));
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING,
					"CustomerPortalViewController: handleCommandFromClient: IOException in Scan button was clicked");
			System.out.println(
					"CustomerPortalViewController: handleCommandFromClient: IOException in Scan button was clicked");
		}
	}

	private void handleLogOutClicked() {
		JSONObject msgForClient = new JSONObject();
		msgForClient.put("command", "handshake");
		msgForClient.put("portalType", "login");
		msgForClient.put("status", "ok");
		try {
			connection.sendToClient(Parser.encode(msgForClient));
			com.switchPortal(connection, "login");
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING, "CustomerPortalViewController: handleCommandFromClient: IOException in Log out");
			System.out.println("CustomerPortalViewController: handleCommandFromClient: IOException in Log out");
		}
		stop();
	}

	@Override
	public String getID() {
		return userID;
	}

	@Override
	public void setID(String ID) {
		userID = ID;
	}

}
