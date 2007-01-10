/**
 * Created on Mar 16, 2005
 * @author jgood
 * 
 * This class handles state information for the Visualizations
 */
package net.sourceforge.tnv.ui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.tnv.db.TNVDbUtil;
import net.sourceforge.tnv.util.TNVUtil;

/**
 * TNVModel
 */
public class TNVModel {

	private PropertyChangeSupport changes = new PropertyChangeSupport( this );

	public static enum DisplayLinkDirection {
		INGRESS,EGRESS,BOTH;
	}
	
	public static enum DisplayPacketType {
		SRC,DST,BOTH;
	}

	public static enum ValueModifier {
		LESS_THAN, GREATER_THAN, EQUAL_TO;
	}
	
	// Property strings
	public final static String PROPERTY_START_TIME = "startTime";
	public final static String PROPERTY_END_TIME = "endTime";

	public final static String PROPERTY_VISIBLE_START_TIME = "visibleStartTime";
	public final static String PROPERTY_VISIBLE_END_TIME = "visibleEndTime";

	public final static String PROPERTY_IS_TIME_ADJUSTING = "isAdjusting";

	public final static String PROPERTY_HIGHLIGHT_HOST_NODES = "highlightNodes";

	public final static String PROPERTY_DETAILS_FOR_HOST_NODES = "detailNodes";

	public final static String PROPERTY_SHOW_TCP_LINKS = "showTCPlinks";
	public final static String PROPERTY_SHOW_UDP_LINKS = "showUDPlinks";
	public final static String PROPERTY_SHOW_ICMP_LINKS = "showICMPlinks";

	public final static String PROPERTY_HIGHLIGHT_TCP_LINKS = "highlightTCPlinks";
	public final static String PROPERTY_HIGHLIGHT_UDP_LINKS = "highlightUDPlinks";
	public final static String PROPERTY_HIGHLIGHT_ICMP_LINKS = "highlightICMPlinks";

	public final static String PROPERTY_LINK_DIRECTION = "linkDirection";

	public final static String PROPERTY_SHOW_TCP_PACKETS = "showTCPpackets";
	public final static String PROPERTY_SHOW_UDP_PACKETS = "showUDPpackets";
	public final static String PROPERTY_SHOW_ICMP_PACKETS = "showICMPpackets";

	public final static String PROPERTY_HIGHLIGHT_TCP_PACKETS = "highlightTCPpackets";
	public final static String PROPERTY_HIGHLIGHT_UDP_PACKETS = "highlightUDPpackets";
	public final static String PROPERTY_HIGHLIGHT_ICMP_PACKETS = "highlightICMPpackets";

	public final static String PROPERTY_HIGHLIGHT_SYN_PACKETS = "highlightSYNpackets";
	public final static String PROPERTY_HIGHLIGHT_ACK_PACKETS = "highlightACKpackets";
	public final static String PROPERTY_HIGHLIGHT_FIN_PACKETS = "highlightFINpackets";
	public final static String PROPERTY_HIGHLIGHT_PSH_PACKETS = "highlightPSHpackets";
	public final static String PROPERTY_HIGHLIGHT_URG_PACKETS = "highlightURGpackets";
	public final static String PROPERTY_HIGHLIGHT_RST_PACKETS = "highlightRSTpackets";

	public final static String PROPERTY_SETUP = "setupNewHosts";

	public final static String PROPERTY_SHOW_PORT_TYPE = "portTypeToShow";
	public final static String PROPERTY_SHOW_PORTS = "portsToShow";

	public final static String PROPERTY_SHOW_TTL_MODIFIER = "showTtlModifier";
	public final static String PROPERTY_SHOW_TTL_VALUE = "showTtlValue";
	
	public final static String PROPERTY_SHOW_LENGTH_MODIFIER = "showLengthModifier";
	public final static String PROPERTY_SHOW_LENGTH_VALUE = "showLengthValue";

	public final static String PROPERTY_HIGHLIGHT_PORT_TYPE = "portTypeToHighlight";
	public final static String PROPERTY_HIGHLIGHT_PORTS = "portsToHighlight";

