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
package com.iris.core.platform.analytics;

public class AnalyticsMessageConstants {
	
	public final static String SUBSYSTEM_SAFETY_ALARM_TRIGGERED_EVENT = "subsys.safety.alarm.triggered.event";
	
	public final static String SUBSYSTEM_SAFETY_ALARM_TYPE = "subsys.safety.alarm.type";
	public final static String SUBSYSTEM_SECURITY_ALARM_ARMED_EVENT = "subsys.security.alarm.armed.event";
	public final static String SUBSYSTEM_SECURITY_ALARM_DISARMED_EVENT = "subsys.security.alarm.disarmed.event";
	public final static String SUBSYSTEM_SECURITY_ALARM_TRIGGERED_EVENT = "subsys.security.alarm.triggered.event";
	public final static String SUBSYSTEM_CARE_ALARM_TRIGGERED_EVENT = "subsys.care.alarm.triggered.event";
	
	public final static String NOTIFICATION_EVENT = "notification.message";
	public final static String NOTIFICATION_CUSTOM_EVENT = "notification.custom";
	public final static String NOTIFICATION_EMAIL_EVENT = "notification.email";
	
	public final static String NOTIFICATION_PERSON = "notification.person";
	public final static String NOTIFICATION_KEY = "notification.key";
	public final static String NOTIFICATION_METHOD = "notification.method";
	public final static String NOTIFICATION_PRIORITY = "notification.priority";
	
	public final static String ATTR_KEY_PERSON_ID = "person.id";  //UUID of the person
	public final static String ATTR_KEY_PLACE_ID = "place.id";  //UUID of the place
	public final static String ATTR_KEY_DEVICE_ADDR = "device.address";  //The Address of a device.
	public final static String ATTR_KEY_TARGET_ADDR = "target.address";  //The Address of something that is the target of an action.
}

