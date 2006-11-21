/**
 * Created on July 19, 2006
 * @author jgood
 * 
 * Dialog window for quick start
 */
package net.sourceforge.tnv;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * TNVHelpDialog
 */
public class TNVQuickStartDialog extends JDialog {

	JEditorPane helpText;


	/**
	 * Private Constructor 
	 */
	private TNVQuickStartDialog() {
		
		JPanel helpPanel = new JPanel();
		helpPanel.setLayout( new BoxLayout( helpPanel, BoxLayout.Y_AXIS ) );
		helpPanel.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );

		this.helpText = new JEditorPane();
		this.helpText.setEditable( false );
		this.helpText.setContentType( "text/html" );

		// For use with linking to anchors
		this.helpText.addHyperlinkListener( new HyperlinkListener() {
			public void hyperlinkUpdate( HyperlinkEvent event ) {
				if ( event.getEventType() == HyperlinkEvent.EventType.ACTIVATED ) {
					try {
						TNVQuickStartDialog.this.helpText.setPage( event.getURL() );
					}
					catch ( IOException ioe ) {
					}
				}
			}
		} );
		java.net.URL helpURL = TNV.class.getResource( "quick.html" );
		if ( helpURL == null ) {
			System.err.println( "Couldn't find file: quick.html" );
			this.dispose();
		}

		try {
			this.helpText.setPage( helpURL );
		}
		catch ( IOException e ) {
			System.err.println( "Attempted to read a bad URL: " + helpURL );
		}

		JScrollPane scrollPane = new JScrollPane( this.helpText );
		helpPanel.add( scrollPane );

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new BoxLayout( buttonPanel, BoxLayout.LINE_AXIS ) );
		buttonPanel.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );

		JButton closeButton = new JButton( "Begin Using TNV" );
		closeButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVQuickStartDialog.this.dispose();
			}
		} );

		buttonPanel.add( Box.createHorizontalGlue() );
		buttonPanel.add( closeButton );
		helpPanel.add( buttonPanel );
		
		this.getContentPane().add( helpPanel, BorderLayout.CENTER );

		this.setTitle("Welcome to TNV");
		this.setModal(true);
		this.setResizable(false);
		this.pack();
		
		this.setSize( new Dimension( 750, 600 ) );
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
	protected static TNVQuickStartDialog createTNVQuickStartDialog() throws HeadlessException {
		return new TNVQuickStartDialog();
	}

}
