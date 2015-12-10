package agents;

import goals.FiremanGoal;
import goals.PersonGoal;
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

import java.util.*;

@Agent
public class PersonBDI {

    protected static int VISION = 3;

    @Agent
    protected BDIAgent person;

    @Belief
    protected Grid2D space = (Grid2D) person.getParentAccess().getExtension("2dspace").get();

    @Belief
    protected ISpaceObject myself = space.getAvatar(person.getComponentDescription(), person.getModel().getFullName());

    @Belief(updaterate = 200)
    protected long currentTime = System.currentTimeMillis();

    @AgentBody
    public void body() {
        System.out.println("PERSON Vision: " + VISION);

        Random rnd = new Random();

        while(true) {
            int y = rnd.nextInt(space.getAreaSize().getYAsInteger()),
                    x = rnd.nextInt(space.getAreaSize().getXAsInteger());

            Vector2Int position = new Vector2Int(x,y);
            if (space.getSpaceObjectsByGridPosition(position,"fire") == null ||
                    space.getSpaceObjectsByGridPosition(position,"fire").size() == 0){
                myself.setProperty("position",position);
                break;
            }
        }

        PersonGoal Goal = new PersonGoal();
        PersonGoal goal = (PersonGoal) person.dispatchTopLevelGoal(Goal).get();
    }

    public boolean isDeadBecauseFire(ArrayList<ISpaceObject> possibleCells, int neededCellsToDie){

        return possibleCells.size() >= neededCellsToDie;
    }

    public int getNumCellsNeed(Vector2Int current){

        int maxHeight = space.getAreaSize().getYAsInteger() - 1,
                maxWidth = space.getAreaSize().getXAsInteger() - 1;

        if (current.getXAsInteger() == 0 || current.getXAsInteger() == maxWidth){
            if (current.getYAsInteger() == 0 || current.getYAsInteger() == maxHeight){
                return 2;
            } else {
                return 3;
            }
        } else if (current.getYAsInteger() == 0 || current.getYAsInteger() == maxHeight){
            if (current.getXAsInteger() == 0 || current.getXAsInteger() == maxWidth){
                return 2;
            } else {
                return 3;
            }
        } else return 4;
    }

