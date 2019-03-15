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
package com.iris.resource.config;

import com.google.inject.multibindings.Multibinder;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.resource.ResourceFactory;
import com.iris.resource.classpath.ClassPathResourceFactory;
import com.iris.resource.environment.EnvironmentVariableResourceFactory;
import com.iris.resource.filesystem.FileSystemResourceFactory;

/**
 * Sets the default {@link ResourceFactory} to be a {@link FileSystemResourceFactory}
 * with a root directory of {@link #getRootDirectory()}.  Override this method
 * to change the default root.  Also registers a {@link ClassPathResourceFactory}.
 * 
 * New resource factories may be added by creating a set bind of type
 * {@link ResourceFactory}.
 */
// TODO should this implement BootstrapModule?
public class ResourceModule extends AbstractIrisModule {

   protected String getRootDirectory() {
      return "";
   }
   
   @Override
   protected void configure() {
      FileSystemResourceFactory fs = new FileSystemResourceFactory(getRootDirectory());
      
      bind(ResourceFactory.class)
         .toInstance(fs);
      
      Multibinder<ResourceFactory> factoryBinder = bindSetOf(ResourceFactory.class);
      factoryBinder.addBinding().toInstance(fs);
      factoryBinder.addBinding().toInstance(new ClassPathResourceFactory());
      factoryBinder.addBinding().toInstance(new EnvironmentVariableResourceFactory());
      
      bind(ResourceConfigurer.class)
         .asEagerSingleton();
   }
}

