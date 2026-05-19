package lb4;

import lb4.Map.*;

import java.util.concurrent.ThreadLocalRandom;

public class Behaviour {


    public Node makeAction(Agent agent) {

        if (agent.currentPosition.isCrossing) {
            // 10 % на разворот
            if (ThreadLocalRandom.current().nextDouble() < 0.1d) {
                agent.moveDirection.turn(2);
                return;
            }


            // иначе

        }

        Direction curDir = agent.moveDirection;
        for (int i = 0; i < Direction.values().length; i++) {
            Node nextPosition = agent.currentPosition.getNeighborByDirection(curDir);

            if (nextPosition != null && nextPosition != agent.previousPosition) {
                return nextPosition;
            }
            curDir = curDir.turn(1);
        }


        return null;
    }

}
