/**
 * Created on Apr 13, 2004
 * @author jgood
 * 
 * This dialog window displays the packet details
 */
package net.sourceforge.tnv;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.net.InetAddress;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.jpcap.net.EthernetPacket;
import net.sourceforge.jpcap.net.ICMPMessage;
import net.sourceforge.jpcap.net.ICMPPacket;
import net.sourceforge.jpcap.net.IGMPPacket;
import net.sourceforge.jpcap.net.IPPacket;
import net.sourceforge.jpcap.net.IPProtocol;
import net.sourceforge.jpcap.net.LinkLayer;
import net.sourceforge.jpcap.net.LinkLayers;
import net.sourceforge.jpcap.net.Packet;
import net.sourceforge.jpcap.net.PacketFactory;
import net.sourceforge.jpcap.net.RawPacket;
import net.sourceforge.jpcap.net.TCPPacket;
import net.sourceforge.jpcap.net.UDPPacket;
import net.sourceforge.jpcap.util.AsciiHelper;
import net.sourceforge.jpcap.util.HexHelper;

/**
 * TNVDetailWindow
 */
public class TNVDetailWindow extends JFrame {

	private static final int WIDTH = 900;
	private static final int HEIGHT = 600;

	private JSplitPane detailSplitPane;
	private JTable detailTable;
	private TNVDetailTableModel tableModel;
	private TNVDetailTableSorter sorter;
	private JTree detailTree;
	private DefaultTreeModel detailTreeModel;
	private DefaultMutableTreeNode rootNode, frameNode, ethernetNode, ipNode, payloadNode;
	private JScrollPane detailScrollPane, indivDetailScrollPane;
	private boolean showAscii, showHex;


	/**
	 * Constructor - show all packets
	 */
	private TNVDetailWindow() {
		super();
		
		initWindow();

		// Populate table
		this.tableModel.addMultipleRows( TNVDbUtil.getInstance().getPacketList( ) );
		this.showWindow("Details for all packets");		
	}
	
	/**
	 * Constructor - show packets for host/time
	 * @param hosts
	 */
	private TNVDetailWindow(Set<TNVLocalHostCell> hosts) {
		super();
		initWindow();

		String title = "Details for: ";
		// Populate table
		for ( TNVLocalHostCell node : hosts ) {
			String name = node.getName();
			title += name + " ";
			this.tableModel.addMultipleRows( TNVDbUtil.getInstance().getPacketList( name, 
					node.getStartTime(), node.getEndTime() ) );
		}

		this.showWindow(title);		
	}


	/**
	 * initialize window 
	 */
	private void initWindow() {
		this.showAscii = true;
		this.showHex = true;

		// Setup detail table
		this.tableModel = new TNVDetailTableModel();
		sorter = new TNVDetailTableSorter( this.tableModel );
		
		// new table with custom renderer for row colors
		this.detailTable = new JTable( sorter );
		sorter.setTableHeader( this.detailTable.getTableHeader() );
		this.detailTable.getTableHeader().setToolTipText( "Click a column heading to sort that column" );
		this.detailTable.setFont( TNVUtil.LABEL_FONT );

		this.detailTable.setGridColor( Color.darkGray );
		this.detailTable.setShowGrid( true );
		this.detailTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
		this.detailTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		setColumnWidths();

		ListSelectionModel rowSM = this.detailTable.getSelectionModel();
		rowSM.addListSelectionListener( new ListSelectionListener() {
			public void valueChanged( ListSelectionEvent e ) {
				// Ignore extra messages.
				if ( e.getValueIsAdjusting() ) 
					return;
				ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				clearTree();
				if ( ! lsm.isSelectionEmpty() ) {
					// get the model index of the selected row in the view from the sorter
					int modelIndex = TNVDetailWindow.this.sorter.modelIndex( TNVDetailWindow.this.detailTable.getSelectedRow() );
					setIndivDetails( TNVDetailWindow.this.tableModel.getRow( modelIndex ), ( modelIndex + 1 ) );
				}
			}
		} );

		this.detailScrollPane = new JScrollPane( this.detailTable );

		// Set up individual packet details tree
		this.rootNode = new DefaultMutableTreeNode( "Packet Details" );
		this.detailTreeModel = new DefaultTreeModel( this.rootNode );
		this.detailTree = new JTree( this.detailTreeModel );
		this.detailTree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );
		this.detailTree.setEditable( false );
		this.detailTree.setFont( TNVUtil.LABEL_FONT );
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		Icon treeIcon = null;
		renderer.setLeafIcon( treeIcon );
		renderer.setClosedIcon( treeIcon );
		renderer.setOpenIcon( treeIcon );
		this.detailTree.setCellRenderer( renderer );
		this.detailTree.setToolTipText( "Expand tree by selecting on items" );

