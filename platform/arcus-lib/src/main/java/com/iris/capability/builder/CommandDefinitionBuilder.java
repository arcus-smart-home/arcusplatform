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
package com.iris.capability.builder;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.iris.capability.attribute.Attributes;
import com.iris.capability.attribute.ChainedAttributeDefinitionBuilder;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.CommandDefinition;

/**
 *
 */
public class CommandDefinitionBuilder {
   private String namespace;
   private String command;
   private String description;
   private Map<String, AttributeDefinition> returns = new LinkedHashMap<>();
   private Map<String, AttributeDefinition> input = new LinkedHashMap<>();

   CommandDefinitionBuilder(String namespace, String command) {
      Preconditions.checkNotNull(namespace, "namespace");
      Preconditions.checkNotNull(command, "command");
      this.namespace = namespace;
      this.command = command;
   }

   public CommandDefinitionBuilder withDescription(String description) {
      this.description = description;
      return this;
   }

   public CommandDefinitionBuilder addReturnParameter(AttributeDefinition attribute) {
      String key = attribute.getName();
      if(returns.containsKey(key)) {
         throw new IllegalArgumentException("There is already a return parameter named [" + key + "] defined");
      }
      returns.put(key, attribute);
      return this;
   }

   public ChainedAttributeDefinitionBuilder<CommandDefinitionBuilder> buildReturnParameter(String name, Class<?> type) {
      return Attributes.build(name, type).chain((attribute) -> this.addReturnParameter(attribute));
   }

   public ChainedAttributeDefinitionBuilder<CommandDefinitionBuilder> buildReturnParameter(AttributeKey<?> key) {
      return Attributes.build(key).chain((attribute) -> this.addReturnParameter(attribute));
   }

   public CommandDefinitionBuilder addInputArgument(AttributeDefinition attribute) {
      String key = attribute.getName();
      if(input.containsKey(key)) {
         throw new IllegalArgumentException("There is already an attribute named [" + key + "] defined");
      }
      input.put(key, attribute);
      return this;
   }

   public ChainedAttributeDefinitionBuilder<CommandDefinitionBuilder> buildInputArgument(String name, Class<?> type) {
      return Attributes.build(name, type).chain((attribute) -> this.addInputArgument(attribute));
   }

   public ChainedAttributeDefinitionBuilder<CommandDefinitionBuilder> buildInputArgument(AttributeKey<?> key) {
      return Attributes.build(key).chain((attribute) -> this.addInputArgument(attribute));
   }

   public CommandDefinition create() {
      return new CommandDefinition(namespace, command, description, returns, input);
   }

}

