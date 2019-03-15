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
package com.iris.tools.kat;

import java.time.Instant;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.iris.kafka.util.Instants;

/**
 * 
 */
public class StartTimeConverter implements IStringConverter<Instant>, IParameterValidator {
   public static final String EARLIEST = "earliest";
   public static final String LATEST = "latest";

   @Override
   public Instant convert(String value) {
      if(EARLIEST.equalsIgnoreCase(value)) {
         return Instant.ofEpochMilli(kafka.api.OffsetRequest.EarliestTime());
      }
      
      if(LATEST.equalsIgnoreCase(value)) {
         return Instant.ofEpochMilli(kafka.api.OffsetRequest.LatestTime());
      }
      
      try {
         return Instants.parse(value);
      }
      catch(IllegalArgumentException e) {
         throw new ParameterException("Invalid start date: " + value + ".\nMust be 'earliest', 'latest', day as '2015-12-2', time as 'T11:12:14ZCST' or offset as '-6d4h20m10s'.");
      }
   }

   @Override
   public void validate(String name, String value) throws ParameterException {
      convert(value);
   }

}

