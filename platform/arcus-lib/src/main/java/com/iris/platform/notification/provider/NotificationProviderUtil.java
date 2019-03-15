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
package com.iris.platform.notification.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.iris.core.dao.AccountDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.model.Account;
import com.iris.messages.model.BaseEntity;
import com.iris.messages.model.Person;
import com.iris.messages.model.Place;
import com.iris.platform.notification.Notification;

public class NotificationProviderUtil {
	public static final String PLACE_KEY="_place";
	public static final String OWNER_KEY="_owner";
	public static final String RECIPIENT_KEY="_person";
	
	public static Map<String, BaseEntity<?,?>> addAdditionalParamsAndReturnRecipient(PlaceDAO placeDao, 
			PersonDAO personDao, AccountDAO accountDao, Notification notification) {		
		return addAdditionalParamsAndReturnRecipient(placeDao, personDao, accountDao, notification.getPlaceId(), notification.getPersonId());
	}
	
	public static Map<String, BaseEntity<?,?>> addAdditionalParamsAndReturnRecipient(PlaceDAO placeDao, 
			PersonDAO personDao, AccountDAO accountDao, String placeIdString, String personIdString) {
		// The personId should always be legit.
		UUID personId = UUID.fromString(personIdString);
		
		// The placeId may not always be legit.
		UUID placeId = null;
		try {
			placeId = StringUtils.isEmpty(placeIdString) ? null : UUID.fromString(placeIdString);
		}
		catch(Throwable ex) {
			placeId = null;
		}
		return addAdditionalParamsAndReturnRecipient(placeDao, personDao, accountDao, placeId, personId);
	}
	
	public static Map<String, BaseEntity<?,?>> addAdditionalParamsAndReturnRecipient(PlaceDAO placeDao, 
			PersonDAO personDao, AccountDAO accountDao, UUID placeId, UUID personId) {		
		Map<String, BaseEntity<?,?>> params = new HashMap<String, BaseEntity<?,?>>(3);
		Person recipient = personDao.findById(personId);
		params.put(RECIPIENT_KEY, recipient);
		if(placeId != null) {
			Place curPlace = placeDao.findById(placeId);
			if(curPlace != null) {
				params.put(PLACE_KEY, curPlace);
				if( curPlace.getAccount() != null) {
					Account account = accountDao.findById(curPlace.getAccount());
					if(account != null && account.getOwner() != null) {
						params.put(OWNER_KEY, personDao.findById(account.getOwner()));
					}				
				}			
			}
		}
		return params;
	}
	
	public static Person getPersonFromParams(Map<String, BaseEntity<?,?>> params) {
		return (Person) params.get(RECIPIENT_KEY);
	}
}

