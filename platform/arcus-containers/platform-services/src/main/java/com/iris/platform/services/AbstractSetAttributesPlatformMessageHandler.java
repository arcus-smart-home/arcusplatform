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
package com.iris.platform.services;

import java.util.Map;

import com.iris.Utils;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CapabilityDefinition;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.util.MapUtil;

public abstract class AbstractSetAttributesPlatformMessageHandler<B> implements ContextualRequestMessageHandler<B> {

   public static final String MESSAGE_TYPE = Capability.CMD_SET_ATTRIBUTES;

   public static final String ERR_UNDEFINED_CAPABILTY_CODE = "error.capability.undefined";
   public static final String ERR_UNDEFINED_CAPABILITY_MSG = "No capability is defined for: ";
   public static final String ERR_UNDEFINED_ATTRIBUTE_CODE = "error.attribute.undefined";
   public static final String ERR_UNDEFINED_ATTRIBUTE_MSG = "No attribute defined with name: ";
   public static final String ERR_ATTRIBUTE_NOTWRITABLE_CODE = "error.attribute.not_writable";
   public static final String ERR_ATTRIBUTE_NOTWRITABLE_MSG = "Attribute is not writable: ";

   private final CapabilityRegistry capabilityRegistry;
   private final BeanAttributesTransformer<B> beanTransformer;
   protected final PlatformMessageBus platformBus;
   protected final PlacePopulationCacheManager populationCacheMgr;

   protected AbstractSetAttributesPlatformMessageHandler(CapabilityRegistry capabilityRegistry, 
         BeanAttributesTransformer<B> beanTransformer,
         PlatformMessageBus platformBus, 
         PlacePopulationCacheManager populationCacheMgr) {
      this.capabilityRegistry = capabilityRegistry;
      this.beanTransformer = beanTransformer;
      this.platformBus = platformBus;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public String getMessageType() {
      return MESSAGE_TYPE;
   }

   protected void assertAccessible(B context, PlatformMessage msg) {
   	if(context == null) {
   		throw new ErrorEventException(Errors.notFound(msg.getDestination()));
   	}
   }

   @Override
   public MessageBody handleRequest(B context, PlatformMessage msg) {
      Utils.assertNotNull(context, "No context found, the object identified does not exist!");
      MessageBody request = msg.getValue();
      Map<String,Object> attributes = request.getAttributes();

      ErrorEvent error = validateSettableAttributes(attributes);
      if(error != null) {
         return error;
      }

      Map<String,Object> beanAttributes = beanTransformer.transform(context);
      Map<String,Object> changes = filterOnlyChanges(beanAttributes, attributes);

      beforeSave(context, changes);
      if(!changes.isEmpty()) {
         Map<String, Object> oldAttributes = beanTransformer.merge(context, changes);
         save(context);
         afterSave(context, oldAttributes);
         sendValueChangeEvent(context, msg, changes);
      }
      
      return MessageBody.emptyMessage();
   }
   
   // Note: The context object has not had changes merged at this point.
   protected void beforeSave(B context, Map<String, Object> changes) {
      // Do nothing by default.
   }

   protected abstract void save(B bean);
   
   // Note: The context object has had changes merged at this point.
   protected void afterSave(B context, Map<String, Object> oldAttributes) {
      // Do nothing by default.
   }

   private Map<String,Object> filterOnlyChanges(final Map<String,Object> beanAttributes, final Map<String,Object> incomingAttributes) {
      return MapUtil.filterChanges(beanAttributes, incomingAttributes);
   }

   protected void sendValueChangeEvent(B context, PlatformMessage request, Map<String, Object> changes) {   
	   sendValueChangeEventForPlace(request, changes, request.getPlaceId());
   }
   
   protected void sendValueChangeEventForPlace(PlatformMessage request, Map<String, Object> changes, String placeId) {
	   MessageBody body = MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, changes);
	   PlatformMessage msg = PlatformMessage.buildBroadcast(body, request.getDestination())
            .withPlaceId(placeId)
            .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
            .create();
	   platformBus.send(msg);
   }

   protected ErrorEvent validateSettableAttributes(Map<String,Object> attributes) {
      for(String key : attributes.keySet()) {
         if(!Utils.isNamespaced(key)) {
            continue;
         }
         CapabilityDefinition capDef = capabilityRegistry.getCapabilityDefinitionByNamespace(Utils.getNamespace(key));
         if(capDef == null) {
            return ErrorEvent.fromCode(ERR_UNDEFINED_CAPABILTY_CODE, ERR_UNDEFINED_CAPABILITY_MSG + Utils.getNamespace(key));
         }
         AttributeDefinition attrDef = capDef.getAttributes().get(key);
         if(attrDef == null) {
            return ErrorEvent.fromCode(ERR_UNDEFINED_ATTRIBUTE_CODE, ERR_UNDEFINED_ATTRIBUTE_MSG + key);
         }
         if(!attrDef.isWritable()) {
            return ErrorEvent.fromCode(ERR_ATTRIBUTE_NOTWRITABLE_CODE, ERR_ATTRIBUTE_NOTWRITABLE_MSG + key);
         }
      }

      return null;
   }
}

