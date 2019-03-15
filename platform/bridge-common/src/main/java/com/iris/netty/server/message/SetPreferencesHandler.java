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
package com.iris.netty.server.message;

import static com.google.common.collect.Sets.difference;
import static com.iris.messages.service.SessionService.SetPreferencesResponse.CODE_PLACE_ACTIVE_NOTSET;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.session.Session;
import com.iris.core.dao.PreferencesDAO;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.ClientMessage;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.SessionService;
import com.iris.messages.service.SessionService.PreferencesChangedEvent;
import com.iris.messages.service.SessionService.SetPreferencesRequest;
import com.iris.messages.service.SessionService.SetPreferencesResponse;
import com.iris.messages.type.CardPreference;
import com.iris.messages.type.Preferences;
import com.iris.population.PlacePopulationCacheManager;

@Singleton
public class SetPreferencesHandler extends BaseClientRequestHandler
{
   public static final String NAME_EXECUTOR = "executor.setpreferences";

   public static final ErrorEvent ACTIVE_PLACE_NOT_SET =
      Errors.fromCode(CODE_PLACE_ACTIVE_NOTSET, "No active place is currently set");

   private static final Set<String> VALID_PREFERENCE_ATTRIBUTES = Preferences.TYPE.asObject().getAttributes().keySet();

   private final PreferencesDAO preferencesDao;
   private final PlatformMessageBus messageBus;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public SetPreferencesHandler(
      PreferencesDAO preferencesDao, 
      PlatformMessageBus messageBus,
      @Named(NAME_EXECUTOR) Executor executor,
      PlacePopulationCacheManager populationCacheMgr
   ) {
      super(executor);

      this.preferencesDao = preferencesDao;
      this.messageBus = messageBus;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public String getRequestType() {
      return SetPreferencesRequest.NAME;
   }

   @Override
   protected Address address() {
      return SessionService.ADDRESS;
   }

   @Override
   protected MessageBody doHandle(ClientMessage request, Session session) {
      validate(request, session);

      UUID personUuid = session.getClient().getPrincipalId();
      UUID placeUuid = UUID.fromString(session.getActivePlace());

      Map<String, Object> prefs = SetPreferencesRequest.getPrefs(request.getPayload());

      Map<String, Object> completePrefs = preferencesDao.findById(personUuid, placeUuid);

      if (completePrefs == null)
      {
         completePrefs = prefs;
      }
      else
      {
         completePrefs.putAll(prefs);
      }

      preferencesDao.merge(personUuid, placeUuid, prefs);

      emitPreferencesChangedEvent(personUuid, placeUuid, populationCacheMgr.getPopulationByPlaceId(placeUuid), completePrefs);

      return SetPreferencesResponse.instance();
   }

   private void validate(ClientMessage request, Session session)
   {
      Client client = session.getClient();
      if (client == null)
      {
         throw new IllegalStateException("client cannot be null");
      }

      UUID personUuid = client.getPrincipalId();
      if (personUuid == null)
      {
         throw new IllegalStateException("principalId cannot be null");
      }

      String placeId = session.getActivePlace();
      if (isEmpty(placeId))
      {
         throw new ErrorEventException(ACTIVE_PLACE_NOT_SET);
      }

      Map<String, Object> prefs = SetPreferencesRequest.getPrefs(request.getPayload());
      Errors.assertValidRequest(difference(prefs.keySet(), VALID_PREFERENCE_ATTRIBUTES).isEmpty(),
         SetPreferencesRequest.ATTR_PREFS + " contains unexpected keys");

      Preferences preferences = new Preferences(prefs);

      if (preferences.getDashboardCards() != null) {
         // This prevents unexpected cards
         Set<String> cards = new HashSet<>();
         for(Map<String, Object> card: preferences.getDashboardCards()) {
            try
            {
               CardPreference cp = new CardPreference(card);
               Errors.assertValidRequest(cards.add(cp.getServiceName()),
                  "Duplicate dashboard card [" + cp.getServiceName() + "]");
            }
            catch (IllegalArgumentException e)
            {
               if (e.getMessage() != null && e.getMessage().contains("is not a valid member of the enumeration set"))
               {
                  Errors.assertValidRequest(false,
                     "Unrecognized dashboard card [" + card.get(CardPreference.ATTR_SERVICENAME) + "]");
               }
               else
               {
                  throw e;
               }
            }
         }
      }
   }

   private void emitPreferencesChangedEvent(UUID personUuid, UUID placeUuid, String population, Map<String, Object> completePrefs)
   {
      MessageBody body = PreferencesChangedEvent.builder()
         .withPrefs(completePrefs)
         .build();

      PlatformMessage message = PlatformMessage.buildEvent(body, address())
         .withActor(Address.platformService(personUuid, PersonCapability.NAMESPACE))
         .withPlaceId(placeUuid)
         .withPopulation(population)
         .create();

      messageBus.send(message);
   }
}

