package pxl;

import edu.warbot.agents.agents.WarEngineer;
import edu.warbot.brains.brains.WarEngineerBrain;

import edu.warbot.tools.geometry.PolarCoordinates;
import edu.warbot.communications.WarMessage;
import java.util.List;
import java.lang.reflect.Method;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import pxl.Utils;
import edu.warbot.brains.capacities.Building;

public abstract class WarEngineerBrainController extends WarEngineerBrain {

	private String ctask = "goToFood";

	public WarEngineerBrainController() {
		super();
	}

	public void sendMessage() {
		broadcastMessageToAgentType(WarAgentType.WarBase, "Ready to break some ass", "");
	}

	public String reflexes() {
		sendMessage();
		return null;
	}

	public String goToBase() {
		setDebugString("ENGINEER : Go To Base");
		List<WarAgentPercept> percepts = getPerceptsAlliesByType(WarAgentType.WarBase);

		if(percepts == null || percepts.size() == 0){
			WarMessage base = getBase();
			if (base != null) {
				setHeading(base.getAngle());
			}
		} else {
			WarAgentPercept base = percepts.get(0);

			if(base.getDistance() > Building.MAX_DISTANCE_BUILD){
				setDebugString("dist : " + base.getDistance());
				setHeading(base.getAngle());
			} else {
				setIdNextBuildingToRepair(base.getID());
				return repair();
			}
		}
		return move();
	}

	public String goToFood() {
		setDebugString("ENGINEER : Go To Food");
		PolarCoordinates foodLocation = getFoodLocationFromBase();
		if (foodLocation != null) {
			if (foodLocation.getDistance() > Utils.MAX_DISTANCE_FROM_FOOD - 50) {
				setHeading(foodLocation.getAngle());
			} else {
				if (Math.random() * 200 <= 1) {
					setNextBuildingToBuild(WarAgentType.WarTurret);
					ctask = "goToBase";
					return build();
				};
			}
		}
		setRandomHeading(5);
		return move();
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
}
