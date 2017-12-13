package TeamPoutine;


import java.math.*;
import edu.warbot.agents.projectiles.WarShell;
import java.lang.reflect.Field;
import edu.warbot.tools.geometry.PolarCoordinates;
import edu.warbot.communications.WarMessage;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.tools.WarMathTools;

import edu.warbot.agents.percepts.WarAgentPercept;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

class Utils {

	public static final int MAX_DISTANCE_FROM_FOOD = 250;
	public static final int MAX_DISTANCE_FROM_BASE = 150;
	public static String teamName = null;

	public static double getShotAngle(WarAgentPercept enemy, double bulletSpeed) {
		double predictionError = 1.0e-6;
		String type = enemy.getType().name();
		try {
			Class enemyClass = Class.forName("edu.warbot.agents.agents." + type);
			Field SPEED = enemyClass.getDeclaredField("SPEED");
			double enemySpeed = (double) SPEED.get(enemy);
			double initialEnemyX = enemy.getDistance() * Math.cos(Math.toRadians(enemy.getAngle()));
			double initialEnemyY = enemy.getDistance() * Math.sin(Math.toRadians(enemy.getAngle()));
			double enemySpeedX = enemySpeed * Math.cos(Math.toRadians(enemy.getHeading()));
			double enemySpeedY = enemySpeed * Math.sin(Math.toRadians(enemy.getHeading()));

			double distanceToEnemy = Math.sqrt(Math.pow(initialEnemyX, 2) + Math.pow(initialEnemyX, 2));
			double bulletTimeToEnemy = distanceToEnemy / bulletSpeed;
			double angleToEnemy = Math.atan2(initialEnemyY, initialEnemyX);
			double angleError = 1000; // dumb initial value
			for (int i = 0; i < 100 || angleError > predictionError; i++) {
				double previousAngleToEnemy = angleToEnemy;
				double enemyX = initialEnemyX + enemySpeedX * bulletTimeToEnemy;
				double enemyY = initialEnemyY + enemySpeedY * bulletTimeToEnemy;

				distanceToEnemy = Math.sqrt(Math.pow(enemyX, 2) + Math.pow(enemyY, 2));
				bulletTimeToEnemy = distanceToEnemy / bulletSpeed;
				angleToEnemy = Math.atan2(enemyY, enemyX);

				angleError = Math.abs(angleToEnemy - previousAngleToEnemy);
			}
			if (bulletTimeToEnemy > WarShell.AUTONOMY) {
				return 0;
			} else {
				return Math.toDegrees(angleToEnemy);
			}
		} catch(ClassNotFoundException e) {
			return 0; // Agent is not killable
		} catch(NoSuchFieldException e) {
			return enemy.getAngle(); // Agent cannot move
		} catch(Exception e) {
			System.out.println(e);
		}
		return 0;
	}

	public static PolarCoordinates getFoodLocationFromBase(List<WarMessage> ms) {
		List<WarMessage> messages = new ArrayList<>(ms);
		for(WarMessage message : messages){
			if(message.getMessage().equals("food location")) {
				String[] content = message.getContent();
				double distance = Double.parseDouble(content[0]);
				double angle = Double.parseDouble(content[1]);
				PolarCoordinates foodLocation = getTargetedAgentPosition(message.getAngle(), message.getDistance(), angle, distance);
				return foodLocation;
			}
		}
		return null;
	}

	public static WarAgentPercept getNearestEnemyBuilding(List<WarAgentPercept> percepts) {
		List<WarAgentPercept> percepts_enemyTarget = new ArrayList<>(percepts);
		percepts_enemyTarget.removeIf((e) ->  !isEnemy(e));
		percepts_enemyTarget.removeIf((e) ->  e.getType() != WarAgentType.WarBase && e.getType() != WarAgentType.WarTurret);
		if(percepts_enemyTarget != null && percepts_enemyTarget.size() != 0){
			Collections.sort(percepts_enemyTarget, (w1, w2) -> Double.compare(w1.getDistance(),w2.getDistance()));
			return percepts_enemyTarget.get(0);
		}
		return null;
	}

	public static WarAgentPercept getNearestEnemyUnit(List<WarAgentPercept> percepts){
		List<WarAgentPercept> percepts_enemyTarget = new ArrayList<>(percepts);
		percepts_enemyTarget.removeIf((e) -> !isEnemy(e));
		percepts_enemyTarget.removeIf((e) -> e.getType() == WarAgentType.WarBase || e.getType() == WarAgentType.WarTurret);
		if(percepts_enemyTarget != null && percepts_enemyTarget.size() != 0){
			Collections.sort(percepts_enemyTarget,(w1, w2) -> Double.compare(w1.getDistance(),w2.getDistance()));
			return percepts_enemyTarget.get(0);
		}
		return null;
	}

	public static WarAgentPercept getNearestEnemy(List<WarAgentPercept> percepts){
		List<WarAgentPercept> percepts_enemyTarget = new ArrayList<>(percepts);
		percepts_enemyTarget.removeIf((e) -> !isEnemy(e));
		if(percepts_enemyTarget != null && percepts_enemyTarget.size() != 0){
			Collections.sort(percepts_enemyTarget,(w1, w2) -> Double.compare(w1.getDistance(),w2.getDistance()));
			return percepts_enemyTarget.get(0);
		}
		return null;
	}

	public static PolarCoordinates getTargetLocationFromExplorer(List<WarMessage> ms) {
		List<WarMessage> messages = new ArrayList<>(ms);
		for(WarMessage message : messages){
			if(message.getMessage().equals("Target here")) {
				String[] content = message.getContent();
				double distance = Double.parseDouble(content[0]);
				double angle = Double.parseDouble(content[1]);
				PolarCoordinates targetLocation = getTargetedAgentPosition(message.getAngle(), message.getDistance(), angle, distance);
				return targetLocation;
			}
		}
		return null;
	}

	public static double getShotAngleEnemyFromBase(List<WarMessage> ms) {
		List<WarMessage> messages = new ArrayList<>(ms);
		for(WarMessage message : messages){
			if(message.getMessage().equals("enemy at base")) {
				String[] content = message.getContent();
				double distance = Double.parseDouble(content[0]);
				double angle = Double.parseDouble(content[1]);
				PolarCoordinates targetLocation = getTargetedAgentPosition(message.getAngle(), message.getDistance(), angle, distance);
				return 0;
			}
		}
		return 0;
	}

	public static PolarCoordinates getTargetedAgentPosition(double angleToAlly, double distanceFromAlly, double angleFromAllyToTarget, double distanceBetweenAllyAndTarget) {
		return WarMathTools.addTwoPoints(new PolarCoordinates(distanceFromAlly, angleToAlly),
				new PolarCoordinates(distanceBetweenAllyAndTarget, angleFromAllyToTarget));
	}

	public static boolean isEnemy(WarAgentPercept percept) {
		return !percept.getTeamName().equals(teamName);
	}

}
