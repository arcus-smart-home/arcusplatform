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
package com.iris.common.subsystem.presence;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;

import java.util.HashSet;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import com.iris.annotation.Version;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.common.subsystem.util.AddressesAttributeBinder;
import com.iris.common.subsystem.util.AddressesVariableBinder;
import com.iris.common.subsystem.util.MapVariableBinder;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.PresenceCapability;
import com.iris.messages.capability.PresenceSubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.PresenceModel;
import com.iris.messages.model.subs.PresenceSubsystemModel;
import com.iris.model.predicate.Predicates;

@Singleton
@Subsystem(PresenceSubsystemModel.class)
@Version(2)
public class PresenceSubsystem extends BaseSubsystem<PresenceSubsystemModel> {
   private static final String PRES_PERSON_UNSET = "UNSET";

   static final Predicate<Model> IS_PERSON = Predicates.isA(PersonCapability.NAMESPACE);
   static final Predicate<Model> IS_PRESENCE_DEVICE = Predicates.isA(PresenceCapability.NAMESPACE);
   static final Predicate<Model> IS_ASSIGNED_DEVICE = 
         Predicates.attributeEquals(PresenceCapability.ATTR_USEHINT, PresenceCapability.USEHINT_PERSON);
   @SuppressWarnings("unchecked")
   static final Predicate<Model> IS_DEVICE_AWAY =
         and(
               Predicates.isA(PresenceCapability.NAMESPACE),
               not(Predicates.attributeEquals(PresenceCapability.ATTR_USEHINT, PresenceCapability.USEHINT_PERSON)),
               not(Predicates.attributeEquals(PresenceCapability.ATTR_PRESENCE, PresenceCapability.PRESENCE_PRESENT))
         );
   @SuppressWarnings("unchecked")
   static final Predicate<Model> IS_DEVICE_PRESENT =
         and(
               Predicates.isA(PresenceCapability.NAMESPACE),
               not(Predicates.attributeEquals(PresenceCapability.ATTR_USEHINT, PresenceCapability.USEHINT_PERSON)),
               Predicates.attributeEquals(PresenceCapability.ATTR_PRESENCE, PresenceCapability.PRESENCE_PRESENT)
         );
   @SuppressWarnings("unchecked")
   static final Predicate<Model> IS_PERSON_AWAY =
         and(
               Predicates.isA(PresenceCapability.NAMESPACE),
               Predicates.attributeEquals(PresenceCapability.ATTR_USEHINT, PresenceCapability.USEHINT_PERSON),
               not(Predicates.attributeEquals(PresenceCapability.ATTR_PRESENCE, PresenceCapability.PRESENCE_PRESENT))
         );
   @SuppressWarnings("unchecked")
   static final Predicate<Model> IS_PERSON_PRESENT =
         and(
               Predicates.isA(PresenceCapability.NAMESPACE),
               Predicates.attributeEquals(PresenceCapability.ATTR_USEHINT, PresenceCapability.USEHINT_PERSON),
               Predicates.attributeEquals(PresenceCapability.ATTR_PRESENCE, PresenceCapability.PRESENCE_PRESENT)
         );

         
   private final AddressesAttributeBinder<PresenceSubsystemModel>   allDevices = new AllFobBinder();
   private final AddressesAttributeBinder<PresenceSubsystemModel>   atHomeDevices = new UnassignedFobPresenceBinder(true);
   private final AddressesAttributeBinder<PresenceSubsystemModel>   awayDevices = new UnassignedFobPresenceBinder(false);
   private final MapVariableBinder<PresenceSubsystemModel, String>  atHomePersons = new AssignedFobPresenceBinder(true);
   private final MapVariableBinder<PresenceSubsystemModel, String>  awayPersons = new AssignedFobPresenceBinder(false);
   private final MapVariableBinder<PresenceSubsystemModel, String>  assignedFobs = new AssignedFobBinder();
   private final AddressesVariableBinder<PresenceSubsystemModel>    persons = new PersonBinder();
   
