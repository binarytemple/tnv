/**
 * Created on May 5, 2006
 * @author jgood
 * 
 * Mysql database class 
 */
package net.sourceforge.tnv;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import net.sourceforge.jpcap.util.TcpdumpWriter;

/**
 * TNVDbMysql
 */
public class TNVDbMysql implements TNVDb {

	private static String PACKET_TABLE = "packet_table";

	private static String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static String JDBC_CONNECT = "jdbc:mysql:"; 
	
	// Min and Max times to use for visualization
	private Timestamp minTime, maxTime;
	
	// Map of local hosts' packets - hostname: map (timestamp: list (packets))
	private Map<String, SortedMap<Timestamp, List<TNVPacket>>> localHostMap = 
		new HashMap<String, SortedMap<Timestamp, List<TNVPacket>>>();

	// Set of remote hosts
	private Set<String> remoteHostList = new HashSet<String>();
	
	private Connection conn = null;

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
	private static TNVDb instance = new TNVDbMysql();


	/**
	 * Empty, private constructor
	 */
	private TNVDbMysql() { }


	/**
	 * Singleton
	 * @return this
	 */
	public static TNVDb getInstance( ) {
		return instance;
	}


	/**
	 * Load the Database Engine JDBC driver and opens connection to a database
	 * @param path the host and db name (format: <host>/<db>)
	 * @param user the username to log in as
	 * @param passwd the password to log in with
	 */
	public void openConnection( String path, String user, String passwd ) throws SQLException {
		if ( this.conn != null )
			return;
		try {
			Class.forName( JDBC_DRIVER ).newInstance();
		}
		catch ( Exception e ) {
			System.out.println( "Error loading JDBC driver (" + JDBC_DRIVER + "): " + e.getMessage() );
		}
		//jdbc:mysql://[host:port],[host:port].../[database][?propertyName1][=propertyValue1][&propertyName2][=propertyValue2]...
		this.conn = DriverManager.getConnection( JDBC_CONNECT + "//" + path 
				+ "?user=" + user + "&password=" + passwd );

		// create tables if necessary
		ResultSet rs = this.conn.getMetaData().getTables(null, null, PACKET_TABLE, new String[]{"TABLE"});
		if ( ! rs.next() )
			this.createTables();
	}


	/**
	 * Closes connection to database safely
	 */
	public void closeConnection( ) throws SQLException {
//		if ( this.conn != null ) {
//			this.conn.close();			// if there are no other open connection
//			this.conn = null;
//		}
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

		this.localHostMap.clear();
		this.remoteHostList.clear();
		
		this.minTime = null;
		this.maxTime = null;
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
		update( this.conn, 
				"CREATE TABLE IF NOT EXISTS " + PACKET_TABLE + " ( " + 
				"id INT AUTO_INCREMENT PRIMARY KEY, " +
				"timestamp TIMESTAMP, " +
				"srcaddr VARCHAR(16), " + 	// source address
				"srcport INT UNSIGNED, " + 
				"dstaddr VARCHAR(16), " + 	// destination address
				"dstport INT UNSIGNED, " + 
				"protocol INT UNSIGNED, " + 	// jpcap protocol int
				"ttl INT UNSIGNED, " + 		// TTL value
				"length INT UNSIGNED, " + 	// packet length
				"packet BLOB )" 		// the packet itself
		);
		// Index
		update( this.conn, "CREATE INDEX " + PACKET_TABLE + "_idx ON " + PACKET_TABLE
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

		if ( insertPacketStmt == null )
			this.insertPacketStmt = this.conn.prepareStatement( 
					"INSERT INTO " + PACKET_TABLE +
					" (timestamp, srcaddr, srcport, dstaddr, dstport, protocol, ttl, length, packet)" +
					" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" );
			
		this.insertPacketStmt.setTimestamp( 1, time );
		this.insertPacketStmt.setString( 2, src );
		this.insertPacketStmt.setInt( 3, srcport );
		this.insertPacketStmt.setString( 4, dst );
		this.insertPacketStmt.setInt( 5, dstport );
		this.insertPacketStmt.setInt( 6, protocol );
		this.insertPacketStmt.setInt( 7, ttl );
		this.insertPacketStmt.setInt( 8, len );
		this.insertPacketStmt.setObject( 9, packet );
		this.insertPacketStmt.executeUpdate();
		
		if ( ! this.localHostMap.containsKey(src) && ! this.remoteHostList.contains(src) )
			this.addHost(src);
		if ( ! this.localHostMap.containsKey(dst) && ! this.remoteHostList.contains(dst) )
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
						" WHERE (timestamp >= ? AND timestamp <= ?)" +
						" UNION DISTINCT SELECT DISTINCT dstaddr FROM " + PACKET_TABLE +
						" WHERE (timestamp >= ? AND timestamp <= ?)");
			}
			this.selectDistinctHostsStmt.setTimestamp(1, this.minTime);
			this.selectDistinctHostsStmt.setTimestamp(2, this.maxTime);
			this.selectDistinctHostsStmt.setTimestamp(3, this.minTime);
			this.selectDistinctHostsStmt.setTimestamp(4, this.maxTime);
			
