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

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.iris.model.Version;
import com.iris.prodcat.Brand;
import com.iris.prodcat.Category;
import com.iris.prodcat.ExternalApplication;
import com.iris.prodcat.ExternalApplication.PlatformType;
import com.iris.prodcat.Input;
import com.iris.prodcat.Input.InputType;
import com.iris.prodcat.Metadata;
import com.iris.prodcat.ProductCatalog;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogEntry.BatterySize;
import com.iris.prodcat.ProductCatalogEntry.Cert;
import com.iris.prodcat.RedirectBaseUrlHelper;
import com.iris.prodcat.Step;
import com.iris.prodcat.Step.StepType;

public class SAXHandler extends DefaultHandler {

	private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	private final SimpleDateFormat productDateFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	private final Stack<String> elementStack = new Stack<String>();
	private ProductCatalogEntry currentProduct;
	private Step currentStep;
	private Metadata metadata;
	private final Map<String, List<ProductCatalogEntry>> populations = new HashMap<>();
	private final Map<String, ProductCatalog> catalogs = new HashMap<>();
	private final RedirectBaseUrlHelper urlHelper;	
	private Map<String,Brand> brands = new HashMap<>();
	private final String defaultPopulationName;
	
	public SAXHandler(String defaultPopulationName, @Nullable RedirectBaseUrlHelper urlHelper) {
	   this.defaultPopulationName = defaultPopulationName;
	   this.urlHelper = urlHelper;
	}

	private Map<String,Category> categories = new HashMap<>();

