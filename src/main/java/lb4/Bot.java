package lb4;

public class Bot extends Agent {

    private int ttl;

    public Bot(Behaviour behaviour, int ttl, Map.Node startPosition, Map.Direction startDirection) {
        this.behaviour = behaviour;
        this.ttl = ttl;
        this.isAlive = true;
        setStartPosition(startPosition, startDirection);
    }

    @Override
    public void applyAction() {
        super.applyAction();
        ttl--;

        if (ttl <= 0) {
            isAlive = false;
        }
    }

    @Override
    public char getSymbol() {
        return 'B';
    }
}
