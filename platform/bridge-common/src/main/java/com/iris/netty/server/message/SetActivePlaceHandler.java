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

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.iris.netty.server.message.LogHandler.log;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.bridge.server.session.Session;
import com.iris.bridge.server.session.SessionUtil;
import com.iris.core.dao.PlaceDAO;
import com.iris.core.dao.PreferencesDAO;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.service.SessionService;
import com.iris.messages.service.SessionService.SetActivePlaceRequest;
import com.iris.security.authz.AuthorizationContext;
import com.iris.security.authz.AuthzUtil;

@Singleton
public class SetActivePlaceHandler extends BaseClientRequestHandler
{
   public static final String NAME_EXECUTOR = "executor.setactiveplace";

   private final PreferencesDAO preferencesDao;
   private final PlaceDAO placeDao;

   @Inject
   public SetActivePlaceHandler(PreferencesDAO preferencesDao, PlaceDAO placeDao, @Named(NAME_EXECUTOR) Executor executor)
   {
      // FIXME: Temporarily disabling background thread pool due to issues observed in devtest
      //super(executor);
      super(directExecutor());

      this.placeDao = placeDao;
      this.preferencesDao = preferencesDao;
   }

   @Override
   public String getRequestType()
   {
      return SetActivePlaceRequest.NAME;
   }

   @Override
   protected MessageBody doHandle(ClientMessage request, Session session)
   {
      MessageBody requestBody = request.getPayload();
      String placeId = SessionService.SetActivePlaceRequest.getPlaceId(requestBody);
      if (placeId == null)
      {
         throw new ErrorEventException("missing.attribute", "The placeId is a required attribute.");
      }

      UUID placeUuid = UUID.fromString(placeId);

      // make sure that the logged in person has at least some permission on the place they are trying to activate
      AuthorizationContext authContext =
         session.getAuthorizationContext(true/* be sure to reload the auth context at this point */);
      if (authContext.getNonInstancePermissions(placeUuid).isEmpty()
         && authContext.getInstancePermissions(placeUuid).isEmpty())
      {
         throw new ErrorEventException(AuthzUtil.createUnauthorizedEvent());
      }

      String oldPlace = session.getActivePlace();
      SessionUtil.setPlace(placeUuid, session);
      // log after the switch so the line carries the new place, not the one being left
      boolean switched = oldPlace != null && !StringUtils.equals(oldPlace, placeId);
      log(session, "session", "place.setactive", switched ? "Set active place (was " + oldPlace + ")" : "Set active place");

      UUID personUuid = session.getClient().getPrincipalId();
      Map<String, Object> prefs = preferencesDao.findById(personUuid, placeUuid);

      return SessionService.SetActivePlaceResponse.builder()
         .withPlaceId(placeId)
         .withPreferences(prefs)
         .build();
   }

   @Override
   protected Address address()
   {
   	return SessionService.ADDRESS;
   }
}

