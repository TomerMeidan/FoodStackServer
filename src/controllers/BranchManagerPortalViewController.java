package controllers;

import java.io.IOException;
import java.sql.SQLException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import ocsf.server.ConnectionToClient;
import serverSide.DataBase;
import common.Logger;
import common.Message;
import common.Parser;
import common.Logger.Level;

/**
 * BranchManagerPortalViewController
 * 
 * This class is the Controller based on ECB pattern.
 * This class controls Branch Manager events.
 * This class holds db, com, connection, ID variables.
 * ComController com - for handling communication.
 * ConnectionToClient connection - for sending messages.
 * String ID - is the userid in db, used for identifying user.
 * @author Roman Milman
 */
public class BranchManagerPortalViewController implements PortalViewController {

	private DataBase db;
	private ComController com;
	private ConnectionToClient connection;
	private String ID;

	/**
	 * BranchManagerPortalViewController constructor
	 * 
	 * @param DataBase db
	 * @param ComController com
	 * @param ConnectionToClient connection
	 * @author Roman Milman
	 */
	public BranchManagerPortalViewController(DataBase db, ComController com, ConnectionToClient connection) {
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
	@SuppressWarnings("unchecked")
	@Override
	public void handleCommandFromClient(JSONObject msg) {
		// log
		Logger.log(Level.DEBUG, "BranchManagerPortalViewController: handleCommandFromClient: " + msg);
		System.out.println("BranchManagerPortalViewController: handleCommandFromClient: " + msg);

		switch (Message.getValue(msg, "command")) {
		case "logout was pressed":
			handleLogout(msg);
			break;
		case "client register was pressed":
			handleEventClientRegister(msg);
			break;
		case "customer register was pressed":
			handleRegistration(msg);
			break;
		case "supplier register was pressed":
			handleRegistration(msg);
			break;
		case "approve employer was pressed":
			handleEventApproveEmployer(msg);
			break;
		case "approve was pressed":
			handleApproveEmployer(msg);
			break;
		case "supplier registration was pressed":
			handleEventSupplierRegistration(msg);
			break;
		case "monthly reports button is pressed":
			handleEventMonthlyReports(msg);
			break;
		case "upload quarterly reports button was pressed":
			handleEventQuarterlyReports(msg);
			break;
		case "upload report file":
			handleEventUploadReports(msg);
			break;
		case "edit information was pressed":
			handleEventEditInformation(msg);
			break;
		case "edit roles was pressed":
			handleEditRoles(msg);
			break;
		case "role switch has been pressed":
			handleSwitchRole(msg);
			break;
		case "edit status was pressed":
			handleEditStatus(msg);
			break;
		case "status switch has been pressed":
			handleSwitchStatus(msg);
			break;

		default:
			Logger.log(Level.DEBUG, "BranchManagerPortalViewController: unknown message in handleCommandFromClient");
			System.out.println("BranchManagerPortalViewController: unknown message in handleCommandFromClient");
			break;
		}
	}

	/**
	 * handleSwitchStatus
	 * 
	 * handles switch commands from client
	 * @param JSONObject msg - contains 'Role' key for identifying which role does the switching.
	 * @author Roman Milman
	 */
	private void handleSwitchStatus(JSONObject msg) {
		String role = Message.getValue(msg, "Role");
		String newStatus;

		if ("active".equals(Message.getValue(msg, "Status")))
			newStatus = "freeze";
		else
			newStatus = "active";

		switch (role) {
		case "Customer":
			handleCustomerStatusSwitch(msg,newStatus);
			break;
		case "Business Customer":
			handleCustomerStatusSwitch(msg,newStatus);
			break;
		case "Employer":
			handleEmployerStatusSwitch(msg,newStatus);
			break;
		case "HR":
			handleHRStatusSwitch(msg,newStatus);
			break;
		case "Supplier":
			handleSupplierStatusSwitch(msg,newStatus);
			break;

		default:
			Logger.log(Level.DEBUG, "BranchManagerPortalViewController: unknown message in handleSwitchStatus");
			System.out.println("BranchManagerPortalViewController: unknown message in handleSwitchStatus");
			break;
		}
	}

	/**
	 * handleSupplierStatusSwitch
	 * 
	 * handles supplier switch commands from client
	 * @param JSONObject msg
	 * @param String newStatus - is the new status we switch into.
	 * @author Roman Milman
	 */
	private void handleSupplierStatusSwitch(JSONObject msg, String newStatus) {
		JSONObject response = new JSONObject();

		try {
			response = db.switchStatusToSupplier(msg,newStatus);
			response.put("command", "update");
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: IOException was caught in handleEmployerStatusSwitch");
			System.out.println("BranchManagerPortalViewController: IOException was caught in handleEmployerStatusSwitch");
		}
	}

	/**
	 * handleHRStatusSwitch
	 * 
	 * handles HR switch commands from client
	 * @param JSONObject msg
	 * @param String newStatus - is the new status we switch into.
	 * @author Roman Milman
	 */
	private void handleHRStatusSwitch(JSONObject msg, String newStatus) {
		JSONObject response = new JSONObject();

		try {
			response = db.switchStatusToHR(msg,newStatus);
			response.put("command", "update");
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: IOException was caught in handleEmployerStatusSwitch");
			System.out.println("BranchManagerPortalViewController: IOException was caught in handleEmployerStatusSwitch");
		}
	}

	/**
	 * handleEmployerStatusSwitch
	 * 
	 * handles Employer switch commands from client
	 * @param JSONObject msg
	 * @param String newStatus - is the new status we switch into.
	 * @author Roman Milman
	 */
	private void handleEmployerStatusSwitch(JSONObject msg, String newStatus) {
		JSONObject response = new JSONObject();

		try {
			response = db.switchStatusToEmployer(msg,newStatus);
			response.put("command", "update");
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: IOException was caught in handleEmployerStatusSwitch");
			System.out.println("BranchManagerPortalViewController: IOException was caught in handleEmployerStatusSwitch");
		}
	}

	/**
	 * handleCustomerStatusSwitch
	 * 
	 * handles Customer switch commands from client
	 * @param JSONObject msg
	 * @param String newStatus - is the new status we switch into.
	 * @author Roman Milman
	 */
	private void handleCustomerStatusSwitch(JSONObject msg, String newStatus) {
		JSONObject response = new JSONObject();

		try {
			response = db.switchStatusToCustomer(msg,newStatus);
			response.put("command", "update");
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: IOException was caught in handleSupplierStatusSwitch");
			System.out.println("BranchManagerPortalViewController: IOException was caught in handleSupplierStatusSwitch");
		}
	}

	/**
	 * handleEditStatus
	 * 
	 * handles edit status commands from client
	 * sends a message to client with information accordingly.
	 * @param JSONObject msg
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	private void handleEditStatus(JSONObject msg) {
		JSONObject response = new JSONObject();
		response = db.getAllUsersByBranch(msg);
		if (response == null)
			return;
		response.put("command", "update");
		response.put("update", "show edit status window");
		try {
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: IOException was caught in handleEventEditStatus");
			System.out.println("BranchManagerPortalViewController: IOException was caught in handleEventEditStatus");
		}
	}

	/**
	 * handleEventUploadReports
	 * 
	 * handles event upload reports
	 * @param JSONObject msg
	 * @author Roman Milman
	 */
	private void handleEventUploadReports(JSONObject msg) {
		try {
			JSONObject responseFromServer = db.addFileToDataBase(msg);
			connection.sendToClient(Parser.encode(responseFromServer));
		} catch (SQLException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: handleCommandFromClient: SQLException was thrown in upload report file case");
			System.out.println(
					"BranchManagerPortalViewController: handleCommandFromClient: SQLException was thrown in upload report file case");
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: handleCommandFromClient: IOException was thrown in upload report file case");
			System.out.println(
					"BranchManagerPortalViewController: handleCommandFromClient: IOException was thrown in upload report file case");
		}
	}

	@SuppressWarnings("unchecked")
	private void handleEventQuarterlyReports(JSONObject msg) {
		JSONObject allReportsData = new JSONObject();
		allReportsData.put("command", "update");
		allReportsData.put("update", "show upload quarterly reports window");
		JSONArray incomeReportData = db.getIncomeByLastQuarter(msg);
		JSONArray performanceReportData = db.getPerformanceByLastQuarter(msg);
		JSONArray itemsReportData = db.getItemsByLastQuarter(msg);

		allReportsData.put("income reports data", incomeReportData);
		allReportsData.put("items reports data", itemsReportData);
		allReportsData.put("performance reports data", performanceReportData);

		try {
			connection.sendToClient(Parser.encode(allReportsData));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: handleCommandFromClient: IOException was thrown in upload quarterly reports button was pressed case");
			System.out.println(
					"BranchManagerPortalViewController: handleCommandFromClient: IOException was thrown in upload quarterly reports button was pressed case");
			return;
		}

		Logger.log(Level.DEBUG,
				"BranchManagerPortalViewController: handleCommandFromClient: Sent all restaurants orders: "
						+ allReportsData + " | -> to user:  " + connection.getInetAddress());
		System.out.println("BranchManagerPortalViewController: handleCommandFromClient: Sent all restaurants orders: "
				+ allReportsData + " | -> to user:  " + connection.getInetAddress());
	}

	/**
	 * handleSwitchRole
	 * 
	 * handles switch role command
	 * @param JSONObject msg - contains 'new role' key representing the new role we switch into.
	 * @author Roman Milman
	 */
	private void handleSwitchRole(JSONObject msg) {
		String newRole = Message.getValue(msg, "new role");

		switch (newRole) {
		case "Customer":
			handleRegularRoleSwitch(msg);
			break;
		case "Business Customer":
			handleBusinessRoleSwitch(msg);
			break;

		default:
			Logger.log(Level.DEBUG, "BranchManagerPortalViewController: unknown message in handleSwitchRole");
			System.out.println("BranchManagerPortalViewController: unknown message in handleSwitchRole");
			break;
		}

	}

	/**
	 * handleBusinessRoleSwitch
	 * 
	 * handles business customer switch role command
	 * @param JSONObject msg
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	private void handleBusinessRoleSwitch(JSONObject msg) {
		JSONObject response = new JSONObject();
		int employerUserID = -1;
		if ((employerUserID = db.isEmployerExists(msg)) == -1) {
			response.put("update", "could not switch role. employer doesn't exists");
		} else {
			response = db.switchRoleToBusinessCustomer(msg, employerUserID);
		}
		try {
			response.put("command", "update");
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: IOException was caught in handleBusinessSwitch");
			System.out.println("BranchManagerPortalViewController: IOException was caught in handleBusinessSwitch");
		}
	}

	/**
	 * handleRegularRoleSwitch
	 * 
	 * handles regular customer switch role command
	 * @param JSONObject msg
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	private void handleRegularRoleSwitch(JSONObject msg) {
		JSONObject response = new JSONObject();

		try {
			response = db.switchRoleToRegularCustomer(msg);
			response.put("command", "update");
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: IOException was caught in handleRegularSwitch");
			System.out.println("BranchManagerPortalViewController: IOException was caught in handleRegularSwitch");
		}
	}

	/**
	 * handleEditRoles
	 * 
	 * handles edit roles command
	 * sends a message to client with information accordingly.
	 * @param JSONObject msg
	 * @author Roman Milman
	 */
	private void handleEditRoles(JSONObject msg) {
		JSONObject response = new JSONObject();
		response = db.getAllCustomersByBranch(msg);
		response.put("update", "show edit role window");
		response.put("command", "update");
		try {
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING, "BranchManagerPortalViewController: IOException was caught in handleEditRoles");
			System.out.println("BranchManagerPortalViewController: IOException was caught in handleEditRoles");
		}
	}

