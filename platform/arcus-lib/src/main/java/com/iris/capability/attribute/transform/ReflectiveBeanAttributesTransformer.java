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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.TypeLiteral;
import com.iris.Utils;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CapabilityDefinition;
import com.iris.messages.model.ChildId;
import com.iris.messages.model.CompositeId;
import com.iris.model.Version;

public class ReflectiveBeanAttributesTransformer<B> implements BeanAttributesTransformer<B> {

   private static final Logger log = LoggerFactory.getLogger(ReflectiveBeanAttributesTransformer.class);

   private static final TypeLiteral<List<UUID>> listOfUUIDS = new TypeLiteral<List<UUID>>() {};
   private static final TypeLiteral<Map<String,UUID>> mapOfUUIDs = new TypeLiteral<Map<String,UUID>>() {};

   private final CapabilityRegistry capabilityRegistry;
   private final Set<String> capabilitiesToTransform;
   private final Class<B> clazz;
   private final Map<String,PropertyDescriptor> properties;
   private final Class idClass;

   // TODO consider changing this to a builder pattern
   public ReflectiveBeanAttributesTransformer(
         CapabilityRegistry capabilityRegistry,
         Set<String> capabilitiesToTransform,
         Class<B> clazz) {

      this.capabilityRegistry = capabilityRegistry;
      this.capabilitiesToTransform = Collections.unmodifiableSet(capabilitiesToTransform);
      this.clazz = clazz;

      try {
         Class<?> idClass = UUID.class;
         Map<String,PropertyDescriptor> properties = new HashMap<>();
         BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
         PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
         for(PropertyDescriptor property : propertyDescriptors) {
            if(property.getName().equals("id")) {
               idClass = property.getPropertyType();
            }
            properties.put(property.getName(), property);
         }
         this.properties = Collections.unmodifiableMap(properties);
         this.idClass = idClass;
      } catch(IntrospectionException ie) {
         throw new RuntimeException("Failure to instantiate ReflectiveBeanAttributeTransformer for " + clazz, ie);
      }
   }

