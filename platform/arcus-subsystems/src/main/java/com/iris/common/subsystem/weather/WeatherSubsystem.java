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
package com.iris.common.subsystem.weather;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.inject.Singleton;
import com.iris.annotation.Version;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.common.subsystem.util.AddressesAttributeBinder;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.WeatherRadioCapability;
import com.iris.messages.capability.WeatherRadioCapability.StopPlayingStationRequest;
import com.iris.messages.capability.WeatherSubsystemCapability;
import com.iris.messages.capability.WeatherSubsystemCapability.SnoozeAllAlertsRequest;
import com.iris.messages.capability.WeatherSubsystemCapability.SnoozeAllAlertsResponse;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.listener.annotation.OnAdded;
import com.iris.messages.listener.annotation.OnRemoved;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.WeatherSubsystemModel;
import com.iris.model.query.expression.ExpressionCompiler;
import com.iris.util.TypeMarker;

@Singleton
@Subsystem(WeatherSubsystemModel.class)
@Version(1)
public class WeatherSubsystem extends BaseSubsystem<WeatherSubsystemModel> {

	static final String QUERY_RADIOS = "noaa:alertstate is supported";
	static final String QUERY_ALERTING = "noaa:alertstate == 'ALERT' or noaa:alertstate == 'ALERT_HUSHED'";
	static final Predicate<Model> IS_RADIO = ExpressionCompiler.compile(QUERY_RADIOS);
	static final Predicate<Model> IS_ALERTING = ExpressionCompiler.compile(QUERY_ALERTING);
	static final Predicate<Model> IS_RADIO_AND_ALERTING = Predicates.and(IS_RADIO, IS_ALERTING);	
	
	private final AddressesAttributeBinder<WeatherSubsystemModel> radios = 
         new AddressesAttributeBinder<WeatherSubsystemModel>(IS_RADIO, WeatherSubsystemCapability.ATTR_WEATHERRADIOS) {
            @Override
            protected void afterAdded(SubsystemContext<WeatherSubsystemModel> context, Model added) {
               context.logger().info("A new weather radio was added {}", added.getAddress());
               syncAlerts(context);
            }
            
            @Override
            protected void afterRemoved(SubsystemContext<WeatherSubsystemModel> context, Address address) {
               context.logger().info("A weather radio was removed {}", address);
               syncAlerts(context);
            }
         };
   
   @Override
   protected void onAdded(SubsystemContext<WeatherSubsystemModel> context) {
      super.onAdded(context);
   }

   @Override
   protected void onStarted(SubsystemContext<WeatherSubsystemModel> context) {
      super.onStarted(context);
      radios.bind(context);
      syncAlerts(context);
   }
   
   @OnAdded(query = QUERY_RADIOS)
   public void onDeviceAdded(ModelAddedEvent event, SubsystemContext<WeatherSubsystemModel> context) {
      context.logger().info("a new weather device was added {}", event);
      syncAlerts(context);
   }

   @OnRemoved(query = QUERY_RADIOS)
   public void onDeviceRemoved(ModelRemovedEvent event, SubsystemContext<WeatherSubsystemModel> context) {
      context.logger().info("a new weather device was removed {}", event);
      syncAlerts(context);
   }
   
   @OnValueChanged(attributes = {WeatherRadioCapability.ATTR_ALERTSTATE})
   public void onAlertStateChange(ModelChangedEvent event,SubsystemContext<WeatherSubsystemModel> context){
   	syncAlerts(context);
   }

   @OnValueChanged(attributes = {WeatherRadioCapability.ATTR_CURRENTALERT})
   public void onCurrentAlertChange(ModelChangedEvent event,SubsystemContext<WeatherSubsystemModel> context){
   	syncAlerts(context);
   }
   
   @Request(SnoozeAllAlertsRequest.NAME)
   public MessageBody onSnoozeAllAlerts(PlatformMessage message, SubsystemContext<WeatherSubsystemModel> context) {
      SubsystemUtils.sendTo(context,IS_RADIO_AND_ALERTING, StopPlayingStationRequest.instance());
      return SnoozeAllAlertsResponse.instance();
   }
   
   private void syncAlerts(SubsystemContext<WeatherSubsystemModel> context){
      Map<String, Set<String>>alerts = new HashMap<String, Set<String>>();

      for(Model model:context.models().getModels(IS_RADIO)){
         if(!(IS_ALERTING.apply(model))){
            continue;
         }
         String easCode = model.getAttribute(TypeMarker.string(), WeatherRadioCapability.ATTR_CURRENTALERT).or("").toUpperCase();
         Set<String> addresses = alerts.get(easCode);
         if (addresses == null) {
         	addresses = new HashSet<String>();
         	alerts.put(easCode, addresses);
         }
         addresses.add(model.getAddress().getRepresentation());
      }
      
      if (!alerts.isEmpty()) {
      	context.model().setAttribute(WeatherSubsystemCapability.ATTR_LASTWEATHERALERTTIME, new Date());
      	context.model().setAttribute(WeatherSubsystemCapability.ATTR_WEATHERALERT, WeatherSubsystemCapability.WEATHERALERT_ALERT);
      } else {
      	context.model().setAttribute(WeatherSubsystemCapability.ATTR_WEATHERALERT, WeatherSubsystemCapability.WEATHERALERT_READY);      	
      }
      context.model().setAttribute(WeatherSubsystemCapability.ATTR_ALERTINGRADIOS, alerts);
      
      syncSubsystemAvailable(context);
   }

   protected void syncSubsystemAvailable(SubsystemContext<WeatherSubsystemModel> context) {
      if (Iterables.size(context.models().getModels(IS_RADIO)) > 0){
         context.model().setAvailable(true);
      }
      else{
         context.model().setAvailable(false);
      }
   }
	
}

