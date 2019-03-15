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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import com.iris.messages.type.Population;
import com.iris.prodcat.ProductCatalog;
import com.iris.prodcat.RedirectBaseUrlHelper;

public class Parser {
	private final RedirectBaseUrlHelper urlHelper;
	
	public Parser() {
		this.urlHelper = null;
	}
	
	public Parser(RedirectBaseUrlHelper urlHelper) {
		this.urlHelper = urlHelper;
	}

	public Map<String, ProductCatalog> parse(InputStream in) throws ParserConfigurationException, SAXException, IOException
	{
		
		SAXParser parser = null;
		parser = SAXParserFactory.newInstance().newSAXParser();

    SAXHandler saxHandler = new SAXHandler(Population.NAME_GENERAL, urlHelper);
        
    parser.parse(in, saxHandler);
    return saxHandler.getProductCatalogs();
	}
	
}

