/**
 * Created on Nov 1, 2005 
 * @author jgood
 * 
 * Embedded database class for internal data
 */
package net.sourceforge.tnv;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.jpcap.net.IPPacket;
import net.sourceforge.jpcap.net.LinkLayers;
import net.sourceforge.jpcap.net.Packet;
import net.sourceforge.jpcap.net.PacketFactory;
import net.sourceforge.jpcap.net.RawPacket;
import net.sourceforge.jpcap.net.TCPPacket;
import net.sourceforge.jpcap.net.UDPPacket;
import net.sourceforge.jpcap.util.TcpdumpWriter;

/**
 * TNVDbEmbedded
 */
public class TNVDbEmbedded extends TNVDbAbstract implements TNVDbInterface {

	private static String PACKET_TABLE = "packet_table";

	private static String JDBC_DRIVER = "org.hsqldb.jdbcDriver";
	private static String JDBC_CONNECT = "jdbc:hsqldb:file:"; 
	
	
	// JDBC Connection
	private Connection conn = null;

	// JDBC prepared statements
	private PreparedStatement 
			insertPacketStmt, 					// insert packet into db
			selectDistinctHostsStmt, 			// get distinct hosts
			selectPacketSummaryStmt,			// get summary of packets 
			selectHostFrequencyStmt, 			// get frequency of host
			selectTimeFrequencyStmt, 			// get frequency by time period (for slider)
			selectHostTalkersStmt,				// get all talkers for a given host (for localhost nodes)
			selectLinksStmt, 					// get links (for visualization links)
			selectAllInPortsStmt,				// get all incoming packets (for port details)
			selectAllOutPortsStmt,				// get all outgoing packets (for port details)
			selectInPortsStmt,					// get incoming packets for a host (for port details)
			selectOutPortsStmt,					// get outgoing packets for a host (for port details)
			selectPacketsForHostStmt,			// get packets for a host (for localhost node)
			selectPacketListStmt;				// get packet details (for detail window)

	// Singleton instance
	private static TNVDbInterface instance = new TNVDbEmbedded();


	/**
	 * Empty, private constructor
	 */
	private TNVDbEmbedded() { }


	/**
	 * Singleton
	 * @return this
	 */
	public static TNVDbInterface getInstance( ) {
		return instance;
	}


	/**
	 * Load the Database Engine JDBC driver and opens connection to a database
	 * @param directoryPath the directory to open
	 * @param user the username to log in as
	 * @param passwd the password to log in with
	 */
	public void openConnection( String path, String user, String passwd ) throws SQLException {
		try {
			Class.forName( JDBC_DRIVER ).newInstance();
		}
		catch ( Exception e ) {
			System.out.println( "Error loading JDBC driver (" + JDBC_DRIVER + "): " + e.getMessage() );
		}
		this.conn = DriverManager.getConnection( JDBC_CONNECT + path, user, passwd );

		// create tables if necessary
		ResultSet rs = this.conn.getMetaData().getTables(null, null, null, new String[]{"TABLE"});
		if ( ! rs.next() )
			this.createTables();
	}


	/**
	 * Closes connection to database safely
	 */
	public void closeConnection( ) throws SQLException {
		if ( this.conn != null ) {
			TNVDbUtil.update( conn, "SHUTDOWN" );
			this.conn.close();			// if there are no other open connection
			this.conn = null;
		}
		this.insertPacketStmt = null;
		this.selectHostFrequencyStmt = null;
		this.selectTimeFrequencyStmt = null; 
		this.selectLinksStmt = null;
		this.selectPacketListStmt = null;
		this.selectDistinctHostsStmt = null;
		this.selectPacketSummaryStmt = null;
		this.selectHostTalkersStmt = null;
		this.selectPacketsForHostStmt = null;
		this.selectAllInPortsStmt = null;
		this.selectAllOutPortsStmt = null;
		this.selectInPortsStmt = null;
		this.selectOutPortsStmt = null;
		
		this.clearHosts();
	}


	/**
	 * Checks to see if there is an open connection
	 * @return if there is a connection
	 */
	public boolean isConnection( ) {
		if ( this.conn != null ) 
			return true;
		return false;
	}


