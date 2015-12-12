package utils;

import jadex.bdiv3.BDIAgent;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.commons.future.IntermediateDefaultResultListener;
import jadex.extension.envsupport.environment.ISpaceObject;
import services.ICommunicationService;

public class SharedCode {

    BDIAgent agent;
    Object myID;

    public SharedCode(BDIAgent agent, Object myID){
        this.agent = agent;
        this.myID = myID;
    };

    public void messageFiremanForRescue(final Object[] firemen){
        SServiceProvider.getServices(agent.getServiceProvider(), ICommunicationService.class, RequiredServiceInfo.SCOPE_PLATFORM)
                .addResultListener(new IntermediateDefaultResultListener<ICommunicationService>()
                {
                    public void intermediateResultAvailable(ICommunicationService ts) {
                        Object fid = null;
                        Object[] test = firemen;
                        if (test != null && test.length > 0){
                            fid = ((ISpaceObject) test[0]).getId();
                        }
                        ts.RescueMessage(myID,fid);
                    }
                });
    }

    public void messageRescueConfirmationForPerson(final Object firemanID, final Object personID){
        SServiceProvider.getServices(agent.getServiceProvider(), ICommunicationService.class, RequiredServiceInfo.SCOPE_PLATFORM)
                .addResultListener(new IntermediateDefaultResultListener<ICommunicationService>()
                {
                    public void intermediateResultAvailable(ICommunicationService ts) {
                        ts.RescueConfirmation(firemanID,personID);
                    }
                });
    }


}
