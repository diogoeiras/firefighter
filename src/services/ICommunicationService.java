package services;

import com.sun.corba.se.spi.ior.ObjectKey;
import jadex.extension.envsupport.math.Vector2Int;

public interface ICommunicationService {

    public void RescueMessage(Object senderID, Object receiverID);

    public void RescueConfirmation(Object firemanID, Object personID);

    public void refuseRescueRequest(Object firemanID, Object personID);

    public void sendQuad(Object leaderID, Object firemanID, int quad);

    public void sendConfirmationQuad(Object leaderID, Object firemanID,int quad);
}
