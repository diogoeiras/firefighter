package goals;

import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.GoalRecurCondition;
import jadex.bdiv3.runtime.ChangeEvent;

@Goal(recur=true)
public class PersonGoal {

    private boolean dead, rescued;

    public PersonGoal(){
        this.dead = false;
        this.rescued = false;
    }

    public void changeDeadStatus(){
        this.dead = !this.dead;
    }

    public void changeRescuedStatus(){
        this.rescued = !this.rescued;
    }

    @GoalRecurCondition(beliefs="currentTime")
    public boolean checkRecur(ChangeEvent event) {
        if (dead || rescued) {
            return false;
        } else return true;
    }
}
