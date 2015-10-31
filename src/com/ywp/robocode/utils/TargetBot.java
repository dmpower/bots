package com.ywp.robocode.utils;

import robocode.HitRobotEvent;
import robocode.ScannedRobotEvent;

public class TargetBot {
	private final String name;
	private final double energy;
	private final double heading;
	private final double bearing;
	private final double distance;
	private final double velocity;
	private final double time;

	public TargetBot(ScannedRobotEvent event){
		name = event.getName();
		energy = event.getEnergy();
		heading = event.getHeadingRadians();
		bearing = event.getBearingRadians();
		distance = event.getDistance();
		velocity = event.getVelocity();
		time = event.getTime();
	}

	public TargetBot(HitRobotEvent event){
		// 36 came from Robot.WIDTH but it is a private final variable accessed by getWidth().
		this(event,36);
	}

	public TargetBot(HitRobotEvent event, int botWidth){
		name = event.getName();
		energy = event.getEnergy();
		heading = 0d;
		bearing = event.getBearingRadians();
		// This is not entirely accurate, but it helps
		distance = botWidth;
		velocity = 0d;
		time = event.getTime();
	}

	/**
	 * Returns the bearing to the robot you scanned, relative to your robot's
	 * heading, in degrees (-180 <= getBearing() < 180)
	 *
	 * @return the bearing to the robot you scanned, in degrees
	 */
	public double getBearing() {
		return bearing * 180.0 / Math.PI;
	}

	/**
	 * Returns the bearing to the robot you scanned, relative to your robot's
	 * heading, in radians (-PI <= getBearingRadians() < PI)
	 *
	 * @return the bearing to the robot you scanned, in radians
	 */
	public double getBearingRadians() {
		return bearing;
	}

	/**
	 * Returns the distance to the robot (your center to his center).
	 *
	 * @return the distance to the robot.
	 */
	public double getDistance() {
		return distance;
	}

	/**
	 * Returns the energy of the robot.
	 *
	 * @return the energy of the robot
	 */
	public double getEnergy() {
		return energy;
	}

	/**
	 * Returns the heading of the robot, in degrees (0 <= getHeading() < 360)
	 *
	 * @return the heading of the robot, in degrees
	 */
	public double getHeading() {
		return heading * 180.0 / Math.PI;
	}

	/**
	 * Returns the heading of the robot, in radians (0 <= getHeading() < 2 * PI)
	 *
	 * @return the heading of the robot, in radians
	 */
	public double getHeadingRadians() {
		return heading;
	}

	/**
	 * Returns the name of the robot.
	 *
	 * @return the name of the robot
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the velocity of the robot.
	 *
	 * @return the velocity of the robot
	 */
	public double getVelocity() {
		return velocity;
	}

	/**
	 * Returns the time the target was seen.
	 *
	 * @return the time the target was seen
	 */
	public double getTime() {
		return time;
	}


}