   @Override
   protected void onAdded(SubsystemContext<PresenceSubsystemModel> context) {
      super.onAdded(context);
      context.model().setAvailable(false);
      context.model().setOccupied(false);
      context.model().setAllDevices(ImmutableSet.<String>of());
      context.model().setDevicesAtHome(ImmutableSet.<String>of());
      context.model().setDevicesAway(ImmutableSet.<String>of());
      context.model().setPeopleAtHome(ImmutableSet.<String>of());
      context.model().setPeopleAway(ImmutableSet.<String>of());
   }

   
   @Override
   protected void onUpgraded(
         SubsystemContext<PresenceSubsystemModel> context,
         com.iris.model.Version oldVersion
   ) {
      super.onUpgraded(context, oldVersion);
      if(oldVersion.getMajor() == 1) {
         // initialize the variables without triggering any events
         persons.sync(context);
         assignedFobs.sync(context);
         
         atHomePersons.sync(context);
         context.model().setAttribute(PresenceSubsystemCapability.ATTR_PEOPLEATHOME, new HashSet<>(atHomePersons.getValues(context).values()));
         awayPersons.sync(context);
         context.model().setAttribute(PresenceSubsystemCapability.ATTR_PEOPLEAWAY, new HashSet<>(awayPersons.getValues(context).values()));
      }
   }


   @Override
   protected void onStarted(SubsystemContext<PresenceSubsystemModel> context) {
      super.onStarted(context);
      persons.bind(context);
      assignedFobs.bind(context);
      
      allDevices.bind(context);
      atHomeDevices.bind(context);
      awayDevices.bind(context);
      atHomePersons.bind(context);
      awayPersons.bind(context);
      syncOccupied(context);
   }

   protected void disassociateFromPerson(SubsystemContext<PresenceSubsystemModel> context, Model m) {
      // update attribute
      MessageBody clear = 
            MessageBody
               .messageBuilder(Capability.CMD_SET_ATTRIBUTES)
               .withAttribute(PresenceCapability.ATTR_PERSON, PRES_PERSON_UNSET)
               .withAttribute(PresenceCapability.ATTR_USEHINT, PresenceCapability.USEHINT_UNKNOWN)
               .create();
      context.request(m.getAddress(), clear);
   }
   
   protected void onFobAssigned(SubsystemContext<PresenceSubsystemModel> context, Model m) {
      String person = PresenceModel.getPerson(m);
      context.logger().debug("Fob [{}] assigned to [{}]", m.getAddress(), person);
      for(Model model: context.models().getModels(IS_PRESENCE_DEVICE)) {
         if(model.getAddress().equals(m.getAddress())) {
            continue;
         }
         
         if(person.equals(PresenceModel.getPerson(model))) {
            context.logger().debug("Person [{}] has been re-assigned to [{}] from [{}]", person, m.getAddress(), model.getAddress());
            disassociateFromPerson(context, model);
         }
      }
      
      MessageBody assigned = 
            PresenceSubsystemCapability.DeviceAssignedToPersonEvent
               .builder()
               .withDevice(m.getAddress().getRepresentation())
               .withPerson(person)
               .build();
      context.broadcast(assigned);
   }
   
   protected void onFobUnassigned(SubsystemContext<PresenceSubsystemModel> context, Address fob, String person) {
      MessageBody assigned = 
            PresenceSubsystemCapability.DeviceUnassignedFromPersonEvent
               .builder()
               .withDevice(fob.getRepresentation())
               .withPerson(person)
               .build();
      context.broadcast(assigned);
   }
   
   protected void onFobArrived(SubsystemContext<PresenceSubsystemModel> context, Model m, boolean assigned) {
      String deviceAddress = m.getAddress().getRepresentation();
      String personAddress = PresenceModel.getPerson(m);
      
      MessageBody arrivedEvent =
            PresenceSubsystemCapability.ArrivedEvent.builder()
               .withDevice(m.getAddress().getRepresentation())
               .withTarget(assigned ? personAddress : deviceAddress)
               .withType(assigned ? PresenceSubsystemCapability.ArrivedEvent.TYPE_PERSON : PresenceSubsystemCapability.ArrivedEvent.TYPE_DEV)
               .build();
      context.broadcast(arrivedEvent);
      
      if(assigned) {
         MessageBody personArrivedEvent =
            PresenceSubsystemCapability.PersonArrivedEvent.builder()
               .withDevice(deviceAddress)
               .withPerson(personAddress)
               .build();
         context.broadcast(personArrivedEvent);
      }
      else {
         MessageBody deviceArrivedEvent =
               PresenceSubsystemCapability.DeviceArrivedEvent.builder()
                  .withDevice(deviceAddress)
                  .build();
            context.broadcast(deviceArrivedEvent);
      }
      syncOccupied(context);
   }

