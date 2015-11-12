package com.ywp.robocode.utils;

import robocode.HitRobotEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;

public class TargetBot implements RepositoryEntry<TargetBot>{
	private final String name;
	private final double energy;
	private final double heading;
	private final double bearing;
	private final double distance;
	private final double velocity;
	private final long time;

	public TargetBot(ScannedRobotEvent event){
		this.name = event.getName();
		this.energy = event.getEnergy();
		this.heading = event.getHeadingRadians();
		this.bearing = event.getBearingRadians();
		this.distance = event.getDistance();
		this.velocity = event.getVelocity();
		this.time = event.getTime();
	}

	public TargetBot(HitRobotEvent event, double botWidth){
		this.name = event.getName();
		this.energy = event.getEnergy();
		this.heading = 0d;
		this.bearing = event.getBearingRadians();
		// This is not entirely accurate, but it helps
		this.distance = botWidth;
		this.velocity = 0d;
		this.time = event.getTime();
	}

	public TargetBot(RobotDeathEvent event){
		this.name = event.getName();
		this.energy = 0d;
		this.heading = 0d;
		this.bearing = 0d;
		this.distance = 0d;
		this.velocity = 0d;
		this.time = event.getTime();

	}

	/**
	 * Returns the bearing to the robot you scanned, relative to your robot's
	 * heading, in degrees (-180 <= getBearing() < 180)
	 *
	 * @return the bearing to the robot you scanned, in degrees
	 */
	public double getBearing() {
		return this.bearing * 180.0 / Math.PI;
	}

	/**
	 * Returns the bearing to the robot you scanned, relative to your robot's
	 * heading, in radians (-PI <= getBearingRadians() < PI)
	 *
	 * @return the bearing to the robot you scanned, in radians
	 */
	public double getBearingRadians() {
		return this.bearing;
	}

	/**
	 * Returns the distance to the robot (your center to his center).
	 *
	 * @return the distance to the robot.
	 */
	public double getDistance() {
		return this.distance;
	}

	/**
	 * Returns the energy of the robot.
	 *
	 * @return the energy of the robot
	 */
	public double getEnergy() {
		return this.energy;
	}

	/**
	 * Returns the heading of the robot, in degrees (0 <= getHeading() < 360)
	 *
	 * @return the heading of the robot, in degrees
	 */
	public double getHeading() {
		return this.heading * 180.0 / Math.PI;
	}

	/**
	 * Returns the heading of the robot, in radians (0 <= getHeading() < 2 * PI)
	 *
	 * @return the heading of the robot, in radians
	 */
	public double getHeadingRadians() {
		return this.heading;
	}

	/**
	 * Returns the name of the robot.
	 *
	 * @return the name of the robot
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the velocity of the robot.
	 *
	 * @return the velocity of the robot
	 */
	public double getVelocity() {
		return this.velocity;
	}

	/**
	 * Returns the time the target was seen.
	 *
	 * @return the time the target was seen
	 */
	public long getTime() {
		return this.time;
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.RepositoryEntry#getGroupId()
	 */
	@Override
	public String getGroupId() {
		return this.name;
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.RepositoryEntry#getUniqueId()
	 */
	@Override
	public String getUniqueId() {
		return this.name + this.time;
	}

	/* (non-Javadoc)
	 * @see com.ywp.robocode.utils.RepositoryEntry#getData()
	 */
	@Override
	public TargetBot getData() {
		return this;
	}



}
