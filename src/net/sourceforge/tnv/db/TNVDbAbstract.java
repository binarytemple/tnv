/**
 * Created on Dec 20, 2006
 * @author jgood
 * 
 * Abstract database class for common host methods 
 */

package net.sourceforge.tnv.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sourceforge.jpcap.net.IPPacket;
import net.sourceforge.jpcap.net.LinkLayers;
import net.sourceforge.jpcap.net.Packet;
import net.sourceforge.jpcap.net.PacketFactory;
import net.sourceforge.jpcap.net.RawPacket;
import net.sourceforge.jpcap.net.TCPPacket;
import net.sourceforge.jpcap.net.UDPPacket;
import net.sourceforge.tnv.dialogs.TNVErrorDialog;
import net.sourceforge.tnv.ui.TNVPacket;
import net.sourceforge.tnv.ui.TNVPreferenceData;
import net.sourceforge.tnv.util.TNVUtil;

public abstract class TNVDbAbstract implements TNVDbInterface {

	protected static String PACKET_SUMMARY_TABLE = "packet_summary_table";
	protected static String PACKET_DATA_TABLE = "packet_data_table";
	protected static String HOST_TABLE = "host_table";

	// unique packet id for foreign key
	private int packet_id = 0;
	
	// JDBC prepared statements
	private PreparedStatement 
			insertPacketStmt, 					// insert packet into db
			insertPacketSummaryStmt,			// insert packet into summary table
			insertHostStmt,						// insert host into table
			updateHostStmt,						// update host's frequency
			selectHostStmt,						// get hosts matching host 
			selectHostFrequencyStmt, 			// get frequency of host
			selectTimeFrequencyStmt, 			// get frequency by time period (for slider)
			selectPacketsForHostStmt;			// get packets for a host (for localhost node)

	// Map of local hosts' packets - hostname: map (timestamp: list (packets))
	private Map<String, SortedMap<Timestamp, List<TNVPacket>>> localHostMap = 
		new HashMap<String, SortedMap<Timestamp, List<TNVPacket>>>();

	// List of arrival order for local hosts
	private List<String> localHostArrivalList = new ArrayList<String>();
	
	// Set of remote hosts
	private Set<String> remoteHostList = new HashSet<String>();
	
	// List of arrival order for remote hosts
	private List<String> remoteHostArrivalList = new ArrayList<String>();

	
	// JDBC methods
	/**
	 * Insert packet into the SUMMARY_PACKET_TABLE and add to hosts lists
	 * @param time Timestamp of packet arrival time
	 * @param src String of src address
	 * @param srcport int of src port
	 * @param dst String of dst address
	 * @param dstport int of dst port
	 * @param protocol int of jpcap protocol (IPProtocol)
	 * @param ttl int of time to live
	 * @param len int of length of packet
	 * @param packet RawPacket
	 */
	public void insertPacket(Timestamp time, String src, int srcport, String dst, int dstport, int protocol, int ttl,
			int len, RawPacket packet ) throws SQLException {

		if ( insertPacketStmt == null )
			this.insertPacketStmt = this.getConnection().prepareStatement( 
					"INSERT INTO " + PACKET_DATA_TABLE +
					" (packet_id,packet)" +
					" VALUES (?,?)" );
		
		ResultSet rs = TNVDbUtil.query(this.getConnection(), "SELECT COUNT(1) FROM " + PACKET_DATA_TABLE);
		if ( rs.next() )
			this.packet_id = rs.getInt(1);
		
		this.packet_id++;

		this.insertPacketStmt.setInt( 1, this.packet_id );
		this.insertPacketStmt.setObject( 2, packet );
		this.insertPacketStmt.executeUpdate();
		
		if ( insertPacketSummaryStmt == null )
			this.insertPacketSummaryStmt = this.getConnection().prepareStatement( 
					"INSERT INTO " + PACKET_SUMMARY_TABLE +
					" (timestamp, srcaddr, srcport, dstaddr, dstport, protocol, ttl, length, packet_id)" +
					" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" );
		this.insertPacketSummaryStmt.setTimestamp( 1, time );
		this.insertPacketSummaryStmt.setString( 2, src );
		this.insertPacketSummaryStmt.setInt( 3, srcport );
		this.insertPacketSummaryStmt.setString( 4, dst );
		this.insertPacketSummaryStmt.setInt( 5, dstport );
		this.insertPacketSummaryStmt.setInt( 6, protocol );
		this.insertPacketSummaryStmt.setInt( 7, ttl );
		this.insertPacketSummaryStmt.setInt( 8, len );
		this.insertPacketSummaryStmt.setInt( 9, this.packet_id );
		this.insertPacketSummaryStmt.executeUpdate();
		
		this.addHost(src);
		this.addHost(dst);
		this.addPacket(packet);
		
	}



