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
package com.iris.common.subsystem.doorsnlocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.annotation.Version;
import com.iris.capability.util.Addresses;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.common.subsystem.util.AddressesAttributeBinder;
import com.iris.common.subsystem.util.NotificationsUtil;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.DoorsNLocksSubsystemCapability;
import com.iris.messages.capability.DoorsNLocksSubsystemCapability.AuthorizePeopleRequest;
import com.iris.messages.capability.DoorsNLocksSubsystemCapability.SynchAuthorizationRequest;
import com.iris.messages.capability.MotorizedDoorCapability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.listener.annotation.OnAdded;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.listener.annotation.OnRemoved;
import com.iris.messages.listener.annotation.OnScheduledEvent;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.ContactModel;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.dev.DoorLockModel;
import com.iris.messages.model.dev.MotorizedDoorModel;
import com.iris.messages.model.serv.PersonModel;
import com.iris.messages.model.subs.DoorsNLocksSubsystemModel;
import com.iris.messages.type.DoorChimeConfig;
import com.iris.messages.type.LockAuthorizationOperation;
import com.iris.messages.type.LockAuthorizationState;

@Singleton
@Subsystem(DoorsNLocksSubsystemModel.class)
@Version(1)
public class DoorsNLocksSubsystem extends BaseSubsystem<DoorsNLocksSubsystemModel> {

   public static final String WARN_OFFLINE = "warning.offline";
   public static final String WARN_POOR_SIGNAL = "warning.poor_signal";
   public static final String WARN_LOW_BATTERY = "warning.low_battery";
   public static final String ERROR_MAX_PIN_EXCEEDED = "error.max_pins";
   public static final String ERROR_MAX_PIN_EXCEEDED_MSG = "Attempt to authorize more people that the lock supports";
   private static final String SLOT_STATE_UNKNOWN = "slot state not available";

   private static final AddressesAttributeBinder<DoorsNLocksSubsystemModel> petdoorsBinder =
         new AddressesAttributeBinder<DoorsNLocksSubsystemModel>(DoorsNLocksPredicates.IS_PETDOOR, DoorsNLocksSubsystemCapability.ATTR_PETDOORDEVICES);

   private static final AddressesAttributeBinder<DoorsNLocksSubsystemModel> petdoorsLockedBinder =
         new AddressesAttributeBinder<DoorsNLocksSubsystemModel>(DoorsNLocksPredicates.IS_PETDOOR_LOCKED, DoorsNLocksSubsystemCapability.ATTR_UNLOCKEDPETDOORS);

   private static final AddressesAttributeBinder<DoorsNLocksSubsystemModel> petdoorsAutoBinder =
         new AddressesAttributeBinder<DoorsNLocksSubsystemModel>(DoorsNLocksPredicates.IS_PETDOOR_AUTO, DoorsNLocksSubsystemCapability.ATTR_AUTOPETDOORS);

   private static final AddressesAttributeBinder<DoorsNLocksSubsystemModel> petdoorsOfflineBinder =
         new AddressesAttributeBinder<DoorsNLocksSubsystemModel>(DoorsNLocksPredicates.IS_PETDOOR_OFFLINE, DoorsNLocksSubsystemCapability.ATTR_OFFLINEPETDOORS);

   private static final AddressesAttributeBinder<DoorsNLocksSubsystemModel> jammedLocksBinder =
      new AddressesAttributeBinder<>(DoorsNLocksPredicates.IS_JAMMED_LOCK, DoorsNLocksSubsystemCapability.ATTR_JAMMEDLOCKS);

   private static final AddressesAttributeBinder<DoorsNLocksSubsystemModel> obstructedDoorsBinder =
      new AddressesAttributeBinder<>(DoorsNLocksPredicates.IS_OBSTRUCTED_DOOR, DoorsNLocksSubsystemCapability.ATTR_OBSTRUCTEDMOTORIZEDDOORS);

   @Inject(optional = true)
   @Named("delay.authorizeperson")
   private long delayAuthPersonInMSec = 5000;


   public long getDelayAuthPersonInMSec() {
	   return delayAuthPersonInMSec;
   }

   public void setDelayAuthPersonInMSec(long delayAuthPersonInMSec) {
	   this.delayAuthPersonInMSec = delayAuthPersonInMSec;
   }

   private final ConcurrentMap<Address,Queue<LockAuthorizationOperation>> deviceQueue = new ConcurrentHashMap<>();

   @Override
   protected void onAdded(SubsystemContext<DoorsNLocksSubsystemModel> context) {
      super.onAdded(context);
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      adapter.clear();
   }

   @Override
   protected void onStarted(SubsystemContext<DoorsNLocksSubsystemModel> context) {
      super.onStarted(context);
      petdoorsBinder.bind(context);
      petdoorsLockedBinder.bind(context);
      petdoorsAutoBinder.bind(context);
      petdoorsOfflineBinder.bind(context);
      jammedLocksBinder.bind(context);
      obstructedDoorsBinder.bind(context);

      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      syncPeople(adapter);
      syncDevices(adapter);
      syncChimeConfig(adapter);
      adapter.updateAvailable();
   }

