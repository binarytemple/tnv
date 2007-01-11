/**
 * Created on Nov 1, 2005 
 * @author jgood
 * 
 * Embedded database class for internal data
 */
package net.sourceforge.tnv.db;

import java.io.IOException;
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
 * TNVDbEmbedded
 */
public class TNVDbEmbedded extends TNVDbAbstract implements TNVDbInterface {

	private static String JDBC_DRIVER = "org.hsqldb.jdbcDriver";
	private static String JDBC_CONNECT = "jdbc:hsqldb:file:"; 
	
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
			System.err.println( "Error loading JDBC driver (" + JDBC_DRIVER + "): " + e.getMessage() );
			System.err.println( "Exiting...");
			System.exit(1);
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
		
		this.selectPacketListStmt = null;
		this.selectDistinctHostsStmt = null;
		this.selectAllInPortsStmt = null;
		this.selectAllOutPortsStmt = null;
		this.selectInPortsStmt = null;
		this.selectOutPortsStmt = null;
		this.selectLinksStmt = null;

		this.clear();
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
		// Do nothing
	}

	
	/**
	 * Create full table including packet object
	 */
	private void createTables( ) throws SQLException {
		// Full table of packets ( Index is created automatically for primary keys)
		TNVDbUtil.update( this.conn, 
				"CREATE CACHED TABLE " + PACKET_DATA_TABLE + " ( " + 
				"packet_id INTEGER PRIMARY KEY, " +
				"packet OBJECT )" 		// the packet itself
		);

		// Summary table of packets (no packet data)
		TNVDbUtil.update( this.conn, 
				"CREATE MEMORY TABLE " + PACKET_SUMMARY_TABLE + " ( " + 
				"id INTEGER IDENTITY PRIMARY KEY, " +
				"timestamp TIMESTAMP, " +
				"srcaddr VARCHAR(16), " + 	// source address
				"srcport INTEGER, " + 
				"dstaddr VARCHAR(16), " + 	// destination address
				"dstport INTEGER, " + 
				"protocol INTEGER, " + 	// jpcap protocol int
				"ttl INTEGER, " + 		// TTL value
				"length INTEGER, " + 	// packet length
				"packet_id INTEGER )"
		);
		// Index
		TNVDbUtil.update( this.conn, "CREATE INDEX " + PACKET_SUMMARY_TABLE + "_idx ON " + PACKET_SUMMARY_TABLE
				+ " (timestamp, srcaddr, srcport, dstaddr, dstport, protocol, ttl, length)" );
		// Foreign key
		TNVDbUtil.update( this.conn, "ALTER TABLE " + PACKET_SUMMARY_TABLE  
				+ " ADD FOREIGN KEY (packet_id) REFERENCES " + PACKET_DATA_TABLE + " (packet_id)" );

		// Table of hosts
		TNVDbUtil.update( this.conn, 
				"CREATE TABLE " + HOST_TABLE + " ( " +
				"host VARCHAR(16) PRIMARY KEY, " + 	// host address
				"frequency INTEGER )" // total number of packets
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
				this.selectDistinctHostsStmt = this.getConnection().prepareStatement( 
						"SELECT DISTINCT srcaddr FROM " + PACKET_SUMMARY_TABLE + 
						" UNION DISTINCT SELECT DISTINCT dstaddr FROM " + PACKET_SUMMARY_TABLE );
			}
			ResultSet rs = this.selectDistinctHostsStmt.executeQuery();
			while ( rs.next() )
				this.addHost( rs.getString( 1 ) );
			
			rs = TNVDbUtil.query( this.getConnection(), "SELECT packet FROM " + PACKET_DATA_TABLE );
			while ( rs.next() )
				this.addPacket( (RawPacket) rs.getObject( 1 ) );
		}
		catch ( SQLException sqlex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL error creating packet statement", sqlex);
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
			ResultSet rs = TNVDbUtil.query( this.getConnection(), "SELECT packet FROM " + PACKET_DATA_TABLE );
			while ( rs.next() ) {
				RawPacket p = (RawPacket) rs.getObject( 1 );
				try {
					TcpdumpWriter.appendPacket( filePath, p, endian );
				}
				catch ( IOException ioex ) {
					TNVErrorDialog.createTNVErrorDialog(this.getClass(), "IO Error: unable to write tcpdump data", ioex);
					return false;
				}
			}
		}
		catch ( SQLException sqlex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL Error: unable to write tcpdump data", sqlex);
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
			ResultSet rs = TNVDbUtil.query( this.getConnection(), 
					"SELECT packet FROM " + PACKET_DATA_TABLE + "," + PACKET_SUMMARY_TABLE
					+ " WHERE " + PACKET_DATA_TABLE + ".packet_id = " + PACKET_SUMMARY_TABLE + ".packet_id ");
			while ( rs.next() )
				l.add( (RawPacket) rs.getObject( 1 ) );
			rs.close();
		}
		catch ( SQLException sqlex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL Error getting all packets", sqlex);
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
						+ ")");
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
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL Error getting packet list for host: " + name, sqlex);
		}
		return l;
	}

	
	/**
	 * Get total packet count in database
	 */
	public int getTotalPacketCount( ) {
		int count = 0;
		try {
			ResultSet rs = TNVDbUtil.query( this.getConnection(), "SELECT COUNT(1) FROM " + PACKET_SUMMARY_TABLE );
			if ( rs.next() ) 
				count = rs.getInt( 1 );
			rs.close();
		}
		catch ( SQLException sqlex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL Error getting total host count", sqlex);
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
			ResultSet rs = TNVDbUtil.query( this.conn, "SELECT MIN(timestamp) FROM " + PACKET_SUMMARY_TABLE );
			if ( rs.next() ) 
				time = rs.getTimestamp( 1 );
			rs.close();
		}
		catch ( SQLException sqlex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL Error getting minimum timestamp", sqlex);
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
			ResultSet rs = TNVDbUtil.query( this.conn, "SELECT MAX(timestamp) FROM " + PACKET_SUMMARY_TABLE );
			if ( rs.next() ) 
				time = rs.getTimestamp( 1 );
			rs.close();
		}
		catch ( SQLException sqlex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL Error getting maximum timestamp", sqlex);
		}
		return time;
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
				selectStmt = this.getConnection().prepareStatement( "SELECT COUNT(1) FROM " + 
					PACKET_SUMMARY_TABLE + " WHERE dstaddr = ?" ); 
			else 
				selectStmt = this.getConnection().prepareStatement( "SELECT COUNT(1) FROM " + 
						PACKET_SUMMARY_TABLE + " WHERE srcaddr = ?" ); 
			
			selectStmt.setString( 1, host );

			ResultSet rs = selectStmt.executeQuery();
			if ( rs.next() ) 
				count = rs.getInt( 1 );
			rs.close();
		}
		catch ( SQLException sqlex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL Error getting packet count for host " + host, sqlex);
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
				this.selectLinksStmt = this.getConnection().prepareStatement( 
						"SELECT srcaddr, dstaddr, protocol, COUNT(1) AS frequency FROM "
						+ PACKET_SUMMARY_TABLE + " WHERE (timestamp >= ? AND timestamp < ?)"
						+ " GROUP BY srcaddr, dstaddr, protocol " );
			}
		}
		catch ( SQLException sqlex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL Error creating links select statement", sqlex);
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
				this.selectAllInPortsStmt = this.getConnection().prepareStatement( 
						"SELECT srcaddr, srcport, dstaddr, dstport, protocol, "
						+ " COUNT(1) AS frequency FROM " + PACKET_SUMMARY_TABLE 
						+ " WHERE dstaddr LIKE '" + homeNet + "%'"
						+ " GROUP BY srcaddr, srcport, dstaddr, dstport, protocol " );
			}
		}
		catch ( SQLException sqlex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL Error creating all incoming ports select statement", sqlex);
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
				this.selectAllOutPortsStmt = this.getConnection().prepareStatement( 
						"SELECT srcaddr, srcport, dstaddr, dstport, protocol, "
						+ " COUNT(1) AS frequency FROM " + PACKET_SUMMARY_TABLE 
						+ " WHERE srcaddr LIKE '" + homeNet + "%'"
						+ " GROUP BY srcaddr, srcport, dstaddr, dstport, protocol " );
			}
		}
		catch ( SQLException sqlex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL Error creating all outgoing ports select statement", sqlex);

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
				this.selectInPortsStmt = this.getConnection().prepareStatement( 
						"SELECT srcaddr, srcport, dstaddr, dstport, protocol, "
						+ " COUNT(1) AS frequency FROM "
						+ PACKET_SUMMARY_TABLE + " WHERE dstaddr = ? "
						+ " AND (timestamp >= ? AND timestamp < ?)"
						+ " GROUP BY srcaddr, srcport, dstaddr, dstport, protocol " );
			}
		}
		catch ( SQLException sqlex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL Error creating incoming ports select statement", sqlex);
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
				this.selectOutPortsStmt = this.getConnection().prepareStatement( 
						"SELECT srcaddr, srcport, dstaddr, dstport, protocol, "
						+ " COUNT(1) AS frequency FROM "
						+ PACKET_SUMMARY_TABLE + " WHERE srcaddr = ? "
						+ " AND (timestamp >= ? AND timestamp < ?)"
						+ " GROUP BY srcaddr, srcport, dstaddr, dstport, protocol " );
			}
		}
		catch ( SQLException sqlex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL Error creating outgoing ports select statement", sqlex);
		}
		return this.selectOutPortsStmt;
	}


}
