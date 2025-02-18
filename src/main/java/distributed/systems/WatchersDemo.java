package distributed.systems;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;

public class WatchersDemo implements Watcher {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String ZOOKEEPER_ADDRESS = "127.0.0.1:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private static final String TARGET_ZNODE = "/target_znode";
    private ZooKeeper zooKeeper;

    public static void main(String[] args) throws InterruptedException, IOException, KeeperException {
        WatchersDemo watchersDemo = new WatchersDemo();
        watchersDemo.connectToZookeeper();
        watchersDemo.watchTargetZnode();
        watchersDemo.run();
        watchersDemo.close();
    }

    public void connectToZookeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
    }

    public void watchTargetZnode() throws KeeperException, InterruptedException {
        Stat stat = zooKeeper.exists(TARGET_ZNODE, this);
        if (stat == null) {
            return;
        }

        byte[] data = zooKeeper.getData(TARGET_ZNODE, this, stat);
        List<String> children = zooKeeper.getChildren(TARGET_ZNODE, this);

        LOGGER.info("Data : {}, children : {}", new String(data), children);
    }

    @Override
    public void process(WatchedEvent event) {
        switch (event.getType()) {
            case None:
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    LOGGER.info("Successfully connected to Zookeeper");
                } else {
                    synchronized (zooKeeper) {
                        LOGGER.info("Disconnected from Zookeeper event");
                        zooKeeper.notifyAll();
                    }
                }
                break;
            case NodeDeleted:
                LOGGER.info(TARGET_ZNODE + " was deleted");
                break;
            case NodeCreated:
                LOGGER.info(TARGET_ZNODE + " was created");
                break;
            case NodeDataChanged:
                LOGGER.info(TARGET_ZNODE + " data changed");
                break;
            case NodeChildrenChanged:
                LOGGER.info(TARGET_ZNODE + " children changed");
                break;
        }

        try {
            watchTargetZnode();
        } catch (KeeperException | InterruptedException ignored) {
        }
    }
}
