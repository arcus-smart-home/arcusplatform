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
package com.iris.messages.model;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

public interface PersistentModel extends Model {

   boolean isPersisted();
   
   boolean isDirty();
   
   @Nullable Date getCreated();
   
   @Nullable Date getModified();
   
   /**
    * Gets the names of all the attributes that have
    * been added, updated or removed since the last
    * time this model was committed.
    * @return
    */
   Set<String> getDirtyAttributeNames();

   /**
    * Gets the name and values of all the attributes that
    * have added, updated or removed since the last
    * time this model was committed.
    * Note that the value may be {@code null} in the
    * case of a deleted attribute.
    * @return
    */
   Map<String, Object> getDirtyAttributes();
   
   /**
    * Clears the deleted attributes, resetting the
    * dirty flag.  This also returns all the
    * attributes that were previously dirty.
    * @return
    */
   Map<String, Object> clearDirtyAttributes();
}

