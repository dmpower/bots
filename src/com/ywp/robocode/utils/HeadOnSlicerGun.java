/**
 *
 */
package com.ywp.robocode.utils;

import java.awt.Graphics2D;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import robocode.AdvancedRobot;
import robocode.Bullet;
import robocode.BulletHitEvent;
import robocode.Rules;

/**
 * This gun is similar to the HeadOnGun but it has a simple guess factor that it
 * uses to adjust the firing angle based on results from firing at a target.
 *
 * @author dpower
 */
public class HeadOnSlicerGun implements Gun {

	private final double				  NUM_LENGTH_SEGMENTS;
	private final double				  NUM_ARC_SEGMENTS;					  					  		   		   		   		   		    // keep
	// this
	// even
	private final double				  ARC_WIDTH_FACTOR;
	private final int					  FIRING_ADJUSTMENT_HISTORY;

	private AdvancedRobot				  owningBot;
	private double						  sliceLength;
	private double						  sliceSegmentLength;
	private double						  arcSegmentSize;
	private double						  halfArcAdjustment;

	private RepositoryManager<TargetBot>  targetRepository;
	private TargetBot					  lastTarget;
	private RepositoryManager<BulletData> bullets = new RepositoryManager<>();
	private static Map<String, GunStats>  stats	  = new HashMap<>();

	public HeadOnSlicerGun(AdvancedRobot owningBot) {
		this(owningBot, 4, 10, Rules.MAX_VELOCITY, 200);
	}

	public HeadOnSlicerGun(AdvancedRobot owningBot, int numLengthSegments, int numArcSegments, double arcWidthFactor,
			int firingAdjustmentHistory) {
		this.NUM_LENGTH_SEGMENTS = numLengthSegments;
		this.NUM_ARC_SEGMENTS = numArcSegments;
		this.ARC_WIDTH_FACTOR = arcWidthFactor;
		this.FIRING_ADJUSTMENT_HISTORY = firingAdjustmentHistory;

		this.owningBot = owningBot;

		// basically find out how far away two bots can get and still see each
		// other
		this.sliceLength = Math.pow(this.owningBot.getBattleFieldWidth() - this.owningBot.getWidth(), 2);
		this.sliceLength += Math.pow(this.owningBot.getBattleFieldHeight() - this.owningBot.getHeight(), 2);
		this.sliceLength = Math.sqrt(this.sliceLength);
		this.sliceLength = Math.min(Rules.RADAR_SCAN_RADIUS, this.sliceLength);

		this.sliceSegmentLength = this.sliceLength / this.NUM_LENGTH_SEGMENTS;

		this.arcSegmentSize = Math.atan(this.sliceLength / this.ARC_WIDTH_FACTOR);
		this.halfArcAdjustment = this.arcSegmentSize * this.NUM_ARC_SEGMENTS / 2;
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
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#aim(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public double aim(TargetBot target) throws IllegalStateException {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#fire()
	 */
	@Override
	public boolean fire() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#update()
	 */
	@Override
	public void update() {
		Vector<RepositoryEntry<BulletData>> expired = new Vector<>();
		for (String targetGroupId : this.bullets.getAllGroupIds()) {
			TargetBot target = this.targetRepository.getAllData(targetGroupId).get(0);
			for (BulletData bulletData : this.bullets.getAllData(targetGroupId)) {
				Bullet bullet = bulletData.getBullet();
				Point bulletLocation = new Point(bullet.getX(), bullet.getY());
				Point owningBotPoint = BotTools.convertToPoint(this.owningBot);
				if (!bullet.isActive()
						|| owningBotPoint.distance(bulletLocation) > owningBotPoint.distance(target.getPoint())) {
					expired.addElement(bulletData);
					if (!HeadOnSlicerGun.stats.containsKey(targetGroupId)) {
						this.owningBot.out
								.println(this.getClass().getName() + " - new gun stats in update for " + targetGroupId);
						HeadOnSlicerGun.stats.put(targetGroupId, new GunStats());
					}
					if (targetGroupId.equals(bullet.getVictim())) {
						// basically if I hit my intended target, add a hit
						HeadOnSlicerGun.stats.get(targetGroupId).addHit();
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
		// still do nothing? I may have to remove this if I don't find a need to
		// use it.
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#isValid(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public boolean isValid(TargetBot target) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#isActive()
	 */
	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#getStats()
	 */
	@Override
	public GunStats getStats() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * com.ywp.robocode.utils.Gun#getStats(com.ywp.robocode.utils.TargetBot)
	 */
	@Override
	public GunStats getStats(TargetBot target) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#onPaint(java.awt.Graphics2D)
	 */
	@Override
	public void onPaint(Graphics2D g) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#printAllStats(java.io.PrintStream)
	 */
	@Override
	public void printAllStats(PrintStream out) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.Gun#lastFired()
	 */
	@Override
	public long lastFired() {
		// TODO Auto-generated method stub
		return 0;
	}

}
