package com.ywp.robocode.bots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Vector;

import com.ywp.robocode.utils.BotTools;
import com.ywp.robocode.utils.Gun;
import com.ywp.robocode.utils.HeadOnGun;
import com.ywp.robocode.utils.MovementWave;
import com.ywp.robocode.utils.RepositoryManager;
import com.ywp.robocode.utils.TargetBot;

import robocode.AdvancedRobot;
import robocode.HitRobotEvent;
import robocode.RobotDeathEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class Starter extends AdvancedRobot {

	/*
	 * change these statistics to see different graphics.
	 */
	final static boolean PAINT_MOVEMENT=true;
	final static boolean PAINT_GUN=false;

	boolean isSweeping;

	ArrayList<MovementWave> moveWaves=new ArrayList<MovementWave>();
	double enemyEnergy = 0d;
	TargetBot currentTarget = null;
	double lastTargetChange = 0d;
	static final double targetChangeThreshold = 20d;

	double sweepTime = 0d;
	static final double sweepInterval = 100d;
	RepositoryManager<TargetBot> targetManager;

	Vector<Gun> gunRack = new Vector<>();



	/* (non-Javadoc)
	 * @see robocode.Robot#run()
	 */
	@Override
	public void run() {
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setColors(Color.red,Color.yellow,Color.blue);

		this.targetManager = new RepositoryManager<TargetBot>();

		this.gunRack.add(new HeadOnGun(this));

		while(true){
			//			out.println("New turn:" + getTime());
			doRadar();
			doGun();
			doMove();
			execute();
		}
	}

	/* (non-Javadoc)
	 * @see robocode.Robot#onScannedRobot(robocode.ScannedRobotEvent)
	 */
	@Override
	public void onScannedRobot(ScannedRobotEvent event) {
		//		out.println("onScannedRobot turn: " + getTime());
		TargetBot target = new TargetBot(event);
		this.targetManager.add(target);
		pickTarget(target);
		if (this.currentTarget.getName().equals(target.getName())) {
			double energyChange=(this.enemyEnergy-target.getEnergy());
			if(energyChange<=3&&energyChange>=0.1){
				logMovementWave(target,energyChange);
			}

			updateTarget(target);
		}
	}

	/* (non-Javadoc)
	 * @see robocode.Robot#onPaint(java.awt.Graphics2D)
	 */
	@Override
	public void onPaint(Graphics2D g) {
		// TODO Auto-generated method stub
		double radius;

		/*
		 * Paints the waves and the imaginary bullets from the movement.
		 */
		if(PAINT_MOVEMENT){
			for(int i=0;i<this.moveWaves.size();i++){
				MovementWave w=this.moveWaves.get(i);
				g.setColor(Color.blue);
				radius=(getTime()-w.startTime)*w.speed+w.speed;
				Point2D.Double hotBullet=BotTools.project(w.origin,radius,w.angle);
				Point2D.Double latBullet=BotTools.project(w.origin,radius,w.angle+w.latVel);
				g.setColor(Color.red);
				double sArc = Math.toDegrees(w.angle);
				double eArc = Math.toDegrees(w.latVel);
				g.drawArc((int)(w.origin.x-radius),(int)(w.origin.y-radius),(int)radius*2,(int)radius*2, (int)sArc, (int)eArc);
				radius = 2d;
				g.fillOval((int)(hotBullet.x-radius),(int)hotBullet.y-3,(int)(radius*2),(int)(radius*2));
				g.fillOval((int)(latBullet.x-radius),(int)latBullet.y-3,(int)(radius*2),(int)(radius*2));
			}
		}
		/*
		 * Just paints the waves for the targeting.
		 */
		//		if(PAINT_GUN){
		//			for(int i=0;i<gunWaves.size();i++){
		//				GunWave w=gunWaves.get(i);
		//				g.setColor(Color.blue);
		//				radius=(getTime()-w.startTime)*w.speed;
		//				g.drawOval((int)(w.origin.x-radius),(int)(w.origin.y-radius),(int)radius*2,(int)radius*2);
		//			}
		//		}

		if (hasTarget()){
			Point2D.Double selfPoint = BotTools.convertToPoint(this);
			double absBearing=this.currentTarget.getBearingRadians()+getHeadingRadians();
			Point2D.Double targetPoint = BotTools.project(selfPoint, this.currentTarget.getDistance()+getBattleFieldWidth()*2, absBearing);
			g.setColor(Color.yellow);
			g.drawLine((int)targetPoint.getX(), (int)targetPoint.getY(), (int)getX(), (int)getY());
		}
	}

	/* (non-Javadoc)
	 * @see robocode.Robot#onRobotDeath(robocode.RobotDeathEvent)
	 */
	@Override
	public void onRobotDeath(RobotDeathEvent event) {
		if (this.currentTarget.getName().equals(event.getName())){
			clearTarget();
		}
		this.targetManager.removeAll(new TargetBot(event));
	}

	/*
	 * This method receives a ScannedRobotEvent and uses that information to create a new wave and place it in
	 * our log. Basically we're going to take all the information we'll need to know later to figure out where
	 * to move to and store it in one object so we can use it easily later.
	 */
	public void logMovementWave(TargetBot e,double energyChange){
		double absBearing=e.getBearingRadians()+getHeadingRadians();
		MovementWave w=new MovementWave();
		//This is the spot that the enemy was in when they fired.
		w.origin=BotTools.project(new Point2D.Double(getX(),getY()),e.getDistance(),absBearing);
		//20-3*bulletPower is the formula to find a bullet's speed.
		w.speed=20-3*energyChange;
		//The time at which the bullet was fired.
		w.startTime=getTime();
		//The absolute bearing from the enemy to us can be found by adding Pi to our absolute bearing.
		w.angle=Utils.normalRelativeAngle(absBearing+Math.PI);
		/*
		 * Our lateral velocity, used to calculate where a bullet fired with linear targeting would be.
		 * Note that the speed has already been factored into the calculation.
		 */
		w.latVel=(getVelocity()*Math.sin(getHeadingRadians()-w.angle))/w.speed;
		//This actually adds the wave to the list.
		this.moveWaves.add(w);
	}
	/*
	 * This method looks at all the directions we could go, then rates them based on how close they will take us
	 * to simulated bullets fired with both linear and head-on targeting generated by the waves we have logged.
	 * It is the core of our movement.
	 */
	private void doMove(){
		if(hasTarget()){
			Point2D.Double enemyLocation = BotTools.convertToPoint(this, this.currentTarget);
			MovementWave w;
			//This for loop rates each angle individually
			double bestRating=Double.POSITIVE_INFINITY;
			for(double moveAngle=0;moveAngle<Math.PI*2;moveAngle+=Math.PI/16D){
				double rating=0;

				//Movepoint is position we would be at if we were to move one robot-length in the given direction.
				Point2D.Double movePoint=BotTools.project(new Point2D.Double(getX(),getY()),36,moveAngle);

				/*
				 * This loop will iterate through each wave and add a risk for the simulated bullets on each one
				 * to the total risk for this angle.
				 */
				for(int i=0;i<this.moveWaves.size();i++){
					w=this.moveWaves.get(i);

					//This part will remove waves that have passed our robot, so we no longer keep taking into account old ones
					if(new Point2D.Double(getX(),getY()).distance(w.origin)<(getTime()-w.startTime)*w.speed+w.speed){
						this.moveWaves.remove(w);
					}
					else{
						/*
						 * This adds two risks for each wave: one based on the distance from where a head-on targeting
						 * bullet would be, and one for where a linear targeting bullet would be.
						 */
						rating+=1D/Math.pow(movePoint.distance(BotTools.project(w.origin,movePoint.distance(w.origin),w.angle)),2);
						rating+=1D/Math.pow(movePoint.distance(BotTools.project(w.origin,movePoint.distance(w.origin),w.angle+w.latVel)),2);
					}
				}
				//This adds a risk associated with being too close to the other robot if there are no waves.
				if(this.moveWaves.size()==0){
					rating=1D/Math.pow(movePoint.distance(enemyLocation),2);
				}
				//This part tells us to go in the direction if it is better than the previous best option and is reachable.
				if(rating<bestRating && new Rectangle2D.Double(50,50,getBattleFieldWidth()-100,getBattleFieldHeight()-100).contains(movePoint)){
					bestRating=rating;
					/*
					 * These next three lines are a very codesize-efficient way to
					 * choose the best direction for moving to a point.
					 */
					int pointDir;
					setAhead(1000*(pointDir=(Math.abs(moveAngle-getHeadingRadians())<Math.PI/2?1:-1)));
					setTurnRightRadians(Utils.normalRelativeAngle(moveAngle+(pointDir==-1?Math.PI:0)-getHeadingRadians()));
				}
			}// end for
		}
	}

	/* (non-Javadoc)
	 * @see robocode.Robot#onHitRobot(robocode.HitRobotEvent)
	 */
	@Override
	public void onHitRobot(HitRobotEvent event) {
		// target who hit us
		setTarget(new TargetBot(event));
	}

	/**
	 * This does the logic to pick the bot to target.
	 * @param target - newly discovered target
	 */
	private void pickTarget(TargetBot target) {
		if (!hasTarget()) {
			setTarget(target);
		}

		if ((this.lastTargetChange + targetChangeThreshold) < getTime()) {
			//			out.println("Time to change target.");
			if (! this.currentTarget.getName().equals(target.getName())) {
				//				out.println("  New Target Found");
				if (target.getDistance() < this.currentTarget.getDistance()) {
					//					out.println("    New Target is closer");
					setTarget(target);
				}

				if (target.getEnergy() == 0d){
					// take a pot shot
					setTarget(target);
				}
			}
		}
	}

	/**
	 * This is used when a new target is selected
	 *
	 * @param target - the new target
	 */
	private void setTarget (TargetBot target) {
		updateTarget(target);
		this.lastTargetChange = getTime();
	}

	/**
	 * This is use to refresh the target's information. It is assumed this is not
	 * changing the target. For changing the target see setTarget()
	 *
	 * @param target - updated information for target
	 */
	private void updateTarget (TargetBot target) {
		this.currentTarget = target;
		this.enemyEnergy = target.getEnergy();
	}

	/**
	 * This is used when the current target is not longer valid.
	 */
	private void clearTarget(){
		this.currentTarget = null;
		this.enemyEnergy = 0d;
	}

	private boolean hasTarget(){
		return this.currentTarget != null;
	}

	private void doRadar(){
		double currentTime = getTime();
		//		out.println("Rader turn: " + currentTime + " remaining turn: " + getRadarTurnRemainingRadians());
		if (getOthers() > 1 && (this.sweepTime == 0 || this.sweepTime+sweepInterval < currentTime)){
			//			out.println("Radar sweeping");
			this.isSweeping = true;
			this.sweepTime = currentTime+1;
			setTurnRadarRightRadians(Math.PI*2); // full circle
		}

		if(!this.isSweeping && hasTarget() && this.currentTarget.getTime() == currentTime){
			//			out.println("Radar scanned bot, turn back");
			double absBearing=this.currentTarget.getBearingRadians()+getHeadingRadians();
			setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing-getRadarHeadingRadians())-(Rules.RADAR_TURN_RATE_RADIANS/2));
		}

		if(Math.abs(getRadarTurnRemainingRadians())< (Rules.RADAR_TURN_RATE_RADIANS/4)){
			//			out.println("Radar completed turn");
			// basically if there is only a little bit of turn remaining, we want to turn
			// the radar back anyways. Otherwise the radar stops mid turn and we lose most
			// of the radar's time sitting still.

			// note: we use POSITIVE_INFINITY instead of Rules.RADAR_TURN_RATE_RADIANS/2
			// because if there is a slip we will keep turning until we find a robot.
			setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
			this.isSweeping = false;
		}

	}

	private void doGun(){
		/*
		 * Aiming our gun and firing
		 */
		if (hasTarget()){
			this.gunRack.get(0).aimRadians(this.currentTarget);
			this.gunRack.get(0).fire();
		}
		for (Gun gun : this.gunRack) {
			gun.update();
			this.out.print(gun.getClass().toString() + ": ");
			this.out.println(gun.getStats().toString());
		}
	}

}
