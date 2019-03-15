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
package com.iris.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.iris.capability.definition.AttributeDefinition;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.capability.definition.EventDefinition;
import com.iris.capability.definition.MethodDefinition;
import com.iris.capability.definition.ObjectDefinition;
import com.iris.capability.definition.ServiceDefinition;

public class DefinitionVerificationUtil {
	private static DefinitionVerificationUtil theInstance = new DefinitionVerificationUtil();
	
	private DefinitionVerificationUtil() {};
	
	public static DefinitionVerificationUtil instance() {
		return theInstance;
	}

	/**
	 * Verify that the given CapabilityDefinition matches with the corresponding generated class.  
	 * Note, this only verifies everything definition in the given CapabilityDefinition is supported 
	 * by the generated class.  Not they are the same.  It is possible CapabilityDefinition is only a 
	 * subset. 
	 * @param definition
	 * @throws Exception 
	 */
	public void assertCapabilityDefinitionMatches(CapabilityDefinition definition) throws Exception {
      
      Class<?> cls = Class.forName("com.iris.messages.capability." + definition.getName() + "Capability");
      assertEquals(definition.getName(), cls.getField("NAME").get(null));
      assertEquals(definition.getNamespace(), cls.getField("NAMESPACE").get(null));
      for(AttributeDefinition attribute: definition.getAttributes()) {
      	assertAttributeMatches(definition, attribute, cls);
      }
      for(MethodDefinition method: definition.getMethods()) {
      	assertMethodMatches(definition, method, cls);
      }
      for(EventDefinition method: definition.getEvents()) {
      	assertEventMatches(definition, method, cls);
      }
	}
	
	public void assertServiceDefinitionMatches(ServiceDefinition definition) throws Exception {
		Class<?> cls = Class.forName("com.iris.messages.service." + definition.getName());
      assertEquals(definition.getName(), cls.getField("NAME").get(null));
      assertEquals(definition.getNamespace(), cls.getField("NAMESPACE").get(null));
      for(MethodDefinition method: definition.getMethods()) {
      	assertMethodMatches(definition, method, cls);
      }
      for(EventDefinition method: definition.getEvents()) {
      	assertEventMatches(definition, method, cls);
      }
	}
	
	private void assertAttributeMatches(ObjectDefinition parentDefinition, AttributeDefinition attribute, Class<?> cls) throws Exception {
		assertEquals(
				parentDefinition.getNamespace() + ":" + attribute.getName(), 
            cls.getField("ATTR_" + attribute.getName().toUpperCase()).get(null)
      );
      assertEquals(
            "Type doesn't match definition for " + parentDefinition.getName() + " attribute " + attribute.getName(),
            attribute.getType(), 
            cls.getField("TYPE_" + attribute.getName().toUpperCase()).get(null)
      );
      if(attribute.getType().isEnum()) {
         for(String enumValue: attribute.getType().asEnum().getValues()) {
            assertEquals(
                  enumValue,
                  cls.getField(attribute.getName().toUpperCase() + "_" + enumValue.toUpperCase()).get(null)
            );
         }
      }
	}
	
	private void assertMethodMatches(ObjectDefinition parentDefinition, MethodDefinition method, Class<?> cls) throws Exception {
		Class<?> requestType = Class.forName(cls.getName() + "$" + method.getName() + "Request");
      assertNotNull(requestType);
      // TODO verify input parameters
      
      Class<?> responseType = Class.forName(cls.getName() + "$" + method.getName() + "Response");
      assertNotNull(responseType);
      // TODO verify return attributes
	}
	
	private void assertEventMatches(ObjectDefinition parentDefinition, EventDefinition event, Class<?> cls) throws Exception {
		Class<?> requestType = Class.forName(cls.getName() + "$" + event.getName() + "Event");
      assertNotNull(requestType);
      // TODO verify input parameters
	}
}

