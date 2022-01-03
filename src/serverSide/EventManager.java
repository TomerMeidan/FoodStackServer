package serverSide;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;

import controllers.CustomerPortalViewController;
import controllers.SupplierPortalViewController;

public class EventManager {
	private DataBase db = new DataBase();
	private HashMap<String, ArrayList<EventListener>> listeners = new HashMap<>();
	private HashMap<String, CustomerPortalViewController> customerViews = new HashMap<>();
	private HashMap<String, SupplierPortalViewController> supplierViews = new HashMap<>();
	
	private static EventManager instance = null;

	public static EventManager getInstance() {
		if (instance == null)
			instance = new EventManager();
		return instance;
	}

	public void subscribe(String event, EventListener l) {
		synchronized (listeners) {
			ArrayList<EventListener> listenerArray = listeners.get(event);

			if (listenerArray == null) {
				listenerArray = new ArrayList<EventListener>();
				listeners.put(event, listenerArray);
			}
			listenerArray.add(l);
		}
	}

	public void unsubscribe(String event, EventListener l) {
		synchronized (listeners) {
			ArrayList<EventListener> listenerArray = listeners.get(event);

			listenerArray.remove(l);
		}
	}

	public void notify(String event, JSONObject json) {
		synchronized (listeners) {
			ArrayList<EventListener> listenerArray = listeners.get(event);

			if (listenerArray == null)
				return;

			for (EventListener l : listenerArray) {
				l.HandleEvent(json);
			}
		}
	}

	public JSONObject orderIsReady(JSONObject json) {
		JSONObject respone = db.approveOrder(json);
		return respone;
	}
}