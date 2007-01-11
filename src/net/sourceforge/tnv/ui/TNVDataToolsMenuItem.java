
package net.sourceforge.tnv.ui;

import javax.swing.JMenuItem;

/**
 * @author jgood
 * created on: Jan 10, 2007
 */
public class TNVDataToolsMenuItem extends JMenuItem {

	private String type = "";
	private String name = "";
	private String command = "";
	
	/**
	 * Constructor
	 */
	public TNVDataToolsMenuItem() {
		super();
	}


	/**
	 * @param text
	 */
	public TNVDataToolsMenuItem(String text) {
		super(text);
		name = text;
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
	 * @param command the String command to set
	 */
	public final void setCommand(String command) {
		this.command = command;
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
	 * @param name the String name to set
	 */
	public final void setName(String name) {
		this.name = name;
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
	 * @param type the String type to set
	 */
	public final void setType(String type) {
		this.type = type;
	}


}
