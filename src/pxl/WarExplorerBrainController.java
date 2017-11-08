package pxl;

import java.util.ArrayList;
import java.util.List;

import edu.warbot.agents.agents.WarExplorer;
import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.brains.WarExplorerBrain;
import edu.warbot.communications.WarMessage;

public abstract class WarExplorerBrainController extends WarExplorerBrain {

    public WarExplorerBrainController() {
        super();

    }

    @Override
    public String action() {

    	if(isBagFull()){
    		setDebugString("Bag full, return base");
    		List<WarAgentPercept> percepts = getPerceptsAlliesByType(WarAgentType.WarBase);
    		
    		if(percepts == null || percepts.size() == 0){
    			List<WarMessage> messages = getMessages();
    		}
    	}
    	
		setDebugString("Vos mamans les catins des bois");

        if (isBlocked())
            setRandomHeading();
        return WarExplorer.ACTION_MOVE;
    }
    

}
