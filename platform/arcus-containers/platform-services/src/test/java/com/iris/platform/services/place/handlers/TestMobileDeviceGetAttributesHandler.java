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
package com.iris.platform.services.place.handlers;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.inject.Inject;
import com.iris.capability.attribute.transform.AttributeMapTransformModule;
import com.iris.capability.attribute.transform.MobileDeviceAttributesTransformer;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.MobileDeviceCapability;
import com.iris.messages.model.Fixtures;
import com.iris.messages.model.MobileDevice;
import com.iris.platform.services.mobiledevice.handlers.MobileDeviceGetAttributesHandler;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Modules;

@Modules({ AttributeMapTransformModule.class, InMemoryMessageModule.class })
@RunWith(Parameterized.class)
public class TestMobileDeviceGetAttributesHandler extends IrisMockTestCase {

   @Inject
   MobileDeviceGetAttributesHandler handler;
   @Inject
   InMemoryPlatformMessageBus platformBus;

   enum TestName {
      IphoneAndNullName,
      Ipad,
      MissingAppleFormFactor,
      NullAppleFormFactor,
      AppleNoTransformation,
      AndroidTabletAndNullName,
      AndroidPhone,
      MissingAndroidFormFactor,
      NullAndroidFormFactor,
      MissingAndroidResolution,
      NullAndroidResolution,
      AndroidNoTransformation
   };

   TestName testName;
   String formFactor;
   String resolution;
   String deviceVendor;
   String name;
   String appVersion;

   MobileDevice mobileDevice;

   private static final String SOME_VERSION = "1.0";
   private static final String MY_IPHONE = "My iPhone";
   private static final String MY_ANDROID = "My Android";
   
   @Parameters(name = "{0} - {1}")
   public static List<Object[]> parameters() {
      return Arrays.asList(
            new Object[] { TestName.IphoneAndNullName, "phone", null, "Apple", null, null },
            new Object[] { TestName.Ipad, "tablet", null, "Apple", null, SOME_VERSION },
            new Object[] { TestName.MissingAppleFormFactor, "", null, "Apple", null, SOME_VERSION },
            new Object[] { TestName.NullAppleFormFactor, null, null, "Apple", null, SOME_VERSION },
            new Object[] { TestName.AppleNoTransformation, "Phone", null, "Apple", MY_IPHONE, SOME_VERSION },
            new Object[] { TestName.AndroidTabletAndNullName, "XXHDPI", "2560 x 1800", "Google", null, null }, // GooGle pixel c
            new Object[] { TestName.AndroidPhone, "XXHDPI", "1080 x 1920", "HTC", null, SOME_VERSION }, // HTC One M9
            new Object[] { TestName.MissingAndroidFormFactor, "", "1080 x 1920", "HTC", null, SOME_VERSION },
            new Object[] { TestName.NullAndroidFormFactor, null, "1080 x 1920", "HTC", null, SOME_VERSION },
            new Object[] { TestName.MissingAndroidResolution, "LDPI", "", "HTC", null, SOME_VERSION },
            new Object[] { TestName.NullAndroidResolution, "MDpI", null, "HTC", null, SOME_VERSION },
            new Object[] { TestName.AndroidNoTransformation, "MDpI", null, "HTC", MY_ANDROID, SOME_VERSION }
            );
   }

   public TestMobileDeviceGetAttributesHandler(TestName testName, String formFactor, String resolution, String deviceVendor, String name, String appVersion) {
      this.testName = testName;
      this.formFactor = formFactor;
      this.resolution = resolution;
      this.deviceVendor = deviceVendor;
      this.name = name;
      this.appVersion = appVersion;
   }

   @Override
   public void setUp() throws Exception {
      super.setUp();
      mobileDevice = new MobileDevice();
      mobileDevice.setAssociated(new Date());
      mobileDevice.setDeviceIdentifier("deviceidentifier");
      mobileDevice.setDeviceIndex(1);
      mobileDevice.setDeviceModel("deviceModel");
      mobileDevice.setLastLatitude(38.97);
      mobileDevice.setLastLongitude(95.23);
      mobileDevice.setLastLocationTime(new Date());
      mobileDevice.setNotificationToken("token");
      mobileDevice.setOsType("ios");
      mobileDevice.setOsVersion("Version 9.3.1 (Build 13E238)");
      mobileDevice.setPersonId(UUID.randomUUID());
      mobileDevice.setPhoneNumber("555-555-5555");      
   }

   @Before
   public void initMobileDevice() {
      // From TestParameters
      mobileDevice.setFormFactor(formFactor);
      mobileDevice.setResolution(resolution);
      mobileDevice.setDeviceVendor(deviceVendor);
      mobileDevice.setName(name);
      mobileDevice.setAppVersion(appVersion);
   }

   @Test
   public void testIphoneAndNullName() throws Exception {
      Assume.assumeTrue(testName == TestName.IphoneAndNullName);
      Map<String, Object> attributes = getAttributesForRequest(MobileDeviceCapability.ATTR_APPVERSION, MobileDeviceCapability.ATTR_NAME);
      assertEquals(attributes.get(MobileDeviceCapability.ATTR_NAME), MobileDeviceAttributesTransformer.AppleDeviceType.IPHONE.getFriendlyName());
      assertEquals(attributes.get(MobileDeviceCapability.ATTR_APPVERSION), "UNKNOWN");
   }

