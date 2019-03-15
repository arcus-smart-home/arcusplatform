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
package com.iris.prodcat.parser;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.iris.messages.type.Population;
import com.iris.model.Version;
import com.iris.prodcat.ExternalApplication;
import com.iris.prodcat.ExternalApplication.PlatformType;
import com.iris.prodcat.Input;
import com.iris.prodcat.Input.InputType;
import com.iris.prodcat.Metadata;
import com.iris.prodcat.ProductCatalog;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;
import com.iris.prodcat.Step;
import com.iris.prodcat.Step.StepType;
import com.iris.resource.classpath.ClassPathResourceFactory;
import com.iris.test.util.TestUtils;

@RunWith(value = Parameterized.class)
public class TestProductCatalogParser extends AbstractProductCatalogTestCase {

	@Parameters(name="url[{0}]")
   public static List<Object []> files() {
      return Arrays.<Object[]>asList(
            new Object [] { "classpath:/test1.xml"}
      );
   }
   
   public TestProductCatalogParser(String productCatalogUri) {
   	this.productCatalogUriStr = productCatalogUri;
   }
	
	@Test
	public void testParsePopulationGeneral() throws Exception {
		
	   ProductCatalog prodcat = productCatalogManager.getCatalog(Population.NAME_GENERAL);
		assertNotNull(prodcat.getMetadata());
		Metadata m = prodcat.getMetadata();
		assertEquals("Human", m.getPublisher());
		assertNotNull(prodcat.getMetadata());
		assertTrue(TestUtils.verifyDate(2015, 4, 23, 18, 23, 9, m.getVersion()));
		
		printProductSorting(prodcat);
		
		assertEquals(expectedProductIdsForGeneral.size(), prodcat.getProducts().size());
		
		ProductCatalogEntry p = prodcat.getProducts().get(9);
		assertEquals("359d72", p.getId());
		assertEquals("GE Plug-In Outdoor Smart Switch", p.getName());
		assertEquals("GE 15-Amp Black Single Electrical Outlet (Works with Iris)", p.getDescription());
		assertEquals("Jasco", p.getManufacturer());
		assertEquals("GE", p.getVendor());
		assertEquals("product.png", p.getAddDevImg());
		assertEquals(ProductCatalogEntry.Cert.WORKS, p.getCert());
		assertEquals(true, p.getCanBrowse());
		assertEquals(true, p.getCanSearch());
		assertEquals("https://microsite/devdb/359d72", p.getHelpUrl());
		assertEquals("https://youtu.be/YE1HvRwH4vg", p.getPairVideoUrl());
		assertEquals("GE, Jasco, outdoor, module, control, outlet, plug, switch, energy, 110, volt, power, waterproof, z-wave, 45635, 12720", p.getKeywords());
		assertEquals(false, p.getOTA());
		assertEquals("z-wave", p.getProtoFamily());
		assertEquals("DriverFoo v1.0-beta", p.getDriver());
		assertEquals(true, TestUtils.verifyDate(2015, 4, 29, 13, 18, 35, p.getAdded()));
		assertEquals(true, TestUtils.verifyDate(2015, 4, 29, 13, 18, 35, p.getLastChanged()));
		assertEquals(Version.fromRepresentation("2.0.0.031"), p.getMinHubFirmware());
		assertEquals(Boolean.TRUE, p.getCanDiscover());
		assertEquals(new Integer(240000), p.getPairingIdleTimeoutMs());
		
		assertEquals(1, p.getCategories().size());
		assertEquals("Lights & Switches", p.getCategories().get(0));
		
		assertEquals(3, p.getPair().size());
		assertEquals(3, p.getRemoval().size());
		verifyStep(Step.StepType.TEXT, 
		      "step1.png", 
		      "Plug the Outdoor module into the outlet where you intend to use it.",
		      1,
		      p.getPair().get(0));
		Step step1 = p.getPair().get(1);
		verifyStep(Step.StepType.EXTERNAL_APP,
		      "step2.png",
		      "The green Connection Status light will flash twice, then remain solid when ready to connect.",
		      2,
		      step1);
		assertEquals(2, step1.getExternalApplications().size());
		verifyExternalApp(ExternalApplication.PlatformType.ANDROID, prodCatConfig.getRedirectBaseUrlHelper().getRedirectBaseUrl()+"/a_android_google_home", step1.getExternalApplications().get(0));
		verifyExternalApp(ExternalApplication.PlatformType.IOS, prodCatConfig.getRedirectBaseUrlHelper().getRedirectBaseUrl()+"/a_ios_google_home", step1.getExternalApplications().get(1));
		     
		Step pairStep3 = p.getPair().get(2);
		verifyStep(Step.StepType.INPUT,
		      "step3.png",
		      "Enter the 14 digit code found on the rating decal, located on the rim below the salt lid hinges.",
		      3,
		      pairStep3);
		List<Input> step3Inputs = pairStep3.getInputs();
		assertEquals(2, step3Inputs.size());
		Input step3Input2 = step3Inputs.get(1);
		assertEquals(new Integer(14), step3Input2.getMinlen());
		assertEquals(new Integer(14), step3Input2.getMaxlen());
		verifyStep(Step.StepType.TEXT,
			      "",
			      "TIP: Do not use other Iris devices at this time to avoid removing them.",
			      0,
			      p.getRemoval().get(0));
		verifyStep(Step.StepType.TEXT,
		      "",
		      "Press the button located on the device.",
		      1,
		      p.getRemoval().get(1));
		verifyStep(Step.StepType.TEXT,
		      "",
		      "The hub will beep when the device is successfully removed.",
		      2,
		      p.getRemoval().get(2));
		assertEquals(3, p.getPopulations().size());
		assertEquals(Population.NAME_GENERAL, p.getPopulations().get(0));			
		assertAllProductsFound(expectedProductIdsForGeneral, prodcat.getProducts());
		
		List<String> expectedProductIdsWithout359d72 = new ArrayList<>(expectedProductIdsForGeneral);
		expectedProductIdsWithout359d72.remove("359d72");
		assertAllProductsFound(expectedProductIdsWithout359d72, prodcat.getAllProducts(Version.fromRepresentation("2.0.0.030")));
      assertAllProductsFound(expectedProductIdsForGeneral, prodcat.getAllProducts(Version.fromRepresentation("2.0.0.032")));
      assertAllProductsFound(expectedProductIdsForGeneral, prodcat.getAllProducts(Version.fromRepresentation("2.0.0.031")));
      assertAllProductsFound(expectedProductIdsForGeneral, prodcat.getAllProducts(null));
		
		
	}
	
	

   

