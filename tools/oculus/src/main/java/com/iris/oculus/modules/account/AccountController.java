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

import com.google.inject.Inject;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Capability;
import com.iris.client.event.Listener;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.model.AccountModel;
import com.iris.client.model.Model;
import com.iris.client.model.PlaceModel;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.account.ux.DeletePrompt;
import com.iris.oculus.modules.place.PlaceController;
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

   @Override
   protected void onSessionExpired() {
      accountSelectionModel.clearSelection();
      super.onSessionExpired();
   }

   public ListenerRegistration addAccountListener(Listener<AccountModel> listener) {
      return accountSelectionModel.addNullableSelectionListener(listener);
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
