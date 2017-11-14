package pxl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import edu.warbot.agents.agents.WarExplorer;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.agents.percepts.WarPercept;
import edu.warbot.agents.resources.WarFood;
import edu.warbot.brains.brains.WarExplorerBrain;
import edu.warbot.communications.WarMessage;

public abstract class WarExplorerBrainController extends WarExplorerBrain {

	private boolean shouldEmptyBag = false;
	private String ctask = "findFood";
	private WarAgentPercept base;

    public WarExplorerBrainController() {
        super();
	}

	public String findFood() {
		// setDebugString("findFood, sac : " + getNbElementsInBag());
		List<WarAgentPercept> percepts_ressource = getPercepts();

		if(percepts_ressource == null || percepts_ressource.size() == 0){
			setRandomHeading(20);
		} else {
			for(WarAgentPercept ressource : percepts_ressource){
				if(ressource.getType().equals(WarAgentType.WarFood)){
					// setDebugString("Nourriture à proximité");
					broadcastMessageToAgentType(WarAgentType.WarExplorer, "food around here", "");
					setHeading(ressource.getAngle());
					if(ressource.getDistance() < WarFood.MAX_DISTANCE_TAKE){
						return take();
					}
					break;
				}
			}
		}
		return move();
	}

	public String returnToBase() {
		setDebugString("returnToBase, sac : " + getNbElementsInBag());
		List<WarAgentPercept> percepts = getPerceptsAlliesByType(WarAgentType.WarBase);

		if(percepts == null || percepts.size() == 0){
			List<WarMessage> messages = getMessages();
			for(WarMessage message : messages){
				if(message.getSenderType() == WarAgentType.WarBase){
					setHeading(message.getAngle());
				}
				broadcastMessageToAgentType(WarAgentType.WarBase, "Where is the base ?", "");
			}
		} else {
			WarAgentPercept base = percepts.get(0);

			if(base.getDistance() > MAX_DISTANCE_GIVE){
				setHeading(base.getAngle());
			} else {
				setIdNextAgentToGive(base.getID());
				return give();
			}
		}
		return move();
	}

	public void decide() {
		if (isBagEmpty() && ctask.equals("returnToBase")) {
			ctask = "findFood";
		} else if (isBagFull()) {
		    ctask = "returnToBase";
		}
		ctask = "returnToBase"; // TOREMOVE
	}

    @Override
    public String action() {
		decide();
		Class c = this.getClass();
		Method method;
		
		if (isBlocked()) setRandomHeading();
		
		String action = move(); // default Action
		try {
			method = c.getMethod(ctask, null);
			action = (String) method.invoke(this, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return action;
    }


}
