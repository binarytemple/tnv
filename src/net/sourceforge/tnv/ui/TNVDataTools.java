
package net.sourceforge.tnv.ui;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author jgood
 * created on: Jan 10, 2007
 */
public class TNVDataTools {

	private static TNVDataToolsMenuItem[] DATA_TOOLS;
	
	// Singleton -- this has to be defined last
	private static TNVDataTools instance = new TNVDataTools();


	/**
	 * Private Constructor
	 */
	private TNVDataTools() {
		this.readDataTools();
	}
	
	/**
	 * This is the only public class
	 * @return the TNVDataToolsMenuItem[] TNVDataTools.java
	 */
	public static final TNVDataToolsMenuItem[] getDataTools() {
		return DATA_TOOLS;
	}


	/**
	 * Read data tools file and save to array for context menu 
	 */
	private void readDataTools() {
		List<TNVDataToolsMenuItem> tempDataTools = new ArrayList<TNVDataToolsMenuItem>();
		
		try {
			BufferedReader br = new BufferedReader( new FileReader("config/datatools.txt") );
			String input = "", type, menu, command;
			Scanner scanner;
			while ((input = br.readLine()) != null) {
				if ( input.startsWith("#") )
					continue;
				scanner = new Scanner(input).useDelimiter("\\|");
				if ( scanner.hasNext() )
					type = scanner.next();
				else {
					System.err.println("Error parsing data tools line:  " + input);
					continue;
				}
				if ( scanner.hasNext() )
					menu = scanner.next();
				else {
					System.err.println("Error parsing data tools line:  " + input);
					continue;
				}
				if ( scanner.hasNext() )
					command = scanner.next();
				else {
					System.err.println("Error parsing data tools line:  " + input);
					continue;
				}
				scanner.close();

				TNVDataToolsMenuItem item = new TNVDataToolsMenuItem(menu);
				item.setType(type);
				item.setCommand(command);
				tempDataTools.add(item);
			}
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		DATA_TOOLS = new TNVDataToolsMenuItem[tempDataTools.size()];
		tempDataTools.toArray(DATA_TOOLS);
	}
	
	
}
