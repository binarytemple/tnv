/**
 * Created onSep 26, 2004
 * @author jgood
 * 
 * Dialog window for help
 */
package net.sourceforge.tnv.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.sourceforge.tnv.TNV;

/**
 * TNVHelpDialog
 */
public class TNVHelpWindow extends JFrame {

	JEditorPane helpText;


	/**
	 * Constructor 
	 */
	public TNVHelpWindow() {
		super( "TNV Help" );
		this.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );

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
						TNVHelpWindow.this.helpText.setPage( event.getURL() );
					}
					catch ( IOException ioe ) {
					}
				}
			}
		} );
		java.net.URL helpURL = TNV.class.getResource( "docs/help.html" );
		if ( helpURL == null ) {
			System.err.println( "Couldn't find file: help.html" );
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

		JButton closeButton = new JButton( "Close Help" );
		closeButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVHelpWindow.this.dispose();
			}
		} );

		buttonPanel.add( Box.createHorizontalGlue() );
		buttonPanel.add( closeButton );
		helpPanel.add( buttonPanel );

		this.getContentPane().add( helpPanel, BorderLayout.CENTER );
		this.pack();
		this.setSize( new Dimension( 800, 600 ) );
		this.setLocationRelativeTo( this.getParent() );
		this.setVisible( true );
	}
}
