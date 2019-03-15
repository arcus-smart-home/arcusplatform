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
package com.iris.driver.platform;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.UniformReservoir;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.scheduler.Scheduler;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.driver.DeviceDriverStateHolder;
import com.iris.device.attributes.AttributeValue;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.ProxyDeviceDriver;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.reflex.ReflexDriverDefinition;
import com.iris.driver.service.DriverConfig;
import com.iris.driver.service.executor.DefaultDriverExecutor;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.driver.service.executor.DriverExecutorRegistry;
import com.iris.driver.service.registry.DriverRegistry;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceDriverAddress;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.model.Device;
import com.iris.messages.model.DriverId;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.util.Initializer;

/**
 *
 */
@Singleton
public class PlatformDriverExecutorRegistry implements DriverExecutorRegistry {
   private static final Logger logger =
         LoggerFactory.getLogger(PlatformDriverExecutorRegistry.class);

   private final DriverRegistry registry;
   private final DeviceDAO deviceDao;
   private final Scheduler scheduler;
   private final PlacePopulationCacheManager populationCacheMgr;

   private final int driverQueueBacklog;
   private final long tombstonedDriverTimeoutMs;
   private final Cache<DeviceProtocolAddress, DeviceDriverAddress> protocolToDriverCache =
         CacheBuilder
            .newBuilder()
            .recordStats()
            .<DeviceProtocolAddress, DeviceDriverAddress>build();
   private final Cache<DeviceDriverAddress, DriverExecutor> executorCache =
         CacheBuilder
            .newBuilder()
            .recordStats()
            .removalListener(new RemovalListener<Object, Object>() {
               @Override
               public void onRemoval(RemovalNotification<Object, Object> notification) {
                  PlatformDriverExecutorRegistry.this.onExecutorEvicted((DriverExecutor) notification.getValue());
               }
            })
            .<DeviceDriverAddress, DriverExecutor>build();

   @Inject
   public PlatformDriverExecutorRegistry(
         DriverConfig config,
         DriverRegistry registry, 
         DeviceDAO deviceDao, 
         Scheduler scheduler, 
         PlacePopulationCacheManager populationCacheMgr
   ) {
      this.driverQueueBacklog = config.getDriverBacklogSize();
      this.tombstonedDriverTimeoutMs = config.getDriverTombstoneTimeout(TimeUnit.MILLISECONDS);
      this.registry = registry;
      this.deviceDao = deviceDao;
      this.scheduler = scheduler;
      this.populationCacheMgr = populationCacheMgr;
      IrisMetricSet drivers = IrisMetrics.metrics("drivers");
      drivers.monitor("cache.executor", executorCache);
      drivers.monitor("cache.protocol", protocolToDriverCache);
      drivers.gauge("backlog", (Gauge<Map<String, Object>>) () -> queueBacklog());
   }

   private Map<String, Object> queueBacklog() {
      Histogram backlog = new Histogram(new UniformReservoir(64));
      for(DriverExecutor executor: executorCache.asMap().values()) {
         backlog.update(((DefaultDriverExecutor) executor).getQueuedMessageCount());
      }
      Snapshot snap = backlog.getSnapshot();
      return ImmutableMap
            .<String, Object>builder()
            .put("count", backlog.getCount())
            .put("min", snap.getMin())
            .put("max", snap.getMax())
            .put("mean", snap.getMean())
            .put("stddev", snap.getStdDev())
            .put("p50", snap.getMedian())
            .put("p75", snap.get75thPercentile())
            .put("p95", snap.get95thPercentile())
            .put("p98", snap.get98thPercentile())
            .put("p99", snap.get99thPercentile())
            .put("p999", snap.get999thPercentile())
            .build();
   }

   /* (non-Javadoc)
    * @see com.iris.driver.service.consumer.IDriverExecutorRegistry#loadConsumer(com.iris.messages.address.Address)
    */
   @Override
   public DriverExecutor loadConsumer(final Address address) {
      return doLoadConsumer(address, Mode.LOAD);
   }

