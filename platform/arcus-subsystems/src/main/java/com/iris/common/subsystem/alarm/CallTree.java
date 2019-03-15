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
package com.iris.common.subsystem.alarm;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.or;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.util.AddressesVariableBinder;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Model;
import com.iris.messages.model.serv.PersonModel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.type.CallTreeEntry;
import com.iris.model.predicate.Predicates;
import com.iris.util.IrisUUID;
import com.iris.util.Subscription;

public class CallTree {
	public static final int MAX_CALLTREE_ENTRIES = 6;
	
	private static final Predicate<Model> isCallTreeEligible(String placeId) {
		return
			and(
					Predicates.isA(PersonCapability.NAMESPACE), 
					or(Predicates.attributeNotEmpty(PersonCapability.ATTR_FIRSTNAME), Predicates.attributeNotEmpty(PersonCapability.ATTR_LASTNAME)),
					Predicates.attributeNotEmpty(PersonCapability.ATTR_MOBILENUMBER),
					Predicates.attributeContains(PersonCapability.ATTR_PLACESWITHPIN, placeId)
			);
	}
	
	private final CallTreeBinder AddressBinder = new CallTreeBinder(IrisUUID.nilUUID());
	
	public CallTree() {
	}
	
	public Subscription bind(SubsystemContext<AlarmSubsystemModel> context) {
		return new CallTreeBinder(context.getPlaceId()).bind(context);
	}
	
	public void assertValid(SubsystemContext<AlarmSubsystemModel> context, List<Map<String, Object>> callTree) {
		Errors.assertValidRequest(callTree != null && !callTree.isEmpty(), "Call tree may not be empty");
		CallTreeEntry first = new CallTreeEntry(callTree.get(0));
		Errors.assertValidRequest(
				Objects.equals(first.getPerson(), SubsystemUtils.getAccountOwnerAddress(context).getRepresentation()), 
				"Account owner must be first in the call tree"
		);
		Errors.assertValidRequest(first.getEnabled(), "Account owner must be enabled");
		
		int size = 1;
		Set<String> treePeopleAddresses = new CallTreeBinder(context.getPlaceId()).getAddresses(context);
		for(int i=1; i<callTree.size(); i++) {
			CallTreeEntry treePerson = new CallTreeEntry(callTree.get(i));
			if(
					// null-safe true, if it's disabled don't bother validating
					Boolean.TRUE.equals(treePerson.getEnabled())
			) {
				if(!treePeopleAddresses.contains(treePerson.getPerson())) {
					// figure out what went wrong
					Model model = context.models().getModelByAddress(Address.fromString(treePerson.getPerson()));
					Errors.assertValidRequest(model != null, "No person with address [" + treePerson.getPerson() + "] exists");
					Errors.assertValidRequest(
							!StringUtils.isEmpty(PersonModel.getFirstName(model)) ||
							!StringUtils.isEmpty(PersonModel.getLastName(model)), 
							"Person [" + treePerson.getPerson() + "] must have a name to be added to the call tree"
					);
					Errors.assertValidRequest(
							!StringUtils.isEmpty(PersonModel.getMobileNumber(model)),
							"Person [" + treePerson.getPerson() + "] must have a phone number to be added to the call tree"
					);
					Errors.assertValidRequest(
							PersonModel.getPlacesWithPin(model, ImmutableSet.<String>of()).contains(context.getPlaceId().toString()),
							"Person [" + treePerson.getPerson() + "] must have a pin at place [SERV:place:" + context.getPlaceId() + "] to be added to the call tree"
					);
				}				
				// if no exception was thrown
				size++;
			}
		}
		
		Errors.assertValidRequest(size <= MAX_CALLTREE_ENTRIES, "Too many call tree entries: " + size + ", only " + MAX_CALLTREE_ENTRIES + " allowed");
	}
	