	/**
	 * Get packet count for a host in a given time period
	 * @param name String of src or dst address
	 * @param start Timestamp of start time
	 * @param end Timestamp of end time
	 * @return number of packets
	 */
	public int getPacketCount( String host, Timestamp start, Timestamp end ) {
		int count = 0;
		try {
			if ( this.selectHostFrequencyStmt == null ) {
				this.selectHostFrequencyStmt = this.getConnection().prepareStatement( "SELECT COUNT(1) FROM " + PACKET_SUMMARY_TABLE
						+ " WHERE (srcaddr = ? OR dstaddr = ?) " + " AND (timestamp >= ? AND timestamp < ?)" );
			}
			
			this.selectHostFrequencyStmt.setString( 1, host );
			this.selectHostFrequencyStmt.setString( 2, host );
			this.selectHostFrequencyStmt.setTimestamp( 3, start );
			this.selectHostFrequencyStmt.setTimestamp( 4, end );
			ResultSet rs = this.selectHostFrequencyStmt.executeQuery();
			if ( rs.next() ) 
				count = rs.getInt( 1 );
			rs.close();
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error getting packet count for host (" + host + "): " + sqlex.getMessage() );
		}
		return count;
	}


	/**
	 * Get packet count for hosts in a given time period
	 * @param name String of src address
	 * @param name String of dst address
	 * @param start Timestamp of start time
	 * @param end Timestamp of end time
	 * @return number of packets
	 */
	public int getPacketCount( String src, String dst, Timestamp start, Timestamp end ) {
		int count = 0;
		try {
			if ( this.selectHostFrequencyStmt == null ) {
				this.selectHostFrequencyStmt = this.getConnection().prepareStatement( "SELECT COUNT(1) FROM " + PACKET_SUMMARY_TABLE
						+ " WHERE (srcaddr = ? OR dstaddr = ?) " + " AND (timestamp >= ? AND timestamp < ?)" );
			}
			
			this.selectHostFrequencyStmt.setString( 1, src );
			this.selectHostFrequencyStmt.setString( 2, dst );
			this.selectHostFrequencyStmt.setTimestamp( 3, start );
			this.selectHostFrequencyStmt.setTimestamp( 4, end );
			ResultSet rs = this.selectHostFrequencyStmt.executeQuery();
			if ( rs.next() ) 
				count = rs.getInt( 1 );
			rs.close();
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error getting packet count for hosts (src: " + src + ", dst" + dst + "): "
					+ sqlex.getMessage() );
		}
		return count;
	}


	/**
	 * Get packet count for a given time period
	 * @param start Timestamp of start time
	 * @param end Timestamp of end time
	 * @return number of packets
	 */
	public int getPacketCount( Timestamp start, Timestamp end ) {
		int count = 0;
		try {
			if ( this.selectTimeFrequencyStmt == null ) {
				this.selectTimeFrequencyStmt = this.getConnection().prepareStatement( "SELECT COUNT(1) FROM " + 
						PACKET_SUMMARY_TABLE + " WHERE (timestamp >= ? AND timestamp < ?)" );
			}
			
			this.selectTimeFrequencyStmt.setTimestamp( 1, start );
			this.selectTimeFrequencyStmt.setTimestamp( 2, end );
			ResultSet rs = this.selectTimeFrequencyStmt.executeQuery();
			if ( rs.next() ) 
				count = rs.getInt( 1 );
			rs.close();
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error getting packet count for times (start: " + start + ", end" + end + "): "
					+ sqlex.getMessage() );
		}
		return count;
	}

	/**
	 * @return list of hosts sorted by count
	 */
	public final List<String> getHostsByFrequency() {
		List<String> hosts = new ArrayList<String>();
		try {
			ResultSet rs = TNVDbUtil.query(this.getConnection(), 
					"SELECT host FROM " + HOST_TABLE + " ORDER BY frequency DESC");
			while (rs.next())
				hosts.add(rs.getString(1));
			rs.close();
		}
		catch (SQLException ex) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "Error getting hosts frequency", ex);
		}
		return hosts;
	}
	

	
	/**
	 * Clear out all saved statements and data 
	 */
	public void clear() {
		this.insertPacketStmt = null;
		this.insertPacketSummaryStmt = null;
		this.insertHostStmt = null;
		this.updateHostStmt = null;
		this.selectHostStmt = null;
		this.selectHostFrequencyStmt = null;
		this.selectTimeFrequencyStmt = null; 
		this.selectPacketsForHostStmt = null;

		this.localHostMap.clear();
		this.localHostArrivalList.clear();
		this.remoteHostList.clear();
		this.remoteHostArrivalList.clear();
	}
	
	/**
	 * inserts a host into the local or remote set
	 * @param name the String name to add to lists
	 */
	public void addHost( String name ) {
		if ( TNVUtil.isOnHomeNet( name, TNVPreferenceData.getInstance().getHomeNet() ) ) {
			if ( ! this.localHostMap.containsKey(name) ) {
				this.localHostMap.put( name, new TreeMap<Timestamp, List<TNVPacket>>() );
				if ( ! this.localHostArrivalList.contains(name) )
					this.localHostArrivalList.add(name);
			}
		}
		else {
			if ( ! this.remoteHostList.contains(name) ) {
				this.remoteHostList.add( name );
				if ( ! this.remoteHostArrivalList.contains(name) )
					this.remoteHostArrivalList.add(name);
			}
		}
		
		try {
			if ( this.selectHostStmt == null ) 
				this.selectHostStmt = this.getConnection().prepareStatement(
						"SELECT frequency from " + HOST_TABLE + " WHERE HOST = ?");
			if ( this.insertHostStmt == null ) 
				this.insertHostStmt = this.getConnection().prepareStatement(
						"INSERT INTO " + HOST_TABLE + " (host,frequency) VALUES(?,?)");
			if ( this.updateHostStmt == null ) 
				this.updateHostStmt = this.getConnection().prepareStatement(
						"UPDATE " + HOST_TABLE + " SET frequency = ? WHERE HOST = ?");
			
			this.selectHostStmt.setString(1, name);
			ResultSet rs = this.selectHostStmt.executeQuery();
			if ( rs.next() ) {
				int count = rs.getInt( 1 ) + 1;
				this.updateHostStmt.setInt(1, count);
				this.updateHostStmt.setString(2, name);
				this.updateHostStmt.execute();
			}
			else {
				this.insertHostStmt.setString(1, name);
				this.insertHostStmt.setInt(2, 1);
				this.insertHostStmt.execute();
			}
			rs.close();
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error inserting host: " + sqlex.getMessage() );
		}

	}


	/**
	 *
	 * @return the List<String> TNVDbAbstract.java
	 */
	public final List<String> getLocalHostArrivalList() {
		return this.localHostArrivalList;
	}

	/**
	 *
	 * @return the Map<String,SortedMap<Timestamp,List<TNVPacket>>> TNVDbAbstract.java
	 */
	public final Map<String, SortedMap<Timestamp, List<TNVPacket>>> getLocalHostMap() {
		return this.localHostMap;
	}

	/**
	 * @return the localHostMap
	 * @param the host to get the map for
	 */
	public final SortedMap<Timestamp, List<TNVPacket>> getLocalHostMap( String host ) {
		return this.localHostMap.get( host );
	}

	
	/**
	 *
	 * @return the List<String> TNVDbAbstract.java
	 */
	public final List<String> getRemoteHostArrivalList() {
		return this.remoteHostArrivalList;
	}

	/**
	 *
	 * @return the Set<String> TNVDbAbstract.java
	 */
	public final Set<String> getRemoteHostList() {
		return this.remoteHostList;
	}

	
	
	/**
	 * inserts a packet into local host map
	 * @param name the String name to add to lists
	 */
	protected void addPacket( RawPacket rawPacket ) {
		try {
			Timestamp time = new Timestamp( ( rawPacket.getTimeval().getDate() ).getTime() );
			Packet packet = PacketFactory.dataToPacket( LinkLayers.IEEE802, rawPacket.getData() );
			IPPacket ipPacket = (IPPacket) packet;
			String srcAddr = ipPacket.getSourceAddress();
			String dstAddr = ipPacket.getDestinationAddress();
			TNVPacket tnvpacket;
			if ( ipPacket instanceof TCPPacket ) {
				TCPPacket tcpPacket = (TCPPacket) ipPacket;
				tnvpacket = new TNVPacket( time, srcAddr, tcpPacket.getSourcePort(), 
						dstAddr, tcpPacket.getDestinationPort(), ipPacket.getProtocol(),
						ipPacket.getTimeToLive(), ipPacket.getLength(),
						tcpPacket.isSyn(), tcpPacket.isAck(), tcpPacket.isFin(),
						tcpPacket.isUrg(), tcpPacket.isPsh(), tcpPacket.isRst());
			}
			else if ( ipPacket instanceof UDPPacket ) {
				UDPPacket udpPacket = (UDPPacket) ipPacket;
				tnvpacket = new TNVPacket( time, srcAddr, udpPacket.getSourcePort(), 
						dstAddr, udpPacket.getDestinationPort(), ipPacket.getProtocol(),
						ipPacket.getTimeToLive(), ipPacket.getLength() );
			}
			else {
				tnvpacket = new TNVPacket( time, srcAddr, -1, dstAddr, -1, ipPacket.getProtocol(),
						ipPacket.getTimeToLive(), ipPacket.getLength() );
			}			
			
			List<TNVPacket> l;
			if ( TNVUtil.isOnHomeNet( srcAddr, TNVPreferenceData.getInstance().getHomeNet() ) ) {
				if ( this.getLocalHostMap().get( srcAddr ).containsKey( time ) )
					l = this.getLocalHostMap().get( srcAddr ).get( time );
				else
					l = new ArrayList<TNVPacket>();
				l.add( tnvpacket );
				this.getLocalHostMap().get( srcAddr ).put( time, l );
			}
			if ( TNVUtil.isOnHomeNet( dstAddr, TNVPreferenceData.getInstance().getHomeNet() ) ) {
				if ( this.getLocalHostMap().get( dstAddr ).containsKey( time ) )
					l = this.getLocalHostMap().get( dstAddr ).get( time );
				else
					l = new ArrayList<TNVPacket>();
				l.add( tnvpacket );			
				this.getLocalHostMap().get( dstAddr ).put( time, l );
			}

		}
		catch ( Exception e ) {
			System.out.println( "Error adding packet to TNV data: " + e.getMessage() );
		}

	}
	
}
