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
package com.iris.platform.location;

import static com.iris.resource.Resources.getResource;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.resource.Resource;
import com.iris.resource.manager.BaseJsonParser;
import com.iris.resource.manager.DefaultSingleFileResourceManager;
import com.iris.resource.manager.SimpleMapCsvManager;
import com.iris.util.TypeMarker;

@Singleton
public class ResourceUspsDataService implements UspsDataService
{
   @Inject(optional = true)
   @Named("usps.directionals.resourceUri")
   private String directionalsUri   = "classpath:/directionals.data";

   @Inject(optional = true)
   @Named("usps.streetSuffixes.resourceUri")
   private String streetSuffixesUri = "classpath:/streetsuffixes.json";

   @Inject(optional = true)
   @Named("usps.secondaryUnits.resourceUri")
   private String secondaryUnitsUri = "classpath:/secondaryunits.data";

   @Inject(optional = true)
   @Named("usps.states.resourceUri")
   private String statesUri         = "classpath:/states.data";

   private SimpleMapCsvManager directionalsManager;
   private StreetSuffixesManager streetSuffixesManager;
   private SimpleMapCsvManager secondaryUnitsManager;
   private SimpleMapCsvManager statesManager;

   @PostConstruct
   public void init()
   {
      directionalsManager = new SimpleMapCsvManager(getResource(directionalsUri));

      streetSuffixesManager = new StreetSuffixesManager(getResource(streetSuffixesUri));

      secondaryUnitsManager = new SimpleMapCsvManager(getResource(secondaryUnitsUri));

      statesManager = new SimpleMapCsvManager(getResource(statesUri));
   }

   @Override
   public Map<String, String> getStates()
   {
      return statesManager.getParsedData();
   }

   @Override
   public Map<String, String> getDirectionals()
   {
      return directionalsManager.getParsedData();
   }

   @Override
   public List<StreetSuffix> getStreetSuffixes()
   {
      return streetSuffixesManager.getParsedData();
   }

   @Override
   public Map<String, StreetSuffix> getStreetSuffixesByCode()
   {
      return streetSuffixesManager.getStreetSuffixesByCode();
   }

   @Override
   public Map<String, StreetSuffix> getStreetSuffixesByName()
   {
      return streetSuffixesManager.getStreetSuffixesByName();
   }

   @Override
   public Map<String, StreetSuffix> getStreetSuffixesByVariant()
   {
      return streetSuffixesManager.getStreetSuffixesByVariant();
   }

   @Override
   public Map<String, String> getSecondaryUnits()
   {
      return secondaryUnitsManager.getParsedData();
   }

   private class StreetSuffixesManager extends DefaultSingleFileResourceManager<List<StreetSuffix>>
   {
      private volatile Map<String, StreetSuffix> streetSuffixesByCode;
      private volatile Map<String, StreetSuffix> streetSuffixesByName;
      private volatile Map<String, StreetSuffix> streetSuffixesByVariant;

      public StreetSuffixesManager(Resource managedResource)
      {
         super(managedResource,
            new BaseJsonParser<List<StreetSuffix>>()
            {
               @Override
               protected TypeMarker<List<StreetSuffix>> getTypeMarker()
               {
                  return TypeMarker.listOf(StreetSuffix.class);
               }
            });
      }

      @Override
      protected void loadCache()
      {
         super.loadCache();

         this.streetSuffixesByCode = unmodifiableMap(
            getCachedData().stream().collect(toMap(s -> s.getCode(), s -> s)));

         this.streetSuffixesByName = unmodifiableMap(
            getCachedData().stream().collect(toMap(s -> s.getName(), s -> s)));

         Map<String, StreetSuffix> streetSuffixesByVariant = new HashMap<>();

         for (StreetSuffix streetSuffix : getCachedData())
         {
            for (String variant : streetSuffix.getVariants())
            {
               streetSuffixesByVariant.put(variant, streetSuffix);
            }
         }

         this.streetSuffixesByVariant = unmodifiableMap(streetSuffixesByVariant);
      }

      public Map<String, StreetSuffix> getStreetSuffixesByCode()
      {
         return streetSuffixesByCode;
      }

      public Map<String, StreetSuffix> getStreetSuffixesByName()
      {
         return streetSuffixesByName;
      }

      public Map<String, StreetSuffix> getStreetSuffixesByVariant()
      {
         return streetSuffixesByVariant;
      }
   }
}

