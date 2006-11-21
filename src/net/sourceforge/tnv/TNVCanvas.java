/**
 * Created on Feb 17, 2006
 * @author jgood
 * 
 * Piccolo PNode for Packet representations
 */
package net.sourceforge.tnv;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;

/**
 * TNVCanvas
 * Main visualization canvas, extends Piccalo PCanvas
 */
public class TNVCanvas extends PCanvas {

	protected static int BASE_LAYER = 0;
	protected static int TOOLTIP_LAYER = 1;

	private static int REMOTE_GRAPH_WIDTH = 100; // remote hosts label size
	private static int INTERGRAPH_WIDTH = 120; // space between local and remote graph
	private static int SIDE_LABEL_WIDTH = 140; // right side label size
	protected static int TOP_LABEL_HEIGHT = 20; // top label size

	// Layers
	private PLayer nodeLayer, // base layer, for nodes
			tooltipLayer = new PLayer(); // top layer, for tooltips

	// graph node holds all of the drawing for local nodes
	private TNVLocalHostsGraph localGraph;
	// intergraph - for edges (edges update themselves when host nodes change position)
	private TNVInterGraph interGraph;
	// graph node holds labels for remote nodes
	private TNVRemoteHostsGraph remoteGraph;

	// list of host nodes that are currently selected
	private Set<TNVLocalHostCell> selectedNodes = new HashSet<TNVLocalHostCell>();

	// popup menu; items are added removed on the fly
	private JPopupMenu popup = new JPopupMenu();


