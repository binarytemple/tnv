/**
 * Created on Apr 29, 2006 
 * @author jgood
 * 
 * 
 */
package net.sourceforge.tnv.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import net.sourceforge.jpcap.net.IPProtocols;
import net.sourceforge.tnv.TNV;
import net.sourceforge.tnv.db.TNVDbUtil;
import net.sourceforge.tnv.dialogs.TNVErrorDialog;
import net.sourceforge.tnv.util.TNVUtil;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PZoomEventHandler;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * TNVPortsPanel
 */
public class TNVPortsPanel extends JPanel {

	public static int BASE_LAYER = 0;
	public static int TOOLTIP_LAYER = 1;

	private static final int LABEL_OFFSET = 50;
	private static final int TOP_LABEL_OFFSET = 35;
	private static final int BOTTOM_LABEL_OFFSET = 20;
	private static final int BOX_WIDTH = 10;
	
	private static final double MAX_SCALE = 10.0;
	private static final double MIN_SCALE = 1;
	
	private static final Color REMOTE_COLOR = new Color(223,194,125);
	private static final Color LOCAL_COLOR = new Color(128,205,193);
	
	private static PPath DEFAULT_BOX_NODE;
	private static PText DEFAULT_BOX_LABEL;
	
	private TNVModel.DisplayLinkDirection direction;

	private PCanvas canvas;
	
	// Layers
	private PLayer nodeLayer, // base layer, for nodes
			tooltipLayer = new PLayer(); // top layer, for tooltips
	
	// Graphs
	private PNode portGraph, srcGraph, dstGraph, interGraph;
		
	// Labels
	private PText nameLabel, topSrcLabel, topDstLabel, bottomSrcLabel, bottomDstLabel;
	
	// Tooltip
	private PText tooltip;
	
	private ButtonGroup directionGroup;

	private List<List<Integer>> links = new ArrayList<List<Integer>>();

	private SortedMap<Integer, Integer> srcFreqMap = new TreeMap<Integer, Integer>();
	private SortedMap<Integer, Integer> dstFreqMap = new TreeMap<Integer, Integer>();

	private List<TNVLocalHostCell> selectedHostList = new ArrayList<TNVLocalHostCell>();
	private boolean allHosts = false;
	
	private int totalCount = 0;
	
	// set up defaults to clone
	static {
		DEFAULT_BOX_NODE = new PPath();
		DEFAULT_BOX_NODE.setStroke(new BasicStroke(0.35f));
		DEFAULT_BOX_NODE.setStrokePaint(Color.DARK_GRAY);
		
		DEFAULT_BOX_LABEL = new PText();
		DEFAULT_BOX_LABEL.setGreekThreshold(1);
		DEFAULT_BOX_LABEL.setFont(TNVUtil.SMALL_LABEL_FONT);
		DEFAULT_BOX_LABEL.setTransparency(0.85f);
	}
	