	public final static String PROPERTY_HIGHLIGHT_TTL_MODIFIER = "highlightTtlModifier";
	public final static String PROPERTY_HIGHLIGHT_TTL_VALUE = "highlightTtlValue";
	
	public final static String PROPERTY_HIGHLIGHT_LENGTH_MODIFIER = "highlightLengthModifier";
	public final static String PROPERTY_HIGHLIGHT_LENGTH_VALUE = "highlightLengthValue";

	public final static String PROPERTY_SHOW_ALL_PORTS = "showAllPorts";

	
	private boolean setupNewHosts = false;	// if the list of hosts has been setup

	private int arrivalOrder = 0;			// Arrival order of the hosts

	// start and end timestamps for entire data set and the current viewing time interval
	private Timestamp startTime, endTime, currentStartTime, currentEndTime;
	private boolean isAdjusting = false;	// if timesliders are adjusting

	private Set<TNVLocalHostCell> detailsForHostNodes = new HashSet<TNVLocalHostCell>();

	private Set<TNVLocalHostCell> highlightHostNodes = new HashSet<TNVLocalHostCell>();

	// Link state
	private boolean highlightTCPlinks, highlightUDPlinks, highlightICMPlinks,
			showTCPlinks, showUDPlinks, showICMPlinks;
	private DisplayLinkDirection linkDirection;

	// Packet state
	private boolean showTCPpackets, showUDPpackets, showICMPpackets;

	private boolean highlightTCPpackets, highlightUDPpackets, highlightICMPpackets,
		highlightSYNpackets, highlightACKpackets, highlightFINpackets,
		highlightPSHpackets, highlightURGpackets, highlightRSTpackets;
	
	private Set showPorts;
	private DisplayPacketType showPortDirection;
	private int showTtlValue, showLengthValue;
	private ValueModifier showTtlModifier, showLengthModifier;

	private Set highlightPorts;
	private DisplayPacketType highlightPortDirection;
	private int highlightTtlValue, highlightLengthValue;
	private ValueModifier highlightTtlModifier, highlightLengthModifier;
	
	// Singleton instance variable
	private static TNVModel instance = new TNVModel();


	// Private constructor
	private TNVModel() {
	}


	// Singleton accessor
	public static TNVModel getInstance( ) {
		return instance;
	}


	// Load database into memory
	public void setupData( ) {
		// set current display times to the entire data set
		setStartTime( TNVDbUtil.getInstance().getMinTime() );
		setEndTime( TNVDbUtil.getInstance().getMaxTime() );
		setCurrentStartTime( this.getStartTime() );
		setCurrentEndTime( this.getEndTime() );
		setSetupNewHosts( true );
	}


	// Reset data
	public void resetData( ) {
		// set current display times to the entire data set
		setAdjusting(true);
		setStartTime( TNVDbUtil.getInstance().getMinTime() );
		setEndTime( TNVDbUtil.getInstance().getMaxTime() );
		setCurrentStartTime( this.getStartTime() );
		setCurrentEndTime( this.getEndTime() );
		setAdjusting(false);
	}

	
	// Clear out the necessary data to reset all components
	public final void clear( ) {
		// try {
		// TNVMemoryDB.getInstance().closeConnection();
		// } catch (Exception e) { }
		setStartTime( null );
		setEndTime( null );
		setCurrentStartTime( null );
		setCurrentEndTime( null );
		setSetupNewHosts( false );
	}


	/**
	 * @param setupNewHosts The buildingHosts to set.
	 */
	public final void setSetupNewHosts( boolean b ) {
		boolean orig = this.setupNewHosts;
		this.setupNewHosts = b;
		this.changes.firePropertyChange( TNVModel.PROPERTY_SETUP, orig, this.setupNewHosts );
	}


	/**
	 * @return Returns the currentStartTime.
	 */
	public final Timestamp getCurrentStartTime( ) {
		return this.currentStartTime;
	}

