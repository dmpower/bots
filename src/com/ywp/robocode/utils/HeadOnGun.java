/**
 *
 */
package com.ywp.robocode.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import robocode.AdvancedRobot;
import robocode.Bullet;
import robocode.BulletHitEvent;
import robocode.util.Utils;

/**
 * @author dpower
 *
 */
public class HeadOnGun implements Gun {

	private AdvancedRobot owningBot;
	private TargetBot lastTarget = null;
	private Map<String,Vector<Bullet>> bullets = new HashMap<>();
	private Map<String,GunStats> stats = new HashMap<>();

	public HeadOnGun(AdvancedRobot owner) {
		this.owningBot = owner;
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#feedTarget(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public void feedTarget(TargetBot target) {
		this.lastTarget = target;
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#aimRadians(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public double aimRadians(TargetBot target) {
		this.lastTarget = target;
		double absBearing=this.lastTarget.getBearingRadians()+this.owningBot.getHeadingRadians();
		double targetAngle = Utils.normalRelativeAngle(absBearing-this.owningBot.getGunHeadingRadians());
		this.owningBot.setTurnGunRightRadians(targetAngle);
		return Utils.normalRelativeAngle(absBearing-this.owningBot.getGunHeadingRadians());
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#aim(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public double aim(TargetBot target) {
		return Utils.normalRelativeAngleDegrees(aimRadians(target) * 180.0 / Math.PI);
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#fire()
	 */
	@Override
	public boolean fire() {
		boolean results = false;
		if(this.owningBot.getGunHeat() == 0 && this.owningBot.getGunTurnRemainingRadians()==0){
			double power = Math.min(2.4,Math.min(this.lastTarget.getEnergy()/4,this.owningBot.getEnergy()/10));
			Bullet theBullet = this.owningBot.setFireBullet(power);
			if( ! this.bullets.containsKey(this.lastTarget.getName())){
				this.bullets.put(this.lastTarget.getName(), new Vector<>());
			}
			this.bullets.get(this.lastTarget.getName()).add(theBullet);
			if( ! this.stats.containsKey(this.lastTarget.getName())){
				this.stats.put(this.lastTarget.getName(), new GunStats());
			}
			this.stats.get(this.lastTarget.getName()).addShot();
			results = true;
		}
		return results;
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#update()
	 */
	@Override
	public void update() {
		//Vector<Bullet> expired = new Vector<Bullet>();
		for (Entry<String, Vector<Bullet>> target : this.bullets.entrySet()) {
			for (Bullet bullet : target.getValue()) {
				if(!bullet.isActive()){
					//expired.addElement(bullet);
					if(!this.stats.containsKey(target.getKey()))
					{
						this.stats.put(target.getKey(), new GunStats());
					}
					if (target.getKey().equals(bullet.getVictim())){
						// basically if I hit my intended target, add a hit
						this.stats.get(target.getKey()).addHit();
					}
					target.getValue().remove(bullet); // I hope this works correctly
				}
			}
		}
		//this.bullets.fremoveAll(expired);
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#update(robocode.BulletHitEvent)
	 */
	@Override
	public void update(BulletHitEvent event) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#isValid(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public boolean isValid(TargetBot target) {
		// this gun is always valid
		return true;
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#isActive()
	 */
	@Override
	public boolean isActive() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#getStats()
	 */
	@Override
	public GunStats getStats() {
		int hitTotal = 0;
		int shotTotal = 0;
		for (GunStats curStat : this.stats.values()) {
			hitTotal += curStat.getHits();
			shotTotal += curStat.getShots();
		}
		return new GunStats(shotTotal, hitTotal);
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#getStats(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public GunStats getStats(TargetBot target) {
		GunStats results = this.stats.get(target.getName());
		if(null == results){
			results = new GunStats();
		}
		return results;
	}

}
