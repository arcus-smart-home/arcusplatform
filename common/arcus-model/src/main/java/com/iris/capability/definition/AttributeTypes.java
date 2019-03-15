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
package com.iris.capability.definition;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;
import com.iris.capability.definition.AttributeType.CollectionType;
import com.iris.capability.definition.AttributeType.EnumType;
import com.iris.capability.definition.AttributeType.ObjectType;
import com.iris.capability.definition.AttributeType.RawType;

public class AttributeTypes {
   private static final Pattern pattern = Pattern.compile("^(\\w+)(<.*>)?$");

   public static AttributeType parse(String type) {
      Matcher m = pattern.matcher(type);
      if(!m.matches()) {
         throw new IllegalArgumentException("Not a valid AttributeType [" + type + "]");
      }
      String rawTypeName = m.group(1);
      RawType rawType = null;
      try {
         rawType = RawType.valueOf(rawTypeName.toUpperCase());
      }
      catch(IllegalArgumentException e) {
         if("uuid".equalsIgnoreCase(rawTypeName)) {
            rawType = RawType.STRING;
         }
         else {
            rawType = RawType.OBJECT;
         }
      }
      switch(rawType) {
      case VOID:
         assertNotParameterized(rawType, m);
         return voidType();
      case ANY:
         assertNotParameterized(rawType, m);
         return anyType();
      case BOOLEAN:
         assertNotParameterized(rawType, m);
         return booleanType();
      case BYTE:
         assertNotParameterized(rawType, m);
         return byteType();
      case INT:
         assertNotParameterized(rawType, m);
         return intType();
      case LONG:
         assertNotParameterized(rawType, m);
         return longType();
      case TIMESTAMP:
         assertNotParameterized(rawType, m);
         return timestampType();
      case DOUBLE:
         assertNotParameterized(rawType, m);
         return doubleType();
      case STRING:
         assertNotParameterized(rawType, m);
         return stringType();
      case SET:
         return setOf(extractInnerType(m));
      case LIST:
         return listOf(extractInnerType(m));
      case MAP:
         return mapOf(extractInnerType(m));

      // TODO need help to construct a generic enum type...
      case ENUM:
         return enumOf(extractEnumValues(m));

      // TODO need help to construct a generic object type...
      case OBJECT:
         return objectOf(rawTypeName, Collections.<String, AttributeType>emptyMap());

      default:
         throw new UnsupportedOperationException("Unsupported type " + rawType);
      }
   }

   public static AttributeType fromJavaType(Type type) {
      if(type == null || Void.TYPE.equals(type)) {
         return voidType();
      }

      if(type instanceof Class) {
         return fromRawJavaType((Class<?>) type);
      }

      if(type instanceof ParameterizedType) {
         return fromParameterizedJavaType((ParameterizedType) type);
      }

      throw new IllegalArgumentException("Unable to convert Java Type " + type + " to AttributeType");
   }

   private static AttributeType fromParameterizedJavaType(ParameterizedType type) {
      if(!(type.getRawType() instanceof Class)) {
         throw new IllegalArgumentException("Unable to convert Java Type " + type + " to AttributeType");
      }
      Class<?> rawType = (Class<?>) type.getRawType();
      if(Enum.class.isAssignableFrom(rawType)) {
         return fromJavaType(rawType);
      }

      Type [] typeArguments = type.getActualTypeArguments();
      if(Set.class.isAssignableFrom(rawType) && typeArguments.length == 1) {
         return setOf(fromJavaType(typeArguments[0]));
      }
      if(
            List.class.isAssignableFrom(rawType) && typeArguments.length == 1 ||
            Collection.class.isAssignableFrom(rawType) && typeArguments.length == 1
      ) {
         return listOf(fromJavaType(typeArguments[0]));
      }
      if(Map.class.isAssignableFrom(rawType) && typeArguments.length == 2 && String.class.equals(typeArguments[0])) {
         return mapOf(fromJavaType(typeArguments[1]));
      }
      throw new IllegalArgumentException("Unable to convert Java Type " + type + " to AttributeType");
   }

