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
package com.iris.protocol.ipcd.definition.reader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.iris.protocol.ipcd.xml.model.CommandType;
import com.iris.protocol.ipcd.xml.model.CommandsType;
import com.iris.protocol.ipcd.xml.model.Definition;
import com.iris.protocol.ipcd.xml.model.EnumType;
import com.iris.protocol.ipcd.xml.model.EnumsType;
import com.iris.protocol.ipcd.xml.model.MessageType;
import com.iris.protocol.ipcd.xml.model.MessagesType;
import com.iris.protocol.ipcd.xml.model.OptionType;
import com.iris.protocol.ipcd.xml.model.PropertyType;
import com.iris.protocol.ipcd.xml.model.ResponseType;
import com.iris.protocol.ipcd.xml.model.TypeType;
import com.iris.protocol.ipcd.xml.model.TypesType;
import com.iris.protocol.ipcd.definition.context.DefinitionContext;
import com.iris.protocol.ipcd.definition.context.IpcdEnum;
import com.iris.protocol.ipcd.definition.context.IpcdEnumValue;
import com.iris.protocol.ipcd.definition.context.IpcdObject;
import com.iris.protocol.ipcd.definition.context.Property;

public class DefinitionContextBuilder {
   
   public DefinitionContext build(Definition definition) {
      DefinitionContext context = new DefinitionContext();
      context.setMessages(buildMessages(definition.getMessages()));
      context.setCommands(buildCommands(context.getCommandDefinition(), definition.getCommands()));
      context.setResponses(buildResponses(context.getResponseDefinition(), definition.getCommands()));
      context.setEnums(buildEnums(definition.getEnums()));
      context.setTypes(buildTypes(definition.getTypes()));
      applyCommands(context.getMessages(), context.getCommands());
      return context;
   }
   
   private void applyCommands(Collection<IpcdObject> messages, List<IpcdObject> commands) {
      for (IpcdObject message : messages) {
         if (message.isHasCommands()) {
            message.setCommands(commands);
         }
      }
   }
   
   private List<IpcdObject> buildMessages(MessagesType messagesType) {
      List<IpcdObject> messages = new ArrayList<>();
      for (MessageType messageType : messagesType.getMessage()) {
         messages.add(buildMessage(messageType));
      }
      return messages;
   }
   
   private List<IpcdObject> buildCommands(IpcdObject commandDefinition, CommandsType commandsType) {
      List<IpcdObject> commands = new ArrayList<>();
      for (CommandType command : commandsType.getCommand()) {
         commands.add(buildCommand(commandDefinition, command));
      }
      return commands;
   }
   
   private List<IpcdObject> buildResponses(IpcdObject responseDefinition, CommandsType commandsType) {
      List<IpcdObject> responses = new ArrayList<>();
      for (CommandType command : commandsType.getCommand()) {
         responses.add(buildResponse(responseDefinition, command));
      }
      return responses;
   }
   
   private List<IpcdEnum> buildEnums(EnumsType enumsType) {
      List<IpcdEnum> enums = new ArrayList<>();
      if (enumsType != null && enumsType.getEnum() != null) {
         for (EnumType enumType : enumsType.getEnum()) {
            IpcdEnum ipcdEnum = new IpcdEnum();
            ipcdEnum.setName(enumType.getName());
            ipcdEnum.setDescription(StringUtils.isEmpty(enumType.getDescription()) ? enumType.getName() : enumType.getDescription());
            List<IpcdEnumValue> values = new ArrayList<>();
            if (enumType.getOption() != null) {
               for (OptionType optionType : enumType.getOption()) {
                  IpcdEnumValue value = new IpcdEnumValue();
                  value.setValue(optionType.getValue());
                  value.setDescription(StringUtils.isEmpty(optionType.getDescription()) ? optionType.getValue() : optionType.getDescription());
                  values.add(value);
               }
            }
            ipcdEnum.setValues(values);
            enums.add(ipcdEnum);
         }
      }
      return enums;
   }
   
   private List<IpcdObject> buildTypes(TypesType typesType) {
      List<IpcdObject> types = new ArrayList<>();
      for (TypeType type : typesType.getType()) {
         types.add(buildType(type));
      }
      return types;
   }
   
