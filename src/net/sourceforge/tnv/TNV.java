/**
 * Created on Mar 9, 2004
 * @author jgood
 * 
 * This is the main class of the package responsible for menu events and delegating UI
 */

package net.sourceforge.tnv;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import net.sourceforge.jpcap.capture.PacketCapture;
import net.sourceforge.jpcap.util.TcpdumpWriter;
import net.sourceforge.tnv.db.TNVDbUtil;
import net.sourceforge.tnv.dialogs.TNVCaptureDialog;
import net.sourceforge.tnv.dialogs.TNVDbTypeChooserDialog;
import net.sourceforge.tnv.dialogs.TNVErrorDialog;
import net.sourceforge.tnv.dialogs.TNVHomeNetDialog;
import net.sourceforge.tnv.dialogs.TNVPreferenceDialog;
import net.sourceforge.tnv.dialogs.TNVQuickStartDialog;
import net.sourceforge.tnv.dialogs.TNVSplashWindow;
import net.sourceforge.tnv.dialogs.TNVStopCaptureDialog;
import net.sourceforge.tnv.dialogs.TNVTimeChooserDialog;
import net.sourceforge.tnv.ui.TNVDetailWindow;
import net.sourceforge.tnv.ui.TNVHelpWindow;
import net.sourceforge.tnv.ui.TNVModel;
import net.sourceforge.tnv.ui.TNVPacketHandler;
import net.sourceforge.tnv.ui.TNVPreferenceData;
import net.sourceforge.tnv.ui.TNVStatusBar;
import net.sourceforge.tnv.ui.TNVUIManager;
import net.sourceforge.tnv.util.TNVTcpdumpFileFilter;
import net.sourceforge.tnv.util.TNVUtil;

/**
 * TNV
 */
public class TNV extends JFrame {

	private static final String BUILD_DATE = "December 11, 2007";
	private static final String VERSION = "0.3.8";
	
	private static final String ABOUT_TEXT = 
		  "tnv:  http://tnv.sourceforge.net/\n"
		+ "    Version:  " + VERSION + "\n"
		+ "    Build date:  " + BUILD_DATE + "\n"
		+ " \n"
		+ "(c) 2006-2007, John Goodall. Some rights reserved.\n"
		+ " \n"
		+ "Released under the MIT License\n"
		+ "    http://www.opensource.org/licenses/mit-license.php";
	private static final java.net.URL ABOUT_IMG_URL = TNV.class.getResource( "images/tnv_thumb.gif" );

	private static final Cursor defaultCursor = new Cursor( Cursor.DEFAULT_CURSOR );
	private static final Cursor crosshairCursor = new Cursor( Cursor.CROSSHAIR_CURSOR );
	private static final Cursor waitCursor = new Cursor( Cursor.WAIT_CURSOR );
	private static final Cursor handCursor = new Cursor( Cursor.HAND_CURSOR );

	private static final int MIN_WIDTH = 800;
	private static final int MIN_HEIGHT = 600;

	private static final int INFINITE = -1;

	// BPF filter for only capturing IP packets
	private static final String FILTER = "ip";

	// path to database
	public static final String DEFAULT_EMBEDDED_DB_DIR = "db";
	public static final String DEFAULT_EMBEDDED_DB_FILE = "tnvdb";

	// This and other windows required
	private static TNV thisFrame;

	// tcpdump file chooser filter
	private static TNVTcpdumpFileFilter TCPD_FILE_FILTER = new TNVTcpdumpFileFilter();

	// UI Components
	private JMenuItem openMenuItem, saveMenuItem, closeMenuItem, removeDataMenuItem, 
			importMenuItem, exportMenuItem, quitMenuItem,
			prefsMenuItem, resetMenuItem, detailsMenuItem, portsMenuItem,
			startMenuItem;

	private TNVUIManager vui;

	// Jpcap packet capture object
	private PacketCapture m_pcap;

	// For synchronized capture
	private Thread captureThread;

	// Capture variables
	private int currentSnaplen;

	// TODO: use linkLayerType for raw packet decoding
	private int linkLayerType;

