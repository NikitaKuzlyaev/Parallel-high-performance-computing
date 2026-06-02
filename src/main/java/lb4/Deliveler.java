package lb4;

public class Deliveler extends Agent {

    public Deliveler(Behaviour behaviour, Map.Node target, Map.Node startPosition, Map.Direction startDirection) {
        this.behaviour = behaviour;
        this.target = target;
        this.isAlive = true;
        setStartPosition(startPosition, startDirection);
    }

    @Override
    public void applyAction() {
        super.applyAction();

        if (currentPosition == target) {
            isAlive = false;
        }
    }

    @Override
    public char getSymbol() {
        return 'A';
    }
}
