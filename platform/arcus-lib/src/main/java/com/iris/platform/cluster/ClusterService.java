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
/**
 * 
 */
package com.iris.platform.cluster;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.StartupListener;
import com.iris.platform.cluster.exception.ClusterIdLostException;

/**
 * 
 */
// TODO expose messaging to other services?
@Singleton
public class ClusterService implements StartupListener {
   private static final Logger logger = LoggerFactory.getLogger(ClusterService.class);
   public static final String NAME_EXECUTOR = "cluster.heartbeat.executor";
   
   private final AtomicReference<ClusterServiceRecord> recordRef = new AtomicReference<>();
   
   private final Set<ClusterServiceListener> listeners;
   private final ClusterServiceDao clusterServiceDao;
   private final ScheduledExecutorService executor;
   
   private final long heartbeatIntervalMs;
   private final long retryDelayMs;
   private final int retries;
   private final boolean exitOnDeregistered;
   
   /**
    * 
    */
   @Inject
   public ClusterService(
         ClusterServiceDao clusterServiceDao,
         Optional<Set<ClusterServiceListener>> listeners,
         @Named(NAME_EXECUTOR) ScheduledExecutorService executor,
         ClusterConfig config
   ) {
      this.clusterServiceDao = clusterServiceDao;
      this.listeners = listeners.or(ImmutableSet.of());
      this.executor = executor;
      this.heartbeatIntervalMs = config.getHeartbeatInterval(TimeUnit.MILLISECONDS);
      this.retryDelayMs = config.getRegistrationRetryDelay(TimeUnit.MILLISECONDS);
      this.retries = config.getRegistrationRetries();
      this.exitOnDeregistered = config.isExitOnDeregistered();
   }

   @Override
   public void onStarted() {
      logger.info("Determining cluster membership...");
      tryRegister(0);
      executor.scheduleAtFixedRate(() -> heartbeat(), heartbeatIntervalMs, heartbeatIntervalMs, TimeUnit.MILLISECONDS);
   }
   
   @PreDestroy
   public void stop() {
      logger.info("Removing cluster membership");
      executor.shutdownNow();
      ClusterServiceRecord record = recordRef.get();
      if(record != null) {
         clusterServiceDao.deregister(record);
      }
   }
   
   public Optional<ClusterServiceRecord> getServiceRecord() {
      ClusterServiceRecord record = recordRef.get();
      return record == null ? Optional.absent() : Optional.of(record.copy());
   }
   
   protected void tryRegister(int attempt) {
      try {
         ClusterServiceRecord record = clusterServiceDao.register();
         onRegistered(record);
      }
      catch(Exception e) {
         if(retries < 0 || attempt < retries) {
            logger.warn("Unable to reserve cluster id, will try again in [{}] ms, attempt [{}]", retryDelayMs, attempt);
            executor.schedule(() -> tryRegister(attempt+1), retryDelayMs, TimeUnit.MILLISECONDS);
         }
         else {
            logger.warn("Unable to reserve cluster id, destroying service", e);
            System.exit(-1);
         }
      }
   }
   
   protected void heartbeat() {
      try {
         ClusterServiceRecord record = this.recordRef.get();
         if(record == null) {
            return;
         }
         
         record = clusterServiceDao.heartbeat(this.recordRef.get());
         this.recordRef.set(record);
      }
      catch(ClusterIdLostException e) {
         onDeregistered();
         logger.warn("Attempting to reserve a new cluster id");
         tryRegister(0);
      }
      catch(Exception e) {
         logger.warn("Error updating heartbeat table", e);
      }
   }
   
   protected void onRegistered(ClusterServiceRecord record) {
      if(this.recordRef.compareAndSet(null, record)) {
         logger.info("Registered as cluster node [{}], notifying [{}] listeners", record, listeners.size());
         for(ClusterServiceListener listener: listeners) {
            try {
               listener.onClusterServiceRegistered(record);
            }
            catch(Exception e) {
               logger.warn("Unable to notify listener of cluster service registration", e);
            }
         }
      }
   }
   
   protected void onDeregistered() {
      if(this.recordRef.getAndSet(null) != null) {
         logger.info("Lost cluster membership, notifying [{}] listeners", listeners.size());
         for(ClusterServiceListener listener: listeners) {
            try {
               listener.onClusterServiceDeregistered();
            }
            catch(Exception e) {
               logger.warn("Unable to notify listener of cluster service deregistration", e);
            }
         }
         
         if(exitOnDeregistered) {
            logger.error("SHUTTING DOWN -- Lost cluster service id and exitOnDeregister is true");
            System.err.println("SHUTTING DOWN -- Lost cluster service id and exitOnDeregister is true");
            System.exit(-1);
         }
      }
   }
   
}

