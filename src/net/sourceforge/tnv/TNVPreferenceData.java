/**
 * Created on Apr 13, 2004 
 * @author jgood
 *
 * This class opens/saves property settings
 */

package net.sourceforge.tnv;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

/**
 * TNVPreferenceData
 */
public class TNVPreferenceData {

	private transient ChangeEvent preferenceChangeEvent = null;

	private EventListenerList listenerList = new EventListenerList();

	private final static String PROPERTIES_FILE = "tnv.properties";

	// Default Settings
	private final static int DEFAULT_COLUMN_COUNT = 5;
	private final static int DEFAULT_ROW_HEIGHT = 75;
	private final static boolean DEFAULT_SHOW_TOOLTIPS = true;
	private final static boolean DEFAULT_CURVED_LINKS = false;
	private final static boolean DEFAULT_SHOW_PACKETS = true;
	private final static boolean DEFAULT_SHOW_FLAGS = false;
	
	private final static Color DEFAULT_TCP_COLOR = new Color( 27, 158, 119 );
	private final static Color DEFAULT_UDP_COLOR = new Color( 217, 95, 2 );
	private final static Color DEFAULT_ICMP_COLOR = new Color( 107, 102, 169 );
	
	private final static Color DEFAULT_SYN_COLOR = new Color( 166, 206, 227 );
	private final static Color DEFAULT_ACK_COLOR = new Color( 31, 120, 180 );
	private final static Color DEFAULT_FIN_COLOR = new Color( 178, 223, 138 );
	private final static Color DEFAULT_URG_COLOR = new Color( 51, 160, 44 );
	private final static Color DEFAULT_PSH_COLOR = new Color( 251, 154, 153 );
	private final static Color DEFAULT_RST_COLOR = new Color( 227, 26, 28 );
	
	private final static int DEFAULT_COLOR_MAP_INDEX = 0;

	// Preferences
	private int columnCount, rowHeight;
	private boolean showTooltips, showPackets, showFlags, curvedLinks;
	private String homeNet;
	private int colorMapIndex;
	private Color tcpColor, udpColor, icmpColor,
		synColor, ackColor, finColor, urgColor, pshColor, rstColor;

	private Properties properties = new Properties();

	// Singleton -- this has to be defined last
	private static TNVPreferenceData instance = new TNVPreferenceData();


	/**
	 * Private Constructor
	 */
	private TNVPreferenceData() {
		loadProperties();
	}


	/**
	 * @return singleton instance
	 */
	protected static TNVPreferenceData getInstance( ) {
		return instance;
	}


	/**
	 * Load preferences from file or use the defaults
	 */
	protected void loadProperties( ) {
		try {
			FileInputStream in = new FileInputStream( PROPERTIES_FILE );
			this.properties.load( in );
			in.close();
		}
		catch ( Exception e ) {
			System.out.println( "Could not load preference file: " + e.getMessage() );
		}
		setHomeNet( getStringProperty( "HomeNet", "" ) );
		setColumnCount( getIntProperty( "ColumnCount", DEFAULT_COLUMN_COUNT ) );
		setRowHeight( getIntProperty( "RowHeight", DEFAULT_ROW_HEIGHT ) );
		setCurvedLinks( getBooleanProperty( "CurvedLinks", DEFAULT_CURVED_LINKS ) );
		setShowTooltips( getBooleanProperty( "ShowTooltips", DEFAULT_SHOW_TOOLTIPS ) );
		setShowPackets( getBooleanProperty( "ShowPackets", DEFAULT_SHOW_PACKETS ) );
		setShowFlags( getBooleanProperty( "ShowFlags", DEFAULT_SHOW_FLAGS ) );
		setTcpColor( getColorProperty( "TCPColor", DEFAULT_TCP_COLOR ) );
		setUdpColor( getColorProperty( "UDPColor", DEFAULT_UDP_COLOR ) );
		setIcmpColor( getColorProperty( "ICMPColor", DEFAULT_ICMP_COLOR ) );
		setSynColor( getColorProperty( "SYNColor", DEFAULT_SYN_COLOR ) );
		setAckColor( getColorProperty( "ACKColor", DEFAULT_ACK_COLOR ) );
		setFinColor( getColorProperty( "FINColor", DEFAULT_FIN_COLOR ) );
		setPshColor( getColorProperty( "PSHColor", DEFAULT_PSH_COLOR ) );
		setUrgColor( getColorProperty( "URGColor", DEFAULT_URG_COLOR ) );
		setRstColor( getColorProperty( "RSTColor", DEFAULT_RST_COLOR ) );
		setColorMapIndex( getIntProperty( "MachineColorMap", DEFAULT_COLOR_MAP_INDEX ) );
	}


