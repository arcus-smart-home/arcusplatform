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
/**
 *
 */
package com.iris.client.impl;


/**
 *
 */
public class MessageConstants {
   // messages that aren't part of the capability structure
   public static final String MSG_ERROR = "Error";
   public static final String MSG_SESSION_CREATED = "SessionCreated";
   public static final String MSG_EMPTY_MESSAGE = "EmptyMessage";

   public static final String MSG_PING_REQUEST = "platform:PingRequest";
   public static final String MSG_PONG_RESPONSE = "platform:PongResponse";

   public static final String MSG_SETACTIVEPLACE = "sess:SetActivePlace";
   public static final String MSG_SETACTIVEPLACE_RESPONSE = "sess:SetActivePlaceResponse";
}

