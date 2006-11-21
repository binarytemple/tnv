/**
 * Created on May 5, 2006
 * @author jgood
 * 
 * Database utility class
 */
package net.sourceforge.tnv;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * TNVDbChooserWindow
 */
public class TNVDbTypeChooserDialog extends JDialog {
    
	private static final Dimension PREF_TEXT_FIELD_DIM = new Dimension(120, 14);
	private static final int DEFAULT_PADDING = 8;
	private static final Insets DEFAULT_INSETS = new Insets(2, 2, 2, 2);
		
	private JLabel chooseDbLabel;
   
    private ButtonGroup dbButtonGroup;
    private JRadioButton hsqldbButton, mysqlButton;
    
    private JLabel hostLabel;
    private JTextField hostField;
    
    private JLabel portLabel;
    private JTextField portField;
    
    private JLabel nameLabel;
    private JTextField nameField;
    
    private JLabel usernameLabel;
    private JTextField usernameField;
    
    private JLabel passwordLabel;
    private JPasswordField passwordField;
    
    private JButton okButton, cancelButton;
    
    private boolean selectionMade = false;

    /**
     * Creates new form TNVDbChooserWindow
     */
    private TNVDbTypeChooserDialog() throws HeadlessException  {
    	super();

		// override window closing to clean up before quitting
		this.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent evt ) {
				if ( ! selectionMade ) {
					System.out.println("You must choose a database type. Exiting...");
            		System.exit( 1 );
				}
			}
		} );

    	GridBagConstraints gridBagConstraints;

        this.chooseDbLabel = new JLabel("Choose the type of database to use:");
        
        this.dbButtonGroup = new ButtonGroup();
        this.hsqldbButton = new JRadioButton("Embedded");
        this.mysqlButton = new JRadioButton("MySQL DB");
        
        this.hostLabel = new JLabel("Host:");
        this.hostField = new JTextField("localhost", 16);
        this.portLabel = new JLabel("Port:");
        this.portField = new JTextField("3306", 5);
        this.nameLabel = new JLabel("DB name:");
        this.nameField = new JTextField("TNV", 12);
        this.usernameLabel = new JLabel("Username:");
        this.usernameField = new JTextField("root",10);
        this.passwordLabel = new JLabel("Password:");
        this.passwordField = new JPasswordField(10);
        
        this.okButton = new JButton();
        this.cancelButton = new JButton();

        this.getContentPane().setLayout(new GridBagLayout());

        
        // Setup and layout
        this.chooseDbLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = DEFAULT_PADDING;
        gridBagConstraints.ipady = DEFAULT_PADDING;
        gridBagConstraints.insets = new Insets(2, 12, 2, 2);
        this.getContentPane().add(this.chooseDbLabel, gridBagConstraints);

        
        this.dbButtonGroup.add(this.hsqldbButton);
        this.hsqldbButton.setMnemonic('E');
        this.hsqldbButton.setActionCommand("hsqldb");
        this.hsqldbButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            	TNVDbTypeChooserDialog.this.hostField.setEnabled(false);
            	TNVDbTypeChooserDialog.this.portField.setEnabled(false);
            	TNVDbTypeChooserDialog.this.nameField.setEnabled(false);
            	TNVDbTypeChooserDialog.this.usernameField.setEnabled(false);
            	TNVDbTypeChooserDialog.this.passwordField.setEnabled(false);
            }
        });

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = DEFAULT_PADDING;
        gridBagConstraints.ipady = DEFAULT_PADDING;
        gridBagConstraints.insets = new Insets(2, 12, 2, 2);
        this.getContentPane().add(this.hsqldbButton, gridBagConstraints);

        
        this.dbButtonGroup.add(this.mysqlButton);
        this.mysqlButton.setMnemonic('M');
        this.mysqlButton.setActionCommand("mysql");
        this.mysqlButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            	TNVDbTypeChooserDialog.this.hostField.setEnabled(true);
            	TNVDbTypeChooserDialog.this.portField.setEnabled(true);
            	TNVDbTypeChooserDialog.this.nameField.setEnabled(true);
            	TNVDbTypeChooserDialog.this.usernameField.setEnabled(true);
            	TNVDbTypeChooserDialog.this.passwordField.setEnabled(true);
            	TNVDbTypeChooserDialog.this.passwordField.requestFocus();
            }
        });
        
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.ipadx = DEFAULT_PADDING;
        gridBagConstraints.ipady = DEFAULT_PADDING;
        gridBagConstraints.insets = new Insets(2, 2, 2, 12);
        this.getContentPane().add(this.mysqlButton, gridBagConstraints);

        
        // HOST FIELD
        this.hostLabel.setLabelFor(this.hostField);
        
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.ipadx = DEFAULT_PADDING;
        gridBagConstraints.ipady = DEFAULT_PADDING;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = DEFAULT_INSETS;
        this.getContentPane().add(this.hostLabel, gridBagConstraints);

        this.hostField.setPreferredSize(PREF_TEXT_FIELD_DIM);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.ipadx = DEFAULT_PADDING;
        gridBagConstraints.ipady = DEFAULT_PADDING;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = DEFAULT_INSETS;
        this.getContentPane().add(this.hostField, gridBagConstraints);

        // PORT FIELD
        this.portLabel.setLabelFor(this.portField);
        
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.ipadx = DEFAULT_PADDING;
        gridBagConstraints.ipady = DEFAULT_PADDING;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = DEFAULT_INSETS;
        this.getContentPane().add(this.portLabel, gridBagConstraints);

        this.portField.setPreferredSize(new Dimension(50, 14));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.ipadx = DEFAULT_PADDING;
        gridBagConstraints.ipady = DEFAULT_PADDING;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = DEFAULT_INSETS;
        this.getContentPane().add(this.portField, gridBagConstraints);

        // DB NAME FIELD
        this.nameLabel.setLabelFor(this.nameField);
        
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.ipadx = DEFAULT_PADDING;
        gridBagConstraints.ipady = DEFAULT_PADDING;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = DEFAULT_INSETS;
        this.getContentPane().add(this.nameLabel, gridBagConstraints);

        this.nameField.setPreferredSize(PREF_TEXT_FIELD_DIM);
        
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.ipady = DEFAULT_PADDING;
        gridBagConstraints.ipady = DEFAULT_PADDING;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = DEFAULT_INSETS;
        this.getContentPane().add(this.nameField, gridBagConstraints);
        
        // USERNAME FIELD
        this.usernameLabel.setLabelFor(this.usernameField);
        
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.ipadx = DEFAULT_PADDING;
        gridBagConstraints.ipady = DEFAULT_PADDING;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = DEFAULT_INSETS;
        this.getContentPane().add(this.usernameLabel, gridBagConstraints);

        this.usernameField.setPreferredSize(PREF_TEXT_FIELD_DIM);
        
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.ipadx = DEFAULT_PADDING;
        gridBagConstraints.ipady = DEFAULT_PADDING;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = DEFAULT_INSETS;
        this.getContentPane().add(this.usernameField, gridBagConstraints);

        // PASSWORD FIELD
        this.passwordLabel.setLabelFor(this.passwordField);
        
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.ipadx = DEFAULT_PADDING;
        gridBagConstraints.ipady = DEFAULT_PADDING;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = DEFAULT_INSETS;
        this.getContentPane().add(this.passwordLabel, gridBagConstraints);

        this.passwordField.setPreferredSize(PREF_TEXT_FIELD_DIM);
        
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.ipadx = DEFAULT_PADDING;
        gridBagConstraints.ipady = DEFAULT_PADDING;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = DEFAULT_INSETS;
        this.getContentPane().add(this.passwordField, gridBagConstraints);

        
        
        // BUTTONS
        this.cancelButton.setText("Cancel");
        this.cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            	System.out.println("You must choose a database type. Exiting...");
            	System.exit( 1 );
            }
        });

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = DEFAULT_INSETS;
        this.getContentPane().add(this.cancelButton, gridBagConstraints);


        this.okButton.setText("OK");
        this.okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                String choice = TNVDbTypeChooserDialog.this.dbButtonGroup.getSelection().getActionCommand();
    			// create the instance but do not open a connection
                if ( choice.equals( "hsqldb" ) )
    				TNVDbUtil.createDBinstance(TNVDbUtil.DB_TYPE.HSQLDB);
    			else {
    				String host = TNVDbTypeChooserDialog.this.hostField.getText();
    				String port = TNVDbTypeChooserDialog.this.portField.getText();
    				String name = TNVDbTypeChooserDialog.this.nameField.getText();
    				String user = TNVDbTypeChooserDialog.this.usernameField.getText();
    				char[] passArray = TNVDbTypeChooserDialog.this.passwordField.getPassword();
    				String pass = "";
    				for (int i = 0; i < passArray.length; i++)
    					pass += passArray[i];
    				try {
    					TNVDbUtil.createDBinstance(TNVDbUtil.DB_TYPE.MYSQL).openConnection( 
    						host + ":" + port + "/" + name, user, pass );
    				}
    				catch (SQLException e) {
    					if ( e.getErrorCode() == 1045 ) {
    						System.out.println("Unable to open mysql db. Error " + e.getErrorCode() +
    								"\n" + e.getMessage());
    						System.exit( 1 );
    					}
    					else 	
    						System.out.println("Unable to open mysql db. Error " + e.getErrorCode());
    					System.out.println(e.getMessage());
    					e.printStackTrace();
    				}
    			}
                selectionMade = true;
    			TNVDbTypeChooserDialog.this.dispose();
            }
        });

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = DEFAULT_INSETS;
        this.getContentPane().add(this.okButton, gridBagConstraints);

        
        // Defaults
        this.hsqldbButton.setSelected(true);   
       	this.hostField.setEnabled(false);
    	this.portField.setEnabled(false);
    	this.nameField.setEnabled(false);
    	this.usernameField.setEnabled(false);
    	this.passwordField.setEnabled(false);
        
    	this.getRootPane().setDefaultButton( this.okButton );
    	
        this.setTitle("Open Database Connection");
        this.setResizable(false);
		this.setModal(true);
        //this.setUndecorated(true);
        this.pack();

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (int) ( (dim.getWidth() / 2) - (this.getWidth() / 2) );
		int y = (int) ( (dim.getHeight() / 2) - (this.getHeight() / 2) );
		this.setLocation(x, y);
		
        this.setVisible(true);
        
    }

	/**
	 * Factory constructor
	 * @return dialog
	 * @throws HeadlessException
	 */
    protected static TNVDbTypeChooserDialog createTNVDbTypeChooserDialog() throws HeadlessException {
		return new TNVDbTypeChooserDialog();
	}

}
