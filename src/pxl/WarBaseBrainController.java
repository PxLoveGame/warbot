package pxl;

import edu.warbot.agents.agents.WarBase;
import edu.warbot.agents.enums.WarAgentCategory;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.brains.WarBaseBrain;
import edu.warbot.communications.WarMessage;
import edu.warbot.tools.geometry.PolarCoordinates;
import java.lang.reflect.Method;
import java.util.List;

public abstract class WarBaseBrainController extends WarBaseBrain {

    private String ctask = "nothingToDo";
    private static final int MAX_LIGHT = 10;
    private static final int MAX_HEAVY = 10;
    private static final int MAX_EXPLORER = 5;
    private static final int MAX_ENGINEER = 2;
    private static final int MAX_ROCKETLAUNCHER = 2;
    private static final int MAX_KAMIKAZE = 0;
    private PolarCoordinates foodLocation;


    public WarBaseBrainController() {
        super();
    }

    public void respondToMessages() {
        List<WarMessage> messages = getMessages();
        for (WarMessage message : messages) {
            if (message.getMessage().equals("Where is the base ?"))
                reply(message, "I'm here");
            if (message.getMessage().equals("food around here")) {
                foodLocation = new PolarCoordinates(message.getDistance(), message.getAngle());
            }
        }
    }

    public void sendMessage() {
        for (WarAgentPercept percept : getPerceptsEnemies()) {
            if (isEnemy(percept) && percept.getType().getCategory().equals(WarAgentCategory.Soldier)) {
                broadcastMessageToAll("I'm under attack",
                        String.valueOf(percept.getAngle()),
                        String.valueOf(percept.getDistance()));
            }
        }
        if (foodLocation != null) {
            setDebugString("I have food Position : " + foodLocation.getDistance());
            broadcastMessageToAgentType(WarAgentType.WarExplorer,
                                        "food location",
                                        String.valueOf(foodLocation.getDistance()),
                                        String.valueOf(foodLocation.getAngle()));
        }
    }


    public String createUnits(){
        int[] nbUnits = calculateUnits();
        if(nbUnits[1] < MAX_HEAVY) setNextAgentToCreate(WarAgentType.WarHeavy);
        if(nbUnits[0] < MAX_LIGHT) setNextAgentToCreate(WarAgentType.WarLight);
        if(nbUnits[2] < MAX_EXPLORER) setNextAgentToCreate(WarAgentType.WarExplorer);
        return create();
    }

    public int[] calculateUnits(){
        int[] nbUnits = new int[6];
        List<WarMessage> messages = getMessages();
        for (WarMessage message : messages) {
            if(message.getSenderType() == WarAgentType.WarLight) nbUnits[0]++;
            else if(message.getSenderType() == WarAgentType.WarHeavy) nbUnits[1]++;
            else if(message.getSenderType() == WarAgentType.WarExplorer) nbUnits[2]++;
            else if(message.getSenderType() == WarAgentType.WarEngineer) nbUnits[3]++;
            else if(message.getSenderType() == WarAgentType.WarRocketLauncher) nbUnits[4]++;
            else if(message.getSenderType() == WarAgentType.WarKamikaze) nbUnits[5]++;
        }
        return nbUnits;
    }

    public String regenerate() {
        return eat();
    }

    public String nothingToDo() {
        return idle();
    }

    public void decide() {
        if (getHealth() > getMaxHealth() * 0.8) {
            // ctask = "createUnits";
        } else if (getNbElementsInBag() >= 0) {
            ctask = "regenerate";
        } else {
            ctask = "nothingToDo";
        }
    }

    public void reflexes() {
        setDebugString("nourriture : " + getNbElementsInBag());
        respondToMessages();
        sendMessage();
    }

    @Override
    public String action() {
        reflexes();
		decide();
		Class c = this.getClass();
		Method method;
		String action = idle(); // default Action else java not happy
		try {
			method = c.getMethod(ctask);
			action = (String) method.invoke(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return action;
    }

}
