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
package com.iris.client.server.session;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.client.bounce.BounceConfig;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.model.Place;
import com.iris.security.authz.AuthorizationGrant;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;

@Mocks({ PersonDAO.class, PlaceDAO.class })
public class TestHandshakeSessionListener extends IrisMockTestCase{

	@Inject
	private PersonDAO personDAO;
	@Inject
   private PlaceDAO placeDAO;

	private HandshakeSessionListener listener = null;
	
	@Override
	@Before
	public void setUp() throws Exception {		
		super.setUp();
		listener = new HandshakeSessionListener(new BounceConfig(), personDAO, placeDAO);
	}

	@Test
	public void testLastConsentBeforeMinConsent() {
		Date lastConsentDate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(60));
		Date minConsentDate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30));
		//both in the past, lastConsentDate is older than minConsentDate
		assertTrue(listener.isRequiresConsent(lastConsentDate, minConsentDate));
	}
	
	
	@Test
	public void testLastConsentAfterMinConsent() {
		Date lastConsentDate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(60));
		Date minConsentDate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90));
		//both in the past, lastConsentDate is later than minConsentDate
		assertFalse(listener.isRequiresConsent(lastConsentDate, minConsentDate));
	}

	@Test
	public void testMinConsentInTheFuture() {
		Date lastConsentDate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(60));
		Date minConsentDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30));
		//minConsentDate is in the future, lastConsentDate is later than minConsentDate
		assertFalse(listener.isRequiresConsent(lastConsentDate, minConsentDate));
	}
	
	@Test
	public void testLastConsentNotSet() {
		
		Date minConsentDate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30));
		//lastConsentDate is null
		assertTrue(listener.isRequiresConsent(null, minConsentDate));		
		
		minConsentDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(60));
		assertTrue(listener.isRequiresConsent(null, minConsentDate));
	}
	
	private AuthorizationGrant createAuthorizationGrant(Place place) {
	   AuthorizationGrant g  = new AuthorizationGrant();
      g.setPlaceId(place.getId());
      g.setAccountId(place.getAccount());
      g.setPlaceName(place.getName());
      g.setAccountOwner(true);
      g.setEntityId(UUID.randomUUID());
      return g;
	}
}

