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
package com.iris.modelmanager.version.dao;

import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import com.iris.modelmanager.BindCapture;
import com.iris.modelmanager.Status;
import com.iris.modelmanager.version.VersionHistory;

public class VersionHistoryDAOTest {

   @Test
   public void testInsertBindsValuesTheDriverCanEncode() {
      Date applied = new Date();
      VersionHistory history = new VersionHistory();
      history.setVersion("2026.11.0");
      history.setStatus(Status.APPLIED);
      history.setTimestamp(applied);
      history.setUsername("user");

      BindCapture capture = new BindCapture();
      new VersionHistoryDAO(capture.session()).insert(history);

      capture.assertDriverCanEncode();
      assertTrue(capture.values().contains(applied.toInstant()));
   }
}
