/**
 * Created on Mar 27, 2005
 * @author jgood
 * 
 * Panel to display legend of colors and time interval represented by a single column
 */
package net.sourceforge.tnv;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.SortedMap;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * TNVLegendPanel
 */
public class TNVLegendPanel extends JPanel {

	private static final int PANEL_HEIGHT = 18;
	private long startTime = 0;
	private long endTime = 0;

	/**
	 * Constructor
	 */
	protected TNVLegendPanel() {
		super();

		// Listen for changes in preference to update colors
		TNVPreferenceData.getInstance().addPreferenceChangeListener( new ChangeListener() {
			public void stateChanged( ChangeEvent evt ) {
				if ( evt.getSource().equals( TNVPreferenceData.getInstance() ) ) 
					TNVLegendPanel.this.repaint();
			}
		} );

		// Listen for changes in the times to set column time interval
		String[] listenProps = { TNVModel.PROPERTY_VISIBLE_START_TIME, TNVModel.PROPERTY_VISIBLE_END_TIME };
		TNVModel.getInstance().addPropertyChangeListener( listenProps, new PropertyChangeListener() {
			public void propertyChange( PropertyChangeEvent evt ) {
				String property = evt.getPropertyName();
				if ( property.equals( TNVModel.PROPERTY_VISIBLE_START_TIME ) ) {
					Timestamp t = ( (Timestamp) evt.getNewValue() );
					if ( t != null )
						TNVLegendPanel.this.startTime = t.getTime();
					else
						TNVLegendPanel.this.startTime = 0;
					TNVLegendPanel.this.repaint();
				}
				else if ( property.equals( TNVModel.PROPERTY_VISIBLE_END_TIME ) ) {
					Timestamp t = ( (Timestamp) evt.getNewValue() );
					if ( t != null )
						TNVLegendPanel.this.endTime = t.getTime();
					else
						TNVLegendPanel.this.endTime = 0;
					TNVLegendPanel.this.repaint();
				}
			}
		} );

		this.setPreferredSize( new Dimension( this.getWidth(), PANEL_HEIGHT ) );
		this.setBackground( TNVUtil.BG_COLOR );
	}


	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paint(java.awt.Graphics)
	 */
	@Override
	public void paint( Graphics g ) {
		Graphics2D g2 = (Graphics2D) g;

		this.setPreferredSize( new Dimension( this.getWidth(), PANEL_HEIGHT ) );

		// reset display
		g2.setColor( this.getBackground() );
		g2.fillRect( 0, 0, this.getWidth(), this.getHeight() );

		int y = 3;
		int x = 5;
		int sz = 11; // height and width of color rectangle

		// draw protocol color labels
		g2.setFont( TNVUtil.SMALL_LABEL_FONT );
		g2.setColor( Color.BLACK );

		g2.drawString( "Protocol colors", x, y + sz );

		x += 85;
		g2.setColor( TNVPreferenceData.getInstance().getTcpColor() );
		g2.fillRect( x, y, sz, sz );
		g2.drawString( "TCP", x + sz + 2, y + sz );

		x += sz + 30;
		g2.setColor( TNVPreferenceData.getInstance().getUdpColor() );
		g2.fillRect( x, y, sz, sz );
		g2.drawString( "UDP", x + sz + 2, y + sz );

		x += sz + 30;
		g2.setColor( TNVPreferenceData.getInstance().getIcmpColor() );
		g2.fillRect( x, y, sz, sz );
		g2.drawString( "ICMP", x + sz + 2, y + sz );

		// tcp flags
		x += sz + 35;
		g2.setColor( TNVPreferenceData.getInstance().getSynColor() );
		g2.fillRect( x, y, sz, sz );
		g2.drawString( "SYN", x + sz + 2, y + sz );

		x += sz + 25;
		g2.setColor( TNVPreferenceData.getInstance().getAckColor() );
		g2.fillRect( x, y, sz, sz );
		g2.drawString( "ACK", x + sz + 2, y + sz );

		x += sz + 25;
		g2.setColor( TNVPreferenceData.getInstance().getFinColor() );
		g2.fillRect( x, y, sz, sz );
		g2.drawString( "FIN", x + sz + 2, y + sz );

		x += sz + 25;
		g2.setColor( TNVPreferenceData.getInstance().getUrgColor() );
		g2.fillRect( x, y, sz, sz );
		g2.drawString( "URG", x + sz + 2, y + sz );

		x += sz + 25;
		g2.setColor( TNVPreferenceData.getInstance().getPshColor() );
		g2.fillRect( x, y, sz, sz );
		g2.drawString( "PSH", x + sz + 2, y + sz );

		x += sz + 25;
		g2.setColor( TNVPreferenceData.getInstance().getRstColor() );
		g2.fillRect( x, y, sz, sz );
		g2.drawString( "RST", x + sz + 2, y + sz );

		// draw host color labels
		x += 50;
		g2.setColor( Color.BLACK );
		g2.drawString( "Host colors", x, y + sz );
		x += 65;
		SortedMap colorMap = TNVUtil.getColorMap( TNVPreferenceData.getInstance().getColorMapIndex() );
		int boxNum = 8;
		int c = 1;
		int colorInterval = ( colorMap.size() / boxNum );
		for ( int i = 0; i < boxNum; i++ ) {
			// Set color and fill in colored rectangle
			g2.setColor( (Color) colorMap.get( new Integer( c ) ) );
			Rectangle2D r = new Rectangle2D.Float( x, y, ( sz * 2 ), sz );
			g2.fill( r );
			g2.setColor( Color.darkGray );
			g2.draw( r );
			// Reset color and draw size
			if ( c < ( colorMap.size() / 2 ) )
				g2.setColor( Color.darkGray );
			else
				g2.setColor( Color.lightGray );
			String l = "" + c;
			g2.drawString( l, (int) r.getCenterX() - ( g2.getFontMetrics().stringWidth( l ) / 2 ), y + sz - 2 );
			c += colorInterval;
			x += ( sz * 2 );
		}

		// Draw time interval string (convert from milliseconds to seconds)
		if ( this.startTime != 0 && this.endTime != 0 ) {
			int numColumns = TNVPreferenceData.getInstance().getColumnCount();
			g2.setColor( Color.BLACK );
			float diff = this.endTime - this.startTime;
			DecimalFormat myFormatter = new DecimalFormat( "##.#" );
			String display = myFormatter.format( ( diff / numColumns ) ) + " ms";
			myFormatter = new DecimalFormat( "##.##" );
			if ( ( diff / numColumns ) > TNVUtil.ONE_HR )
				display = myFormatter.format( TNVUtil.convertMillisecondsToHours( ( diff / numColumns ) ) )
						+ " hr";
			else if ( ( diff / numColumns ) > TNVUtil.ONE_MIN )
				display = myFormatter.format( TNVUtil.convertMillisecondsToMinutes( ( diff / numColumns ) ) )
						+ " min";
			else if ( ( diff / numColumns ) > TNVUtil.ONE_SEC )
				display = myFormatter.format( TNVUtil.convertMillisecondsToSeconds( ( diff / numColumns ) ) )
						+ " sec";
			g2.drawString( "Column time interval: " + display, x + 40, y + sz );
		}

	}
}
