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
package com.iris.driver.service.matcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.messages.model.DriverId;
import com.iris.util.MatchList;

/**
 *
 */
public class SortedDiscoveryAlgorithmFactory implements DiscoveryAlgorithmFactory {

   public static Comparator<DeviceDriver> getDefaultComparator() {
      return DefaultComparator.INSTANCE;
   }

   private final Comparator<DeviceDriver> comparator;

   public SortedDiscoveryAlgorithmFactory() {
      this(getDefaultComparator());
   }

   public SortedDiscoveryAlgorithmFactory(Comparator<DeviceDriver> comparator) {
      this.comparator = comparator;
   }

   @Override
   public DiscoveryAlgorithm create(Collection<DeviceDriver> drivers) {
      List<DeviceDriver> sorted = new ArrayList<>(drivers);

      Collections.sort(sorted, comparator);
      Map<String,MatchList.Builder<MatchListDiscoveryAlgorithm.ReflexVersionAndAttributes, DriverId>> builders = new HashMap<>();
      for(DeviceDriver driver: sorted) {
        DriverPredicate predicate = new DriverPredicate(driver);

         Collection<String> populations = driver.getDefinition().getPopulations();
         for (String population : populations) {
            MatchList.Builder<MatchListDiscoveryAlgorithm.ReflexVersionAndAttributes, DriverId> builder = builders.get(population);
            if (builder == null) {
               builder = MatchList.builder();
               builders.put(population, builder);
            }
         
            builder.addValue(predicate, driver.getDriverId());
         }
      }

      ImmutableMap.Builder<String,MatchList<MatchListDiscoveryAlgorithm.ReflexVersionAndAttributes,DriverId>> matchers = ImmutableMap.builder();
      for (Map.Entry<String,MatchList.Builder<MatchListDiscoveryAlgorithm.ReflexVersionAndAttributes,DriverId>> entry : builders.entrySet()) {
         matchers.put(entry.getKey(), entry.getValue().create());
      }

      return new MatchListDiscoveryAlgorithm(matchers.build());
   }

   private static class DriverPredicate implements Predicate<MatchListDiscoveryAlgorithm.ReflexVersionAndAttributes> {
      private final DeviceDriver driver;
      private final int minimumRequiredReflexVersion;

      DriverPredicate(DeviceDriver driver) {
         this.driver = driver;
         this.minimumRequiredReflexVersion = driver.getDefinition().getMinimumRequiredReflexVersion();
      }

      @Override
      public boolean apply(MatchListDiscoveryAlgorithm.ReflexVersionAndAttributes rvaa) {
         if (minimumRequiredReflexVersion > rvaa.getMaxReflexVersion()) {
            return false;
         }

         return driver.supports(rvaa.getProtocolAttributes());
      }

   }

   private static class DefaultComparator implements Comparator<DeviceDriver> {
      static final DefaultComparator INSTANCE = new DefaultComparator();

      private DefaultComparator() {

      }

      @Override
      public int compare(DeviceDriver o1, DeviceDriver o2) {
         DriverId id1 = o1.getDriverId();
         DriverId id2 = o2.getDriverId();
         return id1.compareTo(id2);
      }

   }

}

