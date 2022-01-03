package controllers;

import org.json.simple.JSONObject;

/**
 * PortalViewController interface
 * 
 * This interface is the base for all portal view controllers.
 * @author Roman Milman
 */
public interface PortalViewController {
	public void start();

	public void stop();

	public void handleCommandFromClient(JSONObject json);

	public String getID();

	public void setID(String ID);
}
