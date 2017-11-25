package pxl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

import edu.warbot.tools.geometry.PolarCoordinates;
import edu.warbot.agents.agents.WarExplorer;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.agents.percepts.WarPercept;
import edu.warbot.agents.resources.WarFood;
import edu.warbot.brains.brains.WarExplorerBrain;
import edu.warbot.communications.WarMessage;

public abstract class WarExplorerBrainController extends WarExplorerBrain {

	public static enum ExplorerGroup {
		EXPLORER, HARVESTER;

		public string defaultTask;

		public static ExplorerGroup fromInteger(int x) {
			switch(x) {
			case 0:
				return EXPLORER;
			case 1:
				return HARVESTER;
			}
			return null;
		}

		public String toString() {
			return String.valueOf(this.ordinal());
		}
	}
	private static final int MAX_TIMER = 600;
	private static final int MAX_DISTANCE_FROM_FOOD = 250;

	private String ctask = "findFood";
	private int food_timer = MAX_TIMER;
	private ExplorerGroup group = ExplorerGroup.EXPLORER;

	public WarExplorerBrainController() {
		super();
	}

	public void sendMessage() {
		broadcastMessageToAgentType(WarAgentType.WarBase, "Ready to break some ass");
		broadcastMessageToAgentType(WarAgentType.WarBase,
									"explorer group",
									group.toString(),
									String.valueOf(getNbElementsInBag()));
		handleEnemyBase();
	}

	public void handleChangeGroup() {
		List<WarMessage> messages = getMessages();
		for (WarMessage message : messages) {
			setDebugString(message.getMessage());
			if (message.getMessage().equals("change group")) {
				if (group == ExplorerGroup.EXPLORER) {
					group = ExplorerGroup.HARVESTER;
					ctask = "findFood";
				} else if (group == ExplorerGroup.HARVESTER && getNbElementsInBag() == 0) {
					group = ExplorerGroup.EXPLORER;
					ctask = "explore";
				}
			}
		}
	}

	public void handleEnemyBase() {
		WarAgentPercept enemyBase = getEnemyBase();
		if (enemyBase != null) {
			broadcastMessageToAll("Enemy Base !!");
			broadcastMessageToAgentType(WarAgentType.WarBase,
										"enemy base",
										String.valueOf(enemyBase.getDistance()),
										String.valueOf(enemyBase.getAngle()));
		}
	}

	public String findFood() {
		if(food_timer > 0) {
			food_timer--;
		}
		if (isBagFull() || (!isBagEmpty() && food_timer <= 0)) {
			ctask = "returnToBase";
			return idle();
		}
		setDebugString("findFood, sac : " + getNbElementsInBag() + " Timer : "+ food_timer);
		List<WarAgentPercept> percepts_ressource = getPercepts();
		boolean noFoodAround = percepts_ressource == null || percepts_ressource.size() == 0;

		if(noFoodAround) {
			WarMessage food = getFoodFromOtherExplorer();
			if(food != null) {
				setHeading(food.getAngle());
			} else {
				PolarCoordinates foodLocation = getFoodLocationFromBase();
				if (foodLocation != null) {
					if (foodLocation.getDistance() > MAX_DISTANCE_FROM_FOOD) {
						setHeading(foodLocation.getAngle());
					}
				}
				setRandomHeading(5);
			}
		} else {
			percepts_ressource.removeIf(r -> !r.getType().equals(WarAgentType.WarFood));
			if (percepts_ressource.size() > 0) {
				Collections.sort(percepts_ressource, (w1, w2) -> Double.compare(w1.getDistance(),w2.getDistance()));
				WarAgentPercept ressource = percepts_ressource.get(0);
				setHeading(ressource.getAngle());
				setDebugString("Nourriture à proximité");
				broadcastMessageToAll("food around here");
				setHeading(ressource.getAngle());
				if(ressource.getDistance() < WarFood.MAX_DISTANCE_TAKE){
					food_timer = MAX_TIMER;
					return take();
				}
			}
		}
		return move();
	}

	public String returnToBase() {
		if (isBagEmpty()) {
			ctask = "findFood";
			return idle();
		}
		setDebugString("returnToBase, sac : " + getNbElementsInBag());
		List<WarAgentPercept> percepts = getPerceptsAlliesByType(WarAgentType.WarBase);

		if(percepts == null || percepts.size() == 0){
			WarMessage base = getBase();
			if (base != null) {
				setHeading(base.getAngle());
			}
		} else {
			WarAgentPercept base = percepts.get(0);
			if(base.getDistance() > MAX_DISTANCE_GIVE){
				setDebugString("dist : " + base.getDistance());
				setHeading(base.getAngle());
			} else {
				setIdNextAgentToGive(base.getID());
				return give();
			}
		}
		return move();
	}

	public String explore() {
		setDebugString("explore");
		setRandomHeading(5);
		return move();
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

	private WarMessage getFoodFromOtherExplorer(){
		List<WarMessage> messages = getMessages();
		for(WarMessage message : messages){
			if(message.getMessage().equals("food around here")) return message;
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

	private WarAgentPercept getEnemyBase() {
		List<WarAgentPercept> percepts = getPerceptsEnemiesByType(WarAgentType.WarBase);
		if (percepts != null && percepts.size() != 0) {
			return percepts.get(0);
		}
		return null;
	}

	private void reflexes() {
		handleChangeGroup();
		sendMessage();
	}

	@Override
	public String action() {
		reflexes();
		Class c = this.getClass();
		Method method;

		String action = move(); // default Action
		try {
			method = c.getMethod(ctask);
			action = (String) method.invoke(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (isBlocked()) setRandomHeading();
		//setDebugString(group.toString());

		return action;
	}


}