   @OnAdded(query=DoorsNLocksPredicates.QUERY_DOORLOCK_DEVICES)
   public void onDeviceAdded(ModelAddedEvent event, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      if(addDoorLockDevice(event.getAddress(), adapter)) {
         adapter.updateAvailable();
         adapter.logger().info("A new doors & locks device was added {}", event);
      }
   }

   @OnRemoved(query=DoorsNLocksPredicates.QUERY_DOORLOCK_DEVICES)
   public void onDeviceRemoved(ModelRemovedEvent event, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      if(removeDoorLockDevice(event.getModel(), adapter)) {
         adapter.updateAvailable();
         adapter.logger().info("A doors & locks device was removed {}", event);
      }
   }

   @OnAdded(query=DoorsNLocksPredicates.QUERY_PEOPLE)
   public void onPersonAdded(ModelAddedEvent event, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      if(addPerson(event.getAddress(), adapter)) {
         adapter.logger().info("A new person was added {}", event);
      }
   }

   @OnRemoved(query=DoorsNLocksPredicates.QUERY_PEOPLE)
   public void onPersonRemoved(ModelRemovedEvent event, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      if(removePerson(event.getModel(), adapter)) {
         adapter.logger().info("A person was removed {}", event);
      }
   }

   @OnMessage(types = { PersonCapability.PinChangedEventEvent.NAME })
   public void onPersonPinChanged(PlatformMessage message, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      if(addPerson(message.getSource(), adapter)) {
         adapter.logger().debug("A new person was added because they now have a pin.");
      }

      String address = message.getSource().getRepresentation();

      Set<Model> locksToAuthorize = new HashSet<>();
      for(Model lock : adapter.getLocks()) {
         Set<LockAuthorizationState> authorization = adapter.getLockAuthorizations(lock);
         LockAuthorizationState state = findAuthorizationFor(address, authorization);
         if(state != null && state.getState().equals(LockAuthorizationState.STATE_AUTHORIZED)) {
            locksToAuthorize.add(lock);
            state.setState(LockAuthorizationState.STATE_PENDING);
            adapter.updateLockAuthorizations(lock, authorization);
         }
      }

      for(Model lock : locksToAuthorize) {
         adapter.authorize(parseId(address), lock);
      }
   }

   @OnValueChanged(attributes={
         DoorLockCapability.ATTR_LOCKSTATE,
         ContactCapability.ATTR_CONTACT,
         MotorizedDoorCapability.ATTR_DOORSTATE
   })
   public void onStateChanged(ModelChangedEvent event, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      Model m = getDeviceFromEvent(event, adapter);
      if(m != null) {
         adapter.updateOpen(m);
         if(DoorsNLocksPredicates.IS_CONTACT.apply(m) && ContactModel.isContactOPENED(m)) {
            chimeIfConfigured(m, adapter);
         }
      }
   }

   @OnValueChanged(attributes={DoorsNLocksSubsystemCapability.ATTR_JAMMEDLOCKS})
   public void onJammedLocksChange(ModelChangedEvent event, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      Set<String> prevJammed = (Set<String>) event.getOldValue();
      if(prevJammed == null) {
         prevJammed = ImmutableSet.of();
      }
      Set<String> nowJammed = (Set<String>) event.getAttributeValue();
      if(nowJammed == null) {
         nowJammed = ImmutableSet.of();
      }

      Set<String> unjammed = Sets.difference(prevJammed, nowJammed);
      for(String lock : unjammed) {
         notifyUnjammed(context, lock);
      }

      Set<String> jammed = Sets.difference(nowJammed, prevJammed);
      for(String lock : jammed) {
         notifyJammed(context, lock);
      }
   }

   private void notifyUnjammed(SubsystemContext<DoorsNLocksSubsystemModel> context, String address) {
      Model m = context.models().getModelByAddress(Address.fromString(address));
      if(m != null) {
         context.send(Address.broadcastAddress(), DoorsNLocksSubsystemCapability.LockUnjammedEvent.builder().withLock(address).build());
         Map<String, String> additionalParams = ImmutableMap.of("lockState", DoorLockModel.getLockstate(m));
         sendNotificationToOwner(notificationParams(m, additionalParams), "device.error.doorlock.jam.cleared", context);
      } else {
         context.logger().info("ignoring notification of jam cleared for device {} that no longer exists.", address);
      }
   }

   private void notifyJammed(SubsystemContext<DoorsNLocksSubsystemModel> context, String address) {
      Model m = context.models().getModelByAddress(Address.fromString(address));
      if(m != null) {
         context.send(Address.broadcastAddress(), DoorsNLocksSubsystemCapability.LockJammedEvent.builder().withLock(address).build());
         sendNotificationToOwner(notificationParams(m), "device.error.doorlock.jam", context);
      } else {
         context.logger().info("ignoring notification of jammed for device {} that no longer exists.", address);
      }
   }

