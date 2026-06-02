package lb4;

import lb4.Map.Direction;
import lb4.Map.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Behaviour {

    public enum Type {
        BOT,
        RIGHT_HAND,
        STRAIGHT,
        LEFT_HAND
    }

    private final Type type;
    private final String name;

    private Behaviour(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    public static Behaviour botBehaviour() {
        return new Behaviour(Type.BOT, "bots");
    }

    public static Behaviour agentBehaviour(Type type) {
        // три стратегии прохождения перекрестка
        return switch (type) {
            case RIGHT_HAND -> new Behaviour(type, "right_hand");
            case STRAIGHT -> new Behaviour(type, "straight");
            case LEFT_HAND -> new Behaviour(type, "left_hand");
            default -> new Behaviour(Type.STRAIGHT, "straight");
        };
    }

    public Node makeAction(Agent agent) {
        if (agent.currentPosition == null) {
            return null;
        }

        if (agent.backAfterStop) {
            agent.backAfterStop = false;
            return getBackNode(agent);
        }

        if (agent.currentPosition.isCrossing) {
            return makeCrossingAction(agent);
        }

        return makeRoadAction(agent);
    }

    public String getName() {
        return name;
    }

    private Node makeCrossingAction(Agent agent) {

        if (type == Type.BOT) {
            if (ThreadLocalRandom.current().nextDouble() < 0.10) {
                agent.backAfterStop = true;
                return agent.currentPosition;
            }
            return chooseRandomExit(agent);
        }

        if (ThreadLocalRandom.current().nextDouble() < 0.05) {
            agent.backAfterStop = true;
            return agent.currentPosition;
        }


        return switch (type) {
            case RIGHT_HAND -> chooseByWeights(agent, 0.40, 0.30, 0.25);
            case LEFT_HAND -> chooseByWeights(agent, 0.25, 0.30, 0.40);
            case STRAIGHT -> chooseByWeights(agent, 0.20, 0.55, 0.20);
            default -> chooseRandomExit(agent);
        };
    }

    private Node makeRoadAction(Agent agent) {

        Node forward = agent.currentPosition.getNeighborByDirection(agent.moveDirection);
        if (forward != null && forward != agent.previousPosition) {
            return forward;
        }

        List<Node> exits = getExits(agent, false);
        if (!exits.isEmpty()) {
            return exits.get(ThreadLocalRandom.current().nextInt(exits.size()));
        }

        return getBackNode(agent);
    }

    private Node chooseByWeights(Agent agent, double rightWeight, double straightWeight, double leftWeight) {
        List<WeightedNode> variants = new ArrayList<>();
        addVariant(variants, agent, agent.moveDirection.right(), rightWeight);
        addVariant(variants, agent, agent.moveDirection, straightWeight);
        addVariant(variants, agent, agent.moveDirection.left(), leftWeight);

        if (variants.isEmpty()) {
            return chooseRandomExit(agent);
        }

        double totalWeight = 0;
        for (WeightedNode variant : variants) {
            totalWeight += variant.weight;
        }

        double point = ThreadLocalRandom.current().nextDouble(totalWeight);
        for (WeightedNode variant : variants) {
            point -= variant.weight;
            if (point <= 0) {
                return variant.node;
            }
        }

        return variants.get(variants.size() - 1).node;
    }

    private void addVariant(List<WeightedNode> variants, Agent agent, Direction direction, double weight) {
        Node node = agent.currentPosition.getNeighborByDirection(direction);
        if (node != null && node != agent.previousPosition) {
            variants.add(new WeightedNode(node, weight));
        }
    }

    private Node chooseRandomExit(Agent agent) {
        List<Node> exits = getExits(agent, false);
        if (exits.isEmpty()) {
            return getBackNode(agent);
        }
        return exits.get(ThreadLocalRandom.current().nextInt(exits.size()));
    }

    private List<Node> getExits(Agent agent, boolean allowBack) {
        List<Node> exits = new ArrayList<>();

        for (Node node : agent.currentPosition.neighbors) {
            if (allowBack || node != agent.previousPosition) {
                exits.add(node);
            }
        }
        return exits;
    }

    private Node getBackNode(Agent agent) {
        if (agent.previousPosition != null) {
            return agent.previousPosition;
        }

        Node backward = agent.currentPosition.getNeighborByDirection(agent.moveDirection.opposite());
        if (backward != null) {
            return backward;
        }

        if (!agent.currentPosition.neighbors.isEmpty()) {
            return agent.currentPosition.neighbors.get(0);
        }

        return agent.currentPosition;
    }

    private static class WeightedNode {
        Node node;
        double weight;

        WeightedNode(Node node, double weight) {
            this.node = node;
            this.weight = weight;
        }
    }
}
