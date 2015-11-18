package com.ywp.robocode.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import robocode.util.Utils;

public class Point extends Point2D.Double {

	/**
	 * something required by Serializable
	 */
	private static final long serialVersionUID = 3356010811330834867L;

	public Point(double x, double y) {
		super(x, y);
	}

	public double angleRadians(Point2D.Double to) {
		return Utils.normalAbsoluteAngle(Math.atan2(
				to.getX() - getX(), to.getY() - getY()));
	}

	public double angle(Point2D.Double to) {
		return Utils.normalRelativeAngleDegrees(Math.toDegrees(angleRadians(to)));
	}

	public void drawBot(Graphics2D g, Color c) {
		g.setColor(c);
		g.drawRect((int)this.x-16, (int)this.y-16, 36, 36);
	}

	public void drawPoint(Graphics2D g, Color c) {
		g.setColor(c);
		g.fillOval((int)this.x-2, (int)this.y-2, 4, 4);
	}
}
