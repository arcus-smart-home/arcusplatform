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
package com.iris.prodcat;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Test;

import com.iris.messages.type.Population;
import com.iris.prodcat.parser.Parser;
import com.iris.prodcat.parser.SAXHandler;

public class TestProductCatalog {
	private RedirectBaseUrlHelper urlHelper = new RedirectBaseUrlHelper();

   public TestProductCatalog() {
      urlHelper.setRedirectBaseUrl("http://fakeurl");
   }

   @Test
   public void testParseProductCatalog() throws Exception {
      try(InputStream is = TestProductCatalog.class.getResourceAsStream("/product_catalog.xml")) {
         Parser parser = new Parser(urlHelper);
         parser.parse(is);
      }
   }
   
   @Test
   public void testFindReconnectStep() throws Exception {
      try(InputStream is = TestProductCatalog.class.getResourceAsStream("/test1.xml")) {
         SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
         SAXHandler saxHandler = new SAXHandler(Population.NAME_GENERAL, urlHelper);
         parser.parse(is, saxHandler);
         ProductCatalog general = saxHandler.getProductCatalogs().get("general");
         List<Step> reconnect = general.getProductById("162918").getReconnect();
         assertTrue(reconnect.size() == 3);
         
      }
   }
}

