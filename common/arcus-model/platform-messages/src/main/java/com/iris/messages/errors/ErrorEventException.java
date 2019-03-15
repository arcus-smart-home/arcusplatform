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
package com.iris.messages.errors;

import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;

/**
 * Represents an {@link Exception} that should escaped all the
 * way to the top and be relayed externally to the requesting client
 * as an {@link ErrorEvent}.
 */
public class ErrorEventException extends RuntimeException {
   private ErrorEvent event;

   
   public ErrorEventException(String code, String description) {
      this(ErrorEvent.fromCode(code, description), null);
   }
   
   public ErrorEventException(String code, String description, Throwable cause) {
      this(ErrorEvent.fromCode(code, description), cause);
   }

   public ErrorEventException(ErrorEvent event) {
      this(event, null);
   }

   public ErrorEventException(ErrorEvent event, Throwable cause) {
      super(event.getCode() + ": " + event.getMessage(), cause);
      this.event = event;
   }

   public ErrorEventException(MessageBody body) {
      this(body, null);
   }

   public ErrorEventException(MessageBody body, Throwable cause) {
      this(Errors.fromCode((String) body.getAttributes().get(ErrorEvent.CODE_ATTR), (String) body.getAttributes().get(ErrorEvent.MESSAGE_ATTR)), cause);
   }

   public String getCode() {
      return this.event.getCode();
   }
   
   public String getDescription() {
      return this.event.getMessage();
   }

   public ErrorEvent toErrorEvent() {
      return this.event;
   }
}

