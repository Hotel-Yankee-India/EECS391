import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

public class ResourceCollectionAgent extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -474102775623154744L;

	public ResourceCollectionAgent(int playernum) {
		super(playernum);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Map<Integer, Action> initialStep(StateView newstate, HistoryView statehistory) {
		// No specialization required at initialStep
		return middleStep(newstate, statehistory);
	}

	@Override
	public void loadPlayerData(InputStream arg0) {
		// Do not load player data for now

	}

	@Override
	public Map<Integer, Action> middleStep(StateView newstate, HistoryView stateHistory) {
		
		// This is field contains the actions each unit will perform
		Map<Integer, Action> actions = new HashMap<Integer, Action>();
		
		//a list of all units of the player in the new state
		List<Integer> myUnitIds = newstate.getUnitIds(playernum);
		
		//a list of unitids for peasants and a list for unitids for townhalls
		List<Integer> peasantIds = new ArrayList<Integer>();
		List<Integer> townhallIds = new ArrayList<Integer>();
		
		for(Integer unitID :  myUnitIds) {
			//Extracting information about the unit with the id of unitID
			UnitView unit = newstate.getUnit(unitID);
			
			//This field contains the unit type name of the current unit
			String unitTypeName = unit.getTemplateView().getName();
			
			if(unitTypeName.equals("TownHall"))
				townhallIds.add(unitID);
			else if(unitTypeName.equals("Peasant"))
				peasantIds.add(unitID);
			else
				System.out.println("Error: Unexpected unit type:" + unitTypeName);
				
		}
		
		//Information about the current amount of each type of resource
		int currentGold = newstate.getResourceAmount(playernum, ResourceType.GOLD);
		int currentWood = newstate.getResourceAmount(playernum, ResourceType.WOOD);
		
		//Information about the resource nodes of each type
		List<Integer> goldMines = newstate.getResourceNodeIds(Type.GOLD_MINE);
		List<Integer> trees = newstate.getResourceNodeIds(Type.TREE);
		
		
		//
		for(Integer peasantID: peasantIds) {
			Action action = null;
			
			if(newstate.getUnit(peasantID).getCargoAmount() > 0) {
				action = new TargetedAction(peasantID, ActionType.COMPOUNDDEPOSIT, townhallIds.get(0));
			}
			else {
				if(currentGold < currentWood) {
					action = new TargetedAction(peasantID, ActionType.COMPOUNDGATHER, goldMines.get(0));
				}
				else {
					action = new TargetedAction(peasantID, ActionType.COMPOUNDGATHER, trees.get(0));
				}
			}
			
			actions.put(peasantID, action);
		}
		
		
		return actions;
	}

	@Override
	public void savePlayerData(OutputStream arg0) {
		// Do not save player data for now

	}

	@Override
	public void terminalStep(StateView arg0, HistoryView arg1) {
		// TODO Auto-generated method stub

	}

}
