package pxl;

import edu.warbot.agents.agents.WarTurret;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.brains.WarTurretBrain;
import edu.warbot.agents.projectiles.WarShell;
import java.util.Collections;
import java.lang.ClassNotFoundException;
import java.lang.NoSuchFieldException;

import java.util.ArrayList;
import java.util.List;
import java.math.*;
import java.lang.reflect.Field;

public abstract class WarTurretBrainController extends WarTurretBrain {

    private int direction;

    public WarTurretBrainController() {
        super();

        direction = 0;
    }

    @Override
    public String action() {

        direction += 90;
        if (direction == 360) {
            direction = 0;
        }
        setHeading(direction);

        List <WarAgentPercept> percepts = getPercepts();
        percepts.removeIf(p -> !isEnemy(p));

        if (percepts.isEmpty()) {
            return idle();
        }

        Collections.sort(percepts, (w1, w2) -> Double.compare(w1.getDistance(),w2.getDistance()));
        WarAgentPercept enemy = percepts.get(0);
        double angle = getShotAngle(enemy);
        if (angle != 0) {
            setHeading(angle);
                if (isReloaded())
                    return fire();
                else if (isReloading())
                    return idle();
                else
                    return beginReloadWeapon();
        }

        return idle();
    }

    double getShotAngle(WarAgentPercept enemy) {
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
            double bulletTimeToEnemy = distanceToEnemy / WarShell.SPEED;
            double angleToEnemy = Math.atan2(initialEnemyY, initialEnemyX);
            double angleError = 1000; // dumb initial value
            for (int i = 0; i < 100 || angleError > predictionError; i++) {
                double previousAngleToEnemy = angleToEnemy;
                double enemyX = initialEnemyX + enemySpeedX * bulletTimeToEnemy;
                double enemyY = initialEnemyY + enemySpeedY * bulletTimeToEnemy;

                distanceToEnemy = Math.sqrt(Math.pow(enemyX, 2) + Math.pow(enemyY, 2));
                bulletTimeToEnemy = distanceToEnemy / WarShell.SPEED;
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

}
