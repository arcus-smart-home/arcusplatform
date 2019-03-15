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
package com.iris.serializer.sax;

import java.util.LinkedList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class SAXTagHandler extends DefaultHandler {
   private List<TagProcessor> handlers = new LinkedList<TagProcessor>();
   private TagProcessor delegate;
   
   public SAXTagHandler(TagProcessor delegate) {
      this.delegate = delegate;
   }

   /* (non-Javadoc)
    * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
    */
   @Override
   public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      TagProcessor parent = delegate;
      handlers.add(0, delegate);
      delegate = delegate.getHandler(qName, attributes);
      if(parent != delegate) {
         parent.enterChildTag(qName, delegate);
      }
      delegate.enterTag(qName, attributes);
   }

   /* (non-Javadoc)
    * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
    */
   @Override
   public void endElement(String uri, String localName, String qName) throws SAXException {
      TagProcessor child = delegate;
      delegate = handlers.remove(0);
      child.exitTag(qName);
      if(child != delegate) {
         delegate.exitChildTag(qName, child);
      }
   }

   /* (non-Javadoc)
    * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
    */
   @Override
   public void characters(char[] ch, int start, int length) throws SAXException {
      delegate.onText(ch, start, length);
   }

   /* (non-Javadoc)
    * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
    */
   @Override
   public void error(SAXParseException e) throws SAXException {
      delegate.getValidator().error(e.getMessage());
   }

}

