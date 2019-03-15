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
package com.iris.agent.controller.hub;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.backup.BackupService;
import com.iris.agent.exec.ExecService;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.router.Port;
import com.iris.agent.router.PortHandler;
import com.iris.agent.storage.StorageService;
import com.iris.agent.util.RxIris;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubBackupCapability;
import com.iris.protocol.ProtocolMessage;

import rx.Observable;

enum BackupHandler implements PortHandler {
   INSTANCE;

   private static final Logger log = LoggerFactory.getLogger(BackupHandler.class);

   private AtomicBoolean inRestore = new AtomicBoolean();

   void start(Port parent) {
      parent.delegate(this, HubBackupCapability.BackupRequest.NAME, HubBackupCapability.RestoreRequest.NAME);
   }

   @Nullable
   @Override
   public Object recv(Port port, PlatformMessage message) throws Exception {
      String type = message.getMessageType();
      switch (type) {
      case HubBackupCapability.BackupRequest.NAME:
         return handleHubBackup(message);

      case HubBackupCapability.RestoreRequest.NAME:
         return handleHubRestore(message);

      default:
         // ignore
         return null;
      }
  }

   @Override
   public void recv(Port port, ProtocolMessage message) {
   }

   @Override
   public void recv(Port port, Object message) {
   }

   @Nullable
   private Object handleHubBackup(PlatformMessage message) throws Exception {
      MessageBody body = message.getValue();

      String type = HubBackupCapability.BackupRequest.getType(body);
      switch (type) {
      case HubBackupCapability.BackupRequest.TYPE_V2:
         return handleHubBackupV2();

      default:
         throw new RuntimeException("cannot backup hub to type: " + type);
      }
   }

   @Nullable
   private Object handleHubRestore(PlatformMessage message) throws Exception {
      MessageBody body = message.getValue();
      String type = HubBackupCapability.RestoreRequest.getType(body);

      String dataStr = HubBackupCapability.RestoreRequest.getData(body);
      byte[] data = Base64.decodeBase64(dataStr);

      switch (type) {
      case HubBackupCapability.RestoreRequest.TYPE_V1:
         return handleHubRestoreV1(data);

      case HubBackupCapability.RestoreRequest.TYPE_V2:
         return handleHubRestoreV2(data);

      default:
         throw new RuntimeException("cannot restore hub from backup of type: " + type);
      }
   }

   @Nullable
   private Object handleHubBackupV2() throws Exception {
      File output = BackupService.doBackup();
      try (InputStream is = new BufferedInputStream(new FileInputStream(output))) {
         byte[] data = IOUtils.toByteArray(is);
         byte[] cdata = compress(data);

         log.info("hub backup complete: {} bytes ({} uncompressed)", cdata.length, data.length);
         return HubBackupCapability.BackupResponse.builder()
            .withData(Base64.encodeBase64String(cdata))
            .build();
      }
   }

   @Nullable
   private Object handleHubRestoreV1(byte[] compressedData) throws Exception {
      if (!inRestore.compareAndSet(false,true)) {
         throw new RuntimeException("hub is already being restored");
      }

      Throwable failure = null;
      Observable<?> restoreTask = null;

      try {
         byte[] data = decompress(compressedData);
         JsonParser parser = new JsonParser();

         String jsonData = new String(data, StandardCharsets.UTF_8);
         JsonObject json = (JsonObject)parser.parse(jsonData);

         try {
           restoreTask =  BackupService.doMigrateV1(json);
         } catch (Throwable th) {
            failure = th;
         }
      } catch (Throwable ex) {
         failure = ex;
      }

      if (restoreTask == null && failure == null) {
         failure = new RuntimeException("could not create restore task");
      }

      if (restoreTask != null) {
         restoreTask.timeout(10, TimeUnit.MINUTES)
            .observeOn(RxIris.io)
            .subscribeOn(RxIris.io)
            .subscribe(new rx.Subscriber<Object>() {
            @Override public void onNext(@Nullable Object t) { }

            @Override
            public void onError(@Nullable Throwable e) {
               log.info("hub migration failed, restating in 10 seconds: {}", e);
               finish();
            }

            @Override
            public void onCompleted() {
               log.info("hub migration completed successfully, restating in 10 seconds");
               finish();
            }

            private void finish() {
               ExecService.periodic().schedule(new Runnable() {
                  @Override
                  public void run() {
                     HubAttributesService.setLastRestartReason(HubAdvancedCapability.LASTRESTARTREASON_MIGRATION);
                     IrisHal.restart();
                  }
               }, 10, TimeUnit.SECONDS);
            }
         });
      }

      if (failure == null) {
         return HubBackupCapability.RestoreResponse.instance();
      } else {
         throw new RuntimeException(failure);
      }
   }

   @Nullable
   private Object handleHubRestoreV2(byte[] cdata) throws Exception {
      if (!inRestore.compareAndSet(false,true)) {
         throw new RuntimeException("hub is already being restored");
      }

      Throwable failure = null;
      try {
         File path = StorageService.createTempFile("restore", "db");

         byte[] data = decompress(cdata);
         log.info("hub restoring from backup: {} bytes ({} compressed)", data.length, cdata.length);

         try (OutputStream os = new BufferedOutputStream(new FileOutputStream(path))) {
            IOUtils.write(data, os);
         }

         BackupService.doRestore(path);
      } catch (Throwable ex) {
         failure = ex;
      }

      ExecService.periodic().schedule(new Runnable() {
         @Override
         public void run() {
            HubAttributesService.setLastRestartReason(HubAdvancedCapability.LASTRESTARTREASON_BACKUP_RESTORE);
            IrisHal.restart();
         }
      }, 10, TimeUnit.SECONDS);

      if (failure == null) {
         log.info("hub restore completed successfully, restating in 10 seconds");
         return HubBackupCapability.RestoreResponse.instance();
      } else {
         throw new RuntimeException(failure);
      }
   }

   private static byte[] compress(byte[] data) throws Exception {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (GZIPOutputStream os = new GZIPOutputStream(baos)) {
         os.write(data);
      }

      return baos.toByteArray();
   }

   private static byte[] decompress(byte[] data) throws Exception {
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      try (GZIPInputStream is = new GZIPInputStream(bais)) {
         return IOUtils.toByteArray(is);
      }
   }
}

