package controllers;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import serverSide.DataBase;
import serverSide.EventManager;

import org.json.simple.JSONObject;

import ocsf.server.ConnectionToClient;
import common.Logger;
import common.Message;
import common.Parser;
import common.Logger.Level;
/**
 * SupplierPortalViewController
 * 
 * This class is the Controller based on ECB pattern.
 * This class controls Supplier events.
 * This class holds db, com, connection, ID variables.
 * ComController com - for handling communication.
 * ConnectionToClient connection - for sending messages.
 * String ID - is the userid in db, used for identifying user.
 * @author Daniel Ohayon
 */
@SuppressWarnings("unchecked")
public class SupplierPortalViewController implements PortalViewController {

	private DataBase db;
	private ComController com;
	private ConnectionToClient connection;
	private EventManager manage;
	private String userID;

	/**
	 * SupplierPortalViewController constructor
	 * 
	 * @param DataBase db
	 * @param ComController com
	 * @param ConnectionToClient connection
	 */
	public SupplierPortalViewController(DataBase db, ComController com, ConnectionToClient connection) {
		this.db = db;
		this.com = com;
		this.connection = connection;
		manage = EventManager.getInstance();
		System.out.println("connection address: " + connection.getInetAddress());
	}

	/**
	 * start
	 * 
	 * No use, for future flexibility.
	 */
	@Override
	public void start() {
		// TODO Auto-generated method stub

	}
	/**
	 * start
	 * 
	 * No use, for future flexibility.
	 */
	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	/**
	 * handleCommandFromClient
	 * 
	 * handles commands from client
	 * @param JSONObject msg - contains 'command' key for identifying which event occurred
	 */
	@Override
	public void handleCommandFromClient(JSONObject json) {
		JSONObject respons = new JSONObject();
		switch (Message.getValue(json, "command")) {
		case "ready":
			com.switchPortal(connection, "Supplier");
			break;
		case "Update menu was clicked":
			handleUpdateMenuDisplayed();
			break;
		case "Types presented is ready":
			handleTypeDisplayed(json);
			break;
		case "Update status was clicked":
			handleUpdateStatusDisplayed();
			break;
		case "Receipts button was clicked":
			handleReceiptsDisplayed(json);
			break;
		case "Enter meals button was pressed":
			handleMealsDisplayed(json);
			break;

		case "Order list presented is ready":
			handleOrderListDisplayed(json);
			break;
		case "Receipts list presented is ready":
			handleReceiptsListDisplayed(json);
			break;
		case "clickOnLogOutButton":
			respons = new JSONObject();
			respons.put("command", "handshake");
			respons.put("portalType", "login");
			respons.put("status", "ok");

			try {

				connection.sendToClient(Parser.encode(respons));
				com.switchPortal(connection, "login");
			} catch (IOException e) {
				// log
				Logger.log(Level.WARNING, "SupplierPortalViewController: IOException in clickOnLogOutButton");
				System.out.println("SupplierPortalViewController: IOException in clickOnLogOutButton");
			}
			break;
		case "Edit type button was pressed":
			respons = json;
			respons.put("command", "update");
			respons.put("update", "showEditTypeDetails");
			// respons.put("itemType", json.get("itemType"));
			try {
				connection.sendToClient(Parser.encode(respons));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			break;
		case "Save edit type was pressed":
			respons = db.editType(json);
			try {
				connection.sendToClient(Parser.encode(respons));
			} catch (IOException e) {
				// log
				Logger.log(Level.WARNING, "SupplierPortalViewController: IOException in updateWindowIsReady");
				System.out.println("SupplierPortalViewController: IOException in updateWindowIsReady");
			}
			break;
		case "Add Type button was pressed":
			respons.put("command", "update");
			respons.put("update", "showAddTypeDetails");
			try {
				connection.sendToClient(Parser.encode(respons));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			break;
		case "Save add type was pressed":
			respons = db.addType(json);
			try {
				connection.sendToClient(Parser.encode(respons));
			} catch (IOException e) {
				// log
				Logger.log(Level.WARNING, "SupplierPortalViewController: IOException in updateWindowIsReady");
				System.out.println("SupplierPortalViewController: IOException in updateWindowIsReady");
			}
			break;
		case "Exit add type was pressed":
			respons.put("command", "update");
			respons.put("update", "exitNewType");
			try {
				connection.sendToClient(Parser.encode(respons));
			} catch (IOException e) {
				// log
				Logger.log(Level.WARNING, "SupplierPortalViewController: IOException in updateWindowIsReady");
				System.out.println("SupplierPortalViewController: IOException in updateWindowIsReady");
			}
			break;
			
		case "Exit edit/add mael was pressed":
			respons.put("command", "update");
			respons.put("update", "exitAdd/EditMeal");
			respons.put("itemType", json.get("itemType"));
			try {
				connection.sendToClient(Parser.encode(respons));
			} catch (IOException e) {
				Logger.log(Level.WARNING, "SupplierPortalViewController: IOException in updateWindowIsReady");
				System.out.println("SupplierPortalViewController: IOException in updateWindowIsReady");
			}
			break;
		case "Delete type Button was pressed":
			respons = db.deleteType(json);
			try {
				connection.sendToClient(Parser.encode(respons));
			} catch (IOException e) {
				// log
				Logger.log(Level.WARNING, "SupplierPortalViewController: IOException in updateWindowIsReady");
				System.out.println("SupplierPortalViewController: IOException in updateWindowIsReady");
			}
			break;
		case "Edit meal button was pressed":
			respons = json;
			respons.put("command", "update");
			respons.put("update", "showEditMealDetails");
			try {
				connection.sendToClient(Parser.encode(respons));
			} catch (IOException e) {
				// log
				Logger.log(Level.WARNING, "SupplierPortalViewController: IOException in updateWindowIsReady");
				System.out.println("SupplierPortalViewController: IOException in updateWindowIsReady");
			}
			break;
		case "Save feature was pressed":
			respons = db.immediateFeaturesCheck(json);
			try {
				connection.sendToClient(Parser.encode(respons));
			} catch (IOException e) {
				// log
				Logger.log(Level.WARNING, "SupplierPortalViewController: IOException in updateWindowIsReady");
				System.out.println("SupplierPortalViewController: IOException in updateWindowIsReady");
			}
			break;

		case "Save edit feature was pressed":
			respons = db.immediateFeaturesEditCheck(json);
			try {
				connection.sendToClient(Parser.encode(respons));
			} catch (IOException e) {
				// log
				Logger.log(Level.WARNING, "SupplierPortalViewController: IOException in updateWindowIsReady");
				System.out.println("SupplierPortalViewController: IOException in updateWindowIsReady");
			}
			break;
		case "Save edit meal was pressed":
			respons = db.saveEditDish(json);
			try {
				connection.sendToClient(Parser.encode(respons));
			} catch (IOException e) {
				// log
				Logger.log(Level.WARNING, "SupplierPortalViewController: IOException in updateWindowIsReady");
				System.out.println("SupplierPortalViewController: IOException in updateWindowIsReady");
			}
			break;
		case "Exit edit type was pressed":
			respons.put("command", "update");
			respons.put("update", "exitEditType");
			try {
				connection.sendToClient(Parser.encode(respons));
			} catch (IOException e) {
				// log
				Logger.log(Level.WARNING, "SupplierPortalViewController: IOException in updateWindowIsReady");
				System.out.println("SupplierPortalViewController: IOException in updateWindowIsReady");
			}
			break;
		case "Add Meal button was pressed":
			respons.put("command", "update");
			respons.put("update", "showAddMealDetails");
			respons.put("itemType", Message.getValue(json, "itemType"));
			try {
				connection.sendToClient(Parser.encode(respons));
			} catch (IOException e) {
				// log
				Logger.log(Level.WARNING, "SupplierPortalViewController: IOException in updateWindowIsReady");
				System.out.println("SupplierPortalViewController: IOException in updateWindowIsReady");
			}
			break;
		case "Save Meal was pressed":
			respons = db.saveAddDish(json);
			try {
				connection.sendToClient(Parser.encode(respons));
			} catch (IOException e) {
				// log
				Logger.log(Level.WARNING, "SupplierPortalViewController: IOException in updateWindowIsReady");
				System.out.println("SupplierPortalViewController: IOException in updateWindowIsReady");
			}
			break;

		case "Delete Meal Button was pressed":
			respons = db.deleteDish(json);
			try {
				connection.sendToClient(Parser.encode(respons));
			} catch (IOException e) {
				// log
				Logger.log(Level.WARNING, "SupplierPortalViewController: IOException in updateWindowIsReady");
				System.out.println("SupplierPortalViewController: IOException in updateWindowIsReady");
			}
			break;
		case "Approve button was pressed":
			try {
				ArrayList<String> approvalCustomer = (ArrayList<String>) json.get("customersID");
				for (String id : approvalCustomer) {
					ConnectionToClient customerConnection = com.findConnection(id);
					respons = manage.orderIsReady(json);
					respons.put("customerID", id);
					if (customerConnection != null) {
						customerConnection.sendToClient(Parser.encode(respons));
					}
					connection.sendToClient(Parser.encode(respons));
				}
			} catch (IOException e) {
				// log
				Logger.log(Level.WARNING, "SupplierPortalViewController: IOException in updateWindowIsReady");
				System.out.println("SupplierPortalViewController: IOException in updateWindowIsReady");
			}
			break;
		case "Order details button was pressed":
			respons = json;
			respons.put("command", "update");
			respons.put("update", "showOrderDetails");

			try {
				connection.sendToClient(Parser.encode(respons));
			} catch (IOException e) {
				// log
				Logger.log(Level.WARNING, "SupplierPortalViewController: IOException in updateWindowIsReady");
				System.out.println("SupplierPortalViewController: IOException in updateWindowIsReady");
			}
			break;

		default:
			// log
			Logger.log(Level.WARNING, "SupplierPortalViewController: Unknown command : " + json.get("command"));
			System.out.println("SupplierPortalViewController: Unknown command : " + json.get("command"));
			break;
		}

	}

	@SuppressWarnings("unchecked")
	private void handleUpdateMenuDisplayed() {
		JSONObject respons = new JSONObject();
		respons.put("command", "update");
		respons.put("update", "showUpdateWindow");
		try {
			connection.sendToClient(Parser.encode(respons));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"SupplierPortalViewController: handleCommandFromClient: IOException was caught in update status window displayed");
			System.out.println(
					"SupplierPortalViewController: handleCommandFromClient: IOException was caught in update status window displayed");
		}
	}

	@SuppressWarnings("unchecked")
	private void handleTypeDisplayed(JSONObject json) {
		JSONObject respons = new JSONObject();
		respons = db.getMenu(Message.getValue(json, "userID"));
		try {
			connection.sendToClient(Parser.encode(respons));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"SupplierPortalViewController: handleCommandFromClient: IOException was caught in type window displayed");
			System.out.println(
					"SupplierPortalViewController: handleCommandFromClient: IOException was caught in type window displayed");
		}
	}