		this.indivDetailScrollPane = new JScrollPane( this.detailTree );

		this.detailSplitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT, this.detailScrollPane,
				this.indivDetailScrollPane );
		this.detailSplitPane.setResizeWeight( 0.4 ); // extra space to details

		this.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
		this.getContentPane().add( this.detailSplitPane, BorderLayout.CENTER );
		this.pack();	
	}
	
	
	/**
	 * Show the window
	 * @param title
	 */
	private void showWindow(String title) {
		// Set up frame size and location
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int x = ( (int) dim.getWidth() / 2 ) - ( WIDTH / 2 );
		int y = (int) dim.getHeight() - HEIGHT - 20;
		this.setBounds( x, y, WIDTH, HEIGHT );

		this.setTitle( title );
		this.setVisible( true );
	}
	
	
	/**
	 * Set up a blank table
	 */
	protected void clearDetails( ) {
		this.tableModel.clear();
		clearTree();
	}


	/**
	 * Set up a blank tree
	 */
	protected void clearTree( ) {
		this.detailTree.collapseRow( 0 );
		this.rootNode.removeAllChildren();
		this.detailTreeModel.reload();
	}


	/**
	 * Set up the tree for individual packet details
	 * @param raw
	 * @param frameNum
	 */
	protected void setIndivDetails( RawPacket raw, int frameNum ) {
		Packet p = PacketFactory.dataToPacket( LinkLayers.IEEE802, raw.getData() );
		EthernetPacket ethp = (EthernetPacket) p;
		IPPacket ipp = (IPPacket) p;
		byte[] data = {};
		String src = "", srcName = "", dst = "", dstName = "", ipCheckCorrect = "";

		// summary node
		this.frameNode = new DefaultMutableTreeNode( "Frame Number: " + frameNum + " " );
		this.rootNode.add( this.frameNode );
		this.frameNode.add( new DefaultMutableTreeNode( "Arrival Time: "
				+ TNVUtil.LONG_FORMAT.format( raw.getTimeval().getDate() ) ) );
		this.frameNode.add( new DefaultMutableTreeNode( "Frame Number: " + frameNum ) );
		this.frameNode.add( new DefaultMutableTreeNode( "Packet Length: " + "" ) );
		this.frameNode.add( new DefaultMutableTreeNode( "Capture Length: " + "" ) );

		// ETHERNET node
		this.ethernetNode = new DefaultMutableTreeNode( "Ethernet, Src Addr: " + ethp.getSourceHwAddress()
				+ ", Dst Addr: " + ethp.getDestinationHwAddress() );

		this.rootNode.add( this.ethernetNode );
		this.ethernetNode.add( new DefaultMutableTreeNode( "Destination: " + ethp.getDestinationHwAddress() ) );
		this.ethernetNode.add( new DefaultMutableTreeNode( "Source: " + ethp.getSourceHwAddress() ) );
		this.ethernetNode.add( new DefaultMutableTreeNode( "Type: " + LinkLayer.getDescription( ethp.getProtocol() )
				+ " (0x" + HexHelper.toString( ethp.getProtocol() ) + ")" ) );

		// IP
		try {
			src = ipp.getSourceAddress();
			srcName = InetAddress.getByName( src ).getHostName();
		}
		catch ( Exception e ) {
			src = ipp.getSourceAddress();
			srcName = src;
		}
		try {
			dst = ipp.getDestinationAddress();
			dstName = InetAddress.getByName( dst ).getHostName();
		}
		catch ( Exception e ) {
			dst = ipp.getDestinationAddress();
			dstName = dst;
		}

		this.ipNode = new DefaultMutableTreeNode( "Internet Protocol, Src Addr: " + srcName + " (" + src
				+ "), Dst Addr: " + dstName + " (" + dst + ")" );
		this.rootNode.add( this.ipNode );

		if ( ipp.isValidIPChecksum() )
			ipCheckCorrect = " (correct) ";
		else
			ipCheckCorrect = " (incorrect) ";
		this.ipNode.add( new DefaultMutableTreeNode( "Version: " + ipp.getVersion() ) );
		this.ipNode.add( new DefaultMutableTreeNode( "Header length: " + ipp.getIPHeaderLength() + " bytes" ) );
		// ipNode.add(new DefaultMutableTreeNode("Total length: " +
		// (ipp.getLength() + ipp.getIPHeaderLength()) + " bytes"));
		this.ipNode.add( new DefaultMutableTreeNode( "Total length: " + ipp.getLength() + " bytes" ) );
		this.ipNode.add( new DefaultMutableTreeNode( "Identification: 0x" + HexHelper.toString( ipp.getId() ) + " ("
				+ ipp.getId() + ")" ) );
		this.ipNode.add( new DefaultMutableTreeNode( "Flags: 0x" + HexHelper.toString( ipp.getFragmentFlags() ) + " ("
				+ ipp.getFragmentFlags() + ")" ) );
		this.ipNode.add( new DefaultMutableTreeNode( "Fragment offset: " + ipp.getFragmentOffset() ) );
		this.ipNode.add( new DefaultMutableTreeNode( "Time to live: " + ipp.getTimeToLive() ) );
		this.ipNode.add( new DefaultMutableTreeNode( "Protocol: " + IPProtocol.getDescription( ipp.getProtocol() )
				+ " (0x" + HexHelper.toString( ipp.getProtocol() ) + ")" ) );
		this.ipNode.add( new DefaultMutableTreeNode( "Header checksum: 0x" + HexHelper.toString( ipp.getIPChecksum() )
				+ ipCheckCorrect ) );
		this.ipNode.add( new DefaultMutableTreeNode( "Source: " + srcName + " (" + src + ")" ) );
		this.ipNode.add( new DefaultMutableTreeNode( "Destination: " + dstName + " (" + dst + ")" ) );

		// Print more detailed information for specific IP protocols
		if ( p instanceof TCPPacket ) {
			TCPPacket tcpP = (TCPPacket) p;
			int srcPort = tcpP.getSourcePort();
			String srcPortName = TNVUtil.getPortDescr( srcPort );
			int dstPort = tcpP.getDestinationPort();
			String dstPortName = TNVUtil.getPortDescr( dstPort );
			DefaultMutableTreeNode tcpNode = new DefaultMutableTreeNode( "Transmission Control Protocol"
					+ ", Src Port: " + srcPortName + " (" + srcPort + ")" + ", Dst Port: " + dstPortName + " ("
					+ dstPort + ")" );
			this.rootNode.add( tcpNode );
			tcpNode.add( new DefaultMutableTreeNode( "Source port: " + srcPortName + " (" + srcPort + ")" ) );
			tcpNode.add( new DefaultMutableTreeNode( "Destination port: " + dstPortName + " (" + dstPort + ")" ) );
			tcpNode.add( new DefaultMutableTreeNode( "Sequence number: " + tcpP.getSequenceNumber() ) );
			tcpNode.add( new DefaultMutableTreeNode( "Acknowledgment number: " + tcpP.getAcknowledgmentNumber() ) );
			tcpNode.add( new DefaultMutableTreeNode( "Header length: " + tcpP.getTCPHeaderLength() + " bytes" ) );
			DefaultMutableTreeNode tcpFlagsNode = new DefaultMutableTreeNode( "Flags" );
			tcpFlagsNode.add( new DefaultMutableTreeNode( "Urgent: " + setFlag( tcpP.isUrg() ) ) );
			tcpFlagsNode.add( new DefaultMutableTreeNode( "Acknowledgment: " + setFlag( tcpP.isAck() ) ) );
			tcpFlagsNode.add( new DefaultMutableTreeNode( "Push: " + setFlag( tcpP.isPsh() ) ) );
			tcpFlagsNode.add( new DefaultMutableTreeNode( "Reset: " + setFlag( tcpP.isRst() ) ) );
			tcpFlagsNode.add( new DefaultMutableTreeNode( "Syn: " + setFlag( tcpP.isSyn() ) ) );
			tcpFlagsNode.add( new DefaultMutableTreeNode( "Fin: " + setFlag( tcpP.isFin() ) ) );
			tcpNode.add( tcpFlagsNode );
			tcpNode.add( new DefaultMutableTreeNode( "Window size: " + tcpP.getWindowSize() ) );
			tcpNode.add( new DefaultMutableTreeNode( "Checksum: 0x" + HexHelper.toString( tcpP.getTCPChecksum() ) ) );
			data = tcpP.getData();
		}
		else if ( p instanceof UDPPacket ) {
			UDPPacket udpP = (UDPPacket) p;
			int srcPort = udpP.getSourcePort();
			String srcPortName = TNVUtil.getPortDescr( srcPort );
			int dstPort = udpP.getDestinationPort();
			String dstPortName = TNVUtil.getPortDescr( dstPort );
			DefaultMutableTreeNode udpNode = new DefaultMutableTreeNode( "User Datagram Protocol" + ", Src Port: "
					+ srcPortName + " (" + srcPort + ")" + ", Dst Port: " + dstPortName + " (" + dstPort + ")" );
			this.rootNode.add( udpNode );
			udpNode.add( new DefaultMutableTreeNode( "Source port: " + srcPortName + " (" + srcPort + ")" ) );
			udpNode.add( new DefaultMutableTreeNode( "Destination port: " + dstPortName + " (" + dstPort + ")" ) );
			udpNode.add( new DefaultMutableTreeNode( "Length: " + udpP.getLength() + " bytes" ) );
			udpNode.add( new DefaultMutableTreeNode( "Checksum: 0x" + HexHelper.toString( udpP.getUDPChecksum() ) ) );
			data = udpP.getData();
		}
		else if ( p instanceof ICMPPacket ) {
			ICMPPacket icmpP = (ICMPPacket) p;
			boolean valid = icmpP.isValidChecksum();
			DefaultMutableTreeNode icmpNode = new DefaultMutableTreeNode( "Internet Control Message Protocol" );
			this.rootNode.add( icmpNode );
			int type = icmpP.getMessageMajorCode();
			int code = icmpP.getMessageMinorCode();
			icmpNode
					.add( new DefaultMutableTreeNode( "Type: " + type + " (" + ICMPMessage.getDescription( type ) + ")" ) );
			icmpNode.add( new DefaultMutableTreeNode( "Code: " + code + " ("
					+ ICMPMessage.getDescription( icmpP.getMessageCode() ) + ")" ) );
			icmpNode.add( new DefaultMutableTreeNode( "Checksum: 0x" + HexHelper.toString( icmpP.getICMPChecksum() ) ) );
			data = icmpP.getData();
		}
		else if ( p instanceof IGMPPacket ) {
			IGMPPacket igmpP = (IGMPPacket) p;
			DefaultMutableTreeNode igmpNode = new DefaultMutableTreeNode( "IGMP" );
			this.rootNode.add( igmpNode );
			igmpNode.add( new DefaultMutableTreeNode( igmpP.toString() ) );
		}

		if ( data.length != 0 ) {
			this.payloadNode = new DefaultMutableTreeNode( "Payload" );
			this.rootNode.add( this.payloadNode );
			if ( this.showAscii ) {
				DefaultMutableTreeNode asciiPayloadNode = new DefaultMutableTreeNode( "ASCII" );
				asciiPayloadNode.add( new DefaultMutableTreeNode( AsciiHelper.toString( data ) ) );
				this.payloadNode.add( asciiPayloadNode );
			}
			if ( this.showHex ) {
				DefaultMutableTreeNode hexPayloadNode = new DefaultMutableTreeNode( "HEX" );
				hexPayloadNode.add( new DefaultMutableTreeNode( HexHelper.toString( data ) ) );
				this.payloadNode.add( hexPayloadNode );
			}
		}

		this.detailTree.expandRow( 0 ); // expand root node
		this.detailTree.treeDidChange();

	}


	/**
	 * Set flag (set or not set) based on boolean argument
	 * @param f
	 * @return string representation
	 */
	private String setFlag( boolean f ) {
		if ( f ) return "Set (1)";
		return "Not set (0)";
	}


	/**
	 * Set preferred column widths (based on default width of 900)
	 */
	private void setColumnWidths( ) {
		TableColumn column = null;
		for ( int i = 0; i < this.tableModel.getColumnCount(); i++ ) {

			column = this.detailTable.getColumnModel().getColumn( i );
			if ( i == 0 || i == 6  ) 
				column.setPreferredWidth( 40 );	// number or protocol
			else if ( i == 1 )
				column.setPreferredWidth( 175 );	// date
			else if ( i == 2 | i == 4 )
				column.setPreferredWidth( 95 );	// src/dst ip addresses
			else if ( i == 7 )
				column.setPreferredWidth( 345 );	// summary data
			else
				column.setPreferredWidth( 45 );	// src/dst ports
		}
	}

	
	/**
	 * Factory constructor
	 * @return window
	 * @throws HeadlessException
	 */
	protected static TNVDetailWindow createTNVDetailWindow() throws HeadlessException {
		return new TNVDetailWindow();
	}

	/**
	 * Factory constructor
	 * @param set
	 * @return window
	 * @throws HeadlessException
	 */
	protected static TNVDetailWindow createTNVDetailWindow(Set<TNVLocalHostCell> hosts) throws HeadlessException {
		return new TNVDetailWindow(hosts);
	}

}
