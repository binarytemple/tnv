/**
 * Created on Mar 23, 2004 
 * @author jgood
 * 
 * Utility class for commonly used static methods and variables
 */
package net.sourceforge.tnv.util;

import java.awt.Color;
import java.awt.Font;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import net.sourceforge.jpcap.net.IPPort;
import net.sourceforge.tnv.dialogs.TNVErrorDialog;
import net.sourceforge.tnv.ui.TNVHost;
import edu.umd.cs.piccolo.nodes.PText;

/**
 * @author jgood
 * created on: Apr 29, 2006
 */
public final class TNVUtil {

	// translation from milliseconds to larger units
	public static final long ONE_SEC = 1000; // 1 s = 1000 ms * 1 s = 1000 ms
	public static final long ONE_MIN = 60000; // 1 m = 1000 ms * 60 s = 60000 ms
	public static final long ONE_HR = 3600000; // 1 h = 1000 ms * 60 s * 60 m = 3600000 ms

	public static final Color BG_COLOR = new Color( 230, 230, 230 );

	public static final Font LABEL_FONT = new Font( "SanSerif", Font.PLAIN, 11 );
	public static final Font LARGE_LABEL_FONT = new Font( "SanSerif", Font.BOLD, 14 );
	public static final Font SMALL_LABEL_FONT = new Font( "SanSerif", Font.PLAIN, 10 );

	public static final SimpleDateFormat SHORT_FORMAT = new SimpleDateFormat( "HH:mm:ss" );
	public static final SimpleDateFormat NORMAL_FORMAT = new SimpleDateFormat( "MM-dd-yy HH:mm:ss" );
	public static final SimpleDateFormat LONG_FORMAT = new SimpleDateFormat( "MM-dd-yy HH:mm:ss.SSS" );
	public static final SimpleDateFormat DATE_ONLY_FORMAT = new SimpleDateFormat( "MM-dd-yy" );
	public static final SimpleDateFormat SHORT_TIME_ONLY_FORMAT = new SimpleDateFormat( "HH:mm" );
	public static final SimpleDateFormat TIME_ONLY_FORMAT = new SimpleDateFormat( "HH:mm:ss.SSS" );

	public static final PText DEFAULT_TOOLTIP_NODE;
	
	public static final SortedMap<Integer, SortedMap> COLOR_MAPS;

	public static final int BLUE_COLOR_MAP = 0;
	public static final int YELLOW_TO_BLUE_COLOR_MAP = 1;
	public static final int GREEN_COLOR_MAP = 2;
	public static final int YELLOW_TO_GREEN_COLOR_MAP = 3;
	public static final int PURPLE_COLOR_MAP = 4;
	public static final int BROWN_COLOR_MAP = 5;
	public static final int GRAY_COLOR_MAP = 6;

	public static final int BLUE_SMALL_COLOR_MAP = 7;
	public static final int YELLOW_TO_BLUE_SMALL_COLOR_MAP = 8;
	public static final int GREEN_SMALL_COLOR_MAP = 9;
	public static final int YELLOW_TO_GREEN_SMALL_COLOR_MAP = 10;
	public static final int PURPLE_SMALL_COLOR_MAP = 11;
	public static final int BROWN_SMALL_COLOR_MAP = 12;
	public static final int GRAY_SMALL_COLOR_MAP = 13;

	public static final Comparator<TNVHost> ALPHA_HOST_COMPARATOR = new Comparator<TNVHost>() {
		public int compare(TNVHost s1, TNVHost s2) {
			try {
				byte host1[] = s1.getIpAddress().getAddress();
				byte host2[] = s2.getIpAddress().getAddress();
				for ( int i = 0; i < host1.length; i++ ) {
					int int1 = host1[i], int2 = host2[i];
					if ( host1[i] < 0 ) 
						int1 += 256;
					if ( host2[i] < 0 ) 
						int2 += 256;
					if ( int1 - int2 > 0 )
						return 1;
					else if ( int1 - int2 < 0 )
						return -1;
				}
			} 
			catch (UnknownHostException e ) {
				TNVErrorDialog.createTNVErrorDialog(this.getClass(),
						"Error getting InetAddress for: " + s1 + " / " + s2, e);
			}
			return 0;
		}
	};
			
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

		COLOR_MAPS = new TreeMap<Integer, SortedMap>(maps);
		
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
	public static long convertMillisecondsToSeconds( long ms ) {
		return ( ms / ONE_SEC );
	}


	public static long convertMillisecondsToMinutes( long ms ) {
		return ( ms / ONE_MIN );
	}


	public static long convertMillisecondsToHours( long ms ) {
		return ( ms / ONE_HR );
	}


	public static float convertMillisecondsToSeconds( float ms ) {
		return ( ms / ONE_SEC );
	}


	public static float convertMillisecondsToMinutes( float ms ) {
		return ( ms / ONE_MIN );
	}


	public static float convertMillisecondsToHours( float ms ) {
		return ( ms / ONE_HR );
	}


	public static long convertSecondsToMilliseconds( long s ) {
		return ( s * ONE_SEC );
	}


	public static long convertMinutesToMilliseconds( long s ) {
		return ( s * ONE_MIN );
	}


	/**
	 * determine if the host is on the "home" network
	 * @param host
	 * @param net
	 * @return is on home net
	 */
	public static boolean isOnHomeNet( String host, String net ) {
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
	public static String getHomeNetString( String net ) {
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
	public static final Color getColor( int s, int mapType ) {
		SortedMap<Integer, Color> colors = COLOR_MAPS.get( new Integer(mapType) );
		int size = colors.size();
		if ( s > size )
			return colors.get(new Integer(size));
		return colors.get(new Integer(s));
	}


	/**
	 * @return Returns a colorMap.
	 */
	public static final SortedMap<Integer, Color> getColorMap( int mapType ) {
		return COLOR_MAPS.get( new Integer(mapType) );
	}


	/**
	 * Get textual description of port if available, otherwise use numeric value
	 * @param port
	 * @return port description
	 */
	public static String getPortDescr( int port ) {
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
