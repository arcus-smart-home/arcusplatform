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
package com.iris.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.iris.Utils;
import com.iris.resource.config.ResourceConfigurer;
import com.iris.resource.config.ResourceModule;
import com.iris.util.IrisCollections;

/**
 * Static helper methods for loading resource.  Note that
 * this is not injected so that it can be used in cases
 * where low-level, early binding is necessary.  However,
 * the {@link ResourceModule} and/or {@link ResourceConfigurer}
 * may be used when injection is a possibility.
 */
public class Resources {

   public static Resource getResource(String uri) {
      return getResource(toUri(uri));
   }
   
   public static Resource getResource(URI uri) {
      Utils.assertNotNull(uri, "Must specify a uri");
      String scheme = uri.getScheme();
      return getImplementation().getFactory(scheme).create(uri);
   }
   
   public static InputStream open(String uri) throws IOException {
      return open(toUri(uri));
   }
   
   public static InputStream open(URI uri) throws IOException {
      return getResource(uri).open();
   }
   
   public static void registerDefaultFactory(ResourceFactory factory) {
      getImplementation().registerDefaultFactory(factory);
   }
   
   public static void registerFactory(ResourceFactory factory) {
      getImplementation().registerFactory(factory);
   }
   
   // TODO create?
   // TODO watch?
   
   private static URI toUri(String uri) {
      try {
         return new URI(uri);
      }
      catch(URISyntaxException e) {
         throw new IllegalArgumentException("Invalid URI: " + uri, e);
      }
   }
   
   private static Implementation getImplementation() {
      return Implementation.INSTANCE;
   }
   
   private static class Implementation {
      private static final Implementation INSTANCE = new Implementation();
      
      private final Map<String, ResourceFactory> factories =
            IrisCollections
               .<String, ResourceFactory>concurrentMap(1)
               .create();
      
      ResourceFactory getFactory(String scheme) {
         if(scheme == null) {
            scheme = "";
         }
         ResourceFactory factory = factories.get(scheme);
         if(factory == null) {
            if(StringUtils.isEmpty(scheme)) {
               throw new IllegalArgumentException("No default resource scheme is registered, please call registerDefaultFactory");
            }
            else {
               throw new IllegalArgumentException("No resource factory is registered for scheme [" + scheme + "], please call registerFactory");
            }
         }
         return factory;
      }

      public void registerFactory(ResourceFactory factory) {
         Utils.assertNotNull(factory, "ResourceFactory may not be null");
         Utils.assertNotEmpty(factory.getScheme(), "ResourceFactory must specify a scheme");
         factories.put(factory.getScheme(), factory);
      }

      public void registerDefaultFactory(ResourceFactory factory) {
         Utils.assertNotNull(factory, "ResourceFactory may not be null");
         factories.put("", factory);
      }
   }
}

