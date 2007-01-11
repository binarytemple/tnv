/**
 * Created on May 7, 2006
 * @author jgood
 * 
 * Error dialog class
 */

package net.sourceforge.tnv.dialogs;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * TNVErrorDialog
 */
public class TNVErrorDialog extends JDialog {

	private JTextArea messageArea;
	
	private Exception exception;
	private String callingClassString = "";
	private String messageString = "";
	private String exceptionCodeString = "";
	private String exceptionMessageString = "";
	
	/**
	 * Constructor
	 * @param owner
	 * @throws HeadlessException
	 */
	private TNVErrorDialog(Class caller, String message, Exception ex) throws HeadlessException {
		super();
		
		// print to standard error
		System.err.println("\n\ntnv Exception: " + new Date().toString() + "\n" + message);
		
		messageString = message;
		exception = ex;
		
		if ( caller != null ) {
			callingClassString = "Calling class: " + caller.getName();
			System.err.println(callingClassString);
		}
		
		if ( exception != null) {
			exceptionMessageString = exception.getLocalizedMessage();
			System.err.println(exceptionMessageString);
			if ( exception instanceof SQLException )
				exceptionCodeString = "(SQL Error Code: " + ((SQLException)exception).getErrorCode() + ")";
		}
		
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				initComponents();				
			}
		});
		
	}

	private void initComponents() {
		messageArea = new JTextArea(15, 60);
		messageArea.setEditable(false);
		messageArea.setLineWrap(true);
		messageArea.setWrapStyleWord(true);
		JScrollPane messageScrollPane = new JScrollPane(messageArea);
		
		messageArea.append(
				"tnv Error: " + "\n" +  
				callingClassString + "\n" +
				messageString + "\n" +
				exceptionCodeString + "\n\n" +
				exceptionMessageString
				);
		
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout( FlowLayout.TRAILING ) );
		
		if ( exception != null ) {
			JButton debugButton = new JButton( "Debug" );
			debugButton.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent e ) {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					exception.printStackTrace(pw);
					messageArea.append( "\n" + sw.toString() );
				}
			} );
			buttonPanel.add( debugButton );
		}
		
		JButton continueButton = new JButton( "Continue" );
		continueButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVErrorDialog.this.dispose();
			}
		} );
		buttonPanel.add( continueButton );

		java.net.URL imgURL = TNVErrorDialog.class.getResource("../images/error_icon.png");
		ImageIcon icon = new ImageIcon(imgURL);
		JLabel errorLabel = new JLabel(icon, SwingConstants.CENTER);
		errorLabel.setPreferredSize(new Dimension(80,80));
		
		this.getContentPane().add( errorLabel, java.awt.BorderLayout.WEST );
		this.getContentPane().add( messageScrollPane, java.awt.BorderLayout.CENTER );
		this.getContentPane().add( buttonPanel, java.awt.BorderLayout.SOUTH );
		
		this.getRootPane().setDefaultButton( continueButton );
		this.setTitle("tnv Error  " + exceptionCodeString);
		this.setModal(true);
		this.setResizable(false);
		
		this.pack();

		this.setSize( new Dimension( 600, 300 ) );
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (int) ( (dim.getWidth() / 2) - (this.getWidth() / 2) );
		int y = (int) ( (dim.getHeight() / 2) - (this.getHeight() / 2) );
		this.setLocation(x, y);

		this.setVisible( true );
	}
	
	/**
	 * Factory constructor
	 * @param message
	 */
	public static void createTNVErrorDialog(String message) throws HeadlessException {
		new TNVErrorDialog(null, message, null);
	}

	/**
	 * Factory constructor
	 * @param callingClass
	 * @param message
	 */
	public static void createTNVErrorDialog(Class callingClass, String message) throws HeadlessException {
		new TNVErrorDialog(callingClass, message, null);
	}

	/**
	 * Factory constructor
	 * @param callingClass
	 * @param message
	 * @param ex
	 */
	public static void createTNVErrorDialog(Class callingClass, String message, Exception ex) throws HeadlessException {
		new TNVErrorDialog(callingClass, message, ex);
	}

}
