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
package com.iris.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bootstrap.ServiceLocator;

/**
 *
 */
public class TestIrisTestCase {

   @Test
   public void testProvideAndInject() throws Exception {
      ProvideAndInject pai = new ProvideAndInject();
      pai.setUp();
      assertServiceLocatorInitialized();
      
      assertEquals("value1", pai.value1);
      assertEquals("value2", pai.value2);
      assertEquals("value3", pai.value3);
      
      assertEquals("value1", ServiceLocator.getNamedInstance(String.class, "value1"));
      assertEquals("value2", ServiceLocator.getNamedInstance(String.class, "value2"));
      assertEquals("value3", ServiceLocator.getNamedInstance(String.class, "value3"));

      // these are injected, changing them should have no affect
      pai.value1 = "A new value1";
      pai.value2 = "A new value2";
      assertEquals("value1", ServiceLocator.getNamedInstance(String.class, "value1"));
      assertEquals("value2", ServiceLocator.getNamedInstance(String.class, "value2"));

      // this is provided, changing it should change the context
      pai.value3 = "A new value3";
      assertEquals("A new value3", ServiceLocator.getNamedInstance(String.class, "value3"));
      pai.tearDown();
      assertServiceLocatorNotInitialized();
   }
   
   @Test
   public void testModulesAnnotation() throws Exception {
      ModuleInject mi = new ModuleInject();
      mi.setUp();
      assertServiceLocatorInitialized();
      
      assertEquals(true, mi.configured);
      assertEquals(0, mi.count);
      assertNotNull(mi.singleton);
      
      assertEquals(Integer.valueOf(1), ServiceLocator.getInstance(int.class));
      assertEquals(Integer.valueOf(2), ServiceLocator.getInstance(int.class));
      assertSame(mi.singleton, ServiceLocator.getInstance(Object.class));
      
      mi.tearDown();
      assertServiceLocatorNotInitialized();
   }
   
   @Test
   public void testOverrideModuleClasses() throws Exception {
      OverrideModuleClasses test = new OverrideModuleClasses();
      test.setUp();
      assertServiceLocatorInitialized();
      
      assertEquals(true, test.configured);
      assertEquals(0, test.count);
      assertNotNull(test.singleton);
      
      assertEquals(Integer.valueOf(1), ServiceLocator.getInstance(int.class));
      assertEquals(Integer.valueOf(2), ServiceLocator.getInstance(int.class));
      assertSame(test.singleton, ServiceLocator.getInstance(Object.class));
      
      test.tearDown();
      assertServiceLocatorNotInitialized();
   }
   
   @Test
   public void testOverrideModuleClassNames() throws Exception {
      OverrideModuleClassNames test = new OverrideModuleClassNames();
      test.setUp();
      assertServiceLocatorInitialized();
      
      assertEquals(true, test.configured);
      assertEquals(0, test.count);
      assertNotNull(test.singleton);
      
      assertEquals(Integer.valueOf(1), ServiceLocator.getInstance(int.class));
      assertEquals(Integer.valueOf(2), ServiceLocator.getInstance(int.class));
      assertSame(test.singleton, ServiceLocator.getInstance(Object.class));
      
      test.tearDown();
      assertServiceLocatorNotInitialized();
   }
   
   @Test
   public void testOverrideModules() throws Exception {
      OverrideModules test = new OverrideModules();
      test.setUp();
      assertServiceLocatorInitialized();
      
      assertEquals(true, test.configured);
      assertEquals(0, test.count);
      assertNotNull(test.singleton);
      
      assertEquals(Integer.valueOf(1), ServiceLocator.getInstance(int.class));
      assertEquals(Integer.valueOf(2), ServiceLocator.getInstance(int.class));
      assertSame(test.singleton, ServiceLocator.getInstance(Object.class));
      
      test.tearDown();
      assertServiceLocatorNotInitialized();
   }
   
   protected static void assertServiceLocatorNotInitialized() {
      try {
         // this won't throw if there are no instances in the context
         // only if the service locator isn't initialize
         ServiceLocator.getInstancesOf(String.class);
         Assert.fail("ServiceLocator is initialized");
      }
      catch(IllegalStateException e) {
         // expected
      }
   }

   protected static void assertServiceLocatorInitialized() {
      try {
         // this won't throw if there are no instances in the context
         // only if the service locator isn't initialize
         ServiceLocator.getInstancesOf(String.class);
         
      }
      catch(IllegalStateException e) {
         Assert.fail("ServiceLocator is not initialized: " + e.getMessage());
      }
   }

   @Test
   public void testLifecycle() throws Exception {
      TestLifeCycle lifecycle = new TestLifeCycle();
      lifecycle.setUp();
      assertEquals(Arrays.asList("init", "provides"), lifecycle.methods);
      
      lifecycle.tearDown();
      assertEquals(Arrays.asList("init", "provides", "predestroy"), lifecycle.methods);
   }
   
   public static class ProvideAndInject extends IrisTestCase {
      @Inject
      @Named("value1")
      private String value1;
      
      @Inject
      @Named("value2")
      private String value2;
      
      private String value3 = "value3";
      
      @Provides
      @Named("value1")
      public String value1() { return "value1"; }

      @Provides
      @Named("value2")
      public String value2() { return "value2"; }

      @Provides
      @Named("value3")
      public String value3() { return value3; }
   }
   
   @Modules(TestModule.class)
   public static class ModuleInject extends IrisTestCase {
      @Inject
      boolean configured;
      
      @Inject
      int count;
      
      @Inject
      Object singleton;
   }
   
   public static class OverrideModuleClasses extends IrisTestCase {
      @Inject
      boolean configured;
      
      @Inject
      int count;
      
      @Inject
      Object singleton;

      @Override
      protected Set<Class<? extends Module>> moduleClasses() {
         return Collections.singleton(TestModule.class);
      }
      
   }

   public static class OverrideModuleClassNames extends IrisTestCase {
      @Inject
      boolean configured;
      
      @Inject
      int count;
      
      @Inject
      Object singleton;

      @Override
      protected Set<String> moduleClassNames() {
         return Collections.singleton(TestModule.class.getName());
      }
      
   }

   public static class OverrideModules extends IrisTestCase {
      @Inject
      boolean configured;
      
      @Inject
      int count;
      
      @Inject
      Object singleton;

      @Override
      protected Set<Module> modules() {
         return Collections.singleton(new TestModule());
      }
      
   }

   public static class TestLifeCycle extends IrisTestCase {
      private List<String> methods = new ArrayList<String>();
      
      @PostConstruct
      public void init() {
         methods.add("init");
         assertServiceLocatorNotInitialized();
      }
      
      @Provides
      @Singleton
      @Named("provides")
      public String provides() {
         methods.add("provides");
         assertServiceLocatorNotInitialized();
         return "provides";
      }
      
      @PreDestroy
      public void predestroy() {
         methods.add("predestroy");
         assertServiceLocatorNotInitialized();
      }
      
   }
   
   public static class TestModule extends AbstractModule {
      private boolean configured = false;
      private int counter = 0;
      
      @Override
      protected void configure() {
         configured = true;
      }
      
      @Provides
      public boolean configured() {
         return configured;
      }
      
      @Provides
      public int counter() {
         return counter++;
      }
      
      @Provides
      @Singleton
      public Object singleton() {
         return new Object();
      }
      
   }
}

