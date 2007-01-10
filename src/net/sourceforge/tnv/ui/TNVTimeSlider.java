/**
 * Created on Mar 26, 2005
 * @author jgood
 * 
 * TNVTimeSlider for controling the display time
 */
package net.sourceforge.tnv.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import net.sourceforge.tnv.db.TNVDbUtil;
import net.sourceforge.tnv.util.TNVUtil;

/**
 * @author jgood
 * created on: Apr 29, 2006
 */
public class TNVTimeSlider extends JPanel implements MouseInputListener, PropertyChangeListener {

	private static final int PANEL_HEIGHT = 60;
	private static final int TIMEBOX_HEIGHT = 42;
	private static final int TOP_INDENT = 2; // amount to indent from top of timebox
	private static final int SIDE_INDENT = 20; // amount to indent on either side, this is the start x value
	private static final int HANDLE_WIDTH = 14;
	private static final int LABEL_Y = TIMEBOX_HEIGHT + TOP_INDENT + 14;
	private static final Color HANDLE_COLOR = new Color( 50, 75, 110 );
	
	// Rectangles for the entire slider, the current area, and handles
	private Rectangle2D timeBox, currentTimeBox;
	private Rectangle2D startHandle, endHandle;

	// times
	private long totalTimeWidth, currentTimeWidth;
	private long startTime = 0, endTime = 0, currentStartTime = 0, currentEndTime = 0;

	// for histogram
	private Rectangle[] histogram = null;
	private Thread buildHistogramThread;
	private boolean isBuildingHistogram = false;

	// position holders
	private int endX; // start and end of timeslider
	private int currentStartX, currentEndX; // start handle and end handle X position
	private int startFromPoint, endFromPoint; // distance from current mouse position of start, end handles

	private boolean isStartAdjusting = false, isEndAdjusting = false, isCurrentAdjusting = false;


