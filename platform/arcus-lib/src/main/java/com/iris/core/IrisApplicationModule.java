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
package com.iris.core;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.iris.info.IrisApplicationInfo;
import com.iris.resource.Resources;
import com.iris.resource.azure.AzureResourceModule;
import com.iris.resource.classpath.ClassPathResourceFactory;
import com.iris.resource.environment.EnvironmentVariableResourceFactory;
import com.iris.resource.filesystem.FileSystemResourceFactory;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;

public class IrisApplicationModule implements BootstrapModule {
   public static final String APPLICATION_PROPS         = "META-INF/application.properties";
   public static final String NAME_APPLICATION_NAME     = "application.name";
   public static final String NAME_APPLICATION_VERSION  = "application.version";
   public static final String NAME_APPLICATION_DIR      = "application.dir";
   
   private static final Logger logger = LoggerFactory.getLogger(IrisApplicationModule.class);

   public final static String DEFAULT_APPLICATION_NAME = "<unknown>";

   private String applicationName = DEFAULT_APPLICATION_NAME;
   private String applicationVersion = "<unknown>";
   private String applicationDir = "";

   @PostConstruct
   public void init() {
      try {
         Enumeration<URL> en = IrisApplicationModule.class.getClassLoader().getResources(APPLICATION_PROPS);
         List<URL> urls = new ArrayList<>(2);
         for(int i=0; en.hasMoreElements() && i < 2; i++) {
            urls.add(en.nextElement());
         }
         if(urls.isEmpty()) {
            logger.warn("No application.properties was found");
            return;
         }
         
         if(urls.size() > 1) {
            logger.warn("Multiple application.properties were found: {}, please remove the incorrect ones from the classpath", urls);
         }
         
         loadProps(urls.get(0));
      }
      catch (IOException e) {
         logger.info("Unable to load application.properties", e);
      }
   }
   
   private void loadProps(URL url) throws IOException {
      Properties props = new Properties();
      props.load(url.openStream());
      applicationName = props.getProperty(NAME_APPLICATION_NAME, applicationName);
      logger.debug("Application Name Loaded From Properties [{}]", applicationName);
      applicationVersion = props.getProperty(NAME_APPLICATION_VERSION, applicationVersion);
      logger.debug("Application Version Loaded From Properties [{}]", applicationVersion);
      if(props.containsKey(NAME_APPLICATION_DIR)) {
         applicationDir = props.getProperty(NAME_APPLICATION_DIR);
      }
      else {
         applicationDir = extractLocation(url);
      }
   }

   private String extractLocation(URL url) {
      try {
         URI uri = url.toURI();
         if("file".equals(uri.getScheme())) {
            // happens from gradle run
            return new File(new File(uri.getPath()).getParentFile(), "../../../../../src/dist").getCanonicalPath();
         }
         
         if("jar".equals(uri.getScheme())) {
            // happens from a deployment
            String spec = uri.getSchemeSpecificPart();
            int start = "file:".length();
            int end = spec.indexOf('!');
            return new File(spec.substring(start, end)).getParentFile().getParentFile().getCanonicalPath();
         }
      }
      catch(URISyntaxException | IOException e) {
         logger.warn("Unable to determine location of [{}], can't conver to URI", url, e);
      }
      
      return "";
   }

   public String getApplicationName() {
      return applicationName;
   }
   
   public String getApplicationVersion() {
      return applicationVersion;
   }
   
   public String getApplicationDirectory() {
      return applicationDir;
   }
   
   @Override
   public void configure(BootstrapBinder binder) {
      init();
      bindIfNotInEnvironmentProperty(binder, NAME_APPLICATION_NAME, getApplicationName());
      bindIfNotInEnvironmentProperty(binder, NAME_APPLICATION_VERSION, getApplicationVersion());
      bindIfNotInEnvironmentProperty(binder, NAME_APPLICATION_DIR, getApplicationDirectory());
      IrisApplicationInfo.setApplicationName(getApplicationName());
      IrisApplicationInfo.setApplicationVersion(getApplicationVersion());
      IrisApplicationInfo.setApplicationDirectory(getApplicationDirectory());
      
      FileSystemResourceFactory fs = new FileSystemResourceFactory(getApplicationDirectory());
      Resources.registerDefaultFactory(fs);
      Resources.registerFactory(fs);
      Resources.registerFactory(new ClassPathResourceFactory());
      Resources.registerFactory(new EnvironmentVariableResourceFactory());
      registerAzureResourceFactory(binder);
   }
   
   private void registerAzureResourceFactory(BootstrapBinder binder) {
   	try {
   		AzureResourceModule azureResourceModule = new AzureResourceModule();
   		binder.requestInjection(azureResourceModule);
   		binder.install(azureResourceModule);
   		
   	}catch(Exception e) {
   		logger.warn("Unable to register AzureResourceFactory with Resources", e);
   	}
	}

	/*
    * Since all environment properties (-D) get bound automatically to their name.  We don't want to bind these props if they
    * are already being bound.  Doing so will cause a com.google.inject.CreationException on LifecycleInjectorBuilder.build().createInjector();.
    * So if we want to override the property value via an environment variable, we won't bind this property.
    */
   private void bindIfNotInEnvironmentProperty(BootstrapBinder binder, String prop, String value){
      String envProp = System.getProperty(prop);
      if(envProp==null){
         binder
         .bind(Key.get(String.class, Names.named(prop)))
         .toInstance(value);
      }
      else{
         logger.info("Found property {} in system properties.  It will be bound with this value {}", prop, envProp);
      }
   }

}

