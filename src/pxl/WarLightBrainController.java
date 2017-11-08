package pxl;

import java.util.List;

import edu.warbot.agents.enums.WarAgentType;
import edu.warbot.agents.percepts.WarAgentPercept;
import edu.warbot.brains.brains.WarLightBrain;

public abstract class WarLightBrainController extends  WarLightBrain {


    public WarLightBrainController() {
        super();
    }

    @Override
    public String action() {

    	List<WarAgentPercept> wps = getPerceptsEnemies();
    	if(wps.isEmpty()){
    		setDebugString("Je cherche la merde");
    		setRandomHeading(20);
    	}else{
	        for (WarAgentPercept wp : wps) {
	       
	        	if (!wp.getType().equals(WarAgentType.WarBase) || !wp.getType().equals(WarAgentType.WarHeavy) || !wp.getType().equals(WarAgentType.WarFood)) {
	
	        		setHeading(wp.getAngle());
	                this.setDebugString("Attaque");
	                if (isReloaded())
	                    return ACTION_FIRE;
	                else if (isReloading())
	                    return ACTION_IDLE;
	                else
	                    return ACTION_RELOAD;
	            }else{
	            	setDebugString("je fui !");
	            	setHeading(wp.getAngle() + 180);
	            	return ACTION_MOVE;
	            }
	        }
    	}

        if (isBlocked())
            setRandomHeading();

        return ACTION_MOVE;
    }
    	

}