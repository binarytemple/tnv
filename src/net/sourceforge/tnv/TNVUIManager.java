/**
 * Created on Apr 13, 2004 
 * @author jgood
 * 
 * This class handles and coordinates all UI related activity
 */

package net.sourceforge.tnv;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

/**
 * @author jgood
 * created on: Apr 29, 2006
 */
public class TNVUIManager extends JPanel implements PropertyChangeListener {

	/**
	 * Main visualization panel
	 */
	private TNVCanvas visPanel;


	/**
	 * Constructor
	 */
	protected TNVUIManager() {
		super();
		initGUI();
	}


	/**
	 * Clear display completely
	 */
	protected static void clearUI( ) {
		TNVModel.getInstance().clear();
	}


	/**
	 *  Set up display completely
	 */
	protected static void setupUI( ) {
		TNVModel.getInstance().setupData();
	}

	
	/**
	 *  Reset display completely
	 */
	protected static void resetUI( ) {
		TNVModel.getInstance().resetData();
	}

	
	/**
	 * Initialize the UI 
	 */
	private void initGUI( ) {

		// to create new detail window when host is selected
		TNVModel.getInstance().addPropertyChangeListener( TNVModel.PROPERTY_DETAILS_FOR_HOST_NODES, this );

		// the main display panel
		// visPanel = new TNVVisPanel();
		this.visPanel = new TNVCanvas();

		// the time slider for navigating through the data
		TNVTimeSlider timeSlider = new TNVTimeSlider();

		// the legend panel for displaying current preferences and time interval
		TNVLegendPanel legendPanel = new TNVLegendPanel();

		// the control panel for perference and display options
		TNVDisplayPanel displayPanel = new TNVDisplayPanel();
		
		// the control panel for filtering and highlighting options
		TNVFilterPanel filterPanel = new TNVFilterPanel();

		// the panel for showing port visualization
		TNVPortsPanel portPanel = new TNVPortsPanel();
		
		/*
		 * This panel is divided into a horizontal split pane
		 * The main vis is on the left and the control panel on the right
		 */

		JSplitPane splitPane = new JSplitPane();
		splitPane.setOrientation( JSplitPane.HORIZONTAL_SPLIT );
		splitPane.setResizeWeight( 1 ); // give visPanel any extra space
		splitPane.setDividerSize( 0 );  // do not show the divider
				
		JPanel bottomVisContainer = new JPanel( new BorderLayout() );
		bottomVisContainer.add( timeSlider, BorderLayout.CENTER );
		bottomVisContainer.add( legendPanel, BorderLayout.SOUTH );

		JPanel topVisContainer = new JPanel( new BorderLayout() );
		topVisContainer.add( this.visPanel, BorderLayout.CENTER );
		topVisContainer.add( bottomVisContainer, BorderLayout.SOUTH );

		splitPane.setLeftComponent( topVisContainer );

		JTabbedPane controlPane = new JTabbedPane(SwingConstants.TOP);
		controlPane.setMinimumSize( new Dimension( 240, 240 ) );
		controlPane.setPreferredSize( new Dimension( 250, 600 ) );
		controlPane.setMaximumSize( new Dimension( 250, 1200 ) );
		
		controlPane.addTab("Display", null, displayPanel, "Display and perference options" );
		controlPane.setMnemonicAt(0, KeyEvent.VK_D);

		controlPane.addTab("Filter", null, filterPanel, "Filtering and highlighting options" );
		controlPane.setMnemonicAt(1, KeyEvent.VK_F);

		controlPane.addTab("Ports", null, portPanel, "Port detail visualization");
		controlPane.setMnemonicAt(2, KeyEvent.VK_P);		

		splitPane.setRightComponent( controlPane );

		this.setLayout( new BorderLayout() );
		this.add( splitPane, BorderLayout.CENTER );
		this.add( TNVStatusBar.getInstance(), BorderLayout.SOUTH );
		this.setVisible( true );

	}


	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange( PropertyChangeEvent evt ) {
		if ( evt.getPropertyName().equals( TNVModel.PROPERTY_DETAILS_FOR_HOST_NODES ) ) {
			Set l = (Set) evt.getNewValue();
			if ( !l.isEmpty() ) {
				TNVDetailWindow.createTNVDetailWindow( l );
				TNVModel.getInstance().getDetailsForHostNodes().clear();
			}
		}
	}

}
