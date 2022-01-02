package util;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * class to make working with Meals(items) more understandable and easier to
 * follow.
 * <p>
 * This class contains a method to convert a Meal to a JSONObject.<br>
 * and another method to convert a JSONObject to Meal.<br>
 * These methods are here to so we can convert easily and then send from server
 * to client and viceversa
 * 
 * @author mosa
 *
 */
@SuppressWarnings("unchecked")
public class Meal {
	private String name;
	private String price;
	private String id;
	private MustFeature mustFeature;
	private ArrayList<OptionalFeature> optionalFeatureList = null;
	private String typeOfMeal;

	public Meal(String name, String price, MustFeature mustFeature, ArrayList<OptionalFeature> optionalFeatureList) {
		this.name = name;
		this.price = price;
		this.mustFeature = mustFeature;
		this.optionalFeatureList = optionalFeatureList;
	}

	public Meal() {

	}

	public void setType(String typeOfMeal) {
		this.typeOfMeal = typeOfMeal;
		
	}
	
	public String getType() {
		return typeOfMeal;
	}
	public void setID(String id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public void setMustFeature(MustFeature mustFeature) {
		this.mustFeature = mustFeature;
	}

	public void addToOptionalFeatureList(OptionalFeature optionalFeature) {
		if (optionalFeatureList == null)
			optionalFeatureList = new ArrayList<>();
		optionalFeatureList.add(optionalFeature);
	}

	public String getName() {
		return name;
	}

	public String getPrice() {
		return price;
	}

	public MustFeature getMustFeature() {
		return mustFeature;
	}

	public ArrayList<OptionalFeature> getOptionalFeatureList() {
		return optionalFeatureList;
	}

	/**
	 * generate a Meal to a JSONObject
	 * <p>
	 * keys: "mustFeatureName", "mustFeaturePrice", "mealName", "mealPrice",
	 * "optionalJArray".<br>
	 * 
	 * @return JSONObject containing the information of the Meal
	 */
	public JSONObject toJSONObject() {
		JSONObject meal = new JSONObject();
		JSONArray optionalJArray = new JSONArray();
		if (mustFeature != null) {
			meal.put("mustFeatureName", mustFeature.getName());
			meal.put("mustFeaturePrice", mustFeature.getPrice());
			meal.put("mustFeatureID", mustFeature.getID());
		}
		meal.put("mealName", name);
		meal.put("mealPrice", price);
		meal.put("mealID", id);
		for (OptionalFeature o : optionalFeatureList) {
			JSONObject optional = new JSONObject();
			if (o != null) {
				optional.put("optionalFeatureName", o.getName());
				optional.put("optionalFeaturePrice", o.getPrice());
				optional.put("optionalFeatureID", o.getID());
				optionalJArray.add(optional);
			}
		}
		meal.put("optionalJArray", optionalJArray);
		return meal;
	}

	/**
	 * static method to generate a Meal object from JSONObject
	 * <p>
	 * recommended to use only with toJSONObject (don't create a JSONObject without
	 * using toJSONObject!)
	 * <p>
	 * If you still want to use without toJSONObject, make sure to include correct
	 * syntax:<br>
	 * keys: "mustFeatureName", "mustFeaturePrice", "mealName", "mealPrice",
	 * "optionalFeatureList".<br>
	 * optionalFeatureList keys: "optionalFeatureName", "optionalFeaturePrice"
	 * 
	 * @param json
	 * @return
	 */
	public static Meal fromJSONObject(JSONObject json) {
		ArrayList<OptionalFeature> optionalList = new ArrayList<>();
		JSONArray optionalJArray = Message.getValueJArray(json, "optionalJArray");
		if (optionalJArray != null) {
			for (int i = 0; i < optionalJArray.size(); i++) {
				JSONObject optionalJ = (JSONObject) optionalJArray.get(i);
				OptionalFeature optional = new OptionalFeature(
						Message.getValueLong(optionalJ, "optionalFeaturePrice").intValue(),
						Message.getValueString(optionalJ, "optionalFeatureName"),
						Message.getValueString(optionalJ, "optionalFeatureID"));
				optionalList.add(optional);
			}
		}
		Meal retMeal = new Meal(Message.getValueString(json, "mealName"), Message.getValueString(json, "mealPrice"),
				new MustFeature(Message.getValueLong(json, "mustFeaturePrice").intValue(),
						Message.getValueString(json, "mustFeatureName"), Message.getValueString(json, "mustFeatureID")),
				optionalList);
		retMeal.setID(Message.getValueString(json, "mealID"));
		retMeal.setType(Message.getValueString(json, "mealType"));
		return retMeal;
	}
}
