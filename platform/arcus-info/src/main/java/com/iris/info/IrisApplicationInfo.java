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
package com.iris.info;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jdt.annotation.Nullable;

public class IrisApplicationInfo {
   private static final Info instance;
   
   static {
      Info info = new Info();
      info.setApplicationName(queryServiceName());
      info.setApplicationVersion(queryServiceVersion());
      info.setApplicationDirectory("");
      info.setContainerName(queryContainerName());
      info.setHostName(queryHostName());
      
      instance = info;
   }
   
   
   /**
    * @return
    */
   public static String getApplicationName() {
      return instance.getApplicationName();
   }
   
   public static void setApplicationName(String applicationName) {
      instance.setApplicationName(applicationName);
   }


   /**
    * @return
    */
   public static String getApplicationVersion() {
      return instance.getApplicationVersion();
   }

   public static void setApplicationVersion(String applicationVersion){
      instance.setApplicationVersion(applicationVersion);
   }

   /**
    * @return
    */
   public static String getApplicationDirectory() {
      return instance.getApplicationDirectory();
   }


   /**
    * @param applicationDirectory
    */
   public static void setApplicationDirectory(String applicationDirectory) {
      instance.setApplicationDirectory(applicationDirectory);
   }


   /**
    * @return
    */
   public static String getHostName() {
      return instance.getHostName();
   }


   /**
    * @param hostName
    */
   public static void setHostName(String hostName) {
      instance.setHostName(hostName);
   }


   /**
    * @return
    */
   public static String getContainerName() {
      return instance.getContainerName();
   }


   /**
    * @param containerName
    */
   public static void setContainerName(String containerName) {
      instance.setContainerName(containerName);
   }

   private static String queryServiceName() {
      String svc = System.getenv("IRIS_SERVICE_NAME");
      if (svc != null && !svc.trim().isEmpty()) {
         return svc;
      }

      return "unknown";
   }

   private static String queryServiceVersion() {
      String svc = System.getenv("IRIS_SERVICE_VERSION");
      if (svc != null && !svc.trim().isEmpty()) {
         return svc;
      }

      return "unknown";
   }

   @Nullable
   private static String queryContainerName() {
      if (System.getenv("MARATHON_APP_ID") == null || System.getenv("HOST") == null) {
         return null;
      }

      return rawHostName();
   }

   private static String queryHostName() {
      String longHost;
      if (System.getenv("MARATHON_APP_ID") == null || System.getenv("HOST") == null) {
         longHost = rawHostName();
      } else {
         longHost = System.getenv("HOST");
      }

      int idx = longHost.indexOf('.');
      return (idx <= 0) ?  longHost : longHost.substring(0,idx);
   }

   private static String rawHostName() {
      try {
         String OS = System.getProperty("os.name").toLowerCase();

         if (OS.indexOf("win") >= 0) {
            String host = System.getenv("COMPUTERNAME");
            if (host != null && !host.trim().isEmpty()) {
               return host;
            }

            host = runHostCommand("hostname");
            if (host != null && !host.trim().isEmpty()) {
               return host;
            }
         } else {
            String host = System.getenv("HOSTNAME");
            if (host != null && !host.trim().isEmpty()) {
               return host;
            }

            try {
               host = runHostCommand("hostname");
               if (host != null && !host.trim().isEmpty()) {
                  return host;
               }
            } catch (Exception ex) {
               // ignore
            }

            host = runHostCommand("cat /etc/hostname");
            if (host != null && !host.trim().isEmpty()) {
               return host;
            }
         }
      } catch (Exception ex) {
         return "unknown";
      }

      return "unknown";
   }

   private static String runHostCommand(String cmd) throws IOException {
      Process proc = Runtime.getRuntime().exec(cmd);
      try (InputStream inp = proc.getInputStream();
           ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
         byte[] buffer = new byte[1024];
         int read = inp.read(buffer);
         while (read > 0) {
            for (int i = 0; i < read; ++i) {
               if (buffer[i] == '\n') {
                  break;
               }

               baos.write(buffer[i]);
            }

            read = inp.read(buffer);
         }

         return baos.toString();
      }
   }

   private static class Info {
      private volatile String applicationName;
      private volatile String applicationVersion;
      private volatile String applicationDirectory;
      private volatile String hostName;
      private volatile String containerName;
      /**
       * @return the applicationName
       */
      public String getApplicationName() {
         return applicationName;
      }
      /**
       * @param applicationName the applicationName to set
       */
      public void setApplicationName(String applicationName) {
         this.applicationName = applicationName;
      }
      /**
       * @return the applicationVersion
       */
      public String getApplicationVersion() {
         return applicationVersion;
      }
      /**
       * @param applicationVersion the applicationVersion to set
       */
      public void setApplicationVersion(String applicationVersion) {
         this.applicationVersion = applicationVersion;
      }
      /**
       * @return the applicationDirectory
       */
      public String getApplicationDirectory() {
         return applicationDirectory;
      }
      /**
       * @param applicationDirectory the applicationDirectory to set
       */
      public void setApplicationDirectory(String applicationDirectory) {
         this.applicationDirectory = applicationDirectory;
      }
      /**
       * @return the hostName
       */
      public String getHostName() {
         return hostName;
      }
      /**
       * @param hostName the hostName to set
       */
      public void setHostName(String hostName) {
         this.hostName = hostName;
      }
      /**
       * @return the containerName
       */
      public String getContainerName() {
         return containerName;
      }
      /**
       * @param containerName the containerName to set
       */
      public void setContainerName(String containerName) {
         this.containerName = containerName;
      }

   }
}

