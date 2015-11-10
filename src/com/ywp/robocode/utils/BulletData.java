package com.ywp.robocode.utils;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

import robocode.Bullet;

public class BulletData implements RepositoryEntry<BulletData> {

	private Bullet bullet;

	private TargetBot target;

	private Point2D.Double origin;

	/**
	 * Main constructor
	 *
	 * @param bullet - the bullet to be tracked
	 * @param target - the target for the bullet
	 * @param origin - the point where the bullet was originally fired from
	 */
	public BulletData(Bullet bullet, TargetBot target, Double origin) {
		this.bullet = bullet;
		this.target = target;
		this.origin = origin;
	}

	/**
	 * Shallow copy constructor by reference.
	 *
	 * @param original
	 */
	public BulletData(BulletData original) {
		this.bullet = original.bullet;
		this.target = original.target;
		this.origin = original.origin;
	}

	@Override
	public String getGroupId() {
		return this.target.getGroupId();
	}

	@Override
	public String getUniqueId() {
		return this.target.getGroupId() + this.bullet.hashCode();
	}

	@Override
	public BulletData getData() {
		return this;
	}

	/**
	 * @return the bullet
	 */
	public Bullet getBullet() {
		return this.bullet;
	}

	/**
	 * @return the target
	 */
	public TargetBot getTarget() {
		return this.target;
	}

	/**
	 * @return the origin
	 */
	public Point2D.Double getOrigin() {
		return this.origin;
	}

}
