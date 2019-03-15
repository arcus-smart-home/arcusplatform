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
package com.iris.messages.capability;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.capability.definition.BaseDefinitionRegistry;
import com.iris.capability.definition.Definition;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.capability.definition.EventDefinition;
import com.iris.capability.definition.MergeableDefinition;
import com.iris.capability.definition.ServiceDefinition;
import com.iris.capability.definition.TypeDefinition;
import com.iris.capability.reader.GenericDefinitionReader;

@Singleton
public class ClasspathDefinitionRegistry implements DefinitionRegistry {
   private static final Logger logger = LoggerFactory.getLogger(ClasspathDefinitionRegistry.class);
   
   public static DefinitionRegistry instance() {
      return RegistryLoader.get();
   }
   
   private static class RegistryLoader {
      private static final DefinitionRegistry instance;
      private static final Throwable error;
      
      static {
         DefinitionRegistry registry = null;
         Throwable cause = null;
         try {
            registry = new ClasspathDefinitionRegistry( findDefinitions() );
         }
         catch(Throwable t) {
            logger.warn("Unable to load registry", t);
            cause = t;
         }
         
         instance = registry;
         error = cause;
      }
      
      public static DefinitionRegistry get() {
         if(error != null) {
            throw new IllegalStateException("Error loading the registry", error);
         }
         return instance;
      }

      private static List<String> findDefinitions() throws Exception {
         Enumeration<URL> urls = ClasspathDefinitionRegistry.class.getClassLoader().getResources("type/");
         if(!urls.hasMoreElements()) {
            throw new IllegalStateException("Unable to determine capability directory");
         }
         List<String> definitions = new LinkedList<>();
         while(urls.hasMoreElements()) {
            URL u = urls.nextElement();
            if("file".equals(u.getProtocol())) {
               definitions.addAll(loadFromFolder(u.getPath()));
            }
            else if("jar".equals(u.getProtocol())) {
               definitions.addAll(loadFromJar(u.getPath()));
            }
            else {
               logger.info("ignoring {}", u);
            }
         }
         return definitions;
      }
      
      private static List<String> loadFromFolder(String path) {
         List<String> resources = new ArrayList<String>();
         File folder = new File(path).getParentFile();
         addAll("capability", folder, resources);
         addAll("service", folder, resources);
         addAll("type", folder, resources);
         
         return resources;
      }
   
      private static void addAll(String prefix, File folder, List<String> resources) {
         File child = new File(folder, prefix);
         if(!child.exists() || !child.isDirectory()) {
            return;
         }
         
         for(File f: child.listFiles()) {
            if(f.getName().endsWith(".xml")) {
               resources.add(prefix + "/" + f.getName());
            }
         }
      }
   
      private static List<String> loadFromJar(String path) throws IOException {
         if(path.startsWith("file:")) {
            path = path.substring(5);
         }
         List<String> files = new ArrayList<>();
         String [] parts = StringUtils.split(path, '!');
         try(JarFile jf = new JarFile(parts[0])) {
            Enumeration<JarEntry> entries = jf.entries();
            while(entries.hasMoreElements()) {
               String name = entries.nextElement().getName();
               if(isDefinitionFolder(name) && name.endsWith("xml")) {
                  files.add(name);
               }
            }
         }
         return files;
      }
      
      private static boolean isDefinitionFolder(String name) {
         return 
               name.startsWith("capability/") ||
               name.startsWith("service/") ||
               name.startsWith("type/");
      }
   
   }

	private final List<String> capabilityResourceFileNames;
	private final DefinitionRegistry delegate;
	
	@Inject
	public ClasspathDefinitionRegistry(@Named("capability.file.dir") List<String> capabilityFileDir) {
		capabilityResourceFileNames = capabilityFileDir;
		// TODO handle reload?
		this.delegate = load();
	}

	/**
    * @param nameOrNamespace
    * @return
    * @see com.iris.capability.definition.DefinitionRegistry#getCapability(java.lang.String)
    */
   public com.iris.capability.definition.CapabilityDefinition getCapability(
         String nameOrNamespace) {
      return delegate.getCapability(nameOrNamespace);
   }

   /**
    * @return
    * @see com.iris.capability.definition.DefinitionRegistry#getCapabilities()
    */
   public Collection<com.iris.capability.definition.CapabilityDefinition> getCapabilities() {
      return delegate.getCapabilities();
   }

   /**
    * @param nameOrNamespace
    * @return
    * @see com.iris.capability.definition.DefinitionRegistry#getService(java.lang.String)
    */
   public ServiceDefinition getService(String nameOrNamespace) {
      return delegate.getService(nameOrNamespace);
   }

   /**
    * @return
    * @see com.iris.capability.definition.DefinitionRegistry#getServices()
    */
   public Collection<ServiceDefinition> getServices() {
      return delegate.getServices();
   }

   /**
    * @param name
    * @return
    * @see com.iris.capability.definition.DefinitionRegistry#getStruct(java.lang.String)
    */
   public TypeDefinition getStruct(String name) {
      return delegate.getStruct(name);
   }

   /**
    * @return
    * @see com.iris.capability.definition.DefinitionRegistry#getStructs()
    */
   public Collection<TypeDefinition> getStructs() {
      return delegate.getStructs();
   }

   /**
    * @param name
    * @return
    * @see com.iris.capability.definition.DefinitionRegistry#getEvent(java.lang.String)
    */
   public EventDefinition getEvent(String name) {
      return delegate.getEvent(name);
   }

   /**
    * @param name
    * @return
    * @see com.iris.capability.definition.DefinitionRegistry#getAttribute(java.lang.String)
    */
   public com.iris.capability.definition.AttributeDefinition getAttribute(
         String name) {
      return delegate.getAttribute(name);
   }

   protected DefinitionRegistry load() {
   	Map<String, Definition> definitionMap = new HashMap<String, Definition>();
   	definitionMap.put(Capability.NAME, Capability.DEFINITION);
		for (String fileName : capabilityResourceFileNames) {

			// Ignore files that aren't .xml
			if (fileName != null && !fileName.toLowerCase().endsWith(".xml")) {
				continue;
			}

			try(InputStream is = ClasspathDefinitionRegistry.class.getResourceAsStream("/" + fileName)) {
   			if (is != null) {
   			   Definition def = new GenericDefinitionReader().readDefinition(is);
   			   Definition d = definitionMap.get(def.getName());
   			   if(d != null && d instanceof MergeableDefinition) {
                  if(!d.getClass().equals(def.getClass())) {
                     throw new RuntimeException("definitions with the same name " + def.getName() + " but differing types (" + def.getClass() + "/" + d.getClass() + ") cannot be merged.");
                  }
                  d = ((MergeableDefinition)d).merge(def);
                  logger.trace("Loaded: [{}] From: [{}] and merged with another defintion", def.getName(), fileName);
               } else {
                  d = def;
                  logger.trace("Loaded: [{}] From: [{}]", def.getName(), fileName);
               }
               definitionMap.put(def.getName(), d);
					
   			} else {
   				logger.warn("Unable to read: [{}], please ensure the file exists and is readable.", fileName);
   			}
			}
			catch(Exception e) {
			   logger.warn("Unable to parse: [{}]", fileName, e);
			}
		}
		return new BaseDefinitionRegistry(new LinkedList<>(definitionMap.values())) {};
	}

}

