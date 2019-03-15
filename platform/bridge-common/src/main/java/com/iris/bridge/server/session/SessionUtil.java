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
package com.iris.bridge.server.session;

import java.util.UUID;

import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Place;

public class SessionUtil {
	
	public static void setPlace(Place curPlace, Session session) {		
		if(curPlace == null) {
      	throw new ErrorEventException(Errors.CODE_INVALID_REQUEST, "The place does not exist");
      }
		setPlace(curPlace.getId(), session);
	}
	
	public static void setPlace(String placeId, Session session) {				
		session.setActivePlace(placeId);				
	}
	
	public static void setPlace(UUID placeId, Session session) {		
		if(placeId == null) {
      	throw new ErrorEventException(Errors.CODE_INVALID_REQUEST, "The place does not exist");
      }
		setPlace(placeId.toString(), session);				
	}
	
	public static void clearPlace(Session session) {
		session.setActivePlace(null);
	}

}

