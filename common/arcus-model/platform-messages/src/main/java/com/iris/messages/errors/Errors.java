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

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.iris.messages.ErrorEvent;
import com.iris.messages.Message;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.services.PlatformConstants;


/**
 * 
 */
public abstract class Errors {
   private static final Logger logger = LoggerFactory.getLogger(Errors.class);
   
   // TODO all these codes need to be available to the clients as well
   public static final String CODE_GENERIC                  = "error";
   public static final String CODE_INVALID_REQUEST          = "request.invalid";
   public static final String CODE_MISSING_PARAM            = "request.param.missing";
   public static final String CODE_INVALID_PARAM            = "request.param.invalid";
   public static final String CODE_TIMEOUT                  = "request.timeout";
   public static final String CODE_CANCELLED                = "request.cancelled";
   public static final String CODE_NOT_FOUND                = "request.destination.notfound";
   
   // TODO remove errors from PlatformConstants
   public static final String CODE_UNSUPPORTED_TYPE         = PlatformConstants.CODE_UNSUPPORTED_MESSAGE_TYPE;
   public static final String CODE_UNSUPPORTED_DESTINATION  = PlatformConstants.CODE_UNSUPPORTED_DESTINATION;
   public static final String CODE_UNSUPPORTED_ATTRIBUTE    = "UnsupportedAttribute";
   
   // TODO should these be put in more specific error classes?
   public static final String CODE_HUB_OFFLINE              = "UnknownDevice";

   // TODO should these be put in more specific error classes?
   public static final String CODE_DRIVER_NOT_SUPPORTED     = "DriverNotSupported";

   public static final String CODE_SERVICE_UNAVAILABLE      = "service.unavailable";

   private static final ErrorEvent GENERIC = ErrorEvent.fromCode(CODE_GENERIC, "Oops. I'm not sure what happened, but you might want to try again.");
   private static final ErrorEvent INVALID_REQUEST = ErrorEvent.fromCode(CODE_INVALID_REQUEST, "Bad request");
   private static final ErrorEvent TIMEOUT = ErrorEvent.fromCode(CODE_TIMEOUT, "Request timed out");
   private static final ErrorEvent CANCELLED = ErrorEvent.fromCode(CODE_CANCELLED, "Request cancelled");
   private static final ErrorEvent HUB_OFFLINE = ErrorEvent.fromCode("UnknownDevice", "Hub is not currently connected");
   private static final ErrorEvent SERVICE_UNAVAILABLE = ErrorEvent.fromCode(CODE_SERVICE_UNAVAILABLE, "Service is currently unavailable");

   public static ErrorEvent fromException(ErrorEventException exception) {
      if(exception == null) {
         logger.warn("Attempted to translate a null exception to an ErrorEvent, see stack trace", new NullPointerException());
         return Errors.genericError();
      }
      ErrorEvent event = exception.toErrorEvent();
      logger.debug("Returning error {}", event, exception);
      return event;
   }
   
   public static ErrorEvent fromException(Throwable cause) {
      if(cause == null) {
         logger.warn("Attempted to translate a null exception to an ErrorEvent, see stack trace", new NullPointerException());
         return Errors.genericError();
      }
      if(cause instanceof ErrorEventException) {
         return fromException((ErrorEventException) cause);
      }
      logger.debug("Translating exception", cause);
      // TODO replace this with unknownError()
      return ErrorEvent.fromCode(cause.getClass().getSimpleName(), cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage());
   }

   public static ErrorEvent fromCode(String code, String message) {
      return ErrorEvent.fromCode(code, message);
   }

   public static ErrorEvent genericError() {
      return GENERIC;
   }
   
   public static ErrorEvent genericError(String message) {
      return StringUtils.isEmpty(message) ? GENERIC : Errors.fromCode(CODE_GENERIC, message);
   }
   
   public static ErrorEvent requestTimeout() {
      return TIMEOUT;
   }
   
   public static ErrorEvent invalidRequest() {
      return INVALID_REQUEST;
   }

   public static ErrorEvent invalidRequest(String message) {
      return StringUtils.isEmpty(message) ? INVALID_REQUEST : Errors.fromCode(CODE_INVALID_REQUEST, message);
   }
   
   public static ErrorEvent missingParam(String parameterName) {
      return ErrorEvent.fromCode(CODE_MISSING_PARAM, "Missing required parameter [" + parameterName  + "]");
   }

   public static ErrorEvent invalidParam(String parameterName) {
      return ErrorEvent.fromCode(CODE_INVALID_PARAM, "Invalid value for parameter [" + parameterName  + "]");
   }

   public static ErrorEvent unsupportedAttribute(String name) {
      return ErrorEvent.fromCode(CODE_UNSUPPORTED_ATTRIBUTE, "Unable to set value for attribute " + name);
   }

   public static ErrorEvent requestCancelled() {
      return CANCELLED;
   }
   
   public static ErrorEvent unsupportedMessageType(String type) {
      return ErrorEvent.fromCode(CODE_UNSUPPORTED_TYPE, "Message type [" + type + "] is not supported");
   }
   
   public static ErrorEvent unsuppportedAddress(Address destination) {
      String representation = destination == null ? Address.broadcastAddress().getRepresentation() : destination.getRepresentation();
      return ErrorEvent.fromCode(CODE_UNSUPPORTED_DESTINATION, "No service named [" + representation + "] exists");
   }

   public static ErrorEvent notFound(Address destination) {
      String representation = destination == null ? Address.broadcastAddress().getRepresentation() : destination.getRepresentation();
      return ErrorEvent.fromCode(CODE_NOT_FOUND, "No object addressed [" + representation + "] was found");
   }
   
   /**
    * Asserts that that the condition is true. If it is not true an {@link ErrorEventException} is
    * thrown.
    *
    * @param expression - Condition to test
    */
   public static void assertTrue(boolean expression, ErrorEvent eventError) throws ErrorEventException {
      if (!expression) {
         throw new ErrorEventException(eventError);
      }
   }
   
   /**
    * Asserts that that the condition is false. If true an {@link ErrorEventException} is
    * thrown.
    *
    * @param expression - Condition to test
    */ 
   public static void assertFalse(boolean expression, ErrorEvent eventError) throws ErrorEventException {
      if (expression) {
         throw new ErrorEventException(eventError);
      }
   }   
   
   public static void assertValidRequest(boolean condition, String message) {
      if(!condition) {
         throw new InvalidRequestException(message);
      }
   }
   
   public static void assertRequiredParam(Object value, String parameterName) throws ErrorEventException {
      if(value == null) {
         throw new MissingParameterException(parameterName);
      }
   }
   
   public static void assertPlaceMatches(Message message, UUID placeId) throws UnauthorizedRequestException {
      Preconditions.checkNotNull(placeId);

      assertPlaceMatches(message, placeId.toString());
   }

   public static void assertPlaceMatches(Message message, String placeId) throws UnauthorizedRequestException {
      Preconditions.checkNotNull(placeId);
      
      String placeHeader = message.getPlaceId();
      // we may change this in the future, but for now no place header means
      // you can talk to any place
      if(placeHeader == null) {
         return;
      }
      if(!placeId.equals(placeHeader)) {
         throw new UnauthorizedRequestException(message.getDestination(), "Unauthorized send from session associated with place " + placeHeader + " to " + placeId);
      }
   }

   public static MessageBody hubOffline() {
      return HUB_OFFLINE;
   }

   public static MessageBody serviceUnavailable() {
      return SERVICE_UNAVAILABLE;
   }

   public static void assertFound(Object value, Address address) {
		if(value == null) {
			throw new NotFoundException(address);
		}
		
   }

}


