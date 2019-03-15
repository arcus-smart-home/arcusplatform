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
package com.iris.oculus.modules.hub;

import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.zip.GZIPInputStream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.EmptyEvent;
import com.iris.client.IrisClient;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Capability.AddedEvent;
import com.iris.client.capability.Hub;
import com.iris.client.capability.Place;
import com.iris.client.capability.Place.GetHubRequest;
import com.iris.client.capability.Place.GetHubResponse;
import com.iris.client.capability.Place.RegisterHubRequest;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.exception.ErrorResponseException;
import com.iris.client.message.ClientErrorCodes.HubRegistration;
import com.iris.client.model.HubModel;
import com.iris.client.model.PlaceModel;
import com.iris.client.util.Result;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.hub.dialog.HubIdPrompt;
import com.iris.oculus.modules.hub.dialog.HubRegistrationDialog;
import com.iris.oculus.modules.hub.dialog.HubRegistrationV2Dialog;
import com.iris.oculus.modules.place.PlaceController;
import com.iris.oculus.modules.session.OculusSession;
import com.iris.oculus.modules.session.SessionAwareController;
import com.iris.oculus.modules.session.SessionController;
import com.iris.oculus.util.Actions;
import com.iris.oculus.util.DefaultSelectionModel;
import com.iris.oculus.util.SelectionModel;
import com.iris.oculus.view.ViewModel;
import com.iris.oculus.widget.ProgressDialog;
import com.iris.oculus.widget.ProgressDialog.Progress;

/**
 *
 */
@Singleton
public class HubController extends SessionAwareController {
   private static final Logger log =
      LoggerFactory.getLogger(HubController.class);

   private IrisClient        client;

   private int registrationPollingIntervalMs = 5000;
   private int registrationTimeoutMs         = 10000;
   private int pairingTimeoutMs              = 300000;

   private Action registerHub       = Actions.build("Register Hub...", this::promptForHubId); 
   private Action registerHubLegacy = Actions.build("Register Hub (Deprecated)...", this::promptForHubIdLegacy); 
   private Action reloadHub         = Actions.build("Refresh Hub", this::reloadHub);
   private Action decodeHubInfo     = Actions.build("Decode Hub Info", this::decodeHubInfo);

   private DefaultSelectionModel<HubModel> hubSelection = new DefaultSelectionModel<>();


   @Inject
   public HubController(IrisClient client) {
      this.client = client;
   }

   public Action actionRegisterHub() {
      return registerHub;
   }

   public Action actionRegisterHubLegacy() {
      return registerHubLegacy;
   }

   public Action actionReloadHubs() {
      return reloadHub;
   }

   public Action actionDecodeHubInfo() {
      return decodeHubInfo;
   }

   public SelectionModel<HubModel> getHubSelection() {
      return hubSelection;
   }

   public List<Action> createHubActions(HubModel model) {
      return ImmutableList.of();
      //return Arrays.<Action> asList(new PairDevicesAction(model), new UnpairDevicesAction(model));
   }

   public ClientFuture<ClientEvent> startUnpairing(String hubAddress) {
      Progress p = ProgressDialog.builder().indeterminate().withTitle("Waiting to Un-Pair Devices...").withMessage("Hub is in un-pairing mode, please un-pair any devices now").withCancelEnable(true).show().onCompletion((event) -> stopUnpairing(hubAddress));
      ClientRequest request = new ClientRequest();
      request.setCommand(Hub.UnpairingRequestRequest.NAME);
      request.setAddress(hubAddress);
      request.setAttribute("actionType", "START_UNPAIRING");
      request.setAttribute("timeout", pairingTimeoutMs);
      request.setAttribute("force", false);
      return client.request(request).onFailure((error) -> Oculus.showError("Unable to enter Un-Pairing Mode", error));
   }

   public ClientFuture<ClientEvent> stopUnpairing(String hubAddress) {
      ClientRequest request = new ClientRequest();
      request.setCommand(Hub.UnpairingRequestRequest.NAME);
      request.setAddress(hubAddress);
      request.setAttribute("actionType", "STOP_UNPAIRING");
      return client.request(request).onFailure((error) -> Oculus.showError("Unable to exit Pairing Mode", error));
   }