   @OnValueChanged(attributes={DoorsNLocksSubsystemCapability.ATTR_OBSTRUCTEDMOTORIZEDDOORS})
   public void onObstructedDoorsChange(ModelChangedEvent event, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      Set<String> prevObstructed = (Set<String>) event.getOldValue();
      if(prevObstructed == null) {
         prevObstructed = ImmutableSet.of();
      }
      Set<String> nowObstructed = (Set<String>) event.getAttributeValue();
      if(nowObstructed == null) {
         nowObstructed = ImmutableSet.of();
      }

      Set<String> unobstructed = Sets.difference(prevObstructed, nowObstructed);
      for(String door : unobstructed) {
         notifyUnobstructed(context, door);
      }

      Set<String> obstructed = Sets.difference(nowObstructed, prevObstructed);
      for(String door : obstructed) {
         notifyObstructed(context, door);
      }
   }

   private void notifyUnobstructed(SubsystemContext<DoorsNLocksSubsystemModel> context, String address) {
      Model m = context.models().getModelByAddress(Address.fromString(address));
      if(m != null) {
         context.send(Address.broadcastAddress(), DoorsNLocksSubsystemCapability.MotorizedDoorUnobstructedEvent.builder().withDoor(address).build());
         Map<String, String> additionalParams = ImmutableMap.of("doorState", MotorizedDoorModel.getDoorstate(m));
         sendNotificationToOwner(notificationParams(m, additionalParams), "device.error.motdoor.obstruction.cleared", context);
      } else {
         context.logger().info("ignoring notification of obstruction cleared for device {} that no longer exists.", address);
      }
   }

   private void notifyObstructed(SubsystemContext<DoorsNLocksSubsystemModel> context, String address) {
      Model m = context.models().getModelByAddress(Address.fromString(address));
      if(m != null) {
         context.send(Address.broadcastAddress(), DoorsNLocksSubsystemCapability.MotorizedDoorObstructedEvent.builder().withDoor(address).build());
         sendNotificationToOwner(notificationParams(m), "device.error.motdoor.obstruction", context);
      } else {
         context.logger().info("ignoring notification of obstruction for device {} that no longer exists.", address);
      }
   }

   private Map<String, String> notificationParams(Model m) {
      return notificationParams(m, ImmutableMap.<String, String>of());
   }

   private Map<String, String> notificationParams(Model m, @Nullable Map<String, String> additionalParams) {
      if(additionalParams == null) {
         additionalParams = ImmutableMap.of();
      }

      return ImmutableMap.<String, String>builder()
         .putAll(additionalParams)
         .put("deviceName", DeviceModel.getName(m, ""))
         .build();
   }

   private void sendNotificationToOwner(Map<String, String> params, String template, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      String accountOwner = NotificationsUtil.getAccountOwnerAddress(context);
      // TTL based on place monitor subsystem
      NotificationsUtil.requestNotification(context, template, accountOwner, NotificationCapability.NotifyRequest.PRIORITY_MEDIUM, params, (int) TimeUnit.DAYS.toMillis(1));
   }

   @OnValueChanged(attributes={ ContactCapability.ATTR_USEHINT })
   public void onUseHintChange(ModelChangedEvent event, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      Model m = adapter.getModel(event.getAddress());
      if(m == null) {
         adapter.logger().debug("ignoring use hint update for nonexistent contact sensor", event);
         return;
      }
      if(ContactModel.isUsehintDOOR(m)) {
         addDoorLockDevice(event.getAddress(), adapter);
      } else {
         removeDoorLockDevice(m, adapter);
      }
      adapter.updateAvailable();
   }

   @OnValueChanged(attributes=Capability.ATTR_CAPS)
   public void onCapsChanged(ModelChangedEvent event, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      Model device = adapter.getModel(event.getAddress());
      if(device == null) {
         context.logger().debug("Received a ValueChange on an unrecognized device [{}]", event);
         return;
      }

      boolean isDoorLockDevice = DoorsNLocksPredicates.IS_DOORLOCK_DEVICE.apply(device);
      boolean isKnownDevice = adapter.deviceExists(event.getAddress());
      if(isDoorLockDevice && !isKnownDevice) {
         addDoorLockDevice(device.getAddress(), adapter);
      } else if(!isDoorLockDevice && isKnownDevice) {
         removeDoorLockDevice(device, adapter);
      }
      adapter.updateAvailable();
   }

   @OnValueChanged(attributes={
         DeviceConnectionCapability.ATTR_STATE,
         DeviceConnectionCapability.ATTR_SIGNAL,
         DevicePowerCapability.ATTR_BATTERY
   })
   public void onConnectivityStateChange(ModelChangedEvent event, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      Model m = getDeviceFromEvent(event, adapter);
      if(m != null) {
         String warning = getWarning(m);
         adapter.updateWarning(m, warning);

         if(event.getAttributeName().equals(DeviceConnectionCapability.ATTR_STATE)) {
            adapter.updateOffline(m);
         }
      }
   }

   @OnMessage(types = { DoorLockCapability.PersonAuthorizedEvent.NAME })
   public void onAuthorizationResponse(PlatformMessage message, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      Model m = adapter.getModel(message.getSource());
      if(m == null) {
         adapter.logger().debug("A response was received from a non-existent lock {}", message);
         return;
      }

      String personId = DoorLockCapability.PersonAuthorizedEvent.getPersonId(message.getValue());

      if(personId == null) {
         adapter.logger().debug("received PersonAuthorizedEvent with no personid");
         return;
      }

      String addr = DoorsNLocksContextAdapter.PERSON_ADDRESS_PREFIX + personId;

      String op = adapter.getOp(personId, m);

      Set<LockAuthorizationState> state = adapter.getLockAuthorizations(m);
      LockAuthorizationState auth = findAuthorizationFor(addr, state);

      if(op == null || LockAuthorizationOperation.OPERATION_AUTHORIZE.equals(op)) {
         adapter.removeOp(personId, m);
         auth.setState(LockAuthorizationState.STATE_AUTHORIZED);
         adapter.updateLockAuthorizations(m, state);
         adapter.emitAuthorized(addr, m);
         applyNext(adapter, m);
      }
   }

