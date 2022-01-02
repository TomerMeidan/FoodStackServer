package util;

public abstract class DateParser {
	/**
	 * convert real date into String representation. Example: date = 12/14/2021,
	 * time = 12:48 -> Dec 14 12:48:00 IST 2021
	 * 
	 * @param date
	 * @param time
	 * @return String containing both date and time (according to what we agreed)
	 */
	public static String toSQLStyle(String date, String time) {
		if (date == null || date.isEmpty() || time.isEmpty() || time == null)
			return "";
		StringBuilder sb = new StringBuilder();
		String split[] = date.split("/");
		String month = split[0];
		switch (month) {
		case "1":
			sb.append("Jan");
			break;
		case "2":
			sb.append("Feb");
			break;
		case "3":
			sb.append("Mar");
			break;
		case "4":
			sb.append("Apr");
			break;
		case "5":
			sb.append("May");
			break;
		case "6":
			sb.append("Jun");
			break;
		case "7":
			sb.append("Jul");
			break;
		case "8":
			sb.append("Aug");
			break;
		case "9":
			sb.append("Sep");
			break;
		case "10":
			sb.append("Oct");
			break;
		case "11":
			sb.append("Nov");
			break;
		case "12":
			sb.append("Dec");
			break;
		default:
			sb.append("Null");
			break;
		}
		sb.append(" ");
		if (Integer.valueOf(split[1]) < 10)
			sb.append("0");
		sb.append(split[1]); // add day
		sb.append(" ");
		sb.append(time);
		sb.append(":00"); // to keep to our format
		sb.append(" IST ");
		sb.append(split[2]);
		return sb.toString();
	}

	/**
	 * Parsering the type of date information from an SQL date string
	 * 
	 * @param dateString - for example: Fri Nov 26 12:43:27 IST 2021
	 * @param type       - year, month, day, time
	 * @return
	 */
	public static String dateParser(String dateString, String type) {

		String parsedString = "";
		String split[] = dateString.split(" ");
		String time[] = split[2].split(":");
		switch (type) {
		case "year":
			parsedString = split[4];
			break;
		case "month":
			switch (split[0]) {
			case "Jan":
				parsedString = "1";
				break;
			case "Feb":
				parsedString = "2";
				break;
			case "Mar":
				parsedString = "3";
				break;
			case "Apr":
				parsedString = "4";
				break;
			case "May":
				parsedString = "5";
				break;
			case "Jun":
				parsedString = "6";
				break;
			case "Jul":
				parsedString = "7";
				break;
			case "Aug":
				parsedString = "8";
				break;
			case "Sep":
				parsedString = "9";
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
			parsedString = split[1];
			break;
		case "seconds":
			parsedString = time[2];
			break;
		case "minutes":
			parsedString = time[1];
			break;
		case "hours":
			parsedString = time[0];
			break;
		default:
			System.out.println("Parser: dateSQL: Invalid date value detected, parsing did not succeed.");
			break;
		}

		return parsedString;

	}
}