	/**
	 * Constructor
	 */
	public TNVCanvas() {
		super();

		this.localGraph = new TNVLocalHostsGraph( this );
		this.remoteGraph = new TNVRemoteHostsGraph( this );
		this.interGraph = new TNVInterGraph( this );

		removeInputEventListener( getZoomEventHandler() );
		removeInputEventListener( getPanEventHandler() );

		// base layer for drawing nodes
		this.nodeLayer = this.getLayer();
		this.nodeLayer.addChild( this.localGraph );
		this.nodeLayer.addChild( this.interGraph );
		this.nodeLayer.addChild( this.remoteGraph );

		// layer for displaying tooltips on top of edges and nodes
		// events for tooltip layer are handled by individual nodes
		this.getRoot().addChild( TOOLTIP_LAYER, this.tooltipLayer );
		this.getCamera().addLayer( TOOLTIP_LAYER, this.tooltipLayer );

		// handle resize events
		this.addComponentListener( new ComponentAdapter() {
			@Override
			public void componentResized( ComponentEvent evt ) {
				TNVCanvas.this.remoteGraph.setBounds( getX(), getY() + TOP_LABEL_HEIGHT, REMOTE_GRAPH_WIDTH,
						getHeight() - TOP_LABEL_HEIGHT - 2 );

				// intergraph in between remote and local
				TNVCanvas.this.interGraph.setBounds( getX() + REMOTE_GRAPH_WIDTH, getY() + TOP_LABEL_HEIGHT,
						INTERGRAPH_WIDTH, getHeight() - TOP_LABEL_HEIGHT - 2 );

				// local graph on right side of screen, with space for labels and space in between
				TNVCanvas.this.localGraph.setBounds( getX() + REMOTE_GRAPH_WIDTH + INTERGRAPH_WIDTH, getY()
						+ TOP_LABEL_HEIGHT, getWidth() - REMOTE_GRAPH_WIDTH - INTERGRAPH_WIDTH - SIDE_LABEL_WIDTH,
						getHeight() - TOP_LABEL_HEIGHT - 2 );
				
				TNVCanvas.this.localGraph.layoutChildren();
				TNVCanvas.this.remoteGraph.layoutChildren();
			}
		});

		
		// listen for initial setup property changes
		String[] listenProps = { TNVModel.PROPERTY_SETUP, TNVModel.PROPERTY_IS_TIME_ADJUSTING };
		TNVModel.getInstance().addPropertyChangeListener( listenProps, new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent evt ) {
				String property = evt.getPropertyName();
				// setup graph
				if ( property.equals( TNVModel.PROPERTY_SETUP ) ) {
					TNV.setWaitCursor();
					boolean value = ( (Boolean) evt.getNewValue() ).booleanValue();
					if ( value == true ) {
						TNVCanvas.this.localGraph.setupHosts();
						TNVCanvas.this.remoteGraph.setupHosts();
						TNVCanvas.this.localGraph.setupColumnTimes();
						TNVCanvas.this.interGraph.createEdges();
						TNVCanvas.this.repaint();
					}
					else {
						TNVCanvas.this.localGraph.clearGraph();
						TNVCanvas.this.remoteGraph.clearGraph();
						TNVCanvas.this.interGraph.clearGraph();
						TNVCanvas.this.repaint();
					}
					TNV.setDefaultCursor();
				}
				else if ( property.equals( TNVModel.PROPERTY_IS_TIME_ADJUSTING ) ) {
					if ( ((Boolean)evt.getNewValue()).booleanValue() == false ) {
						TNVCanvas.this.interGraph.clearGraph();
						TNVCanvas.this.interGraph.createEdges();
					}
					else
						TNVCanvas.this.interGraph.clearGraph();
				}
			}
		} );

		// listen for user selection and interaction
		this.addInputEventListener( new PBasicInputEventHandler() {
			@Override
			public void mouseReleased( PInputEvent event ) {
				super.mouseReleased( event );
				PNode pickedNode = event.getPickedNode();

				// check for right click, trigger popup menu
				if ( TNVCanvas.this.isPopupTrigger( event ) )
					createPopup( event );

				// if a host cell is clicked on once, highlight it
				else if ( event.getClickCount() == 1 ) {
					if ( pickedNode instanceof TNVLocalHostCell ) {
						TNVLocalHostCell node = (TNVLocalHostCell) pickedNode;
						int frequency = node.getFrequency();
						if ( frequency == 0 )
							clearSelectedNodes();
						else {
							if ( node.isSelected() ) {
								node.setSelected( false );
								TNVCanvas.this.selectedNodes.remove( node );
							}
							else {
								node.setSelected( true );
								TNVCanvas.this.selectedNodes.add( node );
							}
						}
						event.getCamera().repaint();
						TNVStatusBar.getInstance().updateSelection( selectedNodes );
						event.setHandled( true );
					}
					else
						clearSelectedNodes();
				}
					
				// if a host cell is clicked twice, expand it
				else if ( event.getClickCount() == 2 && pickedNode instanceof TNVLocalHostCell ) {
					TNVLocalHostCell node = (TNVLocalHostCell) pickedNode;
					if ( node.hasWidthFocus() && node.hasHeightFocus() )
						TNVCanvas.this.localGraph.setFocusNode(node, false);
					else
						TNVCanvas.this.localGraph.setFocusNode(node, true);
				}

			}
		} );

	}


	// clear selected host nodes and edge nodes
	protected void clearSelectedNodes( ) {
		List<TNVLocalHostCell> temp = new ArrayList<TNVLocalHostCell>( this.selectedNodes );
		this.selectedNodes.clear();
		for ( TNVLocalHostCell i : temp )
			i.setSelected( false );
		temp.clear();
	}


	// create and display popup menu
	private void createPopup( PInputEvent event ) {
		event.setHandled( true );

		this.popup.removeAll();

		PNode node = event.getPickedNode();

		if ( node instanceof TNVLocalHostCell && ( (TNVLocalHostCell) node ).getFrequency() > 0 ) {
			final TNVLocalHostCell hostnode = (TNVLocalHostCell) node;
			JMenuItem detailsMenuItem = new JMenuItem( "Show packet details for " + hostnode.getName() );
			detailsMenuItem.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent e ) {
					clearSelectedNodes();
					hostnode.setSelected( true );
					TNVCanvas.this.selectedNodes.add( hostnode );
					TNVModel.getInstance().setDetailsForHostNodes( TNVCanvas.this.selectedNodes );
				}
			} );
			this.popup.add( detailsMenuItem );

			JMenuItem portMenuItem = new JMenuItem( "Show port activity for " + hostnode.getName() );
			portMenuItem.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent e ) {
					clearSelectedNodes();
					hostnode.setSelected( true );
					TNVCanvas.this.selectedNodes.add( hostnode );
					TNVModel.getInstance().setHighlightHostNodes( TNVCanvas.this.selectedNodes );
				}
			} );
			this.popup.add( portMenuItem );
		}
		if ( this.selectedNodes.size() > 1 ) {
			this.popup.addSeparator();
			JMenuItem allDetailsMenuItem = new JMenuItem( "Show packet details for highlighted host(s)" );
			allDetailsMenuItem.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent e ) {
					TNVModel.getInstance().setDetailsForHostNodes( TNVCanvas.this.selectedNodes );
				}
			} );
			this.popup.add( allDetailsMenuItem );

			JMenuItem allPortMenuItem = new JMenuItem( "Show port activity for highlighted host(s)" );
			allPortMenuItem.addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent e ) {
					TNVModel.getInstance().setHighlightHostNodes( TNVCanvas.this.selectedNodes );
				}
			} );
			this.popup.add( allPortMenuItem );
		}

		this.popup.addSeparator();
		JMenuItem resetMenuItem = new JMenuItem( "Reset Display" );
		resetMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVUIManager.setupUI();
			}
		} );
		this.popup.add( resetMenuItem );
		
		JMenuItem prefsMenuItem = new JMenuItem( "Preferences..." );
		prefsMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				TNVPreferenceDialog.createTNVPreferenceDialog();
			}
		} );
		this.popup.add( prefsMenuItem );
		
		this.popup.show( this, (int) event.getCanvasPosition().getX(), (int) event.getCanvasPosition().getY() );

	}


	// WORKAROUND: if isPopupTrigger is used on a Mac, it must be in the mouse
	// pressed method, but that causes the mouse released method to no longer
	// work correctly, and the current node is continually selected. The only
	// way to get out of this cycle is to right (control) click elsewhere on the
	// screen. To get around this, all mouse events are handled in the
	// mouseReleased
	// method. This should work on all platforms but has not been tested.
	private boolean isPopupTrigger( PInputEvent event ) {

		if ( System.getProperty( "os.name" ).equalsIgnoreCase( "mac os x" ) )
			return ( event.isControlDown() || event.isRightMouseButton() );
		return event.isPopupTrigger();
	}

	
	/**
	 * @return the interGraph
	 */
	protected final TNVInterGraph getInterGraph( ) {
		return this.interGraph;
	}

	
	/**
	 * @return the localGraph
	 */
	protected final TNVLocalHostsGraph getLocalGraph( ) {
		return this.localGraph;
	}

	
	/**
	 * @return the remoteGraph
	 */
	protected final TNVRemoteHostsGraph getRemoteGraph( ) {
		return this.remoteGraph;
	}

}
