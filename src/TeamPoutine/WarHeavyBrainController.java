package TeamPoutine;

import java.util.List;
import java.lang.reflect.Method;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.brains.WarHeavyBrain;
import edu.warbot.brains.brains.WarRocketLauncherBrain;
import edu.warbot.communications.WarMessage;

import edu.warbot.tools.geometry.PolarCoordinates;
import TeamPoutine.Utils;
import java.util.Collections;

import edu.warbot.agents.projectiles.WarShell;



public abstract class WarHeavyBrainController extends  WarHeavyBrain {

	public static enum HeavyGroup {
		FIGHTER, DEFENDER;

		public static HeavyGroup fromInteger(int x) {
			switch(x) {
			case 0:
				return FIGHTER;
			case 1:
				return DEFENDER;
			}
			return null;
		}

		public String toString() {
			return String.valueOf(this.ordinal());
		}
	}

	private String ctask = "defender";
	private HeavyGroup group = HeavyGroup.DEFENDER;

	public WarHeavyBrainController() {
		super();
	}

	public void handleChangeGroup() {
		List<WarMessage> messages = getMessages();
		for (WarMessage message : messages) {
			setDebugString(message.getMessage());
			if (message.getMessage().equals("change group")) {
				if (group == HeavyGroup.DEFENDER) {
					group = HeavyGroup.FIGHTER;
					ctask = "fighter";
				} else if (group == HeavyGroup.FIGHTER) {
					group = HeavyGroup.DEFENDER;
					ctask = "defender";
				}
			}
		}
	}

	public void sendMessage() {
		broadcastMessageToAgentType(WarAgentType.WarBase, "Ready to break some ass", "");
		broadcastMessageToAgentType(WarAgentType.WarBase,"Heavy group",group.toString());
		List<WarAgentPercept> wps = getPerceptsEnemies();
		for (WarAgentPercept wp : wps) {
			if (wp.getType().equals(WarAgentType.WarBase)) {
				broadcastMessageToAll("Enemy Base !!");
			}
		}
	}

	public String fighter(){
		setDebugString("HEAVY : Patrouille dans la zone de nourriture");
		PolarCoordinates foodLocation = Utils.getFoodLocationFromBase(getMessages());
		if (foodLocation != null) {
			if (foodLocation.getDistance() > Utils.MAX_DISTANCE_FROM_FOOD) {
				setHeading(foodLocation.getAngle());
			}
		}
		WarAgentPercept target = Utils.getNearestEnemyBuilding(getPercepts());
		if (target != null) {
			setHeading(target.getAngle() + 180);
		}
		setRandomHeading(5);
		return move();
	}

	// DEFENDER ctask,  patrouille autour de sa base.
	public String defender() {
		setDebugString("HEAVY : Je Defend la base");
		setRandomHeading(5);

		WarMessage base = getBase();
		if (base != null) {
			if (base.getDistance() > Utils.MAX_DISTANCE_FROM_BASE) {
				setHeading(base.getAngle());
			}
		}
	return move();
	}


	public String shoot() {
		WarAgentPercept enemy = Utils.getNearestEnemyUnit(getPercepts());
		if (enemy == null) {
			return null;
		}
		double angle = Utils.getShotAngle(enemy, WarShell.SPEED);
		if (angle != 0) {
			setHeading(angle);
				if (isReloaded())
					return fire();
				else if (isReloading())
					return null;
				else
					return beginReloadWeapon();
		}
		return null;
	}

	private WarMessage getBase() {
		broadcastMessageToAgentType(WarAgentType.WarBase, "Where is the base ?", "");
		List<WarMessage> messages = getMessages();
		for(WarMessage message : messages) {
			if(message.getSenderType() == WarAgentType.WarBase){
				return message;
			}
		}
		return null;
	}

	public Boolean runAway(){
		WarAgentPercept target = Utils.getNearestEnemyBuilding(getPercepts());
		if (target != null && target.getType() == WarAgentType.WarTurret) {
			setHeading(target.getAngle() + 180);
			return true;
		}
		return false;
	}

	public String reflexes() {
		handleChangeGroup();
		sendMessage();
		if(runAway()){
			return move();
		}
		return shoot();
	}

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