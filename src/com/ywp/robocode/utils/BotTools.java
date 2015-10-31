package com.ywp.robocode.utils;

import java.awt.geom.Point2D;

import robocode.AdvancedRobot;
import robocode.Robot;

public class BotTools {

	static public Point2D.Double project(Point2D.Double origin,double dist,double angle) {
		return new Point2D.Double(origin.x+dist*Math.sin(angle),origin.y+dist*Math.cos(angle));
	}

	static public Point2D.Double convertToPoint (AdvancedRobot self, TargetBot target) {
		return project(convertToPoint(self), target.getDistance(), target.getBearingRadians()+self.getHeadingRadians());
	}

	static public Point2D.Double convertToPoint (Robot self) {
		return new Point2D.Double(self.getX(), self.getY());
	}


}
