package lb4;

public class Agent {

    public boolean isAlive;
    Behaviour behaviour;
    Map.Node target;
    Map.Node currentPosition;
    Map.Node previousPosition;

    public Agent(){
        this.isAlive = true;
    }

    public void make_action(){
        Map.Node nextNode = behaviour.makeAction(this);
    }

    public void setTarget(){

    }

}