	@SuppressWarnings("unchecked")
	private void handleUpdateStatusDisplayed() {
		JSONObject respons = new JSONObject();
		respons.put("command", "update");
		respons.put("update", "showUpdateStatusWindow");
		try {
			connection.sendToClient(Parser.encode(respons));
		} catch (IOException e1) {
			Logger.log(Level.WARNING,
					"SupplierPortalViewController: handleCommandFromClient: IOException was caught in meals window displayed");
			System.out.println(
					"SupplierPortalViewController: handleCommandFromClient: IOException was caught in meals window displayed");
		}

	}

	@SuppressWarnings("unchecked")
	private void handleReceiptsDisplayed(JSONObject json) {
		
		JSONObject respons = new JSONObject();
		respons.put("command", "update");
		respons.put("update", "showReceiptsWindow");
		try {
			connection.sendToClient(Parser.encode(respons));
		} catch (IOException e1) {
			Logger.log(Level.WARNING,
					"SupplierPortalViewController: handleCommandFromClient: IOException was caught in receipt list window displayed");
			System.out.println(
					"SupplierPortalViewController: handleCommandFromClient: IOException was caught in receipt list window displayed");
		}
	}
	

	@SuppressWarnings("unchecked")
	private void handleMealsDisplayed(JSONObject json) {
		JSONObject respons = new JSONObject();
		respons.put("command", "update");
		respons.put("update", "itemNames");
		respons.put("itemType", json.get("itemType"));
		try {
			connection.sendToClient(Parser.encode(respons));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"SupplierPortalViewController: handleCommandFromClient: IOException was caught in meals window displayed");
			System.out.println(
					"SupplierPortalViewController: handleCommandFromClient: IOException was caught in meals window displayed");
		}

	}

	@SuppressWarnings("unchecked")
	private void handleOrderListDisplayed(JSONObject json) {
		JSONObject respons = new JSONObject();
		respons = db.getOrderList(json);
		try {
			connection.sendToClient(Parser.encode(respons));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"SupplierPortalViewController: handleCommandFromClient: IOException was caught in order list window displayed");
			System.out.println(
					"SupplierPortalViewController: handleCommandFromClient: IOException was caught in order list window displayed");
		}
	}
	
	@SuppressWarnings("unchecked")
	private void handleReceiptsListDisplayed(JSONObject json) {
		JSONObject respons = new JSONObject();
		respons = db.getReceiptList(json);
		try {
			connection.sendToClient(Parser.encode(respons));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"SupplierPortalViewController: handleCommandFromClient: IOException was caught in receipts window displayed");
			System.out.println(
					"SupplierPortalViewController: handleCommandFromClient: IOException was caught in update status window displayed");
		}
	
	}
	/**
	 * getID
	 * 
	 * returns ID.
	 * @return String
	 * @author Roman Milman
	 */
	@Override
	public String getID() {
		return userID;
	}
	/**
	 * setID
	 * 
	 * sets ID.
	 * @return String
	 */
	@Override
	public void setID(String ID) {
		userID = ID;
	}
}
