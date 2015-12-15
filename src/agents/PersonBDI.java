package agents;

import goals.PersonGoal;
import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.*;
import jadex.bdiv3.runtime.impl.PlanAbortedException;
import jadex.bdiv3.runtime.impl.PlanFailureException;
import jadex.bridge.service.annotation.Service;
import jadex.extension.envsupport.environment.ISpaceObject;
import jadex.extension.envsupport.environment.space2d.Grid2D;
import jadex.extension.envsupport.math.IVector1;
import jadex.extension.envsupport.math.Vector1Int;
import jadex.extension.envsupport.math.Vector2Double;
import jadex.extension.envsupport.math.Vector2Int;
import jadex.micro.annotation.*;
import services.ICommunicationService;
import utils.SharedCode;

import java.util.*;

@Description("An agent who does nothing but cry for help.")
@Agent
@Service
@ProvidedServices(@ProvidedService(type = ICommunicationService.class))
public class PersonBDI implements ICommunicationService {

    protected static int VISION = 7;
    protected SharedCode textMessage;
    protected Object firemanResponsibleForSalvation;
    protected boolean helpAsked, responseHelp;
    private ArrayList<Object> firemanWhoWereAskedForHelpButRefused;
    private long helpAskedTime;

    @Agent
    protected BDIAgent person;

    @Belief
    protected Grid2D space = (Grid2D) person.getParentAccess().getExtension("2dspace").get();

    @Belief
    protected ISpaceObject myself = space.getAvatar(person.getComponentDescription(), person.getModel().getFullName());

    @Belief(updaterate = 200)
    protected long currentTime = System.currentTimeMillis();

    protected boolean isThereAFiremanHere = false;


    /*

        Agent functions

     */


    @AgentBody
    public void body() {

        helpAsked = false;
        textMessage = new SharedCode(person, myself.getId());
        firemanWhoWereAskedForHelpButRefused = new ArrayList<>();

        Random rnd = new Random();

        while (true) {
            int y = rnd.nextInt(space.getAreaSize().getYAsInteger()),
                    x = rnd.nextInt(space.getAreaSize().getXAsInteger());

            Vector2Int position = new Vector2Int(x, y);
            if (space.getSpaceObjectsByGridPosition(position, "fire") == null ||
                    space.getSpaceObjectsByGridPosition(position, "fire").size() == 0) {
                myself.setProperty("position", position);
                break;
            }
        }

        PersonGoal goal = (PersonGoal) person.dispatchTopLevelGoal(new PersonGoal()).get();
    }

    // Function that returns the number of cells needed around the person so he get kill
    public int getNumCellsNeed(Vector2Int current) {

        int maxHeight = space.getAreaSize().getYAsInteger() - 1,
                maxWidth = space.getAreaSize().getXAsInteger() - 1;

        if (current.getXAsInteger() == 0 || current.getXAsInteger() == maxWidth) {
            if (current.getYAsInteger() == 0 || current.getYAsInteger() == maxHeight) {
                return 2;
            } else {
                return 3;
            }
        } else if (current.getYAsInteger() == 0 || current.getYAsInteger() == maxHeight) {
            if (current.getXAsInteger() == 0 || current.getXAsInteger() == maxWidth) {
                return 2;
            } else {
                return 3;
            }
        } else return 4;
    }

    // Function that checks if a fire cell affects me
    public boolean fireAffectsMe(Vector2Int currentPosition, Vector2Double positionToExtinguish) {

        int spaceWidth = space.getAreaSize().getXAsInteger(),
                spaceHeight = space.getAreaSize().getYAsInteger();

        if (currentPosition.getXAsInteger() < VISION && positionToExtinguish.getXAsInteger() > spaceWidth - 1 - VISION) {
            return false;
        } else if (currentPosition.getXAsInteger() > spaceWidth - 1 - VISION && positionToExtinguish.getXAsInteger() < VISION) {
            return false;
        } else if (currentPosition.getYAsInteger() < VISION && positionToExtinguish.getYAsInteger() > spaceHeight - 1 - VISION) {
            return false;
        } else if (currentPosition.getYAsInteger() > spaceHeight - 1 - VISION && positionToExtinguish.getYAsInteger() < VISION) {
            return false;
        } else {
            /*
            if ( Math.abs(currentPosition.getXAsInteger() - positionToExtinguish.getXAsInteger()) < 2 &&
                    Math.abs(currentPosition.getYAsInteger() - positionToExtinguish.getYAsInteger()) < 2){
                return true;
            } else {
                return false;
            }
            */
            return true;
        }
    }

