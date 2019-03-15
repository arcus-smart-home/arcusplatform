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
package com.iris.driver.groovy;

import groovy.util.GroovyScriptEngine;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.groovy.classgen.asm.BytecodeDumper;
import org.codehaus.groovy.control.BytecodeProcessor;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bootstrap.guice.AbstractIrisModule;
import com.iris.driver.groovy.customizer.DriverCompilationCustomizer;
import com.iris.driver.groovy.pin.PinManagementPlugin;
import com.iris.driver.groovy.plugin.GroovyDriverPlugin;
import com.iris.driver.groovy.scheduler.SchedulerPlugin;

// TODO expose more compilation configuration options
public class GroovyDriverModule extends AbstractIrisModule {
   private static final Logger log = LoggerFactory.getLogger(GroovyDriverModule.class);
   public static final String NAME_GROOVY_DRIVER_DIRECTORIES = "groovyDriverURLs";

   @Inject(optional = true) @Named("groovy.invoke.dynamic")
   private boolean groovyInvokeDynamic = false;

   @Inject(optional = true) @Named("groovy.recompile")
   private boolean groovyRecompileScript = false;

   @Inject(optional = true) @Named("groovy.debug")
   private boolean groovyDebug = false;

   @Inject(optional = true) @Named("groovy.verbose")
   private boolean groovyVerbose = false;

   @Inject(optional = true) @Named("groovy.dump.bytecode")
   private boolean groovyDumpByteCode = false;

   @Inject(optional = true) @Named("groovy.dump.bytecode.path")
   private String groovyDumpByteCodePath = "";

   @Override
   protected void configure() {
      bind(GroovyDriverFactory.class);
      bindSetOf(CompilationCustomizer.class)
         .addBinding()
         .toInstance(new ImportCustomizer()
            .addImports("groovy.transform.Field")
            .addStaticStars("java.util.concurrent.TimeUnit")
         );
      bindSetOf(CompilationCustomizer.class)
         .addBinding()
         .to(DriverCompilationCustomizer.class);
      bindSetOf(GroovyDriverPlugin.class)
         .addBinding()
         .to(SchedulerPlugin.class);
      bindSetOf(GroovyDriverPlugin.class)
         .addBinding()
         .to(PinManagementPlugin.class);
   }

   @Provides
   @Singleton
   public GroovyScriptEngine scriptEngine(
         @Named("groovyDriverURLs") Set<URL> groovyDriverUrls,
         Set<CompilationCustomizer> customizers
   ) {
      System.setProperty("groovy.target.indy", String.valueOf(groovyInvokeDynamic));
      System.setProperty("groovy.output.verbose", String.valueOf(groovyVerbose));
      System.setProperty("groovy.output.debug", String.valueOf(groovyDebug));
      System.setProperty("groovy.recompile", String.valueOf(groovyRecompileScript));

      GroovyScriptEngine engine = new GroovyScriptEngine(groovyDriverUrls.toArray(new URL[groovyDriverUrls.size()]));
      for(CompilationCustomizer customizer: customizers) {
         engine.getConfig().addCompilationCustomizers(customizer);
      }

      if (groovyDumpByteCode && groovyDumpByteCodePath != null && !groovyDumpByteCodePath.isEmpty()) {
         final File base = new File(groovyDumpByteCodePath);
         try {
            FileUtils.deleteDirectory(base);
         } catch (Exception ex) {
            log.warn("could not clean classes directory", ex);
         }
         base.mkdirs();

         engine.getConfig().setBytecodePostprocessor(new BytecodeProcessor() {
            @Override
            public byte[] processBytecode(String name, byte[] original) {
               try (OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(base,name+".class")))) {
                  out.write(original);
               } catch (Exception ex) {
                  log.warn("failed to output class file", ex);
               }

               return original;
            }
         });
      }

      engine.getConfig().setScriptExtensions(ImmutableSet.of("driver", "capability", "groovy"));
      return engine;
   }
}