	/**
	 * Set all preferences and save the preferences to disk
	 */
	protected void saveProperties( ) {
		this.properties.setProperty( "HomeNet", getHomeNet() );
		this.properties.setProperty( "ColumnCount", getColumnCount() + "" );
		this.properties.setProperty( "RowHeight", getRowHeight() + "" );

		if ( isShowTooltips() )
			this.properties.setProperty( "ShowTooltips", "true" );
		else
			this.properties.setProperty( "ShowTooltips", "false" );

		if ( isShowPackets() )
			this.properties.setProperty( "ShowPackets", "true" );
		else
			this.properties.setProperty( "ShowPackets", "false" );

		if ( isShowFlags() )
			this.properties.setProperty( "ShowFlags", "true" );
		else
			this.properties.setProperty( "ShowFlags", "false" );

		if ( isCurvedLinks() )
			this.properties.setProperty( "CurvedLinks", "true" );
		else
			this.properties.setProperty( "CurvedLinks", "false" );

		this.properties.setProperty( "TCPColor", getTcpColor().getRed() + " " + getTcpColor().getGreen() + " "
				+ getTcpColor().getBlue() );
		this.properties.setProperty( "UDPColor", getUdpColor().getRed() + " " + getUdpColor().getGreen() + " "
				+ getUdpColor().getBlue() );
		this.properties.setProperty( "ICMPColor", getIcmpColor().getRed() + " " + getIcmpColor().getGreen() + " "
				+ getIcmpColor().getBlue() );

		this.properties.setProperty( "SYNColor", getSynColor().getRed() + " " + getSynColor().getGreen() + " "
				+ getSynColor().getBlue() );
		this.properties.setProperty( "ACKColor", getAckColor().getRed() + " " + getAckColor().getGreen() + " "
				+ getAckColor().getBlue() );
		this.properties.setProperty( "FINColor", getFinColor().getRed() + " " + getFinColor().getGreen() + " "
				+ getFinColor().getBlue() );
		this.properties.setProperty( "PSHColor", getPshColor().getRed() + " " + getPshColor().getGreen() + " "
				+ getPshColor().getBlue() );
		this.properties.setProperty( "URGColor", getUrgColor().getRed() + " " + getUrgColor().getGreen() + " "
				+ getUrgColor().getBlue() );
		this.properties.setProperty( "RSTColor", getRstColor().getRed() + " " + getRstColor().getGreen() + " "
				+ getRstColor().getBlue() );

		this.properties.setProperty( "MachineColorMap", getColorMapIndex() + "" );

		// fire change event to any listeners when saving properties
		this.firePreferenceChanged();

		// save properties to disk
		try {
			FileOutputStream out = new FileOutputStream( PROPERTIES_FILE );
			this.properties.store( out, "Saved by TNV " );
			out.close();
		}
		catch ( Exception e ) {
			System.out.println( "Could not save preference file: " + e.getMessage() );
		}
	}


	/**
	 * Reset settings to defaults
	 */
/*	protected void resetProperties( ) {
		setHomeNet( "" );
		setColumnCount( DEFAULT_COLUMN_COUNT );
		setRowHeight( DEFAULT_ROW_HEIGHT );
		setCurvedLinks( DEFAULT_CURVED_LINKS);
		setShowTooltips( DEFAULT_SHOW_TOOLTIPS );
		setShowPackets( DEFAULT_SHOW_PACKETS );
		setShowFlags( DEFAULT_SHOW_FLAGS );
		setTcpColor( DEFAULT_TCP_COLOR );
		setUdpColor( DEFAULT_UDP_COLOR );
		setIcmpColor( DEFAULT_ICMP_COLOR );
		setColorMapIndex( DEFAULT_COLOR_MAP_INDEX );
	}
*/

	/**
	 * @return Returns the columnWidth.
	 */
	protected final int getColumnCount( ) {
		return this.columnCount;
	}

	/**
	 * @param columnCount The columnWidth to set.
	 */
	protected final void setColumnCount( int w ) {
		this.columnCount = w;
		this.firePreferenceChanged();
	}


	/**
	 * @return Returns the rowHeight.
	 */
	protected final int getRowHeight( ) {
		return this.rowHeight;
	}

