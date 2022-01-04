package controllers;

import java.io.IOException;
import java.sql.SQLException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import common.Logger;
import common.Logger.Level;
import common.Message;
import common.Parser;
import ocsf.server.ConnectionToClient;
import serverSide.DataBase;

public class CEOPortalViewController implements PortalViewController {

	private DataBase db;
	private ComController com;
	private ConnectionToClient connection;
	private String selectedItemName;
	private String ID;

	public CEOPortalViewController(DataBase db, ComController com, ConnectionToClient connection) {
		this.db = db;
		this.com = com;
		this.connection = connection;
		System.out.println("Connection address: " + connection.getInetAddress());

	}

	@Override
	public void start() {

	}

	@Override
	public void stop() {

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
		JSONObject response = new JSONObject();

		switch ((String) json.get("command")) {

		case "ready":
			handleEventReady();
			break;

		case "view quarterly income reports button is pressed":

			handleEventViewQuarterlyIncomeReports(json);

			break;
		case "monthly reports button is pressed":

			handleEventViewMonthlyReports(json);

			break;

		case "view quarterly reports button is pressed":
			handleEventViewQuarterlyReportsWindow();
			break;

		case "download quarterly reports button is pressed":
			handleEventDownloadQuarterlyReport();
			break;

		case "logout was pressed":
			handleEventLogout();
			break;

		default:
			Logger.log(Level.INFO,
					"CEOPortalViewController: handleCommandFromClient: " + Message.getValueString(json, "command"));
			System.out
					.println("CEOPortalViewController: handleCommandFromClient: " + Message.getValueString(json, "command"));
			break;

		}
	}
	/** Handle Event View Quarterly Reports Window<p>
	 * This method will return a verification to the client side to view and show the quarterly report window
	 * , the method created a JSON object containing the message to the com controller side to allow the client
	 * access to the quarterly report window.
	 * 
	 * */
	private void handleEventViewQuarterlyReportsWindow() {
		JSONObject response;
		response = new JSONObject();
		response.put("command", "update");
		response.put("update", "show view quarterly reports window");
		try {
			connection.sendToClient(Parser.encode(response));
		} catch (IOException e) {
			Logger.log(Level.WARNING, "CEOPortalViewController: handleCommandFromClient: IOException was thrown");
			System.out.println("CEOPortalViewController: handleCommandFromClient: IOException was thrown");
			return;
		}
	}
	/** Handle Event Download Quarterly Report<p>
	 * This method handles a request from the client to obtain all the PDF file reports from the database.<br>
	 * The method will build a JSON object containing keys such as "command" and also a call for a DB method to summon
	 * a query that returns all the existing files regarding the restaurants income, performance and items that
	 * are stored in different tables on the database.
	 * 
	 * */
	private void handleEventDownloadQuarterlyReport() {
		JSONObject response;
		response = new JSONObject();
		response.put("command", "update");
		response.put("update", "download quarterly reports window");
		try {
			JSONArray filesArray = db.getFilesFromDataBase();
			response.put("filesArray", filesArray);
			connection.sendToClient(Parser.encode(response));

		} catch (SQLException e1) {
			Logger.log(Level.WARNING, "CEOPortalViewController: handleCommandFromClient: SQLException was thrown");
			System.out.println("CEOPortalViewController: handleCommandFromClient: SQLException was thrown");
			return;
		} catch (IOException e) {
			Logger.log(Level.WARNING, "CEOPortalViewController: handleCommandFromClient: IOException was thrown");
			System.out.println("CEOPortalViewController: handleCommandFromClient: IOException was thrown");
			return;
		}
	}

