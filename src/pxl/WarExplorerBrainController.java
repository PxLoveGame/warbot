package pxl;

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

	boolean shouldEmptyBag = false;
	
    public WarExplorerBrainController() {
        super();

    }

    @Override
    public String action() {
    	if(isBagEmpty()){
    		shouldEmptyBag = false;
    	}
    	
    	if(isBagFull() || shouldEmptyBag){
    		shouldEmptyBag = true;
   
    		setDebugString("Bag full, return base");
    		List<WarAgentPercept> percepts = getPerceptsAlliesByType(WarAgentType.WarBase);
    		
    		if(percepts == null || percepts.size() == 0){
    			List<WarMessage> messages = getMessages();
    			
    			for(WarMessage message : messages){
    				if(message.getSenderType() == WarAgentType.WarBase){
    					setHeading(message.getAngle());
    				}
    				broadcastMessageToAgentType(WarAgentType.WarBase, "Where is the base ?", "");
    			}
    		}else{
    			WarAgentPercept base = percepts.get(0);
    			
    			if(base.getDistance() > MAX_DISTANCE_GIVE){
    				setHeading(base.getAngle());
    				return move();
    			}else{
    				setIdNextAgentToGive(base.getID());
    				return give();
    			}
    		}
    	}else{
    		setDebugString("Cherche food ma gueule");
    		List<WarAgentPercept> percepts_ressource = getPercepts();
    		
    		if(percepts_ressource == null || percepts_ressource.size() == 0){
    			setRandomHeading(20);
    			broadcastMessageToAgentType(WarAgentType.WarExplorer, "No food here", "");
    		}else{
    			for(WarAgentPercept ressource : percepts_ressource){
    				if(ressource.getType().equals(WarAgentType.WarFood)){
    					setHeading(ressource.getAngle());
    					if(ressource.getDistance() < WarFood.MAX_DISTANCE_TAKE){
    			    		setDebugString("Nourriture à proximité");
    						broadcastMessageToAgentType(WarAgentType.WarExplorer, "food around here", "");
    						return take();
    					}
    					break;
    				}
    			}
    		}
    	}
    	
		//setDebugString("Vos mamans les catins des bois");

        if (isBlocked())
            setRandomHeading();
        return WarExplorer.ACTION_MOVE;
    }
    

}