   /* (non-Javadoc)
    * @see com.iris.driver.service.executor.DriverExecutorRegistry#associate(com.iris.messages.model.Device, com.iris.driver.DeviceDriver)
    */
   @Override
   public DriverExecutor associate(Device device, DeviceDriver driver, Initializer<DriverExecutor> initializer) {
      DeviceDriverAddress address = (DeviceDriverAddress) Address.fromString(device.getAddress());
      DriverExecutor old = device.getDrivername() != null ? loadConsumer(address) : null;
      
      PlatformDeviceDriverContext context;
      if(old == null) {
         updateDeviceFromDriver(device, driver);
         context = new PlatformDeviceDriverContext(device, driver, populationCacheMgr);
      }
      else {
         // stop all messages to the old driver
         // we may drop some, but we'll hit on connected when the upgrade completes
         old.stop();
         // kill the old driver and wait for it to be fully done
         try {
            old.fire(DriverEvent.driverStopped()).get();
         }
         catch (Exception e) {
            logger.warn("Error shutting down old driver", e);
         }

         Set<String> caps = new HashSet<>();
         for (CapabilityDefinition cap : driver.getDefinition().getCapabilities()) {
            caps.add(cap.getNamespace());
         }

         // FIXME move this out of the registry
         
         // if we try to go to the DB here we risk a race condition between
         // writes from the old driver and the current read
         DeviceDriverStateHolder state = ((PlatformDeviceDriverContext) old.context()).getDriverState();
         context = new PlatformDeviceDriverContext(
               device, 
               driver.getDefinition(), 
               new DeviceDriverStateHolder(state.getAttributes(), state.getVariables()),
               false,
               populationCacheMgr
         );

         // remove attributes from old capabilities
         for(AttributeValue<?> value: state.getAttributes().entries()) {
            String namespace = value.getKey().getNamespace();
            if(!caps.contains(namespace)) {
               context.removeAttribute(value.getKey());
            }
         }

         // add attributes from base
         for(AttributeValue<?> value: driver.getBaseAttributes().entries()) {
            String namespace = value.getKey().getNamespace();
            Object oldValue = context.getAttributeValue(value.getKey());
            // always replace base & dev values
            if(
                  Capability.NAMESPACE.equals(namespace) ||
                  DeviceCapability.NAMESPACE.equals(namespace) ||
                  DeviceAdvancedCapability.NAMESPACE.equals(namespace)
            ) {
               logger.trace("Setting {} because its a core property", value);
               context.setAttributeValue(value);
            }
            // else just add defaults
            else if(oldValue == null) {
               logger.trace("Setting {} because it was null", value);
               context.setAttributeValue(value);
            }
            else {
               logger.trace("Skipping {} because it was already set to {}", value, oldValue);
            }
         }

         // update required attributes not necessarily covered by base
         context.setAttributeValue(Capability.KEY_CAPS, caps);

         String productId = driver.getBaseAttributes().get(DeviceCapability.KEY_PRODUCTID);
         context.setAttributeValue(DeviceCapability.KEY_PRODUCTID, productId);

         ReflexDriverDefinition reflexDriverDefinition = driver.getDefinition().getReflexes();
         boolean isHubLocal = reflexDriverDefinition.getDfa() != null ||
                        (reflexDriverDefinition.getReflexes() != null && 
                         !reflexDriverDefinition.getReflexes().isEmpty());
         context.setAttributeValue(DeviceAdvancedCapability.KEY_HUBLOCAL, isHubLocal);
      }
      
      DriverExecutor executor = createExecutor(context, true);
      executor.upgraded(old != null ? old.driver().getDriverId() : null);
      if(initializer != null) {
         initializer.initialize(executor);
      }
      executorCache.put(address, executor);
      return executor;
   }

   @Override
   public boolean delete(Address address) throws Exception {
      return doDelete(address, Mode.DELETE);
   }
   
   @Override
   public boolean tombstone(Address address) throws Exception {
      return doDelete(address, Mode.TOMBSTONE);
   }

   @Override
   public void remove(Address address) {
      if(address == null) {
         return;
      }
      else if(address instanceof DeviceDriverAddress) {
         executorCache.invalidate(address);
      }
      else if(address instanceof DeviceProtocolAddress) {
         DeviceProtocolAddress protocolAddress = (DeviceProtocolAddress) address;
         DeviceDriverAddress driverAddress = protocolToDriverCache.getIfPresent(protocolAddress);
         if(driverAddress == null) {
            driverAddress = loadExecutorByProtocolAddress(protocolAddress, Mode.REMOVE);
         }
         // always remove from the dispatcher side, removal here will trigger onExecutorEvicted and clean up
         // the protocol side
         executorCache.invalidate(driverAddress);
      }
   }

   protected void onExecutorEvicted(DriverExecutor consumer) {
      logger.debug("Message consumer cache entry expired: [{}]", consumer);
      DeviceProtocolAddress protocolAddress =  (DeviceProtocolAddress) consumer.context().getProtocolAddress();
      protocolToDriverCache.invalidate(protocolAddress);
      scheduler.scheduleDelayed(consumer::stop, 0, TimeUnit.MILLISECONDS);
   }
   
   protected DriverExecutor doLoadConsumer(Address address, Mode mode) {
      Preconditions.checkNotNull(address, "address may not be null");
      try {
         DeviceDriverAddress driverAddress;
         if(address instanceof DeviceDriverAddress) {
            driverAddress = (DeviceDriverAddress) address;
         }
         else if(address instanceof DeviceProtocolAddress) {
            DeviceProtocolAddress protocolAddress = (DeviceProtocolAddress) address;
            driverAddress = 
                  protocolToDriverCache.get(
                        protocolAddress, 
                        () -> loadExecutorByProtocolAddress(protocolAddress, mode)
                  );
         }
         else {
            throw new IllegalArgumentException("Invalid address type [" + address.getClass() + "] for driver");
         }
         return executorCache.get(driverAddress, () -> loadExecutorByDriverAddress(driverAddress, mode));
      }
      catch(UncheckedExecutionException e) {
         throw unwrap(e);
      }
      catch(ExecutionException e) {
         throw unwrap(e);
      }
   }
   
