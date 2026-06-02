package lb4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class Pipeline implements Runnable {

    private static final int MAX_STEPS = 2000;
    private static final int EPISODES_PER_SERIES = 100;

    private final String mapPath;
    private final Path outputPath;
    private final Behaviour botBehaviour;
    private final ExecutorService executor;

    private Map map;

    public Pipeline() {
        this("citymap.txt", "output");
    }

    public Pipeline(String mapPath, String outputPath) {
        this.mapPath = mapPath;
        this.outputPath = Path.of(outputPath);
        this.botBehaviour = Behaviour.botBehaviour();
        this.executor = Executors.newFixedThreadPool(2);
    }

    @Override
    public void run() {
        try {
            Files.createDirectories(outputPath.resolve("frames"));

            map = new Map();
            map.compile(mapPath);
            Files.writeString(outputPath.resolve("map.txt"), map.baseMapText());

            List<ResultRow> results = runExperiments();
            writeJson(results);
            writeDemoFrames();

            System.out.println("Сохранение результатов ->" + outputPath.resolve("results.json"));
            System.out.println("Сохранение кадров ->" + outputPath.resolve("frames"));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            executor.shutdown();
        }
    }

    private List<ResultRow> runExperiments() {
        double[] botSpawnChances = {0.01, 0.02, 0.05, 0.08, 0.10};

        Behaviour.Type[] strategies = {
                Behaviour.Type.RIGHT_HAND,
                Behaviour.Type.STRAIGHT,
                Behaviour.Type.LEFT_HAND
        };

        List<ResultRow> results = new ArrayList<>();

        for (Behaviour.Type strategy : strategies) {
            Behaviour agentBehaviour = Behaviour.agentBehaviour(strategy);

            for (double spawnChance : botSpawnChances) {
                int successCount = 0;
                int totalSteps = 0;

                for (int i = 0; i < EPISODES_PER_SERIES; i++) {
                    EpisodeResult result = playEpisode(agentBehaviour, spawnChance, false);

                    if (result.success) {
                        successCount++;
                        totalSteps += result.steps;
                    }
                }

                double averageSteps = 0;
                if (successCount > 0) {
                    averageSteps = (double) totalSteps / successCount;
                }

                double successRate = (double) successCount / EPISODES_PER_SERIES;

                results.add(new ResultRow(
                        agentBehaviour.getName(),
                        spawnChance,
                        averageSteps,
                        successRate
                ));
            }
        }

        return results;
    }

    private EpisodeResult playEpisode(Behaviour agentBehaviour, double botSpawnChance, boolean saveFrames) {
        Episode episode = new Episode(botSpawnChance);

        Map.Node start = randomNode(map.agentSpawnPoints);
        Map.Node target = randomNode(map.targetPoints);
        Map.Direction direction = map.getRandomDirection(start);

        Deliveler agent = new Deliveler(agentBehaviour, target, start, direction);

        episode.deliveryAgent = agent;
        episode.activeAgents.add(agent);

        if (saveFrames) {
            saveFrame(episode, 0);
        }

        int steps = 0;

        while (agent.isAlive && steps < MAX_STEPS) {
            playStep(episode);
            steps++;

            if (saveFrames) {
                saveFrame(episode, steps);
            }
        }

        boolean success = agent.currentPosition == agent.target;
        return new EpisodeResult(success, steps);
    }

    private void playStep(Episode episode) {
        spawnBots(episode);

        List<Agent> agents = new ArrayList<>(episode.activeAgents);

        try {
            executor.submit(() -> episode.deliveryAgent.make_action()).get();

            executor.submit(() -> {
                for (Agent agent : agents) {
                    if (agent instanceof Bot && agent.isAlive) {
                        agent.make_action();
                    }
                }
            }).get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }

        Set<Agent> crashed = checkCollisions(episode);
        for (Agent agent : crashed) {
            agent.isAlive = false;
        }

        List<Agent> aliveAgents = new ArrayList<>();

        for (Agent agent : episode.activeAgents) {
            if (agent.isAlive) {
                agent.applyAction();
            }

            if (agent.isAlive) {
                aliveAgents.add(agent);
            }
        }

        episode.activeAgents = aliveAgents;
    }

    private void spawnBots(Episode episode) {
        for (Map.Node point : map.botSpawnPoints) {
            double value = ThreadLocalRandom.current().nextDouble();

            if (value < episode.botSpawnChance) {
                int ttl = 15 + ThreadLocalRandom.current().nextInt(136);
                Map.Direction direction = map.getRandomDirection(point);

                Bot bot = new Bot(botBehaviour, ttl, point, direction);
                episode.activeAgents.add(bot);
            }
        }
    }

    private Set<Agent> checkCollisions(Episode episode) {
        Set<Agent> crashed = new HashSet<>();

        java.util.Map<Map.Node, List<Agent>> nextPositions = new HashMap<>();

        for (Agent agent : episode.activeAgents) {
            nextPositions
                    .computeIfAbsent(agent.nextPosition, key -> new ArrayList<>())
                    .add(agent);
        }

        for (List<Agent> agents : nextPositions.values()) {
            if (agents.size() > 1) {
                crashed.addAll(agents);
            }
        }

        for (int i = 0; i < episode.activeAgents.size(); i++) {
            for (int j = i + 1; j < episode.activeAgents.size(); j++) {
                Agent first = episode.activeAgents.get(i);
                Agent second = episode.activeAgents.get(j);

                boolean swappedPlaces =
                        first.nextPosition == second.currentPosition
                                && second.nextPosition == first.currentPosition;

                if (swappedPlaces) {
                    crashed.add(first);
                    crashed.add(second);
                }
            }
        }

        return crashed;
    }

    private void writeJson(List<ResultRow> results) throws IOException {
        StringBuilder json = new StringBuilder();

        json.append("{\n");
        json.append("  \"episodesPerSeries\": ").append(EPISODES_PER_SERIES).append(",\n");
        json.append("  \"maxSteps\": ").append(MAX_STEPS).append(",\n");
        json.append("  \"results\": [\n");

        for (int i = 0; i < results.size(); i++) {
            ResultRow row = results.get(i);

            json.append("    {\n");
            json.append("      \"strategy\": \"").append(row.strategy).append("\",\n");
            json.append("      \"botSpawnChance\": ").append(format(row.botSpawnChance)).append(",\n");
            json.append("      \"averageSteps\": ").append(format(row.averageSteps)).append(",\n");
            json.append("      \"successRate\": ").append(format(row.successRate)).append("\n");
            json.append("    }");

            if (i + 1 < results.size()) {
                json.append(",");
            }

            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        Files.writeString(outputPath.resolve("results.json"), json.toString());
    }

    private void writeDemoFrames() throws IOException {
        Path framesPath = outputPath.resolve("frames");

        try (var files = Files.list(framesPath)) {
            files.forEach(file -> {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }

        Behaviour demoBehaviour = Behaviour.agentBehaviour(Behaviour.Type.RIGHT_HAND);
        playEpisode(demoBehaviour, 0.05, true);
    }

    private void spawnBots(Episode episode) {

        for (int i = 0; i < this.map.botSpawnPoints.size(); i++) {
            if (ThreadLocalRandom.current().nextDouble() < episode.propBotSpawn) {
                int ttl = 15 + ThreadLocalRandom.current().nextInt(136);
                Agent bot = new Bot(botBehaviour, ttl);
                episode.registerAgent(bot);
            }
        }

    private static class Episode {
        List<Agent> activeAgents = new ArrayList<>();
        double botSpawnChance;
        Deliveler deliveryAgent;

        Episode(double botSpawnChance) {
            this.botSpawnChance = botSpawnChance;
        }
    }

    private static class EpisodeResult {
        boolean success;
        int steps;

        EpisodeResult(boolean success, int steps) {
            this.success = success;
            this.steps = steps;
        }
    }

    private static class ResultRow {
        String strategy;
        double botSpawnChance;
        double averageSteps;
        double successRate;

        ResultRow(String strategy, double botSpawnChance, double averageSteps, double successRate) {
            this.strategy = strategy;
            this.botSpawnChance = botSpawnChance;
            this.averageSteps = averageSteps;
            this.successRate = successRate;
        }
    }
}