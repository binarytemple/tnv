/**
 * Created on Apr 13, 2004 
 * @author jgood
 * 
 * Panel that controls all visualization properties
 */
package net.sourceforge.tnv;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EtchedBorder;

/**
 * TNVHighlightPanel
 */
public class TNVFilterPanel extends JPanel {

	private final static String[] COMPARATOR = { "Reset", ">=", "<=", "=" };
	
	// Links
	private JCheckBox highlightTCPlinkChBx, highlightUDPlinkChBx, highlightICMPlinkChBx;
	private JCheckBox filterTCPlinkChBx, filterUDPlinkChBx, filterICMPlinkChBx;

	private ButtonGroup linkDirectionGroup;
	private JRadioButton linkIncomingButton, linkOutgoingButton, linkBothButton;

	// Hosts
	
	
	// Packets
	private JCheckBox highlightTCPpacketChBx, highlightUDPpacketChBx, highlightICMPpacketChBx,
		highlightSYNpacketChBx, highlightACKpacketChBx, highlightFINpacketChBx,
		highlightURGpacketChBx, highlightPSHpacketChBx, highlightRSTpacketChBx;

	private JCheckBox showTCPpacketChBx, showUDPpacketChBx, showICMPpacketChBx;
	
	// Ports
	private JTextField showPortField;
	private ButtonGroup showPortFilterGroup;
	private JRadioButton showSrcPorts, showDstPorts, showBothPorts;

	private JTextField highlightPortField;
	private ButtonGroup portFilterGroup;
	private JRadioButton highlightSrcPorts, highlightDstPorts, highlightBothPorts;

	// Length and TTL
	private JComboBox showTtlComboBox, showLengthComboBox;
	private JTextField showTtlField, showLengthField;

	private JComboBox highlightTtlComboBox, highlightLengthComboBox;
	private JTextField highlightTtlField, highlightLengthField;
	

	// Constructor
	protected TNVFilterPanel() {
		super();
		initGUI();
	}


