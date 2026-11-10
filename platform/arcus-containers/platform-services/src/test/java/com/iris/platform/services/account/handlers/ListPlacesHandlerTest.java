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
package com.iris.platform.services.account.handlers;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.Account;
import com.iris.messages.model.Place;
import com.iris.messages.type.PlaceAccessDescriptor;
import com.iris.security.apikey.ApiKeyPrincipal;

/**
 * Anyone with a grant on the account may send ListPlaces (see RoleAuthorizer), so the handler
 * narrows the answer to the places the sender can see.
 */
public class ListPlacesHandlerTest {
   private final UUID owner = UUID.randomUUID();
   private final UUID guest = UUID.randomUUID();
   private final Place home = place();
   private final Place cabin = place();
   private final Place otherAccountsPlace = place();

   private Account account;
   private PlaceDAO placeDao;
   private PersonPlaceAssocDAO assocDao;
   private ListPlacesHandler handler;

   private static Place place() {
      Place place = new Place();
      place.setId(UUID.randomUUID());
      return place;
   }

   @SuppressWarnings("unchecked")
   @Before
   public void setUp() {
      account = new Account();
      account.setId(UUID.randomUUID());
      account.setOwner(owner);
      account.setPlaceIDs(ImmutableSet.of(home.getId(), cabin.getId()));
      home.setAccount(account.getId());
      cabin.setAccount(account.getId());
      otherAccountsPlace.setAccount(UUID.randomUUID());

      placeDao = EasyMock.createMock(PlaceDAO.class);
      assocDao = EasyMock.createMock(PersonPlaceAssocDAO.class);
      BeanAttributesTransformer<Place> transformer = EasyMock.createMock(BeanAttributesTransformer.class);
      EasyMock.expect(transformer.transform(EasyMock.anyObject(Place.class)))
            .andAnswer(() -> ImmutableMap.<String, Object>of(Capability.ATTR_ID, ((Place) EasyMock.getCurrentArguments()[0]).getId().toString()))
            .anyTimes();
      EasyMock.replay(transformer);
      handler = new ListPlacesHandler(placeDao, assocDao, transformer);
   }

   private static PlaceAccessDescriptor access(Place place) {
      PlaceAccessDescriptor descriptor = new PlaceAccessDescriptor();
      descriptor.setPlaceId(place.getId().toString());
      return descriptor;
   }

   private PlatformMessage listPlaces(Address actor) {
      PlatformMessage.Builder builder = PlatformMessage
            .request(Address.platformService(account.getId(), AccountCapability.NAMESPACE))
            .from(Address.clientAddress("android", "1"))
            .withPayload(AccountCapability.ListPlacesRequest.instance());
      if(actor != null) {
         builder.withActor(actor);
      }
      // as it arrives off the bus
      return JSON.fromJson(JSON.toJson(builder.create()), PlatformMessage.class);
   }

   @SuppressWarnings("unchecked")
   private Set<String> placesIn(MessageBody response) {
      List<Map<String, Object>> places = (List<Map<String, Object>>) response.getAttributes().get(AccountCapability.ListPlacesResponse.ATTR_PLACES);
      return places.stream().map((p) -> (String) p.get(Capability.ATTR_ID)).collect(Collectors.toSet());
   }

   private static Set<String> ids(Place... places) {
      return Arrays.stream(places).map((p) -> p.getId().toString()).collect(Collectors.toSet());
   }

   private void expectAccountPlaces() {
      EasyMock.expect(placeDao.findByPlaceIDIn(account.getPlaceIDs())).andReturn(Arrays.asList(home, cabin));
   }

   @Test
   public void testOwnerGetsEveryPlaceOnTheAccount() {
      expectAccountPlaces();
      EasyMock.replay(placeDao, assocDao);

      MessageBody response = handler.handleRequest(account, listPlaces(Address.platformService(owner, "person")));

      assertEquals(ids(home, cabin), placesIn(response));
      EasyMock.verify(placeDao, assocDao);
   }

   @Test
   public void testGuestGetsOnlyTheirPlace() {
      // the guest also has access to a place on another account, which isn't this account's
      EasyMock.expect(assocDao.listPlaceAccessForPerson(guest)).andReturn(Arrays.asList(access(cabin), access(otherAccountsPlace)));
      EasyMock.expect(placeDao.findByPlaceIDIn(new HashSet<>(Arrays.asList(cabin.getId(), otherAccountsPlace.getId()))))
            .andReturn(Arrays.asList(cabin, otherAccountsPlace));
      EasyMock.replay(placeDao, assocDao);

      MessageBody response = handler.handleRequest(account, listPlaces(Address.platformService(guest, "person")));

      assertEquals(ids(cabin), placesIn(response));
      EasyMock.verify(placeDao, assocDao);
   }

   @Test
   public void testPersonWithNoPlaceOnTheAccountGetsNone() {
      EasyMock.expect(assocDao.listPlaceAccessForPerson(guest)).andReturn(Collections.<PlaceAccessDescriptor>emptyList());
      EasyMock.expect(placeDao.findByPlaceIDIn(Collections.<UUID>emptySet())).andReturn(Collections.<Place>emptyList());
      EasyMock.replay(placeDao, assocDao);

      MessageBody response = handler.handleRequest(account, listPlaces(Address.platformService(guest, "person")));

      assertEquals(Collections.emptySet(), placesIn(response));
      EasyMock.verify(placeDao, assocDao);
   }

   @Test
   public void testApiKeyActorGetsTheAccountsPlaces() {
      // api-bridge narrows the response to the key's own place (ApiPlatformBusListener)
      expectAccountPlaces();
      EasyMock.replay(placeDao, assocDao);

      MessageBody response = handler.handleRequest(account, listPlaces(Address.platformService(UUID.randomUUID(), ApiKeyPrincipal.ACTOR_NAMESPACE)));

      assertEquals(ids(home, cabin), placesIn(response));
      EasyMock.verify(placeDao, assocDao);
   }

   @Test
   public void testInternalCallerWithoutAnActorGetsTheAccountsPlaces() {
      expectAccountPlaces();
      EasyMock.replay(placeDao, assocDao);

      MessageBody response = handler.handleRequest(account, listPlaces(null));

      assertEquals(ids(home, cabin), placesIn(response));
      EasyMock.verify(placeDao, assocDao);
   }
}
