package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
 * Logger class is a singleton.
 * Allowing to create Log files for wanted Levels.
 * DEBUG level - see INFO, WARNING and DEBUG messages.
 * INFO level - see INFO and WARNING messages.
 * WARNING level - see WARNING messages.
 * The log file wanted level is set by setLevel()
 * */
public class Logger {

	public enum Level {
		INFO('I'), WARNING('W'), DEBUG('D');

		private char letter;

		Level(char letter) {
			this.letter = letter;
		}

		public char getLetter() {
			return letter;
		}
	}

	private static Level level = Level.INFO; // Default value

	private static Logger instance;

	private static FileWriter writer;

	// creating/overwriting log.txt file
	private Logger() {
		try {
			File file = new File("log.txt");
			if (file.createNewFile()) {
				System.out.println("Logger: File created: " + file.getName());
			} else {
				System.out.println("Logger: File already exists.");
				file.delete();
				System.out.println("Logger: File has been deleted.");
				file = new File("log.txt");
				System.out.println("Logger: File created: " + file.getName());
			}

			writer = new FileWriter("log.txt", false);
			writer.write("######################## NEW RUN ############################\n");

		} catch (IOException e) {
			System.out.println("Logger: An error occurred.");
		}
	}

	// safe file close
	public static void stop() {
		if (writer != null)
			try {
				writer.close();
				System.out.println("Logger: file writer is closed");
			} catch (IOException e) {
				System.out.println("Logger: failed to close file writer");
			}
	}

	// creating the singleton
	public static Logger init() {
		if (instance == null)
			instance = new Logger();
		return instance;
	}

	public static void setLevel(Level l) {
		System.out.println("Logger: level set is: " + l);
		level = l;
	}

	// writes to file
	private static void outputLog(String msg, char letter) {

		LocalDateTime date = LocalDateTime.now();
		DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		StringBuilder str = new StringBuilder(date.format(dateFormat) + " " + letter + " " + msg);

		if (msg.charAt(msg.length() - 1) != '\n') {
			str.append('\n');
		}

		try {
			writer.write(str.toString());
			writer.flush();
		} catch (IOException e) {
			System.out.println("Logger: failed to write: " + msg);
		}
	}

	/**
	 * @param l   is the Level of this message.
	 * @param msg is the message to be written.
	 */
	public static void log(Level l, String msg) {

		if (level == Level.DEBUG)
			outputLog(msg, l.getLetter());
		else if (level == Level.INFO && (l == Level.INFO || l == Level.WARNING))
			outputLog(msg, l.getLetter());
		else if (level == Level.WARNING && l == Level.WARNING)
			outputLog(msg, l.getLetter());
	}
}
