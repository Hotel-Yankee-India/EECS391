import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.util.Direction;

public class AltCombatAgent extends Agent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 765592400757465965L;
	private int enemyPlayerNum = 1;
	
	private Integer bait = -1;
	

	public AltCombatAgent(int playernum, String[] otherargs) {
		super(playernum);
		
		if(otherargs.length > 0) {
			enemyPlayerNum = new Integer(otherargs[0]);
		}
		
		System.out.println("Constructed combat agent");
	}

	@Override
	public Map<Integer, Action> initialStep(StateView newstate, HistoryView stateHistory) {
		
		//a list that stores all player's units
		List<Integer> myUnitIds = new ArrayList<Integer>();
		
		//a list that stores all player's footman
		List<Integer> myFootmanIds = new ArrayList<Integer>();
		
		myUnitIds = newstate.getUnitIds(playernum);
		
		//add all footman to their list
		for(Integer unitID : myUnitIds) {
			String unitTypeName = newstate.getUnit(unitID).getTemplateView().getName();
			if(unitTypeName.equals("Footman"))
				myFootmanIds.add(unitID);
		}
		
		//let the first footman be the bait
		bait = myFootmanIds.get(0);
		
		return middleStep(newstate, stateHistory);
	}

	@Override
	public void loadPlayerData(InputStream arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<Integer, Action> middleStep(StateView newstate, HistoryView stateHistory) {
		
		//A list of actions that each units will perform in this turn
		Map<Integer, Action> actions = new HashMap<Integer, Action>();
		
		//An int value that stores the sight range of a footman;
		int sightRangeF = 0;
		
		//An int value that stores the sight range of a tower;
		int sightRangeT = 0;
		
		//An int value that stores the range of a tower;
		int rangeT = 0;
		
		//An int value that stores the range of a footman;
		int rangeF = 0;
		
		//An int value that stores the range of an archer;
		int rangeA = 0;
		
		//An int value that stores the range of a ballista;
		int rangeB = 0;
		
		//A list of all player's units
		List<Integer> myUnitIds = newstate.getUnitIds(playernum);
		
		//A list of all player's footman
		List<Integer> myFootmanIds = new ArrayList<Integer>();
		
		//A list of all player's archers
		List<Integer> myArcherIds = new ArrayList<Integer>();
		
		//A list of all player's ballista
		List<Integer> myBallistaIds = new ArrayList<Integer>();
		
		//A list of all enemy's units
		List<Integer> enemyUnitIds = newstate.getUnitIds(enemyPlayerNum);
		
		//A list of all enemy's footman
		List<Integer> enemyFootmanIds = new ArrayList<Integer>();
		
		//A list of all enemy's towers
		List<Integer> enemyTowerIds = new ArrayList<Integer>();
		
		//Categorize all player's units and put them into their respective list
		for(Integer unitID : myUnitIds) {
			String unitTypeName = newstate.getUnit(unitID).getTemplateView().getName();
			if(unitTypeName.equals("Footman"))
				myFootmanIds.add(unitID);
			else if(unitTypeName.equals("Archer"))
				myArcherIds.add(unitID);
			else if(unitTypeName.equals("Ballista"))
				myBallistaIds.add(unitID);
			else
				System.out.println("Error: Unexpected unit type: " + unitTypeName);
				
		}
		
		//Categorize all enemy's units and put them into their respective list
		for(Integer unitID : enemyUnitIds) {
			String unitTypeName = newstate.getUnit(unitID).getTemplateView().getName();
			if(unitTypeName.equals("Footman"))
				enemyFootmanIds.add(unitID);
			else if(unitTypeName.equals("ScoutTower"))
				enemyTowerIds.add(unitID);
			else
				System.out.println("Error: Unexpected unit type: " + unitTypeName);
		}
		
		
		//get the sight range of each unit type
		sightRangeF = newstate.getUnit(enemyFootmanIds.get(0)).getTemplateView().getSightRange();
		sightRangeT = newstate.getUnit(enemyTowerIds.get(0)).getTemplateView().getSightRange();
		rangeF = newstate.getUnit(enemyFootmanIds.get(0)).getTemplateView().getRange();
		rangeT = newstate.getUnit(enemyTowerIds.get(0)).getTemplateView().getRange();
		rangeA = newstate.getUnit(myArcherIds.get(0)).getTemplateView().getRange();
		rangeB = newstate.getUnit(myBallistaIds.get(0)).getTemplateView().getRange();
		
		
		
		//A flag that keeps track of whether the bait is in sight of any enemy footman
		boolean inSight = false;
		
		//A flag that keeps track of whether any enemy units are in attack range of any ballista or archer
		boolean inRange = false;
		
		//check if the bait is in sight of any enemy footman
		for(Integer unitID : enemyFootmanIds) {
			if(dist(unitID, bait, newstate) < sightRangeF)
				inSight = true;
		}
		
		if(!inSight && dist(bait, enemyTowerIds.get(0), newstate) > rangeT)
			//if the bait has not entered sight of enemy footmen then keep going to northeast
			actions.put(bait, Action.createPrimitiveMove(bait, Direction.NORTHEAST));
		else
			//else keep moving to southwest
			actions.put(bait, Action.createPrimitiveMove(bait, Direction.SOUTHWEST));
		
		//Actions for ballista
		for(Integer myUnitID : myBallistaIds) {
			
			if(enemyFootmanIds.size() > 0) {
				//if there still are enemy footmen alive, stay still and attack enemy footmen in range
				for(Integer enemyUnitID : enemyFootmanIds) {
					if(dist(myUnitID, enemyUnitID, newstate) < rangeB) {
						inRange = true;
						actions.put(myUnitID, Action.createPrimitiveAttack(myUnitID, enemyUnitID));
					}
				}
			}else {//else move in range to attack remaining enemy tower(s)
				if(enemyTowerIds.size() > 0) {
					actions.put(myUnitID, Action.createCompoundAttack(myUnitID, enemyTowerIds.get(0)));
				}	
			}
		}
		
		//Actions for archers
		for(Integer myUnitID : myArcherIds) {
			
			if(enemyFootmanIds.size() > 0) {
				//if there still are enemy footmen alive, stay still and attack enemy footmen in range
				for(Integer enemyUnitID : enemyFootmanIds) {
					if(dist(myUnitID, enemyUnitID, newstate) < rangeA) {
						inRange = true;
						actions.put(myUnitID, Action.createPrimitiveAttack(myUnitID, enemyUnitID));
					}
				}
			}else {//else move in range to attack remaining enemy tower(s)
				if(enemyTowerIds.size() > 0) {
					actions.put(myUnitID, Action.createCompoundAttack(myUnitID, enemyTowerIds.get(0)));
				}	
			}
		}

		//Actions for footmen
		for(Integer myUnitID : myFootmanIds) {
			if(enemyFootmanIds.size() > 0 && inRange) {
				//if there are any enemy footman alive and they are in range of friendly archer or ballista
				//then move in position to attack the closest target
				Integer closestTarget = enemyFootmanIds.get(0);
				double minDist = dist(closestTarget, myUnitID, newstate);
				for(Integer enemyUnitID : enemyFootmanIds) {
					if(dist(myUnitID, enemyUnitID, newstate) < minDist) {
						minDist = dist(myUnitID, enemyUnitID, newstate);
						closestTarget = enemyUnitID;
					}
				}
				actions.put(myUnitID, Action.createCompoundAttack(myUnitID, closestTarget));
			} else {
				//else perform compound attack on remaining enemy tower
				if(enemyTowerIds.size() > 0) {
					actions.put(myUnitID, Action.createCompoundAttack(myUnitID, enemyTowerIds.get(0)));
				}
			}
		}
		
		return actions;
	}
	
	//This is a Private helper method that calculates the distance between 2 units
	private double dist(Integer unit1, Integer unit2, StateView newstate) {
		//Creating two arrays of size 2 to store the position of the 2 units
		int[] pos1 = new int[2];
		int[] pos2 = new int[2];
		//Extracting the positional data
		pos1[0] = newstate.getUnit(unit1).getXPosition();
		pos1[1] = newstate.getUnit(unit1).getYPosition();
		pos2[0] = newstate.getUnit(unit2).getXPosition();
		pos2[1] = newstate.getUnit(unit2).getYPosition();
		//Calculating the distance
		double dx = Math.abs(pos1[0] - pos2[0]);
		double dy = Math.abs(pos1[1] - pos2[1]);
		double distance = Math.sqrt(Math.pow(dx, 2.0) + Math.pow(dy, 2.0));
		return distance;
	}

	@Override
	public void savePlayerData(OutputStream arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void terminalStep(StateView arg0, HistoryView arg1) {
		// TODO Auto-generated method stub

	}

}
