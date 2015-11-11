/**
 *
 */
package com.ywp.robocode.utils;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import robocode.AdvancedRobot;
import robocode.Bullet;
import robocode.BulletHitEvent;
import robocode.Rules;
import robocode.util.Utils;

/**
 * @author dpower
 *
 */
public class CircularGun implements Gun {

	private TargetBot lastTarget = null;

	private RepositoryManager<TargetBot> targetRepository;
	private AdvancedRobot owningBot;
	private RepositoryManager<BulletData> bullets = new RepositoryManager<>();
	private Map<String,GunStats> stats = new HashMap<>();

	private static Rectangle2D.Double _battleField = null;

	/**
	 * @param targetRepository
	 * @param owningBot
	 */
	public CircularGun(RepositoryManager<TargetBot> targetRepository, AdvancedRobot owningBot) {
		this.targetRepository = targetRepository;
		this.owningBot = owningBot;
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#feedTarget(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public void feedTarget(TargetBot target) {
		this.targetRepository.add(target);
		this.lastTarget = target;
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#aimRadians(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public double aimRadians(TargetBot target) {
		if (! isValid(target)) {
			throw new IllegalStateException("Cannot aim an invalid gun. Please call isValid first.");
		}
		feedTarget(target);
		double botSize = owningBot.getWidth();
		double botHalfSize = botSize/2;
		Rectangle2D.Double battleField =
				new Rectangle2D.Double(botHalfSize, botHalfSize,
						owningBot.getBattleFieldWidth()-botSize, owningBot.getBattleFieldHeight()-botSize);
		double bulletPower = getPower(target);
		double bulletSpeed = Rules.getBulletSpeed(bulletPower);
		Vector<TargetBot> targetData = this.targetRepository.getAllData(target);
		TargetBot targetEntry1 = targetData.get(0);
		TargetBot targetEntry2 = targetData.get(1);
		double turnRate = targetEntry1.getHeadingRadians() - targetEntry2.getHeadingRadians();
		double speed = targetEntry1.getVelocity();
		double predictedHeading = targetEntry1.getHeadingRadians();
		double absBearing=targetEntry1.getBearingRadians()+this.owningBot.getHeadingRadians();
		Point origin = BotTools.convertToPoint(this.owningBot);
		Point predictedPosition = BotTools.project(origin, targetEntry1.getDistance(), absBearing);
		double timeDelta = 0;
		while ( (++timeDelta) * bulletSpeed < origin.distance(predictedPosition) ){
			predictedPosition = BotTools.project(predictedPosition, speed, predictedHeading);
			predictedHeading += turnRate;
			if (! battleField.contains(predictedPosition)) {
				predictedPosition.x = Math.min(botHalfSize, Math.max(owningBot.getBattleFieldWidth()-botHalfSize, predictedPosition.getX()));
				predictedPosition.y = Math.min(botHalfSize, Math.max(owningBot.getBattleFieldHeight()-botHalfSize, predictedPosition.getY()));
				break; // hit wall we are done
			}
		}

		double firingAdjustment = Utils.normalAbsoluteAngle(origin.angleRadians(predictedPosition)-owningBot.getGunHeadingRadians());
		owningBot.setTurnGunRight(firingAdjustment);
		return firingAdjustment;
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
		// TODO Auto-generated method stub
		double bulletPower = getPower(this.lastTarget);
		boolean results = false;
		if(this.owningBot.getGunHeat() == 0 && this.owningBot.getGunTurnRemainingRadians() < (GUN_TURN_THRESHOLD)){
			Bullet theBullet = this.owningBot.setFireBullet(bulletPower);
			BulletData newEntry = new BulletData(theBullet, this.lastTarget, BotTools.convertToPoint(this.owningBot));
			this.bullets.add(newEntry);
			results = true;
		}
		return results;
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#update()
	 */
	@Override
	public void update() {
		Vector<RepositoryEntry<BulletData>> expired = new Vector<>();
		for (String target : this.bullets.getAllGroupIds()) {
			for (BulletData bulletData : this.bullets.getAllData(target)) {
				Bullet bullet = bulletData.getBullet();
				if(!bullet.isActive()){
					expired.addElement(bulletData);
					if(!this.stats.containsKey(target))
					{
						this.stats.put(target, new GunStats());
					}
					if (target.equals(bullet.getVictim())){
						// basically if I hit my intended target, add a hit
						this.stats.get(target).addHit();
					}
					this.owningBot.out.println(this.getClass().getName() + " - time: " + this.owningBot.getTime() + " target: " + target + " Bullet: " + bullet.toString());
				}
			}
		}

		this.bullets.remove(expired);
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
		boolean results = isActive();
		if(results) {
			Vector<TargetBot> targetData = this.targetRepository.getAllData(target);
			results = targetData.size() > 1;
			if(results) {
				results = (targetData.get(0).getTime()-targetData.get(1).getTime()) == 1;
			}
			if(results) {
				results = (targetData.get(1).getTime()-targetData.get(2).getTime()) == 1;
			}
			if(results) {
				double speedDelta1 = targetData.get(0).getVelocity() - targetData.get(1).getVelocity();
				double speedDelta2 = targetData.get(1).getVelocity() - targetData.get(2).getVelocity();
				results = speedDelta1 == speedDelta2;
			}
			if(results) {
				double turnDelta1 = targetData.get(0).getHeadingRadians() - targetData.get(1).getHeadingRadians();
				double turnDelta2 = targetData.get(1).getHeadingRadians() - targetData.get(2).getHeadingRadians();
				results = turnDelta1 == turnDelta2;
			}
		}
		return results;
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#isActive()
	 */
	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
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

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#onPaint(java.awt.Graphics2D)
	 */
	@Override
	public void onPaint(Graphics2D g) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#printAllStats(java.io.PrintStream)
	 */
	@Override
	public void printAllStats(PrintStream out) {
		out.println(this.getClass().getName() + " - collected stats");
		for ( Entry<String, GunStats> curEntry : this.stats.entrySet()) {
			out.print(curEntry.getKey() + " - ");
			out.println(curEntry.getValue().toString());
		}
		out.println("Total: " + getStats().toString());
	}

	private double getPower(TargetBot target) {
		return Math.min(2.4,Math.min(target.getEnergy()/4,this.owningBot.getEnergy()/10));
	}

}
