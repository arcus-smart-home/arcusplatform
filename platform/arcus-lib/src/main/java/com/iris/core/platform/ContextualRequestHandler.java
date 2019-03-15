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
package com.iris.core.platform;

import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.errors.Errors;

public interface ContextualRequestHandler<M,T> {
   String getMessageType();
   MessageBody handleRequest(T context, M msg);

   default MessageBody handleStaticRequest(M msg) {
      // static request, but this method isn't implemented, so its not supported
      return ErrorEvent.fromCode(Errors.CODE_UNSUPPORTED_TYPE, "This request is not supported at this destination, add an id to the destination");
   }

}

