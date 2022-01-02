package serverSide;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import common.Logger;
import common.Message;
import common.Logger.Level;

/**
 * PeriodicActivityService needs methods like: UpdateEveryMidnight() - resets
 * values at midnight (Business client credit)
 * 
 * @version 1.0
 */
public class PeriodicActivityService {

	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private DataBase dataBase = new DataBase();
	private JSONArray listOfExistingIncomeReport, listOfExistingPerformanceReport,
			listOfExistingItemPerRestaurantReport;
	private Date currentTime;

	public void start() {
		// log
		Logger.log(Level.DEBUG, "PeriodicActivityService : Periodic activity service started");
		System.out.println("PeriodicActivityService : Periodic activity service started");

		reportHandler();
		balanceHandler();
	}

	public void balanceHandler() {
		executor.schedule(new PeriodicResetBalance(), 0, TimeUnit.MILLISECONDS);
	}

	/**this class handles the resetting of the balance of the customers.<br>
	 * the reset is executed every day at midnight<br>
	 * has a condition that prevents resetting if it's not midnight
	 * @author mosa
	 */
	class PeriodicResetBalance extends TimerTask {
		@Override
		public void run() {
			Calendar cal = Calendar.getInstance();
			Calendar timeToUpdate = Calendar.getInstance();
			try {
				if (cal.get(Calendar.HOUR_OF_DAY) == 00) { //reset the balance at midnight only
					dataBase.resetBalance();
					Logger.log(Level.WARNING, "PeriodicActivityService: Reset customers balance");
					System.out.println("PeriodicActivityService: Reset customers balance");
				}
				else {
					Logger.log(Level.WARNING, "PeriodicActivityService: No reset for customer balance");
					System.out.println("PeriodicActivityService: No reset for customer balance");
				}
			} catch (Exception e) {
				System.out.println(e);
				Logger.log(Level.WARNING, "PeriodicActivityService: SQLException in updateBalance");
				System.out.println("PeriodicActivityService: SQLException in updateBalance");
			} finally {
				int day = cal.get(Calendar.DAY_OF_MONTH);
				timeToUpdate.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)+1,
						0,0);

