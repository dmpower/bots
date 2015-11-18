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
import com.ywp.robocode.utils.HeadOnGun;
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
	private final static boolean PAINT_MOVEMENT       = true;
	private final static boolean PAINT_GUN            = true;
	private final static boolean PAINT_BOT_RAYS       = true;
	private static final boolean PAINT_CURRENT_TARGET = true;

	static double duelingDistance = 500d;
	ArrayList<MovementWave> moveWaves=new ArrayList<MovementWave>();

	private TargetBot currentTargetData = null;
	double lastTargetChange = 0d;
	// Basically the time it take to turn the gun a full circle.
	static final double targetChangeThreshold = 2*Math.PI/Rules.GUN_TURN_RATE_RADIANS;
	private static final double GUN_FIRE_THRESHOLD = 200d;

	boolean isSweeping;
	double sweepTime = 0d;
	static final double SWEEP_INTERVAL = 100d;
	static final double RADAR_TOLLARENCE = Rules.RADAR_TURN_RATE_RADIANS/32;
	// allowing a full sweep causes slippage. This allows it to adjust to keep target centered
	static final double RADAR_MAX_SWEEP  = Rules.RADAR_TURN_RATE_RADIANS-RADAR_TOLLARENCE;
	static final double RADAR_HALF_SWEEP = RADAR_MAX_SWEEP/2;

	private static double RAY;

	RepositoryManager<TargetBot> targetManager;

	Vector<Gun> gunRack = new Vector<>();

	private boolean endTurn = true;
	private long time;
	private boolean isRanIntoBot;



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
			resetTurn();
			execute();
		}
	}

	private void initialize() {
		RAY = getBattleFieldWidth() + getBattleFieldHeight();
		duelingDistance = Math.min(500, getBattleFieldWidth()/2);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setColors(Color.red,Color.yellow,Color.blue);
		setBulletColor(Color.red);
		setRadarColor(Color.yellow);

		this.targetManager = new RepositoryManager<TargetBot>();

		this.gunRack.add(new HeadOnGun(this));
		this.gunRack.add(new CircularGun(this.targetManager, this));
		resetTurn();
	}

	private void resetTurn() {
		this.endTurn = true;
		this.isRanIntoBot = false;
	}

	/* (non-Javadoc)
	 * @see robocode.Robot#onScannedRobot(robocode.ScannedRobotEvent)
	 */
	@Override
	public void onScannedRobot(ScannedRobotEvent event) {
		this.out.println("onScannedRobot turn: " + getTime() + " for " + event.getName());
		TargetBot target = new TargetBot(this, event);
		this.targetManager.add(target);
		pickTarget(target);
		if ( ! (this.isRanIntoBot && target.getName().equals(currentTarget().getName()))) {
			Vector<TargetBot> targetData = this.targetManager.getAllData(currentTarget());
			if (targetData.size() > 1 && targetData.get(0).getTime() == targetData.get(1).getTime()+1) {
				double energyChange = targetData.get(1).getEnergy()-targetData.get(0).getEnergy();
				if(energyChange<=3&&energyChange>=0.1){
					logMovementWave(target,energyChange);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see robocode.Robot#onRobotDeath(robocode.RobotDeathEvent)
	 */
	@Override
	public void onRobotDeath(RobotDeathEvent event) {
		if (hasTarget() && currentTarget().getName().equals(event.getName())){
			clearTarget();
		}
		this.targetManager.removeGroup(new TargetBot(this, event));
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
						rating+=Math.abs(BotTools.convertToPoint(this).distance(movePoint)-500);
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
		this.out.println("Turn: " + getTime() + " - hit " + event.getName() + " and they have " + event.getEnergy() + " left.");
		// target who hit us unless we don't have any data on them - they died
		TargetBot target = new TargetBot(this, event, getWidth());
		if(this.targetManager.getAllGroupIds().contains(target.getGroupId())) {
			setTarget(target);
			this.isRanIntoBot = true;
		}
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
		//		this.out.println("Rader turn: " + currentTime + " remaining turn: " + getRadarTurnRemainingRadians());
		if (getOthers() > 1 && (this.sweepTime == 0 || this.sweepTime+SWEEP_INTERVAL < currentTime)){
			//			this.out.println("Radar sweeping");
			this.isSweeping = true;
			this.sweepTime = currentTime+1;
			setTurnRadarRightRadians(Math.PI*2); // full circle
		}

		if(!this.isSweeping && hasTarget() && currentTarget().getTime() == currentTime){
			//absBearing
			double angle=currentTarget().getBearingRadians()+getHeadingRadians();
			//true on adjustment
			angle = Utils.normalRelativeAngle(angle-getRadarHeadingRadians());
			int turnDir = angle>0?1:-1;
			//additional adjustment to maximize sweep
			angle = angle + (turnDir * (RADAR_HALF_SWEEP-RADAR_TOLLARENCE));
			// Do not adjust more than the max turn rate
			if( 1 == turnDir) {
				angle=Math.min(angle, RADAR_MAX_SWEEP);
			}else {
				angle=Math.max(angle, turnDir * RADAR_MAX_SWEEP);
			}
			setTurnRadarRightRadians(Utils.normalRelativeAngle(angle));
		}

		//		this.out.println("Rader remaining turn: " + getRadarTurnRemainingRadians());

		if(Math.abs(getRadarTurnRemainingRadians())< RADAR_TOLLARENCE){
			//			this.out.println("Radar completed turn");
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
				Gun selectedGun = this.gunRack.firstElement();
				for (Gun gun : this.gunRack) {
					if(gun.isValid(currentTarget())) {
						gun.feedTarget(currentTarget());
						if (compareGunStats(gun, selectedGun) > 0)
						{
							selectedGun = gun;
						}
					}
				}
				selectedGun.aimRadians(currentTarget());
				selectedGun.fire();
			} catch (Exception e) {
				// log and eat it
				this.out.println(e.getMessage());
			}
		}
		for (Gun gun : this.gunRack) {
			gun.update();
			//			if(hasTarget()){
			//				GunStats stats = gun.getStats(currentTarget());
			//				this.out.print(gun.getClass().toString() + ": ");
			//				this.out.println(stats.toString() + " for " + currentTarget().getName());
			//			}else{
			//				GunStats stats = gun.getStats();
			//				this.out.print(gun.getClass().toString() + ": ");
			//				this.out.println(stats.toString());
			//
			//			}
		}
	}

	private int compareGunStats(Gun gun1, Gun gun2) {
		GunStats gunStats1 = gun1.getStats(currentTarget());
		GunStats gunStats2 = gun2.getStats(currentTarget());
		// these two checks allow the gun to get some time in after initial firings.
		int results = (gun1.lastFired() + GUN_FIRE_THRESHOLD > getTime())?1:0;
		if (0 == results) {
			results = (gun2.lastFired() + GUN_FIRE_THRESHOLD > getTime())?-1:0;
		}

		// give each gun a chance to fire at least once per target
		if (0 == results) {
			results = gunStats1.getShots()==0?1:0; // give 1 a chance
		}
		if (0 == results) {
			results = gunStats2.getShots()==0?-1:0; // give 2 a chance
		}

		// don't let low stats prevent an overall good gun from having a chance.
		double average1 = Math.max(gunStats1.getAverage(), gun1.getStats().getAverage()/2);
		double average2 = Math.max(gunStats2.getAverage(), gun2.getStats().getAverage()/2);
		double averageDiff = average1 - average2;
		if (0 == results && Math.abs(averageDiff)> 0.01) { // both have had chances, which is better?
			results = averageDiff>0?1:-1;
		}
		// if this is still 0 the guns are close enough to be equal

		return results;
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

	@Override
	public void setTurnGunRightRadians(double radians) {
		super.setTurnGunRightRadians(radians);
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

		if(PAINT_BOT_RAYS) {

			//			if (hasTarget()){
			//				Point selfPoint = BotTools.convertToPoint(this);
			//				double absBearing=currentTarget().getBearingRadians()+getHeadingRadians();
			//				Point targetPoint = BotTools.project(selfPoint, RAY, absBearing);
			//				g.setColor(Color.yellow);
			//				g.drawLine((int)targetPoint.getX(), (int)targetPoint.getY(), (int)getX(), (int)getY());
			//			}

			Point origin = BotTools.convertToPoint(this);

			// Gun direction
			Point tempPoint = BotTools.project(origin, RAY, getGunHeadingRadians());
			g.setColor(Color.blue);
			g.drawLine((int)origin.getX(), (int)origin.getY(), (int)tempPoint.getX(), (int)tempPoint.getY());

			// bot direction
			tempPoint = BotTools.project(origin, RAY, getHeadingRadians());
			g.setColor(Color.white);
			g.drawLine((int)origin.getX(), (int)origin.getY(), (int)tempPoint.getX(), (int)tempPoint.getY());

		}

		if(PAINT_GUN){
			for (Gun gun : this.gunRack) {
				gun.onPaint(g);
			}
		}

		if (PAINT_CURRENT_TARGET) {
			if (hasTarget()) {
				int botWidth = (int)getWidth();
				int botHalfWidth = botWidth/2;
				g.setColor(Color.yellow);
				Point targetPoint = BotTools.convertToPoint(this, currentTarget());
				g.drawRect((int)targetPoint.getX()-botHalfWidth, (int)targetPoint.getY()-botHalfWidth, botWidth, botWidth);
			}
		}
	}

}