	@Test
	public void testParseWithInputSteps() throws Exception {
		ProductCatalog prodcat = productCatalogManager.getCatalog(Population.NAME_GENERAL);
		assertEquals(expectedProductIdsForGeneral.size(), prodcat.getProducts().size());
		ProductCatalogEntry e = prodcat.getProductById("3981d9");
		
		
		Step s = e.getPair().get(0);
		assertEquals(s.getType(), StepType.TEXT);
		assertNull(s.getMessage());
		assertNull(s.getTarget());
		assertEquals(s.getText(), "Install the water softener as described in the manufacturer's instructions.");
		assertTrue(s.isShowInstallManual());
		assertEquals("step1LinkText", s.getLinkText());
		assertEquals("step1LinkUrl", s.getLinkUrl());
		
		
		s = e.getPair().get(1);
		assertEquals(s.getType(), StepType.TEXT);
		assertNull(s.getMessage());
		assertNull(s.getTarget());
		assertFalse(s.isShowInstallManual());
		assertNull(s.getLinkText());
		assertNull(s.getLinkUrl());
		
		s = e.getPair().get(2);
		assertEquals(s.getType(), StepType.INPUT);
		assertEquals(s.getMessage(), "RegisterDevice");
		assertEquals(s.getTarget(), "SERV:ipcd");
		
		Input i = s.getInputs().get(0);
		assertEquals(i.getType(), InputType.HIDDEN);
		assertEquals(i.getName(), "vendor");
		assertEquals(i.getValue(), "Whirlpool");
		assertFalse(i.getRequired());
		
		i = s.getInputs().get(1);
		assertEquals(i.getType(), InputType.HIDDEN);
		assertEquals(i.getName(), "model");
		assertEquals(i.getValue(), "WHESCS");
		assertFalse(i.getRequired());
		
		i = s.getInputs().get(2);
		assertEquals(i.getType(), InputType.TEXT);
		assertEquals(i.getName(), "sn");
		assertNull(i.getValue());
		assertEquals(i.getMaxlen(), new Integer(14));
		assertTrue(i.getRequired());
		
		
		
	}
	
	@Test
	public void testParseBeta() throws Exception {
	   ProductCatalog prodcat = productCatalogManager.getCatalog("beta");
      assertNotNull(prodcat.getMetadata());
      Metadata m = prodcat.getMetadata();
      assertEquals("Human", m.getPublisher());
      assertNotNull(prodcat.getMetadata());
      assertTrue(TestUtils.verifyDate(2015, 4, 23, 18, 23, 9, m.getVersion()));
      
      printProductSorting(prodcat);
      
      assertEquals(expectedProductIdsForBeta.size(), prodcat.getProducts().size());
      
      ProductCatalogEntry p = prodcat.getProducts().get(10);
      assertEquals("23af19", p.getId());
      assertEquals(ProductCatalogEntry.BatterySize.AA, p.getBatteryPrimSize());
      assertEquals(Integer.valueOf(4), p.getBatteryPrimNum());
      
      assertAllProductsFound(expectedProductIdsForBeta, prodcat.getProducts());
	}
	