   @OnMessage(types = { DoorLockCapability.PersonDeauthorizedEvent.NAME })
   public void onDeauthorizationResponse(PlatformMessage message, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      Model m = adapter.getModel(message.getSource());
      if(m == null) {
         adapter.logger().debug("A response was received from a non-existent lock {}", message);
         return;
      }

      String personId = DoorLockCapability.PersonDeauthorizedEvent.getPersonId(message.getValue());

      if(personId == null) {
         adapter.logger().debug("received PersonDeauthorizedEvent with no personId");
         return;
      }

      String addr = DoorsNLocksContextAdapter.PERSON_ADDRESS_PREFIX + personId;

      String op = adapter.getOp(personId, m);

      Set<LockAuthorizationState> state = adapter.getLockAuthorizations(m);
      LockAuthorizationState auth = findAuthorizationFor(addr, state);

      if(op == null || LockAuthorizationOperation.OPERATION_DEAUTHORIZE.equals(op)) {
         adapter.removeOp(personId, m);
         if(auth != null) {
        	 auth.setState(LockAuthorizationState.STATE_UNAUTHORIZED);
         }
         adapter.updateLockAuthorizations(m, state);
         adapter.emitDeauthorized(addr, m);
         applyNext(adapter, m);
      }
   }

   @OnMessage(types = { DoorLockCapability.PinOperationFailedEvent.NAME })
   public void onPinOperationFailed(PlatformMessage message, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      Model m = adapter.getModel(message.getSource());
      if(m == null) {
         adapter.logger().debug("A response was received from a non-existent lock {}", message);
         return;
      }

      MessageBody body = message.getValue();

      String personId = DoorLockCapability.PinOperationFailedEvent.getPersonId(body);
      String msg = DoorLockCapability.PinOperationFailedEvent.getMessage(body);
      if(SLOT_STATE_UNKNOWN.equalsIgnoreCase(msg)) {
         adapter.logger().debug("ignoring PinOperationFailed of type {}", msg);
         return;
      }

      if(personId == null) {
         adapter.logger().debug("received PinOperationFailed with no personId");
         return;
      }

      String addr = DoorsNLocksContextAdapter.PERSON_ADDRESS_PREFIX + personId;

      String op = adapter.removeOp(personId, m);

      if(op == null) {
         adapter.logger().debug("no pending operation found on lock {} for person {}", m.getAddress(), addr);
         return;
      }

      Set<LockAuthorizationState> state = adapter.getLockAuthorizations(m);
      LockAuthorizationState auth = findAuthorizationFor(addr, state);

      // revert to the old state
      switch(op) {
      case LockAuthorizationOperation.OPERATION_AUTHORIZE:
         auth.setState(LockAuthorizationState.STATE_UNAUTHORIZED);
         break;
      case LockAuthorizationOperation.OPERATION_DEAUTHORIZE:
         auth.setState(LockAuthorizationState.STATE_AUTHORIZED);
         break;
      }
      adapter.updateLockAuthorizations(m, state);
      applyNext(adapter, m);
   }

   @OnMessage(types = { DoorLockCapability.AllPinsClearedEvent.NAME })
   public void onPinsCleared(PlatformMessage message, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      Model m = adapter.getModel(message.getSource());
      if(m == null) {
         adapter.logger().debug("A response was received from a non-existent lock {}", message);
         return;
      }

      Set<LockAuthorizationState> states = adapter.getLockAuthorizations(m);
      List<LockAuthorizationOperation> operations = new LinkedList<>();
      for(LockAuthorizationState state : states) {
         if(state.getState().equals(LockAuthorizationState.STATE_AUTHORIZED)) {
            LockAuthorizationOperation operation = new LockAuthorizationOperation();
            operation.setOperation(LockAuthorizationOperation.OPERATION_AUTHORIZE);
            operation.setPerson(state.getPerson());
            operations.add(operation);
            state.setState(LockAuthorizationState.STATE_PENDING);
         }
      }
      adapter.updateLockAuthorizations(m, states);
      adapter.commit();
      applyOperations(operations, m, adapter);
   }

