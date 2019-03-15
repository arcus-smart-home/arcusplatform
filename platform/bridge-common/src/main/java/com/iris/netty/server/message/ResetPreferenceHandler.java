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

import static com.iris.messages.service.SessionService.SetPreferencesResponse.CODE_PLACE_ACTIVE_NOTSET;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.UUID;
import java.util.concurrent.Executor;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.bridge.server.session.Session;
import com.iris.core.dao.PreferencesDAO;
import com.iris.messages.ClientMessage;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.service.SessionService;
import com.iris.messages.service.SessionService.ResetPreferenceRequest;
import com.iris.messages.service.SessionService.ResetPreferenceResponse;

@Singleton
public class ResetPreferenceHandler extends BaseClientRequestHandler
{
   public static final String NAME_EXECUTOR = "executor.resetpreference";

   private static final ErrorEvent ACTIVE_PLACE_NOT_SET =
      Errors.fromCode(CODE_PLACE_ACTIVE_NOTSET, "No active place is currently set");

   private final PreferencesDAO preferencesDao;

   @Inject
   public ResetPreferenceHandler(PreferencesDAO preferencesDao, @Named(NAME_EXECUTOR) Executor executor)
   {
      super(executor);

      this.preferencesDao = preferencesDao;
   }

   @Override
   public String getRequestType()
   {
      return ResetPreferenceRequest.NAME;
   }

   @Override
   protected MessageBody doHandle(ClientMessage request, Session session)
   {
      MessageBody requestBody = request.getPayload();

      UUID personUuid = session.getClient().getPrincipalId();

      String placeId = session.getActivePlace();
      if (isEmpty(placeId))
      {
         throw new ErrorEventException(ACTIVE_PLACE_NOT_SET);
      }
      UUID placeUuid = UUID.fromString(placeId);

      String prefKey = ResetPreferenceRequest.getPrefKey(requestBody);

      preferencesDao.deletePref(personUuid, placeUuid, prefKey);

      return ResetPreferenceResponse.instance();
   }

   @Override
   protected Address address()
   {
   	return SessionService.ADDRESS;
   }
}

