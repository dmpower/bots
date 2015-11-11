package com.ywp.robocode.SuperSampleBots;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

/**
 * SuperSpinBot - a Super Sample Robot by CrazyBassoonist based on the robot Spinbot by Mathew Nelson and maintained by Flemming N. Larsen
 * <p/>
 * Circular movement, Circular targeting.... Everything is circles.
 *
 *                  S                            S
 *              R       P                    R       P
 *             E         I                  E         I
 *            P     O     N                P     O     N
 *             U         B                  U         B
 *               S     O                     S       O
 *                  T                            T
 */





public class SuperSpinBot extends AdvancedRobot {
	//gun variables
	static double enemyVelocities[][]=new double[400][4];
	static int currentEnemyVelocity;
	static int aimingEnemyVelocity;
	double velocityToAimAt;
	boolean fired;
	double oldTime;
	int count;
	int averageCount;

	//movement variables
	static double turn=2;
	int turnDir=1;
	int moveDir=1;
	double oldEnemyHeading;
	double oldEnergy=100;
	@Override
	public void run(){
		// Set colors
		setBodyColor(Color.red);
		setGunColor(Color.blue);
		setRadarColor(Color.black);
		setScanColor(Color.red);

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		do{
			turnRadarRightRadians(Double.POSITIVE_INFINITY);
		}while(true);
	}
	@Override
	public void onScannedRobot(ScannedRobotEvent e){
		double absBearing=e.getBearingRadians()+getHeadingRadians();
		Graphics2D g=getGraphics();

		//increase our turn speed amount each tick,to a maximum of 8 and a minimum of 4
		turn+=0.2*Math.random();
		if(turn>8){
			turn=2;
		}

		//when the enemy fires, we randomly change turn direction and whether we go forwards or backwards
		if(this.oldEnergy-e.getEnergy()<=3&&this.oldEnergy-e.getEnergy()>=0.1){
			if(Math.random()>.5){
				this.turnDir*=-1;
			}
			if(Math.random()>.8){
				this.moveDir*=-1;
			}
		}

		//we set our maximum speed to go down as our turn rate goes up so that when we turn slowly, we speed up and vice versa;
		setMaxTurnRate(turn);
		setMaxVelocity(12-turn);
		setAhead(90*this.moveDir);
		setTurnLeft(90*this.turnDir);
		this.oldEnergy=e.getEnergy();


		//find our which velocity segment our enemy is at right now
		if(e.getVelocity()<-2){
			currentEnemyVelocity=0;
		}
		else if(e.getVelocity()>2){
			currentEnemyVelocity=1;
		}
		else if(e.getVelocity()<=2&&e.getVelocity()>=-2){
			if(currentEnemyVelocity==0){
				currentEnemyVelocity=2;
			}
			else if(currentEnemyVelocity==1){
				currentEnemyVelocity=3;
			}
		}

		//update the one we are using to determine where to store our velocities if we have fired and there has been enough time for a bullet to reach an enemy
		//(only a rough approximation of bullet travel time);
		if(getTime()-this.oldTime>e.getDistance()/12.8&&this.fired==true){
			aimingEnemyVelocity=currentEnemyVelocity;
		}
		else{
			this.fired=false;
		}

		//record a new enemy velocity and raise the count
		enemyVelocities[this.count][aimingEnemyVelocity]=e.getVelocity();
		this.count++;
		if(this.count==400){
			this.count=0;
		}

		//calculate our average velocity for our current segment
		this.averageCount=0;
		this.velocityToAimAt=0;
		while(this.averageCount<400){
			this.velocityToAimAt+=enemyVelocities[this.averageCount][currentEnemyVelocity];
			this.averageCount++;
		}
		this.velocityToAimAt/=400;


		//pulled straight out of the circular targeting code on the Robowiki. Note that all I did was replace the enemy velocity and
		//put in pretty graphics that graph the enemies predicted movement(actually the average of their predicted movement)
		//Press paint on the robot console to see the debugging graphics.
		//Note that this gun can be improved by adding more segments and also averaging turn rate.
		double bulletPower = Math.min(2.4,Math.min(e.getEnergy()/4,getEnergy()/10));
		double myX = getX();
		double myY = getY();
		double enemyX = getX() + e.getDistance() * Math.sin(absBearing);
		double enemyY = getY() + e.getDistance() * Math.cos(absBearing);
		double enemyHeading = e.getHeadingRadians();
		double enemyHeadingChange = enemyHeading - this.oldEnemyHeading;
		this.oldEnemyHeading = enemyHeading;
		double deltaTime = 0;
		double battleFieldHeight = getBattleFieldHeight(),
				battleFieldWidth = getBattleFieldWidth();
		double predictedX = enemyX, predictedY = enemyY;
		while((++deltaTime) * (20.0 - 3.0 * bulletPower) <
				Point2D.Double.distance(myX, myY, predictedX, predictedY)){
			predictedX += Math.sin(enemyHeading) * this.velocityToAimAt;
			predictedY += Math.cos(enemyHeading) * this.velocityToAimAt;
			enemyHeading += enemyHeadingChange;
			g.setColor(Color.red);
			g.fillOval((int)predictedX-2,(int)predictedY-2,4,4);
			if(	predictedX < 18.0
					|| predictedY < 18.0
					|| predictedX > battleFieldWidth - 18.0
					|| predictedY > battleFieldHeight - 18.0){

				predictedX = Math.min(Math.max(18.0, predictedX),
						battleFieldWidth - 18.0);
				predictedY = Math.min(Math.max(18.0, predictedY),
						battleFieldHeight - 18.0);
				// the prediction hit a wall so stop predicting.
				break;
			}
		}
		double theta = Utils.normalAbsoluteAngle(Math.atan2(
				predictedX - getX(), predictedY - getY()));

		setTurnRadarRightRadians(Utils.normalRelativeAngle(
				absBearing - getRadarHeadingRadians())*2);
		setTurnGunRightRadians(Utils.normalRelativeAngle(
				theta - getGunHeadingRadians()));
		if(getGunHeat()==0){
			fire(bulletPower);
			this.fired=true;
		}



	}

}