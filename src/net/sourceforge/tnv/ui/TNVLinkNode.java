/**
 * Created on Feb 17, 2006 
 * @author jgood
 * 
 * Piccolo PNode for Link representations
 */
package net.sourceforge.tnv.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Stack;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.jpcap.net.IPProtocol;
import net.sourceforge.jpcap.net.IPProtocols;
import net.sourceforge.tnv.TNV;
import net.sourceforge.tnv.util.TNVUtil;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;

/**
 * LinkNode
 */
public class TNVLinkNode extends PPath {

    /**
     * Direction of this link
     */
    public static enum LinkDirection {
        INCOMING, OUTGOING, LOCAL;
    }

    /**
     * Direction of this link
     */
    private static enum Selection {
        RESET, HIGHLIGHT, SELECT, HOVER;
    }
	
	// default visual attributes
	private static final float HIGHLIGHTED_TRANSPARENCY = 0.8f;
	private static final float SELECTED_TRANSPARENCY = 0.9f;
	private static final Stroke SELECTED_STROKE = new BasicStroke( 3f );
	private static final float HOVERED_TRANSPARENCY = 1f;
	private static final Stroke HOVERED_STROKE = new BasicStroke( 4.5f );
	
	private Stack<Float> transparencyStack = new Stack<Float>(); 
	private Stack<Stroke> strokeStack = new Stack<Stroke>(); 
	
	// the network link attributes
	private TNVHost src, dst;
	private int protocol;
	private String shortProtocol;
	private int frequency = 0;
	private LinkDirection direction;

	// the start and end points of the line
	private Point2D startPoint, endPoint;

	// if the links are straight (false) or curved (true)
	private boolean isCurvedLinks; 
	
	// tooltip (shown on top layer)
	private PText tooltip;

	// default string to show in tooltip
	private String tooltipText;
	private Color tooltipColor =  new Color( 0, 225, 250 );

