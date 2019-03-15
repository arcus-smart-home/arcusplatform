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
package com.iris.platform.alarm.notification.calltree.cassandra;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.PersonDAO;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.type.CallTreeEntry;
import com.iris.platform.alarm.notification.calltree.CallTreeDAO;
import com.iris.platform.subsystem.SubsystemDao;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class CassandraCallTreeDao implements CallTreeDAO {

	private final SubsystemDao subsystemDao;
	private final PersonDAO personDao;
	
	@Inject
	public CassandraCallTreeDao(SubsystemDao subsytemDao, PersonDAO personDao) {
		this.subsystemDao = subsytemDao;
		this.personDao = personDao;
	}	

	@Override
	public List<CallTreeEntry> callTreeForPlace(UUID placeId) {
		Model alarmSubsystemModel = subsystemDao.findByPlaceAndNamespace(placeId, AlarmSubsystemCapability.NAMESPACE);
		List<Map<String, Object>> callTree = AlarmSubsystemModel.getCallTree(alarmSubsystemModel);
		if(callTree != null && !callTree.isEmpty()) {
			return 
					callTree
						.stream()
						.filter(Objects::nonNull)
						.map((cur) -> new CallTreeEntry(cur))
						.collect(Collectors.toList());
		}else {
			return ImmutableList.<CallTreeEntry>of();
		}
	}

	@Override
	public PersonDAO personDAO() {
		return personDao;
	}
}

