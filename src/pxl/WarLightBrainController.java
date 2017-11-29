package pxl;

import java.util.List;
import java.lang.reflect.Method;

import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.brains.WarLightBrain;
import edu.warbot.communications.WarMessage;
import edu.warbot.tools.geometry.PolarCoordinates;



public abstract class WarLightBrainController extends WarLightBrain {

    public static enum LightGroup {
		FIGHTER, DEFENDER;

		public static LightGroup fromInteger(int x) {
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
    
    private static final int MAX_DISTANCE_FROM_BASE = 100;
    private static final int MAX_DISTANCE_FROM_FOOD = 250;
    
    private String ctask = "defender";
    private LightGroup group = LightGroup.DEFENDER;

    public WarLightBrainController() {
        super();
    }

    public void handleChangeGroup() {
		List<WarMessage> messages = getMessages();
		for (WarMessage message : messages) {
			setDebugString(message.getMessage());
			if (message.getMessage().equals("change group")) {
				if (group == LightGroup.DEFENDER) {
					group = LightGroup.FIGHTER;
					ctask = "fighter";
				} else if (group == LightGroup.FIGHTER) {
					group = LightGroup.DEFENDER;
					ctask = "defender";
				}
			}
		}
    }
    
    // ToDo : Pas bouger toutes les unités / faire le même type de message pour les tours
    public void sendMessage() {
        broadcastMessageToAgentType(WarAgentType.WarBase, "Ready to break some ass", "");
        broadcastMessageToAgentType(WarAgentType.WarBase,"Light group", group.toString());
        List<WarAgentPercept> wps = getPerceptsEnemies();
        for (WarAgentPercept wp : wps) {
            if (wp.getType().equals(WarAgentType.WarBase)) {
                broadcastMessageToAll("Enemy Base !!");
            }
        }
    }

    // Fighter ctask, explore autour de la zone de nourriture.
    public String fighter(){
        setDebugString("LIGHT : J'attaque la base enemies");
        PolarCoordinates foodLocation = getFoodLocationFromBase();
        if (foodLocation != null) {
            if (foodLocation.getDistance() > MAX_DISTANCE_FROM_FOOD) {
                setHeading(foodLocation.getAngle());
            }
        }
        setRandomHeading(5);
        return move();
    }

    // DEFENDER ctask,  patrouille autour de sa base. 
    public String defender() {
        setDebugString("LIGHT : Je Defend la base");
        setRandomHeading(5);

        WarMessage base = getBase();
        if (base != null) {
            if (base.getDistance() > MAX_DISTANCE_FROM_BASE) {
                setHeading(base.getAngle());
            }
        }
    return move();
    }

    public String shoot() {
        List<WarAgentPercept> wps = getPerceptsEnemies();        
        for (WarAgentPercept wp : wps) {
            if (!wp.getType().equals(WarAgentType.WarHeavy) &&
                !wp.getType().equals(WarAgentType.WarFood) &&
                !wp.getType().equals(WarAgentType.WarExplorer)) {

                setHeading(wp.getAngle());
                this.setDebugString("Attaque");
                if (isReloaded())
                    return fire();
                else if (isReloading())
                    return null;
                else
                    return beginReloadWeapon();
            }
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