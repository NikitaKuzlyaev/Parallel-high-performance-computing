package lb4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Map {

    public List<Node> crossings;
    public List<Node> botSpawnPoints;
    public List<Node> agentSpawnPoints;
    public List<Node> targetPoints;
    public List<Node> graph;

    private Node[][] matrix;

    static class Node {

        public boolean isCrossing; // является ли перекрестком
        private final int type;
        private int x;
        private int y;
        private final List<Node> neighbors;

        public Node(int type, int x, int y) {
            this.type = type;
            this.neighbors = new ArrayList<>();
            this.x = x;
            this.y = y;
        }

        public Node getNeighborByDirection(Direction dir) {
            int[] dxdy = Util.castDirection2vector(dir);

            int nx = this.x + dxdy[0];
            int ny = this.y + dxdy[1];

            for (Node u : this.neighbors) {
                if (u.x == nx && u.y == ny) {
                    return u;
                }
            }
            return null;
        }
    }

    public void compile(String filepath) {

        // 1. считать карту из файла
        this.matrix = readMapFromFile(filepath);
        findKeyPoints();
        buildRoads();

        // 3. запустить bfs от перекрустков чтобы для каждой точки определить куда можно двигаться при спавне

    }

    private Node[][] readMapFromFile(String filepath) {
        Path path = Path.of(filepath);

        try {
            List<String> lines = Files.readAllLines(path);

            String firstLine = lines.get(0);
            String[] parts = firstLine.trim().split("\\s+");

            int n = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);

            Node[][] matrix = new Node[n][m];

            for (int i = 1; i <= n; i++) {
                String line = lines.get(i);
                parts = line.trim().split("\\s+");

                assert parts.length == m; // Должно быть m чисел

                for (int j = 0; j < m; j++) {
                    int type = Integer.parseInt(parts[j]);
                    Node node = new Node(type, i - 1, j);
                    matrix[i - 1][j] = node;
                }
            }
            return matrix;
        } catch (IOException ex) {
            System.out.println("Ошибка чтения файла " + ex.getMessage());
            throw new RuntimeException();
        }
    }

    private void findKeyPoints() {
        int n = this.matrix.length;
        int m = this.matrix[0].length;

        for (int x = 0; x < n; x++) {
            for (int y = 0; y < m; y++) {
                Node thisNode = this.matrix[x][y];

                if (thisNode.type == 1) {
                    continue;
                }

                switch (thisNode.type) {
                    case 2 -> this.agentSpawnPoints.add(thisNode);
                    case 3 -> this.botSpawnPoints.add(thisNode);
                    case 4 -> this.targetPoints.add(thisNode);
                }

                int neighbors = 0;

                for (int k = 0; k < Util.directions.length; k++) {
                    int dx = Util.directions[k][0];
                    int dy = Util.directions[k][1];

                    if (x + dx >= 0 && x + dx < n && y + dy >= 0 && y + dy < m) {
                        Node otherNode = this.matrix[x + dx][y + dy];
                        if (otherNode.type != 1) {
                            neighbors++;
                        }
                    }
                }

                if (neighbors > 2) {
                    thisNode.isCrossing = true;
                    this.crossings.add(thisNode);
                }
            }
        }

    }

    private void buildRoads() {
        int n = this.matrix.length;
        int m = this.matrix[0].length;

        for (int x = 0; x < n; x++) {
            for (int y = 0; y < m; y++) {

                Node thisNode = this.matrix[x][y];

                if (thisNode.type == 1) {
                    continue;
                }

                for (int k = 0; k < Util.directions.length; k++) {
                    int dx = Util.directions[k][0];
                    int dy = Util.directions[k][1];

                    if (x + dx >= 0 && x + dx < n && y + dy >= 0 && y + dy < m) {
                        Node otherNode = this.matrix[x + dx][y + dy];
                        if (otherNode.type != 1) {
                            thisNode.neighbors.add(otherNode);
                        }
                    }
                }
            }
        }
    }

    public

    static class Util {
        public static final int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        public static int[] castDirection2vector(Direction dir) {
            switch (dir) {
                case UP -> {
                    return directions[3];
                }
                case DOWN -> {
                    return directions[2];
                }
                case LEFT -> {
                    return directions[1];
                }
                case RIGHT -> {
                    return directions[0];
                }
            }
            return directions[0];
        }

    }

    enum Direction {
        UP, DOWN, LEFT, RIGHT;

        private static final Direction[] DIRS = values();

        public Direction turn(int r) {
            int next = Math.floorMod(this.ordinal() + r, DIRS.length);
            return DIRS[next];
        }
    }
}
