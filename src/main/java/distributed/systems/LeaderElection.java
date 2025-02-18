package distributed.systems;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class LeaderElection implements Watcher {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String ZOOKEEPER_ADDRESS = "127.0.0.1:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private static final String ELECTION_NAMESPACE = "/election";
    private ZooKeeper zooKeeper;
    private String currentZnodeName;

    public static void main(String... strings) throws IOException, InterruptedException, KeeperException {
        LeaderElection leaderElection = new LeaderElection();
        leaderElection.connectToZookeeper();
        leaderElection.volunteerForLeadership();
        leaderElection.electLeader();
        leaderElection.run();
        leaderElection.close();
        LOGGER.info("Disconnected from Zookeeper, exiting the application.");
    }


    public void connectToZookeeper() throws IOException {
        zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    public void volunteerForLeadership() throws InterruptedException, KeeperException {
        String zNodePrefix = ELECTION_NAMESPACE + "/c_";
        String zNodeFullPath = zooKeeper.create(zNodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        LOGGER.info("zNode name: {}", zNodeFullPath);
        currentZnodeName = zNodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
    }

    public void electLeader() throws InterruptedException, KeeperException {
        List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);
        Collections.sort(children);
        String smallestChild = children.get(0);

        if (smallestChild.equals(currentZnodeName)) {
            LOGGER.info("I am the leader");
            return;
        }

        LOGGER.info("I am not the leader, {} is the leader", smallestChild);
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        if (Objects.requireNonNull(watchedEvent.getType()) == Event.EventType.None) {
            if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                LOGGER.info("Successfully connected to Zookeeper");
            } else {
                synchronized (zooKeeper) {
                    LOGGER.info("Received disconnected from Zookeeper event");
                    zooKeeper.notifyAll();
                }
            }
        }
    }
}
