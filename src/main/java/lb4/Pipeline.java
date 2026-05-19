package lb4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        Agent agent = new Deliveler(agentBehaviour);
        episode.registerAgent(agent);


        int steps = 0;

        while (agent.isAlive) {

            playStep(episode);
            steps++;

        }

    }

    private void playStep(Episode episode) {
        // заспавнить ботов
        spawnBots(episode);

        // удалить тех агентов, что столкнутся
        List<Agent> agents = checkCollisions();

        for (var agent: agents){
            episode.activeAgents.remove(agent);
        }

        // передвинуть всех агентов


    }

    private List<Agent> checkCollisions() {
        return null;
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
