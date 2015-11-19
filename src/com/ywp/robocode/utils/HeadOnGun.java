/**
 *
 */
package com.ywp.robocode.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.PrintStream;
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
	private static Map<String,GunStats> stats = new HashMap<>();
	private double ray;
	private long lastFired = 0;

	public HeadOnGun(AdvancedRobot owner) {
		this.owningBot = owner;
		this.ray = this.owningBot.getBattleFieldHeight() + this.owningBot.getBattleFieldWidth();
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
		feedTarget(target);
		double targetAngle = HeadOnGun.aimRadians(this.owningBot, target.getPoint());
		this.owningBot.setTurnGunRightRadians(targetAngle);
		return targetAngle;
	}

	public static double aimRadians (AdvancedRobot theBot, Point targetPoint) {
		double absBearing = BotTools.convertToPoint(theBot).angleRadians(targetPoint);
		double targetAngle = Utils.normalRelativeAngle(absBearing-theBot.getGunHeadingRadians());
		return targetAngle;
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#aim(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public double aim(TargetBot target) {
		return Utils.normalRelativeAngleDegrees(Math.toDegrees(aimRadians(target)));
	}

	/**
	 * Aim head on to a point
	 * @param targetPoint - aim at this point
	 * @return - the degrees adjustment to turn the gun to aim at the point
	 */
	public static double aim(AdvancedRobot theBot, Point targetPoint) {
		return Utils.normalRelativeAngleDegrees(Math.toDegrees(HeadOnGun.aimRadians(theBot, targetPoint)));
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#fire()
	 */
	@Override
	public boolean fire() {
		boolean results = false;
		if(this.owningBot.getGunHeat() == 0 && this.owningBot.getGunTurnRemainingRadians() < (GUN_TURN_THRESHOLD)){
			double power = Math.min(2.4,Math.min(this.lastTarget.getEnergy()/4,this.owningBot.getEnergy()/10));
			Bullet theBullet = this.owningBot.setFireBullet(power);
			if(null != theBullet){
				if( ! this.bullets.containsKey(this.lastTarget.getName())){
					this.bullets.put(this.lastTarget.getName(), new Vector<>());
				}
				this.bullets.get(this.lastTarget.getName()).add(theBullet);
				if( ! HeadOnGun.stats.containsKey(this.lastTarget.getName())){
					HeadOnGun.stats.put(this.lastTarget.getName(), new GunStats());
				}
				HeadOnGun.stats.get(this.lastTarget.getName()).addShot();
				this.lastFired = this.owningBot.getTime();
				results = true;
			}
		}
		return results;
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#update()
	 */
	@Override
	public void update() {
		Vector<Bullet> expired = new Vector<Bullet>();
		for (Entry<String, Vector<Bullet>> target : this.bullets.entrySet()) {
			for (Bullet bullet : target.getValue()) {
				if(!bullet.isActive()){
					expired.addElement(bullet);
					if(!HeadOnGun.stats.containsKey(target.getKey()))
					{
						HeadOnGun.stats.put(target.getKey(), new GunStats());
					}
					if (target.getKey().equals(bullet.getVictim())){
						// basically if I hit my intended target, add a hit
						HeadOnGun.stats.get(target.getKey()).addHit();
					}
					this.owningBot.out.println(this.getClass().getName() + " - time: " + this.owningBot.getTime() + " target: " + target.getKey() + " Bullet: " + bullet.toString());
					//target.getValue().remove(bullet); // this does not work
				}
			}
		}

		// I hate doing this second pass, but the loop above will not allow me to delete it at the same time.
		for (Entry<String, Vector<Bullet>> target : this.bullets.entrySet()) {
			target.getValue().removeAll(expired);
		}
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#update(robocode.BulletHitEvent)
	 */
	@Override
	public void update(BulletHitEvent event) {
		// TODO Auto-generated method stub
		// do nothing?
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#isValid(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public boolean isValid(TargetBot target) {
		// this gun is always valid
		return isActive();
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#isActive()
	 */
	@Override
	public boolean isActive() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#getStats()
	 */
	@Override
	public GunStats getStats() {
		int hitTotal = 0;
		int shotTotal = 0;
		for (GunStats curStat : HeadOnGun.stats.values()) {
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
		GunStats results = HeadOnGun.stats.get(target.getName());
		if(null == results){
			results = new GunStats();
		}
		return results;
	}

	@Override
	public void printAllStats(PrintStream out){
		out.println(this.getClass().getName() + " - collected stats");
		for ( Entry<String, GunStats> curEntry : HeadOnGun.stats.entrySet()) {
			out.print(curEntry.getKey() + " - ");
			out.println(curEntry.getValue().toString());
		}
		out.println("Total: " + getStats().toString());
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#onPaint(java.awt.Graphics2D)
	 */
	@Override
	public void onPaint(Graphics2D g){
		// note, this seems to run before all other events, so it is one turn behind with what it is drawing.
		if(this.lastTarget!=null) {
			//			double absBearing=this.lastTarget.getAbsBearingRadians();
			Point target = this.lastTarget.getPoint();
			double absBearing = BotTools.convertToPoint(this.owningBot).angleRadians(target);
			target.drawBot(this.owningBot.getGraphics(), Color.red);

			Point rayPoint = BotTools.project(BotTools.convertToPoint(this.owningBot),this.ray,absBearing);
			g.setColor(Color.red);
			g.drawLine((int)this.owningBot.getX(), (int)this.owningBot.getY(), (int)rayPoint.getX(), (int)rayPoint.getY());
		}
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#lastFired()
	 */
	@Override
	public long lastFired() {
		return this.lastFired;
	}

}