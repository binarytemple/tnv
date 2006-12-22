/**
 * Created on Apr 13, 2004 
 * @author jgood
 * 
 * Piccolo PNode for Local Hosts Graph representations
 */
package net.sourceforge.tnv;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * LocalHostsGraph
 */
public class TNVLocalHostsGraph extends PNode {

	protected static String PROPERTY_COLUMN_TIME_INTERVAL = "columnTimeInterval";
	protected static String PROPERTY_COLUMN_START_TIMES = "columnStartTimes";
	protected static String PROPERTY_FOCUS_CHANGED = "focusChanged";
	
	protected static double HISTOGRAM_WIDTH = 35;
	
	private static float WIDTH_FOCUS_SIZE_PERCENT = 0.85f;	// expanded nodes take up 85% of total graph

	// parent canvas
	private TNVCanvas canvas;

	private int sortOrder;
	
	// number of rows/cols expanded
	private int colsExpanded = 0;
	private int rowsExpanded = 0;
	
	private int numberOfHostRows = 0;
	private int numberOfColumns = TNVPreferenceData.getInstance().getColumnCount();
	private double defaultRowHeight = TNVPreferenceData.getInstance().getRowHeight();
	
	// timestamps mapped to x position
	private SortedMap<java.lang.Long, java.lang.Integer> timeMap = new TreeMap<java.lang.Long, java.lang.Integer>();

	private double[] columnWidths = {};
	private long[] columnStartTimes = null;
	private long columnTimeInterval = 0;


