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
package com.iris.common.rule.simple;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.event.ScheduledEventHandle;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;
import com.iris.model.type.AttributeTypes;

/**
 * Allows a set of variables to override the values from
 * the delegate context
 */
public abstract class DelegatingContext implements RuleContext {  

	protected RuleContext delegate;

   public DelegatingContext(RuleContext delegate) { 
      Preconditions.checkNotNull(delegate, "delegate may not be null");
      this.delegate = delegate;
   }

   @Override
   public boolean isDirty() {
      return delegate.isDirty();
   }

   @Override
   public void clearDirty() {
      delegate.clearDirty();
   }
   
   @Override
   public Map<String, Object> getDirtyVariables() {
      return delegate.getDirtyVariables();
   }

   
   @Override
   public UUID getPlaceId() {
      return delegate.getPlaceId();
   }
   
   @Override
	public String getPopulation() {
		return delegate.getPopulation();
	}

   @Override
   public boolean isPremium() {
      return delegate.isPremium();
   }

   @Override
   public String getServiceLevel() {
      return delegate.getServiceLevel();
   }

   @Override
   public Map<String, Object> getVariables() {
      return delegate.getVariables();
   }

   @Override
   public Object getVariable(String name) {
      return delegate.getVariable(name);
   }

   @Override
   public <T> T getVariable(String name, Class<T> type) {
      return delegate.getVariable(name, type);
   }

   @Override
   public Object setVariable(String name, Object value) {
      return delegate.setVariable(name, value);
   }

   @Override
   public Logger logger() {
      return delegate.logger();
   }

   @Override
   public Calendar getLocalTime() {
      return delegate.getLocalTime();
   }
   
   @Override
   public Iterable<Model> getModels() {
      return delegate.getModels();
   }

   @Override
   public Model getModelByAddress(Address address) {
      return delegate.getModelByAddress(address);
   }

   @Override
   public Object getAttributeValue(Address address, String attributeName) {
      return delegate.getAttributeValue(address, attributeName);
   }

   @Override
   public void broadcast(MessageBody message) {
      delegate.broadcast(message);
   }

   @Override
   public void send(Address address, MessageBody message) {
      delegate.send(address, message);
   }

   @Override
   public String request(Address address, MessageBody message) {
      return delegate.request(address, message);
   }

   @Override
   public ScheduledEventHandle wakeUpIn(long time, TimeUnit unit) {
      return delegate.wakeUpIn(time, unit);
   }

   @Override
   public ScheduledEventHandle wakeUpAt(Date timestamp) {
      return delegate.wakeUpAt(timestamp);
   }
}

