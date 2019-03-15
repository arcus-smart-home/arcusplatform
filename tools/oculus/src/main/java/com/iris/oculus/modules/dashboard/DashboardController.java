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
package com.iris.oculus.modules.dashboard;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.google.common.collect.ImmutableMap;
import com.iris.capability.util.Addresses;
import com.iris.client.IrisClientFactory;
import com.iris.client.bean.HistoryLog;
import com.iris.client.capability.Capability;
import com.iris.client.capability.Place;
import com.iris.client.capability.Place.ListDashboardEntriesRequest;
import com.iris.client.capability.Place.ListDashboardEntriesResponse;
import com.iris.client.event.Listener;
import com.iris.client.model.PlaceModel;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.session.SessionAwareController;
import com.iris.oculus.util.Actions;
import com.iris.oculus.modules.session.OculusSession;
import com.iris.oculus.view.SimpleViewModel;
import com.iris.oculus.view.ViewModel;

/**
 * 
 */
public class DashboardController extends SessionAwareController {

   SimpleViewModel<HistoryLog> logs = new SimpleViewModel<>();
   List<String> tokens = new ArrayList<>();
   String nextToken;
   
   private final Action refreshAction = Actions.build("Refresh", this::refreshLogs);
   private final Action nextAction = Actions.build("Next >", this::nextPage);
   private final Action previousAction = Actions.build("< Previous", this::prevPage);
   
   /**
    * 
    */
   public DashboardController() {
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.session.SessionAwareController#onSessionInitialized(com.iris.oculus.session.SessionInfo)
    */
   @Override
   protected void onSessionInitialized(OculusSession info) {
      refreshLogs();
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.session.SessionAwareController#onPlaceChanged(java.util.UUID)
    */
   @Override
   protected void onPlaceChanged(String newPlaceId) {
      refreshLogs();
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.session.SessionAwareController#onSessionExpired()
    */
   @Override
   protected void onSessionExpired() {
      logs.removeAll();
   }

   protected void onRefresh(ListDashboardEntriesResponse response) {
      nextToken = response.getNextToken();
      onLogsLoaded(response);
   }
   
   protected void onNext(ListDashboardEntriesResponse response) {
      tokens.add(nextToken);
      nextToken = response.getNextToken();
      onLogsLoaded(response);
   }
   
   protected void onPrev(ListDashboardEntriesResponse response) {
      if(!tokens.isEmpty()) {
         tokens.remove(tokens.size() - 1);
      }
      nextToken = response.getNextToken();
      onLogsLoaded(response);
   }
   
   protected void onLogsLoaded(ListDashboardEntriesResponse response) {
      logs.removeAll(); 
      logs.addAll(response.getResults().stream().map(HistoryLog::new).collect(Collectors.toList()));
      previousAction.setEnabled(!tokens.isEmpty());
      nextAction.setEnabled(nextToken != null);
   }
   
   protected void doLoadLogs(String token, Listener<ListDashboardEntriesResponse> l) {
      if(!isSessionActive()) {
         return;
      }
      OculusSession info = getSessionInfo();
      if(info.getPlaceId() == null) {
         return;
      }
      
      String id = info.getPlaceId().toString();
      
      // TODO do this better...
      ListDashboardEntriesRequest request = new ListDashboardEntriesRequest();
      request.setAddress(Addresses.toObjectAddress(Place.NAMESPACE, id));
      request.setLimit(50);
      request.setToken(token);
      request.setTimeoutMs(30000);
      
      IrisClientFactory
         .getClient()
         .request(request)
         .onSuccess((e) -> l.onEvent(new ListDashboardEntriesResponse(e)))
         .onFailure((error) -> Oculus.error("Unable to load dashboard entries", error))
         ;
   }
   
   /**
    * @return the refreshAction
    */
   public Action refreshAction() {
      return refreshAction;
   }

   /**
    * @return the nextAction
    */
   public Action nextAction() {
      return nextAction;
   }

   /**
    * @return the previousAction
    */
   public Action previousAction() {
      return previousAction;
   }

   public void refreshLogs() {
      doLoadLogs(tokens.isEmpty() ? null : tokens.get(0), (r) -> onRefresh(r));
   }
   
   public void nextPage() {
      doLoadLogs(nextToken, (r) -> onNext(r));
   }
   
   public void prevPage() {
      doLoadLogs(tokens.size() < 2 ? null : tokens.get(tokens.size() - 2), (r) -> onPrev(r));
   }
   
   public ViewModel<HistoryLog> getLogs() {
      return this.logs;
   }

}