	/**
	 * @param rowHeight The rowHeight to set.
	 */
	protected final void setRowHeight( int h ) {
		this.rowHeight = h;
		this.firePreferenceChanged();
	}


	/**
	 * @return Returns the colorMapIndex.
	 */
	protected final int getColorMapIndex( ) {
		return this.colorMapIndex;
	}

	/**
	 * @param colorMapIndex The colorMapIndex to set.
	 */
	protected final void setColorMapIndex( int m ) {
		this.colorMapIndex = m;
		this.firePreferenceChanged();
	}


	/**
	 * @return Returns the homeNet.
	 */
	protected final String getHomeNet( ) {
		return this.homeNet;
	}

	/**
	 * @param homeNet The homeNet to set.
	 */
	protected final void setHomeNet( String net ) {
		this.homeNet = net;
		this.firePreferenceChanged();
	}


	/**
	 * @return Returns the curvedLinks.
	 */
	protected final boolean isCurvedLinks( ) {
		return this.curvedLinks;
	}

	/**
	 * @param showTooltips The showTooltips to set.
	 */
	protected final void setCurvedLinks( boolean l ) {
		this.curvedLinks = l;
		this.firePreferenceChanged();
	}

	
	/**
	 * @return Returns the showTooltips.
	 */
	protected final boolean isShowTooltips( ) {
		return this.showTooltips;
	}

	/**
	 * @param showTooltips The showTooltips to set.
	 */
	protected final void setShowTooltips( boolean tt ) {
		this.showTooltips = tt;
		this.firePreferenceChanged();
	}


	/**
	 *
	 * @return the boolean TNVPreferenceData.java
	 */
	protected final boolean isShowFlags() {
		return this.showFlags;
	}


	/**
	 *
	 * @param showFlags the boolean showFlags to set
	 */
	protected final void setShowFlags(boolean showFlags) {
		this.showFlags = showFlags;
		this.firePreferenceChanged();
	}


	/**
	 *
	 * @return the boolean TNVPreferenceData.java
	 */
	protected final boolean isShowPackets() {
		return this.showPackets;
	}


	/**
	 *
	 * @param showPackets the boolean showPackets to set
	 */
	protected final void setShowPackets(boolean showPackets) {
		this.showPackets = showPackets;
		this.firePreferenceChanged();
	}


	/**
	 * @return Returns the tcpColor.
	 */
	protected final Color getTcpColor( ) {
		return this.tcpColor;
	}

	/**
	 * @param tcpColor The tcpColor to set.
	 */
	protected final void setTcpColor( Color c ) {
		this.tcpColor = c;
		this.firePreferenceChanged();
	}


	/**
	 * @return Returns the udpColor.
	 */
	protected final Color getUdpColor( ) {
		return this.udpColor;
	}

	/**
	 * @param udpColor The udpColor to set.
	 */
	protected final void setUdpColor( Color c ) {
		this.udpColor = c;
		this.firePreferenceChanged();
	}


	/**
	 * @return Returns the icmpColor.
	 */
	protected final Color getIcmpColor( ) {
		return this.icmpColor;
	}

	/**
	 * @param icmpColor The icmpColor to set.
	 */
	protected final void setIcmpColor( Color c ) {
		this.icmpColor = c;
		this.firePreferenceChanged();
	}


	/**
	 *
	 * @return the Color TNVPreferenceData.java
	 */
	protected final Color getAckColor() {
		return this.ackColor;
	}


	/**
	 *
	 * @param ackColor the Color ackColor to set
	 */
	protected final void setAckColor(Color ackColor) {
		this.ackColor = ackColor;
		this.firePreferenceChanged();
	}


	/**
	 *
	 * @return the Color TNVPreferenceData.java
	 */
	protected final Color getFinColor() {
		return this.finColor;
	}


	/**
	 *
	 * @param finColor the Color finColor to set
	 */
	protected final void setFinColor(Color finColor) {
		this.finColor = finColor;
		this.firePreferenceChanged();
	}


	/**
	 *
	 * @return the Color TNVPreferenceData.java
	 */
	protected final Color getPshColor() {
		return this.pshColor;
	}


	/**
	 *
	 * @param pshColor the Color pshColor to set
	 */
	protected final void setPshColor(Color pshColor) {
		this.pshColor = pshColor;
		this.firePreferenceChanged();
	}


	/**
	 *
	 * @return the Color TNVPreferenceData.java
	 */
	protected final Color getRstColor() {
		return this.rstColor;
	}


