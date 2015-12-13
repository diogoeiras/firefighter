package agents;

import events.FiremanPersonEvent;
import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.runtime.impl.PlanFailureException;
import jadex.bridge.service.annotation.Service;
import jadex.extension.envsupport.environment.ISpaceObject;
import jadex.extension.envsupport.environment.space2d.Grid2D;
import jadex.extension.envsupport.math.Vector1Int;
import jadex.extension.envsupport.math.Vector2Double;
import jadex.extension.envsupport.math.Vector2Int;
import jadex.micro.annotation.*;
import goals.FiremanGoal;
import services.ICommunicationService;
import utils.SharedCode;

import java.util.*;

@Description("An agent who does everything since helping people who does nothing but cry to put down fire.")
@Agent
@Service
@ProvidedServices(@ProvidedService(type = ICommunicationService.class))
public class FiremanBDI implements ICommunicationService {
    @Agent
    protected BDIAgent fireman;
    protected FiremanGoal Goal;
    private static final int VISION_CAMPS = 5;
    private static final int EXTINGUISH_CAMPS = 1;
    private SharedCode textMessage;
    private ArrayList<FiremanPersonEvent> stressSignals = new ArrayList<>();


    @Belief
    protected Grid2D space = (Grid2D) fireman.getParentAccess().getExtension("2dspace").get();

    @Belief
    protected Queue<ISpaceObject> nearObjects, nearObjectsToExtinguish;

    @Belief(updaterate = 100)
    protected long currentTime = System.currentTimeMillis();

    @Belief
    protected ISpaceObject myself = space.getAvatar(fireman.getComponentDescription(), fireman.getModel().getFullName());

    @AgentBody
    public void body() {

        textMessage = new SharedCode(fireman, myself.getId());

        System.out.println("[" + fireman.getAgentName() + "]" + " Vision Sight: " + VISION_CAMPS);
        System.out.println("[" + fireman.getAgentName() + "]" + " Vision Extinguish: " + EXTINGUISH_CAMPS);

        nearObjects = Collections.asLifoQueue(new ArrayDeque<ISpaceObject>());
        nearObjectsToExtinguish = new ArrayDeque<>();

        Random r = new Random();

        int spaceHeight = space.getAreaSize().getYAsInteger(),
                spaceWidth = space.getAreaSize().getXAsInteger(), xPosition = r.nextInt(spaceWidth),
                yPosition = r.nextInt(spaceHeight);

        myself.setProperty("position", new Vector2Int(xPosition, yPosition));

        Goal = new FiremanGoal(null);
        Goal.setCurrentPosition(new Vector2Int(xPosition, yPosition));

        // initialize array with ISpaceObjects near current position
        getNearObjects(Goal.getCurrentPosition(), VISION_CAMPS, false);
        getNearObjects(Goal.getCurrentPosition(), EXTINGUISH_CAMPS, true);

        FiremanGoal goal = (FiremanGoal) fireman.dispatchTopLevelGoal(Goal).get();

    }

    // Get space objects that are in some distance from curent position
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

