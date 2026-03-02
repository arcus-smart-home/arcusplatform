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
package com.iris.oculus.modules.apikey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;
import javax.swing.Action;

import com.iris.capability.util.Addresses;
import com.iris.client.ClientEvent;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Place;
import com.iris.client.event.ClientFuture;
import com.iris.client.exception.ErrorResponseException;
import com.iris.client.service.ApiKeyService;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.session.OculusSession;
import com.iris.oculus.modules.session.SessionAwareController;
import com.iris.oculus.util.Actions;

@Singleton
public class ApiKeyController extends SessionAwareController {
   private static final String UNSUPPORTED_MESSAGE = "unsupported.message";

   private List<Map<String, String>> keys = new ArrayList<>();
   private final List<Runnable> listeners = new ArrayList<>();
   private Action reloadAction = Actions.build("Refresh", this, "reload");
   private boolean unsupported = false;

   public ApiKeyController() {
   }

   @Override
   protected void onSessionInitialized(OculusSession info) {
      unsupported = false;
      reload();
   }

   @Override
   protected void onPlaceChanged(String newPlaceId) {
      unsupported = false;
      reload();
   }

   @Override
   protected void onSessionExpired() {
      keys.clear();
      unsupported = false;
      fireChanged();
   }

   public List<Map<String, String>> getKeys() {
      return Collections.unmodifiableList(keys);
   }

   public boolean isUnsupported() {
      return unsupported;
   }

   public Action actionReload() {
      return reloadAction;
   }

   public void addChangeListener(Runnable listener) {
      listeners.add(listener);
   }

   private void fireChanged() {
      for (Runnable r : listeners) {
         r.run();
      }
   }

   private String placeAddress() {
      return Addresses.toObjectAddress(Place.NAMESPACE, getPlaceId());
   }

   public void reload() {
      if (!isSessionActive()) {
         return;
      }
      ApiKeyService.ListKeysRequest request = new ApiKeyService.ListKeysRequest();
      request.setAddress(placeAddress());
      request.setPlaceId(getPlaceId());

      ClientFuture<ClientEvent> result = IrisClientFactory.getClient().request(request);
      result
         .onSuccess((event) -> {
            unsupported = false;
            ApiKeyService.ListKeysResponse response = new ApiKeyService.ListKeysResponse(event);
            List<Map<String, String>> loaded = response.getKeys();
            keys = loaded != null ? new ArrayList<>(loaded) : new ArrayList<>();
            fireChanged();
         })
         .onFailure((error) -> {
            if (isUnsupportedError(error)) {
               unsupported = true;
               keys.clear();
               fireChanged();
            } else {
               Oculus.error("Unable to load API keys", error);
            }
         });
   }

   private boolean isUnsupportedError(Throwable error) {
      return error instanceof ErrorResponseException
         && UNSUPPORTED_MESSAGE.equals(((ErrorResponseException) error).getCode());
   }

   public void createKey(String label, Set<String> permissions) {
      ApiKeyService.CreateRequest request = new ApiKeyService.CreateRequest();
      request.setAddress(placeAddress());
      request.setPlaceId(getPlaceId());
      request.setLabel(label);
      request.setPermissions(permissions);

      ClientFuture<ClientEvent> result = IrisClientFactory.getClient().request(request);
      Oculus.showProgress(result, "Creating API key...");
      result
         .onSuccess((event) -> {
            ApiKeyService.CreateResponse response = new ApiKeyService.CreateResponse(event);
            showRawKey(response.getId(), response.getKey());
            reload();
         })
         .onFailure((error) -> Oculus.showError("Failed to Create API Key", error));
   }

   private void showRawKey(String id, String rawKey) {
      javax.swing.JTextArea textArea = new javax.swing.JTextArea(rawKey);
      textArea.setEditable(false);
      textArea.setFont(new java.awt.Font("monospaced", java.awt.Font.PLAIN, 13));
      textArea.setRows(3);
      textArea.setColumns(50);
      textArea.selectAll();

      javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(textArea);

      javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(0, 8));
      panel.add(new javax.swing.JLabel("Copy this key now. It will not be shown again."), java.awt.BorderLayout.NORTH);
      panel.add(scroll, java.awt.BorderLayout.CENTER);

      javax.swing.JOptionPane.showMessageDialog(
         Oculus.getMainWindow(),
         panel,
         "API Key Created (ID: " + id + ")",
         javax.swing.JOptionPane.INFORMATION_MESSAGE
      );
   }

   public void revokeKey(String id, String label) {
      int result = javax.swing.JOptionPane.showConfirmDialog(
         Oculus.getMainWindow(),
         "Revoke API key \"" + label + "\"?\nThis cannot be undone.",
         "Confirm Revoke",
         javax.swing.JOptionPane.OK_CANCEL_OPTION,
         javax.swing.JOptionPane.WARNING_MESSAGE
      );
      if (result != javax.swing.JOptionPane.OK_OPTION) {
         return;
      }

      ApiKeyService.RevokeRequest request = new ApiKeyService.RevokeRequest();
      request.setAddress(placeAddress());
      request.setPlaceId(getPlaceId());
      request.setId(id);

      ClientFuture<ClientEvent> future = IrisClientFactory.getClient().request(request);
      Oculus.showProgress(future, "Revoking API key...");
      future
         .onSuccess((event) -> reload())
         .onFailure((error) -> Oculus.showError("Failed to Revoke API Key", error));
   }
}
