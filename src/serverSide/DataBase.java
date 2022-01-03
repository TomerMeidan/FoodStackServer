package serverSide;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import java.util.Set;
import java.util.TreeMap;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

import common.Logger;
import common.Logger.Level;
import util.DateParser;
import util.Meal;
import util.Menu;
import util.Message;
import util.MustFeature;
import util.OptionalFeature;
import util.Order;

/**
 * DataBase
 * 
 * This class is the Database controller. This class holds a Connection to MySQL
 * database server. This class has password and user variables to connect.
 * 
 * @author Roman Milman
 */
@SuppressWarnings("unchecked")
public class DataBase {

	private static Connection conn;
	private static Connection connImport;
	private String password;
	private String user;

	/**
	 * start
	 * 
	 * This method starts the connection to MySQL database server.
	 * 
	 * @author Roman Milman
	 */
	public void start() throws SQLException {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();

			// log
			Logger.log(Level.DEBUG, "DataBase : Driver definition succeed");
			System.out.println("DataBase : Driver definition succeed");
		} catch (Exception ex) {
			// log
			Logger.log(Level.WARNING, "DataBase : Driver definition failed");
			System.out.println("DataBase : Driver definition failed");
		}

		System.out.println("DataBase: DB user name: " + user);
		Logger.log(Level.DEBUG, "DataBase: DB user name: " + user);
		System.out.println("DataBase: DB password: " + password);
		Logger.log(Level.DEBUG, "DataBase: DB password: " + password);

		connImport = DriverManager.getConnection("jdbc:mysql://localhost/import_users?serverTimezone=IST", user,
				password);

		conn = DriverManager.getConnection("jdbc:mysql://localhost/bitemedb?serverTimezone=IST", user, password);

