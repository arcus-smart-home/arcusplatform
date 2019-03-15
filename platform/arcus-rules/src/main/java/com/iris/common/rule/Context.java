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
package com.iris.common.rule;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;

import com.iris.common.rule.event.ScheduledEventHandle;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;

/**
 * A context that gives access to the local environment.  This
 * context is generally associated with a place.
 */
public interface Context {
   
   UUID getPlaceId();
   
   String getPopulation();
   
   boolean isPremium();

   String getServiceLevel();

   Logger logger();
   
   Calendar getLocalTime();
   
   Iterable<Model> getModels();

   @Nullable Model getModelByAddress(Address address);
   
   @Nullable Object getAttributeValue(Address address, String attributeName);

   ScheduledEventHandle wakeUpIn(long time, TimeUnit unit);
   
   ScheduledEventHandle wakeUpAt(Date timestamp);
   
}

