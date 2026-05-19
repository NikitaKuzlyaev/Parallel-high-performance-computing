package lb4;

import java.util.concurrent.ThreadLocalRandom;

public class Bot extends Agent {

    private int ttl;

    public Bot(Behaviour behaviour, int ttl) {
        this.behaviour = behaviour;
        this.isAlive = true;
        this.ttl = ttl;
    }

    @Override
    public void applyAction() {
        super.applyAction();
        this.ttl--;

        if (this.ttl == 0) {
            this.isAlive = false;
        }
    }
}
