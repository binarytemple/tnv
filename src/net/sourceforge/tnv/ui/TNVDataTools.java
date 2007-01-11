
package net.sourceforge.tnv.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import net.sourceforge.tnv.dialogs.TNVErrorDialog;

/**
 * @author jgood
 * created on: Jan 10, 2007
 */
public class TNVDataTools {

	// default path for data tools
	private static String DATA_TOOLS_CONFIG_FILE = "config/datatools.txt";
	
	// default data file contents
	private static String DEFAULT_DATA_TOOLS_CONTENTS = 
		"# Data tools are external commands and URLs that can be used within tnv\n" +
		"# Format of each row is: TYPE|MENU|COMMAND|[TIMEOUT]\n" +
		"# Valid TYPEs are:\n" +
		"#  URL to open URL in a web browser\n" +
		"#  EXE to run a Windows command and show output in a dialog\n" +
		"#  SH to run a Unix/Mac shell command and show output in a dialog\n" +
		"# MENU is the name of the context menu\n" +
		"# COMMAND is the name of the exe or sh command to run or the URL to open\n" +
		"# TIMEOUT (in seconds) is optional and is only used for SH or EXE\n" +
		"# Beware of long running commands!\n" +
		"# To insert an IP address into a MENU or COMMAND use ##IP##\n" +
		"URL|whois (Arin) ##IP##|http://ws.arin.net/whois/?queryinput=##IP##\n" +
		"URL|reverse DNS lookup for ##IP##|http://www.zoneedit.com/lookup.html?ipaddress=##IP##&server=&reverse=Look+it+up\n" +
		"SH|ping ##IP##|ping -c 3 -t 3 ##IP##|20\n" +
		"EXE|ping ##IP##|ping -n 3 -w 3 ##IP##|20\n";

	// data tools menu item array which is dynamically built when using contextual menus
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
	 * When called the data tools will be read from the text file
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
		
		// If data tools config file does not exist, create a new one with defaults
		File configFile = new File(DATA_TOOLS_CONFIG_FILE);
		if ( ! configFile.exists() )	 {
			try {
				configFile.createNewFile();
			} catch (IOException ex) {
				TNVErrorDialog.createTNVErrorDialog(this.getClass(), "Unable to create data tools configuration file: "
						+ DATA_TOOLS_CONFIG_FILE + "\nCheck permissions.", ex);
			}
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(configFile));
				out.write(DEFAULT_DATA_TOOLS_CONTENTS);
				out.close();
			} catch (IOException ex) {
				TNVErrorDialog.createTNVErrorDialog(this.getClass(), "Unable to write new data tools configuration file: "
						+ DATA_TOOLS_CONFIG_FILE + "\nCheck permissions.", ex);
			}
		}

		// check that config file can be read, if not, return
		if ( ! configFile.canRead() )	 {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "Unable to read data tools configuration file: "
					+ configFile.getAbsolutePath() + "\n Fix the file's permissions.");
			return;
		}

		// parse the data and populate array of TNVDataToolsMenuItem (JMenuItems)
		try {
			BufferedReader br = new BufferedReader( new FileReader(configFile) );
			String input = "", type, menu, command;
			int timeout;
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

				// Only show sh for Unix/Mac and exe for Windows
				String os = System.getProperty("os.name").toLowerCase();
			    if ( os.indexOf("windows") != -1 && ! type.equalsIgnoreCase("exe") )
			       continue;
			    else if ( os.indexOf("windows") == -1 && ! type.equalsIgnoreCase("sh") )
			       continue;
				
			    // Create new menu item
				TNVDataToolsMenuItem item = new TNVDataToolsMenuItem(type, menu, command);
				
				// optional timeout
				if ( scanner.hasNext() )
					item.setTimeout((scanner.nextInt() * 1000));

				scanner.close();
				tempDataTools.add(item);
			}
		} 
		catch (IOException ex) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "IO Error parsing data tools configuration file", ex);
		}
		DATA_TOOLS = new TNVDataToolsMenuItem[tempDataTools.size()];
		tempDataTools.toArray(DATA_TOOLS);
	}
	
	
}
