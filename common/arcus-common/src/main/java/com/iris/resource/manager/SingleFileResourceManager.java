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
package com.iris.resource.manager;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import com.iris.resource.Resource;
import com.iris.resource.ResourceListener;

public abstract class SingleFileResourceManager<T> implements ResourceManager<T> {
   private final Resource managedResource;
   private final AtomicReference<T> cacheRef = new AtomicReference<>();
   private final ResourceParser<T> parser;
   
   public SingleFileResourceManager(Resource managedResource, ResourceParser<T> parser) {
      this.managedResource = managedResource;
      this.parser = parser;
      initManagedResource(managedResource);
   }
  
   
   private void initManagedResource(Resource resource) {
      loadCache();
      if (managedResource.isWatchable()) {
         managedResource.addWatch(new ResourceListener() {
            @Override
            public void onChange() {
               loadCache();
            }
         });
      }
   }
   
   public void addListener(ResourceListener listener) {
	   if(managedResource != null && managedResource.isWatchable()) {
		   managedResource.addWatch(listener);
	   }
   }
   
   protected T getCachedData() {
      return cacheRef.get();
   }
   
   protected void loadCache() {
      try (InputStream is = managedResource.open()) {
    	  T data = parser.parse(is);
         cacheRef.set(data);
      } catch (Exception e) {
         throw new RuntimeException("Unable to parse data at: " + managedResource.getRepresentation(), e);
      }
   }
}

