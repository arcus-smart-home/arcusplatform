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
package com.iris.oculus.modules.account;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.iris.billing.client.RecurlyTokenClient;
import com.iris.billing.client.model.request.BillingInfoRequest;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Capability;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.event.SettableClientFuture;
import com.iris.client.model.AccountModel;
import com.iris.client.model.Model;
import com.iris.client.model.PlaceModel;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.account.ux.BillingInformationPrompt;
import com.iris.oculus.modules.account.ux.DeletePrompt;
import com.iris.oculus.modules.place.PlaceController;
import com.iris.oculus.modules.session.OculusSession;
import com.iris.oculus.modules.session.SessionAwareController;
import com.iris.oculus.util.DefaultSelectionModel;
import com.iris.oculus.util.SelectionModel;


public class AccountController extends SessionAwareController {
   private DefaultSelectionModel<AccountModel> accountSelectionModel =
         new DefaultSelectionModel<>();

   @Inject
   public AccountController(PlaceController placeController) {
      placeController.getActivePlace().addSelectionListener((selection) -> {
         if(selection.isPresent()) {
            setActivePlace(selection.get());
         }
      });
   }

   private void setActivePlace(PlaceModel placeModel) {
      ClientRequest request = new ClientRequest();
      request.setAddress("SERV:account:" + placeModel.getAccount());
      request.setCommand(Capability.CMD_GET_ATTRIBUTES);
      getSessionInfo().setAccountId(placeModel.getAccount());
      
      IrisClientFactory
         .getClient()
         .request(request)
         .onSuccess((response) -> accountSelectionModel.setSelection((AccountModel) IrisClientFactory.getModelCache().addOrUpdate(response.getAttributes())))
         .onFailure((e) -> Oculus.error("Unable to load account information", e));
   }

   /* (non-Javadoc)
    * @see com.iris.oculus.session.SessionAwareController#onSessionExpired()
    */
   @Override
   protected void onSessionExpired() {
      accountSelectionModel.clearSelection();
      super.onSessionExpired();
   }

   public ListenerRegistration addAccountListener(Listener<AccountModel> listener) {
      return accountSelectionModel.addNullableSelectionListener(listener);
   }

   public void promptBillingInformation() {
      BillingInformationPrompt.prompt("Create Billing Account").onSuccess((billingInfo) -> createBillingAccount(billingInfo)
            .onSuccess((event) -> Oculus.info("Created a new billing account: " + event))
            .onFailure((error) -> Oculus.error("Unable to create a billing account", error)));
   }

   public void promptUpdateBillingInformation() {
      BillingInformationPrompt.prompt("Create/Update Billing Account").onSuccess((billingInfo) -> updateBillingAccount(billingInfo)
            .onSuccess((event) -> Oculus.info("Updating billing account: " + event))
            .onFailure((error) -> Oculus.error("Unable to update billing account", error)));
   }

   public void promptDelete() {
      DeletePrompt.prompt().onSuccess((deleteLogin) -> {
         if(accountSelectionModel.hasSelection()) {
            accountSelectionModel
               .getSelectedItem()
               .get()
               .delete(deleteLogin)
               .onFailure((t) -> Oculus.warn("Unable to delete account", t));
         }
      });
   }

   private ClientFuture<Boolean> createBillingAccount(BillingInfoRequest request) {
      OculusSession info = this.getSessionInfo();

      if(!validateBillingCall(info)) {
         return null;
      }

      SettableClientFuture<Boolean> future = new SettableClientFuture<>();

      ListenableFuture<String> tokenFuture = getBillingToken(request, info);
      com.google.common.util.concurrent.Futures.addCallback(tokenFuture, new FutureCallback<String>() {
         @Override
         public void onSuccess(String result) {
            accountSelectionModel
               .getSelectedItem()
               .get()
               .createBillingAccount(result, info.getPlaceId().toString())
               .onSuccess((e) -> future.setValue(true))
               .onFailure((t) -> future.setError(t));
         }

         @Override
         public void onFailure(Throwable t) {
            future.setError(t);
         }
      }, MoreExecutors.directExecutor());

      return future;
   }

   private ClientFuture<Boolean> updateBillingAccount(BillingInfoRequest request) {
      OculusSession info = this.getSessionInfo();
      if(!validateBillingCall(info)) {
         return null;
      }

      SettableClientFuture<Boolean> future = new SettableClientFuture<>();

      ListenableFuture<String> tokenFuture = getBillingToken(request, info);
      com.google.common.util.concurrent.Futures.addCallback(tokenFuture, new FutureCallback<String>() {
         @Override
         public void onSuccess(String result) {
            accountSelectionModel
               .getSelectedItem()
               .get()
               .updateBillingInfoCC(result)
               .onSuccess((e) -> future.setValue(true))
               .onFailure((t) -> future.setError(t));
         }

         @Override
         public void onFailure(Throwable t) {
            future.setError(t);
         }
      }, MoreExecutors.directExecutor());

      return future;
   }

   private ListenableFuture<String> getBillingToken(BillingInfoRequest request, OculusSession info) {
      RecurlyTokenClient client = new RecurlyTokenClient();
      request.setPublicKey(info.getBillingPublicKey());
      return client.getBillingToken(request);
   }

   private boolean validateBillingCall(OculusSession info) {
      if(info == null) {
         Oculus.warn("Cannot create a billing account, not currently logged in");
         return false;
      }

      if(info.getAccountId() == null) {
         Oculus.warn("Cannot create a billing account when no account is active");
         return false;
      }

      if(info.getPlaceId() == null) {
         Oculus.warn("Cannot create a billing account when no place is active");
         return false;
      }
      return true;
   }

   public void refreshAccount() {
      AccountModel account = accountSelectionModel.getSelectedItem().orNull();
      if(account != null) {
         account
            .refresh()
            .onFailure((error) -> Oculus.warn("Unable to load account settings", error))
            ;
      }
   }

   public SelectionModel<? extends Model> getAccountSelectionModel() {
      return accountSelectionModel;
   }
}

