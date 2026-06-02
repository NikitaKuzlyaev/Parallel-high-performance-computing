package lb4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Map {

    public List<Node> crossings;
    public List<Node> botSpawnPoints;
    public List<Node> agentSpawnPoints;
    public List<Node> targetPoints;
    public List<Node> graph;

    private Node[][] matrix;

    public Map() {
        crossings = new ArrayList<>();
        botSpawnPoints = new ArrayList<>();
        agentSpawnPoints = new ArrayList<>();
        targetPoints = new ArrayList<>();
        graph = new ArrayList<>();
    }

    static class Node {

        public boolean isCrossing; // является ли перекрестком
        private final int type;
        private int x;
        private int y;
        private final List<Node> neighbors;

        public Node(int type, int x, int y) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.neighbors = new ArrayList<>();
        }

        public Node getNeighborByDirection(Direction direction) {
            int nx = x + direction.dx;
            int ny = y + direction.dy;

            for (Node node : neighbors) {
                if (node.x == nx && node.y == ny) {
                    return node;
                }
            }
            return null;
        }
    }

    public void compile(String filepath) {
        matrix = readMapFromFile(filepath);
        buildRoads();
        findKeyPoints();
    }

    public Direction getRandomDirection(Node node) {
        Node next = node.neighbors.get(ThreadLocalRandom.current().nextInt(node.neighbors.size()));
        return Direction.between(node, next);
    }

    public String asText(Collection<Agent> agents) {
        char[][] chars = new char[matrix.length][matrix[0].length];

        for (int x = 0; x < matrix.length; x++) {
            for (int y = 0; y < matrix[0].length; y++) {
                Node node = matrix[x][y];
                chars[x][y] = switch (node.type) {
                    case 1 -> '#';
                    case 2 -> 'S';
                    case 3 -> 'P';
                    case 4 -> 'T';
                    default -> '.';
                };
            }
        }

        for (Agent agent : agents) {
            if (agent.isAlive && agent.currentPosition != null) {
                chars[agent.currentPosition.x][agent.currentPosition.y] = agent.getSymbol();
            }
        }

        StringBuilder builder = new StringBuilder();
        for (char[] row : chars) {
            for (char cell : row) {
                builder.append(cell);
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    public String baseMapText() {
        return asText(List.of());
    }

    private Node[][] readMapFromFile(String filepath) {
        Path path = Path.of(filepath);

        try {
            List<String> lines = Files.readAllLines(path);
            String[] parts = lines.get(0).trim().split("\\s+");

            int n = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            Node[][] result = new Node[n][m];

            for (int i = 1; i <= n; i++) {
                parts = lines.get(i).trim().split("\\s+");
                for (int j = 0; j < m; j++) {
                    int type = Integer.parseInt(parts[j]);
                    result[i - 1][j] = new Node(type, i - 1, j);
                }
            }
            return result;
        } catch (IOException ex) {
            System.out.println("Ошибка чтения файла " + ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    private void findKeyPoints() {
        crossings.clear();
        botSpawnPoints.clear();
        agentSpawnPoints.clear();
        targetPoints.clear();
        graph.clear();

        for (Node[] row : matrix) {
            for (Node node : row) {
                if (node.type == 1) {
                    continue;
                }

                graph.add(node);

                switch (node.type) {
                    case 2 -> agentSpawnPoints.add(node);
                    case 3 -> botSpawnPoints.add(node);
                    case 4 -> targetPoints.add(node);
                }

                if (node.neighbors.size() > 2) {
                    node.isCrossing = true;
                    crossings.add(node);
                }
            }
        }
    }

    private void buildRoads() {
        for (int x = 0; x < matrix.length; x++) {
            for (int y = 0; y < matrix[0].length; y++) {
                Node node = matrix[x][y];

                if (node.type == 1) {
                    continue;
                }

                for (Direction direction : Direction.values()) {
                    int nx = x + direction.dx;
                    int ny = y + direction.dy;

                    if (nx >= 0 && nx < matrix.length && ny >= 0 && ny < matrix[0].length) {
                        Node otherNode = matrix[nx][ny];
                        if (otherNode.type != 1) {
                            node.neighbors.add(otherNode);
                        }
                    }
                }
            }
        }
    }

    enum Direction {
        UP(-1, 0),
        DOWN(1, 0),
        LEFT(0, -1),
        RIGHT(0, 1);

        final int dx;
        final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        public Direction left() {
            return switch (this) {
                case UP -> LEFT;
                case DOWN -> RIGHT;
                case LEFT -> DOWN;
                case RIGHT -> UP;
            };
        }

        public Direction right() {
            return switch (this) {
                case UP -> RIGHT;
                case DOWN -> LEFT;
                case LEFT -> UP;
                case RIGHT -> DOWN;
            };
        }

        public Direction opposite() {
            return switch (this) {
                case UP -> DOWN;
                case DOWN -> UP;
                case LEFT -> RIGHT;
                case RIGHT -> LEFT;
            };
        }

        public static Direction between(Node from, Node to) {
            int dx = to.x - from.x;
            int dy = to.y - from.y;

            for (Direction direction : values()) {
                if (direction.dx == dx && direction.dy == dy) {
                    return direction;
                }
            }
            return RIGHT;
        }
    }
}
