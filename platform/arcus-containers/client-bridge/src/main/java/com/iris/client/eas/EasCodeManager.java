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
package com.iris.client.eas;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.iris.Utils;
import com.iris.resource.Resource;

/*
 * The EasCodeManager loads the EAS Code data file into memory. This
 * file is relatively small.  
 * 
 * The EasCodeManager will reload this data in an atomic fashion any time
 * one of the data files is updated. In the event of error the previously loaded
 * mappings will be kept in place. 
 */
public class EasCodeManager {

   private static final Logger logger = LoggerFactory.getLogger(EasCodeManager.class);

   private final Resource easCodesResource;

   private LoadingCache<Boolean, Map<String, EasCode>> easCodeCache = CacheBuilder.newBuilder().maximumSize(1).build(new CacheLoader<Boolean, Map<String, EasCode>>() {
      @Override
      public Map<String, EasCode> load(Boolean key) throws Exception {
         return loadEasCodes(key);
      }
   });

   public EasCodeManager(Resource easCodesResource) {
      this.easCodesResource = easCodesResource;

      /*
       * We want to reload the SAME Codes if either load file changes
       */
      if (easCodesResource.isWatchable()) {
         /*
          * Refresh vs Invalidate. In my case I would prefer the behavior of
          * refresh. In the case of exceptions Refresh will keep the old value.
          * Old value is also returned during the reload in refresh.
          */
         easCodesResource.addWatch(() -> easCodeCache.refresh(Boolean.TRUE));
      }else{
         logger.warn(
               "The Data Input Resource for EAS Codes should be \"watchable\" to allow for dynamic updates to the data");
      }
   }

   public List<EasCode> listEasCodes() {
      try{
         return ImmutableList.copyOf(new ArrayList<EasCode>(easCodeCache.get(Boolean.TRUE).values()));
      }catch (ExecutionException e){
         throw new UncheckedExecutionException(e.getCause());
      }
   }

   private Map<String, EasCode> loadEasCodes(Boolean key) throws IOException {

      try (InputStream is = easCodesResource.open()){
         EasCodeResourceLoader resourceLoader = new EasCodeResourceLoader();
         Map<String, EasCode> easCodes = resourceLoader.parse(is);
         Utils.assertNotNull(easCodes, "Error loading EAS Codes");
         return easCodes;
      }
   }
}

