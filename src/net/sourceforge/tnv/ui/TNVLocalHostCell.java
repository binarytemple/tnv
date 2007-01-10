/**
 * Created on Feb 17, 2006
 * @author jgood
 * 
 * Piccolo PNode for individual cell within a host row representations
 */
package net.sourceforge.tnv.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.jpcap.net.IPProtocols;
import net.sourceforge.tnv.TNV;
import net.sourceforge.tnv.db.TNVDbUtil;
import net.sourceforge.tnv.util.TNVUtil;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * LocalHostCell
 */
public class TNVLocalHostCell extends PNode {

	/**
	 * The parent (owner) host row
	 */
	private TNVLocalHost host;
	
	/**
	 * The start and end timestamps for this cell
	 */
	private Timestamp startTime, endTime;
	
	/**
	 * The frequency count of packets represented by this cell
	 */
	private int frequency;
	
	/**
	 * List of ports, built during paint, used in tooltip
	 */
	private SortedSet<Integer> 
				tcpPortSet = new TreeSet<Integer>(),
				udpPortSet = new TreeSet<Integer>();
	
	/**
	 * Column index of this cell within the row  
	 */
	private int columnIndex;
	
	/**
	 * if this cell node has width focus (expanded)
	 */
	private boolean widthFocus = false;

	/**
	 * if this cell node has height focus (expanded)
	 */
	private boolean heightFocus = false;

	/**
	 * tooltip (shown on top layer)
	 */
	private PText tooltip = new PText();

	/**
	 * if this node is currently selected by the user
	 */ 
	private boolean selected = false;

	/**
	 * whether to show packets or flags, based on preferences 
	 */
	private boolean showPackets = TNVPreferenceData.getInstance().isShowPackets(), 
		showFlags = TNVPreferenceData.getInstance().isShowFlags();
	
	/**
	 * whether or not to paint packets - this is false when time is adjusting 
	 */
	private boolean paintPackets = true;
	
	/**
	 * parent canvas, for getting camera
	 */
	private TNVCanvas canvas;
	