    // Function responsible to send a rescue request to the nearest fireman available
    public void sendRescueRequest() {
        this.helpAsked = true;
        this.helpAskedTime = System.currentTimeMillis();

        ISpaceObject nearest = getNearestFiremanAvailable();
        if (nearest != null) {
            firemanWhoWereAskedForHelpButRefused.add(nearest);
            textMessage.messageFiremanForRescue(nearest);
        }
    }

    // Public get nearest fireman
    public ISpaceObject getNearestFiremanAvailable() {

        ISpaceObject[] allFiremen = space.getSpaceObjectsByType("fireman");
        Vector2Int currentPosition = (Vector2Int) myself.getProperty("position");

        int distance = Integer.MAX_VALUE;
        Object nearestSoFarID = null;

        for (ISpaceObject e : allFiremen) {
            Vector2Int position = FiremanBDI.currentPositionOfAnObjectId(space, e.getId());
            IVector1 thisDistance = space.getDistance(currentPosition, position);

            if (thisDistance.getAsInteger() < distance && !didIRequestAlreadyThisFiremanHelp(e.getId())) {
                distance = thisDistance.getAsInteger();
                nearestSoFarID = e.getId();
            }
        }

        if (nearestSoFarID != null)
            return space.getSpaceObject(nearestSoFarID);
        else return null;
    }


    public boolean didIRequestAlreadyThisFiremanHelp(Object id) {
        for (Object e : firemanWhoWereAskedForHelpButRefused) {
            if (e == id) return true;
        }
        return false;
    }


    /*
        Functions related to communication between agents

     */

    @Override
    public void refuseRescueRequest(Object firemanID, Object personID) {
        if (personID == myself.getId() && firemanResponsibleForSalvation == null) {
            this.responseHelp = false;
            this.helpAsked = false;
            this.helpAskedTime = System.currentTimeMillis();
        }
    }

    @Override
    public void RescueConfirmation(Object firemanID, Object personID) {
        if (personID == myself.getId()) {
            this.responseHelp = true;
            this.firemanResponsibleForSalvation = firemanID;
        }
    }

    @Override
    public void RescueMessage(Object senderID, Object receiverID) {
    }

    @Override
    public void sendConfirmationQuad(Object leaderID, Object firemanID, int quad) {

    }

    @Override
    public void sendQuad(Object leaderID, Object firemanID, int quad) {

    }

    /*

            Agent' Plans

     */


    @Plan(trigger = @Trigger(goals = PersonGoal.class, service = @ServiceTrigger(type = ICommunicationService.class)))
    public class SavingPlan {

