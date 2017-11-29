package pxl;

import java.util.List;
import java.lang.reflect.Method;

import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.brains.WarLightBrain;
import edu.warbot.communications.WarMessage;


public abstract class WarLightBrainController extends WarLightBrain {

    public static enum ArmyGroup {
		FIGHTER, DEFENDER;

		public static ArmyGroup fromInteger(int x) {
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
    
    private static final int MAX_DISTANCE_FROM_BASE = 250;
    private String ctask = "patrol";
    private ArmyGroup group = ArmyGroup.FIGHTER;

    public WarLightBrainController() {
        super();
    }

    public void handleChangeGroup() {
		List<WarMessage> messages = getMessages();
		for (WarMessage message : messages) {
			setDebugString(message.getMessage());
			if (message.getMessage().equals("change group")) {
				if (group == ArmyGroup.DEFENDER && ctask != "attaque") {
					group = ArmyGroup.FIGHTER;
					ctask = "explore";
				} else if (group == ArmyGroup.FIGHTER && ctask != "attaque") {
					group = ArmyGroup.DEFENDER;
					ctask = "patrol";
				}
			}
		}
    }
    
    // ToDo : Pas bouger toutes les unités / faire le même type de message pour les tours
    public void sendMessage() {
        broadcastMessageToAgentType(WarAgentType.WarBase, "Ready to break some ass", "");
        List<WarAgentPercept> wps = getPerceptsEnemies();
        for (WarAgentPercept wp : wps) {
            if (wp.getType().equals(WarAgentType.WarBase)) {
                broadcastMessageToAll("Enemy Base !!");
            }
        }
    }

    // Fighter ctask, explore le coté enemies de la map
    public String explore(){

    }

    // DEFENDER ctask,  patrouille autour de sa base. 
    public String patrol() {
        List<WarAgentPercept> wps = getPerceptsEnemies();
        setDebugString("LIGHT : Je Defend la base");
        setRandomHeading(20);

        if (!wps.isEmpty()) {
            ctask = "attaque";
            return  idle();
        } else {
            WarMessage base = getBase();
            if (base != null) {
                if (base.getDistance() > MAX_DISTANCE_FROM_BASE) {
                    setHeading(base.getAngle());
                }
            }
        }
        return move();
    }

    public String attaque() {
        List<WarAgentPercept> wps = getPerceptsEnemies();
        if(wps.isEmpty()){
            ctask = "patrol";
            return idle();
        }
        for (WarAgentPercept wp : wps) {
            if (!wp.getType().equals(WarAgentType.WarHeavy) &&
                !wp.getType().equals(WarAgentType.WarFood) &&
                !wp.getType().equals(WarAgentType.WarExplorer)) {

                setHeading(wp.getAngle());
                this.setDebugString("Attaque");
                if (isReloaded())
                    return fire();
                else if (isReloading())
                    return idle();
                else
                    return beginReloadWeapon();
            } else {
                setDebugString("je fui !");
                setHeading(wp.getAngle() + 180);
                return move();
            }
        }
        return move();
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

    public void reflexes() {
        sendMessage();
    }

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