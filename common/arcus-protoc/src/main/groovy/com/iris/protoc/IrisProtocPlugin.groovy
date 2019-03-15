package com.iris.protoc

import org.gradle.api.Plugin
import org.gradle.api.Project

class IrisProtocPlugin implements Plugin<Project> {
   void apply(Project project) {
      // This adds the configuration block for the Iris Protoc
      // compiler extension. The configuration block is called
      // irispc and the configurable properties are defined
      // in IrisProtocPluginExtension.
      project.extensions.create("irispc", IrisProtocPluginExtension)
      
      // Add a task that uses the configuration
          project.task('hello') {
                 doLast {
            println project.greeting.message
         }
      }

   }
}