   protected void onFobDeparted(SubsystemContext<PresenceSubsystemModel> context, Model m, boolean assigned) {
      String deviceAddress = m.getAddress().getRepresentation();
      String personAddress = PresenceModel.getPerson(m);
      
      MessageBody departedEvent =
            PresenceSubsystemCapability.DepartedEvent.builder()
               .withDevice(m.getAddress().getRepresentation())
               .withTarget(assigned ? personAddress : deviceAddress)
               .withType(assigned ? PresenceSubsystemCapability.ArrivedEvent.TYPE_PERSON : PresenceSubsystemCapability.ArrivedEvent.TYPE_DEV)
               .build();
      context.broadcast(departedEvent);
      
      if(assigned) {
         MessageBody personDepartedEvent =
            PresenceSubsystemCapability.PersonDepartedEvent.builder()
               .withDevice(deviceAddress)
               .withPerson(personAddress)
               .build();
         context.broadcast(personDepartedEvent);
      }
      else {
         MessageBody deviceDepartedEvent =
               PresenceSubsystemCapability.DeviceDepartedEvent.builder()
                  .withDevice(deviceAddress)
                  .build();
            context.broadcast(deviceDepartedEvent);
      }
      syncOccupied(context);
   }
   
   protected void onFobAdded(SubsystemContext<PresenceSubsystemModel> context, Model fob) {
      context.model().setAvailable(true);
   }
   
   protected void onFobDeleted(SubsystemContext<PresenceSubsystemModel> context, Address fob) {
      context.model().setAvailable(!context.model().getAllDevices().isEmpty());
      syncOccupied(context);
   }
   
   protected void onPersonDeleted(SubsystemContext<PresenceSubsystemModel> context, Address person) {
      String repr = person.getRepresentation();
      for(Model device: context.models().getModels(IS_PRESENCE_DEVICE)) {
         if(repr.equals(PresenceModel.getPerson(device, PRES_PERSON_UNSET))) {
            context.logger().debug("Disassociating person [{}] from device [{}] because person was removed", repr, device.getAddress());
            disassociateFromPerson(context, device);
         }
      }
   }
   
   protected void syncOccupied(SubsystemContext<PresenceSubsystemModel> context) {
      boolean occupied = !context.model().getPeopleAtHome().isEmpty();
      if(context.model().getOccupied(Boolean.FALSE) == occupied) {
         return;
      }
      
      context.model().setOccupied(occupied);
      if(occupied) {
         context.broadcast(PresenceSubsystemCapability.PlaceOccupiedEvent.instance());
      }
      else {
         context.broadcast(PresenceSubsystemCapability.PlaceUnoccupiedEvent.instance());
      }
   }

   private class AllFobBinder extends AddressesAttributeBinder<PresenceSubsystemModel> {
      public AllFobBinder() {
         super(IS_PRESENCE_DEVICE, PresenceSubsystemCapability.ATTR_ALLDEVICES);
      }

      @Override
      protected void afterAdded(SubsystemContext<PresenceSubsystemModel> context, Model model) {
         super.afterAdded(context, model);
         onFobAdded(context, model);
      }

      @Override
      protected void afterRemoved(SubsystemContext<PresenceSubsystemModel> context, Address address) {
         super.afterRemoved(context, address);
         onFobDeleted(context, address);
      }
      
   }
   
   private class AssignedFobBinder extends MapVariableBinder<PresenceSubsystemModel, String> {

      public AssignedFobBinder() {
         super("assignedFobs", String.class);
      }
      
      @Override
      protected String getValue(SubsystemContext<PresenceSubsystemModel> context, Model m) {
         if(!IS_ASSIGNED_DEVICE.apply(m)) {
            return null;
         }
         
         // now we have to make sure this person actually exists
         String person = PresenceModel.getPerson(m, PRES_PERSON_UNSET);
         return persons.getAddresses(context).contains(person) ? person : null;
      }

      @Override
      protected void afterSet(
            SubsystemContext<PresenceSubsystemModel> context, 
            Model model,
            String person
      ) {
         super.afterSet(context, model, person);
         onFobAssigned(context, model);
      }

      @Override
      protected void afterChanged(
            SubsystemContext<PresenceSubsystemModel> context, 
            Model model,
            String oldValue, 
            String newValue
      ) {
         onFobUnassigned(context, model.getAddress(), oldValue);
         onFobAssigned(context, model);
      }