   @Override
	public void startDocument() throws SAXException {
		dateFmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		productDateFmt.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		this.elementStack.push(qName);

		if ("product".equals(qName)) {

		   String id = attributes.getValue("id");

		   String vendor = attributes.getValue("vendor");
		   if(vendor == null || (!vendor.equals("UNKNOWN") && !brands.containsKey(vendor))) {
		      throw new SAXException("No brand exists with vendor name [" + vendor + "] referenced by product [" + id + "]");
		   }

			currentProduct = new ProductCatalogEntry();

			currentProduct.setId(id);
			currentProduct.setName(attributes.getValue("name"));
			currentProduct.setShortName(attributes.getValue("shortname"));
			currentProduct.setDescription(attributes.getValue("description"));
			currentProduct.setManufacturer(attributes.getValue("manufacturer"));
			currentProduct.setVendor(vendor);
			currentProduct.setAddDevImg(attributes.getValue("adddevimg"));
			currentProduct.setCert(Cert.fromValue(attributes.getValue("cert")));
			currentProduct.setCanBrowse(Boolean.parseBoolean(attributes.getValue("canbrowse")));
			currentProduct.setCanSearch(Boolean.parseBoolean(attributes.getValue("canbrowse")));
			currentProduct.setHelpUrl(attributes.getValue("helpurl"));
			currentProduct.setPairVideoUrl(attributes.getValue("pairvideourl"));
			currentProduct.setBlacklisted(Boolean.parseBoolean(attributes.getValue("blacklisted")));
			try {
			   currentProduct.setBatteryPrimSize(BatterySize.valueOf(translateBatteryName(attributes.getValue("batteryprimsize"))));
			} catch (NullPointerException e) {}
			try {
			   currentProduct.setBatteryPrimNum(Integer.parseInt(attributes.getValue("batteryprimnum")));
			} catch (NumberFormatException e) {}
			try {
			   currentProduct.setBatteryBackSize(BatterySize.valueOf(translateBatteryName(attributes.getValue("batterybacksize"))));
			} catch (NullPointerException e) {}
			try {
			   currentProduct.setBatteryBackNum(Integer.parseInt(attributes.getValue("batterybacknum")));
			} catch (NumberFormatException e) {}
			currentProduct.setKeywords(attributes.getValue("keywords"));
			currentProduct.setOTA(Boolean.parseBoolean(attributes.getValue("ota")));
			currentProduct.setProtoFamily(attributes.getValue("protofamily"));
			currentProduct.setProtoSpec(attributes.getValue("protospec"));
			currentProduct.setDriver(attributes.getValue("driver"));
			currentProduct.setAdded(productDateFmt.parse(attributes.getValue("added"), new ParsePosition(0)));
			currentProduct.setLastChanged(productDateFmt.parse(attributes.getValue("lastchanged"), new ParsePosition(0)));
			currentProduct.setScreen(attributes.getValue("screen"));
			
			if (attributes.getValue("hubrequired") == null){ /* default to true when missing */
				currentProduct.setHubRequired(Boolean.TRUE);
			} else {
				currentProduct.setHubRequired(Boolean.parseBoolean(attributes.getValue("hubrequired")));
			}
			
			currentProduct.setMinAppVersion(attributes.getValue("minappversion"));
			currentProduct.setDevRequired( attributes.getValue("devrequired"));
			String fwVerStr = attributes.getValue("minhubfirmware");
			if(StringUtils.isNotBlank(fwVerStr)) {
			   currentProduct.setMinHubFirmware(Version.fromRepresentation(fwVerStr));			   
			}	
			String voiceIntegratedStr = attributes.getValue("canDiscover");
			if(StringUtils.isNotBlank(voiceIntegratedStr)) {
			   currentProduct.setCanDiscover(Boolean.parseBoolean(voiceIntegratedStr));
			}else{
			   currentProduct.setCanDiscover(true); //default is true
			}
			String appRequiredStr = attributes.getValue("appRequired");
			if(StringUtils.isNotBlank(appRequiredStr)) {
			   currentProduct.setAppRequired(Boolean.parseBoolean(appRequiredStr));
			}else{
			   currentProduct.setAppRequired(false); //default is false
			}
			currentProduct.setInstallManualUrl(attributes.getValue("installManualUrl"));
			currentProduct.setPairingMode(ProductCatalogEntry.PairingMode.fromValue(attributes.getValue("pairingMode")));
			String idleTimeoutStr = attributes.getValue("pairingIdleTimeoutMs");
			if(StringUtils.isNotBlank(idleTimeoutStr)) {
				currentProduct.setPairingIdleTimeoutMs(Integer.valueOf(idleTimeoutStr));
			}
			String pairingTimeoutStr = attributes.getValue("pairingTimeoutMs");
			if(StringUtils.isNotBlank(pairingTimeoutStr)) {
				currentProduct.setPairingTimeoutMs(Integer.valueOf(pairingTimeoutStr));
			}
		}
 
		else if ("metadata".equals(qName)) {
			metadata = new Metadata();

			metadata.setPublisher(attributes.getValue("publisher"));
			metadata.setVersion(dateFmt.parse(attributes.getValue("version"), new ParsePosition(0)));


		}

		else if ("step0".equals(qName) || "step1".equals(qName) || "step2".equals(qName) || "step3".equals(qName) ||
				"step4".equals(qName) || "step5".equals(qName) || "step6".equals(qName)) {
			currentStep = new Step();
			currentStep.setOrder(new Integer(qName.substring(4)));
			currentStep.setType(StepType.valueOf(attributes.getValue("type").toUpperCase()));
			currentStep.setImg(attributes.getValue("img"));
			currentStep.setText(attributes.getValue("text"));
			currentStep.setSubText(attributes.getValue("subtext"));
			currentStep.setMessage(attributes.getValue("message"));
			currentStep.setTarget(attributes.getValue("target"));
			currentStep.setLinkText(attributes.getValue("linkText"));
			currentStep.setLinkUrl(attributes.getValue("linkUrl"));
			String showInstallManualStr = attributes.getValue("showInstallManual");
			if(StringUtils.isNotBlank(showInstallManualStr)) {
				currentStep.setShowInstallManual(Boolean.parseBoolean(showInstallManualStr));
			}
			
			if (elementStack.contains("pair")) {
				currentProduct.getPair().add(currentStep);
			} else if (elementStack.contains("removal")) {
				currentProduct.getRemoval().add(currentStep);
			} else if (elementStack.contains("reset")) {
				currentProduct.getReset().add(currentStep);
			} else if(elementStack.contains("reconnect")) {
				currentProduct.getReconnect().add(currentStep);
			}
			
		}
		
		else if ("input".equals(qName)) {
			Input input = new Input();
			
			input.setName(attributes.getValue("name"));
			input.setType(InputType.valueOf(attributes.getValue("type").toUpperCase()));
			input.setLabel(attributes.getValue("label"));
			input.setValue(attributes.getValue("value"));
			if (attributes.getValue("maxlen") != null) {
				input.setMaxlen(Integer.valueOf(attributes.getValue("maxlen")));
			}
			if (attributes.getValue("minlen") != null) {
				input.setMinlen(Integer.valueOf(attributes.getValue("minlen")));
			}
			if (attributes.getValue("required")!= null) {
				input.setRequired(Boolean.valueOf(attributes.getValue("required")));
			} else {
				input.setRequired(false);
			}
			
			currentStep.addInput(input);
		}
		
		else if("app".equals(qName)) {
			ExternalApplication app = new ExternalApplication();
			
			app.setPlatform(PlatformType.valueOf(attributes.getValue("platform").toUpperCase()));
			app.setAppUrl(parseUrl(attributes.getValue("appUrl")));
			
			currentStep.addExternalApplication(app);
		}

		else if ("category".equals(qName)) {
		   String name = attributes.getValue("name");
		   if(currentProduct != null) {
		      if(!categories.containsKey(name)) {
		         throw new SAXException("No category exists with name [" + name + "] referenced by product [" + currentProduct.getId() + "]");
		      }
		      currentProduct.getCategories().add(name);
		   } else {
		      categories.put(name, new Category(name, attributes.getValue("image")));
		   }
		}

		else if ("population".equals(qName)) {
			currentProduct.getPopulations().add(attributes.getValue("name"));
		}

		else if("brand".equals(qName)) {
		   Brand brand = new Brand(attributes.getValue("name"),
		                           attributes.getValue("image"),
		                           attributes.getValue("description"));
		   brands.put(brand.getName(), brand);
		}
	}

	

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		String elementName = this.elementStack.pop();
		if ("product".equals(elementName)) {
		   addToPopulations(currentProduct);
		   currentProduct = null;
		}
	}

	@Override
	public void endDocument() throws SAXException {
		// Build product catalogs for populations
		for (Entry<String, List<ProductCatalogEntry>> entry : populations.entrySet()) {
		   ProductCatalog catalog = new ProductCatalog();
		   catalog.setMetadata(metadata);		   
		   catalog.setData(brands.values(), categories.values(), entry.getValue());
		   catalogs.put(entry.getKey(), catalog);
		}
		populations.clear();
	}

	public Map<String, ProductCatalog> getProductCatalogs() {
		return catalogs;
	}

	private void addToPopulations(ProductCatalogEntry entry) {
	   List<String> poplist = entry.getPopulations();
	   if (poplist != null) {
	   	for (String population : poplist) {
		      if (!populations.containsKey(population)) {
		         List<ProductCatalogEntry> entries = new ArrayList<>();	                   
		         populations.put(population, entries);
		      }
		      populations.get(population).add(entry);
		   }	      
	   }
	}

	private String translateBatteryName(String name) {
		if (name.equalsIgnoreCase("9V")) {
			return "_9V";
		}
		else if("".equals(name)) {
		   return null;
		}
		return name;
	}
	
	//TODO - we could use this method for all the URL attributes in the product catalog
	private String parseUrl(String urlLink) {
		if(this.urlHelper != null) {
			return this.urlHelper.replaceRedirectBaseUrl(urlLink);
		}else{
			return urlLink;
		}
	}
}