   @OnScheduledEvent
   public void onScheduledEvent(ScheduledEvent event, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      Map<String,String> opData = adapter.removeOpData(event.getScheduledTimestamp());
      if(opData == null) {
         adapter.logger().debug("Ignoring scheduled event with no op data");
         return;
      }

      String person = opData.get(DoorsNLocksContextAdapter.OPDATA_PERSON);
      String lock = opData.get(DoorsNLocksContextAdapter.OPDATA_LOCK);
      String op = opData.get(DoorsNLocksContextAdapter.OPDATA_OPERATION);

      adapter.logger().debug("DoorsNLocksSubsystem.onScheduledEvent is triggered {},{},{}", op, person, lock);

      Model m = adapter.getModel(lock);
      if(m == null) {
         adapter.logger().debug("Ignoring a scheduled event for a lock that no longer exists {}", lock);
         return;
      }

      Set<LockAuthorizationState> state = adapter.getLockAuthorizations(m);
      LockAuthorizationState personState = findAuthorizationFor(person, state);
      if(personState == null) {
         adapter.logger().debug("Ignoring a scheduled event for a person that no longer exists {}", person);
         return;
      }

      if(!personState.getState().equals(LockAuthorizationState.STATE_PENDING) && !DoorsNLocksContextAdapter.OPDATA_OPERATION_DELAY_AUTHORIZATION.equals(op)) {
         adapter.logger().debug("Ignoring a scheduled event, the operation is no longer in the pending state so succeeded");
         return;
      }

      String personId = adapter.removePersonAddressPrefix(person);

      if(DoorsNLocksContextAdapter.OPDATA_OPERATION_DELAY_AUTHORIZATION.equals(op)) {
         adapter.logger().info("Authorizing account owner on new doorlock after a delay of "+getDelayAuthPersonInMSec() +" msec.");
         adapter.authorize(personId, m);
         return;
      }

      String currentOperation = adapter.getOp(personId, m);
      if(!Objects.equals(currentOperation, op)) {
         adapter.logger().debug("ignoring a scheduled event {} on {} because the current operation is {}", op, person, currentOperation);
         return;
      }

      // revert to the old state
      switch(op) {
      case LockAuthorizationOperation.OPERATION_AUTHORIZE:
         personState.setState(LockAuthorizationState.STATE_UNAUTHORIZED);
         break;
      case LockAuthorizationOperation.OPERATION_DEAUTHORIZE:
         personState.setState(LockAuthorizationState.STATE_AUTHORIZED);
         break;
      }
      adapter.updateLockAuthorizations(m, state);
      applyNext(adapter, m);
   }

   @Request(AuthorizePeopleRequest.NAME)
   public MessageBody authorizePeople(PlatformMessage message, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      MessageBody body = message.getValue();
      String device = AuthorizePeopleRequest.getDevice(body);

      if(StringUtils.isBlank(device)) {
         return Errors.fromCode(Errors.CODE_MISSING_PARAM, "The device is a required parameter.");
      }

      if(!adapter.containsLock(device)) {
         return Errors.fromCode(Errors.CODE_INVALID_PARAM, "No door lock exists for " + device);
      }

      Model lock = adapter.getModel(device);

      List<LockAuthorizationOperation> operations = operationsFromMaps(AuthorizePeopleRequest.getOperations(body));

      if(!operations.isEmpty()) {

         if(countAuthorizationRequests(operations) > DoorLockModel.getNumPinsSupported(lock)) {
            return Errors.fromCode(ERROR_MAX_PIN_EXCEEDED, ERROR_MAX_PIN_EXCEEDED_MSG);
         }

         Set<LockAuthorizationState> lockState = adapter.getLockAuthorizations(lock);
         Map<String,LockAuthorizationState> stateByPerson = stateByPeople(lockState);

         Set<String> pendingOperations = checkPending(stateByPerson, operations);
         if(!pendingOperations.isEmpty()) {
            return Errors.fromCode("doorsnlocks.already.pending", "An existing pin operation is already taking place for " + pendingOperations);
         }

         List<LockAuthorizationOperation> filtered = filter(stateByPerson, operations);
         if(filtered.isEmpty()) {
            return DoorsNLocksSubsystemCapability.AuthorizePeopleResponse.builder().withChangesPending(false).build();
         }
         markPending(lock, stateByPerson, filtered, adapter);
         applyOperations(filtered, lock, adapter);
         return DoorsNLocksSubsystemCapability.AuthorizePeopleResponse.builder().withChangesPending(true).build();
      }

      return DoorsNLocksSubsystemCapability.AuthorizePeopleResponse.builder().withChangesPending(false).build();
   }

   @Request(SynchAuthorizationRequest.NAME)
   public MessageBody synchAuthorization(PlatformMessage message, SubsystemContext<DoorsNLocksSubsystemModel> context) {
      DoorsNLocksContextAdapter adapter = new DoorsNLocksContextAdapter(context);
      MessageBody body = message.getValue();
      String device = AuthorizePeopleRequest.getDevice(body);

      if(StringUtils.isBlank(device)) {
         return Errors.fromCode(Errors.CODE_MISSING_PARAM, "The device is a required parameter.");
      }

      if(!adapter.containsLock(device)) {
         return Errors.fromCode(Errors.CODE_INVALID_PARAM, "No door lock exists for " + device);
      }

      Model lock = adapter.getModel(device);
      adapter.clearPins(lock);
      return DoorsNLocksSubsystemCapability.SynchAuthorizationResponse.instance();
   }

   private void chimeIfConfigured(Model m, DoorsNLocksContextAdapter adapter) {
      DoorChimeConfig config = adapter.getChimeConfig(m);
      if(config == null || !config.getEnabled()) {
         return;
      }

      for(Model chimeDev : adapter.getChimeDevices()) {
         adapter.chime(chimeDev);
      }
   }

