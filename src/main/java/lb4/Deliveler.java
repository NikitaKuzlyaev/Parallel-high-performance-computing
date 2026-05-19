package lb4;

public class Deliveler extends Agent {


    public Deliveler(Behaviour behaviour, Map.Node target){
        this.behaviour = behaviour;
        this.target = target;
    }

    @Override
    public void applyAction() {
        super.applyAction();

        if (this.currentPosition == this.target){
            this.isAlive = false;
        }
    }
}
