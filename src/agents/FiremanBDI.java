package agents;

import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.*;
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


    @Belief(updaterate = 500)
    protected long currentTime = System.currentTimeMillis();

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

        FiremanGoal goal = (FiremanGoal) fireman.dispatchTopLevelGoal(Goal).get();

    }

    @Plan(trigger = @Trigger(goals=FiremanGoal.class))
    public class MovingPlan{
        @PlanBody
        protected  void changePosition(FiremanGoal goal){
            Vector2Int currentPosition = goal.getCurrentPosition();

            if (currentPosition.getXAsInteger() != goal.getDesiredPosition().getXAsInteger() ||
                    currentPosition.getYAsInteger() != goal.getDesiredPosition().getYAsInteger()){

                int difX;
                int difY;

                if(currentPosition.getXAsInteger()==goal.getDesiredPosition().getXAsInteger()){
                    difX = 0;
                }
                else{
                    difX = (goal.getDesiredPosition().getXAsInteger()-currentPosition.getXAsInteger());
                    difX = difX/Math.abs(difX);
                }
                if(currentPosition.getYAsInteger()==goal.getDesiredPosition().getYAsInteger()){
                    difY = 0;
                }
                else{
                    difY = (goal.getDesiredPosition().getYAsInteger()-currentPosition.getYAsInteger());
                    difY = difY/Math.abs(difY);
                }
                goal.setCurrentPosition(new Vector2Int(currentPosition.getXAsInteger() + difX ,currentPosition.getYAsInteger() + difY));
                myself.setProperty("position", new Vector2Int(goal.getCurrentPosition().getXAsInteger(),goal.getCurrentPosition().getYAsInteger()));
                throw new PlanFailureException();
            }
        }

        @PlanPassed
        public void passed() {
            System.out.println("Reached destination");
        }

    }


}
