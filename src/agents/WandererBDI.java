package agents;

import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.Belief;
import jadex.extension.envsupport.environment.ISpaceObject;
import jadex.extension.envsupport.environment.space2d.Grid2D;
import jadex.extension.envsupport.math.Vector2Int;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;

import java.util.Random;

/**
 * Created by Leonel Araújo on 22/11/2014.
 */

@Agent
public class WandererBDI {
    @Agent
    protected BDIAgent agent;

    @Belief
    protected Grid2D space = (Grid2D)agent.getParentAccess().getExtension("2dspace").get();

    @Belief
    protected ISpaceObject myself = space.getAvatar(agent.getComponentDescription(), agent.getModel().getFullName());


    @AgentBody
    public void body(){
        ISpaceObject[] arvoresNoEspaco = space.getSpaceObjectsByType("terrain");

        Random r = new Random();

        int spaceHeight = space.getAreaSize().getXAsInteger();
        int spaceWidth = space.getAreaSize().getYAsInteger();

        myself.setProperty("position", new Vector2Int(r.nextInt(spaceWidth), r.nextInt(spaceHeight)));

    }

}
