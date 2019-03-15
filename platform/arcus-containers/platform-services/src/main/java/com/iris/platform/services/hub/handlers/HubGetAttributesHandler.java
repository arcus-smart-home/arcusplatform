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
package com.iris.platform.services.hub.handlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Hub;
import com.iris.platform.services.AbstractGetAttributesPlatformMessageHandler;

import java.util.Objects;

// TODO:  jettison when the hub can handle get attributes on its own
@Singleton
public class HubGetAttributesHandler extends AbstractGetAttributesPlatformMessageHandler<Hub> {

   @Inject
   public HubGetAttributesHandler(BeanAttributesTransformer<Hub> hubTransformer) {
      super(hubTransformer);
   }

	@Override
	protected void assertAccessible(Hub context, PlatformMessage msg) {
		// TODO Auto-generated method stub
		super.assertAccessible(context, msg);
		
      Address actor = msg.getActor();

      // allow services
      if(!MessageConstants.CLIENT.equals(msg.getSource().getNamespace())) {
      	return;
      }
      
      if(context.getPlace() != null && Objects.equals(context.getPlace().toString(), msg.getPlaceId())) {
      	return;
      }
      
  		throw new UnauthorizedRequestException(msg.getDestination(), "Actor " + msg.getActor() + " not authorized to access hub " + msg.getDestination());
	}

}