	/**
	 * Constructor 
	 */
	public TNVPortsPanel() {
		super();
		
		this.setLayout(new BorderLayout());
		
		// incoming/outgoing radio panel
		JPanel radioButtonPanel = new JPanel();

		JRadioButton inButton = new JRadioButton( "Ingress" );
		inButton.setFont( TNVUtil.LABEL_FONT );
		inButton.setToolTipText( "Show port activity for ingress (incoming) traffic of selected hosts" );
		inButton.setActionCommand( "in" );

		JRadioButton outButton = new JRadioButton( "Egress" );
		outButton.setFont( TNVUtil.LABEL_FONT );
		outButton.setToolTipText( "Show port activity for for egress (outgoing) traffic of selected hosts" );
		outButton.setActionCommand( "out" );

		inButton.setSelected( true ); // default
		this.direction = TNVModel.DisplayLinkDirection.INGRESS;
		
		this.directionGroup = new ButtonGroup();
		this.directionGroup.add( inButton );
		this.directionGroup.add( outButton );

		ActionListener directionActionListener = new DirectionActionListener();
		inButton.addActionListener( directionActionListener );
		outButton.addActionListener( directionActionListener );

		radioButtonPanel.add( inButton );
		radioButtonPanel.add( outButton );
				
		this.add(radioButtonPanel, BorderLayout.NORTH);
		
		// default pan is with left button
		// default zoom is with right button: to right to zoom in, left to zoom out
		this.canvas = new PCanvas();

		// set min and max scales for zooming
		PZoomEventHandler zoomHandler = this.canvas.getZoomEventHandler();
		zoomHandler.setMinScale(MIN_SCALE);
		zoomHandler.setMaxScale(MAX_SCALE);
		this.canvas.setZoomEventHandler(zoomHandler);
		
		// use wheel up to zoom in, wheel down to zoom out
		this.canvas.addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				PCamera camera = TNVPortsPanel.this.canvas.getCamera();
				double currentScale = camera.getViewScale();
				int notches = e.getWheelRotation();
				if ( notches < 0 && (currentScale * 1.1) <= MAX_SCALE )
					camera.scaleView( 1.1 );
				else if ( notches > 0 && (currentScale * 0.9) >= MIN_SCALE )
					camera.scaleView( 0.9 );
			}
		});
		
		this.add(this.canvas, BorderLayout.CENTER);
		
		// Reset zoom/pan
		JButton showAllButton = new JButton("Show All");
		showAllButton.setToolTipText("Show port activity for entire data set");
		showAllButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				try {
					TNVPortsPanel.this.allHosts = true;
					TNVPortsPanel.this.updatePorts();
				} catch (NullPointerException ex) { } // no data yet
			}
		} );
		JButton resetButton = new JButton("Reset");
		resetButton.setToolTipText("Reset zoom and center ports");
		resetButton.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				PCamera camera = TNVPortsPanel.this.canvas.getCamera();
				camera.setViewScale(1.0);
				camera.setViewTransform(new AffineTransform());
			}
		} );
		JPanel resetPanel = new JPanel();
		resetPanel.add(showAllButton);
		resetPanel.add(resetButton);
		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(resetPanel, BorderLayout.NORTH);
		
		// instructions
		String instructions = 
			"To show incoming traffic (coming into the local network), click 'Ingress', click 'Egress' for outgoing traffic.\n" +
			"Pan by dragging left button in direction to pan.\n" +
			"Zoom by dragging right button (towards right for in, left for out) or using mouse wheel.\n" +
			"Use the reset button to reset the zoom and pan to the default.";
		JPanel labelPanel = new JPanel(new BorderLayout());
		JTextArea textArea = new JTextArea(4,5);
		textArea.setFont(TNVUtil.LABEL_FONT);
		textArea.setEditable(false);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.append(instructions);
		textArea.setCaretPosition(1);
		JScrollPane scrollPane = new JScrollPane(textArea);
		bottomPanel.add(scrollPane, BorderLayout.CENTER);
		
		this.add(bottomPanel, BorderLayout.SOUTH);
	
		this.portGraph = new PNode();
		this.canvas.getLayer().addChild(this.portGraph);
		
		this.srcGraph = new PNode();
		this.portGraph.addChild(this.srcGraph);
		this.dstGraph = new PNode();
		this.portGraph.addChild(this.dstGraph);
		this.interGraph = new PNode();
		this.portGraph.addChild(this.interGraph);
		
		// Labels
		this.nameLabel = new PText("");
		this.nameLabel.setFont(TNVUtil.SMALL_LABEL_FONT);
		this.portGraph.addChild(0,this.nameLabel);
		this.nameLabel.setX(1);
		this.nameLabel.setY(1);

		this.topSrcLabel = new PText("SRC");
		this.topDstLabel = new PText("DST");
		this.topSrcLabel.setFont(TNVUtil.LARGE_LABEL_FONT);
		this.portGraph.addChild(1,this.topSrcLabel);
		this.topDstLabel.setFont(TNVUtil.LARGE_LABEL_FONT);
		this.portGraph.addChild(2,this.topDstLabel);
		
		this.bottomSrcLabel = new PText("Remote"); // default is incoming
		this.bottomDstLabel = new PText("Local");
		this.bottomSrcLabel.setFont(TNVUtil.LARGE_LABEL_FONT);
		this.bottomSrcLabel.setTextPaint(REMOTE_COLOR);
		this.portGraph.addChild(3,this.bottomSrcLabel);
		this.bottomDstLabel.setFont(TNVUtil.LARGE_LABEL_FONT);
		this.bottomDstLabel.setTextPaint(LOCAL_COLOR);
		this.portGraph.addChild(4,this.bottomDstLabel);

		// base layer for drawing nodes
		this.nodeLayer = this.canvas.getLayer();
		this.nodeLayer.addChild( this.portGraph );

		// layer for displaying tooltips on top of edges and nodes
		// events for tooltip layer are handled by individual nodes
		this.canvas.getRoot().addChild( TOOLTIP_LAYER, this.tooltipLayer );
		this.canvas.getCamera().addLayer( TOOLTIP_LAYER, this.tooltipLayer );

		// setup tooltip node
		this.tooltip = (PText) TNVUtil.DEFAULT_TOOLTIP_NODE.clone();

		// handle resize events
		this.addComponentListener( new ComponentAdapter() {
			@Override
			public void componentResized( ComponentEvent evt ) {
				double w = TNVPortsPanel.this.canvas.getWidth();
				double h = TNVPortsPanel.this.canvas.getHeight();
				TNVPortsPanel.this.portGraph.setBounds( 0, TNVPortsPanel.this.canvas.getY(), w, h - 2 );
				topSrcLabel.setX(LABEL_OFFSET + (BOX_WIDTH/2) - (topSrcLabel.getWidth()/2));
				topSrcLabel.setY(16);
				topDstLabel.setX(w - LABEL_OFFSET - (BOX_WIDTH/2) - (topDstLabel.getWidth()/2));
				topDstLabel.setY(16);
				bottomSrcLabel.setX(LABEL_OFFSET + (BOX_WIDTH/2) - (bottomSrcLabel.getWidth()/2));
				bottomSrcLabel.setY(h - 16);
				bottomDstLabel.setX(w - LABEL_OFFSET - (BOX_WIDTH/2) - (bottomDstLabel.getWidth()/2));
				bottomDstLabel.setY(h - 16);
				TNVPortsPanel.this.updatePorts();
			}
		});

		// listen for initial setup property changes, menu item to fire to show all ports, 
		// and changes to highlighted hosts
		String[] listenProps = { TNVModel.PROPERTY_SETUP, TNVModel.PROPERTY_SHOW_ALL_PORTS, 
				TNVModel.PROPERTY_HIGHLIGHT_HOST_NODES };
		TNVModel.getInstance().addPropertyChangeListener( listenProps, new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent evt ) {
				String property = evt.getPropertyName();
				// setup graph
				if ( property.equals( TNVModel.PROPERTY_SETUP ) ) {
					boolean value = ( (Boolean) evt.getNewValue() ).booleanValue();
					if ( value == true ) {
						TNVPortsPanel.this.allHosts = true;
						TNVPortsPanel.this.updatePorts();
					}
					else {
						TNVPortsPanel.this.clearGraph();
					}
				}
				else if ( property.equals( TNVModel.PROPERTY_SHOW_ALL_PORTS ) ) {
					TNVPortsPanel.this.allHosts = true;
					TNVPortsPanel.this.updatePorts();
					Container parent = TNVPortsPanel.this.getParent();
					if ( parent instanceof JTabbedPane )
						((JTabbedPane)parent).setSelectedComponent(TNVPortsPanel.this);
				}
				else if ( property.equals( TNVModel.PROPERTY_HIGHLIGHT_HOST_NODES ) ) {
					TNVPortsPanel.this.selectedHostList.clear();
					TNVPortsPanel.this.selectedHostList.addAll( (Set) evt.getNewValue() );
					TNVModel.getInstance().getHighlightHostNodes().clear();
					TNVPortsPanel.this.allHosts = false;
					TNVPortsPanel.this.updatePorts();
					Container parent = TNVPortsPanel.this.getParent();
					if ( parent instanceof JTabbedPane )
						((JTabbedPane)parent).setSelectedComponent(TNVPortsPanel.this);
				}
			}
		});
	}

	
	/**
	 * Clear the graph completely
	 */
	public void clearGraph() {
		this.srcGraph.removeAllChildren();
		this.interGraph.removeAllChildren();
		this.dstGraph.removeAllChildren();
		this.nameLabel.setText("");
		PCamera camera = TNVPortsPanel.this.canvas.getCamera();
		camera.setViewScale(1.0);
		camera.setViewTransform(new AffineTransform());
	}
	
	
	/**
	 * Update the display
	 */
	private void updatePorts() {

		this.clearGraph();
		
		// calculate frequencies
		if ( allHosts ) {
			this.getPortsForAllHosts();
			this.nameLabel.setText("All Hosts");
		}
		else
			this.getPortsForSelectedHosts( selectedHostList );
				
		this.drawBoxes(this.srcGraph, this.srcFreqMap, true);
		this.drawBoxes(this.dstGraph, this.dstFreqMap, false);

		Iterator srcIt = this.srcGraph.getChildrenIterator();
		while ( srcIt.hasNext() ) {
			PNode srcNode = (PNode) srcIt.next();
			if ( srcNode instanceof PText )
				continue;
			
			for ( List<Integer> link : this.links ) {
				if ( ((Integer)srcNode.getAttribute("port")).intValue() == 
						link.get(0).intValue() ) {
					
					Iterator dstIt = this.dstGraph.getChildrenIterator();
					while ( dstIt.hasNext() ) {
						PNode dstNode = (PNode) dstIt.next();
						if ( dstNode instanceof PText )
							continue;

						if ( ((Integer)dstNode.getAttribute("port")).intValue() == 
								link.get(1).intValue() ) {
							int offset = 0;
							Color paint = TNVPreferenceData.getInstance().getIcmpColor();
							int proto = link.get(2).intValue();
							if ( proto == IPProtocols.TCP ) {
								paint = TNVPreferenceData.getInstance().getTcpColor();
								offset = -1;
							}
							else if ( proto == IPProtocols.UDP ) {
								paint = TNVPreferenceData.getInstance().getUdpColor();
								offset = 1;
							}
							float stroke = 1.0f;
							if ( link.get(3).intValue() > 100 )
								stroke = 1.5f;
							else if ( link.get(3).intValue() > 10 ) 
								stroke = 1.15f;
							PPath linkLine = PPath.createLine(
									(float) ( srcNode.getX() + srcNode.getWidth() - 1 ),
									(float) ( srcNode.getY() + (srcNode.getHeight() / 2) + offset ),
									(float) ( dstNode.getX() + 1 ),
									(float) ( dstNode.getY() + (dstNode.getHeight() / 2) + offset )
									);

							linkLine.setStroke(new BasicStroke(stroke));
							linkLine.setStrokePaint(paint);
							
							this.interGraph.addChild( linkLine );
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * 
	 */
	private void getPortsForAllHosts() {
		this.totalCount = 0;
		this.srcFreqMap.clear();
		this.dstFreqMap.clear();
		this.links.clear();

		PreparedStatement statement;
		String srcaddr, dstaddr;
		int srcport, dstport, protocol, count;

		try {
			if ( this.direction.equals(TNVModel.DisplayLinkDirection.INGRESS) )
				statement = TNVDbUtil.getInstance().getSelectAllInPortsStmt();
			else
				statement = TNVDbUtil.getInstance().getSelectAllOutPortsStmt();
			ResultSet rs = statement.executeQuery();
			while ( rs.next() ) {
				srcaddr = rs.getString( "srcaddr" );
				srcport = rs.getInt( "srcport" );
				dstaddr = rs.getString( "dstaddr" );
				dstport = rs.getInt( "dstport" );
				protocol = rs.getInt( "protocol" );
				count = rs.getInt( "frequency" );

				if ( protocol != IPProtocols.TCP && protocol != IPProtocols.UDP )
					continue;
				
				// Integer values to be added to maps
				Integer srcPrtInteger = new Integer( srcport );
				Integer dstPrtInteger = new Integer( dstport );

				if ( this.srcFreqMap.containsKey( srcPrtInteger ) )
					this.srcFreqMap.put( srcPrtInteger, 
							new Integer( this.srcFreqMap.get( srcPrtInteger ).intValue() + count ) );
				else
					this.srcFreqMap.put( srcPrtInteger, new Integer( count ) );
				
				if ( this.dstFreqMap.containsKey( dstPrtInteger ) )
					this.dstFreqMap.put( dstPrtInteger, 
							new Integer( this.dstFreqMap.get( dstPrtInteger ).intValue() + count ) );
				else
					this.dstFreqMap.put( dstPrtInteger, new Integer( count ) );

				List<Integer> link = new ArrayList<Integer>();
				link.add(0, srcPrtInteger);
				link.add(1, dstPrtInteger);
				link.add(2, new Integer(protocol));
				link.add(3, new Integer(count));
				this.links.add( link );
				
				this.totalCount += count;
			}
			rs.close();
		}
		catch ( SQLException ex ) {
			TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL Error getting packets for port details", ex);
		}

	}
	
	/**
	 * @param selectedHosts
	 */
	private void getPortsForSelectedHosts(List<TNVLocalHostCell> selectedHosts) {
		this.totalCount = 0;
		this.srcFreqMap.clear();
		this.dstFreqMap.clear();
		this.links.clear();

		PreparedStatement statement;
		String srcaddr, dstaddr;
		int srcport, dstport, protocol, count;

		Iterator hostListIt = selectedHosts.iterator();
		while ( hostListIt.hasNext() ) {
			TNVLocalHostCell cell = (TNVLocalHostCell) hostListIt.next();
			String name = cell.getName();
			this.nameLabel.setText(this.nameLabel.getText() + " " + name);

			statement = null;
			try {
				if ( this.direction.equals(TNVModel.DisplayLinkDirection.INGRESS) ) {
					statement = TNVDbUtil.getInstance().getSelectInPortsStmt();
					statement.setString( 1, name );
					statement.setTimestamp( 2, cell.getStartTime() );
					statement.setTimestamp( 3, cell.getEndTime() );
				}
				else if ( this.direction.equals(TNVModel.DisplayLinkDirection.EGRESS) ) {
					statement = TNVDbUtil.getInstance().getSelectOutPortsStmt();
					statement.setString( 1, name );
					statement.setTimestamp( 2, cell.getStartTime() );
					statement.setTimestamp( 3, cell.getEndTime() );
				}
				ResultSet rs = statement.executeQuery();
				while ( rs.next() ) {
					srcaddr = rs.getString( "srcaddr" );
					srcport = rs.getInt( "srcport" );
					dstaddr = rs.getString( "dstaddr" );
					dstport = rs.getInt( "dstport" );
					protocol = rs.getInt( "protocol" );
					count = rs.getInt( "frequency" );

					if ( protocol != IPProtocols.TCP && protocol != IPProtocols.UDP )
						continue;
					
					// Integer values to be added to maps
					Integer srcPrtInteger = new Integer( srcport );
					Integer dstPrtInteger = new Integer( dstport );

					if ( this.srcFreqMap.containsKey( srcPrtInteger ) )
						this.srcFreqMap.put( srcPrtInteger, 
								new Integer( this.srcFreqMap.get( srcPrtInteger ).intValue() + count ) );
					else
						this.srcFreqMap.put( srcPrtInteger, new Integer( count ) );
					
					if ( this.dstFreqMap.containsKey( dstPrtInteger ) )
						this.dstFreqMap.put( dstPrtInteger, 
								new Integer( this.dstFreqMap.get( dstPrtInteger ).intValue() + count ) );
					else
						this.dstFreqMap.put( dstPrtInteger, new Integer( count ) );

					List<Integer> link = new ArrayList<Integer>();
					link.add(0, srcPrtInteger);
					link.add(1, dstPrtInteger);
					link.add(2, new Integer(protocol));
					link.add(3, new Integer(count));
					this.links.add( link );
					
					this.totalCount += count;
				}
				rs.close();
			}
			catch ( SQLException ex ) {
				TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL Error getting packets for port details", ex);
			}
		}
	}
	
	/**
	 * set up the src or dst boxes
	 * @param graph
	 * @param freqMap
	 * @param x
	 * @param w
	 * @param labelToLeft
	 */
	private void drawBoxes(PNode graph, SortedMap<Integer, Integer> freqMap, boolean labelToLeft) {
		float x, y = TOP_LABEL_OFFSET, h, labelX, labelY;
		Color defaultBoxColor; 
		if ( labelToLeft ) {
			x = LABEL_OFFSET;
			labelX = 1;
			if ( TNVPortsPanel.this.direction == TNVModel.DisplayLinkDirection.INGRESS )
				defaultBoxColor = REMOTE_COLOR;
			else
				defaultBoxColor = LOCAL_COLOR;
		}
		else {
			x = this.getWidth() - LABEL_OFFSET - BOX_WIDTH;
			labelX = x + BOX_WIDTH + 1;
			if ( TNVPortsPanel.this.direction == TNVModel.DisplayLinkDirection.INGRESS )
				defaultBoxColor = LOCAL_COLOR;
			else
				defaultBoxColor = REMOTE_COLOR;
		}
		float totalHeight = this.canvas.getHeight() - BOTTOM_LABEL_OFFSET - TOP_LABEL_OFFSET;
		int i = 0; // alternate between lighter and darker shades of default
		Iterator mapIt = freqMap.entrySet().iterator();
		while ( mapIt.hasNext() ) {
			Map.Entry e = (Map.Entry) mapIt.next();
			Object key = e.getKey();
			h = ( totalHeight * ( ( (Integer) e.getValue() ).intValue() / (float) this.totalCount ) );
			
			PPath boxNode = (PPath) DEFAULT_BOX_NODE.clone();
			boxNode.setPathToRectangle(x, y, BOX_WIDTH, h);
			boxNode.addAttribute("port", key);
			boxNode.addAttribute("frequency", e.getValue());
			boxNode.setPaint( defaultBoxColor );
			if ( i++ % 2 == 0 )
				boxNode.setTransparency(0.85f);
			else
				boxNode.setTransparency(0.65f);
			// listen for mouse movements to handle tooltips
			boxNode.addInputEventListener( new PBasicInputEventHandler() {
				@Override
				public void mouseEntered( PInputEvent event ) {
					if ( event.getButton() == MouseEvent.NOBUTTON ) {
						TNV.setCrosshairCursor();
						if ( TNVPreferenceData.getInstance().isShowTooltips() ) {
							PNode node = event.getPickedNode();
							String port = ((Integer) node.getAttribute("port")).toString();
							String frequency = ((Integer) node.getAttribute("frequency")).toString();
							TNVPortsPanel.this.tooltip.setText(" Port " + port + ": " + frequency + "  ");
							event.getCamera().getLayer( TNVPortsPanel.TOOLTIP_LAYER ).addChild( TNVPortsPanel.this.tooltip );
							double xPosition = node.getX() - 25; 
							double centerX = TNVPortsPanel.this.canvas.getWidth() / 2;
							if ( xPosition > centerX )
								xPosition = node.getX() - (TNVPortsPanel.this.tooltip.getWidth()) + BOX_WIDTH + 25;
							TNVPortsPanel.this.tooltip.setOffset( xPosition, event.getPosition().getY() - 25 );
						}
					}
				}
				@Override
				public void mouseExited( PInputEvent event ) {
					if ( event.getButton() == MouseEvent.NOBUTTON ) {
						TNV.setDefaultCursor();
						if ( TNVPreferenceData.getInstance().isShowTooltips() )
							event.getCamera().getLayer( TNVCanvas.TOOLTIP_LAYER ).removeAllChildren();
					}
				}
			} );
			

			graph.addChild(boxNode);
			PText label = (PText) DEFAULT_BOX_LABEL.clone();
			label.setText( ((Integer)key).intValue() + "" );
			
			double scale = h / 12;
			if ( scale < 0.1 )
				scale = 0.1;
			else if ( scale > 1.4 )
				scale = 1.4;
			label.scale( scale );
			
			PBounds globalBounds = label.getGlobalBounds();
			// right alight if the label is to the left of boxes
			if ( labelToLeft ) 
				labelX = LABEL_OFFSET - 1 - (float) globalBounds.getWidth();
			// vertical align to center
			labelY = y + (h/2) - ((float) globalBounds.getHeight()/2);
			
			label.offset(labelX, labelY);
			graph.addChild(label);
			y += h;
		}
	}

	
	/**
	 * action listener for direction
	 */
	private class DirectionActionListener implements ActionListener {
		public void actionPerformed( ActionEvent e ) {
			String choice = TNVPortsPanel.this.directionGroup.getSelection().getActionCommand();
			if ( choice.equals( "in" ) ) {
				TNVPortsPanel.this.bottomSrcLabel.setText("Remote");
				TNVPortsPanel.this.bottomSrcLabel.setTextPaint(REMOTE_COLOR);
				TNVPortsPanel.this.bottomDstLabel.setText("Local");
				TNVPortsPanel.this.bottomDstLabel.setTextPaint(LOCAL_COLOR);
				TNVPortsPanel.this.direction = TNVModel.DisplayLinkDirection.INGRESS;
			}
			else if ( choice.equals( "out" ) ) {
				TNVPortsPanel.this.bottomSrcLabel.setText("Local");
				TNVPortsPanel.this.bottomSrcLabel.setTextPaint(LOCAL_COLOR);
				TNVPortsPanel.this.bottomDstLabel.setText("Remote");
				TNVPortsPanel.this.bottomDstLabel.setTextPaint(REMOTE_COLOR);
				TNVPortsPanel.this.direction = TNVModel.DisplayLinkDirection.EGRESS;
			}
			bottomSrcLabel.setX(LABEL_OFFSET + (BOX_WIDTH/2) - (bottomSrcLabel.getWidth()/2));
			bottomDstLabel.setX(TNVPortsPanel.this.canvas.getWidth() - LABEL_OFFSET - 
					(BOX_WIDTH/2) - (bottomDstLabel.getWidth()/2));
			TNVPortsPanel.this.updatePorts();
			PCamera camera = TNVPortsPanel.this.canvas.getCamera();
			camera.setViewScale(1.0);
			camera.setViewTransform(new AffineTransform());
		}
	}
		
}
