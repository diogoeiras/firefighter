package agents;

import events.FiremanPersonEvent;
import goals.FiremanQuadGoal;
import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.runtime.impl.PlanFailureException;
import jadex.bridge.service.annotation.Service;
import jadex.extension.envsupport.environment.ISpaceObject;
import jadex.extension.envsupport.environment.space2d.Grid2D;
import jadex.extension.envsupport.math.IVector2;
import jadex.extension.envsupport.math.Vector1Int;
import jadex.extension.envsupport.math.Vector2Double;
import jadex.extension.envsupport.math.Vector2Int;
import jadex.micro.annotation.*;
import goals.FiremanGoal;
import processes.FireProcess;
import services.ICommunicationService;
import utils.SharedCode;

import java.util.*;

// TODO: LInha diagonal
@Description("An agent who does everything since helping people who does nothing but cry to put down fire.")
@Agent
@Service
@ProvidedServices(@ProvidedService(type = ICommunicationService.class))
public class FiremanBDI implements ICommunicationService {
    @Agent
    protected BDIAgent fireman;
    public static String BEHAVIOR = "QUAD1";
    protected FiremanGoal Goal;
    protected FiremanQuadGoal QuadGoal;

    private static final int VISION_CAMPS = 5;
    private static final int EXTINGUISH_CAMPS = 1;
    private SharedCode textMessage;
    private ArrayList<FiremanPersonEvent> stressSignals = new ArrayList<>();
    private int stucked = 0;

    // QUAD
    protected boolean LEADER = false;
    protected boolean alreadyDefinedQuads = false;
    protected ArrayList<ISpaceObject> firemanWithoutQuads = new ArrayList<>();
    protected int numberOfQuads;
    protected boolean getFiremanInPosition;

    protected Vector2Int secondChoicePosition = null;


    @Belief
    protected Grid2D space = (Grid2D) fireman.getParentAccess().getExtension("2dspace").get();

    @Belief
    protected Queue<ISpaceObject> nearObjects, nearObjectsToExtinguish;

    @Belief(updaterate = 150)
    protected long currentTime = System.currentTimeMillis();

    @Belief
    protected ISpaceObject myself = space.getAvatar(fireman.getComponentDescription(), fireman.getModel().getFullName());

