package serverSide;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.simple.JSONObject;

import common.Logger;
import common.Message;
import common.Logger.Level;

public class DBImport {
	@SuppressWarnings("unchecked")
	private static Connection conn;
	private static Connection connImport;
	
	public DBImport(Connection conn,Connection connImport ) {
		this.conn = conn;
		this.connImport = connImport;
	}
	
	public void importAll()
	{
		resetUsers();
		resetEmployer();
		resetHR();
		resetBM();
		resetSupplier();
		resetCustomer();
		resetCEO();
		
	}
	
	
	private void resetUsers() {
		ResultSet rs;
		Statement stmtIm;
		PreparedStatement stmtBM;

		try {
			stmtIm = connImport.createStatement();
			rs = stmtIm.executeQuery("SELECT * FROM import_users.users WHERE LastName IS NOT NULL"); // without
																										// employers
			stmtBM = conn.prepareStatement(
					"INSERT INTO bitemedb.USERS (USERNAME,PASSWORD,ROLE,BRANCH,FIRSTNAME,LASTNAME, USERID) VALUES(?,?,?,?,?,?,?)");
			while (rs.next()) {
				stmtBM.setString(1, rs.getString("UserName"));
				stmtBM.setString(2, rs.getString("Password"));
				stmtBM.setString(3, rs.getString("Role"));
				stmtBM.setString(4, rs.getString("Branch"));
				stmtBM.setString(5, rs.getString("FirstName"));
				stmtBM.setString(6, rs.getString("LastName"));
				stmtBM.setString(7, rs.getString("UserID"));
				stmtBM.executeUpdate();
			}

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}
		Logger.log(Level.DEBUG, "DataBase : import users succeed");
		System.out.println("DataBase : import users succeed");
	}

	@SuppressWarnings("unchecked")
	private void resetEmployer() {
		ResultSet rs;
		Statement stmtIm;
		PreparedStatement stmtBM;

		try {
			stmtIm = connImport.createStatement();
			rs = stmtIm.executeQuery("SELECT * FROM import_users.users WHERE Role = 'employer' ");
			stmtBM = conn.prepareStatement(
					"INSERT INTO bitemedb.employers (Name, EmployerID, Credit, PhoneNumber, Email, Branch) VALUES(?,?,?,?,?,?)");
			while (rs.next()) {
				stmtBM.setString(1, rs.getString("BusinessName"));
				stmtBM.setString(2, rs.getString("UserID"));
				stmtBM.setInt(3, rs.getInt("Credit"));
				stmtBM.setString(4, rs.getString("Phone"));
				stmtBM.setString(5, rs.getString("Email"));
				stmtBM.setString(6, rs.getString("Branch"));
				stmtBM.executeUpdate();
			}

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}
		Logger.log(Level.DEBUG, "DataBase : import employers succeed");
		System.out.println("DataBase : import employers succeed");
	}

	@SuppressWarnings("unchecked")
	private void resetHR() {
		ResultSet rs;
		Statement stmtIm;
		PreparedStatement stmtBM;

		try {
			stmtIm = connImport.createStatement();
			rs = stmtIm.executeQuery("SELECT * FROM import_users.users WHERE Role = 'HR' ");
			stmtBM = conn.prepareStatement(
					"INSERT INTO bitemedb.hr (EmployerID, ID, PhoneNumber, Email, UserID, Status) VALUES(?,?,?,?,?,?)");

			while (rs.next()) {
				stmtBM.setInt(1, rs.getInt("EmployerID"));
				stmtBM.setInt(2, rs.getInt("ID"));
				stmtBM.setString(3, rs.getString("Phone"));
				stmtBM.setString(4, rs.getString("Email"));
				stmtBM.setString(5,  rs.getString("UserID"));
				stmtBM.setString(6,  "active");
				stmtBM.executeUpdate();
			}

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}
		Logger.log(Level.DEBUG, "DataBase : import HR succeed");
		System.out.println("DataBase : import HR succeed");
	}

