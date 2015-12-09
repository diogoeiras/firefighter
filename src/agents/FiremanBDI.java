package agents;

import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.runtime.impl.PlanFailureException;
import jadex.extension.envsupport.environment.ISpaceObject;
import jadex.extension.envsupport.environment.space2d.Grid2D;
import jadex.extension.envsupport.math.Vector1Int;
import jadex.extension.envsupport.math.Vector2Double;
import jadex.extension.envsupport.math.Vector2Int;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;

import goals.FiremanGoal;

import java.util.*;

@Agent
public class FiremanBDI {
    @Agent
    protected BDIAgent fireman;

    private static final int VISION_CAMPS = 5;
    private static final int EXTINGUISH_CAMPS = 1;

    @Belief
    protected Grid2D space = (Grid2D) fireman.getParentAccess().getExtension("2dspace").get();

    @Belief
    protected Queue < ISpaceObject > nearObjects, nearObjectsToExtinguish;

    @Belief(updaterate = 350)
    protected long currentTime = System.currentTimeMillis();

    @Belief
    protected ISpaceObject myself = space.getAvatar(fireman.getComponentDescription(), fireman.getModel().getFullName());

    @AgentBody
    public void body() {

        System.out.println("Vision Sight: " + VISION_CAMPS);
        System.out.println("Vision Extinguish: " + EXTINGUISH_CAMPS);
        System.out.println("\n\n");

        nearObjects = Collections.asLifoQueue(new ArrayDeque<ISpaceObject>());
        nearObjectsToExtinguish = new ArrayDeque<ISpaceObject>();

        Random r = new Random();

        int spaceHeight = space.getAreaSize().getYAsInteger(),
                spaceWidth = space.getAreaSize().getXAsInteger(), xPosition = r.nextInt(spaceWidth),
                yPosition = r.nextInt(spaceHeight);

        myself.setProperty("position", new Vector2Int(xPosition, yPosition));

        FiremanGoal Goal = new FiremanGoal(null);
        Goal.setCurrentPosition(new Vector2Int(xPosition, yPosition));

        // initialize array with ISpaceObjects near current position
        getNearObjects(Goal.getCurrentPosition(),VISION_CAMPS, false);
        getNearObjects(Goal.getCurrentPosition(),EXTINGUISH_CAMPS, true);

        FiremanGoal goal = (FiremanGoal) fireman.dispatchTopLevelGoal(Goal).get();

    }

    public void getNearObjects(Vector2Int currPosition, int SIGHT, boolean toExtinguish) {

        Object[] nearObj = space.getNearObjects(currPosition, new Vector1Int(SIGHT), "fire").toArray();
        for (int i = 0; i < nearObj.length; i++) {
            if (!toExtinguish) {
                nearObjects.add((ISpaceObject) nearObj[i]);
            } else {
                nearObjectsToExtinguish.add((ISpaceObject) nearObj[i]);
            }
        }
    }

    public static Vector2Int returnDirection(Grid2D space, Vector2Int curr, Vector2Int Des){
        Vector2Int direction = new Vector2Int();

        if (Des.getXAsInteger() < curr.getXAsInteger() && space.getDistance(Des, curr).getAsInteger() >= 1) {
            direction.setX(new Vector1Int(-1));
        } else if (Des.getXAsInteger() > curr.getXAsInteger() && space.getDistance(Des, curr).getAsInteger() >= 1) {
            direction.setX(new Vector1Int(1));
        } else {
            direction.setX(new Vector1Int(0));
        }

        // Y position
        if (Des.getYAsInteger() < curr.getYAsInteger() && space.getDistance(Des, curr).getAsInteger() >= 1) {
            direction.setY(new Vector1Int(-1));
        } else if (Des.getYAsInteger() > curr.getYAsInteger() && space.getDistance(Des, curr).getAsInteger() >= 1) {
            direction.setY(new Vector1Int(1));
        } else {
            direction.setY(new Vector1Int(0));
        }

        return direction;
    }