	/**
	 * handleEventEditInformation
	 * 
	 * handles edit information event
	 * @param JSONObject msg
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	private void handleEventEditInformation(JSONObject msg) {
		JSONObject response = new JSONObject();
		response.put("command", "update");
		response.put("update", "show edit choices window");
		try {
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: IOException was caught in handleEventEditInformation");
			System.out
					.println("BranchManagerPortalViewController: IOException was caught in handleEventEditInformation");
		}
	}

	/**
	 * handleEventMonthlyReports
	 * 
	 * handles monthly reports event
	 * sends a message to client with information accordingly.
	 * @param JSONObject msg
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	private void handleEventMonthlyReports(JSONObject msg) {
		Logger.log(Level.WARNING, "Message received from user: " + Message.getValue(msg, "message"));
		System.out.println("Message received from user: " + Message.getValue(msg, "message"));

		JSONObject allReportsData = new JSONObject();
		allReportsData.put("command", "update");
		allReportsData.put("update", "all restaurants reports");

		JSONArray incomeReportData = db.getExistingIncomeReportInformation();
		JSONArray itemsReportData = db.getExistingItemsPerRestaurantReportInformation();
		JSONArray performanceReportData = db.getExistingPerformanceReportInformation();

		allReportsData.put("income reports data", incomeReportData);
		allReportsData.put("items reports data", itemsReportData);
		allReportsData.put("performance reports data", performanceReportData);

		try {
			connection.sendToClient(Parser.encode(allReportsData));
		} catch (IOException e) {
			Logger.log(Level.WARNING,

					"BranchManagerPortalViewController: handleCommandFromClient: IOException was thrown in monthly reports button is pressed case");
			System.out.println(
					"BranchManagerPortalViewController: handleCommandFromClient: IOException was thrown in monthly reports button is pressed case");

			return;
		}

		Logger.log(Level.DEBUG,
				"BranchManagerPortalViewController: handleCommandFromClient: Sent all restaurants orders: "
						+ allReportsData + " | -> to user:  " + connection.getInetAddress());
		System.out.println("BranchManagerPortalViewController: handleCommandFromClient: Sent all restaurants orders: "
				+ allReportsData + " | -> to user:  " + connection.getInetAddress());
	}

	/**
	 * handleEventSupplierRegistration
	 * 
	 * handles supplier registration event
	 * sends a message to client with information accordingly.
	 * @param JSONObject msg
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	private void handleEventSupplierRegistration(JSONObject msg) {
		JSONObject response = new JSONObject();
		response = db.getInactiveSupplier(msg);
		response.put("command", "update");
		response.put("update", "show supplier registration window");
		try {
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: IOException was caught in handleEventMonthlyReports");
			System.out
					.println("BranchManagerPortalViewController: IOException was caught in handleEventMonthlyReports");
		}
	}

	/**
	 * handleApproveEmployer
	 * 
	 * handles approve employer command
	 * @param JSONObject msg
	 * @author Roman Milman
	 */
	private void handleApproveEmployer(JSONObject msg) {
		JSONObject response = new JSONObject();
		response = db.activeEmployer(msg);
		try {
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: IOException was caught in handleApproveEmployer");
			System.out.println("BranchManagerPortalViewController: IOException was caught in handleApproveEmployer");
		}
	}

