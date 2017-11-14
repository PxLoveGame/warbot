package pxl;

import edu.warbot.agents.agents.WarBase;
import edu.warbot.agents.enums.WarAgentCategory;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.brains.WarBaseBrain;
import edu.warbot.communications.WarMessage;
import java.lang.reflect.Method;
import java.util.List;

public abstract class WarBaseBrainController extends WarBaseBrain {

    public String ctask;

    public WarBaseBrainController() {
        super();
    }

    public void respondToMessages() {
        List<WarMessage> messages = getMessages();

        for (WarMessage message : messages) {
            if (message.getMessage().equals("Where is the base ?"))
                reply(message, "I'm here");
        }
    }

    public void sendMessage() {
        for (WarAgentPercept percept : getPerceptsEnemies()) {
            if (isEnemy(percept) && percept.getType().getCategory().equals(WarAgentCategory.Soldier))
                broadcastMessageToAll("I'm under attack",
                        String.valueOf(percept.getAngle()),
                        String.valueOf(percept.getDistance()));
        }

        for (WarAgentPercept percept : getPerceptsResources()) {
            if (percept.getType().equals(WarAgentType.WarFood))
                broadcastMessageToAgentType(WarAgentType.WarExplorer, "I detected food",
                        String.valueOf(percept.getAngle()),
                        String.valueOf(percept.getDistance()));
        }
    }

    public String createWarLight() {
        setNextAgentToCreate(WarAgentType.WarLight);
        return create();
    }

    public String createExplorer() {
        setNextAgentToCreate(WarAgentType.WarExplorer);
        return create();
    }

    public String regenerate() {
        return eat();
    }

    public String nothingToDo() {
        return idle();
    }

    public void decide() {
        if (getHealth() > getMaxHealth() * 0.8) {
            ctask = "createWarLight";
        } else if (getNbElementsInBag() >= 0) {
            ctask = "regenerate";
        } else {
            ctask = "nothing";
        }
        ctask = "idle"; // TOREMOVE
    }

    @Override
    public String action() {
        respondToMessages();
		decide();
		Class c = this.getClass();
		Method method;
		String action = idle(); // default Action else java not happy
		try {
			method = c.getMethod(ctask, null);
			action = (String) method.invoke(this, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return action;
    }

}
