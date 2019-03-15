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
package com.iris.firmware.ota;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import com.google.inject.Inject;
import com.iris.core.dao.PopulationDAO;
import com.iris.core.dao.file.PopulationDAOModule;
import com.iris.messages.type.Population;
import com.iris.resource.Resources;
import com.iris.resource.classpath.ClassPathResourceFactory;
import com.iris.resource.filesystem.FileSystemResourceFactory;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Modules;

@RunWith(value = Parameterized.class)
@Modules({PopulationDAOModule.class})
public class TestDeviceFirmwareResolver extends IrisMockTestCase {

	@Inject
   private DeviceOTAFirmwareResolver resolver;	
	@Inject
   private PopulationDAO populationDao;
	
	private final String population;
	private final String productId;
	private final String currentVersion;
	private final Integer retryAttmpts;
	private final boolean isUpgrade;
	private final String targetVersion;
	private final String targetImage;
	
	@Parameters(name="population[{0}],product[{1}],currentVersion[{2}], retryAttempts[{3}], isUpgrade[{4}],targetVersion[{5}],targetImage[{6}]")
   public static List<Object []> files() {
      return Arrays.<Object[]>asList(
            new Object [] { Population.NAME_GENERAL, "product0", "1.0",  null, false, null, null},	//product0 not exist in xml
            new Object [] { Population.NAME_BETA, "product0", "1.0",  null, false, null, null},	//product0 not exist in xml
            new Object [] { Population.NAME_GENERAL, "product1", "1.0",  null, true, "1.1", "mockitron/product1/1.1.bin"},	//ok
            new Object [] { Population.NAME_QA, "product1", "1.0",  null, true, "1.1", "mockitron/product1/1.1-qa.bin"},	//ok
            new Object [] { Population.NAME_GENERAL, "product1", "1.1",  null, false, null, null},	//current version already at targetVersion
            new Object [] { Population.NAME_BETA, "product1", "1.0",  null, false, null, null},	//does not support population 
            new Object [] { Population.NAME_BETA, "product2", "1.0",  null, true, "1.2", "mockitron/product2/1.2.bin"},	//ok
            new Object [] { Population.NAME_QA, "product2", "1.0",  null, false, null, null},	//does not support population
            new Object [] { "noexist", "product2", "1.0",  null, false, null, null},	//does not support population
            new Object [] { Population.NAME_GENERAL, "product1", "1.0",  6, false, "1.1", "mockitron/product1/1.1.bin"},	//max retryAttempts have reached
            new Object [] { Population.NAME_BETA, "product2", "1.0",  6, true, "1.2", "mockitron/product2/1.2.bin"},
            new Object [] { Population.NAME_GENERAL, "product2", "1.0",  100, false, "1.2", "mockitron/product2/1.2.bin"},	//max retryAttempts have reached
            new Object [] { Population.NAME_GENERAL, "product4", "1.0",  null, true, "1.1", "mockitron/product4/1.1.bin"},	//exact version match
            new Object [] { Population.NAME_BETA, "product4", "1.0",  null, false, null, null},	//does not support population
            new Object [] { Population.NAME_GENERAL, "product4", "1.1",  null, true, "1.2", "mockitron/product4/1.2.bin"},	//exact version match
            new Object [] { Population.NAME_BETA, "product4", "1.1",  null, false, null, null}	//does not support population
          
      		);
   }
   
  
   
   public TestDeviceFirmwareResolver(String population, String productId, String currentVersion, Integer retryAttempts, boolean isUpgrade, String targetVersion, String targetImage) {
   	this.population = population;
   	this.productId = productId;
   	this.currentVersion = currentVersion;
   	this.retryAttmpts = retryAttempts;
   	this.isUpgrade = isUpgrade;
   	this.targetVersion = targetVersion;
   	this.targetImage = targetImage;
   			
   }
   
   @Override
	protected Set<String> configs() {
    	Set<String> configs = super.configs();
		configs.add("src/test/resources/test.properties");
		return configs;
	}	
   
   
   @Before
   public void setUpData() {
      Resources.registerDefaultFactory(new ClassPathResourceFactory());
      Resources.registerFactory(new ClassPathResourceFactory());
      Resources.registerFactory(new FileSystemResourceFactory());
      
      
      
      replay();
   }

   @After
   public void tearDown() {
   }
   
   @Test
   public void testResolve() {
   	DeviceOTAFirmwareResponse response = null;
   	if(retryAttmpts == null) {
   		response = resolver.resolve(Optional.of(this.population), productId, currentVersion);
   	}else{
   		response = resolver.resolve(Optional.of(this.population), productId, currentVersion, retryAttmpts.intValue());
   	}
      assertEquals(isUpgrade,response.isUpgrade());
      assertEquals(targetVersion,response.getTargetVersion());
      if(targetImage == null) {
      	assertNull(response.getTargetImage());
      }else{
      	assertTrue(response.getTargetImage().endsWith(targetImage));
      }
      
   }
 
   
   
}

