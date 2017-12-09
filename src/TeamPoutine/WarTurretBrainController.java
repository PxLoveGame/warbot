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

		List <WarAgentPercept> percepts = getPercepts();
		percepts.removeIf(p -> !isEnemy(p));

		if (percepts.isEmpty()) {
			return idle();
		}

		Collections.sort(percepts, (w1, w2) -> Double.compare(w1.getDistance(),w2.getDistance()));
		WarAgentPercept enemy = percepts.get(0);
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
