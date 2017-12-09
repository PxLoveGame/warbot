package TeamPoutine;


import java.math.*;
import edu.warbot.agents.projectiles.WarShell;
import java.lang.reflect.Field;
import edu.warbot.agents.percepts.WarAgentPercept;


class Utils {

    public static final int MAX_DISTANCE_FROM_FOOD = 250;
    public static final int MAX_DISTANCE_FROM_BASE = 100;

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

}
