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
public class FiremanBDI {@Agent
protected BDIAgent fireman;

    private static final int VISION_CAMPS = 5;
    private static final int EXTINGUISH_CAMPS = 1;

    @Belief
    protected Grid2D space = (Grid2D) fireman.getParentAccess().getExtension("2dspace").get();

    @Belief
    protected Queue < ISpaceObject > nearObjects, nearObjectsToExtinguish;

    @Belief
    protected Vector2Int previousPosition;

    @Belief(updaterate = 100)
    protected long currentTime = System.currentTimeMillis();

    @Belief
    protected ISpaceObject myself = space.getAvatar(fireman.getComponentDescription(), fireman.getModel().getFullName());

    @AgentBody
    public void body() {

        nearObjects = Collections.asLifoQueue(new ArrayDeque<ISpaceObject>());
        nearObjectsToExtinguish = new ArrayDeque<ISpaceObject>();
        previousPosition = null;

        Random r = new Random();

        int spaceHeight = space.getAreaSize().getYAsInteger(),
                spaceWidth = space.getAreaSize().getXAsInteger(), xPosition = r.nextInt(spaceWidth),
                yPosition = r.nextInt(spaceHeight);

        myself.setProperty("position", new Vector2Int(xPosition, yPosition));
        myself.setProperty("speed", 10);

        FiremanGoal Goal = new FiremanGoal(new Vector2Int(0, 0));
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

    public Vector2Int returnDirection(Vector2Int curr, Vector2Int Des){
        Vector2Int direction = new Vector2Int();

        if (Des.getXAsInteger() < curr.getXAsInteger() && space.getDistance(Des, curr).getAsInteger() > 1) {
            direction.setX(new Vector1Int(-1));
        } else if (Des.getXAsInteger() > curr.getXAsInteger() && space.getDistance(Des, curr).getAsInteger() > 1) {
            direction.setX(new Vector1Int(1));
        } else {
            direction.setX(new Vector1Int(0));
        }

        // Y position
        if (Des.getYAsInteger() < curr.getYAsInteger() && space.getDistance(Des, curr).getAsInteger() > 1) {
            direction.setY(new Vector1Int(-1));
        } else if (Des.getYAsInteger() > curr.getYAsInteger() && space.getDistance(Des, curr).getAsInteger() > 1) {
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
        } else {
            return true;
        }
    }

    @Plan(trigger = @Trigger(goals = FiremanGoal.class))
    public class MovingPlan {

        @PlanBody
        protected void changePosition(FiremanGoal goal) {
            Vector2Int currentPosition = goal.getCurrentPosition();
            Vector2Int direction = null;

            // Check if we can extinguish fire
            if (nearObjectsToExtinguish.size() > 0){
                Vector2Double positionToExtinguish = (Vector2Double)nearObjectsToExtinguish.peek().getProperty("position");
                if ( canExtinguish(goal.getCurrentPosition(),positionToExtinguish) ) {
                    ISpaceObject toEliminateFire = nearObjectsToExtinguish.remove();
                    space.destroySpaceObject(toEliminateFire.getId());

                    Vector2Double doubleVector = (Vector2Double) toEliminateFire.getProperty("position");
                    Map properties = new HashMap();
                    properties.put("position", new Vector2Int(doubleVector.getXAsInteger(), doubleVector.getYAsInteger()));
                    properties.put("type", 1);
                    space.createSpaceObject("wetTerrain", properties, null);
                } else {
                    nearObjectsToExtinguish.remove();
                }
            } else {

                if (nearObjects.size() > 0){

                    System.out.println(nearObjects.peek().getProperty("position") + "\n-------");
                    Vector2Double nObject = (Vector2Double) nearObjects.remove().getProperty("position");
                    direction = returnDirection(currentPosition,new Vector2Int(nObject.getXAsInteger(),nObject.getYAsInteger()));
                    if (nObject.getXAsInteger() + direction.getXAsInteger() == previousPosition.getXAsInteger() &&
                            nObject.getYAsInteger() + direction.getYAsInteger() == previousPosition.getYAsInteger()) {
                        direction = null;
                    }
                } else {
                    ISpaceObject[] fireObjects = space.getSpaceObjectsByType("fire");
                    if (fireObjects.length > 0){
                        for(int i = 0; i < fireObjects.length; i++) {
                            Vector2Double heliTip = (Vector2Double) fireObjects[i].getProperty("position");
                            direction = returnDirection(currentPosition, new Vector2Int(heliTip.getXAsInteger(), heliTip.getYAsInteger()));
                            if (previousPosition == null){
                                break;
                            }
                            else if (currentPosition.getXAsInteger()+direction.getXAsInteger() != previousPosition.getXAsInteger()
                                    && currentPosition.getYAsInteger()+ direction.getYAsInteger() !=  previousPosition.getYAsInteger()) {
                                break;
                            } else {
                                direction = null;
                            }
                        }
                    } else {
                        goal.changeNoMoreFireCells();
                    }
                }
            }

            if (direction != null) {
                previousPosition = goal.getCurrentPosition();
                goal.setCurrentPosition(new Vector2Int(currentPosition.getXAsInteger() + direction.getXAsInteger(), currentPosition.getYAsInteger() + direction.getYAsInteger()));

                ISpaceObject[] allFireman = space.getSpaceObjectsByType("fireman");

                boolean canChangePosition = true;
                for(int i = 0; i < allFireman.length; i++){
                    Vector2Int pos = (Vector2Int) allFireman[i].getProperty("position");
                    if (pos.getXAsInteger() == goal.getCurrentPosition().getXAsInteger()
                            && pos.getYAsInteger() == pos.getYAsInteger()){
                        canChangePosition = false;
                    }
                }

                if (canChangePosition)
                    myself.setProperty("position", new Vector2Int(goal.getCurrentPosition().getXAsInteger(), goal.getCurrentPosition().getYAsInteger()));
                else {
                    goal.setCurrentPosition(previousPosition);
                }
                getNearObjects(goal.getCurrentPosition(),EXTINGUISH_CAMPS,true);
                getNearObjects(goal.getCurrentPosition(),VISION_CAMPS,false);
            } else {
                System.out.println("direction null");
            }
            throw new PlanFailureException();

        }

        @PlanPassed
        public void passed() {
            System.out.println("Reached destination");
        }

    }


}