      @Override
      protected void afterCleared(
            SubsystemContext<PresenceSubsystemModel> context, 
            Address address,
            String person
      ) {
         super.afterCleared(context, address, person);
         Model device = context.models().getModelByAddress(address);
         if(device != null) {
            onFobUnassigned(context, address, person);
         }
         // else deleted 
      }
      
      
   }
   
   private class AssignedFobPresenceBinder extends MapVariableBinder<PresenceSubsystemModel, String> {
      private final boolean present;
      private final Predicate<? super Model> predicate;
      private final String attributeName;
      
      public AssignedFobPresenceBinder(boolean present) {
         super(present ? "presentPersons" : "awayPersons", String.class);
         this.present = present;
         this.predicate = present ? IS_PERSON_PRESENT : IS_PERSON_AWAY;
         this.attributeName = present ? PresenceSubsystemCapability.ATTR_PEOPLEATHOME : PresenceSubsystemCapability.ATTR_PEOPLEAWAY;
      }
      
      @Override
      protected String getValue(SubsystemContext<PresenceSubsystemModel> context, Model m) {
         if(!predicate.apply(m)) {
            return null;
         }
         String person = PresenceModel.getPerson(m, PRES_PERSON_UNSET);
         if(persons.getAddresses(context).contains(person)) {
            return person;
         }
         else {
            // this is a bit abusive...
            context.logger().debug("Forcing fob associated with unknown person [{}] back into devices attribute", person);
            if(present) {
               addAddressToSet(m.getAddress().getRepresentation(), PresenceSubsystemCapability.ATTR_DEVICESATHOME, context.model());
            }
            else {
               addAddressToSet(m.getAddress().getRepresentation(), PresenceSubsystemCapability.ATTR_DEVICESAWAY, context.model());
            }
            return null;
         }
      }

      @Override
      protected void afterSet(
            SubsystemContext<PresenceSubsystemModel> context, 
            Model model,
            String person
      ) {
         super.afterSet(context, model, person);
         context.model().setAttribute(attributeName, new HashSet<>(getValues(context).values()));
         syncOccupied(context);
      }

      @Override
      protected void afterChanged(
            SubsystemContext<PresenceSubsystemModel> context, 
            Model model,
            String oldValue, 
            String newValue
      ) {
         super.afterChanged(context, model, oldValue, newValue);
         context.model().setAttribute(attributeName, new HashSet<>(getValues(context).values()));
      }

      @Override
      protected void afterCleared(
            SubsystemContext<PresenceSubsystemModel> context, 
            Address address,
            String person
      ) {
         super.afterCleared(context, address, person);
         context.model().setAttribute(attributeName, new HashSet<>(getValues(context).values()));
         syncOccupied(context);
         
         Model m = context.models().getModelByAddress(address);
         if(m == null) {
            // no longer in this set because its been deleted
            return;
         }
         if(!IS_ASSIGNED_DEVICE.apply(m)) {
            // no longer in this set because its been unassigned
            return;
         }
         if(present) {
            onFobDeparted(context, m, true);
         }
         else {
            onFobArrived(context, m, true);
         }
      }
      
      
   }
   
   private class UnassignedFobPresenceBinder extends AddressesAttributeBinder<PresenceSubsystemModel> {
      private final boolean present;
      
      public UnassignedFobPresenceBinder(boolean present) {
         super(
               present ? IS_DEVICE_PRESENT : IS_DEVICE_AWAY,
               present ? PresenceSubsystemCapability.ATTR_DEVICESATHOME : PresenceSubsystemCapability.ATTR_DEVICESAWAY
         );
         this.present = present;
      }

      @Override
      protected void afterRemoved(SubsystemContext<PresenceSubsystemModel> context, Address address) {
         super.afterRemoved(context, address);
         Model m = context.models().getModelByAddress(address);
         if(m == null) {
            // no longer in this set because its been deleted
            return;
         }
         if(IS_ASSIGNED_DEVICE.apply(m)) {
            // no longer in this set because its been assigned to someone
            return;
         }
         if(present) {
            onFobDeparted(context, m, false);
         }
         else {
            onFobArrived(context, m, false);
         }
      }
      
   }
   
   private class PersonBinder extends AddressesVariableBinder<PresenceSubsystemModel> {
      public PersonBinder() {
         super(IS_PERSON, "persons");
      }
      
      @Override
      protected void afterRemoved(
            SubsystemContext<PresenceSubsystemModel> context,
            Address address
      ) {
         onPersonDeleted(context, address);
      }
   }

}

