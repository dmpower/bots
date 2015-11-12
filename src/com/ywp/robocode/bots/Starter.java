package com.ywp.robocode.bots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Vector;

import com.ywp.robocode.utils.BotTools;
import com.ywp.robocode.utils.CircularGun;
import com.ywp.robocode.utils.Gun;
import com.ywp.robocode.utils.GunStats;
import com.ywp.robocode.utils.MovementWave;
import com.ywp.robocode.utils.Point;
import com.ywp.robocode.utils.RepositoryManager;
import com.ywp.robocode.utils.TargetBot;

import robocode.AdvancedRobot;
import robocode.DeathEvent;
import robocode.HitRobotEvent;
import robocode.RobotDeathEvent;
import robocode.Rules;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;
import robocode.util.Utils;

public class Starter extends AdvancedRobot {

	/*
	 * change these statistics to see different graphics.
	 */
	final static boolean PAINT_MOVEMENT=true;
	final static boolean PAINT_GUN=false;

	ArrayList<MovementWave> moveWaves=new ArrayList<MovementWave>();

	private TargetBot currentTargetData = null;
	double lastTargetChange = 0d;
	// Basically the time it take to turn the gun a full circle.
	static final double targetChangeThreshold = 2*Math.PI/Rules.GUN_TURN_RATE_RADIANS;

	boolean isSweeping;
	double sweepTime = 0d;
	static final double SWEEP_INTERVAL = 100d;
	static final double RADAR_HALF_SWEEP = Rules.RADAR_TURN_RATE_RADIANS/2;
	static final double RADAR_TOLLARENCE = RADAR_HALF_SWEEP/16;
	private static double RAY;
	RepositoryManager<TargetBot> targetManager;

	Vector<Gun> gunRack = new Vector<>();
	private boolean endTurn = true;
	private long time;
	private static Rectangle2D.Double battleField = null;



	/* (non-Javadoc)
	 * @see robocode.Robot#run()
	 */
	@Override
	public void run() {
		initialize();

		while(true){
			//			out.println("New turn:" + getTime());
			doRadar();
			doGun();
			doMove();
			this.endTurn = true;
			execute();
		}
	}

	private void initialize() {
		RAY = this.getBattleFieldWidth() + this.getBattleFieldHeight();
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setColors(Color.red,Color.yellow,Color.blue);

		this.targetManager = new RepositoryManager<TargetBot>();

		//this.gunRack.add(new HeadOnGun(this));
		this.gunRack.add(new CircularGun(this.targetManager, this));
	}

	/* (non-Javadoc)
	 * @see robocode.Robot#onScannedRobot(robocode.ScannedRobotEvent)
	 */
	@Override
	public void onScannedRobot(ScannedRobotEvent event) {
		this.out.println("onScannedRobot turn: " + getTime());
		TargetBot target = new TargetBot(event);
		this.targetManager.add(target);
		pickTarget(target);
		Vector<TargetBot> targetData = this.targetManager.getAllData(currentTarget());
		if (targetData.size() > 1 && targetData.get(0).getTime() == targetData.get(1).getTime()+1) {
			double energyChange = targetData.get(1).getEnergy()-targetData.get(0).getEnergy();
			if(energyChange<=3&&energyChange>=0.1){
				logMovementWave(target,energyChange);
			}
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
				Point hotBullet=BotTools.project(w.origin,radius,w.angle);
				Point latBullet=BotTools.project(w.origin,radius,w.angle+w.latVel);
				g.setColor(Color.red);
				double sArc = Math.toDegrees(w.angle);
				double eArc = Math.toDegrees(w.latVel);
				g.drawArc((int)(w.origin.x-radius),(int)(w.origin.y-radius),(int)radius*2,(int)radius*2, (int)sArc, (int)eArc);
				radius = 2d;
				g.fillOval((int)(hotBullet.x-radius),(int)hotBullet.y-3,(int)(radius*2),(int)(radius*2));
				g.fillOval((int)(latBullet.x-radius),(int)latBullet.y-3,(int)(radius*2),(int)(radius*2));
			}
		}

		if(PAINT_GUN){
			for (Gun gun : this.gunRack) {
				gun.onPaint(g);
			}
		}

		if (hasTarget()){
			Point selfPoint = BotTools.convertToPoint(this);
			double absBearing=currentTarget().getBearingRadians()+getHeadingRadians();
			Point targetPoint = BotTools.project(selfPoint, currentTarget().getDistance()+getBattleFieldWidth()*2, absBearing);
			g.setColor(Color.yellow);
			g.drawLine((int)targetPoint.getX(), (int)targetPoint.getY(), (int)getX(), (int)getY());
		}

		Point origin = BotTools.convertToPoint(this);

		// Gun direction
		Point tempPoint = BotTools.project(origin, RAY, this.getGunHeadingRadians());
		g.setColor(Color.blue);
		g.drawLine((int)origin.getX(), (int)origin.getY(), (int)tempPoint.getX(), (int)tempPoint.getY());

