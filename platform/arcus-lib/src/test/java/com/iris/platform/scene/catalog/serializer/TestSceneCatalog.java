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
package com.iris.platform.scene.catalog.serializer;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.iris.io.xml.JAXBUtil;
import com.iris.resource.Resource;
import com.iris.resource.Resources;
import com.iris.resource.classpath.ClassPathResourceFactory;
import com.iris.test.IrisMockTestCase;

public class TestSceneCatalog extends IrisMockTestCase {

   
   @Before
   public void setUp() {
      Resources.registerDefaultFactory(new ClassPathResourceFactory());
      Resources.registerFactory(new ClassPathResourceFactory());
   }

   @After
   public void tearDown() {
   }

   @Test
   public void testDeSerialize() {
      Resource xml = Resources.getResource("conf/scene-catalog.xml");
      SceneCatalog catalog = JAXBUtil.fromXml(xml, SceneCatalog.class);
      assertEquals(catalog.getMetadata().publisher, "louis parks");
      
      //test premium flag
      List<ActionTemplateType> actionTemplateList = catalog.getActionTemplates().getActionTemplate();
      for(ActionTemplateType curTemplate : actionTemplateList) {
    	  if(curTemplate.getId().equals("cameras")) {
    		  assertTrue(curTemplate.isPremium());
    	  }else{
    		  assertFalse(Boolean.TRUE.equals(curTemplate.isPremium()));
    	  }
      }
      
   }
   
   @Test
   public void testSerialize() {
      ObjectFactory of = new ObjectFactory();
      SceneCatalog catalog = of.createSceneCatalog();
      catalog.setMetadata(of.createMetadataType());
      catalog.getMetadata().setPublisher("louis parks");
      catalog.getMetadata().setVersion("1.0.1");

      
      catalog.setScenes(of.createScenesType());
      SceneType scene = of.createSceneType();
      scene.setDescription("Scene Description");
      scene.setId("home");
      scene.setName("I'm Home");
      scene.setDescription("Wherever you go, receive a warm welcome when you return.");
      scene.setPopulations("general,beta");
      catalog.getScenes().getScene().add(scene);

      ActionTemplateType action = of.createActionTemplateType();
      action.setDefaultScenes("home");
      action.setId("e33df");
      action.setName("Lock & Unlock Doors");
      action.setSatifisableIf("base:caps contains 'lock'");
      action.setPremium(true);
      catalog.setActionTemplates(of.createActionTemplatesType());
      catalog.getActionTemplates().getActionTemplate().add(action);
      
      SelectorType selector = of.createSelectorType();
      selector.setType(SelectorTypeType.GROUP);
      selector.setQuery("base:caps contains 'swit'");

      SelectorType selector2 = of.createSelectorType();
      selector2.setType(SelectorTypeType.LIST);
      selector2.setName("mode");
      
      action.getSelector().add(selector);
      action.getSelector().add(selector2);
      
      OptionType option4 = of.createOptionType();
      option4.setValue("on");
      option4.setVar("on-devices");
      OptionType option5 = of.createOptionType();
      option5.setValue("off");
      option5.setVar("off-devices");

      selector.setOptions(of.createOptionsType());
      selector.getOptions().getOption().add(option4);
      selector.getOptions().getOption().add(option5);

      
      OptionType option = of.createOptionType();
      option.setLabel("On");
      option.setValue("ON");

      OptionType option2 = of.createOptionType();
      option2.setLabel("Partial");
      option2.setValue("PARTIAL");
      
      OptionType option3 = of.createOptionType();
      option3.setLabel("Disarmed");
      option3.setValue("DISRMED");

      selector2.setOptions(of.createOptionsType());
      selector2.getOptions().getOption().add(option);
      selector2.getOptions().getOption().add(option2);
      selector2.getOptions().getOption().add(option3);
      

      SetAttributesType sat = of.createSetAttributesType();
      sat.setName("swit:state");
      sat.setValue("ON");

      SetAttributesType sat2 = of.createSetAttributesType();
      sat2.setName("swit:state");
      sat2.setValue("OFF");

      //action.setActions(of.createActionsType());
      //action.getActions().getSetAttributes().add(sat);
      ///zqaction.getActions().getSetAttributes().add(sat2);
      
      String xml = JAXBUtil.toXml(catalog);
      System.out.println(xml);
      
   }

   
}

