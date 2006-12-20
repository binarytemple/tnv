/**
 * Created on Dec 20, 2006
 * @author jgood
 * 
 * Abstract database class for common host methods 
 */

package net.sourceforge.tnv;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class TNVDbAbstract {

	// Map of local hosts' packets - hostname: map (timestamp: list (packets))
	private Map<String, SortedMap<Timestamp, List<TNVPacket>>> localHostMap = 
		new HashMap<String, SortedMap<Timestamp, List<TNVPacket>>>();

	// List of arrival order for local hosts
	private List<String> localHostArrivalList = new ArrayList<String>();
	
	// Set of remote hosts
	private Set<String> remoteHostList = new HashSet<String>();
	
	// List of arrival order for remote hosts
	private List<String> remoteHostArrivalList = new ArrayList<String>();

	public void clearHosts() {
		this.localHostMap.clear();
		this.localHostArrivalList.clear();
		this.remoteHostList.clear();
		this.remoteHostArrivalList.clear();
	}
	
	/**
	 * inserts a host into the local or remote set
	 * @param name the String name to add to lists
	 */
	public void addHost( String name ) {
		if ( TNVUtil.isOnHomeNet( name, TNVPreferenceData.getInstance().getHomeNet() ) ) {
			if ( ! this.localHostMap.containsKey(name) ) {
				this.localHostMap.put( name, new TreeMap<Timestamp, List<TNVPacket>>() );
				if ( ! this.localHostArrivalList.contains(name) )
					this.localHostArrivalList.add(name);
			}
		}
		else {
			if ( ! this.remoteHostList.contains(name) ) {
				this.remoteHostList.add( name );
				if ( ! this.remoteHostArrivalList.contains(name) )
					this.remoteHostArrivalList.add(name);
			}
		}
	}


	/**
	 *
	 * @return the List<String> TNVDbAbstract.java
	 */
	public final List<String> getLocalHostArrivalList() {
		return this.localHostArrivalList;
	}


	/**
	 *
	 * @return the Map<String,SortedMap<Timestamp,List<TNVPacket>>> TNVDbAbstract.java
	 */
	public final Map<String, SortedMap<Timestamp, List<TNVPacket>>> getLocalHostMap() {
		return this.localHostMap;
	}

	/**
	 * @return the localHostMap
	 * @param the host to get the map for
	 */
	public final SortedMap<Timestamp, List<TNVPacket>> getLocalHostMap( String host ) {
		return this.localHostMap.get( host );
	}

	/**
	 *
	 * @return the List<String> TNVDbAbstract.java
	 */
	public final List<String> getRemoteHostArrivalList() {
		return this.remoteHostArrivalList;
	}


	/**
	 *
	 * @return the Set<String> TNVDbAbstract.java
	 */
	public final Set<String> getRemoteHostList() {
		return this.remoteHostList;
	}

	
}
