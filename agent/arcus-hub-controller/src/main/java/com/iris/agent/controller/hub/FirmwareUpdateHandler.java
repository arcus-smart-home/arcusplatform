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

import java.io.File;
import java.net.URI;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.attributes.HubAttributesService;
import com.iris.agent.exec.ExecService;
import com.iris.agent.fourg.FourgService;
import com.iris.agent.hal.IrisHal;
import com.iris.agent.hal.LEDState;
import com.iris.agent.hal.SounderMode;
import com.iris.agent.http.HttpService;
import com.iris.agent.router.Port;
import com.iris.agent.router.PortHandler;
import com.iris.agent.util.Progress;
import com.iris.agent.util.ProgressMonitor;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.HubAdvancedCapability;
import com.iris.messages.capability.HubAdvancedCapability.FirmwareUpdateRequest;
import com.iris.protocol.ProtocolMessage;

enum FirmwareUpdateHandler implements PortHandler {
   INSTANCE;

   private static final Logger log = LoggerFactory.getLogger(FirmwareUpdateHandler.class);

   public enum State {
      IDLE,
      STARTED,
      DOWNLOADING,
      INSTALLING,
      SUCCESS,
   }

   @Nullable
   private Port port = null;
   private State state = State.IDLE;

   void start(Port parent) {
      this.port = parent.delegate(this, HubAdvancedCapability.FirmwareUpdateRequest.NAME);
   }

   @Nullable
   @Override
   public Object recv(Port port, PlatformMessage message) throws Exception {
      if (state != State.IDLE) {
         throw new Exception("currently installing another firmware");
      }

      state = State.STARTED;
      ExecService.io().submit(new UpdateProcess(message));

      return HubAdvancedCapability.FirmwareUpdateResponse.builder()
         .withStatus(HubAdvancedCapability.FirmwareUpdateResponse.STATUS_OK)
         .build();
   }

   @Override
   public void recv(Port port, ProtocolMessage message) {
   }

   @Override
   public void recv(Port port, Object message) {
      // NOTE: This callback is invoked with the UpdateProcess when
      //       that process has completed (either successfully or
      //       because of an error).
      //
      //       If the process completed successfully then most
      //       likely the system rebooted and we never reached
      //       here, we handle that case anyways just in case
      //       a given platform does not reboot.
      UpdateProcess process = (UpdateProcess)message;
      log.debug("firmware updater process finished: {}", process);

      Throwable error = process.getFailure();
      if (error != null) {
         log.debug("firmware updater sending error: {}", error.getMessage());
         port.error(process.getRequest(), error);
      } else {
         MessageBody msg = createFirmwareProgressMessage(String.format("%s", State.SUCCESS));
         log.debug("firmware updater sending: {}", msg);

         port.send(process.getRequest().getSource(), msg);
      }

      state = State.IDLE;
   }

   private static MessageBody createFirmwareProgressMessage(String status) {
      return createFirmwareProgressMessage(status, 0.0);
   }

   private static MessageBody createFirmwareProgressMessage(String status, double percent) {
      return HubAdvancedCapability.FirmwareUpgradeProcessEvent.builder()
         .withStatus(status)
         .withPercentDone(percent)
         .build();
   }

   private final class UpdateProcess implements Runnable, ProgressMonitor<Long> {
      private final PlatformMessage message;
      private final MessageBody body;
      private final String priority;
      private final String type;
      private final URI uri;
      private int transmitDecaPercent = -1;
      private boolean force = false;

      @Nullable
      private Throwable cause;

      public UpdateProcess(PlatformMessage message) {
         this.message = message;
         this.body = message.getValue();
         this.uri = URI.create(HubAdvancedCapability.FirmwareUpdateRequest.getUrl(body));
         this.type = HubAdvancedCapability.FirmwareUpdateRequest.getType(body);

         String pri = HubAdvancedCapability.FirmwareUpdateRequest.getPriority(body);
         this.priority = (pri != null) ? pri : FirmwareUpdateRequest.PRIORITY_NORMAL;
         this.force = this.priority.equals(HubAdvancedCapability.FirmwareUpdateRequest.PRIORITY_URGENT);
      }

      @Override
      public void onProgressChange(Progress<? extends Long> progress, Long update) {
         // NOTE: This method is invoked during downloading to inform us of the
         //       progress only if the content length is known during downloading.
         Long current = progress.progress();
         Long complete = progress.complete();

         double percent = ((double)current) / ((double)complete);
         update(percent);
      }

      @Override
      public void onProgressComplete(Progress<? extends Long> progress) {
         // NOTE: This method is invoked when downloading is complete.
      }

      @Override
      public void onProgressFailed(Progress<? extends Long> progress) {
         // NOTE: This method is invoked when downloading fails.
      }

      public PlatformMessage getRequest() {
         return message;
      }

      @Nullable
      public Throwable getFailure() {
         return cause;
      }

      private void update(double percent) {
         int newDecaPercent = (int)(percent * 10);
         if (newDecaPercent > transmitDecaPercent) {
            transmitDecaPercent = newDecaPercent;

            MessageBody msg = createFirmwareProgressMessage(String.format("%s", state), percent);
            log.debug("firmware updater sending: {}", msg);

            if (port != null) {
            	port.send(message.getSource(), msg);
            }
         }
      }

      @Override
      public void run() {
         File downloaded = null;

         try {
        	 if (priority.equals(HubAdvancedCapability.FirmwareUpdateRequest.PRIORITY_BELOW_MINIMUM)) {
        		 IrisHal.setLedState(LEDState.UPGRADE_ROOTFS);
        		 IrisHal.setSounderMode(SounderMode.FIRMWARE_UPDATE_NEEDED);
        	 }
        	 
            // If we are using the backup connection, only allow urgent updates
            if (FourgService.isAuthorized() && !priority.equals(HubAdvancedCapability.FirmwareUpdateRequest.PRIORITY_URGENT)) {
               log.info("skipping normal priority firmware update while using backup connection.");
               cause = new Throwable("Non-urgent install while using backup connection");
            } else {
               state = State.DOWNLOADING;
               update(0.0);

               // TODO: These credentials need to be passed into the hub or the
               //       hub needs to use its client certificate.
               log.info("downloading new firmware from: {}", uri);
               downloaded = HttpService.download(uri, "firmware", ".bin", IrisHal.getFirmwareDownloadDir(type), this);

               log.info("installing new firmware downloaded to: {}", downloaded);
               state = State.INSTALLING;
               update(0.0);

               HubAttributesService.setLastRestartReason(HubAdvancedCapability.LASTRESTARTREASON_FIRMWARE_UPDATE);
               IrisHal.installHubFirmware(downloaded,type,force);
            }
         } catch (Throwable ex) {
            log.warn("exception during downloading: {}", ex.getMessage(), ex);
            cause = ex;

            if (downloaded != null) {
               if (!downloaded.delete()) {
                  log.warn("failed to delete downloaded file");
               }
            }
         } finally {
            state = State.IDLE;
         }

         if (port != null) {
            port.queue(this);
         }
      }
   }
}