   private IpcdObject buildMessage(MessageType message) {
      IpcdObject ipcdObject = makeIpcdObject(message.getName(), message.getDescription());
      ipcdObject.setType(message.getType());
      ipcdObject.setClosures(message.isClosures());
      if (!StringUtils.isEmpty(message.getCommandElement())) {
         ipcdObject.setCommandElement(message.getCommandElement());
      }
      List<Property> properties = new ArrayList<>();
      List<Property> virtualProperties = new ArrayList<>();
      buildProperties(properties, virtualProperties, message.getProperty(), true);
      ipcdObject.setProperties(properties);
      ipcdObject.setVirtualProperties(virtualProperties);
      return ipcdObject;
   }
   
   private IpcdObject buildCommand(IpcdObject commandDefinition, CommandType command) {
      IpcdObject ipcdObject = makeIpcdObject(command.getName(), command.getDescription());
      List<Property> properties = new ArrayList<>(commandDefinition.getProperties());
      if (command.getParameters() != null) {
         buildProperties(properties, null, command.getParameters().getProperty(), false);
      }
      setOverrides(properties, getVirtualPropertyNames(commandDefinition));
      ipcdObject.setProperties(properties);
      return ipcdObject;
   }
   
   private IpcdObject buildResponse(IpcdObject responseDefinition, CommandType command) {
      ResponseType response = command.getResponse();
      IpcdObject ipcdObject = makeIpcdObject(command.getName() + "Response", response != null ? response.getDescription() : null);
      List<Property> properties = new ArrayList<>(responseDefinition.getProperties());
      Property cmdProp = new Property();
      cmdProp.setName("request");
      cmdProp.setType(command.getName() + "Command");
      cmdProp.setDescription("The " + command.getName() + " request this is a response to.");
      properties.add(cmdProp);
      Property resProp = new Property();
      resProp.setName("response");
      if (response != null) {
         resProp.setType(TypeParser.parse(response.getType()));
         resProp.setDescription(ipcdObject.getDescription());
      }
      else {
         resProp.setType("EmptyResponse");
         resProp.setDescription("Empty Response");
      }
      properties.add(resProp);
      setOverrides(properties, getVirtualPropertyNames(responseDefinition));
      ipcdObject.setProperties(properties);
      return ipcdObject;
   }
   
   private IpcdObject buildType(TypeType type) {
      IpcdObject ipcdObject = makeIpcdObject(type.getName(), type.getDescription());
      List<Property> properties = new ArrayList<>();
      buildProperties(properties, null, type.getProperty(), false);
      ipcdObject.setProperties(properties);
      return ipcdObject;
   }

   private void buildProperties(List<Property> properties, List<Property> virtualProperties, List<PropertyType> propertyTypes, boolean isDef) {
      if (propertyTypes != null && propertyTypes.size() > 0) {
         for (PropertyType propertyType : propertyTypes) {
            Property property = buildProperty(propertyType, isDef);
            if (virtualProperties != null && property.isVirtual()) {
               virtualProperties.add(property);
            }
            else {
               properties.add(property);
            }
         }
      }
   }
   
   private Property buildProperty(PropertyType propertyType, boolean isDef) {
      Property property = new Property();
      property.setName(propertyType.getName());
      property.setType(TypeParser.parse(propertyType.getType()));
      property.setKey(propertyType.isKey());
      property.setDef(isDef);
      property.setVirtual(propertyType.isVirtual());
      property.setDescription(StringUtils.isEmpty(propertyType.getDescription()) ? propertyType.getName() : propertyType.getDescription());
      property.setRequired(propertyType.isRequired());
      property.setOverride(false); // Overrides are always initialized to false
      return property;
   }
   
   private Set<String> getVirtualPropertyNames(IpcdObject def) {
      List<Property> vps = def.getVirtualProperties();
      if (vps == null || vps.isEmpty()) {
         return null;
      }
      Set<String> set = new HashSet<>();
      for (Property vp : vps) {
         set.add(vp.getName());
      }
      return set;
   }
   
   private void setOverrides(List<Property> properties, Set<String> virtuals) {
      if (virtuals == null || virtuals.isEmpty()) {
         return;
      }
      for (Property property : properties) {
         if (virtuals.contains(property.getName())) {
            property.setOverride(true);
         }
      }
   }
   
   private IpcdObject makeIpcdObject(String name, String description) {   
      IpcdObject ipcdObject = new IpcdObject();
      ipcdObject.setName(name);
      ipcdObject.setDescription(StringUtils.isEmpty(description) ? name: description);
      return ipcdObject;
   }
}