				long delay = timeToUpdate.getTimeInMillis() - cal.getTimeInMillis();
				executor.schedule(this, delay, TimeUnit.MILLISECONDS);
			}

		}
	}

	/**
	 * reportHandler
	 * 
	 * This method build \ update report information about Income, Items and
	 * Performance, stores it in the Data Base tables at income_report,
	 * items_report, performance_table. The information is given in a JSON object
	 * from the table orders, items at the data base.
	 * 
	 * @author Tomer Meidan
	 * @version 1.0
	 */
	public void reportHandler() {
		Logger.log(Level.DEBUG, "PeriodicActivityService : Report Handler initialized.");
		System.out.println("PeriodicActivityService : Report Handler initialized.");

		/*
		 * ScheduledExecutorService - executor: An ExecutorService object that can
		 * schedule commands to run after a given delay, or to execute periodically. The
		 * executor will perform the task as soon as the server side is initiated.
		 * Input: TimerTask = saveOrdersInformationTask, class with a periodic task at
		 * the specified time rate delay = 0, will start the TimerTask implemented class
		 * immediately TimeUnit = TimeUnit.MILLISECONDS, represent in milliseconds.
		 */
		Calendar calendar = Calendar.getInstance();
		Date now = new Date();
		calendar.setTime(now);
		long delay = setDelay(calendar, now);
		Date nextUpdate = new Date();

		nextReportUpdateCycle(delay);

		executor.schedule(new saveOrdersInformationTask(), delay, TimeUnit.MILLISECONDS);

	}

	/**
	 * saveOrdersInformationTask
	 * 
	 * This class extends TimerTask in order to be performed in a periodic time
	 * manner. This class sets a timer delay to the start of the next month using
	 * Calendar objects. JSONObjects are being delivered through data base methods
	 * which holds information regarding existing report information in the database
	 * in order to check to avoid duplicates and update only relevant data to the
	 * report tables.
	 * 
	 * @author Tomer Meidan
	 * @version 1.0
	 * 
	 */
	public class saveOrdersInformationTask extends TimerTask {

		// JSON holds all the information about restaurants orders from the database
		private JSONObject restaurantsReportInformation;

		int i = 1;

		public void run() {

			Calendar calendar = Calendar.getInstance();
			Date now = new Date();
			calendar.setTime(now);
			long delay = setDelay(calendar, now);
			try {

				// Start of periodic task at the specified time rate
				// ..........................................................
				Logger.log(Level.DEBUG, "PeriodicActivityService : saveOrdersInformationTask : Update is in progress.");
				System.out.println("PeriodicActivityService : saveOrdersInformationTask : Update is in progress.");

				listOfExistingIncomeReport = dataBase.getExistingIncomeReportInformation();
				listOfExistingItemPerRestaurantReport = dataBase.getExistingItemsPerRestaurantReportInformation();
				listOfExistingPerformanceReport = dataBase.getExistingPerformanceReportInformation();

				restaurantsReportInformation = dataBase.getAllRestaurantsInformation();
				
				JSONArray orders = (JSONArray) restaurantsReportInformation.get("restaurantsOrdersList");

				if(!orders.isEmpty()) {
					// Update Income related information in a table called income_report on the
					// database
					updateIncomeReportData(restaurantsReportInformation);

					// Update item Types related information in a table called items_report on
					// the database
					updateItemsReportData(restaurantsReportInformation);

					// Update Performance related information in a table called
					// performance_report on the database
					updatePerformanceReportData(restaurantsReportInformation);
				} else {
					Logger.log(Level.DEBUG, "PeriodicActivityService : There are no orders in the JSON from db.");
					System.out.println("PeriodicActivityService : There are no orders in the JSON from db.");
				}

				// ..........................................................
				// End of periodic task at the specified time rate

			} catch (Exception e) {
				Logger.log(Level.DEBUG, "PeriodicActivityService : Exception was thrown in saveOrdersInformationTask");
				System.out.println("PeriodicActivityService : Exception was thrown in saveOrdersInformationTask");
			} finally {
				nextReportUpdateCycle(delay);
				// Executing the next update cycle according to the value of delay
				executor.schedule(this, delay, TimeUnit.MILLISECONDS);

			}

		}

	}



	/** Next Report Update Cycle<p>
	 * This method determines the next time periodic activity for report update will initiate.<br>
	 * the delay is being calculated in a way that it takes the end of the month miliseconds value<br>
	 * and subtracts it from the current time in miliseconds in order to obtain the right amount of delay<br>
	 * for the start of the next month.
	 * @param delay - time value in miliseconds that represents the remaining of the month.
	 * */
	private void nextReportUpdateCycle(long delay) {
		Date nextUpdate = new Date();
		nextUpdate.setSeconds((int) (delay / 1000));
		String nextUpdateStr = nextUpdate.getDate() + "\\" + (nextUpdate.getMonth() + 1) + "\\"
				+ (nextUpdate.getYear() + 1900);
		Logger.log(Level.DEBUG,
				"PeriodicActivityService : Next report update will take place on " + nextUpdate.toString());
		System.out.println("PeriodicActivityService : Next report update will take place on " + nextUpdate.toString());
	}

	private long setDelay(Calendar calendar, Date now) {
		long delay;
		if (now.getDate() == 1) { // if its the 1st of the month
			calendar.add(calendar.MONTH, 1); // Add one month forward
			delay = calendar.getTimeInMillis() - System.currentTimeMillis();
		} else {
			// else its not the 1st of the month, take the difference between
			// the end of the month and the current date of the month
			int currentDayOfMonth = now.getDate();
			int endOfMonth = calendar.getActualMaximum(Calendar.DATE);
			int diff = endOfMonth - currentDayOfMonth + 1;
			calendar.add(calendar.DAY_OF_MONTH, diff); // Each month
			delay = calendar.getTimeInMillis() - System.currentTimeMillis();
		}
		return delay;
	}

	/**
	 * updateIncomeReportData
	 * 
	 * This method will take all the related orders from the restaurants and
	 * compress them to amount of orders made by each restaurant and the amount of
	 * income the restaurant made by the specific month and year.
	 * 
	 * @param restaurantsReportInformation - JSONObject which holds all related
	 *                                     information about orders, meal items from
	 *                                     the data base. Note: The received
	 *                                     information regarding the orders are
	 *                                     sorted by : orders.RestaurantName,
	 *                                     orders.OrderDate
	 * @author Tomer Meidan
	 * @version 1.0
	 */
	@SuppressWarnings("unchecked")
	public void updateIncomeReportData(JSONObject restaurantsReportInformation) {

		JSONArray restaurantOrdersInfoToDataBase = new JSONArray();
		JSONArray restaurantsOrdersList = (JSONArray) restaurantsReportInformation.get("restaurantsOrdersList");

		boolean firstEnterance = true;

		Integer size = restaurantsOrdersList.size();
		Integer totalIncome = 0, totalOrders = 0;

		// InCheck variables - if an InCheck is the same as Current on mohth,year and
		// restaurant name. then it will be added to current.
		String restaurantNameInCheck;
		String restaurantMonthInCheck;
		String restaurantYearInCheck;

		// Current variables - restaurant on a specific month and year,
		// summing into it the amount of orders and income.
		String currentRestaurantName = "";
		String currentRestaurantDateMonth = "";
		String currentRestaurantDateYear = "";
		String currentRestaurantBranch = "";

		for (int i = 0; i < size; i++) {

			JSONObject restaurantInfo = (JSONObject) restaurantsOrdersList.get(i);
			restaurantNameInCheck = Message.getValue(restaurantInfo, "restaurantName");
			restaurantMonthInCheck = dateParser(Message.getValue(restaurantInfo, "orderDate"), "month");
			restaurantYearInCheck = dateParser(Message.getValue(restaurantInfo, "orderDate"), "year");

			if (firstEnterance) {
				currentRestaurantName = restaurantNameInCheck;
				currentRestaurantDateMonth = restaurantMonthInCheck;
				currentRestaurantDateYear = restaurantYearInCheck;
				currentRestaurantBranch = Message.getValue(restaurantInfo, "branch");
				totalIncome = ((Integer) restaurantInfo.get("total"));
				totalOrders = 1;

				firstEnterance = false;
			}

			// Checks if the next restaurant is the same as the one in current.
			else if (currentRestaurantName.equals(restaurantNameInCheck)
					&& restaurantMonthInCheck.equals(currentRestaurantDateMonth)
					&& restaurantYearInCheck.equals(currentRestaurantDateYear)) {
				totalIncome += ((Integer) restaurantInfo.get("total"));
				totalOrders++;
			} else {

				// Adding a certain restaurant income information on a specific month,year to
				// the list
				JSONObject newRestaurantOrderData = newRestaurantData(currentRestaurantName, currentRestaurantDateMonth,
						currentRestaurantDateYear, currentRestaurantBranch);
				newRestaurantOrderData.put("totalIncome", totalIncome);
				newRestaurantOrderData.put("totalOrders", totalOrders);

				// Checking if the current data is from this month and year

				if (!equalToCurrentTime(currentRestaurantDateMonth, currentRestaurantDateYear)) {

					// Checking to see if the month and year are already existing in the report
					// table
					if (!listOfExistingIncomeReport.contains(newRestaurantOrderData))
						restaurantOrdersInfoToDataBase.add(newRestaurantOrderData);
				}

				// Setting the next restaurant to sum its values on order amount and income
				// amount
				currentRestaurantName = restaurantNameInCheck;
				currentRestaurantDateMonth = restaurantMonthInCheck;
				currentRestaurantDateYear = restaurantYearInCheck;
				currentRestaurantBranch = Message.getValue(restaurantInfo, "branch");
				totalIncome = ((Integer) restaurantInfo.get("total"));
				totalOrders = 1;
			}

		}
		// Adding a certain restaurant income information on a specific month,year to
		// the list
		// Adding the last restaurant information
		JSONObject newRestaurantOrderData = newRestaurantData(currentRestaurantName, currentRestaurantDateMonth,
				currentRestaurantDateYear, currentRestaurantBranch);
		newRestaurantOrderData.put("totalIncome", totalIncome);
		newRestaurantOrderData.put("totalOrders", totalOrders);

		// Checking if the last current data is from this month and year

		if (!equalToCurrentTime(currentRestaurantDateMonth, currentRestaurantDateYear)) {
			// Checking to see if the month and year are already existing in the report
			// table
			if (!listOfExistingIncomeReport.contains(newRestaurantOrderData))
				restaurantOrdersInfoToDataBase.add(newRestaurantOrderData);
		}

		JSONObject response = dataBase.saveIncomeReportInformation(restaurantOrdersInfoToDataBase);
		Logger.log(Level.DEBUG,
				"PeriodicActivityService : updateIncomeReportData: " + Message.getValue(response, "status"));
		System.out.println("PeriodicActivityService : updateIncomeReportData: " + Message.getValue(response, "status"));
	}

	/**
	 * preformanceCheck
	 * 
	 * This method will subtract the DeliverDate value from DueDate value in order
	 * to determine how long has passed since the initial due order date until the
	 * customer accepted the order.
	 * 
	 * @param restauranDueDate      - String object such as the following: Fri Dec
	 *                              26 10:43:27 IST 2021, this date represents the
	 *                              order's due date, defined by the customer.
	 * @param restaurantDeliverDate - String object such as the following: Fri Dec
	 *                              26 10:43:27 IST 2021, this date represents the
	 *                              customer accepting the order.
	 * @return timeDifference - Returned time value equals to the subtracted amount
	 *         from the delivered date (approved by customer) minus the due date
	 *         value which is defined by the customer.
	 * @author Tomer Meidan
	 * @version 1.0
	 */
	public int preformanceCheck(String restauranDueDate, String restaurantDeliverDate) {
		try {

			Date dueDate = new Date();
			buildDate(restauranDueDate, dueDate);

			Date deliverDate = new Date();
			buildDate(restaurantDeliverDate, deliverDate);

			Integer timeDifference = (int) (deliverDate.getTime() - dueDate.getTime());
			timeDifference /= 1000; // Convert to seconds
			timeDifference /= 60; // Convert to minutes

			return timeDifference;

		} catch (NumberFormatException e) {
			Logger.log(Level.DEBUG,
					"PeriodicActivityService : preformanceCheck : NumberFormatException was thrown in preformanceCheck");
			System.out.println(
					"PeriodicActivityService : preformanceCheck : NumberFormatException was thrown in preformanceCheck");
		} catch (Exception e) {
			Logger.log(Level.DEBUG,
					"PeriodicActivityService : preformanceCheck : Exception was thrown in preformanceCheck");
			System.out.println("PeriodicActivityService : preformanceCheck : Exception was thrown in preformanceCheck");

		}
		return 0;

	}

	public boolean equalToCurrentTime(String Month, String Year) {

		currentTime = new Date();
		int inputMonth = Integer.parseInt(Month);
		int inputYear = Integer.parseInt(Year);
		int currentMonth = currentTime.getMonth() + 1;
		int currentYear = currentTime.getYear() + 1900;
		if ((inputMonth == currentMonth) && (inputYear == currentYear))
			return true;
		return false;

	}

	private void buildDate(String restauranDueDate, Date dueDate) {
		dueDate.setSeconds(Integer.parseInt(dateParser(restauranDueDate, "seconds")));
		dueDate.setMinutes(Integer.parseInt(dateParser(restauranDueDate, "minutes")));
		dueDate.setHours(Integer.parseInt(dateParser(restauranDueDate, "hours")));
		dueDate.setDate(Integer.parseInt(dateParser(restauranDueDate, "day")));
		dueDate.setMonth(Integer.parseInt(dateParser(restauranDueDate, "month")) - 1);
		dueDate.setYear(Integer.parseInt(dateParser(restauranDueDate, "year")) - 1900);
	}

	/**
	 * updatePerformanceReportData
	 * 
	 * This method will take all the related orders from the restaurants and
	 * compress them for each restaurant the amount of ordered main items from each
	 * restaurant by the restaurant name, month and year. by using all the
	 * information from the orders table, the method will determine how much ordered
	 * items a restaurant had in a specific month, year.
	 * 
	 * @param restaurantsReportInformation - JSONObject which holds all related
	 *                                     information about the orders items
	 *                                     amounts.
	 * @author Tomer Meidan
	 * @version 1.0
	 */
	@SuppressWarnings("unchecked")
	public void updatePerformanceReportData(JSONObject restaurantsReportInformation) {

		JSONArray restaurantPerformanceInfoToDataBase = new JSONArray();
		JSONArray restaurantsOrdersList = (JSONArray) restaurantsReportInformation.get("restaurantsOrdersList");

		boolean firstEnterance = true;

		Integer size = restaurantsOrdersList.size();

		String restaurantNameInCheck;
		String restaurantMonthInCheck;
		String restaurantYearInCheck;

		String currentRestaurantName = "";
		String currentRestaurantDateMonth = "";
		String currentRestaurantDateYear = "";
		String currentRestaurantBranch = "";
		String currentDueDate = ""; // Approve Date is a time variable for when the supplier presses approve
		String currentDeliverDate = ""; // Deliver Date is a time variable for when the customer presses approve
		String currentEarlyBooking = "";

		Integer onTimeCount = 0, lateTimeCount = 0;
		Integer onTimeAverage = 0, lateTimeAverage = 0;
		int timeDifference = 0;

		// START NOW
		// START NOW
		for (int i = 0; i < size; i++) {

			JSONObject restaurantInfo = (JSONObject) restaurantsOrdersList.get(i);
			restaurantNameInCheck = Message.getValue(restaurantInfo, "restaurantName");
			restaurantMonthInCheck = dateParser(Message.getValue(restaurantInfo, "orderDate"), "month");
			restaurantYearInCheck = dateParser(Message.getValue(restaurantInfo, "orderDate"), "year");

			if (firstEnterance) {
				currentRestaurantName = restaurantNameInCheck;
				currentRestaurantDateMonth = restaurantMonthInCheck;
				currentRestaurantDateYear = restaurantYearInCheck;
				currentRestaurantBranch = Message.getValue(restaurantInfo, "branch");

				onTimeCount = lateTimeCount = 0;
				onTimeAverage = lateTimeAverage = 0;

				firstEnterance = false;
			}

			if (currentRestaurantName.equals(restaurantNameInCheck)
					&& restaurantMonthInCheck.equals(currentRestaurantDateMonth)
					&& restaurantYearInCheck.equals(currentRestaurantDateYear)) {

				currentDueDate = Message.getValue(restaurantInfo, "dueDate");
				currentDeliverDate = Message.getValue(restaurantInfo, "deliverDate");
				currentEarlyBooking = Message.getValue(restaurantInfo, "earlyBooking");

				timeDifference = preformanceCheck(currentDueDate, currentDeliverDate);

				// Check with others to put in either True or False
				if (currentEarlyBooking == null || currentEarlyBooking.equals("False")) {
					if (timeDifference > 60) {
						lateTimeCount++;
						lateTimeAverage += timeDifference;
					} else {
						onTimeCount++;
						onTimeAverage += timeDifference;
					}
				} else if (currentEarlyBooking.equals("True")) {
					if (timeDifference > 20) {
						lateTimeCount++;
						lateTimeAverage += timeDifference;
					} else {
						onTimeCount++;
						onTimeAverage += timeDifference;
					}
				}

			} else {

				JSONObject newRestaurantPerformanceData = newRestaurantData(currentRestaurantName,
						currentRestaurantDateMonth, currentRestaurantDateYear, currentRestaurantBranch);
				newRestaurantPerformanceData.put("onTimeCount", onTimeCount);
				newRestaurantPerformanceData.put("lateTimeCount", lateTimeCount);

				if (onTimeCount == 0)
					onTimeAverage = 0;
				else
					onTimeAverage /= onTimeCount;
				if (lateTimeCount == 0)
					lateTimeAverage = 0;
				else
					lateTimeAverage /= lateTimeCount;

				newRestaurantPerformanceData.put("onTimeAverage", onTimeAverage);
				newRestaurantPerformanceData.put("lateTimeAverage", lateTimeAverage);

				// Checking if the current data is from this month and year

				if (!equalToCurrentTime(currentRestaurantDateMonth, currentRestaurantDateYear)) {
					// Checking to see if the month and year are already existing in the report
					// table
					if (!listOfExistingPerformanceReport.contains(newRestaurantPerformanceData))
						restaurantPerformanceInfoToDataBase.add(newRestaurantPerformanceData);
				}

				currentRestaurantName = restaurantNameInCheck;
				currentRestaurantDateMonth = restaurantMonthInCheck;
				currentRestaurantDateYear = restaurantYearInCheck;
				currentRestaurantBranch = Message.getValue(restaurantInfo, "branch");
				currentDueDate = Message.getValue(restaurantInfo, "dueDate");
				currentDeliverDate = Message.getValue(restaurantInfo, "deliverDate");
				currentEarlyBooking = Message.getValue(restaurantInfo, "earlyBooking");

				onTimeCount = lateTimeCount = 0;
				onTimeAverage = lateTimeAverage = 0;

				// Add an if to check if timeDifference is below zero maybe?
				timeDifference = preformanceCheck(currentDueDate, currentDeliverDate);

				if (timeDifference >= 0) {
					// Check with others to put in either True or False
					if (currentEarlyBooking.equals("True")) {
						if (timeDifference > 20) {
							lateTimeCount++;
							lateTimeAverage += timeDifference;
						} else {
							onTimeCount++;
							onTimeAverage += timeDifference;
						}
					} else if (currentEarlyBooking.equals("False")) {
						if (timeDifference > 60) {
							lateTimeCount++;
							lateTimeAverage += timeDifference;
						} else {
							onTimeCount++;
							onTimeAverage += timeDifference;
						}
					}
				} else {
					Logger.log(Level.DEBUG,
							"PeriodicActivityService : updatePerformanceReportData: Time difference variable is negative, didnt add row to Performance Report");
					System.out.println(
							"PeriodicActivityService : updatePerformanceReportData: Time difference variable is negative, didnt add row to Performance Report");
				}
			}

		}

		JSONObject newRestaurantPerformanceData = newRestaurantData(currentRestaurantName, currentRestaurantDateMonth,
				currentRestaurantDateYear, currentRestaurantBranch);
		newRestaurantPerformanceData.put("onTimeCount", onTimeCount);
		newRestaurantPerformanceData.put("lateTimeCount", lateTimeCount);

		if (onTimeCount == 0)
			onTimeAverage = 0;
		else
			onTimeAverage /= onTimeCount;
		if (lateTimeCount == 0)
			lateTimeAverage = 0;
		else
			lateTimeAverage /= lateTimeCount;

		newRestaurantPerformanceData.put("onTimeAverage", onTimeAverage);
		newRestaurantPerformanceData.put("lateTimeAverage", lateTimeAverage);

		// Checking if the current data is from this month and year

		if (!equalToCurrentTime(currentRestaurantDateMonth, currentRestaurantDateYear)) {
			// Checking to see if the month and year are already existing in the report
			// table
			if (!listOfExistingPerformanceReport.contains(newRestaurantPerformanceData))
				restaurantPerformanceInfoToDataBase.add(newRestaurantPerformanceData);
		}

		JSONObject response = dataBase.savePerformanceReportInformation(restaurantPerformanceInfoToDataBase);
		Logger.log(Level.DEBUG,
				"PeriodicActivityService : updateIncomeReportData: " + Message.getValue(response, "status"));
		System.out.println("PeriodicActivityService : updateIncomeReportData: " + Message.getValue(response, "status"));

	}

	/**
	 * updateItemsReportData
	 * 
	 * This method will take all the related orders from the restaurants and
	 * compress them for each restaurant the amount of orders that were delivered on
	 * time, the average time it took and the amount of time the deliver was late.
	 * by using all the information from the orders table, the method will determine
	 * how many orderes were late or on time for each restaurant by a specific
	 * month, year.
	 * 
	 * @param restaurantsReportInformation - JSONObject which holds all related
	 *                                     information about the orders deliver
	 *                                     dates and approve dates.
	 * @author Tomer Meidan
	 * @version 1.0
	 */
	@SuppressWarnings("unchecked")
	public void updateItemsReportData(JSONObject restaurantsReportInformation) {

		// restaurantsMealTypesList - JSON that will be inserted into the meal types
		// table in database
		// Structure of the JSON: [{ItemID, ItemType, ItemPrice, RestaurantName, Month,
		// Year, Branch},...,]
		JSONArray restaurantsItemsList = (JSONArray) restaurantsReportInformation.get("restaurantsMealTypesList");

		boolean firstEnterance = true;

		Integer sizeOfRestaurantItems = restaurantsItemsList.size();
		Integer totalItemCount = 0;
		Integer specificItemCount = 0;

		String currentRestaurantName = "";
		String currentRestaurantDateMonth = "";
		String currentRestaurantDateYear = "";
		String currentRestaurantBranch = "";
		String currentRestaurantItem = "";
		String currentRestaurantItemName = "";

		String checkRestaurantItemName = "";
		JSONArray itemsPerRestaurant = new JSONArray();
		for (int i = 0; i < sizeOfRestaurantItems; i++) {

			JSONArray restaurantInfo = (JSONArray) restaurantsItemsList.get(i);

			Integer sizeOfSingleItem = restaurantInfo.size();

			firstEnterance = true;

			for (int j = 0; j < sizeOfSingleItem; j++) {

				JSONObject singleMealRow = (JSONObject) restaurantInfo.get(j);
				checkRestaurantItemName = Message.getValue(singleMealRow, "itemName");

				if (firstEnterance) {
					currentRestaurantName = Message.getValue(singleMealRow, "restaurantName");
					currentRestaurantDateMonth = dateParser(Message.getValue(singleMealRow, "orderDate"), "month");
					currentRestaurantDateYear = dateParser(Message.getValue(singleMealRow, "orderDate"), "year");
					currentRestaurantBranch = Message.getValue(singleMealRow, "branch");
					currentRestaurantItem = Message.getValue(singleMealRow, "itemType");
					currentRestaurantItemName = checkRestaurantItemName;

					specificItemCount = 1;
					totalItemCount = 1;

					firstEnterance = false;
				}

				else {
					if (checkRestaurantItemName.equals(currentRestaurantItemName))
						specificItemCount++;
					else {
						// This Json Object goes into the itemsPerRestaurant_report table
						JSONObject specificRestaurantItem = addToJson(specificItemCount, currentRestaurantName,
								currentRestaurantDateMonth, currentRestaurantDateYear, currentRestaurantBranch,
								currentRestaurantItem, currentRestaurantItemName);
						// Checking if the current data is from this month and year

						if (!equalToCurrentTime(currentRestaurantDateMonth, currentRestaurantDateYear)) {
							if (!listOfExistingItemPerRestaurantReport.contains(specificRestaurantItem))
								itemsPerRestaurant.add(specificRestaurantItem);
						}
						currentRestaurantItemName = checkRestaurantItemName;
						specificItemCount = 1;
					}
					totalItemCount++; // increment the size of said item
				}

			}

			// This Json Object goes into the itemsPerRestaurant_report table
			JSONObject specificRestaurantItem = addToJson(specificItemCount, currentRestaurantName,
					currentRestaurantDateMonth, currentRestaurantDateYear, currentRestaurantBranch,
					currentRestaurantItem, currentRestaurantItemName);

			// Checking if the current data is from this month and year

			if (!equalToCurrentTime(currentRestaurantDateMonth, currentRestaurantDateYear)) {
				if (!listOfExistingItemPerRestaurantReport.contains(specificRestaurantItem))
					itemsPerRestaurant.add(specificRestaurantItem);
			}

			currentRestaurantItemName = checkRestaurantItemName;
			specificItemCount = 1;

		}

		JSONObject response = dataBase.saveItemsPerRestaurantReportInformation(itemsPerRestaurant);
		Logger.log(Level.DEBUG,
				"PeriodicActivityService : updateItemsReportData: " + Message.getValue(response, "status"));
		System.out.println("PeriodicActivityService : updateItemsReportData: " + Message.getValue(response, "status"));

	}

	/** Add to json<p>
	 * This method creates a new item row for the database regarding a spesific item that was purchased from the restaurant.
	 * @param specificItemCount - hold the item amount representing the amount it was bought from the restaurant.
	 * @param currentRestaurantName - one of the restaurants that will be added to the database in a report row holding item value of a certain meal.
	 * @param currentRestaurantDateMonth - what is the current month this item was bought X times
	 * @param currentRestaurantDateYear -  what is the current year this item was bought X times.
	 * @param currentRestaurantBranch -  which branch the restaurant belongs
	 * @param currentRestaurantItem - which item is being counted
	 * @param currentRestaurantItemName - what is the specific name of the item
	 * @return returns a json that holds the restaurant name, its item count and other various info regarding the restaurant.
	 * */
	private JSONObject addToJson(Integer specificItemCount, String currentRestaurantName,
			String currentRestaurantDateMonth, String currentRestaurantDateYear, String currentRestaurantBranch,
			String currentRestaurantItem, String currentRestaurantItemName) {
		JSONObject specificRestaurantItem = new JSONObject();
		specificRestaurantItem.put("restaurantName", currentRestaurantName);
		specificRestaurantItem.put("dateMonth", currentRestaurantDateMonth);
		specificRestaurantItem.put("dateYear", currentRestaurantDateYear);
		specificRestaurantItem.put("itemName", currentRestaurantItemName);
		specificRestaurantItem.put("itemCount", specificItemCount);
		specificRestaurantItem.put("branch", currentRestaurantBranch);
		specificRestaurantItem.put("itemType", currentRestaurantItem);
		return specificRestaurantItem;
	}
	/** New Restaurant Data<p>
	 * This method creates a new item row for the database regarding a report row that was inside the order table DB from the restaurant.
	 * @param restaurantName - holds the restaurant name
	 * @param restaurantDateMonth - holds the month for this restaurant
	 * @param restaurantDateYear - holds the year for this restaurant
	 * @param restaurantBranch -  holds the branch for this restaurant
	 * @return returns a json that holds the restaurant name, branch, year and branch for the report tables on the database.
	 * */
	@SuppressWarnings("unchecked")
	public JSONObject newRestaurantData(String restaurantName, String restaurantDateMonth, String restaurantDateYear,
			String restaurantBranch) {

		JSONObject json = new JSONObject();
		json.put("restaurantName", restaurantName);
		json.put("dateMonth", restaurantDateMonth);
		json.put("dateYear", restaurantDateYear);
		json.put("branch", restaurantBranch);
		return json;
	}

	public static String dateParser(String dateString, String type) {

		String parsedString = "";

		switch (type) {
		case "year":
			parsedString = dateString.substring(20, 24);
			break;
		case "month":
			switch (dateString.substring(0, 3)) {
			case "Jan":
				parsedString = "01";
				break;
			case "Feb":
				parsedString = "02";
				break;
			case "Mar":
				parsedString = "03";
				break;
			case "Apr":
				parsedString = "04";
				break;
			case "May":
				parsedString = "05";
				break;
			case "Jun":
				parsedString = "06";
				break;
			case "Jul":
				parsedString = "07";
				break;
			case "Aug":
				parsedString = "08";
				break;
			case "Sep":
				parsedString = "09";
				break;
			case "Oct":
				parsedString = "10";
				break;
			case "Nov":
				parsedString = "11";
				break;
			case "Dec":
				parsedString = "12";
				break;
			default:
				System.out.println("Parser: dateSQL: Invalid month detected, parsing did not succeed.");
				break;
			}
			break;
		case "day":
			parsedString = dateString.substring(4, 6);
			break;
		case "seconds":
			parsedString = dateString.substring(13, 15);
			break;
		case "minutes":
			parsedString = dateString.substring(10, 12);
			break;
		case "hours":
			parsedString = dateString.substring(7, 9);
			break;
		default:
			System.out.println("Parser: dateSQL: Invalid date value detected, parsing did not succeed.");
			break;
		}

		return parsedString;

	}

}
