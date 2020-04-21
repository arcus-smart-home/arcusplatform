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
package com.iris.model.predicate;

import java.util.List;
import java.util.regex.Pattern;

import com.google.common.base.Predicate;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.model.Model;
import com.iris.util.IrisAttributeLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class Predicates {
   private static final Logger logger = LoggerFactory.getLogger(Predicates.class);

   private static final Predicate<Model> isDevice = typeEquals(DeviceCapability.NAMESPACE);
   private static final Predicate<Model> isDeviceOffline = attributeEquals(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE);

   public static Predicate<Model> isDevice() {
      return isDevice;
   }

   public static Predicate<Model> isDeviceOffline() { return isDeviceOffline; }

   public static Predicate<Model> hasTag(String tag) {
      return attributeContains(Capability.ATTR_TAGS, tag);
   }
   
   public static Predicate<Model> typeEquals(String namespace) {
      return attributeEquals(Capability.ATTR_TYPE, namespace);
   }
   
   public static Predicate<Model> addressEquals(Address address) {
      return attributeEquals(Capability.ATTR_ADDRESS, address.getRepresentation());
   }
   
   public static Predicate<Model> addressEquals(String address) {
      return attributeEquals(Capability.ATTR_ADDRESS, address);
   }
   
   public static Predicate<Model>addressExistsIn(final List<String>addresses){
      return new Predicate<Model>() {
         @Override
         public boolean apply(Model model) {
            for(String address:addresses){
               if(addressEquals(address).apply(model)){
                  return true; 
               }
            }
            return false;
         }
      };
   } 
   
   /**
    * Gets a predicate that checks if a model implements the
    * given capability namespace.
    * @param namespace
    * @return
    */
   public static Predicate<Model> isA(String namespace) {
      return new AttributeContainsPredicate(Capability.ATTR_CAPS, namespace);
   }
   
   public static Predicate<Model> isNotA(String namespace) {
      return new AttributeDoesNotContainPredicate(Capability.ATTR_CAPS, namespace);
   }
   
   public static Predicate<Model> hasA(String namespace) {
   	return new AttributeValueContainsPredicate(Capability.ATTR_INSTANCES, namespace);
   }
   
   public static Predicate<Model> attributeEquals(String attributeName, Object attributeValue) {
      return new AttributeEqualsPredicate(attributeName, coerce(attributeName, attributeValue));
   }
   
   public static Predicate<Model> attributeNotEquals(String attributeName, Object attributeValue) {
      return new AttributeNotEqualsPredicate(attributeName, coerce(attributeName, attributeValue));
   }

   public static Predicate<Model> attributeContains(String attributeName, Object attributeValue) {
      return new AttributeContainsPredicate(attributeName, attributeValue);
   }

   public static Predicate<Model> attributeContainsKey(String attributeName, String attributeKey) {
      return new AttributeContainsKeyPredicate(attributeName, attributeKey);
   }

   public static Predicate<Model> attributeContainsValue(String attributeName, Object attributeValue) {
      return new AttributeContainsValuePredicate(attributeName, attributeValue);
   }

	public static Predicate<Model> attributeNotEmpty(String attributeName) {
		return new AttributeNotEmptyPredicate(attributeName);
	}
	
   public static Predicate<Model> attributeLike(String attributeName, String attributePattern) {
      return new AttributeLikePredicate(attributeName, attributePattern);
   }

   public static Predicate<Model> attributeLike(String attributeName, Pattern attributePattern) {
      return new AttributeLikePredicate(attributeName, attributePattern);
   }

   public static Predicate<Model> supportsAttribute(String attributeName) {
      return new AttributeSupportedPredicate(attributeName);
   }

   public static Predicate<Model> attributeGreaterThan(String attributeName, Object attributeValue) {
      return new AttributeGreaterThanPredicate(attributeName,attributeValue);
   }

   public static Predicate<Model> attributeGreaterThanEqualTo(String attributeName, Object attributeValue) {
      return new AttributeGreaterThanEqualToPredicate(attributeName,attributeValue);
   }

   public static Predicate<Model> attributeLessThan(String attributeName, Object attributeValue) {
      return new AttributeLessThanPredicate(attributeName,attributeValue);
   }

   public static Predicate<Model> attributeLessThanEqualTo(String attributeName, Object attributeValue) {
      return new AttributeLessThanEqualToPredicate(attributeName,attributeValue);
   }

   private static Object coerce(String name, Object value) {
      Object coerced = null;

      if (value == null) {
         return value;
      }

      try {
         coerced = IrisAttributeLookup.coerce(name, value);
      } catch (Exception e) {
         logger.trace("failed to coerce [attribute={}, oldType={} newType={}]: ",name, value.getClass(), value.getClass());
      }

      if (coerced == null) {
         return value; // Don't convert to null.
      }

      if (coerced.getClass().equals(value.getClass())) {
         return value;
      }

      logger.debug("coercing [{}] from [{}] to [{}]", name, value.getClass(), coerced.getClass());
      return coerced;
   }
}

