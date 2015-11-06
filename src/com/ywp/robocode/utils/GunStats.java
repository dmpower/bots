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

	public void addShot(){
		this.shots++;
	}

	public void addHit(){
		this.hits++;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString(){
		return "Hits: " + this.hits + " Shots: " + this.shots;
	}

}
