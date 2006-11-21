/**
 * Created on Apr 13, 2004 
 * @author jgood
 * 
 * Piccolo PNode for Local Host Row representations
 */
package net.sourceforge.tnv;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * LocalHost
 */
public class TNVLocalHost extends TNVHost implements PropertyChangeListener {

	private final static Color INGRESS_COLOR = new Color(127, 201, 127);
	private final static Color EGRESS_COLOR = new Color(190, 174, 212);
	
	private PText label;				// label for this host

	// packet summary map
	private SortedMap<Timestamp, List<TNVPacket>> packetMap = new TreeMap<Timestamp, List<TNVPacket>>();
	
	private boolean selected = false;	// if this host has edges selected

	private boolean heightFocus = false;

	//private int ingressCount, egressCount;
	
	/**
	 * Constructor
	 * @param n host name
	 */
	public TNVLocalHost(String n) {
		super();
		this.setName( n );
		
		this.label = new PText( n );
		this.label.setFont( TNVUtil.LABEL_FONT );
		this.addChild( 0, this.label );
		
		// ingress and egress histogram to the right of cells, minimum size of 2
		int ingressCount = TNVDbUtil.getInstance().getPacketCount(n, TNVLinkNode.LinkDirection.INCOMING);
		int egressCount = TNVDbUtil.getInstance().getPacketCount(n, TNVLinkNode.LinkDirection.OUTGOING);
		int totalCount = TNVDbUtil.getInstance().getTotalPacketCount();
		
		PNode ingressBar = new PNode();
		ingressBar.setPaint(INGRESS_COLOR);
		double ingressWidth, egressWidth;
		if ( ingressCount > 0 )
			ingressWidth = (((double)ingressCount/totalCount) * TNVLocalHostsGraph.HISTOGRAM_WIDTH) + 2;
		else
			ingressWidth = 0;
		ingressBar.setWidth( ingressWidth );
		ingressBar.setPickable(false);
		PText ingressLabel = new PText(ingressCount + "");
		ingressLabel.setFont(new Font( "SanSerif", Font.PLAIN, 8 ));
		ingressLabel.setPickable(false);
		ingressBar.addChild(ingressLabel);
		this.addChild(1, ingressBar);
		
		PNode egressBar = new PNode();
		egressBar.setPaint(EGRESS_COLOR);
		if ( egressCount > 0 )
			egressWidth = (((double)egressCount/totalCount) * TNVLocalHostsGraph.HISTOGRAM_WIDTH) + 2;
		else
			egressWidth = 0;
		egressBar.setWidth( egressWidth );
		egressBar.setPickable(false);
		PText egressLabel = new PText(egressCount + "");
		egressLabel.setFont(new Font( "SanSerif", Font.PLAIN, 8 ));
		egressLabel.setPickable(false);
		egressBar.addChild(egressLabel);
		this.addChild(2, egressBar);
		
		// Listen for changes in size
		this.addPropertyChangeListener( PROPERTY_FULL_BOUNDS, new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent evt ) {
				TNVLocalHost.this.validateFullBounds();
			}
		});
		this.addPropertyChangeListener( PROPERTY_BOUNDS, new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent evt ) {
				TNVLocalHost.this.validateFullBounds();
			}
		});
	
	}


	/* (non-Javadoc)
	 * @see edu.umd.cs.piccolo.PNode#paint(edu.umd.cs.piccolo.util.PPaintContext)
	 */
	@Override
	public void paint( PPaintContext aPaintContext ) {
		Graphics2D g2 = aPaintContext.getGraphics();
		
		// draw highlighted host border
		if ( this.selected ) {
			g2.setPaint( Color.BLUE );
			g2.draw( new Rectangle2D.Double( this.getX() + 1, this.getY() + 1, 
					this.getWidth() - 2, this.getHeight() - 2 ) );
		}
		
	}


	/**
	 * @param selected the boolean selected to set
	 */
	protected final void setSelected( boolean selected ) {
		this.selected = selected;
		this.repaint();
	}
	
	/**
	 * @param selected the boolean selected to set
	 */
	protected final void setSelectedLink( boolean selected ) {
		this.selected = selected;
		this.repaint();
	}

	/**
	 * @return Returns the heightFocus.
	 */
	protected final boolean hasHeightFocus( ) {
		return this.heightFocus;
	}

	/**
	 * @param heightFocus The heightFocus to set.
	 */
	protected final void setHeightFocus( boolean heightFocus ) {
		this.heightFocus = heightFocus;
	}

	/**
	 * @param evt The PropertyChangeEvent to listen for.
	 */
	public void propertyChange( PropertyChangeEvent evt ) {
		String property = evt.getPropertyName();
		if ( property.equals( TNVLocalHostsGraph.PROPERTY_FOCUS_CHANGED ) ) {
			TNVLocalHost.this.repaint();
		}
		else if ( property.equals( TNVLocalHostsGraph.PROPERTY_COLUMN_START_TIMES ) ) {
			long[] columnStartTimes = (long[]) evt.getNewValue();
			long timeInterval = ( TNVModel.getInstance().getCurrentEndTime().getTime() - 
					TNVModel.getInstance().getCurrentStartTime().getTime() ) / columnStartTimes.length;
			
			int i = 0;
			Timestamp startTimestamp, endTimestamp;
			Iterator it = this.getChildrenIterator();
			while ( it.hasNext() ) {
				PNode node = (PNode) it.next();
				if ( node instanceof TNVLocalHostCell ) {
					TNVLocalHostCell cell = (TNVLocalHostCell) node;
					startTimestamp = new Timestamp( columnStartTimes[i] );
					endTimestamp = new Timestamp( columnStartTimes[i] + timeInterval - 1 );
					cell.setStartTime( startTimestamp );
					cell.setEndTime( endTimestamp );
					try {
						int freq = 0;
						Iterator packetListIt = TNVDbUtil.getInstance().getLocalHostMap(this.getName()).subMap( 
								startTimestamp, endTimestamp ).values().iterator();
						while ( packetListIt.hasNext() ) 
							freq += ((List) packetListIt.next()).size();
						cell.setFrequency(freq);
					}
					catch (Exception e) {
						cell.setFrequency(0);
					}
					i++;
				}
			}
			TNVLocalHost.this.validateFullPaint();
		}
	}

}
