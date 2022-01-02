package controllers;

import java.io.IOException;

import org.json.simple.JSONObject;

import common.Logger;
import common.Message;
import common.Parser;
import common.Logger.Level;
import ocsf.server.ConnectionToClient;
import serverSide.DataBase;

public class HRPortalViewController implements PortalViewController {

	private DataBase db;
	private ComController com;
	private ConnectionToClient connection;
	private String ID;

	public HRPortalViewController(DataBase db, ComController com, ConnectionToClient connection) {
		this.db = db;
		this.com = com;
		this.connection = connection;
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
	public String getID() {
		return ID;
	}

	@Override
	public void setID(String ID) {
		this.ID = ID;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handleCommandFromClient(JSONObject json) {

		// log
		Logger.log(Level.DEBUG, "HRPortalViewController: handleCommandFromClient: " + json);
		System.out.println("HRPortalViewController: handleCommandFromClient: " + json);

		switch (Message.getValue(json, "command")) {
		case "logout was pressed":
			handleLogout(json);
			break;
		case "approve business client was pressed":
			handleEventApproveBusiness(json);
			break;
		case "approve was pressed":
			handleApprove(json);
			break;
		case "register employer was pressed":
			handleEventRegisterEmployer();
			break;
		case "supplier register was pressed":
			handleEventSupplierRegister(json);
			break;

		default:
			Logger.log(Level.DEBUG, "HRPortalViewController: unknown message in handleCommandFromClient");
			System.out.println("HRPortalViewController: unknown message in handleCommandFromClient");
			break;
		}
	}

	@SuppressWarnings("unchecked")
	private void handleEventSupplierRegister(JSONObject json) {
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

	private void handleApprove(JSONObject json) {
		JSONObject response = new JSONObject();
		response = db.activeCustomer(json);
		try {
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING, "HRPortalViewController: IOException was caught in handleCommandFromClient");
			System.out.println("HRPortalViewController: IOException was caught in handleCommandFromClient");
		}
	}

	@SuppressWarnings("unchecked")
	private void handleEventApproveBusiness(JSONObject json) {
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
