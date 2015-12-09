package processes;

import agents.PersonBDI;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.SimplePropertyObject;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceProcess;
import jadex.extension.envsupport.environment.space2d.Grid2D;


public class UtilProcess extends SimplePropertyObject implements ISpaceProcess {

    private Grid2D space;
    private long initTime;

    @Override
    public void start(IClockService arg0, IEnvironmentSpace arg1) {
        System.out.println("[" + arg0.getTime() + "] Initializing UtilProcess");
    }

    @Override
    public void shutdown(IEnvironmentSpace iEnvironmentSpace){}

    @Override
    public void execute(IClockService iClockService, IEnvironmentSpace iEnvironmentSpace){
        if (iClockService.getTime() - initTime > 100) {

        }
    }

}