	/**
	 * Create full table including packet object
	 */
	private void createTables( ) throws SQLException {
		// Cached table of packets
		TNVDbUtil.update( this.conn, 
				"CREATE TABLE " + PACKET_TABLE + " ( " + 
				"id INTEGER IDENTITY PRIMARY KEY, " +
				"timestamp TIMESTAMP, " +
				"srcaddr VARCHAR, " + 	// source address
				"srcport INTEGER, " + 
				"dstaddr VARCHAR, " + 	// destination address
				"dstport INTEGER, " + 
				"protocol INTEGER, " + 	// jpcap protocol int
				"ttl INTEGER, " + 		// TTL value
				"length INTEGER, " + 	// packet length
				"packet OBJECT )" 		// the packet itself
		);
		// Index
		TNVDbUtil.update( this.conn, "CREATE INDEX " + PACKET_TABLE + "_idx ON " + PACKET_TABLE
				+ " (timestamp, srcaddr, srcport, dstaddr, dstport, protocol, ttl, length)" );
	}


	/**
	 * Insert packet into the PACKET_TABLE and add to hosts lists
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

		if ( insertPacketStmt == null ) {
			this.insertPacketStmt = this.conn.prepareStatement( "INSERT INTO " + PACKET_TABLE
					+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" );
		}
			
		this.insertPacketStmt.setTimestamp( 2, time );
		this.insertPacketStmt.setString( 3, src );
		this.insertPacketStmt.setInt( 4, srcport );
		this.insertPacketStmt.setString( 5, dst );
		this.insertPacketStmt.setInt( 6, dstport );
		this.insertPacketStmt.setInt( 7, protocol );
		this.insertPacketStmt.setInt( 8, ttl );
		this.insertPacketStmt.setInt( 9, len );
		this.insertPacketStmt.setObject( 10, packet );
		this.insertPacketStmt.executeUpdate();
		
		this.addHost(src);
		this.addHost(dst);
		this.addPacket(packet);
	}


	/**
	 * For saved databases, set up the hosts lists and add packets to local host
	 */
	public void setupHosts() {
		try {
			if ( this.selectDistinctHostsStmt == null ) {
				this.selectDistinctHostsStmt = this.conn.prepareStatement( 
						"SELECT DISTINCT srcaddr FROM " + PACKET_TABLE + 
						" UNION DISTINCT SELECT DISTINCT dstaddr FROM " + PACKET_TABLE );
			}
			ResultSet rs = this.selectDistinctHostsStmt.executeQuery();
			while ( rs.next() )
				this.addHost( rs.getString( 1 ) );
			
			rs = TNVDbUtil.query( this.conn, "SELECT packet FROM " + PACKET_TABLE );
			while ( rs.next() )
				this.addPacket( (RawPacket) rs.getObject( 1 ) );
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error creating packet statement: " + sqlex.getMessage() );
		}
	}

	
	/**
	 * Writes out packets to specified file and endian format
	 * @param filePath to write packets to
	 * @param endian to format
	 * @return success
	 */
	public boolean writeRawPackets( String filePath, int endian ) {
		try {
			ResultSet rs = TNVDbUtil.query( this.conn, "SELECT packet FROM " + PACKET_TABLE );
			while ( rs.next() ) {
				RawPacket p = (RawPacket) rs.getObject( 1 );
				try {
					TcpdumpWriter.appendPacket( filePath, p, endian );
				}
				catch ( IOException ioex ) {
					System.out.println( "Unable to write tcpdump data: " + ioex.getMessage() );
					return false;
				}
			}
		}
		catch ( SQLException e ) {
			System.out.println( "Unable to read packets from database: " + e.getMessage() );
			return false;
		}
		return true;
	}