	/**
	 * Constructor
	 */
	public TNVTimeSlider() {
		super();

		this.timeBox = new Rectangle2D.Float( SIDE_INDENT, TOP_INDENT, this.getWidth() - ( SIDE_INDENT * 2 ),
				TIMEBOX_HEIGHT );
		this.currentTimeBox = new Rectangle2D.Float();
		this.startHandle = new Rectangle2D.Float();
		this.endHandle = new Rectangle2D.Float();

		// Listen for changes in start/end times
		String[] listenProps = { TNVModel.PROPERTY_START_TIME, TNVModel.PROPERTY_END_TIME,
				TNVModel.PROPERTY_VISIBLE_START_TIME, TNVModel.PROPERTY_VISIBLE_END_TIME };
		TNVModel.getInstance().addPropertyChangeListener( listenProps, this );

		// Panel resizing, set up again and repaint
		this.addComponentListener( new ComponentAdapter() {
			@Override
			public void componentResized( ComponentEvent e ) {
				Dimension size = new Dimension( e.getComponent().getWidth(), PANEL_HEIGHT );
				TNVTimeSlider.this.setSize( size );
				TNVTimeSlider.this.setPreferredSize( size );
				TNVTimeSlider.this.timeBox.setFrame( SIDE_INDENT, TOP_INDENT, TNVTimeSlider.this.getWidth()
						- ( SIDE_INDENT * 2 ), TIMEBOX_HEIGHT );
				TNVTimeSlider.this.repaint();
				if ( ! TNVTimeSlider.this.isBuildingHistogram 
						&& TNVTimeSlider.this.startTime != 0
						&& TNVTimeSlider.this.endTime != 0 ) {
					buildHistogramThread = new Thread( new BuildHistogramThread() );
					buildHistogramThread.setPriority( Thread.MIN_PRIORITY );
					buildHistogramThread.start();
				}
			}
		} );

		// Mouse events
		this.addMouseListener( this );
		this.addMouseMotionListener( this );

		this.setPreferredSize( new Dimension( this.getWidth(), PANEL_HEIGHT ) );
		this.setBackground( TNVUtil.BG_COLOR );
	}


	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paint(java.awt.Graphics)
	 */
	@Override
	public void paint( Graphics g ) {
		Graphics2D g2 = (Graphics2D) g;

		// reset display
		g2.setColor( this.getBackground() );
		g2.fillRect( 0, 0, this.getWidth(), this.getHeight() );

		g2.setColor( Color.BLACK );
		g2.setFont( TNVUtil.LABEL_FONT );

		this.endX = this.getWidth() - SIDE_INDENT; // absolute end of drawing area
		this.currentTimeWidth = this.endTime - this.startTime; // width of current times

		g2.draw( this.timeBox );

		if ( this.startTime == 0 || this.endTime == 0 ) 
			return;

		// loop through the array and draw rectangles that represent the relative
		// number of packets (compared to maxCount) for each time interval
		g2.setColor( Color.DARK_GRAY );
		if ( ! isBuildingHistogram && this.histogram != null ) 
			for ( int i = 0; i < this.histogram.length; i++ )
				g2.fill( this.histogram[i] );

		// Set the frames of the handles
		this.currentStartX = timeToPosition( this.currentStartTime );
		this.currentEndX = timeToPosition( this.currentEndTime );

		this.startHandle.setFrame( this.currentStartX, TOP_INDENT, HANDLE_WIDTH, TIMEBOX_HEIGHT );
		this.endHandle.setFrame( this.currentEndX - HANDLE_WIDTH, TOP_INDENT, HANDLE_WIDTH, TIMEBOX_HEIGHT );

		// Draw handles at either endTime of current box
		Composite origComposite = g2.getComposite();
		g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.75f ) );
		g2.setColor( HANDLE_COLOR );
		g2.fill( this.startHandle );
		g2.fill( this.endHandle );
		g2.setComposite( origComposite );
		g2.setColor( Color.BLACK );

		// draw starting handle with hash marks
		g2.draw( this.startHandle );
		int startCenterX = (int) this.startHandle.getCenterX();
		g2.drawLine( startCenterX - 1, TOP_INDENT + ( TIMEBOX_HEIGHT / 2 ) - 4, startCenterX - 1, TOP_INDENT
				+ ( TIMEBOX_HEIGHT / 2 ) + 4 );
		g2.drawLine( startCenterX + 1, TOP_INDENT + ( TIMEBOX_HEIGHT / 2 ) - 4, startCenterX + 1, TOP_INDENT
				+ ( TIMEBOX_HEIGHT / 2 ) + 4 );
		String startHandleLabel = TNVUtil.SHORT_FORMAT.format( new Date( this.currentStartTime ) );
		g2.drawString( startHandleLabel, startCenterX - ( g2.getFontMetrics().stringWidth( startHandleLabel ) / 2 ),
				LABEL_Y );

		// draw ending handle with hash marks
		g2.draw( this.endHandle );
		int endCenterX = (int) this.endHandle.getCenterX();
		g2.drawLine( endCenterX - 1, TOP_INDENT + ( TIMEBOX_HEIGHT / 2 ) - 4, endCenterX - 1, TOP_INDENT
				+ ( TIMEBOX_HEIGHT / 2 ) + 4 );
		g2.drawLine( endCenterX + 1, TOP_INDENT + ( TIMEBOX_HEIGHT / 2 ) - 4, endCenterX + 1, TOP_INDENT
				+ ( TIMEBOX_HEIGHT / 2 ) + 4 );
		String endHandleLabel = TNVUtil.SHORT_FORMAT.format( new Date( this.currentEndTime ) );
		g2.drawString( endHandleLabel, endCenterX - ( g2.getFontMetrics().stringWidth( endHandleLabel ) / 2 ), LABEL_Y );

		// if handles are far enough from edges, draw startTime/endTime labels
		String dataStartLabel = TNVUtil.SHORT_FORMAT.format( new Date( this.startTime ) );
		String dataEndLabel = TNVUtil.SHORT_FORMAT.format( new Date( this.endTime ) );
		if ( this.startHandle.getX() > 
				SIDE_INDENT + g2.getFontMetrics().stringWidth( dataStartLabel ) +
				(g2.getFontMetrics().stringWidth( startHandleLabel ) / 2) )
			g2.drawString( dataStartLabel, SIDE_INDENT, LABEL_Y );
		if ( this.endHandle.getX() + HANDLE_WIDTH < 
				this.endX - g2.getFontMetrics().stringWidth( dataEndLabel ) -
				(g2.getFontMetrics().stringWidth( endHandleLabel ) / 2) )
			g2.drawString( dataEndLabel, this.endX - g2.getFontMetrics().stringWidth( dataEndLabel ), LABEL_Y );

		// Draw current box
		this.currentTimeBox.setFrame( this.startHandle.getX() + HANDLE_WIDTH, TOP_INDENT, this.endHandle.getX()
				- this.startHandle.getX() - HANDLE_WIDTH, TIMEBOX_HEIGHT );
		origComposite = g2.getComposite();
		g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_OVER, 0.15f ) );
		g2.setColor( HANDLE_COLOR );
		g2.fill( this.currentTimeBox );
		g2.setComposite( origComposite );
		g2.setColor( Color.BLACK );
		g2.draw( this.currentTimeBox );

	}


	/**
	 * convert time unit to x position
	 * @param t
	 * @return x position
	 */
	private int timeToPosition( long t ) {
		float timePercentage = (float) ( t - this.startTime ) / (float) ( this.endTime - this.startTime );
		int x = ( (int) ( timePercentage * ( this.endX - SIDE_INDENT ) ) ) + SIDE_INDENT;
		return x;
	}


	/**
	 * convert x position to time unit
	 * @param x
	 * @return timestamp
	 */
	private Timestamp positionToTimestamp( int x ) {
		float positionPercentage = (float) ( x - SIDE_INDENT ) / (float) ( this.endX - SIDE_INDENT );
		long time = ( (long) ( positionPercentage * ( this.endTime - this.startTime ) ) ) + this.startTime;
		return new Timestamp( time );
	}


	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked( MouseEvent e ) { }


	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed( MouseEvent e ) {
		Point2D p = e.getPoint();
		if ( this.startHandle.contains( p ) ) {
			this.isStartAdjusting = true;
			TNVModel.getInstance().setAdjusting( true );
		}
		else if ( this.endHandle.contains( p ) ) {
			this.isEndAdjusting = true;
			TNVModel.getInstance().setAdjusting( true );
		}
		else if ( this.currentTimeBox.contains( p ) ) {
			this.isCurrentAdjusting = true;
			this.startFromPoint = (int) ( p.getX() - this.startHandle.getX() );
			this.endFromPoint = (int) ( this.endHandle.getX() - p.getX() );
			TNVModel.getInstance().setAdjusting( true );
		}
	}


	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased( MouseEvent e ) {
		Point2D p = e.getPoint();
		if ( this.isStartAdjusting ) {
			this.isStartAdjusting = false;
			TNVModel.getInstance().setCurrentStartTime( positionToTimestamp( (int) p.getX() ) );
			TNVModel.getInstance().setAdjusting( false );
		}
		else if ( this.isEndAdjusting ) {
			this.isEndAdjusting = false;
			TNVModel.getInstance().setCurrentEndTime( positionToTimestamp( (int) p.getX() ) );
			TNVModel.getInstance().setAdjusting( false );
		}
		else if ( this.isCurrentAdjusting ) {
			this.isCurrentAdjusting = false;
			TNVModel.getInstance().setCurrentStartTime( positionToTimestamp( (int) p.getX() - this.startFromPoint ) );
			TNVModel.getInstance().setCurrentEndTime( positionToTimestamp( (int) p.getX() + this.endFromPoint ) );
			TNVModel.getInstance().setAdjusting( false );
		}
	}


	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered( MouseEvent e ) { }


	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited( MouseEvent e ) { }


	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged( MouseEvent e ) {
		int xPosition = (int) e.getPoint().getX();
		if ( this.isStartAdjusting ) {
			// if handle moved to left edge of screen, stop at edge
			if ( xPosition <= SIDE_INDENT ) {
				this.isStartAdjusting = false;
				TNVModel.getInstance().setCurrentStartTime( positionToTimestamp( SIDE_INDENT ) );
				TNVModel.getInstance().setAdjusting( false );
			}
			// if handle moved to other handle, stop there
			else if ( xPosition + HANDLE_WIDTH > timeToPosition( TNVModel.getInstance().getCurrentEndTime().getTime() ) ) {
				this.isStartAdjusting = false;
				TNVModel.getInstance().setCurrentStartTime(
						positionToTimestamp( timeToPosition( TNVModel.getInstance().getCurrentEndTime().getTime() )
								- ( HANDLE_WIDTH * 2 ) ) );
				TNVModel.getInstance().setAdjusting( false );
			}
			// else update the position and time
			else
				TNVModel.getInstance().setCurrentStartTime( positionToTimestamp( xPosition ) );
		}
		else if ( this.isEndAdjusting ) {
			// if handle moved to right edge of screen, stop at edge
			if ( xPosition >= this.endX ) {
				this.isEndAdjusting = false;
				TNVModel.getInstance().setCurrentEndTime( positionToTimestamp( this.endX ) );
				TNVModel.getInstance().setAdjusting( false );
			}
			// if handle moved to other handle, stop there
			else if ( xPosition - HANDLE_WIDTH <= timeToPosition( TNVModel.getInstance().getCurrentStartTime()
					.getTime() ) ) {
				this.isEndAdjusting = false;
				TNVModel.getInstance().setCurrentEndTime(
						positionToTimestamp( timeToPosition( TNVModel.getInstance().getCurrentStartTime().getTime() )
								+ ( HANDLE_WIDTH * 2 ) ) );
				TNVModel.getInstance().setAdjusting( false );
			}
			// else update the position and time
			else
				TNVModel.getInstance().setCurrentEndTime( positionToTimestamp( xPosition ) );
		}
		else if ( this.isCurrentAdjusting ) {
			// if handle moved to either edge of screen, stop at edge
			if ( ( xPosition - this.startFromPoint ) <= SIDE_INDENT ) {
				this.isCurrentAdjusting = false;
				TNVModel.getInstance().setCurrentStartTime( positionToTimestamp( SIDE_INDENT ) );
				TNVModel.getInstance().setAdjusting( false );
			}
			else if ( ( xPosition + this.endFromPoint ) >= this.endX ) {
				this.isCurrentAdjusting = false;
				TNVModel.getInstance().setCurrentEndTime( positionToTimestamp( this.endX ) );
				TNVModel.getInstance().setAdjusting( false );
			}
			else {
				TNVModel.getInstance().setCurrentStartTime( positionToTimestamp( xPosition - this.startFromPoint ) );
				TNVModel.getInstance().setCurrentEndTime( positionToTimestamp( this.endFromPoint + xPosition ) );
			}
		}
	}


	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	public void mouseMoved( MouseEvent e ) { }


	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange( PropertyChangeEvent evt ) {
		String property = evt.getPropertyName();
		if ( property.equals( TNVModel.PROPERTY_START_TIME ) ) {
			Timestamp t = ( (Timestamp) evt.getNewValue() );
			if ( t != null ) {
				this.startTime = t.getTime();
				if ( this.endTime != 0 && !this.isBuildingHistogram ) {
					buildHistogramThread = new Thread( new BuildHistogramThread() );
					buildHistogramThread.setPriority( Thread.MIN_PRIORITY );
					buildHistogramThread.start();
				}
			}
			else
				this.startTime = 0;
			TNVTimeSlider.this.repaint();
		}
		else if ( property.equals( TNVModel.PROPERTY_END_TIME ) ) {
			Timestamp t = ( (Timestamp) evt.getNewValue() );
			if ( t != null ) {
				this.endTime = t.getTime();
				if ( this.startTime != 0 && !this.isBuildingHistogram ) {
					buildHistogramThread = new Thread( new BuildHistogramThread() );
					buildHistogramThread.setPriority( Thread.MIN_PRIORITY );
					buildHistogramThread.start();
				}
			}
			else
				this.endTime = 0;
			TNVTimeSlider.this.repaint();
		}
		else if ( property.equals( TNVModel.PROPERTY_VISIBLE_START_TIME ) ) {
			Timestamp t = ( (Timestamp) evt.getNewValue() );
			if ( t != null )
				this.currentStartTime = t.getTime();
			else
				this.currentStartTime = 0;
			TNVTimeSlider.this.repaint();
		}
		else if ( property.equals( TNVModel.PROPERTY_VISIBLE_END_TIME ) ) {
			Timestamp t = ( (Timestamp) evt.getNewValue() );
			if ( t != null )
				this.currentEndTime = t.getTime();
			else
				this.currentEndTime = 0;
			TNVTimeSlider.this.repaint();
		}
	}


	/**
	 * Inner class thread to read data into a histogram
	 * Do not want to hold up entire UI, so this runs in separate thread
	 * When done, the panel will repaint
	 * @author jgood
	 * created on: Apr 29, 2006
	 */
	class BuildHistogramThread implements Runnable {

		/**
		 * Constructor
		 */
		public BuildHistogramThread() {
			TNVTimeSlider.this.isBuildingHistogram = true;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		public void run( ) {
			TNVTimeSlider.this.histogram = null;
			List<Rectangle> temporaryList = new ArrayList<Rectangle>();

			// Draw timeline
			int numOfPoints = (int) TNVTimeSlider.this.timeBox.getWidth() / 3;
			long timeInterval = ( TNVTimeSlider.this.endTime - TNVTimeSlider.this.startTime ) / numOfPoints;
			long timePosition = TNVTimeSlider.this.startTime;
			int xInterval = (int) TNVTimeSlider.this.timeBox.getWidth() / numOfPoints;
			int xPosition = 0;

			int[] positionArray = new int[( numOfPoints * 2 )];
			int count, maxCount = 0;

			// create an array and get the maximum frequency for entire data set
			// array is the beginning x-position and the number of packets in
			// that time period
			// TODO: fix for very small time period
			for ( int i = 0; i < ( numOfPoints * 2 ); i += 2 ) {
				count = TNVDbUtil.getInstance().getPacketCount( new Timestamp( timePosition ),
						new Timestamp( timePosition += timeInterval ) );
				positionArray[i] = xPosition;
				positionArray[i + 1] = count;
				xPosition += xInterval;
				if ( count > maxCount ) maxCount = count;
			}

			// loop through the array and draw rectangles that represent the relative
			// number of packets (compared to maxCount) for each time interval
			for ( int i = 0; i < ( numOfPoints * 2 ); i += 2 ) {
				int xPos = positionArray[i] + SIDE_INDENT;
				int cnt = positionArray[i + 1];
				float percentage = 1 - ( cnt / (float) maxCount );
				int yPos = TOP_INDENT + (int) ( percentage * TIMEBOX_HEIGHT );
				if ( yPos < TOP_INDENT && cnt > 0 ) yPos = TOP_INDENT + 1;
				int yHeight = ( TOP_INDENT + TIMEBOX_HEIGHT ) - yPos;
				Rectangle r = new Rectangle( xPos, yPos, xInterval, yHeight );
				temporaryList.add( r );
			}

			// convert to an array of rectangles
			TNVTimeSlider.this.histogram = temporaryList.toArray( new Rectangle[temporaryList.size()] );

			TNVTimeSlider.this.isBuildingHistogram = false;

			// repaint
			SwingUtilities.invokeLater( new Runnable() {
				public void run( ) {
					TNVTimeSlider.this.repaint();
				}
			} );

		}
	}

}
