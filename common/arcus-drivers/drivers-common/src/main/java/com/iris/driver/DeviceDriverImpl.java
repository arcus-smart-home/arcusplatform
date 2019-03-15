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
package com.iris.driver;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.iris.capability.definition.AttributeDefinition;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.config.DriverConfigurationStateMachine;
import com.iris.driver.event.DeviceAssociatedEvent;
import com.iris.driver.event.DeviceConnectedEvent;
import com.iris.driver.event.DeviceDisassociatedEvent;
import com.iris.driver.event.DeviceDisconnectedEvent;
import com.iris.driver.event.DriverEvent;
import com.iris.driver.handler.AttributeBindingHandler;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.driver.reflex.ReflexDriver;
import com.iris.driver.reflex.ReflexDriverContext;
import com.iris.driver.reflex.ReflexRunMode;
import com.iris.messages.ErrorEvent;
import com.iris.messages.Message;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.DriverId;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.control.ControlProtocol;
import com.iris.protocol.control.DeviceOfflineEvent;
import com.iris.protocol.control.DeviceOnlineEvent;
import com.iris.protocol.reflex.ReflexProtocol;

/**
 *
 */
class DeviceDriverImpl implements DeviceDriver {
   static final AttributeKey<String> DRIVER_NAME = 
         AttributeKey.create(DeviceAdvancedCapability.ATTR_DRIVERNAME, String.class);
   static final AttributeKey<String> DRIVER_VERSION = 
         AttributeKey.create(DeviceAdvancedCapability.ATTR_DRIVERVERSION, String.class);
   static final AttributeKey<String> DRIVER_HASH = 
         AttributeKey.create(DeviceAdvancedCapability.ATTR_DRIVERHASH, String.class);
   static final AttributeKey<String> DRIVER_COMMIT = 
         AttributeKey.create(DeviceAdvancedCapability.ATTR_DRIVERCOMMIT, String.class);
   static final AttributeKey<String> DEVICE_CONNECTION =
         AttributeKey.create(DeviceConnectionCapability.ATTR_STATE, String.class);
   
   private final Logger driverLogger;

   private final DeviceDriverDefinition definition;
   private final Predicate<AttributeMap> matcher;
   private final AttributeMap baseAttributes;
   private final ContextualEventHandler<DriverEvent> driverEventHandler;
   // TODO split apart protocol and platform handlers?
   private final ContextualEventHandler<Message> messageHandler;
   private final AttributeBindingHandler attrBindingHandler;
   private final ReflexDriver reflexes;
   private final boolean runReflexLifecycleActions;

   DeviceDriverImpl(
         DeviceDriverDefinition definition,
         Predicate<AttributeMap> matcher,
         AttributeMap baseAttributes,
         ContextualEventHandler<DriverEvent> driverEventHandler,
         ContextualEventHandler<Message> messageHandler,
         AttributeBindingHandler attrBindingHandler,
         ReflexDriver reflexes,
         boolean runReflexLifecycleActions
   ) {
      this.driverLogger = LoggerFactory.getLogger("driver." + definition.getName());
      this.definition = definition;
      this.matcher = matcher;
      this.baseAttributes = baseAttributes;
      this.driverEventHandler = driverEventHandler;
      this.messageHandler = messageHandler;
      this.attrBindingHandler = attrBindingHandler;
      this.reflexes = reflexes;
      this.runReflexLifecycleActions = runReflexLifecycleActions;
   }

   @Override
   public DriverId getDriverId() {
      return definition.getId();
   }

   /* (non-Javadoc)
    * @see com.iris.core.driver.DeviceDriver#supportsDevice(com.iris.messages.model.Device, com.iris.device.attributes.AttributeMap)
    */
   @Override
   public boolean supports(AttributeMap attributes) {
      return matcher.apply(attributes);
   }

   /* (non-Javadoc)
    * @see com.iris.core.driver.DeviceDriver#getDefinition()
    */
   @Override
   public DeviceDriverDefinition getDefinition() {
      return definition;
   }

