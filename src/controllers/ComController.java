package controllers;

import java.io.IOException;

import org.json.simple.JSONObject;

import common.Logger;
import common.Logger.Level;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import common.Parser;

/**
 * ComController
 * 
 * This class is the Controller based on ECB pattern.
 * This class controls the communication with clients.
 * This class extends AbstractServer.
 * This class holds factory and mutex.
 * factory - to create PortalViewController interface's
 * mutex - is used to keep method of checkIsOnlyUser Thread save.
 * @author Roman Milman
 */
public class ComController extends AbstractServer {

	private final String CLIENT_CONTROLLER_KEY = "controller";

	private PortalViewControllerFactory factory;
	public Object mutex = new Object();

	public ComController(int port) {
		super(port);
	}

	public void setFactory(PortalViewControllerFactory factory) {
		this.factory = factory;
	}

	/**
	 * start
	 * 
	 * This method starts listening to Port for clients.
	 * @author Roman Milman
	 */
	public void start() {
		try {
			listen();
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING, "ComController: IOException exception in start");
			System.out.println("ComController: IOException exception in start");
		}
	}

	/**
	 * clientConnected
	 * 
	 * This Hook method is called when a client successfully connects.
	 * Default PortalViewController is LoginPortalViewController
	 * @param ConnectionToClient client
	 * @author Roman Milman
	 */
	@Override
	protected void clientConnected(ConnectionToClient client) {
		PortalViewController c = factory.createPortalViewController("login", client);
		client.setInfo(CLIENT_CONTROLLER_KEY, c);

		// log
		Logger.log(Level.DEBUG, "ComController: client : " + client.toString() + " connected");
		System.out.println("ComController: client : " + client.toString() + " connected");
	}

	/**
	 * handleMessageFromClient
	 * 
	 * This Hook method is called when a message received from a client.
	 * This method decodes the message and sends to its PortalViewController to handle.
	 * @param ConnectionToClient client
	 * @param Object msg
	 * @author Roman Milman
	 */
	@Override
	protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
		PortalViewController c = (PortalViewController) client.getInfo(CLIENT_CONTROLLER_KEY);
		JSONObject j = Parser.decode(msg);
		c.handleCommandFromClient(j);
	}

	/**
	 * clientException
	 * 
	 * This Hook method is called when a client disconnects.
	 * @param ConnectionToClient client
	 * @param Throwable exception
	 * @author Roman Milman
	 */
	@Override
	protected synchronized void clientException(ConnectionToClient client, Throwable exception) {
		Logger.log(Level.WARNING, "ComController: client exception");
		System.out.println("ComController: client exception");
	}

	/**
	 * switchPortal
	 * 
	 * This method switches clients portal view by given portalType as input.
	 * @param ConnectionToClient client
	 * @param String portalType
	 * @author Roman Milman
	 */
	protected void switchPortal(ConnectionToClient client, String portalType) {
		String id = "";

		PortalViewController newController = factory.createPortalViewController(portalType, client);
		PortalViewController oldController = (PortalViewController) client.getInfo(CLIENT_CONTROLLER_KEY);

		if (!(newController instanceof LoginPortalViewController))
			id = oldController.getID();

		synchronized (mutex) {
			newController.setID(id);
			client.setInfo(CLIENT_CONTROLLER_KEY, newController);
		}
	}

	/**
	 * isUserOnline
	 * 
	 * This method checks if user is already online, userID given as input.
	 * @param String id
	 * @author Roman Milman
	 */
	// returning from a synchronized block releases the lock
	public boolean isUserOnline(String id) {
		synchronized (mutex) {

			Thread[] allClients = getClientConnections();

			for (Thread client : allClients) {
				ConnectionToClient c = (ConnectionToClient) client;
				PortalViewController portalViewController = (PortalViewController) c.getInfo(CLIENT_CONTROLLER_KEY);
				if (id.equals(portalViewController.getID()))
					return true;
			}
			return false;
		}
	}
	
	/**
	 * findConnection
	 * 
	 * This method searches for a user by userID that is given as input.
	 * This method returns client connection if found, otherwise returns null.
	 * @param String id
	 * @return ConnectionToClient 
	 * @author Roman Milman
	 */
	public ConnectionToClient findConnection(String id) {
		synchronized (mutex) {

			Thread[] allClients = getClientConnections();

			for (Thread client : allClients) {
				ConnectionToClient c = (ConnectionToClient) client;
				PortalViewController portalViewController = (PortalViewController) c.getInfo(CLIENT_CONTROLLER_KEY);
				if (id.equals(portalViewController.getID()))
					return c;
			}
			return null;
		}
	}

}
