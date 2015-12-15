package goals;

import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.GoalRecurCondition;
import jadex.bdiv3.runtime.ChangeEvent;

@Goal(recur=true)
public class PersonGoal {

    private boolean dead, rescued;
    private boolean firemanHere;

    public PersonGoal(){
        this.dead = false;
        this.rescued = false;
        this.firemanHere = false;
    }

    public void changeDeadStatus(){
        this.dead = !this.dead;
    }

    public void changeRescuedStatus(){
        this.rescued = !this.rescued;
    }

    public boolean getRescued(){
        return this.rescued;
    }

    public void changeFiremanHere(){
        this.firemanHere = !this.firemanHere;
    }

    @GoalRecurCondition(beliefs="currentTime")
    public boolean checkRecur(ChangeEvent event) {
        if (dead || rescued || firemanHere) {
            return false;
        } else return true;
    }
}
