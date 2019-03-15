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
package com.iris.voice.google.homegraph;

import java.text.MessageFormat;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.iris.messages.model.Model;

/**
 * Google gRPC builds their JSON objects with the use of Struct and Value classes.  Our JSON is built with Map<String, Object> and GSON.
 * This utility class transforms our Maps into Google's Struct/Value tree.
 * 
 * The logic for converting a device model into Google traits is defined in {@link com.iris.google.Transformers#modelToStateMap(Model, boolean)}
 * It's used in both the Sync Response and the ReportState apis.  Both calls need the Google Traits, but the Sync is a response that goes over the bus
 * while ReportState calls to Google directly with gRPC.  Because there are two response paths, we need two different data formats.
 */
public enum TransformToGoogleValue {
   ;

   private static final Logger logger = LoggerFactory.getLogger(TransformToGoogleValue.class);

   public static Value transformMapToValue(Map<String, Object> items) {
      com.google.protobuf.Struct.Builder builder = Struct.newBuilder();

      items.keySet().forEach(key -> {
         Object obj = items.get(key);

         if (obj != null) {
            Value value = transformObjectToValue(obj);
            builder.putFields(key, value);
         }
         else {
            logger.trace("Object value null for key [{}]", key);
         }
      });

      Value rtn = Value.newBuilder().setStructValue(builder.build()).build();

      return rtn;
   }

   private static Value transformObjectToValue(Object obj) {
      if (obj instanceof Integer) {
         return Value.newBuilder().setNumberValue((Integer) obj).build();
      }

      if (obj instanceof Double) {
         return Value.newBuilder().setNumberValue((Double) obj).build();
      }

      if (obj instanceof Boolean) {
         return Value.newBuilder().setBoolValue((Boolean) obj).build();
      }

      if (obj instanceof String) {
         return Value.newBuilder().setStringValue((String) obj).build();
      }

      if (obj instanceof Map) {
         @SuppressWarnings("unchecked") // all of this hinges on Transformers.modelToStateMap
         Map<String, Object> oMap = (Map<String, Object>) obj;
         return transformMapToValue(oMap);
      }

      throw new IllegalArgumentException(MessageFormat.format("Object is of unknow type [{0}]", obj.getClass().getName()));
   }
}