   public ClientFuture<ClientEvent> getLogs(HubModel hub) {
      ClientRequest request = new ClientRequest();
      request.setCommand(Hub.GetLogsRequest.NAME);
      request.setAddress(hub.getAddress());
      return client.request(request)
         .onFailure((error) -> Oculus.showError("Unable to retreive Hub logs", error))
         .onSuccess((event) -> showLogs((String)event.getAttribute(Hub.GetLogsResponse.ATTR_LOGS)));
   }

   public ClientFuture<ClientEvent> delete(HubModel hub) {
      ClientRequest request = new ClientRequest();
      request.setCommand(Hub.DeleteRequest.NAME);
      request.setAddress(hub.getAddress());
      return client.request(request).onFailure((error) -> Oculus.showError("Unable to delete hub",  error));
   }

   private void showLogs(String base64) {
      byte[] gzipped = Base64.getDecoder().decode(base64);

      List<String> logs;
      ByteArrayInputStream bais = new ByteArrayInputStream(gzipped);
      try (GZIPInputStream is = new GZIPInputStream(bais)) {
         logs = IOUtils.readLines(is);
      } catch (Exception ex) {
         Oculus.showError("Hub Logs in Incorrect Format", ex);
         return;
      }

      String formatted = null;
      try {
         SimpleDateFormat dateFormat = new SimpleDateFormat("MMdd HH:mm:ss.SSS");
         StringBuilder formattedLogs = new StringBuilder(512*1024);
         for (String line : logs) {
            JsonObject elem = new JsonParser().parse(line).getAsJsonObject();

            long ts = elem.get("ts").getAsLong();
            String lvl = elem.get("lvl").getAsString();
            String thd = elem.get("thd").getAsString();
            String log = elem.get("log").getAsString();
            String msg = elem.get("msg").getAsString();

            // H    Hour in day (0-23)       Number    0
            // k    Hour in day (1-24)       Number    24
            // K    Hour in am/pm (0-11)     Number    0
            // h    Hour in am/pm (1-12)     Number    12
            // m    Minute in hour           Number    30
            // s    Second in minute         Number    55
            // S    Millisecond              Number    978

            formattedLogs.append(lvl.charAt(0));
            formattedLogs.append(dateFormat.format(new Date(ts))).append(" ");
            formattedLogs.append(thd).append(" ");
            formattedLogs.append(log).append("] ");
            formattedLogs.append(msg).append("\n");

            if (elem.get("exc") != null && !elem.get("exc").isJsonNull()) {
               String exc = elem.get("exc").getAsString();
               formattedLogs.append(exc);
            }
         }

         formatted = formattedLogs.toString();
      } catch (Exception ex) {
         log.info("failed to format logs, displaying as JSON");
         Oculus.showDialog("Hub Logs", StringUtils.join(logs,"\n"), JOptionPane.PLAIN_MESSAGE);
      }

      Oculus.showDialog("Hub Logs", formatted, JOptionPane.PLAIN_MESSAGE);
   }

   public ClientFuture<ClientEvent> getConfig(HubModel hub) {
      ClientRequest request = new ClientRequest();
      request.setCommand(Hub.GetConfigRequest.NAME);
      request.setAddress(hub.getAddress());
      return client.request(request)
         .onFailure((error) -> Oculus.showError("Unable to retreive Hub config", error))
         .onSuccess((event) -> showConfig((Map<String,String>)event.getAttribute(Hub.GetConfigResponse.ATTR_CONFIG)));
   }

   private void showConfig(Map<String,String> config) {
      TreeMap<String,String> sort = new TreeMap<>();
      sort.putAll(config);

      StringBuilder bld = new StringBuilder();
      for (Map.Entry<String,String> entry : sort.entrySet()) {
         bld.append(entry.getKey())
            .append( " => ")
            .append(entry.getValue())
            .append("\n");
      }

      Oculus.showDialog("Hub Config", bld.toString(), JOptionPane.PLAIN_MESSAGE);
   }

