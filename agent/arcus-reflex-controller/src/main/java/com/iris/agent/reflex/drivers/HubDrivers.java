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
package com.iris.agent.reflex.drivers;

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.agent.reflex.ReflexController;
import com.iris.messages.address.Address;
import com.iris.model.Version;

public final class HubDrivers {
   private static final Logger log = LoggerFactory.getLogger(HubDrivers.class);

   private HubDrivers() {
   }

   public static @Nullable Factory getFactory(String driver, Version version) {
      Map<Version,Factory> driverFactoryByVersion = FACTORIES.get(driver);
      Factory driverFactory = (driverFactoryByVersion != null) ? driverFactoryByVersion.get(version) : null;

      if (driverFactory == null && driverFactoryByVersion != null) {
         log.warn("cannot find exact match for hub driver using highest known driver version instead: {} {}", driver, version);

         Version vers = null;
         Factory best = null;
         for (Map.Entry<Version,Factory> drivers : driverFactoryByVersion.entrySet()) {
            if (vers == null || drivers.getKey().compareTo(vers) > 0) {
               vers = drivers.getKey();
               best = drivers.getValue();
            }
         }

         driverFactory = best;
      }

      if (driverFactory == null) {
         log.warn("could not find hub driver for: {} {}", driver, version);
         return null;
      }

      return driverFactory;
   }

   private static final Map<String,Map<Version,Factory>> FACTORIES;
   static {
      FACTORIES = ImmutableMap.<String,Map<Version,Factory>>builder()
         .put(CentraLiteKeyPad.DRIVER_NAME, ImmutableMap.of(
            CentraLiteKeyPad.VERSION_2_4, new Factory() {
               @Override
               public HubDriver create(ReflexController parent, Address addr) {
                  return new CentraLiteKeyPad(parent, addr);
               }
		
               @Override
		         public String driver() {
		            return CentraLiteKeyPad.DRIVER_NAME;
		         }
		
               @Override
		         public Version version() {
		            return CentraLiteKeyPad.VERSION_2_4;
		         }
            }))
         .put(GreatStarKeyPad.DRIVER_NAME, ImmutableMap.of(
            GreatStarKeyPad.VERSION_2_12, new Factory() {
               @Override
               public HubDriver create(ReflexController parent, Address addr) {
                  return new GreatStarKeyPad(parent, addr);
               }
		
               @Override
		         public String driver() {
		            return GreatStarKeyPad.DRIVER_NAME;
		         }
		
               @Override
		         public Version version() {
		            return GreatStarKeyPad.VERSION_2_12;
		         }
            }))
         .put(AlertmeKeyPad.DRIVER_NAME, ImmutableMap.of(
            AlertmeKeyPad.VERSION_2_4, new Factory() {
               @Override
               public HubDriver create(ReflexController parent, Address addr) {
                  return new AlertmeKeyPad(parent, addr);
               }
		
               @Override
		         public String driver() {
		            return AlertmeKeyPad.DRIVER_NAME;
		         }
		
               @Override
		         public Version version() {
		            return AlertmeKeyPad.VERSION_2_4;
		         }
            }))
         .build();
   }

   public static interface Factory {
		HubDriver create(ReflexController parent, Address addr);
		String driver();
		Version version();
   }
}

