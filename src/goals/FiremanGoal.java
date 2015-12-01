package goals;

import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.GoalRecurCondition;
import jadex.bdiv3.runtime.ChangeEvent;
import jadex.extension.envsupport.math.Vector2Int;

@Goal(recur=true)
public class FiremanGoal {

    protected Vector2Int desiredPosition;

    protected Vector2Int currentPosition;

    public FiremanGoal(Vector2Int desiredPosition){
        this.desiredPosition = desiredPosition;
    }

    public Vector2Int getCurrentPosition(){
        return this.currentPosition;
    }

    public Vector2Int getDesiredPosition(){
        return this.desiredPosition;
    }

    public void setCurrentPosition(Vector2Int currentPosition){
        this.currentPosition = currentPosition;
    }

    public void setDesiredPosition(Vector2Int desiredPosition){
        this.desiredPosition = desiredPosition;
    }

    @GoalRecurCondition(beliefs="currentTime")
    public boolean checkRecur(ChangeEvent event) {
        if(currentPosition != desiredPosition)
            return true;
        else
            return false;
    }
}
