/**
 * Created on Apr 13, 2004 
 * @author jgood
 * 
 * Piccolo PNode for Packet representations
 */
package net.sourceforge.tnv;

import java.sql.Timestamp;


/**
 * TNVPacketNode
 */
public class TNVPacket {
	
	protected static final int PACKET_WIDTH = 6;
			
	private Timestamp timestamp;	
	private String srcAddr, dstAddr;
	private int protocol, ttl, length;
	private int srcPort = -1, dstPort = -1;			// TCP or UDP only	
	private boolean syn = false, ack = false, fin = false, 
		urg = false, psh = false, rst = false; 		// TCP Packet flags
	

	/**
	 * @param t timestamp
	 * @param s source host
	 * @param sp source port
	 * @param d destination host
	 * @param dp destination port
	 * @param p jpcap protocol
	 * @param tt time to live
	 * @param l packet length
	 */
	public TNVPacket(Timestamp t, String s, int sp, String d, int dp, int p, int tt, int l) {
		this.timestamp = t;
		this.srcAddr = s;
		this.srcPort = sp;
		this.dstAddr = d;
		this.dstPort = dp;
		this.protocol = p;
		this.ttl = tt;
		this.length = l;
		
	}
	

	/**
	 * TCP Packet constructor
	 * @param t timestamp
	 * @param s source host
	 * @param sp source port
	 * @param d destination host
	 * @param dp destination port
	 * @param p jpcap protocol
	 * @param tt time to live
	 * @param l packet length
	 * @param sF SYN
	 * @param aF ACK
	 * @param fF FIN
	 * @param uF URG
	 * @param pF PSH
	 * @param rF RST
	 */
	public TNVPacket(Timestamp t, String s, int sp, String d, int dp, int p, int tt, int l, 
			boolean sF, boolean aF, boolean fF, boolean uF, boolean pF, boolean rF) {
		this(t, s,sp,d,dp,p,tt,l);
		
		this.syn = sF;
		this.ack = aF;
		this.fin = fF;
		this.urg = uF;
		this.psh = pF;
		this.rst = rF;
	}

		
	
	/**
	 * @return the timestamp
	 */
	protected final Timestamp getTimestamp( ) {
		return this.timestamp;
	}


	/**
	 * @return Returns the src host name.
	 */
	public final String getSrcAddr( ) {
		return this.srcAddr;
	}


	/**
	 * @return Returns the dst host name.
	 */
	public final String getDstAddr( ) {
		return this.dstAddr;
	}


	/**
	 * @return Returns the protocol (compare with
	 *         net.sourceforge.jpcap.net.IPProtocols).
	 */
	public final int getProtocol( ) {
		return this.protocol;
	}


	/**
	 * @return Returns the packet length.
	 */
	public final int getLength( ) {
		return this.length;
	}


	/**
	 * @return Returns the TTL.
	 */
	public final int getTtl( ) {
		return this.ttl;
	}


	/**
	 * @return Returns the dstPort.
	 */
	protected final int getDstPort( ) {
		return this.dstPort;
	}


	/**
	 * @return Returns the srcPort.
	 */
	protected final int getSrcPort( ) {
		return this.srcPort;
	}


	/**
	 * @return Returns the ack.
	 */
	protected final boolean isAck( ) {
		return this.ack;
	}


	/**
	 * @return Returns the fin.
	 */
	protected final boolean isFin( ) {
		return this.fin;
	}


	/**
	 * @return Returns the psh.
	 */
	protected final boolean isPsh( ) {
		return this.psh;
	}


	/**
	 * @return Returns the rst.
	 */
	protected final boolean isRst( ) {
		return this.rst;
	}


	/**
	 * @return Returns the syn.
	 */
	protected final boolean isSyn( ) {
		return this.syn;
	}


	/**
	 * @return Returns the urg.
	 */
	protected final boolean isUrg( ) {
		return this.urg;
	}


	// overrides equals
	@Override
	public boolean equals( Object other ) {
		if ( this.getClass() == other.getClass() ) {
			TNVPacket otherPacket = (TNVPacket) other;
			if ( this.timestamp.equals( otherPacket.getTimestamp() )
					&& this.srcAddr.equals( otherPacket.getSrcAddr() )
					&& this.dstAddr.equals( otherPacket.getDstAddr() )
					&& this.protocol == otherPacket.getProtocol() 
					&& this.ttl == otherPacket.getTtl()
					&& this.length == otherPacket.getLength()
			)
				return true;
		}
		return false;
	}

	// override to string method
	@Override
	public String toString( ) {
		return this.timestamp.toString() + ": " + 
			this.srcAddr + "(" + this.srcPort + ") > " + this.dstAddr + "(" + this.dstPort +   
			")\n\t" + super.toString();
	}

}
