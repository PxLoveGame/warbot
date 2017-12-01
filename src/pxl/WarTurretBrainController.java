package pxl;

import edu.warbot.agents.agents.WarTurret;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.brains.WarTurretBrain;

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
        if (isReloaded()) {
            return WarTurret.ACTION_FIRE;
        }

        List <WarAgentPercept> percepts = getPercepts();
        for (WarAgentPercept p : percepts) {
            if (isEnemy(p)) {
                getPerfectShot(p);
                setHeading(p.getAngle());
                if (isReloaded()) {
                    return WarTurret.ACTION_FIRE;
                } else
                    return WarTurret.ACTION_RELOAD;
            }
        }

        return WarTurret.ACTION_IDLE;
    }

    void getShotAngle(WarAgentPercept enemy) {
        String type = enemy.getType().name();
        try {
            Class enemyClass = Class.forName("edu.warbot.agents.agents." + type);
            Field SPEED = enemyClass.getDeclaredField("SPEED");
            double enemySpeed = SPEED.get(enemy);
            System.out.println(SPEED.get(enemy));
            double enemyX = enemySpeed * Math.cos(Math.toRadians(enemy.getAngle()));
            double enemyY = enemySpeed * Math.sin(Math.toRadians(enemy.getAngle()));

            double speedRatio = enemySpeed/WarShell.SPEED;
            double b = 180 - enemy.getHeading() - enemy.getAngle();
            double C = enemy.getDistance();

            double angle = 
        } catch(Exception e) { System.out.println(e); }
    }
}
