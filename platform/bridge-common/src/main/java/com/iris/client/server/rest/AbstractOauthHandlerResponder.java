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
package com.iris.client.server.rest;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.bridge.server.http.HttpException;
import com.iris.bridge.server.http.Responder;
import com.iris.core.dao.PlaceDAO;
import com.iris.messages.model.Place;

import io.netty.handler.codec.http.HttpResponseStatus;

public abstract class AbstractOauthHandlerResponder implements Responder {
	private static final Logger logger = LoggerFactory.getLogger(AbstractOauthHandlerResponder.class);
	protected static final String STATE_PARAM = "state";
	protected static final String CODE_PARAM = "code";	
	protected static final String TEMPLATE_PARAM = "template";	
	private final PlaceDAO placeDao;
	
	public AbstractOauthHandlerResponder(PlaceDAO placeDao) {
		this.placeDao = placeDao;
	}
	
	protected Place loadPlace(UUID placeId) throws HttpException {
      Place p = placeDao.findById(placeId);
      if(p == null) {
         logger.warn("attempt to authorize for a non-existent place {}", placeId);
         throw new HttpException(HttpResponseStatus.BAD_REQUEST.code());
      }
      return p;
   }
	
	protected UUID getAccountId(UUID placeId) throws HttpException {    	
       Place p = loadPlace(placeId);
       return p.getAccount();
    }
}

