/*
 * Copyright 2020 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iris.platform.cluster.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZookeeperMonitor implements Watcher, StatCallback {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperMonitor.class);

    private final ZooKeeper zk;

    public ZookeeperMonitor(ZooKeeper zk) {
        this.zk = zk;
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.None) {
            switch (event.getState()) {
                case SyncConnected:
                    break;
                case Expired:
                    // TODO: move this elsewhere?
                    logger.error("SHUTTING DOWN -- zookeeper session has been marked as expired");
                    System.err.println("SHUTTING DOWN -- zookeeper session has been marked as expired");
                    System.exit(-1);

                    break;
            }
        } else {

        }
    }

    public void processResult(int rc, String path, Object ctx, Stat stat) {
    }
}