	@Test
   public void testParseAlpha() {
      ProductCatalog prodcat = productCatalogManager.getCatalog("alpha");
      assertNotNull(prodcat.getMetadata());
      Metadata m = prodcat.getMetadata();
      assertEquals("Human", m.getPublisher());
      assertNotNull(prodcat.getMetadata());
      assertTrue(TestUtils.verifyDate(2015, 4, 23, 18, 23, 9, m.getVersion()));
      
      /**
       * New sorting:
       * 0***Iris***WiFi Smart Switch***162918
			1***Iris***1st Gen Contact Sensor***4ff66a
			2***Amazon***Alexa***7dfa41
			3***First Alert***First Alert Smoke Detector***bc45b5
			4***First Alert***First Alert Smoke and CO Detector***798086
			5***GE***GE In-Wall Smart Dimmer***6c56c8
			6***GE***GE In-Wall Smart Fan Control Switch***979695
			7***GE***GE In-Wall Smart Outlet***700faf
			8***GE***GE In-Wall Smart Switch***671eee
			9***GE***GE Plug-In Outdoor Smart Switch***359d72
			10***GE***GE Plug-In Smart Switch***0c9a66
			11***Whirlpool***Water Softener***3981d9
       */
      assertEquals(expectedProductIdsForAlpha.size(), prodcat.getProducts().size());
      ProductCatalogEntry product0 = prodcat.getProducts().get(0);
      ProductCatalogEntry product2 = prodcat.getProducts().get(2);
      ProductCatalogEntry product9 = prodcat.getProducts().get(9);
      ProductCatalogEntry product4 = prodcat.getProducts().get(4);
      
		ProductCatalogEntry product10 = prodcat.getProducts().get(10);
      assertEquals("0c9a66", product10.getId());
      assertEquals(Version.fromRepresentation("2.0.0.048"), product10.getMinHubFirmware());
      assertEquals(Boolean.FALSE, product10.getCanDiscover());
      assertEquals(Boolean.TRUE, product10.getAppRequired());
		assertEquals(ProductCatalogEntry.PairingMode.HUB, product10.getPairingMode());
      assertNull(product10.getInstallManualUrl());
      
      assertEquals("359d72", product9.getId());
      assertEquals(Version.fromRepresentation("2.0.0.031"), product9.getMinHubFirmware());
      assertEquals(Boolean.TRUE, product9.getCanDiscover());
      assertEquals(Boolean.FALSE, product9.getAppRequired());
		assertEquals(ProductCatalogEntry.PairingMode.HUB, product9.getPairingMode());
		
      assertEquals("798086", product4.getId());
      assertNull(product4.getMinHubFirmware());
      assertEquals(Boolean.TRUE, product4.getCanDiscover());
      assertEquals(Boolean.FALSE, product4.getAppRequired());
		assertEquals(ProductCatalogEntry.PairingMode.HUB, product4.getPairingMode());
		assertEquals("798086", product4.getId());

		assertEquals("7dfa41", product2.getId());
		assertEquals(ProductCatalogEntry.PairingMode.EXTERNAL_APP, product2.getPairingMode());

		assertEquals("162918", product0.getId());
		assertEquals(ProductCatalogEntry.PairingMode.WIFI, product0.getPairingMode());

      assertAllProductsFound(expectedProductIdsForAlpha, prodcat.getProducts());
      assertAllProductsFound(expectedProductIdsForAlpha, prodcat.getProducts(Version.fromRepresentation("2.0.0.048")));
      assertAllProductsFound(expectedProductIdsForAlpha, prodcat.getProducts(Version.fromRepresentation("2.0.0.049")));
      assertAllProductsFound(expectedProductIdsForAlpha, prodcat.getProducts(Version.fromRepresentation("2.0.1.001")));
      assertAllProductsFound(expectedProductIdsForAlpha, prodcat.getProducts(null));
      
      List<String> expectedProductIdsWithout0c9a66 = new ArrayList<>(expectedProductIdsForAlpha);
      expectedProductIdsWithout0c9a66.remove("0c9a66");
      assertAllProductsFound(expectedProductIdsWithout0c9a66, prodcat.getProducts(Version.fromRepresentation("2.0.0.047")));
      assertAllProductsFound(expectedProductIdsWithout0c9a66, prodcat.getProducts(Version.fromRepresentation("2.0.0.032")));
      assertAllProductsFound(expectedProductIdsWithout0c9a66, prodcat.getProducts(Version.fromRepresentation("2.0.0.031")));
      
      expectedProductIdsWithout0c9a66.remove("359d72");
      assertAllProductsFound(expectedProductIdsWithout0c9a66, prodcat.getProducts(Version.fromRepresentation("2.0.0.030")));
   }
	
