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
package com.iris.oculus.modules.place;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.swing.Action;
import javax.swing.JOptionPane;

import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.util.Addresses;
import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClient;
import com.iris.client.IrisClientFactory;
import com.iris.client.bean.PlaceAccessDescriptor;
import com.iris.client.bean.TimeZone;
import com.iris.client.capability.Account;
import com.iris.client.capability.Capability;
import com.iris.client.capability.Place;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.model.PlaceModel;
import com.iris.client.model.Store;
import com.iris.client.service.PlaceService.ListTimezonesRequest;
import com.iris.client.service.PlaceService.ListTimezonesResponse;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.place.dialog.AddPlacePrompt;
import com.iris.oculus.modules.place.dialog.SelectNewPlacePrompt;
import com.iris.oculus.modules.place.dialog.SelectTimezonePrompt;
import com.iris.oculus.modules.place.dialog.ValidateAddressPrompt;
import com.iris.oculus.modules.session.OculusSession;
import com.iris.oculus.modules.session.SessionAwareController;
import com.iris.oculus.modules.session.SessionController;
import com.iris.oculus.util.Actions;
import com.iris.oculus.util.DefaultSelectionModel;
import com.iris.oculus.util.SelectionModel;
import com.iris.oculus.util.Source;

public class PlaceController extends SessionAwareController {

	private IrisClient client;
	private Store<PlaceModel> store;

	private SessionController controller;
	private TimezoneSource timezoneSource;
	private DefaultSelectionModel<PlaceModel> activePlace = new DefaultSelectionModel<PlaceModel>();


   private Action addPlace = Actions.build("Add Place...", () -> promptForAddPlace());
	private Action deletePlace = Actions.build("Delete Current", () -> doDeletePlace());
	private Action setTimezone = Actions.build("Set Timezone", () -> promptForTimezone());
   private Action startAddingDevices = Actions.build("Start Adding Devices", () -> startAddingDevices());
   private Action stopAddingDevices = Actions.build("Stop Adding Devices", () -> stopAddingDevices());
   private Action validateAddress = Actions.build("Validate Address", this::promptForValidateAddress);

	@Inject
	public PlaceController(IrisClient client, SessionController controller) {
		this.client = client;
		this.store = IrisClientFactory.getStore(PlaceModel.class);
		this.controller = controller;
		this.timezoneSource = new TimezoneSource();

		if (controller != null) {
         controller.addSessionListener((e) -> onSessionEvent(e));
      }
	}

	public Action actionAddPlace() {
	   return addPlace;
	}

	public Action actionDeletePlace() {
	   return deletePlace;
	}

	public Action actionSetTimezone() {
	   return setTimezone;
	}

	public Action actionStartAddingDevices() {
      return startAddingDevices;
   }
   public Action actionStopAddingDevices() {
      return stopAddingDevices;
   }
   public Action actionValidateAddress() { return validateAddress; }

	public SelectionModel<PlaceModel> getActivePlace() {
	   return activePlace;
	}

	public Source<TimeZone> getTimeZones() {
	   return timezoneSource;
	}

	public void refresh() {
	   onPlaceSelected();
	}

	public ClientFuture<PlaceAccessDescriptor> promptForSelectPlace() {
	   return SelectNewPlacePrompt.prompt("Select a new place:");
	}

	public void selectPlace(PlaceAccessDescriptor p) {
	   controller.changePlace(p.getPlaceId());
	}

	private void promptForAddPlace() {
	   AddPlacePrompt.prompt().onSuccess((placeInfo) -> addPlace(placeInfo)
	         .onSuccess((event) -> Oculus.info("Added place: " + event))
	         .onFailure((error) -> Oculus.error("Unable to add place", error)));
	}

	private void promptForTimezone() {
	   SelectTimezonePrompt
	      .prompt(timezoneSource)
	      .onSuccess((tz) -> setTimezone(tz));
	}

	public void startAddingDevices() {
	   ServiceLocator.getInstance(PlaceAddDevicesController.class).startAddingDevices();
	}

   public void stopAddingDevices() {
      ServiceLocator.getInstance(PlaceAddDevicesController.class).stopAddingDevices();
   }

