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
package com.iris.core.dao;

import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeType.ObjectType;
import com.iris.io.json.JSON;
import com.iris.util.TypeMarker;

public class DaoUtils
{
   private static final Logger logger = getLogger(DaoUtils.class);

   public static Map<String, String> encodeAttributesToJson(Map<String, Object> attributes)
   {
      if (attributes == null) return ImmutableMap.of();

      return attributes.entrySet().stream().collect(
         toMap(Entry::getKey, e -> JSON.toJson(e.getValue())));
   }

   public static Map<String, Object> decodeAttributesFromJson(Map<String, String> encodedAttributes,
      ObjectType containerType)
   {
      if (encodedAttributes == null) return ImmutableMap.of();

      return encodedAttributes.entrySet().stream().collect(
         toMap(Entry::getKey, e -> decodeAttributeFromJson(e.getKey(), e.getValue(), containerType)));
   }

   public static Object decodeAttributeFromJson(String name, String encodedValue, ObjectType containerType)
   {
      AttributeType attributeType = containerType.getAttributes().get(name);

      if (attributeType == null)
      {
         logger.warn("Unrecognized attribute [{}] may lose type information when deserialized", name);

         return TypeMarker.object();
      }

      TypeMarker<?> marker = TypeMarker.wrap(attributeType.getJavaType());

      return JSON.fromJson(encodedValue, marker);
   }

   private DaoUtils() { }
}