   @Override
   public AttributeMap getBaseAttributes() {
      return baseAttributes;
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DeviceDriver#onRestored(com.iris.driver.DeviceDriverContext)
    */
   @Override
   public void onRestored(DeviceDriverContext context) {
      // if there is no change, this should be a no-op
      context.setAttributeValue(DRIVER_HASH, definition.getHash());
      context.setAttributeValue(DRIVER_COMMIT, definition.getCommit());

      DriverConfigurationStateMachine configuration = definition.getConfiguration();
      if (configuration != null) {
         configuration.onDriverRestored(context);
      }
      // don't commit here for now, wait for the connect event, otherwise we get multiple value changes
      // and break lots of test cases
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DeviceDriver#onUpgraded(com.iris.messages.model.DriverId, com.iris.driver.DeviceDriverContext)
    */
   @Override
   public void onUpgraded(DriverEvent event, DriverId previous, DeviceDriverContext context) throws Exception {
      context.setAttributeValue(DRIVER_NAME, definition.getName());
      context.setAttributeValue(DRIVER_VERSION, definition.getVersion().getRepresentation());
      context.setAttributeValue(DRIVER_HASH, definition.getHash());
      context.setAttributeValue(DRIVER_COMMIT, definition.getCommit());
      handleDriverEvent(event, context);
   }

   /* (non-Javadoc)
    * @see com.iris.driver.DeviceDriver#onSuspended(com.iris.driver.DeviceDriverContext)
    */
   @Override
   public void onSuspended(DeviceDriverContext context) {
      // no-op
   }

   @Override
	public void onAttributesUpdated(DeviceDriverContext context, Map<AttributeKey<?>,Object> attributes, Integer reflexVersion, boolean isDeviceMessage) {
      boolean wasConnected = context.isConnected();
      try {
         if (isDeviceMessage) {
            context.setLastProtocolMessageTimestamp(new Date().getTime());
            if(!wasConnected) {
               driverLogger.debug("Received protocol message for offline device, marking device as connected");
               context.setConnected();
               connected(context, reflexVersion);
               wasConnected = true;
            }
         }

         for (Map.Entry<AttributeKey<?>,Object> entry : attributes.entrySet()) {
            context.setAttributeValue((AttributeKey<Object>)entry.getKey(), entry.getValue());
         }
      } finally {
         commit(context, wasConnected, reflexVersion);
      }
   }

   @Override
   public void handleDriverEvent(DriverEvent event, DeviceDriverContext context) throws Exception {
      if(context.isDeleted()) {
         context.getLogger().warn("Dropping event [{}] because driver has been destroyed", event);
         return;
      }
      if(context.isTombstoned()) {
         context.getLogger().warn("Dropping event [{}] because device [{}] is tombstoned", event, context.getDriverAddress());
         return;
      }

      try {
         boolean wasConnected = context.isConnected();
         driverEventHandler.handleEvent(context, event);

         Integer reflexVersion = null;
         if (reflexes != null && runReflexLifecycleActions) {
            try {
               if (event instanceof DeviceAssociatedEvent) {
                  DeviceAssociatedEvent assoc = (DeviceAssociatedEvent)event;
                  Map<String,Object> assocAttrs = assoc.getAttributes();
                  reflexVersion = assocAttrs != null ? (Integer)assocAttrs.get("reflexVersion") : null; 
                  if (reflexVersion != null) {
                     reflexes.fireOnAdded(context.getReflexContext(), reflexVersion);
                  }
               } else if (event instanceof DeviceConnectedEvent) {
                  DeviceConnectedEvent conn = (DeviceConnectedEvent)event;
                  reflexVersion = conn.getReflexVersion();
                  if (reflexVersion != null) {
                     reflexes.fireOnConnected(context.getReflexContext(), reflexVersion);
                  }
               } else if (event instanceof DeviceDisconnectedEvent) {
                  DeviceDisconnectedEvent disconn = (DeviceDisconnectedEvent)event;
                  reflexVersion = disconn.getReflexVersion();
                  if (reflexVersion != null) {
                     reflexes.fireOnDisconnected(context.getReflexContext(), reflexVersion);
                  }
               }
            } catch (Exception ex) {
               context.getLogger().warn("failed to process lifecycle event with reflexes", ex);
            }
         }

         switch (event.getActionAfterHandled()) {
         case COMMIT:
            commit(context, wasConnected, reflexVersion);
            break;

         case INITIALIZE_BINDINGS:
            attrBindingHandler.processInitialAttributes(context, new Date());
            break;

         case NONE:
            break;

         default:
            driverLogger.error("Event returned unrecognized action after handled");
            break;
         }
      }
      catch (Exception e) {
         driverLogger.warn("Error handling message [{}]", event, e);
         throw e;
      }
   }

   /* (non-Javadoc)
    * @see com.iris.core.driver.DeviceDriver#handleProtocolMessage(com.iris.protocol.ProtocolMessage, com.iris.core.driver.DeviceDriverContext)
    */
   @Override
   public void handleProtocolMessage(ProtocolMessage message, DeviceDriverContext context) {
      if(context.isDeleted()) {
         context.getLogger().warn("Dropping protocol message [{}] because driver has been destroyed", message);
         return;
      }
      if(context.isTombstoned()) {
         context.getLogger().warn("Dropping protocol message [{}] because device [{}] is tombstoned", message, context.getDriverAddress());
         return;
      }

      Integer ver = message.getReflexVersion();
      if (ver == null || definition.getReflexes().getMode() == ReflexRunMode.PLATFORM) {
         ver = ReflexDriver.V0;
      }

      boolean wasConnected = context.isConnected();
      // ---------------  ITWO-7101 Fix null connection state information ----------------
      if (
            !message.isError() && 
            ControlProtocol.NAMESPACE.equals(message.getMessageType()) &&
            context.getAttributeValue(DEVICE_CONNECTION) == null
      ) {
         MessageBody event = message.getValue(ControlProtocol.INSTANCE);
         if(DeviceOnlineEvent.ATTR_MESSAGE.equals(event.getMessageType())) {
            driverLogger.info("Syncing connection state to ONLINE for null connection state.");
            wasConnected = true;
            context.setConnected();
         }
         else if(DeviceOfflineEvent.ATTR_MESSAGE.equals(event.getMessageType())) {
            driverLogger.info("Syncing connection state to OFFLINE for null connection state.");
            wasConnected = false;
            context.setDisconnected();
         }
      }
      // ---------------------------------------------------------------------------------
      if (!message.isError() && !ControlProtocol.NAMESPACE.equals(message.getMessageType())) {
         context.setLastProtocolMessageTimestamp(new Date().getTime());
         if(!wasConnected) {
            driverLogger.debug("Received protocol message for offline device, marking device as connected");
            context.setConnected();
            connected(context, ver);
            wasConnected = true;
         }
      }

      if (reflexes != null && !ReflexProtocol.NAMESPACE.equals(message.getMessageType())) {
         ReflexDriverContext rctx = context.getReflexContext();
         if (rctx != null) {
            boolean handled = false;
            try {

               handled = reflexes.handle(rctx, message, ver);
            } catch (Exception e) {
               driverLogger.warn("Driver reflexes caused exception while processing protocol message [{}]", message, e);
            }

            if (handled) {
               driverLogger.trace("driver reflexes handled message message [{}]", message);
               commit(context, wasConnected, ver);
               return;
            }
         }
      }

      try {
         if(!messageHandler.handleEvent(context, message)) {
            // not every type of protocol message will be handled -- this is normal, but something you should know about
            driverLogger.debug("Unhandled protocol message [{}]", message);
         }
      }
      catch(Exception e) {
         driverLogger.warn("Error handling message [{}]", message, e);
         sendError(message, context, e);
      }
      commit(context, wasConnected, ver);
   }

   /* (non-Javadoc)
    * @see com.iris.core.driver.DeviceDriver#handlePlatformMessage(com.iris.messages.PlatformMessage, com.iris.core.driver.DeviceDriverContext)
    */
   @Override
   public void handlePlatformMessage(PlatformMessage message, DeviceDriverContext context) {
      context.setMessageContext(message);
      if(context.isDeleted()) {
         context.getLogger().warn("Dropping platform message [{}] because driver has been destroyed", message);
         sendError(message, context, DeviceErrors.deviceDeleted());
         return;
      }
      if(context.isTombstoned()) {
         context.getLogger().warn("Dropping platform message [{}] because device [{}] is tombstoned", message, context.getDriverAddress());
         sendError(message, context, Errors.notFound(context.getDriverAddress()));
         return;
      }

      boolean wasConnected = context.isConnected();
      if (reflexes != null) {
         ReflexDriverContext rctx = context.getReflexContext();
         if (rctx != null) {
            boolean handled = false;
            try {
               handled = reflexes.handle(rctx, message);
               if (handled) {
                  Set<String> setAttrsConsumed = rctx.getAndResetSetAttributesConsumed();
                  if (setAttrsConsumed != null && !setAttrsConsumed.isEmpty()) {
                     Map<String,Object> oldAttrs = message.getValue().getAttributes();
                     if (setAttrsConsumed.equals(oldAttrs.keySet())) {
                        // set attributes message fully consumed 
                        context.respondToPlatform(MessageBody.buildMessage(MessageConstants.MSG_EMPTY_MESSAGE));
                     } else {
                        // set attributes message not fully consumed
                        Map<String,Object> newAttrs = new HashMap<>();
                        for (Map.Entry<String,Object> oldAttr : oldAttrs.entrySet()) {
                           if (!setAttrsConsumed.contains(oldAttr.getKey())) {
                              newAttrs.put(oldAttr.getKey(), oldAttr.getValue());
                           }
                        }

                        handled = false;
                        message = PlatformMessage.builder(message)
                           .withPayload(MessageBody.buildMessage(message.getMessageType(), newAttrs))
                           .create();
                     }

                  }
               }
            } catch (Exception e) {
               driverLogger.warn("Driver reflexes caused exception while processing platform message [{}]", message, e);
            }

            if (handled) {
               driverLogger.trace("driver reflexes handled message message [{}]", message);

               MessageBody rsp = rctx.getAndResetResponse();
               if (rsp != null) {
                  context.respondToPlatform(rsp);
               }

               commit(context, wasConnected, null);
               return;
            }
         }
      }

      try {
         if(!messageHandler.handleEvent(context, message)) {
            sendError(message, context, "UnsupportedMessageType", "Driver was unable to handle the message");
         }
      }
      catch(Exception e) {
         driverLogger.warn("Error handling message [{}]", message, e);
         sendError(message, context, e);
      }
      commit(context, wasConnected,null);
   }

   @Override
   public void handleError(ErrorEvent event, DeviceDriverContext context) {
      throw new UnsupportedOperationException("Not yet implemented");
   }

   private void commit(DeviceDriverContext context, boolean wasConnected, Integer reflexVersion) {
      preCommit(context);
      context.commit();
      postCommit(context, wasConnected, reflexVersion);
   }

   private void preCommit(DeviceDriverContext context) {
      // The call to getDirtyAttributes is more expensive than this
      // inexpensive call so perform a quick out check here.
      if (context.hasDirtyAttributes()) {
         attrBindingHandler.processDirtyAttributes(context, context.getDirtyAttributes(), new Date());
      }
   }

   private void postCommit(DeviceDriverContext context, boolean wasConnected, Integer reflexVersion) {
      boolean isConnected = context.isConnected();
      if (wasConnected == isConnected) {
         return;
      }

      try {
         if (isConnected) {
            handleDriverEvent(DriverEvent.createConnected(reflexVersion), context);
            context.broadcast(DeviceCapability.DeviceConnectedEvent.instance());
         } else {
            handleDriverEvent(DriverEvent.createDisconnected(reflexVersion), context);
            context.broadcast(DeviceCapability.DeviceDisconnectedEvent.instance());
         }
      } catch(Exception e) {
         driverLogger.warn("Error updating connected status", e);
      }
   }
   
   private void connected(DeviceDriverContext context, Integer reflexVersion) {
      try {
         handleDriverEvent(DriverEvent.createConnected(reflexVersion), context);
         context.broadcast(DeviceCapability.DeviceConnectedEvent.instance());
      }
      catch (Exception e) {
         // this is already logged, ignore it
      }
   }

   private void sendError(Message message, DeviceDriverContext context, String code, String errorMessage) {
      sendError(message, context, Errors.fromCode(code, errorMessage));
   }

   private void sendError(Message message, DeviceDriverContext context, Throwable cause) {
      sendError(message, context, Errors.fromException(cause));
   }

   private void sendError(Message message, DeviceDriverContext context, ErrorEvent error) {
      if(message.isRequest()) { // don't respond to things that aren't requests
         context.respondToPlatform(error);
      }
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "DeviceDriverImpl [" + definition + "]";
   }


}

