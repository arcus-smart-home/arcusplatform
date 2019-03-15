package com.iris.driver.reflex.generator

import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import org.gradle.api.file.FileCollection

class ReflexGeneratorTask extends JavaExec {
   def args = []
   def source
   def destination
   def version

   public ReflexGeneratorTask() {
      setMain "com.iris.driver.reflex.generator.ReflexGenerator"
   }

   FileCollection getSource() {
      if (source == null) {
          return project.files([])
      }

      project.files(source)
   }

   File getDestination() {
      if (destination == null) {
         return project.file("$project.buildDir")
      }

      project.file(destination)
   }

   @Override
   JavaExec setArgs(Iterable<?> args) {
      this.args = args
   }

   @Override
   List<String> getArgs() {
      return args + ["--output", destination, "--version", version] + source
   }
}