	/**
	 * @param currentStartTime The currentStartTime to set.
	 */
	public final void setCurrentStartTime( Timestamp s ) {
		Timestamp orig = this.currentStartTime;
		this.currentStartTime = s;
		this.changes.firePropertyChange( TNVModel.PROPERTY_VISIBLE_START_TIME, orig, this.currentStartTime );
	}


	/**
	 * @return Returns the currentEndTime.
	 */
	public final Timestamp getCurrentEndTime( ) {
		return this.currentEndTime;
	}

	/**
	 * @param currentEndTime The currentEndTime to set.
	 */
	public final void setCurrentEndTime( Timestamp e ) {
		Timestamp orig = this.currentEndTime;
		this.currentEndTime = e;
		this.changes.firePropertyChange( TNVModel.PROPERTY_VISIBLE_END_TIME, orig, this.currentEndTime );
	}

	
	/**
	 * @return the isAdjusting
	 */
	public final boolean isAdjusting( ) {
		return this.isAdjusting;
	}
	
	/**
	 * @param isAdjusting the boolean isAdjusting to set
	 */
	public final void setAdjusting( boolean a ) {
		boolean orig = this.isAdjusting;
		this.isAdjusting = a;
		this.changes.firePropertyChange( TNVModel.PROPERTY_IS_TIME_ADJUSTING, orig, this.isAdjusting );
	}


	/**
	 * @return Returns the startTime.
	 */
	public final Timestamp getStartTime( ) {
		return this.startTime;
	}

	/**
	 * @param startTime The startTime to set.
	 */
	public final void setStartTime( Timestamp s ) {
		Timestamp orig = this.startTime;
		// round down to nearest minute
		if ( s != null )
			this.startTime = new Timestamp( s.getTime() - ( s.getTime() % TNVUtil.ONE_MIN ) );
		else
			this.startTime = null;
		this.changes.firePropertyChange( TNVModel.PROPERTY_START_TIME, orig, this.startTime );
	}


	/**
	 * @return Returns the endTime.
	 */
	public final Timestamp getEndTime( ) {
		return this.endTime;
	}

	/**
	 * @param endTime The endTime to set.
	 */
	public final void setEndTime( Timestamp e ) {
		Timestamp orig = this.endTime;
		// round up to nearest minute
		if ( e != null )
			this.endTime = new Timestamp( e.getTime() - ( e.getTime() % TNVUtil.ONE_MIN ) + TNVUtil.ONE_MIN );
		else
			this.endTime = null;
		this.changes.firePropertyChange( TNVModel.PROPERTY_END_TIME, orig, this.endTime );
	}


	/**
	 * @return Returns the detailsForHostNodes.
	 */
	public final Set<TNVLocalHostCell> getDetailsForHostNodes( ) {
		return this.detailsForHostNodes;
	}

	/**
	 * @param HostNodes The Set to set detailsForHostNodes.
	 */
	public final void setDetailsForHostNodes( Set<TNVLocalHostCell> l ) {
		this.detailsForHostNodes.clear();
		this.detailsForHostNodes.addAll( l );
		this.changes.firePropertyChange( TNVModel.PROPERTY_DETAILS_FOR_HOST_NODES, null, this.detailsForHostNodes );
	}


	/**
	 * @return Returns the highlightHostNodes.
	 */
	public final Set<TNVLocalHostCell> getHighlightHostNodes( ) {
		return this.highlightHostNodes;
	}

