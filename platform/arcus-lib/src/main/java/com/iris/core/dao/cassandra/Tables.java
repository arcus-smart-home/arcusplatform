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
package com.iris.core.dao.cassandra;

/**
 * Collection of table definitions shared between DAOs.
 * 
 * @author wlarson
 *
 */
class Tables {
	
	static final String LOGIN = "login";

	static final String MOBILE_DEVICES = "mobiledevices";
	
	static final String NOTIFICATION_TOKEN_MOBILE_DEVICE = "notificationtoken_mobiledevice";
	
	static final String PERSON = PersonDAOImpl.TABLE;
	
	static final class PersonCols {
		public static final String ID = BaseCassandraCRUDDao.BaseEntityColumns.ID;
		public static final String CREATED = BaseCassandraCRUDDao.BaseEntityColumns.CREATED;
		public static final String MODIFIED = BaseCassandraCRUDDao.BaseEntityColumns.MODIFIED;
		public static final String IMAGES = BaseCassandraCRUDDao.BaseEntityColumns.IMAGES;
		public static final String TAGS = BaseCassandraCRUDDao.BaseEntityColumns.TAGS;
		public final static String ACCOUNT_ID = PersonDAOImpl.PersonEntityColumns.ACCOUNT_ID;
		public final static String CONSENT_OFFERSPROMOTIONS = PersonDAOImpl.PersonEntityColumns.CONSENT_OFFERSPROMOTIONS;
		public final static String CONSENT_STATEMENT = PersonDAOImpl.PersonEntityColumns.CONSENT_STATEMENT;
		public final static String CURRENT_LOCATION = PersonDAOImpl.PersonEntityColumns.CURRENT_LOCATION;
		public final static String CURRENT_LOCATION_METHOD = PersonDAOImpl.PersonEntityColumns.CURRENT_LOCATION_METHOD;
		public final static String CURRENT_LOCATION_TIME = PersonDAOImpl.PersonEntityColumns.CURRENT_LOCATION_TIME;
		public final static String CURRENT_PLACE = PersonDAOImpl.PersonEntityColumns.CURRENT_PLACE;
		public final static String CURRENT_PLACE_METHOD = PersonDAOImpl.PersonEntityColumns.CURRENT_PLACE_METHOD;
		public final static String EMAIL = PersonDAOImpl.PersonEntityColumns.EMAIL;
		public final static String EMAIL_VERIFIED = PersonDAOImpl.PersonEntityColumns.EMAIL_VERIFIED;
		public final static String FIRST_NAME = PersonDAOImpl.PersonEntityColumns.FIRST_NAME;
		public final static String HAS_LOGIN = PersonDAOImpl.PersonEntityColumns.HAS_LOGIN;
		public final static String LAST_NAME = PersonDAOImpl.PersonEntityColumns.LAST_NAME;
		public final static String MOBILE_NOTIFICATION_ENDPOINTS = PersonDAOImpl.PersonEntityColumns.MOBILE_NOTIFICATION_ENDPOINTS;
		public final static String MOBILE_NUMBER = PersonDAOImpl.PersonEntityColumns.MOBILE_NUMBER;
		public final static String MOBILE_VERIFIED = PersonDAOImpl.PersonEntityColumns.MOBILE_VERIFIED;
		@Deprecated
		public final static String PIN = PersonDAOImpl.PersonEntityColumns.PIN;
		public final static String PIN2 = PersonDAOImpl.PersonEntityColumns.PIN2;
		public final static String PRIVACY_POLICY_AGREED = PersonDAOImpl.PersonEntityColumns.PRIVACY_POLICY_AGREED;
		public final static String SECURITY_ANSWERS = PersonDAOImpl.PersonEntityColumns.SECURITY_ANSWERS;
		public final static String TERMS_AGREED = PersonDAOImpl.PersonEntityColumns.TERMS_AGREED;
		public final static String MOBILE_DEVICE_SEQUENCE = "mobileDeviceSequence";
		public final static String HANDOFF_TOKEN = "handoffToken";
	}

	static final class MobileDevicesCols {
		public static final String PERSON_ID = "personId";
		public static final String DEVICE_INDEX = "deviceIndex";
		public static final String ASSOCIATED = "associated";
		public static final String OS_TYPE = "osType";
		public static final String OS_VERSION = "osVersion";
		public static final String FORM_FACTOR = "formFactor";
		public static final String PHONE_NUMBER = "phoneNumber";
		public static final String DEVICE_IDENTIFIER = "deviceIdentifier";
		public static final String DEVICE_MODEL = "deviceModel";
		public static final String DEVICE_VENDOR = "deviceVendor";
		public static final String RESOLUTION = "resolution";
		public static final String NOTIFICATION_TOKEN = "notificationToken";
		public static final String LAST_LATITUDE = "lastLatitude";
		public static final String LAST_LONGITUDE = "lastLongitude";
		public static final String LAST_LOCATION_TIME = "lastLocationTime";
		public static final String NAME = "name";	
		public static final String APP_VERSION = "appVersion";		
	}
	
	static final class NotificationTokenMobileDeviceCols {
		public final static String NOTIFICATION_TOKEN = "notificationToken";
		public final static String PERSON_ID = "personId";
		public final static String DEVICE_INDEX = "deviceIndex";
	}
}

