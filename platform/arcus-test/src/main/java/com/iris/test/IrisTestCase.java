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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.internal.ProviderMethodsModule;
import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.BootstrapException;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.resource.Resources;
import com.iris.resource.classpath.ClassPathResourceFactory;
import com.iris.resource.filesystem.FileSystemResourceFactory;

/**
 * Helper base class for writing tests that use {@link Bootstrap}.
 *
 * Features:
 *
 * <pre>
 *    - Supports injection via @Inject either fields or
 *      setters annotated with @Inject.
 *      Does not support constructor injection.
 *    - Allows services to be added via @Provide on methods.
 *    - Allows Module dependencies to be declared via
 *      @Modules
 *    - Extends Assert so you don't have to static import
 * </pre>
 *
 * To inject a value from a module setup your case as follows:
 *
 * <pre>
 *    @Modules(MyModule.class)
 *    public MyTest extends IrisTestCase {
 *       // field injection is generally preferred for test
 *       // cases due to its brevity and the general lack
 *       // of concern about normal construciton patterns for an object
 *       @Inject MyService service;
 *       OtherService otherService;
 *
 *       // but you can still use setter injection if you like
 *       @Inject
 *       public void setOtherService(OtherService otherService) {
 *          this.otherService = otherService;
 *       }
 *
 *       @Test
 *       public void myTest() throws Exception {
 *          assertNotNull(service);
 *          assertNotNull(otherService);
 *       }
 *    }
 * </pre>
 *
 * To expose a test case value to the context use @Provides
 *
 * <pre>
 *    @Modules(MyModule.class)
 *    public MyTest extends IrisTestCase {
 *       MyService mockService;
 *       MyOtherServiceImpl contextService;
 *
 *       // this will be called BEFORE any @Provides methods
 *       // ServiceLocator will NOT be available yet
 *       @PostConstruct
 *       public void initialize() {
 *          mockService = EasyMock.createMock(MyService.class);
 *          EasyMock.expect(mockService.doSomething()).andReturn(true).anyTimes();
 *          EasyMock.replay(mockService);
 *       }
 *
 *       @Provides
 *       @Singleton
 *       public MyService myService() { return service; }
 *
 *       @Provides
 *       @Singleton
 *       public MyOtherService myOtherService { return otherService; }
 *
 *       // ServiceLocator is now available
 *       @Before
 *       public void setUp() throws Exception {
 *          super.setUp();
 *          // not using @Inject so that we can up-cast the result to the implementation
 *          contextService = (MyOtherServiceImpl) ServiceLocator.getInstance(MyOtherService.class);
 *       }
 *
 *       // note we could also define another @Before method which will run after setUp(), but
 *       // that is a little less clear
 *
 *    }
 * </pre>
 */
public abstract class IrisTestCase extends Assert {

    protected Injector injector;

   @Before
   public void setUp() throws Exception {
      FileSystemResourceFactory fsf = new FileSystemResourceFactory("src/dist");
      Resources.registerDefaultFactory(fsf);
      Resources.registerFactory(fsf);
      Resources.registerFactory(new ClassPathResourceFactory());
      ServiceLocator.init(GuiceServiceLocator.create(bootstrap()));
   }

   @After
   public void tearDown() throws Exception {
      ServiceLocator.destroy();
   }

   protected Injector bootstrap() throws ClassNotFoundException, BootstrapException {
      final Module m = ProviderMethodsModule.forObject(this);
      return
         Bootstrap
            .builder()
            .withModuleClassnames(moduleClassNames())
            .withModuleClasses(moduleClasses())
            .withModules(modules())
            .withModules(new AbstractModule() {
               @Override
               protected void configure() {
                  // generating sources for ProviderMethods will result in an NPE
                  m.configure(binder().skipSources(IrisTestCase.this.getClass()));
                  // get injections
                  bind(IrisTestCase.class).toInstance(IrisTestCase.this);
                  IrisTestCase.this.configure(binder());
               }
            })
            .withConfigPaths(configs())
            .build()
            .bootstrap();
   }

   protected Set<String> moduleClassNames() {
      return new HashSet<String>();
   }

   protected Set<Class<? extends Module>> moduleClasses() {
      Set<Class<? extends Module>> modules = new LinkedHashSet<Class<? extends Module>>();
      addModules(modules, this.getClass());
      return modules;
   }

   protected Set<Module> modules() {
      return new HashSet<Module>();
   }

   protected Set<String> configs() {
     return new HashSet<String>();
   }

   protected void configure(Binder binder) {

   }

   private void addModules(
         Set<Class<? extends Module>> modules,
         Class<?> type
   ) {
      Modules discovered = type.getAnnotation(Modules.class);
      if(discovered != null) {
         for(Class<? extends Module> cls: discovered.value()) {
            modules.add(cls);
         }
      }
      for(Class<?> iface: type.getInterfaces()) {
         addModules(modules, iface);
      }
      Class<?> parent = type.getSuperclass();
      if(parent != null && !Object.class.equals(parent)) {
         addModules(modules, parent);
      }
   }

}

