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
package com.iris.platform.subsystem.impl;

import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.scheduler.Scheduler;
import com.iris.common.subsystem.Subsystem;
import com.iris.common.subsystem.SubsystemExecutor;
import com.iris.common.subsystem.SubsystemContext.ResponseAction;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.context.PlaceContext;
import com.iris.messages.event.AddressableEvent;
import com.iris.messages.event.Listener;
import com.iris.messages.model.Model;
import com.iris.messages.model.Place;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.subsystem.SubsystemCatalog;
import com.iris.platform.subsystem.SubsystemConfig;
import com.iris.platform.subsystem.SubsystemDao;
import com.iris.platform.subsystem.SubsystemFactory;
import com.iris.util.IrisCorrelator;

/**
 * 
 */
@Singleton
public class PlatformSubsystemFactory implements SubsystemFactory {
   private static final Logger log =
      LoggerFactory.getLogger(PlatformSubsystemFactory.class);

   private final SubsystemCatalog catalog;
   private final SubsystemDao dao;
   private final PlaceDAO placeDao;
   private final PlatformMessageBus platformBus;
   private final IrisCorrelator<ResponseAction<?>> correlator;
   private final Scheduler scheduler;
   private final Listener<AddressableEvent> scheduledEventListener;
   private final int maxQueueDepth;

   /**
    * 
    */
   @Inject
   public PlatformSubsystemFactory(
         SubsystemConfig config,
         SubsystemCatalog catalog,
         SubsystemDao dao,
         PlaceDAO placeDao,
         PlatformMessageBus platformBus,
         IrisCorrelator<ResponseAction<?>> correlator,
         Scheduler scheduler,
         Listener<AddressableEvent> scheduledEventListener
   ) {
      this.maxQueueDepth = config.getPerSubsystemQueueDepth();
      this.platformBus = platformBus;
      this.correlator = correlator;
      this.scheduler = scheduler;
      this.catalog = catalog;
      this.dao = dao;
      this.placeDao=placeDao;
      this.scheduledEventListener = scheduledEventListener;
   }
   
   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemFactory#createExecutor(com.iris.messages.context.PlaceContext)
    */
   @Override
   public SubsystemExecutor createExecutor(PlaceContext rootContext) {
      return new PlatformSubsystemExecutor(platformBus, correlator, this, rootContext, catalog.getSubsystems(), maxQueueDepth);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.subsystem.SubsystemFactory#createContext(com.iris.common.subsystem.Subsystem, com.iris.messages.event.Listener, com.iris.messages.context.PlaceContext)
    */
   @Override
   public <M extends SubsystemModel> PlatformSubsystemContext<M> createContext(
         Subsystem<M> subsystem,
         Listener<AddressableEvent> eventBus,
         PlaceContext rootContext
   ) {
      try {
         Logger logger = LoggerFactory.getLogger("subsystem." + subsystem.getNamespace());
         
         String id = subsystem.getNamespace() + ":" + rootContext.getPlaceId().toString();
         Address addr = Address.platformService(rootContext.getPlaceId(), subsystem.getNamespace());

         Model existing = rootContext.models().getModelByAddress(addr);
         ModelEntity entity;
         if (existing != null) {
            entity = (ModelEntity) existing;
            log.trace("reusing already loaded subsystem for place: {}:{}", rootContext.getPlaceId(), subsystem.getNamespace());
         } else {
            // TODO: If the model entity is not in the already loaded entities won't this always be null?
            log.info("reloading subsystem for place: {}:{}", rootContext.getPlaceId(), subsystem.getNamespace());
            entity = dao.findByPlaceAndNamespace(rootContext.getPlaceId(), subsystem.getNamespace());
         }

         TimeZone tz=null;
         try{
            tz=loadTimeZone(rootContext.getPlaceId());
         }
         catch(Exception e){
            logger.warn("Unable to load timezone ", e);
         }
         
         if(entity == null) {
            // TODO move this into the subsystem
            entity = new ModelEntity(
               ImmutableMap
                  .of(
                        // need id to be unique per-root type, so add in the namespace
                        Capability.ATTR_ID, id,
                        Capability.ATTR_TYPE, SubsystemCapability.NAMESPACE,
                        Capability.ATTR_ADDRESS, addr.getRepresentation(),
                        Capability.ATTR_CAPS, ImmutableSet.of(Capability.NAMESPACE, SubsystemCapability.NAMESPACE, subsystem.getNamespace()),
                        SubsystemCapability.ATTR_PLACE, rootContext.getPlaceId().toString()
                  )
            );
            ((PlatformSubsystemModelStore) rootContext.models()).addModel(entity);
         }
         return 
               PlatformSubsystemContext
                  .builder()
                  .withPlaceId(rootContext.getPlaceId())
                  .withPopulation(rootContext.getPopulation())
                  .withAccountId(rootContext.getAccountId())
                  .withModels(rootContext.models())
                  .withLogger(logger)
                  .withSubsystemDao(dao)
                  .withPlatformBus(platformBus)
                  .withCorrelator(correlator)
                  .withScheduler(scheduler)
                  .withScheduledEventListener(scheduledEventListener)
                  .withTimezone(tz)
                  .build( subsystem.getType(), entity )
                  ;
      }
      catch(RuntimeException e) {
         throw e;
      }
      catch(Exception e) {
         throw new UncheckedExecutionException("Unable to load model for subsystem of type " + subsystem.getType(), e);
      }
   }
   private TimeZone loadTimeZone(UUID placeId) throws Exception {
      Place place = placeDao.findById(placeId);
      String tzId = place != null ? place.getTzId() : null;
      TimeZone tz = TimeZone.getDefault();
      if (!StringUtils.isEmpty(tzId)) {
         try {
            tz = TimeZone.getTimeZone(tzId);
            return tz;
         }
         catch(Exception e) {
            throw new IllegalStateException("Unable to parse tz " + tz,e);
         }
      }
      return null;
   }

}