	/**
	 * Constructor
	 * @param h
	 * @param c
	 */
	public TNVLocalHostCell(TNVLocalHost h, TNVCanvas c, int i) {
		super();
		this.host = h;
		this.canvas = c;
		this.columnIndex = i;
		
		this.tooltip = (PText) TNVUtil.DEFAULT_TOOLTIP_NODE.clone();
		
		TNVModel.getInstance().addPropertyChangeListener(  new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent evt ) {
				String property = evt.getPropertyName();
				if ( property.equals(TNVModel.PROPERTY_IS_TIME_ADJUSTING) ) {
					if ( ((Boolean)evt.getNewValue()).booleanValue() == false ) 
						TNVLocalHostCell.this.paintPackets = true;
					else
						TNVLocalHostCell.this.paintPackets = false;
					TNVLocalHostCell.this.repaint();
				}

				if ( property.equals( TNVModel.PROPERTY_SHOW_TCP_PACKETS )
						|| property.equals( TNVModel.PROPERTY_SHOW_UDP_PACKETS )
						|| property.equals( TNVModel.PROPERTY_SHOW_ICMP_PACKETS )
						|| property.equals( TNVModel.PROPERTY_SHOW_PORTS )
						|| property.equals( TNVModel.PROPERTY_SHOW_PORT_TYPE ) 
						|| property.equals( TNVModel.PROPERTY_SHOW_TTL_VALUE ) 
						|| property.equals( TNVModel.PROPERTY_SHOW_TTL_MODIFIER )
						|| property.equals( TNVModel.PROPERTY_SHOW_LENGTH_VALUE ) 
						|| property.equals( TNVModel.PROPERTY_SHOW_LENGTH_MODIFIER)

						|| property.equals( TNVModel.PROPERTY_HIGHLIGHT_TCP_PACKETS )
						|| property.equals( TNVModel.PROPERTY_HIGHLIGHT_UDP_PACKETS )
						|| property.equals( TNVModel.PROPERTY_HIGHLIGHT_ICMP_PACKETS )
						|| property.equals( TNVModel.PROPERTY_HIGHLIGHT_SYN_PACKETS )
						|| property.equals( TNVModel.PROPERTY_HIGHLIGHT_ACK_PACKETS )
						|| property.equals( TNVModel.PROPERTY_HIGHLIGHT_FIN_PACKETS )
						|| property.equals( TNVModel.PROPERTY_HIGHLIGHT_PSH_PACKETS )
						|| property.equals( TNVModel.PROPERTY_HIGHLIGHT_URG_PACKETS )
						|| property.equals( TNVModel.PROPERTY_HIGHLIGHT_RST_PACKETS )
						|| property.equals( TNVModel.PROPERTY_HIGHLIGHT_PORTS )
						|| property.equals( TNVModel.PROPERTY_HIGHLIGHT_PORT_TYPE ) 
						|| property.equals( TNVModel.PROPERTY_HIGHLIGHT_TTL_VALUE ) 
						|| property.equals( TNVModel.PROPERTY_HIGHLIGHT_TTL_MODIFIER )
						|| property.equals( TNVModel.PROPERTY_HIGHLIGHT_LENGTH_VALUE ) 
						|| property.equals( TNVModel.PROPERTY_HIGHLIGHT_LENGTH_MODIFIER) ) 
					TNVLocalHostCell.this.repaint();			
			}
		} );

		// listen for changes in preferences
		TNVPreferenceData.getInstance().addPreferenceChangeListener( new ChangeListener() {
			public void stateChanged( ChangeEvent evt ) {
				if ( showPackets != TNVPreferenceData.getInstance().isShowPackets() ) {
					showPackets = TNVPreferenceData.getInstance().isShowPackets();
					TNVLocalHostCell.this.repaint();
				}
				else if ( showFlags != TNVPreferenceData.getInstance().isShowFlags() ) {
					showFlags = TNVPreferenceData.getInstance().isShowFlags();
					TNVLocalHostCell.this.repaint();
				}
			}
		} );

		// listen for mouse movements to handle tooltips
		this.addInputEventListener( new PBasicInputEventHandler() {
			@Override
			public void mouseEntered( PInputEvent event ) {
				if ( event.getButton() == MouseEvent.NOBUTTON && TNVLocalHostCell.this.frequency > 0 ) {
					TNV.setCrosshairCursor();
					if ( TNVPreferenceData.getInstance().isShowTooltips() ) {
						event.getCamera().getLayer( TNVCanvas.TOOLTIP_LAYER ).addChild( TNVLocalHostCell.this.tooltip );
						double y = TNVLocalHostCell.this.getY() - 10;
						String ttText = " " + TNVLocalHostCell.this.host.getName() + "  ("
							+ TNVLocalHostCell.this.frequency + " packets)  ";
						if ( ! tcpPortSet.isEmpty() ) {
							ttText += "\n  TCP Ports: " + TNVLocalHostCell.this.getTooltipPorts( 
									tcpPortSet.toString() );
							y -= 15;
						}
						if ( ! udpPortSet.isEmpty() ) {
							ttText += "\n  UDP Ports: " + TNVLocalHostCell.this.getTooltipPorts( 
									udpPortSet.toString() );
							y -= 15;
						}
						TNVLocalHostCell.this.tooltip.setText( ttText );
						if ( y < 5 )
							y = 5;
						double ttWidth = TNVLocalHostCell.this.tooltip.getWidth();
						double x = TNVLocalHostCell.this.getX() + (TNVLocalHostCell.this.getWidth()/2)- (ttWidth / 2);
						double canvasWidth = event.getCamera().getLayer( TNVCanvas.BASE_LAYER ).getFullBounds().getWidth();
						if ( (x + ttWidth) > canvasWidth )
							x = canvasWidth - ttWidth - 10;
						TNVLocalHostCell.this.tooltip.setOffset( x, y );
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
	}


	/* (non-Javadoc)
	 * @see edu.umd.cs.piccolo.PNode#paint(edu.umd.cs.piccolo.util.PPaintContext)
	 */
	@Override
	public void paint( PPaintContext aPaintContext ) {
		Graphics2D g2 = aPaintContext.getGraphics();

		Composite origComposite = g2.getComposite();
		g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.75f ) );
		
		// fill cell with color based on frequency and draw grid
		if ( this.frequency > 0 ) {
			g2.setColor( TNVUtil.getColor( this.frequency, TNVPreferenceData.getInstance().getColorMapIndex() ) );
			g2.fill( getBoundsReference() );
		}
		

		// draw grid outline
		g2.setPaint( Color.LIGHT_GRAY );
		g2.draw( getBoundsReference() );

		// if the frequency is zero, do nothing more
		if ( this.frequency == 0 )
			return;
		
		// draw highlighted host border inside cell if selected
		if ( this.selected ) {
			g2.setPaint( Color.RED );
			g2.draw( new Rectangle2D.Double( this.getX() + 1, this.getY() + 1, this.getWidth() - 2,
					this.getHeight() - 2 ) );
		}
		
		
		// if the cell is not expanded or if show packets is not true, dont draw packets
		if ( ! ( this.heightFocus && this.widthFocus) && ! this.showPackets ) 
			return;
		
		// if the time period is adjusting, do not paint packets
		if ( this.paintPackets == false )
			return;
		
		// reset ports
		tcpPortSet.clear();
		udpPortSet.clear();

		int center = (int) this.getHeight() / 2;
		int offset = (int)(this.getHeight() * 0.075f);
		int y = (int)this.getY();
		
		int inTopY = y + offset;
		int inBottomY = y + center - 1;
		int height = inBottomY - inTopY;
		int inCenterY = inTopY + (height / 2);
		
		int outTopY = y + center + 1;
		int outBottomY = y + (int) this.getHeight() - offset;
		int outCenterY = outTopY + (height / 2);

		float flagHeight = (float) (height-4) / 6;
		float flagWidth;
		
		Timestamp packetTime;
		int width, protocol;

		Iterator it = TNVDbUtil.getInstance().getLocalHostMap(host.getName()).subMap( 
				this.startTime, this.endTime ).values().iterator();
		while ( it.hasNext() ) {
			List packets = (List) it.next();
			Iterator packetIt = packets.iterator();
			while ( packetIt.hasNext() ) {
				TNVPacket p = (TNVPacket) packetIt.next();
				protocol = p.getProtocol();
				packetTime = p.getTimestamp();				

				// check for filtering options on protocol and get ports for tooltips
				if ( protocol == IPProtocols.TCP ) {
					if ( ! TNVModel.getInstance().isShowTCPpackets() )
						continue;
				}
				else if ( protocol == IPProtocols.UDP ) {
					if ( ! TNVModel.getInstance().isShowUDPpackets() )
						continue;
				}
				else if ( protocol == IPProtocols.ICMP ) {
					if ( ! TNVModel.getInstance().isShowICMPpackets() )
						continue;
				}
				
				// check for filtering options on port list if TCP or UDP
				// if blank or not TCP or UDP, ignore
				// if not blank, only show those ports
				Set ports = TNVModel.getInstance().getShowPorts();
				if ( ( ports != null && ! ports.isEmpty() )
						&& ( protocol == IPProtocols.TCP || protocol == IPProtocols.UDP ) ) {
					// if this src port is not in set and type is src
					if ( ! ports.contains( p.getSrcPort() + "" ) &&
							( TNVModel.getInstance().getShowPortDirection().equals(TNVModel.DisplayPacketType.SRC) )
						)
						continue;
					// if this dst port is not in set and type is dst
					else if ( ! ports.contains( p.getDstPort() + "" ) && 
							( TNVModel.getInstance().getShowPortDirection().equals(TNVModel.DisplayPacketType.DST) )
						)
						continue;
					// if this neither src or dst port are in set and type is both
					else if ( ( ! ports.contains( p.getSrcPort() + "" ) && ! ports.contains( p.getDstPort() + "" ) ) && 
							( TNVModel.getInstance().getShowPortDirection().equals(TNVModel.DisplayPacketType.BOTH) )
						)
						continue;
				}
				
				// check for filtering options on length values
				if ( TNVModel.getInstance().getShowLengthValue() != 0 ) {
					if ( TNVModel.getInstance().getShowLengthModifier().equals(TNVModel.ValueModifier.EQUAL_TO) && 
							p.getLength() != TNVModel.getInstance().getShowLengthValue() ) 
						continue;
					else if ( TNVModel.getInstance().getShowLengthModifier().equals(TNVModel.ValueModifier.LESS_THAN) && 
							p.getLength() > TNVModel.getInstance().getShowLengthValue()  ) 
						continue;
					else if ( TNVModel.getInstance().getShowLengthModifier().equals(TNVModel.ValueModifier.GREATER_THAN) && 
							p.getLength() < TNVModel.getInstance().getShowLengthValue() )
						continue;
				}

				// check for filtering options on TTL values
				if ( TNVModel.getInstance().getShowTtlValue() != 0 ) {
					if ( TNVModel.getInstance().getShowTtlModifier().equals(TNVModel.ValueModifier.EQUAL_TO) && 
							p.getTtl() != TNVModel.getInstance().getShowTtlValue() ) 
						continue;
					else if ( TNVModel.getInstance().getShowTtlModifier().equals(TNVModel.ValueModifier.LESS_THAN) && 
							p.getTtl() > TNVModel.getInstance().getShowTtlValue()  ) 
						continue;
					else if ( TNVModel.getInstance().getShowTtlModifier().equals(TNVModel.ValueModifier.GREATER_THAN) && 
							p.getTtl() < TNVModel.getInstance().getShowTtlValue() ) 
						continue;
				}


				// get ports for tooltips
				if ( protocol == IPProtocols.TCP ) {
					tcpPortSet.add( new Integer(p.getSrcPort()) );
					tcpPortSet.add( new Integer(p.getDstPort()) );
				}
				else if ( protocol == IPProtocols.UDP ) {
					udpPortSet.add( new Integer(p.getSrcPort()) );
					udpPortSet.add( new Integer(p.getDstPort()) );
				}

								
				// set color and transparency level based on protocol, port, length, ttl highlight values
				boolean highlight = false;
				if ( protocol == IPProtocols.TCP ) {
					g2.setColor( TNVPreferenceData.getInstance().getTcpColor() );
					
					if ( TNVModel.getInstance().isHighlightTCPpackets() ) 
						highlight=true;
					
					if ( TNVModel.getInstance().isHighlightSYNpackets() && p.isSyn() ) 
						highlight=true;
					if ( TNVModel.getInstance().isHighlightACKpackets()  && p.isAck() ) 
						highlight=true;
					if ( TNVModel.getInstance().isHighlightFINpackets()  && p.isFin() ) 
						highlight=true;
					if ( TNVModel.getInstance().isHighlightPSHpackets()  && p.isPsh() ) 
						highlight=true;
					if ( TNVModel.getInstance().isHighlightURGpackets()  && p.isUrg() ) 
						highlight=true;
					if ( TNVModel.getInstance().isHighlightRSTpackets()  && p.isRst() )
						highlight=true;
				}
				else if ( protocol == IPProtocols.UDP ) {
					g2.setColor( TNVPreferenceData.getInstance().getUdpColor() );
					if ( TNVModel.getInstance().isHighlightUDPpackets() )
						highlight=true;
				}
				else {
					g2.setColor( TNVPreferenceData.getInstance().getIcmpColor() );
					if ( TNVModel.getInstance().isHighlightICMPpackets() ) 
						highlight=true;
				}

				// if port list is specified and packet is TCP or UDP
				ports = TNVModel.getInstance().getHighlightPorts();
				if ( ( ports != null && ! ports.isEmpty() )
						&& ( protocol == IPProtocols.TCP || protocol == IPProtocols.UDP ) ) {
					// if this src port is in set and type is all or src
					if ( ( ports.contains( p.getSrcPort() + "" ) )
							&& ( TNVModel.getInstance().getHighlightPortDirection().equals(TNVModel.DisplayPacketType.BOTH) 
							|| TNVModel.getInstance().getHighlightPortDirection().equals(TNVModel.DisplayPacketType.SRC) ) )
						highlight=true;
					// if this dst port is in set and type is all or dst
					if ( ( ports.contains( p.getDstPort() + "" ) )
							&& ( TNVModel.getInstance().getHighlightPortDirection().equals(TNVModel.DisplayPacketType.BOTH)
							|| TNVModel.getInstance().getHighlightPortDirection().equals(TNVModel.DisplayPacketType.DST) ) )
						highlight=true;
				}
				
				// TTL values
				if ( TNVModel.getInstance().getHighlightTtlValue() != 0 ) {
					if ( TNVModel.getInstance().getHighlightTtlModifier().equals(TNVModel.ValueModifier.EQUAL_TO) && 
							p.getTtl() == TNVModel.getInstance().getHighlightTtlValue() ) 
						highlight=true;
					else if ( TNVModel.getInstance().getHighlightTtlModifier().equals(TNVModel.ValueModifier.LESS_THAN) && 
							p.getTtl() <= TNVModel.getInstance().getHighlightTtlValue()  ) 
						highlight=true;
					else if ( TNVModel.getInstance().getHighlightTtlModifier().equals(TNVModel.ValueModifier.GREATER_THAN) && 
							p.getTtl() >= TNVModel.getInstance().getHighlightTtlValue() ) 
						highlight=true;
				}

				// Length values
				if ( TNVModel.getInstance().getHighlightLengthValue() != 0 ) {
					if ( TNVModel.getInstance().getHighlightLengthModifier().equals(TNVModel.ValueModifier.EQUAL_TO) && 
							p.getLength() == TNVModel.getInstance().getHighlightLengthValue() ) 
						highlight=true;
					else if ( TNVModel.getInstance().getHighlightLengthModifier().equals(TNVModel.ValueModifier.LESS_THAN) && 
							p.getLength() <= TNVModel.getInstance().getHighlightLengthValue()  ) 
						highlight=true;
					else if ( TNVModel.getInstance().getHighlightLengthModifier().equals(TNVModel.ValueModifier.GREATER_THAN) && 
							p.getLength() >= TNVModel.getInstance().getHighlightLengthValue() )
						highlight=true;
				}
				
				// set size and position
				if ( this.heightFocus && this.widthFocus ) {
					width = (int) (TNVPacket.PACKET_WIDTH * 1.25);
					flagWidth = 4;
				}
				else {
					width = TNVPacket.PACKET_WIDTH;
					flagWidth = 2;
				}
				
				if ( highlight )
					width = (int) (TNVPacket.PACKET_WIDTH * 1.25);
				
				int x = this.canvas.getLocalGraph().getPositionForTimestamp(packetTime);
				Polygon packetPolygon = new Polygon();
				float flagX, flagY;
				
				// if this packet is the source, draw outgoing <|
				if ( p.getSrcAddr().equals( this.getName() ) ) {
					packetPolygon.addPoint( x + width, outTopY );
					packetPolygon.addPoint( x + width, outBottomY );
					packetPolygon.addPoint( x, outCenterY );
					flagX = x + width;
					flagY = outTopY + 2;
				}
				// if this packet is the destination, draw incoming |>
				else {
					packetPolygon.addPoint( x, inTopY );
					packetPolygon.addPoint( x + width, inCenterY );
					packetPolygon.addPoint( x, inBottomY );
					flagX = x - flagWidth;
					flagY = inTopY + 2;
				}

				// draw packet 
				g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.3f ) );

				// if highlighted, draw border and more opaque
				if ( highlight ) {
					Color origColor = g2.getColor();
					Stroke origStroke = g2.getStroke();
					g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 1f ) );
					g2.setColor(Color.BLACK);
					g2.setStroke(new BasicStroke(1.2f));
					g2.draw(packetPolygon);
					g2.setColor(origColor);
					g2.setStroke(origStroke);
					g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.6f ) );
				}
				
				g2.fill( packetPolygon );

				// rectangles for TCP flags
				if ( this.showFlags ) {
					Shape[] flagRects = new Shape[6];
					flagRects[0] = new Rectangle2D.Float(flagX, flagY, flagWidth, flagHeight);
					flagRects[1] = new Rectangle2D.Float(flagX, flagY+=flagHeight, flagWidth, flagHeight);
					flagRects[2] = new Rectangle2D.Float(flagX, flagY+=flagHeight, flagWidth, flagHeight);
					flagRects[3] = new Rectangle2D.Float(flagX, flagY+=flagHeight, flagWidth, flagHeight);
					flagRects[4] = new Rectangle2D.Float(flagX, flagY+=flagHeight, flagWidth, flagHeight);
					flagRects[5] = new Rectangle2D.Float(flagX, flagY+=flagHeight, flagWidth, flagHeight);

					g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.8f ) );
					if ( p.isSyn() ) {
						g2.setColor( TNVPreferenceData.getInstance().getSynColor() );
						g2.fill(flagRects[0]);
					}
					if ( p.isAck() ) {
						g2.setColor( TNVPreferenceData.getInstance().getAckColor() );
						g2.fill(flagRects[1]);
					}
					if ( p.isFin() ) {
						g2.setColor( TNVPreferenceData.getInstance().getFinColor() );
						g2.fill(flagRects[2]);
					}
					if ( p.isUrg() ) {
						g2.setColor( TNVPreferenceData.getInstance().getUrgColor() );
						g2.fill(flagRects[3]);
					}
					if ( p.isPsh() ) {
						g2.setColor( TNVPreferenceData.getInstance().getPshColor() );
						g2.fill(flagRects[4]);
					}
					if ( p.isRst() ) {
						g2.setColor( TNVPreferenceData.getInstance().getRstColor() );
						g2.fill(flagRects[5]);
					}
				}

				g2.setComposite( origComposite );
			}
		}

	}

	
	/**
	 * Return the string to use for tooltip port info, shortened if necessary and without [ ]
	 * @param s
	 * @return
	 */
	private String getTooltipPorts(String s) {
		s=s.replaceAll("[\\[\\]]", "");
		if ( s.length() > 50 )
			s = s.substring(0, 49) + "...";
		return s;
	}
	
	/**
	 * @return Returns the heightFocus.
	 */
	public final boolean hasHeightFocus( ) {
		return this.heightFocus;
	}


	/**
	 * @param heightFocus The heightFocus to set.
	 */
	public final void setHeightFocus( boolean heightFocus ) {
		this.heightFocus = heightFocus;
	}


	/**
	 * @return Returns the widthFocus.
	 */
	public final boolean hasWidthFocus( ) {
		return this.widthFocus;
	}


	/**
	 * @param widthFocus The widthFocus to set.
	 */
	public final void setWidthFocus( boolean widthFocus ) {
		this.widthFocus = widthFocus;
	}


	/**
	 *
	 * @return the int LocalHostCell.java
	 */
	public final int getColumnIndex() {
		return this.columnIndex;
	}


	/**
	 * @param startTime The startTime to set.
	 */
	public final void setStartTime( Timestamp t ) {
		this.startTime = t;
		this.invalidatePaint();
	}


	/**
	 * @return Returns the startTime.
	 */
	public final Timestamp getStartTime( ) {
		return this.startTime;
	}


	/**
	 * @param endTime The endTime to set.
	 */
	public final void setEndTime( Timestamp t ) {
		this.endTime = t;
		this.invalidatePaint();
	}


	/**
	 * @return Returns the endTime.
	 */
	public final Timestamp getEndTime( ) {
		return this.endTime;
	}


	/**
	 * @return Returns the name.
	 */
	public final String getName( ) {
		return this.host.getName();
	}


	/**
	 * @return Returns the frequency.
	 */
	public final int getFrequency( ) {
		return this.frequency;
	}
	
	/**
	 * @param frequency the int frequency to set
	 */
	public final void setFrequency( int frequency ) {
		this.frequency = frequency;
	}

	/**
	 * @param selected the selected to set
	 */
	public final void setSelected( boolean selected ) {
		this.host.addAttribute(TNVHost.PROPERTY_SELECTED_NODE, selected);
		this.selected = selected;
	}

	/**
	 * @return selected
	 */
	public final boolean isSelected() {
		return this.selected;
	}
	
	// override to string method
	@Override
	public String toString( ) {
		return this.host.getName() + " (" + this.startTime.toString() + " - " + this.endTime.toString() + "): "
				+ super.toString();
	}

}