   private static AttributeType fromRawJavaType(Class<?> type) {
      if(Object.class.equals(type)) {
         return anyType();
      }
      if(Boolean.class.equals(type)) {
         return booleanType();
      }
      if(Byte.class.equals(type)) {
         return byteType();
      }
      if(Short.class.equals(type) || Integer.class.equals(type)) {
         return intType();
      }
      if(Long.class.equals(type)) {
         return longType();
      }
      if(Number.class.isAssignableFrom(type)) {
         return doubleType();
      }
      if(Date.class.isAssignableFrom(type)) {
         return timestampType();
      }
      if(String.class.isAssignableFrom(type)) {
         return stringType();
      }
      if(Set.class.isAssignableFrom(type)) {
         return setOf(anyType());
      }
      if(List.class.isAssignableFrom(type)) {
         return listOf(anyType());
      }
      if(Map.class.isAssignableFrom(type)) {
         return mapOf(anyType());
      }
      // TODO handle object types
      if(Enum.class.isAssignableFrom(type)) {
         Set<Enum<?>> values = (Set<Enum<?>>) EnumSet.allOf((Class)type);
         List<String> names = new ArrayList<String>(values.size() + 1);
         for(Enum<?> e: values) {
            names.add(e.name());
         }
         return enumOf(names);
      }
      return anyType();
   }

   public static AttributeType voidType() {
      return PrimitiveType.VOID;
   }

   public static AttributeType anyType() {
      return PrimitiveType.ANY;
   }

   public static AttributeType booleanType() {
      return PrimitiveType.BOOLEAN;
   }

   public static AttributeType byteType() {
      return PrimitiveType.BYTE;
   }

   public static AttributeType intType() {
      return PrimitiveType.INT;
   }

   public static AttributeType longType() {
      return PrimitiveType.LONG;
   }

   public static AttributeType timestampType() {
      return PrimitiveType.TIMESTAMP;
   }

   public static AttributeType doubleType() {
      return PrimitiveType.DOUBLE;
   }

   public static AttributeType stringType() {
      return PrimitiveType.STRING;
   }

   public static AttributeType attributesType() {
      return ATTRIBUTES_TYPE;
   }

   public static EnumType enumOf(Collection<String> values) {
      return new EnumTypeImpl(values);
   }

   public static CollectionType setOf(AttributeType innerType) {
      return new CollectionTypeImpl(RawType.SET, innerType, new ParameterizedTypeImpl(RawType.SET.getJavaType(), innerType.getJavaType()));
   }

   public static CollectionType listOf(AttributeType innerType) {
      return new CollectionTypeImpl(RawType.LIST, innerType, new ParameterizedTypeImpl(RawType.LIST.getJavaType(), innerType.getJavaType()));
   }

   public static CollectionType mapOf(AttributeType innerType) {
      return new CollectionTypeImpl(RawType.MAP, innerType, new ParameterizedTypeImpl(RawType.MAP.getJavaType(), String.class, innerType.getJavaType()));
   }

   // TODO should this be AttributeDefinition instead of just types?
   public static ObjectType objectOf(Map<String, AttributeType> attributes) {
      return new ObjectTypeImpl("Object", attributes);
   }

   // TODO tie into some sort of object registry?
   public static ObjectType objectOf(String name, Map<String, AttributeType> attributes) {
      return new ObjectTypeImpl(name, attributes);
   }

   public static Boolean coerceBoolean(Object obj) {
      if(obj == null) {
         return null;
      }

      if(obj instanceof Boolean) {
         return (Boolean) obj;
      }

      if(obj instanceof Number) {
         // TODO:  should we only allow 0 or 1?
         return ((Number) obj).longValue() != 0;
      }

      if(obj instanceof String) {
         // TODO:  should we coerce text such as "yes", "1"?
         return Boolean.valueOf((String) obj);
      }

      throw new IllegalArgumentException("Cannot coerce object of type " + obj.getClass() + " to " + PrimitiveType.BOOLEAN.getRepresentation());
   }

   public static Byte coerceByte(Object obj) {
      if(obj == null) {
         return null;
      }

      if(obj instanceof Byte) {
         return (Byte) obj;
      }

      if(obj instanceof String) {
         return Byte.valueOf((String) obj);
      }

      // TODO:  assuming signed bytes, do we need to support unsigned?
      if(obj instanceof Number) {
         long value = ((Number) obj).longValue();
         if(value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            return (byte) value;
         }
         throw new IllegalArgumentException("Numeric value " + obj + " cannot be coerced to " + PrimitiveType.BYTE.getRepresentation() + " without data loss.");
      }

      throw new IllegalArgumentException("Cannot coerce object of type " + obj.getClass() + " to " + PrimitiveType.BYTE.getRepresentation());
   }

