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
package com.iris.resource.environment;

import java.net.URI;
import java.net.URISyntaxException;

import com.iris.resource.Resource;
import com.iris.resource.ResourceFactory;

/**
 * 
 */
public class EnvironmentVariableResourceFactory implements ResourceFactory {
   public static final String SCHEME = "env";
   
   /**
    * 
    */
   public EnvironmentVariableResourceFactory() {
   }
   
   @Override
   public String getScheme() {
      return SCHEME;
   }

   @Override
   public Resource create(URI uri) throws IllegalArgumentException {
      String variable = uri.getPath();
      if(variable.charAt(0) == '/') {
         variable = variable.substring(1);
      }
      try {
         return new EnvironmentVariableResource(variable);
      }
      catch(URISyntaxException e) {
         throw new IllegalArgumentException("Invalid URI variable: " + variable, e);
      }
   }

}