    private float defaultTransparency = 0.35f;
	private Stroke defaultStroke = new BasicStroke( 1f );
	
	
	/**
	 * Constructor
	 * @param s the source host
	 * @param d the destination host
	 * @param p the jpcap protocol int
	 * @param f the frequency (count) of this link
	 * @param dir the direction (incoming or outgoing or local)
	 */
	public TNVLinkNode(TNVHost s, TNVHost d, int p, int f, LinkDirection dir) {
		super();
		this.src = s;
		this.dst = d;
		this.protocol = p;
		this.frequency = f;
		this.direction = dir;

		this.setPickable( true );

		// set the short protocolo
		this.shortProtocol = IPProtocol.getDescription( this.protocol );
		if ( this.protocol == IPProtocols.TCP )
			this.shortProtocol = "TCP";
		else if ( this.protocol == IPProtocols.UDP )
			this.shortProtocol = "UDP";
		else if ( this.protocol == IPProtocols.ICMP ) 
			this.shortProtocol = "ICMP";

		// create tooltip, to show when mouse enters this link node
		if ( direction.equals( LinkDirection.OUTGOING ) )
			this.tooltipText = this.dst.getName() + " < " + this.src.getName();
		else
			this.tooltipText = this.src.getName() + " > " + this.dst.getName();
		this.tooltipText += ": " + this.frequency + " " + this.shortProtocol + "   ";
		this.tooltip = (PText) TNVUtil.DEFAULT_TOOLTIP_NODE.clone();
		this.tooltip.setText( this.tooltipText );
		this.tooltip.setPaint( tooltipColor );
		
		// set stroke according to frequency
		if ( this.frequency > 1000 )
			this.defaultStroke = new BasicStroke( 3f );
		else if ( this.frequency > 100 )
			this.defaultStroke = new BasicStroke( 2f );
		this.setStroke( this.defaultStroke );
		
		// set color according to protocol
		this.setStrokePaint( TNVPreferenceData.getInstance().getIcmpColor() );
		if ( this.protocol == IPProtocols.TCP )
			this.setStrokePaint( TNVPreferenceData.getInstance().getTcpColor() );
		else if ( this.protocol == IPProtocols.UDP )
			this.setStrokePaint( TNVPreferenceData.getInstance().getUdpColor() );

		// set transparency
		this.setTransparency( this.defaultTransparency );

		// set to curved or straight
		this.isCurvedLinks = TNVPreferenceData.getInstance().isCurvedLinks();
		
		// update edge position
		this.updateEdgeBounds();

		// listen for changes in bounds of src and dst to update link position
		this.src.addPropertyChangeListener( PNode.PROPERTY_BOUNDS, new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent evt ) {
				TNVLinkNode.this.updateEdgeBounds();
			}
		} );
		this.dst.addPropertyChangeListener( PNode.PROPERTY_BOUNDS, new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent evt ) {
				TNVLinkNode.this.updateEdgeBounds();
			}
		} );

		// listen for selection to host nodes and increase opacity
		this.src.addPropertyChangeListener( TNVHost.PROPERTY_SELECTED_NODE, new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent evt ) {
				TNVLinkNode.this.setSelected( ((Boolean)evt.getNewValue()).booleanValue() );
			}
		} );
		this.dst.addPropertyChangeListener( TNVHost.PROPERTY_SELECTED_NODE, new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent evt ) {
				TNVLinkNode.this.setSelected( ((Boolean)evt.getNewValue()).booleanValue() );
			}
		} );

		// listen for mouse movements to handle tooltips
		this.addInputEventListener( new PBasicInputEventHandler() {
			@Override
			public void mouseEntered( PInputEvent event ) {
				if ( event.getButton() == MouseEvent.NOBUTTON && TNVLinkNode.this.getVisible() ) {
					TNV.setCrosshairCursor();
					if ( TNVPreferenceData.getInstance().isShowTooltips() ) {
						Point2D cursorPoint = event.getCanvasPosition();
						TNVLinkNode.this.tooltip.setOffset( 
								cursorPoint.getX() - ( TNVLinkNode.this.tooltip.getWidth() / 4 ),
								cursorPoint.getY() - 20 );
						event.getCamera().getLayer( TNVCanvas.TOOLTIP_LAYER ).addChild( TNVLinkNode.this.tooltip );
						
						TNVLinkNode.this.setHovered(true);
						
						// highlight hosts
						PNode srcNode = new PNode();
						srcNode.setBounds( src.getBoundsReference().getFrame() );
						srcNode.setPaint( tooltipColor );
						srcNode.setTransparency( 0.2f );
						event.getCamera().getLayer( TNVCanvas.TOOLTIP_LAYER ).addChild( srcNode );
						
						PNode dstNode = new PNode();
						dstNode.setBounds( dst.getBoundsReference().getFrame() );
						dstNode.setPaint( tooltipColor );
						dstNode.setTransparency( 0.2f );
						event.getCamera().getLayer( TNVCanvas.TOOLTIP_LAYER ).addChild( dstNode );
						
						event.setHandled( true );
					}
				}
			}
			@Override
			public void mouseExited( PInputEvent event ) {
				if ( event.getButton() == MouseEvent.NOBUTTON ) {
					TNV.setDefaultCursor();
					if ( TNVPreferenceData.getInstance().isShowTooltips() ) {
						event.getCamera().getLayer( TNVCanvas.TOOLTIP_LAYER ).removeAllChildren();
						TNVLinkNode.this.setHovered(false);
						event.setHandled( true );
					}
				}
			}
		} );

		// listen for changes to link filtering and highlighting
		TNVModel.getInstance().addPropertyChangeListener( new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent evt ) {
				String property = evt.getPropertyName();
				if ( property.equals( TNVModel.PROPERTY_LINK_DIRECTION ) ) 
					TNVLinkNode.this.updateEdgeBounds();
				
				else if ( property.equals( TNVModel.PROPERTY_SHOW_TCP_LINKS ) 
						&& TNVLinkNode.this.protocol == IPProtocols.TCP ) 
					TNVLinkNode.this.setVisible( ((Boolean)evt.getNewValue()).booleanValue() );
				else if ( property.equals( TNVModel.PROPERTY_SHOW_UDP_LINKS )
						&& TNVLinkNode.this.protocol == IPProtocols.UDP ) 
					TNVLinkNode.this.setVisible( ((Boolean)evt.getNewValue()).booleanValue() );
				else if ( property.equals( TNVModel.PROPERTY_SHOW_ICMP_LINKS ) 
						&& TNVLinkNode.this.protocol == IPProtocols.ICMP )
					TNVLinkNode.this.setVisible( ((Boolean)evt.getNewValue()).booleanValue() );
				
				else if ( property.equals( TNVModel.PROPERTY_HIGHLIGHT_TCP_LINKS ) 
						&& TNVLinkNode.this.protocol == IPProtocols.TCP ) 
					setHighlighted( ((Boolean)evt.getNewValue()).booleanValue() );
				else if ( property.equals( TNVModel.PROPERTY_HIGHLIGHT_UDP_LINKS )
						&& TNVLinkNode.this.protocol == IPProtocols.UDP ) 
					setHighlighted( ((Boolean)evt.getNewValue()).booleanValue() );
				else if ( property.equals( TNVModel.PROPERTY_HIGHLIGHT_ICMP_LINKS ) 
						&& TNVLinkNode.this.protocol == IPProtocols.ICMP )
					setHighlighted( ((Boolean)evt.getNewValue()).booleanValue() );
			}
		} );
		
		// listen for preference changes
		TNVPreferenceData.getInstance().addPreferenceChangeListener( new ChangeListener() {
			public void stateChanged( ChangeEvent evt ) {
				if ( TNVLinkNode.this.isCurvedLinks != TNVPreferenceData.getInstance().isCurvedLinks() ) {
					TNVLinkNode.this.isCurvedLinks = TNVPreferenceData.getInstance().isCurvedLinks();
					TNVLinkNode.this.updateEdgeBounds();
				}
			}
		} );

		// For newly drawn links, check filters and highlighting
		if ( ! TNVModel.getInstance().isShowTCPlinks()
				&& TNVLinkNode.this.protocol == IPProtocols.TCP ) 
			TNVLinkNode.this.setVisible( false );
		if ( ! TNVModel.getInstance().isShowUDPlinks()
				&& TNVLinkNode.this.protocol == IPProtocols.UDP ) 
			TNVLinkNode.this.setVisible( false );
		if ( ! TNVModel.getInstance().isShowICMPlinks()
				&& TNVLinkNode.this.protocol == IPProtocols.ICMP )
			TNVLinkNode.this.setVisible( false );
		
		if ( ! TNVModel.getInstance().isHighlightTCPlinks()
				&& TNVLinkNode.this.protocol == IPProtocols.TCP ) 
			setHighlighted( false );
		if ( ! TNVModel.getInstance().isHighlightUDPlinks()
				&& TNVLinkNode.this.protocol == IPProtocols.UDP ) 
			setHighlighted( false );
		if ( ! TNVModel.getInstance().isHighlightICMPlinks()
				&& TNVLinkNode.this.protocol == IPProtocols.ICMP )
			setHighlighted( false );

	}


	/**
	 * @param hover
	 */
	private void setHovered(boolean hover) {
		if ( hover )
			this.setVisualAttributes(Selection.HOVER);
		else
			this.setVisualAttributes(Selection.RESET);
	}

	/**
	 * @param highlight
	 */
	private void setHighlighted(boolean highlight) {
		if ( highlight )
			this.setVisualAttributes(Selection.HIGHLIGHT);
		else
			this.setVisualAttributes(Selection.RESET);
	}

	/**
	 * @param selected the selected to set
	 */
	public final void setSelected( boolean selected ) {
		this.src.setSelectedLink( selected );
		this.dst.setSelectedLink( selected );
		if ( selected )
			this.setVisualAttributes(Selection.SELECT);
		else
			this.setVisualAttributes(Selection.RESET);
	}

	/**
	 * Set and update location based on src and dst hosts, offset slightly for direction and protocol
	 * Links are drawn either as a quad curve or straight line segment
	 */
	private void updateEdgeBounds( ) {
		this.reset();
		
		float protocolOffset = 0;
		if ( this.protocol == IPProtocols.TCP )
			protocolOffset = 1.5f;
		else if ( this.protocol == IPProtocols.UDP )
			protocolOffset = -1.5f;

		float startX, startY, endX, endY, controlX, controlY;
		float controlXoffset = 35, controlYoffset = 90;
		TNVModel.DisplayLinkDirection modelDirection = TNVModel.getInstance().getLinkDirection();
		if ( direction.equals( LinkDirection.OUTGOING ) && 
				( modelDirection == TNVModel.DisplayLinkDirection.EGRESS ||
				modelDirection == TNVModel.DisplayLinkDirection.BOTH )
			) {
			startX = (float) this.src.getX();
			startY = (float) ( this.src.getY() + ( this.src.getHeight() / 2 ) ) - 1 + protocolOffset;
			endX = (float) ( this.dst.getX() + this.dst.getWidth() );
			endY = (float) ( this.dst.getY() + ( this.dst.getHeight() / 2 ) ) - 1 + protocolOffset;
			// midpoint +/- value
			controlX = ((endX + startX) / 2) + controlXoffset;
			controlY = ((endY + startY) / 2) - controlYoffset;
		}
		else if ( direction.equals( LinkDirection.INCOMING ) && 
				( modelDirection == TNVModel.DisplayLinkDirection.INGRESS ||
						modelDirection == TNVModel.DisplayLinkDirection.BOTH )
			) {
			startX = (float) ( this.src.getX() + this.src.getWidth() );
			startY = (float) ( this.src.getY() + ( this.src.getHeight() / 2 ) ) + 1 + protocolOffset;
			endX = (float) this.dst.getX();
			endY = (float) ( this.dst.getY() + ( this.dst.getHeight() / 2 ) ) + 1 + protocolOffset;
			controlX = ((endX + startX) / 2) - controlXoffset;
			controlY = ((endY + startY) / 2) + controlYoffset;
		}
		else if ( direction.equals( LinkDirection.LOCAL ) ) {
			startX = (float) this.src.getX();
			startY = (float) ( this.src.getY() + ( this.src.getHeight() / 2 ) ) + protocolOffset;
			endX = (float) this.dst.getX();
			endY = (float) ( this.dst.getY() + ( this.dst.getHeight() / 2 ) ) + protocolOffset;
			controlX = endX - 80;
			if ( startY > endY )
				controlY = endY + ((startY - endY) /2) - 25;
			else
				controlY = startY + ((endY - startY) /2) - 25;
		}
		else
			return;
		
		// move to starting point
		this.moveTo( startX, startY );

		// quadratic curve control point coordinates
		if ( this.isCurvedLinks || direction.equals( LinkDirection.LOCAL ) )
			this.quadTo( controlX, controlY, endX, endY );
		
		// line segement to end point
		this.lineTo( endX, endY );
		
	}


	/**
	 * Set the stroke and transparency and save previous state to stack to reset
	 * @param type
	 */
	private final void setVisualAttributes(Selection type) {
		if ( type.equals(Selection.HIGHLIGHT) ) {
			this.transparencyStack.push(this.getTransparency());
			this.setTransparency( HIGHLIGHTED_TRANSPARENCY );
		}
		else if ( type.equals(Selection.SELECT) ) {
			this.transparencyStack.push(this.getTransparency());
			this.strokeStack.push(this.getStroke());
			this.setTransparency( SELECTED_TRANSPARENCY );
			this.setStroke( SELECTED_STROKE );
		}
		else if ( type.equals(Selection.HOVER) ) {
			this.transparencyStack.push(this.getTransparency());
			this.strokeStack.push(this.getStroke());
			this.setTransparency( HOVERED_TRANSPARENCY );
			this.setStroke( HOVERED_STROKE );
		}
		else {
			if ( ! this.transparencyStack.isEmpty() )
				this.setTransparency(this.transparencyStack.pop());
			if ( ! this.strokeStack.isEmpty() )
				this.setStroke(this.strokeStack.pop());
		}
		this.repaint();
	}
	
	/**
	 * @return the src
	 */
	public final TNVHost getSrc( ) {
		return this.src;
	}

	/**
	 * @return the dst
	 */
	public final TNVHost getDst( ) {
		return this.dst;
	}

	/**
	 * @return the protocol
	 */
	public final int getProtocol( ) {
		return this.protocol;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals( Object other ) {
		if ( getClass() == other.getClass() ) {
			TNVLinkNode otherLink = (TNVLinkNode) other;
			//TODO: src/dst pair
			if ( ( ( this.src.equals( otherLink.getSrc() ) && this.dst.equals( otherLink.getDst() ) )
					|| ( this.src.equals( otherLink.getDst() ) && this.dst.equals( otherLink.getSrc() ) ) )
					&& this.protocol == otherLink.getProtocol()  ) return true;
		}
		return false;
	}


	/* (non-Javadoc)
	 * @see edu.umd.cs.piccolo.PNode#toString()
	 */
	@Override
	public String toString( ) {
		return this.src.getName() + " > " + this.dst.getName() + ": " + super.toString();
	}

}
