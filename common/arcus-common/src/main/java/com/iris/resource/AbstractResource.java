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

import com.iris.util.Subscription;

/**
 * 
 */
public abstract class AbstractResource implements Resource {
   private final URI uri;
   
   protected AbstractResource(URI uri) {
      this.uri = uri;
   }

   @Override
   public InputStream open() throws IOException {
      return uri.toURL().openStream();
   }

   @Override
   public String getRepresentation() {
      return uri.toString();
   }

   @Override
   public URI getUri() {
      return uri;
   }

   @Override
   public boolean isWatchable() {
      return false;
   }

   @Override
   public Subscription addWatch(ResourceListener listener) {
      throw new UnsupportedOperationException("Watch capability is not implemented for this type of resource.");
   }

   @Override
   public void dispose() {
      // Do nothing by default.
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " [" + getRepresentation() + "]";
   }

}