	/** Handle Event Logout<p>
	 * This method will logout the user by switiching its com controller portal view back to login screen
	 * and send a message back to the user to switch its com controller on his side to login screen as well.
	 * 
	 * The response is being send by a created JSON object, after the portal is being switched on the server side.
	 * 
	 * */
	private void handleEventLogout() {
		JSONObject response;
		response = new JSONObject();
		response.put("command", "handshake");
		response.put("portalType", "login");
		response.put("status", "ok");
		try {
			connection.sendToClient(Parser.encode(response));
			com.switchPortal(connection, "login");

			Logger.log(Level.DEBUG,
					"CEOPortalViewController: handleCommandFromClient: " + connection.toString() + " logged out");
			System.out.println(
					"CEOPortalViewController: handleCommandFromClient: " + connection.toString() + " logged out");

		} catch (IOException e) {
			Logger.log(Level.DEBUG, "CEOPortalViewController: handleCommandFromClient: IOException was thrown");
			System.out.println("CEOPortalViewController: handleCommandFromClient: IOException was thrown");

		}
	}
	/** Handle Event View Monthly Reports<p>
	 * This method handles a request from the client to obtain all the monthly reports from the database.<br>
	 * The method will build a JSON object containing keys such as "command" and also a call for a DB method to summon
	 * a query that returns all the existing reports regarding the restaurants income, performance and items that
	 * are stored in different tables on the database.
	 * 
	 * @param json - this json states a request from the client to view the income quarterly reports.
	 * 
	 * */
	private void handleEventViewMonthlyReports(JSONObject json) {
		Logger.log(Level.WARNING, "Message received from user: " + Message.getValueString(json, "message"));
		System.out.println("Message received from user: " + Message.getValueString(json, "message"));

		JSONObject allReportsData = new JSONObject();
		allReportsData.put("command", "update");
		allReportsData.put("update", "all restaurants reports");

		JSONArray incomeReportData = db.getExistingIncomeReportInformation(); // returns all data regarding income
		JSONArray itemsReportData = db.getExistingItemsPerRestaurantReportInformation(); // returns all data regarding items of restaurants
		JSONArray performanceReportData = db.getExistingPerformanceReportInformation(); // returns all data regarding restaurants performance

		// inserting all the data by specific keys
		allReportsData.put("income reports data", incomeReportData);
		allReportsData.put("items reports data", itemsReportData);
		allReportsData.put("performance reports data", performanceReportData);

		try {
			connection.sendToClient(Parser.encode(allReportsData)); // send data to client
		} catch (IOException e) {
			Logger.log(Level.WARNING, "CEOPortalViewController: handleCommandFromClient: IOException was thrown");
			System.out.println("CEOPortalViewController: handleCommandFromClient: IOException was thrown");
			return;
		}

		Logger.log(Level.DEBUG, "CEOPortalViewController: handleCommandFromClient: Sent all restaurants orders: "
				+ allReportsData + " | -> to user:  " + connection.getInetAddress());
		System.out.println("CEOPortalViewController: handleCommandFromClient: Sent all restaurants orders: "
				+ allReportsData + " | -> to user:  " + connection.getInetAddress());
	}

	/** Handle Event View Quarterly Income Reports<p>
	 * This method handles a request from the client to obtain all the quarterly report from the database.<br>
	 * The method will build a JSON object containing keys such as "command" and also a call for a DB method to summon<br>
	 * a query that returns all the existing reports regarding the restaurants income.
	 * 
	 * @param json - this json states a request from the client to view the income quarterly reports.
	 * 
	 * */
	private void handleEventViewQuarterlyIncomeReports(JSONObject json) {
		Logger.log(Level.WARNING, "Message received from user: " + Message.getValueString(json, "message"));
		System.out.println("Message received from user: " + Message.getValueString(json, "message"));

		JSONObject incomeReportsData = new JSONObject(); // holds all related quarterly income report info
		incomeReportsData.put("command", "update");
		incomeReportsData.put("update", "quarterly income restaurants reports");
		JSONArray incomeReportTable = db.getExistingIncomeReportInformationQuarterly(); // query to return income report data
		incomeReportsData.put("income reports data", incomeReportTable);

		try {
			connection.sendToClient(Parser.encode(incomeReportsData)); // send to client the data
		} catch (IOException e) {
			Logger.log(Level.WARNING, "CEOPortalViewController: handleCommandFromClient: IOException was thrown");
			System.out.println("CEOPortalViewController: handleCommandFromClient: IOException was thrown");
			return;
		}

		Logger.log(Level.DEBUG, "CEOPortalViewController: handleCommandFromClient: Sent all restaurants orders: "
				+ incomeReportsData + " | -> to user:  " + connection.getInetAddress());
		System.out.println("CEOPortalViewController: handleCommandFromClient: Sent all restaurants orders: "
				+ incomeReportsData + " | -> to user:  " + connection.getInetAddress());
	}

	/** Handle Event Ready<p>
	 * 
	 * This method will switch the current communication controller of the client to the controller of the ceo.
	 * the event will switch the controllers and alert the client regarding the switch and that it is ready for use.
	 * */
	private void handleEventReady() {
		com.switchPortal(connection, "CEO");
		Logger.log(Level.INFO, "CEOPortalViewController: handleCommandFromClient: ready case: CEO portal is ready");
		System.out.println("CEOPortalViewController: handleCommandFromClient: ready case: CEO portal is ready");
	}

}
