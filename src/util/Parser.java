package util;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Parser {
	public static String encode(JSONObject json) {
		String ret = JSONValue.toJSONString(json);
		ret += '\n';
		return ret;
	}
	
	/*
	 * decode function converts String to JSONObject.
	 * assuming str is the message received from server and was built with Parser.code()
	 * */
	public static JSONObject decode(Object str) {
		if (!(str instanceof String)) {
			// TODO maybe throw exception instead of returning null
			return null;
		}

		String msg = (String) str;

		StringBuilder string = new StringBuilder(msg);
		string.deleteCharAt(msg.length() - 1);
		JSONObject jsonObj = (JSONObject) JSONValue.parse(string.toString());
		return jsonObj;
	}
}