	/**
	 * Gets the details of all packets for a host in a given time period
	 * @param name String of src or dst address
	 * @param start Timestamp of start time
	 * @param end Timestamp of end time
	 * @return List of packets
	 */
	public List<RawPacket> getPacketList( String name, Timestamp start, Timestamp end ) {
		List<RawPacket> l = new ArrayList<RawPacket>();
		try {
			if ( this.selectPacketListStmt == null ) {
				this.selectPacketListStmt = this.conn.prepareStatement( "SELECT packet FROM " + PACKET_TABLE
						+ " WHERE (srcaddr = ? OR dstaddr = ?) " + " AND (timestamp >= ? AND timestamp < ?)" );
			}
			this.selectPacketListStmt.setString( 1, name );
			this.selectPacketListStmt.setString( 2, name );
			this.selectPacketListStmt.setTimestamp( 3, start );
			this.selectPacketListStmt.setTimestamp( 4, end );
			ResultSet rs = this.selectPacketListStmt.executeQuery();
			while ( rs.next() )
				l.add( (RawPacket) rs.getObject( 1 ) );
			rs.close();
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error getting packet list for host (" + name + ": " + sqlex.getMessage() );
		}
		return l;
	}


	/**
	 * Gets the details of all packets for all hosts
	 * @return List of packets
	 */
	public List<RawPacket> getPacketList( ) {
		List<RawPacket> l = new ArrayList<RawPacket>();
		try {
			ResultSet rs = TNVDbUtil.query( this.conn, "SELECT packet FROM " + PACKET_TABLE );
			while ( rs.next() )
				l.add( (RawPacket) rs.getObject( 1 ) );
			rs.close();
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error getting all packets: " + sqlex.getMessage() );
		}
		return l;
	}

	
	/**
	 * Get total packet count in database
	 */
	public int getTotalPacketCount( ) {
		int count = 0;
		try {
			ResultSet rs = TNVDbUtil.query( this.conn, "SELECT COUNT(1) FROM " + PACKET_TABLE );
			if ( rs.next() ) 
				count = rs.getInt( 1 );
			rs.close();
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error getting total host count: " + sqlex.getMessage() );
		}
		return count;
	}


	/**
	 * Get first timestamp in database
	 * @return minimum time
	 */
	public Timestamp getTotalMinTime() {
		return getMinTime();
	}

	/**
	 * Get last timestamp in database
	 * @return maximum time
	 */
	public Timestamp getTotalMaxTime(){
		return getMaxTime();
	}

	/**
	 * Set first timestamp in data
	 * @param minimum time
	 */
	public void setMinTime(Timestamp t) {
		// DOES NOTHING FOR EMBEDDED DB
	}

	/**
	 * Set last timestamp in data
	 * @param maximum time
	 */
	public void setMaxTime(Timestamp t){
		// DOES NOTHING FOR EMBEDDED DB
	}

	/**
	 * Get first timestamp in database
	 * @return minimum time
	 */
	public Timestamp getMinTime( ) {
		Timestamp time = null;
		try {
			ResultSet rs = TNVDbUtil.query( this.conn, "SELECT MIN(timestamp) FROM " + PACKET_TABLE );
			if ( rs.next() ) 
				time = rs.getTimestamp( 1 );
			rs.close();
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error getting minimum timestamp: " + sqlex.getMessage() );
		}
		return time;
	}


	/**
	 * Get last timestamp in database
	 * @return maximum time
	 */
	public Timestamp getMaxTime( ) {
		Timestamp time = null;
		try {
			ResultSet rs = TNVDbUtil.query( this.conn, "SELECT MAX(timestamp) FROM " + PACKET_TABLE );
			if ( rs.next() ) 
				time = rs.getTimestamp( 1 );
			rs.close();
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error getting maximum timestamp: " + sqlex.getMessage() );
		}
		return time;
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
				this.selectHostFrequencyStmt = this.conn.prepareStatement( "SELECT COUNT(1) FROM " + PACKET_TABLE
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
				this.selectHostFrequencyStmt = this.conn.prepareStatement( "SELECT COUNT(1) FROM " + PACKET_TABLE
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
				this.selectTimeFrequencyStmt = this.conn.prepareStatement( "SELECT COUNT(1) FROM " + 
						PACKET_TABLE + " WHERE (timestamp >= ? AND timestamp < ?)" );
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
	 * Get packet count for a host for ingress or egress
	 * @param name String of src address
	 * @param direction src or dst
	 * @return number of packets
	 */
	public int getPacketCount( String host, TNVLinkNode.LinkDirection direction ) {
		int count = 0;
		try {
			PreparedStatement selectStmt;
			if ( direction.equals(TNVLinkNode.LinkDirection.INCOMING) )
				selectStmt = this.conn.prepareStatement( "SELECT COUNT(1) FROM " + 
					PACKET_TABLE + " WHERE dstaddr = ?" ); 
			else 
				selectStmt = this.conn.prepareStatement( "SELECT COUNT(1) FROM " + 
						PACKET_TABLE + " WHERE srcaddr = ?" ); 
			
			selectStmt.setString( 1, host );

			ResultSet rs = selectStmt.executeQuery();
			if ( rs.next() ) 
				count = rs.getInt( 1 );
			rs.close();
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error getting packet count for host " + host + ": " + sqlex.getMessage() );
		}
		return count;
	}


	/**
	 * Return PreparedStatement to select unique groups of links by host/protocol
	 * Must enter the start and end Timestamp values
	 * @return the links PreparedStatement
	 */
	public final PreparedStatement getSelectLinksStmt( ) {
		try {
			if ( this.selectLinksStmt == null ) {
				this.selectLinksStmt = this.conn.prepareStatement( 
						"SELECT srcaddr, dstaddr, protocol, COUNT(1) AS frequency FROM "
						+ PACKET_TABLE + " WHERE (timestamp >= ? AND timestamp < ?)"
						+ " GROUP BY srcaddr, dstaddr, protocol " );
			}
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error creating links statement: " + sqlex.getMessage() );
		}
		
		return this.selectLinksStmt;
	}

	/**
	 * Return PreparedStatement to select unique groups of packets by host/port/protocol
	 * @return the packets PreparedStatement
	 */
	public final PreparedStatement getSelectAllInPortsStmt( ) {
		try {
			if ( this.selectAllInPortsStmt == null ) {
				String homeNet = TNVUtil.getHomeNetString( TNVPreferenceData.getInstance().getHomeNet() );
				this.selectAllInPortsStmt = this.conn.prepareStatement( 
						"SELECT srcaddr, srcport, dstaddr, dstport, protocol, "
						+ " COUNT(1) AS frequency FROM " + PACKET_TABLE 
						+ " WHERE dstaddr LIKE '" + homeNet + "%'"
						+ " GROUP BY srcaddr, srcport, dstaddr, dstport, protocol " );
			}
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error creating ports statement: " + sqlex.getMessage() );
		}
		return this.selectAllInPortsStmt;
	}

	/**
	 * Return PreparedStatement to select unique groups of packets by host/port/protocol
	 * @return the packets PreparedStatement
	 */
	public final PreparedStatement getSelectAllOutPortsStmt( ) {
		try {
			if ( this.selectAllOutPortsStmt == null ) {
				String homeNet = TNVUtil.getHomeNetString( TNVPreferenceData.getInstance().getHomeNet() );
				this.selectAllOutPortsStmt = this.conn.prepareStatement( 
						"SELECT srcaddr, srcport, dstaddr, dstport, protocol, "
						+ " COUNT(1) AS frequency FROM " + PACKET_TABLE 
						+ " WHERE srcaddr LIKE '" + homeNet + "%'"
						+ " GROUP BY srcaddr, srcport, dstaddr, dstport, protocol " );
			}
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error creating ports statement: " + sqlex.getMessage() );
		}
		return this.selectAllOutPortsStmt;
	}

	/**
	 * Return PreparedStatement to select unique groups of packets by host/port/protocol
	 * Must enter the dst address String values and the start and end Timestamp values
	 * @return the packets PreparedStatement
	 */
	public final PreparedStatement getSelectInPortsStmt( ) {
		try {
			if ( this.selectInPortsStmt == null ) {
				this.selectInPortsStmt = this.conn.prepareStatement( 
						"SELECT srcaddr, srcport, dstaddr, dstport, protocol, "
						+ " COUNT(1) AS frequency FROM "
						+ PACKET_TABLE + " WHERE dstaddr = ? "
						+ " AND (timestamp >= ? AND timestamp < ?)"
						+ " GROUP BY srcaddr, srcport, dstaddr, dstport, protocol " );
			}
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error creating ports statement: " + sqlex.getMessage() );
		}
		return this.selectInPortsStmt;
	}

	
	/**
	 * Return PreparedStatement to select unique groups of packets by host/port/protocol
	 * Must enter the src address String values and the start and end Timestamp values
	 * @return the packets PreparedStatement
	 */
	public final PreparedStatement getSelectOutPortsStmt( ) {
		try {
			if ( this.selectOutPortsStmt == null ) {
				this.selectOutPortsStmt = this.conn.prepareStatement( 
						"SELECT srcaddr, srcport, dstaddr, dstport, protocol, "
						+ " COUNT(1) AS frequency FROM "
						+ PACKET_TABLE + " WHERE srcaddr = ? "
						+ " AND (timestamp >= ? AND timestamp < ?)"
						+ " GROUP BY srcaddr, srcport, dstaddr, dstport, protocol " );
			}
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error creating ports statement: " + sqlex.getMessage() );
		}
		return this.selectOutPortsStmt;
	}

	
	/**
	 * inserts a packet into local host map
	 * @param name the String name to add to lists
	 */
	private void addPacket( RawPacket rawPacket ) {
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