	/**
	 * handleEventApproveEmployer
	 * 
	 * handles approve employer event.
	 * sends a message to client with information accordingly.
	 * @param JSONObject msg
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	private void handleEventApproveEmployer(JSONObject msg) {
		JSONObject response = new JSONObject();
		response = db.getInactiveEmployers(msg);
		response.put("command", "update");
		response.put("update", "show approve employer window");
		try {
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: IOException was caught in handleEventApproveEmployer");
			System.out
					.println("BranchManagerPortalViewController: IOException was caught in handleEventApproveEmployer");
		}
	}

	/**
	 * handleEventClientRegister
	 * 
	 * handles client registration event.
	 * sends a message to client with information accordingly.
	 * @param JSONObject msg
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	private void handleEventClientRegister(JSONObject msg) {
		JSONObject response = new JSONObject();
		response = db.getInactiveCustomer(msg);
		response.put("command", "update");
		response.put("update", "show registration window");
		try {
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: IOException was caught in handleEventClientRegister");
			System.out
					.println("BranchManagerPortalViewController: IOException was caught in handleEventClientRegister");
		}
	}

	/**
	 * handleRegistration
	 * 
	 * handles registration command.
	 * @param JSONObject msg
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	private void handleRegistration(JSONObject msg) {

		switch (Message.getValue(msg, "register type")) {
		case "regular":
			handleRegistrationRegularCustomer(msg);
			break;
		case "business":
			handleRegistrationBusinessCustomer(msg);
			break;
		case "supplier":
			handleRegistrationSupplier(msg);
			break;

		default:
			Logger.log(Level.WARNING, "BranchManagerPortalViewController: unknown message in handleRegistration");
			System.out.println("BranchManagerPortalViewController: unknown message in handleRegistration");
			break;
		}
	}

	/**
	 * handleRegistrationSupplier
	 * 
	 * handles supplier registration command.
	 * @param JSONObject msg
	 * @author Roman Milman
	 */
	private void handleRegistrationSupplier(JSONObject msg) {
		JSONObject response = new JSONObject();
		response = db.registerSupplier(msg);
		response.put("command", "update");
		try {
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: IOException was caught in handleCommandFromClient");
			System.out.println("BranchManagerPortalViewController: IOException was caught in handleCommandFromClient");
		}
	}