	private TNVCaptureDialog captureDialog; // capture dialog
	private JProgressBar captureProgress;

	// file chooser - only create once for opening files and imports
	private JFileChooser fileChooser = new JFileChooser();


	/** Constructor */
	public TNV() {
		super( "TNV" );
		thisFrame = this;
		
		SwingUtilities.invokeLater( new Runnable() {
			public void run( ) {
				initComponents();
			}
		} );

	}


	/** STATIC * */
	public static final void setDefaultCursor( ) {
		thisFrame.setCursor( defaultCursor );
	}

	public static final void setCrosshairCursor( ) {
		thisFrame.setCursor( crosshairCursor );
	}

	public static final void setWaitCursor( ) {
		thisFrame.setCursor( waitCursor );
	}

	public static final void setHandCursor( ) {
		thisFrame.setCursor( handCursor );
	}


	/**
	 * Initialize the GUI components
	 */
	private void initComponents( ) {

		// splash screen first
		TNVSplashWindow.createTNVSplashWindow(ABOUT_TEXT, ABOUT_IMG_URL, this, 2250);
		
		// override window closing to clean up before quitting
		this.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent evt ) {
				quit( evt );
			}
		} );
		
		// Minimum window resizing
		this.addComponentListener( new ComponentAdapter() {
			@Override
			public void componentResized( ComponentEvent e ) {
				int width = getWidth();
				int height = getHeight();
				boolean resize = false;

				if (width < MIN_WIDTH) {
					resize = true;
					width = MIN_WIDTH;
				}
				if (height < MIN_HEIGHT) {
					resize = true;
					height = MIN_HEIGHT;
				}
				if (resize)
					setSize(width, height);
			}
		} );

		// check if the home network is defined already in the preferences
		// or open up a dialog box prompting for it
		String homeNet = TNVPreferenceData.getInstance().getHomeNet();
		if ( homeNet == null || homeNet.equalsIgnoreCase("") ) {
			// if home net is not defined, first open up quickstart window
			TNVQuickStartDialog.createTNVQuickStartDialog();
			
			TNVHomeNetDialog.createTNVHomeNetDialog();
		}
			
		// Add main UI to panel
		this.vui = new TNVUIManager();

		// choose a type of DB connection before showing frame
		TNVDbTypeChooserDialog.createTNVDbTypeChooserDialog();
		
		TNV.setWaitCursor();
		
		// create all menus
		this.createMenus();

		getContentPane().add( this.vui, BorderLayout.CENTER );
		this.pack();

		// Get the size of the current screen
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		int w = (int) ( dim.getWidth() - 50 );
		int h = (int) ( dim.getHeight() - 50 );
		this.setBounds( 25, 0, w, h );
		
		TNV.setDefaultCursor();
		
		this.setVisible( true );
		
	}	


	/**
	 * Set up menus
	 */
	private void createMenus( ) {

		JMenuBar mainMenuBar = new JMenuBar();

		// File Menu
		JMenu fileMenu = new JMenu( "File" );
		fileMenu.setMnemonic('F');
		
		// Open menu, set title according to type of DB
		this.openMenuItem = new JMenuItem(  );
		
		if ( TNVDbUtil.getType().equals(TNVDbUtil.DB_TYPE.HSQLDB) )
			this.openMenuItem.setText("Open saved TNV database...");
		else
			this.openMenuItem.setText("Open MySql data...");
		this.openMenuItem.setAccelerator( KeyStroke.getKeyStroke( 'O', Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMask() ) );
		this.openMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				if ( !close( e ) ) 
					return;
				TNV.this.saveMenuItem.setEnabled( true );
				TNV.this.exportMenuItem.setEnabled( true );
				TNV.this.closeMenuItem.setEnabled( true );
				TNV.this.detailsMenuItem.setEnabled( true );
				TNV.this.portsMenuItem.setEnabled( true );
				TNV.this.resetMenuItem.setEnabled( true );
				if ( TNVDbUtil.getType().equals(TNVDbUtil.DB_TYPE.HSQLDB) )
					openTNVDB();
				else
					openMySqlDB();
			}
		} );
		
		this.saveMenuItem = new JMenuItem( "Save TNV data..." );
		// No need to save files if using a database connection
		if ( TNVDbUtil.getType().equals(TNVDbUtil.DB_TYPE.MYSQL) )
			this.saveMenuItem.setVisible(false);
		this.saveMenuItem.setAccelerator( KeyStroke.getKeyStroke( 'S', Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMask() ) );
		this.saveMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				saveTNVDB();
			}
		} );
		
		this.closeMenuItem = new JMenuItem( "Close" );
		this.closeMenuItem.setAccelerator( KeyStroke.getKeyStroke( 'W', Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMask() ) );
		this.closeMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				if ( !close( e ) ) 
					return;
				TNV.this.saveMenuItem.setEnabled( false );
				TNV.this.exportMenuItem.setEnabled( false );
				TNV.this.closeMenuItem.setEnabled( false );
				TNV.this.detailsMenuItem.setEnabled( false );
				TNV.this.portsMenuItem.setEnabled( false );
				TNV.this.resetMenuItem.setEnabled( false );
			}
		} );

		if ( ! TNVDbUtil.getType().equals(TNVDbUtil.DB_TYPE.HSQLDB) ) {
			removeDataMenuItem = new JMenuItem("Remove data from DB");
			removeDataMenuItem.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent e ) {
					int ret = JOptionPane.showConfirmDialog( TNV.this, 
							"Are you sure you want to remove all data from the database? ",
							"Drop All Data from Database", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE );
					if ( ret == JOptionPane.NO_OPTION ) 
						return;
					TNVUIManager.clearUI();
					TNV.this.setTitle( "TNV" );
					TNV.this.saveMenuItem.setEnabled( false );
					TNV.this.exportMenuItem.setEnabled( false );
					TNV.this.closeMenuItem.setEnabled( false );
					TNV.this.detailsMenuItem.setEnabled( false );
					TNV.this.portsMenuItem.setEnabled( false );
					TNV.this.resetMenuItem.setEnabled( false );
					try {
						TNVDbUtil.getInstance().removeData();
					}
					catch (SQLException ex) {
						TNVErrorDialog.createTNVErrorDialog(this.getClass(), "Error destroying database", ex);
					}
				}
			} );
		}

		this.importMenuItem = new JMenuItem( "Import pcap file..." );
		this.importMenuItem.setAccelerator( KeyStroke.getKeyStroke( 'I', Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMask() ) );
		this.importMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				if ( !close( e ) ) 
					return;
				if ( importTcpdumpFile() ) {
					TNV.this.saveMenuItem.setEnabled( true );
					TNV.this.exportMenuItem.setEnabled( true );
					TNV.this.closeMenuItem.setEnabled( true );
					TNV.this.detailsMenuItem.setEnabled( true );
					TNV.this.portsMenuItem.setEnabled( true );
					TNV.this.resetMenuItem.setEnabled( true );
				}
			}
		} );

		this.exportMenuItem = new JMenuItem( "Export pcap file..." );
		this.exportMenuItem.setAccelerator( KeyStroke.getKeyStroke( 'E', Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMask() ) );
		this.exportMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				exportTcpdumpFile();
			}
		} );

		this.quitMenuItem = new JMenuItem( "Quit" );
		this.quitMenuItem.setAccelerator( KeyStroke.getKeyStroke( 'Q', Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMask() ) );
		this.quitMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				quit( e );
			}
		} );

		fileMenu.add( this.openMenuItem );
		fileMenu.add( this.saveMenuItem );
		fileMenu.add( this.closeMenuItem );
		if ( ! TNVDbUtil.getType().equals(TNVDbUtil.DB_TYPE.HSQLDB) )
			fileMenu.add( this.removeDataMenuItem );
		fileMenu.add( new JSeparator() );
		fileMenu.add( this.importMenuItem );
		fileMenu.add( this.exportMenuItem );
		fileMenu.add( new JSeparator() );
		fileMenu.add( this.quitMenuItem );

		// View menu
		JMenu viewMenu = new JMenu( "View" );
		viewMenu.setMnemonic('V');
		
		this.detailsMenuItem = new JMenuItem( "View All Packet Details" );
		this.detailsMenuItem.setAccelerator( KeyStroke.getKeyStroke( 'D', Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMask() | java.awt.event.InputEvent.SHIFT_MASK  ) );
		this.detailsMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVDetailWindow.createTNVDetailWindow( );
			}
		} );

		this.portsMenuItem = new JMenuItem( "View All Port Activity" );
		this.portsMenuItem.setAccelerator( KeyStroke.getKeyStroke( 'P', Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMask() | java.awt.event.InputEvent.SHIFT_MASK  ) );
		this.portsMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVModel.getInstance().showAllPorts();
			}
		} );

		this.resetMenuItem = new JMenuItem( "Reset Display" );
		this.resetMenuItem.setAccelerator( KeyStroke.getKeyStroke( 'R', Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMask() | java.awt.event.InputEvent.SHIFT_MASK  ) );
		this.resetMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVUIManager.resetUI();
			}
		} );

		this.prefsMenuItem = new JMenuItem( "Color Preferences..." );
		this.prefsMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVPreferenceDialog.createTNVPreferenceDialog();
			}
		} );

		viewMenu.add( this.detailsMenuItem );
		viewMenu.add( this.portsMenuItem );
		viewMenu.add( this.resetMenuItem );
		viewMenu.addSeparator();
		viewMenu.add( this.prefsMenuItem );
		
		// Capture menu
		JMenu captureMenu = new JMenu( "Capture" );
		captureMenu.setMnemonic('C');
		
		this.startMenuItem = new JMenuItem( "Capture Packets..." );
		this.startMenuItem.setAccelerator( KeyStroke.getKeyStroke( 'C', Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMask() | java.awt.event.InputEvent.SHIFT_MASK  ) );
		this.startMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				if ( !close( e ) ) 
					return;
				TNV.this.startMenuItem.setEnabled( false );
				TNV.this.detailsMenuItem.setEnabled( true );
				TNV.this.portsMenuItem.setEnabled( true );
				TNV.this.resetMenuItem.setEnabled( true );
				TNV.this.exportMenuItem.setEnabled( true );
				TNV.this.saveMenuItem.setEnabled( true );
				startPacketCapture();
			}
		} );

		captureMenu.add( this.startMenuItem );

		// Help menu
		JMenu helpMenu = new JMenu( "Help" );
		helpMenu.setMnemonic('H');
		
		JMenuItem quickStartMenuItem = new JMenuItem( "Quick Start" );
		quickStartMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVQuickStartDialog.createTNVQuickStartDialog();
			}
		} );

		JMenuItem helpMenuItem = new JMenuItem( "Help" );
		helpMenuItem.setAccelerator( KeyStroke.getKeyStroke( 'H', Toolkit.getDefaultToolkit()
				.getMenuShortcutKeyMask() | java.awt.event.InputEvent.SHIFT_MASK  ) );
		helpMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				new TNVHelpWindow();
			}
		} );

		JMenuItem forumMenuItem = new JMenuItem( "Help Forums" );
		forumMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				net.sourceforge.tnv.util.BareBonesBrowserLaunch.openURL(
						"http://sourceforge.net/forum/?group_id=182807");
			}
		} );

		JMenuItem bugMenuItem = new JMenuItem( "Report Bug" );
		bugMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				net.sourceforge.tnv.util.BareBonesBrowserLaunch.openURL(
						"http://sourceforge.net/tracker/?func=add&group_id=182807&atid=902696");
			}
		} );

		JMenuItem featureMenuItem = new JMenuItem( "Request Feature" );
		featureMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				net.sourceforge.tnv.util.BareBonesBrowserLaunch.openURL(
						"http://sourceforge.net/tracker/?func=add&group_id=182807&atid=902699");
			}
		} );

		JMenuItem projectMenuItem = new JMenuItem( "Project Home Page" );
		projectMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				net.sourceforge.tnv.util.BareBonesBrowserLaunch.openURL(
						"http://sourceforge.net/projects/tnv/");
			}
		} );

		JMenuItem sfMenuItem = new JMenuItem( "SourceForge Home Page" );
		sfMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				net.sourceforge.tnv.util.BareBonesBrowserLaunch.openURL(
						"http://sourceforge.net/");
			}
		} );

		JMenu webLinksSubMenu = new JMenu("Web Links");
		webLinksSubMenu.add( forumMenuItem );
		webLinksSubMenu.add( bugMenuItem );
		webLinksSubMenu.add( featureMenuItem );
		webLinksSubMenu.add( projectMenuItem );
		webLinksSubMenu.addSeparator();
		webLinksSubMenu.add( sfMenuItem );
		
		JMenuItem aboutMenuItem = new JMenuItem( "About" );
		aboutMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				if ( ABOUT_IMG_URL != null ) {
					ImageIcon tnvIcon = new ImageIcon( ABOUT_IMG_URL );
					JOptionPane.showMessageDialog( TNV.this, ABOUT_TEXT, "About", JOptionPane.PLAIN_MESSAGE, tnvIcon );
				}
				else
					JOptionPane.showMessageDialog( TNV.this, ABOUT_TEXT, "About", JOptionPane.PLAIN_MESSAGE );
			}
		} );

		
		helpMenu.add( quickStartMenuItem );
		helpMenu.add( helpMenuItem );
		helpMenu.add( new JSeparator() );
		helpMenu.add( webLinksSubMenu );
		helpMenu.add( new JSeparator() );
		helpMenu.add( aboutMenuItem );

		// Disable certain menus by default
		this.saveMenuItem.setEnabled( false );
		this.exportMenuItem.setEnabled( false );
		this.closeMenuItem.setEnabled( false );

		this.detailsMenuItem.setEnabled( false );
		this.portsMenuItem.setEnabled( false );
		this.resetMenuItem.setEnabled( false );

		mainMenuBar.add( fileMenu );
		mainMenuBar.add( viewMenu );
		mainMenuBar.add( captureMenu );
		mainMenuBar.add(Box.createHorizontalGlue()); // right align help menu
		mainMenuBar.add( helpMenu );

		setJMenuBar( mainMenuBar );
	}


	/** 
	 * Exit the Application 
	 * @param e
	 */
	private void quit( AWTEvent e ) {
		if ( !close( e ) ) 
			return;
		System.exit( 0 );
	}


	/** 
	 * Close the current data set
	 * @param e
	 * @return success
	 */
	private boolean close( AWTEvent e ) {
		TNVUIManager.clearUI();
		this.setTitle( "TNV" );
		try {
			TNVDbUtil.getInstance().closeConnection();
		}
		catch ( SQLException ex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL error removing tables", ex);
			return false;
		}
		if ( ! destroyDB() ) 
			return false;
		return true;
	}


	/** 
	 * Open a database connection
	 * @param path
	 * @return success
	 */
	private boolean openDBConnection( String path ) {
		try {
			TNVDbUtil.getInstance().openConnection( path, "sa", "" );
		}
		catch ( SQLException ex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL error opening connection" , ex );
			return false;
		}
		return true;
	}
	

	/** 
	 * Check for and optionally remove db
	 * @return success
	 */
	private boolean destroyDB( ) {
		if ( TNVDbUtil.getType().equals(TNVDbUtil.DB_TYPE.HSQLDB) ) {
			File dbDirectory = new File( DEFAULT_EMBEDDED_DB_DIR );
			if ( ! dbDirectory.exists() ) 
				return true;
			File dbDataFile = new File( dbDirectory + File.separator + DEFAULT_EMBEDDED_DB_FILE + ".data" );
			
			// if data file is too small, just delete it without asking
			if ( ( ! dbDataFile.exists() || dbDataFile.length() < 1000000 ) && dbDirectory.isDirectory() ) {
				String[] children = dbDirectory.list();
				for ( String element : children )
					( new File( dbDirectory, element ) ).delete();
				dbDirectory.delete();
				return true;
			}
			
			int ret = JOptionPane.showConfirmDialog( TNV.this, 
					"Are you sure you want to remove your working data? " +
					"\n\n(Choose File->Save to save working data to another directory)\n ",
					"Close", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE );
			if ( ret == JOptionPane.NO_OPTION ) 
				return false;
			if ( dbDirectory.exists() && dbDirectory.isDirectory() ) {
				String[] children = dbDirectory.list();
				for ( String element : children )
					( new File( dbDirectory, element ) ).delete();
				dbDirectory.delete();
			}
		}
		else {
			// TODO: remove tables?
		}
		return true;
	}


	/** 
	 * Open a TNV database
	 * @return success
	 */
	private boolean openTNVDB( ) {
		fileChooser.setDialogTitle( "Choose a saved TNV database directory to open" );
		fileChooser.removeChoosableFileFilter( TCPD_FILE_FILTER );
		fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
		fileChooser.setMultiSelectionEnabled( false );
		int returnVal = fileChooser.showDialog( this, "Open Directory" );
		if ( returnVal != JFileChooser.APPROVE_OPTION )
			return false;
		openDBConnection( fileChooser.getSelectedFile().getPath() + File.separator + DEFAULT_EMBEDDED_DB_FILE );
		TNVDbUtil.getInstance().setupHosts();
		setupTNV();
		return true;
	}

	/** 
	 * Open MySql data through an already open connection
	 * @return success
	 */
	private boolean openMySqlDB( ) {
		if (TNVTimeChooserDialog.createTNVTimeChooserDialog() == null)
			return false;
		TNVDbUtil.getInstance().setupHosts();
		setupTNV();
		return true;
	}

	/**  
	 * Save a TNV database
	 * @return success
	 */
	private boolean saveTNVDB( ) {
		fileChooser.setDialogTitle( "Choose a directory to save TNV data" );
		fileChooser.removeChoosableFileFilter( TCPD_FILE_FILTER );
		fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
		fileChooser.setMultiSelectionEnabled( false );
		int returnVal = fileChooser.showSaveDialog( this );
		if ( returnVal != JFileChooser.APPROVE_OPTION )
			return false;

		File saveDir = fileChooser.getSelectedFile();

		File dbDirectory = new File( DEFAULT_EMBEDDED_DB_DIR );
		if ( ! dbDirectory.renameTo( saveDir ) ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "Unable to save " + dbDirectory + " to " + saveDir );
			return false;
		}
		
		JOptionPane.showMessageDialog( TNV.this, "Saved " + saveDir.getName(), "Save Complete",
				JOptionPane.PLAIN_MESSAGE );
		return true;
	}


	/** 
	 * Export data to lipcap file
	 * @return success
	 */
	private boolean exportTcpdumpFile( ) {
		fileChooser.setDialogTitle( "Choose a file to export pcap (tcpdump) data" );
		fileChooser.removeChoosableFileFilter( TCPD_FILE_FILTER );
		fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
		fileChooser.setMultiSelectionEnabled( false );
		int returnVal = fileChooser.showSaveDialog( this );
		if ( returnVal != JFileChooser.APPROVE_OPTION )
			return false;
		String filePath = fileChooser.getSelectedFile().getPath();

		TNVStatusBar.getInstance().updateStatus( TNVStatusBar.Operations.EXPORT );
		// write header information
		int endian = TcpdumpWriter.LITTLE_ENDIAN;
		try {
			TcpdumpWriter.writeHeader( filePath, endian, this.currentSnaplen );
		}
		catch ( IOException ex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "Unable to write tcpdump header", ex );
			return false;
		}

		// write files in current database
		boolean success = TNVDbUtil.getInstance().writeRawPackets( filePath, endian );
		if ( success )
			JOptionPane.showMessageDialog( TNV.this, "Saved " + filePath, "Export complete", JOptionPane.PLAIN_MESSAGE );

		TNVStatusBar.getInstance().clearStatus();
		
		return success;
	}


	/** 
	 * Open a lipcap data file
	 * @return success
	 */
	private boolean importTcpdumpFile( ) {
		fileChooser.setDialogTitle( "Choose a pcap (tcpdump or ethereal) file to open" );
		if ( fileChooser.getChoosableFileFilters().length < 2 )
			fileChooser.addChoosableFileFilter( TCPD_FILE_FILTER );
		fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
		fileChooser.setMultiSelectionEnabled( false );
		int returnVal = fileChooser.showOpenDialog( this );
		if ( returnVal != JFileChooser.APPROVE_OPTION )
			return false;
		String filePath = fileChooser.getSelectedFile().getPath();
		this.m_pcap = new PacketCapture();
		Cursor prevCursor = this.getCursor();
		this.setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
		openDBConnection( DEFAULT_EMBEDDED_DB_DIR + File.separator + DEFAULT_EMBEDDED_DB_FILE );
		TNVStatusBar.getInstance().updateStatus( TNVStatusBar.Operations.IMPORT );
		try {
			this.m_pcap.openOffline( filePath );
			this.linkLayerType = this.m_pcap.getLinkLayerType();
			this.currentSnaplen = this.m_pcap.getSnapshotLength(); // for later
			// exporting
			this.m_pcap.setFilter( FILTER, true );
			this.m_pcap.addRawPacketListener( new TNVPacketHandler() );
			this.m_pcap.capture( INFINITE );
		}
		catch ( Exception e ) {
			JOptionPane.showMessageDialog( TNV.this, "Error opening file " + filePath + ": \n" + e.getMessage(),
					"File Open Error", JOptionPane.ERROR_MESSAGE );
			return false;
		}
		this.setCursor( prevCursor );
		TNVStatusBar.getInstance().clearStatus();
//		JOptionPane.showMessageDialog( TNV.this, "Imported " + filePath, "Import Complete", JOptionPane.PLAIN_MESSAGE );
		setupTNV();
		return true;
	}


	/** 
	 * Start packet capture
	 */
	private void startPacketCapture( ) {
		openDBConnection( DEFAULT_EMBEDDED_DB_DIR + File.separator + DEFAULT_EMBEDDED_DB_FILE );
		try {
			this.m_pcap = new PacketCapture();
		}
		catch ( Exception ex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "Unable to start packet capture", ex);
		}
		this.captureDialog = TNVCaptureDialog.createTNVCaptureDialog(this);
	}


	/** 
	 * Stop packet capture and set up display
	 */
	private void stopPacketCapture( ) {
		this.startMenuItem.setEnabled( true );
		if ( postCaptureSetup() ) {
			this.saveMenuItem.setEnabled( true );
			this.exportMenuItem.setEnabled( true );
			this.closeMenuItem.setEnabled( true );
			this.detailsMenuItem.setEnabled( true );
			this.portsMenuItem.setEnabled( true );
			this.resetMenuItem.setEnabled( true );
			setupTNV();
		}
		else {
			this.saveMenuItem.setEnabled( false );
			this.exportMenuItem.setEnabled( false );
		}
	}


	/**
	 *  Set up the UI components after opening file or finishing capture
	 */
	private void setupTNV( ) {		
		TNV.setWaitCursor();

		TNVUIManager.setupUI();
		
		String title = "TNV: " + TNVDbUtil.getInstance().getTotalPacketCount() + " packets: "
				+ TNVUtil.NORMAL_FORMAT.format( TNVDbUtil.getInstance().getMinTime() ) + " -- "
				+ TNVUtil.NORMAL_FORMAT.format( TNVDbUtil.getInstance().getMaxTime() );
		this.setTitle( title );

		TNV.setDefaultCursor();
	}


	/** 
	 * Process libcap data 
	 * @return success
	 */
	private boolean postCaptureSetup( ) {
		TNVStatusBar.getInstance().clearStatus();
		
		if ( TNVDbUtil.getInstance().getTotalPacketCount() == 0 ) {
			JOptionPane.showMessageDialog( TNV.this, "No IP packets were captured", "No packets captured",
					JOptionPane.ERROR_MESSAGE );
			return false;
		}

		return true;
	}


	/**
	 * Start the thread to begin synchronized capture
	 * @param stopNumber
	 */
	private void startCaptureThread( int stopNumber ) {
		// if thread is already running, then do nothing
		if ( this.captureThread != null ) return;
		
		TNVStatusBar.getInstance().updateStatus( TNVStatusBar.Operations.CAPTURE );
		
		final int stopNum = stopNumber;
		this.captureThread = new Thread( new Runnable() {
			public void run( ) {
				while ( TNV.this.captureThread != null ) {
					try {
						TNV.this.m_pcap.capture( stopNum );
					}
					catch ( Exception e ) {
						JOptionPane.showMessageDialog( TNV.this, "Error capturing packets: " + e.getMessage(),
								"Capture Error", JOptionPane.ERROR_MESSAGE );
						TNV.this.m_pcap.close();
						TNV.this.m_pcap = null;
					}
				}
				TNV.this.m_pcap.close();
				TNV.this.m_pcap = null;
			}
		} );
		this.captureThread.setPriority( Thread.NORM_PRIORITY );
		this.captureThread.start();
	}


	/**
	 * Stop the synchronized capture thread
	 */
	private void stopCaptureThread( ) {
		this.m_pcap.endCapture();
		this.captureThread = null;
	}


	// 
	/**
	 * Setup variables for capturing packets after captureDialog
	 * @param cont
	 * @param device
	 * @param promiscuous
	 * @param snaplen
	 * @param stopNumber
	 * @return success
	 */
	public boolean setupCapture( boolean cont, String device, boolean promiscuous, int snaplen, int stopNumber ) {
		if ( this.captureDialog != null )
			this.captureDialog.dispose();

		// if the user hit cancel, reenable menu items and return
		if ( !cont ) {
			this.startMenuItem.setEnabled( true );
			this.saveMenuItem.setEnabled( false );
			this.exportMenuItem.setEnabled( false );
			this.closeMenuItem.setEnabled( false );
			this.detailsMenuItem.setEnabled( false );
			this.portsMenuItem.setEnabled( false );
			this.resetMenuItem.setEnabled( false );
			return false;
		}
		this.currentSnaplen = snaplen;

		// open the device based on snaplen, promiscuous, timeout
		try {
			this.m_pcap.open( device, snaplen, promiscuous, 600000 );
		}
		catch ( Exception ex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "Error opening device " + device, ex);
			this.startMenuItem.setEnabled( true );
			this.saveMenuItem.setEnabled( false );
			this.exportMenuItem.setEnabled( false );
			this.closeMenuItem.setEnabled( false );
			this.detailsMenuItem.setEnabled( false );
			this.portsMenuItem.setEnabled( false );
			this.resetMenuItem.setEnabled( false );
			return false;
		}
		
		try {
			this.m_pcap.setFilter( FILTER, true );
			this.linkLayerType = this.m_pcap.getLinkLayerType();
			this.m_pcap.addRawPacketListener( new TNVPacketHandler() );
		}
		catch ( Exception ex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "Error creating packet handler for " + device, ex);
			this.startMenuItem.setEnabled( true );
			this.saveMenuItem.setEnabled( false );
			this.exportMenuItem.setEnabled( false );
			this.closeMenuItem.setEnabled( false );
			this.detailsMenuItem.setEnabled( false );
			this.portsMenuItem.setEnabled( false );
			this.resetMenuItem.setEnabled( false );
			return false;
		}

		// if user specified to stop by number of packets, set that
		if ( stopNumber != 0 ) {
			startCaptureThread( stopNumber );
			
			// TODO:  wait for thread to finish
			
		}
		else {
			startCaptureThread( INFINITE );

			TNVStopCaptureDialog.createTNVStopCaptureDialog(this, this.m_pcap);
		}
		return true;
	}

	/**
	 * For StopCaptureDialog to end capturing
	 */
	public void stopCapture() {
		stopCaptureThread();
		stopPacketCapture();
	}
	
	/**
	 * @param args the command line arguments
	 */
	public static void main( String args[] ) {
		
		// check version - must be 1.5+ 
		String version = System.getProperty("java.version");
		if ( version.startsWith("1.2") || version.startsWith("1.3") || version.startsWith("1.4") ) {
			System.err.println ("\nJava " + version + " is not supported:   tnv requires JRE 1.5 or higher\n\n");
			System.exit(1);
		}
		
		new TNV();
	}

}
