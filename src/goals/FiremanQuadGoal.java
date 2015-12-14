package goals;

import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.GoalRecurCondition;
import jadex.bdiv3.runtime.ChangeEvent;
import jadex.extension.envsupport.math.Vector2Int;

@Goal(recur=true)
public class FiremanQuadGoal {
    protected Vector2Int desiredPosition;

    protected Vector2Int currentPosition;

    protected boolean noMoreFireCells = false;

    protected int QUAD_AREA;

    public FiremanQuadGoal(Vector2Int desiredPosition){
        this.desiredPosition = desiredPosition;
        QUAD_AREA = -1;
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

    public void setQuad(int i){
        QUAD_AREA = i;
    }

    public int getQuad(){
        return QUAD_AREA;
    }

    @GoalRecurCondition(beliefs="currentTime")
    public boolean checkRecur(ChangeEvent event) {
        if (noMoreFireCells) {
            return false;
        } else return true;
    }

    public void changeNoMoreFireCells(){
        this.noMoreFireCells = !this.noMoreFireCells;
    }

}
