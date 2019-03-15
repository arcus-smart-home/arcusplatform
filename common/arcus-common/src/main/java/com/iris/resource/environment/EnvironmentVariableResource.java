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
package com.iris.resource.environment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.codec.binary.Base64InputStream;

import com.google.common.collect.ImmutableList;
import com.iris.resource.AbstractResource;
import com.iris.resource.Resource;
import com.iris.resource.ResourceNotFoundException;

public class EnvironmentVariableResource extends AbstractResource {
   private final String variable;
   
   EnvironmentVariableResource(String variable) throws URISyntaxException {
      super(new URI(EnvironmentVariableResourceFactory.SCHEME, "/" + variable, null));
      this.variable = variable;
   }
   
   @Override
   public boolean exists() {
      return System.getenv().containsKey(variable);
   }

   @Override
   public boolean isReadable() {
      return exists();
   }

   @Override
   public boolean isFile() {
      return false;
   }
   
   @Override
   public boolean isDirectory() {
      return false;
   }

   @Override
   public File getFile() {
      return null;
   }

   /* (non-Javadoc)
    * @see com.iris.resource.AbstractResource#open()
    */
   @Override
   public InputStream open() throws IOException {
      String value = System.getenv(variable);
      if(value == null) {
         throw new ResourceNotFoundException("No environment variable named '" + variable + "' is set");
      }
      return new Base64InputStream(new ByteArrayInputStream(value.getBytes()));
   }

   @Override
   public List<Resource> listResources() {
      return ImmutableList.of();
   }

}

