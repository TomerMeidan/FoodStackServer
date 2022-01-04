package serverSide;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;

import controllers.CustomerPortalViewController;
import controllers.SupplierPortalViewController;

/**
 * EventManager
 * 
 * This class is an Observer class + Singleton.
 * This class currently is unused, this might be usefull in hte future.
 * This class was originaly designed to handle Event's such as 'Make order' for suppliers.
 * Could be useful if a supplier would Subscribe for an event when log in.
 * This class is an Singleton aswell in order to have only 1 EventManager and could be view everywhere.
 * This class holds db, listeners, customerViews and supplierViews.
 * DataBase db - for handling any database usage.
 * listeners - for storing 'Events'
 * @author Roman Milman
 */
public class EventManager {
	private DataBase db = new DataBase();
	private HashMap<String, ArrayList<EventListener>> listeners = new HashMap<>();
	
	private static EventManager instance = null;

	/**
	 * getInstance
	 * 
	 * This static method returns an instance of this class.
	 * This class creates an instance if never created.
	 * @return EventManager
	 * @author Roman Milman
	 */
	public static EventManager getInstance() {
		if (instance == null)
			instance = new EventManager();
		return instance;
	}

	/**
	 * subscribe
	 * 
	 * This method subscribes an EventListener to an event and stores.
	 * @param String event
	 * @param EventListener l
	 * @author Roman Milman
	 */
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

	/**
	 * unsubscribe
	 * 
	 * This method unsubscribes an EventListener to an event.
	 * @param String event
	 * @param EventListener l
	 * @author Roman Milman
	 */
	public void unsubscribe(String event, EventListener l) {
		synchronized (listeners) {
			ArrayList<EventListener> listenerArray = listeners.get(event);

			listenerArray.remove(l);
		}
	}

	/**
	 * notify
	 * 
	 * This notify's to listeners to wake up all the events that subscribed to an event.
	 * Woke up events passed with json, as input to the event.
	 * @param String event
	 * @param JSONObject json
	 * @author Roman Milman
	 */
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