	/**
	 *
	 * @param rstColor the Color rstColor to set
	 */
	protected final void setRstColor(Color rstColor) {
		this.rstColor = rstColor;
		this.firePreferenceChanged();
	}


	/**
	 *
	 * @return the Color TNVPreferenceData.java
	 */
	protected final Color getSynColor() {
		return this.synColor;
	}


	/**
	 *
	 * @param synColor the Color synColor to set
	 */
	protected final void setSynColor(Color synColor) {
		this.synColor = synColor;
		this.firePreferenceChanged();
	}


	/**
	 *
	 * @return the Color TNVPreferenceData.java
	 */
	protected final Color getUrgColor() {
		return this.urgColor;
	}


	/**
	 *
	 * @param urgColor the Color urgColor to set
	 */
	protected final void setUrgColor(Color urgColor) {
		this.urgColor = urgColor;
		this.firePreferenceChanged();
	}


	/**
	 * @param l
	 */
	public void addPreferenceChangeListener( ChangeListener l ) {
		this.listenerList.add( ChangeListener.class, l );
	}

	/**
	 * @param l
	 */
	public void removePreferenceChangeListener( ChangeListener l ) {
		this.listenerList.remove( ChangeListener.class, l );
	}

	/**
	 * @return listeners
	 */
	public ChangeListener[] getPreferenceChangeListener( ) {
		return this.listenerList.getListeners( ChangeListener.class );
	}


	/**
	 * fire preference changed event
	 */
	protected void firePreferenceChanged( ) {
		Object[] listeners = this.listenerList.getListenerList();
		for ( int i = listeners.length - 2; i >= 0; i -= 2 ) {
			if ( listeners[i] == ChangeListener.class ) {
				if ( this.preferenceChangeEvent == null ) this.preferenceChangeEvent = new ChangeEvent( this );
				( (ChangeListener) listeners[i + 1] ).stateChanged( this.preferenceChangeEvent );
			}
		}
	}


	/********************************************************************************
	 * 								PRIVATE METHODS
	 *******************************************************************************/

	/**
	 * @param key
	 * @param defaultValue
	 * @return string property
	 */
	private String getStringProperty( String key, String defaultValue ) {
		String string = this.properties.getProperty( key, defaultValue );
		return string;
	}


	/**
	 * @param key
	 * @param defaultValue
	 * @return integer property
	 */
	private int getIntProperty( String key, int defaultValue ) {
		String string = this.properties.getProperty( key, Integer.toString( defaultValue ) );
		if ( string == null ) {
			System.err.println( "WARN: couldn't find integer value under '" + key + "'" );
			return 0;
		}
		return Integer.parseInt( string );
	}


	/**
	 * @param key
	 * @param defaultValue
	 * @return boolean property
	 */
	private boolean getBooleanProperty( String key, boolean defaultValue ) {
		String string = this.properties.getProperty( key, Boolean.toString( defaultValue ) );
		if ( string == null ) {
			System.err.println( "WARN: couldn't find boolean value under '" + key + "'" );
			return false;
		}
		if ( string.toLowerCase().equals( "true" ) || string.toLowerCase().equals( "on" )
				|| string.toLowerCase().equals( "yes" ) || string.toLowerCase().equals( "1" ) )
			return true;
		return false;
	}


	/**
	 * @param key
	 * @param defaultValue
	 * @return color property
	 */
	private Color getColorProperty( String key, Color defaultValue ) {
		String r = Integer.toString( defaultValue.getRed() );
		String g = Integer.toString( defaultValue.getGreen() );
		String b = Integer.toString( defaultValue.getBlue() );
		String colorString = r + " " + g + " " + b;
		String string = this.properties.getProperty( key, colorString );
		if ( string == null ) {
			System.err.println( "WARN: couldn't find color tuplet under '" + key + "'" );
			return Color.BLACK;
		}
		return parseColorString( string );
	}


	/**
	 * @param string
	 * @return color
	 */
	private Color parseColorString( String string ) {
		StringTokenizer st = new StringTokenizer( string, " " );
		Color c;
		try {
			c = new Color( Integer.parseInt( st.nextToken() ), Integer.parseInt( st.nextToken() ), 
					Integer.parseInt( st.nextToken() ) );
		}
		catch ( NoSuchElementException e ) {
			c = Color.BLACK;
			System.err.println( "WARN: invalid color spec '" + string + "' in property file" );
		}
		return c;
	}

}