    public boolean canExtinguish(Vector2Int currentPosition, Vector2Double positionToExtinguish){

        int spaceWidth = space.getAreaSize().getXAsInteger(),
                spaceHeight = space.getAreaSize().getYAsInteger();

        if (currentPosition.getXAsInteger() == spaceWidth-1 && positionToExtinguish.getXAsInteger() == 0){
            return false;
        } else if (currentPosition.getXAsInteger() == 0 && positionToExtinguish.getXAsInteger() == spaceWidth-1){
            return false;
        } else if (currentPosition.getYAsInteger() == spaceHeight-1 && positionToExtinguish.getYAsInteger() == 0){
            return false;
        } else if (currentPosition.getYAsInteger() == 0 && positionToExtinguish.getYAsInteger() == spaceHeight-1) {
            return false;
        } else if ( Math.abs(currentPosition.getXAsInteger() - positionToExtinguish.getXAsInteger()) > 1 ||
                Math.abs(currentPosition.getYAsInteger() - positionToExtinguish.getYAsInteger()) > 1){
            return false;
        }
        else {
            return true;
        }
    }

    public void putDownFireCell(ISpaceObject cell){
        space.destroySpaceObject(cell.getId());

        Vector2Double doubleVector = (Vector2Double) cell.getProperty("position");
        Map properties = new HashMap();
        properties.put("position", new Vector2Int(doubleVector.getXAsInteger(), doubleVector.getYAsInteger()));
        properties.put("type", 1);
        space.createSpaceObject("wetTerrain", properties, null);

    }

    public void removeRepeatedCells(Vector2Int current){

        Queue<ISpaceObject> newNear = Collections.asLifoQueue(new ArrayDeque<ISpaceObject>());

        nearObjects = getQueueRepeatedFree(nearObjects, current, false);

        Queue<ISpaceObject> newVisionSight = new ArrayDeque<ISpaceObject>();

        nearObjectsToExtinguish = getQueueRepeatedFree(nearObjectsToExtinguish, current, true);
    }

    public boolean wasAlreadyExtinguished(ISpaceObject cell){
        if (cell.getType() == "fire"){
            return true;
        } else {
            return false;
        }
    }

    public Queue<ISpaceObject> getQueueRepeatedFree(Queue<ISpaceObject> oldQueue,Vector2Int currentPosition, boolean toExtinguished){

        Queue<ISpaceObject> newQueue = new ArrayDeque<ISpaceObject>();
        Queue<ISpaceObject> newVisionSight = new ArrayDeque<ISpaceObject>();

        String[][] grid = new String[space.getAreaSize().getXAsInteger()][space.getAreaSize().getYAsInteger()];

        while(oldQueue.size() != 0){
            Vector2Double current = (Vector2Double) oldQueue.peek().getProperty("position");
            if(grid[current.getXAsInteger()][current.getYAsInteger()] != "X"){
                if (toExtinguished && canExtinguish(currentPosition,current)){
                    newQueue.add(oldQueue.remove());
                    grid[current.getXAsInteger()][current.getYAsInteger()] = "X";
                } else if (!toExtinguished && canExtinguish(currentPosition,current)){
                    if (wasAlreadyExtinguished(oldQueue.peek())) {
                        newVisionSight.add(oldQueue.remove());
                    } else {
                        oldQueue.remove();
                    }
                    grid[current.getXAsInteger()][current.getYAsInteger()] = "X";
                } else {
                    oldQueue.remove();
                }
            } else {
                oldQueue.remove();
            }
        }
        if (toExtinguished){
            Queue<ISpaceObject> toReturn = Collections.asLifoQueue(new ArrayDeque<ISpaceObject>());
            Object[] temp = newQueue.toArray();
            for( int i = temp.length - 1; i >= 0; i--){
                toReturn.add((ISpaceObject) temp[i]);
            }
            return toReturn;
        } else
            return newVisionSight;
    }

