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
package com.iris.agent.test;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;

import com.google.common.io.Files;
import com.google.inject.Module;
import com.iris.agent.boot.BootUtils;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.hal.simulated.IrisHalSimulated;
import com.iris.agent.storage.StorageService;
import com.iris.bootstrap.ServiceLocator;

// TODO: this test harness is not thread safe.
public abstract class AbstractSystemTestCase {
   private static final AtomicReference<File> BASE_PATH = new AtomicReference<>();

   public static void startIrisSystem(Collection<? extends Class<? extends Module>> extra) throws Exception {
      File basePath = BASE_PATH.get();
      if (basePath != null) {
         throw new IllegalStateException("iris test harness already started: " + basePath);
      }

      basePath = Files.createTempDir();
      if (!BASE_PATH.compareAndSet(null, basePath)) {
         FileUtils.deleteDirectory(basePath);
         throw new IllegalStateException("iris test harness startup race condition");
      }

      System.out.println("Starting up simulated iris hal: " + basePath + "...");
      FileUtils.deleteDirectory(basePath);

      BootUtils.addExtraModules(extra);
      BootUtils.initialize(new IrisHalTest(), basePath, Collections.<String>emptyList());
   }

   public static void shutdownIrisSystem() throws Exception {
      File basePath = BASE_PATH.getAndSet(null);

      System.out.println("Shutting down simulated iris hal: " + basePath + "...");
      IrisHal.shutdown();

      if (basePath != null) {
         FileUtils.deleteDirectory(basePath);
      }

      BootUtils.clearExtraModules();
      ServiceLocator.destroy();
   }

   protected static final class IrisHalTest extends IrisHalSimulated {
      @Override
      protected void startStorageService(File base) {
         String basePath = base.getPath();

         StorageService.start(200, TimeUnit.MILLISECONDS);
         StorageService.addRootMapping("agent://", "file://" + base.getAbsolutePath());
         StorageService.addRootMapping("tmp://", "file://" + basePath + "/tmp");
         StorageService.addRootMapping("file://", "file://" + basePath + "/data");
         StorageService.addRootMapping("db://", "file://" + basePath + "/db");
      }
   }

   /*
   protected static enum Empty {
      INSTANCE;
   }

   protected static enum EmptySerializer implements Serializer<Empty> {
      INSTANCE;

      @Override
      public byte[] serialize(@Nullable Empty value) throws IllegalArgumentException {
         return new byte[] { (byte)Empty.INSTANCE.ordinal() };
      }

      @Override
      public void serialize(@Nullable Empty value, @Nullable OutputStream out) throws IOException, IllegalArgumentException {
         if (out != null && value != null) {
            out.write(Empty.INSTANCE.ordinal());
         }
      }
   }

   protected static enum EmptyDeserializer implements Deserializer<Empty> {
      INSTANCE;

      @Override
      public Empty deserialize(@Nullable byte[] input) throws IllegalArgumentException {
         if (input != null && input.length == 1 && input[0] == Empty.INSTANCE.ordinal()) {
            return Empty.INSTANCE;
         }

         throw new IllegalArgumentException("bad input");
      }

      @Override
      public Empty deserialize(@Nullable InputStream input) throws IOException, IllegalArgumentException {
         if (input != null && input.read() == Empty.INSTANCE.ordinal()) {
            return Empty.INSTANCE;
         }

         throw new IllegalArgumentException("bad input");
      }
   }
   */

   /*
   protected static enum EmptyProtocol implements Protocol<Empty> {
      INSTANCE;

      @Override
      public String getName() {
         return "EMPT";
      }

      @Override
      public Serializer<Empty> createSerializer() {
         return EmptySerializer.INSTANCE;
      }

      @Override
      public Deserializer<Empty> createDeserializer() {
         return EmptyDeserializer.INSTANCE;
      }

      @Override
      @SuppressWarnings("null")
      public ProtocolDefinition getDefinition() {
         return null;
      }

      @Override
      public String getNamespace() {
         return "EMPT";
      }
   }
   */
}

