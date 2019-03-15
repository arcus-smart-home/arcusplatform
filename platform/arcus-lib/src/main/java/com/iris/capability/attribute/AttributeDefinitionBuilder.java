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

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.model.AttributeDefinition;
import com.iris.device.model.AttributeFlag;
import com.iris.model.type.AttributeType;
import com.iris.model.type.StringType;

/**
 *
 */
public class AttributeDefinitionBuilder {
	private AttributeKey<?> key;
	private String description = "";
	private String units = "";
	private Set<AttributeFlag> flags = EnumSet.of(AttributeFlag.READABLE, AttributeFlag.WRITABLE);
	private AttributeType attributeType = StringType.INSTANCE;

	AttributeDefinitionBuilder(AttributeKey<?> key) {
		Preconditions.checkNotNull(key, "key");
	   this.key = key;
   }

	AttributeDefinitionBuilder(AttributeDefinitionBuilder delegate) {
	   this.key = delegate.key;
	   this.description = delegate.description;
	   this.units = delegate.units;
	   this.flags = EnumSet.copyOf(delegate.flags);
	   this.attributeType = delegate.attributeType;
   }

	public <P> ChainedAttributeDefinitionBuilder<P> chain(final P parent, final Consumer<AttributeDefinition> consumer) {
		return new ChainedAttributeDefinitionBuilder<P>(this, (attribute) -> {
			consumer.accept(attribute);
			return parent;
		});
	}

	public <P> ChainedAttributeDefinitionBuilder<P> chain(Function<AttributeDefinition, P> consumer) {
		return new ChainedAttributeDefinitionBuilder<P>(this, consumer);
	}

	public AttributeDefinitionBuilder readWrite() {
		flags.add(AttributeFlag.READABLE);
		flags.add(AttributeFlag.WRITABLE);
		return this;
	}

	public AttributeDefinitionBuilder readOnly() {
		flags.add(AttributeFlag.READABLE);
		flags.remove(AttributeFlag.WRITABLE);
		return this;
	}

	public AttributeDefinitionBuilder writeOnly() {
		flags.remove(AttributeFlag.READABLE);
		flags.add(AttributeFlag.WRITABLE);
		return this;
	}

	public AttributeDefinitionBuilder optional() {
		flags.add(AttributeFlag.OPTIONAL);
		return this;
	}

	public AttributeDefinitionBuilder required() {
		flags.remove(AttributeFlag.OPTIONAL);
		return this;
	}

	public AttributeDefinitionBuilder withDescription(String description) {
		this.description = description;
		return this;
	}

	public AttributeDefinitionBuilder withUnits(String units) {
		this.units = units;
		return this;
	}

	public AttributeDefinitionBuilder withAttributeType(AttributeType attributeType) {
	   this.attributeType = attributeType;
	   return this;
	}

	public AttributeDefinition create() {
		return new AttributeDefinition(
				key,
				flags,
				description,
				units,
				attributeType
		);
	}
}

