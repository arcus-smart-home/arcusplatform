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
package com.iris.messages;

import com.google.common.collect.ImmutableMap;


/**
 *
 */
public class ErrorEvent extends MessageBody {

   private static final long serialVersionUID = -5289018988227349316L;

   public static final String MESSAGE_TYPE = "Error";

	public static final String CODE_ATTR = "code";
	public static final String MESSAGE_ATTR = "message";

	/**
	 * @deprecated Use Errors#fromException instead
	 * @param cause
	 * @return
	 */
	public static ErrorEvent fromException(Throwable cause) {
		return new ErrorEvent(cause.getClass().getSimpleName(), cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage());
	}

	/**
	 * @deprecated Use Errors#fromCode instead
	 * @param code
	 * @param message
	 * @return
	 */
	public static ErrorEvent fromCode(String code, String message) {
		return new ErrorEvent(code, message);
	}

	ErrorEvent(String code, String message) {
	   super(MESSAGE_TYPE, ImmutableMap.<String,Object>of(CODE_ATTR, code, MESSAGE_ATTR, message));
	}

	public String getCode() {
		return (String) getAttributes().get(CODE_ATTR);
	}

	public String getMessage() {
		return (String) getAttributes().get(MESSAGE_ATTR);
	}

   @Override
   public String toString() {
      return "ErrorEvent [code=" + getCode() + ", message=" + getMessage() + "]";
   }
}

