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
package com.iris.core.dao.file;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.Inject;
import com.iris.core.dao.HubBlacklistDAO;
import com.iris.messages.model.BlacklistedHub;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@Modules({HubBlacklistDAOModule.class})
public class TestHubBlacklistDAOFileImpl extends IrisTestCase {
   
    @Override
	protected Set<String> configs() {
    	Set<String> configs = super.configs();
		configs.add("src/test/resources/test.properties");
		return configs;
	}	
	
	@Inject
	private HubBlacklistDAO hubBlacklistDAO;

    @Before
    public void setup(){  

      assertNotNull(hubBlacklistDAO);
    }
   
    @Test
    public void testFindBlacklistedHubByCertSn() {
		String cert = "testcertSn1";
		BlacklistedHub fromDb = hubBlacklistDAO.findBlacklistedHubByCertSn(cert);
		assertNotNull(fromDb);
		assertEquals(cert, fromDb.getCertSn());
	}

    @Test
	public void testSave() {
    	String cert = "testSaveCert1";
    	BlacklistedHub newHub = createNew(cert);
    	assertNotNull(newHub);
    	BlacklistedHub fromDb = hubBlacklistDAO.findBlacklistedHubByCertSn(cert);
    	assertEquals(newHub, fromDb);
	}
    
    
    private BlacklistedHub createNew(String cert) {
    	BlacklistedHub newHub = new BlacklistedHub();
    	newHub.setBlacklistedDate(new Date());
    	newHub.setCertSn(cert);
    	newHub.setHubid(UUID.randomUUID().toString());
    	newHub.setReason("testSaveReason");
    	newHub = hubBlacklistDAO.save(newHub);
    	return newHub;
    }

	@Test
	public void testDelete() {
		String cert = "testSaveCert1";
    	BlacklistedHub newHub = createNew(cert);
    	assertNotNull(newHub);
    	BlacklistedHub fromDb = hubBlacklistDAO.findBlacklistedHubByCertSn(cert);
    	assertNotNull(fromDb);
    	hubBlacklistDAO.delete(fromDb);
    	fromDb = hubBlacklistDAO.findBlacklistedHubByCertSn(cert);
    	assertNull(fromDb);
	}
	
}