	/**
	 * Constructor
	 * 
	 * @param c
	 */
	public TNVLocalHostsGraph(TNVCanvas c) {
		super();
		this.canvas = c;
		this.setPickable( false );

		this.sortOrder = TNVPreferenceData.getInstance().getLocalSort();
		
		// Listen for changes in start and end time selections
		String[] listenProps = { TNVModel.PROPERTY_VISIBLE_START_TIME, TNVModel.PROPERTY_VISIBLE_END_TIME };
		TNVModel.getInstance().addPropertyChangeListener( listenProps, new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent evt ) {
				String property = evt.getPropertyName();
				if ( ( property.equals( TNVModel.PROPERTY_VISIBLE_START_TIME ) 
						|| property.equals( TNVModel.PROPERTY_VISIBLE_END_TIME ) )
						&& ( TNVModel.getInstance().getCurrentStartTime() != null 
								&& TNVModel.getInstance().getCurrentEndTime() != null ) ) {
					TNVLocalHostsGraph.this.setupColumnTimes();
				}
			}
		} );

		// Listen for changes in size
		this.addPropertyChangeListener( PROPERTY_BOUNDS, new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent evt ) {
				TNVLocalHostsGraph.this.setupWidths(-1);
				if ( TNVModel.getInstance().getCurrentStartTime() != null &&
						TNVModel.getInstance().getCurrentEndTime() != null )
					TNVLocalHostsGraph.this.setupTimemap();
				TNVLocalHostsGraph.this.layoutChildren();
			}
		});

		// Listen for changes in preferences for column count and height
		TNVPreferenceData.getInstance().addPreferenceChangeListener( new ChangeListener() {
			public void stateChanged( ChangeEvent evt ) {
				if ( numberOfColumns != TNVPreferenceData.getInstance().getColumnCount() ) {
					numberOfColumns = TNVPreferenceData.getInstance().getColumnCount();
					TNVLocalHostsGraph.this.setupWidths(-1);
					Iterator rowIt = TNVLocalHostsGraph.this.getChildrenIterator();
					while ( rowIt.hasNext() ) {
						TNVLocalHost row = (TNVLocalHost) rowIt.next();
						PNode label = row.getChild( 0 );
						PNode inBar = row.getChild( 1 );
						PNode outBar = row.getChild( 2 );
						row.removeAllChildren();
						row.addChild( 0, label );
						row.addChild( 1, inBar );
						row.addChild( 2, outBar );
						for ( int i = 0; i < numberOfColumns; i++ )
							row.addChild( new TNVLocalHostCell( row, TNVLocalHostsGraph.this.canvas, i ) );
						row.validateFullPaint();
					}
					if ( TNVLocalHostsGraph.this.getChildrenCount() != 0 ) {
						TNVLocalHostsGraph.this.setupColumnTimes();
						TNVLocalHostsGraph.this.layoutChildren();
					}
				}
				if ( defaultRowHeight != TNVPreferenceData.getInstance().getRowHeight() )
					TNVLocalHostsGraph.this.layoutChildren();
				if ( TNVPreferenceData.getInstance().getLocalSort() != TNVLocalHostsGraph.this.sortOrder ) {
					TNVLocalHostsGraph.this.sortOrder = TNVPreferenceData.getInstance().getLocalSort();
					TNVLocalHostsGraph.this.sortHosts();
					TNVLocalHostsGraph.this.layoutChildren();
				}
			}
		} );

		// listen for dragging events to reorder the rows
		this.addInputEventListener( new TNVHostDragEventHandler(this)  );

	}


	/* (non-Javadoc)
	 * @see edu.umd.cs.piccolo.PNode#paint(edu.umd.cs.piccolo.util.PPaintContext)
	 */
	@Override
	public void paint( PPaintContext aPaintContext ) {
		Graphics2D g2 = aPaintContext.getGraphics();
		if ( this.columnStartTimes != null ) {
			// draw column labels
			float fontSize = 12;
			g2.setColor( Color.BLACK );
			double x = this.getX();
			double w;
			double y = this.getY() + 1;
			String dateLabel;
			int last = this.columnStartTimes.length - 1;
			for ( int i = 0; i < this.columnWidths.length; i++ ) {
				w = this.columnWidths[i];
				// shorten date format for very small columns
				if ( w < 30 )
					dateLabel = TNVUtil.SHORT_TIME_ONLY_FORMAT.format( new Date( this.columnStartTimes[i] ) );
				else
					dateLabel = TNVUtil.SHORT_FORMAT.format( new Date( this.columnStartTimes[i] ) );
				// set font size according to column width
				if ( w < 35 )
					fontSize = 7;
				else if ( w < 40 ) 
					fontSize = 8;
				else if ( w < 47 ) 
					fontSize = 9;
				else if ( w < 55 ) 
					fontSize = 10;
				else if ( w < 65 ) 
					fontSize = 11;
				else
					fontSize = 12;
				g2.setFont( TNVUtil.LABEL_FONT.deriveFont(fontSize) );
				g2.drawString( dateLabel, 
						(int) (x + ( w / 2 )) - ( g2.getFontMetrics().stringWidth( dateLabel ) / 2 ), 
						(int) y - g2.getFontMetrics().getAscent() + 1);
				g2.fill( new Rectangle2D.Double( x + 1, y - 4, 2, 4 ) ); // draw marker at left
				x += w;
			}
		}
	}


	/**
	 * Clear all data from graph
	 */
	protected void clearGraph( ) {
		this.removeAllChildren();
		this.numberOfHostRows = 0;
		this.columnStartTimes = null;
		this.columnWidths = null;
		this.setupWidths(-1);
		this.timeMap.clear();
		this.repaint();
	}

	
	/**
	 * Setup all local hosts and all nodes to the graph
	 */
	protected void setupHosts() {
		Set hosts = TNVDbUtil.getInstance().getLocalHostMap().keySet();
		numberOfHostRows = hosts.size();

		double defaultHeight = getDefaultHeight();
		Iterator it = hosts.iterator();
		String hostname;
		while ( it.hasNext() ) {
			hostname = (String) it.next();
			TNVLocalHost h = new TNVLocalHost( hostname );
			this.addPropertyChangeListener( h );
			this.addChild( h );
			for ( int i = 0; i < numberOfColumns; i++ )
				h.addChild( new TNVLocalHostCell( h, this.canvas, i ) );
		}
		this.sortHosts();
		this.layoutChildren();
	}


	/**
	 * Get a Host that corresponds to a host string name
	 * @param the requested host name
	 * @return the corresponding host node
	 */
	protected final TNVHost getHostByString( String h ) {
		Iterator it = this.getChildrenIterator();
		while ( it.hasNext() ) {
			TNVHost host = (TNVHost)it.next(); 
			if ( host.getName().equals(h) )
				return host;
		}
		return null;
	}


	/**
	 * Get an x position int for a given timestamp
	 * @param t
	 * @return x position
	 */
	protected int getPositionForTimestamp(Timestamp timestamp) {
		return (timeMap.get( timeMap.headMap( timestamp.getTime() ).lastKey() )).intValue();
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.piccolo.PNode#layoutChildren()
	 */
	protected void layoutChildren(  ) {

		// Focus and collapse height if cell has been expanded (width is done separately)
		double focusHeight = 0;
		if ( this.colsExpanded != 0 && this.rowsExpanded != 0 )
			focusHeight = this.getDefaultHeight() * 2;
			//focusHeight = (this.getHeight() * HEIGHT_FOCUS_SIZE_PERCENT) / this.rowsExpanded;
		double collapsedHeight = (this.getHeight() - 
				(focusHeight * this.rowsExpanded)) / (this.numberOfHostRows - this.rowsExpanded);
		if ( collapsedHeight > this.getDefaultHeight() )
			collapsedHeight = this.getDefaultHeight();
		
		double xOffset = this.getX() + 1;
		double yOffset = this.getY() + 1;
		double height, width;

		TNVLocalHost row = null;
		TNVLocalHostCell cell = null;
		int index;
		Iterator it = this.getChildrenIterator();
		while ( it.hasNext() ) {
			row = (TNVLocalHost) it.next();
			
			height = collapsedHeight;
			if ( row.hasHeightFocus() )
				height = focusHeight;
			
			row.setBounds( xOffset, yOffset, this.getWidth(), height );
			
			// setup label, right justified			
			row.getChild( 0 ).setX( this.getX() + this.getWidth() + HISTOGRAM_WIDTH + 5 );
			row.getChild( 0 ).setY(yOffset);

			// TODO: Exception thrown when changing preferences for bars
			
			// setup ingress/egress bars to right of label
			PNode ingressBar = row.getChild( 1 );
			ingressBar.setX( this.getX() + this.getWidth() + 2 );
			ingressBar.setY( yOffset + 2 );
			ingressBar.setHeight( (height - 4) / 2);
			ingressBar.getChild(0).setX( ingressBar.getX() + ingressBar.getWidth() );
			ingressBar.getChild(0).setY( ingressBar.getY() + (ingressBar.getHeight() / 2) );
			
			PNode egressBar = row.getChild( 2 );
			egressBar.setX( this.getX() + this.getWidth() + 2 );
			egressBar.setY( yOffset + 2 + ((height - 4) / 2) );
			egressBar.setHeight( (height - 4) / 2);			
			egressBar.getChild(0).setX( egressBar.getX() + egressBar.getWidth() );
			egressBar.getChild(0).setY( egressBar.getY() + (egressBar.getHeight() / 4) );
			
			// setup individual cells within the host row
			index = 0;
			Iterator it2 = row.getChildrenIterator();
			while ( it2.hasNext() ) {
				PNode n = (PNode) it2.next();
				if ( n instanceof TNVLocalHostCell ) {
					cell = (TNVLocalHostCell) n;
					width = columnWidths[index++];
					cell.setBounds( xOffset, yOffset, width, height );
					xOffset += width;
				}
			}
			xOffset = this.getX() + 1;
			yOffset += height;
		}
	}


	/**
	 * Setup the times for each column, called whenever the start/end times are changed 
	 */
	protected void setupColumnTimes( ) {
		canvas.clearSelectedNodes(); // Clear selected host nodes first
		this.columnStartTimes = new long[numberOfColumns];
		long thisTime = TNVModel.getInstance().getCurrentStartTime().getTime();
		this.columnTimeInterval = ( TNVModel.getInstance().getCurrentEndTime().getTime() - thisTime ) / numberOfColumns;
		for ( int i = 0; i < numberOfColumns; i++ ) {
			this.columnStartTimes[i] = thisTime;
			thisTime += this.columnTimeInterval;
		}
		
		this.setupTimemap();
		
		// tell listeners (hosts) to update
		this.firePropertyChange( PROPERTY_CODE_CLIENT_PROPERTIES, TNVLocalHostsGraph.PROPERTY_COLUMN_START_TIMES, null,
				this.columnStartTimes );

		// repaint column times
		this.canvas.repaint( 0, 0, this.canvas.getWidth(), TNVCanvas.TOP_LABEL_HEIGHT );
	}

	
	/**
	 * Set a HostCellNode to expand or collapse
	 * @param node
	 * @param setFocus
	 */
	protected void setFocusNode(TNVLocalHostCell node, boolean setFocus) {
		int rowIndex = this.indexOfChild( node.getParent() );
		int colIndex = node.getColumnIndex();
		
		// Reset to 0
		this.clearFocusNodes();
		
		TNVLocalHost row;
		TNVLocalHostCell cell;

		if ( setFocus ) {
			this.colsExpanded=1;
			this.rowsExpanded=1;
			int i = 0;
			Iterator it = this.getChildrenIterator();
			while ( it.hasNext() ) {
				row = (TNVLocalHost) it.next();
				if ( i == rowIndex ) {
					row.setHeightFocus(true);
				}
				Iterator it2 = row.getChildrenIterator();
				while ( it2.hasNext() ) {
					PNode n = (PNode) it2.next();
					if ( n instanceof TNVLocalHostCell ) {
						cell = (TNVLocalHostCell) n;
						if ( cell.getColumnIndex() == colIndex ) 
							cell.setWidthFocus(true);
						if ( i == rowIndex )
							cell.setHeightFocus(true);
					}
				}
				i++;
			}
			this.setupWidths(colIndex);
		}
		else
			this.setupWidths(-1);

		this.setupTimemap();
		
		this.layoutChildren();
		
		// get all nodes to recalculate packet locations
		this.firePropertyChange( PROPERTY_CODE_CLIENT_PROPERTIES, TNVLocalHostsGraph.PROPERTY_FOCUS_CHANGED,
				false, true );
	}
//TODO: MULTIPLE FOCUS NODES
//	protected void setFocusNode(LocalHostCell node, boolean setFocus) {
//		int rowIndex = this.indexOfChild( node.getParent() );
//		int colIndex = node.getColumnIndex();
//
//		if ( ! setFocus ) {
//			this.colsExpanded--;
//			this.rowsExpanded--;
//		} 
//		else {
//			this.colsExpanded++;
//			this.rowsExpanded++;
//		}
//				
//		LocalHost row;
//		LocalHostCell cell;
//		int i = 0;
//		Iterator it = this.getChildrenIterator();
//		while ( it.hasNext() ) {
//			row = (LocalHost) it.next();
//			if ( i == rowIndex )
//				row.setHeightFocus(setFocus);			
//			Iterator it2 = row.getChildrenIterator();
//			while ( it2.hasNext() ) {
//				PNode n = (PNode) it2.next();
//				if ( n instanceof LocalHostCell ) {
//					cell = (LocalHostCell) n;
//					if ( cell.getColumnIndex() == colIndex ) 
//						cell.setWidthFocus(setFocus);
//					if ( i == rowIndex )
//						cell.setHeightFocus(setFocus);
//				}
//			}
//			i++;
//		}
//
//		this.animate = true;
//		this.layoutChildren();
//		this.animate = false;
//	}
	
	private void clearFocusNodes() {
		TNVLocalHost row;
		TNVLocalHostCell cell;
		this.colsExpanded=0;
		this.rowsExpanded=0;
		Iterator it = this.getChildrenIterator();
		while ( it.hasNext() ) {
			row = (TNVLocalHost) it.next();
			row.setHeightFocus(false);
			Iterator it2 = row.getChildrenIterator();
			while ( it2.hasNext() ) {
				PNode n = (PNode) it2.next();
				if ( n instanceof TNVLocalHostCell ) {
					cell = (TNVLocalHostCell) n;
					cell.setWidthFocus(false);
					cell.setHeightFocus(false);
				}
			}
		}
	}
	
	/**
	 * setup the column widths
	 * @param focusColumnIndex the index of the column to gain the focus or -1 for none
	 */
	private void setupWidths(int focusColumnIndex) {
		columnWidths = new double[numberOfColumns];
		double defaultWidth = this.getWidth() / this.numberOfColumns;
		double focusWidth, collapsedWidth;
		if ( focusColumnIndex == -1 ) {
			this.clearFocusNodes();
			focusWidth = defaultWidth;
			collapsedWidth = defaultWidth;
		}
		else {
			focusWidth = (this.getWidth() * WIDTH_FOCUS_SIZE_PERCENT) / this.colsExpanded;
			collapsedWidth = (this.getWidth() - 
					(focusWidth * this.colsExpanded)) / (this.numberOfColumns - this.colsExpanded);
		}
		for ( int i = 0 ; i < numberOfColumns ; i++ ) {
			if ( focusColumnIndex == i )
				columnWidths[i] = focusWidth;
			else
				columnWidths[i] = collapsedWidth;	
		}
	}
	
	/**
	 * Setup the x position to time mapping 
	 */
	private void setupTimemap() {
		timeMap.clear();
		long startTime, endTime;
		int interval, width;
		int x = (int) this.getX();
		for ( int i = 0 ; i < numberOfColumns ; i++ ) {
			startTime = this.columnStartTimes[i];
			endTime = startTime + this.columnTimeInterval;
			width = (int) this.columnWidths[i];
			interval = (int) ((endTime - startTime) / width);
			for ( int j = x; j < (x + width) ; j++ ) {
				timeMap.put( new java.lang.Long(startTime), new java.lang.Integer(j) );
				startTime += interval;
			}
			x += width;
		}
	}
	
	/**
	 * Get the default row height, based on preferences and size of window
	 * @return the default height for a row
	 */
	private double getDefaultHeight() {
		double defaultHeight = TNVPreferenceData.getInstance().getRowHeight();
		double maximumHeight = this.getHeight() / numberOfHostRows;
		if ( defaultHeight > maximumHeight )
			defaultHeight = maximumHeight;
		this.defaultRowHeight = defaultHeight;
		return defaultHeight;
	}


	/**
	 * Sort hosts 
	 */
	private void sortHosts() {
		List<TNVHost> hosts = this.getChildrenReference();
		
		if ( this.sortOrder == TNVPreferenceData.SORT_ARRIVAL ) {
			Collections.sort(hosts, new Comparator<TNVHost>() {
				public int compare(TNVHost s1, TNVHost s2) {
					List arrivalList = TNVDbUtil.getInstance().getLocalHostArrivalList();
					if ( arrivalList.indexOf(s1.getName()) > arrivalList.indexOf(s2.getName()) )
						return 1;
					return -1;
				}
			});
		}
		else if ( this.sortOrder == TNVPreferenceData.SORT_ARRIVAL_REVERSE ) {
			Collections.sort(hosts, new Comparator<TNVHost>() {
				public int compare(TNVHost s1, TNVHost s2) {
					List arrivalList = TNVDbUtil.getInstance().getLocalHostArrivalList();
					if ( arrivalList.indexOf(s1.getName()) < arrivalList.indexOf(s2.getName()) )
						return 1;
					return -1;
				}
			});
		} 
		else if ( this.sortOrder == TNVPreferenceData.SORT_ALPHA ) {
			Collections.sort(hosts, TNVUtil.ALPHA_HOST_COMPARATOR);
		} 
		else if ( this.sortOrder == TNVPreferenceData.SORT_ALPHA_REVERSE ) {
			Collections.sort(hosts, TNVUtil.ALPHA_HOST_COMPARATOR);
			Collections.reverse(hosts);
		} 
		

	}

	
	/**
	 *
	 * @return the long[] TNVLocalHostsGraph.java
	 */
	protected final long[] getColumnStartTimes() {
		return this.columnStartTimes;
	}


	/**
	 *
	 * @return the long TNVLocalHostsGraph.java
	 */
	protected final long getColumnTimeInterval() {
		return this.columnTimeInterval;
	}


	/**
	 *
	 * @return the double[] TNVLocalHostsGraph.java
	 */
	protected final double[] getColumnWidths() {
		return this.columnWidths;
	}


}
