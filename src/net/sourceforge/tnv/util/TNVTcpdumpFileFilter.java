/**
 * Created on Aug 11, 2004
 * @author jgood
 * 
 * File Filter for tcpdump files
 */
package net.sourceforge.tnv.util;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * @author jgood
 * created on: Apr 29, 2006
 */
public class TNVTcpdumpFileFilter extends FileFilter {

	/* (non-Javadoc)
	 * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
	 */
	@Override
	public boolean accept( File f ) {
		if ( f.isDirectory() ) return true;

		String extension = getExtension( f );
		if ( extension != null ) {
			if ( extension.equals( "tcpdump" ) || extension.equals( "dump" ) || extension.equals( "tcpd" )
					|| extension.equals( "pcap" ) || extension.equals( "cap" ) ) {
				return true;
			}
			return false;
		}
		return false;
	}


	/* (non-Javadoc)
	 * @see javax.swing.filechooser.FileFilter#getDescription()
	 */
	@Override
	public String getDescription( ) {
		return "tcpdump/pcap files";
	}


	/**
	 * @param f
	 * @return extension
	 */
	private static String getExtension( File f ) {
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf( '.' );
		if ( i > 0 && i < s.length() - 1 ) ext = s.substring( i + 1 ).toLowerCase();
		return ext;
	}
}
