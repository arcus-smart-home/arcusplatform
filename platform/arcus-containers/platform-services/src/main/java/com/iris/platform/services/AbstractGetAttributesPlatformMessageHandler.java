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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import com.iris.Utils;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.services.PlatformConstants;
import com.iris.util.Results;

public abstract class AbstractGetAttributesPlatformMessageHandler<B> implements ContextualRequestMessageHandler<B> {

   public static final String MESSAGE_TYPE = Capability.CMD_GET_ATTRIBUTES;

   private final BeanAttributesTransformer<B> beanTransformer;

   protected AbstractGetAttributesPlatformMessageHandler(BeanAttributesTransformer<B> beanTransformer) {
      this.beanTransformer = beanTransformer;
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
      assertAccessible(context, msg);
      
      MessageBody request = msg.getValue();
      Map<String,Object> attributes = request.getAttributes();
      Collection<String> names = (Collection<String>) attributes.get("names");

      Map<String,Object> beanAttributes = beanTransformer.transform(context);
      Map<String,Object> response = filter(beanAttributes, names);

      return MessageBody.buildResponse(request, response);
   }

   private Map<String,Object> filter(Map<String,Object> attributes, Collection<String> requestedNames) {
      Predicate<String> keys = null;
      if(requestedNames == null || requestedNames.isEmpty()) {
         keys = (key) -> true;
      } else {
         for(String attributeName: requestedNames) {
            Predicate<String> p = toPredicate(attributeName);
            keys = keys != null ? keys.or(p) : toPredicate(attributeName);
         }
      }

      Map<String, Object> results = new HashMap<>();
      for(Map.Entry<String,Object> entry : attributes.entrySet()) {
         if(!keys.test(entry.getKey())) {
            continue;
         }
         results.put(entry.getKey(), entry.getValue());
      }

      // add errors for unsupported parameters that were explicitly requested
      if(requestedNames != null && !requestedNames.isEmpty()) {
         for(String attributeName : requestedNames) {
            if(!Utils.isNamespaced(attributeName)) {
               continue;
            }
            if(!results.containsKey(attributeName)) {
               results.put(attributeName, Results.fromError(new Exception("Device does not support attribute [" + attributeName + "]")));
            }
         }
      }

      return results;
   }

   private Predicate<String> toPredicate(String attributeName) {
      if(Utils.isNamespaced(attributeName)) {
         return (key) -> key.equals(attributeName);
      }
      String prefix = attributeName + ":";
      return (key) -> key.startsWith(prefix);
   }
}

