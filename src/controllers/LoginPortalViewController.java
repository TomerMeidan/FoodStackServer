package controllers;

import java.io.IOException;

import org.json.simple.JSONObject;

import ocsf.server.ConnectionToClient;
import serverSide.DataBase;
import common.Logger;
import common.Parser;
import common.Logger.Level;
import common.Message;

public class LoginPortalViewController implements PortalViewController {

	private DataBase db;
	private ComController com;
	private ConnectionToClient connection;
	private String portalType = "login";
	private String ID;

	public LoginPortalViewController(DataBase db, ComController com, ConnectionToClient connection) {
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

	@Override
	public void handleCommandFromClient(JSONObject json) {

		// log
		Logger.log(Level.DEBUG, "LoginPortalViewController: handleCommandFromClient: " + json);
		System.out.println("LoginPortalViewController: handleCommandFromClient: " + json);

		switch (Message.getValue(json, "command")) {
		case "login was pressed":
			JSONObject response = db.validateUser(json);
			if (response.containsKey("portalType")) {

				response = addRoleExtensions(response);
				response = checkIsOnlyUser(response);
				response = removeID(response);
			}

			try {
				if (response.get("status").equals("ok")) {
					portalType = (String) response.get("portalType");

					Logger.log(Level.DEBUG, "LoginPortalViewController: Client = " + connection.toString()
							+ " logged in as : " + portalType);
					System.out.println("LoginPortalViewController: Client = " + connection.toString() + " logged as "
							+ portalType);
				}

				connection.sendToClient(Parser.encode(response));
			} catch (IOException e) {
				// log
				Logger.log(Level.WARNING,
						"LoginPortalViewController: IOException exception in handleCommandFromClient");
				System.out.println("LoginPortalViewController: IOException exception in handleCommandFromClient");
			}
			break;
		case "home page is ready":
			// log
			Logger.log(Level.DEBUG,
					"LoginPortalViewController: home page : " + Message.getValue(json, "home page") + " is ready");
			System.out.println(
					"LoginPortalViewController: home page : " + Message.getValue(json, "home page") + " is ready");

			com.switchPortal(connection, portalType);

			// log
			Logger.log(Level.WARNING, "LoginPortalViewController: portal been switched to: " + portalType);
			System.out.println("LoginPortalViewController: portal been switched to: " + portalType);

			stop();
			break;

		default:
			// log
			Logger.log(Level.WARNING, "LoginPortalViewController: Unknown command : " + json.get("command"));
			System.out.println("LoginPortalViewController: Unknown command : " + json.get("command"));
			break;
		}
	}

	private JSONObject addRoleExtensions(JSONObject response) {

			switch (Message.getValue(response, "portalType")) {
			case "HR":
				String employerID = db.getEmployerID(response);

				if (employerID == null) {
					response.replace("status", "notOk");
				} else {
					response.put("employerID", employerID);
				}
				break;

			default:
				break;
			}
		
		return response;
	}

	@SuppressWarnings("unchecked")
	private JSONObject checkIsOnlyUser(JSONObject response) {
		String id = Message.getValue(response, "userID");

		synchronized (com.mutex) {
			if (!com.isUserOnline(id)) {
				ID = id;
			} else {
				response.replace("status", "notOk");
			}
		}
		return response;
	}

	private JSONObject removeID(JSONObject response) {
//		if (response.containsKey("userID"))
//			response.remove("userID");
		return response;
	}
}