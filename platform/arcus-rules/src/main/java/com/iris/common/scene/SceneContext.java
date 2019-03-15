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
package com.iris.common.scene;

import com.iris.common.rule.action.ActionContext;
import com.iris.messages.address.Address;
import com.iris.messages.model.PersistentModel;

public interface SceneContext extends ActionContext {
   
   boolean isPersisted();
   
   boolean isDeleted();
   
   PersistentModel model();

   /**
    * Requests that the context be deleted when
    * commit() is called next.
    */
   void delete();
   
   /**
    * Saves any changes to model() and emits necessary
    * events.
    */
   void commit();
   
   void setActor(Address actor);
}

