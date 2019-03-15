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
package com.iris.capability.attribute.transform;

import java.util.Date;

import com.google.common.collect.ImmutableSet;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.device.model.AttributeDefinition;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.model.Person;

public class PersonAttributesTransformer extends ReflectiveBeanAttributesTransformer<Person> {

	public PersonAttributesTransformer(CapabilityRegistry registry) {
      super(
            registry,
            ImmutableSet.of(PersonCapability.NAMESPACE, Capability.NAMESPACE),
            Person.class);
   }
	
	@Override
   protected Object getValue(Person bean, AttributeDefinition definition) throws Exception {
      if (PersonCapability.ATTR_EMAILVERIFIED.equals(definition.getName())){
      	if(bean.getEmailVerified() != null && bean.getEmailVerified().getTime() > 0) {
      		return Boolean.TRUE;
      	}else{
      		return Boolean.FALSE;
      	}
      }else{
         return super.getValue(bean, definition);
      }
   }

	@Override
	protected void setValue(Person bean, Object value, AttributeDefinition definition) throws Exception{
		if (PersonCapability.ATTR_EMAILVERIFIED.equals(definition.getName())){
			/*boolean verified = false;
      	if(value != null) {
      		verified = (boolean)value;      		
      	}
      	if(verified) {
      		if(bean.getEmailVerified() == null) {
      			bean.setEmailVerified(new Date());  //TODO - only overwrite the date if it is not already set.  Is this correct?
      		}
   		}else {
   			bean.setEmailVerified(null); //set it to null if not verified
   		}*/
			//readonly, so ignore
      	
      }else{
      	super.setValue(bean, value, definition);
      }
		
	}
	
	

}

