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
package com.iris.platform.scene.resolver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.iris.capability.definition.AttributeTypes;
import com.iris.common.rule.action.Action;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.action.SendAction;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.CameraCapability;
import com.iris.messages.model.Model;
import com.iris.messages.service.VideoService;
import com.iris.messages.type.ActionSelector;
import com.iris.model.predicate.Predicates;

public class CameraResolver extends BaseResolver {
   public static final int MIN_DURATION = 30;
   public static final int MAX_DURATION = (int) TimeUnit.MINUTES.toSeconds(20);
   
   private final Predicate<Model> isAccount =
         Predicates.isA(AccountCapability.NAMESPACE);
   private final Predicate<Model> isCamera =
         Predicates.isA(CameraCapability.NAMESPACE);
   private final Address videoServiceAddress = Address.platformService(VideoService.NAMESPACE);

   public CameraResolver() {
      super("camera", "Record Video", "camera");
   }

   @Override
   protected List<ActionSelector> resolve(ActionContext context, Model model) {
      if(!isCamera.apply(model)) {
         return ImmutableList.of();
      }
      
      ActionSelector selector = new ActionSelector();
      selector.setName("duration");
      selector.setType(ActionSelector.TYPE_DURATION);
      selector.setUnit(ActionSelector.UNIT_SEC);
      selector.setMin(30);
      selector.setMax((int) TimeUnit.MINUTES.toSeconds(10));
      return ImmutableList.of(selector);
   }

   @Override
   public Action generate(ActionContext context, Address target, Map<String, Object> variables) {
      Object duration = (Object) variables.get("duration");
      Preconditions.checkNotNull(duration, "duration is required");
      int durationSec = AttributeTypes.coerceInt(duration);
      Preconditions.checkArgument(durationSec >= MIN_DURATION && durationSec <= MAX_DURATION, "Invalid duration");
      
      MessageBody payload =
         VideoService.StartRecordingRequest
            .builder()
            .withAccountId(getAccountId(context))
            .withCameraAddress(target.getRepresentation())
            .withDuration(durationSec)
            .withPlaceId(context.getPlaceId().toString())
            .withStream(false)
            .build();
         
      return new SendAction(payload.getMessageType(), Functions.constant(videoServiceAddress), payload.getAttributes());
   }

   private String getAccountId(ActionContext context) {
      // this will throw an NPE if there is no account in the context -- that is intended behavior
      return Iterables.getFirst(Iterables.filter(context.getModels(), isAccount), null).getId();
   }

}