	public List<Map<String, Object>> normalize(SubsystemContext<AlarmSubsystemModel> context, List<Map<String, Object>> callTree) {
		String accountOwner = SubsystemUtils.getAccountOwnerAddress(context).getRepresentation();
		Set<String> persons = new HashSet<>(AddressBinder.getAddresses(context));
		List<Map<String, Object>> normalized = new ArrayList<>(persons.size());
		int enabled = 0;
		if(persons.remove(accountOwner)) {
			CallTreeEntry cte = new CallTreeEntry();
			cte.setPerson(accountOwner);
			cte.setEnabled(true);
			normalized.add(cte.toMap());
			enabled++;
		}
		for(Map<String, Object> callTreeEntry: callTree) {
			if(callTreeEntry == null) {
				continue;
			}
			
			CallTreeEntry e = new CallTreeEntry(callTreeEntry);
			if(!persons.contains(e.getPerson())) {
				if(!Objects.equals(accountOwner, e.getPerson())) {
					context.logger().warn("Skipping person [{}] because they aren't supported by this call tree", e.getPerson());
				}
				continue;
			}
			if(e.getEnabled() == null) {
				e.setEnabled(false);
			}
			if(e.getEnabled()) {
				enabled++;
			}
			if(enabled > MAX_CALLTREE_ENTRIES) {
				context.logger().warn("Disabling person [{}] because there are too many call tree entries", e.getPerson());
				e.setEnabled(false);
			}
			
			if(Objects.equals(accountOwner, e.getPerson())) {
				normalized.add(0, e.toMap());
			}
			else {
				normalized.add(e.toMap());
			}
			persons.remove(e.getPerson());
		}
		for(String person: persons) {
			CallTreeEntry e = new CallTreeEntry();
			e.setPerson(person);
			if(Objects.equals(accountOwner, e.getPerson())) {
				e.setEnabled(true);
				normalized.add(0, e.toMap());
			}
			else {
				e.setEnabled(false);
				normalized.add(e.toMap());
			}
		}
		return normalized;
	}

	public void setCallTree(SubsystemContext<AlarmSubsystemModel> context, List<Map<String, Object>> callTree) {
		List<Map<String, Object>> normalized = normalize(context, callTree);
		context.model().setCallTree(normalized);
	}
	
	private class CallTreeBinder extends AddressesVariableBinder<AlarmSubsystemModel> {
		
		private CallTreeBinder(UUID placeId) {
			super(isCallTreeEligible(placeId.toString()), "treePeople");
		}
		
		@Override
		protected void init(SubsystemContext<AlarmSubsystemModel> context) {
			context.logger().debug("Initializing call tree");
			context.model().setCallTree(normalize(context, AlarmSubsystemModel.getCallTree(context.model(), ImmutableList.<Map<String,Object>>of())));
			super.init(context);
		}

		@Override
		protected void afterAdded(SubsystemContext<AlarmSubsystemModel> context, Model model) {
			boolean isAccountOwner = Objects.equals(model.getAddress(), SubsystemUtils.getAccountOwnerAddress(context));
			
			CallTreeEntry entry = new CallTreeEntry();
			entry.setPerson(model.getAddress().getRepresentation());
			entry.setEnabled(isAccountOwner);

			List<Map<String, Object>> entries = AlarmSubsystemModel.getCallTree(context.model(), ImmutableList.<Map<String, Object>>of());
			List<Map<String, Object>> updates = new ArrayList<>(entries.size() + 1);
			if(isAccountOwner) {
				updates.add(entry.toMap());
				updates.addAll(entries);
			}
			else {
				updates.addAll(entries);
				updates.add(entry.toMap());
			}
			
			context.model().setCallTree(updates);
		}

		@Override
		protected void afterRemoved(SubsystemContext<AlarmSubsystemModel> context, Address address) {
			List<Map<String, Object>> updates = new ArrayList<>(AlarmSubsystemModel.getCallTree(context.model(), ImmutableList.<Map<String, Object>>of()));
			Iterator<Map<String, Object>> it = updates.iterator();
			String person = address.getRepresentation();
			while(it.hasNext()) {
				if(person.equals(it.next().get(CallTreeEntry.ATTR_PERSON))) {
					it.remove();
				}
			}
			
			context.model().setCallTree(updates);
		}
		
	}

}