   private int countAuthorizationRequests(List<LockAuthorizationOperation> operations) {
      int authorizations = 0;
      for(LockAuthorizationOperation op : operations) {
         if(op.getOperation().equals(LockAuthorizationOperation.OPERATION_AUTHORIZE)) {
            authorizations++;
         }
      }
      return authorizations;
   }

   private void applyOperations(List<LockAuthorizationOperation> operations, Model lock, DoorsNLocksContextAdapter adapter) {
      if(operations == null || operations.isEmpty()) {
         return;
      }

      // sort to do the deauthorizations first
      Collections.sort(operations, new Comparator<LockAuthorizationOperation>() {
         @Override
         public int compare(LockAuthorizationOperation o1, LockAuthorizationOperation o2) {
            String op1 = o1.getOperation();
            String op2 = o2.getOperation();
            return op2.compareTo(op1);
         }
      });

      Queue<LockAuthorizationOperation> queue = new LinkedList<>();
      Queue<LockAuthorizationOperation> existing = deviceQueue.putIfAbsent(lock.getAddress(), queue);
      if(existing != null) {
         queue = existing;
      }

      LockAuthorizationOperation first = existing != null ? existing.poll() : null;
      for(LockAuthorizationOperation operation : operations) {
         if(first == null) {
            first = operation;
         } else {
            queue.add(operation);
         }
      }

      applyOperation(adapter, lock, first);
   }

   private void applyOperation(DoorsNLocksContextAdapter adapter, Model lock, LockAuthorizationOperation op) {
      switch(op.getOperation()) {
      case LockAuthorizationOperation.OPERATION_AUTHORIZE:
         adapter.authorize(parseId(op.getPerson()), lock);
         break;
      case LockAuthorizationOperation.OPERATION_DEAUTHORIZE:
         adapter.deauthorize(parseId(op.getPerson()), lock, true);
         break;
      }
   }

   private String parseId(String address) {
      return address.split(":")[2];
   }

   private void markPending(Model lock, Map<String,LockAuthorizationState> stateByPeople, List<LockAuthorizationOperation> operations, DoorsNLocksContextAdapter adapter) {
      for(LockAuthorizationOperation operation : operations) {
         LockAuthorizationState state = stateByPeople.get(operation.getPerson());
         if(state == null) {
            state = new LockAuthorizationState();
            state.setPerson(operation.getPerson());
            stateByPeople.put(operation.getPerson(), state);
         }
         state.setState(LockAuthorizationState.STATE_PENDING);
      }
      adapter.updateLockAuthorizations(lock, new HashSet<>(stateByPeople.values()));
      adapter.commit();
   }

   private List<LockAuthorizationOperation> filter(Map<String,LockAuthorizationState> curState, List<LockAuthorizationOperation> operations) {
      List<LockAuthorizationOperation> ops = new ArrayList<>(operations.size());
      for(LockAuthorizationOperation op : operations) {
         LockAuthorizationState state = curState.get(op.getPerson());
         switch(op.getOperation()) {
         case LockAuthorizationOperation.OPERATION_AUTHORIZE:
            if(stateAllowsAuthorize(state)) { ops.add(op); }
            break;
         case LockAuthorizationOperation.OPERATION_DEAUTHORIZE:
            if(stateAllowsUnauthorize(state)) { ops.add(op); }
            break;
         }
      }

      return ops;
   }

   private boolean stateAllowsAuthorize(LockAuthorizationState state) {
      return state.getState().equals(LockAuthorizationState.STATE_UNAUTHORIZED) ||
             state.getState().equals(LockAuthorizationState.STATE_ERROR);
   }

   private boolean stateAllowsUnauthorize(LockAuthorizationState state) {
      return state.getState().equals(LockAuthorizationState.STATE_AUTHORIZED) ||
             state.getState().equals(LockAuthorizationState.STATE_ERROR);
   }

   private Set<String> checkPending(Map<String,LockAuthorizationState> curState, List<LockAuthorizationOperation> operations) {
      Set<String> pending = new HashSet<>();
      for(LockAuthorizationOperation operation : operations) {
         LockAuthorizationState state = curState.get(operation.getPerson());
         if(state.getState().equals(LockAuthorizationState.STATE_PENDING)) {
            pending.add(operation.getPerson());
         }
      }
      return pending;
   }

   private Map<String,LockAuthorizationState> stateByPeople(Set<LockAuthorizationState> lockState) {
      Map<String,LockAuthorizationState> byPeople = new HashMap<>(lockState.size());
      for(LockAuthorizationState state : lockState) {
         byPeople.put(state.getPerson(), state);
      }
      return byPeople;
   }

   private List<LockAuthorizationOperation> operationsFromMaps(List<Map<String,Object>> operations) {
      if(operations == null) {
         return new ArrayList<>();
      }
      List<LockAuthorizationOperation> ops = new ArrayList<>(operations.size());
      for(Map<String,Object> op : operations) {
         ops.add(new LockAuthorizationOperation(op));
      }
      return ops;
   }

