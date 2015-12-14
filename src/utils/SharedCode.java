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

    public void messageFiremanForRescue(final ISpaceObject firemen){
        SServiceProvider.getServices(agent.getServiceProvider(), ICommunicationService.class, RequiredServiceInfo.SCOPE_PLATFORM)
                .addResultListener(new IntermediateDefaultResultListener<ICommunicationService>()
                {
                    public void intermediateResultAvailable(ICommunicationService ts) {
                        ts.RescueMessage(myID,firemen.getId());
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

    public void messageRefuseRescue(final Object firemanID, final Object personId){
        SServiceProvider.getServices(agent.getServiceProvider(), ICommunicationService.class, RequiredServiceInfo.SCOPE_PLATFORM)
                .addResultListener(new IntermediateDefaultResultListener<ICommunicationService>()
                {
                    public void intermediateResultAvailable(ICommunicationService ts) {
                        ts.refuseRescueRequest(myID,firemanID);
                    }
                });
    }

    public void sendQuads(final Object leaderID, final Object firemanID,final int quad){
        SServiceProvider.getServices(agent.getServiceProvider(), ICommunicationService.class, RequiredServiceInfo.SCOPE_PLATFORM)
                .addResultListener(new IntermediateDefaultResultListener<ICommunicationService>()
                {
                    public void intermediateResultAvailable(ICommunicationService ts) {
                        ts.sendQuad(leaderID,firemanID,quad);
                    }
                });
    }

    public void confirmationQuads(final Object leaderID, final Object firemanID, final int quad){
        SServiceProvider.getServices(agent.getServiceProvider(), ICommunicationService.class, RequiredServiceInfo.SCOPE_PLATFORM)
                .addResultListener(new IntermediateDefaultResultListener<ICommunicationService>()
                {
                    public void intermediateResultAvailable(ICommunicationService ts) {
                        ts.sendConfirmationQuad(leaderID,firemanID,quad);
                    }
                });
    }
}
