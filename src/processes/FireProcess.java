package processes;

import agents.FiremanBDI;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.SimplePropertyObject;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceObject;
import jadex.extension.envsupport.environment.ISpaceProcess;
import jadex.extension.envsupport.environment.space2d.Grid2D;
import jadex.extension.envsupport.math.Vector2Double;

import java.util.*;


public class FireProcess extends SimplePropertyObject implements ISpaceProcess {

    Grid2D space;
    ISpaceObject[] fireElement;
    long initTime;
    int spaceHeight , spaceWidth;
    boolean leadershipNeeded = true;
    public static Object LEADER_ID;

    // Random variable
    Random rnd = new Random();

    // TEMPORARY LOCAL FOR VARIABLES
    String direction = "N", secondDirection;
    double Kmh = 20.0, humidity = 95;
    int cellsToFire = 1;

    @Override
    public void start(IClockService arg0, IEnvironmentSpace arg1) {
        System.out.println("[" + arg0.getTime() + "] Initializing FireProcess");

        space = (Grid2D)arg1;
        spaceHeight = space.getAreaSize().getYAsInteger();
        spaceWidth = space.getAreaSize().getXAsInteger();


        System.out.println("[" + arg0.getTime() + "] Getting Fire first element");

        fireElement = space.getSpaceObjectsByType("fire");

        System.out.println("[" + arg0.getTime() + "] Initialize clock.");
        initTime = arg0.getTime();
    }

    @Override
    public void shutdown(IEnvironmentSpace iEnvironmentSpace) {
        System.out.println("[TERMINATED] Fire Process consumed everything or got extinguished");
    }

    @Override
    public void execute(IClockService iClockService, IEnvironmentSpace iEnvironmentSpace) {

        if (iClockService.getTime() - initTime > 300) {

            if (FiremanBDI.BEHAVIOR == "QUAD" && leadershipNeeded) {
                ISpaceObject[] fireman = space.getSpaceObjectsByType("fireman");
                if (fireman != null && fireman.length > 0) {
                    LEADER_ID = fireman[rnd.nextInt(fireman.length)].getId();
                    leadershipNeeded = false;
                }
            }

            ISpaceObject[] terrain = space.getSpaceObjectsByType("terrain");
            if ( terrain.length == 0 || terrain == null){
                shutdown(iEnvironmentSpace);
            }

            cellsToFire = 1;
            fireElement = space.getSpaceObjectsByType("fire");

            for (int i = 0; i < fireElement.length; i++) {

                Vector2Double pos = (Vector2Double) fireElement[i].getProperty("position");

                /*
                    Probability :
                            Direction -> 70% of getting to that direction & 10% for other each direction.
                            Humidity -> (1 - %humidity) of going to another direction.
                            Wind -> if (Kmh > 30) 30% of advancing 2 cells instead of one.

                 */

                // Getting direction
                setFireDirection(rnd.nextDouble());

                // Getting humidity
                setChanceOfAnotherSpread(rnd.nextDouble());

                // Getting wind
                setCellsToFire(rnd.nextDouble());

                // Unitary vectors of adding fire cells.
                ArrayList<Vector2Double> unitaryVectors = getNewPositionsForFireCells();

                for (int j = 0; j < unitaryVectors.size(); j++) {
                    Vector2Double newPos = new Vector2Double(pos.getXAsInteger() + unitaryVectors.get(j).getXAsInteger()
                            , pos.getYAsInteger() + unitaryVectors.get(j).getYAsInteger());
                    if (newPos.getXAsInteger() >= 0 && newPos.getXAsInteger() < spaceHeight
                            && newPos.getYAsInteger() >= 0 && newPos.getYAsInteger() < spaceWidth) {
                            createFireCell(newPos);
                    }
                }
            }
            initTime = iClockService.getTime();
        }
    }

    // Create a cell of fire in position Pos in a space
    public void createFireCell(Vector2Double Pos) {

        ArrayList<ISpaceObject> terrainObjects = (ArrayList)space.getSpaceObjectsByGridPosition(Pos,"terrain");
        ArrayList<ISpaceObject> fireObjects = (ArrayList)space.getSpaceObjectsByGridPosition(Pos,"fire");
        ArrayList<ISpaceObject> wetObjects = (ArrayList)space.getSpaceObjectsByGridPosition(Pos,"wetTerrain");

        if (wetObjects == null || wetObjects.size() == 0){
            if (fireObjects == null){
                if (terrainObjects != null) {
                    for (int i = 0; i < terrainObjects.size(); i++) {
                        if (terrainObjects.get(i) != null)
                            space.destroySpaceObject(terrainObjects.get(i).getId());
                    }

                    Map properties = new HashMap();
                    properties.put("type", 1);
                    properties.put("position", Pos);
                    space.createSpaceObject("fire", properties, null);
                }
            }
        }
    }

    // Retrieve a list of cardinal points without the one set as wind direction.
    public ArrayList<String> retrieveDirectionList(){
        ArrayList<String> possibleDirections = new ArrayList<String>();

        if (direction != "N")
            possibleDirections.add("N");
        if (direction != "S")
            possibleDirections.add("S");
        if (direction != "W")
            possibleDirections.add("W");
        if (direction != "E")
            possibleDirections.add("E");

        return possibleDirections;
    }

    // Set variable "direction" to a cardinal point
    public void setFireDirection(Double randomValue){

        ArrayList<String> directionList = retrieveDirectionList();

        if (randomValue > 0.7 && randomValue < 0.8){
            direction = directionList.get(0);
        } else if (randomValue > 0.8 && randomValue < 0.9){
            direction = directionList.get(1);
        } else if (randomValue > 0.9 && randomValue <= 1.0){
            direction = directionList.get(2);
        } else {
            // direction mantains.
            // random was between 0 and 0.7 getting 70% chance.
        }
    }

    // Set other direction of spreading if applicable
    public void setChanceOfAnotherSpread(Double randomValue){
        if (randomValue < (1 - humidity )) {
            ArrayList<String> directions = retrieveDirectionList();

            secondDirection = directions.get(rnd.nextInt(4));
        }
    }

    // Increase variable "cellsToFire" if necessary.
    public void setCellsToFire(Double randomValue){
        if (Kmh > 30 && randomValue <= 0.3){
            cellsToFire++;
        }
    }

    // Set arrayList of positions for new fire cells
    public ArrayList<Vector2Double> getNewPositionsForFireCells(){
        ArrayList<Vector2Double> positionToReturn = new ArrayList<Vector2Double>(){};

        // Add default direction
        positionToReturn.add(getPositionDirection(direction,1));

        // Add secondary direction in case of humidity
        if (secondDirection != null){
            positionToReturn.add(getPositionDirection(secondDirection,1));
        }

        // Add another cell to default direction in case of wind.
        if (cellsToFire > 1){
            positionToReturn.add(getPositionDirection(direction,2));
        }

        return positionToReturn;
    }

    // Get a Vector2Double according to a direction. Multiplier to the case we need various cells
    public Vector2Double getPositionDirection(String direc, int multiplier){
        Vector2Double pos;
        switch (direc){
            case "N":
                pos = new Vector2Double(0,1*multiplier);
                break;
            case "S":
                pos = new Vector2Double(0,-1*multiplier);
                break;
            case "E":
                pos = new Vector2Double(1*multiplier,0);
                break;
            case "W":
                pos = new Vector2Double(-1*multiplier,0);
                break;
            default:
                pos = null;
        }
        return pos;
    }
}
