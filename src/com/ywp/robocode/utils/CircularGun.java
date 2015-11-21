package com.ywp.robocode.utils;

import java.awt.Color;
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
 * This gun uses circular tracking to aim the gun head on to the
 * predicted position where the target bot and the bullet will collide.
 * It makes use of the static methods of HeadOnGun to find the final gun
 * adjustment.
 * @author dpower
 */
public class CircularGun implements Gun {

	private static final double CALULATION_BUFFER = 0.001;

	private RepositoryManager<TargetBot>  targetRepository;
	private TargetBot					  lastTarget = null;
	private RepositoryManager<BulletData> bullets	 = new RepositoryManager<>();
	private static Map<String, GunStats>  stats		 = new HashMap<>();
	private long						  lastFired	 = 0;

	private AdvancedRobot	   owningBot;
	private double			   ray;
	private Rectangle2D.Double battleField;
	private double			   botSize;
	private double			   botHalfSize;

	private Point aimPoint;

	/**
	 * @param targetRepository
	 * @param owningBot
	 */
	public CircularGun(RepositoryManager<TargetBot> targetRepository, AdvancedRobot owningBot) {
		this.targetRepository = targetRepository;
		this.owningBot = owningBot;
		this.ray = this.owningBot.getBattleFieldHeight() + this.owningBot.getBattleFieldWidth();
		this.botSize = this.owningBot.getWidth();
		this.botHalfSize = this.botSize / 2;
		this.battleField = new Rectangle2D.Double(this.botHalfSize - CALULATION_BUFFER,
				this.botHalfSize - CALULATION_BUFFER,
				this.owningBot.getBattleFieldWidth() - this.botSize + (CALULATION_BUFFER * 2),
				this.owningBot.getBattleFieldHeight() - this.botSize + (CALULATION_BUFFER * 2));

	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.ywp.robocode.utils.Gun#feedTarget(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public void feedTarget(TargetBot target) {
		this.targetRepository.add(target);
		this.lastTarget = target;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.ywp.robocode.utils.Gun#aimRadians(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public double aimRadians(TargetBot target) throws IllegalStateException {
		if (!isValid(target)) {
			throw new IllegalStateException("Cannot aim an invalid gun. Please call isValid first.");
		}
		feedTarget(target);
		double bulletPower = getPower(target);
		double bulletSpeed = Rules.getBulletSpeed(bulletPower);

		Vector<TargetBot> targetData = this.targetRepository.getAllData(target);
		TargetBot targetEntry1 = targetData.get(0);
		TargetBot targetEntry2 = targetData.get(1);

		double turnRate = targetEntry1.getHeadingRadians() - targetEntry2.getHeadingRadians();
		double speed = targetEntry1.getVelocity();
		double predictedHeading = targetEntry1.getHeadingRadians();
		double absBearing = targetEntry1.getBearingRadians() + this.owningBot.getHeadingRadians();
		Point origin = BotTools.convertToPoint(this.owningBot);
		this.aimPoint = BotTools.project(origin, targetEntry1.getDistance(), absBearing);

		double timeDelta = 0;
		while ((timeDelta++) * bulletSpeed < origin.distance(this.aimPoint)) {
			this.aimPoint = BotTools.project(this.aimPoint, speed, predictedHeading);
			predictedHeading += turnRate;
			if (!this.battleField.contains(this.aimPoint)) {
				this.aimPoint.x = Math.max(this.botHalfSize,
						Math.min(this.owningBot.getBattleFieldWidth() - this.botHalfSize, this.aimPoint.getX()));
				this.aimPoint.y = Math.max(this.botHalfSize,
						Math.min(this.owningBot.getBattleFieldHeight() - this.botHalfSize, this.aimPoint.getY()));
				break; // hit wall we are done
			}
		}

		double firingAdjustment = HeadOnGun.aimRadians(this.owningBot, this.aimPoint);
		this.owningBot.setTurnGunRightRadians(firingAdjustment);
		return firingAdjustment;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#aim(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public double aim(TargetBot target) throws IllegalStateException {
		return Utils.normalRelativeAngleDegrees(Math.toDegrees(aimRadians(target)));
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#fire()
	 */
	@Override
	public boolean fire() {
		// TODO Auto-generated method stub
		double bulletPower = getPower(this.lastTarget);
		boolean results = false;
		if (this.owningBot.getGunHeat() == 0 && this.owningBot.getGunTurnRemainingRadians() < (GUN_TURN_THRESHOLD)) {
			Bullet theBullet = this.owningBot.setFireBullet(bulletPower);
			if (null != theBullet) { // only log an entry if we succeeded to
				// fire
				BulletData newEntry = new BulletData(theBullet, this.lastTarget,
						BotTools.convertToPoint(this.owningBot));
				this.bullets.add(newEntry);
				results = true;
				this.lastFired = this.owningBot.getTime();
				if (!CircularGun.stats.containsKey(this.lastTarget.getGroupId())) {
					this.owningBot.out.println(
							this.getClass().getName() + " - new gun stats in fire for " + this.lastTarget.getName());
					CircularGun.stats.put(this.lastTarget.getGroupId(), new GunStats());
				}
				CircularGun.stats.get(this.lastTarget.getGroupId()).addShot();
			}
		}
		return results;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#update()
	 */
	@Override
	public void update() {
		Vector<RepositoryEntry<BulletData>> expired = new Vector<>();
		for (String targetGroupId : this.bullets.getAllGroupIds()) {
			for (BulletData bulletData : this.bullets.getAllData(targetGroupId)) {
				Bullet bullet = bulletData.getBullet();
				if (!bullet.isActive()) {
					expired.addElement(bulletData);
					if (!CircularGun.stats.containsKey(targetGroupId)) {
						this.owningBot.out
								.println(this.getClass().getName() + " - new gun stats in update for " + targetGroupId);
						CircularGun.stats.put(targetGroupId, new GunStats());
					}
					if (targetGroupId.equals(bullet.getVictim())) {
						// basically if I hit my intended target, add a hit
						CircularGun.stats.get(targetGroupId).addHit();
					}
					this.owningBot.out.println(this.getClass().getName() + " - time: " + this.owningBot.getTime()
							+ " target: " + targetGroupId + " Bullet: " + bullet.toString());
				}
			}
		}

		this.bullets.remove(expired);
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#update(robocode.BulletHitEvent)
	 */
	@Override
	public void update(BulletHitEvent event) {
		// TODO Auto-generated method stub
		// do nothing?
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#isValid(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public boolean isValid(TargetBot target) {
		boolean results = isActive();
		if (results) {
			Vector<TargetBot> targetData = this.targetRepository.getAllData(target);
			results = targetData.size() > 1;
			if (results) {
				results = (targetData.get(0).getTime() - targetData.get(1).getTime()) == 1;
			}
			if (results) {
				results = (targetData.get(1).getTime() - targetData.get(2).getTime()) == 1;
			}
			if (results) {
				double speedDelta1 = targetData.get(0).getVelocity() - targetData.get(1).getVelocity();
				double speedDelta2 = targetData.get(1).getVelocity() - targetData.get(2).getVelocity();
				results = speedDelta1 == speedDelta2;
			}
			if (results) {
				double turnDelta1 = targetData.get(0).getHeadingRadians() - targetData.get(1).getHeadingRadians();
				double turnDelta2 = targetData.get(1).getHeadingRadians() - targetData.get(2).getHeadingRadians();
				results = turnDelta1 == turnDelta2;
			}
		}

		if (!results) {
			this.aimPoint = null;
		}
		return results;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#isActive()
	 */
	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#getStats()
	 */
	@Override
	public GunStats getStats() {
		int hitTotal = 0;
		int shotTotal = 0;
		for (GunStats curStat : CircularGun.stats.values()) {
			hitTotal += curStat.getHits();
			shotTotal += curStat.getShots();
		}
		return new GunStats(shotTotal, hitTotal);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.ywp.robocode.utils.Gun#getStats(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public GunStats getStats(TargetBot target) {
		GunStats results = CircularGun.stats.get(target.getName());
		if (null == results) {
			results = new GunStats();
		}
		return results;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#onPaint(java.awt.Graphics2D)
	 */
	@Override
	public void onPaint(Graphics2D g) {

		if (null != this.aimPoint) {
			this.aimPoint.drawPoint(g, Color.blue);

			Point origin = BotTools.convertToPoint(this.owningBot);
			double absBearing = origin.angleRadians(this.aimPoint);// this is
			// proven
			// accurate
			Point tempPoint = BotTools.project(origin, this.ray, absBearing);
			g.setColor(Color.green);
			g.drawLine((int) origin.getX(), (int) origin.getY(), (int) tempPoint.getX(), (int) tempPoint.getY());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#printAllStats(java.io.PrintStream)
	 */
	@Override
	public void printAllStats(PrintStream out) {
		out.println(this.getClass().getName() + " - collected stats");
		for (Entry<String, GunStats> curEntry : CircularGun.stats.entrySet()) {
			out.print(curEntry.getKey() + " - ");
			out.println(curEntry.getValue().toString());
		}
		out.println("Total: " + getStats().toString());
	}

	private double getPower(TargetBot target) {
		return Math.min(2.4, Math.min(target.getEnergy() / 4, this.owningBot.getEnergy() / 10));
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#lastFired()
	 */
	@Override
	public long lastFired() {
		return this.lastFired;
	}

}
