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
package com.iris.capability.attribute;

import java.util.function.Function;

import com.iris.device.model.AttributeDefinition;
import com.iris.model.type.AttributeType;

/**
 *
 */
// TODO extract AttributeBuilder interface?
public class ChainedAttributeDefinitionBuilder<P> extends AttributeDefinitionBuilder {
	private final Function<AttributeDefinition, P> consumer;

	ChainedAttributeDefinitionBuilder(AttributeDefinitionBuilder delegate, Function<AttributeDefinition, P> consumer) {
		super(delegate);
		this.consumer = consumer;
   }

	@Override
   public ChainedAttributeDefinitionBuilder<P> readWrite() {
	   super.readWrite();
	   return this;
   }

	@Override
   public ChainedAttributeDefinitionBuilder<P> readOnly() {
	   super.readOnly();
	   return this;
   }

	@Override
   public ChainedAttributeDefinitionBuilder<P> writeOnly() {
	   super.writeOnly();
	   return this;
   }

	@Override
   public ChainedAttributeDefinitionBuilder<P> optional() {
	   super.optional();
	   return this;
   }

	@Override
   public ChainedAttributeDefinitionBuilder<P> required() {
	   super.required();
	   return this;
   }

	@Override
   public ChainedAttributeDefinitionBuilder<P> withDescription(String description) {
	   super.withDescription(description);
	   return this;
   }

	@Override
   public ChainedAttributeDefinitionBuilder<P> withUnits(String units) {
	   super.withUnits(units);
	   return this;
   }

	@Override
   public ChainedAttributeDefinitionBuilder<P> withAttributeType(AttributeType attributeType) {
      super.withAttributeType(attributeType);
      return this;
   }

   @Override
   public AttributeDefinition create() {
		AttributeDefinition attribute = super.create();
		consumer.apply(attribute);
		return attribute;
   }

	public P add() {
		return consumer.apply(super.create());
	}

	public AttributeDefinition addAndGet() {
		return create();
	}
}

