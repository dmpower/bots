/**
 *
 */
package com.ywp.robocode.utils;

import java.awt.Graphics2D;
import java.io.PrintStream;

import robocode.BulletHitEvent;
import robocode.Rules;

/**
 * This is a general interface for Gun implementations
 * @author dpower
 *
 */
public interface Gun {

	static final double GUN_TURN_THRESHOLD = Rules.GUN_TURN_RATE_RADIANS/4;

	/**
	 * This provides target data to the gun but does not ask it to aim.
	 *
	 * @param target - data to feed to gun for use later
	 */
	void feedTarget (TargetBot target);

	/**
	 * This asks the gun to aim at a specific target.
	 *
	 * @param target - the target to aim at
	 * @return Bearing in Radians to aim the gun at.
	 * @throws IllegalStateException - remember to call isValid first
	 * @see #isValid(TargetBot)
	 */
	double aimRadians(TargetBot target) throws IllegalStateException;

	/**
	 * This asks the gun to aim at a specific target.
	 *
	 * @param target - the target to aim at
	 * @return Bearing in Degrees to aim the gun at.
	 * @throws IllegalStateException - remember to call isValid first
	 * @see #isValid(TargetBot)
	 */
	double aim(TargetBot target) throws IllegalStateException;

	/**
	 * Fire this gun if it can.
	 *
	 * @return returns true if the gun fired otherwise returns false.
	 */
	boolean fire();

	/**
	 * Do any gun maintenance that is needed. For instance, go through the shots fired by this gun
	 * and expire the ones that are not needed. Update states if needed.
	 */
	void update();

	/**
	 * A bullet hit a bot. This provides the event information to the gun so it can update
	 * its stats if it was the owner of the shot. Note, code should expect events that do
	 * not belong to them.
	 *
	 * @param event - The BulletHitEvent
	 */
	void update(BulletHitEvent event);

	/**
	 * Let the gun decide if it is even valid to use against the target.
	 *
	 * @param target - evaluate the guns validity against this target
	 * @return true if the gun is valid against the target
	 */
	boolean isValid(TargetBot target);

	/**
	 * This represents a way for the gun to turn itself on or off
	 * @return
	 */
	boolean isActive();

	/**
	 * This information is provided so that other code can decide which gun to use
	 *
	 * @return returns the general shot and hits for this gun
	 */
	GunStats getStats();

	/**
	 * This information is provided so that other code can decide which gun to use against the target
	 *
	 * @param target
	 * @return returns the shot and hits against the target
	 */
	GunStats getStats(TargetBot target);

	/**
	 * Give this gun a chance to do some painting.
	 *
	 * @param g - graphics object to paint with.
	 */
	void onPaint(Graphics2D g);

	/**
	 * This prints all the stats for this gun to the PrintStream
	 * @param out - Where to print
	 */
	void printAllStats(PrintStream out);

	/**
	 * Returns the time/turn this gun last fired.
	 * @return time this gun last fired
	 */
	long lastFired();
}