   private void promptForValidateAddress() {
		ValidateAddressPrompt.prompt(client);
	}

	private void setTimezone(TimeZone tz) {
	   PlaceModel place = activePlace.getSelectedItem().orNull();
	   if(place == null) {
	      Oculus.showDialog("Unable to Set Timezone", "No place is currently selected", JOptionPane.OK_OPTION);
	      return;
	   }

	   place.setTzId(tz.getId());
	   place.commit().onFailure((e) -> Oculus.showError("Unable to set timezone", e));
	}

	private void doDeletePlace() {
	   PlaceModel place = store.get(controller.getSessionInfo().getPlaceId().toString());
	   int res = Oculus.showOKCancelDialog("Confirm Place Delete", "Are you sure you want to delete this place?"); //returns 1 if user selects ok, 0 if they select cancel
	   if(res==0){
	   	return;
	   }
	   else if (res == 1){
		   place.delete()
		   .onFailure((t) -> {
			   Oculus.warn("Failed to delete place:  " + t.getMessage());
		   });
	   }
	}

	private ClientFuture<ClientEvent> addPlace(PlaceInfo place) {
	   OculusSession info = this.controller.getSessionInfo();
	   if(info == null) {
	      Oculus.warn("Cannot create a place, not currently logged in", this.controller.getReconnectAction());
	      return null;
	   }

	   if(info.getAccountId() == null) {
	      Oculus.warn("Cannot create a place when no account is active");
	      return null;
	   }

	   Map<String,Boolean> addons = place.getServiceAddons().stream().collect(Collectors.toMap((s) -> { return s; }, (s) -> { return Boolean.TRUE; }));

	   ClientRequest req = new ClientRequest();
	   req.setAddress("SERV:" + Account.NAMESPACE + ":" + info.getAccountId());
	   req.setCommand(Account.CMD_ADDPLACE);
	   req.setTimeoutMs(30000);
	   req.setAttribute(Account.AddPlaceRequest.ATTR_PLACE, place.asAttributes());
	   req.setAttribute(Account.AddPlaceRequest.ATTR_SERVICELEVEL, place.getServiceLevel());
	   req.setAttribute(Account.AddPlaceRequest.ATTR_ADDONS, addons);

	   return client.request(req);
	}

   @Override
   protected void onSessionInitialized(OculusSession info) {
      if(getPlaceId() != null) {
         onPlaceSelected();
      }
   }

   @Override
   protected void onPlaceChanged(String placeId) {
      onPlaceSelected();
   }

   private void onPlaceSelected() {
		String placeId = getPlaceId();
		ClientRequest request = new ClientRequest();
		request.setCommand(Capability.CMD_GET_ATTRIBUTES);
		request.setAddress(Addresses.toObjectAddress(Place.NAMESPACE, placeId));
		client
		   .request(request)
		   .onSuccess((response) -> {
		      PlaceModel m = (PlaceModel) IrisClientFactory.getModelCache().addOrUpdate(response.getAttributes());
		      store.add(m);
		      activePlace.setSelection(store.get(placeId));
		   })
		   .onFailure((e) -> Oculus.error("Unable to load places", e));
	}

	private class TimezoneSource implements Source<TimeZone> {
	   ClientFuture<List<TimeZone>> tz = null;

      @Override
      public synchronized ClientFuture<List<TimeZone>> getData() {
         if(tz == null) {
            ListTimezonesRequest request = new ListTimezonesRequest();
            request.setAddress(Addresses.toServiceAddress(Place.NAMESPACE));
            request.setRestfulRequest(true);

            tz = Futures.<List<TimeZone>, ClientEvent>transform(
                  client.request(request),
                  (event) -> {
                     ListTimezonesResponse response = new ListTimezonesResponse(event);
                     return
                           response
                              .getTimezones()
                              .stream()
                              .map(TimeZone::new)
                              .collect(Collectors.toList());
                  })
                     // don't cache failures
                  .onFailure((e) -> {
                     synchronized(TimezoneSource.this) {
                        tz = null;
                     }
                  });
         }
         return tz;
      }

      @Override
      public synchronized ClientFuture<List<TimeZone>> refreshData() {
         tz = null;
         return getData();
      }

	}
}

