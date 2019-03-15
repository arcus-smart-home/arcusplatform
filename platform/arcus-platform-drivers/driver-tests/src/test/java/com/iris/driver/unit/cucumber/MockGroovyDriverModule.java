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
package com.iris.driver.unit.cucumber;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.easymock.EasyMock;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.iris.driver.DeviceDriver;
import com.iris.driver.capability.Capability;
import com.iris.driver.groovy.DriverBinding;
import com.iris.driver.groovy.GroovyDriverModule;
import com.iris.driver.groovy.binding.CapabilityEnvironmentBinding;
import com.iris.driver.groovy.binding.EnvironmentBinding;
import com.iris.driver.groovy.customizer.DriverCompilationCustomizer;
import com.iris.driver.groovy.pin.PinManagementContext;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.groovy.scheduler.OnScheduledClosure;
import com.iris.driver.groovy.scheduler.SchedulerContext;

public class MockGroovyDriverModule extends GroovyDriverModule {

   private final PinManagementContext mockPinManager = 
         EasyMock
            .createMockBuilder(PinManagementContext.class)
            .addMockedMethod("validatePin", String.class)
            .addMockedMethod("setActor")
            .createMock();
   private final CapturingSchedulerContext mockScheduler = new CapturingSchedulerContext(); 
         
   
   GroovyDriverPlugin mockGroovyDriverPlugin = new GroovyDriverPlugin() {
      
      @Override
      public void postProcessEnvironment(EnvironmentBinding binding) {
      }
      
      @Override
      public void enhanceEnvironment(EnvironmentBinding binding) {
         binding.setProperty("onEvent", new OnScheduledClosure(binding));
         binding.setProperty("Scheduler", mockScheduler);
         binding.setProperty("PinManagement", mockPinManager);
      }
      
      @Override
      public void enhanceDriver(DriverBinding binding, DeviceDriver driver) {
      }
      
      @Override
      public void enhanceCapability(CapabilityEnvironmentBinding binding, Capability capability) {

      }
   };
   
   
   @Override
   protected void configure() {
      bindSetOf(GroovyDriverPlugin.class)
        .addBinding()
        .toInstance(mockGroovyDriverPlugin);
      bindSetOf(CompilationCustomizer.class)
         .addBinding()
         .toInstance(new ImportCustomizer()
            .addImports("groovy.transform.Field")
            .addStaticStars("java.util.concurrent.TimeUnit")
         );
      bindSetOf(CompilationCustomizer.class)
         .addBinding()
         .to(DriverCompilationCustomizer.class);
   }
   
   @Provides @Singleton
   public CapturingSchedulerContext mockDriverScheduler() {
      return mockScheduler;
   }
   
   @Provides @Singleton
   public PinManagementContext mockPinManagement() {
      return mockPinManager;
   }

   public static class CapturingSchedulerContext extends SchedulerContext {
      private final Queue<CapturedScheduledEvent> events = new LinkedBlockingQueue<CapturedScheduledEvent>();
      
      public Queue<CapturedScheduledEvent> events() {
         return events;
      }
      
      @Override
      public void defer(String name) {
         scheduled("defer", name, 0, 0, null);
      }

      @Override
      public void defer(String name, Object data) {
         scheduled("defer", name, 0, 0, data);
      }

      @Override
      public void scheduleIn(String name, long delayMs) {
         scheduled("scheduleIn", name, delayMs, 0, null);
      }

      @Override
      public void scheduleIn(String name, Object data, long delayMs) {
         scheduled("scheduleIn", name, delayMs, 0, data);
      }

      @Override
      public void scheduleRepeating(String name, long delayMs, int maxRetry) {
         scheduled("scheduleRepeating", name, delayMs, maxRetry, null);
      }

      @Override
      public void scheduleRepeating(String name, Map<String, Object> data, long delayMs, int maxRetry) {
         scheduled("scheduleRepeating", name, delayMs, maxRetry, data);
      }

      @Override
      public void cancel(String name) {
         scheduled("cancel", name, 0, 0, null);
      }
      
      private void scheduled(String method, String event, long delayMs, int maxRetries, Object data) {
         events.add(new CapturedScheduledEvent(method, event, delayMs, maxRetries, data));
      }
   }

   public static class CapturedScheduledEvent {
      private static final Object DONT_CARE = new Object();
      String method;
      String event;
      long delayMs;
      int maxRetries;
      Object data;
      
      public CapturedScheduledEvent() {
         this.method = null;
         this.event = null;
         this.delayMs = -1;
         this.maxRetries = -1;
         this.data = DONT_CARE;
      }
      
      public CapturedScheduledEvent(
           String method, 
           String event, 
           long delayMs, 
           int maxRetries,
           Object data
      ) {
         super();
         this.method = method;
         this.event = event;
         this.delayMs = delayMs;
         this.maxRetries = maxRetries;
         this.data = data;
      }

     /**
      * @return the method
      */
     public String getMethod() {
        return method;
     }

     /**
      * @param method the method to set
      */
     public void setMethod(String method) {
        this.method = method;
     }

     /**
      * @return the event
      */
     public String getEvent() {
        return event;
     }

     /**
      * @param event the event to set
      */
     public void setEvent(String event) {
        this.event = event;
     }

     /**
      * @return the delayMs
      */
     public long getDelayMs() {
        return delayMs;
     }

     /**
      * @param delayMs the delayMs to set
      */
     public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
     }

     /**
      * @return the maxRetries
      */
     public int getMaxRetries() {
        return maxRetries;
     }

     /**
      * @param maxRetries the maxRetries to set
      */
     public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
     }

     /**
      * @return the data
      */
     public Object getData() {
        return data;
     }

     /**
      * @param data the data to set
      */
     public void setData(Object data) {
        this.data = data;
     }
     
     /* (non-Javadoc)
      * @see java.lang.Object#toString()
      */
     @Override
     public String toString() {
        return "ScheduledEvent [method=" + (method == null ? "<don't care>" : method)
              + ", event=" + (event == null ? "<don't care>" : event)
              + ", delayMs=" + (delayMs < 0 ? "<don't care>" : delayMs)
              + ", maxRetries=" + (maxRetries < 0 ? "<don't care>" : maxRetries)
              + ", data=" + (data == DONT_CARE ? "<don't care>" : data)
              + "]";
     }

     boolean matches(CapturedScheduledEvent other) {
        if(other == null) {
           return false;
        }
        
        if(!matches(method, other.getMethod())) {
           System.out.println("METHOD does not match");
           return false;
        }
        if(!matches(event, other.getEvent())) {
           System.out.println("EVENT does not match");
           return false;
        }
        if(!matches(delayMs, other.getDelayMs())) {
           System.out.println("DELAY MS does not match");
           return false;
        }
        if(!matches(maxRetries, other.getMaxRetries())) {
           System.out.println("MAX RETRIES does not match");
           return false;
        }
        if(!matches(data, other.getData())) {
           System.out.println("DATA does not match");
           return false;
        }

        return true;
     }

     private boolean matches(Object o1, Object o2) {
        if(o1 == null) {
           return true;
        }
        return o1.hashCode() ==o2.hashCode();
     }

     private boolean matches(int i1, int i2) {
        if(i1 < 0) {
           return true;
        }
        return i1 == i2;
     }
     
     private boolean matches(long l1, long l2) {
        if(l1 < 0) {
           return true;
        }
        return l1 == l2;
     }
     
   }

}

