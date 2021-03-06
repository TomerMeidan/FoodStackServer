package controllers;

import ocsf.server.ConnectionToClient;

import serverSide.DataBase;
import common.Logger;
import common.Logger.Level;

/**
 * PortalViewControllerFactory
 * 
 * This class is the factory that creates PortalViewController interfaces
 * This class holds db, com variables.
 * ComController com - for handling communication.
 * @author Roman Milman
 */
public class PortalViewControllerFactory {

	private DataBase db;
	private ComController com;

	public PortalViewControllerFactory(DataBase db, ComController com) {
		this.db = db;
		this.com = com;
	}

	/**
	 * createPortalViewController
	 * 
	 * returns new PortalViewController interface based by portalType as input.
	 * @param String portalType
	 * @param ConnectionToClient connection
	 * @return  PortalViewController
	 * @author Roman Milman
	 */
	public PortalViewController createPortalViewController(String portalType, ConnectionToClient connection) {
		if (portalType == null)
			return null;

		switch (portalType) {
		case "login":
			return new LoginPortalViewController(db, com, connection);
		case "Branch Manager":
			return new BranchManagerPortalViewController(db, com, connection);
		case "Customer":
			return new CustomerPortalViewController(db, com, connection);
		case "Business Customer":
			return new CustomerPortalViewController(db, com, connection);
		case "Supplier":
			return new SupplierPortalViewController(db, com, connection);
		case "CEO":
			return new CEOPortalViewController(db, com, connection);
		case "HR":
			return new HRPortalViewController(db, com, connection);

		default:
			// log
			Logger.log(Level.WARNING, "PortalFactory: unknown portal type");
			System.out.println("PortalFactory: unknown portal type");
			return null;
		}
	}
}
