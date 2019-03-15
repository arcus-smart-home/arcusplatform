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
package com.iris.capability.attribute;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.ValidationException;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.iris.Utils;
import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.attribute.transform.AttributeMapTransformer;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.device.attributes.AttributeValue;
import com.iris.device.model.AttributeDefinition;

/**
 * @author ted
 *
 */
public class Attributes {

   public static <T> AttributeDefinitionBuilder build(String name, final Class<T> type) {
		return build(AttributeKey.create(name, type));
	}

	public static AttributeDefinitionBuilder build(AttributeKey<?> key) {
	   return new AttributeDefinitionBuilder(key);
	}

   public static <T> AttributeKey<T> createKey(String namespace, String name, Class<T> type) {
      Preconditions.checkNotNull(namespace, "namespace my not be null");
      return createKey(Utils.namespace(namespace, name), type);
   }

   /**
    * @deprecated see {@link AttributeKey#create(String, Class)}
    * @param name
    * @param type
    * @return
    */
	@Deprecated
   public static <T> AttributeKey<T> createKey(String name, Class<T> type) {
		return AttributeKey.create(name, type);
	}

	/**
	 * @deprecated see {@link AttributeMap#newMap()}
	 * @return
	 */
	@Deprecated
	public static AttributeMap createMap() {
		return AttributeMap.newMap();
	}

	/**
    * @deprecated see {@link AttributeMap#mapOf(AttributeValue...)}
	 * @param values
	 * @return
	 */
   @Deprecated
   public static AttributeMap mapOf(AttributeValue<?>... values) {
      return AttributeMap.mapOf(values);
   }

   /**
    * @deprecated see {@link AttributeMap#unmodifiableCopy(AttributeMap)}
    * @param map
    * @return
    */
   @Deprecated
   public static AttributeMap unmodifiableCopy(AttributeMap map) {
      return AttributeMap.unmodifiableCopy(map);
   }

   /**
    * @deprecated see {@link AttributeMap#emptyMap()}
    * @return
    */
   @Deprecated
   public static AttributeMap emptyMap() {
      return AttributeMap.emptyMap();
   }

	public static <T> FutureAttributeValue<T> futureValueOf(AttributeKey<T> key, ListenableFuture<T> value) {
		return new FutureAttributeValue<T>(key, value);
	}

	public static <T> FutureAttributeValue<T> immediateValueOf(AttributeKey<T> key, T value) {
		return new FutureAttributeValue<T>(key, Futures.immediateFuture(value));
	}

	public static <T> FutureAttributeValue<T> immediateValueOf(AttributeValue<T> value) {
	   return immediateValueOf(value.getKey(), value.getValue());
   }

	public static <T> FutureAttributeValue<T> immediateErrorOf(AttributeKey<T> key, Throwable cause) {
		return new FutureAttributeValue<T>(key, Futures.immediateFailedFuture(cause));
	}

	public static void validate(Iterable<AttributeDefinition> definition, AttributeMap attributes) throws ValidationException {
		Set<AttributeKey<?>> missing = new HashSet<>();
	   for(AttributeDefinition attribute: definition) {
	   	if(attribute.isOptional()) {
	   		continue;
	   	}
	   	if(attributes.containsKey(attribute.getKey())) {
	   		continue;
	   	}
	   	missing.add(attribute.getKey());
	   }
	   if(!missing.isEmpty()) {
	   	throw new ValidationException("Missing required attribute(s): " + missing);
	   }
   }

	public static AttributeMap transformToAttributeMap(Map<String,Object> attributes) {
	   return ServiceLocator.getInstance(AttributeMapTransformer.class).transformToAttributeMap(attributes);
	}

	public static Map<String,Object> transformFromAttributeMap(AttributeMap attributes) {
	   return ServiceLocator.getInstance(AttributeMapTransformer.class).transformFromAttributeMap(attributes);
	}
}