   protected boolean doDelete(Address address, Mode mode) throws Exception {
      final DriverExecutor executor;
      try {
         executor = doLoadConsumer(address, mode);
      }
      catch(NotFoundException e) {
         logger.warn("could not {} device: device {} not found", mode, address);
         return false;
      }
      
      // TODO review threading model -- since we're accessing context outside of the event loop, be sure to synchronize it
      synchronized(executor.context()) {
         ListenableFuture<?> result = executor.fire(DriverEvent.createDisassociated());
         result.addListener(
                  () -> {
                     try {
                        result.get();
                     }
                     catch(Exception e) {
                        logger.warn("Unable to disassociate driver", e);
                     }
                     if(mode == Mode.DELETE) {
                        executor.context().delete();
                     }
                     else {
                        executor.context().tombstone();
                     }
                  },
                  MoreExecutors.directExecutor()
            );
      }
      remove(address);
      return true;
   }
   
   private void updateDeviceFromDriver(Device device, DeviceDriver driver) {
      Set<String> caps = new HashSet<>();
      Set<CapabilityDefinition> capDefs = driver.getDefinition().getCapabilities();
      for (CapabilityDefinition capDef : capDefs) {
         caps.add(capDef.getNamespace());
      }
      device.setCaps(caps);
      device.setDriverId(driver.getDriverId());
   }
   
   private DeviceDriverAddress loadExecutorByProtocolAddress(DeviceProtocolAddress address, Mode mode) {
      final Device device = deviceDao.findByProtocolAddress(address.getRepresentation());
      if(device == null) {
         throw new NotFoundException(address);
      }
      DeviceDriverAddress driverAddress = (DeviceDriverAddress) Address.fromString(device.getAddress());
      boolean cacheDriver = mode != Mode.REMOVE;
      if(cacheDriver) {
         // warm the cache while we've got the device object loaded
         try {
            executorCache.get(driverAddress, () -> this.loadExecutor(device, mode));
         }
         catch (ExecutionException e) {
            logger.warn("Unable to load driver for device [{}]", device.getAddress(), e);
            throw unwrap(e);
         }
      }
      return driverAddress;
   }

   private DriverExecutor loadExecutorByDriverAddress(DeviceDriverAddress address, Mode mode) {
      return loadExecutor( loadDeviceByDriverAddress(address), mode );
   }

   private Device loadDeviceByDriverAddress(DeviceDriverAddress address) {
      Device device = deviceDao.findById(address.getDeviceId());
      if(device == null) {
         throw new NotFoundException(address);
      }
      return device;
   }

   private DriverExecutor loadExecutor(Device device, Mode mode) {
      if(device.getDriverId() == null) {
         throw new IllegalStateException("Unable to load driver for device");
      }
      DeviceDriverContext context = loadContext(device, device.getDriverId());
      if(context == null) {
         throw new IllegalStateException("Unable to load context for device");
      }
      boolean startDriver = mode != Mode.TOMBSTONE; // don't start tombstoned drivers we're just loading all the data to fully remove them
      if(mode == Mode.DELETE && context.isTombstoned()) {
         // also don't start it if we're just cleaning out the tombstone
         startDriver = false;
      }
      return createExecutor(context, startDriver);
   }
   
   private DriverExecutor createExecutor(DeviceDriverContext context, boolean start) {
      final DeviceDriverAddress address = (DeviceDriverAddress) context.getDriverAddress();
      DriverExecutor executor = new DefaultDriverExecutor(
            new ProxyDeviceDriver(registry, context.getDriverId()),
            context, 
            scheduler, 
            driverQueueBacklog
      );
      // evict after timeout when tombstoned
      if(context.isTombstoned() && tombstonedDriverTimeoutMs > 0) {
         scheduler.scheduleDelayed(
               () -> executorCache.invalidate(address),
               tombstonedDriverTimeoutMs,
               TimeUnit.MILLISECONDS
         );
      }
      
      if(start) {
         executor.start();
      }
      return executor;
   }

   private DeviceDriverContext loadContext(Device device, DriverId driverId) {
      DeviceDriver driver = registry.loadDriverById(driverId);
      if(driver == null) {
         logger.warn("Unable to load driver [{}], using Fallback driver instead", driverId);
         driver = registry.getFallback();
      }
      if(driver == null) {
         throw new IllegalArgumentException("Unable to load driver for id [" + driverId + "]");
      }

      return new PlatformDeviceDriverContext(device, driver.getDefinition(), deviceDao.loadDriverState(device), populationCacheMgr);
   }

   private RuntimeException unwrap(ExecutionException e) {
      Throwable cause = e.getCause();
      if(cause instanceof RuntimeException) {
         return (RuntimeException) cause;
      }
      else {
         return new RuntimeException(cause);
      }      
   }

   private RuntimeException unwrap(UncheckedExecutionException e) {
      Throwable cause = e.getCause();
      if(cause instanceof RuntimeException) {
         return (RuntimeException) cause;
      }
      else {
         return new RuntimeException(cause);
      }      
   }
   
   private enum Mode {
      LOAD,
      REMOVE,
      DELETE,
      TOMBSTONE
   }
}

