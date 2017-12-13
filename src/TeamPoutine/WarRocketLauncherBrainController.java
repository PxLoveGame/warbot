package TeamPoutine;

import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.projectiles.WarRocket;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.brains.WarRocketLauncherBrain;
import edu.warbot.tools.geometry.PolarCoordinates;
import edu.warbot.communications.WarMessage;


import java.lang.reflect.Method;
import java.util.List;
import java.lang.Math;
import java.util.ArrayList;

public abstract class WarRocketLauncherBrainController extends WarRocketLauncherBrain {

	private String ctask = "waitingInstruction";

	private static final double MIN_DISTANCE_FROM_ENEMYTARGET = (WarRocket.AUTONOMY * WarRocket.SPEED);

	public WarRocketLauncherBrainController() {
		super();
	}

	private void handleMessages(){
		broadcastMessageToAgentType(WarAgentType.WarBase, "Ready to break some ass");
		List<WarMessage> messages = getMessages();
		for(WarMessage message : messages){
			if(message.getMessage().equals("Target here")) {
				this.ctask = "orderToShoot";
			}
		}
	}

	public String orderToShoot() {
		setDebugString("Let's go break some noobs !");
		broadcastMessageToAll("Rocket attack");
		PolarCoordinates targetLocation = Utils.getTargetLocationFromExplorer(getMessages());
		if(targetLocation != null) {
			setHeading(targetLocation.getAngle());
			if(targetLocation.getDistance() <= MIN_DISTANCE_FROM_ENEMYTARGET){
				this.setTargetDistance(targetLocation.getDistance());
				if (isReloaded())
					return fire();
				else if (isReloading())
					return idle();
				else
					return beginReloadWeapon();
			} else {
				return move();
			}
		} else {
			this.ctask = "waitingInstruction";
		}
		return idle();
	}

	public String waitingInstruction() {
		setDebugString("Waiting for instructions !");
		PolarCoordinates foodLocation = Utils.getFoodLocationFromBase(getMessages());
		if (foodLocation != null) {
			if (foodLocation.getDistance() > Utils.MAX_DISTANCE_FROM_FOOD - 50) {
				setHeading(foodLocation.getAngle());
			}
		}
		setRandomHeading(5);
		return move();
	}

	public String reflexes() {
		handleMessages();
		return null;
	}

	@Override
	public String action() {
		String action = reflexes();
		if (action != null) { return action; }

		Class c = this.getClass();
		Method method;

		action = move(); // default Action
		try {
			method = c.getMethod(ctask);
			action = (String) method.invoke(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (isBlocked()) setRandomHeading();

		return action;

	}

}