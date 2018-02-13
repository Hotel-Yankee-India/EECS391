package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class AstarAgent extends Agent {
    
    class MapLocation
    {
        public int x, y;
        public MapLocation cameFrom;
        public float cost;
        
        public MapLocation(int x, int y, MapLocation cameFrom, float cost)
        {
            this.x = x;
            this.y = y;
            this.cameFrom = cameFrom;
            this.cost = cost;
        }
    }
    
    Stack<MapLocation> path;
    int footmanID, townhallID, enemyFootmanID;
    MapLocation nextLoc;
    
    private long totalPlanTime = 0; // nsecs
    private long totalExecutionTime = 0; //nsecs
    
    public AstarAgent(int playernum)
    {
        super(playernum);
        
        System.out.println("Constructed AstarAgent");
    }
    
    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        // get the footman location
        List<Integer> unitIDs = newstate.getUnitIds(playernum);
        
        if(unitIDs.size() == 0)
        {
            System.err.println("No units found!");
            return null;
        }
        
        footmanID = unitIDs.get(0);
        
        // double check that this is a footman
        if(!newstate.getUnit(footmanID).getTemplateView().getName().equals("Footman"))
        {
            System.err.println("Footman unit not found");
            return null;
        }
        
        // find the enemy playernum
        Integer[] playerNums = newstate.getPlayerNumbers();
        int enemyPlayerNum = -1;
        for(Integer playerNum : playerNums)
        {
            if(playerNum != playernum) {
                enemyPlayerNum = playerNum;
                break;
            }
        }
        
        if(enemyPlayerNum == -1)
        {
            System.err.println("Failed to get enemy playernumber");
            return null;
        }
        
        // find the townhall ID
        List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);
        
        if(enemyUnitIDs.size() == 0)
        {
            System.err.println("Failed to find enemy units");
            return null;
        }
        
        townhallID = -1;
        enemyFootmanID = -1;
        for(Integer unitID : enemyUnitIDs)
        {
            Unit.UnitView tempUnit = newstate.getUnit(unitID);
            String unitType = tempUnit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall"))
            {
                townhallID = unitID;
            }
            else if(unitType.equals("footman"))
            {
                enemyFootmanID = unitID;
            }
            else
            {
                System.err.println("Unknown unit type");
            }
        }
        
        if(townhallID == -1) {
            System.err.println("Error: Couldn't find townhall");
            return null;
        }
        
        long startTime = System.nanoTime();
        path = findPath(newstate);
        totalPlanTime += System.nanoTime() - startTime;
        
        return middleStep(newstate, statehistory);
    }
    
    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        long startTime = System.nanoTime();
        long planTime = 0;
        
        Map<Integer, Action> actions = new HashMap<Integer, Action>();
        
        if(shouldReplanPath(newstate, statehistory, path)) {
            long planStartTime = System.nanoTime();
            path = findPath(newstate);
            planTime = System.nanoTime() - planStartTime;
            totalPlanTime += planTime;
        }
        
        Unit.UnitView footmanUnit = newstate.getUnit(footmanID);
        
        int footmanX = footmanUnit.getXPosition();
        int footmanY = footmanUnit.getYPosition();
        
        if(!path.empty() && (nextLoc == null || (footmanX == nextLoc.x && footmanY == nextLoc.y))) {
            
            // stat moving to the next step in the path
            nextLoc = path.pop();
            
            System.out.println("Moving to (" + nextLoc.x + ", " + nextLoc.y + ")");
        }
        
        if(nextLoc != null && (footmanX != nextLoc.x || footmanY != nextLoc.y))
        {
            int xDiff = nextLoc.x - footmanX;
            int yDiff = nextLoc.y - footmanY;
            
            // figure out the direction the footman needs to move in
            Direction nextDirection = getNextDirection(xDiff, yDiff);
            
            actions.put(footmanID, Action.createPrimitiveMove(footmanID, nextDirection));
        } else {
            Unit.UnitView townhallUnit = newstate.getUnit(townhallID);
            
            // if townhall was destroyed on the last turn
            if(townhallUnit == null) {
                terminalStep(newstate, statehistory);
                return actions;
            }
            
            if(Math.abs(footmanX - townhallUnit.getXPosition()) > 1 ||
               Math.abs(footmanY - townhallUnit.getYPosition()) > 1)
            {
                System.err.println("Invalid plan. Cannot attack townhall");
                totalExecutionTime += System.nanoTime() - startTime - planTime;
                return actions;
            }
            else {
                System.out.println("Attacking TownHall");
                // if no more movements in the planned path then attack
                actions.put(footmanID, Action.createPrimitiveAttack(footmanID, townhallID));
            }
        }
        
        totalExecutionTime += System.nanoTime() - startTime - planTime;
        return actions;
    }
    
    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {
        System.out.println("Total turns: " + newstate.getTurnNumber());
        System.out.println("Total planning time: " + totalPlanTime/1e9);
        System.out.println("Total execution time: " + totalExecutionTime/1e9);
        System.out.println("Total time: " + (totalExecutionTime + totalPlanTime)/1e9);
    }
    
    @Override
    public void savePlayerData(OutputStream os) {
        
    }
    
    @Override
    public void loadPlayerData(InputStream is) {
        
    }
    
    /**
     * You will implement this method.
     *
     * This method should return true when the path needs to be replanned
     * and false otherwise. This will be necessary on the dynamic map where the
     * footman will move to block your unit.
     *
     * @param state
     * @param history
     * @param currentPath
     * @return
     */
    private boolean shouldReplanPath(State.StateView state, History.HistoryView history, Stack<MapLocation> currentPath)
    {
    		/* TODO: Write this method
    		 * Check if the path is blocked every step or check if the next spot is blocked?
    		 * Does enemy attack if you get too close?
    		 */
    	
    	
    		//checking if the next step is blocked
    		//setting up local fields
    		UnitView enemy = state.getUnit(enemyFootmanID);
    		MapLocation next;
    		if (!path.isEmpty()) {
    			next = path.peek();
    		
    			if(next.x == enemy.getXPosition() && next.y == enemy.getYPosition()) 
    				return true;
    			
    		}
    		
        return false;
    }
    
    /**
     * This method is implemented for you. You should look at it to see examples of
     * how to find units and resources in Sepia.
     *
     * @param state
     * @return
     */
    private Stack<MapLocation> findPath(State.StateView state)
    {
        Unit.UnitView townhallUnit = state.getUnit(townhallID);
        Unit.UnitView footmanUnit = state.getUnit(footmanID);
        
        MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(), footmanUnit.getYPosition(), null, 0);
        
        MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(), townhallUnit.getYPosition(), null, 0);
        
        MapLocation footmanLoc = null;
        if(enemyFootmanID != -1) {
            Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
            footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0);
        }
        
        // get resource locations
        List<Integer> resourceIDs = state.getAllResourceIds();
        Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
        for(Integer resourceID : resourceIDs)
        {
            ResourceNode.ResourceView resource = state.getResourceNode(resourceID);
            
            resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0));
        }
        
        return AstarSearch(startLoc, goalLoc, state.getXExtent(), state.getYExtent(), footmanLoc, resourceLocations);
    }
    /**
     * This is the method you will implement for the assignment. Your implementation
     * will use the A* algorithm to compute the optimum path from the start position to
     * a position adjacent to the goal position.
     *
     * You will return a Stack of positions with the top of the stack being the first space to move to
     * and the bottom of the stack being the last space to move to. If there is no path to the townhall
     * then return null from the method and the agent will print a message and do nothing.
     * The code to execute the plan is provided for you in the middleStep method.
     *
     * As an example consider the following simple map
     *
     * F - - - -
     * x x x - x
     * H - - - -
     *
     * F is the footman
     * H is the townhall
     * x's are occupied spaces
     *
     * xExtent would be 5 for this map with valid X coordinates in the range of [0, 4]
     * x=0 is the left most column and x=4 is the right most column
     *
     * yExtent would be 3 for this map with valid Y coordinates in the range of [0, 2]
     * y=0 is the top most row and y=2 is the bottom most row
     *
     * resourceLocations would be {(0,1), (1,1), (2,1), (4,1)}
     *
     * The path would be
     *
     * (1,0)
     * (2,0)
     * (3,1)
     * (2,2)
     * (1,2)
     *
     * Notice how the initial footman position and the townhall position are not included in the path stack
     *
     * @param start Starting position of the footman
     * @param goal MapLocation of the townhall
     * @param xExtent Width of the map
     * @param yExtent Height of the map
     * @param resourceLocations Set of positions occupied by resources
     * @return Stack of positions with top of stack being first move in plan
     */
    private Stack<MapLocation> AstarSearch(MapLocation start, MapLocation goal, int xExtent, int yExtent, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations)
    {
    	
    		/* This implementation uses a PriorityQueue to store states to explore
    		 * and orders them by cost such that the next state expanded will always be the one with the lowest cost.
    		 * 
    		 * Once the state being checked is the goal state, the locations, 
    		 * starting with the location before the goal location, are pushed onto the path Stack
    		 * using the cameFrom member of the MapLocations to trace back to the starting location.
    		 * 
    		 * TODO: infinite loop if no solution: if can't find a solution, print "No available path." call System.exit(0)
         * 
         * Can use loc.cameFrom to check path that a location came from, at beginning of path, loc.cameFrom = null
         * Need a check to prevent it from expanding a state it has already visited in that branch.
         * Then we can implement a solution for when there are no more states to check 
         */
    	
        MapLocationCostComparator mapLocCostComparator = new MapLocationCostComparator();
        
        // This PriorityQueue is sorted by the cost associated with the state, lower cost first
        PriorityQueue<MapLocation> statesToCheck = new PriorityQueue<MapLocation>(11, mapLocCostComparator);
        
        Stack<MapLocation> path = new Stack<MapLocation>();
        MapLocation locationBeingExpanded = start;
        locationBeingExpanded.cameFrom = null;
        
        
        // While the locationBeingExpanded is not the location of the TownHall
        while (locationBeingExpanded.x != goal.x || locationBeingExpanded.y != goal.y) {
            // Check every nearby possibility
            for (int x = -1; x <= 1; x++) {     //where x is horizontal change in position
                for (int y = -1; y <= 1; y++) { //where y is vertical change in position
                    int newX = locationBeingExpanded.x + x;
                    int newY = locationBeingExpanded.y + y;
                    if(newX >= 0 && newY >= 0 /*TODO: CHECK IF IS TOO GREAT, GET MAP SIZE? use xExtent and yExtent, passed in for us*/) //checks if is valid location
                    {
                        MapLocation locToCheck = new MapLocation(newX, newY, locationBeingExpanded, locationBeingExpanded.cost - heuristic(locationBeingExpanded.x, locationBeingExpanded.y, goal) + heuristic(newX, newY, goal) + 1);
                        
                        boolean isResourceSpot = false;
                        //checks if nearby locations are resource spots
                        for (MapLocation loc : resourceLocations) {
                            if (loc.x == locToCheck.x && loc.y == locToCheck.y) {
                                isResourceSpot = true;
                            }
                        }
                        if (!isResourceSpot ) {
                            statesToCheck.add(locToCheck);
                        }
                    }
                }
            }
            if(statesToCheck.contains(enemyFootmanLoc)) {
            		statesToCheck.remove(enemyFootmanLoc);
            }
            	
            locationBeingExpanded = statesToCheck.poll();
        }
        
        // Set current to the location before the goal
        locationBeingExpanded = locationBeingExpanded.cameFrom;
        
        // Push locations that are part of the path to the goal to the stack
        while (locationBeingExpanded.cameFrom != null) {
            path.push(locationBeingExpanded);
            locationBeingExpanded = locationBeingExpanded.cameFrom;
        }
        
        return path;
    }
    
    /* The heuristic function used by the A* search
     * The Chebyshev Distance is the heuristic
     */
    private int heuristic(int x, int y, MapLocation goal) {
        return Math.max(Math.abs(x - goal.x), Math.abs(y - goal.y));
    }
    
    /* A comparator used in the PriorityQueue of MapLocations
     * that lets the queue prioritize the location with the lowest cost
     */
    class MapLocationCostComparator implements Comparator<MapLocation>
    {
        
        @Override
        public int compare(MapLocation x, MapLocation y) {
            if (x.cost < y.cost) {
                return -1;
            }
            if (x.cost > y.cost) {
                return 1;
            }
            return 0;
        }
    }
    
    
    
    /**
     * Primitive actions take a direction (e.g. NORTH, NORTHEAST, etc)
     * This converts the difference between the current position and the
     * desired position to a direction.
     *
     * @param xDiff Integer equal to 1, 0 or -1
     * @param yDiff Integer equal to 1, 0 or -1
     * @return A Direction instance (e.g. SOUTHWEST) or null in the case of error
     */
    private Direction getNextDirection(int xDiff, int yDiff) {
        
        // figure out the direction the footman needs to move in
        if(xDiff == 1 && yDiff == 1)
        {
            return Direction.SOUTHEAST;
        }
        else if(xDiff == 1 && yDiff == 0)
        {
            return Direction.EAST;
        }
        else if(xDiff == 1 && yDiff == -1)
        {
            return Direction.NORTHEAST;
        }
        else if(xDiff == 0 && yDiff == 1)
        {
            return Direction.SOUTH;
        }
        else if(xDiff == 0 && yDiff == -1)
        {
            return Direction.NORTH;
        }
        else if(xDiff == -1 && yDiff == 1)
        {
            return Direction.SOUTHWEST;
        }
        else if(xDiff == -1 && yDiff == 0)
        {
            return Direction.WEST;
        }
        else if(xDiff == -1 && yDiff == -1)
        {
            return Direction.NORTHWEST;
        }
        
        System.err.println("Invalid path. Could not determine direction");
        return null;
    }
}

