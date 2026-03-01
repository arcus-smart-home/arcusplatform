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
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Session;

/**
 * Tracks Cassandra health using the DataStax driver's
 * {@link Host.StateListener} interface. When a host goes down, a
 * lightweight probe query is issued to verify whether the remaining
 * hosts can still satisfy the configured consistency level. If the
 * probe fails, {@link #isHealthy()} returns false, which causes TCP
 * health checks to stop reporting ONLINE so that K8s can restart the pod.
 *
 * This is a static singleton so it works across multiple Cassandra
 * modules (different keyspaces) without Guice binding conflicts.
 * Services that don't use Cassandra are unaffected — isHealthy()
 * returns true until the first cluster is registered.
 */
public class CassandraHealth implements Host.StateListener {
   private static final Logger logger = LoggerFactory.getLogger(CassandraHealth.class);
   private static final CassandraHealth INSTANCE = new CassandraHealth();
   private static final String PROBE_QUERY = "SELECT defaultId FROM default_population LIMIT 1";

   private final Set<InetAddress> upHosts = ConcurrentHashMap.newKeySet();
   private final AtomicReference<Session> sessionRef = new AtomicReference<>();
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
    * Seeds the live host set from the cluster's current metadata and
    * stores a session reference for probe queries.
    * Safe to call multiple times (e.g. from multiple keyspace modules).
    */
   public void initializeFrom(Cluster cluster, Session session) {
      sessionRef.compareAndSet(null, session);
      for (Host host : cluster.getMetadata().getAllHosts()) {
         if (host.isUp()) {
            upHosts.add(host.getAddress());
         }
      }
      active = true;
      logger.info("Cassandra health initialized with {} live hosts", upHosts.size());
   }

   /**
    * @deprecated Use {@link #initializeFrom(Cluster, Session)} instead.
    */
   @Deprecated
   public void initializeFrom(Cluster cluster) {
      for (Host host : cluster.getMetadata().getAllHosts()) {
         if (host.isUp()) {
            upHosts.add(host.getAddress());
         }
      }
      active = true;
      logger.info("Cassandra health initialized with {} live hosts (no session for probe queries)", upHosts.size());
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
      healthy = true;
      logger.info("Cassandra host up: {}, live hosts: {}", host.getAddress(), upHosts.size());
   }

   @Override
   public void onDown(Host host) {
      upHosts.remove(host.getAddress());
      int remaining = upHosts.size();
      if (remaining == 0) {
         healthy = false;
         logger.error("All Cassandra hosts are down, health check will report unhealthy");
      } else {
         healthy = probeQuery();
         if (!healthy) {
            logger.error("Cassandra host down: {}, probe query failed, health check will report unhealthy (live hosts: {})",
                  host.getAddress(), remaining);
         } else {
            logger.warn("Cassandra host down: {}, probe query OK (live hosts: {})", host.getAddress(), remaining);
         }
      }
   }

   @Override
   public void onRemove(Host host) {
      upHosts.remove(host.getAddress());
      int remaining = upHosts.size();
      if (remaining == 0) {
         healthy = false;
         logger.error("All Cassandra hosts removed, health check will report unhealthy");
      } else {
         healthy = probeQuery();
         if (!healthy) {
            logger.error("Cassandra host removed: {}, probe query failed, health check will report unhealthy (live hosts: {})",
                  host.getAddress(), remaining);
         } else {
            logger.warn("Cassandra host removed: {}, probe query OK (live hosts: {})", host.getAddress(), remaining);
         }
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

   /**
    * Issues a lightweight query against the system table using the
    * session's configured consistency level. Returns true if the
    * query succeeds, false otherwise.
    */
   private boolean probeQuery() {
      Session session = sessionRef.get();
      if (session == null) {
         logger.warn("No session available for probe query, assuming unhealthy");
         return false;
      }
      try {
         session.execute(PROBE_QUERY);
         return true;
      } catch (Exception e) {
         logger.debug("Cassandra probe query failed", e);
         return false;
      }
   }
}
