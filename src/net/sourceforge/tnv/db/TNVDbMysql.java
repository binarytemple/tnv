/**
 * Created on May 5, 2006
 * @author jgood
 * 
 * Mysql database class 
 */
package net.sourceforge.tnv.db;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.jpcap.net.RawPacket;
import net.sourceforge.jpcap.util.TcpdumpWriter;
import net.sourceforge.tnv.dialogs.TNVErrorDialog;
import net.sourceforge.tnv.ui.TNVLinkNode;
import net.sourceforge.tnv.ui.TNVPreferenceData;
import net.sourceforge.tnv.util.TNVUtil;

/**
 * TNVDbMysql
 */
public class TNVDbMysql extends TNVDbAbstract implements TNVDbInterface {

	private static String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static String JDBC_CONNECT = "jdbc:mysql:"; 
	
	// Min and Max times to use for visualization
	private Timestamp minTime, maxTime;
		
	// JDBC prepared statements
	private PreparedStatement 
			selectPacketListStmt,				// get packet details (for detail window)
			selectDistinctHostsStmt, 			// get distinct hosts
			selectLinksStmt, 					// get links (for visualization links)
			selectAllInPortsStmt,				// get all incoming packets (for port details)
			selectAllOutPortsStmt,				// get all outgoing packets (for port details)
			selectInPortsStmt,					// get incoming packets for a host (for port details)
			selectOutPortsStmt;					// get outgoing packets for a host (for port details)

	// JDBC Connection
	private Connection conn = null;

	// Singleton instance
	private static TNVDbInterface instance = new TNVDbMysql();


	/**
	 * Empty, private constructor
	 */
	private TNVDbMysql() { }


