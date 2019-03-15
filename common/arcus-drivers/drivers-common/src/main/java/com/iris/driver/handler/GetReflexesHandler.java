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
package com.iris.driver.handler;

import com.google.gson.JsonElement;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.DeviceDriverDefinition;
import com.iris.driver.reflex.ReflexDriverDefinition;
import com.iris.driver.reflex.ReflexJson;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.DeviceAdvancedCapability;

public class GetReflexesHandler implements ContextualEventHandler<MessageBody> {
   private JsonElement json;

   public GetReflexesHandler(DeviceDriverDefinition def) {
      ReflexDriverDefinition rdef = def.getReflexes();
      this.json = (rdef == null) ? null : ReflexJson.toJsonObject(def.getReflexes());
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, MessageBody event) throws Exception {
      context.respondToPlatform(
         DeviceAdvancedCapability.GetReflexesResponse.builder()
            .withReflexes(json)
            .build()
      );

      return true;
   }
}

