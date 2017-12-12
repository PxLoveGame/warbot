package TeamPoutine;

import edu.warbot.agents.agents.WarBase;
import edu.warbot.agents.enums.WarAgentCategory;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.brains.WarBaseBrain;
import edu.warbot.communications.WarMessage;
import edu.warbot.tools.geometry.PolarCoordinates;
import TeamPoutine.WarExplorerBrainController.ExplorerGroup;
import TeamPoutine.WarLightBrainController.LightGroup;
import TeamPoutine.WarHeavyBrainController.HeavyGroup;


import java.lang.reflect.Method;
import java.util.List;
import java.lang.Math;
import java.util.ArrayList;

public abstract class WarBaseBrainController extends WarBaseBrain {

	private static final int BEST_FOOD_DISTANCE = 450;

	private String ctask = "nothingToDo";
	private static final int MAX_LIGHT = 6;
	private static final int MAX_HEAVY = 6;
	private static final int MAX_EXPLORER = 7;
	private static final int MAX_ENGINEER = 2;
	private static final int MAX_ROCKETLAUNCHER = 4;
	private static final int MAX_KAMIKAZE = 0;
	private PolarCoordinates foodLocation;
	private PolarCoordinates enemyBaseLocation;

	private static final int MIN_HARVESTER = 5;
	private static final int MIN_LIGHT_DEFENDER = 0;
	private static final int MIN_HEAVY_DEFENDER = 0;


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
		updateFighterHeavy();
		updateFighterLight();
	}

	public void updateFighterLight(){
		List<WarMessage> messages = getMessages();
		ArrayList<WarMessage> fighters = new ArrayList<>();
		ArrayList<WarMessage> defenders = new ArrayList<>();
		for(WarMessage message : messages){
			if(message.getSenderType() == WarAgentType.WarLight && message.getMessage().equals("Light group")) {
				int index = Integer.parseInt(message.getContent()[0]);
				LightGroup g = LightGroup.fromInteger(index);
				if (g == LightGroup.FIGHTER) fighters.add(message);
				if (g == LightGroup.DEFENDER) defenders.add(message);
			}
		}

		if (defenders.size() < MIN_LIGHT_DEFENDER && fighters.size() > 0) {
			int neededdefenders = MIN_LIGHT_DEFENDER - defenders.size();
			int canConvert = fighters.size();
			for (int i = 0; i < neededdefenders  && i < canConvert; i++) {
				reply(fighters.get(i), "change group");
			}
		} else if (defenders.size() > MIN_LIGHT_DEFENDER) {
			int canConvert = defenders.size() - MIN_LIGHT_DEFENDER - 1;
			for (int i = 0; i < canConvert; i++) {
				reply(defenders.get(i), "change group");
			}
		}
		setDebugString(fighters.size() + "|" + defenders.size());
	}

	public void updateFighterHeavy() {
		List<WarMessage> messages = getMessages();
		ArrayList<WarMessage> fighters = new ArrayList<>();
		ArrayList<WarMessage> defenders = new ArrayList<>();
		for(WarMessage message : messages){
			if(message.getSenderType() == WarAgentType.WarHeavy && message.getMessage().equals("Heavy group")) {
				int index = Integer.parseInt(message.getContent()[0]);
				HeavyGroup g = HeavyGroup.fromInteger(index);
				if (g == HeavyGroup.FIGHTER) fighters.add(message);
				if (g == HeavyGroup.DEFENDER) defenders.add(message);
			}
		}

		if (defenders.size() < MIN_LIGHT_DEFENDER && fighters.size() > 0) {
			int neededdefenders = MIN_LIGHT_DEFENDER - defenders.size();
			int canConvert = fighters.size();
			for (int i = 0; i < neededdefenders  && i < canConvert; i++) {
				reply(fighters.get(i), "change group");
			}
		} else if (defenders.size() > MIN_LIGHT_DEFENDER) {
			int canConvert = defenders.size() - MIN_LIGHT_DEFENDER - 1;
			for (int i = 0; i < canConvert; i++) {
				reply(defenders.get(i), "change group");
			}
		}
		setDebugString(fighters.size() + "|" + defenders.size());
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
		} else if (harvesters.size() > MIN_HARVESTER && explorers.size() > 2) {
			int canConvert = harvesters.size() - MIN_HARVESTER - 1;
			for (int i = 0; i < canConvert; i++) {
				reply(harvesters.get(i), "change group");
			}
		}
		setDebugString(harvesters.size() + "|" + explorers.size());
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

	public void sendMessage() {
		for (WarAgentPercept percept : getPerceptsEnemies()) {
			if (isEnemy(percept) && percept.getType().getCategory().equals(WarAgentCategory.Soldier)) {
				broadcastMessageToAll("I'm under attack",
						String.valueOf(percept.getAngle()),
						String.valueOf(percept.getDistance()));
			}
		}
		if (foodLocation != null) {
			broadcastMessageToAll("food location",
								String.valueOf(foodLocation.getDistance()),
								String.valueOf(foodLocation.getAngle()));
		}
	}

	public String createUnits(){
		int[] nbUnits = calculateUnits();
		if(nbUnits[1] < MAX_HEAVY) setNextAgentToCreate(WarAgentType.WarHeavy);
		if(nbUnits[4] < MAX_ROCKETLAUNCHER) setNextAgentToCreate(WarAgentType.WarRocketLauncher);
		if(nbUnits[0] < MAX_LIGHT) setNextAgentToCreate(WarAgentType.WarLight);
		if(nbUnits[3] < MAX_ENGINEER) setNextAgentToCreate(WarAgentType.WarEngineer);
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
		if (getHealth() > getMaxHealth() * 0.8) {
			ctask = "createUnits";
		} else if (getNbElementsInBag() >= 0) {
			ctask = "regenerate";
		} else {
			ctask = "nothingToDo";
		}
	}

	private void setUpTeamName() {
		if (Utils.teamName == null) {
			Utils.teamName = getTeamName();
		}
	}

	private String emergency(){
		List<WarAgentPercept> percepts = getPerceptsEnemies();
		percepts.removeIf((e) -> e.getType() == WarAgentType.WarExplorer || e.getType() == WarAgentType.WarEngineer);
		if(percepts.size() >= 1){
			if(getHealth() >= getMaxHealth() * 0.6 && getHealth() < getMaxHealth() * 0.8){
				setNextAgentToCreate(WarAgentType.WarHeavy);
				return create();
			}
		}

		return null;
		
	}

	public String reflexes() {
		setUpTeamName();
		setDebugString("nourriture : " + getNbElementsInBag());
		handleMessages();
		sendMessage();
		return emergency();
	}

	@Override
	public String action() {
		String action = reflexes();
		if (action != null) { 
			return action; 
		}
		decide();
		Class c = this.getClass();
		Method method;
		action = idle(); // default Action else java not happy
		try {
			method = c.getMethod(ctask);
			action = (String) method.invoke(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return action;
	}

}
