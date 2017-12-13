package TeamPoutine;

import edu.warbot.agents.agents.WarTurret;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.brains.WarTurretBrain;
import edu.warbot.agents.projectiles.WarShell;
import java.util.Collections;
import java.lang.ClassNotFoundException;
import java.lang.NoSuchFieldException;

import java.util.ArrayList;
import java.util.List;
import TeamPoutine.Utils;

import edu.warbot.agents.projectiles.WarShell;


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

		WarAgentPercept enemy = Utils.getNearestEnemyUnit(getPercepts());
		if (enemy == null) {
			//PolaCoordinate enemyLocation = Utils.getEnemyFromBase(getMessages());
			return idle();
		}
		double angle = Utils.getShotAngle(enemy, WarShell.SPEED);
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

}