    public boolean fireAffectsme(Vector2Int currentPosition, Vector2Double positionToExtinguish){

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
        }
        else {
            return true;
        }
    }

    @Plan(trigger = @Trigger(goals = PersonGoal.class))
    public class SavingPlan {

        @PlanBody
        protected void changePosition(PersonGoal goal) {

            Vector2Int currentPosition = (Vector2Int) myself.getProperty("position");

            Object[] nearFire = space.getNearObjects(currentPosition, new Vector1Int(VISION),"fire").toArray();
            Object[] nearFireman = space.getNearObjects(currentPosition, new Vector1Int(1),"fireman").toArray();

            if (nearFireman != null && nearFireman.length > 0){
                System.out.println("A person was saved on: (" + currentPosition + ")");
                //myself.setProperty("position",new Vector2Int(-1,-1));
                goal.changeRescuedStatus();
                space.destroySpaceObject(myself.getId());
            }
            else if (nearFire != null && nearFire.length > 0){

                // TODO: Send distress signal
                String[][] allDir = new String[3][3];
                Vector2Int nextDirection = null;

                for(int i = 0; i < nearFire.length; i++){
                    Vector2Double thisFireCell = (Vector2Double) ((ISpaceObject) nearFire[i]).getProperty("position");

                    if (fireAffectsme(currentPosition,thisFireCell)) {
                        Vector2Int thisFireCell_int = new Vector2Int(thisFireCell.getXAsInteger(), thisFireCell.getYAsInteger());
                        Vector2Int resultDirection = FiremanBDI.returnDirection(space, currentPosition, thisFireCell_int);

                        allDir[resultDirection.getXAsInteger() + 1][resultDirection.getYAsInteger() + 1] = "X";
                    }
                }

                if (allDir[1][1] == "X"){
                    // TODO: DEAD;
                    System.out.println("A person died on: (" + currentPosition + ")");
                    goal.changeDeadStatus();
                    space.destroySpaceObject(myself.getId());
                    //myself.setProperty("position",new Vector2Int(-1,-1));
                } else {
                    for (int i = 0; i < allDir.length; i++) {
                        for (int j = 0; j < allDir[i].length; j++) {
                            if (allDir[i][j] != "X") {
                                nextDirection = new Vector2Int(i-1, j-1);
                                if (currentPosition.getXAsInteger() + nextDirection.getXAsInteger() > space.getAreaSize().getXAsInteger()-1 ||
                                        currentPosition.getXAsInteger() + nextDirection.getXAsInteger() < 0 ||
                                            currentPosition.getYAsInteger() + nextDirection.getYAsInteger() > space.getAreaSize().getYAsInteger()-1 ||
                                                currentPosition.getYAsInteger() + nextDirection.getYAsInteger() < 0 ){
                                    nextDirection = null;
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                    if (nextDirection == null){

                        Object[] fireInFrontof = space.getNearObjects(currentPosition,new Vector1Int(0),"fire").toArray();
                        int widthMax = space.getAreaSize().getXAsInteger()-1,
                                heightMax = space.getAreaSize().getYAsInteger()-1;

                        ArrayList<ISpaceObject> selectiveArray = new ArrayList<ISpaceObject>();
                        int numCellsNeeded = getNumCellsNeed(currentPosition);

                        if (fireInFrontof != null && fireInFrontof.length > 0) {
                            if (currentPosition.getXAsInteger() == 0) {

                                for (int i = 0; i < fireInFrontof.length; i++){
                                    Vector2Double pos = (Vector2Double)((ISpaceObject)fireInFrontof[i]).getProperty("position");
                                    if (pos.getXAsInteger() >= 0 && pos.getXAsInteger() != widthMax
                                                && pos.getYAsInteger() >= 0 && pos.getYAsInteger() <= heightMax){
                                            selectiveArray.add((ISpaceObject) fireInFrontof[i]);
                                    }
                                }
                            } else if (currentPosition.getXAsInteger() == widthMax){
                                for (int i = 0; i < fireInFrontof.length; i++){
                                    Vector2Double pos = (Vector2Double)((ISpaceObject)fireInFrontof[i]).getProperty("position");
                                    if (pos.getXAsInteger() <= widthMax && pos.getXAsInteger() != 0
                                            && pos.getYAsInteger() >= 0 && pos.getYAsInteger() <= heightMax){
                                        selectiveArray.add((ISpaceObject) fireInFrontof[i]);
                                    }
                                }
                            } else if (currentPosition.getYAsInteger() == 0){
                                for (int i = 0; i < fireInFrontof.length; i++){
                                    Vector2Double pos = (Vector2Double)((ISpaceObject)fireInFrontof[i]).getProperty("position");
                                    if (pos.getXAsInteger() >= 0 && pos.getXAsInteger() <= widthMax
                                            && pos.getYAsInteger() >= 0 && pos.getYAsInteger() != heightMax){
                                        selectiveArray.add((ISpaceObject) fireInFrontof[i]);
                                    }
                                }
                            } else if (currentPosition.getYAsInteger() == heightMax){
                                for (int i = 0; i < fireInFrontof.length; i++){
                                    Vector2Double pos = (Vector2Double)((ISpaceObject)fireInFrontof[i]).getProperty("position");
                                    if (pos.getXAsInteger() >= 0 && pos.getXAsInteger() <= widthMax
                                            && pos.getYAsInteger() <= heightMax && pos.getYAsInteger() != 0){
                                        selectiveArray.add((ISpaceObject) fireInFrontof[i]);
                                    }
                                }
                            } else {
                                for (int i = 0; i < fireInFrontof.length; i++){
                                    selectiveArray.add((ISpaceObject) fireInFrontof[i]);
                                }
                            }

                            if (isDeadBecauseFire(selectiveArray, numCellsNeeded)){
                                System.out.println("A person died on: (" + currentPosition + ")");
                                goal.changeDeadStatus();
                                space.destroySpaceObject(myself.getId());
                            }
                        }
                    } else {
                        Vector2Int nextPos = new Vector2Int(currentPosition.getXAsInteger() + nextDirection.getXAsInteger(),
                                currentPosition.getYAsInteger() + nextDirection.getXAsInteger());
                        myself.setProperty("position",nextPos);
                    }
                }
            }

            throw new PlanFailureException();
        }

        @PlanPassed
        public void passed() {
            System.out.println("[" + currentTime + "] ~~~ DESTINY IS NEVER WRONG ~~~");
        }

    }
}
