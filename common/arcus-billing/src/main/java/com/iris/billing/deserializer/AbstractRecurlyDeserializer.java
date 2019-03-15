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
package com.iris.billing.deserializer;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.base.Strings;
import com.iris.billing.client.model.BaseRecurlyModel;
import com.iris.billing.client.model.RecurlyModel;
import com.iris.billing.client.model.RecurlyModels;

//TODO: All times are GMT, need to format this based on user location?
abstract class AbstractRecurlyDeserializer<T extends BaseRecurlyModel> extends DefaultHandler implements RecurlyDeserializerInterface<T> {
	private static final Logger logger = LoggerFactory.getLogger(AbstractRecurlyDeserializer.class);

	public static final String HREF_TEXT = "href";
	public static final String TYPE_ATTRIBUTE = "type";

	private CharArrayWriter currentTextValue;
	private SAXParser saxParser;
	private AbstractRecurlyDeserializer<?> parentHandler;
	private Class<?> clazz;
	private final String tagName;

	protected abstract T getResult();
	protected abstract void onReturnFrom(AbstractRecurlyDeserializer<?> childDeserializer);
	protected abstract void onStartElement(
			String uri, String localName, String qName, Attributes attributes) throws SAXException;
	protected abstract void onEndElement(
			String uri, String localName, String qName) throws SAXException;

	AbstractRecurlyDeserializer(@Nullable SAXParser saxParser, @Nullable String tagName) {
		currentTextValue = new CharArrayWriter();
		this.tagName = tagName;
		this.saxParser = saxParser;
	}
	
	public String getTagName() {
	   return tagName;
	}

	@Override
	public <X extends BaseRecurlyModel> X parse(InputSource is, Class<X> clazz)
			throws ParserConfigurationException, SAXException, IOException {
		createParserIfNull();
		this.clazz = clazz;

		saxParser.parse(is, this);
		return castResult(clazz);
	}

	@Override
	public <X extends BaseRecurlyModel> X parse(InputStream is, Class<X> clazz)
			throws ParserConfigurationException, SAXException, IOException {
		createParserIfNull();
		this.clazz = clazz;

		saxParser.parse(is, this);
		return castResult(clazz);
	}

	@Override
	public <X extends BaseRecurlyModel> X parse(String string, Class<X> clazz)
			throws ParserConfigurationException, SAXException, IOException {
		createParserIfNull();
		this.clazz = clazz;

		saxParser.parse(new InputSource(new StringReader(string)), this);
		return castResult(clazz);
	}

	@Override
	public <X extends BaseRecurlyModel> X parse(File file, Class<X> clazz)
			throws ParserConfigurationException, SAXException, IOException {
		createParserIfNull();
		this.clazz = clazz;
		saxParser.parse(file, this);
		return castResult(clazz);
	}

	@SuppressWarnings("unchecked")
	protected <X extends BaseRecurlyModel>  X castResult(Class<X> clazz) {
	   BaseRecurlyModel deserializerResult = getResult();
	   
	   if (RecurlyModels.class.isAssignableFrom(clazz) 
	         && RecurlyModel.class.isAssignableFrom(deserializerResult.getClass())) {
	      return (X) ((RecurlyModel)deserializerResult).createContainer();
	   }
	   
		if (BaseRecurlyModel.class.isAssignableFrom(clazz)) {
			return (X) getResult();
		}

		throw new ClassCastException("Unable to cast " + clazz + " to " + BaseRecurlyModel.class);
	}

	@SuppressWarnings("unchecked")
	protected final <X extends BaseRecurlyModel> Class<X> getRequestedClass() {
		return (Class<X>) clazz;
	}

	protected final void createParserIfNull() throws ParserConfigurationException, SAXException {
		if (saxParser == null) {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			this.saxParser = factory.newSAXParser();
		}
	}

	protected final <X extends BaseRecurlyModel> void setParentHandler(AbstractRecurlyDeserializer<X> handler) {
		this.parentHandler = handler;
	}

	protected final void sendBackToParentHandler(AbstractRecurlyDeserializer<?> deserializer) throws SAXException {
		if (parentHandler != null) {
		   parentHandler.onReturnFrom(deserializer);
			saxParser.getXMLReader().setContentHandler(parentHandler);
		}
	}

	protected final SAXParser getParser() {
		return saxParser;
	}

	protected <X extends BaseRecurlyModel> void setContentHandler(AbstractRecurlyDeserializer<X> handler) throws SAXException {
		saxParser.getXMLReader().setContentHandler(handler);
	}

	@Override
	public final void characters (char[] ch, int start, int length) throws SAXException {
		currentTextValue.write(ch, start, length);
	}

	protected String getCurrentTextValue() {
		String currentText = currentTextValue.toString();

		if (!Strings.isNullOrEmpty(currentText)) {
			currentTextValue.reset();
			return currentText.trim();
		}

		return null;
	}
	
	protected void onFinish() {
	   // No-op by default
	}

	@Override
	public final void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		currentTextValue.reset();
		redirectStartElement(uri, localName, qName, attributes);
	}

	protected void redirectStartElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		onStartElement(uri, localName, qName, attributes);
	}

	@Override
	public final void endElement (String uri, String localName, String qName) throws SAXException {
	   if (tagName != null && tagName.equals(qName)) {
	      onFinish();
	      sendBackToParentHandler(this);
	      return;
	   }
		onEndElement(uri, localName, qName);
	}

	protected Date getDateFromString(String date) {
		if (Strings.isNullOrEmpty(date)) {
			return null;
		}

		try {
		   SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-d'T'HH:mm:ss");
		   sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			return sdf.parse(date);
		} catch (Exception ex) {
			logger.debug("Parse Exception while tryint to convert date. Original [{}]", date);
			return null;
		}
	}
}

