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
package com.iris.firmware.ota;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.PopulationDAO;
import com.iris.resource.Resource;
import com.iris.resource.Resources;

@Singleton
public class DeviceOTAFirmwareResolver {

   public static final String SERVICE_NAME = "DeviceOTAFirmwareResolver";
   private static final Logger LOGGER = LoggerFactory.getLogger(DeviceOTAFirmwareResolver.class);

   @Inject(optional = true)
   @Named(value = "device.ota.firmware.update.path")
   private String deviceOTAFirmwareUpdatePath = "classpath:/device-ota-firmware.xml";
   
   
   Resource firmwareFileResource = null;
   private AtomicReference<Multimap<String, DeviceOTAFirmwareItem>> firmwareIndex =
         new AtomicReference<Multimap<String, DeviceOTAFirmwareItem>>();

   private final PopulationDAO populationDao;
   private DeviceOTAFirmwareURLBuilder urlBuilder;
   
   DeviceOTAFirmware firmwares;

   @Inject
   public DeviceOTAFirmwareResolver(PopulationDAO populationDao, DeviceOTAFirmwareURLBuilder urlBuilder) {
      super();
      this.populationDao = populationDao;
      this.urlBuilder = urlBuilder;
   }

   // Backwords compatible or when retry count does not matter.
   public DeviceOTAFirmwareResponse resolve(Optional<String> population, String productId, String currentVersion) {
	   return resolve(population,productId,currentVersion,0);
   }
   
   public DeviceOTAFirmwareResponse resolve(Optional<String> population, String productId, String currentVersion, int retryCount) {
      Preconditions.checkNotNull(productId, "productId must not be null.");
      String defaultPopulation = populationDao.getDefaultPopulation().getName();
      String populationStr = population.orElse(defaultPopulation);

      // Get this correct collection of firmwares
      Collection<DeviceOTAFirmwareItem> firmwareCollection = firmwareIndex.get().get(DeviceOTAFirmwareItem.createProductKey(populationStr, productId));
      
      DeviceOTAFirmwareItem fw  = firmwareCollection
      		.stream()
      		.filter((item) -> item.isVersionUpgradable(currentVersion))
      		.findFirst()
      		.orElse(null);

      DeviceOTAFirmwareResponse response = null;
      if (fw == null) {
          response = new DeviceOTAFirmwareResponse(false);    	  
      }else{
         int attempts = fw.getRetryAttemptsMax() == null?firmwares.getRetryAttemptsMax():fw.getRetryAttemptsMax();
         int interval = fw.getRetryIntervalMins() == null?firmwares.getRetryIntervalMins():fw.getRetryIntervalMins();
         boolean isUpgradable = fw.isVersionUpgradable(currentVersion) && (retryCount < attempts);
         response = new DeviceOTAFirmwareResponse( isUpgradable,               
                                                   fw.getVersion(), 
                                                   urlBuilder.buildURL(productId, fw.getPath()),
                                                   attempts,
                                                   interval,
                                                   fw.getMd5()
                                                 );
      }
      return response;
   }
   
   public List<DeviceOTAFirmwareItem> getFirmwares() {
	   return firmwares.getFirmwares();
   }

   private Multimap<String, DeviceOTAFirmwareItem> loadFirmwareFile() {
      Multimap<String, DeviceOTAFirmwareItem> index = HashMultimap.create();
      firmwares = new DeviceOTAFirmwareDeserielizer().deserialize(firmwareFileResource);
      
      for (DeviceOTAFirmwareItem firmware : firmwares.getFirmwares()){
         for (String key : firmware.productKeys()){
            index.put(key, firmware);
         }
      }
      return index;
   }

   @PostConstruct
   public void init() {
      LOGGER.info("Loading device firmware file - {}", deviceOTAFirmwareUpdatePath);
      firmwareFileResource = Resources.getResource(deviceOTAFirmwareUpdatePath);
      if (firmwareFileResource.isWatchable()){
         firmwareFileResource.addWatch(() -> {
            firmwareFileUpdated();
         });
      }
      firmwareIndex.set(loadFirmwareFile());
   }

   private void firmwareFileUpdated() {
      LOGGER.info("firmware file was updated");
      firmwareIndex.set(loadFirmwareFile());
   }
   
   public void add(DeviceOTAFirmwareItem item) {
      firmwareIndex.get().put(DeviceOTAFirmwareItem.createProductKey(item.getPopulation(),item.getProductId()), item );
   }

}

