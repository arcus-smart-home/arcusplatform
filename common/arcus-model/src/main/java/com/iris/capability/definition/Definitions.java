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
package com.iris.capability.definition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.CharMatcher;
import com.iris.capability.definition.AttributeDefinition.AttributeOption;

/**
 * 
 */
public class Definitions {

   public static CapabilityDefinitionBuilder capabilityBuilder() {
      return new CapabilityDefinitionBuilder();
   }
   
   public static ProtocolDefinitionBuilder protocolBuilder() {
      return new ProtocolDefinitionBuilder();
   }
   
   public static ServiceDefinitionBuilder serviceBuilder() {
      return new ServiceDefinitionBuilder();
   }
   
   public static TypeDefinitionBuilder typeBuilder() {
      return new TypeDefinitionBuilder();
   }
   
   public static AttributeDefinitionBuilder attributeBuilder() {
      return new AttributeDefinitionBuilder();
   }
   
   public static MethodDefinitionBuilder methodBuilder() {
      return new MethodDefinitionBuilder();
   }
   
   public static EventDefinitionBuilder eventBuilder() {
      return new EventDefinitionBuilder();
   }
   
   public static ParameterDefinitionBuilder parameterBuilder() {
      return new ParameterDefinitionBuilder();
   }
   
   public static ErrorCodeDefinitionBuilder errorCodeBuilder() {
	   return new ErrorCodeDefinitionBuilder();
   }	   
   
   public static abstract class DefinitionBuilder<B extends DefinitionBuilder<?, T>, T> {
      protected String name;
      protected String description;
      
      public B withName(String name) {
         this.name = name;
         return (B) this;
      }
      
      public B withDescription(String description) {
         this.description = description;
         return (B) this;
      }
      
      public abstract T build();
      
   }

   public static abstract class ObjectDefinitionBuilder<B extends ObjectDefinitionBuilder<?, T>, T>
      extends DefinitionBuilder<B, T> 
   {
      protected String namespace;
      protected String version;
      protected final List<MethodDefinition> methods = new ArrayList<MethodDefinition>();
      protected final List<EventDefinition> events = new ArrayList<EventDefinition>();
      protected final List<AttributeDefinition> attributes = new ArrayList<AttributeDefinition>();
      protected final Set<ErrorCodeDefinition> errorEventExceptions = new HashSet<ErrorCodeDefinition>();
      
      public B withNamespace(String namespace) {
         this.namespace = namespace;
         return (B) this;
      }
      
      public B withVersion(String version) {
         this.version = version;
         return (B) this;
      }
      
      public B addMethod(MethodDefinition method) {
         this.methods.add(method);
         return (B) this;
      }
      
      public B withMethods(Collection<MethodDefinition> methods) {
         this.methods.clear();
         this.methods.addAll(methods);
         return (B) this;
      }

      public B addEvent(EventDefinition event) {
         this.events.add(event);
         return (B) this;
      }
      
      public B withEvents(Collection<EventDefinition> events) {
         this.events.clear();
         this.events.addAll(events);
         return (B) this;
      }
      
      public B addAttribute(AttributeDefinition attribute) {
         this.attributes.add(attribute);
         return (B) this;
      }
      
      public B withAttributes(Collection<AttributeDefinition> attributes) {
         this.attributes.clear();
         this.attributes.addAll(attributes);
         return (B) this;
      }

      public B withErrorEventExceptions(Collection<ErrorCodeDefinition> errorEventExceptions) {
          this.errorEventExceptions.clear();
          this.errorEventExceptions.addAll(errorEventExceptions);
          return (B) this;
       }      
   }
   
   public static abstract class ParameterizedDefinitionBuilder<B extends DefinitionBuilder<?, T>, T>
      extends DefinitionBuilder<B, T> 
   {
      protected final List<ParameterDefinition> parameters = new ArrayList<ParameterDefinition>();
      
      public B addParameter(ParameterDefinition parameter) {
         this.parameters.add(parameter);
         return (B) this;
      }
      
      public B withParameters(Collection<ParameterDefinition> parameters) {
         this.parameters.clear();
         this.parameters.addAll(parameters);
         return (B) this;
      }

   }
   
