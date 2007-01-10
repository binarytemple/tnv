/**
 * Created on Mar 20, 2006
 * @author jgood
 * 
 * Piccolo PNode super class for local and remote host representations
 */
package net.sourceforge.tnv.ui;

import java.net.InetAddress;
import java.net.UnknownHostException;

import edu.umd.cs.piccolo.PNode;

/**
 * TNVHost
 */
public abstract class TNVHost extends PNode {

	/**
	 * Property string for if this node is currently selected
	 */
	public static final String PROPERTY_SELECTED_NODE = "selectedHost";
	
	/**
	 * String IP address of host
	 */
	private String name;

	/**
	 * frequency of total number of packets for this
	 */
	private int frequency = 0;


	/**
	 * @param select
	 */
	public abstract void setSelected( boolean select );

	/**
	 * @param select
	 */
	public abstract void setSelectedLink( boolean select );

	
	/**
	 * @return the name
	 */
	public String getName( ) {
		return this.name;
	}

	/**
	 * @param name the name to set
	 */
	public final void setName( String name ) {
		this.name = name;
	}

	/**
	 * @return the ipAddress
	 */
	public InetAddress getIpAddress( ) throws UnknownHostException {
		return InetAddress.getByName( this.name );
	}

	/**
	 * @return the frequency
	 */
	public int getFrequency( ) {
		return this.frequency;
	}

	/**
	 * @param frequency the frequency to set
	 */
	public final void setFrequency( int frequency ) {
		this.frequency = frequency;
	}

	/**
	 * @param other
	 * @return -1 for less, 0 for equals, 1 for more
	 */
	public int compareTo( Object other ) {
		TNVHost otherHost = (TNVHost) other;
		try {
			byte thisName[] = this.getIpAddress().getAddress();
			byte otherName[] = otherHost.getIpAddress().getAddress();
			for ( int i = 0; i < thisName.length; i++ ) {
				int thisInt = thisName[i], otherInt = otherName[i];
				if ( thisName[i] < 0 ) thisInt += 256;
				if ( otherName[i] < 0 ) otherInt += 256;

				if ( thisInt - otherInt > 0 )
					return -1;
				else if ( thisInt - otherInt < 0 ) return 1;
			}
		}
		// TODO: fix in case cant get address
		catch ( Exception ex ) {

		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode( ) {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ( ( this.name == null ) ? 0 : this.name.hashCode() );
		result = PRIME * result + this.frequency;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( this.getClass() != obj.getClass() )
			return false;
		final TNVHost other = (TNVHost) obj;
		if ( this.name == null ) {
			if ( other.name != null ) 
				return false;
		}
		else if ( !this.name.equals( other.name ) ) 
			return false;
		if ( this.frequency != other.frequency ) 
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.piccolo.PNode#toString()
	 */
	@Override
	public String toString( ) {
		return this.name + ": " + super.toString();
	}

}