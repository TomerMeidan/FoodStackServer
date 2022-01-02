package controllers;

import java.io.IOException;

import org.json.simple.JSONObject;

import common.Logger;
import common.Logger.Level;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import common.Parser;

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

	public void start() {
		try {
			listen();
		} catch (IOException e) {
			// log
			Logger.log(Level.WARNING, "ComController: IOException exception in start");
			System.out.println("ComController: IOException exception in start");
		}
	}

	@Override
	protected void clientConnected(ConnectionToClient client) {
		PortalViewController c = factory.createPortalViewController("login", client);
		client.setInfo(CLIENT_CONTROLLER_KEY, c);

		// log
		Logger.log(Level.DEBUG, "ComController: client : " + client.toString() + " connected");
		System.out.println("ComController: client : " + client.toString() + " connected");
	}

	@Override
	protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
		PortalViewController c = (PortalViewController) client.getInfo(CLIENT_CONTROLLER_KEY);
		JSONObject j = Parser.decode(msg);
		c.handleCommandFromClient(j);
	}

	@Override
	protected synchronized void clientException(ConnectionToClient client, Throwable exception) {
		Logger.log(Level.WARNING, "ComController: client exception");
		System.out.println("ComController: client exception");
	}

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
