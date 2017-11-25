package pxl;

import edu.warbot.agents.agents.WarBase;
import edu.warbot.agents.enums.WarAgentCategory;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.brains.WarBaseBrain;
import edu.warbot.communications.WarMessage;
import edu.warbot.tools.geometry.PolarCoordinates;
import pxl.WarExplorerBrainController.ExplorerGroup;

import java.lang.reflect.Method;
import java.util.List;
import java.lang.Math;
import java.util.ArrayList;


public abstract class WarBaseBrainController extends WarBaseBrain {

	private static final int BEST_FOOD_DISTANCE = 480;

	private String ctask = "nothingToDo";
	private static final int MAX_LIGHT = 10;
	private static final int MAX_HEAVY = 10;
	private static final int MAX_EXPLORER = 7;
	private static final int MAX_ENGINEER = 2;
	private static final int MAX_ROCKETLAUNCHER = 2;
	private static final int MAX_KAMIKAZE = 0;
	private PolarCoordinates foodLocation;
	private PolarCoordinates enemyBaseLocation;

	private static final int MIN_HARVESTER = 5;

	public WarBaseBrainController() {
		super();
	}

	public void handleMessages() {
		List<WarMessage> messages = getMessages();
		for (WarMessage message : messages) {
			if (message.getMessage().equals("Where is the base ?")) {
				reply(message, "I'm here");
			} else if (message.getMessage().equals("enemy base")) {
				String[] content = message.getContent();
				double distance = Double.parseDouble(content[0]);
				double angle = Double.parseDouble(content[1]);
				enemyBaseLocation = getTargetedAgentPosition(message.getAngle(), message.getDistance(), angle, distance);
			}
		}
		updateFoodLocation();
		updateHarvesterCount();
	}

	public void updateFoodLocation() {
		List<WarMessage> messages = getMessages();
		for (WarMessage message : messages) {
			if (message.getMessage().equals("food around here")) {
				boolean isNewLocationBetter = foodLocation != null &&
							Math.abs(message.getDistance() - BEST_FOOD_DISTANCE)
							< Math.abs(foodLocation.getDistance() - BEST_FOOD_DISTANCE);
				if (foodLocation == null || isNewLocationBetter) {
					foodLocation = new PolarCoordinates(message.getDistance(), message.getAngle());
				}
			}
		}
	}

	private void updateHarvesterCount() {
		List<WarMessage> messages = getMessages();
		ArrayList<WarMessage> harvesters = new ArrayList<>();
		ArrayList<WarMessage> explorers = new ArrayList<>();
		for(WarMessage message : messages){
			if(message.getSenderType() == WarAgentType.WarExplorer && message.getMessage().equals("explorer group")) {
				int index = Integer.parseInt(message.getContent()[0]);
				ExplorerGroup g = ExplorerGroup.fromInteger(index);
				if (g == ExplorerGroup.HARVESTER) harvesters.add(message);
				if (g == ExplorerGroup.EXPLORER) explorers.add(message);
			}
		}

		if (harvesters.size() < MIN_HARVESTER && explorers.size() > 0) {
			int neededHarvesters = MIN_HARVESTER - harvesters.size();
			int canConvert = explorers.size();
			for (int i = 0; i < neededHarvesters  && i < canConvert; i++) {
				reply(explorers.get(i), "change group");
			}
		} else if (explorers.size() > MIN_HARVESTER && harvesters.size() > 0) {
			int canConvert = harvesters.size() - MIN_HARVESTER;
			for (int i = 0; i < canConvert; i++) {
				reply(explorers.get(i), "change group");
			}
		}
		setDebugString(harvesters.size() + "|" + explorers.size());

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
		messages.removeIf((message) -> !message.getMessage().equals("Ready to break some ass"));
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
		if (getHealth() > getMaxHealth() * 0.9) {
			ctask = "createUnits";
		} else if (getNbElementsInBag() >= 0) {
			ctask = "regenerate";
		} else {
			ctask = "nothingToDo";
		}
	}

	public void reflexes() {
		// setDebugString("nourriture : " + getNbElementsInBag());
		handleMessages();
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
