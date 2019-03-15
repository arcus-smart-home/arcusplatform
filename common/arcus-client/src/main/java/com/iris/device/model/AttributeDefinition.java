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
package com.iris.device.model;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.iris.Utils;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeValue;
import com.iris.model.type.AttributeType;

/**
 * @deprecated use com.iris.capability.definition.AttributeDefinition instead
 */
public class AttributeDefinition {
	private final AttributeKey<?> key;
	private final Set<AttributeFlag> flags;
	private final String description;
	private final String unit; // TODO should this be enumerated?
	// TODO validations
	private final AttributeType attrType;

	public AttributeDefinition(
			AttributeKey<?> key,
         Set<AttributeFlag> flags,
         String description,
         String unit,
         AttributeType attrType
   ) {
	   Utils.assertNotNull(key, "key may not be null");
	   this.key = key;
	   this.flags = Collections.unmodifiableSet( EnumSet.copyOf(flags) );
	   this.description = description;
	   this.unit = unit;
	   this.attrType = attrType;
   }

	public AttributeKey<?> getKey() {
	   return key;
	}

	public String getName() {
		return key.getName();
	}

	public Type getType() {
		return key.getType();
	}

	public boolean isReadable() {
		return flags.contains(AttributeFlag.READABLE);
	}

	public boolean isWritable() {
		return flags.contains(AttributeFlag.WRITABLE);
	}

	public boolean isOptional() {
		return flags.contains(AttributeFlag.OPTIONAL);
	}

	public Set<AttributeFlag> getFlags() {
		return flags;
	}

	public String getDescription() {
		return description;
	}

	public String getUnit() {
		return unit;
	}

	public AttributeType getAttributeType() {
	   return attrType;
	}

   @SuppressWarnings({ "unchecked", "rawtypes" })
   public AttributeValue<?> coerceToValue(Object value) {
      // this is fine so long as key and attrType are compatible types
      return ((AttributeKey) key).valueOf(attrType.coerce(value));
   }
   
	@Override
   public String toString() {
	   return "AttributeDefinition [key=" + key
	         + ", flags=" + flags + ", description=" + description + ", unit="
	         + unit + "]";
   }

	@Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((attrType == null) ? 0 : attrType.hashCode());
      result = prime * result
            + ((description == null) ? 0 : description.hashCode());
      result = prime * result + ((flags == null) ? 0 : flags.hashCode());
      result = prime * result + ((key == null) ? 0 : key.hashCode());
      result = prime * result + ((unit == null) ? 0 : unit.hashCode());
      return result;
   }

	@Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      AttributeDefinition other = (AttributeDefinition) obj;
      if (attrType == null) {
         if (other.attrType != null)
            return false;
      } else if (!attrType.equals(other.attrType))
         return false;
      if (description == null) {
         if (other.description != null)
            return false;
      } else if (!description.equals(other.description))
         return false;
      if (flags == null) {
         if (other.flags != null)
            return false;
      } else if (!flags.equals(other.flags))
         return false;
      if (key == null) {
         if (other.key != null)
            return false;
      } else if (!key.equals(other.key))
         return false;
      if (unit == null) {
         if (other.unit != null)
            return false;
      } else if (!unit.equals(other.unit))
         return false;
      return true;
   }

}