   protected void promptForHubId() {
      try {
         PlaceModel place = ServiceLocator.getInstance(PlaceController.class).getActivePlace().getSelectedItem().get();
         HubRegistrationV2Dialog.prompt(place);
      }
      catch(Exception e) {
         Oculus.showError("Unable to Determine the Active Place (#reasons)", e);
      }
   }

   protected void promptForHubIdLegacy() {
      HubIdPrompt
         .prompt()
         .onSuccess((hubId) -> registerHub(hubId))
          // failure is a cancellation, ignore it
         ;
   }

   protected void registerHub(String hubId) {
      new HubRegistrationProcess(hubId).register().onSuccess((event) -> {
            HubModel hub = (HubModel) IrisClientFactory.getModelCache().addOrUpdate(event.getAttributes());
            Oculus.info("Successfully registered hub [" + hub.getId() + "]");
            hubSelection.setSelection(hub);
      });
   }

   protected ClientFuture<ClientEvent> sendRegisterRequest(OculusSession info, String hubId) {
      RegisterHubRequest request = new RegisterHubRequest();
      request.setAddress("SERV:" + Place.NAMESPACE + ":" + info.getPlaceId().toString());
      request.setHubId(hubId);
      request.setTimeoutMs(registrationTimeoutMs);
      return client.request(request);
   }

   public void reloadHub() {
      String placeId = getPlaceId();
      if (placeId == null) {
         Oculus.warn("Can't load hub, no place is currently selected");
         return;
      }

      GetHubRequest request = new GetHubRequest();
      request.setAddress("SERV:" + Place.NAMESPACE + ":" + placeId);
      client.request(request)
         .onFailure((error) -> Oculus.warn("Unable to load hub", error))
         .onSuccess((event) -> {
            GetHubResponse response = new GetHubResponse(event);
            HubModel hub = (HubModel) IrisClientFactory.getModelCache().addOrUpdate(response.getHub());
            hubSelection.setSelection(hub);
         });
   }

