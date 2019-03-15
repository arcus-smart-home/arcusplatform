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
package com.iris.messages.capability;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.Utils;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.definition.MethodDefinition;
import com.iris.capability.definition.ParameterDefinition;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.model.*;
import com.iris.model.type.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import java.util.*;

@Singleton
public class DefinitionTransformCapabilityRegistry implements CapabilityRegistry {
   private final List<CapabilityDefinition> capabilities;
	private final Map<String, CapabilityDefinition> capabilitiesByName;
	private final Map<String, CapabilityDefinition> capabilitiesByNamespace;

	@Inject
	public DefinitionTransformCapabilityRegistry(DefinitionRegistry registry) {
		capabilitiesByName = new HashMap<String, CapabilityDefinition>();
		capabilitiesByNamespace = new HashMap<String, CapabilityDefinition>();
		capabilities = new ArrayList<>();
		ingest(registry.getCapabilities());
	}

   @Override
   @Nullable
   public AttributeDefinition getAttributeDefinition(String name) {
      if(StringUtils.isEmpty(name)) {
         return null;
      }
      String namespace = Utils.getNamespace(name);
      CapabilityDefinition definition = getCapabilityDefinitionByNamespace(namespace);
      if(definition == null) {
         return null;
      }
      return definition.getAttributes().get(name);
   }

	@Override
	public CapabilityDefinition getCapabilityDefinitionByName(String name) {
		return capabilitiesByName.get(name);
	}

	@Override
	public CapabilityDefinition getCapabilityDefinitionByNamespace(String namespace) {
		return capabilitiesByNamespace.get(namespace);
	}

	@Override
	public List<CapabilityDefinition> listCapabilityDefinitions() {
		return capabilities;
	}

	private void ingest(Collection<com.iris.capability.definition.CapabilityDefinition> definitions) {
      for(com.iris.capability.definition.CapabilityDefinition def: definitions) {
	         CapabilityDefinition value = transform(def);
	         capabilities.add(value);
	         capabilitiesByName.put(value.getCapabilityName(), value);
	         capabilitiesByNamespace.put(value.getNamespace(), value);
      }
	}

   private CapabilityDefinition transform(com.iris.capability.definition.CapabilityDefinition def) {
      return new CapabilityDefinition(
            def.getName(),
            def.getNamespace(),
            def.getEnhances(),
            def.getDescription(),
            transformAttributes(def.getNamespace(), def.getAttributes()),
            transformCommands(def.getNamespace(), def.getMethods()),
            transformEvents(def.getNamespace(), def.getEvents())
      );
   }

   private Map<String, AttributeDefinition> transformAttributes(String namespace, List<com.iris.capability.definition.AttributeDefinition> attributes) {
      Map<String, AttributeDefinition> transformed = new HashMap<String, AttributeDefinition>(2 * Math.max(1, attributes.size()));
      for(com.iris.capability.definition.AttributeDefinition attribute: attributes) {
         AttributeDefinition a = transform(namespace, attribute);
         transformed.put(a.getName(), a);
      }
      return transformed;
   }

   private Map<String, CommandDefinition> transformCommands(String namespace, List<MethodDefinition> definitions) {
      Map<String, CommandDefinition> transformed = new HashMap<String, CommandDefinition>(2 * Math.max(1, definitions.size()));
      for(MethodDefinition definition: definitions) {
         CommandDefinition command = transform(namespace, definition);
         transformed.put(command.getName(), command);
      }
      return transformed;
   }

   private Map<String, EventDefinition> transformEvents(String namespace, List<com.iris.capability.definition.EventDefinition> events) {
      Map<String, EventDefinition> transformed = new HashMap<String, EventDefinition>(2 * Math.max(1, events.size()));
      for(com.iris.capability.definition.EventDefinition event: events) {
         EventDefinition e = transform(namespace, event);
         transformed.put(e.getName(), e);
      }
      return transformed;
   }

   private AttributeDefinition transform(String namespace, com.iris.capability.definition.AttributeDefinition attribute) {
      Set<AttributeFlag> flags = EnumSet.of(AttributeFlag.READABLE);
      if(attribute.isOptional()) {
         flags.add(AttributeFlag.OPTIONAL);
      }
      if(attribute.isWritable()) {
         flags.add(AttributeFlag.WRITABLE);
      }
      return new AttributeDefinition(
            AttributeKey
                  .createType(namespace + ":" + attribute.getName(), attribute.getType().getJavaType()),
            flags,
            attribute.getDescription(),
            attribute.getUnit(),
            transform(attribute.getType())
      );
   }

   private CommandDefinition transform(String namespace, MethodDefinition method) {
      Map<String, AttributeDefinition> inputAttributes = new HashMap<>();
      for(ParameterDefinition parameter: method.getParameters()) {
         AttributeDefinition ad = transform(parameter);
         inputAttributes.put(parameter.getName(), ad);
      }
      Map<String, AttributeDefinition> returnAttributes = new HashMap<>();
      for(ParameterDefinition parameter: method.getReturnValues()) {
         AttributeDefinition ad = transform(parameter);
         returnAttributes.put(parameter.getName(), ad);
      }
      return new CommandDefinition(
         namespace,
         method.getName(),
         method.getDescription(),
         returnAttributes,
         inputAttributes
      );
   }

   private EventDefinition transform(String namespace, com.iris.capability.definition.EventDefinition event) {
      Map<String, AttributeDefinition> attributes = new HashMap<>();
      for(ParameterDefinition parameter: event.getParameters()) {
         AttributeDefinition ad = transform(parameter);
         attributes.put(parameter.getName(), ad);
      }


      return new EventDefinition(
         namespace,
         event.getName(),
         attributes
      );
   }

   private AttributeDefinition transform(ParameterDefinition parameter) {
      Set<AttributeFlag> flags = EnumSet.of(AttributeFlag.READABLE, AttributeFlag.WRITABLE);
      return new AttributeDefinition(
            AttributeKey
                  .createType(parameter.getName(), parameter.getType().getJavaType()),
            flags,
            parameter.getDescription(),
            "",
            transform(parameter.getType())
      );
   }

   private AttributeType transform(com.iris.capability.definition.AttributeType type) {
      switch(type.getRawType()) {
      case BOOLEAN:
         return BooleanType.INSTANCE;
      case BYTE:
         return ByteType.INSTANCE;
      case INT:
         return IntType.INSTANCE;
      case LONG:
         return LongType.INSTANCE;
      case DOUBLE:
         return DoubleType.INSTANCE;
      case STRING:
         return StringType.INSTANCE;
      case TIMESTAMP:
         return TimestampType.INSTANCE;
      case VOID:
         return VoidType.INSTANCE;
      case ANY:
         return AnyType.INSTANCE;

      case ENUM:
         return new EnumType(type.asEnum().getValues());

      case MAP:
         return new MapType(transform(type.asCollection().getContainedType()));
      case LIST:
         return new ListType(transform(type.asCollection().getContainedType()));
      case SET:
         return new SetType(transform(type.asCollection().getContainedType()));

      // TODO better support here
      case OBJECT:
      case ATTRIBUTES:
         return new MapType(AnyType.INSTANCE);

      default:
         throw new IllegalArgumentException("Unrecognized type " + type);
      }
   }

}