   @Override
   public Map<String, Object> transform(B bean) {
      try {
         Map<String,Object> attributes = new HashMap<>();
         for(String cap : capabilitiesToTransform) {
            CapabilityDefinition capDef = capabilityRegistry.getCapabilityDefinitionByNamespace(cap);
            if(capDef == null) {
               log.warn("Unable to find capability definition for {}, all properties for it will be skipped", cap);
               continue;
            }

            Map<String, AttributeDefinition> attrDefinitions = capDef.getAttributes();
            for(AttributeDefinition attrDef : attrDefinitions.values()) {
               Object value = getValue(bean, attrDef);
               if(value != null) {
                  attributes.put(attrDef.getName(), attrDef.getAttributeType().coerce(value));
               }
            }
         }
         return attributes;
      } catch(Exception e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public B transform(Map<String, Object> attributes) {
      try {
         B bean = clazz.newInstance();
         merge(bean, attributes);
         return bean;
      } catch(Exception e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public Map<String, Object> merge(B bean, Map<String, Object> newAttributes) {
      try {
         if(newAttributes == null || newAttributes.isEmpty()) {
            return Collections.emptyMap();
         }

         Map<String, Object> oldAttributes = new HashMap<>();
         for(Map.Entry<String, Object> entry : newAttributes.entrySet()) {
            if(!Utils.isNamespaced(entry.getKey())) {
               log.warn("Ignoring unnamespaces attribute {}", entry.getKey());
               continue;
            }
            String capName = Utils.getNamespace(entry.getKey());
            if(!capabilitiesToTransform.contains(capName)) {
               continue;
            }
            CapabilityDefinition capDef = capabilityRegistry.getCapabilityDefinitionByNamespace(capName);
            if(capDef == null) {
               log.warn("Unable to find capability definition for {}, attribute {} will be ignored", capName, entry.getKey());
               continue;
            }
            AttributeDefinition attrDef = capDef.getAttributes().get(entry.getKey());
            if(attrDef == null) {
               log.warn("No attribute definition found for {}, within capability definition {}", entry.getKey(), capName);
               continue;
            }
            oldAttributes.put(entry.getKey(), getValue(bean, attrDef));
            setValue(bean, entry.getValue(), attrDef);
         }
         return oldAttributes;
      } catch(Exception e) {
         throw new RuntimeException(e);
      }
   }

   protected PropertyDescriptor getPropertyDescriptor(String name) {
      return properties.get(deNamespace(name));
   }

   protected Object getValue(B bean, AttributeDefinition definition) throws Exception {
      PropertyDescriptor property = getPropertyDescriptor(definition.getName());
      if(property == null) {
         log.trace("Unable to find bean property for attribute definition {}", definition.getName());
         return null;
      } else {
         Object value = property.getReadMethod().invoke(bean);
         if(value != null) {
            if(value instanceof Version) {
               value = ((Version) value).getRepresentation();
            }
            else if(value instanceof CompositeId) {
               value = ((CompositeId<?, ?>) value).getRepresentation();
            }
         }
         return value;
      }
   }

   protected void setValue(B bean, Object value, AttributeDefinition definition) throws Exception {
      PropertyDescriptor property = getPropertyDescriptor(definition.getName());
      if(property == null) {
         log.trace("Unable to find bean property for attribute {}", definition.getKey());
      } else {
         Method writer = property.getWriteMethod();
         if(writer != null) {
            writer.invoke(bean, coerceSetterValue(writer, value, definition));
         }
      }
   }

   protected Object coerceSetterValue(Method m, Object value, AttributeDefinition attrDef) throws Exception {
      Object retValue = value;
      if(value == null) {
         return value;
      }
      if(m.getName().equalsIgnoreCase("setid")) {
         if(idClass.isAssignableFrom(UUID.class)) { retValue = UUID.fromString((String) value); }
         else if(idClass.isAssignableFrom(CompositeId.class)) { retValue = ChildId.fromString((String) value); }
         return retValue;
      }

      if(m.getParameterTypes()[0].isEnum()) {
         Class enumType = m.getParameterTypes()[0];
         return Enum.valueOf(enumType, (String) value);
      }

      if(setterExpectsMapOfUUIDs(m)) {
         retValue = coerceToMapOfUUIDs((Map<String,String>) value);
      } else if(setterExpectsListOfUUIDs(m)) {
         retValue = coerceToListOfUUIDS((Collection<String>) value);
      } else if(setterExpectsUUID(m)) {
         retValue = UUID.fromString((String) value);
      } else if(setterExpectsURL(m)) {
         retValue = new URL((String) value);
      } else if(setterExpectsVersion(m)) {
         retValue = Version.fromRepresentation((String) value);
      } else {
         retValue = attrDef.getAttributeType().coerce(value);
      }
      return retValue;
   }

   private boolean setterExpectsMapOfUUIDs(Method m) {
      Type[] genericParameters = m.getGenericParameterTypes();
      return mapOfUUIDs.toString().equals(genericParameters[0].toString());
   }

   private boolean setterExpectsListOfUUIDs(Method m) {
      Type[] genericParameters = m.getGenericParameterTypes();
      return listOfUUIDS.toString().equals(genericParameters[0].toString());
   }

   private boolean setterExpectsUUID(Method m) {
      Class<?>[] parameters = m.getParameterTypes();
      return parameters[0].isAssignableFrom(UUID.class);
   }

   private boolean setterExpectsURL(Method m) {
      Class<?>[] parameters = m.getParameterTypes();
      return parameters[0].isAssignableFrom(URL.class);
   }

   private boolean setterExpectsVersion(Method m) {
      Class<?>[] parameters = m.getParameterTypes();
      return parameters[0].isAssignableFrom(Version.class);
   }

   private List<UUID> coerceToListOfUUIDS(Collection<String> uuidStrs) {
      return uuidStrs.stream().map((s) -> { return UUID.fromString(s); }).collect(Collectors.toList());
   }

   private Map<String,UUID> coerceToMapOfUUIDs(Map<String,String> uuidStrs) {
      Map<String,UUID> asUUIDs = new HashMap<>();
      for(Map.Entry<String, String> entry : uuidStrs.entrySet()) {
         asUUIDs.put(entry.getKey(), UUID.fromString(entry.getValue()));
      }
      return asUUIDs;
   }

   private String deNamespace(String attrName) {
      return attrName.split(":")[1];
   }
}