	/**
	 * @param HostNodes The HostNodes to add to highlightHostNodes.
	 */
	public final void setHighlightHostNodes( Set<TNVLocalHostCell> l ) {
		this.highlightHostNodes.clear();
		this.highlightHostNodes.addAll( l );
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_HOST_NODES, null, this.highlightHostNodes );
	}


	/**
	 * @return Returns the showTCPlinks.
	 */
	public final boolean isHighlightTCPlinks( ) {
		return this.highlightTCPlinks;
	}

	/**
	 * @param showTCPlinks The showTCPlinks to set.
	 */
	public final void setHighlightTCPlinks( boolean show ) {
		boolean old = this.highlightTCPlinks;
		this.highlightTCPlinks = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_TCP_LINKS, old, this.highlightTCPlinks );
	}


	/**
	 * @return Returns the showUDPlinks.
	 */
	public final boolean isHighlightUDPlinks( ) {
		return this.highlightUDPlinks;
	}

	/**
	 * @param showUDPlinks The showUDPlinks to set.
	 */
	public final void setHighlightUDPlinks( boolean show ) {
		boolean old = this.highlightUDPlinks;
		this.highlightUDPlinks = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_UDP_LINKS, old, this.highlightUDPlinks );
	}


	/**
	 * @return Returns the showICMPlinks.
	 */
	public final boolean isHighlightICMPlinks( ) {
		return this.highlightICMPlinks;
	}

	/**
	 * @param showUDPlinks The showICMPlinks to set.
	 */
	public final void setHighlightICMPlinks( boolean show ) {
		boolean old = this.highlightICMPlinks;
		this.highlightICMPlinks = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_ICMP_LINKS, old, this.highlightICMPlinks );
	}


	/**
	 * @return Returns the showTCPlinks.
	 */
	public final boolean isShowTCPlinks( ) {
		return this.showTCPlinks;
	}

	/**
	 * @param showTCPlinks The showTCPlinks to set.
	 */
	public final void setShowTCPlinks( boolean show ) {
		boolean old = this.showTCPlinks;
		this.showTCPlinks = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_SHOW_TCP_LINKS, old, this.showTCPlinks );
	}


	/**
	 * @return Returns the showUDPlinks.
	 */
	public final boolean isShowUDPlinks( ) {
		return this.showUDPlinks;
	}

	/**
	 * @param showUDPlinks The showUDPlinks to set.
	 */
	public final void setShowUDPlinks( boolean show ) {
		boolean old = this.showUDPlinks;
		this.showUDPlinks = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_SHOW_UDP_LINKS, old, this.showUDPlinks );
	}


	/**
	 * @return Returns the showICMPlinks.
	 */
	public final boolean isShowICMPlinks( ) {
		return this.showICMPlinks;
	}

	/**
	 * @param showUDPlinks The showICMPlinks to set.
	 */
	public final void setShowICMPlinks( boolean show ) {
		boolean old = this.showICMPlinks;
		this.showICMPlinks = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_SHOW_ICMP_LINKS, old, this.showICMPlinks );
	}

	
	/**
	 * @return Returns the linkDirection.
	 */
	public final DisplayLinkDirection getLinkDirection( ) {
		return this.linkDirection;
	}

	/**
	 * @param linkDirection The linkDirection to set.
	 */
	public final void setLinkDirection( DisplayLinkDirection dir ) {
		DisplayLinkDirection old = this.linkDirection;
		this.linkDirection = dir;
		this.changes.firePropertyChange( TNVModel.PROPERTY_LINK_DIRECTION, old, this.linkDirection );
	}

	
	/**
	 * @return Returns the showTCPpackets.
	 */
	public final boolean isShowTCPpackets( ) {
		return this.showTCPpackets;
	}

	/**
	 * @param showTCPpackets The showTCPpackets to set.
	 */
	public final void setShowTCPpackets( boolean show ) {
		boolean old = this.showTCPpackets;
		this.showTCPpackets = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_SHOW_TCP_PACKETS, old, this.showTCPpackets );
	}


	/**
	 * @return Returns the showUDPpackets.
	 */
	public final boolean isShowUDPpackets( ) {
		return this.showUDPpackets;
	}

	/**
	 * @param showUDPpackets The showUDPpackets to set.
	 */
	public final void setShowUDPpackets( boolean show ) {
		boolean old = this.showUDPpackets;
		this.showUDPpackets = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_SHOW_UDP_PACKETS, old, this.showUDPpackets );
	}


	/**
	 * @return Returns the showICMPpackets.
	 */
	public final boolean isShowICMPpackets( ) {
		return this.showICMPpackets;
	}

	/**
	 * @param showUDPpackets The showICMPpackets to set.
	 */
	public final void setShowICMPpackets( boolean show ) {
		boolean old = this.showICMPpackets;
		this.showICMPpackets = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_SHOW_ICMP_PACKETS, old, this.showICMPpackets );
	}


	/**
	 * @return Returns the showTCPpackets.
	 */
	public final boolean isHighlightTCPpackets( ) {
		return this.highlightTCPpackets;
	}

	/**
	 * @param showTCPpackets The showTCPpackets to set.
	 */
	public final void setHighlightTCPpackets( boolean show ) {
		boolean old = this.highlightTCPpackets;
		this.highlightTCPpackets = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_TCP_PACKETS, old, this.highlightTCPpackets );
	}


	/**
	 * @return Returns the showSYNpackets.
	 */
	public final boolean isHighlightSYNpackets( ) {
		return this.highlightSYNpackets;
	}

	/**
	 * @param showSYNpackets The showSYNpackets to set.
	 */
	public final void setHighlightSYNpackets( boolean show ) {
		boolean old = this.highlightSYNpackets;
		this.highlightSYNpackets = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_SYN_PACKETS, old, this.highlightSYNpackets );
	}


	/**
	 * @return Returns the showACKpackets.
	 */
	public final boolean isHighlightACKpackets( ) {
		return this.highlightACKpackets;
	}

	/**
	 * @param showACKpackets The showACKpackets to set.
	 */
	public final void setHighlightACKpackets( boolean show ) {
		boolean old = this.highlightACKpackets;
		this.highlightACKpackets = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_ACK_PACKETS, old, this.highlightACKpackets );
	}


	/**
	 * @return Returns the showFINpackets.
	 */
	public final boolean isHighlightFINpackets( ) {
		return this.highlightFINpackets;
	}

	/**
	 * @param showFINpackets The showFINpackets to set.
	 */
	public final void setHighlightFINpackets( boolean show ) {
		boolean old = this.highlightFINpackets;
		this.highlightFINpackets = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_FIN_PACKETS, old, this.highlightFINpackets );
	}


	/**
	 * @return Returns the showURGpackets.
	 */
	public final boolean isHighlightURGpackets( ) {
		return this.highlightURGpackets;
	}

	/**
	 * @param showURGpackets The showURGpackets to set.
	 */
	public final void setHighlightURGpackets( boolean show ) {
		boolean old = this.highlightURGpackets;
		this.highlightURGpackets = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_URG_PACKETS, old, this.highlightURGpackets );
	}


	/**
	 * @return Returns the showPSHpackets.
	 */
	public final boolean isHighlightPSHpackets( ) {
		return this.highlightPSHpackets;
	}

	/**
	 * @param showPSHpackets The showPSHpackets to set.
	 */
	public final void setHighlightPSHpackets( boolean show ) {
		boolean old = this.highlightPSHpackets;
		this.highlightPSHpackets = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_PSH_PACKETS, old, this.highlightPSHpackets );
	}


	/**
	 * @return Returns the showRSTpackets.
	 */
	public final boolean isHighlightRSTpackets( ) {
		return this.highlightRSTpackets;
	}

	/**
	 * @param showRSTpackets The showRSTpackets to set.
	 */
	public final void setHighlightRSTpackets( boolean show ) {
		boolean old = this.highlightRSTpackets;
		this.highlightRSTpackets = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_RST_PACKETS, old, this.highlightRSTpackets );
	}


	/**
	 * @return Returns the showUDPpackets.
	 */
	public final boolean isHighlightUDPpackets( ) {
		return this.highlightUDPpackets;
	}

	/**
	 * @param showUDPpackets The showUDPpackets to set.
	 */
	public final void setHighlightUDPpackets( boolean show ) {
		boolean old = this.highlightUDPpackets;
		this.highlightUDPpackets = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_UDP_PACKETS, old, this.highlightUDPpackets );
	}


	/**
	 * @return Returns the showICMPpackets.
	 */
	public final boolean isHighlightICMPpackets( ) {
		return this.highlightICMPpackets;
	}

	/**
	 * @param showUDPpackets The showICMPpackets to set.
	 */
	public final void setHighlightICMPpackets( boolean show ) {
		boolean old = this.highlightICMPpackets;
		this.highlightICMPpackets = show;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_ICMP_PACKETS, old, this.highlightICMPpackets );
	}

	
	/**
	 * @return Returns the portsToShow.
	 */
	public final Set getShowPorts( ) {
		return this.showPorts;
	}

	/**
	 * @param showPorts The portsToShow to set.
	 */
	public final void setShowPorts( Set ports ) {
		this.showPorts = ports;
		this.changes.firePropertyChange( TNVModel.PROPERTY_SHOW_PORTS, null, this.showPorts );
	}


	/**
	 * @return Returns the portTypeToShow.
	 */
	public final DisplayPacketType getShowPortDirection( ) {
		return this.showPortDirection;
	}

	/**
	 * @param showPortDirection The portTypeToShow to set.
	 */
	public final void setShowPortDirection( DisplayPacketType type ) {
		DisplayPacketType old = this.showPortDirection;
		this.showPortDirection = type;
		this.changes.firePropertyChange( TNVModel.PROPERTY_SHOW_PORT_TYPE, old, this.showPortDirection );
	}

	
	/**
	 *
	 * @return the int TNVModel.java
	 */
	public final int getShowLengthValue() {
		return this.showLengthValue;
	}

	/**
	 *
	 * @param showLengthValue the int showLengthValue to set
	 */
	public final void setShowLengthValue(int showLengthValue) {
		int old = this.showLengthValue;
		this.showLengthValue = showLengthValue;
		this.changes.firePropertyChange( TNVModel.PROPERTY_SHOW_LENGTH_VALUE, old, this.showLengthValue );
	}

	/**
	 *
	 * @return the ValueModifier TNVModel.java
	 */
	public final ValueModifier getShowLengthModifier() {
		return this.showLengthModifier;
	}

	/**
	 *
	 * @param showLengthModifier the ValueModifier showLengthModifier to set
	 */
	public final void setShowLengthModifier(ValueModifier showLengthModifier) {
		ValueModifier old = this.showLengthModifier;
		this.showLengthModifier = showLengthModifier;
		this.changes.firePropertyChange( TNVModel.PROPERTY_SHOW_LENGTH_MODIFIER, old, this.showLengthModifier );
	}


	/**
	 *
	 * @return the int TNVModel.java
	 */
	public final int getShowTtlValue() {
		return this.showTtlValue;
	}

	/**
	 *
	 * @param showTtlValue the int showTtlValue to set
	 */
	public final void setShowTtlValue(int showTtlValue) {
		int old = this.showTtlValue;
		this.showTtlValue = showTtlValue;
		this.changes.firePropertyChange( TNVModel.PROPERTY_SHOW_TTL_VALUE, old, this.showTtlValue );
	}

	/**
	 *
	 * @return the ValueModifier TNVModel.java
	 */
	public final ValueModifier getShowTtlModifier() {
		return this.showTtlModifier;
	}

	/**
	 *
	 * @param showTtlModifier the ValueModifier showTtlModifier to set
	 */
	public final void setShowTtlModifier(ValueModifier showTtlModifier) {
		ValueModifier old = this.showTtlModifier;
		this.showTtlModifier = showTtlModifier;
		this.changes.firePropertyChange( TNVModel.PROPERTY_SHOW_TTL_MODIFIER, old, this.showTtlModifier );
	}

	
	/**
	 * @return Returns the portsToHighlight.
	 */
	public final Set getHighlightPorts( ) {
		return this.highlightPorts;
	}

	/**
	 * @param highlightPorts The portsToHighlight to set.
	 */
	public final void setHighlightPorts( Set ports ) {
		this.highlightPorts = ports;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_PORTS, null, this.highlightPorts );
	}


	/**
	 * @return Returns the portTypeToHighlight.
	 */
	public final DisplayPacketType getHighlightPortDirection( ) {
		return this.highlightPortDirection;
	}

	/**
	 * @param highlightPortDirection The portTypeToHighlight to set.
	 */
	public final void setHighlightPortDirection( DisplayPacketType type ) {
		DisplayPacketType old = this.highlightPortDirection;
		this.highlightPortDirection = type;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_PORT_TYPE, old, this.highlightPortDirection );
	}

	
	/**
	 *
	 * @return the int TNVModel.java
	 */
	public final int getHighlightLengthValue() {
		return this.highlightLengthValue;
	}

	/**
	 *
	 * @param highlightLengthValue the int highlightLengthValue to set
	 */
	public final void setHighlightLengthValue(int highlightLengthValue) {
		int old = this.highlightLengthValue;
		this.highlightLengthValue = highlightLengthValue;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_LENGTH_VALUE, old, this.highlightLengthValue );
	}

	/**
	 *
	 * @return the ValueModifier TNVModel.java
	 */
	public final ValueModifier getHighlightLengthModifier() {
		return this.highlightLengthModifier;
	}

	/**
	 *
	 * @param highlightLengthModifier the ValueModifier highlightLengthModifier to set
	 */
	public final void setHighlightLengthModifier(ValueModifier highlightLengthModifier) {
		ValueModifier old = this.highlightLengthModifier;
		this.highlightLengthModifier = highlightLengthModifier;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_LENGTH_MODIFIER, old, this.highlightLengthModifier );
	}


	/**
	 *
	 * @return the int TNVModel.java
	 */
	public final int getHighlightTtlValue() {
		return this.highlightTtlValue;
	}

	/**
	 *
	 * @param highlightTtlValue the int highlightTtlValue to set
	 */
	public final void setHighlightTtlValue(int highlightTtlValue) {
		int old = this.highlightTtlValue;
		this.highlightTtlValue = highlightTtlValue;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_TTL_VALUE, old, this.highlightTtlValue );
	}

	/**
	 *
	 * @return the ValueModifier TNVModel.java
	 */
	public final ValueModifier getHighlightTtlModifier() {
		return this.highlightTtlModifier;
	}

	/**
	 *
	 * @param highlightTtlModifier the ValueModifier highlightTtlModifier to set
	 */
	public final void setHighlightTtlModifier(ValueModifier highlightTtlModifier) {
		ValueModifier old = this.highlightTtlModifier;
		this.highlightTtlModifier = highlightTtlModifier;
		this.changes.firePropertyChange( TNVModel.PROPERTY_HIGHLIGHT_TTL_MODIFIER, old, this.highlightTtlModifier );
	}




	/**
	 * Show all ports for all hosts (does not really belong here...).
	 */
	public final void showAllPorts( ) {
		this.changes.firePropertyChange( TNVModel.PROPERTY_SHOW_ALL_PORTS, false, true );
	}


	/************************************************************************
	 * Add/Remove property change listeners using Beans
	 ************************************************************************/
	
	/**
	 * @param listener The PropertyChangeListener to add.
	 */
	public void addPropertyChangeListener( PropertyChangeListener l ) {
		this.changes.addPropertyChangeListener( l );
	}

	/**
	 * @param property The Property to listen for.
	 * @param listener The PropertyChangeListener to add.
	 */
	public void addPropertyChangeListener( String prop, PropertyChangeListener l ) {
		this.changes.addPropertyChangeListener( prop, l );
	}

	/**
	 * @param properties The Properties to listen for.
	 * @param listeners The PropertyChangeListeners to add.
	 */
	public void addPropertyChangeListener( String[] props, PropertyChangeListener l ) {
		for ( String element : props )
			this.changes.addPropertyChangeListener( element, l );
	}

	/**
	 * @param listener The PropertyChangeListener to remove.
	 */
	public void removePropertyChangeListener( PropertyChangeListener l ) {
		this.changes.removePropertyChangeListener( l );
	}
	
}