        @PlanBody
        protected void changePosition(PersonGoal goal) {

            Vector2Int direction = null, currentPosition = (Vector2Int) myself.getProperty("position");
            ArrayList<ISpaceObject> fireElements = getFireObjects();

            ArrayList<ISpaceObject> nearFireman = new ArrayList<>();

            if (amIOnFire()) {
                try {
                    System.out.println("A person died on: (" + currentPosition + ")");
                    goal.changeDeadStatus();
                    person.adoptPlan(new PersonBDI.CleanPerson(myself.getId()));
                } catch (NullPointerException e){
                    System.out.println("A person died on: (" + currentPosition + ")");
                    //goal.changeDeadStatus();
                }
            } else {
                // TODO: Test
                //nearFireman = null;

                try {
                    Vector2Int curr = FiremanBDI.currentPositionOfAnObjectId(space, myself.getId());
                    ISpaceObject[] temp = space.getSpaceObjectsByType("fireman");

                    for (ISpaceObject e : temp) {
                        Vector2Int pos = FiremanBDI.currentPositionOfAnObjectId(space, e.getId());

                        if (pos.getXAsInteger() == curr.getXAsInteger() &&
                                pos.getYAsInteger() == curr.getYAsInteger()) {
                            nearFireman.add(e);
                        }
                    }


                } catch (RuntimeException e) {
                    nearFireman = null;
                }


                if (nearFireman != null && nearFireman.size() > 0) {
                    try {
                        System.out.println("[" + currentTime + "] A person was saved on: (" + currentPosition + ")");
                        goal.changeRescuedStatus();
                        person.adoptPlan(new PersonBDI.CleanPerson(myself.getId()));
                    } catch (NullPointerException e){
                        System.out.println("[" + currentTime + "] A person was already dead on: (" + currentPosition + ")");
                    }
                } else if (fireElements != null && fireElements.size() > 0) {

                    // Send a rescue request to the nearest fireman available
                    if (!responseHelp) {
                        if (!helpAsked) {
                            if (System.currentTimeMillis() - helpAskedTime > 500) {
                                sendRescueRequest();
                                helpAsked = true;
                                firemanWhoWereAskedForHelpButRefused = new ArrayList<>();
                            }
                        }
                    }

                    String[][] fireDirections = fireDirection(fireElements);

                    Vector2Int directionToFireman = firemanDirection();
                    Vector2Int smartMovement = null;

                    if (directionToFireman != null) {
                        smartMovement = getPriorityMovement(directionToFireman, fireDirections);
                    }

                    if (directionToFireman != null && smartMovement != null && acceptedMovement(currentPosition, smartMovement)) {
                        direction = smartMovement;
                    } else {
                        // Check if there is a safe spot to go
                        ArrayList<Vector2Int> possibleMovs = new ArrayList<>();
                        for (int i = 0; i < fireDirections.length; i++) {
                            for (int j = 0; j < fireDirections[i].length; j++) {
                                if (fireDirections[i][j] != "X") {
                                    Vector2Int temp = new Vector2Int(i, j);
                                    direction = revertConvertUnitaryVector(temp);
                                    Vector2Int desired = new Vector2Int(currentPosition.getXAsInteger() + direction.getXAsInteger(),
                                            currentPosition.getYAsInteger() + direction.getYAsInteger());
                                    if (acceptedMovement(currentPosition, desired)) {
                                        possibleMovs.add(direction);
                                    }
                                }
                            }
                        }

                        if (possibleMovs.size() > 0) {
                            Random rnd = new Random();

                            int TheValueWasChosenMRYODA = rnd.nextInt(possibleMovs.size());

                            direction = possibleMovs.get(TheValueWasChosenMRYODA);
                        } else {
                            direction = null;
                        }
                    }
                }

                if (direction != null) {
                    Vector2Int nextPos = new Vector2Int(currentPosition.getXAsInteger() + direction.getXAsInteger(),
                            currentPosition.getYAsInteger() + direction.getXAsInteger());
                    myself.setProperty("position", nextPos);
                } else {
                    Object[] selectiveArray = space.getNearObjects(currentPosition, new Vector1Int(1), "fire").toArray();
                    if (selectiveArray.length >= getNumCellsNeed(currentPosition) && !goal.getRescued()) {
                        System.out.println("A person died on: (" + currentPosition + ")");
                        goal.changeDeadStatus();
                        space.destroySpaceObject(myself.getId());
                    }
                }
            }


            throw new PlanFailureException();
        }

        @PlanPassed
        public void passed() {
            System.out.println("[" + currentTime + "] ~~~ DESTINY IS NEVER WRONG ~~~");
        }

        // Function that gets all the nearest fire cells in a radius of 10 from the person's current position.
        private ArrayList<ISpaceObject> getFireObjects() {

            Vector2Int currentPosition = (Vector2Int) myself.getProperty("position");

            Object[] fireCells = space.getNearObjects(currentPosition, new Vector1Int(VISION), "fire").toArray();

            // There is a need of filtering fireCells, since it can exist cells of the other side of the map.
            ArrayList<ISpaceObject> fireCellsFiltered = filterFireCells(fireCells, currentPosition);


            return fireCellsFiltered;
        }

        // Function that filters from an object[] , cells that cant affect the person on CurrentPosition
        private ArrayList<ISpaceObject> filterFireCells(Object[] firecells, Vector2Int currentPosition) {

            ArrayList<ISpaceObject> filteredCellsObject = new ArrayList<>();

            for (Object fireCell : firecells) {
                Vector2Double fireCellPosition = (Vector2Double) ((ISpaceObject) fireCell).getProperty("position");


                if (fireAffectsMe(currentPosition, fireCellPosition)) {
                    filteredCellsObject.add((ISpaceObject) fireCell);
                }
            }

            return filteredCellsObject;
        }

