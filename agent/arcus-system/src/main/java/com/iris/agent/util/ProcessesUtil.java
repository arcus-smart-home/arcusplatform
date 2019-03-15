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
package com.iris.agent.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessesUtil {
   private static final Logger           log                  = LoggerFactory.getLogger(ProcessesUtil.class);

   public static boolean isRunning(String processName) {
      log.trace("isRunning {}", processName);

      final String program = "ps";
      Process pr = null;
      List<String> procs;

      try {
         pr = Runtime.getRuntime().exec(program);
         pr.getErrorStream().close();
         pr.getOutputStream().close();

         procs = IOUtils.readLines(pr.getInputStream(), StandardCharsets.UTF_8);
         if (procs.size() > 1) {
            for ( String proc : procs ) {
               String[] line = proc.trim().split("\\s+");

               if (line[4].startsWith(processName) ) {
                  return true;
               }
            }
         }
      } catch (IOException e) {
         log.warn("cannot execute '{}' because:", program, e);
      } finally {
         if (pr != null) {
            pr.destroy();
         }
      }

      return false;
   }

   public static List<String> getRunning() {
      final String program = "ps";
      Process pr = null;

      try {
         pr = Runtime.getRuntime().exec(program);
         pr.getErrorStream().close();
         pr.getOutputStream().close();

         return IOUtils.readLines(pr.getInputStream(), StandardCharsets.UTF_8);
      } catch (IOException e) {
         log.warn("cannot execute '{}' because:", program, e);
      } finally {
         if (pr != null) {
            pr.destroy();
         }
      }

      return Collections.emptyList();

   }

   // Test Main Ignore.
   public static void main(String...args) {
      List<String> procs = ProcessesUtil.getRunning();
      for (String proc : procs ) {
         System.out.println(proc);
      }
      System.out.println(ProcessesUtil.isRunning("/usr/sbin/dropbear"));
   }

}

