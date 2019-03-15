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
package com.iris.alexa.shs.handlers;

import java.util.Optional;
import java.util.UUID;

import com.google.common.util.concurrent.ListenableFuture;
import com.iris.alexa.message.AlexaMessage;

public interface SmartHomeSkillHandler {

   boolean supports(AlexaMessage message);
   ListenableFuture<AlexaMessage> handle(AlexaMessage message, UUID placeId);
   AlexaMessage transformException(AlexaMessage message, Throwable e);
   Optional<String> extractOAuthToken(AlexaMessage message);

}

