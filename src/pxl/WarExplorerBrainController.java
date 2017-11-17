package pxl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import edu.warbot.tools.geometry.PolarCoordinates;
import edu.warbot.agents.agents.WarExplorer;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.agents.percepts.WarPercept;
import edu.warbot.agents.resources.WarFood;
import edu.warbot.brains.brains.WarExplorerBrain;
import edu.warbot.communications.WarMessage;

public abstract class WarExplorerBrainController extends WarExplorerBrain {

	private String ctask = "findFood";
	private int food_timer = 600;

    public WarExplorerBrainController() {
        super();
	}

	public void sendMessage() {
        broadcastMessageToAgentType(WarAgentType.WarBase, "Ready to break some ass", "");
	}

	public String findFood() {
		if (isBagFull() || (!isBagEmpty() && food_timer <= 0)) {
			ctask = "returnToBase";
			return idle();
		}
		setDebugString("findFood, sac : " + getNbElementsInBag() + " Timer : "+ food_timer);
		List<WarAgentPercept> percepts_ressource = getPercepts();

		if(percepts_ressource == null || percepts_ressource.size() == 0){
			WarMessage food = getFood();
			if(food != null) {
				setHeading(food.getAngle());
			} else {
				PolarCoordinates foodLocation = getFoodLocation();
				if (foodLocation != null) {
					if (foodLocation.getDistance() > 200) {
						setHeading(foodLocation.getAngle());
					}
				} else {
					setRandomHeading(5);
				}
			}
		} else {
			for(WarAgentPercept ressource : percepts_ressource){
				if(ressource.getType().equals(WarAgentType.WarFood)){
					setDebugString("Nourriture à proximité");
					broadcastMessageToAll("food around here");
					setHeading(ressource.getAngle());
					if(ressource.getDistance() < WarFood.MAX_DISTANCE_TAKE){
						food_timer = 600;
						return take();
					} 
					break;
				}
			}
		}
		if(food_timer > 0) food_timer--;	
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

	private PolarCoordinates getFoodLocation() {
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

	private WarMessage getFood(){
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

	private void reflexes() {
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

		return action;
    }


}
