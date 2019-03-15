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
package com.iris.agent.config;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.iris.agent.test.SystemTestCase;

@Ignore
@RunWith(JUnit4.class)
public class TestConfigService extends SystemTestCase {
   @Test
   public void testConfigStrings() throws Exception {
      final int NUM = 100;

      for (int i = 0; i < NUM; ++i) {
         ConfigService.put("str" + i, "value" + i);
      }

      for (int i = 0; i < 100*NUM; ++i) {
         String value = ConfigService.get("str" + (i % NUM), String.class, "unknown");
         Assert.assertEquals("value" + (i % NUM), value);
      }
   }

   @Test
   public void testConfigIntegers() throws Exception {
      final int NUM = 100;

      for (int i = 0; i < NUM; ++i) {
         ConfigService.put("int" + i, i);
      }

      for (int i = 0; i < 100*NUM; ++i) {
         int value = ConfigService.get("int" + (i % NUM), int.class, -1);
         Assert.assertEquals(i % NUM, value);
      }
   }

   @Test
   public void testConfigLong() throws Exception {
      final long NUM = 100;

      for (long i = 0; i < NUM; ++i) {
         ConfigService.put("long" + i, i);
      }

      for (long i = 0; i < 100*NUM; ++i) {
         long value = ConfigService.get("long" + (i % NUM), long.class, -1L);
         Assert.assertEquals(i % NUM, value);
      }
   }
   
   @Test 
   public void testConfigByteArray() throws Exception {
      byte[] bytes = new byte[16];
      int i;
      for ( i = 0;i < 16;i++ ) {
         bytes[i] = (byte) (i + 1);
      }
      ConfigService.put("bytes", bytes);
      byte[] out = ConfigService.get("bytes", byte[].class);
     
      Assert.assertNotNull(out);
      for ( i = 0;i < 16;i++ ) {
         Assert.assertTrue ( out[i] == ((byte) i + 1 ));
      }
   }
}