	/**
	 * handleRegistrationBusinessCustomer
	 * 
	 * handles business customer registration command.
	 * @param JSONObject msg
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	private void handleRegistrationBusinessCustomer(JSONObject msg) {
		JSONObject response = new JSONObject();
		int employerUserID = -1;
		if ((employerUserID = db.isEmployerExists(msg)) == -1) {
			response.put("update", "could not add business user to database");
		} else {
			response = db.registerBusinessCustomer(msg, employerUserID);
		}
		try {
			response.put("command", "update");
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: IOException was caught in handleRegistration");
			System.out.println("BranchManagerPortalViewController: IOException was caught in handleRegistration");
		}
	}

	/**
	 * handleRegistrationRegularCustomer
	 * 
	 * handles regular customer registration command.
	 * @param JSONObject msg
	 * @author Roman Milman
	 */
	@SuppressWarnings("unchecked")
	private void handleRegistrationRegularCustomer(JSONObject msg) {
		JSONObject response = new JSONObject();
		response = db.registerRegularCustomer(msg);
		response.put("command", "update");
		try {
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING,
					"BranchManagerPortalViewController: IOException was caught in handleEventClientRegister");
			System.out
					.println("BranchManagerPortalViewController: IOException was caught in handleEventClientRegister");
		}
	}

	/**
	 * handleLogout
	 * 
	 * handles logout command.
	 * @param JSONObject msg
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
			Logger.log(Level.DEBUG,
					"BranchManagerPortalViewController: Client = " + connection.toString() + " logged out");
			System.out.println("BranchManagerPortalViewController: Client = " + connection.toString() + " logged out");

			connection.sendToClient(Parser.encode(response));
			com.switchPortal(connection, "login");

		} catch (IOException e) {
			Logger.log(Level.WARNING, "BranchManagerPortalViewController: IOException was caught in handleLogout");
			System.out.println("BranchManagerPortalViewController: IOException was caught in handleLogout");
		}
	}

}
