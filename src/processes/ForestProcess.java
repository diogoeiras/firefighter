package processes;

import agents.FiremanBDI;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.SimplePropertyObject;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceProcess;
import jadex.extension.envsupport.environment.space2d.Space2D;
import jadex.extension.envsupport.math.Vector2Int;

import java.text.DateFormat;
import java.util.Date;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ForestProcess extends SimplePropertyObject implements ISpaceProcess {

    @Override
    public void start(IClockService arg0, IEnvironmentSpace arg1) {

        try {
            DateFormat dateFormat = new SimpleDateFormat("dd_mm_yyyy_HH_mm_ss");
            Date date = new Date();
            String dateForLog = "logs/" + dateFormat.format(date) + ".log";
            System.out.println("Debug file: " + dateForLog);
            System.setOut(new PrintStream(new FileOutputStream( dateForLog, true)));
            System.out.println("[STARTING RUN OF "+ dateForLog + " ]\n\n");
        } catch(FileNotFoundException e){
            System.out.println(e);
            System.out.println("Logging to file is not possible.");
        }

        Space2D space = (Space2D)arg1;

        int spaceHeight = space.getAreaSize().getYAsInteger();
        int spaceWidth = space.getAreaSize().getXAsInteger();

        for(int i = 0; i < spaceWidth; i++) {
            for(int j = 0; j < spaceHeight; j++) {

                Map properties = new HashMap();
                properties.put("position", new Vector2Int(i, j));

                properties.put("type", 1);
                space.createSpaceObject("terrain", properties, null);
            }
        }

        Random rnd = new Random();

        // set fire
        Map properties = new HashMap();
        properties.put("position", new Vector2Int(rnd.nextInt() % spaceWidth, rnd.nextInt() % spaceHeight));

        properties.put("type", 1);
        space.createSpaceObject("fire", properties, null);

    }

    @Override
    public void shutdown(IEnvironmentSpace iEnvironmentSpace) {
        if (iEnvironmentSpace.getSpaceObjectsByType("terrain").length == 0){
            iEnvironmentSpace.removeSpaceProcessType("fireProcess");
            System.out.println("[TERMINATING PROCESS] Fire process ended.");
        }
    }

    @Override
    public void execute(IClockService iClockService, IEnvironmentSpace iEnvironmentSpace) {

    }


}
