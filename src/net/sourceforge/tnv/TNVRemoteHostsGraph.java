/**
 * Created on Feb 17, 2006
 * @author jgood
 * 
 * Piccolo PNode representing remote hosts graph
 */
package net.sourceforge.tnv;

import java.awt.Color;
import java.awt.FontMetrics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PText;

/**
 * RemoteHostsGraph
 */
public class TNVRemoteHostsGraph extends PNode {

	private static int MAX_HEIGHT = 25;
	private static int MIN_HEIGHT = 8;

	private double defaultRowHeight;
	
	// parent canvas
	private TNVCanvas canvas;


	/**
	 * Constructor
	 * @param c parent canvas
	 */
	public TNVRemoteHostsGraph(TNVCanvas c) {
		super();
		this.canvas = c;
		this.setPickable( false );

		// Listen for changes in start and end time selections
		String[] listenProps = { TNVModel.PROPERTY_VISIBLE_START_TIME, TNVModel.PROPERTY_VISIBLE_END_TIME };
		TNVModel.getInstance().addPropertyChangeListener( listenProps, new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent evt ) {
				String property = evt.getPropertyName();
				if ( ( property.equals( TNVModel.PROPERTY_VISIBLE_START_TIME ) 
						|| property.equals( TNVModel.PROPERTY_VISIBLE_END_TIME ) )
						&& ( TNVModel.getInstance().getCurrentStartTime() != null 
								&& TNVModel.getInstance().getCurrentEndTime() != null ) ) {
					// TODO: set order?
				}

			}
		} );
		
		// Listen for changes in size
		this.addPropertyChangeListener( PROPERTY_BOUNDS, new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent evt ) {
				TNVRemoteHostsGraph.this.setDefaultHeight();
				TNVRemoteHostsGraph.this.layoutChildren();
			}
		});

		// listen for dragging events to reorder the rows
		this.addInputEventListener( new TNVHostDragEventHandler(this)  );

	}


	// clear all data from graph
	protected void clearGraph( ) {
		this.removeAllChildren();
		this.repaint();
	}


	/**
	 * setup all local hosts and all nodes to the graph
	 */
	protected void setupHosts() {
		Iterator it = TNVDbUtil.getInstance().getRemoteHostList().iterator();
		while ( it.hasNext() )
			this.addChild( new TNVRemoteHost( (String)it.next() ) );
		this.setDefaultHeight();
		this.layoutChildren();
	}

	/**
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


	/* (non-Javadoc)
	 * @see edu.umd.cs.piccolo.PNode#layoutChildren()
	 */
	@Override
	protected void layoutChildren( ) {
		double yOffset = this.getY() + 1;
		double height = defaultRowHeight;
		Color[] colors = { new Color( 225, 235, 225 ), new Color( 245, 245, 255 ) };

		TNVRemoteHost remoteHostNode = null;

		int i = 0;
		Iterator it = this.getChildrenIterator();
		while ( it.hasNext() ) {
			remoteHostNode = (TNVRemoteHost) it.next();
			remoteHostNode.setBounds( 1, yOffset, this.getWidth(), height );
			remoteHostNode.setPaint( colors[i++ % 2] );

			// setup label, right justified
			PText label = (PText) remoteHostNode.getChild( 0 );
			FontMetrics metrics = this.canvas.getFontMetrics( label.getFont() );
			label.setX( remoteHostNode.getWidth() - metrics.stringWidth( label.getText() ) - 5 );
			label.setY( yOffset + ( height / 2 ) - ( metrics.getAscent() / 2 ) );

			yOffset += height;
		}
	}

	/**
	 * Set up the default height based on size of window and number of remote hosts 
	 */
	private void setDefaultHeight() {
		this.defaultRowHeight = this.getHeight() / TNVDbUtil.getInstance().getRemoteHostList().size();
		if ( this.defaultRowHeight > MAX_HEIGHT )
			this.defaultRowHeight = MAX_HEIGHT;
		if ( this.defaultRowHeight < MIN_HEIGHT )
			this.defaultRowHeight = MIN_HEIGHT;
	}
}
