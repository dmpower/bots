package com.ywp.robocode.utils;

import robocode.AdvancedRobot;
import robocode.Robot;

public class BotTools {

	static public Point project(Point origin,double dist,double angle) {
		return new Point(origin.x + dist * Math.sin(angle), origin.y + dist * Math.cos(angle));
	}

	static public Point convertToPoint (AdvancedRobot self, TargetBot target) {
		return project(convertToPoint(self), target.getDistance(), target.getBearingRadians()+self.getHeadingRadians());
	}

	static public Point convertToPoint (Robot self) {
		return new Point(self.getX(), self.getY());
	}


}
