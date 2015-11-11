package com.ywp.robocode.utils;

import robocode.Bullet;

public class BulletData implements RepositoryEntry<BulletData> {

	private Bullet bullet;

	private TargetBot target;

	private Point origin;

	/**
	 * Main constructor
	 *
	 * @param bullet - the bullet to be tracked
	 * @param target - the target for the bullet
	 * @param origin - the point where the bullet was originally fired from
	 */
	public BulletData(Bullet bullet, TargetBot target, Point origin) {
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
	public Point getOrigin() {
		return this.origin;
	}

}
