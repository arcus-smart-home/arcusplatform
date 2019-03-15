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
package com.iris.model.type;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EnumType implements AttributeType {

	private final Set<String> values = new HashSet<String>();

	public EnumType(String... values) {
	   if(values != null) {
	      this.values.addAll(Arrays.asList(values));
	   }
	}

	public EnumType(Collection<String> values) {
		this.values.addAll(values);
	}

	@Override
	public Class<String> getJavaType() {
		return StringType.INSTANCE.getJavaType();
	}

	@Override
	public String getTypeName() {
		return "enum";
	}

	public Set<String> getValues() {
		return Collections.unmodifiableSet(values);
	}

	@Override
   public String coerce(Object obj) {
	   if(obj == null) {
	      return null;
	   }

	   String value = String.valueOf(obj);
	   if(values.contains(value)) {
	      return value;
	   }
	   
	   for(String v: values) {
	      if(value.equalsIgnoreCase(v)) {
	         return v;
	      }
	   }

	   throw new IllegalArgumentException(obj + " is not a valid member of the enumeration set " + values);
   }

   @Override
   public boolean isAssignableFrom(Type type) {
      if(type == null) {
         return false;
      }
      return String.class.equals(type);
   }

   @Override
   public String toString() {
      return "enum" + values;
   }
   
   @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((values == null) ? 0 : values.hashCode());
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
		EnumType other = (EnumType) obj;
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}

}

