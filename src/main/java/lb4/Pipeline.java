package lb4;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Pipeline implements Runnable {

    private Behaviour agentBehaviour;
    private Behaviour botBehaviour;
    private Map map;

    @Override
    public void run() {


    }

    class Episode {
        public Set<Agent> activeAgents;
        public double propBotSpawn;

        public Episode() {
            this.activeAgents = new HashSet<>();
            this.propBotSpawn = 0.02;
        }

        public boolean registerAgent(Agent agent) {
            return this.activeAgents.add(agent);
        }

    }

    private void playEpisode(Map map, double propBotSpawn) {

        Episode episode = new Episode();

        Map.Node deliveryTarget = map.targetPoints.get(ThreadLocalRandom.current().nextInt(map.targetPoints.size()));
        Agent agent = new Deliveler(agentBehaviour, deliveryTarget);
        episode.registerAgent(agent);

        int steps = 0;

        while (agent.isAlive) {

            playStep(episode);
            steps++;
        }

        if (agent.currentPosition == agent.target) {
            // победа
        }

    }

    private void playStep(Episode episode) {
        // заспавнить ботов
        spawnBots(episode);

        for (Agent agent : episode.activeAgents) {
            agent.make_action();
        }
        // удалить тех агентов, что столкнутся
        Set<Agent> collidesAgents = checkCollisions(episode);
        for (var agent : collidesAgents) {
            episode.activeAgents.remove(agent);
        }

        // передвинуть всех агентов
        for (var agent : episode.activeAgents) {
            agent.applyAction();
            if (!agent.isAlive) {
                episode.activeAgents.remove(agent);
            }
        }


    }

    private Set<Agent> checkCollisions(Episode episode) {
        // столкновение или на дороге или на ноде
        Set<Agent> collidesAgents = new HashSet<>();

        // НОДА
        // для каждого бота надо проверить куда он идет
        HashMap<Map.Node, List<Agent>> endPositions = new HashMap<>();
        for (Agent agent : episode.activeAgents) {
            endPositions
                    .computeIfAbsent(agent.nextPosition, k -> new ArrayList<>())
                    .add(agent);
        }
        for (var key : endPositions.keySet()) {
            if (endPositions.get(key).size() >= 2) {
                collidesAgents.addAll(endPositions.get(key));
            }
        }
        // ДОРОГА
        // перебрать все пары агентов. проверить что они сталкиваются
        for (Agent agent1 : episode.activeAgents) {
            for (Agent agent2 : episode.activeAgents) {
                if (agent1 == agent2) {
                    continue;
                }
                if (agent1.nextPosition == agent2.currentPosition && agent1.currentPosition == agent2.nextPosition) {
                    collidesAgents.add(agent1);
                    collidesAgents.add(agent2);
                }
            }
        }
        return collidesAgents;
    }

    private void spawnBots(Episode episode) {

        for (int i = 0; i < this.map.botSpawnPoints.size(); i++) {
            if (ThreadLocalRandom.current().nextDouble() < episode.propBotSpawn) {
                int ttl = 15 + ThreadLocalRandom.current().nextInt(136);
                Agent bot = new Bot(botBehaviour, ttl);
                episode.registerAgent(bot);
            }
        }

    }


}
