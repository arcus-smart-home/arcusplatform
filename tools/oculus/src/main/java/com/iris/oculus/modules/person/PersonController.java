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
package com.iris.oculus.modules.person;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.iris.capability.util.Addresses;
import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClient;
import com.iris.client.capability.Person.ChangePinResponse;
import com.iris.client.capability.Person.ChangePinV2Response;
import com.iris.client.capability.Person.VerifyPinResponse;
import com.iris.client.capability.Place;
import com.iris.client.capability.Place.ListPersonsResponse;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.model.PersonModel;
import com.iris.client.model.PlaceModel;
import com.iris.client.util.Result;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.BaseController;
import com.iris.oculus.modules.person.dialog.ChangePinDialog;
import com.iris.oculus.modules.person.dialog.ChangePinV2Dialog;
import com.iris.oculus.modules.person.dialog.VerifyPinDialog;
import com.iris.oculus.modules.session.OculusSession;
import com.iris.oculus.modules.session.SessionController;
import com.iris.oculus.util.Actions;
import com.iris.oculus.util.Models;



@Singleton
public class PersonController extends BaseController<PersonModel> {
   private IrisClient irisClient;
   private SessionController sessionController;

   private Action changePinAction = Actions.build("Change Pin (Deprecated)", () -> this.changePin());
   private Action changePinV2Action = Actions.build("Change Pin V2", () -> this.changePinV2());
   private Action verifyPinAction = Actions.build("Verify Pin", () -> this.verifyPin());

   @Inject
   public PersonController(
         IrisClient client,
         SessionController controller
   ) {
      super(PersonModel.class);
      this.irisClient = client;
      this.sessionController = controller;
   }

   public @Nullable PlaceModel getCurrentPlace() {
      return Models.getIfLoaded(Addresses.toObjectAddress(Place.NAMESPACE, getPlaceId()), PlaceModel.class).orElse(null);
   }

   public Action actionChangePin() {
      return changePinAction;
   }

   public Action actionChangePinV2() {
      return changePinV2Action;
   }

   public void changePinV2() {
      changePinV2(null);
   }
   
   public Action actionVerifyPin() {
      return verifyPinAction;
   }   
   
   public void verifyPin() {
      verifyPin(null);
   }
   
   public void verifyPin(String errorMessage) {
      if(!getSelection().hasSelection()) {
         Oculus.showDialog("Select a Person", "No person is selected, choose a person in order to verify their pin", JOptionPane.WARNING_MESSAGE);
         return;
      }

      String personAddress = getSelection().getSelectedItem().get().getAddress();
      VerifyPinDialog
         .prompt()
         .onSuccess((request) -> doVerifyPin(request, personAddress));
   }

   private void doVerifyPin(ClientRequest req, String personAddress) {
      req.setAddress(personAddress);
      req.setRestfulRequest(true);
      ClientFuture<?> future =
            irisClient
               .request(req)
               .onCompletion((result) -> onVerifyPin(result))
               ;
         Oculus.showProgress(future, "Verifying pin...");
   }
   
   private void onVerifyPin(Result<ClientEvent> result) {
      if(result.isError()) {
         Oculus.showError("Unable to Verify Pin", result.getError());
         return;
      }

      VerifyPinResponse response = new VerifyPinResponse(result.getValue());
      if(!Boolean.TRUE.equals(response.getSuccess())) {
         verifyPin("Pin is not correct");
         return;
      }
      
      Oculus.showSuccess("VerifyPin Success!", "The Pins Match");
   }
   
   public void changePinV2(String errorMessage) {
      if(!getSelection().hasSelection()) {
         Oculus.showDialog("Select a Person", "No person is selected, choose a person in order to change their pin", JOptionPane.WARNING_MESSAGE);
         return;
      }

      String personAddress = getSelection().getSelectedItem().get().getAddress();
      ChangePinV2Dialog
         .prompt()
         .onSuccess((request) -> doChangePin(request, personAddress, true));
   }

   private void doChangePin(ClientRequest req, String personAddress, boolean v2) {
      req.setAddress(personAddress);
      req.setRestfulRequest(true);
      ClientFuture<?> future =
            irisClient
               .request(req)
               .onCompletion((result) -> onPinChanged(result, v2))
               ;
         Oculus.showProgress(future, "Changing pin...");
   }

   /**
    * Changes the pin on the currently selected person
    */
   public void changePin() {
      changePin(null);
   }

   protected void changePin(String errorMessage) {
      if(!getSelection().hasSelection()) {
         Oculus.showDialog("Select a Person", "No person is selected, choose a person in order to change their pin", JOptionPane.WARNING_MESSAGE);
         return;
      }

      String personAddress = getSelection().getSelectedItem().get().getAddress();
      ChangePinDialog
         .prompt()
         .onSuccess((request) -> { doChangePin(request, personAddress, false); });
   }

   private void onPinChanged(Result<ClientEvent> result, boolean v2) {
      if(result.isError()) {
         Oculus.showError("Unable to Change Pin", result.getError());
         return;
      }

      if(!v2) {
         ChangePinResponse response = new ChangePinResponse(result.getValue());
         if(!Boolean.TRUE.equals(response.getSuccess())) {
            changePin("Previous pin was not correct");
         }
      } else {
         ChangePinV2Response response = new ChangePinV2Response(result.getValue());
         if(!Boolean.TRUE.equals(response.getSuccess())) {
            changePinV2("Previous pin was not correct");
         }
      }
   }

   @Override
   protected ClientFuture<? extends Collection<Map<String, Object>>> doLoad() {
      OculusSession info = getSessionInfo();
      if (info == null) {
         Oculus.warn("Can't load person, not currently logged in", sessionController.getReconnectAction());
         return Futures.succeededFuture(ImmutableList.of());
      }
      if (info.getPlaceId() == null) {
         Oculus.warn("Can't load persons, no current place selected.");
         return Futures.succeededFuture(ImmutableList.of());
      }

      ClientRequest request = new ClientRequest();
      request.setCommand(Place.ListPersonsRequest.NAME);
      request.setAddress(Addresses.toObjectAddress(Place.NAMESPACE, info.getPlaceId()));
      request.setTimeoutMs(30000);
      return
         irisClient
            .request(request)
            .transform((r) -> new ListPersonsResponse(r).getPersons());
   }

   @Override
   protected void onPlaceChanged(String newPlaceId) {
      super.onPlaceChanged(newPlaceId);
      reload();
   }

}

