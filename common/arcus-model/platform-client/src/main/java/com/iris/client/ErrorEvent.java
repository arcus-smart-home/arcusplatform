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
package com.iris.client;



/**
 *
 */
public class ErrorEvent extends ClientEvent {
   public static final String NAME = "Error";

	public static final String ATTR_CODE = "code";
	public static final String ATTR_MESSAGE = "message";

   public ErrorEvent(String sourceAddress) {
      super(NAME, sourceAddress);
   }

   public ErrorEvent(String sourceAddress, java.util.Map<String, Object> attributes) {
      super(NAME, sourceAddress, attributes);
   }

   public ErrorEvent(com.iris.client.ClientEvent copy) {
      super(NAME, copy.getSourceAddress(), new java.util.HashMap<String, Object>(copy.getAttributes()));
   }

	public String getCode() {
		return (String) getAttributes().get(ATTR_CODE);
	}

	public String getMessage() {
		return (String) getAttributes().get(ATTR_MESSAGE);
	}

   @Override
   public String toString() {
      return "ErrorEvent [code=" + getCode() + ", message=" + getMessage() + "]";
   }
}