	private void initGUI( ) {


		/*
		 * FILTER
		 */
		
		// FILTER LINKS
		
		// Direction of link
		JPanel outLinkDirectionPanel = new JPanel();
		outLinkDirectionPanel.setLayout( new BoxLayout( outLinkDirectionPanel, BoxLayout.Y_AXIS ) );

		JPanel linkDirectionPanel = new JPanel( new BorderLayout() );
		linkDirectionPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Show links by direction" ) );
		linkDirectionPanel.setMinimumSize( new Dimension( 200,50 ) );
		linkDirectionPanel.setPreferredSize( new Dimension( 200,50 ) );
		linkDirectionPanel.setMaximumSize( new Dimension( 250,55 ) );
		
		JPanel linkDirectionInnerPanel = new JPanel();
		linkDirectionInnerPanel.setLayout( new BoxLayout( linkDirectionInnerPanel, BoxLayout.X_AXIS ) );

		this.linkIncomingButton = new JRadioButton( "In" );
		this.linkIncomingButton.setToolTipText( "Show only ingress links" );
		this.linkIncomingButton.setActionCommand( "in" );

		this.linkOutgoingButton = new JRadioButton( "Out" );
		this.linkOutgoingButton.setToolTipText( "Show only egress links" );
		this.linkOutgoingButton.setActionCommand( "out" );

		this.linkBothButton = new JRadioButton( "Both" );
		this.linkBothButton.setToolTipText( "Show both ingress and egress links" );
		this.linkBothButton.setActionCommand( "both" );

		this.linkBothButton.setSelected( true ); // default, both in and out
		TNVModel.getInstance().setLinkDirection( TNVModel.DisplayLinkDirection.BOTH );

		this.linkDirectionGroup = new ButtonGroup();
		this.linkDirectionGroup.add( this.linkIncomingButton );
		this.linkDirectionGroup.add( this.linkOutgoingButton );
		this.linkDirectionGroup.add( this.linkBothButton );

		ActionListener linkDirectionActionListener = new LinkDirectionActionListener();
		this.linkIncomingButton.addActionListener( linkDirectionActionListener );
		this.linkOutgoingButton.addActionListener( linkDirectionActionListener );
		this.linkBothButton.addActionListener( linkDirectionActionListener );

		linkDirectionInnerPanel.add( this.linkIncomingButton );
		linkDirectionInnerPanel.add( this.linkOutgoingButton );
		linkDirectionInnerPanel.add( this.linkBothButton );
		linkDirectionPanel.add( linkDirectionInnerPanel, BorderLayout.CENTER );
		outLinkDirectionPanel.add(linkDirectionPanel);
				

		// filter links panel
		JPanel showLinkPanel = new JPanel();
		showLinkPanel.setLayout( new BoxLayout( showLinkPanel, BoxLayout.Y_AXIS ) );
		showLinkPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Show links by protocol" ) );

		// filter TCP links
		this.filterTCPlinkChBx = new JCheckBox( "Show TCP links" );
		this.filterTCPlinkChBx.setToolTipText( "Check to show TCP links" );
		this.filterTCPlinkChBx.setMaximumSize( new Dimension( 250, 20 ) );
		this.filterTCPlinkChBx.setSelected( true );
		TNVModel.getInstance().setShowTCPlinks( this.filterTCPlinkChBx.isSelected() );
		this.filterTCPlinkChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setShowTCPlinks( true );
				else
					TNVModel.getInstance().setShowTCPlinks( false );
			}
		} );
		JPanel showTcpPanel = new JPanel();
		showTcpPanel.setLayout( new BoxLayout( showTcpPanel, BoxLayout.X_AXIS ) );
		showTcpPanel.add( this.filterTCPlinkChBx );
		showLinkPanel.add( showTcpPanel );

		// filter UDP links
		this.filterUDPlinkChBx = new JCheckBox( "Show UDP links" );
		this.filterUDPlinkChBx.setToolTipText( "Check to show UDP links" );
		this.filterUDPlinkChBx.setMaximumSize( new Dimension( 250, 20 ) );
		this.filterUDPlinkChBx.setSelected( true );
		TNVModel.getInstance().setShowUDPlinks( this.filterUDPlinkChBx.isSelected() );
		this.filterUDPlinkChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setShowUDPlinks( true );
				else
					TNVModel.getInstance().setShowUDPlinks( false );
			}
		} );
		JPanel showUdpPanel = new JPanel();
		showUdpPanel.setLayout( new BoxLayout( showUdpPanel, BoxLayout.X_AXIS ) );
		showUdpPanel.add( this.filterUDPlinkChBx );
		showLinkPanel.add( showUdpPanel );

		// filter ICMP links
		this.filterICMPlinkChBx = new JCheckBox( "Show ICMP links" );
		this.filterICMPlinkChBx.setToolTipText( "Check to show ICMP links" );
		this.filterICMPlinkChBx.setMaximumSize( new Dimension( 250, 20 ) );
		this.filterICMPlinkChBx.setSelected( true );
		TNVModel.getInstance().setShowICMPlinks( this.filterICMPlinkChBx.isSelected() );
		this.filterICMPlinkChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setShowICMPlinks( true );
				else
					TNVModel.getInstance().setShowICMPlinks( false );
			}
		} );
		JPanel showIcmpPanel = new JPanel();
		showIcmpPanel.setLayout( new BoxLayout( showIcmpPanel, BoxLayout.X_AXIS ) );
		showIcmpPanel.add( this.filterICMPlinkChBx );
		showLinkPanel.add( showIcmpPanel );
		
		// FILTER PACKETS
		
		// show panel
		JPanel showPacketPanel = new JPanel();
		showPacketPanel.setLayout( new BoxLayout( showPacketPanel, BoxLayout.Y_AXIS ) );
		showPacketPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Show packets by protocol" ) );

		// show TCP packets
		this.showTCPpacketChBx = new JCheckBox( "Show TCP packets" );
		this.showTCPpacketChBx.setToolTipText( "Check to show TCP packets" );
		this.showTCPpacketChBx.setMaximumSize( new Dimension( 200, 20 ) );
		this.showTCPpacketChBx.setSelected( true );
		TNVModel.getInstance().setShowTCPpackets( this.showTCPpacketChBx.isSelected() );
		this.showTCPpacketChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setShowTCPpackets( true );
				else
					TNVModel.getInstance().setShowTCPpackets( false );
			}
		} );
		JPanel showTcpPacketPanel = new JPanel();
		showTcpPacketPanel.setLayout( new BoxLayout( showTcpPacketPanel, BoxLayout.X_AXIS ) );
		showTcpPacketPanel.add( this.showTCPpacketChBx );
		showPacketPanel.add( showTcpPacketPanel );
		
		// show UDP packets
		this.showUDPpacketChBx = new JCheckBox( "Show UDP packets" );
		this.showUDPpacketChBx.setToolTipText( "Check to show UDP packets" );
		this.showUDPpacketChBx.setMaximumSize( new Dimension( 200, 20 ) );
		this.showUDPpacketChBx.setSelected( true );
		TNVModel.getInstance().setShowUDPpackets( this.showUDPpacketChBx.isSelected() );
		this.showUDPpacketChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setShowUDPpackets( true );
				else
					TNVModel.getInstance().setShowUDPpackets( false );
			}
		} );
		JPanel showUdpPacketPanel = new JPanel();
		showUdpPacketPanel.setLayout( new BoxLayout( showUdpPacketPanel, BoxLayout.X_AXIS ) );
		showUdpPacketPanel.add( this.showUDPpacketChBx );
		showPacketPanel.add( showUdpPacketPanel );

		// show ICMP packets
		this.showICMPpacketChBx = new JCheckBox( "Show ICMP packets" );
		this.showICMPpacketChBx.setToolTipText( "Check to show ICMP packets" );
		this.showICMPpacketChBx.setMaximumSize( new Dimension( 200, 20 ) );
		this.showICMPpacketChBx.setSelected( true );
		TNVModel.getInstance().setShowICMPpackets( this.showICMPpacketChBx.isSelected() );
		this.showICMPpacketChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setShowICMPpackets( true );
				else
					TNVModel.getInstance().setShowICMPpackets( false );
			}
		} );
		JPanel showIcmpPacketPanel = new JPanel();
		showIcmpPacketPanel.setLayout( new BoxLayout( showIcmpPacketPanel, BoxLayout.X_AXIS ) );
		showIcmpPacketPanel.add( this.showICMPpacketChBx );
		showPacketPanel.add( showIcmpPacketPanel );
				
		// show packets by port panel
		JPanel showPacketPortPanel = new JPanel();
		showPacketPortPanel.setLayout( new BoxLayout( showPacketPortPanel, BoxLayout.Y_AXIS ) );
		showPacketPortPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Show packets by port" ) );
		
		// Label for port showing
		JPanel showPortLabelPanel = new JPanel();
		showPortLabelPanel.setLayout( new BoxLayout( showPortLabelPanel, BoxLayout.X_AXIS ) );
		JLabel showPortLabel = new JLabel( "Ports (enter to apply)" );
		showPortLabel.setToolTipText( "If not blank, show _only_ these ports; type enter to apply" );
		showPortLabel.setMaximumSize( new Dimension( 200, 25 ) );
		showPortLabelPanel.add( showPortLabel );
		showPacketPortPanel.add( showPortLabelPanel );

		// Input field for showing ports
		JPanel showPortFieldPanel = new JPanel();
		showPortFieldPanel.setLayout( new BoxLayout( showPortFieldPanel, BoxLayout.X_AXIS ) );
		this.showPortField = new JTextField( 10 );
		this.showPortField.setToolTipText( "Comma delimited list of ports to show (deleted text to undo); type enter to apply" );
		this.showPortField.setMaximumSize( new Dimension( 180, 25 ) );
		this.showPortField.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				SortedSet<String> portsToShowSet = new TreeSet<String>();
				String s = ( (JTextField)e.getSource() ).getText();
				String removedSpaces = Pattern.compile( "\\s*" ).matcher( s ).replaceAll( "" ); // strip																				// spaces
				// split on comma and add digits to list
				StringTokenizer st = new StringTokenizer( removedSpaces, "," );
				while ( st.hasMoreTokens() ) {
					String nextStr = st.nextToken();
					if ( Pattern.compile( "\\d+" ).matcher( nextStr ).matches() 
							&& Integer.parseInt( nextStr ) < 65536 )
						portsToShowSet.add( nextStr );
				}
				TNVModel.getInstance().setShowPorts( portsToShowSet );
			}
		} );
		showPortFieldPanel.add( this.showPortField );
		showPacketPortPanel.add( showPortFieldPanel );

		// Selection for src, dst, or both ports
		JPanel showPortTypePanel = new JPanel( new BorderLayout() );
		showPortTypePanel.setMaximumSize( new Dimension( 200, 25 ) );

		JPanel showPortTypeInnerPanel = new JPanel();
		showPortTypeInnerPanel.setLayout( new BoxLayout( showPortTypeInnerPanel, BoxLayout.X_AXIS ) );
		
		this.showSrcPorts = new JRadioButton( "Src" );
		this.showSrcPorts.setToolTipText( "Show only for source ports" );
		this.showSrcPorts.setActionCommand( "src" );
		showPortTypeInnerPanel.add( this.showSrcPorts );

		this.showDstPorts = new JRadioButton( "Dst" );
		this.showDstPorts.setToolTipText( "Show only for destination ports" );
		this.showDstPorts.setActionCommand( "dst" );
		showPortTypeInnerPanel.add( this.showDstPorts );
		
		this.showBothPorts = new JRadioButton( "Both" );
		this.showBothPorts.setToolTipText( "Show for both source and destination ports" );
		this.showBothPorts.setActionCommand( "all" );
		showPortTypeInnerPanel.add( this.showBothPorts );

		this.showBothPorts.setSelected( true );
		TNVModel.getInstance().setShowPortDirection( TNVModel.DisplayPacketType.BOTH );

		this.showPortFilterGroup = new ButtonGroup();
		this.showPortFilterGroup.add( this.showBothPorts );
		this.showPortFilterGroup.add( this.showSrcPorts );
		this.showPortFilterGroup.add( this.showDstPorts );

		ActionListener showPortTypeListener = new ShowPortTypeActionListener();
		this.showBothPorts.addActionListener( showPortTypeListener );
		this.showSrcPorts.addActionListener( showPortTypeListener );
		this.showDstPorts.addActionListener( showPortTypeListener );
		
		showPortTypePanel.add(showPortTypeInnerPanel, BorderLayout.CENTER );
		showPacketPortPanel.add( showPortTypePanel );
		
		
		// show packets by Length panel
		JPanel showPacketLengthPanel = new JPanel();
		showPacketLengthPanel.setLayout( new BoxLayout( showPacketLengthPanel, BoxLayout.Y_AXIS ) );
		showPacketLengthPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Show packets by size" ) );

		JPanel showLengthInnerLabelPanel = new JPanel();
		showLengthInnerLabelPanel.setLayout( new BoxLayout( showLengthInnerLabelPanel, BoxLayout.X_AXIS ) );
		
		JLabel showLengthPacketLabel = new JLabel("Length ");
		
		this.showLengthComboBox = new JComboBox(COMPARATOR);
		this.showLengthComboBox.setToolTipText("Change comparator or reset filter");
		TNVModel.getInstance().setShowLengthModifier( TNVModel.ValueModifier.GREATER_THAN ); // set default
		this.showLengthComboBox.setMaximumSize( new Dimension( 60, 25 ) );
		this.showLengthComboBox.setSelectedIndex(1);
		this.showLengthComboBox.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
		        String modifier = (String)cb.getSelectedItem();
		        TNVModel.ValueModifier mod = TNVModel.ValueModifier.GREATER_THAN;
		        if ( modifier.equals("Reset") ) {
		        	TNVFilterPanel.this.showLengthComboBox.setSelectedIndex(1); //greater than is default
		        	TNVFilterPanel.this.showLengthField.setText("");
		        	TNVModel.getInstance().setShowLengthValue( 0 );
		        }
		        else if ( modifier.equals("=") )
		        	mod = TNVModel.ValueModifier.EQUAL_TO;
		        else if ( modifier.equals("<=") )
		        	mod = TNVModel.ValueModifier.LESS_THAN;
		        TNVModel.getInstance().setShowLengthModifier( mod );
			}
		} );

		this.showLengthField = new JTextField( 5 );
		this.showLengthField.setMaximumSize( new Dimension( 60, 25 ) );
		this.showLengthField.setToolTipText("Filter on total packet length; type enter to apply");
		this.showLengthField.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				String s = ( (JTextField)e.getSource() ).getText();
				int value = Integer.parseInt(s);
				TNVModel.getInstance().setShowLengthValue( value );
			}
		} );

		showLengthInnerLabelPanel.add( showLengthPacketLabel );
		showLengthInnerLabelPanel.add( this.showLengthComboBox );
		showLengthInnerLabelPanel.add( this.showLengthField );

		JPanel showLengthPanel = new JPanel();
		showLengthPanel.setLayout( new BorderLayout() );
		showLengthPanel.add(showLengthInnerLabelPanel, BorderLayout.CENTER);
		showLengthPanel.setMaximumSize( new Dimension( 200, 25 ) );
		
		showPacketLengthPanel.add( showLengthPanel );

		
		// show packets by TTL panel
		JPanel showPacketTtlPanel = new JPanel();
		showPacketTtlPanel.setLayout( new BoxLayout( showPacketTtlPanel, BoxLayout.Y_AXIS ) );
		showPacketTtlPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Show packets by TTL" ) );
		
		JPanel showTtlInnerPanel = new JPanel();
		showTtlInnerPanel.setLayout( new BoxLayout( showTtlInnerPanel, BoxLayout.X_AXIS ) );
		
		JLabel showTtlPacketLabel = new JLabel("TTL ");
		
		this.showTtlComboBox = new JComboBox(COMPARATOR);
		this.showTtlComboBox.setToolTipText("Change comparator or reset filter");
		TNVModel.getInstance().setShowTtlModifier( TNVModel.ValueModifier.GREATER_THAN ); // set default
		this.showTtlComboBox.setMaximumSize( new Dimension( 60, 25 ) );
		this.showTtlComboBox.setSelectedIndex(1);
		this.showTtlComboBox.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
		        String modifier = (String)cb.getSelectedItem();
		        TNVModel.ValueModifier mod = TNVModel.ValueModifier.GREATER_THAN;
		        if ( modifier.equals("Reset") ) {
		        	TNVFilterPanel.this.showTtlComboBox.setSelectedIndex(1);
		        	TNVFilterPanel.this.showTtlField.setText("");
		        	TNVModel.getInstance().setShowTtlValue( 0 );
		        }
		        else if ( modifier.equals("=") )
		        	mod = TNVModel.ValueModifier.EQUAL_TO;
		        else if ( modifier.equals("<=") )
		        	mod = TNVModel.ValueModifier.LESS_THAN;
		        TNVModel.getInstance().setShowTtlModifier( mod );
			}
		} );

		this.showTtlField = new JTextField( 5 );
		this.showTtlField.setMaximumSize( new Dimension( 60, 25 ) );
		this.showTtlField.setToolTipText("Filter on time to live value; type enter to apply");
		this.showTtlField.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				String s = ( (JTextField)e.getSource() ).getText();
				int value = Integer.parseInt(s);
				TNVModel.getInstance().setShowTtlValue( value );
			}
		} );

		showTtlInnerPanel.add( showTtlPacketLabel );
		showTtlInnerPanel.add( this.showTtlComboBox );
		showTtlInnerPanel.add( this.showTtlField );

		JPanel showTtlPanel = new JPanel();
		showTtlPanel.setLayout( new BorderLayout() );
		showTtlPanel.add(showTtlInnerPanel, BorderLayout.CENTER);
		showTtlPanel.setMaximumSize( new Dimension( 200, 25 ) );
		
		showPacketTtlPanel.add( showTtlPanel );
	
		
		
		
		/*
		 * HIGHLIGHT
		 */
				
		// highlight links panel
		JPanel highlightLinkPanel = new JPanel();
		highlightLinkPanel.setLayout( new BoxLayout( highlightLinkPanel, BoxLayout.Y_AXIS ) );
		highlightLinkPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Highlight links by protocol" ) );

		// highlight TCP links
		this.highlightTCPlinkChBx = new JCheckBox( "Highlight TCP links" );
		this.highlightTCPlinkChBx.setToolTipText( "Check to emphasize TCP links" );
		this.highlightTCPlinkChBx.setMaximumSize( new Dimension( 200, 20 ) );
		this.highlightTCPlinkChBx.setSelected( false );
		TNVModel.getInstance().setHighlightTCPlinks( this.highlightTCPlinkChBx.isSelected() );
		this.highlightTCPlinkChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setHighlightTCPlinks( true );
				else
					TNVModel.getInstance().setHighlightTCPlinks( false );
			}
		} );
		JPanel tcpPanel = new JPanel();
		tcpPanel.setLayout( new BoxLayout( tcpPanel, BoxLayout.X_AXIS ) );
		tcpPanel.add( this.highlightTCPlinkChBx );
		highlightLinkPanel.add( tcpPanel );

		// highlight UDP links
		this.highlightUDPlinkChBx = new JCheckBox( "Highlight UDP links" );
		this.highlightUDPlinkChBx.setToolTipText( "Check to emphasize UDP links" );
		this.highlightUDPlinkChBx.setMaximumSize( new Dimension( 200, 20 ) );
		this.highlightUDPlinkChBx.setSelected( false );
		TNVModel.getInstance().setHighlightUDPlinks( this.highlightUDPlinkChBx.isSelected() );
		this.highlightUDPlinkChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setHighlightUDPlinks( true );
				else
					TNVModel.getInstance().setHighlightUDPlinks( false );
			}
		} );
		JPanel udpPanel = new JPanel();
		udpPanel.setLayout( new BoxLayout( udpPanel, BoxLayout.X_AXIS ) );
		udpPanel.add( this.highlightUDPlinkChBx );
		highlightLinkPanel.add( udpPanel );

		// highlight ICMP links
		this.highlightICMPlinkChBx = new JCheckBox( "Highlight ICMP links" );
		this.highlightICMPlinkChBx.setToolTipText( "Check to emphasize ICMP links" );
		this.highlightICMPlinkChBx.setMaximumSize( new Dimension( 200, 20 ) );
		this.highlightICMPlinkChBx.setSelected( false );
		TNVModel.getInstance().setHighlightICMPlinks( this.highlightICMPlinkChBx.isSelected() );
		this.highlightICMPlinkChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setHighlightICMPlinks( true );
				else
					TNVModel.getInstance().setHighlightICMPlinks( false );
			}
		} );
		JPanel icmpPanel = new JPanel();
		icmpPanel.setLayout( new BoxLayout( icmpPanel, BoxLayout.X_AXIS ) );
		icmpPanel.add( this.highlightICMPlinkChBx );
		highlightLinkPanel.add( icmpPanel );
		highlightLinkPanel.add( Box.createVerticalStrut(5) );
		

		/*
		 * PACKETS 
		 */
		
		// highlight packets by protocol panel
		JPanel highlightPacketProtocolPanel = new JPanel();
		highlightPacketProtocolPanel.setLayout( new BoxLayout( highlightPacketProtocolPanel, BoxLayout.Y_AXIS ) );
		highlightPacketProtocolPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Highlight packets by protocol" ) );

		// highlight TCP packets
		this.highlightTCPpacketChBx = new JCheckBox( "TCP packets" );
		this.highlightTCPpacketChBx.setToolTipText( "Check to emphasize TCP packets" );
		this.highlightTCPpacketChBx.setMaximumSize( new Dimension( 200, 20 ) );
		this.highlightTCPpacketChBx.setSelected( false );
		TNVModel.getInstance().setHighlightTCPpackets( this.highlightTCPpacketChBx.isSelected() );
		this.highlightTCPpacketChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setHighlightTCPpackets( true );
				else
					TNVModel.getInstance().setHighlightTCPpackets( false );
			}
		} );
		JPanel tcpPacketPanel = new JPanel();
		tcpPacketPanel.setLayout( new BoxLayout( tcpPacketPanel, BoxLayout.X_AXIS ) );
		tcpPacketPanel.add( this.highlightTCPpacketChBx );
		highlightPacketProtocolPanel.add( tcpPacketPanel );
		
		// highlight TCP flags
		
		this.highlightSYNpacketChBx = new JCheckBox( "TCP SYN packets" );
		this.highlightSYNpacketChBx.setToolTipText( "Check to emphasize TCP SYN packets" );
		this.highlightSYNpacketChBx.setMaximumSize( new Dimension( 200, 20 ) );
		this.highlightSYNpacketChBx.setSelected( false );
		TNVModel.getInstance().setHighlightSYNpackets( this.highlightSYNpacketChBx.isSelected() );
		this.highlightSYNpacketChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setHighlightSYNpackets( true );
				else
					TNVModel.getInstance().setHighlightSYNpackets( false );
			}
		} );
		JPanel synPacketPanel = new JPanel();
		synPacketPanel.setLayout( new BoxLayout( synPacketPanel, BoxLayout.X_AXIS ) );
		synPacketPanel.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );
		synPacketPanel.add( this.highlightSYNpacketChBx );
		highlightPacketProtocolPanel.add( synPacketPanel );

		this.highlightACKpacketChBx = new JCheckBox( "TCP ACK packets" );
		this.highlightACKpacketChBx.setToolTipText( "Check to emphasize TCP ACK packets" );
		this.highlightACKpacketChBx.setMaximumSize( new Dimension( 200, 20 ) );
		this.highlightACKpacketChBx.setSelected( false );
		TNVModel.getInstance().setHighlightACKpackets( this.highlightACKpacketChBx.isSelected() );
		this.highlightACKpacketChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setHighlightACKpackets( true );
				else
					TNVModel.getInstance().setHighlightACKpackets( false );
			}
		} );
		JPanel ackPacketPanel = new JPanel();
		ackPacketPanel.setLayout( new BoxLayout( ackPacketPanel, BoxLayout.X_AXIS ) );
		ackPacketPanel.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );
		ackPacketPanel.add( this.highlightACKpacketChBx );
		highlightPacketProtocolPanel.add( ackPacketPanel );

		this.highlightFINpacketChBx = new JCheckBox( "TCP FIN packets" );
		this.highlightFINpacketChBx.setToolTipText( "Check to emphasize TCP FIN packets" );
		this.highlightFINpacketChBx.setMaximumSize( new Dimension( 200, 20 ) );
		this.highlightFINpacketChBx.setSelected( false );
		TNVModel.getInstance().setHighlightFINpackets( this.highlightFINpacketChBx.isSelected() );
		this.highlightFINpacketChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setHighlightFINpackets( true );
				else
					TNVModel.getInstance().setHighlightFINpackets( false );
			}
		} );
		JPanel finPacketPanel = new JPanel();
		finPacketPanel.setLayout( new BoxLayout( finPacketPanel, BoxLayout.X_AXIS ) );
		finPacketPanel.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );
		finPacketPanel.add( this.highlightFINpacketChBx );
		highlightPacketProtocolPanel.add( finPacketPanel );

		this.highlightPSHpacketChBx = new JCheckBox( "TCP PSH packets" );
		this.highlightPSHpacketChBx.setToolTipText( "Check to emphasize TCP PSH packets" );
		this.highlightPSHpacketChBx.setMaximumSize( new Dimension( 200, 20 ) );
		this.highlightPSHpacketChBx.setSelected( false );
		TNVModel.getInstance().setHighlightPSHpackets( this.highlightPSHpacketChBx.isSelected() );
		this.highlightPSHpacketChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setHighlightPSHpackets( true );
				else
					TNVModel.getInstance().setHighlightPSHpackets( false );
			}
		} );
		JPanel pshPacketPanel = new JPanel();
		pshPacketPanel.setLayout( new BoxLayout( pshPacketPanel, BoxLayout.X_AXIS ) );
		pshPacketPanel.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );
		pshPacketPanel.add( this.highlightPSHpacketChBx );
		highlightPacketProtocolPanel.add( pshPacketPanel );

		this.highlightURGpacketChBx = new JCheckBox( "TCP URG packets" );
		this.highlightURGpacketChBx.setToolTipText( "Check to emphasize TCP URG packets" );
		this.highlightURGpacketChBx.setMaximumSize( new Dimension( 200, 20 ) );
		this.highlightURGpacketChBx.setSelected( false );
		TNVModel.getInstance().setHighlightURGpackets( this.highlightURGpacketChBx.isSelected() );
		this.highlightURGpacketChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setHighlightURGpackets( true );
				else
					TNVModel.getInstance().setHighlightURGpackets( false );
			}
		} );
		JPanel urgPacketPanel = new JPanel();
		urgPacketPanel.setLayout( new BoxLayout( urgPacketPanel, BoxLayout.X_AXIS ) );
		urgPacketPanel.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );
		urgPacketPanel.add( this.highlightURGpacketChBx );
		highlightPacketProtocolPanel.add( urgPacketPanel );

		this.highlightRSTpacketChBx = new JCheckBox( "TCP RST packets" );
		this.highlightRSTpacketChBx.setToolTipText( "Check to emphasize TCP RST packets" );
		this.highlightRSTpacketChBx.setMaximumSize( new Dimension( 200, 20 ) );
		this.highlightRSTpacketChBx.setSelected( false );
		TNVModel.getInstance().setHighlightRSTpackets( this.highlightRSTpacketChBx.isSelected() );
		this.highlightRSTpacketChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setHighlightRSTpackets( true );
				else
					TNVModel.getInstance().setHighlightRSTpackets( false );
			}
		} );
		JPanel rstPacketPanel = new JPanel();
		rstPacketPanel.setLayout( new BoxLayout( rstPacketPanel, BoxLayout.X_AXIS ) );
		rstPacketPanel.add( Box.createRigidArea( new Dimension( 10, 0 ) ) );
		rstPacketPanel.add( this.highlightRSTpacketChBx );
		highlightPacketProtocolPanel.add( rstPacketPanel );
		

		// highlight UDP packets
		this.highlightUDPpacketChBx = new JCheckBox( "UDP packets" );
		this.highlightUDPpacketChBx.setToolTipText( "Check to emphasize UDP packets" );
		this.highlightUDPpacketChBx.setMaximumSize( new Dimension( 200, 20 ) );
		this.highlightUDPpacketChBx.setSelected( false );
		TNVModel.getInstance().setHighlightUDPpackets( this.highlightUDPpacketChBx.isSelected() );
		this.highlightUDPpacketChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setHighlightUDPpackets( true );
				else
					TNVModel.getInstance().setHighlightUDPpackets( false );
			}
		} );
		JPanel udpPacketPanel = new JPanel();
		udpPacketPanel.setLayout( new BoxLayout( udpPacketPanel, BoxLayout.X_AXIS ) );
		udpPacketPanel.add( this.highlightUDPpacketChBx );
		highlightPacketProtocolPanel.add( udpPacketPanel );

		// highlight ICMP packets
		this.highlightICMPpacketChBx = new JCheckBox( "ICMP packets" );
		this.highlightICMPpacketChBx.setToolTipText( "Check to emphasize ICMP packets" );
		this.highlightICMPpacketChBx.setMaximumSize( new Dimension( 200, 20 ) );
		this.highlightICMPpacketChBx.setSelected( false );
		TNVModel.getInstance().setHighlightICMPpackets( this.highlightICMPpacketChBx.isSelected() );
		this.highlightICMPpacketChBx.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVModel.getInstance().setHighlightICMPpackets( true );
				else
					TNVModel.getInstance().setHighlightICMPpackets( false );
			}
		} );
		JPanel icmpPacketPanel = new JPanel();
		icmpPacketPanel.setLayout( new BoxLayout( icmpPacketPanel, BoxLayout.X_AXIS ) );
		icmpPacketPanel.add( this.highlightICMPpacketChBx );
		highlightPacketProtocolPanel.add( icmpPacketPanel );
		highlightPacketProtocolPanel.add( Box.createVerticalStrut(20) );
		

		// highlight packets by port panel
		JPanel highlightPacketPortPanel = new JPanel();
		highlightPacketPortPanel.setLayout( new BoxLayout( highlightPacketPortPanel, BoxLayout.Y_AXIS ) );
		highlightPacketPortPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Highlight packets by port" ) );
		
		// Label for port highlighting
		JPanel portLabelPanel = new JPanel();
		portLabelPanel.setLayout( new BoxLayout( portLabelPanel, BoxLayout.X_AXIS ) );
		JLabel highlightPortLabel = new JLabel( "Ports (enter to apply)" );
		highlightPortLabel.setToolTipText( "If not blank, highlight these ports; type enter to apply" );
		highlightPortLabel.setMaximumSize( new Dimension( 200, 25 ) );
		portLabelPanel.add( highlightPortLabel );
		highlightPacketPortPanel.add( portLabelPanel );

		// Input field for highlighting ports
		JPanel portFieldPanel = new JPanel();
		portFieldPanel.setLayout( new BoxLayout( portFieldPanel, BoxLayout.X_AXIS ) );
		this.highlightPortField = new JTextField( 10 );
		this.highlightPortField.setToolTipText( "Comma delimited list of ports to emphasize (deleted text to undo); type enter to apply" );
		this.highlightPortField.setMaximumSize( new Dimension( 180, 25 ) );
		this.highlightPortField.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				SortedSet<String> portsToShowSet = new TreeSet<String>();
				String s = ( (JTextField)e.getSource() ).getText();
				String removedSpaces = Pattern.compile( "\\s*" ).matcher( s ).replaceAll( "" ); // strip																				// spaces
				// split on comma and add digits to list
				StringTokenizer st = new StringTokenizer( removedSpaces, "," );
				while ( st.hasMoreTokens() ) {
					String nextStr = st.nextToken();
					if ( Pattern.compile( "\\d+" ).matcher( nextStr ).matches() 
							&& Integer.parseInt( nextStr ) < 65536 )
						portsToShowSet.add( nextStr );
				}
				TNVModel.getInstance().setHighlightPorts( portsToShowSet );
			}
		} );
		portFieldPanel.add( this.highlightPortField );
		highlightPacketPortPanel.add( portFieldPanel );

		// Selection for src, dst, or both ports
		JPanel portTypePanel = new JPanel( new BorderLayout() );
		portTypePanel.setMaximumSize( new Dimension( 200, 25 ) );

		JPanel portTypeInnerPanel = new JPanel();
		portTypeInnerPanel.setLayout( new BoxLayout( portTypeInnerPanel, BoxLayout.X_AXIS ) );
		
		this.highlightSrcPorts = new JRadioButton( "Src" );
		this.highlightSrcPorts.setToolTipText( "Emphasize for only source ports" );
		this.highlightSrcPorts.setActionCommand( "src" );
		portTypeInnerPanel.add( this.highlightSrcPorts );

		this.highlightDstPorts = new JRadioButton( "Dst" );
		this.highlightDstPorts.setToolTipText( "Emphasize for only destination ports" );
		this.highlightDstPorts.setActionCommand( "dst" );
		portTypeInnerPanel.add( this.highlightDstPorts );
		
		this.highlightBothPorts = new JRadioButton( "Both" );
		this.highlightBothPorts.setToolTipText( "Emphasize for both source and destination ports" );
		this.highlightBothPorts.setActionCommand( "all" );
		portTypeInnerPanel.add( this.highlightBothPorts );

		this.highlightBothPorts.setSelected( true );
		TNVModel.getInstance().setHighlightPortDirection( TNVModel.DisplayPacketType.BOTH );

		this.portFilterGroup = new ButtonGroup();
		this.portFilterGroup.add( this.highlightBothPorts );
		this.portFilterGroup.add( this.highlightSrcPorts );
		this.portFilterGroup.add( this.highlightDstPorts );

		ActionListener portTypeListener = new PortTypeActionListener();
		this.highlightBothPorts.addActionListener( portTypeListener );
		this.highlightSrcPorts.addActionListener( portTypeListener );
		this.highlightDstPorts.addActionListener( portTypeListener );
		
		portTypePanel.add(portTypeInnerPanel, BorderLayout.CENTER );
		highlightPacketPortPanel.add( portTypePanel );
		
		
		// highlight packets by Length panel
		JPanel highlightPacketLengthPanel = new JPanel();
		highlightPacketLengthPanel.setLayout( new BoxLayout( highlightPacketLengthPanel, BoxLayout.Y_AXIS ) );
		highlightPacketLengthPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Highlight packets by size" ) );

		JPanel lengthInnerLabelPanel = new JPanel();
		lengthInnerLabelPanel.setLayout( new BoxLayout( lengthInnerLabelPanel, BoxLayout.X_AXIS ) );
		
		JLabel lengthPacketLabel = new JLabel("Length ");
		
		this.highlightLengthComboBox = new JComboBox(COMPARATOR);
		this.highlightLengthComboBox.setToolTipText("Change comparator or reset filter");
		TNVModel.getInstance().setHighlightLengthModifier( TNVModel.ValueModifier.GREATER_THAN ); // set default
		this.highlightLengthComboBox.setMaximumSize( new Dimension( 60, 25 ) );
		this.highlightLengthComboBox.setSelectedIndex(1);
		this.highlightLengthComboBox.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
		        String modifier = (String)cb.getSelectedItem();
		        TNVModel.ValueModifier mod = TNVModel.ValueModifier.GREATER_THAN;
		        if ( modifier.equals("Reset") ) {
		        	TNVFilterPanel.this.highlightLengthComboBox.setSelectedIndex(1);
		        	TNVFilterPanel.this.highlightLengthField.setText("");
		        	TNVModel.getInstance().setHighlightLengthValue( 0 );
		        }
		        else if ( modifier.equals("=") )
		        	mod = TNVModel.ValueModifier.EQUAL_TO;
		        else if ( modifier.equals("<=") )
		        	mod = TNVModel.ValueModifier.LESS_THAN;
		        TNVModel.getInstance().setHighlightLengthModifier( mod );
			}
		} );

		this.highlightLengthField = new JTextField( 5 );
		this.highlightLengthField.setMaximumSize( new Dimension( 60, 25 ) );
		this.highlightLengthField.setToolTipText("Emphasize on total packet length; type enter to apply");
		this.highlightLengthField.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				String s = ( (JTextField)e.getSource() ).getText();
				int value = Integer.parseInt(s);
				TNVModel.getInstance().setHighlightLengthValue( value );
			}
		} );

		lengthInnerLabelPanel.add( lengthPacketLabel );
		lengthInnerLabelPanel.add( this.highlightLengthComboBox );
		lengthInnerLabelPanel.add( this.highlightLengthField );

		JPanel lengthPanel = new JPanel();
		lengthPanel.setLayout( new BorderLayout() );
		lengthPanel.add(lengthInnerLabelPanel, BorderLayout.CENTER);
		lengthPanel.setMaximumSize( new Dimension( 200, 25 ) );
		
		highlightPacketLengthPanel.add( lengthPanel );

		
		// highlight packets by TTL panel
		JPanel highlightPacketTtlPanel = new JPanel();
		highlightPacketTtlPanel.setLayout( new BoxLayout( highlightPacketTtlPanel, BoxLayout.Y_AXIS ) );
		highlightPacketTtlPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Highlight packets by TTL" ) );
		
		JPanel ttlInnerPanel = new JPanel();
		ttlInnerPanel.setLayout( new BoxLayout( ttlInnerPanel, BoxLayout.X_AXIS ) );
		
		JLabel ttlPacketLabel = new JLabel("TTL ");
		
		this.highlightTtlComboBox = new JComboBox(COMPARATOR);
		this.highlightTtlComboBox.setToolTipText("Change comparator or reset filter");
		TNVModel.getInstance().setHighlightTtlModifier( TNVModel.ValueModifier.GREATER_THAN ); // set default
		this.highlightTtlComboBox.setMaximumSize( new Dimension( 60, 25 ) );
		this.highlightTtlComboBox.setSelectedIndex(1);
		this.highlightTtlComboBox.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox)e.getSource();
		        String modifier = (String)cb.getSelectedItem();
		        TNVModel.ValueModifier mod = TNVModel.ValueModifier.GREATER_THAN;
		        if ( modifier.equals("Reset") ) {
		        	TNVFilterPanel.this.highlightTtlComboBox.setSelectedIndex(1);
		        	TNVFilterPanel.this.highlightTtlField.setText("");
		        	TNVModel.getInstance().setHighlightTtlValue( 0 );
		        }
		        else if ( modifier.equals("=") )
		        	mod = TNVModel.ValueModifier.EQUAL_TO;
		        else if ( modifier.equals("<=") )
		        	mod = TNVModel.ValueModifier.LESS_THAN;
		        TNVModel.getInstance().setHighlightTtlModifier( mod );
			}
		} );

		this.highlightTtlField = new JTextField( 5 );
		this.highlightTtlField.setMaximumSize( new Dimension( 60, 25 ) );
		this.highlightTtlField.setToolTipText("Emphasize on time to live; type enter to apply");
		this.highlightTtlField.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				String s = ( (JTextField)e.getSource() ).getText();
				int value = Integer.parseInt(s);
				TNVModel.getInstance().setHighlightTtlValue( value );
			}
		} );

		ttlInnerPanel.add( ttlPacketLabel );
		ttlInnerPanel.add( this.highlightTtlComboBox );
		ttlInnerPanel.add( this.highlightTtlField );

		JPanel ttlPanel = new JPanel();
		ttlPanel.setLayout( new BorderLayout() );
		ttlPanel.add(ttlInnerPanel, BorderLayout.CENTER);
		ttlPanel.setMaximumSize( new Dimension( 200, 25 ) );
		
		highlightPacketTtlPanel.add( ttlPanel );
		
				
		
		
		
		// SPLIT PANE
		
		// Top split for filter controls
		JPanel filterControls = new JPanel();

		// filter label
		JPanel filterLabelPanel = new JPanel( new FlowLayout(FlowLayout.CENTER) );
		filterLabelPanel.setMinimumSize( new Dimension( 200,25 ) );
		filterLabelPanel.setPreferredSize( new Dimension( 200,25 ) );
		filterLabelPanel.setMaximumSize( new Dimension( 250,30 ) );
		JLabel filterLabel = new JLabel("- Filter Controls -");
		filterLabel.setFont(TNVUtil.LARGE_LABEL_FONT);
		filterLabelPanel.add(filterLabel);
		
		filterControls.setLayout( new BoxLayout( filterControls, BoxLayout.Y_AXIS ) );
		filterControls.add( filterLabelPanel );
		filterControls.add( Box.createVerticalStrut(5) );
		filterControls.add( outLinkDirectionPanel );
		filterControls.add( Box.createVerticalStrut(5) );
		filterControls.add( showLinkPanel );
		filterControls.add( Box.createVerticalStrut(5) );
		filterControls.add( showPacketPanel );
		filterControls.add( Box.createVerticalStrut(5) );
		filterControls.add( showPacketPortPanel );
		filterControls.add( Box.createVerticalStrut(5) );
		filterControls.add( showPacketLengthPanel );
		filterControls.add( Box.createVerticalStrut(5) );
		filterControls.add( showPacketTtlPanel );
		filterControls.add( Box.createVerticalStrut(5) );
		
		JScrollPane filterScrollPane = new JScrollPane(filterControls, 
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		
		// Bottom split for highlight controls
		JPanel highlightControls = new JPanel();

		// highglight label
		JPanel highlightLabelPanel = new JPanel( new FlowLayout(FlowLayout.CENTER) );
		highlightLabelPanel.setMinimumSize( new Dimension( 200,25 ) );
		highlightLabelPanel.setPreferredSize( new Dimension( 200,25 ) );
		highlightLabelPanel.setMaximumSize( new Dimension( 250,30 ) );
		JLabel highlightLabel = new JLabel("- Highlight Controls -");
		highlightLabel.setFont(TNVUtil.LARGE_LABEL_FONT);
		highlightLabelPanel.add(highlightLabel);
		
		highlightControls.setLayout( new BoxLayout( highlightControls, BoxLayout.Y_AXIS ) );
		highlightControls.add( highlightLabelPanel );
		highlightControls.add( Box.createVerticalStrut(5) );
		highlightControls.add( highlightLinkPanel );
		highlightControls.add( Box.createVerticalStrut(5) );
		highlightControls.add( highlightPacketProtocolPanel );
		highlightControls.add( Box.createVerticalStrut(5) );
		highlightControls.add( highlightPacketPortPanel );
		highlightControls.add( Box.createVerticalStrut(5) );
		highlightControls.add( highlightPacketLengthPanel );
		highlightControls.add( Box.createVerticalStrut(5) );
		highlightControls.add( highlightPacketTtlPanel );
		highlightControls.add( Box.createVerticalStrut(5) );
				
		JScrollPane highlightScrollPane = new JScrollPane(highlightControls, 
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		// Add both scroll panes to split pane
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                filterScrollPane, highlightScrollPane);
		splitPane.setResizeWeight(0.5);
		splitPane.setDividerSize(12);
		splitPane.setToolTipText("Change pane size by moving the divider up or down");
		
		this.setLayout( new BorderLayout() );
		this.add( splitPane, BorderLayout.CENTER );
		
	}


	
	/**
	 * action listener for showing packets by port type
	 * ShowPortTypeActionListener
	 */
	private class ShowPortTypeActionListener implements ActionListener {
		public void actionPerformed( ActionEvent e ) {
			String choice = TNVFilterPanel.this.showPortFilterGroup.getSelection().getActionCommand();
			if ( choice.equals( "all" ) )
				TNVModel.getInstance().setShowPortDirection( TNVModel.DisplayPacketType.BOTH );
			else if ( choice.equals( "src" ) )
				TNVModel.getInstance().setShowPortDirection( TNVModel.DisplayPacketType.SRC );
			else if ( choice.equals( "dst" ) )
				TNVModel.getInstance().setShowPortDirection( TNVModel.DisplayPacketType.DST );
		}
	}

	/**
	 * action listener for port type highlighting
	 * PortTypeActionListener
	 */
	private class PortTypeActionListener implements ActionListener {
		public void actionPerformed( ActionEvent e ) {
			String choice = TNVFilterPanel.this.portFilterGroup.getSelection().getActionCommand();
			if ( choice.equals( "all" ) )
				TNVModel.getInstance().setHighlightPortDirection( TNVModel.DisplayPacketType.BOTH );
			else if ( choice.equals( "src" ) )
				TNVModel.getInstance().setHighlightPortDirection( TNVModel.DisplayPacketType.SRC );
			else if ( choice.equals( "dst" ) )
				TNVModel.getInstance().setHighlightPortDirection( TNVModel.DisplayPacketType.DST );
		}
	}

	
	/**
	 * action listener for link type 
	 * LinkDirectionActionListener
	 */
	private class LinkDirectionActionListener implements ActionListener {
		public void actionPerformed( ActionEvent e ) {
			String choice = TNVFilterPanel.this.linkDirectionGroup.getSelection().getActionCommand();
			if ( choice.equals( "in" ) )
				TNVModel.getInstance().setLinkDirection( TNVModel.DisplayLinkDirection.INGRESS );
			else if ( choice.equals( "out" ) )
				TNVModel.getInstance().setLinkDirection( TNVModel.DisplayLinkDirection.EGRESS );
			else if ( choice.equals( "both" ) )
				TNVModel.getInstance().setLinkDirection( TNVModel.DisplayLinkDirection.BOTH );
		}
	}

}
