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
package com.iris.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

import com.iris.type.TypeUtil;

public class IrisBeans {
	
	public static <T> BeanGetter<T> getter(Class<T> clazz) {
		return new BeanGetter<T>(clazz);
	}

	public static class BeanGetter<T> {
		private final Map<String, PropertyDescriptor> properties = new HashMap<>();
		
		public BeanGetter(Class<T> clazz) {
			try {
	         BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
	         if (beanInfo.getPropertyDescriptors() != null)
	         for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
	         	properties.put(descriptor.getName().toLowerCase(), descriptor);
	         }
         } catch (IntrospectionException e) {
	         throw new IllegalArgumentException("Cannot get BeanInfo for class: " + clazz.getName(), e);
         }
		}
		
		public Object get(String propertyName, T obj) {
			String lcPropertyName = propertyName.toLowerCase();
			if (properties.containsKey(lcPropertyName)) {
				try {
	            return properties.get(lcPropertyName).getReadMethod().invoke(obj);
            } catch (Exception e) {
	            return null;
            }
			}
			return null;
		}
		
		public <R> R getAs(Class<R> clazz, String propertyName, T obj) {
			Object value = get(propertyName, obj);
			return value != null ? TypeUtil.INSTANCE.attemptCoerce(clazz, value) : null;
		}
	}
}

