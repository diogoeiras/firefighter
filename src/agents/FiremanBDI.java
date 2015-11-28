package agents;

import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.Belief;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.PlanPassed;
import jadex.bdiv3.annotation.Trigger;
import jadex.bdiv3.runtime.impl.PlanFailureException;
import jadex.extension.envsupport.environment.ISpaceObject;
import jadex.extension.envsupport.environment.space2d.Grid2D;
import jadex.extension.envsupport.math.Vector2Int;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;

import goals.FiremanGoal;
import java.util.Random;

@Agent
public class FiremanBDI {
    @Agent
    protected BDIAgent fireman;

    @Belief
    protected Grid2D space = (Grid2D)fireman.getParentAccess().getExtension("2dspace").get();

    @Belief
    protected ISpaceObject myself = space.getAvatar(fireman.getComponentDescription(), fireman.getModel().getFullName());

    @AgentBody
    public void body(){
        ISpaceObject[] arvoresNoEspaco = space.getSpaceObjectsByType("terrain");

        Random r = new Random();

        int spaceHeight = space.getAreaSize().getXAsInteger(),
            spaceWidth = space.getAreaSize().getYAsInteger(), xPosition = r.nextInt(spaceWidth),
            yPosition = r.nextInt(spaceHeight);

        myself.setProperty("position", new Vector2Int(xPosition,yPosition));
        myself.setProperty("speed",10);

        FiremanGoal Goal = new FiremanGoal(new Vector2Int(0,0));
        Goal.setCurrentPosition(new Vector2Int(xPosition,yPosition));


        // TODO: Eliminate this DEBUG
        fireman.waitForDelay(1500).get();

        FiremanGoal goal = (FiremanGoal) fireman.dispatchTopLevelGoal(Goal).get();

    }

    @Plan(trigger = @Trigger(goals=FiremanGoal.class))
    protected  void changePosition(FiremanGoal goal){
        Vector2Int currentPosition = goal.getCurrentPosition();

        if (currentPosition.getXAsInteger() != goal.getDesiredPosition().getXAsInteger() ||
                currentPosition.getYAsInteger() != goal.getDesiredPosition().getYAsInteger()){

            goal.setCurrentPosition(new Vector2Int(currentPosition.getXAsInteger() - 1 ,currentPosition.getYAsInteger() - 1));
            myself.setProperty("position", new Vector2Int(goal.getCurrentPosition().getXAsInteger(),goal.getCurrentPosition().getYAsInteger()));

        }
    }

}