        // Function that returns an array[3][3] in which each position is marked with an "X" if there is a fire in that
        // direction
        private String[][] fireDirection(ArrayList<ISpaceObject> fireElements) {

            Vector2Int currentPosition = (Vector2Int) myself.getProperty("position");
            String[][] fireDirections = new String[3][3];

            for (ISpaceObject cell : fireElements) {

                Vector2Double temp = (Vector2Double) cell.getProperty("position");
                Vector2Int cellObjetPosition = new Vector2Int(temp.getXAsInteger(), temp.getYAsInteger());

                Vector2Int unitaryVectorOfDirection = getUnitaryVectorOfDirection(currentPosition, cellObjetPosition);

                // Convert the unitary vector so the values fit correctly on fireDirections array.
                Vector2Int convertedUnitaryVector = convertUnitaryVector(unitaryVectorOfDirection);

                fireDirections[convertedUnitaryVector.getXAsInteger()][convertedUnitaryVector.getYAsInteger()] = "X";

            }

            return fireDirections;
        }

        // Function that returns a unitary vector ( direction )
        private Vector2Int getUnitaryVectorOfDirection(Vector2Int currentPosition, Vector2Int desiredPos) {

            Vector2Int direction = new Vector2Int();

            // Calculating X value
            if (currentPosition.getXAsInteger() < desiredPos.getXAsInteger()) {
                direction.setX(new Vector1Int(1));
            } else if (currentPosition.getXAsInteger() == desiredPos.getXAsInteger()) {
                direction.setX(new Vector1Int(0));
            } else {
                direction.setX(new Vector1Int(-1));
            }

            // Calculating Y value
            if (currentPosition.getYAsInteger() < desiredPos.getYAsInteger()) {
                direction.setY(new Vector1Int(1));
            } else if (currentPosition.getYAsInteger() == desiredPos.getYAsInteger()) {
                direction.setY(new Vector1Int(0));
            } else {
                direction.setY(new Vector1Int(-1));
            }

            return direction;
        }

        // Function that converts a vector so he can fit in a array.
        // Direction: [-1,-1] -> Array[][]: [2,0]
        private Vector2Int convertUnitaryVector(Vector2Int directionVector) {
            int x = 0, y = 0;

            if (directionVector.getXAsInteger() == 0) {
                if (directionVector.getYAsInteger() == -1) {
                    x = 2;
                    y = 1;
                } else if (directionVector.getYAsInteger() == 0) {
                    x = 0;
                    y = 0;
                } else if (directionVector.getYAsInteger() == 1) {
                    x = 0;
                    y = 1;
                }
            } else if (directionVector.getXAsInteger() == -1) {
                if (directionVector.getYAsInteger() == -1) {
                    x = 2;
                    y = 0;
                } else if (directionVector.getYAsInteger() == 0) {
                    x = 0;
                    y = 0;
                } else if (directionVector.getYAsInteger() == 1) {
                    x = 0;
                    y = 2;
                }
            } else if (directionVector.getXAsInteger() == 1) {
                if (directionVector.getYAsInteger() == -1) {
                    x = 0;
                    y = 2;
                } else if (directionVector.getYAsInteger() == 0) {
                    x = 0;
                    y = 2;
                } else if (directionVector.getYAsInteger() == 1) {
                    x = 2;
                    y = 2;
                }
            }

            Vector2Int convertedVector = new Vector2Int(x, y);

            return convertedVector;
        }

        // Reverse what the convertUnitaryVector does.
        private Vector2Int revertConvertUnitaryVector(Vector2Int directionVector) {
            int x = 0, y = 0;

            if (directionVector.getXAsInteger() == 0) {

                if (directionVector.getYAsInteger() == 0) {
                    x = -1;
                    y = -1;
                } else if (directionVector.getYAsInteger() == 1) {
                    x = 0;
                    y = -1;
                } else if (directionVector.getYAsInteger() == 2) {
                    x = 1;
                    y = -1;
                }
            } else if (directionVector.getXAsInteger() == 1) {
                if (directionVector.getYAsInteger() == 0) {
                    x = -1;
                    y = 0;
                } else if (directionVector.getYAsInteger() == 1) {
                    x = 0;
                    y = 0;
                } else if (directionVector.getYAsInteger() == 2) {
                    x = 1;
                    y = 0;
                }
            } else if (directionVector.getXAsInteger() == 2) {
                if (directionVector.getYAsInteger() == 0) {
                    x = -1;
                    y = 1;
                } else if (directionVector.getYAsInteger() == 1) {
                    x = 0;
                    y = 1;
                } else if (directionVector.getYAsInteger() == 2) {
                    x = 1;
                    y = 1;
                }
            }

            return new Vector2Int(x, y);
        }

