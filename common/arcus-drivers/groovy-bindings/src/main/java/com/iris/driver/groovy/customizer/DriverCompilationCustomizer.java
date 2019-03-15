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
package com.iris.driver.groovy.customizer;

import java.io.InputStream;
import java.lang.reflect.Modifier;

import org.apache.commons.io.input.ReaderInputStream;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.iris.Utils;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.device.model.CapabilityDefinition;
import com.iris.driver.groovy.context.GroovyCapabilityDefinition;
import com.iris.driver.groovy.context.GroovyCapabilityDefinitionFactory;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;

/**
 * Adds capability definitions to the compilation context.
 *
 */
public class DriverCompilationCustomizer extends CompilationCustomizer {
   private static final Logger LOGGER = LoggerFactory.getLogger(DriverCompilationCustomizer.class);

   private final CapabilityRegistry capabilityRegistry;
   
   @Inject
   public DriverCompilationCustomizer(CapabilityRegistry registry) {
      // copied from ImportCustomizer, not sure what the best phase is,
      // but this should be early enough to add imports, etc
      super(CompilePhase.CONVERSION);
      this.capabilityRegistry = registry;
   }
   
   /* (non-Javadoc)
    * @see org.codehaus.groovy.control.CompilationUnit.PrimaryClassNodeOperation#call(org.codehaus.groovy.control.SourceUnit, org.codehaus.groovy.classgen.GeneratorContext, org.codehaus.groovy.ast.ClassNode)
    */
   @Override
   public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
      LOGGER.trace("Customize [phase: {} {}, classNode: {}]", source.getPhase(), source.getPhaseDescription(), classNode);
      if(classNode.getField("_HASH") == null) {
         String hash = hash(source);
         if(hash != null) {
            classNode.addField(
                  "_HASH", 
                  Modifier.PUBLIC | Modifier.FINAL, 
                  new ClassNode(String.class), 
                  new ConstantExpression(hash)
            );
         }
      }
      
      ClassNode groovyCapabilityDefinition = new ClassNode(GroovyCapabilityDefinition.class);
      for(CapabilityDefinition definition: capabilityRegistry.listCapabilityDefinitions()) {
         if(classNode.getProperty(definition.getCapabilityName()) != null) {
            continue;
         }
         
         if(!isDeviceCapability(definition)) {
            continue;
         }
         
         String fieldName = definition.getNamespace();
         FieldNode field = classNode.addField(
               fieldName,
               Modifier.PRIVATE | Modifier.FINAL, 
               groovyCapabilityDefinition,
               new StaticMethodCallExpression(
                     new ClassNode(GroovyCapabilityDefinitionFactory.class),
                     "create",
                     new TupleExpression(
                           new ConstantExpression(definition.getCapabilityName()),
                           VariableExpression.THIS_EXPRESSION
                     )
               )
         );
         
         
         classNode.addProperty(
               definition.getCapabilityName(),
               Modifier.PUBLIC | Modifier.FINAL,
               groovyCapabilityDefinition,
               new FieldExpression(field),
               new ReturnStatement(new FieldExpression(field)),
               null
          );
      }
   }
   
   private boolean isDeviceCapability(CapabilityDefinition capability) {
      if(Capability.NAME.equals(capability.getCapabilityName())) {
         return true;
      }
      if(DeviceCapability.NAME.equals(capability.getCapabilityName())) {
         return true;
      }
      if(DeviceCapability.NAME.equals(capability.getEnhances())) {
         return true;
      }
      return false;
   }

   private String hash(SourceUnit source) {
      try(InputStream is = new ReaderInputStream(source.getSource().getReader())) {
         return Utils.shortHash(is);
      }
      catch(Exception e) {
         LOGGER.warn("Error hashing {}", source.getName(), e);
         return null;
      }
   }

}

