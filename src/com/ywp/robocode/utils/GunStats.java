/**
 *
 */
package com.ywp.robocode.utils;

/**
 * @author dpower
 *
 */
public class GunStats {
	private int shots;
	private int hits;

	public GunStats() {
		this(0,0);
	}

	public GunStats(int shots, int hits)
	{
		this.shots = shots;
		this.hits  = hits;
	}

	public int getShots() {
		return this.shots;
	}

	public int getHits() {
		return this.hits;
	}



}