		// bot direction
		tempPoint = BotTools.project(origin, RAY, this.getHeadingRadians());
		g.setColor(Color.white);
		g.drawLine((int)origin.getX(), (int)origin.getY(), (int)tempPoint.getX(), (int)tempPoint.getY());


	}

	/* (non-Javadoc)
	 * @see robocode.Robot#onRobotDeath(robocode.RobotDeathEvent)
	 */
	@Override
	public void onRobotDeath(RobotDeathEvent event) {
		if (currentTarget().getName().equals(event.getName())){
			clearTarget();
		}
		this.targetManager.removeGroup(new TargetBot(event));
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
		w.origin=BotTools.project(new Point(getX(),getY()),e.getDistance(),absBearing);
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
			Point enemyLocation = BotTools.convertToPoint(this, currentTarget());
			MovementWave w;
			//This for loop rates each angle individually
			double bestRating=Double.POSITIVE_INFINITY;
			for(double moveAngle=0;moveAngle<Math.PI*2;moveAngle+=Math.PI/16D){
				double rating=0;

				//Movepoint is position we would be at if we were to move one robot-length in the given direction.
				Point movePoint=BotTools.project(new Point(getX(),getY()),36,moveAngle);

				/*
				 * This loop will iterate through each wave and add a risk for the simulated bullets on each one
				 * to the total risk for this angle.
				 */
				for(int i=0;i<this.moveWaves.size();i++){
					w=this.moveWaves.get(i);

					//This part will remove waves that have passed our robot, so we no longer keep taking into account old ones
					if(new Point(getX(),getY()).distance(w.origin)<(getTime()-w.startTime)*w.speed+w.speed){
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
		setTarget(new TargetBot(event, getWidth()));
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
			this.out.println("Time to change target.");
			if (! currentTarget().getName().equals(target.getName())) {
				this.out.println("  New Target Found");
				if (target.getDistance() < currentTarget().getDistance()) {
					this.out.println("    New Target is closer");
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
		this.out.println("Turn: " + getTime() + " selected " + target.getName() + " for new target.");
		this.lastTargetChange = getTime();
		this.currentTargetData = target;
	}

	/**
	 * This is used when the current target is not longer valid.
	 */
	private void clearTarget(){
		this.currentTargetData = null;
	}

	private boolean hasTarget(){
		return this.currentTargetData != null;
	}

	private void doRadar(){
		long currentTime = getTime();
		this.out.println("Rader turn: " + currentTime + " remaining turn: " + getRadarTurnRemainingRadians());
		if (getOthers() > 1 && (this.sweepTime == 0 || this.sweepTime+SWEEP_INTERVAL < currentTime)){
			this.out.println("Radar sweeping");
			this.isSweeping = true;
			this.sweepTime = currentTime+1;
			setTurnRadarRightRadians(Math.PI*2); // full circle
		}

		if(!this.isSweeping && hasTarget() && currentTarget().getTime() == currentTime){
			//absBearing
			double angle=currentTarget().getBearingRadians()+getHeadingRadians();
			//true on adjustment
			angle = Utils.normalRelativeAngle(angle-getRadarHeadingRadians());
			//additional adjustment to maximize sweep
			angle = angle + ((angle>0?1:-1) * RADAR_HALF_SWEEP);
			setTurnRadarRightRadians(Utils.normalRelativeAngle(angle));
		}

		this.out.println("Rader remaining turn: " + getRadarTurnRemainingRadians());

		if(Math.abs(getRadarTurnRemainingRadians())< RADAR_TOLLARENCE){
			this.out.println("Radar completed turn");
			// basically if there is only a little bit of turn remaining, we want to turn
			// the radar back anyways. Otherwise the radar stops mid turn and we lose most
			// of the radar's time sitting still.

			// note: we use POSITIVE_INFINITY instead of RADAR_HALF_SWEEP because
			// if there is a slip we will keep turning until we find another robot.
			setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
			this.isSweeping = false;
		}

	}

	private void doGun(){
		/*
		 * Aiming our gun and firing
		 */
		if (hasTarget()){
			try {
				if(this.gunRack.get(0).isValid(currentTarget())) {
					this.gunRack.get(0).aimRadians(currentTarget());
					this.gunRack.get(0).fire();
				}
			} catch (Exception e) {
				// log and eat it
				this.out.println(e.getMessage());
			}
		}
		for (Gun gun : this.gunRack) {
			gun.update();
			GunStats stats = gun.getStats();
			this.out.print(gun.getClass().toString() + ": ");
			this.out.println(stats.toString());
		}
	}

	/* (non-Javadoc)
	 * @see robocode.AdvancedRobot#onDeath(robocode.DeathEvent)
	 */
	@Override
	public void onDeath(DeathEvent event) {
		for (Gun gun : this.gunRack) {
			gun.printAllStats(this.out);
		}
	}

	/* (non-Javadoc)
	 * @see robocode.Robot#onWin(robocode.WinEvent)
	 */
	@Override
	public void onWin(WinEvent event) {
		for (Gun gun : this.gunRack) {
			gun.printAllStats(this.out);
		}
	}

	/* (non-Javadoc)
	 * @see robocode.Robot#getTime()
	 */
	@Override
	public long getTime() {
		// This is an important override. GetTime is the most used call and we have a set number of calls
		// per turn. This will close to halve the number of calls.
		if(this.endTurn) {
			this.time = super.getTime();
			this.endTurn = false;
		}
		return this.time;
	}

	private TargetBot currentTarget() {
		if(this.currentTargetData.getTime() != getTime()){
			Vector<TargetBot> targetData = this.targetManager.getAllData(this.currentTargetData.getGroupId());
			this.currentTargetData = targetData.firstElement();
		}
		return this.currentTargetData;
	}
}
