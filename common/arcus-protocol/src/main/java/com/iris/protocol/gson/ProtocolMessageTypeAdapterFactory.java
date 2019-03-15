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
package com.iris.protocol.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.protocol.ProtocolMessage;

public class ProtocolMessageTypeAdapterFactory implements TypeAdapterFactory {
   @Inject(optional=true) @Named("gson.serialize.optimize.expired.default.ttl")
   private int optimizeExpiredDefaultTtl = 0;

   @Override
   public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      if (type.getRawType() == ProtocolMessage.class) {
         return (TypeAdapter<T>)new ProtocolMessageTypeAdapter(optimizeExpiredDefaultTtl);
      }

      return null;
   }
}

