/**
 * Created on Apr 25, 2006
 * @author jgood
 * 
 * Status bar for status and selection updates
 */
package net.sourceforge.tnv;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * This class is a status bar panel that shows the current selection and status
 * @author jgood
 * created on: Apr 27, 2006
 *
 */
public class TNVStatusBar extends JPanel
{

    /**************************************************************************
    /*                          Instance Variables                    
    /*************************************************************************/
     
    /**
     * Status operations and string descriptions for long running operations
     */
    protected enum Operations {
        CAPTURE("Capturing packets"),
        IMPORT("Importing tcpdump data"),
        EXPORT("Exporting data to tcpdump file"),
        BUILD_LINKS("Building links");
        
        private String description;
        Operations (String d) {
            description = d;
        }
        String getDescription() {
            return description;
        }
    }

    /**
     * Default label strings
     */
    private static final String SELECT_STRING = "Selected: ";
    private static final String STATUS_STRING = "Status: ";

    /**
     * List of current selection
     */
    private List<String> selectList = new ArrayList<String>();
        
    /**
     *  Current canvas selection
     */
    private JLabel selectLabel;
    private JTextField selectField;

    /**
     *  Current operation status
     */
    private JLabel statusLabel;
    private JTextField statusField;
    private JProgressBar statusProgressBar;


    /**
     *  Singleton instance
     */
    private static TNVStatusBar instance = new TNVStatusBar();


    /**
     * Singleton
     * @return this
     * 
     * @version Apr 25, 2006 (jgood) - created
     * @since 	Version 1.0, Apr 25, 2006
     */
    protected static TNVStatusBar getInstance( ) {
        return instance;
    }


    /**
     * Private constructor
     * @version Apr 25, 2006 (jgood) - created
     * @since 	Version 1.0, Apr 25, 2006
     */
    private TNVStatusBar()
    {
        super();

        this.selectLabel = new JLabel(SELECT_STRING);
        this.selectLabel.setForeground(Color.DARK_GRAY);
        this.selectLabel.setFont( new Font( "SanSerif", Font.PLAIN, 10 ) );
        this.selectLabel.setBackground( this.getBackground() );
        this.selectLabel.setHorizontalAlignment( SwingConstants.LEFT );       

        this.selectField = new JTextField(175);
        this.selectField.setEditable(false);
        this.selectField.setFont( new Font( "SanSerif", Font.PLAIN, 10 ) );
        this.selectField.setBackground( this.getBackground() );
        this.selectField.setHorizontalAlignment( SwingConstants.LEFT );
        this.selectField.setBorder(new EmptyBorder(1, 1, 1, 1));
        
        
        this.statusLabel = new JLabel(STATUS_STRING);
        this.statusLabel.setForeground(Color.DARK_GRAY);
        this.statusLabel.setFont( new Font( "SanSerif", Font.PLAIN, 10 ) );
        this.statusLabel.setBackground( this.getBackground() );
        this.statusLabel.setHorizontalAlignment( SwingConstants.LEFT );       

        this.statusField = new JTextField(100);
        this.statusField.setEditable(false);
        this.statusField.setFont( new Font( "SanSerif", Font.PLAIN, 10 ) );
        this.statusField.setBackground( this.getBackground() );
        this.statusField.setHorizontalAlignment( SwingConstants.LEFT );
        this.statusField.setBorder(new EmptyBorder(1, 1, 1, 1));
        
        this.statusProgressBar = new JProgressBar(SwingConstants.HORIZONTAL);
        this.statusProgressBar.setIndeterminate(true);
        this.statusProgressBar.setMinimumSize(new Dimension(40,10));
        this.statusProgressBar.setMaximumSize(new Dimension(60,10));
        this.statusProgressBar.setPreferredSize(new Dimension(50,10));
        this.statusProgressBar.setVisible(false);
        
        this.setLayout(new BoxLayout( this, BoxLayout.X_AXIS ));
        
        this.add( Box.createHorizontalStrut(10) );
        
        this.add(this.selectLabel);
        this.add(this.selectField);

        this.add( Box.createHorizontalStrut(20) );
        
        this.add(this.statusLabel);
        this.add(this.statusField);
        this.add(this.statusProgressBar);
        
        this.add( Box.createHorizontalStrut(20) );
    }

    /**************************************************************************
    /*                         Protected Methods
    /*************************************************************************/
        
 
    /**
     * Updates the current selection
     * @param selectionText
     * @version Apr 25, 2006 (jgood) - created
     * @since   Version 1.0, Apr 25, 2006
     */
    protected void updateSelection( Set<TNVLocalHostCell> selectedNodes ) {
    	this.selectField.setText( "" );
    	for ( TNVLocalHostCell node : selectedNodes )
    		this.selectField.setText( this.selectField.getText() + " " 
    				+ node.getName() + " (" + node.getFrequency() + ") ");
     }

    
    /**
     * Set the status and runs a progress bar when a job is starting
     * @param operation
     * @param cmd: start or stop
     * @version Apr 25, 2006 (jgood) - created
     * @since   Version 1.0, Apr 25, 2006
     */
    protected void updateStatus(final Operations op) {
    	Thread.yield();
      	SwingUtilities.invokeLater( new Runnable() {
			public void run( ) {
				TNVStatusBar.this.statusField.setText( op.getDescription() + ".");
		    	TNVStatusBar.this.statusProgressBar.setVisible(true);
			}
       	});
   }

    /**
     * 
     * Clear the status
     * @version Apr 25, 2006 (jgood) - created
     * @since   Version 1.0, Apr 25, 2006
     */
    protected void clearStatus() {
       	SwingUtilities.invokeLater( new Runnable() {
			public void run( ) {
				TNVStatusBar.this.statusField.setText( "" );
				TNVStatusBar.this.statusProgressBar.setVisible(false);
			}
       	});
    }

}