    @AgentBody
    public void body() {

        textMessage = new SharedCode(fireman, myself.getId());

        nearObjects = Collections.asLifoQueue(new ArrayDeque<ISpaceObject>());
        nearObjectsToExtinguish = new ArrayDeque<>();

        Random r = new Random();

        int spaceHeight = space.getAreaSize().getYAsInteger(),
                spaceWidth = space.getAreaSize().getXAsInteger(), xPosition = r.nextInt(spaceWidth),
                yPosition = r.nextInt(spaceHeight);

        myself.setProperty("position", new Vector2Int(xPosition, yPosition));

        if (FiremanBDI.BEHAVIOR == "QUAD") {
            if (myself.getId() == FireProcess.LEADER_ID) {
                this.LEADER = true;
                getFiremanInPosition = false;
            }
            QuadGoal = new FiremanQuadGoal(null);
            QuadGoal.setCurrentPosition(new Vector2Int(xPosition, yPosition));
            FiremanQuadGoal goal = (FiremanQuadGoal) fireman.dispatchTopLevelGoal(QuadGoal).get();

            // initialize array with ISpaceObjects near current position
            getNearObjects(QuadGoal.getCurrentPosition(), VISION_CAMPS, false);
            getNearObjects(QuadGoal.getCurrentPosition(), EXTINGUISH_CAMPS, true);
        } else {
            Goal = new FiremanGoal(null);
            Goal.setCurrentPosition(new Vector2Int(xPosition, yPosition));
            FiremanGoal goal = (FiremanGoal) fireman.dispatchTopLevelGoal(Goal).get();

            // initialize array with ISpaceObjects near current position
            getNearObjects(Goal.getCurrentPosition(), VISION_CAMPS, false);
            getNearObjects(Goal.getCurrentPosition(), EXTINGUISH_CAMPS, true);
        }

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

        if (curr == null || Des == null) return null;
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

    // Check if a fireman can eliminate an Object on a determined cell from his current position
    public boolean canEliminate(Vector2Int currentPosition, Vector2Double positionToExtinguish) {

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

    // Function to eliminate repeated ISpaceObjects on queues that determinate fireman next movement
    public void removeRepeatedCells(Vector2Int current) {

        nearObjects = getQueueRepeatedFree(nearObjects, current, false);

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
                if (toExtinguished && canEliminate(currentPosition, current)) {
                    newQueue.add(oldQueue.remove());
                    grid[current.getXAsInteger()][current.getYAsInteger()] = "X";
                } else if (!toExtinguished && canEliminate(currentPosition, current)) {
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
                    person = space.getSpaceObject(event.getPerson());
                } catch (RuntimeException e) {
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
            if (!e.getDeadEvent() || e.getStatus()) {
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
    public static Vector2Int currentPositionOfAnObjectId(Grid2D space, Object id) {
        ISpaceObject objectSeeked = space.getSpaceObject0(id);
        if (objectSeeked != null) {
            if (objectSeeked.getProperty("position") instanceof Vector2Int) {
                Vector2Int vec = (Vector2Int) objectSeeked.getProperty("position");
                return vec;
            } else if (objectSeeked.getProperty("position") instanceof Vector2Double) {
                Vector2Int vec = VECTOR2DOUBLE_TO_VECTOR2INT((Vector2Double) objectSeeked.getProperty("position"));
                return vec;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /*
            FOR QUAD PLAN

     */

    // calculate center of quad
    public Vector2Int getCenterOfQuad(int quad) {
        int x, y;

        if (quad == 0) {
            x = 12;
            y = 12;
        } else if (quad == 1) {
            x = 36;
            y = 12;
        } else if (quad == 2) {
            x = 12;
            y = 36;
        } else {
            x = 36;
            y = 36;
        }

        return new Vector2Int(x, y);
    }

    ;

    /*
            MESSAGING

     */

    @Override
    public void refuseRescueRequest(Object firemanID, Object personID) {
    }

    @Override
    public void RescueMessage(Object personID, Object firemanID) {

        if (firemanID == myself.getId()) {

            ISpaceObject personObj = space.getSpaceObject0(personID);

            Vector2Int position = (Vector2Int) personObj.getProperty("position");


            // Check if there is already an active event. We need this to verify if this fireman can accept this event.
            boolean canAcceptThisEvent = true;
            if (existsActiveEvent()) {
                canAcceptThisEvent = false;
            }

            if (canAcceptThisEvent) {
                // Change Goal desired Position and add this accepted event to the stressSignals
                if (BEHAVIOR == "QUAD") {
                    QuadGoal.setDesiredPosition(position);
                } else {
                    Goal.setDesiredPosition(position);
                }
                FiremanPersonEvent rescue = new FiremanPersonEvent(firemanID, personID);
                stressSignals.add(rescue);
                textMessage.messageRescueConfirmationForPerson(firemanID, personID);

                System.out.println("[Fireman " + firemanID + "] - Accept a rescue request from [Person " + personID + "]");
                return;
            } else {
                textMessage.messageRefuseRescue(firemanID, personID);
                System.out.println("[Fireman " + firemanID + "] - Refused a rescue request from [Person " + personID + "]");
            }
        }
    }

    @Override
    public void RescueConfirmation(Object firemanID, Object personID) {

    }

    @Override
    public void sendConfirmationQuad(Object leaderID, Object firemanID, int quad) {
        if (leaderID == myself.getId()) {
            ArrayList<ISpaceObject> temp = new ArrayList<>();
            for (ISpaceObject e : firemanWithoutQuads) {
                if (e.getId() != firemanID) {
                    temp.add(e);
                } else {
                    System.out.println("[" + currentTime + "] Fireman " + e.getId() + " accepted the " + quad + " quad.");
                }
            }
            firemanWithoutQuads = temp;
            if (firemanWithoutQuads.size() == 0) {
                this.alreadyDefinedQuads = true;
            }
        }
    }

    @Override
    public void sendQuad(Object leaderID, Object firemanID, int quad) {
        if (firemanID == myself.getId()) {
            QuadGoal.setQuad(quad);
            System.out.println("[" + currentTime + "] Fireman " + firemanID + " has the " + quad + " quad.");
            textMessage.confirmationQuads(leaderID, firemanID, quad);
        }
    }

    @Plan(trigger = @Trigger(goals = FiremanGoal.class))
    public class MovingPlan {

        @PlanBody
        protected void changePosition(FiremanGoal goal) {

            // CHECKING ALIVENESS OF EVENTS
            removeUselessEvents();

            Vector2Int direction = new Vector2Int(0, 0);

            // Checking if there is a person near this fireman
            //ISpaceObject nearPerson = (ISpaceObject) personNear();
            Object[] person = getPersonOnCurrentPosition(goal);

            if (person != null && person.length > 0) {
                tryToEliminatePerson((ISpaceObject) person[0]);
            } else if (goal.getDesiredPosition() != null && currentEqualDesire(goal)) {
                goal.setDesiredPosition(null);
            } else if (nearObjectsToExtinguish.size() > 0) {

                // get where i am clean first
                Object[] mySpot = getObjectsInCurrentPosition();
                if (mySpot != null && mySpot.length > 0) {
                    putDownFireCell((ISpaceObject) mySpot[0]);
                }

                if (existsActiveEvent()) {

                    Vector2Int currentPositionOfPerson = currentPositionOfAnObjectId(space, getActiveEvent().getPerson()),
                            directionToPerson = null;
                    Vector2Double desiredCell = null;

                    if (currentPositionOfPerson != null) {
                        directionToPerson = returnDirection(space, goal.getCurrentPosition(), currentPositionOfPerson);
                        if (directionToPerson != null) {
                            desiredCell = new Vector2Double(goal.getCurrentPosition().getXAsInteger() + directionToPerson.getXAsInteger(),
                                    goal.getCurrentPosition().getYAsInteger() + directionToPerson.getYAsInteger());

                            if (desiredCell != null) {
                                if (canEliminate(currentPositionOfAnObjectId(space, myself.getId()), desiredCell)) {
                                    ISpaceObject cell = getSpaceObjectInPos(desiredCell);
                                    if (cell != null)
                                        putDownFireCell(cell);
                                }
                            }
                        }
                    }

                    nearObjectsToExtinguish = null;
                    nearObjectsToExtinguish = new ArrayDeque<>();

                    if (directionToPerson != null) {
                        direction = directionToPerson;
                    } else {
                        direction = new Vector2Int(0, 0);
                    }

                } else {
                    Vector2Int desiredPosition = new Vector2Int(0, 0);
                    ArrayList<Vector2Double> temp = new ArrayList<>();
                    while (nearObjectsToExtinguish.size() > 0) {
                        desiredPosition = currentPositionOfAnObjectId(space, nearObjectsToExtinguish.peek().getId());

                        if (desiredPosition != null) {
                            Vector2Double desired = new Vector2Double(desiredPosition.getXAsInteger(), desiredPosition.getYAsInteger());
                            if (canEliminate(currentPositionOfAnObjectId(space, myself.getId()), desired)) {
                                temp.add(desired);
                                putDownFireCell(nearObjectsToExtinguish.peek());
                            }
                        }

                        nearObjectsToExtinguish.remove();
                    }

                    //direction = new Vector2Int(0, 0);
                    if (temp.size() > 0) {
                        Vector2Int __temp = VECTOR2DOUBLE_TO_VECTOR2INT(temp.get(new Random().nextInt(temp.size())));
                        direction = returnDirection(space, goal.getCurrentPosition(), __temp);
                        if (direction != null && direction.getXAsInteger() == 0 && direction.getYAsInteger() == 0)
                            stucked += 1;
                    } else {
                        direction = returnDirection(space, goal.getCurrentPosition(), desiredPosition);
                    }
                }
            } else if (existsActiveEvent()) {
                FiremanPersonEvent event = getActiveEvent();

                if (checkAlivenessOfObject(event.getPerson())) {
                    goal.setDesiredPosition(currentPositionOfAnObjectId(space, event.getPerson()));
                    direction = returnDirection(space, goal.getCurrentPosition(), currentPositionOfAnObjectId(space, event.getPerson()));
                } else {
                    getActiveEvent().changeDeadEvent();
                    goal.setDesiredPosition(null);
                    removeUselessEvents();
                }

            } else if (nearObjects.size() > 0) {

                // change desired position
                if (!existsActiveEvent()) {
                    if (currentPositionOfAnObjectId(space, nearObjects.peek().getId()) != null) {
                        direction = returnDirection(space, goal.getCurrentPosition(), currentPositionOfAnObjectId(space, nearObjects.peek().getId()));
                        goal.setDesiredPosition(currentPositionOfAnObjectId(space, nearObjects.peek().getId()));
                    }
                } else {
                    System.out.println("There is an active Event with " + getActiveEvent().getFireman() + ", " + getActiveEvent().getStatus());
                }
                nearObjects.remove();
            } else {

                direction = getAnotherDirection(goal);
                if (direction == null) {
                    goal.changeNoMoreFireCells();
                } else {
                    goal.setDesiredPosition(secondChoicePosition);
                }
            }

            if (direction != null) {

                Vector2Int nextDir = null;

                nextDir = new Vector2Int(goal.getCurrentPosition().getXAsInteger() + direction.getXAsInteger(),
                        goal.getCurrentPosition().getYAsInteger() + direction.getYAsInteger());

                Vector2Int myNextDirConfirmed = changePositionFireman(goal, nextDir);
                goal.setCurrentPosition(myNextDirConfirmed);
                myself.setProperty("position", myNextDirConfirmed);

            } else {
                System.out.println("Direction null");
            }

            getNearObjects(goal.getCurrentPosition(), EXTINGUISH_CAMPS, true);
            getNearObjects(goal.getCurrentPosition(), VISION_CAMPS, false);

            removeRepeatedCells(goal.getCurrentPosition());

            throw new PlanFailureException();

        }

        @PlanPassed
        public void passed() {
            System.out.println("[" + currentTime + "] ~~~Reached destination~~~");
        }


        // Function that checks if there is some person near this fireman
        private Object personNear() {

            Object[] person = space.getNearObjects(currentPositionOfAnObjectId(space, myself.getId()), new Vector1Int(1), "person").toArray();

            if (person != null && person.length > 0) {
                return person[0];
            } else {
                return null;
            }
        }

        // Function responsible to eliminate certain person id
        private boolean tryToEliminatePerson(ISpaceObject person) {

            Vector2Int personPosition = currentPositionOfAnObjectId(space, person.getId());
            Vector2Int currentPosition = currentPositionOfAnObjectId(space, myself.getId());

            if (currentPosition == null || personPosition == null) return false;
            else if (canEliminate(currentPosition, new Vector2Double(personPosition.getXAsInteger(), personPosition.getYAsInteger()))) {
                if (savePerson(person)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        // Function that eliminates the person object so it can be saved
        private boolean savePerson(ISpaceObject person) {
            Vector2Int cur = currentPositionOfAnObjectId(space, person.getId()),
                    myCur = currentPositionOfAnObjectId(space, myself.getId());
            if (cur == null) return false;
            if (cur.getXAsInteger() == myCur.getXAsInteger() && cur.getYAsInteger() == myCur.getYAsInteger()) {
                // TODO: Criar HELICOPETRO PARA SALVAR
                return true;
            } else {
                return false;
            }
        }

        // Function that handles nearObjects
        private Vector2Int retrieveDirectionToFireCell() {

            ISpaceObject fireCellToEliminate = retrievingFirstPossiblePosition();

            if (fireCellToEliminate != null) {
                Vector2Int direction = returnDirection(space, currentPositionOfAnObjectId(space, myself.getId()), currentPositionOfAnObjectId(space, fireCellToEliminate.getId()));
                return direction;
            } else {
                return null;
            }
        }

        // Function that retrieves the first possible position to eliminate in the queue
        private ISpaceObject retrievingFirstPossiblePosition() {

            while (nearObjectsToExtinguish.size() > 0) {
                Vector2Double thisCandidate = (Vector2Double) nearObjectsToExtinguish.peek().getProperty("position");
                if (canEliminate(currentPositionOfAnObjectId(space, myself.getId()), thisCandidate)) {
                    return nearObjectsToExtinguish.remove();
                } else {
                    nearObjectsToExtinguish.remove();
                }
            }
            return null;
        }

        // Function that destroys a object of type "fire" and create a new one of "wetTerrain"
        private void putDownFireCell(ISpaceObject cell) {

            if (space.destroyAndVerifySpaceObject(cell.getId())) {

                Vector2Double doubleVector = (Vector2Double) cell.getProperty("position");
                Map properties = new HashMap();
                properties.put("position", VECTOR2DOUBLE_TO_VECTOR2INT(doubleVector));
                properties.put("type", 1);
                space.createSpaceObject("wetTerrain", properties, null);
            }

        }

        // Function to find a direction in case of nearObjectsToExtinguish.size() == 0 && nearObjects.size() == 0
        private Vector2Int getAnotherDirection(FiremanGoal goal) {
            Vector2Int direction = null;

            if (goal.getDesiredPosition() != null && !currentEqualDesire(goal)) {
                direction = returnDirection(space, goal.getCurrentPosition(), goal.getDesiredPosition());
                return direction;
            }

            Object[] fire = space.getSpaceObjectsByType("fire");

            if (fire != null && fire.length > 0) {

                Random rnd = new Random();

                Vector2Int pos = currentPositionOfAnObjectId(space, ((ISpaceObject) fire[rnd.nextInt(fire.length)]).getId());
                if (pos != null) {
                    secondChoicePosition = pos;
                    direction = returnDirection(space, goal.getCurrentPosition(), pos);
                }
            }

            return direction;
        }

        // Function that checks prevents getSpaceObjectByGrid for throwing a exception
        private Object[] getObjectsInCurrentPosition() {
            Object[] thisCellObjects;

            try {
                thisCellObjects = space.getSpaceObjectsByGridPosition(currentPositionOfAnObjectId(space, myself.getId()), "fire").toArray();
            } catch (RuntimeException e) {
                thisCellObjects = null;
            }
            return thisCellObjects;
        }

        // Function to change positions if is it ok to move
        private Vector2Int changePositionFireman(FiremanGoal goal, Vector2Int desired) {
            Collection<ISpaceObject> obj = space.getSpaceObjectsByGridPosition(desired, "fireman");

            if (obj == null || obj.size() == 0) {
                return desired;
            }

            return goal.getCurrentPosition();
        }

        // Function that compares two Vector2Int
        private boolean currentEqualDesire(FiremanGoal goal) {

            return goal.getCurrentPosition().getXAsInteger() == goal.getDesiredPosition().getXAsInteger() &&
                    goal.getCurrentPosition().getYAsInteger() == goal.getDesiredPosition().getYAsInteger();
        }

        // Function to return person in the same position of fireman
        public Object[] getPersonOnCurrentPosition(FiremanGoal goal) {
            ArrayList<ISpaceObject> person = new ArrayList<>();
            try {
                Vector2Int curr = goal.getCurrentPosition();
                String per = "person";
                ISpaceObject[] temp = space.getSpaceObjectsByType("person");

                for (ISpaceObject e : temp) {
                    Vector2Int pos = currentPositionOfAnObjectId(space, e.getId());

                    if (pos.getXAsInteger() == goal.getCurrentPosition().getXAsInteger() &&
                            pos.getYAsInteger() == goal.getCurrentPosition().getYAsInteger()) {
                        person.add(e);
                    }
                }

            } catch (RuntimeException e) {
                person = null;
                return null;
            }

            return person.toArray();
        }

        // Function to return a IspaceObject of type "fire" in position pos
        private ISpaceObject getSpaceObjectInPos(Vector2Double pos) {

            ISpaceObject fireCell = null;

            try {

                ISpaceObject[] fireElem = space.getSpaceObjectsByType("fire");

                for (ISpaceObject fire : fireElem) {
                    Vector2Int curr = currentPositionOfAnObjectId(space, fire.getId());

                    if (curr.getXAsInteger() == pos.getXAsInteger() &&
                            curr.getYAsInteger() == pos.getYAsInteger()) {
                        fireCell = fire;
                        break;
                    }
                }

            } catch (RuntimeException e) {
                fireCell = null;
            }

            return fireCell;
        }
    }

    @Plan(trigger = @Trigger(goals = FiremanQuadGoal.class))
    public class QuadPlan {

        @PlanBody
        protected void Quad(FiremanQuadGoal goal) {

            if (LEADER || myself.getId() == FireProcess.LEADER_ID) {
                LEADER = true;
                if (firemanWithoutQuads.size() == 0 && !alreadyDefinedQuads) {
                    ISpaceObject[] fireman = space.getSpaceObjectsByType("fireman");
                    if (fireman != null) {
                        int i = 0;
                        for (ISpaceObject e : fireman) {
                            firemanWithoutQuads.add(e);
                            textMessage.sendQuads(myself.getId(), e.getId(), i);
                            i++;
                        }
                        numberOfQuads = i;
                    }
                }
            }

            if (QuadGoal.getQuad() != -1 && !getFiremanInPosition) {
                changePosition();
            } else {
                // Movement
                Vector2Int direction = null;
                if (QuadGoal.getDesiredPosition() != null && QuadGoal.getCurrentPosition().getXAsInteger() == QuadGoal.getDesiredPosition().getXAsInteger() && QuadGoal.getCurrentPosition().getYAsInteger() == QuadGoal.getDesiredPosition().getYAsInteger()) {
                    QuadGoal.setDesiredPosition(null);
                }
                if (nearObjectsToExtinguish.size() > 0) {

                    while (nearObjectsToExtinguish.size() != 0) {
                        if (canPutDown(QuadGoal.getCurrentPosition(), currentPositionOfAnObjectId(space, nearObjectsToExtinguish.peek().getId()), QuadGoal.getQuad())) {
                            putDownFire(nearObjectsToExtinguish.peek());
                            nearObjectsToExtinguish.remove();
                            direction = new Vector2Int(0, 0);
                            break;
                        }
                        nearObjectsToExtinguish.remove();
                    }
                } else if (existsActiveEvent()) {
                    direction = returnDirection(space, QuadGoal.getCurrentPosition(), currentPositionOfAnObjectId(space, getActiveEvent().getPerson()));
                } else if (nearObjects.size() > 0) {

                    if (!existsActiveEvent()) {
                        if (checkMovement(QuadGoal.getCurrentPosition(), currentPositionOfAnObjectId(space, nearObjects.peek().getId()))) {
                            direction = returnDirection(space, QuadGoal.getCurrentPosition(), currentPositionOfAnObjectId(space, nearObjects.peek().getId()));
                            QuadGoal.setDesiredPosition(currentPositionOfAnObjectId(space, nearObjects.peek().getId()));
                        }
                        nearObjects.remove();
                    }
                } else {

                    if (QuadGoal.getDesiredPosition() != null) {
                        direction = returnDirection(space, QuadGoal.getCurrentPosition(), QuadGoal.getDesiredPosition());

                        if (direction != null) {
                            Vector2Int vec = new Vector2Int(QuadGoal.getCurrentPosition().getXAsInteger() + direction.getXAsInteger(), QuadGoal.getCurrentPosition().getYAsInteger());
                            if (!checkMovement(QuadGoal.getCurrentPosition(), vec)) {
                                QuadGoal.setDesiredPosition(null);
                                direction = null;
                            }
                        }
                    } else {
                        ISpaceObject[] fireObjects = space.getSpaceObjectsByType("fire");

                        for (ISpaceObject e : fireObjects) {

                            try {
                                Vector2Int vec = currentPositionOfAnObjectId(space, e.getId());

                                if (vec != null && inThisQuad(vec)) {
                                    QuadGoal.setDesiredPosition(vec);
                                    direction = returnDirection(space, QuadGoal.getCurrentPosition(), vec);
                                }
                            } catch (NullPointerException P) {
                                direction = null;
                            }
                        }
                    }
                }

                if (direction != null) {

                    Vector2Int nextPosition = new Vector2Int(QuadGoal.getCurrentPosition().getXAsInteger() + direction.getXAsInteger(), QuadGoal.getCurrentPosition().getYAsInteger() + direction.getYAsInteger());

                    myself.setProperty("position", nextPosition);
                    goal.setCurrentPosition(nextPosition);
                } else {
                    ISpaceObject[] fireObjects = space.getSpaceObjectsByType("fire");

                    for (ISpaceObject e : fireObjects) {
                        try {
                            Vector2Int vec = currentPositionOfAnObjectId(space, e.getId());

                            if (vec != null && inThisQuad(vec)) {
                                QuadGoal.setDesiredPosition(vec);
                                direction = returnDirection(space, QuadGoal.getCurrentPosition(), vec);
                                if (direction == null) {
                                    break;
                                }
                                Vector2Int curr = new Vector2Int(QuadGoal.getCurrentPosition().getXAsInteger() + direction.getXAsInteger(), QuadGoal.getCurrentPosition().getYAsInteger() + direction.getYAsInteger());
                                myself.setProperty("position", curr);
                                goal.setCurrentPosition(curr);
                                break;
                            }
                        } catch (NullPointerException e1) {
                            continue;
                        }
                    }

                    if (direction == null) {
                        direction = returnDirection(space, QuadGoal.getCurrentPosition(), getCenterOfQuad(QuadGoal.getQuad()));
                        Vector2Int curr = new Vector2Int(QuadGoal.getCurrentPosition().getXAsInteger() + direction.getXAsInteger(), QuadGoal.getCurrentPosition().getYAsInteger() + direction.getYAsInteger());
                        myself.setProperty("position", curr);
                        goal.setCurrentPosition(curr);
                    }
                }

                getNearObjects(QuadGoal.getCurrentPosition(), VISION_CAMPS, false);
                getNearObjects(QuadGoal.getCurrentPosition(), EXTINGUISH_CAMPS, true);
            }


            removeRepeatedCells(QuadGoal.getCurrentPosition());

            throw new PlanFailureException();
        }

        @PlanPassed
        public void passed() {
            System.out.println("[" + currentTime + "] ~~~Reached destination~~~");
        }


        private void changePosition() {
            if (!getFiremanInPosition) {
                QuadGoal.setCurrentPosition(getCenterOfQuad(QuadGoal.getQuad()));
                myself.setProperty("position", QuadGoal.getCurrentPosition());
                getFiremanInPosition = true;
            }
        }

        private boolean canPutDown(Vector2Int currentPosition, Vector2Int desiredPosition, int quad) {

            int[][] bounds = getBoundaries(quad);

            if (desiredPosition == null) return false;

            if (desiredPosition.getXAsInteger() < bounds[0][0] || desiredPosition.getXAsInteger() > bounds[0][1]) {
                return false;
            } else if (desiredPosition.getYAsInteger() < bounds[1][0] || desiredPosition.getYAsInteger() > bounds[1][1]) {
                return false;
            } else if (Math.abs(desiredPosition.getXAsInteger() - currentPosition.getXAsInteger()) > 1 ||
                    Math.abs(desiredPosition.getYAsInteger() - currentPosition.getYAsInteger()) > 1) {
                return false;
            }

            return true;
        }

        private int[][] getBoundaries(int quad) {
            int[][] bound = new int[2][2];

            if (quad == 0) {
                bound[0][0] = 0;
                bound[0][1] = 24;
                bound[1][0] = 0;
                bound[1][1] = 24;
            } else if (quad == 1) {
                bound[0][0] = 25;
                bound[0][1] = 49;
                bound[1][0] = 0;
                bound[1][1] = 24;
            } else if (quad == 2) {
                bound[0][0] = 0;
                bound[0][1] = 24;
                bound[1][0] = 25;
                bound[1][1] = 49;
            } else {
                bound[0][0] = 25;
                bound[0][1] = 49;
                bound[1][0] = 25;
                bound[1][1] = 49;
            }

            return bound;
        }

        private void putDownFire(ISpaceObject cell) {
            if (space.destroyAndVerifySpaceObject(cell.getId())) {
                Vector2Double doubleVector = (Vector2Double) cell.getProperty("position");
                Vector2Int vec = new Vector2Int(doubleVector.getXAsInteger(), doubleVector.getYAsInteger());
                Map properties = new HashMap();
                properties.put("position", vec);
                properties.put("type", 1);
                space.createSpaceObject("wetTerrain", properties, null);
            }
        }

        private boolean checkMovement(Vector2Int current, Vector2Int dest) {

            if (canPutDown(current, dest, QuadGoal.getQuad())) {
                return true;
            } else return false;
        }

        private boolean inThisQuad(Vector2Int vec) {

            int[][] bounds = getBoundaries(QuadGoal.getQuad());

            if (vec.getXAsInteger() >= bounds[0][0] && vec.getXAsInteger() <= bounds[0][1] && vec.getYAsInteger() >= bounds[1][0] && vec.getYAsInteger() <= bounds[1][1]) {
                return true;
            } else {
                return false;
            }

        }
    }


}