		// log
		Logger.log(Level.DEBUG, "DataBase : SQL connection succeed");
		System.out.println("DataBase : SQL connection succeed");
		importUsers();
	}

	public void importUsers() {
		new DBImport(conn, connImport).importAll();
	}

	/**
	 * validateUser
	 * 
	 * This method validates user information. This method checks if username and
	 * password given as input are correct.
	 * 
	 * @param JSONObject json - includes 'username' and 'password' keys as users
	 *                   values accordingly.
	 * @return JSONObject
	 * @author Roman Milman
	 */
	public JSONObject validateUser(JSONObject json) {
		ResultSet rs;
		String username = Message.getValueString(json, "username");
		JSONObject response = new JSONObject();
		String userID = "", status = "";
		//
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM USERS WHERE Username = ?");

			stmt.setString(1, username);

			rs = stmt.executeQuery();
			if (rs.next()) {
				if (json.get("password").equals(rs.getString("Password"))) {

					// log
					Logger.log(Level.DEBUG, "DATABASE: user (" + username + ") is approved");
					System.out.println("DATABASE: user (" + username + ") is approved");

					response.put("command", "handshake");
					response.put("portalType", rs.getString("Role"));
					response.put("userID", rs.getString("UserID"));
					userID = rs.getString("UserID");

					switch (rs.getString("Role")) {

					case "Customer":
						status = getStatusByRole(userID, "customers");
						break;
					case "Business Customer":
						status = getStatusByRole(userID, "customers");
						break;
					case "Supplier":
						status = getStatusByRole(userID, "suppliers");
						break;
					case "HR":
						status = getStatusByRole(userID, "hr");
						break;
					default:
						status = "";
						break;

					}

					response.put("branch", rs.getString("Branch"));
					response.put("FirstName", rs.getString("FirstName"));
					response.put("LastName", rs.getString("LastName"));
					response.put("userID", userID);

					if (status == null)
						status = "null";

					if (!status.equals("") && !status.equals("active")) {

						response.put("status", "notOk");
						response.put("notOk", status);

					} else
						response.put("status", "ok");
					return response;
				}
				// log
				Logger.log(Level.DEBUG, "DATABASE: user (" + username + ") is NOT approved");
				System.out.println("DATABASE: user (" + username + ") is NOT approved");

				response.put("command", "handshake");
				response.put("status", "notOk");
				response.put("notOk", "");
				return response;
			}
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}
		// log
		Logger.log(Level.DEBUG, "DATABASE: user (" + username + ") is NOT approved");
		System.out.println("DATABASE: user (" + username + ") is NOT approved");

		response.put("command", "handshake");
		response.put("status", "notOk");
		response.put("notOk", "");
		return response;
	}

	public String getStatusByRole(String userID, String role) {
		ResultSet rs;

		try {
			PreparedStatement stmt = conn.prepareStatement("select status\n" + "from users INNER JOIN " + role
					+ " on users.UserID = " + role + ".UserID" + "\n" + "WHERE users.UserID = ?");
			stmt.setInt(1, Integer.parseInt(userID));

			rs = stmt.executeQuery();
			if (rs.next()) {
				// log
				Logger.log(Level.DEBUG, "DATABASE: getStatusByRole: ID:(" + userID + ") have been found");
				System.out.println("DATABASE: getStatusByRole: ID:(" + userID + ") have been found");

				return rs.getString("Status");

			}
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: getStatusByRole: SQLException was thrown");
			System.out.println("DATABASE: getStatusByRole: SQLException was thrown");
			return null;

		}
		// log
		Logger.log(Level.DEBUG, "DATABASE: getStatusByRole: ID:(" + userID + ") have NOT been found");
		System.out.println("DATABASE: getStatusByRole: ID:(" + userID + ") have NOT been found");
		return null;
	}

	/**
	 * access Database (MySQL) and get RefundBalance for a specific customer in a
	 * selected restaurant
	 * 
	 * @author mosa
	 * @param json with keys:<br>
	 *             "supplierID", "userID", both with value String
	 * @return JSONObject with keys:<br>
	 *         "command", value "update"<br>
	 *         "update", value "Show payment methods"
	 *         <p>
	 *         IF RefundBalance found<br>
	 *         "refundBalance", value String
	 */
	public JSONObject getRefundBalance(JSONObject json) {
		JSONObject response = new JSONObject();
		try {
			PreparedStatement preStmt = conn.prepareStatement(
					"SELECT RefundBalance FROM bitemedb.refund WHERE SupplierID = ? AND CustomerID = ?");
			preStmt.setString(1, Message.getValueString(json, "supplierID"));
			preStmt.setString(2, Message.getValueString(json, "userID"));
			ResultSet rs = preStmt.executeQuery();
			if (rs.next()) {
				response.put("refundBalance", rs.getString("RefundBalance"));
			} else {
				response.put("refundBalance", "0");
				preStmt = conn
						.prepareStatement("INSERT INTO refund (SupplierID,CustomerID,RefundBalance) VALUES (?,?,?)");
				preStmt.setString(1, Message.getValueString(json, "supplierID"));
				preStmt.setString(2, Message.getValueString(json, "userID"));
				preStmt.setString(3, "0");
				preStmt.executeUpdate();
			}

			response.put("command", "update");
			response.put("update", "Show payment methods");
		} catch (SQLException e) {
			System.out.println(e);
			Logger.log(Level.WARNING, "DATABASE: SQLException in getRefundBalance");
			System.out.println("DATABASE: SQLException in getRefundBalance");
		}
		return response;
	}

	/**
	 * Get all the active restaurants
	 * 
	 * @author mosa
	 * @return A JSONObject with keys:<br>
	 *         "command", value "update"<br>
	 *         "update", value "Show restaurant list"<br>
	 *         "restaurantList", value JSONArray containing restaurants
	 *         <p>
	 *         for every JSONObject in the JSONArray, keys:<br>
	 *         "branch", "restaurantName", "phoneNumber", "deliveryTypes",
	 *         "supplierID"
	 */
	public JSONObject getRestaurants() {
		ResultSet rs;
		JSONObject response = new JSONObject();
		JSONArray restaurantList = new JSONArray();

		Statement stmt;
		response.put("command", "update");
		response.put("update", "Show restaurant list");

		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT * FROM suppliers " + "WHERE Status = 'ACTIVE'");
			while (rs.next()) {
				JSONObject row = new JSONObject();
				row.put("branch", rs.getString("Branch"));
				row.put("restaurantName", rs.getString("Name"));
				row.put("phoneNumber", rs.getString("PhoneNumber"));
				row.put("deliveryTypes", rs.getString("deliveryTypes"));
				row.put("supplierID", rs.getString("UserID"));
				restaurantList.add(row);
			}
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in getRestaurants");
			System.out.println("DATABASE: SQLException in getRestaurants");
		}
		response.put("restaurantList", restaurantList);
		return response;
	}

	/**
	 * Get from the database the menu of the selected restauraunt
	 * 
	 * @author mosa
	 * @param restaurantInfo containing key:<br>
	 *                       "restaurantName", value String
	 * @return A JSONObject containing keys:<br>
	 *         "command", value "update"<br>
	 *         "update", value "Show item types list"<br>
	 *         "menu", value contains menu of the restaurant
	 */
	public JSONObject getRestaurantMenu(JSONObject restaurantInfo) {
		JSONObject ret = new JSONObject();
		try {
			Statement stmt = conn.createStatement();
			;
			Menu menu = new Menu(stmt);
			String selectedRestaurant = Message.getValueString(restaurantInfo, "restaurantName");
			if (selectedRestaurant == null) {
				System.out.println("Database: getRestaurantMenu: restaurantName is null");
			}
			menu.buildMenu(selectedRestaurant);
			ret = menu.getMenu();
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "Database: getRestaurantMenu: SQLException");
			System.out.println("Database: getRestaurantMenu: SQLException");
		}
		ret.put("command", "update");
		ret.put("update", "Show item types list");
		return ret;
	}

	/**
	 * This method adds an order to the database using the help of a few private
	 * methods:<br>
	 * addMealsPerOrder(), , updateRefundBalance(), handleBusCustomerBalance()
	 * <p>
	 * Make sure to include the correct keys: "totalPrice", "userID", "userID",
	 * "paymentType", "orderTime", "dueDate", "restaurantName", "pickUpType"
	 * "earlyBooking", "address", "phoneNumber", "supplierID", "leftToPay",
	 * "refundBalance".<br>
	 * All values String, except for totalPrice, which is Long
	 * 
	 * @author mosa
	 * @param order
	 * @return a JSONObject with keys:<br>
	 *         "command" with value "update", "update" with value:<br>
	 *         "Order was successfuly added" if success<br>
	 *         "Order cancelled, not enough funds" if customer doesn't have enough
	 *         balance<br>
	 *         .
	 */
	public JSONObject addOrder(JSONObject order) {
		PreparedStatement preStmt;
		JSONObject response = new JSONObject();
		boolean addFailFlag = true; // flag to check if adding to database fails
		response.put("command", "update");
		try {
			conn.setAutoCommit(false);
			Integer totalPrice = Message.getValueLong(order, "totalPrice").intValue();
			String userID = Message.getValueString(order, "userID");
			String paymentType = Message.getValueString(order, "paymentType");
			preStmt = conn.prepareStatement(
					"INSERT INTO orders (UserID,OrderDate,DueDate, RestaurantName, PaymentType, Total, PickUpType, EarlyBooking,Address,PhoneNumber,Status,SupplierID) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
					Statement.RETURN_GENERATED_KEYS);
			preStmt.setInt(1, Integer.valueOf(userID)); // UserID
			preStmt.setString(2, Message.getValueString(order, "orderTime")); // OrderDate
			preStmt.setString(3, Message.getValueString(order, "dueDate")); // dueDate
			preStmt.setString(4, Message.getValueString(order, "restaurantName"));
			preStmt.setString(5, paymentType);
			preStmt.setInt(6, totalPrice);
			preStmt.setString(7, Message.getValueString(order, "pickUpType"));// order type
			preStmt.setString(8, Message.getValueString(order, "earlyBooking"));
			preStmt.setString(9, Message.getValueString(order, "address"));// address
			preStmt.setString(10, Message.getValueString(order, "phoneNumber"));// phone
			preStmt.setString(11, "Waiting for approval");// status
			preStmt.setString(12, Message.getValueString(order, "supplierID"));//
			preStmt.executeUpdate();
			ResultSet keys = preStmt.getGeneratedKeys();
			keys.next();

			order.put("orderID", keys.getInt(1));
			if (addMealsPerOrder(order))
				addFailFlag = false;
			updateRefundBalance(order);
			if (paymentType.equals("Business")) {
				if (handleBusCustomerBalance(order)) {
					addFailFlag = false;
				} else {
					addFailFlag = true;
				}
			}
			if (addFailFlag) {
				conn.rollback();
				response.put("update", "Order cancelled, not enough funds");
			} else {
				conn.commit();
				response.put("update", "Order was successfuly added");
			}
			conn.setAutoCommit(true);
		} catch (SQLException e) {
			System.out.println(e);
			System.out.println("DATABASE: SQLException in addOrder");
			Logger.log(Level.WARNING, "DATABASE: SQLException in addOrder");
		}
		return response;
	}

	/**
	 * Adds to mealsperorder table in MySQL
	 * 
	 * @param json containing keys:<br>
	 *             "orderID", with value Integer<br>
	 *             "mealsJArray, with value JSONArray<br>
	 * 
	 */
	private boolean addMealsPerOrder(JSONObject json) {
		Integer orderID = (Integer) json.get("orderID");
		JSONArray mealsJArray = Message.getValueJArray(json, "mealsJArray");
		if (mealsJArray == null)
			return false;
		try {
			PreparedStatement stmt = conn.prepareStatement(
					"INSERT INTO mealsperorder (OrderID, ItemID, MustFeatureID) VALUES (?,?,?)",
					Statement.RETURN_GENERATED_KEYS);
			for (int i = 0; i < mealsJArray.size(); i++) {
				JSONObject meal = (JSONObject) mealsJArray.get(i);
				JSONArray optionalList = (JSONArray) meal.get("optionalJArray");
				stmt.setInt(1, Integer.valueOf(orderID));
				stmt.setInt(2, Integer.valueOf(Message.getValueString(meal, "mealID")));
				if (Message.getValueString(meal, "mustFeatureID") == null) // if mustFeature is not available, set to 0
					stmt.setInt(3, 0);
				else
					stmt.setInt(3, Integer.valueOf(Message.getValueString(meal, "mustFeatureID")));
				stmt.executeUpdate();
				ResultSet keys = stmt.getGeneratedKeys();
				keys.next();
				int mealsPerOrderID = keys.getInt(1);
				if (!addOptionalFeaturePerItem(optionalList, mealsPerOrderID))
					return false;
			}

		} catch (SQLException e) {
			System.out.println(e);
			System.out.println("DATABASE: SQLException in addMealsPerOrder");
			Logger.log(Level.WARNING, "DATABASE: SQLException in addMealsPerOrder");
			return false;
		}
		return true;
	}

	/**
	 * Adds to optionalfeaturesperitem table in MySQL
	 * <p>
	 * 
	 * @param optionalList
	 * @param mealsPerOrderID
	 */
	private boolean addOptionalFeaturePerItem(JSONArray optionalList, int mealsPerOrderID) {
		if (optionalList == null)
			return false;
		try {
			PreparedStatement stmt = conn.prepareStatement(
					"INSERT INTO optionalfeaturesperitem (MealsPerOrderID, OptionalFeatureID) VALUES (?,?)");
			for (int i = 0; i < optionalList.size(); i++) {
				JSONObject optional = (JSONObject) optionalList.get(i);
				stmt.setInt(1, mealsPerOrderID);
				stmt.setInt(2, Integer.valueOf(Message.getValueString(optional, "optionalFeatureID")));
				stmt.executeUpdate();
			}
		} catch (SQLException e) {
			System.out.println(e);
			System.out.println("DATABASE: SQLException in addMealsPerOrder");
			Logger.log(Level.WARNING, "DATABASE: SQLException in addMealsPerOrder");
			return false;
		}
		return true;
	}

	/**
	 * if a customer is Business customer, he has a Balance (daily cap).
	 * <p>
	 * This method updates the Balance of the customer in the database
	 * <p>
	 * Make sure to include keys:<br>
	 * "userID", with value String<br>
	 * "leftToPay", with value Long<br>
	 * 
	 * @author mosa
	 * @param order
	 * @return false if customer doesn't have enough in his balance else true
	 */
	private boolean handleBusCustomerBalance(JSONObject order) {
		String userID = Message.getValueString(order, "userID");
		int leftToPay = Message.getValueLong(order, "leftToPay").intValue();
		int newBalance = leftToPay;
		int balance;
		try {
			PreparedStatement preStmt = conn.prepareStatement("SELECT Balance FROM customers WHERE UserID = ?");
			preStmt.setString(1, userID);
			ResultSet rs = preStmt.executeQuery();
			if (rs.next()) {
				balance = rs.getInt("Balance");
			} else
				balance = 0; // ??
			newBalance = balance - leftToPay;
			if (newBalance < 0)
				return false;
			preStmt = conn.prepareStatement("UPDATE customers SET Balance =? WHERE UserID = ?");
			preStmt.setInt(1, newBalance);
			preStmt.setString(2, userID);
			preStmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e);
			System.out.println("DATABASE: SQLException in handleBusCustomerBalance");
			Logger.log(Level.WARNING, "DATABASE: SQLException in handleBusCustomerBalance");
		}
		return true;
	}

	/**
	 * This method updates the refund balance of a business customer<br>
	 * Condition: key "refundBalanceUsed" has value boolean true
	 * 
	 * @param order make sure to include keys: "refundBalance", with value Long<br>
	 *              "refundBalanceUsed", with value boolean<br>
	 *              "userID", with value String<br>
	 *              "supplierID", with value String
	 */
	private void updateRefundBalance(JSONObject order) {
		if (!((boolean) order.get("refundBalanceUsed")))
			return;
		int refundBalance = Message.getValueLong(order, "refundBalance").intValue();
		try {
			PreparedStatement preStmt = conn
					.prepareStatement("UPDATE refund SET RefundBalance =? WHERE CustomerID = ? AND SupplierID =?");
			preStmt.setInt(1, refundBalance);
			preStmt.setString(2, Message.getValueString(order, "userID"));
			preStmt.setString(3, Message.getValueString(order, "supplierID"));
			preStmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e);
			System.out.println("DATABASE: SQLException in updateRefundBalance");
			Logger.log(Level.WARNING, "DATABASE: SQLException in updateRefundBalance");
		}
	}

	public void clearTables() {
		PreparedStatement stmt;
		try {
			stmt = conn.prepareStatement("TRUNCATE `bitemedb`.`orders`;");
			stmt.executeUpdate();
			stmt = conn.prepareStatement("TRUNCATE `bitemedb`.`mealsperorder`;");
			stmt.executeUpdate();
			stmt = conn.prepareStatement("TRUNCATE `bitemedb`.`optionalfeaturesperitem`;");
			stmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * registerRegularCustomer
	 * 
	 * This method updates customer in database as 'active' and sets w4c and id
	 * columns. This method sets user as 'Customer'. This method sets 'username' and
	 * 'password' as given in input. This method updates both Tables, if any fails
	 * rollback's.
	 * 
	 * @param JSONObject json - includes: 'w4c','id','username','password' keys for
	 *                   customers accordingly values.
	 * @return JSONObject - "update" : "regular customer has been registered" if
	 *         succeed, otherwise "could not register regular customer"
	 * @author Roman Milman
	 */
	public JSONObject registerRegularCustomer(JSONObject json) {
		String w4c = Message.getValueString(json, "w4c");
		String id = Message.getValueString(json, "id");
		JSONObject response = new JSONObject();
		response.put("w4c", w4c);

		try {
			conn.setAutoCommit(false);

			PreparedStatement stmt = conn
					.prepareStatement("update bitemedb.customers set status = 'active', w4c = ? where id = ?");
			stmt.setString(1, w4c);
			stmt.setString(2, id);

			stmt.executeUpdate();

			stmt = conn.prepareStatement(
					"update bitemedb.users set username = ?, password = ?, role = 'Customer' where users.userid = (select userid from bitemedb.customers where id = ?)");
			stmt.setString(1, Message.getValueString(json, "username"));
			stmt.setString(2, Message.getValueString(json, "password"));
			stmt.setString(3, id);

			stmt.executeUpdate();

		} catch (SQLException e) {
			// log
			Logger.log(Level.WARNING, "DATABASE: SQLException in registerRegularCustomer");
			System.out.println("DATABASE: SQLException in registerRegularCustomer");

			try {
				conn.rollback();
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in registerRegularCustomer, rollback");
				System.out.println("DATABASE: SQLException in registerRegularCustomer, rollback");
			}
			response.put("update", "could not register regular customer");

			try {
				conn.setAutoCommit(true);
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in registerRegularCustomer, setAutoCommit");
				System.out.println("DATABASE: SQLException in registerRegularCustomer, setAutoCommit");
			}

			return response;
		}

		try {
			conn.commit();
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in registerRegularCustomer, commit");
			System.out.println("DATABASE: SQLException in registerRegularCustomer, commit");

			response.put("update", "could not register regular customer");

			try {
				conn.setAutoCommit(true);
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in registerRegularCustomer, setAutoCommit");
				System.out.println("DATABASE: SQLException in registerRegularCustomer, setAutoCommit");
			}

			return response;
		}
		response.put("update", "regular customer has been registered");

		try {
			conn.setAutoCommit(true);
		} catch (SQLException e1) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in registerRegularCustomer, setAutoCommit");
			System.out.println("DATABASE: SQLException in registerRegularCustomer, setAutoCommit");
		}

		return response;
	}

	/**
	 * isEmployerExists
	 * 
	 * This method checks if employer exists in DB. This method returns employersId
	 * as defined in DB, otherwise returns -1.
	 * 
	 * @param JSONObject json - includes: 'employer name'.
	 * @return int
	 * @author Roman Milman
	 */
	public int isEmployerExists(JSONObject json) {
		ResultSet rs;
		String employer = Message.getValueString(json, "employer name");
		//
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM EMPLOYERS WHERE name = ?");
			stmt.setString(1, employer);

			rs = stmt.executeQuery();
			if (rs.next()) {
				if ("active".equals(rs.getString("status"))) {

					// log
					Logger.log(Level.DEBUG, "DATABASE: employer (" + employer + ") is active");
					System.out.println("DATABASE: employer (" + employer + ") is active");

					return rs.getInt("employerID");
				}
				// log
				Logger.log(Level.DEBUG, "DATABASE: employer (" + employer + ") is freeze/inacrive/null");
				System.out.println("DATABASE: employer (" + employer + ") is freeze/inacrive/null");

				return -1;
			}
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in isEmployerExists");
		}
		// log
		Logger.log(Level.DEBUG, "DATABASE: employer (" + employer + ") was NOT found");
		System.out.println("DATABASE: employer (" + employer + ") was NOT found");

		return -1;
	}

	/**
	 * registerBusinessCustomer
	 * 
	 * This method updates customer in database as 'inactive' and sets w4c, id,
	 * employerID and balance columns. This method sets user as 'Business Customer'.
	 * This method sets 'username' and 'password' as given in input. This method
	 * updates both Tables, if any fails rollback's.
	 * 
	 * @param JSONObject json - includes: 'w4c','id','username','password' keys for
	 *                   customers accordingly values.
	 * @param int        employerID
	 * @return JSONObject - "update" : "business customer has been registered" if
	 *         succeed, otherwise "could not add business user to database"
	 * @author Roman Milman
	 */
	public JSONObject registerBusinessCustomer(JSONObject json, int employerID) {
		String w4c = Message.getValueString(json, "w4c");
		JSONObject response = new JSONObject();
		response.put("w4c", w4c);

		try {
			conn.setAutoCommit(false);

			PreparedStatement stmt = conn.prepareStatement(
					"update bitemedb.customers set status = 'inactive', employerID = ?, w4c = ?, balance = (select balance from bitemedb.employers where employerid = ?) where id = ?");
			stmt.setInt(1, employerID);
			stmt.setString(2, w4c);
			stmt.setString(3, String.valueOf(employerID));
			stmt.setString(4, Message.getValueString(json, "id"));

			stmt.executeUpdate();

			stmt = conn.prepareStatement(
					"update bitemedb.users set username = ?, password = ?, role = 'Business Customer' where users.userid = (select userid from bitemedb.customers where id = ?)");
			stmt.setString(1, Message.getValueString(json, "username"));
			stmt.setString(2, Message.getValueString(json, "password"));
			stmt.setString(3, Message.getValueString(json, "id"));

			stmt.executeUpdate();

		} catch (SQLException e) {
			// log
			Logger.log(Level.WARNING, "DATABASE: SQLException in registerBusinessCustomer");
			System.out.println("DATABASE: SQLException in registerBusinessCustomer");

			try {
				conn.rollback();
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in registerBusinessCustomer, rollback");
				System.out.println("DATABASE: SQLException in registerBusinessCustomer, rollback");
			}
			response.put("update", "could not add business user to database");

			try {
				conn.setAutoCommit(true);
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in registerBusinessCustomer, setAutoCommit");
				System.out.println("DATABASE: SQLException in registerBusinessCustomer, setAutoCommit");
			}

			return response;
		}

		try {
			conn.commit();
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in registerBusinessCustomer, commit");
			System.out.println("DATABASE: SQLException in registerBusinessCustomer, commit");

			response.put("update", "could not add business user to database");

			try {
				conn.setAutoCommit(true);
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in registerBusinessCustomer, setAutoCommit");
				System.out.println("DATABASE: SQLException in registerBusinessCustomer, setAutoCommit");
			}

			return response;
		}
		response.put("update", "business customer has been registered");

		try {
			conn.setAutoCommit(true);
		} catch (SQLException e1) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in registerBusinessCustomer, setAutoCommit");
			System.out.println("DATABASE: SQLException in registerBusinessCustomer, setAutoCommit");
		}

		return response;
	}

	/**
	 * getInactiveEmployers
	 * 
	 * This method builds JSONArray with employers that defined with status
	 * 'inactive' in DB, in the given branch as input.
	 * 
	 * @param JSONObject json - includes: 'branch'.
	 * @return JSONObject - "employers" : JSONArray with employers
	 * @author Roman Milman
	 */
	public JSONObject getInactiveEmployers(JSONObject json) {
		ResultSet rs;
		String branch = Message.getValueString(json, "branch");
		JSONObject response = new JSONObject();
		JSONArray employers = new JSONArray();

		try {
			PreparedStatement stmt = conn
					.prepareStatement("SELECT * FROM EMPLOYERS WHERE branch = ? AND status = 'inactive'");
			stmt.setString(1, branch);

			rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject employerInfo = new JSONObject();
				String employerName = rs.getString("name");

				// log
				Logger.log(Level.DEBUG, "DATABASE: employer (" + employerName + ") found as status-inactive");
				System.out.println("DATABASE: employer (" + employerName + ") found as status-inactive");

				employerInfo.put("name", rs.getString("name"));
				employerInfo.put("credit", rs.getString("credit"));
				employerInfo.put("number", rs.getString("phonenumber"));
				employerInfo.put("email", rs.getString("email"));

				employers.add(employerInfo);
			}
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in getFreezeEmployers");
		}
		response.put("employers", employers);
		return response;
	}

	/**
	 * getEmployerForHr
	 * 
	 * This method builds JSONArray of employers that this is their HR.
	 * 
	 * @param String userID.
	 * @return JSONObject - "employers" : JSONArray of employers info.
	 * @author Roman Milman
	 */
	public JSONObject getEmployerForHr(String userID) {
		ResultSet rs;
		JSONObject response = new JSONObject();
		JSONArray employers = new JSONArray();

		try {
			PreparedStatement stmt = conn.prepareStatement(
					"select * from bitemedb.employers where employerID = (select employerID from bitemedb.hr where userid = ?) and status is null");
			stmt.setString(1, userID);

			rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject employerInfo = new JSONObject();
				String employerName = rs.getString("name");

				// log
				Logger.log(Level.DEBUG, "DATABASE: employer (" + employerName + ") found as unregistered");
				System.out.println("DATABASE: employer (" + employerName + ") found as unregistered");

				employerInfo.put("employer name", rs.getString("name"));
				employerInfo.put("credit", rs.getString("credit"));
				employerInfo.put("phone number", rs.getString("phonenumber"));
				employerInfo.put("email", rs.getString("email"));

				employers.add(employerInfo);
			}
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in getEmployerForHr");
		}
		response.put("employers", employers);
		return response;
	}

	/**
	 * activeEmployer
	 * 
	 * This method sets employers status to 'active' by given name as input.
	 * 
	 * @param JSONObject json - includes: 'name'.
	 * @return JSONObject - "update" : "employer has been activated" if succeeded,
	 *         otherwise null.
	 * @author Roman Milman
	 */
	public JSONObject activeEmployer(JSONObject json) {
		String name = Message.getValueString(json, "name");
		JSONObject response = new JSONObject();

		try {
			PreparedStatement stmt = conn.prepareStatement("update employers set status='active' where name=?");
			stmt.setString(1, name);

			stmt.executeUpdate();
			// log
			Logger.log(Level.DEBUG, "DATABASE: employer (" + name + ") status changed to Active");
			System.out.println("DATABASE: employer (" + name + ") status changed to Active");

			response.put("command", "update");
			response.put("update", "employer has been activated");
			response.put("employer", name);

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in activeEmployer");
		}

		return response;
	}

	/**
	 * <b>Get All Restaurants Information</b>
	 * <p>
	 * Obtaining from MySql database all the information regarding the orders from
	 * all restaurants. All the information obtained is related to the reports. The
	 * retuned data is from the tables on DB itemsperrestaurant_report,
	 * income_report and performance_report
	 * 
	 * @author Tomer Meidan
	 * @return returns a JSONObject object, holding all the orders from all the
	 *         restaurants in the orders table.
	 */
	public JSONObject getAllRestaurantsInformation() {

		ResultSet rs;
		JSONObject response = new JSONObject();
		JSONArray listOfOrders = new JSONArray();
		Statement stmt;

		response.put("command", "update");
		response.put("update", "all restaurants information for reports");

		// Building the JSON for all the orders in the database
		try {
			stmt = conn.createStatement();

			rs = stmt.executeQuery(
					"SELECT orders.OrderID, orders.UserID, orders.OrderDate,orders.ApproveDate, orders.DueDate, orders.DeliverDate, orders.RestaurantName,\n"
							+ "orders.PaymentType, orders.Total, orders.PickUpType, orders.EarlyBooking, orders.Address, orders.PhoneNumber, suppliers.Branch\n"
							+ "FROM bitemedb.orders INNER JOIN suppliers ON suppliers.Name = orders.RestaurantName\n"
							+ "ORDER BY orders.RestaurantName, orders.OrderDate");

			while (rs.next()) {
				JSONObject orderRow = new JSONObject();
				orderRow.put("orderID", rs.getInt("OrderID"));
				orderRow.put("userID", rs.getInt("UserID"));
				orderRow.put("orderDate", rs.getString("OrderDate"));
				orderRow.put("approveDate", rs.getString("ApproveDate"));
				orderRow.put("dueDate", rs.getString("DueDate"));
				orderRow.put("deliverDate", rs.getString("DeliverDate"));
				orderRow.put("restaurantName", rs.getString("RestaurantName"));
				orderRow.put("paymentType", rs.getString("PaymentType"));
				orderRow.put("total", rs.getInt("Total"));
				orderRow.put("pickUpType", rs.getString("PickUpType"));
				orderRow.put("earlyBooking", rs.getString("EarlyBooking"));
				orderRow.put("address", rs.getString("Address"));
				orderRow.put("phoneNumber", rs.getString("PhoneNumber"));
				orderRow.put("branch", rs.getString("Branch"));

				listOfOrders.add(orderRow);

			}
			response.put("restaurantsOrdersList", listOfOrders);

		} catch (SQLException e) {
			System.out.println("DATABASE: getAllRestaurantsOrders: SQL Exception in orders table");
			Logger.log(Level.WARNING, "DATABASE: getAllRestaurantsOrders: SQL Exception in orders table");
		}

		// Building the JSON for all the meal types in the database
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(
					"SELECT items.ItemID, items.ItemType,items.ItemName, items.ItemPrice, items.UserID, mealsperorder.OrderID, orders.RestaurantName, orders.OrderDate, suppliers.Branch\n"
							+ "FROM bitemedb.items INNER JOIN mealsperorder ON mealsperorder.ItemID = items.ItemID\n"
							+ "INNER JOIN orders ON mealsperorder.OrderID = orders.OrderID\n"
							+ "INNER JOIN suppliers ON suppliers.Name = orders.RestaurantName\n"
							+ "ORDER BY orders.RestaurantName, items.ItemType, items.ItemName, orders.OrderDate");

			boolean firstEnterance = true;
			JSONArray listOfMealTypes = new JSONArray();
			JSONArray mealTypesPerRestauratns = new JSONArray();
			String currentRestaurantName = "";
			String currentMealTypeName = "";
			String currentMealTypeYear = "";
			String currentMealTypeMonth = "";

			while (rs.next()) {
				JSONObject mealTypesRow = new JSONObject();
				mealTypesRow.put("itemID", rs.getInt("ItemID"));
				mealTypesRow.put("itemType", rs.getString("ItemType"));
				mealTypesRow.put("itemName", rs.getString("ItemName"));
				mealTypesRow.put("itemPrice", rs.getString("ItemPrice"));
				mealTypesRow.put("userID", rs.getInt("UserID"));
				mealTypesRow.put("orderID", rs.getInt("OrderID"));
				mealTypesRow.put("restaurantName", rs.getString("RestaurantName"));
				mealTypesRow.put("orderDate", rs.getString("OrderDate"));
				mealTypesRow.put("branch", rs.getString("Branch"));

				String restaurantName = rs.getString("RestaurantName");
				String mealTypeName = rs.getString("ItemType");
				String month = dateParser(rs.getString("OrderDate"), "month");
				String year = dateParser(rs.getString("OrderDate"), "year");

				if (firstEnterance) {
					currentRestaurantName = restaurantName;
					currentMealTypeName = mealTypeName;
					currentMealTypeYear = year;
					currentMealTypeMonth = month;
					firstEnterance = false;

				} else if (!restaurantName.equals(currentRestaurantName) || !mealTypeName.equals(currentMealTypeName)
						|| !currentMealTypeYear.equals(year) || !currentMealTypeMonth.equals(month)) {
					currentRestaurantName = restaurantName;
					currentMealTypeName = mealTypeName;
					currentMealTypeYear = year;
					currentMealTypeMonth = month;
					listOfMealTypes.add(mealTypesPerRestauratns);
					mealTypesPerRestauratns = new JSONArray();
				}
				mealTypesPerRestauratns.add(mealTypesRow);

			}
			listOfMealTypes.add(mealTypesPerRestauratns);

			response.put("restaurantsMealTypesList", listOfMealTypes);

		} catch (SQLException e) {
			System.out.println("DATABASE: getAllRestaurantsOrders: SQL Exception in meal types table");
			Logger.log(Level.WARNING, "DATABASE: getAllRestaurantsOrders: SQL Exception in meal types table");
		}

		return response;
	}

	/**
	 * <b>Save Income Report Information</b>
	 * <p>
	 * This method updates the database table income_report with the last month info
	 * regarding orders. Also on the case of missing data in the income report data,
	 * the method will add missing data regarding passing months.
	 * 
	 * @param json - Related data to income report that will be inserted into the
	 *             database.
	 * @author Tomer Meidan
	 * @return returns a JSONObject object to the client side with a response
	 *         depending if the update to database was a success or failure.
	 */
	public JSONObject saveIncomeReportInformation(JSONArray json) {

		JSONObject response = new JSONObject();

		int ordersSize = json.size();

		try {
			conn.setAutoCommit(false);

			for (int i = 0; i < ordersSize; i++) {
				JSONObject incomeRow = (JSONObject) json.get(i);

				PreparedStatement stmt1 = conn.prepareStatement(
						"INSERT INTO bitemedb.income_report (RestaurantName, Branch,TotalIncome, TotalOrders, Month,Year) VALUES(?,?,?,?,?,?)");
				stmt1.setString(1, Message.getValueString(incomeRow, "restaurantName"));
				stmt1.setString(2, Message.getValueString(incomeRow, "branch"));
				String totalIncome = String.valueOf(incomeRow.get("totalIncome"));
				stmt1.setString(3, totalIncome);
				String totalOrders = String.valueOf(incomeRow.get("totalOrders"));
				stmt1.setString(4, totalOrders);
				stmt1.setString(5, Message.getValueString(incomeRow, "dateMonth"));
				stmt1.setString(6, Message.getValueString(incomeRow, "dateYear"));
				stmt1.executeUpdate();
			}

		} catch (SQLException e) {
			// log
			Logger.log(Level.WARNING, "DATABASE: SQLException in saveIncomeReportInformation");
			System.out.println("DATABASE: SQLException in saveIncomeReportInformation");

			try {
				conn.rollback();
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in saveIncomeReportInformation, rollback");
				System.out.println("DATABASE: SQLException in saveIncomeReportInformation, rollback");
			}
			response.put("status", "could not update income reports to database");
			try {
				conn.setAutoCommit(true);
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in saveIncomeReportInformation, setAutoCommit");
				System.out.println("DATABASE: SQLException in saveIncomeReportInformation, setAutoCommit");
			}

			return response;
		}

		try {
			conn.commit();
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in saveIncomeReportInformation, commit");
			System.out.println("DATABASE: SQLException in saveIncomeReportInformation, commit");

			response.put("status", "could not update income reports to database");

			try {
				conn.setAutoCommit(true);
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in saveIncomeReportInformation, setAutoCommit");
				System.out.println("DATABASE: SQLException in saveIncomeReportInformation, setAutoCommit");
			}

			return response;
		}
		response.put("status", "income reports has been updated in to database");

		try {
			conn.setAutoCommit(true);
		} catch (SQLException e1) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in saveIncomeReportInformation, setAutoCommit");
			System.out.println("DATABASE: SQLException in saveIncomeReportInformation, setAutoCommit");
		}

		return response;
	}

	/**
	 * <b>Save Items Per Restaurant Report Information</b>
	 * <p>
	 * This method updates the database table itemsperrestaurant_report with the
	 * last month info regarding items that were bought. Also on the case of missing
	 * data in the items report data for a certain date, the method will add missing
	 * data regarding passing months.
	 * 
	 * @param json - Related data to items report that will be inserted into the
	 *             database.
	 * @author Tomer Meidan
	 * @return returns a JSONObject object to the client side with a response
	 *         depending if the update to database was a success or failure.
	 */
	public JSONObject saveItemsPerRestaurantReportInformation(JSONArray json) {

		JSONObject response = new JSONObject();

		int itemTypesSize = json.size();

		try {
			conn.setAutoCommit(false);

			for (int i = 0; i < itemTypesSize; i++) {
				JSONObject mealTypeRow = (JSONObject) json.get(i);

				PreparedStatement stmt1 = conn.prepareStatement(
						"INSERT INTO bitemedb.itemsperrestaurant_report (RestaurantName, ItemName,ItemType, ItemCount,Branch, Month, Year) VALUES(?,?,?,?,?,?,?)");
				stmt1.setString(1, Message.getValueString(mealTypeRow, "restaurantName"));
				stmt1.setString(2, Message.getValueString(mealTypeRow, "itemName"));
				stmt1.setString(3, Message.getValueString(mealTypeRow, "itemType"));
				String itemCount = String.valueOf(mealTypeRow.get("itemCount"));
				stmt1.setString(4, itemCount);
				stmt1.setString(5, Message.getValueString(mealTypeRow, "branch"));
				stmt1.setString(6, Message.getValueString(mealTypeRow, "dateMonth"));
				stmt1.setString(7, Message.getValueString(mealTypeRow, "dateYear"));

				stmt1.executeUpdate();
			}

		} catch (SQLException e) {
			// log
			Logger.log(Level.WARNING, "DATABASE: SQLException in saveItemsReportInformation");
			System.out.println("DATABASE: SQLException in saveItemsReportInformation");

			try {
				conn.rollback();
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in saveItemsReportInformation, rollback");
				System.out.println("DATABASE: SQLException in saveItemsReportInformation, rollback");
			}

			response.put("status", "could not update items report to database");

			try {
				conn.setAutoCommit(true);
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in saveItemsReportInformation, setAutoCommit");
				System.out.println("DATABASE: SQLException in saveItemsReportInformation, setAutoCommit");
			}
			return response;
		}

		try {
			conn.commit();
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in saveItemsReportInformation, commit");
			System.out.println("DATABASE: SQLException in saveItemsReportInformation, commit");

			response.put("status", "could not update items report to database");

			try {
				conn.setAutoCommit(true);
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in saveItemsReportInformation, setAutoCommit");
				System.out.println("DATABASE: SQLException in saveItemsReportInformation, setAutoCommit");
			}

			return response;
		}

		response.put("status", "items reports has been updated in to database");

		try {
			conn.setAutoCommit(true);
		} catch (SQLException e1) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in saveItemsReportInformation, setAutoCommit");
			System.out.println("DATABASE: SQLException in saveItemsReportInformation, setAutoCommit");
		}

		return response;
	}

	/**
	 * <b>Save Performance Report Information</b>
	 * <p>
	 * This method updates the database table performance_report with the last month
	 * info regarding performance of deliveries from the restaurants. Also on the
	 * case of missing data in the performance report data for a certain date, the
	 * method will add missing data regarding passing months.
	 * 
	 * @param json - Related data to performance report that will be inserted into
	 *             the database.
	 * @author Tomer Meidan
	 * @return returns a JSONObject object to the client side with a response
	 *         depending if the update to database was a success or failure.
	 */
	public JSONObject savePerformanceReportInformation(JSONArray json) {

		JSONObject response = new JSONObject();

		int performanceSize = json.size();

		try {
			conn.setAutoCommit(false);

			for (int i = 0; i < performanceSize; i++) {
				JSONObject performanceRow = (JSONObject) json.get(i);

				PreparedStatement stmt1 = conn.prepareStatement(
						"INSERT INTO bitemedb.performance_report (RestaurantName, Branch, OnTimeCount, LateTimeCount, OnTimeAverage, LateTimeAverage, Month, Year) VALUES(?,?,?,?,?,?,?,?)");
				stmt1.setString(1, Message.getValueString(performanceRow, "restaurantName"));
				stmt1.setString(2, Message.getValueString(performanceRow, "branch"));
				String onTimeCount = String.valueOf(performanceRow.get("onTimeCount"));
				stmt1.setString(3, onTimeCount);
				String lateTimeCount = String.valueOf(performanceRow.get("lateTimeCount"));
				stmt1.setString(4, lateTimeCount);
				String onTimeAverage = String.valueOf(performanceRow.get("onTimeAverage"));
				stmt1.setString(5, onTimeAverage);
				String lateTimeAverage = String.valueOf(performanceRow.get("lateTimeAverage"));
				stmt1.setString(6, lateTimeAverage);
				stmt1.setString(7, Message.getValueString(performanceRow, "dateMonth"));
				stmt1.setString(8, Message.getValueString(performanceRow, "dateYear"));

				stmt1.executeUpdate();
			}

		} catch (SQLException e) {
			// log
			Logger.log(Level.WARNING, "DATABASE: SQLException in savePerformanceReportInformation");
			System.out.println("DATABASE: SQLException in savePerformanceReportInformation");

			try {
				conn.rollback();
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in savePerformanceReportInformation, rollback");
				System.out.println("DATABASE: SQLException in savePerformanceReportInformation, rollback");
			}

			response.put("status", "could not update performance report to database");

			try {
				conn.setAutoCommit(true);
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in savePerformanceReportInformation, setAutoCommit");
				System.out.println("DATABASE: SQLException in savePerformanceReportInformation, setAutoCommit");
			}
			return response;
		}

		try {
			conn.commit();
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in savePerformanceReportInformation, commit");
			System.out.println("DATABASE: SQLException in savePerformanceReportInformation, commit");

			response.put("status", "could not update performance report to database");

			try {
				conn.setAutoCommit(true);
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in savePerformanceReportInformation, setAutoCommit");
				System.out.println("DATABASE: SQLException in savePerformanceReportInformation, setAutoCommit");
			}

			return response;
		}

		response.put("status", "performance reports has been updated in to database");

		try {
			conn.setAutoCommit(true);
		} catch (SQLException e1) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in savePerformanceReportInformation, setAutoCommit");
			System.out.println("DATABASE: SQLException in savePerformanceReportInformation, setAutoCommit");
		}

		return response;
	}

	/**
	 * <b>Get Existing Income Report Information</b>
	 * <p>
	 * This method will obtain from the database all the existing and related data
	 * regarding income reports from the income_report table from the database.
	 * <p>
	 * <b>Note: the data is presented in a MONTHLY order.</b>
	 * 
	 * @author Tomer Meidan
	 * @return returns a JSONArray object to the client side with a all the related
	 *         data for income reports.
	 */
	public JSONArray getExistingIncomeReportInformation() {

		ResultSet rs;
		JSONArray listOfExistingIncomeReport = new JSONArray();
		Statement stmt;

		// Building the JSON for all the orders in the database
		try {
			stmt = conn.createStatement();

			rs = stmt
					.executeQuery("SELECT * FROM bitemedb.income_report ir " + "ORDER BY ir.Branch, ir.Month, ir.Year");

			while (rs.next()) {
				JSONObject dateRow = new JSONObject();
				dateRow.put("restaurantName", rs.getString("RestaurantName"));
				dateRow.put("dateMonth", rs.getString("Month"));
				dateRow.put("dateYear", rs.getString("Year"));
				dateRow.put("branch", rs.getString("Branch"));
				dateRow.put("totalIncome", rs.getInt("TotalIncome"));
				dateRow.put("totalOrders", rs.getInt("TotalOrders"));
				listOfExistingIncomeReport.add(dateRow);
			}

		} catch (SQLException e) {
			System.out.println("DATABASE: getExistingIncomeReportInformation: SQL Exception in orders table");
			Logger.log(Level.WARNING, "DATABASE: getExistingIncomeReportInformation: SQL Exception in orders table");
		}

		return listOfExistingIncomeReport;
	}

	/**
	 * <b>Get Existing Income Report Information Quarterly</b>
	 * <p>
	 * This method will obtain from the database all the existing and related data
	 * regarding income reports from the income_report table from the database.
	 * <p>
	 * <b>Note: the data is presented in a QUARTERLY order.</b>
	 * 
	 * @author Tomer Meidan
	 * @return returns a JSONArray object to the client side with a all the related
	 *         data for income reports.
	 */
	public JSONArray getExistingIncomeReportInformationQuarterly() {

		ResultSet rs;
		JSONArray listOfExistingIncomeReport = new JSONArray();
		JSONArray listOfSameRestaurantRows = new JSONArray();
		Statement stmt;
		boolean isFirst = true;
		String currentRestaurantName = "";
		// Building the JSON for all the orders in the database
		try {
			stmt = conn.createStatement();

			rs = stmt.executeQuery("SELECT *\n" + "FROM bitemedb.income_report ir\n"
					+ "ORDER BY ir.RestaurantName, ir.Month, ir.Year");

			while (rs.next()) {
				JSONObject dateRow = new JSONObject();
				dateRow.put("restaurantName", rs.getString("RestaurantName"));
				dateRow.put("dateMonth", rs.getString("Month"));
				dateRow.put("dateYear", rs.getString("Year"));
				dateRow.put("branch", rs.getString("Branch"));
				dateRow.put("totalIncome", rs.getInt("TotalIncome"));
				dateRow.put("totalOrders", rs.getInt("TotalOrders"));

				if (isFirst) {
					currentRestaurantName = rs.getString("RestaurantName");
					isFirst = false;
				}
				if (currentRestaurantName.equals(rs.getString("RestaurantName"))) {
					listOfSameRestaurantRows.add(dateRow);
				} else {
					listOfExistingIncomeReport.add(listOfSameRestaurantRows);
					currentRestaurantName = rs.getString("RestaurantName");
					listOfSameRestaurantRows = new JSONArray();
					listOfSameRestaurantRows.add(dateRow);
				}
			}
			listOfExistingIncomeReport.add(listOfSameRestaurantRows);

		} catch (SQLException e) {
			System.out.println("DATABASE: getExistingIncomeReportInformationQuarterly: SQL Exception in orders table");
			Logger.log(Level.WARNING,
					"DATABASE: getExistingIncomeReportInformationQuarterly: SQL Exception in orders table");
		}

		return listOfExistingIncomeReport;
	}

	/**
	 * <b>Get Existing Items Per Restaurant Report Information</b>
	 * <p>
	 * This method will obtain from the database all the existing and related data
	 * regarding restaurant items reports from the itemsperrestaurant_report table
	 * from the database.
	 * <p>
	 * <b>Note: the data is presented in a MONTHLY order.</b>
	 * 
	 * @author Tomer Meidan
	 * @return returns a JSONArray object to the client side with a all the related
	 *         data for items reports.
	 */
	public JSONArray getExistingItemsPerRestaurantReportInformation() {

		ResultSet rs;
		JSONArray listOfExistingItemsPerRestaurant = new JSONArray();
		Statement stmt;

		// Building the JSON for all the orders in the database
		try {
			stmt = conn.createStatement();

			rs = stmt.executeQuery("SELECT * \n" + "FROM bitemedb.itemsperrestaurant_report it\n"
					+ "ORDER BY it.RestaurantName, it.ItemType, it.Month, it.Year");

			while (rs.next()) {
				JSONObject dateRow = new JSONObject();
				dateRow.put("restaurantName", rs.getString("RestaurantName"));
				dateRow.put("itemType", rs.getString("ItemType"));
				dateRow.put("itemName", rs.getString("ItemName"));
				dateRow.put("branch", rs.getString("Branch"));
				dateRow.put("itemCount", rs.getInt("ItemCount"));
				dateRow.put("dateMonth", rs.getString("Month"));
				dateRow.put("dateYear", rs.getString("Year"));

				listOfExistingItemsPerRestaurant.add(dateRow);
			}

		} catch (SQLException e) {
			System.out
					.println("DATABASE: getExistingItemsPerRestaurantReportInformation: SQL Exception in orders table");
			Logger.log(Level.WARNING,
					"DATABASE: getExistingItemsPerRestaurantReportInformation: SQL Exception in orders table");
		}

		return listOfExistingItemsPerRestaurant;
	}

	/**
	 * <b>Get Existing Performance Report Information</b>
	 * <p>
	 * This method will obtain from the database all the existing and related data
	 * regarding restaurant performance reports from the performance_report table
	 * from the database.
	 * <p>
	 * <b>Note: the data is presented in a MONTHLY order.</b>
	 * 
	 * @author Tomer Meidan
	 * @return returns a JSONArray object to the client side with a all the related
	 *         data for performance reports.
	 */
	public JSONArray getExistingPerformanceReportInformation() {
		ResultSet rs;
		JSONArray listOfExistingPerformanceReport = new JSONArray();
		Statement stmt;

		// Building the JSON for all the orders in the database
		try {
			stmt = conn.createStatement();

			rs = stmt.executeQuery(
					"SELECT * FROM bitemedb.performance_report pr " + "ORDER BY pr.Branch, pr.Month, pr.Year;");

			while (rs.next()) {
				JSONObject dateRow = new JSONObject();
				dateRow.put("restaurantName", rs.getString("RestaurantName"));
				dateRow.put("dateMonth", rs.getString("Month"));
				dateRow.put("dateYear", rs.getString("Year"));
				dateRow.put("branch", rs.getString("Branch"));
				dateRow.put("onTimeCount", rs.getInt("OnTimeCount"));
				dateRow.put("lateTimeCount", rs.getInt("LateTimeCount"));
				dateRow.put("onTimeAverage", rs.getInt("OnTimeAverage"));
				dateRow.put("lateTimeAverage", rs.getInt("LateTimeAverage"));

				listOfExistingPerformanceReport.add(dateRow);
			}

		} catch (SQLException e) {
			System.out.println("DATABASE: getExistingPerformanceReportInformation: SQL Exception in orders table");
			Logger.log(Level.WARNING,
					"DATABASE: getExistingPerformanceReportInformation: SQL Exception in orders table");
		}

		return listOfExistingPerformanceReport;
	}

	// Queries for UPLOAD QUARTERLY REPORTS feature -----------------

	/**
	 * <b>Get Income By Last Quarter</b>
	 * <p>
	 * This method will obtain from the database all the existing and related data
	 * regarding restaurant income reports from the income_report table from the
	 * database.
	 * <p>
	 * <b>Note: the data is presented in a QUARTERLY order and summed together <br>
	 * where certain rows have equal year, branch and quarter columns.</b>
	 * 
	 * @param json - the JSON contains certain branch, year and quarter that are
	 *             equal to the last quarter that has passed.
	 * @author Tomer Meidan
	 * @return returns a JSONArray object to the client side with a all the related
	 *         data for income reports in a quarterly fashion.
	 */
	public JSONArray getIncomeByLastQuarter(JSONObject json) {

		String currentBranch = Message.getValueString(json, "current branch");
		String currentYear = Message.getValueString(json, "current year");
		ArrayList<String> quarterArray = (ArrayList<String>) json.get("current quarter");

		ResultSet rs;
		JSONArray listOfExistingIncomeReport = new JSONArray();
		Statement stmt;

		// Building the JSON for all the orders in the database
		try {
			stmt = conn.createStatement();

			rs = stmt.executeQuery(
					"SELECT ir.RestaurantName,ir.Branch, SUM(ir.TotalIncome) as \"TotalIncome\", SUM(ir.TotalOrders) as \"TotalOrders\", ir.Year\n"
							+ "FROM bitemedb.income_report ir\n" + "WHERE (ir.Branch = \"" + currentBranch
							+ "\") AND (ir.Month = \"" + quarterArray.get(0) + "\" OR ir.Month = \""
							+ quarterArray.get(1) + "\" OR ir.Month = \"" + quarterArray.get(2)
							+ "\") AND (ir.Year = \"" + currentYear + "\")" + "GROUP BY ir.RestaurantName");

			while (rs.next()) {
				JSONObject dateRow = new JSONObject();
				dateRow.put("restaurantName", rs.getString("RestaurantName"));
				dateRow.put("dateQuarter", quarterArray.get(0) + " - " + quarterArray.get(2));
				dateRow.put("dateYear", rs.getString("Year"));
				dateRow.put("branch", rs.getString("Branch"));
				dateRow.put("totalIncome", rs.getInt("TotalIncome"));
				dateRow.put("totalOrders", rs.getInt("TotalOrders"));
				listOfExistingIncomeReport.add(dateRow);
			}

		} catch (SQLException e) {
			System.out.println("DATABASE: getIncomeQuarterly: SQL Exception in income report table");
			Logger.log(Level.WARNING, "DATABASE: getIncomeQuarterly: SQL Exception in income report table");
		}

		return listOfExistingIncomeReport;

	}

	/**
	 * <b>Get Items By Last Quarter</b>
	 * <p>
	 * This method will obtain from the database all the existing and related data
	 * regarding restaurant items reports from the itemsperrestaurant_report table
	 * from the database.
	 * <p>
	 * <b>Note: the data is presented in a QUARTERLY order and summed together <br>
	 * where certain rows have equal year, branch and quarter columns.</b>
	 * 
	 * @param json - the JSON contains certain branch, year and quarter that are
	 *             equal to the last quarter that has passed.
	 * @author Tomer Meidan
	 * @return returns a JSONArray object to the client side with a all the related
	 *         data for items reports in a quarterly fashion.
	 */
	public JSONArray getItemsByLastQuarter(JSONObject json) {

		String currentBranch = Message.getValueString(json, "current branch");
		String currentYear = Message.getValueString(json, "current year");
		ArrayList<String> quarterArray = (ArrayList<String>) json.get("current quarter");

		ResultSet rs;
		JSONArray listOfExistingItemsReport = new JSONArray();
		Statement stmt;

		// Building the JSON for all the orders in the database
		try {
			stmt = conn.createStatement();

			rs = stmt.executeQuery(
					"SELECT ir.RestaurantName,ir.Branch,ir.ItemType, SUM(ir.ItemCount) as \"ItemCount\", ir.Year\n"
							+ "FROM bitemedb.itemsperrestaurant_report ir\n" + "WHERE (ir.Branch = \"" + currentBranch
							+ "\") AND (ir.Month = \"" + quarterArray.get(0) + "\" OR ir.Month = \""
							+ quarterArray.get(1) + "\" OR ir.Month = \"" + quarterArray.get(2)
							+ "\") AND (ir.Year = \"" + currentYear + "\")"
							+ "GROUP BY ir.RestaurantName, ir.ItemType");

			while (rs.next()) {
				JSONObject dateRow = new JSONObject();
				dateRow.put("restaurantName", rs.getString("RestaurantName"));
				dateRow.put("dateQuarter", quarterArray.get(0) + " - " + quarterArray.get(2));
				dateRow.put("dateYear", rs.getString("Year"));
				dateRow.put("branch", rs.getString("Branch"));
				dateRow.put("itemType", rs.getString("ItemType"));
				dateRow.put("itemCount", rs.getInt("ItemCount"));
				listOfExistingItemsReport.add(dateRow);
			}

		} catch (SQLException e) {
			System.out.println("DATABASE: getPerformanceByChosenQuarter: SQL Exception in income report table");
			Logger.log(Level.WARNING, "DATABASE: getPerformanceByChosenQuarter: SQL Exception in income report table");
		}

		return listOfExistingItemsReport;

	}

	/**
	 * <b>Get Performance By Last Quarter</b>
	 * <p>
	 * This method will obtain from the database all the existing and related data
	 * regarding restaurant performance reports from the performance_report table
	 * from the database.
	 * <p>
	 * <b>Note: the data is presented in a QUARTERLY order and summed together <br>
	 * where certain rows have equal year, branch and quarter columns.</b>
	 * 
	 * @param json - the JSON contains certain branch, year and quarter that are
	 *             equal to the last quarter that has passed.
	 * @author Tomer Meidan
	 * @return returns a JSONArray object to the client side with a all the related
	 *         data for performance reports in a quarterly fashion.
	 */
	public JSONArray getPerformanceByLastQuarter(JSONObject json) {

		String currentBranch = Message.getValueString(json, "current branch");
		String currentYear = Message.getValueString(json, "current year");
		ArrayList<String> quarterArray = (ArrayList<String>) json.get("current quarter");

		ResultSet rs;
		JSONArray listOfExistingPerformanceReport = new JSONArray();
		Statement stmt;

		// Building the JSON for all the orders in the database
		try {
			stmt = conn.createStatement();

			rs = stmt.executeQuery(
					"SELECT pr.RestaurantName,pr.Branch, SUM(pr.OnTimeCount) as \"OnTimeCount\", SUM(pr.LateTimeCount) as \"LateTimeCount\", pr.Year\n"
							+ "FROM bitemedb.performance_report pr\n" + "WHERE (pr.Branch = \"" + currentBranch
							+ "\") AND (pr.Month = \"" + quarterArray.get(0) + "\" OR pr.Month = \""
							+ quarterArray.get(1) + "\" OR pr.Month = \"" + quarterArray.get(2)
							+ "\") AND (pr.Year = \"" + currentYear + "\")" + "GROUP BY pr.RestaurantName");

			while (rs.next()) {
				JSONObject dateRow = new JSONObject();

				dateRow.put("restaurantName", rs.getString("RestaurantName"));
				dateRow.put("dateQuarter", quarterArray.get(0) + " - " + quarterArray.get(2));
				dateRow.put("dateYear", rs.getString("Year"));
				dateRow.put("branch", rs.getString("Branch"));
				dateRow.put("onTimeCount", rs.getInt("OnTimeCount"));
				dateRow.put("lateTimeCount", rs.getInt("LateTimeCount"));

				listOfExistingPerformanceReport.add(dateRow);
			}

		} catch (SQLException e) {
			System.out.println("DATABASE: getExistingPerformanceReportInformation: SQL Exception in orders table");
			Logger.log(Level.WARNING,
					"DATABASE: getExistingPerformanceReportInformation: SQL Exception in orders table");
			System.out.println("DATABASE: getPerformanceByChosenQuarter: SQL Exception in income report table");
			Logger.log(Level.WARNING, "DATABASE: getPerformanceByChosenQuarter: SQL Exception in income report table");
		}

		return listOfExistingPerformanceReport;

	}

	/**
	 * <b>Add File To Data Base</b>
	 * <p>
	 * This method will insert a new file row to the database at the pdf_reports
	 * table and store the file by its quarter, branch, year and the file itself
	 * (stored as a LONGBLOB). if a row with the branch, year and quarter names
	 * already exist in the databse, then the row will not be added.
	 * 
	 * @param json - the JSON contains certain branch, year and quarter that are
	 *             equal to the last quarter that has passed and also a file which
	 *             is decoded into a BYTEARRAY using Base64 function.
	 * @author Tomer Meidan
	 * @return returns a JSONObject object to the client side with a response if the
	 *         file was uploaded, already exists or wasn't uploaded cause of an
	 *         Exception.
	 */
	public JSONObject addFileToDataBase(JSONObject json) throws SQLException {

		conn.setAutoCommit(false);

		String sql = "INSERT INTO pdf_reports(Branch, Quarter, Year, BlobPDF) VALUES(?,?,?,?)";
		JSONObject response = new JSONObject();

		try {

			PreparedStatement statement = conn.prepareStatement(sql);
			statement.setString(1, Message.getValueString(json, "currentBranch"));
			statement.setString(2, Message.getValueString(json, "currentQuarter"));
			statement.setString(3, Message.getValueString(json, "currentYear"));
			try {
				byte[] fileByteArray;
				fileByteArray = Base64.decode(Message.getValueString(json, "byteArray"));
				ByteArrayInputStream bais = new ByteArrayInputStream(fileByteArray);
				statement.setBlob(4, bais);

			} catch (Base64DecodingException e) {
				Logger.log(Level.WARNING, "DATEBASE :  addFileToDataBase: Base64DecodingException was thrown");
				System.out.println("DATEBASE :  addFileToDataBase: Base64DecodingException was thrown");

				response.put("command", "update");
				response.put("update", "file was uploaded");
				response.put("message", "The Quarterly Report was not uploaded to the Datebase!");
				conn.setAutoCommit(true);
				return response;
			}

			statement.execute();
			conn.commit();
		} catch (SQLIntegrityConstraintViolationException e) {
			Logger.log(Level.WARNING,
					"DATEBASE :  addFileToDataBase: SQLIntegrityConstraintViolationException was thrown");
			System.out.println("DATEBASE :  addFileToDataBase: SQLIntegrityConstraintViolationException was thrown");
			conn.rollback();

			response.put("command", "update");
			response.put("update", "file was uploaded");
			response.put("message", "The Quarterly Report already exists in the database!");
			conn.setAutoCommit(true);
			return response;
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATEBASE :  addFileToDataBase: SQLException was thrown");
			System.out.println("DATEBASE :  addFileToDataBase: SQLException was throwns");
			conn.rollback();

			response.put("command", "update");
			response.put("update", "file was uploaded");
			response.put("message", "The Quarterly Report was not uploaded to the Datebase!");
			conn.setAutoCommit(true);
			return response;
		}
		Logger.log(Level.WARNING, "DATEBASE : addFileToDataBase : File was uploaded to DataBase");
		System.out.println("DATEBASE : addFileToDataBase : File was uploaded to DataBase");

		response.put("command", "update");
		response.put("update", "file was uploaded");
		response.put("message", "The Quarterly Report was successfully uploaded to the Datebase!");
		conn.setAutoCommit(true);
		return response;

	}

	/**
	 * <b>Get Files From Data Base</b>
	 * <p>
	 * This method will obtain all file rows from the database pdf_reports table by
	 * its quarter, branch, year and the file itself (stored as a LONGBLOB).
	 * 
	 * @author Tomer Meidan
	 * @return the returned JSON contains all branch, year and quarter report file
	 *         rows and also the file is encoded into a STRING using Base64
	 *         function.
	 */
	public JSONArray getFilesFromDataBase() throws SQLException {

		String sql = "SELECT * FROM bitemedb.pdf_reports;";
		InputStream input = null;
		ResultSet rs;
		JSONArray filesArray = new JSONArray();
		try {
			PreparedStatement statement = conn.prepareStatement(sql);
			rs = statement.executeQuery();

			while (rs.next()) {
				JSONObject fileRow = new JSONObject();

				// write binary stream into file
				input = rs.getBinaryStream("BlobPDF");
				byte[] byteArray = getByteArrayFromStream(input);
				String byteArrayEncoded = Base64.encode(byteArray);
				fileRow.put("file", byteArrayEncoded);

				// write other information from file table
				fileRow.put("branch", rs.getString("Branch"));
				fileRow.put("quarter", rs.getString("Quarter"));
				fileRow.put("year", rs.getString("Year"));
				String fileName = rs.getString("Branch") + "_" + rs.getString("Quarter") + "_" + rs.getString("Year")
						+ ".pdf";
				fileRow.put("fileName", fileName);
				filesArray.add(fileRow);
			}

			input.close();
		} catch (SQLException | IOException e) {
			Logger.log(Level.WARNING, "DataBase : getFilesFromDataBase: SQL query failed.");
			System.out.println("DataBase : getFilesFromDataBase: SQL query failed.");

		}

		Logger.log(Level.WARNING, "DataBase : getFilesFromDataBase: Files were downloaded from DataBase");
		System.out.println("DataBase : getFilesFromDataBase: Files were downloaded from DataBase");
		return filesArray;

	}

	/**
	 * <b>Get Byte Array From Stream</b>
	 * <p>
	 * This method will recieves a file input presented as an input stream object
	 * and transforms it into a bytearray.
	 * 
	 * @param inputStream - representing an input stream of bytes.
	 * @author Tomer Meidan
	 * @return the returned byte array will hold a stream of values according to the
	 *         inputStream given.
	 */
	public static byte[] getByteArrayFromStream(InputStream inputStream) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final InputStream in = inputStream;
		final byte[] buffer = new byte[10000];

		int read = -1;
		while ((read = in.read(buffer)) > 0) {
			baos.write(buffer, 0, read);
		}
		in.close();

		return baos.toByteArray();
	}

	// ------------------------------------------------------

	/**
	 * Parsering the type of date information from an SQL date string
	 * 
	 * @param dateString - for example: Fri Nov 26 12:43:27 IST 2021
	 * @param type       - year, month, day, time
	 * @return
	 */
	public static String dateParser(String dateString, String type) {

		String parsedString = "";

		switch (type) {
		case "year":
			parsedString = dateString.substring(20, 24);
			break;
		case "month":
			switch (dateString.substring(0, 3)) {
			case "Jan":
				parsedString = "01";
				break;
			case "Feb":
				parsedString = "02";
				break;
			case "Mar":
				parsedString = "03";
				break;
			case "Apr":
				parsedString = "04";
				break;
			case "May":
				parsedString = "05";
				break;
			case "Jun":
				parsedString = "06";
				break;
			case "Jul":
				parsedString = "07";
				break;
			case "Aug":
				parsedString = "08";
				break;
			case "Sep":
				parsedString = "09";
				break;
			case "Oct":
				parsedString = "10";
				break;
			case "Nov":
				parsedString = "11";
				break;
			case "Dec":
				parsedString = "12";
				break;
			default:
				System.out.println("Parser: dateSQL: Invalid month detected, parsing did not succeed.");
				break;
			}
			break;
		case "day":
			parsedString = dateString.substring(4, 6);
			break;
		case "seconds":
			parsedString = dateString.substring(13, 15);
			break;
		case "minutes":
			parsedString = dateString.substring(10, 12);
			break;
		case "hours":
			parsedString = dateString.substring(7, 9);
			break;
		default:
			System.out.println("Parser: dateSQL: Invalid date value detected, parsing did not succeed.");
			break;
		}

		return parsedString;

	}

	/**
	 * registerSupplier
	 * 
	 * This method updates supplier in database as 'active' and sets deliveryTypes
	 * columns. This method sets 'username' and 'password' as given in input. This
	 * method updates both Tables, if any fails rollback's.
	 * 
	 * @param JSONObject json - includes: 'supplier
	 *                   name','username','password','delivery types' keys for
	 *                   supplier info accordingly.
	 * @author Roman Milman
	 * @return JSONObject - "update" : "supplier has been registered" if succeed,
	 *         other wise "could not register supplier"
	 */
	public JSONObject registerSupplier(JSONObject json) {
		JSONObject response = new JSONObject();
		String supplierName = Message.getValueString(json, "supplier name");
		response.put("supplier name", supplierName);

		try {
			conn.setAutoCommit(false);

			PreparedStatement stmt = conn.prepareStatement(
					"update bitemedb.users set username = ?, password = ? where userid = (select userid from bitemedb.suppliers where name = ?) ");
			stmt.setString(1, Message.getValueString(json, "username"));
			stmt.setString(2, Message.getValueString(json, "password"));
			stmt.setString(3, Message.getValueString(json, "supplier name"));

			stmt.executeUpdate();

			stmt = conn.prepareStatement(
					"update bitemedb.suppliers set status = 'active', deliveryTypes = ? where name = ?");
			stmt.setString(1, Message.getValueString(json, "delivery types"));
			stmt.setString(2, Message.getValueString(json, "supplier name"));

			stmt.executeUpdate();

		} catch (SQLException e) {
			// log
			Logger.log(Level.WARNING, "DATABASE: SQLException in registerSupplier");
			System.out.println("DATABASE: SQLException in registerSupplier");

			try {
				conn.rollback();
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in registerSupplier, rollback");
				System.out.println("DATABASE: SQLException in registerSupplier, rollback");
			}
			response.put("update", "could not register supplier");

			try {
				conn.setAutoCommit(true);
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in registerSupplier, setAutoCommit");
				System.out.println("DATABASE: SQLException in registerSupplier, setAutoCommit");
			}

			return response;
		}

		try {
			conn.commit();
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in registerSupplier, commit");
			System.out.println("DATABASE: SQLException in registerSupplier, commit");

			response.put("update", "could not register supplier");

			try {
				conn.setAutoCommit(true);
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in registerSupplier, setAutoCommit");
				System.out.println("DATABASE: SQLException in registerSupplier, setAutoCommit");
			}

			return response;
		}
		response.put("update", "supplier has been registered");

		try {
			conn.setAutoCommit(true);
		} catch (SQLException e1) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in registerSupplier, setAutoCommit");
			System.out.println("DATABASE: SQLException in registerSupplier, setAutoCommit");
		}

		return response;
	}

	/**
	 * getMenu
	 * 
	 * This method take the items from specific restaurant's menu. The data was
	 * taken from items, mustfeatures and optional features tables.
	 * 
	 * @param String userID - id of the supplier
	 * @return returns a JSONObject object to the client side with key of hash map
	 *         that present the menu.
	 */
	public JSONObject getMenu(String userID) {
		JSONObject response = new JSONObject();
		HashMap<String, JSONArray> menu = new HashMap<>();
		response.put("command", "update");
		response.put("update", "menuList");
		ResultSet rs;
		Statement stmt;
		try {

			stmt = conn.createStatement();
			rs = stmt.executeQuery(
					"SELECT * FROM items LEFT JOIN optionalfeatures ON optionalfeatures.ItemID = items.ItemID LEFT JOIN mustfeatures ON mustfeatures.ItemID = items.ItemID WHERE items.UserID = '"
							+ userID + "'");

			while (rs.next()) {
				JSONObject json = new JSONObject();
				int itemID = rs.getInt("ItemID");
				json.put("itemName", rs.getString("ItemName"));
				json.put("itemPrice", rs.getString("ItemPrice"));
				json.put("itemID", rs.getString("ItemID"));
				json.put("imgMeal", rs.getString("ImgMeal"));
				json.put("imgType", rs.getString("ImgType"));
				JSONArray optionalFeatures = new JSONArray();
				JSONArray mustFeatures = new JSONArray();
				int flag = 0;
				while (itemID == rs.getInt("ItemID")) {
					flag = 1;
					if (rs.getString("OptionalFeature") != null) {
						JSONObject optional = new JSONObject();
						optional.put("optional", rs.getString("OptionalFeature"));
						optional.put("price", rs.getString("OptionalPrice"));
						optional.put("opID", rs.getString("OptionalFeatureID"));
						if (!optionalFeatures.contains(optional))
							optionalFeatures.add(optional);
					}
					if (rs.getString("MustFeature") != null) {
						JSONObject must = new JSONObject();
						must.put("must", rs.getString("MustFeature"));
						must.put("price", rs.getString("MustPrice"));
						must.put("muID", rs.getString("MustFeatureID"));
						if (!mustFeatures.contains(must))
							mustFeatures.add(must);
					}
					if (!rs.next())
						break;

				}
				if (flag == 1)
					rs.previous();
				flag = 0;
				json.put("optionalFeatures", optionalFeatures);
				json.put("mustFeatures", mustFeatures);
				if (!menu.containsKey(rs.getString("ItemType"))) {

					JSONArray dishs = new JSONArray();
					dishs.add(json);
					menu.put(rs.getString("ItemType"), dishs);
				} else
					menu.get(rs.getString("ItemType")).add(json);
			}

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}

		response.put("menuList", menu);
		return response;
	}

	/**
	 * checkType
	 * 
	 * This method checks the input of edit/add type. This method checks if type
	 * name include only letters and space, length no more then 10 and exist type.
	 * 
	 * @param JSONObject json - includes: 'itemType','nameType','userID'(of
	 *                   supplier), JSONObject response - include returns value and
	 *                   is a parameter for ass relevant feedback if the test
	 *                   failed, String key- sign if check edit/add options.
	 * 
	 * @return boolean - true if type is incorrect.
	 */
	private boolean checkType(JSONObject json, JSONObject response, String key) {
		String itemType = "";
		if (key.equals("edit"))
			itemType = Message.getValueString(json, "itemType");
		String newTypeName = Message.getValueString(json, "nameType");
		String userID = Message.getValueString(json, "userID");
		// -------------------- check input--------------//
		if (!checkLetter(newTypeName)) {
			Logger.log(Level.DEBUG, "DATABASE: edit (" + newTypeName + ") type is NOT approved");
			System.out.println("DATABASE: edit (" + newTypeName + ") type is NOT approved");
			response.put("feedback", "Error: Can get only letters!");
			response.put("newTypeName", itemType);
			return true;
		}
		// -------------------------------------------------//

		if (newTypeName.length() > 10) {
			Logger.log(Level.DEBUG, "DATABASE: edit (" + newTypeName + ") type is NOT approved");
			System.out.println("DATABASE: edit (" + newTypeName + ") type is NOT approved");
			response.put("feedback", "Please type not more then 10 letters");
			response.put("newTypeName", itemType);
			return true;
		}

		Statement stmt;
		ResultSet rs;

		try {
			stmt = conn.createStatement();
			if (!itemType.equals(newTypeName)) {
				// ------------------if exist ----------------------------//
				rs = stmt.executeQuery("SELECT * FROM items WHERE items.UserID ='" + userID + "' AND items.ItemType ='"
						+ newTypeName + "'");
				if (rs.next()) {
					Logger.log(Level.DEBUG, "DATABASE: add (" + newTypeName + ") type NOT approved");
					System.out.println("DATABASE: user add (" + newTypeName + ") type is NOT approved");
					response.put("feedback", "Error: " + newTypeName + " type already exist!");
					return true;
				}
				// ---------------------------------------------------------//
			}

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}
		return false;
	}

	/**
	 * editType
	 * 
	 * This method replace type meal details of restaurant menu in database. First
	 * the method takes data from items table and then replace the relevant fields.
	 * 
	 * @param JSONObject json - includes: 'imgType','itemType',
	 *                   'nameType','userID'(of supplier).
	 * @return returns a JSONObject object to the client side with relevant data of
	 *         edited type.
	 */
	public JSONObject editType(JSONObject json) {
		JSONObject response = new JSONObject();
		response.put("command", "update");
		response.put("update", "saveType");
		String imgType = Message.getValueString(json, "imgType");
		Statement stmt;
		PreparedStatement stmtP;
		ResultSet rs1;
		ArrayList<JSONObject> itemsDetails = new ArrayList<>();
		String userID = Message.getValueString(json, "userID");
		String itemType = Message.getValueString(json, "itemType");
		String newTypeName = Message.getValueString(json, "nameType");
		response.put("feedOK", true);
		response.put("menu", getMenu(userID));
		response.put("oldTypeName", itemType);
		if (checkType(json, response, "edit")) {
			response.put("feedOK", false);
			return response;
		}
		try {
			stmt = conn.createStatement();
			rs1 = stmt.executeQuery(
					"SELECT * FROM items WHERE items.UserID ='" + userID + "' AND items.ItemType ='" + itemType + "'");
			while (rs1.next()) {
				JSONObject item = new JSONObject();
				item.put("itemID", rs1.getString("ItemID"));
				item.put("itemName", rs1.getString("ItemName"));
				item.put("itemPrice", rs1.getInt("ItemPrice"));
				item.put("imgMeal", rs1.getString("ImgMeal"));
				itemsDetails.add(item);

			}
			stmtP = conn.prepareStatement(
					"REPLACE INTO items (ItemID, ItemType, ItemName, ImgType, ImgMeal, ItemPrice, UserID) VALUES (?, ?, ?, ?,?,?,  "
							+ userID + ")");
			for (JSONObject ob : itemsDetails) {
				stmtP.setString(1, Message.getValueString(ob, "itemID"));
				stmtP.setString(2, newTypeName);
				stmtP.setString(3, Message.getValueString(ob, "itemName"));
				stmtP.setString(4, imgType);
				stmtP.setString(5, Message.getValueString(ob, "imgMeal"));
				stmtP.setInt(6, (int) ob.get("itemPrice"));
				stmtP.executeUpdate();

			}
			conn.commit();
		} catch (Exception e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}
		Logger.log(Level.DEBUG, "DATABASE: edit (" + newTypeName + ") type is approved");
		System.out.println("DATABASE: user edit (" + newTypeName + ") type is approved");
		response.put("menu", getMenu(userID));
		response.put("newTypeName", newTypeName);
		response.put("feedback", "Changes saved");

		return response;
	}

	/**
	 * addType
	 * 
	 * This method insert type meal details of restaurant menu in database.
	 * 
	 * @param JSONObject json - includes: 'imgType','nameType','userID'(of
	 *                   supplier).
	 * @return returns a JSONObject object to the client side with relevant data of
	 *         new type.
	 */
	public JSONObject addType(JSONObject json) {
		JSONObject response = new JSONObject();
		response.put("command", "update");
		response.put("update", "addNewType");
		String newTypeName = Message.getValueString(json, "nameType");
		String userID = Message.getValueString(json, "userID");
		String imgType = Message.getValueString(json, "imgType");
		response.put("menu", getMenu(userID));
		response.put("feedOK", true);
		if (checkType(json, response, "add")) {
			response.put("feedOK", false);
			return response;
		}
		Statement stmt;
		PreparedStatement stmtP;

		try {
			stmt = conn.createStatement();
			stmtP = conn.prepareStatement("INSERT INTO items (ItemType, ImgType, UserID) VALUES (?,?, " + userID + ")");
			stmtP.setString(1, newTypeName);
			stmtP.setString(2, imgType);
			stmtP.executeUpdate();
			conn.commit();

		} catch (Exception e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}

		Logger.log(Level.DEBUG, "DATABASE: add (" + newTypeName + ") type is approved");
		System.out.println("DATABASE: user add (" + newTypeName + ") type is approved");
		response.put("menu", getMenu(userID));
		response.put("newTypeName", newTypeName);
		response.put("feedback", newTypeName + " was added");

		return response;
	}

	/**
	 * deleteType
	 * 
	 * This method delete type meal of restaurant menu in database. First get the
	 * fields of meals that belong to this type.
	 * 
	 * @param JSONObject json - includes: 'itemType','userID'(of supplier).
	 * @return returns a JSONObject object to the client side with relevant data of
	 *         deleted type.
	 */
	public JSONObject deleteType(JSONObject json) {
		JSONObject response = new JSONObject();
		response.put("command", "update");
		response.put("update", "deleteType");
		Statement stmt;
		PreparedStatement stmtPItem;
		PreparedStatement stmtPOptional;
		PreparedStatement stmtPMust;
		ResultSet rs;

		String nameType = Message.getValueString(json, "itemType");
		String userID = Message.getValueString(json, "userID");
		response.put("itemType", nameType);
		response.put("menu", getMenu(userID));
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(
					"SELECT * FROM items WHERE items.UserID ='" + userID + "' AND items.ItemType ='" + nameType + "'");

			stmtPItem = conn.prepareStatement("DELETE FROM items WHERE items.ItemID = ?");
			stmtPOptional = conn.prepareStatement("DELETE FROM optionalfeatures WHERE optionalfeatures.ItemID = ?");
			stmtPMust = conn.prepareStatement("DELETE FROM mustfeatures WHERE mustfeatures.ItemID = ?");
			while (rs.next()) {
				stmtPOptional.setInt(1, rs.getInt("ItemID"));
				stmtPOptional.executeUpdate();
				stmtPMust.setInt(1, rs.getInt("ItemID"));
				stmtPMust.executeUpdate();
				stmtPItem.setInt(1, rs.getInt("ItemID"));
				stmtPItem.executeUpdate();
			}
			conn.commit();
		} catch (Exception e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in deleteType");
		}
		Logger.log(Level.DEBUG, "DATABASE: delete (" + nameType + ") type is approved");
		System.out.println("DATABASE: user delete (" + nameType + ") type is approved");

		response.put("menu", getMenu(userID));
		response.put("deletedType", nameType);
		response.put("feedback", nameType + " deleted");
		return response;
	}

	/**
	 * checkLetter
	 * 
	 * This method checks word input. This method checks if name include only
	 * letters and space.
	 * 
	 * @param String word - the input check
	 * @return boolean - true if the input is correct
	 */
	public boolean checkLetter(String word) {
		String[] forCheck = word.split(" ");
		for (int i = 0; i < forCheck.length; i++)
			if (!(forCheck[i].matches("[a-zA-Z]+")))
				return false;
		return true;
	}

	/**
	 * checkMeals
	 * 
	 * This method checks the input of edit/add meals. This method checks if meal
	 * name include only letters and space, length no more then 10, exist meal,
	 * price include only digits.
	 * 
	 * @param JSONObject json - includes: 'itemType','nameType','userID'(of
	 *                   supplier), JSONObject response - is return value and is a
	 *                   parameter for relevant feedback if the test failed, String
	 *                   key- sign if check edit/add options.
	 * 
	 * @return boolean - true if type is incorrect.
	 */
	private boolean checkMeals(JSONObject json, JSONObject response, String key) {

		String newDishName = Message.getValueString(json, "newDishName");
		String newDishPrice = Message.getValueString(json, "newDishPrice");
		String oldDishName = "";
		if (key.equals("edit"))
			oldDishName = Message.getValueString(json, "dishName");
		String userID = Message.getValueString(json, "userID");
		String itemType = Message.getValueString(json, "itemType");
		// ------------------ Input Check---------//
		if (!checkLetter(newDishName) || !newDishPrice.matches("[0-9]+")) {
			Logger.log(Level.DEBUG, "DATABASE: edit (" + newDishName + ") dish is NOT approved");
			System.out.println("DATABASE: edit (" + newDishName + ") dish is NOT approved");
			response.put("feedback", "Error: Input values type are incorrect");
			return true;
		}
		// ------------------------------------//

		if (newDishName.length() > 10) {
			Logger.log(Level.DEBUG, "DATABASE: edit (" + newDishName + ") dish is NOT approved");
			System.out.println("DATABASE: edit (" + newDishName + ") dish is NOT approved");
			response.put("feedback", "Please type not more then 10 letters");
			return true;
		}

		Statement stmt;
		ResultSet rs;

		if (!oldDishName.endsWith(newDishName)) {
			// ------------------if exist ----------------------------//
			try {
				stmt = conn.createStatement();
				rs = stmt.executeQuery("SELECT * FROM items WHERE items.UserID ='" + userID + "' AND items.ItemType ='"
						+ itemType + "' AND items.ItemName ='" + newDishName + "'");
				if (rs.next()) {
					Logger.log(Level.DEBUG, "DATABASE: add (" + newDishName + ") dish NOT approved");
					System.out.println("DATABASE: user add (" + newDishName + ") dish is NOT approved");
					response.put("feedback", "Error: " + newDishName + " dish already exist!");
					return true;
				}
			} catch (SQLException e) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
			}

		}

		return false;
	}

	/**
	 * saveFeatures
	 * 
	 * This method edit/add must and optional features. The method
	 * update/delete/insert optionalfetures and mustfeatures tables.
	 * 
	 * @param JSONObject json - 'optionalEdit'/'mustEdit'(JSONArray of features that
	 *                   designed to be edited), 'optionalFeat'/'mustEdit' (HashMap
	 *                   of features that designed to be added),JSONObject response
	 *                   - is return value and is a parameter for relevant feedback
	 *                   if the test, String feedback - parameter to which add a
	 *                   feedback, String key - edit/add, String itID -itemID failed
	 */
	private void saveFeatures(JSONObject json, JSONObject response, String feedback, String key, String itID) {
		JSONArray optionalEdit = null;
		JSONArray mustEdit = null;

		HashMap<String, String> optionalFeat = optionalFeat = (HashMap<String, String>) json.get("optionalFeat");
		HashMap<String, String> mustFeat = mustFeat = (HashMap<String, String>) json.get("mustFeat");
		if (key.equals("edit")) {
			optionalEdit = (JSONArray) json.get("optionalEdit");

			for (int i = 0; i < optionalEdit.size(); i++) {
				JSONObject opEdit = (JSONObject) optionalEdit.get(i);
				String newOpName = Message.getValueString(opEdit, "newName");
				if (optionalFeat.containsKey(newOpName))
					optionalFeat.remove(newOpName);
			}

			mustEdit = (JSONArray) json.get("mustEdit");

			for (int i = 0; i < mustEdit.size(); i++) {
				JSONObject muEdit = (JSONObject) mustEdit.get(i);
				String newMuName = Message.getValueString(muEdit, "newName");
				if (mustFeat.containsKey(newMuName))
					mustFeat.remove(newMuName);
			}
		}
		String itemID = itID;

		Statement stmt;
		ResultSet rs;
		PreparedStatement stmtPOptional;
		PreparedStatement stmtPMust;
		PreparedStatement stmtP;

		try {

			stmt = conn.createStatement();
			if (optionalEdit != null) {
				for (int i = 0; i < optionalEdit.size(); i++) {

					JSONObject opEdit = (JSONObject) optionalEdit.get(i);
					String newOpName = Message.getValueString(opEdit, "newName");
					String newOpPrice = Message.getValueString(opEdit, "newPrice");
					boolean edit = (boolean) opEdit.get("edit");
					int newOpPriceI = Integer.parseInt(newOpPrice);
					boolean deleteOp = (boolean) opEdit.get("delete");
					String opID = Message.getValueString(opEdit, "ID");

					if (deleteOp) {
						stmtPOptional = conn.prepareStatement(
								"DELETE FROM optionalfeatures WHERE optionalfeatures.OptionalFeatureID = " + opID);
						stmtPOptional.executeUpdate();
						response.put("feedback", feedback + newOpName + " optional feature deleted");
					} else {
						if (edit) {
							stmtP = conn
									.prepareStatement("UPDATE optionalfeatures SET OptionalFeature =? WHERE ItemID = "
											+ itemID + " AND OptionalFeatureID = " + opID);
							stmtP.setString(1, newOpName);
							stmtP.executeUpdate();
							stmtP = conn.prepareStatement("UPDATE optionalfeatures SET OptionalPrice =? WHERE ItemID = "
									+ itemID + " AND OptionalFeatureID = " + opID);
							stmtP.setInt(1, newOpPriceI);
							stmtP.executeUpdate();
						}
					}
				}
			}
			if (mustEdit != null) {

				// ----------------------------------------------------------------------------------------------//
				for (int i = 0; i < mustEdit.size(); i++) {
					JSONObject muEdit = (JSONObject) mustEdit.get(i);
					String newMuName = Message.getValueString(muEdit, "newName");
					String newMuPrice = Message.getValueString(muEdit, "newPrice");
					boolean edit = (boolean) muEdit.get("edit");
					int newMuPriceI = Integer.parseInt(newMuPrice);
					boolean deleteMust = (boolean) muEdit.get("delete");
					String muID = Message.getValueString(muEdit, "ID");
					if (deleteMust) {
						stmtPMust = conn.prepareStatement(
								"DELETE FROM mustfeatures WHERE mustfeatures.MustFeatureID = " + muID);
						stmtPMust.executeUpdate();
						response.put("feedback", feedback + newMuName + " must feature deleted");
					} else {
						if (edit) {
							stmtP = conn.prepareStatement("UPDATE mustfeatures SET MustFeature =? WHERE ItemID = "
									+ itemID + " AND MustFeatureID = " + muID);
							stmtP.setString(1, newMuName);
							stmtP.executeUpdate();
							stmtP = conn.prepareStatement("UPDATE mustfeatures SET MustPrice =? WHERE ItemID = "
									+ itemID + " AND MustFeatureID = " + muID);
							stmtP.setInt(1, newMuPriceI);
							stmtP.executeUpdate();
						}
					}

				}
			}

			for (String name : optionalFeat.keySet()) {

				String nameOp = name;
				String priceOp = optionalFeat.get(name);
				int priceOpI = Integer.parseInt(priceOp);

				rs = stmt.executeQuery("SELECT * FROM optionalfeatures WHERE optionalfeatures.OptionalFeature ='"
						+ nameOp + "' AND optionalfeatures.ItemID = " + itemID);

				stmtPOptional = conn.prepareStatement(
						"INSERT INTO optionalfeatures (ItemID, OptionalFeature, OptionalPrice) VALUES (?,?, ?)");
				stmtPOptional.setString(1, itemID);
				stmtPOptional.setString(2, nameOp);
				stmtPOptional.setInt(3, priceOpI);
				System.out.println(stmtPOptional.toString());
				stmtPOptional.executeUpdate();

			}

			for (String name : mustFeat.keySet()) {

				String nameMu = name;
				String priceMu = mustFeat.get(name);
				int priceMuI = Integer.parseInt(priceMu);

				stmtPMust = conn
						.prepareStatement("INSERT INTO mustfeatures (ItemID, MustFeature, MustPrice) VALUES (?,?, ?)");
				stmtPMust.setString(1, itemID);
				stmtPMust.setString(2, nameMu);
				stmtPMust.setInt(3, priceMuI);
				stmtPMust.executeUpdate();

			}

		} catch (Exception e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");

		}

	}

	/**
	 * saveEditDish
	 * 
	 * This method update meal details of restaurant menu in database. First the
	 * method call to checkMeals for input check.
	 * 
	 * @param JSONObject json - includes: 'dishName','itemType',
	 *                   'newDishName','userID'(of supplier),
	 *                   'imgMeal','newDishPrice', 'itemID' .
	 * @return returns a JSONObject object to the client side with relevant data of
	 *         edit meal.
	 */
	public JSONObject saveEditDish(JSONObject json) {
		JSONObject response = new JSONObject();
		response.put("command", "update");
		response.put("update", "editDish");
		String oldDishName = Message.getValueString(json, "dishName");
		String newDishName = Message.getValueString(json, "newDishName");
		String newDishPrice = Message.getValueString(json, "newDishPrice");
		String itemType = Message.getValueString(json, "itemType");

		String imgMeal = Message.getValueString(json, "imgMeal");
		response.put("itemType", itemType);

		String userID = Message.getValueString(json, "userID");
		String itemID = Message.getValueString(json, "itemID");
		PreparedStatement stmtP;
		response.put("menu", getMenu(userID));
		response.put("feedOK", true);
		if (checkMeals(json, response, "edit")) {
			response.put("feedOK", false);
			return response;
		}

		int newDishPriceI = Integer.parseInt(newDishPrice);
		try {

			stmtP = conn.prepareStatement("UPDATE items SET ItemName =? WHERE ItemID = " + itemID);
			stmtP.setString(1, newDishName);
			stmtP.executeUpdate();
			stmtP = conn.prepareStatement("UPDATE items SET ItemPrice =? WHERE ItemID = " + itemID);
			stmtP.setInt(1, newDishPriceI);
			stmtP.executeUpdate();
			stmtP = conn.prepareStatement("UPDATE items SET ImgMeal =? WHERE ItemID = " + itemID);
			stmtP.setString(1, imgMeal);
			stmtP.executeUpdate();

			String feedback = "Meal changed ";
			response.put("feedback", feedback);
			saveFeatures(json, response, feedback, "edit", itemID);
			conn.commit();
		} catch (Exception e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");

		}

		response.put("menu", getMenu(userID)); // update menu
		Logger.log(Level.DEBUG, "DATABASE: edit (" + oldDishName + ") dish is approved");
		System.out.println("DATABASE: edit (" + oldDishName + ") dish is approved");
		return response;
	}

	/**
	 * saveAddDish
	 * 
	 * This method add new meal details of restaurant menu in database. First the
	 * method call to checkMeals for input check. Second the method check if this is
	 * the first meal on his type.
	 * 
	 * @param JSONObject json - includes: 'imgMeal','itemType',
	 *                   'newDishName','userID'(of supplier),
	 *                   'imgMeal','newDishPrice', 'itemID' .
	 * @return returns a JSONObject object to the client side with relevant data of
	 *         add meal.
	 */
	public JSONObject saveAddDish(JSONObject json) {
		JSONObject response = new JSONObject();
		response.put("command", "update");
		response.put("update", "editDish");

		String nameDish = Message.getValueString(json, "newDishName");
		String priceDish = Message.getValueString(json, "newDishPrice");

		String itemType = Message.getValueString(json, "itemType");
		response.put("itemType", itemType);
		String userID = Message.getValueString(json, "userID");
		response.put("menu", getMenu(userID));
		PreparedStatement stmtPItem;
		Statement stmt;
		ResultSet rs;
		String imgMeal = Message.getValueString(json, "imgMeal");
		String imgType = Message.getValueString(json, "imgType");
		response.put("feedOK", true);
		if (checkMeals(json, response, "add")) {
			response.put("feedOK", false);
			return response;
		}
		try {

			stmt = conn.createStatement();
			int priceDishI = Integer.parseInt(priceDish);

			// ----add first meal to new type----//

			rs = stmt.executeQuery(
					"SELECT * FROM items WHERE items.UserID ='" + userID + "' AND items.ItemType ='" + itemType + "'");
			rs.next();
			String itemID = rs.getString("ItemID");
			if (rs.getString("ItemName") == null) {

				stmtPItem = conn.prepareStatement("UPDATE items SET ItemName =? WHERE ItemID = " + itemID);
				stmtPItem.setString(1, nameDish);
				stmtPItem.executeUpdate();
				stmtPItem = conn.prepareStatement("UPDATE items SET ItemPrice =? WHERE ItemID = " + itemID);
				stmtPItem.setInt(1, priceDishI);
				stmtPItem.executeUpdate();
				stmtPItem = conn.prepareStatement("UPDATE items SET ImgMeal =? WHERE ItemID = " + itemID);
				stmtPItem.setString(1, imgMeal);
				stmtPItem.executeUpdate();

			}

			else {
				stmtPItem = conn.prepareStatement(
						"INSERT INTO items (ItemType, ItemName, ImgMeal, ImgType, ItemPrice, UserID) VALUES (?,?, ?,?,?,  "
								+ userID + ")");
				stmtPItem.setString(1, itemType);
				stmtPItem.setString(2, nameDish);
				stmtPItem.setString(3, imgMeal);
				stmtPItem.setString(4, imgType);
				stmtPItem.setInt(5, priceDishI);

				stmtPItem.executeUpdate();

				rs = stmt.executeQuery("SELECT * FROM items WHERE items.UserID ='" + userID + "' AND items.ItemType ='"
						+ itemType + "' AND items.ItemName ='" + nameDish + "'");
				rs.next();
				itemID = rs.getString("ItemID");

			}

			String feedback = "Meal added ";
			response.put("feedback", feedback);
			saveFeatures(json, response, feedback, "add", itemID);
			conn.commit();
		} catch (Exception e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");

		}

		// update menu
		response.put("menu", getMenu(userID));
		// ---//
		Logger.log(Level.DEBUG, "DATABASE: add (" + nameDish + ") dish is approved");
		System.out.println("DATABASE: add (" + nameDish + ") dish is approved");
		return response;
	}

	/**
	 * deleteDish
	 * 
	 * This method delete meal of restaurant menu in database. First check if this
	 * meal is single of his type.
	 * 
	 * @param JSONObject json - includes: 'itemType','userID'(of
	 *                   supplier),'itemID','itemName'.
	 * @return returns a JSONObject object to the client side with relevant data of
	 *         deleted meal.
	 */
	public JSONObject deleteDish(JSONObject json) {
		JSONObject response = new JSONObject();
		response.put("command", "update");
		response.put("update", "deleteDish");
		PreparedStatement stmtPItem;
		PreparedStatement stmtPOptional;
		PreparedStatement stmtPMust;
		JSONArray items = (JSONArray) json.get("items");
		int numOfItems = items.size();
		Statement stmt;
		ResultSet rs;
		String itemName = Message.getValueString(json, "itemName");
		String itemID = Message.getValueString(json, "itemID");
		String userID = Message.getValueString(json, "userID");
		String itemType = Message.getValueString(json, "itemType");

		response.put("menu", getMenu(userID));
		try {
			if (numOfItems == 1) {
				stmt = conn.createStatement();

				rs = stmt.executeQuery("SELECT * FROM items WHERE items.ItemID ='" + itemID + "'");

				rs.next();

				stmtPItem = conn.prepareStatement("UPDATE items SET ItemName =? WHERE ItemID = " + itemID);
				stmtPItem.setString(1, null);
				stmtPItem.executeUpdate();
				stmtPItem = conn.prepareStatement("UPDATE items SET ItemPrice =? WHERE ItemID = " + itemID);
				stmtPItem.setString(1, null);
				stmtPItem.executeUpdate();
				stmtPItem = conn.prepareStatement("UPDATE items SET ImgMeal =? WHERE ItemID = " + itemID);
				stmtPItem.setString(1, null);
				stmtPItem.executeUpdate();

			} else {

				stmtPItem = conn.prepareStatement("DELETE FROM items WHERE items.ItemID = " + itemID);
				stmtPItem.executeUpdate();
			}
			stmtPOptional = conn
					.prepareStatement("DELETE FROM optionalfeatures WHERE optionalfeatures.ItemID = " + itemID);
			stmtPMust = conn.prepareStatement("DELETE FROM mustfeatures WHERE mustfeatures.ItemID = " + itemID);
			stmtPOptional.executeUpdate();
			stmtPMust.executeUpdate();
			conn.commit();
		} catch (Exception e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}
		Logger.log(Level.DEBUG, "DATABASE: delete (" + itemName + ") dish is approved");
		System.out.println("DATABASE: user delete (" + itemName + ") dish is approved");

		response.put("menu", getMenu(userID));
		response.put("itemType", itemType);
		response.put("feedback", itemName + " deleted");
		return response;

	}

	/**
	 * ordersByHour
	 * 
	 * This method put order in hash map according to the key - hours. The method
	 * put the orders that will receive on this day.
	 * 
	 * @param HashMap<String, JSONObject> oreders - list of all orders.
	 * @return returns a HashMap<String, HashMap<String, JSONObject>> object to the
	 *         client side with today's orders that arranged by hours.
	 */
	public HashMap<String, HashMap<String, JSONObject>> ordersByHour(HashMap<String, JSONObject> oreders) {

		HashMap<String, HashMap<String, JSONObject>> orderByGroup = new HashMap<>();
		for (String i : oreders.keySet()) {
			JSONObject order = oreders.get(i);
			String date = Message.getValueString(order, "recieveTime");

			String[] recieveTime1 = date.split(" ");
			String month = recieveTime1[0];
			String day = recieveTime1[1];
			String recieveTime = recieveTime1[2];

			Date dateD = Calendar.getInstance().getTime();
			String dateStr = dateD.toString();
			String[] dateStrSplit = dateStr.split(" ");
			String monthNeeds = dateStrSplit[1];
			String dayNeeds = dateStrSplit[2];

			String[] toHour = recieveTime.split(":");
			String hourS = toHour[0];
			String minuteS = toHour[1];
			int hour = Integer.parseInt(hourS);
			int minute = Integer.parseInt(minuteS);
			String hourto = hour + "";
			int minute_30 = 30 - minute;
			if (monthNeeds.equals(month) && dayNeeds.equals(day)) {

				if (minute_30 <= 20 && minute_30 > -10)
					hourto = hour + ":30";
				else if (minute_30 > 20 && minute_30 <= 30)
					hourto = hour + ":00";
				else
					hourto = (hour + 1) + ":00";

				if (!orderByGroup.containsKey(hourto)) {
					HashMap<String, JSONObject> orederG = new HashMap<>();
					orederG.put(i, order);
					orderByGroup.put(hourto, orederG);
				} else
					orderByGroup.get(hourto).put(i, order);

			}
		}
		return orderByGroup;
	}

	/**
	 * getOrderList
	 * 
	 * This method put order in hash map according to the key - order ID. The method
	 * put all of the order details on this hash map in JSONObject.
	 * 
	 * @param JSONObject json - includes:' supplierID'.
	 * @return returns a JSONObject object to the client side with today's orders
	 *         that arranged by hours(from ordersByHour method) and restaurant name.
	 */
	public JSONObject getOrderList(JSONObject json) {
		JSONObject response = new JSONObject();
		response.put("command", "update");
		response.put("update", "ordersList");
		HashMap<String, JSONObject> orders = new HashMap<>();
		Statement stmt1, stmt2, stmt3;
		ResultSet rs1, rs2, r3;
		String supplierID = Message.getValueString(json, "supplierID");

		try {
			stmt1 = conn.createStatement();
			stmt2 = conn.createStatement();
			stmt3 = conn.createStatement();
			rs1 = stmt1.executeQuery("SELECT * FROM orders WHERE orders.supplierID ='" + supplierID + "'");
			while (rs1.next()) {
				JSONObject orderJson = new JSONObject();
				String orderID = rs1.getString("OrderID");
				String total = rs1.getString("Total");
				String restaurantName = rs1.getString("RestaurantName");
				response.put("restaurantName", restaurantName);
				String recieveTime = rs1.getString("DueDate");
				orderJson.put("recieveTime", recieveTime);
				orderJson.put("total", total);
				String earlyBooking = rs1.getString("EarlyBooking");
				String userID = rs1.getString("UserID"); // added
				JSONArray arrOrders = new JSONArray();
				r3 = stmt3.executeQuery("SELECT * FROM mealsperorder WHERE OrderID ='" + orderID + "'");

				while (r3.next()) {
					JSONObject j = new JSONObject();
					String itemID = r3.getString("ItemID");
					String mustFeatureID = r3.getString("MustFeatureID");
					String mealsPerOrderID = r3.getString("MealsPerOrderID");

					rs2 = stmt2.executeQuery("SELECT * FROM items WHERE ItemID ='" + itemID + "'");
					rs2.next();
					j.put("itemType", rs2.getString("ItemType"));
					j.put("itemName", rs2.getString("ItemName"));

					if (!mustFeatureID.equals("0")) {
						rs2 = stmt2.executeQuery(
								"SELECT * FROM mustfeatures WHERE MustFeatureID ='" + mustFeatureID + "'");
						rs2.next();
						j.put("mustFeature", rs2.getString("MustFeature"));
					}

					ArrayList<String> opID = new ArrayList<>();
					rs2 = stmt2.executeQuery(
							"SELECT * FROM optionalfeaturesperitem WHERE MealsPerOrderID ='" + mealsPerOrderID + "'");
					while (rs2.next())
						opID.add(rs2.getString("OptionalFeatureID"));
					if (!opID.isEmpty()) {
						ArrayList<String> opName = new ArrayList<>();
						for (int i = 0; i < opID.size(); i++) {
							rs2 = stmt2.executeQuery(
									"SELECT * FROM optionalfeatures WHERE OptionalFeatureID ='" + opID.get(i) + "'");
							rs2.next();
							opName.add(i, rs2.getString("OptionalFeature"));
						}
						j.put("optionalNames", opName);
					}

					arrOrders.add(j);
				}

				rs2 = stmt2.executeQuery("SELECT * FROM customers WHERE UserID ='" + userID + "'");
				rs2.next();

				orderJson.put("employerID", rs2.getString("EmployerID")); // if not business --> employerID = null
				orderJson.put("clientID", userID);
				orderJson.put("status", rs1.getString("Status"));

				orderJson.put("recieveTime", recieveTime);
				orderJson.put("earlyBooking", earlyBooking);
				orderJson.put("arrOrders", arrOrders);
				orderJson.put("email", rs2.getString("Email"));
				orders.put(orderID, orderJson);

			}

		} catch (Exception e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}

		HashMap<String, HashMap<String, JSONObject>> orderByGroup = ordersByHour(orders);
		response.put("orders", orderByGroup);

		return response;
	}

	/**
	 * approveOrder
	 * 
	 * This method update status field in orders table. Also, the method check if
	 * the approval orders includes in co-delivery.
	 * 
	 * @param JSONObject json - includes:' ordersPerHour'(orders that waiting to be
	 *                   approval),'approveTime','approvalNum', 'restaurantName'. .
	 * @return returns a JSONObject object to the client side with updated order
	 *         list, restaurant name and approvalOrders - connect details of users.
	 */
	public JSONObject approveOrder(JSONObject json) {
		JSONObject response = new JSONObject();
		response.put("command", "update");
		response.put("update", "Order is ready");
		JSONArray ordersPerHour = (JSONArray) json.get("ordersPerHour");
		String approveTime = Message.getValueString(json, "approveTime");
		String approvalNumS = Message.getValueString(json, "approvalNum");
		response.put("restaurantName", Message.getValueString(json, "restaurantName"));
		int approvalNum = Integer.parseInt(approvalNumS);
		JSONArray approvalOrders = new JSONArray();
		for (int i = 0; i < ordersPerHour.size(); i++) {
			JSONObject approvalOrder = new JSONObject();
			JSONObject order = (JSONObject) ordersPerHour.get(i);
			String orderID = Message.getValueString(order, "orderID");
			String totalS = Message.getValueString(order, "total");
			String employerID = Message.getValueString(order, "employerID");
			String customerID = Message.getValueString(order, "customerID");

			boolean selected = (boolean) order.get("selected");
			int total = Integer.parseInt(totalS);
			PreparedStatement stmtP;

			approvalOrder.put("email", Message.getValueString(order, "email"));
			approvalOrder.put("orderID", orderID);
			approvalOrder.put("customerID", customerID);
			approvalOrders.add(approvalOrder);
			try {
				if (selected) {
					stmtP = conn.prepareStatement(
							"UPDATE orders SET ApproveDate=? WHERE orders.OrderID ='" + orderID + "'");
					stmtP.setString(1, approveTime);
					stmtP.executeUpdate();
					stmtP = conn
							.prepareStatement("UPDATE orders SET Status =? WHERE orders.OrderID ='" + orderID + "'");
					stmtP.setString(1, "Ready");
					stmtP.executeUpdate();
					if (approvalNum > 1 && !employerID.equals(null)) {
						int cl = total - 5;
						if (cl >= 15) {
							stmtP = conn.prepareStatement(
									"UPDATE orders SET Total =? WHERE orders.OrderID ='" + orderID + "'");
							stmtP.setInt(1, cl);
							stmtP.executeUpdate();
						}
					}
					conn.commit();
				}
			} catch (Exception e) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
			}

		}
		response.put("approvalOrders", approvalOrders);
		response.put("orders", getOrderList(json));
		return response;
	}

	/**
	 * getReceiptList
	 * 
	 * The method gets all price of order list for this month from specific
	 * restaurant. The method calculate the price after 7% fee.
	 * 
	 * @param JSONObject json - includes:' supplierID'.
	 * @return returns a JSONObject object to the client side with receipts list.
	 */
	public JSONObject getReceiptList(JSONObject json) {
		JSONObject response = new JSONObject();
		response.put("command", "update");
		response.put("update", "receiptList");
		JSONArray receipts = new JSONArray();
		Date date = Calendar.getInstance().getTime();
		String dateS = date.toString();
		String[] months = dateS.split(" ");
		String month = months[1];

		Statement stmt;
		ResultSet rs;
		String supplierID = Message.getValueString(json, "supplierID");
		try {
			stmt = conn.createStatement();

			rs = stmt.executeQuery("SELECT * FROM suppliers WHERE UserID ='" + supplierID + "'");
			rs.next();
			String restaurantName = rs.getString("Name");
			rs = stmt.executeQuery("SELECT * FROM orders WHERE orders.RestaurantName ='" + restaurantName + "'");
			while (rs.next()) {
				JSONObject pay = new JSONObject();
				String dateVal = rs.getString("DueDate");
				String[] monthsVal = dateVal.split(" ");
				String monthVal = monthsVal[0];
				if (monthVal.equals(month)) {
					pay.put("price", rs.getString("Total"));
					pay.put("date", rs.getString("DueDate"));
					receipts.add(pay);
				}
			}
		} catch (Exception e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}
		response.put("receipts", receipts);
		return response;
	}

	public String getEmployerID(JSONObject response) {
		ResultSet rs;
		String id = Message.getValueString(response, "userID");
		//
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT (EMPLOYERID) FROM HR WHERE userID = ?");
			stmt.setString(1, id);

			rs = stmt.executeQuery();
			if (rs.next()) {
				// log
				Logger.log(Level.DEBUG, "DATABASE: HRS ID:(" + id + ") have been found");
				System.out.println("DATABASE: HRS ID:(" + id + ") have been found");

				String employerID = String.valueOf(rs.getInt("employerID"));
				return employerID;

			}
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}
		// log
		Logger.log(Level.DEBUG, "DATABASE: HRS ID:(" + id + ") have NOT been found");
		System.out.println("DATABASE: HRS ID:(" + id + ") have NOT been found");

		return null;
	}

	private Boolean isEmployerActive(int employerID) {
		ResultSet rs;
		//
		try {
			PreparedStatement stmt = conn
					.prepareStatement("SELECT * FROM bitemedb.employers WHERE employerID = ? AND status = 'Active'");
			stmt.setInt(1, employerID);

			rs = stmt.executeQuery();
			if (rs.next()) {

				return true;
			}
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}

		return false;
	}

	/**
	 * getInactiveBusinessCustomers
	 * 
	 * This method builds JSONArray with 'inactive' business customers by employerID
	 * given as input.
	 * 
	 * @param JSONObject json - includes "employerID" key for employerID id value.
	 * @return JSONObject - "customers" : JSONArray with customer info.
	 * @author Roman Milman
	 */
	public JSONObject getInactiveBusinessCustomers(JSONObject json) {
		ResultSet rs;
		int employerID = Integer.valueOf(Message.getValueString(json, "employerID"));
		JSONObject response = new JSONObject();
		JSONArray customers = new JSONArray();

		if (isEmployerActive(employerID)) {

			try {
				PreparedStatement stmt = conn
						.prepareStatement("SELECT * FROM CUSTOMERS WHERE employerID = ? AND status = 'inactive'");
				stmt.setInt(1, employerID);

				rs = stmt.executeQuery();
				while (rs.next()) {
					JSONObject customerInfo = new JSONObject();
					String customerID = rs.getString("id");

					// log
					Logger.log(Level.DEBUG, "DATABASE: customer ID:(" + customerID + ") found as status-inactive");
					System.out.println("DATABASE: customer ID:(" + customerID + ") found as status-inactive");

					customerInfo.put("id", customerID);
					customerInfo.put("credit", rs.getString("creditNumber"));
					customerInfo.put("number", rs.getString("phoneNumber"));
					customerInfo.put("email", rs.getString("email"));

					customers.add(customerInfo);
				}
			} catch (SQLException e) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in getInactiveBusinessCustomers");
			}
		}

		response.put("customers", customers);
		return response;
	}

	/**
	 * activeCustomer
	 * 
	 * This method updates customers table with status 'active' by given id as
	 * input.
	 * 
	 * @param JSONObject json - includes "id" key for customers id value.
	 * @return JSONObject - "update" : "customer has been activated" if succeeded,
	 *         otherwise returns null.
	 * @author Roman Milman
	 */
	public JSONObject activeCustomer(JSONObject json) {
		String id = Message.getValueString(json, "id");
		JSONObject response = new JSONObject();

		try {
			PreparedStatement stmt = conn.prepareStatement("update customers set status='active' where id=?");
			stmt.setString(1, id);

			stmt.executeUpdate();
			// log
			Logger.log(Level.DEBUG, "DATABASE: customer (" + id + ") status changed to Active");
			System.out.println("DATABASE: customer (" + id + ") status changed to Active");

			response.put("command", "update");
			response.put("update", "customer has been activated");
			response.put("id", id);

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in activeCustomer");
		}

		return response;
	}

	/**
	 * registerEmployer
	 * 
	 * This method updates employer in database as 'inactive' and sets w4c, balance
	 * columns. This method sets 'balance' and 'w4c' as given in input.
	 * 
	 * @param JSONObject json - includes: 'employer name','balance','w4c' keys for
	 *                   employer accordingly values.
	 * @return JSONObject - "update" : "employer has been registered" if succeed,
	 *         otherwise "could not registered employer"
	 * @author Roman Milman
	 */
	public JSONObject registerEmployer(JSONObject json) {
		String employerName = Message.getValueString(json, "employer name");
		JSONObject response = new JSONObject();
		response.put("employer name", employerName);

		try {
			PreparedStatement stmt = conn.prepareStatement(
					"update bitemedb.employers set status = 'inactive', balance = ?, w4c = ? where name = ?");
			stmt.setString(1, Message.getValueString(json, "balance"));
			stmt.setString(2, Message.getValueString(json, "w4c"));
			stmt.setString(3, employerName);

			stmt.executeUpdate();
			// log
			Logger.log(Level.DEBUG, "DATABASE: employer (" + employerName + ") was registered");
			System.out.println("DATABASE: employer (" + employerName + ") was registered");

			response.put("update", "employer has been registered");

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in registerEmployer");
			System.out.println("DATABASE: SQLException in registerEmployer");

			response.put("update", "could not registered employer");
		}

		return response;
	}

	/**
	 * Get all the active orders made by a customer
	 * 
	 * @param userID
	 * @return JSONObject with keys:<br>
	 *         "command", value "update"<br>
	 *         "update", value "Show orders"<br>
	 *         "orders", value JSONArray containing the orders made by customer with
	 *         userID
	 */
	public JSONObject getOrders(String userID) {
		JSONObject response = new JSONObject();
		JSONArray orders = new JSONArray();
		try {
			Statement stmt = conn.createStatement();
			System.out.println("SELECT * FROM bitemedb.orders WHERE UserID = " + userID
					+ " AND (Status = 'Waiting for approval' OR Status = 'Ready')");
			ResultSet rs = stmt.executeQuery("SELECT * FROM bitemedb.orders WHERE UserID = " + userID
					+ " AND (Status = 'Waiting for approval' OR Status = 'Ready')");
			while (rs.next()) {
				Order order = new Order(Long.valueOf(rs.getInt("OrderID")), rs.getString("OrderDate"),
						rs.getString("DueDate"), rs.getString("RestaurantName"), rs.getString("Status"),
						rs.getInt("Total"), rs.getString("PickUpType"), rs.getString("EarlyBooking"),
						rs.getString("Address"), rs.getString("PhoneNumber"), rs.getString("SupplierID"));
				order.addMeals(getOrderDetails(order.getOrderID().intValue()));
				orders.add(order.intoJSONObject());
			}
			response.put("orders", orders);
			response.put("command", "update");
			response.put("update", "Show orders");
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in getOrders");
			System.out.println("DATABASE: SQLException in getOrders");
		}
		return response;
	}

	/**
	 * This method pulls an order's meals from the database
	 * 
	 * @param orderID
	 * @return ArrayList<Meal> (see Meal class)
	 */
	private ArrayList<Meal> getOrderDetails(int orderID) {
		ArrayList<OptionalFeature> optionalFeatureList;
		ArrayList<Meal> mealsList = new ArrayList<>();
		MustFeature mustFeature = null;
		int mealsPerOrderID;
		int itemID;
		int mustFeatureID;
		try {
			PreparedStatement stmt1 = conn.prepareStatement("SELECT MealsPerOrderID, ItemID,MustFeatureID"
					+ " FROM bitemedb.mealsperorder" + " WHERE OrderID = ?;");
			PreparedStatement stmt2 = conn.prepareStatement(
					"SELECT MustFeature, MustPrice" + " FROM bitemedb.mustfeatures " + "WHERE MustFeatureID = ?;");
			PreparedStatement stmt3 = conn.prepareStatement("SELECT OptionalFeatureID "
					+ "FROM bitemedb.optionalfeaturesperitem" + " WHERE MealsPerOrderID = ?;");
			PreparedStatement stmt4 = conn.prepareStatement("SELECT OptionalFeature, OptionalPrice"
					+ " FROM bitemedb.optionalfeatures" + " WHERE OptionalFeatureID = ?;");
			PreparedStatement stmt5 = conn
					.prepareStatement("SELECT ItemName, ItemPrice " + "FROM bitemedb.items " + "WHERE ItemID = ?;");

			stmt1.setInt(1, orderID);
			ResultSet rs1 = stmt1.executeQuery();
			while (rs1.next()) {
				mealsPerOrderID = rs1.getInt(1);
				itemID = rs1.getInt(2);
				mustFeatureID = rs1.getInt(3);
				optionalFeatureList = new ArrayList<>();
				stmt2.setInt(1, mustFeatureID);
				ResultSet rs2 = stmt2.executeQuery();
				if (rs2.next()) {
					mustFeature = new MustFeature(rs2.getInt(2), rs2.getString(1), "");
				}

				stmt3.setInt(1, mealsPerOrderID);
				ResultSet rs3 = stmt3.executeQuery();
				while (rs3.next()) {
					stmt4.setInt(1, rs3.getInt(1));
					ResultSet rs4 = stmt4.executeQuery();
					if (rs4.next()) {
						OptionalFeature optionalFeature = new OptionalFeature(rs4.getInt(2), rs4.getString(1), "");
						optionalFeatureList.add(optionalFeature);
					}
				}

				stmt5.setInt(1, itemID);
				ResultSet rs5 = stmt5.executeQuery();
				if (rs5.next()) {
					Meal meal = new Meal(rs5.getString(1), rs5.getString(2), mustFeature, optionalFeatureList);
					mealsList.add(meal);
				}
			}
		} catch (SQLException e) {
			System.out.println(e);
			System.out.println("DATABASE: SQLException in getOrderDetails");
			Logger.log(Level.WARNING, "DATABASE: SQLException in getOrderDetails");
		}
		return mealsList;
	}

	/**
	 * Update status of order to Delivered.<br>
	 * Also checks if eligible for refund using method checkIfEligibleForRefund Make
	 * sure to include keys:<br>
	 * "orderID", "deliverDate".
	 * 
	 * @param json
	 * @return JSONObject, with keys:<br>
	 *         "command" with value update,<br>
	 *         "update" with value: "Show pop up: order finished".
	 */
	public JSONObject updateOrderRecieved(JSONObject json) {
		int orderID = ((Message.getValueLong(json, "orderID"))).intValue();
		String deliverDate = Message.getValueString(json, "deliverDate");
		JSONObject response = new JSONObject();
		try {
			PreparedStatement preStmt = conn
					.prepareStatement("UPDATE orders SET DeliverDate = ? , Status = ? WHERE OrderID = ?");
			preStmt.setString(1, deliverDate);
			preStmt.setString(2, "Delivered");
			preStmt.setInt(3, orderID);
			preStmt.executeUpdate();
			checkIfEligibleForRefund(json);

		} catch (SQLException e) {
			System.out.println(e);
			System.out.println("DATABASE: SQLException in updateOrderRecieved");
			Logger.log(Level.WARNING, "DATABASE: SQLException in updateOrderRecieved");
		}
		response.put("command", "update");
		response.put("update", "Show pop up: order finished");
		return response;
	}

	/**
	 * Method to check if (difference) time has passed from start to end
	 * 
	 * @param start
	 * @param end
	 * @param difference (minutes)
	 * @return true if end - start > difference
	 */
	public boolean checkIfDifferencePassed(String start, String end, int difference) {
		int endHour = Integer.valueOf(DateParser.dateParser(end, "hours"));
		int endMinutes = Integer.valueOf(DateParser.dateParser(end, "minutes"));
		int startHour = Integer.valueOf(DateParser.dateParser(start, "hours"));
		int startMinutes = Integer.valueOf(DateParser.dateParser(start, "minutes"));
		if ((endHour * 60 + endMinutes) - (startHour * 60 + startMinutes) > difference)
			return true;
		else
			return false;
	}

	/**
	 * This method checks if a customer is eligible for a refund (according to the
	 * story)<br>
	 * It uses the method checkIfDifferencePassed to find out the difference between
	 * dueDate and deliverDate<br>
	 * If eligible, update the refund balance of the customer, else do nothing
	 * 
	 * @param json containing keys:<br>
	 *             "deliverDate", "dueDate", "supplierID", "totalCost", "userID",
	 *             "earlyBooking".
	 */
	private void checkIfEligibleForRefund(JSONObject json) {
		String deliverDate = Message.getValueString(json, "deliverDate");
		String dueDate = Message.getValueString(json, "dueDate");
		String supplierID = Message.getValueString(json, "supplierID");
		int totalCost = Message.getValueLong(json, "totalCost").intValue();
		int addToBalance = (int) (totalCost * 0.5);
		String currentBalance = "0";
		String customerID = Message.getValueString(json, "userID");
		boolean earlyBooking = (boolean) json.get("earlyBooking");
		boolean refundFlag = false;
		if (earlyBooking) { // check if it's been more than 20 minutes
			if (checkIfDifferencePassed(dueDate, deliverDate, 20))
				refundFlag = true;
		} else {
			if (checkIfDifferencePassed(dueDate, deliverDate, 60))
				refundFlag = true;
		}
		if (refundFlag) {
			try {
				PreparedStatement preStmt = conn.prepareStatement(
						"SELECT RefundBalance " + "FROM refund WHERE CustomerID = ? AND SupplierID = ?");
				preStmt.setString(1, customerID);
				preStmt.setString(2, supplierID);
				ResultSet rs = preStmt.executeQuery();
				if (rs.next()) {
					currentBalance = rs.getString("RefundBalance");
				} else { // if customer is not registered in the database table refund, add him
					preStmt = conn.prepareStatement(
							"INSERT INTO refund (SupplierID, CustomerID, RefundBalance) VALUES (?,?,0);");
					preStmt.setString(1, supplierID);
					preStmt.setString(2, customerID);
					preStmt.executeUpdate();
				}
				addToBalance += Integer.valueOf(currentBalance);
				preStmt = conn.prepareStatement(
						"UPDATE refund SET RefundBalance = ? WHERE CustomerID = ? AND SupplierID = ?");
				preStmt.setString(1, String.valueOf(addToBalance));
				preStmt.setString(2, customerID);
				preStmt.setString(3, supplierID);
				preStmt.executeUpdate();
			} catch (SQLException e) {
				System.out.println(e);
				System.out.println("DATABASE: SQLException in checkIfEligibleForRefund");
				Logger.log(Level.WARNING, "DATABASE: SQLException in checkIfEligibleForRefund");
			}
		}
	}

	/**
	 * This method returns a user's details.
	 * 
	 * @param userID
	 * @return JSONObject with keys:<br>
	 *         "ID", "email", "address", "phoneNumber", "creditNumber",
	 *         "employerID", "update", "command".<br>
	 *         "command" with value "update" "update" with value "W4C found", if if
	 *         success "update" with value "W4C not found", if fail
	 */
	public JSONObject fetchW4CDetails(String userID) {
		JSONObject response = new JSONObject();
		String employerID = null;
		response.put("command", "update");
		try {
			PreparedStatement preStmt = conn
					.prepareStatement("SELECT ID,Email,Address,PhoneNumber,EmployerID, CreditNumber "
							+ "FROM customers " + "WHERE UserID = ?");
			preStmt.setInt(1, Integer.valueOf(userID));
			ResultSet rs = preStmt.executeQuery();
			if (rs.next()) {
				response.put("ID", rs.getString("ID"));
				response.put("email", rs.getString("Email"));
				response.put("address", rs.getString("Address"));
				response.put("phoneNumber", rs.getString("PhoneNumber"));
				response.put("creditNumber", rs.getString("CreditNumber"));
				employerID = rs.getString("EmployerID");
				response.put("employerID", employerID);
				response.put("update", "W4C found");
				preStmt = conn.prepareStatement("SELECT W4C, Name FROM bitemedb.employers WHERE EmployerID = ?;");
				preStmt.setString(1, employerID);
				rs = preStmt.executeQuery();
				if (rs.next()) {
					response.put("employerW4C", rs.getString("W4C"));
					response.put("employerName", rs.getString("Name"));
				}
			} else
				response.put("update", "W4C not found");
		} catch (SQLException e) {
			System.out.println(e);
			System.out.println("DATABASE: SQLException in checkIfEligibleForRefund");
			Logger.log(Level.WARNING, "DATABASE: SQLException in checkIfEligibleForRefund");
		}
		return response;
	}

	/**
	 * getInactiveCustomer
	 * 
	 * This method builds JSONArray with customers that defined with status null in
	 * DB.
	 * 
	 * @param JSONObject json - includes: 'branch'.
	 * @return JSONObject - "customers" : JSONArray with customers
	 * @author Roman Milman
	 */
	public JSONObject getInactiveCustomer(JSONObject json) {
		ResultSet rs;

		String branch = Message.getValueString(json, "branch");
		JSONObject response = new JSONObject();
		JSONArray customers = new JSONArray();

		try {
			PreparedStatement stmt = conn.prepareStatement(
					"SELECT FirstName,LastName,id FROM bitemedb.users inner join bitemedb.customers on users.userid = customers.userID where Role is NULL and Branch = ?");
			stmt.setString(1, branch);

			rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject customerInfo = new JSONObject();
				int id = rs.getInt("id");

				// log
				Logger.log(Level.DEBUG, "DATABASE: customer ID:(" + id + ") is inactive");
				System.out.println("DATABASE: customer ID:(" + id + ") is inactive");

				customerInfo.put("first name", rs.getString("FirstName"));
				customerInfo.put("last name", rs.getString("LastName"));
				customerInfo.put("id", rs.getString("id"));

				customers.add(customerInfo);
			}

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in getInactiveCustomer");
			System.out.println("DATABASE: SQLException in getInactiveCustomer");
		}
		response.put("customers", customers);
		return response;
	}

	/**
	 * getInactiveSupplier
	 * 
	 * This method builds JSONArray with unregistered suppliers by given branch as
	 * input.
	 * 
	 * @param JSONObject json - includes: 'branch'.
	 * @return JSONObject - "suppliers" : JSONArray with suppliers
	 * @author Roman Milman
	 */
	public JSONObject getInactiveSupplier(JSONObject json) {
		ResultSet rs;

		String branch = Message.getValueString(json, "branch");
		JSONObject response = new JSONObject();
		JSONArray customers = new JSONArray();

		try {
			PreparedStatement stmt = conn.prepareStatement(
					"SELECT * FROM bitemedb.users inner join bitemedb.suppliers where Role = 'Supplier' and users.Branch = ? and username is NULL and password is NULL and users.userID = suppliers.userID");
			stmt.setString(1, branch);

			rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject customerInfo = new JSONObject();
				String supplierName = rs.getString("name");

				// log
				Logger.log(Level.DEBUG, "DATABASE: supplier name:(" + supplierName + ") is inactive");
				System.out.println("DATABASE: supplier name:(" + supplierName + ") is inactive");

				customerInfo.put("first name", rs.getString("FirstName"));
				customerInfo.put("last name", rs.getString("LastName"));
				customerInfo.put("supplier name", supplierName);
				customerInfo.put("phone number", rs.getString("phoneNumber"));
				customerInfo.put("email", rs.getString("email"));

				customers.add(customerInfo);
			}

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in getInactiveSupplier");
			System.out.println("DATABASE: SQLException in getInactiveSupplier");
		}
		response.put("suppliers", customers);
		return response;
	}

	/**
	 * getAllCustomersByBranch
	 * 
	 * This method builds JSONArray with all customers by given branch as input;
	 * except status is null.
	 * 
	 * @param JSONObject json - includes: 'branch'.
	 * @return JSONObject - "customers" : JSONArray with customers
	 * @author Roman Milman
	 */
	public JSONObject getAllCustomersByBranch(JSONObject json) {
		ResultSet rs;

		String branch = Message.getValueString(json, "branch");
		JSONObject response = new JSONObject();
		JSONArray customers = new JSONArray();

		try {
			PreparedStatement stmt = conn
					.prepareStatement("select Firstname,Lastname,Role,name,id from (bitemedb.employers right join\r\n"
							+ "(select FirstName,LastName,Role,employerID,id from bitemedb.users inner join bitemedb.customers on users.userid = customers.userid where (role = 'Customer' or role = 'Business Customer') and branch = ?) a on a.employerID = employers.employerid)");
			stmt.setString(1, branch);

			rs = stmt.executeQuery();
			while (rs.next()) {
				JSONObject customerInfo = new JSONObject();
				String id = rs.getString("id");

				// log
				Logger.log(Level.DEBUG, "DATABASE: id:(" + id + ") have been found as in getAllCustomers");
				System.out.println("DATABASE: id:(" + id + ") have been found as in getAllCustomers");

				customerInfo.put("first name", rs.getString("FirstName"));
				customerInfo.put("last name", rs.getString("LastName"));
				customerInfo.put("role", rs.getString("role"));
				customerInfo.put("employer name", rs.getString("name"));
				customerInfo.put("id", id);

				customers.add(customerInfo);
			}

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in getInactiveSupplier");
			System.out.println("DATABASE: SQLException in getInactiveSupplier");
		}
		response.put("customers", customers);
		return response;
	}

	/**
	 * switchRoleToBusinessCustomer
	 * 
	 * This method switches customers role to 'Business Customer', handles all the
	 * necessary changes in the Tables. This method updates both Tables, if any
	 * fails rollback's.
	 * 
	 * @param int        employerUserID.
	 * @param JSONObject json - includes: 'id'.
	 * @return JSONObject - "update" : "customer role has been switched" if succeed,
	 *         otherwise "could not switch customer role", 'id' : same as input.
	 * @author Roman Milman
	 */
	public JSONObject switchRoleToBusinessCustomer(JSONObject json, int employerUserID) {
		String id = Message.getValueString(json, "id");
		JSONObject response = new JSONObject();
		response.put("id", id);

		try {
			conn.setAutoCommit(false);

			PreparedStatement stmt = conn.prepareStatement(
					"update bitemedb.customers set status = 'inactive', employerID = ?, balance = (select balance from bitemedb.employers where employerid = ?) where id = ?");
			stmt.setInt(1, employerUserID);
			stmt.setString(2, String.valueOf(employerUserID));
			stmt.setString(3, id);

			stmt.executeUpdate();

			stmt = conn.prepareStatement(
					"update bitemedb.users set role = 'Business Customer' where users.userid = (select userid from bitemedb.customers where id = ?)");
			stmt.setString(1, id);

			stmt.executeUpdate();

		} catch (SQLException e) {
			// log
			Logger.log(Level.WARNING, "DATABASE: SQLException in switchRoleToBusinessCustomer");
			System.out.println("DATABASE: SQLException in switchRoleToBusinessCustomer");

			try {
				conn.rollback();
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in switchRoleToBusinessCustomer, rollback");
				System.out.println("DATABASE: SQLException in switchRoleToBusinessCustomer, rollback");
			}
			response.put("update", "could not switch customer role");

			try {
				conn.setAutoCommit(true);
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in switchRoleToBusinessCustomer, setAutoCommit");
				System.out.println("DATABASE: SQLException in switchRoleToBusinessCustomer, setAutoCommit");
			}

			return response;
		}

		try {
			conn.commit();

			// log
			Logger.log(Level.DEBUG, "DATABASE: customer id: (" + id + ") was switched to Business Customer");
			System.out.println("DATABASE: customer id: (" + id + ") was switched to Business Customer");
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in switchRoleToBusinessCustomer, commit");
			System.out.println("DATABASE: SQLException in switchRoleToBusinessCustomer, commit");

			response.put("update", "could not switch customer role");

			try {
				conn.setAutoCommit(true);
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in switchRoleToBusinessCustomer, setAutoCommit");
				System.out.println("DATABASE: SQLException in switchRoleToBusinessCustomer, setAutoCommit");
			}

			return response;
		}
		response.put("update", "customer role has been switched");

		try {
			conn.setAutoCommit(true);
		} catch (SQLException e1) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in switchRoleToBusinessCustomer, setAutoCommit");
			System.out.println("DATABASE: SQLException in switchRoleToBusinessCustomer, setAutoCommit");
		}

		return response;
	}

	/**
	 * switchRoleToRegularCustomer
	 * 
	 * This method switches customers role to 'Customer', handles all the necessary
	 * changes in the Tables. This method updates both Tables, if any fails
	 * rollback's.
	 * 
	 * @param JSONObject json - includes: 'id'.
	 * @return JSONObject - "update" : "customer role has been switched" if succeed,
	 *         otherwise "could not switch customer role", 'id' : same as input.
	 * @author Roman Milman
	 */
	public JSONObject switchRoleToRegularCustomer(JSONObject json) {
		String id = Message.getValueString(json, "id");
		JSONObject response = new JSONObject();
		response.put("id", id);

		try {
			conn.setAutoCommit(false);

			PreparedStatement stmt = conn.prepareStatement(
					"update bitemedb.customers set status = 'active', employerID = null, balance = null where id = ?");
			stmt.setString(1, id);

			stmt.executeUpdate();

			stmt = conn.prepareStatement(
					"update bitemedb.users set role = 'Customer' where users.userid = (select userid from bitemedb.customers where id = ?)");
			stmt.setString(1, id);

			stmt.executeUpdate();

		} catch (SQLException e) {
			// log
			Logger.log(Level.WARNING, "DATABASE: SQLException in switchRoleToRegularCustomer");
			System.out.println("DATABASE: SQLException in switchRoleToRegularCustomer");

			try {
				conn.rollback();
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in switchRoleToRegularCustomer, rollback");
				System.out.println("DATABASE: SQLException in switchRoleToRegularCustomer, rollback");
			}
			response.put("update", "could not switch customer role");

			try {
				conn.setAutoCommit(true);
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in switchRoleToRegularCustomer, setAutoCommit");
				System.out.println("DATABASE: SQLException in switchRoleToRegularCustomer, setAutoCommit");
			}

			return response;
		}

		try {
			conn.commit();

			// log
			Logger.log(Level.DEBUG, "DATABASE: customer id: (" + id + ") was switched to Regular Customer");
			System.out.println("DATABASE: customer id: (" + id + ") was switched to Regular Customer");
		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in switchRoleToRegularCustomer, commit");
			System.out.println("DATABASE: SQLException in switchRoleToRegularCustomer, commit");

			response.put("update", "could not switch customer role");

			try {
				conn.setAutoCommit(true);
			} catch (SQLException e1) {
				Logger.log(Level.WARNING, "DATABASE: SQLException in switchRoleToRegularCustomer, setAutoCommit");
				System.out.println("DATABASE: SQLException in switchRoleToRegularCustomer, setAutoCommit");
			}

			return response;
		}
		response.put("update", "customer role has been switched");

		try {
			conn.setAutoCommit(true);
		} catch (SQLException e1) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in switchRoleToRegularCustomer, setAutoCommit");
			System.out.println("DATABASE: SQLException in switchRoleToRegularCustomer, setAutoCommit");
		}

		return response;
	}

	/**
	 * checkAddFeature
	 * 
	 * This method checks the input of edit/add features. This method checks if
	 * feature name include only letters and space, length no more then 15, exist
	 * feature, price include only digits, price length no mor then 3 digits. Also
	 * checks the count of features - must only 2 and optional only 3.
	 * 
	 * @param JSONObject json - includes: 'feature','price','key'(must/optional),
	 *                   'delete'(indicate if will be deletion),'map'( hash map with
	 *                   features), 'oldName'.
	 * 
	 * @return boolean - false if type is incorrect.
	 */
	private boolean checkAddFeature(JSONObject json, JSONObject response, String method) {

		String feature = Message.getValueString(json, "feature");
		String price = Message.getValueString(json, "price");
		String key = Message.getValueString(json, "key");
		boolean delete = false;
		if (method.equals("edit"))
			delete = (boolean) json.get("delete");
		HashMap<String, String> map = (HashMap<String, String>) json.get("map");
		response.put("map", map);

		response.put("feature", feature);
		response.put("price", price);
		response.put("key", key);
		if (delete) {
			map.remove(feature);
			Logger.log(Level.DEBUG, "DATABASE: delete (" + feature + ") feature is approved");
			System.out.println("DATABASE: delete (" + feature + ") feature is approved");
			response.put("feedback", feature + " delete");
			response.put("feedOK", true);
			return false;
		}
		String oldName = "";
		if (method.equals("edit"))
			oldName = Message.getValueString(json, "oldName");

		if (method.equals("add")) {

			if (key.equals("Must")) {
				if (map.size() == 2) {
					Logger.log(Level.DEBUG, "DATABASE: add (" + feature + ") feature is NOT approved");
					System.out.println("DATABASE: add (" + feature + ") feature is NOT approved");
					response.put("feedback", "Can add no more then 2 Must featurs!");
					response.put("feedOK", false);
					return false;
				}

			}

			if (key.equals("Optional")) {
				if (map.size() == 3) {
					Logger.log(Level.DEBUG, "DATABASE: add (" + feature + ") feature is NOT approved");
					System.out.println("DATABASE: add (" + feature + ") feature is NOT approved");
					response.put("feedback", "Can add no more then 3 Optional featurs!");
					response.put("feedOK", false);
					return false;
				}

			}
		}

		if (!checkLetter(feature) || !price.matches("[0-9]+")) {
			Logger.log(Level.DEBUG, "DATABASE: add (" + feature + ") feature is NOT approved");
			System.out.println("DATABASE: add (" + feature + ") feature is NOT approved");
			response.put("feedback", "Incorrect inpute!");
			response.put("feedOK", false);
			return false;
		}

		else if (feature.length() > 15) {
			Logger.log(Level.DEBUG, "DATABASE: add (" + feature + ") feature is NOT approved");
			System.out.println("DATABASE: add (" + feature + ") feature is NOT approved");
			response.put("feedback", "Only 15 letters!");
			response.put("feedOK", false);
			return false;
		} else if (price.length() > 3) {
			Logger.log(Level.DEBUG, "DATABASE: add (" + feature + ") feature is NOT approved");
			System.out.println("DATABASE: add (" + feature + ") feature is NOT approved");
			response.put("feedback", "Only 3 digits!");
			response.put("feedOK", false);
			return false;
		}
		if (!oldName.equals(feature)) { // edit price only
			if (map.containsKey(feature)) {
				Logger.log(Level.DEBUG, "DATABASE: add (" + feature + ") feature is NOT approved");
				System.out.println("DATABASE: add (" + feature + ") feature is NOT approved");
				response.put("feedback", "This feature already added!");
				response.put("feedOK", false);
				return false;
			}
		}
		map.put(feature, price);
		response.put("feedback", feature + " added");
		response.put("feedOK", true);
		response.put("map", map);
		return true;

	}

	/**
	 * immediateFeaturesCheck
	 * 
	 * This method checks the input of add features by call to checkAddFeature
	 * method.
	 * 
	 * @param JSONObject json - includes: 'feature','price','key'(must/optional),
	 *                   'delete'(indicate if will be deletion),'map'( hash map with
	 *                   features), 'oldName'.
	 * 
	 * @return JSONObject response - include data from checkAddFeature method.
	 */
	public JSONObject immediateFeaturesCheck(JSONObject json) {
		JSONObject response = new JSONObject();
		response.put("command", "update");
		response.put("update", "checkAddFeature");
		checkAddFeature(json, response, "add");
		return response;
	}

	/**
	 * immediateFeaturesEditCheck
	 * 
	 * This method checks the input of edit features by call to checkAddFeature
	 * method.
	 * 
	 * @param JSONObject json - includes: 'feature','price','key'(must/optional),
	 *                   'delete'(indicate if will be deletion),'map'( hash map with
	 *                   features), 'oldName', 'editFeat'(JSONArray with feature
	 *                   detail), 'forEditFeat'(JSONObject object that will
	 *                   presented in return map)
	 * 
	 * @return JSONObject response - include data from checkAddFeature method and
	 *         parameters from this method.
	 */
	public JSONObject immediateFeaturesEditCheck(JSONObject json) {
		JSONObject response = new JSONObject();
		JSONArray editFeat = (JSONArray) json.get("editFeat");
		JSONObject forEditFeat = (JSONObject) json.get("forEditFeat");
		String oldName = Message.getValueString(json, "oldName");
		response.put("oldName", oldName);
		response.put("feedOK", true);
		boolean delete = (boolean) json.get("delete");

		response.put("command", "update");
		response.put("update", "checkEditFeature");

		if (delete) {
			editFeat.add(forEditFeat);
		}
		if (checkAddFeature(json, response, "edit")) {
			HashMap<String, String> map = (HashMap<String, String>) response.get("map");
			editFeat.add(forEditFeat);
			map.remove(oldName);
			response.put("map", map);
		}
		response.put("editFeat", editFeat);
		return response;
	}

	/**
	 * This method resets every business customer's balance by cap set by the
	 * employer in the database
	 * 
	 * @return a JSONObject with key:<br>
	 *         "resetStatus" with value: "resetBalanceSuccess" if success<br>
	 *         "resetBalanceFail" if failed
	 */
	public JSONObject resetBalance() {
		JSONObject response = new JSONObject();
		try {
			conn.setAutoCommit(false);
			PreparedStatement pStmt = conn.prepareStatement("SELECT EmployerID, Balance FROM bitemedb.employers;");
			ResultSet rs = pStmt.executeQuery();
			while (rs.next()) {
				String employerID = rs.getString("EmployerID");
				String setBalance = rs.getString("Balance");
				pStmt = conn.prepareStatement("UPDATE customers SET Balance = ? WHERE EmployerID = ?;");
				pStmt.setString(1, setBalance);
				pStmt.setString(2, employerID);
				pStmt.executeUpdate();
			}
			conn.commit();
			conn.setAutoCommit(true);
			response.put("resetStatus", "resetBalanceSuccess");
		} catch (SQLException e) {
			System.out.println(e);
			Logger.log(Level.WARNING, "DATABASE: SQLException in resetBalance");
			System.out.println("DATABASE: SQLException in resetBalance");
			response.put("resetStatus", "resetBalanceFail");
			try {
				conn.rollback();
			} catch (SQLException e1) {
				System.out.println(e1);
				Logger.log(Level.WARNING, "DATABASE: SQLException in resetBalance");
				System.out.println("DATABASE: SQLException in resetBalance");
			}
		}
		return response;
	}

	/**
	 * getAllUsersByBranch
	 * 
	 * This method builds customers JSONArray, employers JSONArray, hrs JSONArray,
	 * suppliers JSONArray.
	 * 
	 * @param JSONObject json - includes: 'branch'.
	 * @return JSONObject - "customers" : customers JSONArray, "employers" :
	 *         employers JSONArray, "hrs" : hrs JSONArray, "suppliers" : suppliers
	 *         JSONArray. returns null if any query faild.
	 * @author Roman Milman
	 */
	public JSONObject getAllUsersByBranch(JSONObject json) {
		ResultSet rs;

		String branch = Message.getValueString(json, "branch");
		JSONObject response = new JSONObject();
		JSONArray customers = new JSONArray();
		JSONArray employers = new JSONArray();
		JSONArray hrs = new JSONArray();
		JSONArray suppliers = new JSONArray();

		try {
			customers = getCustomersForGetAllUsersByBranch(branch);
			employers = getEmployersForGetAllUsersByBranch(branch);
			hrs = getHrsForGetAllUsersByBranch(branch);
			suppliers = getSuppliersForGetAllUsersByBranch(branch);

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in getAllUsersByBranch");
			System.out.println("DATABASE: SQLException in getAllUsersByBranch");

			return null;
		}

		response.put("customers", customers);
		response.put("employers", employers);
		response.put("hrs", hrs);
		response.put("suppliers", suppliers);

		return response;
	}

	/**
	 * getCustomersForGetAllUsersByBranch
	 * 
	 * This method builds customers JSONArray with all customers that are registered
	 * in DB.
	 * 
	 * @param String branch
	 * @return JSONArray - "customers"
	 * @throws SQLException
	 * @author Roman Milman
	 */
	private JSONArray getCustomersForGetAllUsersByBranch(String branch) throws SQLException {
		ResultSet rs;
		JSONArray customers = new JSONArray();

		PreparedStatement stmt = conn.prepareStatement(
				"SELECT FirstName,LastName,role,email,phoneNumber,status,username from bitemedb.customers inner join bitemedb.users on customers.userid = users.userid where (status = 'active' or status = 'inactive' or status = 'freeze') and Branch = ?");
		stmt.setString(1, branch);

		rs = stmt.executeQuery();
		while (rs.next()) {
			JSONObject customerInfo = new JSONObject();

			customerInfo.put("name", rs.getString("FirstName") + ' ' + rs.getString("LastName"));
			customerInfo.put("phone number", rs.getString("phonenumber"));
			customerInfo.put("email", rs.getString("email"));
			customerInfo.put("status", rs.getString("status"));
			customerInfo.put("role", rs.getString("role"));
			customerInfo.put("username", rs.getString("username"));

			customers.add(customerInfo);
		}
		return customers;
	}

	/**
	 * getEmployersForGetAllUsersByBranch
	 * 
	 * This method builds employers JSONArray with all employers that are registered
	 * in DB.
	 * 
	 * @param String branch
	 * @return JSONArray - "employers"
	 * @throws SQLException
	 * @author Roman Milman
	 */
	private JSONArray getEmployersForGetAllUsersByBranch(String branch) throws SQLException {
		ResultSet rs;
		JSONArray employers = new JSONArray();

		PreparedStatement stmt = conn.prepareStatement(
				"SELECT name,email,phoneNumber,status FROM bitemedb.employers where (status = 'active' or status = 'inactive' or status = 'freeze') and Branch = ?");
		stmt.setString(1, branch);

		rs = stmt.executeQuery();
		while (rs.next()) {
			JSONObject employerInfo = new JSONObject();

			employerInfo.put("name", rs.getString("name"));
			employerInfo.put("phone number", rs.getString("phonenumber"));
			employerInfo.put("email", rs.getString("email"));
			employerInfo.put("status", rs.getString("status"));
			employerInfo.put("role", "Employer");

			employers.add(employerInfo);
		}
		return employers;
	}

	/**
	 * getHrsForGetAllUsersByBranch
	 * 
	 * This method builds hrs JSONArray with all hrs that are registered in DB.
	 * 
	 * @param String branch
	 * @return JSONArray - "hrs"
	 * @throws SQLException
	 * @author Roman Milman
	 */
	private JSONArray getHrsForGetAllUsersByBranch(String branch) throws SQLException {
		ResultSet rs;
		JSONArray hrs = new JSONArray();

		PreparedStatement stmt = conn.prepareStatement(
				"SELECT FirstName,LastName,role,email,phoneNumber,status,username from bitemedb.hr inner join bitemedb.users on hr.userid = users.userid where (status = 'active' or status = 'inactive' or status = 'freeze') and Branch = ?");
		stmt.setString(1, branch);

		rs = stmt.executeQuery();
		while (rs.next()) {
			JSONObject hrInfo = new JSONObject();

			hrInfo.put("name", rs.getString("FirstName") + ' ' + rs.getString("LastName"));
			hrInfo.put("phone number", rs.getString("phonenumber"));
			hrInfo.put("email", rs.getString("email"));
			hrInfo.put("status", rs.getString("status"));
			hrInfo.put("role", "HR");
			hrInfo.put("username", rs.getString("username"));

			hrs.add(hrInfo);
		}
		return hrs;
	}

	/**
	 * getSuppliersForGetAllUsersByBranch
	 * 
	 * This method builds suppliers JSONArray with all suppliers that are registered
	 * in DB.
	 * 
	 * @param String branch
	 * @return JSONArray - "suppliers"
	 * @throws SQLException
	 * @author Roman Milman
	 */
	private JSONArray getSuppliersForGetAllUsersByBranch(String branch) throws SQLException {
		ResultSet rs;
		JSONArray suppliers = new JSONArray();

		PreparedStatement stmt = conn.prepareStatement(
				"SELECT name,email,phoneNumber,status FROM bitemedb.suppliers where (status = 'active' or status = 'inactive' or status = 'freeze') and Branch = ?");
		stmt.setString(1, branch);

		rs = stmt.executeQuery();
		while (rs.next()) {
			JSONObject supplierInfo = new JSONObject();

			supplierInfo.put("name", rs.getString("name"));
			supplierInfo.put("phone number", rs.getString("phonenumber"));
			supplierInfo.put("email", rs.getString("email"));
			supplierInfo.put("status", rs.getString("status"));
			supplierInfo.put("role", "Supplier");

			suppliers.add(supplierInfo);
		}
		return suppliers;
	}

	/**
	 * switchStatusToCustomer
	 * 
	 * This method updates customers status to newStatus by input.
	 * 
	 * @param JSONObject json - includes: 'Phone number','Email','Name','username'.
	 * @param String     newStatus.
	 * @return JSONObject - "update" : "user status has been changed" if succeeded,
	 *         otherwise "could not change user status".
	 * @author Roman Milman
	 */
	public JSONObject switchStatusToCustomer(JSONObject json, String newStatus) {
		JSONObject response = new JSONObject();
		response.put("phone number", Message.getValueString(json, "Phone number"));
		response.put("email", Message.getValueString(json, "Email"));
		response.put("name", Message.getValueString(json, "Name"));

		try {
			PreparedStatement stmt = conn.prepareStatement(
					"update bitemedb.customers set status = ? where customers.userid = (select userid from bitemedb.users where username = ?)");
			stmt.setString(1, newStatus);
			stmt.setString(2, Message.getValueString(json, "username"));

			stmt.executeUpdate();
			// log
			Logger.log(Level.DEBUG, "DATABASE: customer (" + Message.getValueString(json, "Name")
					+ ") status changed to: " + newStatus);
			System.out.println("DATABASE: customer (" + Message.getValueString(json, "Name") + ") status changed to: "
					+ newStatus);

			response.put("update", "user status has been changed");

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in switchStatusToCustomer");
			System.out.println("DATABASE: SQLException in switchStatusToCustomer");

			response.put("update", "could not change user status");
		}

		return response;
	}

	/**
	 * switchStatusToEmployer
	 * 
	 * This method updates employers status to newStatus by input.
	 * 
	 * @param JSONObject json - includes: 'Phone number','Email','Name'.
	 * @param String     newStatus.
	 * @return JSONObject - "update" : "user status has been changed" if succeeded,
	 *         otherwise "could not change user status".
	 * @author Roman Milman
	 */
	public JSONObject switchStatusToEmployer(JSONObject json, String newStatus) {
		JSONObject response = new JSONObject();
		response.put("phone number", Message.getValueString(json, "Phone number"));
		response.put("email", Message.getValueString(json, "Email"));
		response.put("name", Message.getValueString(json, "Name"));

		try {
			PreparedStatement stmt = conn.prepareStatement("update bitemedb.employers set status = ? where name = ?");
			stmt.setString(1, newStatus);
			stmt.setString(2, Message.getValueString(json, "Name"));

			stmt.executeUpdate();
			// log
			Logger.log(Level.DEBUG, "DATABASE: employer (" + Message.getValueString(json, "Name")
					+ ") status changed to: " + newStatus);
			System.out.println("DATABASE: employer (" + Message.getValueString(json, "Name") + ") status changed to: "
					+ newStatus);

			response.put("update", "user status has been changed");

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in switchStatusToEmployer");
			System.out.println("DATABASE: SQLException in switchStatusToEmployer");

			response.put("update", "could not change user status");
		}

		return response;
	}

	/**
	 * switchStatusToHR
	 * 
	 * This method updates HR status to newStatus by input.
	 * 
	 * @param JSONObject json - includes: 'Phone number','Email','Name','username'.
	 * @param String     newStatus.
	 * @return JSONObject - "update" : "user status has been changed" if succeeded,
	 *         otherwise "could not change user status".
	 * @author Roman Milman
	 */
	public JSONObject switchStatusToHR(JSONObject json, String newStatus) {
		JSONObject response = new JSONObject();
		response.put("phone number", Message.getValueString(json, "Phone number"));
		response.put("email", Message.getValueString(json, "Email"));
		response.put("name", Message.getValueString(json, "Name"));

		try {
			PreparedStatement stmt = conn.prepareStatement(
					"update bitemedb.hr set status = ? where hr.userid = (select userid from bitemedb.users where username = ?)");
			stmt.setString(1, newStatus);
			stmt.setString(2, Message.getValueString(json, "username"));

			stmt.executeUpdate();
			// log
			Logger.log(Level.DEBUG,
					"DATABASE: HR (" + Message.getValueString(json, "Name") + ") status changed to: " + newStatus);
			System.out.println(
					"DATABASE: HR (" + Message.getValueString(json, "Name") + ") status changed to: " + newStatus);

			response.put("update", "user status has been changed");

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in switchStatusToHR");
			System.out.println("DATABASE: SQLException in switchStatusToHR");

			response.put("update", "could not change user status");
		}

		return response;
	}

	/**
	 * switchStatusToSupplier
	 * 
	 * This method updates supplier status to newStatus by input.
	 * 
	 * @param JSONObject json - includes: 'Phone number','Email','Name'.
	 * @param String     newStatus.
	 * @return JSONObject - "update" : "user status has been changed" if succeeded,
	 *         otherwise "could not change user status".
	 * @author Roman Milman
	 */
	public JSONObject switchStatusToSupplier(JSONObject json, String newStatus) {
		JSONObject response = new JSONObject();
		response.put("phone number", Message.getValueString(json, "Phone number"));
		response.put("email", Message.getValueString(json, "Email"));
		response.put("name", Message.getValueString(json, "Name"));

		try {
			PreparedStatement stmt = conn.prepareStatement("update bitemedb.suppliers set status = ? where name = ?");
			stmt.setString(1, newStatus);
			stmt.setString(2, Message.getValueString(json, "Name"));

			stmt.executeUpdate();
			// log
			Logger.log(Level.DEBUG, "DATABASE: Supplier (" + Message.getValueString(json, "Name")
					+ ") status changed to: " + newStatus);
			System.out.println("DATABASE: Supplier (" + Message.getValueString(json, "Name") + ") status changed to: "
					+ newStatus);

			response.put("update", "user status has been changed");

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in switchStatusToSupplier");
			System.out.println("DATABASE: SQLException in switchStatusToSupplier");

			response.put("update", "could not change user status");
		}

		return response;
	}

}