   private LockAuthorizationState findAuthorizationFor(String person, Set<LockAuthorizationState> authorizationEntries) {
      for(LockAuthorizationState auth : authorizationEntries) {
         if(auth.getPerson().equals(person)) {
            return auth;
         }
      }
      return null;
   }

   private void syncDevices(DoorsNLocksContextAdapter adapter) {
      adapter.logger().debug("syncing devices with model");
      Set<String> previousLocks = adapter.getModel().getLockDevices();

      adapter.clearDevices();
      for(Model m : adapter.getDevices()) {
         boolean added = addDevice(m, adapter);
         // provision newly discovered
         if(added) {
            String address = m.getAddress().getRepresentation();
            if(DoorsNLocksPredicates.IS_DOORLOCK.apply(m) && !previousLocks.contains(address)) {
               afterAddDoorLock(m, adapter);
            }
         }
      }

      // clean-up the dead
      previousLocks.removeAll(adapter.getModel().getLockDevices());
      for(String address: previousLocks) {
         adapter.removeLockAuthorizations(Address.fromString(address));
      }
   }

   private void syncChimeConfig(DoorsNLocksContextAdapter adapter) {
      adapter.logger().debug("syncing device chime config with model");
      Set<DoorChimeConfig> currentConfigs = adapter.getChimeConfig();
      Set<DoorChimeConfig> newConfig = new HashSet<>();
      Set<String> contactSensors = adapter.getContactSensorAddresses();
      for(DoorChimeConfig config : currentConfigs) {
         if(!contactSensors.contains(config.getDevice())) { continue; }
         newConfig.add(config);
         contactSensors.remove(config.getDevice());
      }
      for(String contact : contactSensors) {
         DoorChimeConfig config = new DoorChimeConfig();
         config.setDevice(contact);
         config.setEnabled(false);
         newConfig.add(config);
      }
      adapter.setChimeConfig(newConfig);
   }

   private void syncPeople(DoorsNLocksContextAdapter adapter) {
      adapter.logger().debug("syncing people with model");
      adapter.clearPeople();
      for(Model m : adapter.getPeople()) {
         adapter.addPerson(m);
      }

      adapter.logger().debug("syncing lock state with model");
      for(Model m : adapter.getLocks()) {
         adapter.updateLockAuthorizations(m, syncAuthForLock(m, adapter));
      }
   }

   private Set<LockAuthorizationState> syncAuthForLock(Model m, DoorsNLocksContextAdapter adapter) {
      Set<String> people = adapter.getAllPeople();
      Set<LockAuthorizationState> newStates = new HashSet<>();
      Map<String,String> slots = DoorLockModel.getSlots(m);

      if(slots != null) {
	      for(String person : slots.values()) {
	         String personAddr = DoorsNLocksContextAdapter.PERSON_ADDRESS_PREFIX + person;
	         if(people.remove(personAddr)) {
	            LockAuthorizationState auth = new LockAuthorizationState();
	            auth.setPerson(personAddr);
	            auth.setState(LockAuthorizationState.STATE_AUTHORIZED);
	            newStates.add(auth);
	         } else {
	            adapter.logger().info("would deauthorize person {} at startup.  the person is in lock {} slot map but could not be found in model", personAddr, m.getAddress().getRepresentation());
	            //adapter.deauthorize(person, m);
	         }
	      }
      }

      for(String person : people) {
         LockAuthorizationState auth = new LockAuthorizationState();
         auth.setPerson(person);
         auth.setState(LockAuthorizationState.STATE_UNAUTHORIZED);
         newStates.add(auth);
      }

      return newStates;
   }

   private boolean addDoorLockDevice(Address address, DoorsNLocksContextAdapter adapter) {
      Model m = adapter.getModel(address);

      if(addDevice(m, adapter)) {
         if(DoorsNLocksPredicates.IS_DOORLOCK.apply(m)) {
            afterAddDoorLock(m, adapter);
         }
         if(DoorsNLocksPredicates.IS_CONTACT.apply(m)) {
            afterAddContactSensor(m, adapter);
         }
         return true;
      }
      return false;
   }

   private boolean addDevice(Model m, DoorsNLocksContextAdapter adapter) {
      if(!adapter.addTotal(m)) {
         return false;
      }
      adapter.updateOffline(m);
      adapter.updateOpen(m);
      adapter.updateWarning(m, getWarning(m));
      return true;
   }

   private boolean addPerson(Address address, DoorsNLocksContextAdapter adapter)  {
      Model m = adapter.getModel(address);
      if(PersonModel.getPlacesWithPin(m) == null || !PersonModel.getPlacesWithPin(m).contains(adapter.getPlaceId())) {
         return false;
      }
      if(adapter.addPerson(m)) {
         for(Model lock : adapter.getLocks()) {
            Set<LockAuthorizationState> authorization = adapter.getLockAuthorizations(lock);
            LockAuthorizationState auth = new LockAuthorizationState();
            auth.setPerson(m.getAddress().getRepresentation());
            auth.setState(LockAuthorizationState.STATE_UNAUTHORIZED);
            authorization.add(auth);
            adapter.updateLockAuthorizations(lock, authorization);
         }
         return true;
      }
      return false;
   }

