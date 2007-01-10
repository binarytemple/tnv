/**
 * Created on Mar 23, 2006
 * @author jgood
 * 
 * Piccolo PNode representing the intergraph for edges between local and remote nodes 
 */
package net.sourceforge.tnv.ui;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.sourceforge.tnv.db.TNVDbUtil;
import net.sourceforge.tnv.dialogs.TNVErrorDialog;
import net.sourceforge.tnv.shared.SwingWorker;

import edu.umd.cs.piccolo.PNode;

/**
 * InterGraph
 */
/**
 * @author jgood
 * created on: Nov 9, 2006
 */
public class TNVInterGraph extends PNode {

	/**
	 * Remote host graph to connect
	 */
	private TNVRemoteHostsGraph remoteGraph;

	/**
	 * Local host graph to connect
	 */
	private TNVLocalHostsGraph localGraph;

	/**
	 * Swingworker Thread to create edges 
	 */
	SwingWorker worker;


	/**
	 * Constructor
	 * @param c
	 */
	public TNVInterGraph(TNVCanvas canvas) {
		super( );
		this.setPickable( false );
		this.remoteGraph = canvas.getRemoteGraph();
		this.localGraph = canvas.getLocalGraph();
	}



	/**
	 *  Create swing worker thread to create edges
	 */
	public void createEdges( ) {
		if ( this.localGraph.getChildrenCount() == 0 ) 
			return;

		worker = new SwingWorker() {
			public Object construct() {
				return buildLinks();
			}
			public void finished() {
				TNVInterGraph.this.validateFullPaint();
			}
		};
		worker.start();
	}

	/**
	 * Interrupt any running threads and clear the graph
	 */
	public void clearGraph( ) {
		if ( worker != null ) {
			worker.interrupt();
			worker = null;
		}
		TNVStatusBar.getInstance().clearStatus();
		this.removeAllChildren();
		this.validateFullPaint();
	}

	
	/**
	 * Build the links, drawing as they are built; update status bar on start/end
	 * @return completion status
	 */
	private Object buildLinks() {
		TNVStatusBar.getInstance().updateStatus(TNVStatusBar.Operations.BUILD_LINKS);
		try {
			TNVInterGraph.this.removeAllChildren();

			TNVHost srcHost, dstHost;
			TNVLinkNode.LinkDirection direction;
			String srcaddr, dstaddr;
			int protocol, count;

			try {
				PreparedStatement getLinksStatement = TNVDbUtil.getInstance().getSelectLinksStmt();
				getLinksStatement.setTimestamp( 1, TNVModel.getInstance().getCurrentStartTime() );
				getLinksStatement.setTimestamp( 2, TNVModel.getInstance().getCurrentEndTime() );
				ResultSet rs = getLinksStatement.executeQuery();
				while ( rs.next() ) {
					srcaddr = rs.getString( "srcaddr" );
					dstaddr = rs.getString( "dstaddr" );
					protocol = rs.getInt( "protocol" );
					count = rs.getInt( "frequency" );

					// local src and dst
					if ( TNVDbUtil.getInstance().getLocalHostMap().containsKey( srcaddr)  && 
							TNVDbUtil.getInstance().getLocalHostMap().containsKey( dstaddr) ) {
						srcHost = TNVInterGraph.this.localGraph.getHostByString( srcaddr );
						dstHost = TNVInterGraph.this.localGraph.getHostByString( dstaddr );
						direction = TNVLinkNode.LinkDirection.LOCAL;
					}
					// remote src and local dst (ingress)
					else if ( TNVDbUtil.getInstance().getRemoteHostList().contains( srcaddr)  &&
							TNVDbUtil.getInstance().getLocalHostMap().containsKey( dstaddr) ) {
						srcHost = TNVInterGraph.this.remoteGraph.getHostByString( srcaddr );
						dstHost = TNVInterGraph.this.localGraph.getHostByString( dstaddr );
						direction = TNVLinkNode.LinkDirection.INCOMING;
					}
					// local src and remote dst (egress)
					else if ( TNVDbUtil.getInstance().getLocalHostMap().containsKey( srcaddr ) &&
							TNVDbUtil.getInstance().getRemoteHostList().contains( dstaddr) ) {
						srcHost = TNVInterGraph.this.localGraph.getHostByString( srcaddr );
						dstHost = TNVInterGraph.this.remoteGraph.getHostByString( dstaddr );
						direction = TNVLinkNode.LinkDirection.OUTGOING;
					}
					else
						continue;

					TNVInterGraph.this.addChild( new TNVLinkNode( srcHost, dstHost, protocol, count, direction ) );
					TNVInterGraph.this.validateFullPaint();

					// Check for thread interruption and toss exception
					if ( Thread.interrupted() )
						throw new InterruptedException();
				}
				rs.close();
			}
			catch ( SQLException ex ) {
				TNVErrorDialog.createTNVErrorDialog(this.getClass(), "SQL Error getting edge list", ex);
			}
		}
		catch ( InterruptedException e ) {
			TNVStatusBar.getInstance().clearStatus();
			return "Interrupted";  
		}
		TNVStatusBar.getInstance().clearStatus();
		return "Completed"; 
	}
}
