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
package com.iris.oculus.modules.behaviors;

import java.awt.event.ActionEvent;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.google.inject.Inject;
import com.iris.client.IrisClient;
import com.iris.client.capability.CareSubsystem;
import com.iris.client.capability.CareSubsystem.ListBehaviorsRequest;
import com.iris.client.capability.CareSubsystem.ListBehaviorsResponse;
import com.iris.client.capability.CareSubsystem.RemoveBehaviorRequest;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.bean.CareBehavior;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.session.OculusSession;
import com.iris.oculus.modules.session.SessionAwareController;
import com.iris.oculus.modules.session.OculusSession;
import com.iris.oculus.util.DefaultSelectionModel;
import com.iris.oculus.view.SimpleViewModel;
import com.iris.oculus.view.ViewModel;

/**
 * 
 */
public class BehaviorController extends SessionAwareController {

   SimpleViewModel<CareBehavior> behaviors = new SimpleViewModel<>();
   private DefaultSelectionModel<CareBehavior> behaviorSelection = new DefaultSelectionModel<>();
   private IrisClient client;

   @SuppressWarnings("serial")
   private final Action refreshAction = new AbstractAction("Refresh") {
      @Override
      public void actionPerformed(ActionEvent e) {
         refreshBehaviors();
      }
   };
   @SuppressWarnings("serial")
   private final Action deleteAction = new AbstractAction("Delete") {
      @Override
      public void actionPerformed(ActionEvent e) {
         removeBehavior();
      }
   };

   @Inject
   public BehaviorController(IrisClient client) {
      this.client = client;
   }

   public ListenerRegistration addBehaviorSelectedListener(Listener<CareBehavior> l) {
      return behaviorSelection.addNullableSelectionListener(l);
   }

   public DefaultSelectionModel<CareBehavior> getSelectionModel() {
      return behaviorSelection;
   }

   @Override
   protected void onSessionInitialized(OculusSession info) {
      refreshBehaviors();
   }

   @Override
   protected void onPlaceChanged(String newPlaceId) {
      refreshBehaviors();
   }

   @Override
   protected void onSessionExpired() {
      behaviors.removeAll();
   }

   protected void onRefresh(ListBehaviorsResponse response) {
      onBehaviorsLoaded(response);
   }

   protected void onBehaviorsLoaded(ListBehaviorsResponse response) {
      behaviors.removeAll();
      behaviors.addAll(response.getBehaviors().stream().map(CareBehavior::new).collect(Collectors.toList()));
   }

   protected void doLoadBehaviors(Listener<ListBehaviorsResponse> l) {
      if (!isSessionActive()) {
         return;
      }
      OculusSession info = getSessionInfo();
      if (info.getPlaceId() == null) {
         return;
      }

      ListBehaviorsRequest request = new ListBehaviorsRequest();
      request.setAddress("SERV:" + CareSubsystem.NAMESPACE + ":" + getPlaceId());

      client.request(request)
            .onSuccess((e) -> l.onEvent(new ListBehaviorsResponse(e)))
            .onFailure((error) -> {
               if (error instanceof CancellationException){
                  //timeout not sure what to do here.  but lets be quiet
               }
               else{
                  Oculus.error("Unable to load behavior entries", error);
               }
            });
   }

   public Action refreshAction() {
      return refreshAction;
   }

   public Action deleteAction() {
      return deleteAction;
   }

   public void refreshBehaviors() {
      doLoadBehaviors((r) -> onRefresh(r));
   }

   public void removeBehavior() {
      if (behaviorSelection.hasSelection()) {
         RemoveBehaviorRequest request = new RemoveBehaviorRequest();
         request.setAddress("SERV:" + CareSubsystem.NAMESPACE + ":" + getPlaceId());
         request.setAttribute(RemoveBehaviorRequest.ATTR_ID, behaviorSelection.getSelectedItem().get().getId());
         client.request(request)
               .onSuccess((e) -> {
                  behaviorSelection.clearSelection();
                  Oculus.info("Behavior Removed");
               })
               .onFailure((error) -> Oculus.error("Unable to remove behavior", error));
      }
   }

   public ViewModel<CareBehavior> getBehaviors() {
      return this.behaviors;
   }
}

