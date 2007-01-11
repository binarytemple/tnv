
package net.sourceforge.tnv.ui;

import javax.swing.JMenuItem;

/**
 * @author jgood
 * created on: Jan 10, 2007
 */
public class TNVDataToolsMenuItem extends JMenuItem {

	// default timeout for operations (milliseconds)
	private static int DEFAULT_DATA_TOOLS_TIMEOUT = 15000;
	
	private String type = "";
	private String name = "";
	private String command = "";
	private int timeout = DEFAULT_DATA_TOOLS_TIMEOUT;
	
	/**
	 * Constructor
	 * @param type
	 * @param name
	 * @param command
	 */
	public TNVDataToolsMenuItem(String type, String name, String command) {
		super(name);
		this.type = type;
		this.name = name;
		this.command = command;
	}

	/**
	 *
	 * @return the String TNVDataToolsMenuItem.java
	 */
	public final String getCommand() {
		return this.command;
	}

	/**
	 *
	 * @return the String TNVDataToolsMenuItem.java
	 */
	public final String getName() {
		return this.name;
	}

	/**
	 *
	 * @return the String TNVDataToolsMenuItem.java
	 */
	public final String getType() {
		return this.type;
	}


	/**
	 *
	 * @return the int TNVDataToolsMenuItem.java
	 */
	public final int getTimeout() {
		return this.timeout;
	}


	/**
	 *
	 * @param timeout the int timeout to set
	 */
	public final void setTimeout(int timeout) {
		this.timeout = timeout;
	}
}