   public static Integer coerceInt(Object obj) {
      if(obj == null) {
         return null;
      }

      // TODO:  should we worry about BigInteger or BigDecimal?
      if(obj instanceof Number) {
         long l = ((Number) obj).longValue();
         if(l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
            return (int) l;
         }
         throw new IllegalArgumentException("Numerical value " + obj + " could not be coerced to " + PrimitiveType.INT.getRepresentation() + " without data loss");
      }

      if(obj instanceof String) {
         return Integer.valueOf((String) obj);
      }

      throw new IllegalArgumentException("Cannot coerce object of type " + obj.getClass() + " to " + PrimitiveType.INT.getRepresentation());
   }

   public static Long coerceLong(Object obj) {
      if(obj == null) {
         return null;
      }

      if(obj instanceof Number) {
         return ((Number) obj).longValue();
      }

      if(obj instanceof String) {
         return Long.valueOf((String) obj);
      }

      throw new IllegalArgumentException("Cannot coerce object of type " + obj.getClass() + " to " + PrimitiveType.LONG.getRepresentation());
   }

   public static Double coerceDouble(Object obj) {
      if(obj == null) {
         return null;
      }

      // TODO:  do we need to check if a BigInteger or BigDecimal could fit into a double?
      if(obj instanceof Number) {
         return ((Number) obj).doubleValue();
      }

      if(obj instanceof String) {
         return Double.valueOf((String) obj);
      }

      throw new IllegalArgumentException("Cannot coerce object of type " + obj.getClass() + " to " + PrimitiveType.DOUBLE.getRepresentation());

   }

   public static Date coerceTimestamp(Object obj) {
      if(obj == null) {
         return null;
      }

      if(obj instanceof Date) {
         return (Date) obj;
      }

      if(obj instanceof Calendar) {
         return ((Calendar) obj).getTime();
      }

      if(obj instanceof Number) {
         return new Date(((Number)obj).longValue());
      }

      if(obj instanceof String) {
         // TODO:  we could support parsing strings as well, but it is unclear what datetime format
         // we can expect
         // -- To do this you also need the format of the date time string, like "yy-mm-dd HH:SS.msec".
         //    It is easier to store in number format and track time zone and daylight savings as needed
         //    and then format on the end points to match local heuristics. - HH
         try {
            Long millis = Long.parseLong((String) obj);
            return new Date(millis);
         } catch(NumberFormatException nfe) {
            throw new IllegalArgumentException("Cannot coerce " + obj + " to " + PrimitiveType.TIMESTAMP.getRepresentation());
         }
      }

      throw new IllegalArgumentException("Cannot coerce object of type " + obj.getClass() + " to " + PrimitiveType.TIMESTAMP.getRepresentation());
   }

   public static String coerceString(Object obj) {
      if(obj == null) {
         return null;
      }

      return String.valueOf(obj);
   }

   public static Set<Object> coerceSet(Object obj) {
      return coerceSet(obj, anyType());
   }

   @SuppressWarnings({ "unchecked" })
   public static Set<Object> coerceSet(Object obj, AttributeType containedType) {
      if(obj == null) {
         return null;
      }

      Set<Object> set = new LinkedHashSet<Object>();

      if(obj instanceof Iterable) {
         Iterable<Object> iter = (Iterable<Object>) obj;
         for(Object o : iter) {
            set.add(containedType.coerce(o));
         }
      }
      else if(obj.getClass().isArray()) {
         for(int i = 0; i < Array.getLength(obj); i++) {
            set.add(containedType.coerce(Array.get(obj, i)));
         }
      }
      else {
         throw new IllegalArgumentException("Cannot coerce object of type " + obj.getClass() + " to " + setOf(containedType).getRepresentation());
      }

      return set;
   }

   public static List<Object> coerceList(Object obj) {
      return coerceList(obj, anyType());
   }

