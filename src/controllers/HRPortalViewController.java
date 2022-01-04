package controllers;

import java.io.IOException;

import org.json.simple.JSONObject;

import common.Logger;
import common.Logger.Level;
import common.Message;
import common.Parser;
import ocsf.server.ConnectionToClient;
import serverSide.DataBase;

/**
 * HRPortalViewController
 * 
 * This class is the Controller based on ECB pattern.
 * This class controls HR events.
 * This class holds db, com, connection, ID variables.
 * ComController com - for handling communication.
 * ConnectionToClient connection - for sending messages.
 * String ID - is the userid in db, used for identifying user.
 * @author Roman Milman
 */
public class HRPortalViewController implements PortalViewController {

	private DataBase db;
	private ComController com;
	private ConnectionToClient connection;
	private String ID;

	/**
	 * HRPortalViewController constructor
	 * 
	 * @param DataBase db
	 * @param ComController com
	 * @param ConnectionToClient connection
	 * @author Roman Milman
	 */
	public HRPortalViewController(DataBase db, ComController com, ConnectionToClient connection) {
		this.db = db;
		this.com = com;
		this.connection = connection;
	}

	/**
	 * start
	 * 
	 * No use, for future flexibility.
	 * @author Roman Milman
	 */
	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	/**
	 * start
	 * 
	 * No use, for future flexibility.
	 * @author Roman Milman
	 */
	@Override
	public void stop() {
		// TODO Auto-generated method stub

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
		return ID;
	}

	/**
	 * setID
	 * 
	 * sets ID.
	 * @author Roman Milman
	 */
	@Override
	public void setID(String ID) {
		this.ID = ID;
	}

	/**
	 * handleCommandFromClient
	 * 
	 * handles commands from client
	 * @param JSONObject msg - contains 'command' key for identifying which event occurred
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void handleCommandFromClient(JSONObject json) {

		// log
		Logger.log(Level.DEBUG, "HRPortalViewController: handleCommandFromClient: " + json);
		System.out.println("HRPortalViewController: handleCommandFromClient: " + json);

		switch (Message.getValueString(json, "command")) {
		case "logout was pressed":
			handleLogout(json);
			break;
		case "approve business client was pressed":
			handleEventApproveBusinessCustomers(json);
			break;
		case "approve was pressed":
			handleApproveBusinessCustomer(json);
			break;
		case "register employer was pressed":
			handleEventRegisterEmployer();
			break;
		case "supplier register was pressed":
			handleEmployerRegister(json);
			break;

		default:
			Logger.log(Level.DEBUG, "HRPortalViewController: unknown message in handleCommandFromClient");
			System.out.println("HRPortalViewController: unknown message in handleCommandFromClient");
			break;
		}
	}

	/**
	 * handleEmployerRegister
	 * 
	 * handles register employer commands from client
	 * @param JSONObject msg.
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	private void handleEmployerRegister(JSONObject json) {
		JSONObject response = new JSONObject();
		response = db.registerEmployer(json);
		response.put("command", "update");
		try {
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"HRPortalViewController: IOException was caught in handleCommandFromClient");
			System.out.println(
					"HRPortalViewController: IOException was caught in handleCommandFromClient");
		}
	}

	/**
	 * handleEventRegisterEmployer
	 * 
	 * handles event employer registration
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	private void handleEventRegisterEmployer() {
		JSONObject response = new JSONObject();
		response = db.getEmployerForHr(getID());
		response.put("command", "update");
		response.put("update", "show employer registration window");
		try {
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"HRPortalViewController: IOException was caught in handleCommandFromClient");
			System.out.println(
					"HRPortalViewController: IOException was caught in handleCommandFromClient");
		}
	}

	/**
	 * handleApproveBusinessCustomer
	 * 
	 * handles approve business customer command
	 * @param JSONObject msg.
	 * @author Roman Milman
	 */
	private void handleApproveBusinessCustomer(JSONObject json) {
		JSONObject response = new JSONObject();
		response = db.activeCustomer(json);
		try {
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING, "HRPortalViewController: IOException was caught in handleCommandFromClient");
			System.out.println("HRPortalViewController: IOException was caught in handleCommandFromClient");
		}
	}

	/**
	 * handleEventApproveBusinessCustomers
	 * 
	 * handles event approve business customer
	 * @param JSONObject msg.
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	private void handleEventApproveBusinessCustomers(JSONObject json) {
		JSONObject response = new JSONObject();
		response = db.getInactiveBusinessCustomers(json);
		response.put("command", "update");
		response.put("update", "show approve business client window");
		try {
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING, "HRPortalViewController: IOException was caught in handleCommandFromClient");
			System.out.println("HRPortalViewController: IOException was caught in handleCommandFromClient");
		}
	}

	/**
	 * handleLogout
	 * 
	 * handles logout command
	 * @param JSONObject msg.
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	private void handleLogout(JSONObject msg) {
		JSONObject response = new JSONObject();

		response.put("command", "handshake");
		response.put("portalType", "login");
		response.put("status", "ok");
		try {
			// log
			Logger.log(Level.DEBUG, "HRPortalViewController: Client = " + connection.toString() + " logged out");
			System.out.println("HRPortalViewController: Client = " + connection.toString() + " logged out");

			connection.sendToClient(Parser.encode(response));
			com.switchPortal(connection, "login");

		} catch (IOException e) {
			Logger.log(Level.WARNING, "HRPortalViewController: IOException was caught in handleCommandFromClient");
			System.out.println("HRPortalViewController: IOException was caught in handleCommandFromClient");
		}
	}

}
