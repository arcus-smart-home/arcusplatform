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

import java.lang.reflect.Type;

import com.google.common.base.Predicate;
import com.iris.type.handler.CollectionHandler;

public class SupportedCollectionPredicate implements Predicate<Type> {
   private final CollectionHandler handler;
   private final Class<?> containedClazz;
   
   public SupportedCollectionPredicate(CollectionHandler handler, Class<?> containedClazz) {
      this.handler = handler;
      this.containedClazz = containedClazz;
   }

   @Override
   public boolean apply(Type input) {
      return input != null ? handler.isSupportedType(containedClazz, input) : false;
   }

}