   public static class CapabilityDefinitionBuilder extends ObjectDefinitionBuilder<CapabilityDefinitionBuilder, CapabilityDefinition> {
      private String enhances;
      
      public CapabilityDefinitionBuilder enhances(String capabilityName) {
         this.enhances = capabilityName;
         return this;
      }
      
      public CapabilityDefinition build() {
         return new CapabilityDefinition(name, description, namespace, version, methods, events, enhances, attributes, errorEventExceptions);
      }
   }
   
   public static class ProtocolDefinitionBuilder extends ObjectDefinitionBuilder<ProtocolDefinitionBuilder, ProtocolDefinition> {

      @Override
      public ProtocolDefinition build() {
         return new ProtocolDefinition(name, description, namespace, version, attributes);
      }
   }

   public static class TypeDefinitionBuilder extends ObjectDefinitionBuilder<TypeDefinitionBuilder, TypeDefinition> {

      @Override
      public TypeDefinition build() {
         return new TypeDefinition(name, description, version, attributes);
      }
   }

   public static class ServiceDefinitionBuilder extends ObjectDefinitionBuilder<ServiceDefinitionBuilder, ServiceDefinition> {
      
      public ServiceDefinition build() {
         return new ServiceDefinition(name, description, namespace, version, methods, events);
      }
   }
   
   public static class AttributeDefinitionBuilder extends DefinitionBuilder<AttributeDefinitionBuilder, AttributeDefinition> {
      private AttributeType type;
      private final Set<AttributeOption> flags = EnumSet.of(AttributeOption.READABLE);
      private final List<String> enumValues = new ArrayList<String>();
      private String min;
      private String max;
      private String unit;
      
      public AttributeDefinitionBuilder withType(String type) {
         this.type = AttributeTypes.parse(type);
         return this;
      }
      
      public AttributeDefinitionBuilder withType(AttributeType type) {
         this.type = type;
         return this;
      }
      
      public AttributeDefinitionBuilder readable() {
         flags.add(AttributeOption.READABLE);
         return this;
      }
      
      public AttributeDefinitionBuilder writable() {
         flags.add(AttributeOption.WRITABLE);
         return this;
      }

      public AttributeDefinitionBuilder optional() {
         flags.add(AttributeOption.OPTIONAL);
         return this;
      }
      
      public AttributeDefinitionBuilder withEnumValues(Collection<String> enumValues) {
         this.enumValues.clear();
         this.enumValues.addAll(enumValues);
         return this;
      }
      
      public AttributeDefinitionBuilder addEnumValue(String enumValue) {
         this.enumValues.add(enumValue);
         return this;
      }
      
      public AttributeDefinitionBuilder withMin(String min) {
         this.min = min;
         return this;
      }

      public AttributeDefinitionBuilder withMax(String max) {
         this.max = max;
         return this;
      }

      public AttributeDefinitionBuilder withUnit(String unit) {
         this.unit = unit;
         return this;
      }

      public AttributeDefinition build() {
         this.type = wrapEnum(this.type);
         // TODO handle object types as well
         return new AttributeDefinition(name, description, type, flags, enumValues, min, max, unit);
      }

      private AttributeType wrapEnum(AttributeType at) {
         switch(at.getRawType()) {
         case ENUM:
            return AttributeTypes.enumOf(enumValues);
         case SET:
            return AttributeTypes.setOf(wrapEnum(at.asCollection().getContainedType()));
         case LIST: 
            return AttributeTypes.listOf(wrapEnum(at.asCollection().getContainedType()));
         case MAP: 
            return AttributeTypes.mapOf(wrapEnum(at.asCollection().getContainedType()));
         default:
            return at;
         }
      }
   }

   public static class ParameterDefinitionBuilder extends DefinitionBuilder<ParameterDefinitionBuilder, ParameterDefinition> {
      private AttributeType type;
      private boolean optional;
      private List<String> enumValues;

      public ParameterDefinitionBuilder isOptional(boolean optional) {
         this.optional = optional;
         return this;
      }
      
      public ParameterDefinitionBuilder withType(String type) {
         this.type = AttributeTypes.parse(type);
         return this;
      }
      
