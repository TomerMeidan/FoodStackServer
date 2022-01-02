package controllers;

import org.json.simple.JSONObject;

public interface PortalViewController {
	public void start();

	public void stop();

	public void handleCommandFromClient(JSONObject json);

	public String getID();

	public void setID(String ID);
}
