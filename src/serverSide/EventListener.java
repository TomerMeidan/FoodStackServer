package serverSide;

import org.json.simple.JSONObject;

public interface EventListener {

	public void HandleEvent(JSONObject event);
}
