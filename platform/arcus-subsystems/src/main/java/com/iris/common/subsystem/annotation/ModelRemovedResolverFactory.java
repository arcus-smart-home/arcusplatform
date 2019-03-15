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
package com.iris.common.subsystem.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.google.common.base.Function;
import com.iris.common.subsystem.event.SubsystemEventAndContext;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.model.Model;

/**
 * 
 */
public class ModelRemovedResolverFactory extends EventResolverFactory {
   private static final Function<SubsystemEventAndContext, Model> GetModel = new Function<SubsystemEventAndContext, Model>() {
      @Override
      public Model apply(SubsystemEventAndContext input) {
         ModelRemovedEvent event = (ModelRemovedEvent) input.getEvent();
         return event.getModel();
      }
   };

   public ModelRemovedResolverFactory() {
      super(ModelRemovedEvent.class);
   }

   /* (non-Javadoc)
    * @see com.iris.common.subsystem.annotation.EventResolverFactory#getResolverForParameter(java.lang.reflect.Method, java.lang.reflect.Parameter)
    */
   @Override
   public Function<? super SubsystemEventAndContext, ?> getResolverForParameter(Method method, Type parameter, Annotation[] annotations) {
      if(parameter instanceof Class<?>) {
         Class<?> type = (Class<?>) parameter;
         if(type.equals(Model.class)) {
            return GetModel;
         }
      }
      return super.getResolverForParameter(method, parameter, annotations);
   }

}