   private boolean removePerson(Model m, DoorsNLocksContextAdapter adapter) {
      if(adapter.removePerson(m)) {
         clearPinsForPerson(m, adapter);
         return true;
      }
      return false;
   }

   private void clearPinsForPerson(Model person, DoorsNLocksContextAdapter adapter) {
      String address = person.getAddress().getRepresentation();
      for(Model lock : adapter.getLocks()) {
         Set<LockAuthorizationState> auths = adapter.getLockAuthorizations(lock);
         LockAuthorizationState auth = findAuthorizationFor(address, auths);
         if(auth != null) {
            auths.remove(auth);
            adapter.updateLockAuthorizations(lock, auths);
            if(auth.getState().equals(LockAuthorizationState.STATE_AUTHORIZED)) {
               adapter.deauthorize(parseId(address), lock);
            }
         }
      }
   }

   private void afterAddDoorLock(Model lock, DoorsNLocksContextAdapter adapter) {
      String accountOwnerAddress = "";
      Model accountOwner = adapter.getAccountOwner();
      if(accountOwner != null && PersonModel.getHasPin(accountOwner, false)) {
         accountOwnerAddress = accountOwner.getAddress().getRepresentation();
      }
      else {
         adapter.logger().warn("Account owner is not set or has no pin -- won't provision lock");
      }

      Set<String> authorizedPersonIds = new HashSet<String>(DoorLockModel.getSlots(lock, ImmutableMap.<String, String>of()).values());
      Set<LockAuthorizationState> authorizations = new HashSet<LockAuthorizationState>();
      for(String person : adapter.getAllPeople()) {
         LockAuthorizationState auth = new LockAuthorizationState();
         auth.setPerson(person);

         String personId = Addresses.getId(person);
         if(authorizedPersonIds.contains(personId)) {
            auth.setState(LockAuthorizationState.STATE_AUTHORIZED);
         }
         else if(person.equals(accountOwnerAddress)) {
            long delayMs = getDelayAuthPersonInMSec();
            auth.setState(LockAuthorizationState.STATE_PENDING);
            if(delayMs > 0) {
            	adapter.authorizeWithDelay(accountOwner.getId(), lock, delayMs);
            }
            else {
            	adapter.logger().info("Authorizing account owner on new doorlock.");
            	adapter.authorize(accountOwner.getId(), lock);
            }
         }
         else {
            auth.setState(LockAuthorizationState.STATE_UNAUTHORIZED);
         }
         authorizations.add(auth);
      }
      adapter.updateLockAuthorizations(lock, authorizations);
   }

   private void afterAddContactSensor(Model m, DoorsNLocksContextAdapter adapter) {
      DoorChimeConfig chimeConfig = new DoorChimeConfig();
      chimeConfig.setDevice(m.getAddress().getRepresentation());
      chimeConfig.setEnabled(true);
      adapter.addChimeConfig(chimeConfig);
   }

   private boolean removeDevice(Model m, DoorsNLocksContextAdapter adapter) {
      if(!adapter.removeTotal(m)) {
         return false;
      }
      adapter.removeOffline(m);
      adapter.removeOpen(m);
      return true;
   }

   private boolean removeDoorLockDevice(Model m, DoorsNLocksContextAdapter adapter) {
      if(removeDevice(m, adapter)) {
         if(DoorsNLocksPredicates.IS_DOORLOCK.apply(m)) {
            deviceQueue.remove(m.getAddress());
            adapter.removeLockAuthorizations(m.getAddress());
         }
         if(DoorsNLocksPredicates.IS_CONTACT.apply(m)) {
            adapter.removeChimeConfig(m.getAddress());
         }

         adapter.removeWarning(m);
         return true;
      }
      return false;
   }

   private String getWarning(Model m) {
      if(!DoorsNLocksPredicates.IS_ONLINE.apply(m)) {
         return WARN_OFFLINE;
      }
      if(DoorsNLocksPredicates.IS_LOW_BATTERY.apply(m)) {
         return WARN_LOW_BATTERY;
      }
      if(DoorsNLocksPredicates.IS_POOR_SIGNAL.apply(m)) {
         return WARN_POOR_SIGNAL;
      }
      return null;
   }

   private Model getDeviceFromEvent(ModelEvent event, DoorsNLocksContextAdapter adapter) {
      if(!adapter.deviceExists(event.getAddress())) {
         adapter.logger().debug("Ignoring event from non-doors n locks device {}", event);
         return null;
      }
      Model model = adapter.getModel(event.getAddress());
      if(model == null) {
         adapter.logger().warn("Unable to retrieve model for doors n locks device {}", event.getAddress());
         return null;
      }

      return model;
   }

   private void applyNext(DoorsNLocksContextAdapter adapter, Model lock) {
      LockAuthorizationOperation op = nextOp(lock);
      if(op != null) {
         applyOperation(adapter, lock, op);
      }
   }

   private LockAuthorizationOperation nextOp(Model lock) {
      Address addr = lock.getAddress();
      Queue<LockAuthorizationOperation> queue = deviceQueue.get(addr);
      if(queue == null) {
         return null;
      }

      LockAuthorizationOperation next = queue.poll();
      if(next == null) {
         deviceQueue.remove(addr, queue);
      }

      return next;
   }
}

