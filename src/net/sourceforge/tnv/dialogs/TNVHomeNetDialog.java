/**
 * Created on Apr 27, 2006
 * @author jgood
 *
 * Dialog window for choosing the home network when none is selected
 */
package net.sourceforge.tnv.dialogs;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.tnv.ui.TNVHomeNetPanel;
import net.sourceforge.tnv.ui.TNVPreferenceData;

/**
 * TNVPreferenceDialog
 */
public class TNVHomeNetDialog extends JDialog {

	private String localNet;
	private TNVHomeNetPanel netPanel;
	
	/**
	 * Constructor
	 * @throws HeadlessException
	 */
	private TNVHomeNetDialog() throws HeadlessException {
		super( );
		
		JLabel label1 = new JLabel( "Enter home (local) network address (0-255 in each field) and netmask" );
		JLabel label2 = new JLabel(" (You can change this setting later.) ");
		JPanel labelPanel = new JPanel();
		labelPanel.setLayout( new BoxLayout( labelPanel, BoxLayout.Y_AXIS ) );
		labelPanel.add(label1);
		labelPanel.add(label2);
		
		this.netPanel = new TNVHomeNetPanel();

		// Add settings panel
		JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout( new FlowLayout( FlowLayout.TRAILING ) );
		JButton cancelButton = new JButton( "Exit" );
		cancelButton.setToolTipText("Exit without entering a home network");
		cancelButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				System.out.println("You must choose a home network, exiting...");
				System.exit(1);
			}
		} );
		JButton saveButton = new JButton( "Save" );
		saveButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVPreferenceData.getInstance().setHomeNet( netPanel.getNetworkAddr() );
				TNVPreferenceData.getInstance().saveProperties();
				TNVHomeNetDialog.this.dispose();
			}
		} );
		settingsPanel.add( cancelButton );
		settingsPanel.add( saveButton );
		this.getRootPane().setDefaultButton( saveButton );
		
		this.getContentPane().add( labelPanel, java.awt.BorderLayout.NORTH );
		this.getContentPane().add( this.netPanel, 
				java.awt.BorderLayout.CENTER );
		this.getContentPane().add( settingsPanel, java.awt.BorderLayout.SOUTH );
		
		this.setTitle("Setup your home (local) network");
		this.setModal(true);
		this.setResizable(false);
		this.pack();
		
		this.setSize( new Dimension( 450, 165 ) );
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (int) ( (dim.getWidth() / 2) - (this.getWidth() / 2) );
		int y = (int) ( (dim.getHeight() / 2) - (this.getHeight() / 2) );
		this.setLocation(x, y);
		
		this.setVisible( true );
	}
	
	/**
	 * Factory constructor
	 * @return dialog
	 * @throws HeadlessException
	 */
	public static TNVHomeNetDialog createTNVHomeNetDialog() throws HeadlessException {
		return new TNVHomeNetDialog();
	}

}
