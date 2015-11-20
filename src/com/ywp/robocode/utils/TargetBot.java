package com.ywp.robocode.utils;

import robocode.AdvancedRobot;
import robocode.HitRobotEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class TargetBot implements RepositoryEntry<TargetBot> {
	private final String name;
	private final double energy;
	private final double heading;
	private final double bearing;
	private final double absBearing;
	private final double distance;
	private final double velocity;
	private final long	 time;
	private final Point	 origin;

	public TargetBot(AdvancedRobot source, ScannedRobotEvent event) {
		this.name = event.getName();
		this.energy = event.getEnergy();
		this.heading = event.getHeadingRadians();
		this.bearing = event.getBearingRadians();
		this.absBearing = Utils.normalAbsoluteAngle(event.getBearingRadians() + source.getHeadingRadians());
		this.distance = event.getDistance();
		this.velocity = event.getVelocity();
		this.time = event.getTime();
		this.origin = BotTools.convertToPoint(source);
	}

	public TargetBot(AdvancedRobot source, HitRobotEvent event, double botWidth) {
		this.name = event.getName();
		this.energy = event.getEnergy();
		this.heading = 0d;
		this.bearing = event.getBearingRadians();
		this.absBearing = Utils.normalAbsoluteAngle(event.getBearingRadians() + source.getHeadingRadians());
		// This is not entirely accurate, but it helps
		this.distance = botWidth;
		this.velocity = 0d;
		this.time = event.getTime();
		this.origin = BotTools.convertToPoint(source);
	}

	public TargetBot(AdvancedRobot source, RobotDeathEvent event) {
		this.name = event.getName();
		this.energy = 0d;
		this.heading = 0d;
		this.bearing = 0d;
		this.absBearing = 0d;
		this.distance = 0d;
		this.velocity = 0d;
		this.time = event.getTime();
		this.origin = BotTools.convertToPoint(source);
	}

	/**
	 * Returns the bearing to the robot you scanned, relative to your robot's
	 * heading, in degrees (-180 <= getBearing() < 180)
	 *
	 * @return the bearing to the robot you scanned, in degrees
	 */
	public double getBearing() {
		return Utils.normalRelativeAngleDegrees(Math.toDegrees(this.bearing));
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
	 * Returns the absolute bearing to the robot you scanned, relative to your
	 * robot's heading, in degrees (0 <= getBearing() < 360)
	 *
	 * @return the bearing to the robot you scanned, in degrees
	 */
	public double getAbsBearing() {
		return Utils.normalAbsoluteAngleDegrees(Math.toDegrees(this.absBearing));
	}

	/**
	 * Returns the bearing to the robot you scanned, relative to your robot's
	 * heading, in radians (0 <= getBearingRadians() < 2*PI)
	 *
	 * @return the bearing to the robot you scanned, in radians
	 */
	public double getAbsBearingRadians() {
		return this.absBearing;
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

	/**
	 * Returns the origin when TargetBot was created
	 *
	 * @return the origin when TargetBot was created.
	 */
	public Point getOrigin() {
		return this.origin;
	}

	public Point getPoint() {
		return BotTools.project(this.origin, this.distance, this.absBearing);
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.RepositoryEntry#getGroupId()
	 */
	@Override
	public String getGroupId() {
		return this.name;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.RepositoryEntry#getUniqueId()
	 */
	@Override
	public String getUniqueId() {
		return this.name + this.time;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ywp.robocode.utils.RepositoryEntry#getData()
	 */
	@Override
	public TargetBot getData() {
		return this;
	}

}
