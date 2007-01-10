/**
 * Created on Apr 24, 2004 
 * @author jgood
 * 
 * Handles the capturing of packets from file or the wire and gives them to DB
 */
package net.sourceforge.tnv.ui;

import java.sql.Timestamp;

import net.sourceforge.jpcap.capture.RawPacketListener;
import net.sourceforge.jpcap.net.IPPacket;
import net.sourceforge.jpcap.net.LinkLayers;
import net.sourceforge.jpcap.net.Packet;
import net.sourceforge.jpcap.net.PacketFactory;
import net.sourceforge.jpcap.net.RawPacket;
import net.sourceforge.jpcap.net.TCPPacket;
import net.sourceforge.jpcap.net.UDPPacket;
import net.sourceforge.tnv.db.TNVDbUtil;

/**
 * TNVPacketHandler
 */
public class TNVPacketHandler implements RawPacketListener {

	/**
	 * Constructor
	 */
	public TNVPacketHandler() {
		super();
	}


	/* (non-Javadoc)
	 * @see net.sourceforge.jpcap.capture.RawPacketListener#rawPacketArrived(net.sourceforge.jpcap.net.RawPacket)
	 */
	public void rawPacketArrived( RawPacket rawPacket ) {
		try {
			Timestamp time = new Timestamp( ( rawPacket.getTimeval().getDate() ).getTime() );
//			time.setNanos( rawPacket.getTimeval().getMicroSeconds() * 1000 );
			
			Packet packet = PacketFactory.dataToPacket( LinkLayers.IEEE802, rawPacket.getData() );

			// Only capture IP packets
			if ( !( packet instanceof IPPacket ) ) return;

			// Get source and destination ports
			IPPacket ipPacket = (IPPacket) packet; // Cast into IP to get machines

			int srcPort = 0, dstPort = 0;
			if ( ipPacket instanceof TCPPacket ) {
				TCPPacket tcpPacket = (TCPPacket) ipPacket;
				srcPort = tcpPacket.getSourcePort();
				dstPort = tcpPacket.getDestinationPort();
			}
			else if ( ipPacket instanceof UDPPacket ) {
				UDPPacket udpPacket = (UDPPacket) ipPacket;
				srcPort = udpPacket.getSourcePort();
				dstPort = udpPacket.getDestinationPort();
			}

			TNVDbUtil.getInstance().insertPacket( time, ipPacket.getSourceAddress(), srcPort,
					ipPacket.getDestinationAddress(), dstPort, ipPacket.getProtocol(),
					ipPacket.getTimeToLive(), ipPacket.getLength(), rawPacket );

		}
		catch ( Exception e ) {
			System.out.println( "Error during packet capture: " + e.getMessage() );
		}
	}
}
