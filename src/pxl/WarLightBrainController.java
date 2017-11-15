package pxl;

import java.util.List;
import java.lang.reflect.Method;

import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.brains.WarLightBrain;
import edu.warbot.communications.WarMessage;


public abstract class WarLightBrainController extends WarLightBrain {

    public String ctask = "patrol";

    public WarLightBrainController() {
        super();
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

    public String patrol() {
        List<WarAgentPercept> wps = getPerceptsEnemies();
        setDebugString("LIGHT : Je cherche la merde");
        setRandomHeading(20);

        if (!wps.isEmpty()) {
            ctask = "attaque";
            return  idle();
        } else {
            WarMessage enemyBase = getEnemyBase();
            if (enemyBase != null) {
                setHeading(enemyBase.getAngle());
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