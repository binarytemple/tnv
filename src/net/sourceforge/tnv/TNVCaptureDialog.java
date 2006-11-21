/**
 * Created on Sep 26, 2004
 * @author jgood
 * 
 * Dialog window for setting up packet capture
 */
package net.sourceforge.tnv;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

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

/**
 * TNVCaptureDialog
 */
public class TNVCaptureDialog extends JDialog {

	private JLabel deviceLabel;
	private SpinnerNumberModel limitPacketSizeModel;
	private JSpinner limitPacketSizeSpinner;
	private SpinnerNumberModel stopByPacketsModel;
	private JSpinner stopByPacketsSpinner;
	private SpinnerNumberModel stopByTimeModel;
	private JButton cancelButton, startButton;

	private Object[] deviceList;
	private Object[] deviceDescriptions;
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
			List<String> jDevsList = new ArrayList<String>();
			List<String> jDescrList = new ArrayList<String>();
			try {
				Enumeration e = NetworkInterface.getNetworkInterfaces();
				while ( e.hasMoreElements() ) {
					String device = ( (NetworkInterface)e.nextElement() ).getName();
					jDevsList.add( device );
					NetworkInterface nic = NetworkInterface.getByName( device );
					String description = nic.getDisplayName() + ": ";
					Enumeration descEn = nic.getInetAddresses();
					while ( descEn.hasMoreElements() ) {
						description += ((InetAddress) descEn.nextElement()).getHostAddress() + "  ";
					}
					jDescrList.add(description);
				}
			}
			catch ( Exception e ) { }
			this.deviceList = jDevsList.toArray();
			this.deviceDescriptions = jDescrList.toArray();
		}
		else {
			try {
				 String[] devs = PacketCapture.lookupDevices();
				 this.deviceList = new Object[devs.length];
				 this.deviceDescriptions = new Object[devs.length];
				 for ( int i = 0 ; i < devs.length ; i++ ) {
					 int lineBreakIndex = devs[i].indexOf('\n'); 
					 String deviceName = devs[i].substring(0,lineBreakIndex);
					 String description = devs[i].substring(lineBreakIndex+1);
					 if ( deviceName != null && deviceName.length() > 0 )
						 this.deviceList[i] = deviceName;
					 else
						 this.deviceList[i] = devs[i];
					 if ( description != null && description.length() > 0 ) 
						 deviceDescriptions[i] = description;
					 else
						 deviceDescriptions[i] = "";
				 }
			}
			catch (Exception ex) { }
		}
						
		if ( this.deviceList == null || this.deviceList.length == 0 ) {
			JOptionPane.showMessageDialog( TNVCaptureDialog.this, "Could not find any devices", "Device Error",
					JOptionPane.WARNING_MESSAGE );
			this.parent.setupCapture( false, "", false, 0, 0 );
			return;
		}

		// Create GUI
		JPanel devicePanel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );
		JComboBox devicePopup = new JComboBox( this.deviceList );
		devicePopup.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVCaptureDialog.this.device = ( (JComboBox) e.getSource() ).getSelectedItem().toString();
				setDeviceLabel( TNVCaptureDialog.this.device );
			}
		} );
		this.device = devicePopup.getSelectedItem().toString();
		this.deviceLabel = new JLabel();
		setDeviceLabel( this.device );
		devicePanel.add( devicePopup );
		devicePanel.add( this.deviceLabel );

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
		this.setLocationRelativeTo( this.parent );
		this.setVisible( true );

	}


	/**
	 * @param dev
	 */
	private void setDeviceLabel(String dev) {
		String descr = "";
		for ( int i = 0 ; i < this.deviceList.length ; i++ ) {
			if ( this.deviceList[i].equals(dev) )
				descr = this.deviceDescriptions[i].toString();
		}
		this.deviceLabel.setText( descr );
	}

	/**
	 * Factory constructor
	 * @param p
	 * @return dialog
	 * @throws HeadlessException
	 */
	protected static TNVCaptureDialog createTNVCaptureDialog(TNV p) throws HeadlessException {
		return new TNVCaptureDialog(p);
	}

}