   @SuppressWarnings("unchecked")
   public static List<Object> coerceList(Object obj, AttributeType containedType) {
      if(obj == null) {
         return null;
      }

      List<Object> list = new ArrayList<Object>();

      if(obj instanceof Iterable) {
         Iterable<Object> iter = (Iterable<Object>) obj;
         for(Object o : iter) {
            list.add(containedType.coerce(o));
         }
      }
      else if(obj.getClass().isArray()) {
         for(int i = 0; i < Array.getLength(obj); i++) {
            list.add(containedType.coerce(Array.get(obj, i)));
         }
      }
      else {
         throw new IllegalArgumentException("Cannot coerce object of type " + obj.getClass() + " to " + listOf(containedType).getRepresentation());
      }

      return list;
   }

   public static Map<String, Object> coerceMap(Object obj) {
      return coerceMap(obj, anyType());
   }

   @SuppressWarnings("unchecked")
   public static Map<String, Object> coerceMap(Object obj, AttributeType containedType) {
      if(obj == null) {
         return null;
      }

      Map<String, Object> map = new LinkedHashMap<String, Object>();

      if(obj instanceof Map) {
         Map<Object, Object> m = (Map<Object, Object>) obj;
         for(Map.Entry<Object, Object> o : m.entrySet()) {
            String key = String.valueOf(o.getKey());
            map.put(key, containedType.coerce(o.getValue()));
         }
      }
      else {
         throw new IllegalArgumentException("Cannot coerce object of type " + obj.getClass() + " to " + mapOf(containedType).getRepresentation());
      }

      return map;
   }

   private static Collection<String> extractEnumValues(Matcher m) {
      if(m.group(2) == null) {
         return Collections.<String>emptySet();
      }
      return Arrays.asList(split(strip(m.group(2))));
   }

   private static AttributeType extractInnerType(Matcher m) {
      if(m.group(2) == null) {
         return anyType();
      }
      return parse(strip(m.group(2)));
   }

   private static void assertNotParameterized(RawType type, Matcher m) {
      if(m.group(2) != null) {
         throw new IllegalArgumentException("type " + type.name().toLowerCase() + " may not be parameterized");
      }
   }

   private static String strip(String generic) {
      return generic.substring(1, generic.length() - 1);
   }

   private static String [] split(String string) {
      if(string == null || string.isEmpty()) {
         return new String [] {};
      }
      return string.split("\\,");
   }

   public enum PrimitiveType implements AttributeType {
      VOID(RawType.VOID),
      BOOLEAN(RawType.BOOLEAN),
      BYTE(RawType.BYTE),
      INT(RawType.INT),
      LONG(RawType.LONG),
      TIMESTAMP(RawType.TIMESTAMP),
      DOUBLE(RawType.DOUBLE),
      STRING(RawType.STRING),
      ANY(RawType.ANY);

      private final RawType type;

      PrimitiveType(RawType type) {
         this.type = type;
      }

      @Override
      public RawType getRawType() {
         return type;
      }

      @Override
      public Type getJavaType() {
         return type.getJavaType();
      }

      @Override
      public String getRepresentation() {
         return name().toLowerCase();
      }

      @Override
      public boolean isPrimitive() {
         return true;
      }

      @Override
      public boolean isEnum() {
         return false;
      }

      @Override
      public boolean isCollection() {
         return false;
      }

      @Override
      public boolean isObject() {
         return false;
      }

      @Override
      public EnumType asEnum() {
         return null;
      }

      @Override
      public CollectionType asCollection() {
         return null;
      }

      @Override
      public ObjectType asObject() {
         return null;
      }

      @Override
      public Object coerce(Object o) {
         switch(this) {
         case BOOLEAN:   return coerceBoolean(o);
         case BYTE:      return coerceByte(o);
         case INT:       return coerceInt(o);
         case LONG:      return coerceLong(o);
         case DOUBLE:    return coerceDouble(o);
         case TIMESTAMP: return coerceTimestamp(o);
         case STRING:    return coerceString(o);
         case VOID:      return o; // should this always be null
         case ANY:       return o;
         default:
            throw new IllegalArgumentException("Unable to coerce type; " + type);
         }
      }

      @Override
      public String toString() {
         return "AttributeType [" + getRepresentation() + "]";
      }

   }

   private static class EnumTypeImpl implements EnumType {
      private final Map<String,String> values;

      EnumTypeImpl(Collection<String> values) {
         ImmutableMap.Builder<String,String> bld = ImmutableMap.builder();
         for (String value : values) {
            bld.put(value,value);
         }

         this.values = bld.build();
      }

      @Override
      public RawType getRawType() {
         return RawType.ENUM;
      }

      @Override
      public Type getJavaType() {
         return String.class;
      }