    // Function that returns a unitary vector, so the fireman can move from his current position to his destination.
    public static Vector2Int returnDirection(Grid2D space, Vector2Int curr, Vector2Int Des) {
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

    // Check if a fireman can put down a fire on a determined cell from his current position
    public boolean canExtinguish(Vector2Int currentPosition, Vector2Double positionToExtinguish) {

        int spaceWidth = space.getAreaSize().getXAsInteger(),
                spaceHeight = space.getAreaSize().getYAsInteger();

        if (currentPosition.getXAsInteger() == spaceWidth - 1 && positionToExtinguish.getXAsInteger() == 0) {
            return false;
        } else if (currentPosition.getXAsInteger() == 0 && positionToExtinguish.getXAsInteger() == spaceWidth - 1) {
            return false;
        } else if (currentPosition.getYAsInteger() == spaceHeight - 1 && positionToExtinguish.getYAsInteger() == 0) {
            return false;
        } else if (currentPosition.getYAsInteger() == 0 && positionToExtinguish.getYAsInteger() == spaceHeight - 1) {
            return false;
        } else if (Math.abs(currentPosition.getXAsInteger() - positionToExtinguish.getXAsInteger()) > 1 ||
                Math.abs(currentPosition.getYAsInteger() - positionToExtinguish.getYAsInteger()) > 1) {
            return false;
        } else {
            return true;
        }
    }

    // Function that destroys a object of type "fire" and create a new one of "wetTerrain"
    public void putDownFireCell(ISpaceObject cell) {

        if (space.destroyAndVerifySpaceObject(cell.getId())) {

            Vector2Double doubleVector = (Vector2Double) cell.getProperty("position");
            Map properties = new HashMap();
            properties.put("position", VECTOR2DOUBLE_TO_VECTOR2INT(doubleVector));
            properties.put("type", 1);
            space.createSpaceObject("wetTerrain", properties, null);
        }

    }

    // Function to eliminate repeated ISpaceObjects on queues that determinate fireman next movement
    public void removeRepeatedCells(Vector2Int current) {

        Queue<ISpaceObject> newNear = Collections.asLifoQueue(new ArrayDeque<ISpaceObject>());

        nearObjects = getQueueRepeatedFree(nearObjects, current, false);

        Queue<ISpaceObject> newVisionSight = new ArrayDeque<ISpaceObject>();

        nearObjectsToExtinguish = getQueueRepeatedFree(nearObjectsToExtinguish, current, true);
    }

    // Function that checks if some IspaceObject is not of "fire" type.
    public boolean wasAlreadyExtinguished(ISpaceObject cell) {
        return cell.getType().equals("fire");
    }

    // Check [Void removeRepeatedCells]
    public Queue<ISpaceObject> getQueueRepeatedFree(Queue<ISpaceObject> oldQueue, Vector2Int currentPosition, boolean toExtinguished) {

        Queue<ISpaceObject> newQueue = new ArrayDeque<>();
        Queue<ISpaceObject> newVisionSight = new ArrayDeque<>();

        String[][] grid = new String[space.getAreaSize().getXAsInteger()][space.getAreaSize().getYAsInteger()];

        while (oldQueue.size() != 0) {
            Vector2Double current = (Vector2Double) oldQueue.peek().getProperty("position");
            if (grid[current.getXAsInteger()][current.getYAsInteger()] != "X") {
                if (toExtinguished && canExtinguish(currentPosition, current)) {
                    newQueue.add(oldQueue.remove());
                    grid[current.getXAsInteger()][current.getYAsInteger()] = "X";
                } else if (!toExtinguished && canExtinguish(currentPosition, current)) {
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
        if (toExtinguished) {
            Queue<ISpaceObject> toReturn = Collections.asLifoQueue(new ArrayDeque<ISpaceObject>());
            Object[] temp = newQueue.toArray();
            for (int i = temp.length - 1; i >= 0; i--) {
                toReturn.add((ISpaceObject) temp[i]);
            }
            return toReturn;
        } else
            return newVisionSight;
    }

    // Check if there is some event which status is true ( Active ).
    public boolean existsActiveEvent() {
        for (FiremanPersonEvent event : stressSignals) {
            if (event.getStatus()) {
                ISpaceObject person;

                try {
                   person =  space.getSpaceObject(event.getPerson());
                } catch (RuntimeException e){
                    person = null;
                }
                if (person != null)
                    return true;
                else {
                    event.getDeadEvent();
                    event.changeStatus();
                }
            }
        }
        return false;
    }

    // Get active Event from stressSignals
    public FiremanPersonEvent getActiveEvent() {

        for (FiremanPersonEvent e : stressSignals) {
            if (e.getStatus()) {
                return e;
            }
        }

        return null;
    }

    // Remove completed or not alive events
    public void removeUselessEvents() {

        ArrayList<FiremanPersonEvent> usefulArray = new ArrayList<>();

        for (FiremanPersonEvent e : stressSignals) {
            if ( (!e.getCompleted() && !e.getDeadEvent()) || !e.getStatus()) {
                usefulArray.add(e);
            }
        }

        stressSignals = usefulArray;
    }

    // Check if a object is still alive
    public boolean checkAlivenessOfObject(Object id) {
        ISpaceObject obj;
        try {
            obj = space.getSpaceObject(id);
        } catch (RuntimeException e) {
            return false;
        }
        return true;
    }

    // Converts Vector2Double into Vector2Int
    public static Vector2Int VECTOR2DOUBLE_TO_VECTOR2INT(Vector2Double vec) {
        Vector2Int toReturn = new Vector2Int(vec.getXAsInteger(), vec.getYAsInteger());
        return toReturn;
    }

    // Get current position of an object id
    public Vector2Int currentPositionOfAnObjectId(Object id) {
        ISpaceObject objectSeeked = space.getSpaceObject0(id);
        if (objectSeeked != null) {
            return (Vector2Int) objectSeeked.getProperty("position");
        } else {
            return null;
        }
    }

    @Override
    public void RescueMessage(Object personID, Object firemanID) {

        if (firemanID == myself.getId()) {

            ISpaceObject personObj = space.getSpaceObject0(personID);

            Vector2Int position = (Vector2Int) personObj.getProperty("position");


            // Check if there is already an active event. We need this to verify if this fireman can accept this event.
            boolean canAcceptThisEvent = true;
            for (FiremanPersonEvent event : stressSignals) {
                if (event.getStatus()) {
                    canAcceptThisEvent = false;
                }
            }

            if (canAcceptThisEvent) {
                // Change Goal desired Position and add this accepted event to the stressSignals
                Goal.setDesiredPosition(position);
                FiremanPersonEvent rescue = new FiremanPersonEvent(firemanID, personID);
                stressSignals.add(rescue);
                textMessage.messageRescueConfirmationForPerson(firemanID, personID);

                System.out.println("[Fireman " + firemanID + "] - Accept a rescue request from [Person " + personID + "]");
                return;
            }
        }
        // TODO: Mandar uma mensagem a dizer para procurar outro.
    }

    @Override
    public void RescueConfirmation(Object firemanID, Object personID) {

    }

    @Plan(trigger = @Trigger(goals = FiremanGoal.class))
    public class MovingPlan {

        @PlanBody
        protected void changePosition(FiremanGoal goal) {

            Vector2Int direction = new Vector2Int(0, 0);

            if (goal.getDesiredPosition() != null &&
                    (goal.getDesiredPosition().getXAsInteger() == goal.getCurrentPosition().getXAsInteger()
                            && goal.getDesiredPosition().getYAsInteger() == goal.getCurrentPosition().getYAsInteger())) {
                goal.setDesiredPosition(null);
                direction = new Vector2Int(0, 0);
            } else if (nearObjectsToExtinguish.size() > 0) {

                // Check if fire cells are not on other sides since getNearObjects returns cells as the map is connected from both sides.
                if (canExtinguish(goal.getCurrentPosition(), (Vector2Double) nearObjectsToExtinguish.peek().getProperty("position"))) {
                    putDownFireCell(nearObjectsToExtinguish.peek());

                    Vector2Double positionToExtinguish = (Vector2Double) nearObjectsToExtinguish.peek().getProperty("position");
                    Vector2Int pos = VECTOR2DOUBLE_TO_VECTOR2INT(positionToExtinguish);
                }
                direction = new Vector2Int(0, 0);
                nearObjectsToExtinguish.remove();
            } else if (nearObjects.size() > 0) {
                Vector2Double pos = (Vector2Double) nearObjects.peek().getProperty("position");
                goal.setDesiredPosition(VECTOR2DOUBLE_TO_VECTOR2INT(pos));
                nearObjects.remove();
                if (!existsActiveEvent()) {
                    direction = returnDirection(space, goal.getCurrentPosition(), new Vector2Int(pos.getXAsInteger(), pos.getYAsInteger()));
                }

            } else {

                if (goal.getDesiredPosition() != null) {
                    direction = returnDirection(space, goal.getCurrentPosition(), goal.getDesiredPosition());
                } else {
                    ISpaceObject[] fire = space.getSpaceObjectsByType("fire");

                    if (fire.length > 0) {
                        Vector2Double pos = (Vector2Double) fire[0].getProperty("position");
                        goal.setDesiredPosition(VECTOR2DOUBLE_TO_VECTOR2INT(pos));
                        direction = returnDirection(space, goal.getCurrentPosition(), goal.getDesiredPosition());
                    } else {
                        goal.changeNoMoreFireCells();
                    }
                }
            }

            Vector2Int actualPosition = goal.getCurrentPosition();
            Vector2Int desired = new Vector2Int(actualPosition.getXAsInteger() + direction.getXAsInteger(),
                    actualPosition.getYAsInteger() + direction.getYAsInteger());

            Collection<ISpaceObject> obj = space.getSpaceObjectsByGridPosition(desired,"fireman");

            if (obj == null || obj.size() == 0) {
                goal.setCurrentPosition(desired);
            }

            myself.setProperty("position", goal.getCurrentPosition());

            if (!existsActiveEvent()) {
                getNearObjects(goal.getCurrentPosition(), VISION_CAMPS, false);
            } else {

                if (!checkAlivenessOfObject(getActiveEvent().getPerson())) {
                    goal.setDesiredPosition(currentPositionOfAnObjectId(getActiveEvent().getPerson()));
                } else {
                    goal.setDesiredPosition(null);
                    getActiveEvent().changeDeadEvent();
                    removeUselessEvents();
                }
            }

            getNearObjects(goal.getCurrentPosition(), EXTINGUISH_CAMPS, true);

            /*
            System.out.println("[" + currentTime + "] NearObjects size: " + nearObjects.size());
            System.out.println("[" + currentTime + "] NearObjectsToExtinguish: " + nearObjectsToExtinguish.size());
            System.out.println("[" + currentTime + "] Current position: (" + goal.getCurrentPosition() + ")");
            System.out.println("[" + currentTime + "] Desired Position: (" + goal.getDesiredPosition() + ")");
            System.out.println("[" + currentTime + "] Direction: (" + direction + ")");
            System.out.println("______________");
            */

            removeRepeatedCells(goal.getCurrentPosition());
            throw new PlanFailureException();

        }

        @PlanPassed
        public void passed() {
            System.out.println("[" + currentTime + "] ~~~Reached destination~~~");
        }

    }


}