    @Plan(trigger = @Trigger(goals = FiremanGoal.class))
    public class MovingPlan {

        @PlanBody
        protected void changePosition(FiremanGoal goal) {

            Vector2Int direction = new Vector2Int(0,0);

            if (goal.getDesiredPosition() != null &&
                    (goal.getDesiredPosition().getXAsInteger() == goal.getCurrentPosition().getXAsInteger()
                        && goal.getDesiredPosition().getYAsInteger() == goal.getCurrentPosition().getYAsInteger())) {
                goal.setDesiredPosition(null);
                direction = new Vector2Int(0,0);
            }
            else if (nearObjectsToExtinguish.size() > 0){

                if(canExtinguish(goal.getCurrentPosition(),(Vector2Double) nearObjectsToExtinguish.peek().getProperty("position"))){
                    putDownFireCell(nearObjectsToExtinguish.peek());
                    System.out.println("[" + currentTime + "] Eliminating (" + nearObjectsToExtinguish.peek().getProperty("position") + ") from ("
                    + goal.getCurrentPosition() + ")");
                    Vector2Double positionToExtinguish = (Vector2Double) nearObjectsToExtinguish.peek().getProperty("position");
                    Vector2Int pos = new Vector2Int(positionToExtinguish.getXAsInteger(),positionToExtinguish.getYAsInteger());
                }
                direction = new Vector2Int(0,0);
                nearObjectsToExtinguish.remove();
            } else if (nearObjects.size() > 0){

                Vector2Double pos = (Vector2Double) nearObjects.peek().getProperty("position");
                //goal.setDesiredPosition(new Vector2Int(pos.getXAsInteger(),pos.getYAsInteger()));
                nearObjects.remove();
                direction = returnDirection(space, goal.getCurrentPosition(),new Vector2Int(pos.getXAsInteger(),pos.getYAsInteger()));

            } else {

                if (goal.getDesiredPosition() != null){
                    direction = returnDirection(space, goal.getCurrentPosition(),goal.getDesiredPosition());
                } else {
                    ISpaceObject[] fire = space.getSpaceObjectsByType("fire");

                    if (fire.length > 0) {
                        Vector2Double pos = (Vector2Double) fire[0].getProperty("position");
                        goal.setDesiredPosition(new Vector2Int(pos.getXAsInteger(), pos.getYAsInteger()));
                        direction = returnDirection(space, goal.getCurrentPosition(),goal.getDesiredPosition());
                    } else {
                        goal.changeNoMoreFireCells();
                    }
                }
            }

            Vector2Int actualPosition = goal.getCurrentPosition();
            goal.setCurrentPosition(new Vector2Int(actualPosition.getXAsInteger() + direction.getXAsInteger(),
                    actualPosition.getYAsInteger() + direction.getYAsInteger()));

            myself.setProperty("position",goal.getCurrentPosition());

            getNearObjects(goal.getCurrentPosition(),VISION_CAMPS,false);

            getNearObjects(goal.getCurrentPosition(),EXTINGUISH_CAMPS,true);

            System.out.println("[" + currentTime + "] NearObjects size: " + nearObjects.size());
            System.out.println("[" + currentTime + "] NearObjectsToExtinguish: " + nearObjectsToExtinguish.size());
            System.out.println("[" + currentTime + "] Current position: (" + goal.getCurrentPosition() + ")");
            System.out.println("[" + currentTime + "] Desired Position: (" + goal.getDesiredPosition() + ")");
            System.out.println("[" + currentTime + "] Direction: (" + direction + ")");
            System.out.println("______________");


            removeRepeatedCells(goal.getCurrentPosition());
            throw new PlanFailureException();

        }

        @PlanPassed
        public void passed() {
            System.out.println("[" + currentTime + "] ~~~Reached destination~~~");
        }

    }


}