	@Test
	public void testParseMVP() throws Exception {
		
		ClassPathResourceFactory factory = new ClassPathResourceFactory();
		ProductCatalogManager manager2 = new ProductCatalogManager(factory.create(new URI("classpath:/test2.xml")));
		
		ProductCatalog prodcat = manager2.getCatalog(Population.NAME_GENERAL);
		assertNotNull(prodcat.getMetadata());
	}	
	
	@Test
	public void testGetProductById() throws Exception {
		ClassPathResourceFactory factory = new ClassPathResourceFactory();
		ProductCatalogManager manager2 = new ProductCatalogManager(factory.create(new URI("classpath:/test2.xml")));
		
		ProductCatalog prodcat = manager2.getCatalog(Population.NAME_GENERAL);
		assertNotNull(prodcat.getProductById("840941"));  //canbrowse=false
		assertNotNull(prodcat.getProductById("f80f79"));  //canbrowse=true
		
	}
	
	@Test
	public void testPairingTimeoutAttribute() throws Exception {
		ClassPathResourceFactory factory = new ClassPathResourceFactory();
		ProductCatalogManager manager2 = new ProductCatalogManager(factory.create(new URI("classpath:/test2.xml")));
		
		ProductCatalog prodcat = manager2.getCatalog(Population.NAME_GENERAL);
		ProductCatalogEntry product1 = prodcat.getProductById("d0f50f");  
		assertEquals(Integer.valueOf(1000), product1.getPairingTimeoutMs());
		ProductCatalogEntry product2 = prodcat.getProductById("f80f79");  
		assertNull(product2.getPairingTimeoutMs());  
		
	}
	
	@Test
	public void testGetProductsByBrandSorting() throws Exception {
		ProductCatalog prodcat = productCatalogManager.getCatalog(Population.NAME_GENERAL);
		List<ProductCatalogEntry> products = prodcat.getProductsByBrand("Iris");
		List<String> expectedList = filterByPopulation(expectedProductIdsForIris, expectedProductIdsForGeneral);
		assertEquals(expectedList.size(), products.size());
		assertEquals(expectedList.get(0), products.get(0).getId());
		assertEquals(expectedList.get(1), products.get(1).getId());
		
		products = prodcat.getProductsByBrand("GE");
		expectedList = filterByPopulation(expectedProductIdsForGE, expectedProductIdsForGeneral);
		assertEquals(expectedList.size(), products.size());
		int i = 0;
		for(ProductCatalogEntry curProduct : products) {
			assertEquals(expectedList.get(i++), curProduct.getId());
		}
		
	}
	
	private List<String> filterByPopulation(List<String> sourceList, List<String> intersectionList) {
		return sourceList.stream().filter((p) -> intersectionList.contains(p)).collect(Collectors.toList());
	}
	
	public void printProductSorting(ProductCatalog prodcat) throws Exception {
		List<ProductCatalogEntry> products = prodcat.getAllProducts();
		products.forEach(p -> {
			System.out.println("***"+ p.getVendor()+"***"+p.getName()+"***"+p.getId());
		});
	}
	
	
	
	/*@Test
	public void testVersions() {
	   Version v1 = Version.fromRepresentation("2.0.0.031");
	   Version v2 = Version.fromRepresentation("1.0.0.031");
	   Version v3 = Version.fromRepresentation("2.0.0.039");
	   Version v4 = Version.fromRepresentation("2.0.1.001");
	   SortedMap<Version, String> map = new TreeMap<Version, String>();
	   map.put(v1, v1.toString());
	   map.put(v2, v2.toString());
	   map.put(v3, v3.toString());
	   map.put(v4, v4.toString());
	   
	   for(Entry<Version, String> cur : map.entrySet()) {
	      System.out.println(cur.getKey().toString());
	   }
	   
	   System.out.println(v4.compareTo(v2));
	}*/
	
	private void verifyStep(Step.StepType type, String img, String text, int order, Step step) {
	   assertEquals(type, step.getType());
	   assertEquals(img, step.getImg());
	   assertEquals(text, step.getText());
	   assertEquals(order, step.getOrder());
	}
	
	private void verifyExternalApp(PlatformType platform, String appUrl, ExternalApplication externalApplication) {
		assertEquals(platform, externalApplication.getPlatform());
		assertEquals(appUrl, externalApplication.getAppUrl());
	}
}