   @Test
   public void testIpad() throws Exception {
      Assume.assumeTrue(testName == TestName.Ipad);
      Map<String, Object> attributes = getAttributesForRequest(MobileDeviceCapability.ATTR_APPVERSION, MobileDeviceCapability.ATTR_NAME);
      assertEquals(attributes.get(MobileDeviceCapability.ATTR_NAME), MobileDeviceAttributesTransformer.AppleDeviceType.IPAD.getFriendlyName());
   }

   @Test
   public void testMissingAppleFormFactor() throws Exception {
      Assume.assumeTrue(testName == TestName.MissingAppleFormFactor);
      Map<String, Object> attributes = getAttributesForRequest(MobileDeviceCapability.ATTR_APPVERSION, MobileDeviceCapability.ATTR_NAME);
      assertEquals(attributes.get(MobileDeviceCapability.ATTR_NAME), MobileDeviceAttributesTransformer.AppleDeviceType.GENERIC.getFriendlyName());
   }

   @Test
   public void testNullAppleFormFactor() throws Exception {
      Assume.assumeTrue(testName == TestName.NullAppleFormFactor);
      Map<String, Object> attributes = getAttributesForRequest(MobileDeviceCapability.ATTR_APPVERSION, MobileDeviceCapability.ATTR_NAME);
      assertEquals(attributes.get(MobileDeviceCapability.ATTR_NAME), MobileDeviceAttributesTransformer.AppleDeviceType.GENERIC.getFriendlyName());
   }
   
   @Test
   public void testAppleNoTransformation() throws Exception {
      Assume.assumeTrue(testName == TestName.AppleNoTransformation);
      Map<String, Object> attributes = getAttributesForRequest(MobileDeviceCapability.ATTR_APPVERSION, MobileDeviceCapability.ATTR_NAME);
      assertEquals(attributes.get(MobileDeviceCapability.ATTR_NAME), TestMobileDeviceGetAttributesHandler.MY_IPHONE);
      assertEquals(attributes.get(MobileDeviceCapability.ATTR_APPVERSION), TestMobileDeviceGetAttributesHandler.SOME_VERSION);
   }   
   
   @Test
   public void testAndroidTabletAndNullName() throws Exception {
      Assume.assumeTrue(testName == TestName.AndroidTabletAndNullName);
      Map<String, Object> attributes = getAttributesForRequest(MobileDeviceCapability.ATTR_APPVERSION, MobileDeviceCapability.ATTR_NAME);
      assertEquals(attributes.get(MobileDeviceCapability.ATTR_NAME), MobileDeviceAttributesTransformer.AndroidDeviceType.TABLET.getFriendlyName());
      assertEquals(attributes.get(MobileDeviceCapability.ATTR_APPVERSION), "UNKNOWN");
   }  
  
   @Test
   public void testAndroidPhone() throws Exception {
      Assume.assumeTrue(testName == TestName.AndroidPhone);
      Map<String, Object> attributes = getAttributesForRequest(MobileDeviceCapability.ATTR_APPVERSION, MobileDeviceCapability.ATTR_NAME);
      assertEquals(attributes.get(MobileDeviceCapability.ATTR_NAME), MobileDeviceAttributesTransformer.AndroidDeviceType.PHONE.getFriendlyName());
   }   
   
   @Test
   public void testMissingAndroidFormFactor() throws Exception {
      Assume.assumeTrue(testName == TestName.MissingAndroidResolution);
      Map<String, Object> attributes = getAttributesForRequest(MobileDeviceCapability.ATTR_APPVERSION, MobileDeviceCapability.ATTR_NAME);
      assertEquals(attributes.get(MobileDeviceCapability.ATTR_NAME), MobileDeviceAttributesTransformer.AndroidDeviceType.GENERIC.getFriendlyName());
   }

   @Test
   public void testNullAndroidFormFactor() throws Exception {
      Assume.assumeTrue(testName == TestName.NullAndroidResolution);
      Map<String, Object> attributes = getAttributesForRequest(MobileDeviceCapability.ATTR_APPVERSION, MobileDeviceCapability.ATTR_NAME);
      assertEquals(attributes.get(MobileDeviceCapability.ATTR_NAME), MobileDeviceAttributesTransformer.AndroidDeviceType.GENERIC.getFriendlyName());
   }   
   
   @Test
   public void testAndroidNoTransformation() throws Exception {
      Assume.assumeTrue(testName == TestName.AndroidNoTransformation);
      Map<String, Object> attributes = getAttributesForRequest(MobileDeviceCapability.ATTR_APPVERSION, MobileDeviceCapability.ATTR_NAME);
      assertEquals(attributes.get(MobileDeviceCapability.ATTR_NAME), TestMobileDeviceGetAttributesHandler.MY_ANDROID);
      assertEquals(attributes.get(MobileDeviceCapability.ATTR_APPVERSION), TestMobileDeviceGetAttributesHandler.SOME_VERSION);
   }   
   
   private Map<String, Object> getAttributesForRequest(String... getAttributes) {
      UUID devId = UUID.randomUUID();
      PlatformMessage mesg = Fixtures.createGetAttributes(devId, getAttributes);
      MessageBody body = handler.handleRequest(mobileDevice, mesg);
      return body.getAttributes();
   }  
}

