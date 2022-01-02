package util;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * class to help in writing cleaner code by: casting, creating messages by
 * appending various strings, etc
 * 
 * @author mosa
 */
public abstract class Message {

	/**
	 * if no such key exists, return empty string
	 * 
	 * @param ob - the mapped JSON
	 * @param key - the key whose value you're searching for
	 * @return the value of the key cast as String
	 */
	public static String getValueString(JSONObject ob, String key) {
		if (ob == null || !(ob.containsKey(key)))
			return "";
		return (String) ob.get(key);
	}

	/**
	 * if no such key exists, return -1
	 * 
	 * @param ob - the mapped JSON
	 * @param key - the key whose value you're searching for
	 * @return the value of the key cast as Long
	 */
	public static Long getValueLong(JSONObject ob, String key) {
		if (ob == null || !(ob.containsKey(key)))
			return (long) -1;
		return (Long) ob.get(key);
	}

	/**
	 * if no such key exists, return -1
	 * 
	 * @param ob - the mapped JSON
	 * @param key - the key whose value you're searching for
	 * @return the value of the key cast as Double
	 */
	public static Double getValueDouble(JSONObject ob, String key) {
		if (ob == null || !(ob.containsKey(key)))
			return null;
		return (Double) ob.get(key);
	}

	/**
	 * if no such key exists, return null
	 * 
	 * @param ob - the mapped JSON
	 * @param key - the key whose value you're searching for
	 * @return the value of the key cast as JSONArray
	 */
	public static JSONArray getValueJArray(JSONObject ob, String key) {
		if (ob == null || !(ob.containsKey(key)))
			return null;

		return (JSONArray) ob.get(key);
	}

	/**
	 * if no such key exists, return null
	 * 
	 * @param ob - the mapped JSON
	 * @param key - the key whose value you're searching for
	 * @return the value of the key cast as JSONObject
	 */
	public static JSONObject getValueJObject(JSONObject ob, String key) {
		if (ob == null || !(ob.containsKey(key)))
			return null;
		return (JSONObject) ob.get(key);
	}

	/**
	 * @param ob  - the mapped JSON
	 * @param msg - the message you want to display
	 * @param key - the key whose value you're searching for
	 * @return a String constructed by appending msg with the key
	 */
	public static String createMessage(JSONObject ob, String msg, String key) {
		StringBuilder sb = new StringBuilder(msg);
		sb.append(getValueString(ob, key));
		return sb.toString();
	}

	public static ArrayList<String> getValues(JSONArray ob, String msg) {
		ArrayList<String> ret = new ArrayList<>();
		for (int i = 0; i < ob.size(); i++) {
			ret.add(getValueString((JSONObject) ob.get(i), msg));
		}
		return ret;
	}

}
