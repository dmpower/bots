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
	private final double velocity;
	private final long	 time;
	private final Point	 point;
	private final Point	 origin;

	public TargetBot(AdvancedRobot source, ScannedRobotEvent event) {
		this.name = event.getName();
		this.energy = event.getEnergy();
		this.heading = event.getHeadingRadians();
		this.bearing = event.getBearingRadians();
		this.velocity = event.getVelocity();
		this.time = event.getTime();
		this.point = BotTools.project(this.origin, event.getDistance(),
				(event.getBearingRadians() + source.getHeadingRadians()));
		this.origin = BotTools.convertToPoint(source);
	}

	public TargetBot(AdvancedRobot source, HitRobotEvent event, double botWidth) {
		this.name = event.getName();
		this.energy = event.getEnergy();
		this.heading = 0d;
		this.bearing = event.getBearingRadians();
		this.velocity = 0d;
		this.time = event.getTime();
		// This is not entirely accurate, but it helps
		this.point = BotTools.project(this.origin, source.getWidth(),
				(event.getBearingRadians() + source.getHeadingRadians()));
		this.origin = BotTools.convertToPoint(source);
	}

	public TargetBot(AdvancedRobot source, RobotDeathEvent event) {
		this.name = event.getName();
		this.energy = 0d;
		this.heading = 0d;
		this.bearing = 0d;
		this.velocity = 0d;
		this.time = event.getTime();
		this.point = new Point(0, 0);
		this.origin = BotTools.convertToPoint(source);
	}

	/**
	 * Returns the bearing to the robot you scanned, relative to your robot's
	 * heading, in degrees (-180 <= getBearing() < 180)
	 * @return the bearing to the robot you scanned, in degrees
	 */
	public double getBearing() {
		return Utils.normalRelativeAngleDegrees(Math.toDegrees(this.bearing));
	}

	/**
	 * Returns the bearing to the robot you scanned, relative to your robot's
	 * heading, in radians (-PI <= getBearingRadians() < PI)
	 * @return the bearing to the robot you scanned, in radians
	 */
	public double getBearingRadians() {
		return this.bearing;
	}

	/**
	 * Returns the absolute bearing to the robot you scanned, relative to your
	 * robot's heading, in degrees (0 <= getBearing() < 360)
	 * @return the bearing to the robot you scanned, in degrees
	 */
	public double getAbsBearing() {
		return Utils.normalAbsoluteAngleDegrees(Math.toDegrees(getAbsBearingRadians()));
	}

	/**
	 * Returns the bearing to the robot you scanned, relative to your robot's
	 * heading, in radians (0 <= getBearingRadians() < 2*PI)
	 * @return the bearing to the robot you scanned, in radians
	 */
	public double getAbsBearingRadians() {
		return this.origin.angleRadians(this.point);
	}

	/**
	 * Returns the distance from source to target. Note: this number is
	 * recalculated so it will not equal the distance from the event.
	 * @return the distance from source to target.
	 */
	public double getDistance() {
		return this.origin.distance(this.point);
	}

	/**
	 * Returns the distance from the provided source to the target.
	 * @param source the point to calculate the distance from
	 * @return the distance from the provided source to the target.
	 */
	public double getDistance(Point source) {
		return source.distance(this.point);
	}

	/**
	 * Returns the energy of the robot.
	 * @return the energy of the robot
	 */
	public double getEnergy() {
		return this.energy;
	}

	/**
	 * Returns the heading of the robot, in degrees (0 <= getHeading() < 360)
	 * @return the heading of the robot, in degrees
	 */
	public double getHeading() {
		return this.heading * 180.0 / Math.PI;
	}

	/**
	 * Returns the heading of the robot, in radians (0 <= getHeading() < 2 * PI)
	 * @return the heading of the robot, in radians
	 */
	public double getHeadingRadians() {
		return this.heading;
	}

	/**
	 * Returns the name of the robot.
	 * @return the name of the robot
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the velocity of the robot.
	 * @return the velocity of the robot
	 */
	public double getVelocity() {
		return this.velocity;
	}

	/**
	 * Returns the time the target was seen.
	 * @return the time the target was seen
	 */
	public long getTime() {
		return this.time;
	}

	/**
	 * Returns the origin when TargetBot was created
	 * @return the origin when TargetBot was created.
	 */
	public Point getOrigin() {
		return this.origin;
	}

	public Point getPoint() {
		return this.point;
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
