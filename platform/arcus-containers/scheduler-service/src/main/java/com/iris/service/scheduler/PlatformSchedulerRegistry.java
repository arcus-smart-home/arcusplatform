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
package com.iris.service.scheduler;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.common.sunrise.GeoLocation;
import com.iris.common.sunrise.SunriseSunsetCalc;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.address.Address;
import com.iris.messages.model.Place;
import com.iris.messages.model.serv.SchedulerModel;
import com.iris.platform.model.ModelEntity;
import com.iris.platform.scheduler.SchedulerModelDao;
import com.iris.population.PlacePopulationCacheManager;

/**
 * 
 */
// TODO add in caching...
@Singleton
public class PlatformSchedulerRegistry implements SchedulerRegistry {
   private static final Logger logger = LoggerFactory.getLogger(PlatformSchedulerRegistry.class);

   private final SunriseSunsetCalc calculator;
   private final SchedulerModelDao schedulerDao;
   private final PlaceDAO placeDao;
   private final PlatformMessageBus platformBus;
   private final EventSchedulerService schedulerService;
   private final DefinitionRegistry registry;
   private final PlacePopulationCacheManager populationCacheMgr;
   
   @Inject
   public PlatformSchedulerRegistry(
         SunriseSunsetCalc calculator,
         SchedulerModelDao schedulerDao, 
         PlaceDAO placeDao,
         PlatformMessageBus platformBus,
         EventSchedulerService schedulerService,
         DefinitionRegistry registry,
         PlacePopulationCacheManager populationCacheMgr
   ) {
      this.calculator = calculator;
      this.schedulerDao = schedulerDao;
      this.placeDao = placeDao;
      this.platformBus = platformBus;
      this.schedulerService = schedulerService;
      this.registry = registry;
      this.populationCacheMgr = populationCacheMgr;
   }
   
   @Override
   public List<SchedulerCapabilityDispatcher> loadByPlace(UUID placeId, boolean includeWeekdays) {
      return 
            schedulerDao.listByPlace(placeId, includeWeekdays)
               .stream()
               .filter(Objects::nonNull)
               .map((scheduler) -> createExecutor(scheduler))
               .collect(Collectors.toList())
               ;
   }

   @Override
   public Optional<SchedulerCapabilityDispatcher> loadByAddress(Address address) {
      ModelEntity scheduler = schedulerDao.findByAddress(address);
      if(scheduler == null) {
         return Optional.empty();
      }
      return Optional.of( createExecutor(scheduler) );
   }

   @Override
   public SchedulerCapabilityDispatcher loadOrCreateByAddress(UUID placeId, Address target) {
      ModelEntity scheduler = schedulerDao.findOrCreateByTarget(placeId, target);
      return createExecutor(scheduler);
   }

   @Override
   public void deleteByAddress(Address address) {
      ModelEntity scheduler = schedulerDao.findByAddress(address);
      if(scheduler == null) {
         return;
      }
      
      SchedulerCapabilityDispatcher executor = createExecutor(scheduler);
      executor.delete();
   }

   @Override
   public void clear() {
      // no-op
   }

   private SchedulerCapabilityDispatcher createExecutor(ModelEntity scheduler) {
      Place place = placeDao.findById(UUID.fromString(SchedulerModel.getPlaceId(scheduler)));
      SchedulerContext context = new SchedulerContext();
      context.setCalculator(calculator);
      context.setTimeZone(timeZoneOf(place, scheduler.getAddress()));
      context.setLocation(locationOf(place));
      context.setScheduler(scheduler);
      // TODO replace this with assisted injection factory
      return new PlatformSchedulerCapabilityDispatcher(context, platformBus, schedulerDao, schedulerService, registry, populationCacheMgr);
   }

   private TimeZone timeZoneOf(Place place, Address scheduler) {
      TimeZone tz = null;
      if(place == null) {
         logger.warn("Missing place for scheduler [{}], reverting to server time", scheduler);
      }
      else if(StringUtils.isEmpty(place.getTzId())) {
         logger.warn("Missing timezone for place [{}], reverting to server time", place.getId());
      }
      else {
         try {
            tz = TimeZone.getTimeZone(place.getTzId());
         }
         catch(Exception e) {
            logger.warn("Error loading timezone [{}], reverting to server time", place.getTzId());
         }
      }
      if(tz == null) {
         tz = TimeZone.getDefault();
      }
      return tz;
   }

   private GeoLocation locationOf(Place place) {
      Double lat = place.getAddrLatitude();
      Double lon = place.getAddrLongitude();
      if(lat == null  || lon == null) {
         return null;
      }
      return GeoLocation.fromCoordinates(lat, lon);
   }

}

