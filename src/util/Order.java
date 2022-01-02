package util;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * class to make working with Orders more understandable and easier to follow.
 * <p>
 * This class contains a method to convert am Order to a JSONObject.<br>
 * and another method to convert a JSONObject to Order.<br>
 * These methods are here to so we can convert easily and then send from server
 * to client and viceversa
 * 
 * @author mosa
 *
 */
@SuppressWarnings("unchecked")
public class Order {
	private String dueTime;
	private String orderTime;
	private String restaurantName;
	private Long orderID;
	private String status;
	private int total;
	private String earlyBooking;
	private String pickUpType;
	private String address;
	private String phoneNumber;
	private String supplierID;
	private ArrayList<Meal> mealsList;

	public Order(Long orderID, String orderTime, String dueTime, String restaurantName, String status, int total,
			String pickUpType, String earlyBooking, String address, String phoneNumber, String supplierID) {
		this.orderID = orderID;
		this.orderTime = orderTime;
		this.dueTime = dueTime;
		this.restaurantName = restaurantName;
		this.status = status;
		this.total = total;
		this.pickUpType = pickUpType;
		this.earlyBooking = earlyBooking;
		this.phoneNumber = phoneNumber;
		this.address = address;
		this.supplierID = supplierID;
	}

	public int getMealsListCost() {
		int total = 0;
		for (Meal meal : mealsList) {
			total += getMealCost(meal);
		}
		return total;
	}

	public int getMealCost(Meal meal) {
		int total = 0;
		if (meal.getMustFeature() != null && meal.getMustFeature().getName() != null) {
			int mustFeaturePrice = meal.getMustFeature().getPrice();
			if(mustFeaturePrice != -1)
				total += mustFeaturePrice;
		}

		ArrayList<OptionalFeature> optionalArray = meal.getOptionalFeatureList();
		if (optionalArray != null) {
			for (OptionalFeature op : optionalArray)
				if (op.getName() != null)
					total += op.getPrice();
		}
		total += Double.valueOf(meal.getPrice());
		return total;
	}

	/**
	 * convert an Order to a JSONObject
	 * <p>
	 * keys: "orderID", "orderDate", "dueDate", "restaurantName", "totalCost"
	 * "pickUpType", "earlyBooking", "address", "phoneNumber", "status",
	 * "supplierID", "mealsJArray".<br>
	 * 
	 * @return JSONObject containing the information of the Meal
	 */
	public JSONObject intoJSONObject() {
		JSONObject orderJ = new JSONObject();
		JSONArray mealsJArray = new JSONArray();
		orderJ.put("orderID", orderID);
		orderJ.put("orderDate", orderTime);
		orderJ.put("dueDate", dueTime);
		orderJ.put("restaurantName", restaurantName);
		orderJ.put("totalCost", total);
		orderJ.put("pickUpType", pickUpType);
		orderJ.put("earlyBooking", earlyBooking);
		orderJ.put("address", address);
		orderJ.put("phoneNumber", phoneNumber);
		orderJ.put("status", status);
		orderJ.put("supplierID", supplierID);
		if (mealsList != null)
			for (Meal meal : mealsList)
				mealsJArray.add(meal.toJSONObject());
		orderJ.put("mealsJArray", mealsJArray);
		return orderJ;
	}

	/**
	 * static method to generate an Order object from JSONObject
	 * <p>
	 * recommended to use only with toJSONObject (don't create a JSONObject without
	 * using toJSONObject!)
	 * <p>
	 * If you still want to use without toJSONObject, make sure to include correct
	 * syntax:<br>
	 * * keys: "orderID", "orderDate", "dueDate", "restaurantName", "totalCost"
	 * "pickUpType", "earlyBooking", "address", "phoneNumber", "status",
	 * "supplierID", "mealsJArray".<br>
	 * 
	 * @param json
	 * @return
	 */
	public static Order fromJSONObject(JSONObject orderJ) {
		ArrayList<Meal> mealsList = new ArrayList<>();
		JSONArray mealsJArray = Message.getValueJArray(orderJ, "mealsJArray");
		Order order = new Order(Message.getValueLong(orderJ, "orderID"), Message.getValueString(orderJ, "orderDate"),
				Message.getValueString(orderJ, "dueDate"), Message.getValueString(orderJ, "restaurantName"),
				Message.getValueString(orderJ, "status"), (Message.getValueLong(orderJ, "totalCost")).intValue(),
				Message.getValueString(orderJ, "pickUpType"), Message.getValueString(orderJ, "earlyBooking"),
				Message.getValueString(orderJ, "address"), Message.getValueString(orderJ, "phoneNumber"),
				Message.getValueString(orderJ, "supplierID"));
		for (int i = 0; i < mealsJArray.size(); i++) {
			Meal meal = Meal.fromJSONObject((JSONObject) mealsJArray.get(i));
			mealsList.add(meal);
		}
		order.addMeals(mealsList);
		return order;
	}

	public boolean addMeal(Meal meal) {
		if (mealsList == null)
			mealsList = new ArrayList<>();
		return mealsList.add(meal);
	}

	public boolean addMeals(ArrayList<Meal> meals) {
		if (mealsList == null)
			mealsList = new ArrayList<>();
		return mealsList.addAll(meals);
	}

	public boolean checkEarlyBooking() {
		return earlyBooking.equals("True");
	}

	public String getPickUpType() {
		return pickUpType;
	}

	public String getSupplierID() {
		return supplierID;
	}

	public String getDueTime() {
		return dueTime;
	}

	public String getOrderTime() {
		return orderTime;
	}

	public String getRestaurantName() {
		return restaurantName;
	}

	public Long getOrderID() {
		return orderID;
	}

	public String getStatus() {
		return status;
	}

	public int getTotal() {
		return total;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public ArrayList<Meal> getMeals() {
		return mealsList;
	}

	public int getPickUpCost() {
		if (pickUpType == null) {
			return 0;
		}
		switch (pickUpType) {
		case "Delivery":
			return 25;
		case "Self Pickup":
			return 0;
		case "Robot":
			return 0;
		default:
			return 0;
		}
	}
}
