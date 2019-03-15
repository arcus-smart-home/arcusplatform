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
package com.iris.platform.subsystem.placemonitor;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.w3c.dom.Document;

import com.iris.platform.subsystem.placemonitor.config.WatchedJAXBConfigFileReference;
import com.iris.platform.subsystem.placemonitor.config.XMLUtil;
import com.iris.platform.subsystem.placemonitor.offlinenotifications.NotificationThresholdsConfig;
import com.iris.resource.Resource;
import com.iris.resource.Resources;
import com.iris.test.IrisTestCase;

public class TestJAXBConfig extends IrisTestCase {

   private AtomicReference<NotificationThresholdsConfig> ref; 

   @Override
   public void setUp() throws Exception {
      super.setUp();
      ref = new WatchedJAXBConfigFileReference<NotificationThresholdsConfig>("classpath:/conf/notification-thresholds-config.xml",NotificationThresholdsConfig.class).getReference();
   }

   @Test(expected = RuntimeException.class)
   public void testFileNotFound() {
      new WatchedJAXBConfigFileReference<NotificationThresholdsConfig>("classpath:/conf/some-nonexistent-file.xml",NotificationThresholdsConfig.class).getReference();
      fail("Watched non-existent file");
   }
   
   @Test
   public void testDeserialize() {
      NotificationThresholdsConfig config = ref.get();
      assertEquals(86400, config.getDeviceOfflineTimeoutSec(""));
   }

   @Test
   public void testSchema() {
      Document doc = com.iris.io.xml.XMLUtil.parseDocumentDOM("conf/notification-thresholds-config.xml","classpath:/schema/notification-thresholds-config.xsd");
      assertNotNull("doc was validated successfully",doc);
   }

   @Test
   public void testJAXBDeserialize() throws Exception{
      Resource resource = Resources.getResource("classpath:/conf/notification-thresholds-config.xml");
      NotificationThresholdsConfig config = XMLUtil.deserializeJAXB(resource, NotificationThresholdsConfig.class);
      config.afterLoad();
      assertEquals(86400, config.getDeviceOfflineTimeoutSec(""));
      XMLUtil.serializeJAXB(config, System.out);
   }
   
}

