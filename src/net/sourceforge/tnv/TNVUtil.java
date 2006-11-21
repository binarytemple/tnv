/**
 * Created on Mar 23, 2004 
 * @author jgood
 * 
 * Utility class for commonly used static methods and variables
 */
package net.sourceforge.tnv;

import java.awt.Color;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import net.sourceforge.jpcap.net.IPPort;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;

/**
 * @author jgood
 * created on: Apr 29, 2006
 */
public final class TNVUtil {

	// translation from milliseconds to larger units
	protected static final long ONE_SEC = 1000; // 1 s = 1000 ms * 1 s = 1000 ms
	protected static final long ONE_MIN = 60000; // 1 m = 1000 ms * 60 s = 60000 ms
	protected static final long ONE_HR = 3600000; // 1 h = 1000 ms * 60 s * 60 m = 3600000 ms

	protected static final Color BG_COLOR = new Color( 230, 230, 230 );

	protected static final Font LABEL_FONT = new Font( "SanSerif", Font.PLAIN, 11 );
	protected static final Font LARGE_LABEL_FONT = new Font( "SanSerif", Font.BOLD, 14 );
	protected static final Font SMALL_LABEL_FONT = new Font( "SanSerif", Font.PLAIN, 10 );

	protected static final SimpleDateFormat SHORT_FORMAT = new SimpleDateFormat( "HH:mm:ss" );
	protected static final SimpleDateFormat NORMAL_FORMAT = new SimpleDateFormat( "MM-dd-yy HH:mm:ss" );
	protected static final SimpleDateFormat LONG_FORMAT = new SimpleDateFormat( "MM-dd-yy HH:mm:ss.SSS" );
	protected static final SimpleDateFormat DATE_ONLY_FORMAT = new SimpleDateFormat( "MM-dd-yy" );
	protected static final SimpleDateFormat SHORT_TIME_ONLY_FORMAT = new SimpleDateFormat( "HH:mm" );
	protected static final SimpleDateFormat TIME_ONLY_FORMAT = new SimpleDateFormat( "HH:mm:ss.SSS" );

	protected static final PText DEFAULT_TOOLTIP_NODE;
	
	protected static final SortedMap<Integer, SortedMap> colorMaps;

	protected static final int BLUE_COLOR_MAP = 0;
	protected static final int YELLOW_TO_BLUE_COLOR_MAP = 1;
	protected static final int GREEN_COLOR_MAP = 2;
	protected static final int YELLOW_TO_GREEN_COLOR_MAP = 3;
	protected static final int PURPLE_COLOR_MAP = 4;
	protected static final int BROWN_COLOR_MAP = 5;
	protected static final int GRAY_COLOR_MAP = 6;

	protected static final int BLUE_SMALL_COLOR_MAP = 7;
	protected static final int YELLOW_TO_BLUE_SMALL_COLOR_MAP = 8;
	protected static final int GREEN_SMALL_COLOR_MAP = 9;
	protected static final int YELLOW_TO_GREEN_SMALL_COLOR_MAP = 10;
	protected static final int PURPLE_SMALL_COLOR_MAP = 11;
	protected static final int BROWN_SMALL_COLOR_MAP = 12;
	protected static final int GRAY_SMALL_COLOR_MAP = 13;

	static {
		SortedMap<Integer, SortedMap> maps = new TreeMap<Integer, SortedMap>();
		// Set up the color map, based on number of packets (500 or 100)
		/*
		 * Colors were selected using Cynthia A. Brewer's excellent ColorBrewer:
		 * http://www.personal.psu.edu/cab38/ColorBrewer/ColorBrewer_intro.html
		 */
		
		maps.put(new Integer(BLUE_COLOR_MAP), 
				createColorMap( 1, 500, new Color( 241, 238, 246 ), new Color( 3, 78, 123 ) ));
		maps.put(new Integer(YELLOW_TO_BLUE_COLOR_MAP), 
				createColorMap( 1, 500, new Color( 255, 255, 204 ), new Color( 12, 44, 132 ) ));
		maps.put(new Integer(GREEN_COLOR_MAP), 
				createColorMap( 1, 500, new Color( 237, 248, 251 ), new Color( 0, 88, 36 ) ));
		maps.put(new Integer(YELLOW_TO_GREEN_COLOR_MAP), 
				createColorMap( 1, 500, new Color( 255, 255, 204 ), new Color( 0, 90, 50 ) ));
		maps.put(new Integer(PURPLE_COLOR_MAP), 
				createColorMap( 1, 500, new Color( 237, 248, 251 ), new Color( 110, 1, 107 ) ));
		maps.put(new Integer(BROWN_COLOR_MAP), 
				createColorMap( 1, 500, new Color( 254, 237, 222 ), new Color( 140, 45, 4 ) ));
		maps.put(new Integer(GRAY_COLOR_MAP), 
				createColorMap( 1, 500, new Color( 247, 247, 247 ), new Color( 37, 37, 37 ) ));

		maps.put(new Integer(BLUE_SMALL_COLOR_MAP), 
				createColorMap( 1, 100, new Color( 241, 238, 246 ), new Color( 3, 78, 123 ) ));
		maps.put(new Integer(YELLOW_TO_BLUE_SMALL_COLOR_MAP), 
				createColorMap( 1, 100, new Color( 255, 255, 204 ), new Color( 12, 44, 132 ) ));
		maps.put(new Integer(GREEN_SMALL_COLOR_MAP), 
				createColorMap( 1, 100, new Color( 237, 248, 251 ), new Color( 0, 88, 36 ) ));
		maps.put(new Integer(YELLOW_TO_GREEN_SMALL_COLOR_MAP), 
				createColorMap( 1, 100, new Color( 255, 255, 204 ), new Color( 0, 90, 50 ) ));
		maps.put(new Integer(PURPLE_SMALL_COLOR_MAP), 
				createColorMap( 1, 100, new Color( 237, 248, 251 ), new Color( 110, 1, 107 ) ));
		maps.put(new Integer(BROWN_SMALL_COLOR_MAP), 
				createColorMap( 1, 100, new Color( 254, 237, 222 ), new Color( 140, 45, 4 ) ));
		maps.put(new Integer(GRAY_SMALL_COLOR_MAP), 
				createColorMap( 1, 100, new Color( 247, 247, 247 ), new Color( 37, 37, 37 ) ));

		colorMaps = new TreeMap<Integer, SortedMap>(maps);
		
		// create default tooltip node
		PText tempTooltip = new PText();
		tempTooltip.setFont( new Font( "SanSerif", Font.PLAIN, 14 ) );
		//tempTooltip.setPaint( new Color( 250, 240, 100 ) );
		tempTooltip.setPaint( Color.LIGHT_GRAY );
		tempTooltip.setTextPaint( Color.BLACK );
		tempTooltip.setTransparency( 0.9f );
		tempTooltip.setPickable( false );
		DEFAULT_TOOLTIP_NODE = (PText) tempTooltip.clone();
	}