			ResultSet rs = this.selectDistinctHostsStmt.executeQuery();
			while ( rs.next() )
				this.addHost( rs.getString( 1 ) );
			
			PreparedStatement selectPackets = this.conn.prepareStatement(
					"SELECT packet FROM " + PACKET_TABLE + " WHERE (timestamp >= ? AND timestamp <= ?)");
			selectPackets.setTimestamp(1, this.minTime);
			selectPackets.setTimestamp(2, this.maxTime);
			
			rs = selectPackets.executeQuery();
			while ( rs.next() ) {
				try {
					this.addPacket( (RawPacket) new ObjectInputStream(rs.getBlob(1).getBinaryStream()).readObject() );
				}
				catch (Exception e) {
					System.out.println( "Error getting packet: " + e.getMessage() );
				}
			}
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
			PreparedStatement selectPackets = this.conn.prepareStatement(
					"SELECT packet FROM " + PACKET_TABLE + " WHERE (timestamp >= ? AND timestamp <= ?)");
			selectPackets.setTimestamp(1, this.minTime);
			selectPackets.setTimestamp(2, this.maxTime);
			ResultSet rs = selectPackets.executeQuery();
			while ( rs.next() ) {
				try {
					RawPacket p = (RawPacket) new ObjectInputStream(rs.getBlob(1).getBinaryStream()).readObject();
					TcpdumpWriter.appendPacket( filePath, p, endian );
				}
				catch ( IOException ioex ) {
					System.out.println( "Unable to write tcpdump data: " + ioex.getMessage() );
					return false;
				}
				catch ( Exception ex ) {
					System.out.println( "Unable to write tcpdump data: " + ex.getMessage() );
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
				l.add( (RawPacket) new ObjectInputStream(rs.getBlob(1).getBinaryStream()).readObject() );
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error getting packet list for host (" + name + ": " + sqlex.getMessage() );
		}
		catch ( IOException ioex ) {
			System.out.println( "IO Error getting raw packet for host (" + name + ": " + ioex.getMessage() );
		}
		catch ( Exception ex ) {
			System.out.println( "Error getting packets for host (" + name + ": " + ex.getMessage() );
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
			PreparedStatement selectPackets = this.conn.prepareStatement(
					"SELECT packet FROM " + PACKET_TABLE + " WHERE (timestamp >= ? AND timestamp <= ?)");
			selectPackets.setTimestamp(1, this.minTime);
			selectPackets.setTimestamp(2, this.maxTime);
			ResultSet rs = selectPackets.executeQuery();
			while ( rs.next() )
				l.add( (RawPacket) new ObjectInputStream(rs.getBlob(1).getBinaryStream()).readObject() );
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error getting all packets: " + sqlex.getMessage() );
		}
		catch ( IOException ioex ) {
			System.out.println( "IO Error getting all raw packets: " + ioex.getMessage() );
		}
		catch ( Exception ex ) {
			System.out.println( "Error getting all packets: " + ex.getMessage() );
		}
		return l;
	}

	
	/**
	 * Get total packet count in database
	 */
	public int getTotalPacketCount( ) {
		int count = 0;
		try {
			PreparedStatement selectPackets = this.conn.prepareStatement(
					"SELECT COUNT(1) FROM " + PACKET_TABLE + " WHERE (timestamp >= ? AND timestamp <= ?)");
			selectPackets.setTimestamp(1, this.minTime);
			selectPackets.setTimestamp(2, this.maxTime);
			ResultSet rs = selectPackets.executeQuery();
			if ( rs.next() ) 
				count = rs.getInt( 1 );
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
		Timestamp time = null;
		try {
			ResultSet rs = query( this.conn, "SELECT MIN(timestamp) FROM " + PACKET_TABLE );
			if ( rs.next() ) 
				time = rs.getTimestamp( 1 );
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
	public Timestamp getTotalMaxTime(){
		Timestamp time = null;
		try {
			ResultSet rs = query( this.conn, "SELECT MAX(timestamp) FROM " + PACKET_TABLE );
			if ( rs.next() ) 
				time = rs.getTimestamp( 1 );
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error getting maximum timestamp: " + sqlex.getMessage() );
		}
		return time;
	}

	/**
	 * Set first timestamp in data
	 * @param minimum time
	 */
	public void setMinTime(Timestamp t) {
		minTime = t;
	}

	/**
	 * Set last timestamp in data
	 * @param maximum time
	 */
	public void setMaxTime(Timestamp t){
		maxTime = t;
	}

	/**
	 * Get first timestamp in data
	 * @return minimum time
	 */
	public Timestamp getMinTime( ) {
		if ( minTime == null )
			return getTotalMinTime();
		return minTime;
	}


	/**
	 * Get last timestamp in data
	 * @return maximum time
	 */
	public Timestamp getMaxTime( ) {
		if ( maxTime == null )
			return getTotalMinTime();
		return maxTime;
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
						+ " WHERE (srcaddr = ? OR dstaddr = ?) " + " AND (timestamp >= ? AND timestamp <= ?)" );
			}
			
			this.selectHostFrequencyStmt.setString( 1, host );
			this.selectHostFrequencyStmt.setString( 2, host );
			this.selectHostFrequencyStmt.setTimestamp( 3, start );
			this.selectHostFrequencyStmt.setTimestamp( 4, end );
			ResultSet rs = this.selectHostFrequencyStmt.executeQuery();
			if ( rs.next() ) 
				count = rs.getInt( 1 );
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
				this.selectTimeFrequencyStmt = this.conn.prepareStatement( "SELECT COUNT(1) FROM " + PACKET_TABLE
						+ " WHERE (timestamp >= ? AND timestamp < ?)" );
			}
			
			this.selectTimeFrequencyStmt.setTimestamp( 1, start );
			this.selectTimeFrequencyStmt.setTimestamp( 2, end );
			ResultSet rs = this.selectTimeFrequencyStmt.executeQuery();
			if ( rs.next() ) 
				count = rs.getInt( 1 );
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
					PACKET_TABLE + " WHERE dstaddr = ? AND (timestamp >= ? AND timestamp <= ?)" ); 
			else 
				selectStmt = this.conn.prepareStatement( "SELECT COUNT(1) FROM " + 
					PACKET_TABLE + " WHERE srcaddr = ? AND (timestamp >= ? AND timestamp <= ?)" ); 
			
			selectStmt.setString( 1, host );
			selectStmt.setTimestamp(2, this.minTime);
			selectStmt.setTimestamp(3, this.maxTime);

			ResultSet rs = selectStmt.executeQuery();
			if ( rs.next() ) 
				count = rs.getInt( 1 );
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error getting packet count for host " + host + ": " + sqlex.getMessage() );
		}
		return count;
	}

	
	/**
	 * @return the localHostMap
	 */
	public final Map<String, SortedMap<Timestamp, List<TNVPacket>>> getLocalHostMap( ) {
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
	 * @return Returns all remote hosts.
	 */
	public final Set<String> getRemoteHostList( ) {
		return this.remoteHostList;
	}

	
	/**
	 * @return the getLinksStatement
	 */
	public final PreparedStatement getSelectLinksStmt( ) {
		try {
			if ( selectLinksStmt == null ) {
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
						+ " AND (timestamp >= ? AND timestamp <= ?)"
						+ " GROUP BY srcaddr, srcport, dstaddr, dstport, protocol " );
			}
			this.selectAllInPortsStmt.setTimestamp(1, this.minTime);
			this.selectAllInPortsStmt.setTimestamp(2, this.maxTime);
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
						+ " AND (timestamp >= ? AND timestamp <= ?)"
						+ " GROUP BY srcaddr, srcport, dstaddr, dstport, protocol " );
			}
			this.selectAllOutPortsStmt.setTimestamp(1, this.minTime);
			this.selectAllOutPortsStmt.setTimestamp(2, this.maxTime);
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
	 * Executes a query and returns a resultset
	 * @param conn
	 * @param expression
	 * @return resultset
	 * @throws SQLException
	 */
	private ResultSet query( Connection conn, String expression ) throws SQLException {
		Statement st = null;
		st = conn.createStatement();
		return st.executeQuery( expression );
	}


	/**
	 * Updates this database connection
	 * @param conn
	 * @param expression
	 * @throws SQLException
	 */
	private void update( Connection conn, String expression ) throws SQLException {
		Statement st = null;
		st = conn.createStatement(); // statements
		int i = st.executeUpdate( expression ); // run the query
		if ( i == -1 ) {
			System.out.println( "db error : " + expression );
		}
		st.close();
	}
	
	/**
	 * inserts a host into the local or remote set
	 * @param name the String name to add to lists
	 */
	private void addHost( String name ) {
		if ( TNVUtil.isOnHomeNet( name, TNVPreferenceData.getInstance().getHomeNet() ) )
			this.localHostMap.put( name, new TreeMap<Timestamp, List<TNVPacket>>() );
		else
			this.remoteHostList.add( name );
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
				if ( this.localHostMap.get( srcAddr ).containsKey( time ) )
					l = this.localHostMap.get( srcAddr ).get( time );
				else
					l = new ArrayList<TNVPacket>();
				l.add( tnvpacket );
				this.localHostMap.get( srcAddr ).put( time, l );
			}
			if ( TNVUtil.isOnHomeNet( dstAddr, TNVPreferenceData.getInstance().getHomeNet() ) ) {
				if ( this.localHostMap.get( dstAddr ).containsKey( time ) )
					l = this.localHostMap.get( dstAddr ).get( time );
				else
					l = new ArrayList<TNVPacket>();
				l.add( tnvpacket );			
				this.localHostMap.get( dstAddr ).put( time, l );
			}

		}
		catch ( Exception e ) {
			System.out.println( "Error adding packet to TNV data: " + e.getMessage() );
		}
	}

}