	/**
	 * Singleton
	 * @return this
	 */
	public static TNVDbInterface getInstance( ) {
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
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), 
					"Error loading JDBC driver (" + JDBC_DRIVER + "): " + e.getMessage() );
		}
		//jdbc:mysql://[host:port],[host:port].../[database][?propertyName1][=propertyValue1][&propertyName2][=propertyValue2]...
		this.conn = DriverManager.getConnection( JDBC_CONNECT + "//" + path 
				+ "?user=" + user + "&password=" + passwd );

		// create tables if necessary
		ResultSet rs = this.conn.getMetaData().getTables(null, null, PACKET_SUMMARY_TABLE, new String[]{"TABLE"});
		if ( ! rs.next() )
			this.createTables();
	}


	/**
	 * Closes connection to database safely
	 */
	public void closeConnection( ) throws SQLException {
		this.selectPacketListStmt = null;
		this.selectDistinctHostsStmt = null;
		this.selectAllInPortsStmt = null;
		this.selectAllOutPortsStmt = null;
		this.selectInPortsStmt = null;
		this.selectOutPortsStmt = null;
		this.selectLinksStmt = null;
		
		this.clear();
		
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
	 *
	 * @return the Connection
	 */
	public Connection getConnection() {
		return this.conn;
	}


	/**
	 * Remove all data from database
	 */
	public final void removeData() throws SQLException {
		this.clear();
		this.minTime = null;
		this.maxTime = null;
		TNVDbUtil.update( this.conn, "DELETE FROM " + PACKET_DATA_TABLE);
		TNVDbUtil.update( this.conn, "DELETE FROM " + PACKET_SUMMARY_TABLE);
		TNVDbUtil.update( this.conn, "DELETE FROM " + HOST_TABLE);
	}
	
	
	/**
	 * Create full table including packet object
	 */
	private void createTables( ) throws SQLException {
		// Full table of packets
		TNVDbUtil.update( this.conn, 
				"CREATE TABLE IF NOT EXISTS " + PACKET_DATA_TABLE + " ( " + 
				"packet_id INT UNSIGNED PRIMARY KEY, " +
				"packet BLOB )" 		// the packet itself
		);
		// Index
		TNVDbUtil.update( this.conn, "CREATE INDEX " + PACKET_DATA_TABLE + "_idx ON " + PACKET_DATA_TABLE
				+ " (packet_id)" );

		// Summary table of packets (no data)
		TNVDbUtil.update( this.conn, 
				"CREATE TABLE IF NOT EXISTS " + PACKET_SUMMARY_TABLE + " ( " + 
				"id INT AUTO_INCREMENT PRIMARY KEY, " +
				"timestamp TIMESTAMP, " +
				"srcaddr VARCHAR(16), " + 	// source address
				"srcport INT UNSIGNED, " + 
				"dstaddr VARCHAR(16), " + 	// destination address
				"dstport INT UNSIGNED, " + 
				"protocol INT UNSIGNED, " + 	// jpcap protocol int
				"ttl INT UNSIGNED, " + 		// TTL value
				"length INT UNSIGNED, " + 	// packet length
				"packet_id INT UNSIGNED )"
		);
		// Index
		TNVDbUtil.update( this.conn, "CREATE INDEX " + PACKET_SUMMARY_TABLE + "_idx ON " + PACKET_SUMMARY_TABLE
				+ " (timestamp, srcaddr, srcport, dstaddr, dstport, protocol, ttl, length)" );
		// Foreign key
		TNVDbUtil.update( this.conn, "ALTER TABLE " + PACKET_SUMMARY_TABLE  
				+ " ADD FOREIGN KEY (packet_id) REFERENCES " + PACKET_DATA_TABLE + " (packet_id)" );

		// Table of hosts
		TNVDbUtil.update( this.conn, 
				"CREATE TABLE IF NOT EXISTS " + HOST_TABLE + " ( " + 
				"host VARCHAR(16) PRIMARY KEY, " + 	// host address
				"frequency INT UNSIGNED )" // total number of packets
		);
		// Index
		TNVDbUtil.update( this.conn, "CREATE INDEX " + HOST_TABLE + "_idx ON " + HOST_TABLE
				+ " (host, frequency)" );
	}


	/**
	 * For saved databases, set up the hosts lists and add packets to local host
	 */
	public void setupHosts() {
		try {
			if ( this.selectDistinctHostsStmt == null ) {
				this.selectDistinctHostsStmt = this.conn.prepareStatement( 
						"SELECT DISTINCT srcaddr FROM " + PACKET_SUMMARY_TABLE + 
						" WHERE (timestamp >= ? AND timestamp <= ?)" +
						" UNION DISTINCT SELECT DISTINCT dstaddr FROM " + PACKET_SUMMARY_TABLE +
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
					"SELECT packet FROM " + PACKET_DATA_TABLE + "," + PACKET_SUMMARY_TABLE
					+ " WHERE " 
					+ PACKET_DATA_TABLE + ".packet_id = " + PACKET_SUMMARY_TABLE + ".packet_id "
					+ " AND (timestamp >= ? AND timestamp <= ?)");
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
					"SELECT packet FROM " + PACKET_DATA_TABLE + "," + PACKET_SUMMARY_TABLE
					+ " WHERE " 
					+ PACKET_DATA_TABLE + ".packet_id = " + PACKET_SUMMARY_TABLE + ".packet_id "
					+ " AND (timestamp >= ? AND timestamp <= ?)"
					);
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
	 * Gets the details of all packets for all hosts
	 * @return List of packets
	 */
	public List<RawPacket> getPacketList( ) {
		List<RawPacket> l = new ArrayList<RawPacket>();
		try {
			PreparedStatement selectPackets = this.conn.prepareStatement(
					"SELECT packet FROM " + PACKET_DATA_TABLE + "," + PACKET_SUMMARY_TABLE
					+ " WHERE " + PACKET_DATA_TABLE + ".packet_id = " + PACKET_SUMMARY_TABLE + ".packet_id "
					+ " AND (timestamp >= ? AND timestamp <= ?)");
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
				this.selectPacketListStmt = this.getConnection().prepareStatement( 
						"SELECT packet FROM " + PACKET_DATA_TABLE + "," + PACKET_SUMMARY_TABLE
						+ " WHERE " 
						+ PACKET_DATA_TABLE + ".packet_id = " + PACKET_SUMMARY_TABLE + ".packet_id "
						+ "AND (" 
							+ "(" + PACKET_SUMMARY_TABLE + ".srcaddr = ? OR " + PACKET_SUMMARY_TABLE + ".dstaddr = ?) " 
							+ " AND (" + PACKET_SUMMARY_TABLE + ".timestamp >= ? "
							+ "AND " + PACKET_SUMMARY_TABLE + ".timestamp < ?)"
						+ ") ORDER BY " + PACKET_DATA_TABLE + ".packet_id");
			}
			this.selectPacketListStmt.setString( 1, name );
			this.selectPacketListStmt.setString( 2, name );
			this.selectPacketListStmt.setTimestamp( 3, start );
			this.selectPacketListStmt.setTimestamp( 4, end );
			ResultSet rs = this.selectPacketListStmt.executeQuery();
			while ( rs.next() )
				l.add( (RawPacket) new ObjectInputStream(rs.getBlob(1).getBinaryStream()).readObject() );
			rs.close();
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error getting packet list for host (" + name + ": " + sqlex.getMessage() );
		}
		catch ( IOException ioex ) {
			System.out.println( "IO Error getting packet list for host (" + name + ": " + ioex.getMessage() );
		}
		catch ( Exception ex ) {
			System.out.println( "Error getting packet list for host (" + name + ": " + ex.getMessage() );
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
					"SELECT COUNT(1) FROM " + PACKET_SUMMARY_TABLE + " WHERE (timestamp >= ? AND timestamp <= ?)");
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
			ResultSet rs = TNVDbUtil.query( this.conn, "SELECT MIN(timestamp) FROM " + PACKET_SUMMARY_TABLE );
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
			ResultSet rs = TNVDbUtil.query( this.conn, "SELECT MAX(timestamp) FROM " + PACKET_SUMMARY_TABLE );
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
						PACKET_SUMMARY_TABLE + " WHERE dstaddr = ? AND (timestamp >= ? AND timestamp <= ?)" ); 
			else 
				selectStmt = this.conn.prepareStatement( "SELECT COUNT(1) FROM " + 
						PACKET_SUMMARY_TABLE + " WHERE srcaddr = ? AND (timestamp >= ? AND timestamp <= ?)" ); 
			
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
	 * @return the getLinksStatement
	 */
	public final PreparedStatement getSelectLinksStmt( ) {
		try {
			if ( selectLinksStmt == null ) {
				this.selectLinksStmt = this.conn.prepareStatement( 
						"SELECT srcaddr, dstaddr, protocol, COUNT(1) AS frequency FROM "
						+ PACKET_SUMMARY_TABLE + " WHERE (timestamp >= ? AND timestamp < ?)"
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
						+ " COUNT(1) AS frequency FROM " + PACKET_SUMMARY_TABLE 
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
						+ " COUNT(1) AS frequency FROM " + PACKET_SUMMARY_TABLE 
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
						+ PACKET_SUMMARY_TABLE + " WHERE dstaddr = ? "
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
						+ PACKET_SUMMARY_TABLE + " WHERE srcaddr = ? "
						+ " AND (timestamp >= ? AND timestamp < ?)"
						+ " GROUP BY srcaddr, srcport, dstaddr, dstport, protocol " );
			}
		}
		catch ( SQLException sqlex ) {
			System.out.println( "SQL Error creating ports statement: " + sqlex.getMessage() );
		}
		return this.selectOutPortsStmt;
	}

	
}
