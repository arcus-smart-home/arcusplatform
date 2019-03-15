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
package com.iris.platform.rule.catalog.action;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import com.google.common.base.Function;
import com.iris.common.rule.Context;
import com.iris.platform.rule.catalog.function.FunctionFactory;

public class ParameterValueTime implements ParameterValue {
   
   public enum Type { DATE, TIME, DATETIME }
   
   private Date date;
   private Type type;

   @Override
   public Function<Context, String> getValueFunction(Map<String, Object> variables) {
      if (date != null) {
         return FunctionFactory.INSTANCE.createConstant(Context.class, getDateFormat().format(date));
      }
      else {
         return FunctionFactory.INSTANCE.createCurrentTimeFormatted(getDateFormat());
      }
   }
   
   public Date getDate() {
      return date;
   }
   
   public void setDate(Date date) {
      this.date = date;
   }
   
   public Type getType() {
      return type;
   }
   
   public void setType(Type type) {
      this.type = type;
   }
   
   private DateFormat getDateFormat() {
      if (type == null) {
         throw new IllegalArgumentException("A datetime parameter value must specify the type of date format.");
      }
      DateFormat df = type == Type.DATE
            ? DateFormat.getDateInstance() 
            : (type == Type.TIME ? DateFormat.getTimeInstance() : DateFormat.getDateTimeInstance());
      return df;
   }
}