   public void decodeHubInfo() {
      String infoInput = Oculus.showInputDialog("Decode Manufacturer Info","Enter Manufacturer Info:", JOptionPane.PLAIN_MESSAGE);
      if (infoInput == null) {
         return;
      }

      try {
         long info = Long.valueOf(infoInput);
         long tester = info & 0x3FF;
         long station = (info >> 10) & 0x7F;
         long factory = (info >> 17) & 0x1F;
         long year = (info >> 22) & 0x1F;
         long day = (info >> 27) & 0x1F;
         long month = (info >> 32) & 0xF;
         long hwver = (info >> 36) & 0x7;
         long model = (info >> 39) & 0x7;

         String modelName;
         switch ((int)model) {
         case 0: modelName = "HUB520"; break;
         case 1: modelName = "IH200"; break;
         default: modelName = "UNKNOWN"; break;
         }

         StringBuilder bld = new StringBuilder();
         bld.append("Manufacturer Info:\n")
            .append("Model: ").append(modelName).append(" (").append(model).append(")\n")
            .append("HW Version: ").append(hwver).append("\n")
            .append("Date: ").append(month).append("/").append(day).append("/").append(2000+year).append("\n")
            .append("Factory: ").append(factory).append("\n")
            .append("Station: ").append(station).append("\n")
            .append("Tester: ").append(tester)
            ;

         Oculus.showDialog("Manufacturer Info", bld.toString(), JOptionPane.INFORMATION_MESSAGE);
      } catch (Throwable th) {
         Oculus.showError("Could not Decode", th);
      }
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.modules.session.SessionAwareController#onPlaceChanged(java.lang.String)
    */
   @Override
   protected void onPlaceChanged(String newPlaceId) {
      reloadHub();
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.modules.session.SessionAwareController#onSessionExpired()
    */
   @Override
   protected void onSessionExpired() {
      // unselected
      hubSelection.clearSelection();
      super.onSessionExpired();
   }



   private class UnpairDevicesAction extends AbstractAction {
      private String hubId;

      UnpairDevicesAction(HubModel model) {
         super("Un-Pair Devices");
         hubId = model.getAddress();
      }

      @Override
      public void actionPerformed(ActionEvent e) {
         startUnpairing(hubId);
      }
   }

   private class HubRegistrationProcess extends Timer implements Listener<Result<ClientEvent>> {
      private HubRegistrationDialog dialog;
      private String                hubId;
      private boolean               done = false;

      public HubRegistrationProcess(String hubId) {
         super(registrationPollingIntervalMs, null);
         this.dialog = new HubRegistrationDialog();
         this.hubId = hubId;
         this.setRepeats(false);
         this.addActionListener((event) -> tryRegister());
      }

      public ClientFuture<AddedEvent> register() {
         SwingUtilities.invokeLater(() -> tryRegister());
         return dialog.prompt().onCompletion((r) -> stop());
      }

      @Override
      public void stop() {
         this.done = true;
         this.dialog.dispose();
         super.stop();
      }

      public void tryRegister() {
         if (done) {
            return;
         }
         try {
            OculusSession info = getSessionInfo();
            tryRegister(info);
         } catch (IllegalStateException e) {
            dialog.setErrorMessage("Not currently logged in!", new AbstractAction("Login") {
               @Override
               public void actionPerformed(ActionEvent e) {
                  ServiceLocator.getInstance(SessionController.class)
                     .promptLogin()
                     .onSuccess((info) -> tryRegister(getSessionInfo()))
                     .onFailure((ex) -> dialog.dispose());
               }
            });
         }
      }

      protected void tryRegister(OculusSession info) {
         if (info.getPlaceId() == null) {
            Oculus.warn("No place selected, can't start pairing");
            return;
         }
         sendRegisterRequest(info, hubId).onCompletion(this);
      }

      @Override
      public void onEvent(Result<ClientEvent> result) {
         if (result.isError()) {
            Throwable error = result.getError();
            if (error instanceof ErrorResponseException) {
               ErrorResponseException errorEvent = (ErrorResponseException) error;
               switch (errorEvent.getCode()) {
                  case HubRegistration.NOT_FOUND:
                     showErrorAndRetry("Hub '" + hubId + "' is not registered.\nPlease connect your hub to the platform to begin pairing.\nWill retry shortly.");
                     break;
                  case HubRegistration.ALREADY_REGISTERED:
                     // TODO just break here?
                     showErrorAndRetry("Hub '" + hubId + "' is registered to another account.\nPlease disassociate it from that account.\nWill retry shortly.");
                     break;
                  case HubRegistration.MISSING_ARGUMENT:
                     showErrorAndClose("Invalid request to the platform.  This is likely due to a version mis-match between Oculus and Iris, please check your versions");
                     break;

                  default:
                     showErrorAndRetry(errorEvent.getMessage() + "\nWill retry shortly");
               }
            } else if (error instanceof CancellationException) {
               showErrorAndRetry("Request timed out, is platform-services running?\nWill retry shortly.");
            } else {
               showErrorAndRetry("Error during registration request.\nDetails: " + error.getMessage());
            }
         } else {
            ClientEvent body = result.getValue();
            if (body == null || body instanceof EmptyEvent) {
               // TODO some sort of progress update?
               clearErrorAndRetry();
            } else if (body instanceof AddedEvent) {
               this.dialog.submit((AddedEvent) body);
               this.stop();
            }
            // TODO hopefully this is temporary
            else if (AddedEvent.NAME.equals(body.getType())) {
               this.dialog.submit(new AddedEvent(body));
               this.stop();
            } else {
               showErrorAndRetry("Unexpected response: " + body);
            }
         }
      }

      private void showErrorAndClose(String message) {
         this.stop();
         JOptionPane.showMessageDialog(null, message, "Registration Failed", JOptionPane.ERROR_MESSAGE);
      }

      private void clearErrorAndRetry() {
         this.dialog.clearErrorMessage();
         this.start();
      }

      private void showErrorAndRetry(String message) {
         this.dialog.setErrorMessage("[" + new Date() + "]\n" + message);
         this.start();
      }

   }

   public ListenerRegistration addHubSelectedListener(Listener<HubModel> l) {
      return hubSelection.addNullableSelectionListener(l);
   }
}