        // Function to return the direction of fireman
        private Vector2Int firemanDirection() {

            Vector2Int helperDirection, currentPosition = (Vector2Int) myself.getProperty("position");

            if (firemanResponsibleForSalvation != null) {
                ISpaceObject fireman = space.getSpaceObject(firemanResponsibleForSalvation);
                Vector2Int firemanPos = (Vector2Int) fireman.getProperty("position");

                helperDirection = getUnitaryVectorOfDirection(currentPosition, firemanPos);
                return convertUnitaryVector(helperDirection);
            } else return null;

        }

        // Function that returns a vector if there is some kind of best movement ( get closer to fireman ) or null if
        // there are not any priority move.
        private Vector2Int getPriorityMovement(Vector2Int firemanDir, String[][] matrix) {

            Vector2Int direcToReturn = null;
            if (matrix[firemanDir.getXAsInteger()][firemanDir.getYAsInteger()] != "X") {
                direcToReturn = firemanDir;
            } else {
                if (firemanDir.getXAsInteger() != 0) {
                    for (int i = firemanDir.getYAsInteger(); i >= 0; i--) {
                        if (matrix[firemanDir.getXAsInteger()][i] != "X") {
                            direcToReturn = new Vector2Int(firemanDir.getXAsInteger(), i);
                            break;
                        }
                    }
                } else if (firemanDir.getYAsInteger() != 0) {
                    for (int i = firemanDir.getXAsInteger(); i >= 0; i--) {
                        if (matrix[i][firemanDir.getYAsInteger()] != "X") {
                            direcToReturn = new Vector2Int(i, firemanDir.getYAsInteger());
                            break;
                        }
                    }
                }
            }

            if (direcToReturn != null)
                return revertConvertUnitaryVector(direcToReturn);
            else return null;
        }

        // Function that determines if a Person can go to a determined position
        private boolean acceptedMovement(Vector2Int currentPosition, Vector2Int wantedPosition) {
            int spaceWidth = space.getAreaSize().getXAsInteger(),
                    spaceHeight = space.getAreaSize().getYAsInteger();

            if (wantedPosition.getXAsInteger() > spaceWidth - 1 || wantedPosition.getXAsInteger() < 0) {
                return false;
            } else if (wantedPosition.getYAsInteger() > spaceHeight - 1 || wantedPosition.getYAsInteger() < 0) {
                return false;
            } else {
                return true;
            }
        }

        // Function that checks if there is a cell fire on my spot
        private boolean amIOnFire() {
            Collection<ISpaceObject> temp;
            try {
                temp = space.getSpaceObjectsByGridPosition(FiremanBDI.currentPositionOfAnObjectId(space, myself.getId()), "fire");
            } catch (RuntimeException e) {
                temp = null;
            }

            if (temp == null || temp.size() == 0) {
                return false;
            } else {
                return true;
            }

        }
    }

    @Plan(trigger = @Trigger(goals = PersonGoal.class))
    public class CleanPerson{

        private Object idToEliminate;

        public CleanPerson(){};

        public CleanPerson(Object id){
            idToEliminate = id;
        }

        @PlanBody
        protected void saving(PersonGoal SaveGoal) {

            try{
                if (space.destroyAndVerifySpaceObject(idToEliminate)){
                    SaveGoal.changeFiremanHere();
                }else {
                    throw new PlanFailureException();
                }
            } catch (RuntimeException e){
                person.adoptPlan(new SavingPlan());
                throw new PlanFailureException();
            }
        }

        @PlanPassed
        public void passed() {

            System.out.println("A person was saved on: (" + FiremanBDI.currentPositionOfAnObjectId(space, myself.getId()) + ")");
            space.destroySpaceObject(myself.getId());

            System.out.println("[" + currentTime + "] ~~~ I was saved ~~~");
        }
    }
}
