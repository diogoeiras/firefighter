package processes;

import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.SimplePropertyObject;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceProcess;
import jadex.extension.envsupport.environment.space2d.Space2D;
import jadex.extension.envsupport.math.Vector2Int;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ForestProcess extends SimplePropertyObject implements ISpaceProcess {

    @Override
    public void start(IClockService arg0, IEnvironmentSpace arg1) {

        Space2D space = (Space2D)arg1;

        int spaceHeight = space.getAreaSize().getXAsInteger();
        int spaceWidth = space.getAreaSize().getYAsInteger();


        boolean isFireSet = false;
        for(int i = 0; i < spaceHeight; i++) {
            for(int j = 0; j < spaceWidth; j++) {

                Map properties = new HashMap();
                properties.put("position", new Vector2Int(j, i));

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

    }

    @Override
    public void execute(IClockService iClockService, IEnvironmentSpace iEnvironmentSpace) {

    }


}