      @Override
      public String getRepresentation() {
         StringBuilder sb = new StringBuilder(RawType.ENUM.name().toLowerCase()).append("<");
         boolean first = true;
         for(String value: values.keySet()) {
            if(first) {
               first = false;
            }
            else {
               sb.append(",");
            }
            sb.append(value);
         }
         sb.append(">");
         return sb.toString();
      }

      @Override
      public Set<String> getValues() {
         return values.keySet();
      }

      @Override
      public boolean isPrimitive() {
         return false;
      }

      @Override
      public boolean isEnum() {
         return true;
      }

      @Override
      public boolean isCollection() {
         return false;
      }

      @Override
      public boolean isObject() {
         return false;
      }

      @Override
      public EnumType asEnum() {
         return this;
      }

      @Override
      public CollectionType asCollection() {
         return null;
      }

      @Override
      public ObjectType asObject() {
         return null;
      }

      @Override
      public Object coerce(Object obj) {
         if(obj == null) {
            return null;
         }

         String value = String.valueOf(obj);
         String val = values.get(obj);
         if (val != null) {
            return val;
         }

         for(String v: values.keySet()) {
            if(value.equalsIgnoreCase(v)) {
               return v;
            }
         }

         throw new IllegalArgumentException(obj + " is not a valid member of the enumeration set " + values);
      }
      
      public int ordinal(Object obj) {
         if (obj == null) {
            throw new IllegalArgumentException(obj + " cannot be null");
         }
         
         String key = (String) coerce(obj);  // this will throw if it isn't a legal part of this EnumType
         List<String> keys = new ArrayList<>(this.values.keySet()); // insert order is preserved by ImmutableMap.builder(), so the set will be the same each time.
         return keys.indexOf(key);
      }

      @Override
      public String toString() {
         return "AttributeType [" + getRepresentation() + "]";
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
         if (this == obj) return true;
         if (obj == null) return false;
         if (!(obj instanceof EnumType)) return false;
         EnumType other = (EnumType) obj;
         if (values == null) {
            if (other.getValues() != null) return false;
         }
         else if (!getValues().equals(other.getValues())) return false;
         return true;
      }
   }

   private static class CollectionTypeImpl implements CollectionType {
      private final RawType rawType;
      private final Type javaType;
      private final AttributeType containedType;

      CollectionTypeImpl(RawType rawType, AttributeType containedType, Type javaType) {
         this.rawType = rawType;
         this.containedType = containedType;
         this.javaType = javaType;
      }

      @Override
      public RawType getRawType() {
         return rawType;
      }

      @Override
      public Type getJavaType() {
         return javaType;
      }

      @Override
      public String getRepresentation() {
         return rawType.name().toLowerCase() + "<" + containedType.getRepresentation() + ">";
      }

      @Override
      public AttributeType getContainedType() {
         return containedType;
      }

      @Override
      public boolean isPrimitive() {
         return false;
      }

      @Override
      public boolean isEnum() {
         return false;
      }

      @Override
      public boolean isCollection() {
         return true;
      }

      @Override
      public boolean isObject() {
         return false;
      }

      @Override
      public EnumType asEnum() {
         return null;
      }

      @Override
      public CollectionType asCollection() {
         return this;
      }

      @Override
      public ObjectType asObject() {
         return null;
      }

      @Override
      public String toString() {
         return "AttributeType [" + getRepresentation() + "]";
      }

      @Override
      public Object coerce(Object value) {
         if(rawType == RawType.SET) {
            return coerceSet(value, containedType);
         } else if(rawType == RawType.MAP) {
            return coerceMap(value, containedType);
         }
         else {
            return coerceList(value, containedType);
         }
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result
               + ((containedType == null) ? 0 : containedType.hashCode());
         result = prime * result + ((rawType == null) ? 0 : rawType.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) return true;
         if (obj == null) return false;
         if (!(obj instanceof CollectionType)) return false;
         CollectionType other = (CollectionType) obj;
         if (containedType == null) {
            if (other.getContainedType() != null) return false;
         }
         else if (!containedType.equals(other.getContainedType())) return false;
         if (rawType != other.getRawType()) return false;
         return true;
      }

   }

   private static class ObjectTypeImpl implements ObjectType {
      private final String name;
      private final Type javaType = new ParameterizedTypeImpl(Map.class, String.class, Object.class);
      private final Map<String, AttributeType> attributes;

