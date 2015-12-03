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

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

@Agent
public class FiremanBDI {@Agent
protected BDIAgent fireman;

    @Belief
    protected Grid2D space = (Grid2D) fireman.getParentAccess().getExtension("2dspace").get();

    @Belief
    protected Queue < ISpaceObject > nearObjects;

    @Belief(updaterate = 500)
    protected long currentTime = System.currentTimeMillis();

    @Belief
    protected ISpaceObject myself = space.getAvatar(fireman.getComponentDescription(), fireman.getModel().getFullName());

    @AgentBody
    public void body() {

        nearObjects = new LinkedList < ISpaceObject > ();

        Random r = new Random();

        int spaceHeight = space.getAreaSize().getXAsInteger(),
                spaceWidth = space.getAreaSize().getYAsInteger(), xPosition = r.nextInt(spaceWidth),
                yPosition = r.nextInt(spaceHeight);

        myself.setProperty("position", new Vector2Int(xPosition, yPosition));
        myself.setProperty("speed", 10);

        FiremanGoal Goal = new FiremanGoal(new Vector2Int(0, 0));
        Goal.setCurrentPosition(new Vector2Int(xPosition, yPosition));

        // initialize array with ISpaceObjects near current position
        getNearObjects(Goal.getCurrentPosition());

        FiremanGoal goal = (FiremanGoal) fireman.dispatchTopLevelGoal(Goal).get();

    }

    public void getNearObjects(Vector2Int currPosition) {

        Object[] nearObj = space.getNearObjects(currPosition, new Vector1Int(10), "fire").toArray();
        for (int i = 0; i < nearObj.length; i++) {
            nearObjects.add((ISpaceObject) nearObj[i]);
        }
    }

    @Plan(trigger = @Trigger(goals = FiremanGoal.class))
    public class MovingPlan {

        @PlanBody
        protected void changePosition(FiremanGoal goal) {
            Vector2Int currentPosition = goal.getCurrentPosition();

            Vector2Int direction = new Vector2Int();

            System.out.println("1. Queue size: " + nearObjects.size());

            if (nearObjects.size() == 0) {
                getNearObjects(goal.getCurrentPosition());
                if (space.getSpaceObjectsByType("fire").length == 0){
                    goal.changeNoMoreFireCells();
                } else {
                    System.out.println("Not done yet.");
                    myself.setProperty("position", new Vector2Int(goal.getCurrentPosition().getXAsInteger()+1, goal.getCurrentPosition().getYAsInteger()+1));
                }
            } else {
                ISpaceObject object = nearObjects.peek();

                Vector2Double peekedObjCoords = (Vector2Double) object.getProperty("position");

                // X position
                if (peekedObjCoords.getXAsInteger() < currentPosition.getXAsInteger() && space.getDistance(peekedObjCoords, currentPosition).getAsInteger() > 1) {
                    direction.setX(new Vector1Int(-1));
                } else if (peekedObjCoords.getXAsInteger() < currentPosition.getXAsInteger()) {
                    // TODO: Apagar incendio
                    direction.setX(new Vector1Int(0));
                    nearObjects.remove();
                } else if (peekedObjCoords.getXAsInteger() > currentPosition.getXAsInteger() && space.getDistance(peekedObjCoords, currentPosition).getAsInteger() > 1) {
                    // TODO: Apagar incendio
                    direction.setX(new Vector1Int(1));
                } else if (peekedObjCoords.getXAsInteger() > currentPosition.getXAsInteger()) {
                    // TODO: Apagar incendio
                    direction.setX(new Vector1Int(0));
                    nearObjects.remove();
                }

                // Y position
                if (peekedObjCoords.getYAsInteger() < currentPosition.getYAsInteger() && space.getDistance(peekedObjCoords, currentPosition).getAsInteger() > 1) {
                    direction.setY(new Vector1Int(-1));
                } else if (peekedObjCoords.getYAsInteger() < currentPosition.getYAsInteger()) {
                    // TODO: Apagar incendio
                    direction.setY(new Vector1Int(0));
                    nearObjects.remove();
                } else if (peekedObjCoords.getYAsInteger() > currentPosition.getYAsInteger() && space.getDistance(peekedObjCoords, currentPosition).getAsInteger() > 1) {
                    // TODO: Apagar incendio
                    direction.setY(new Vector1Int(1));
                } else if (peekedObjCoords.getYAsInteger() > currentPosition.getYAsInteger()) {
                    // TODO: Apagar incendio
                    direction.setY(new Vector1Int(0));
                    nearObjects.remove();
                }

            }

            System.out.println("2. Queue size: " + nearObjects.size());
            goal.setCurrentPosition(new Vector2Int(currentPosition.getXAsInteger() + direction.getXAsInteger(), currentPosition.getYAsInteger() + direction.getYAsInteger()));
            myself.setProperty("position", new Vector2Int(goal.getCurrentPosition().getXAsInteger(), goal.getCurrentPosition().getYAsInteger()));
            throw new PlanFailureException();
        }

            @PlanPassed
            public void passed() {
                System.out.println("Reached destination");
            }

        }


}