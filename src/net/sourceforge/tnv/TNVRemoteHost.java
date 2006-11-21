/**
 * Created on Feb 17, 2006
 * @author jgood
 * 
 * Piccolo PNode for Remote (non-local) host representations
 */
package net.sourceforge.tnv;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * RemoteHost
 */
public class TNVRemoteHost extends TNVHost {

	private boolean selected = false;
	private PText label; // label for this host


	/**
	 * Constructor
	 * @param n the name of this host
	 */
	public TNVRemoteHost(String n) {
		super();
		this.setName( n );

		this.label = new PText( n + " " );
		this.label.setFont( TNVUtil.SMALL_LABEL_FONT );
		this.addChild( 0, this.label );

	}


	/**
	 * @param selected the boolean selected to set
	 */
	protected final void setSelected( boolean selected ) {
		this.addAttribute(TNVHost.PROPERTY_SELECTED_NODE, selected);
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

	/* (non-Javadoc)
	 * @see edu.umd.cs.piccolo.PNode#paint(edu.umd.cs.piccolo.util.PPaintContext)
	 */
	@Override
	public void paint( PPaintContext aPaintContext ) {
		Graphics2D g2 = aPaintContext.getGraphics();

		// fill
		g2.setPaint( this.getPaint() );
		g2.fill( this.getBoundsReference() );

		// draw grid border
		g2.setPaint( Color.GRAY );
		g2.draw( this.getBoundsReference() );

		// draw interior border if selected
		if ( selected ) {
			g2.setPaint( Color.BLUE );
			g2.draw( new Rectangle2D.Double( this.getX() + 1, this.getY() + 1, this.getWidth() - 2,
					this.getHeight() - 2 ) );
		}
	}


	/* (non-Javadoc)
	 * @see edu.umd.cs.piccolo.PNode#setBounds(double, double, double, double)
	 */
	@Override
	public boolean setBounds(double x, double y, double w, double h) {
		if ( h < 12)
			this.label.setFont( TNVUtil.SMALL_LABEL_FONT.deriveFont( (float)h - 2 ) );
		return super.setBounds(x, y, w, h);
	}

}
