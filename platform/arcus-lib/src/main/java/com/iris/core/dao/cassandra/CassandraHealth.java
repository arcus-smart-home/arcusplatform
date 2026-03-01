/*
 * Copyright 2019 Arcus Project
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
package com.iris.core.dao.cassandra;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.metadata.NodeState;
import com.datastax.oss.driver.api.core.metadata.NodeStateListener;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Tracks Cassandra health. The primary health signal comes from
 * {@link com.iris.platform.cluster.ClusterService} which calls
 * {@link #setHealthy(boolean)} on each heartbeat cycle. The
 * {@link NodeStateListener} callbacks provide supplementary logging
 * of host topology changes.
 *
 * This is a static singleton so it works across multiple Cassandra
 * modules (different keyspaces) without Guice binding conflicts.
 * Services that don't use Cassandra are unaffected — isHealthy()
 * returns true until the first cluster is registered.
 */
public class CassandraHealth implements NodeStateListener {
   private static final Logger logger = LoggerFactory.getLogger(CassandraHealth.class);
   private static final CassandraHealth INSTANCE = new CassandraHealth();

   private final Set<InetSocketAddress> upHosts = ConcurrentHashMap.newKeySet();
   private volatile boolean active = false;
   private volatile boolean healthy = true;

   private CassandraHealth() {}

   @Override
   public void close() {
      // nothing to close
   }

   public static CassandraHealth instance() {
      return INSTANCE;
   }

   /**
    * Returns true if Cassandra connectivity is healthy.
    * Always returns true for services that don't use Cassandra.
    */
   public boolean isHealthy() {
      return !active || healthy;
   }

   /**
    * Allows external components (e.g. ClusterService heartbeat) to
    * report Cassandra health based on actual query success/failure.
    */
   public void setHealthy(boolean healthy) {
      active = true;
      this.healthy = healthy;
   }

   /**
    * Seeds the live host set from the session's current metadata.
    * Safe to call multiple times (e.g. from multiple keyspace modules).
    */
   public void initializeFrom(CqlSession session) {
      for (Node node : session.getMetadata().getNodes().values()) {
         if (node.getState() == NodeState.UP) {
            InetSocketAddress addr = resolveAddress(node);
            if (addr != null) {
               upHosts.add(addr);
            }
         }
      }
      active = true;
      logger.info("Cassandra health initialized with {} live hosts", upHosts.size());
   }

   @Override
   public void onUp(@NonNull Node node) {
      InetSocketAddress addr = resolveAddress(node);
      if (addr != null) {
         upHosts.add(addr);
         logger.info("Cassandra host up: {}, live hosts: {}", addr, upHosts.size());
      }
   }

   @Override
   public void onDown(@NonNull Node node) {
      InetSocketAddress addr = resolveAddress(node);
      if (addr != null) {
         upHosts.remove(addr);
      }
      int remaining = upHosts.size();
      if (remaining == 0) {
         logger.error("All Cassandra hosts are down");
      } else {
         logger.warn("Cassandra host down: {}, live hosts: {}", addr, remaining);
      }
   }

   @Override
   public void onAdd(@NonNull Node node) {
      if (node.getState() == NodeState.UP) {
         InetSocketAddress addr = resolveAddress(node);
         if (addr != null) {
            upHosts.add(addr);
            logger.info("Cassandra host added (up): {}, live hosts: {}", addr, upHosts.size());
         }
      } else {
         logger.info("Cassandra host added (down): {}", resolveAddress(node));
      }
   }

   @Override
   public void onRemove(@NonNull Node node) {
      InetSocketAddress addr = resolveAddress(node);
      if (addr != null) {
         upHosts.remove(addr);
      }
      int remaining = upHosts.size();
      if (remaining == 0) {
         logger.error("All Cassandra hosts removed");
      } else {
         logger.warn("Cassandra host removed: {}, live hosts: {}", addr, remaining);
      }
   }

   private static InetSocketAddress resolveAddress(Node node) {
      Object endpoint = node.getEndPoint().resolve();
      if (endpoint instanceof InetSocketAddress) {
         return (InetSocketAddress) endpoint;
      }
      return null;
   }
}
