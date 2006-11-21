/**
 * Created on Mar 31, 2006
 * @author jgood
 * 
 * Piccolo drag event handler for reordering both local and remote hosts
 */
package net.sourceforge.tnv;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PDragSequenceEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PText;


/**
 * TNVHostDragEventHandler
 */
public class TNVHostDragEventHandler extends PDragSequenceEventHandler {

	
	/**
	 * picked host row
	 */
	private PNode pickedHost;

	/**
	 * overlay of picked host
	 */
	private PNode pickedHostGhost;
	
	/**
	 * graph of picked host
	 */
	private PNode pickedHostGraph;
	
	
	/**
	 * Constructor
	 * @param graph
	 */
	public TNVHostDragEventHandler(PNode graph) {
		super();
		this.pickedHostGraph = graph;
	}


	/**
	 * Enter, change to hand cursor if over a label to denote that it can be moved
	 * @param event PInputEvent
	 */
	@Override
	public void mouseEntered( PInputEvent event ) {
		if ( event.getButton() == MouseEvent.NOBUTTON && event.getPickedNode() instanceof PText )
			TNV.setHandCursor();
	}

	/**
	 * Exit, return to default cursor
	 * @param event PInputEvent
	 */
	@Override
	public void mouseExited( PInputEvent event ) {
		if ( event.getButton() == MouseEvent.NOBUTTON && event.getPickedNode() instanceof PText ) 
			TNV.setDefaultCursor();
	}

	/**
	 * Select hosts to highlight links
	 * @param event PInputEvent
	 */
	@Override
	public void mouseReleased(PInputEvent event) {
		super.mouseReleased(event);
		PNode labelNode = event.getPickedNode();
		if ( labelNode instanceof PText && event.getClickCount() == 2 ) {
			event.setHandled( true );
			PNode hostNode = labelNode.getParent();
			boolean select = true;
			if ( hostNode.getAttribute(TNVHost.PROPERTY_SELECTED_NODE) != null ) 
				select = ! ((Boolean)hostNode.getAttribute(TNVHost.PROPERTY_SELECTED_NODE)).booleanValue();
			hostNode.addAttribute(TNVHost.PROPERTY_SELECTED_NODE, select);
		}
	}


	/**
	 * Start drag
	 * @param event PInputEvent
	 */
	@Override
	protected void startDrag( PInputEvent event ) {
		super.startDrag( event );

		PNode labelNode = event.getPickedNode();
		
		// draw translucent overlay of the host while moving
		if ( labelNode instanceof PText && event.getClickCount() == 1 ) {
			event.setHandled( true );

			this.pickedHost = labelNode.getParent();
			this.pickedHostGraph = this.pickedHost.getParent();

			// create a ghost node and add to the top layer
			this.pickedHostGhost = new PNode();
			this.pickedHostGhost.setBounds( this.pickedHost.getGlobalBounds() );
			this.pickedHostGhost.setPaint( Color.BLUE );
			this.pickedHostGhost.setTransparency( 0.5f );
			this.pickedHostGhost.addChild( (PText)labelNode.clone() );
			event.getCamera().getLayer( TNVCanvas.TOOLTIP_LAYER ).addChild(this.pickedHostGhost );
		}
	}


	/**
	 * drag
	 * @param event PInputEvent
	 */
	@Override
	protected void drag( PInputEvent event ) {
		super.drag( event );
		if ( this.pickedHost != null ) {
			// check if moved to top or bottom and stop drag then, otherwise, update
			double y = event.getCanvasPosition().getY();
			PNode last = this.pickedHostGraph.getChild( this.pickedHostGraph.getChildrenCount() - 1 );
			if ( y >= this.pickedHostGraph.getChild( 0 ).getY() && y <= ( last.getY() + last.getHeight() ) )
				this.pickedHostGhost.translate( 0, event.getCanvasDelta().getHeight() );
		}
	}


	/**
	 * End drag
	 * @param event PInputEvent
	 */
	@Override
	protected void endDrag( PInputEvent event ) {
		super.endDrag( event );

		// If moving a host, swap the picked index with the dropped index on the parent
		if ( this.pickedHost != null ) {
			boolean found = false;
			List hosts = this.pickedHostGraph.getChildrenReference();
			int pickedIndex = hosts.indexOf( this.pickedHost );
			Point2D endPoint = event.getCanvasPosition();
			Iterator it = hosts.iterator();
			while ( it.hasNext() ) {
				PNode thisHost = (PNode) it.next();
				if ( thisHost.getFullBounds().contains( endPoint ) ) {
					int droppedIndex = hosts.indexOf( thisHost );
					this.pickedHostGraph.addChild( droppedIndex,
							this.pickedHostGraph.removeChild( pickedIndex ) );
					found = true;
					break;
				}
			}
			// moved to top, index 0
			int lastIndex = this.pickedHostGraph.getChildrenCount() - 1;
			if ( !found && endPoint.getY() < this.pickedHostGraph.getChild( 0 ).getY() ) {
				this.pickedHostGraph.addChild( 0, this.pickedHostGraph.removeChild( pickedIndex ) );
			}
			// moved to bottom, lastIndex
			else if ( !found
					&& endPoint.getY() > ( this.pickedHostGraph.getChild( lastIndex ).getY() + 
							this.pickedHostGraph.getChild( lastIndex ).getHeight() ) ) {
				this.pickedHostGraph.addChild( lastIndex, this.pickedHostGraph.removeChild( pickedIndex ) );
			}
			// remove the pickedHostGhost from the top layer
			event.getCamera().getLayer( TNVCanvas.TOOLTIP_LAYER ).removeAllChildren();
			this.pickedHostGraph = null;
			this.pickedHostGhost = null;
			this.pickedHost = null;
		}
	}
	
}
