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
package com.iris.type.functional;

import java.io.Serializable;
import java.util.Collection;

import com.google.common.base.Function;
import com.iris.type.handler.CollectionFactory;
import com.iris.type.handler.CollectionHandler;

@SuppressWarnings("serial")
public class CollectionTransformer<T, C extends Collection<T>> extends DescribedFunction implements Function<Object, C>, Serializable {
   private final CollectionHandler handler;
   private final Class<T> containedClazz;
   private final CollectionFactory<T, C> factory;
   
   public CollectionTransformer(CollectionHandler handler, Class<T> containedClazz, CollectionFactory<T, C> factory) {
      this(handler, containedClazz, factory, "toCollectionOf" + containedClazz.getSimpleName());
   }
   
   public CollectionTransformer(CollectionHandler handler, Class<T> containedClazz, CollectionFactory<T, C> factory, String description) {
      super(description);
      this.handler = handler;
      this.containedClazz = containedClazz;
      this.factory = factory;
   }

   @Override
   public C apply(Object input) {
      if (input == null) {
         return null;
      }
      return handler.coerce(containedClazz, input, factory);
   }
}

