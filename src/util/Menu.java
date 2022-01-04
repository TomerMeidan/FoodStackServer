package util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import common.Message;
@SuppressWarnings("unchecked")
public class Menu {
	private JSONObject menu;
	private JSONObject menu1;
	private Statement stmt;
	
	public Menu(Statement stmt) {
		this.menu = new JSONObject();
		this.menu1 = new JSONObject();
		this.stmt = stmt;
	}
	
	/**Builds a JSONObject that contains all the types/meals/features that exist in a restauraunt
	 * @param selectedRestauraunt
	 */
	public void buildMenu(String selectedRestaurant) {
		System.out.println("Menu: buildMenu: Building menu for "+selectedRestaurant);
		menu.put("restaurantName", selectedRestaurant);
		JSONArray typesOfItemsArray = getTypesOfItems();
		for(int i = 0; i<typesOfItemsArray.size();i++) {
			JSONObject temp = (JSONObject)typesOfItemsArray.get(i);
			String s = temp.keySet().toString(); //get the key of the JSON (I.E - [Salad]
			String itemType = s.substring(1, s.length()-1); //remove [] and get Salad
			//JSONArray itemsArray = new JSONArray();
			JSONArray itemsArray = (JSONArray) temp.get(itemType);
			itemsArray.addAll(getItemsByType(itemType));
			for(int j =1; j<itemsArray.size();j++) {
				JSONObject item = (JSONObject)itemsArray.get(j);
				String itemID = Message.getValueString(item, "mealID");
				JSONArray mustFeatureJArray = getItemMustFeatureList(itemID);
				JSONArray optionalFeatureJArray = getItemOptionalFeatureList(itemID);
				if(mustFeatureJArray.isEmpty()) {
					//Logger.log(Level.WARNING, "ItemMustFeatureList is empty for itemID = "+itemID);
					System.out.println("Menu: ItemMustFeatureList is empty for ItemID = "+itemID);
				}
				else item.put("mustFeatureJArray", mustFeatureJArray);
				
				if(optionalFeatureJArray.isEmpty()) {
					//Logger.log(Level.WARNING, "ItemMustFeatureList is empty for itemID = "+itemID);
					System.out.println("Menu class: ItemMustFeatureList is empty for ItemID = "+itemID);
				}
				else item.put("optionalFeatureJArray", optionalFeatureJArray);
				
			}
			((JSONObject)typesOfItemsArray.get(i)).put(itemType, itemsArray);
			menu1.put(itemType, itemsArray);
		}
		menu.put("menu", menu1);
		System.out.println("Menu: buildMenu: Finished menu for "+selectedRestaurant);
	}
	
	/**Get the different types of items available in the restaurant
	 * @return JSONArray, where every JSONObject in it has keys:<p>
	 * "imgType", value String (path to Image for the type)<br>
	 * "itemType", value JSONArray (itemType = Salad, Pizza, etc..)
	 */
	public JSONArray getTypesOfItems() {
		JSONArray itemTypesArray = new JSONArray();
		try { 
			String restaurantName = Message.getValueString(menu, "restaurantName");
			ResultSet rs = stmt.executeQuery(
					"SELECT DISTINCT ItemType, ImgType FROM items INNER JOIN suppliers ON suppliers.UserID = items.UserID AND suppliers.Name ='"
							+ restaurantName + "'");
			while(rs.next()) { 
				JSONObject itemType = new JSONObject();	
				JSONObject imgType = new JSONObject();
				JSONArray arrayForType = new JSONArray();
				imgType.put("imgType", rs.getString("ImgType"));
				arrayForType.add(imgType);
				itemType.put(rs.getString("ItemType"), arrayForType);
				itemTypesArray.add(itemType);
			}
		} catch (SQLException e) {
			System.out.println("Menu class: SQLException in getTypes");
		}
		return itemTypesArray;
	}
	
	/**Get the different items of a given itemType available in the restaurant
	 * @param itemType
	 * @return A JSONArray where every JSONObject contains keys:<p>
	 * "mealName", "mealPrice", "mealID", "imgMeal" (path to image of the meal)
	 */
	public JSONArray getItemsByType(String itemType) {
		JSONArray itemsArray = new JSONArray();	
		try { 
			String restaurantName = Message.getValueString(menu, "restaurantName");
			ResultSet rs = stmt.executeQuery(
					"SELECT ItemName, ItemPrice, ItemID,imgMeal FROM items INNER JOIN suppliers ON suppliers.UserID = items.UserID AND suppliers.Name ='"
							+ restaurantName + "' WHERE ItemType ='"+itemType+"'");
			while(rs.next()) { 
				JSONObject item = new JSONObject();
				item.put("mealName", rs.getString("ItemName"));
				item.put("mealPrice", rs.getString("ItemPrice"));
				item.put("mealID", rs.getString("ItemID"));
				item.put("imgMeal", rs.getString("imgMeal"));
				itemsArray.add(item);
			}		
		} catch (SQLException e) {
			System.out.println("Menu: SQLException in method getItemsByType");
		}
		return itemsArray;
	}


	/**For a given itemID, find the available Must Features and the prices
	 * @param itemID
	 * @return A JSONArray where every JSONObject contains keys:<p>
	 * "mustFeatureName", "mustFeatureID", "mustFeaturePrice"
	 */
	public JSONArray getItemMustFeatureList(String itemID) {
		JSONArray mustFeatureArray = new JSONArray();	
		try { 
			ResultSet rs = stmt.executeQuery(
					"Select MustFeature, MustFeatureID, MustPrice FROM mustfeatures WHERE ItemID ="+itemID);
			while(rs.next()) { 
				JSONObject mustFeature = new JSONObject();
				mustFeature.put("mustFeatureName", rs.getString("MustFeature"));
				mustFeature.put("mustFeatureID", rs.getString("MustFeatureID"));
				mustFeature.put("mustFeaturePrice", rs.getInt("MustPrice"));
				mustFeatureArray.add(mustFeature);
			}		
		} catch (SQLException e) {
			System.out.println("Menu: getItemMustFeatureList: SQLException");
		}
		return mustFeatureArray;
	}
	
	/**For a given itemID, find the available Optional Features and the prices
	 * @param itemID
	 * @return A JSONArray where every JSONObject contains keys:<p>
	 * "optionalFeatureName", "optionalFeatureID", "optionalFeaturePrice"
	 */
	public JSONArray getItemOptionalFeatureList(String itemID) {
		JSONArray optionalFeatureArray = new JSONArray();	
		try { 
			ResultSet rs = stmt.executeQuery(
					"Select OptionalFeature, OptionalFeatureID, OptionalPrice FROM optionalfeatures WHERE ItemID = "+itemID);
			while(rs.next()) { 
				JSONObject optionalFeature = new JSONObject();
				optionalFeature.put("optionalFeatureName", rs.getString("OptionalFeature"));
				optionalFeature.put("optionalFeatureID", rs.getString("OptionalFeatureID"));
				optionalFeature.put("optionalFeaturePrice", rs.getInt("OptionalPrice"));
				optionalFeatureArray.add(optionalFeature);
			}		
		} catch (SQLException e) {
			System.out.println("Menu: SQLException in getItemOptionalFeatureList");
		}
		return optionalFeatureArray;
	}
	
	public JSONObject getMenu() {
		return menu;
	}
}
