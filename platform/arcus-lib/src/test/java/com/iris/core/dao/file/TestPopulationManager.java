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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.inject.Inject;
import com.iris.core.dao.PopulationDAO;
import com.iris.messages.type.Population;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

@RunWith(Parameterized.class)
@Modules({PopulationDAOModule.class})
public class TestPopulationManager extends IrisTestCase {
	
	@Parameters(name="name[{0}],default[{1}],exist[{2}], minHubV2Version[{3}],minHubV3Version[{4}]")
	public static List<Object []> files() {
      return Arrays.<Object[]>asList(
            new Object [] { "general", true, true,  "2.0.1.004", "3.0.0.008"},
            new Object [] { "beta", false, true,  "2.0.1.004", "3.0.0.008"},
            new Object [] { "qa", false, true,  "2.0.1.004", "3.0.0.008"},
            new Object [] { "alpha", false, true,  "2.2.1.004", "3.0.0.008"},
            new Object [] { "gamma", false, false,  null, null}
      );
   }
	
	private final String expectedPopulationName;
	private final String expectedMinHubV2Version;
	private final String expectedMinHubV3Version;
	private final boolean isDefault;
	private final boolean exist;
	
	public TestPopulationManager(String expectedPopulationName, boolean isDefault, boolean exist, String expectedMinHubV2Version, String expectedMinHubV3Version) {
		this.expectedPopulationName = expectedPopulationName;
		this.expectedMinHubV2Version = expectedMinHubV2Version;
		this.expectedMinHubV3Version = expectedMinHubV3Version;
		this.isDefault = isDefault;
		this.exist = exist;
	}
   
    @Override
	protected Set<String> configs() {
    	Set<String> configs = super.configs();
		configs.add("src/test/resources/test.properties");
		return configs;
	}

	@Inject
	private PopulationDAO popDao;
		
    @Before
    public void setup(){  
      assertNotNull(popDao);      
    }

	@Test
	public void testFindByName() {

		Population defaultPopulation = popDao.getDefaultPopulation();
		assertNotNull(defaultPopulation);
		assertEquals(Population.NAME_GENERAL, defaultPopulation.getName());
		
		Population curPop = popDao.findByName(expectedPopulationName);
		if(exist) {
			assertNotNull(curPop);
			if(isDefault) {
				assertEquals(defaultPopulation.getName(), curPop.getName());
			}else{
				assertNotEquals(defaultPopulation.getName(), curPop.getName());
			}
			assertEquals(expectedMinHubV2Version, curPop.getMinHubV2Version());
			assertEquals(expectedMinHubV3Version, curPop.getMinHubV3Version());
		}else{
			assertNull(curPop);
		}		
	}

	@Test
	public void testListPopulations() {
		List<Population> popList = popDao.listPopulations();
		assertTrue(popList.size() > 0);
	}
	
}

