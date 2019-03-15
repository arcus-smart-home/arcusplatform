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
package com.iris.platform.model;

import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.iris.core.dao.CRUDDao;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.PersistentModel;

public class PersistentModelWrapper<P extends PersistentModel> {
	private final PlatformMessageBus messageBus;
	private final CRUDDao<String, P> dao;
	private final UUID placeId;
	private final String population;
	private P model;
	
	public PersistentModelWrapper(PlatformMessageBus messageBus, CRUDDao<String, P> dao, UUID placeId, String population, P model) {
		this.messageBus = messageBus;
		this.dao = dao;
		this.placeId = placeId;
		this.population = population;
		this.model = model;
	}

	/**
	 * A live view of the model, this will
	 * be {@code null} after delete has been called.
	 * @return
	 */
	@Nullable
	public P model() {
		return model;
	}
	
	public boolean isPersisted() {
		return model != null && model.isPersisted();
	}
	
	public boolean isDeleted() {
		return model == null;
	}
	
	public void emit(MessageBody event) {
		PlatformMessage message =
			PlatformMessage
				.broadcast()
				.from(model.getAddress())
				.withPlaceId(placeId)
				.withPopulation(population)
				.withPayload(event)
				.create();
		messageBus.send(message);
	}
	
	public void send(Address destination, MessageBody request) {
		PlatformMessage message =
			PlatformMessage
				.request(destination)
				.from(model.getAddress())
				.withPlaceId(placeId)
				.withPopulation(population)
				.withPayload(request)
				.create();
		messageBus.send(message);
	}
	
	public void save() {
		Preconditions.checkState(model != null, "This model has been deleted, can't be saved");
		if(!model.isPersisted()) {
			create();
		}
		else {
			update();
		}
	}
	
	private void create() {
		model = dao.save(model);
		emit(MessageBody.buildMessage(Capability.EVENT_ADDED, model.toMap()));
	}
	
	private void update() {
		Map<String, Object> changes = model.getDirtyAttributes();
		model = dao.save(model);
		emit(MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, changes));
	}
	
	public void delete() {
		if(model == null) {
			return;
		}
		dao.delete(model);
		emit(MessageBody.buildMessage(Capability.EVENT_DELETED));
		model = null;
	}

}

