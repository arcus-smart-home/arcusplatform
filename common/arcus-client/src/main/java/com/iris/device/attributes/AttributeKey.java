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
package com.iris.device.attributes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.iris.Utils;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeTypes;
import com.iris.capability.key.NamespacedKey;


/**
 * Represents a strongly typed attribute.
 */
public final class AttributeKey<T> implements Serializable {
   private static final long serialVersionUID = 3571191925508696021L;

   public static <T> AttributeKey<T> create(String name, Class<T> simpleType) {
      return new AttributeKey<T>(name, simpleType);
   }
   
   public static <T> AttributeKey<Set<T>> createSetOf(String name, Class<T> containedType) {
      return new AttributeKey<Set<T>>(name, TypeUtils.parameterize(Set.class, containedType));
   }
   
   public static <T> AttributeKey<List<T>> createListOf(String name, Class<T> containedType) {
      return new AttributeKey<List<T>>(name, TypeUtils.parameterize(List.class, containedType));
      
   }
   
   public static <T> AttributeKey<Map<String, T>> createMapOf(String name, Class<T> containedType) {
      return new AttributeKey<Map<String, T>>(name, TypeUtils.parameterize(Map.class, String.class, containedType));
   }
   
   // TODO this may be too wide open...
   public static AttributeKey<?> createType(String name, Type complexType) {
      Preconditions.checkNotNull(complexType);
      return new AttributeKey<Object>(name, wrap(complexType));
   }
   
   private static Type wrap(Type type) {
      if(type instanceof ParameterizedType) {
         ParameterizedType pType  = (ParameterizedType) type;
         // re-wrap in the internal representation of TypeUtils for equalities sake (and esp hashCode)
         type = TypeUtils.parameterize((Class<?>) pType.getRawType(), wrap(pType.getActualTypeArguments()));
      }
      return type;
   }
   
   private static Type[] wrap(Type[] types) {
      int length = types.length;
      Type[] wrapped = new Type[length];
      for(int i=0; i<length; i++) {
         wrapped[i] = wrap(types[i]);
      }
      return wrapped;
   }
   
   
	private final String name;
	// these should be final, but is not due to java serialization weirdness
	private transient String namespace;
	private transient String id;
	private transient String instance;
	private transient Type type;
	private transient AttributeType attributeType;

	AttributeKey(String name, Type type) {
		Utils.assertNotEmpty(name, "name may not be empty");
		Utils.assertNotNull(type, "type may not be null");
		this.name = name;
		this.type = type;
		if(AttributeMap.class.equals(type)) {
		   // not quite right, but close enough
		   this.attributeType = AttributeTypes.mapOf(AttributeTypes.anyType());
		}
		else {
		   this.attributeType = AttributeTypes.fromJavaType(type);
		}
		parseName();
	}
	
	public String getName() {
      return name;
   }

	public Type getType() {
      return type;
   }

	/**
    * @return the namespace
    */
   public String getNamespace() {
      return namespace;
   }

   /**
    * @return the id
    */
   public String getId() {
      return id;
   }

   /**
    * @return the instance
    */
   public @Nullable String getInstance() {
      return instance;
   }
   
   public boolean isInstance() {
      return instance != null;
   }

   public AttributeKey<T> instance(String instanceId) {
      return new AttributeKey<T>(NamespacedKey.representation(this.namespace, this.id, instanceId), type);
   }
   
   public AttributeValue<T> valueOf(final T value) {
		return new AttributeValue<T>(this, value);
	}
   
   public AttributeValue<T> coerceToValue(Object value) {
      return new AttributeValue<T>(this, (T) attributeType.coerce(value));
   }
	
	@Override
	public String toString() {
		return name + "<" + type + ">";
	}

	@Override
   public int hashCode() {
	   final int prime = 31;
	   int result = 1;
	   result = prime * result + ((name == null) ? 0 : name.hashCode());
	   result = prime * result + ((type == null) ? 0 : type.hashCode());
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
	   AttributeKey other = (AttributeKey) obj;
	   if (name == null) {
		   if (other.name != null)
			   return false;
	   } else if (!name.equals(other.name))
		   return false;
	   if (type == null) {
		   if (other.type != null)
			   return false;
	   } else if (!type.equals(other.type))
		   return false;
	   return true;
   }
	
	private void parseName() {
      String [] parts = StringUtils.split(name, ":", 3);
      switch(parts.length) {
      case 1:
         namespace = "";
         id = parts[0];
         instance = null;
         break;
         
      case 2:
         namespace = parts[0];
         id = parts[1];
         instance = null;
         break;
         
      case 3:
         namespace = parts[0];
         id = parts[1];
         instance = parts[2];
         break;
      
      default:
         // what?
         namespace = "";
         id = "";
         instance = null;
      }
	}
	
	private void writeObject(ObjectOutputStream oos) throws IOException {
	   oos.defaultWriteObject();
	   oos.writeObject(Utils.serializeType(type));
	}
	
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
	   ois.defaultReadObject();
      parseName();
	   String typeName = (String) ois.readObject();
	   this.type = Utils.deserializeType(typeName);
	   this.attributeType = AttributeTypes.fromJavaType(type);
	}
}