      ObjectTypeImpl(String name, Map<String, AttributeType> attributes) {
         this.name = name;
         this.attributes = Collections.unmodifiableMap(new LinkedHashMap<String, AttributeType>(attributes));
      }

      @Override
      public RawType getRawType() {
         return RawType.OBJECT;
      }

      @Override
      public Type getJavaType() {
         return javaType;
      }

      @Override
      public String getRepresentation() {
         return name;
      }

      @Override
      public Map<String, AttributeType> getAttributes() {
         return attributes;
      }

      @Override
      public boolean isPrimitive() {
         return false;
      }

      @Override
      public boolean isEnum() {
         return false;
      }

      @Override
      public boolean isCollection() {
         return false;
      }

      @Override
      public boolean isObject() {
         return true;
      }

      @Override
      public EnumType asEnum() {
         return null;
      }

      @Override
      public CollectionType asCollection() {
         return null;
      }

      @Override
      public ObjectType asObject() {
         return this;
      }

      @SuppressWarnings("unchecked")
      @Override
      public Object coerce(Object obj) {
         if(obj == null) {
            return null;
         }

         Map<String, Object> map = new LinkedHashMap<String, Object>();

         if(obj instanceof Map) {
            Map<Object, Object> m = (Map<Object, Object>) obj;
            for(Map.Entry<Object, Object> o : m.entrySet()) {
               String key = String.valueOf(o.getKey());
               AttributeType type = attributes.get(key);
               if(type == null) {
                  type = anyType();
               }
               map.put(key, type.coerce(o.getValue()));
            }
         }
         else {
            throw new IllegalArgumentException("Cannot coerce object of type " + obj.getClass() + " to " + getRepresentation());
         }

         return map;
      }

      @Override
      public String toString() {
         return "AttributeType [" + getRepresentation() + " " + attributes + "]";
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result
               + ((attributes == null) ? 0 : attributes.hashCode());
         result = prime * result + ((name == null) ? 0 : name.hashCode());
         return result;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) return true;
         if (obj == null) return false;
         if (!(obj instanceof ObjectType)) return false;
         ObjectType other = (ObjectType) obj;
         if (attributes == null) {
            if (other.getAttributes() != null) return false;
         }
         else if (!attributes.equals(other.getAttributes())) return false;
         if (name == null) {
            if (other.getRepresentation() != null) return false;
         }
         else if (!name.equals(other.getRepresentation())) return false;
         return true;
      }

   }

   private static final AttributeType ATTRIBUTES_TYPE =
         new CollectionTypeImpl(RawType.ATTRIBUTES, anyType(), new ParameterizedTypeImpl(Map.class, String.class, Object.class));

   private static class ParameterizedTypeImpl implements ParameterizedType {
      private final Class<?> rawType;
      private final Type [] typeArguments;

      ParameterizedTypeImpl(Class<?> rawType, Type... typeArguments) {
         this.rawType = rawType;
         this.typeArguments = typeArguments.clone();
      }

      @Override
      public Type[] getActualTypeArguments() {
         return typeArguments.clone();
      }

      @Override
      public Type getRawType() {
         return rawType;
      }

      @Override
      public Type getOwnerType() {
         return null;
      }

      @Override
      public String toString() {
         if(typeArguments.length == 0) {
            return rawType.toString();
         }
         StringBuilder sb = new StringBuilder(rawType.getName());
         sb.append("<");
         for(int i=0; i<typeArguments.length; i++) {
            if(i > 0) {
               sb.append(",");
            }
            Type type = typeArguments[i];
            if(type instanceof Class) {
               sb.append(((Class) type).getName());
            }
            else {
               sb.append(type.toString());
            }
         }
         sb.append(">");
         return sb.toString();
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((rawType == null) ? 0 : rawType.hashCode());
         result = prime * result + Arrays.hashCode(typeArguments);
         return result;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) return true;
         if (obj == null) return false;
         if (!(obj instanceof ParameterizedType)) return false;
         ParameterizedType other = (ParameterizedTypeImpl) obj;
         if (rawType == null) {
            if (other.getRawType() != null) return false;
         }
         else if (!rawType.equals(other.getRawType())) return false;
         if (!Arrays.equals(typeArguments, other.getActualTypeArguments())) return false;
         return true;
      }

   }

}

