package serverSide;

import java.sql.SQLException;

import javafx.application.Application;
import javafx.stage.Stage;
import common.Logger;
import common.Logger.Level;
import controllers.ComController;
import controllers.PortalViewControllerFactory;

/**
 * BMServer
 * 
 * Main class
 * 
 * @author Daniel Ohayon
 */
public class BMServer extends Application {
	final public static int DEFAULT_PORT = 5555;
	final public static String DEFAULT_USER = "root";

	final public static String DEFAULT_PASSWORD = "Dan11111";

	private static DataBase db = new DataBase();
	private static ComController com;
	private static PortalViewControllerFactory factory;
	private OrderManager orderMngr = new OrderManager(db);
	private static PeriodicActivityService periodicSrvc = new PeriodicActivityService();

	public static void main(String args[]) throws Exception {
		Logger.init();
		Logger.setLevel(Level.DEBUG);

		launch(args);

		System.exit(0);
	} // end main

	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub
		BMServerWindow aFrame = new BMServerWindow();
		aFrame.start(primaryStage);
	}

	public static void runServer(String p) {
		try {
			com = new ComController(Integer.valueOf(p));
			factory = new PortalViewControllerFactory(db, com);

			db.start();
		} catch (SQLException e) {
			// log
			Logger.log(Level.DEBUG, "BMServer : SQLException was caught");
			System.out.println("BMServer : SQLException was caught");

			Logger.log(Level.WARNING, "SQLException: " + e.getMessage());
			Logger.log(Level.WARNING, "SQLState: " + e.getSQLState());
			Logger.log(Level.WARNING, "VendorError: " + e.getErrorCode());

			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());

			System.exit(-1);
		}

		com.setFactory(factory);
		com.start();

		periodicSrvc.start();

	}

	public static void stopServer() {
		System.exit(1);
	}

	public static void sendPassword(String password) {
		db.setPassword(password);
	}

	public static void sendUser(String user) {
		db.setUser(user);
	}
}
