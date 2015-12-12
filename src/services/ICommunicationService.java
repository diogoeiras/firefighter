package services;

import jadex.extension.envsupport.math.Vector2Int;

public interface ICommunicationService {

    public void RescueMessage(Object senderID, Object receiverID);

    public void RescueConfirmation(Object firemanID, Object personID);

}