      public ParameterDefinitionBuilder withType(AttributeType type) {
         this.type = type;
         return this;
      }
      
      public ParameterDefinitionBuilder withEnumValues(String enumValues) {
         this.enumValues = Arrays.asList(enumValues.split(","));
         return this;
      }
      
      public ParameterDefinitionBuilder addEnumValue(String enumValue) {
         if(this.enumValues == null) {
            this.enumValues = new ArrayList<String>();
         }
         this.enumValues.add(enumValue);
         return this;
      }

      public ParameterDefinition build() {
         if(type != null && type.isEnum()) {
            if(enumValues == null) {
               throw new IllegalStateException("Parameter " + name + " is defined as an enum but no values were provided");
            }
            type = AttributeTypes.enumOf(enumValues);
         }
         return new ParameterDefinition(this.name, this.description, this.type, this.optional);
      }

   }
   
   public static class EventDefinitionBuilder extends ParameterizedDefinitionBuilder<EventDefinitionBuilder, EventDefinition> {
      
      public EventDefinition build() {
         return new EventDefinition(name, description, parameters);
      }
   }
	
	public static class MethodDefinitionBuilder extends ParameterizedDefinitionBuilder<MethodDefinitionBuilder, MethodDefinition> {
		protected boolean isRESTful = false;
		protected final List<ParameterDefinition> returnValues = new ArrayList<ParameterDefinition>();
		protected final List<ErrorCodeDefinition> errorCodes = new ArrayList<ErrorCodeDefinition>();

		public MethodDefinitionBuilder isRestful(boolean isRestful) {
			this.isRESTful = isRestful;
			return this;
		}

		public MethodDefinitionBuilder addReturnValue(ParameterDefinition parameter) {
			this.returnValues.add(parameter);
			return this;
		}

		public MethodDefinitionBuilder addErrorCode(ErrorCodeDefinition errorCode) {
			this.errorCodes.add(errorCode);
			return this;
		}

		public MethodDefinitionBuilder withErrorCodes(Collection<ErrorCodeDefinition> errorCodes) {
			this.errorCodes.clear();
			this.errorCodes.addAll(errorCodes);
			return this;
		}
		
		public MethodDefinitionBuilder withReturnValues(Collection<ParameterDefinition> returnValues) {
			this.returnValues.clear();
			this.returnValues.addAll(returnValues);
			return this;
		}

		public MethodDefinition build() {
			return new MethodDefinition(name, description, isRESTful, parameters, returnValues, errorCodes);
		}
	}
	
	public static class ErrorCodeDefinitionBuilder extends DefinitionBuilder<ErrorCodeDefinitionBuilder, ErrorCodeDefinition> {

		private String code;
		
		public ErrorCodeDefinitionBuilder withCode(String code) {
			this.code = code;
			return this;
		}
		
		@Override
		public ErrorCodeDefinitionBuilder withName(String name) {
			// no-op
			return this;
		}

		/**
		 * The errorType definition in Capability.xsd does not include a name. However we still
		 * want to handle this as any other Definition type internally so we will derive a name 
		 * from the "code".
		 * 
		 * The derivation of name replaces all periods\dots from the code input string and returns
		 * the remaining String in upper camel case.
		 * 
		 * e.g. "invalid.pin" will result in a return value of "InvalidPin"
		 * 
		 * For generation of Static Strings representing the code we will convert to uppercase and
		 * replace the "." chars with "_"
		 * 
		 * e.g. "invalid.pin" will result in INVALID_PIN, and be used to generate a Static Final String
		 * with the name CODE_INVALID_PIN.
		 */
		@Override
		public ErrorCodeDefinition build() {
			String codeId = (code.contains(".")) ? CharMatcher.is('.').replaceFrom(code, "_") : code;
			return new ErrorCodeDefinition(upperCamel(code), description, code, codeId.toUpperCase());
		}

		private String upperCamel(String codeIn) {
			StringBuilder b = new StringBuilder();
			
			String[] strs = StringUtils.split(codeIn, ".");
			
			for (String toConver : strs) {
				b.append(StringUtils.capitalize(toConver));
			}
			
			return b.toString();
		}
	}		

}

