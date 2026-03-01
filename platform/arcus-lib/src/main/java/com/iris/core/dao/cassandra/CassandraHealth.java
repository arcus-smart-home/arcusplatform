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

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;

/**
 * Tracks Cassandra health. The primary health signal comes from
 * {@link com.iris.platform.cluster.ClusterService} which calls
 * {@link #setHealthy(boolean)} on each heartbeat cycle. The
 * {@link Host.StateListener} callbacks provide supplementary logging
 * of host topology changes.
 *
 * This is a static singleton so it works across multiple Cassandra
 * modules (different keyspaces) without Guice binding conflicts.
 * Services that don't use Cassandra are unaffected — isHealthy()
 * returns true until the first cluster is registered.
 */
public class CassandraHealth implements Host.StateListener {
   private static final Logger logger = LoggerFactory.getLogger(CassandraHealth.class);
   private static final CassandraHealth INSTANCE = new CassandraHealth();

   private final Set<InetAddress> upHosts = ConcurrentHashMap.newKeySet();
   private volatile boolean active = false;
   private volatile boolean healthy = true;

   private CassandraHealth() {}

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
    * Seeds the live host set from the cluster's current metadata.
    * Safe to call multiple times (e.g. from multiple keyspace modules).
    */
   public void initializeFrom(Cluster cluster) {
      for (Host host : cluster.getMetadata().getAllHosts()) {
         if (host.isUp()) {
            upHosts.add(host.getAddress());
         }
      }
      active = true;
      logger.info("Cassandra health initialized with {} live hosts", upHosts.size());
   }

   @Override
   public void onAdd(Host host) {
      if (host.isUp()) {
         upHosts.add(host.getAddress());
         logger.info("Cassandra host added (up): {}, live hosts: {}", host.getAddress(), upHosts.size());
      } else {
         logger.info("Cassandra host added (down): {}", host.getAddress());
      }
   }

   @Override
   public void onUp(Host host) {
      upHosts.add(host.getAddress());
      logger.info("Cassandra host up: {}, live hosts: {}", host.getAddress(), upHosts.size());
   }

   @Override
   public void onDown(Host host) {
      upHosts.remove(host.getAddress());
      int remaining = upHosts.size();
      if (remaining == 0) {
         logger.error("All Cassandra hosts are down");
      } else {
         logger.warn("Cassandra host down: {}, live hosts: {}", host.getAddress(), remaining);
      }
   }

   @Override
   public void onRemove(Host host) {
      upHosts.remove(host.getAddress());
      int remaining = upHosts.size();
      if (remaining == 0) {
         logger.error("All Cassandra hosts removed");
      } else {
         logger.warn("Cassandra host removed: {}, live hosts: {}", host.getAddress(), remaining);
      }
   }

   @Override
   public void onRegister(Cluster cluster) {
      // no-op
   }

   @Override
   public void onUnregister(Cluster cluster) {
      // no-op
   }
}
