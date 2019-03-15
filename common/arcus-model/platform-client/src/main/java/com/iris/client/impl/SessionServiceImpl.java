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
package com.iris.client.impl;

import java.util.Map;

import com.google.common.base.Function;
import com.iris.capability.util.Addresses;
import com.iris.client.ClientEvent;
import com.iris.client.IrisClient;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.service.SessionService;

public class SessionServiceImpl implements SessionService {
   private static final String ADDRESS = Addresses.toServiceAddress(SessionService.NAMESPACE);
   private IrisClient client;

   public SessionServiceImpl(IrisClient client) {
      this.client = client;
   }

   @Override
   public String getName() {
      return SessionService.NAME;
   }

   @Override
   public String getAddress() {
      return ADDRESS;
   }

   @Override
   public ClientFuture<SetActivePlaceResponse> setActivePlace(String placeId) {
      SetActivePlaceRequest request = new SetActivePlaceRequest();
      request.setAddress(getAddress());
      request.setRestfulRequest(false);

      request.setPlaceId(placeId);

      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, SetActivePlaceResponse>() {
         @Override
         public SetActivePlaceResponse apply(ClientEvent input) {
            return new SetActivePlaceResponse(input);
         }
      });
   }

   @Override
   public ClientFuture<LogResponse> log(String category, String code, String message) {
      LogRequest request = new LogRequest();
      request.setAddress(getAddress());
      request.setRestfulRequest(false);

      request.setCategory(category);
      request.setCode(code);
      request.setMessage(message);

      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, LogResponse>() {
         @Override
         public LogResponse apply(ClientEvent input) {
            return new LogResponse(input);
         }
      });
   }

   @Override
   public ClientFuture<TagResponse> tag(String name, Map<String, String> context) {
	   TagRequest request = new TagRequest();
	   request.setAddress(getAddress());
	   request.setRestfulRequest(false);

	   request.setName(name);
	   request.setContext(context);

	   ClientFuture<ClientEvent> result = client.request(request);
	   return Futures.transform(result,  new Function<ClientEvent, TagResponse>() {
		  @Override
		  public TagResponse apply(ClientEvent input) {
			  return new TagResponse(input);
		  }
	   });
   }

   @Override
   public ClientFuture<ListAvailablePlacesResponse> listAvailablePlaces() {
      ListAvailablePlacesRequest request = new ListAvailablePlacesRequest();
      request.setAddress(getAddress());
      request.setRestfulRequest(false);

      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result,  new Function<ClientEvent, ListAvailablePlacesResponse>() {
         @Override
         public ListAvailablePlacesResponse apply(ClientEvent input) {
            return new ListAvailablePlacesResponse(input);
         }
      });
   }

   @Override
   public ClientFuture<GetPreferencesResponse> getPreferences() {
      GetPreferencesRequest request = new GetPreferencesRequest();
      request.setAddress(getAddress());
      request.setRestfulRequest(false);

      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, GetPreferencesResponse>() {
         @Override
         public GetPreferencesResponse apply(ClientEvent input) {
            return new GetPreferencesResponse(input);
         }
      });
   }

   @Override
   public ClientFuture<SetPreferencesResponse> setPreferences(Map<String, Object> prefs) {
      SetPreferencesRequest request = new SetPreferencesRequest();
      request.setAddress(getAddress());
      request.setRestfulRequest(false);

      request.setPrefs(prefs);

      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, SetPreferencesResponse>() {
         @Override
         public SetPreferencesResponse apply(ClientEvent input) {
            return new SetPreferencesResponse(input);
         }
      });
   }

   @Override
   public ClientFuture<ResetPreferenceResponse> resetPreference(String prefKey) {
      ResetPreferenceRequest request = new ResetPreferenceRequest();
      request.setAddress(getAddress());
      request.setRestfulRequest(false);

      request.setPrefKey(prefKey);

      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, ResetPreferenceResponse>() {
         @Override
         public ResetPreferenceResponse apply(ClientEvent input) {
            return new ResetPreferenceResponse(input);
         }
      });
   }

	@Override
	public ClientFuture<LockDeviceResponse> lockDevice(String deviceIdentifier, String reason) {
		LockDeviceRequest request = new LockDeviceRequest();
      request.setAddress(getAddress());
      request.setRestfulRequest(true);
      request.setDeviceIdentifier(deviceIdentifier);
      request.setReason(reason);

      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, LockDeviceResponse>() {
         @Override
         public LockDeviceResponse apply(ClientEvent response) {
            return new LockDeviceResponse(response);
         }
      });
	}
}

