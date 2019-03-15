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
package com.iris.platform.rule.catalog.function;

import java.text.DateFormat;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.iris.capability.definition.AttributeType;
import com.iris.common.rule.Context;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.time.DayOfWeek;
import com.iris.common.rule.time.TimeOfDay;
import com.iris.common.rule.type.RuleTypeUtil;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;
import com.iris.messages.model.ModelStore;
import com.iris.platform.rule.catalog.template.ConstantValue;
import com.iris.platform.rule.catalog.template.TemplatedValue;

public enum FunctionFactory {
   INSTANCE;
   
   private RuleTypeUtil types;
   
   // Pregenerated instances.
   private final Function<Object, Set<DayOfWeek>> toSetOfDays;
   private final Function<Object, Integer> toInteger;
   private final Function<Object, TimeOfDay> toTimeOfDay;
   private final Function<Object, Address> toAddress;
   private final Function<Object, Predicate<Model>> toModelPredicate;
   private final Function<ModelStore, Address> accountOwner;
   private final Function<String, AttributeType> toAttributeType;
   private final Function<Object, Double> toDouble;
   private final Function<Object, Boolean> toBoolean;
   
   private FunctionFactory() {
      this.types = RuleTypeUtil.INSTANCE;
      toSetOfDays = types.createSetTransformer(DayOfWeek.class, "toSetOfDays");
      toInteger = types.createTransformer(Integer.class, "toInteger");
      toTimeOfDay = types.createTransformer(TimeOfDay.class, "toTimeOfDay");
      toAddress = types.createTransformer(Address.class, "toAddress");
      toModelPredicate = new ParseQueryFunction();
      accountOwner = new GetAccountOwnerQuery();
      toAttributeType = ToAttributeType.instance();
      toDouble = types.createTransformer(Double.class, "toDouble");
      toBoolean = types.createTransformer(Boolean.class, "toBoolean");
   }
   
   public static Integer toInteger(TemplatedValue<Object> template, Map<String, Object> values) {
      return TemplatedValue.transform(INSTANCE.toInteger, template).apply(values);
   }
   
   public static Double toDouble(TemplatedValue<Object> template, Map<String, Object> values) {
      return TemplatedValue.transform(INSTANCE.toDouble, template).apply(values);
   }
   
   public static Boolean toBoolean(TemplatedValue<Object> template, Map<String, Object> values) {
	  return TemplatedValue.transform(INSTANCE.toBoolean, template).apply(values);
   }
   
   public static String toString(TemplatedValue<Object> template, Map<String, Object> values) {
      return TemplatedValue.transform(Functions.toStringFunction(), template).apply(values);
   }
   
   public static Address toAddress(TemplatedValue<Object> template, Map<String, Object> values) {
      return TemplatedValue.transform(INSTANCE.toAddress, template).apply(values);
   }
   
   public static Predicate<Model> toModelPredicate(TemplatedValue<Object> template, Map<String, Object> values) {
      return TemplatedValue.transform(INSTANCE.toModelPredicate, template).apply(values);
   }
   
   public static Set<DayOfWeek> toSetOfDays(TemplatedValue<Object> template, Map<String, Object> values) {
      return TemplatedValue.transform(INSTANCE.toSetOfDays, template).apply(values);
   }
   
   public static TimeOfDay toTimeOfDay(TemplatedValue<Object> template, Map<String, Object> values) {
      return TemplatedValue.transform(INSTANCE.toTimeOfDay, template).apply(values);
   }
   
   public static Object toAttributeValue(TemplatedValue<Object> template, AttributeType type, Map<String, Object> values) {
      Object o = template.apply(values);
      return type.coerce(o);
   }
   
   public static <V> Function<ActionContext, V> toActionContextFunction(TemplatedValue<V> template) {
      return INSTANCE.createGetTemplatedValueFromActionContext(template);
   }
   
   public static <K, V> Predicate<Map<K, V>> containsAll(Map<K, Predicate<? super V>> matchers) {
      return new MapContainsAllPredicate<>(matchers);
   }
   
   public static Function<String, AttributeType> toAttributeTypeFunction() {
      return INSTANCE.toAttributeType;
   }
   
   public static Function<Object, Object> createCoerceFunction(AttributeType type) {
      return new CoerceFunction(type);
   }

   public Function<Object, Set<DayOfWeek>> getToSetOfDays() {
      return toSetOfDays;
   }
   
   public Function<Object, Integer> getToInteger() {
      return toInteger;
   }
   
   public Function<Object, Boolean> getToBoolean() {
      return toBoolean;
   }
   
   public Function<Object, Double> getToDouble() {
      return toDouble;
   }
   
   public Function<Object, TimeOfDay> getToTimeOfDay() {
      return toTimeOfDay;
   }
   
   public Function<Object, Address> getToAddress() {
      return toAddress;
   }
   
   public Function<Object, Predicate<Model>> getToModelPredicate() {
      return toModelPredicate;
   }
   
   public Function<ModelStore, Address> getAccountOwner() {
      return accountOwner;
   }

   public <I, O> Function<I, O> createConstant(O value) {
      return new ConstantFunction<I, O>(value);
   }
   
   public <I, O> Function<I, O> createConstant(Class<I> inputClazz, final O value) {
      return new ConstantFunction<I, O>(value);
   }
   
   public <O> Function<Object, O> createTransformer(Class<O> targetClazz) {
      return types.createTransformer(targetClazz);
   }
   
   public <O> Function<Object, O> createTransformer(Class<O> targetClazz, String description) {
      return types.createTransformer(targetClazz, description);
   }
   
   public <E extends Enum<E>> Function<String, E> createToEnum(Class<E> type) {
      return new ToEnumFunction<E>(type);
   }
   
   public <T> Function<Context, T> createGetAttribute(Class<T> target, Address address, String attribute) {
      return new GetAttributeValue<T>(target, address, attribute, types);
   }

   public Function<Context, String> createCurrentTimeFormatted(DateFormat dateFormat) {
      return new GetCurrentTimeFormatted(dateFormat);
   } 
   
   public <T> Function<ActionContext, T> createGetVariableFromActionContext(Class<T> target, String var) {
      return new GetVariableFromActionContext<T>(target, var, types);
   }
   
   public<T> Function<ActionContext, T> createGetTemplatedValueFromActionContext(TemplatedValue<T> value) {
      if(value instanceof ConstantValue) {
         return createConstant(value.apply(ImmutableMap.of()));
      }
      return new GetTemplatedValueFromActionContext<T>(value);
   }

}

