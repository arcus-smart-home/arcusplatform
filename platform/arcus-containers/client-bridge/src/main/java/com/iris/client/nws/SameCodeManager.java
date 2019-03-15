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
package com.iris.client.nws;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.iris.Utils;
import com.iris.resource.Resource;

/*
 * The SameCodeManager loads the NWS SAME Code data file into memory. This
 * file is relatively small. The manager is also responsible for loading a
 * Short 2 or 3 character abbreviation {@code stateCode} closely aligned 
 * to US Postal codes that map states and territories to their long name 
 * representations. The NWS Mappings use the same mappings as provided by 
 * the US Census bureau FIPS system.
 * 
 * Mappings are typically keyed off the the abbreviated stateCode
 * 
 * The SameCodeManager will reload this data in an atomic fashion any time
 * one of the data files is updated. In the event of error the previously loaded
 * mappings will be kept in place. 
 */
public class SameCodeManager {

   private static final Logger logger = LoggerFactory.getLogger(SameCodeManager.class);

   private final Resource sameCodesResource;
   private final Resource stateCodeMappingResource;

   private LoadingCache<Boolean, SameCodeRegistry> sameCodes = CacheBuilder.newBuilder().maximumSize(1).build(new CacheLoader<Boolean, SameCodeRegistry>() {
      @Override
      public SameCodeRegistry load(Boolean key) throws Exception {
         return loadSAMECodes(key);
      }
   });

   public SameCodeManager(Resource sameCodesResource, Resource stateCodeMappingResource) {
      this.sameCodesResource = sameCodesResource;
      this.stateCodeMappingResource = stateCodeMappingResource;

      /*
       * We want to reload the SAME Codes if either load file changes
       */
      if (sameCodesResource.isWatchable()) {
         /*
          * Refresh vs Invalidate. In my case I would prefer the behavior of
          * refresh. In the case of exceptions Refresh will keep the old value.
          * Old value is also returned during the reload in refresh.
          */
         sameCodesResource.addWatch(() -> sameCodes.refresh(Boolean.TRUE));
      }else{
         logger.warn(
               "The Data Input Resource for NWS SAME Codes should be \"watchable\" to allow for dynamic updates to the data");
      }

      if (stateCodeMappingResource.isWatchable()) {
         /*
          * Refresh vs Invalidate. In my case I would prefer the behavior of
          * refresh. In the case of exceptions Refresh will keep the old value.
          * Old value is also returned during the reload in refresh.
          */
         stateCodeMappingResource.addWatch(() -> sameCodes.refresh(Boolean.TRUE));
      }else{
         logger.warn(
               "The Data Input Resource for NWS State Code Mappings should be \"watchable\" to allow for dynamic updates to the data");
      }
   }

   /*
    * Get a sorted list of all SAME Codes
    */
   public List<SameCode> listSameCodes() {
      // SAME Codes are immutable
      try{
         return ImmutableList.copyOf(new ArrayList<SameCode>(sameCodes.get(Boolean.TRUE).getSameCodes().values()));
      }catch (ExecutionException e){
         throw new UncheckedExecutionException(e.getCause());
      }
   }

   /*
    * Given a State Code and a Count return the 6 Character SAME Code
    */
   public String getSameCode(String stateCode, String county) {
      // SAME Codes are immutable
      try{
         return sameCodes.get(Boolean.TRUE).getSameCodes().get(stateCode, county).getCode();
      }catch (ExecutionException e){
         throw new UncheckedExecutionException(e.getCause());
      }
   }

   /*
    * Get a List of all counties for a given 2 or 3 character state code
    */
   public List<String> listSameCounties(String stateCode) {
      try{
         ListMultimap<String, String> sameCounties = sameCodes.get(Boolean.TRUE).getSameCounties();
         List<String> sameCountiesForState = sameCounties.get(stateCode);
         return ImmutableList.copyOf(sameCountiesForState);
      }catch (ExecutionException e){
         throw new UncheckedExecutionException(e.getCause());
      }
   }

   /*
    * Get a map of all State Names Keyed by the 2 or 3 character State Code
    * 
    * Go Guava
    */
   public List<SameState> listSameStates() {
      // SameStates are immutable
      try{
         return ImmutableList.copyOf(sameCodes.get(Boolean.TRUE).getSameStates());
      }catch (ExecutionException e){
         throw new UncheckedExecutionException(e.getCause());
      }
   }

   private SameCodeRegistry loadSAMECodes(Boolean key) throws IOException {
      Map<String, SameState> sameStates = loadStateCodeMappings();

      try (InputStream is = sameCodesResource.open()){
         SameCodeResourceLoader resourceLoader = new SameCodeResourceLoader(sameStates);
         SameCodeRegistry registry = resourceLoader.parse(is);
         // Cache Loader does not accept nulls
         Utils.assertNotNull(registry, "Error loading SAME Codes");
         return registry;
      }
   }

   /*
    * The {@code loadStateCodeMappings} loads NWS SAME Code State Name Mappings
    * for each two character US Postal state code abbreviation that is
    * referenced in the NWS SAME Code data file.
    * 
    * Each mapping will be stored in a {@link java.util.Map Map} on a state by
    * state basis.
    * 
    * For SAME Code mappings by County and State source data: See {@linktourl
    * http://www.nws.noaa.gov/nwr/data/SameCode.txt}
    * 
    * For Specific County Coverage Listings by State source data: See
    * {@linktourl http://www.nws.noaa.gov/nwr/data/SameCode.txt}
    */
   private Map<String, SameState> loadStateCodeMappings() throws IOException {
      try (InputStream is = stateCodeMappingResource.open()){
         StateCodeMappingResourceLoader resourceLoader = new StateCodeMappingResourceLoader();
         return resourceLoader.parse(is);
      }
   }
}

