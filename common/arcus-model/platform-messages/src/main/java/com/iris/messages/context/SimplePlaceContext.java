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
package com.iris.messages.context;

import java.util.Calendar;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.iris.messages.model.ModelStore;
import com.iris.messages.type.Population;

/**
 * 
 */
public class SimplePlaceContext implements PlaceContext {

   private final UUID placeId;
   private final UUID accountId;
   private final String population;
   private final Logger logger;
   private final ModelStore models;
   private volatile Calendar localTime;

   public SimplePlaceContext(
         UUID placeId,
         UUID accountId,
         Logger logger,
         ModelStore models
   ) { 
      this(placeId, null, accountId, logger, models);
   }
   
   public SimplePlaceContext(
         UUID placeId,
         String population,
         UUID accountId,
         Logger logger,
         ModelStore models
   ) { 
      Preconditions.checkNotNull(placeId, "place may not be null");
      Preconditions.checkNotNull(accountId, "account may not be null");
      Preconditions.checkNotNull(logger, "logger may not be null");
      Preconditions.checkNotNull(models, "models may not be null");
      this.placeId = placeId;
      this.population = population!=null?population:Population.NAME_GENERAL;
      this.accountId = accountId;
      this.logger = logger;
      this.models = models;
   }
   
   public void setLocalTime(@Nullable Calendar localTime) {
      if(localTime == null) {
         clearLocalTime();
      }
      this.localTime = (Calendar) localTime.clone();
   }
   
   public void clearLocalTime() {
      this.localTime = null;
   }
   
   @Override
   public UUID getPlaceId() {
      return placeId;
   }

   @Override
   public UUID getAccountId() {
      return accountId;
   }

   @Override
   public ModelStore models() {
      return models;
   }

   @Override
   public Logger logger() {
      return logger;
   }
   
   @Override
   public Calendar getLocalTime() {
      Calendar localTime = this.localTime;
      if(localTime == null) {
         return Calendar.getInstance();
      }
      return (Calendar) localTime.clone();
   }

	@Override
	public String getPopulation() {
		return population;
	}

}

