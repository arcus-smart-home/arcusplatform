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
package com.iris.driver.service.init;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PopulationDAO;
import com.iris.device.attributes.AttributeKey;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.service.DeviceInitializer;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.model.Device;
import com.iris.messages.type.Population;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.prodcat.ProductCatalog;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;

/**
 * 
 */
@Singleton
public class DefaultNameInitializer implements DeviceInitializer {
   static final AttributeKey<String> ATTR_PRODUCT_ID =
         AttributeKey.create(DeviceCapability.ATTR_PRODUCTID, String.class);
   static final AttributeKey<String> ATTR_NAME =
         AttributeKey.create(DeviceCapability.ATTR_NAME, String.class);
   static final Pattern NAME = Pattern.compile("(.*)\\s(\\d+)");
   
   private static final Logger logger = LoggerFactory.getLogger(DefaultNameInitializer.class);

   @Inject(optional = true) @Named("driver.defaultName")
   private String defaultName = "New Device";
   
   private final ProductCatalogManager catalogManager;
   private final PopulationDAO populationDao;
   private final PlacePopulationCacheManager populationCacheMgr;
   private final DeviceDAO deviceDao;
   
   /**
    * 
    */
   @Inject
   public DefaultNameInitializer(
         ProductCatalogManager catalogManager,
         PlacePopulationCacheManager populationCacheMgr,
         PopulationDAO populationDao,
         DeviceDAO deviceDao
   ) {
      this.catalogManager = catalogManager;
      this.populationCacheMgr = populationCacheMgr;
      this.deviceDao = deviceDao;
      this.populationDao = populationDao;
   }

   /* (non-Javadoc)
    * @see com.iris.driver.service.DeviceInitializer#intialize(com.iris.driver.DeviceDriver)
    */
   @Override
   public void intialize(DeviceDriverContext context) {
      String name;
      try {
         String productId = context.getAttributeValue(ATTR_PRODUCT_ID);
         String population = getPopulation(context.getPlaceId());
         name = getNameForProductId(productId, population);
         name = uniquefy(context.getPlaceId(), name);
      }
      catch(Exception e) {
         logger.warn("Error resolving default name", e);
         name = defaultName;
      }
      logger.debug("Setting device name to {}", name);
      context.setAttributeValue(ATTR_NAME, name);
   }

   String getPopulation(UUID placeId) {
   	return populationCacheMgr.getPopulationByPlaceId(placeId);
   }

   String getNameForProductId(String productId, String population) {
      ProductCatalogEntry entry = getCatalogForPopulation(population).getProductById(productId);
      if(entry == null) {
         logger.warn("No product: [{}] found for population: [{}] falling back to defaultName",productId, population);
         return defaultName;
      }
      
      return String.format("%s %s", entry.getVendor(), !StringUtils.isEmpty(entry.getShortName()) ? entry.getShortName() : (!StringUtils.isEmpty(entry.getName()) ? entry.getName() : "")).trim();
   }

   private ProductCatalog getCatalogForPopulation(String populationName) {
      Population population = populationName != null ? populationDao.findByName(populationName) : null;
      if(population == null) {
         logger.warn("No population with id [{}] found, falling back to default population", populationName);
         population = populationDao.getDefaultPopulation();
      }
      
      ProductCatalog catalog = catalogManager.getCatalog(population.getName());
      if(catalog == null) {
         logger.warn("No catalog for population [{}], falling back to default catalog", population.getName());
         catalog = catalogManager.getCatalog(populationDao.getDefaultPopulation().getName());
      }
      return catalog;
   }

	String uniquefy(UUID placeId, String name) {
      // to really make this safe we should hold a lock or share an atomic counter around the place...
      int count = 0;
      
      for(Device device: deviceDao.listDevicesByPlaceId(placeId)) {
         if(StringUtils.isEmpty(device.getName())) {
            continue;
         }
         
         try {
            Matcher m = NAME.matcher(device.getName());
            if(!m.matches()) {
               continue;
            }
            
            String deviceName = m.group(1);
            int deviceCount = Integer.parseInt(m.group(2));
            if(StringUtils.equals(name, deviceName) && deviceCount > count) {
               count = deviceCount;
            }
         }
         catch(NumberFormatException e) {
            // not a real int, ignore
            continue;
         }
      }
      return name + " "  + (count + 1);
   }

}

