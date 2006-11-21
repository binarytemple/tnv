/**
 * Created on Apr 13, 2004 
 * @author jgood
 * 
 * Panel that controls all visualization properties
 */
package net.sourceforge.tnv;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * TNVHighlightPanel
 */
public class TNVDisplayPanel extends JPanel {

	// Constants
	private static final int MIN_MACHINE_HEIGHT = 25, MAX_MACHINE_HEIGHT = 100;
	private static final int MIN_COLUMN_COUNT = 3, MAX_COLUMN_COUNT = 15;

	// Home network
	private TNVHomeNetPanel homeNetPanel;
	
	// Column and Row preferences
	private JSlider rowHeightSlider, columnCountSlider;

	// Tooltips
	private JCheckBox showTooltipsCheckBox;
	
	// Links
	private ButtonGroup linkTypeGroup;
	private JRadioButton linkLineButton, linkArcButton;
	
	// Packets
	private JCheckBox showPacketCheckBox, showTcpFlagsCheckBox;

	// Constructor
	protected TNVDisplayPanel() {
		super();
		initGUI();
	}


	private void initGUI( ) {

		/*
		 * Home network
		 */
		
		JPanel outNetPanel = new JPanel();
		outNetPanel.setLayout( new BoxLayout( outNetPanel, BoxLayout.Y_AXIS ) );

		JPanel netPanel = new JPanel( new BorderLayout() );
		netPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Home Network" ) );
		netPanel.setMinimumSize( new Dimension( 200,150 ) );
		netPanel.setPreferredSize( new Dimension( 200,155 ) );
		netPanel.setMaximumSize( new Dimension( 250,155 ) );

		this.homeNetPanel = new TNVHomeNetPanel( TNVPreferenceData.getInstance().getHomeNet() );

		JPanel setHomeNetPanel = new JPanel( new FlowLayout( FlowLayout.TRAILING ) );
		JButton setHomeNetButton = new JButton( "Set" );
		setHomeNetButton.setToolTipText("Click to set new home network; you must close/reopen data set to apply changes");
		setHomeNetButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVPreferenceData.getInstance().setHomeNet( TNVDisplayPanel.this.homeNetPanel.getNetworkAddr() );
				TNVPreferenceData.getInstance().saveProperties();
			}
		} );
		setHomeNetPanel.add(setHomeNetButton);

		JPanel homeNetLabelPanel = new JPanel( new FlowLayout( FlowLayout.CENTER ) );
		JLabel homeNetLabel = new JLabel("(You must restart to apply)");
		homeNetLabel.setFont(TNVUtil.SMALL_LABEL_FONT);
		homeNetLabelPanel.add( homeNetLabel );
		
		netPanel.add(homeNetPanel, BorderLayout.NORTH);
		netPanel.add(setHomeNetPanel, BorderLayout.CENTER);
		netPanel.add(homeNetLabelPanel, BorderLayout.SOUTH);
		outNetPanel.add(netPanel);
		
		
		/*
		 * Number of columns
		 */
		
		JPanel outColumnPanel = new JPanel();
		outColumnPanel.setLayout( new BoxLayout( outColumnPanel, BoxLayout.Y_AXIS ) );

		JPanel columnPanel = new JPanel( new BorderLayout() );
		columnPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Number of Columns" ) );
		columnPanel.setMinimumSize( new Dimension( 200,80 ) );
		columnPanel.setPreferredSize( new Dimension( 200,80 ) );
		columnPanel.setMaximumSize( new Dimension( 250,80 ) );

		this.columnCountSlider = new JSlider( SwingConstants.HORIZONTAL );
		this.columnCountSlider.setToolTipText("Use to adjust the number of columns displayed for local hosts");
		this.columnCountSlider.setMajorTickSpacing( 4 );
		this.columnCountSlider.setMaximum( MAX_COLUMN_COUNT );
		this.columnCountSlider.setMinimum( MIN_COLUMN_COUNT );
		this.columnCountSlider.setMinorTickSpacing( 1 );
		this.columnCountSlider.setPaintLabels( true );
		this.columnCountSlider.setPaintTicks( true );
		this.columnCountSlider.setSnapToTicks( true );
		this.columnCountSlider.setValue(TNVPreferenceData.getInstance().getColumnCount());
		this.columnCountSlider.addChangeListener( new ChangeListener() {
			public void stateChanged( ChangeEvent e ) {
				JSlider source = (JSlider) e.getSource();
				if ( !source.getValueIsAdjusting() ) {
					TNVPreferenceData.getInstance().setColumnCount( source.getValue() );
					TNVPreferenceData.getInstance().saveProperties();
				}
			}
		} );
		columnPanel.add( this.columnCountSlider );
		
		outColumnPanel.add(columnPanel, BorderLayout.CENTER);

		
		/*
		 * Default local machine heights
		 */
		JPanel outHeightPanel = new JPanel();
		outHeightPanel.setLayout( new BoxLayout( outHeightPanel, BoxLayout.Y_AXIS ) );

		JPanel heightPanel = new JPanel( new BorderLayout() );
		heightPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Default Host Height" ) );
		heightPanel.setMinimumSize( new Dimension( 200,80 ) );
		heightPanel.setPreferredSize( new Dimension( 200,80 ) );
		heightPanel.setMaximumSize( new Dimension( 250,80 ) );

		this.rowHeightSlider = new JSlider( SwingConstants.HORIZONTAL );
		this.rowHeightSlider.setToolTipText("Use to adjust the default row height; display size may ignore this");
		this.rowHeightSlider.setMajorTickSpacing( 25 );
		this.rowHeightSlider.setMaximum( MAX_MACHINE_HEIGHT );
		this.rowHeightSlider.setMinimum( MIN_MACHINE_HEIGHT );
		this.rowHeightSlider.setMinorTickSpacing( 5 );
		this.rowHeightSlider.setPaintLabels( true );
		this.rowHeightSlider.setPaintTicks( true );
		this.rowHeightSlider.setSnapToTicks( true );
		this.rowHeightSlider.setValue( TNVPreferenceData.getInstance().getRowHeight() );
		this.rowHeightSlider.addChangeListener( new ChangeListener() {
			public void stateChanged( ChangeEvent e ) {
				JSlider source = (JSlider) e.getSource();
				if ( !source.getValueIsAdjusting() ) {
					TNVPreferenceData.getInstance().setRowHeight( source.getValue() );
					TNVPreferenceData.getInstance().saveProperties();
				}
			}
		} );
		heightPanel.add( this.rowHeightSlider );
		
		outHeightPanel.add(heightPanel, BorderLayout.CENTER);

		
		/*
		 * Link type
		 */
		
		JPanel linkPanel = new JPanel();
		linkPanel.setLayout( new BoxLayout( linkPanel, BoxLayout.Y_AXIS ) );

		JPanel linkTypePanel = new JPanel( new BorderLayout() );
		linkTypePanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Link Type" ) );
		linkTypePanel.setMinimumSize( new Dimension( 200,50 ) );
		linkTypePanel.setPreferredSize( new Dimension( 200,55 ) );
		linkTypePanel.setMaximumSize( new Dimension( 250,55 ) );
		
		JPanel linkTypeInnerPanel = new JPanel();
		linkTypeInnerPanel.setLayout( new BoxLayout( linkTypeInnerPanel, BoxLayout.X_AXIS ) );

		this.linkLineButton = new JRadioButton( "Straight" );
		this.linkLineButton.setToolTipText( "Show links as straight line segments" );
		this.linkLineButton.setActionCommand( "line" );

		this.linkArcButton = new JRadioButton( "Curved" );
		this.linkArcButton.setToolTipText( "Show links as curved arcs" );
		this.linkArcButton.setActionCommand( "arc" );

		if ( TNVPreferenceData.getInstance().isCurvedLinks() )
			this.linkArcButton.setSelected( true );
		else 
			this.linkLineButton.setSelected( true );

		this.linkTypeGroup = new ButtonGroup();
		this.linkTypeGroup.add( this.linkLineButton );
		this.linkTypeGroup.add( this.linkArcButton );

		ActionListener linkTypeActionListener = new LinkTypeActionListener();
		this.linkLineButton.addActionListener( linkTypeActionListener );
		this.linkArcButton.addActionListener( linkTypeActionListener );

		linkTypeInnerPanel.add( this.linkLineButton );
		linkTypeInnerPanel.add( this.linkArcButton );
		linkTypePanel.add( linkTypeInnerPanel, BorderLayout.CENTER );
		
		linkPanel.add( linkTypePanel );
		
		
		/*
		 * Tooltips
		 */
		JPanel outTooltipPanel = new JPanel();
		outTooltipPanel.setLayout( new BoxLayout( outTooltipPanel, BoxLayout.Y_AXIS ) );

		JPanel tooltipPanel = new JPanel( new BorderLayout() );
		tooltipPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Tooltips" ) );
		tooltipPanel.setMinimumSize( new Dimension( 200,50 ) );
		tooltipPanel.setPreferredSize( new Dimension( 200,55 ) );
		tooltipPanel.setMaximumSize( new Dimension( 250,55 ) );

		this.showTooltipsCheckBox = new JCheckBox( "Show tooltips" );
		this.showTooltipsCheckBox.setToolTipText( "Check this to show tooltips for hosts and links" );
		this.showTooltipsCheckBox.setSelected( TNVPreferenceData.getInstance().isShowTooltips() );
		this.showTooltipsCheckBox.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.SELECTED )
					TNVPreferenceData.getInstance().setShowTooltips( true );
				else
					TNVPreferenceData.getInstance().setShowTooltips( false );
				TNVPreferenceData.getInstance().saveProperties();
			}
		} );
		tooltipPanel.add( this.showTooltipsCheckBox, BorderLayout.CENTER );
		outTooltipPanel.add(tooltipPanel);
		
		
		/*
		 * Packet Display
		 */
		JPanel outPacketPanel = new JPanel();
		outPacketPanel.setLayout( new BoxLayout( outPacketPanel, BoxLayout.Y_AXIS ) );

		JPanel packetPanel = new JPanel( new BorderLayout() );
		packetPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Packet Display" ) );
		packetPanel.setMinimumSize( new Dimension( 200,65 ) );
		packetPanel.setPreferredSize( new Dimension( 200,70 ) );
		packetPanel.setMaximumSize( new Dimension( 250,70 ) );

		// Show packets checkbox
		this.showPacketCheckBox = new JCheckBox( "Display packets" );
		this.showPacketCheckBox.setAlignmentX( Component.LEFT_ALIGNMENT );
		this.showPacketCheckBox.setToolTipText( "Check to show packets for localhosts" );
		this.showPacketCheckBox.setSelected( TNVPreferenceData.getInstance().isShowPackets() ); 
		this.showPacketCheckBox.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.DESELECTED )
					TNVPreferenceData.getInstance().setShowPackets( false );
				else 
					TNVPreferenceData.getInstance().setShowPackets( true );
				TNVPreferenceData.getInstance().saveProperties();
			}
		} );
		JPanel showPacketPanel = new JPanel();
		showPacketPanel.setLayout( new BoxLayout( showPacketPanel, BoxLayout.X_AXIS ) );
		showPacketPanel.add( this.showPacketCheckBox );
		packetPanel.add( showPacketPanel, BorderLayout.NORTH );

		// Show tcp flags checkbox
		this.showTcpFlagsCheckBox = new JCheckBox( "Display TCP flags" );
		this.showTcpFlagsCheckBox.setAlignmentX( Component.LEFT_ALIGNMENT );
		this.showTcpFlagsCheckBox.setToolTipText( "Check to display TCP flags on packets" );
		this.showTcpFlagsCheckBox.setSelected( TNVPreferenceData.getInstance().isShowFlags() );
		this.showTcpFlagsCheckBox.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.DESELECTED )
					TNVPreferenceData.getInstance().setShowFlags( false );
				else
					TNVPreferenceData.getInstance().setShowFlags( true );
				TNVPreferenceData.getInstance().saveProperties();
			}
		} );
		JPanel showTcpFlagsPanel = new JPanel();
		showTcpFlagsPanel.setLayout( new BoxLayout( showTcpFlagsPanel, BoxLayout.X_AXIS ) );
		showTcpFlagsPanel.add( this.showTcpFlagsCheckBox );
		packetPanel.add( showTcpFlagsPanel, BorderLayout.CENTER );
		
		outPacketPanel.add(packetPanel);

		
		/*
		 * Reset Button 
		 */
		JPanel outResetPanel = new JPanel();
		outResetPanel.setLayout( new BoxLayout( outResetPanel, BoxLayout.Y_AXIS ) );

		JPanel resetPanel = new JPanel( new FlowLayout( FlowLayout.TRAILING ) );
		resetPanel.setBorder( BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Reset Display" ) );
		resetPanel.setMinimumSize( new Dimension( 200,60 ) );
		resetPanel.setPreferredSize( new Dimension( 200,60 ) );
		resetPanel.setMaximumSize( new Dimension( 250,60 ) );
		JButton resetButton = new JButton( "Reset" );
		resetButton.setToolTipText("Click to reset to initial display");
		resetButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVUIManager.resetUI();
			}
		} );
		resetPanel.add(resetButton);
		outResetPanel.add(resetPanel);
		
		
		
		/*
		 * BUILD THIS PANEL
		 */
		JPanel controls = new JPanel();
		controls.setLayout( new BoxLayout( controls, BoxLayout.Y_AXIS ) );
		controls.add( Box.createVerticalStrut(5) );
		controls.add( outNetPanel );
		controls.add( Box.createVerticalStrut(10) );
		controls.add( outColumnPanel );
		controls.add( Box.createVerticalStrut(10) );
		controls.add( outHeightPanel );
		controls.add( Box.createVerticalStrut(10) );
		controls.add( linkPanel );
		controls.add( Box.createVerticalStrut(10) );
		controls.add( outTooltipPanel );
		controls.add( Box.createVerticalStrut(10) );
		controls.add( outPacketPanel );
		controls.add( Box.createVerticalStrut(10) );
		controls.add( outResetPanel );
		controls.add( Box.createVerticalStrut(5) );
		
		JScrollPane displayScroll = new JScrollPane(controls, 
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		this.setLayout( new BorderLayout() );
		this.add( displayScroll, BorderLayout.CENTER );
	}


	
	/**
	 * action listener for link type
	 * LinkTypeActionListener
	 */
	private class LinkTypeActionListener implements ActionListener {
		public void actionPerformed( ActionEvent e ) {
			String choice = TNVDisplayPanel.this.linkTypeGroup.getSelection().getActionCommand();
			if ( choice.equals( "line" ) )
				TNVPreferenceData.getInstance().setCurvedLinks(false);
			else if ( choice.equals( "arc" ) )
				TNVPreferenceData.getInstance().setCurvedLinks(true);
			TNVPreferenceData.getInstance().saveProperties();
		}
	}

}
