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
 * LoginPortalViewController
 * 
 * This class is the Controller based on ECB pattern.
 * This class controls Login events.
 * This class holds db, com, connection, ID variables.
 * ComController com - for handling communication.
 * ConnectionToClient connection - for sending messages.
 * String ID - is the userid in db, this is initialized as null.
 * @author Roman Milman
 */
public class LoginPortalViewController implements PortalViewController {

	private DataBase db;
	private ComController com;
	private ConnectionToClient connection;
	private String portalType = "login";
	private String ID;

	/**
	 * LoginPortalViewController constructor
	 * 
	 * @param DataBase db
	 * @param ComController com
	 * @param ConnectionToClient connection
	 * @author Roman Milman
	 */
	public LoginPortalViewController(DataBase db, ComController com, ConnectionToClient connection) {
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
	 * stop
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
	@Override
	public void handleCommandFromClient(JSONObject json) {

		// log
		Logger.log(Level.DEBUG, "LoginPortalViewController: handleCommandFromClient: " + json);
		System.out.println("LoginPortalViewController: handleCommandFromClient: " + json);

		switch (Message.getValueString(json, "command")) {
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
					"LoginPortalViewController: home page : " + Message.getValueString(json, "home page") + " is ready");
			System.out.println(
					"LoginPortalViewController: home page : " + Message.getValueString(json, "home page") + " is ready");

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

	/**
	 * addRoleExtensions
	 * 
	 * This method adds extra information if needed to response.
	 * @param JSONObject response
	 * @return JSONObject
	 * @author Roman Milman
	 */
	private JSONObject addRoleExtensions(JSONObject response) {

			switch (Message.getValueString(response, "portalType")) {
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

	/**
	 * checkIsOnlyUser
	 * 
	 * This method checks if any other user logged in into wanted account by userID that given as input.
	 * If someone else allready logged in, returns 'notOk' status.
	 * @param JSONObject response - includes "userID" as key to userID value in DB.
	 * @return JSONObject
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	private JSONObject checkIsOnlyUser(JSONObject response) {
		String id = Message.getValueString(response, "userID");

		synchronized (com.mutex) {
			if (!com.isUserOnline(id)) {
				ID = id;
			} else {
				response.replace("status", "notOk");
				response.put("notOk", "User already logged in");
			}
		}
		return response;
	}

	/**
	 * removeID
	 * 
	 * This method deletes userID that defined at DB from response, in order to not leak sensitive information.
	 * @param JSONObject response.
	 * @return JSONObject
	 * @author Roman Milman
	 */
	private JSONObject removeID(JSONObject response) {
//		if (response.containsKey("userID"))
//			response.remove("userID");
		return response;
	}
}