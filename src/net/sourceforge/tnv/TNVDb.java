/**
 * Created on Nov 1, 2005 
 * @author jgood
 * 
 * Database interface
 */
package net.sourceforge.tnv;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;


import net.sourceforge.jpcap.net.RawPacket;

/**
 * TNVDb
 */
public interface TNVDb {

	/**
	 * Load the Database Engine JDBC driver and opens connection to a database
	 * @param directoryPath the directory to open
	 * @param user the username to log in as
	 * @param passwd the password to log in with
	 * @throws SQLException
	 */
	public abstract void openConnection(String path, String user, String passwd)
			throws SQLException;

	/**
	 * Closes connection to database safely
	 * @throws SQLException
	 */
	public abstract void closeConnection() throws SQLException;

	/**
	 * Checks to see if there is an open connection
	 * @return if there is a connection
	 */
	public abstract boolean isConnection();

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
	 * @throws SQLException
	 */
	public abstract void insertPacket(Timestamp time, String src, int srcport,
			String dst, int dstport, int protocol, int ttl, int len,
			RawPacket packet) throws SQLException;

	/**
	 * For saved databases, set up the hosts lists and add packets to local host
	 */
	public abstract void setupHosts();

	/**
	 * Writes out packets to specified file and endian format
	 * @param filePath to write packets to
	 * @param endian to format
	 * @return success
	 */
	public abstract boolean writeRawPackets(String filePath, int endian);

	/**
	 * Gets the details of all packets for a host in a given time period
	 * @param name String of src or dst address
	 * @param start Timestamp of start time
	 * @param end Timestamp of end time
	 * @return List of packets
	 */
	public abstract List<RawPacket> getPacketList(String name, Timestamp start,
			Timestamp end);

	/**
	 * Gets the details of all packets for all hosts
	 * @return List of packets
	 */
	public abstract List<RawPacket> getPacketList();

	/**
	 * Get total packet count in database
	 */
	public abstract int getTotalPacketCount();

	/**
	 * Get first timestamp in database
	 * @return minimum time
	 */
	public abstract Timestamp getTotalMinTime();

	/**
	 * Get last timestamp in database
	 * @return maximum time
	 */
	public abstract Timestamp getTotalMaxTime();

	/**
	 * Set first timestamp in this data
	 * @param minimum time
	 */
	public abstract void setMinTime(Timestamp t);

	/**
	 * Set last timestamp in this data
	 * @param maximum time
	 */
	public abstract void setMaxTime(Timestamp t);

	/**
	 * Get first timestamp in this data
	 * @return minimum time
	 */
	public abstract Timestamp getMinTime();

	/**
	 * Get last timestamp in this data
	 * @return maximum time
	 */
	public abstract Timestamp getMaxTime();

	/**
	 * Get packet count for a host in a given time period
	 * @param name String of src or dst address
	 * @param start Timestamp of start time
	 * @param end Timestamp of end time
	 * @return number of packets
	 */
	public abstract int getPacketCount(String host, Timestamp start,
			Timestamp end);

	/**
	 * Get packet count for hosts in a given time period
	 * @param name String of src address
	 * @param name String of dst address
	 * @param start Timestamp of start time
	 * @param end Timestamp of end time
	 * @return number of packets
	 */
	public abstract int getPacketCount(String src, String dst, Timestamp start,
			Timestamp end);

	/**
	 * Get packet count for a given time period
	 * @param start Timestamp of start time
	 * @param end Timestamp of end time
	 * @return number of packets
	 */
	public abstract int getPacketCount(Timestamp start, Timestamp end);

	/**
	 * Get packet count for a host for ingress or egress
	 * @param name String of src address
	 * @param direction src or dst
	 * @return number of packets
	 */
	public abstract int getPacketCount( String host, TNVLinkNode.LinkDirection direction );

	/**
	 * @return the localHostMap
	 */
	public abstract Map<String, SortedMap<Timestamp, List<TNVPacket>>> getLocalHostMap();

	/**
	 * @return the localHostMap
	 * @param the host to get the map for
	 */
	public abstract SortedMap<Timestamp, List<TNVPacket>> getLocalHostMap(String host);

	/**
	 * @return Returns all remote hosts.
	 */
	public abstract Set<String> getRemoteHostList();

	/**
	 * @return the PreparedStatement
	 */
	public abstract PreparedStatement getSelectLinksStmt();

	/**
	 * @return PreparedStatement
	 */
	public abstract PreparedStatement getSelectAllInPortsStmt();

	/**
	 * @return PreparedStatement
	 */
	public abstract PreparedStatement getSelectAllOutPortsStmt();

	/**
	 * @return PreparedStatement
	 */
	public abstract PreparedStatement getSelectInPortsStmt();

	/**
	 * @return PreparedStatement
	 */
	public abstract PreparedStatement getSelectOutPortsStmt();

}