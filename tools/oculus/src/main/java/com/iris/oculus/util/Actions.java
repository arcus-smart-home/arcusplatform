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
package com.iris.oculus.util;

import java.awt.event.ActionEvent;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.iris.oculus.Oculus;

/**
 * 
 */
// not instantiable
public abstract class Actions {
   private static final Logger logger = LoggerFactory.getLogger(Actions.class);
   
   public static Builder builder() {
      return new Builder();
   }
   
   public static Action build(String name, Consumer<? super ActionEvent> action) {
      return builder().withName(name).withAction(action).build();
   }
   
   public static Action build(String name, Runnable action) {
      return builder().withName(name).withRunnableAction(action).build();
   }
   
   public static Action build(String name, Object ths, String function) {
      return builder().withName(name).withReflectiveAction(ths, function).build();
   }
   
   public static <T> Action build(String name, T data, Consumer<T> function) {
      return builder().withName(name).withFunctionalAction(data, function).build();
   }
   
   public static <T> Action build(String name, Supplier<T> data, Consumer<T> function) {
      return builder().withName(name).withFunctionalAction(data, function).build();
   }
   
   protected Actions() { }
   
   public static class Builder {
      private Map<String, Object> keys = new LinkedHashMap<>();
      private Consumer<? super ActionEvent> action;
      
      public Builder withName(String name) {
         this.keys.put(Action.NAME, name);
         return this;
      }
      
      public Builder withAction(Consumer<? super ActionEvent> action) {
         this.action = action;
         return this;
      }
      
      public Builder withRunnableAction(Runnable runnable) {
         return withAction((event) -> runnable.run());
      }
      
      public <T> Builder withFunctionalAction(T data, Consumer<T> consumer) {
         return withAction((event) -> consumer.accept(data));
      }
      
      public <T> Builder withFunctionalAction(Supplier<T> supplier, Consumer<T> consumer) {
         return withAction((event) -> consumer.accept(supplier.get()));
      }
      
      public Builder withReflectiveAction(Object o, String functionName) {
         try {
            Method m = o.getClass().getMethod(functionName);
            return withAction((event) -> {
               try {
                  m.invoke(o);
               }
               catch(Exception e) {
                  Oculus.error("Error invoking callback", e);
               }
            });
         }
         catch(Exception e) {
            throw new IllegalArgumentException("Error loading callback method " + functionName, e);
         }
      }
      
      public Builder withToolTip(String text) {
         keys.put(JButton.TOOL_TIP_TEXT_KEY, text);
         return this;
      }
      
      public Builder withSmallIcon(Icon icon) {
         keys.put(Action.SMALL_ICON, icon);
         return this;
      }
      
      public Builder withSmallIcon(String resource) {
         try {
            withSmallIcon(Icons.load(resource));
         }
         catch(Exception e) {
            logger.warn("Unable to load resource {}", e);
         }
         return this;
      }
      
      public Action build() {
         Preconditions.checkState(action != null, "Must declare an action");
         RunnableAction action = new RunnableAction(this.action);
         for(Map.Entry<String, Object> key: keys.entrySet()) {
            action.putValue(key.getKey(), key.getValue());
         }
         return action;
      }

   }
   
   private static class RunnableAction extends AbstractAction {
      private Consumer<? super ActionEvent> consumer;
      
      public RunnableAction(Consumer<? super ActionEvent> consumer) {
         this.consumer = consumer;
      }
      
      @Override
      public void actionPerformed(ActionEvent e) {
         consumer.accept(e);
      }
      
   }
}

