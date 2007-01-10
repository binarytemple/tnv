/**
 * Created on May 13, 2004
 * @author jgood
 * 
 * Table Model for detail table
 */
package net.sourceforge.tnv.ui;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.sourceforge.jpcap.net.ICMPMessage;
import net.sourceforge.jpcap.net.ICMPPacket;
import net.sourceforge.jpcap.net.IPPacket;
import net.sourceforge.jpcap.net.IPPort;
import net.sourceforge.jpcap.net.IPProtocol;
import net.sourceforge.jpcap.net.IPProtocols;
import net.sourceforge.jpcap.net.LinkLayers;
import net.sourceforge.jpcap.net.Packet;
import net.sourceforge.jpcap.net.PacketFactory;
import net.sourceforge.jpcap.net.RawPacket;
import net.sourceforge.jpcap.net.TCPPacket;
import net.sourceforge.jpcap.net.UDPPacket;

/**
 * TNVDetailTableModel
 */
public class TNVDetailTableModel extends AbstractTableModel {

	private static int NUM_COLUMNS = 8;
	private static int START_NUM_ROWS = 0;
	private static final String num = "Num";
	private static final String time = "Date";
	private static final String srcip = "Src IP";
	private static final String srcport = "SPort";
	private static final String dstip = "Dst IP";
	private static final String dstport = "DPort";
	private static final String proto = "Proto";
	private static final String info = "Packet Info";

	private int numRows = 0;

	private List<RawPacket> data = new ArrayList<RawPacket>();


	/**
	 * Constructor
	 */
	public TNVDetailTableModel() {
		super();
	}


	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount( ) {
		return NUM_COLUMNS;
	}


	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	public int getRowCount( ) {
		if ( this.numRows < START_NUM_ROWS ) {
			return START_NUM_ROWS;
		}
		return this.numRows;
	}


	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
	 */
	@Override
	public String getColumnName( int col ) {
		switch ( col ) {
		case 0:
			return num;
		case 1:
			return time;
		case 2:
			return srcip;
		case 3:
			return srcport;
		case 4:
			return dstip;
		case 5:
			return dstport;
		case 6:
			return proto;
		case 7:
			return info;
		}
		return "";
	}


	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	public Object getValueAt( int row, int col ) {
		try {
			RawPacket raw = this.data.get( row );
			Packet p = PacketFactory.dataToPacket( LinkLayers.IEEE802, raw.getData() );
			IPPacket ipp = (IPPacket) p;
			switch ( col ) {
			case 0:
				return new Integer( this.data.indexOf( raw ) + 1 );
			case 1:
				Timestamp time = new Timestamp( raw.getTimeval().getDate().getTime() );
				time.setNanos( raw.getTimeval().getMicroSeconds() * 1000 );
				return time.toString();
			case 2:
				return ipp.getSourceAddress();
			case 3:
				if ( p instanceof TCPPacket ) {
					TCPPacket tcpP = (TCPPacket) p;
					return getPortDescr( tcpP.getSourcePort() );
				}
				else if ( p instanceof UDPPacket ) {
					UDPPacket udpP = (UDPPacket) p;
					return getPortDescr( udpP.getSourcePort() );
				}
				return "-";
			case 4:
				return ipp.getDestinationAddress();
			case 5:
				if ( p instanceof TCPPacket ) {
					TCPPacket tcpP = (TCPPacket) p;
					return getPortDescr( tcpP.getDestinationPort() );
				}
				else if ( p instanceof UDPPacket ) {
					UDPPacket udpP = (UDPPacket) p;
					return getPortDescr( udpP.getDestinationPort() );
				}
				return "-";
			case 6:
				return getProtocolDescr( ipp );
			case 7:
				String res = "";
				if ( p instanceof TCPPacket ) {
					TCPPacket tcpP = (TCPPacket) p;
					if ( tcpP.isAck() ) res += "[ACK]";
					if ( tcpP.isFin() ) res += "[FIN]";
					if ( tcpP.isPsh() ) res += "[PSH]";
					if ( tcpP.isRst() ) res += "[RST]";
					if ( tcpP.isSyn() ) res += "[SYN]";
					if ( tcpP.isUrg() ) res += "[URG]";
					res += " Seq=" + tcpP.getSequenceNumber();
					res += " Ack=" + tcpP.getAcknowledgementNumber();
					res += " Win=" + tcpP.getWindowSize();
					res += " Len=" + tcpP.getLength();
				}
				else if ( p instanceof UDPPacket ) {
					UDPPacket udpP = (UDPPacket) p;
					res = udpP.toString();
				}
				else if ( p instanceof ICMPPacket ) {
					ICMPPacket icmpP = (ICMPPacket) p;
					res = ICMPMessage.getDescription( icmpP.getMessageCode() );
				}
				else {
					res = ipp.toString();
				}

				return res;
			}
		}
		catch ( Exception e ) {
		}
		return "";
	}


	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
	 */
	@Override
	public Class<? extends Object> getColumnClass( int c ) {
		return getValueAt( 0, c ).getClass();
	}

	
	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
	 */
	@Override
	public boolean isCellEditable( int row, int column ) {
		return false;
	}


	/**
	 * Get packet data by row number
	 * @param row
	 * @return packet
	 */
	public RawPacket getRow( int row ) {
		return this.data.get( row );
	}

	
	/**
	 * Add a packet to model
	 * @param p
	 */
	public void addRow( RawPacket p ) {
		this.data.add( p );
		this.numRows++;
		fireTableRowsInserted( this.numRows, this.numRows );
	}


	/**
	 * Add multiple packets to model
	 * @param c
	 */
	public void addMultipleRows( List<RawPacket> c ) {
		int oldSize = this.data.size();
		this.data.addAll( c );
		this.numRows += c.size();
		fireTableRowsInserted( oldSize, this.numRows );
	}


	/**
	 * Clear model
	 */
	public void clear( ) {
		int oldNumRows = this.numRows;
		this.numRows = START_NUM_ROWS;
		this.data.clear();

		if ( oldNumRows > START_NUM_ROWS ) {
			fireTableRowsDeleted( START_NUM_ROWS, oldNumRows - 1 );
		}
		fireTableRowsUpdated( 0, START_NUM_ROWS - 1 );
	}


	/**
	 * Get textual description of port if available, otherwise use numeric value
	 * @param port
	 * @return port description
	 */
	private static String getPortDescr( int port ) {
		String s = IPPort.getDescription( port );
		if ( s.equals( "unknown" ) ) s = "" + port;
		return s;
	}


	/**
	 * Get textual description of protocol, use short string if available
	 * @param ipp
	 * @return protocol description
	 */
	private String getProtocolDescr( IPPacket ipp ) {
		int protocol = ipp.getIPProtocol();
		if ( protocol == IPProtocols.TCP )
			return "TCP";
		else if ( protocol == IPProtocols.UDP )
			return "UDP";
		else if ( protocol == IPProtocols.ICMP ) 
			return "ICMP";
		return IPProtocol.getDescription( ipp.getProtocol() );
	}

}