	// Conversion utilities
	protected static long convertMillisecondsToSeconds( long ms ) {
		return ( ms / ONE_SEC );
	}


	protected static long convertMillisecondsToMinutes( long ms ) {
		return ( ms / ONE_MIN );
	}


	protected static long convertMillisecondsToHours( long ms ) {
		return ( ms / ONE_HR );
	}


	protected static float convertMillisecondsToSeconds( float ms ) {
		return ( ms / ONE_SEC );
	}


	protected static float convertMillisecondsToMinutes( float ms ) {
		return ( ms / ONE_MIN );
	}


	protected static float convertMillisecondsToHours( float ms ) {
		return ( ms / ONE_HR );
	}


	protected static long convertSecondsToMilliseconds( long s ) {
		return ( s * ONE_SEC );
	}


	protected static long convertMinutesToMilliseconds( long s ) {
		return ( s * ONE_MIN );
	}


	/**
	 * determine if the host is on the "home" network
	 * @param host
	 * @param net
	 * @return is on home net
	 */
	protected static boolean isOnHomeNet( String host, String net ) {
		StringTokenizer stNet = new StringTokenizer( net, "." );
		StringTokenizer stHost = new StringTokenizer( host, "." );
		while ( stHost.hasMoreTokens() & stNet.hasMoreTokens() ) {
			String h = stHost.nextToken();
			String n = stNet.nextToken();
			if ( !n.equals( "0" ) & !h.equals( n ) ) 
				return false;
		}
		return true;
	}

	/**
	 * get the home network string up to the 0
	 * @param net
	 * @return homeNet
	 */
	protected static String getHomeNetString( String net ) {
		String homeNet = "";
		StringTokenizer tok = new StringTokenizer( net, "." );
		while ( tok.hasMoreTokens() ) {
			String octet = tok.nextToken();
			if ( ! octet.equals( "0" ) ) 
				homeNet += octet + ".";
		}
		return homeNet;
	}

	/**
	 * Returns a color based on some integer in the colorMap
	 * @param s
	 * @param mapType
	 * @return color
	 */
	protected static final Color getColor( int s, int mapType ) {
		SortedMap<Integer, Color> colors = colorMaps.get( new Integer(mapType) );
		int size = colors.size();
		if ( s > size )
			return colors.get(new Integer(size));
		return colors.get(new Integer(s));
	}


	/**
	 * @return Returns a colorMap.
	 */
	protected static final SortedMap<Integer, Color> getColorMap( int mapType ) {
		return colorMaps.get( new Integer(mapType) );
	}


	/**
	 * Get textual description of port if available, otherwise use numeric value
	 * @param port
	 * @return port description
	 */
	protected static String getPortDescr( int port ) {
		String s = IPPort.getDescription( port );
		if ( s.equals( "unknown" ) ) s = "" + port;
		return s;
	}


	/**
	 * Create color maps
	 * @param startNum
	 * @param maxNum
	 * @param start
	 * @param end
	 * @return sortedmap
	 */
	private static final SortedMap<Integer, Color> createColorMap( int startNum, int maxNum, Color start, Color end ) {
		SortedMap<Integer, Color> tmpColMap = new TreeMap<Integer, Color>();
		Color c1 = start, c2 = end;
		for ( int i = startNum; i < maxNum; i++ ) {
			float ratio = (float) i / (float) maxNum;
			int red = (int) ( c2.getRed() * ratio + c1.getRed() * ( 1 - ratio ) );
			int green = (int) ( c2.getGreen() * ratio + c1.getGreen() * ( 1 - ratio ) );
			int blue = (int) ( c2.getBlue() * ratio + c1.getBlue() * ( 1 - ratio ) );
			Color c = new Color( red, green, blue );
			tmpColMap.put( new Integer( i ), c );
		}
		return tmpColMap;
	}

}