	@SuppressWarnings("unchecked")
	private void resetBM() {
		ResultSet rs;
		Statement stmtIm;
		PreparedStatement stmtBM;

		try {
			stmtIm = connImport.createStatement();
			rs = stmtIm.executeQuery("SELECT * FROM import_users.users WHERE Role = 'Branch Manager' ");
			stmtBM = conn.prepareStatement(
					"INSERT INTO bitemedb.branchmanager (Branch, FirstName, LastName, IDNumber, PhoneNumber, Email, UserID) VALUES(?,?,?,?,?,?,?)");

			while (rs.next()) {
				stmtBM.setString(1, rs.getString("Branch"));
				stmtBM.setString(2, rs.getString("FirstName"));
				stmtBM.setString(3, rs.getString("LastName"));
				stmtBM.setInt(4, rs.getInt("ID"));
				stmtBM.setString(5, rs.getString("Phone"));
				stmtBM.setString(6, rs.getString("Email"));
				stmtBM.setString(7, rs.getString("UserID"));
				stmtBM.executeUpdate();
			}

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}
		Logger.log(Level.DEBUG, "DataBase : import BM succeed");
		System.out.println("DataBase : import BM succeed");
	}
	
	@SuppressWarnings("unchecked")
	private void resetSupplier() {
		ResultSet rs;
		Statement stmtIm;
		PreparedStatement stmtBM;

		try {
			stmtIm = connImport.createStatement();
			rs = stmtIm.executeQuery("SELECT * FROM import_users.users WHERE Role = 'Supplier' ");
			stmtBM = conn.prepareStatement(
					"INSERT INTO bitemedb.suppliers (Branch, PhoneNumber, Email, UserID, Name) VALUES(?,?,?,?,?)");
			while (rs.next()) {
				stmtBM.setString(1, rs.getString("Branch"));
				stmtBM.setString(2, rs.getString("Phone"));
				stmtBM.setString(3, rs.getString("Email"));
				stmtBM.setString(4, rs.getString("UserID"));
				stmtBM.setString(5, rs.getString("BusinessName"));
				stmtBM.executeUpdate();
			}

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}
		Logger.log(Level.DEBUG, "DataBase : import Supplier succeed");
		System.out.println("DataBase : import Supplier succeed");
	}
	
	@SuppressWarnings("unchecked")
	private void resetCustomer() {
		ResultSet rs;
		Statement stmtIm;
		PreparedStatement stmtBM;

		try {
			stmtIm = connImport.createStatement();
			rs = stmtIm.executeQuery("SELECT * FROM import_users.users WHERE Role IS NULL");
			stmtBM = conn.prepareStatement(
					"INSERT INTO bitemedb.customers (ID, PhoneNumber, Email, CreditNumber, UserID, Address) VALUES(?,?,?,?,?,?)");
			while (rs.next()) {
				stmtBM.setInt(1, rs.getInt("ID"));
				stmtBM.setString(2, rs.getString("Phone"));
				stmtBM.setString(3, rs.getString("Email"));
				stmtBM.setInt(4,  rs.getInt("Credit"));
				stmtBM.setString(5, rs.getString("UserID"));
				stmtBM.setString(6, rs.getString("Address"));
				stmtBM.executeUpdate();
			}

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}
		Logger.log(Level.DEBUG, "DataBase : import Customer succeed");
		System.out.println("DataBase : import Customer succeed");
	}
	
	@SuppressWarnings("unchecked")
	private void resetCEO() {
		ResultSet rs;
		Statement stmtIm;
		PreparedStatement stmtBM;

		try {
			stmtIm = connImport.createStatement();
			rs = stmtIm.executeQuery("SELECT * FROM import_users.users WHERE Role = 'CEO'");
			stmtBM = conn.prepareStatement(
					"INSERT INTO bitemedb.ceo (FirstName, LastName, IDNumber, PhoneNumber, Email, UserID) VALUES(?,?,?,?,?,?)");
			while (rs.next()) {
				stmtBM.setString(1, rs.getString("FirstName"));
				stmtBM.setString(2, rs.getString("LastName"));
				stmtBM.setInt(3, rs.getInt("ID"));
				stmtBM.setString(4, rs.getString("Phone"));
				stmtBM.setString(5, rs.getString("Email"));
				stmtBM.setString(6, rs.getString("UserID"));
				stmtBM.executeUpdate();
			}

		} catch (SQLException e) {
			Logger.log(Level.WARNING, "DATABASE: SQLException in approveUser");
		}
		Logger.log(Level.DEBUG, "DataBase : import CEO succeed");
		System.out.println("DataBase : import CEO succeed");
	}
	
	

}
