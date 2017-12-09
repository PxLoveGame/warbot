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
		setDebugString("HEAVY : J'attaque la base enemies");
		PolarCoordinates foodLocation = getFoodLocationFromBase();
		if (foodLocation != null) {
			if (foodLocation.getDistance() > Utils.MAX_DISTANCE_FROM_FOOD) {
				setHeading(foodLocation.getAngle());
			}
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
		List <WarAgentPercept> percepts = getPercepts();
		percepts.removeIf(p -> !isEnemy(p));

		if (percepts.isEmpty()) {
			return null;
		}

		Collections.sort(percepts, (w1, w2) -> Double.compare(w1.getDistance(),w2.getDistance()));
		WarAgentPercept enemy = percepts.get(0);
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

	private WarMessage getEnemyBase(){
		List<WarMessage> messages = getMessages();
		for(WarMessage message : messages){
			if(message.getMessage().equals("Enemy Base !!")) return message;
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

	private PolarCoordinates getFoodLocationFromBase() {
		List<WarMessage> messages = getMessages();
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

	public String reflexes() {
		handleChangeGroup();
		sendMessage();
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