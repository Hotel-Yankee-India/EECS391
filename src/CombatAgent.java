
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.util.Direction;

public class CombatAgent extends Agent {

	private int enemyPlayerNum = 1;
	
	public CombatAgent(int playernum, String[] otherargs) {
		super(playernum);

        if(otherargs.length > 0)
        {
                enemyPlayerNum = new Integer(otherargs[0]);
        }

        System.out.println("Constructed MyCombatAgent");
	}

	@Override
	public Map<Integer, Action> initialStep(StateView newstate,
            HistoryView statehistory) {
	    // This stores the action that each unit will perform
	    // if there are no changes to the current actions then this
	    // map will be empty
	    Map<Integer, Action> actions = new HashMap<Integer, Action>();
	
	    // This is a list of all of your units
	    // Refer to the resource agent example for ways of
	    // differentiating between different unit types based on
	    // the list of IDs
	    List<Integer> myUnitIDs = newstate.getUnitIds(playernum);
	
	    // This is a list of enemy units
	    List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);
	
	    if(enemyUnitIDs.size() == 0)
	    {
	            // Nothing to do because there is no one left to attack
	            return actions;
	    }
	    
	    // Set initial action to try to attack the nearest enemy
	    for(Integer myUnitID : myUnitIDs)
	    {
	    		// Finding closest enemy
	    		Integer closestEnemyID = enemyUnitIDs.get(0);
	    		int minDistance = newstate.getUnit(closestEnemyID).getXPosition() - newstate.getUnit(myUnitID).getXPosition() + newstate.getUnit(closestEnemyID).getYPosition() - newstate.getUnit(myUnitID).getYPosition();
	    		for (Integer enemyUnitID : enemyUnitIDs) {
	    			int distance = newstate.getUnit(enemyUnitID).getXPosition() - newstate.getUnit(myUnitID).getXPosition() + newstate.getUnit(enemyUnitID).getYPosition() - newstate.getUnit(myUnitID).getYPosition();
	    			if (distance < minDistance) {
	    				minDistance = distance;
	    				closestEnemyID = enemyUnitID;
	    			}
	    		}
	    		
	    		System.out.println("Targeting enemy " + closestEnemyID);
	        actions.put(myUnitID, Action.createPrimitiveAttack(myUnitID, closestEnemyID));
	    }

	    return actions;
	}

	@Override
	public void loadPlayerData(InputStream arg0) {
		// TODO Auto-generated method stub

	}

	public Map<Integer, Action> middleStep(StateView newstate,
            HistoryView statehistory) {
	    // This stores the action that each unit will perform
	    // if there are no changes to the current actions then this
	    // map will be empty
	    Map<Integer, Action> actions = new HashMap<Integer, Action>();
	
	    // This is a list of enemy units
	    List<Integer> myUnitIDs = newstate.getUnitIds(playernum);
		
	    // This is a list of enemy units
	    List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);
	
	    if(enemyUnitIDs.size() == 0)
	    {
	            // Nothing to do because there is no one left to attack
	            return actions;
	    }
	
	    int currentStep = newstate.getTurnNumber();
	
	    // go through the action history
	    for(ActionResult feedback : statehistory.getCommandFeedback(playernum, currentStep-1).values())
	    {
		    	// if the previous action is no longer in progress (either due to failure or completion)
		    	// then add a new action for this unit
	    		System.out.println(feedback.getFeedback().toString() + " " + feedback.getAction().getUnitId());
	    		int unitID = feedback.getAction().getUnitId();
	    		if (myUnitIDs.contains(unitID)) { //If unit still is alive
	    			
	    			//Finds closest enemy
	    			Integer closestEnemyID = enemyUnitIDs.get(0);
		    		int minDistance = Math.abs(newstate.getUnit(closestEnemyID).getXPosition() - newstate.getUnit(unitID).getXPosition() + newstate.getUnit(closestEnemyID).getYPosition() - newstate.getUnit(unitID).getYPosition());
		    		for (Integer enemyUnitID : enemyUnitIDs) {
		    			int distance = Math.abs(newstate.getUnit(enemyUnitID).getXPosition() - newstate.getUnit(unitID).getXPosition() + newstate.getUnit(enemyUnitID).getYPosition() - newstate.getUnit(unitID).getYPosition());
		    			if (distance < minDistance) {
		    				minDistance = distance;
		    				closestEnemyID = enemyUnitID;
		    			}
		    		}
		    		
		    		// If the last action did not fail or if the last actionwas a primitivemove that failed, try to attack the closest enemy
			    	if(feedback.getFeedback() != ActionFeedback.FAILED || (feedback.getAction().getType() == ActionType.PRIMITIVEMOVE && feedback.getFeedback() == ActionFeedback.FAILED))
			    	{
			    		// attack the first enemy unit in the list	
			    		actions.put(unitID, Action.createPrimitiveAttack(unitID, closestEnemyID));
			    	}
			    	
			    	// Otherwise move towards the closest enemy
			    	else if (feedback.getAction().getType() == ActionType.PRIMITIVEATTACK){
			    		int xDist = newstate.getUnit(closestEnemyID).getXPosition() - newstate.getUnit(unitID).getXPosition();
			    		int yDist = newstate.getUnit(closestEnemyID).getXPosition() - newstate.getUnit(unitID).getXPosition();
			    		if (xDist > 0 && yDist > 0) {
	    	    				actions.put(unitID, Action.createPrimitiveMove(unitID, Direction.NORTHEAST));
			    		}
			    		else if (xDist > 0 && yDist < 0) {
			    			actions.put(unitID, Action.createPrimitiveMove(unitID, Direction.SOUTHEAST));
			    		}
			    		else if (xDist < 0 && yDist > 0) {
			    			actions.put(unitID, Action.createPrimitiveMove(unitID, Direction.NORTHWEST));
			    		}
			    		else if (xDist < 0 && yDist < 0) {
			    			actions.put(unitID, Action.createPrimitiveMove(unitID, Direction.SOUTHWEST));
			    		}
			    		else if (xDist > 0) {
			    			actions.put(unitID, Action.createPrimitiveMove(unitID, Direction.EAST));
			    		}
			    		else if (xDist < 0) {
			    			actions.put(unitID, Action.createPrimitiveMove(unitID, Direction.WEST));
			    		}
			    		else if (yDist > 0) {
			    			actions.put(unitID, Action.createPrimitiveMove(unitID, Direction.NORTH));
			    		}
			    		else {
			    			actions.put(unitID, Action.createPrimitiveMove(unitID, Direction.SOUTH));
			    		}
			    	}
	    		}
	    }
	
	    return actions;
	}

	@Override
	public void savePlayerData(OutputStream arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void terminalStep(StateView arg0, HistoryView arg1) {
		// TODO Auto-generated method stub
		System.out.println("Finished the episode");

	}

}
