package lb4;

public class Agent {

    public boolean isAlive;
    Behaviour behaviour;
    Map.Node target;
    Map.Node currentPosition;
    Map.Node previousPosition;
    Map.Direction moveDirection;
    Map.Node nextPosition;
    boolean backAfterStop;

    public Agent() {
        this.isAlive = true;
    }

    public Map.Node make_action() {
        if (nextPosition != null) {
            return nextPosition;
        }
        nextPosition = behaviour.makeAction(this);
        return nextPosition;
    }

    public void applyAction() {
        if (nextPosition == null || nextPosition == currentPosition) {
            nextPosition = null;
            return;
        }

        moveDirection = Map.Direction.between(currentPosition, nextPosition);
        previousPosition = currentPosition;
        currentPosition = nextPosition;
        nextPosition = null;
    }

    public void setStartPosition(Map.Node startPosition, Map.Direction startDirection) {
        this.currentPosition = startPosition;
        this.moveDirection = startDirection;
    }

    public void setTarget(Map.Node target) {
        this.target = target;
    }

    public char getSymbol() {
        return '?';
    }
}
