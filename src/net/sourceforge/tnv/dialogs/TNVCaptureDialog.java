/**
 * Created on Sep 26, 2004
 * @author jgood
 * 
 * Dialog window for setting up packet capture
 */
package net.sourceforge.tnv.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.jpcap.capture.PacketCapture;
import net.sourceforge.tnv.TNV;

/**
 * TNVCaptureDialog
 */
public class TNVCaptureDialog extends JDialog {

	private JLabel deviceDesc;
	private SpinnerNumberModel limitPacketSizeModel;
	private JSpinner limitPacketSizeSpinner;
	private SpinnerNumberModel stopByPacketsModel;
	private JSpinner stopByPacketsSpinner;
	private SpinnerNumberModel stopByTimeModel;
	private JButton cancelButton, startButton;

	private Map<String,String> devices = new HashMap<String,String>();
	private String device;

	private boolean promiscuousMode = true;
	private boolean limitPacketBySize = true;
	private int limitPacketSize = 1500;
	private boolean stopByPackets = false;
	private int stopByPacketNumber = 100;
	
	private TNV parent;

	/**
	 * Constructor
	 * @param title
	 * @throws java.awt.HeadlessException
	 */
	private TNVCaptureDialog(TNV p) throws HeadlessException {
		super(p, "Capture Packets");
		this.parent = p;

		JPanel framePanel = new JPanel();
		framePanel.setLayout( new BoxLayout( framePanel, BoxLayout.Y_AXIS ) );
		this.getContentPane().add( framePanel, BorderLayout.CENTER );

		// CAPTURE PANEL
		JPanel capturePanel = new JPanel();
		TitledBorder capturePanelBorder = BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Capture" );
		capturePanel.setBorder( capturePanelBorder );
		capturePanel.setLayout( new BoxLayout( capturePanel, BoxLayout.Y_AXIS ) );

		// Get device list
				
		// for a Mac, workaround to use Java instead of jpcap
		if ( System.getProperty("os.name").toLowerCase().startsWith("mac") ) {
			try {
				Enumeration e = NetworkInterface.getNetworkInterfaces();
				while ( e.hasMoreElements() ) {
					String device = ( (NetworkInterface)e.nextElement() ).getName();
					NetworkInterface nic = NetworkInterface.getByName( device );
					String description = nic.getDisplayName() + ": ";
					Enumeration descEn = nic.getInetAddresses();
					while ( descEn.hasMoreElements() )
						description += ((InetAddress) descEn.nextElement()).getHostAddress() + "  ";
					devices.put(device, description);
				}
			}
			catch ( Exception ex ) {
				TNVErrorDialog.createTNVErrorDialog(this.getClass(), "Error getting devices", ex);
			}
		}
		else {
			try {
				 String[] devs = PacketCapture.lookupDevices();
				 for ( int i = 0 ; i < devs.length ; i++ ) {
					 int lineBreakIndex = devs[i].indexOf("\n");
					 String deviceName = "", deviceDescription = "";
					 if ( lineBreakIndex >= 0 ) {
						 deviceName = devs[i].substring(0,lineBreakIndex);
						 deviceDescription = devs[i].substring(lineBreakIndex+1);
					 }
					 else {
						 deviceName = devs[i];
					 }
					 devices.put(deviceName, deviceDescription);
				 }
			}
			catch (Exception ex) {
				TNVErrorDialog.createTNVErrorDialog(this.getClass(), "Error getting devices", ex);
			}
		}
						
		if ( this.devices.isEmpty() ) {
			JOptionPane.showMessageDialog( TNVCaptureDialog.this, "Could not find any devices" ,
					"Device Error", JOptionPane.WARNING_MESSAGE );
			this.parent.setupCapture( false, "", false, 0, 0 );
			return;
		}

		// Create GUI
		JPanel devicePanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		JLabel devicePopupLabel = new JLabel("Device:      ");
		JComboBox devicePopup = new JComboBox( this.devices.keySet().toArray() );
		devicePopup.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVCaptureDialog.this.device = ( (JComboBox) e.getSource() ).getSelectedItem().toString();
				TNVCaptureDialog.this.deviceDesc.setText(
						TNVCaptureDialog.this.devices.get(TNVCaptureDialog.this.device) );
			}
		} );
		devicePopupLabel.setLabelFor(devicePopup);
		this.device = devicePopup.getSelectedItem().toString();
		devicePanel.add( devicePopupLabel );
		devicePanel.add( devicePopup );
		
		JPanel deviceLabelPanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		this.deviceDesc = new JLabel();
		this.deviceDesc.setText(this.devices.get(this.device));
		deviceLabelPanel.add( this.deviceDesc );

		JPanel promiscPanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		JCheckBox promiscBox = new JCheckBox( "Capture packets in promiscuous mode" );
		promiscBox.setSelected( this.promiscuousMode );
		promiscBox.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.DESELECTED )
					TNVCaptureDialog.this.promiscuousMode = false;
				else
					TNVCaptureDialog.this.promiscuousMode = true;
			}
		} );
		promiscPanel.add( promiscBox );

		JPanel limitPanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		JCheckBox limitBox = new JCheckBox( "Limit each packet to " );
		limitBox.setSelected( this.limitPacketBySize );
		limitBox.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.DESELECTED )
					TNVCaptureDialog.this.limitPacketBySize = false;
				else
					TNVCaptureDialog.this.limitPacketBySize = true;
			}
		} );
		this.limitPacketSizeModel = new SpinnerNumberModel( this.limitPacketSize, 68, 65535, 1 );
		this.limitPacketSizeSpinner = new JSpinner( this.limitPacketSizeModel );
		this.limitPacketSizeSpinner.addChangeListener( new ChangeListener() {
			public void stateChanged( ChangeEvent e ) {
				TNVCaptureDialog.this.limitPacketSize = 
					TNVCaptureDialog.this.limitPacketSizeModel.getNumber().intValue();
			}
		} );
		limitPanel.add( limitBox );
		limitPanel.add( this.limitPacketSizeSpinner );

		capturePanel.add( devicePanel );
		capturePanel.add( deviceLabelPanel );
		capturePanel.add( promiscPanel );
		capturePanel.add( limitPanel );
		framePanel.add( capturePanel );

		// STOP CAPTURE PANEL
		JPanel stopPanel = new JPanel();
		TitledBorder stopPanelBorder = BorderFactory.createTitledBorder( BorderFactory
				.createEtchedBorder( EtchedBorder.RAISED ), "Stop Capture" );
		stopPanel.setBorder( stopPanelBorder );
		stopPanel.setLayout( new BoxLayout( stopPanel, BoxLayout.Y_AXIS ) );

		JPanel stopByPacketsPanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		JCheckBox stopByPacketsBox = new JCheckBox( "Stop after number of packets", this.stopByPackets );
		// TODO: Implement
		stopByPacketsBox.setEnabled(false);
		stopByPacketsBox.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				if ( e.getStateChange() == ItemEvent.DESELECTED )
					TNVCaptureDialog.this.stopByPackets = false;
				else
					TNVCaptureDialog.this.stopByPackets = true;
			}
		} );
		this.stopByPacketsModel = new SpinnerNumberModel( this.stopByPacketNumber, 0, 65535, 1 );
		this.stopByPacketsSpinner = new JSpinner( this.stopByPacketsModel );
		// TODO: Implement
		this.stopByPacketsSpinner.setEnabled(false);
		this.stopByPacketsSpinner.addChangeListener( new ChangeListener() {
			public void stateChanged( ChangeEvent e ) {
				TNVCaptureDialog.this.stopByPacketNumber = 
					TNVCaptureDialog.this.stopByPacketsModel.getNumber().intValue();
			}
		} );
		stopByPacketsPanel.add( stopByPacketsBox );
		stopByPacketsPanel.add( this.stopByPacketsSpinner );

		stopPanel.add( stopByPacketsPanel );
		framePanel.add( stopPanel );

		// START/CANCEL PANEL
		JPanel goPanel = new JPanel();
		goPanel.setLayout( new BoxLayout( goPanel, BoxLayout.LINE_AXIS ) );
		goPanel.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
		this.cancelButton = new JButton( "Cancel" );
		this.cancelButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVCaptureDialog.this.parent.setupCapture( false, "", false, 0, 0 );
				return;
			}
		} );

		this.startButton = new JButton( "Start" );
		this.startButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				if ( TNVCaptureDialog.this.stopByPackets == false ) 
					TNVCaptureDialog.this.stopByPacketNumber = 0;
				TNVCaptureDialog.this.parent.setupCapture( true, TNVCaptureDialog.this.device,
						TNVCaptureDialog.this.promiscuousMode, TNVCaptureDialog.this.limitPacketSize,
						TNVCaptureDialog.this.stopByPacketNumber );
			}
		} );

		goPanel.add( Box.createHorizontalGlue() );
		goPanel.add( this.cancelButton );
		goPanel.add( this.startButton );
		framePanel.add( goPanel );

		this.pack();
		this.getRootPane().setDefaultButton( this.startButton );
		this.setLocationRelativeTo(this.parent);
		this.setVisible( true );

	}


	/**
	 * Factory constructor
	 * @param p
	 * @return dialog
	 * @throws HeadlessException
	 */
	public static TNVCaptureDialog createTNVCaptureDialog(TNV p) throws HeadlessException {
		return new TNVCaptureDialog(p);
	}

}
