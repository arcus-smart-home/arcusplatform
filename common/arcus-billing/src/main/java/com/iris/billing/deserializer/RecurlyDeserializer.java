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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.iris.billing.client.model.BaseRecurlyModel;

public class RecurlyDeserializer implements RecurlyDeserializerInterface<BaseRecurlyModel> {

   @Override
   public <X extends BaseRecurlyModel> X parse(InputSource is, Class<X> clazz)
         throws ParserConfigurationException, SAXException, IOException {
      return new RecurlyDeserializerImpl().parse(is, clazz);
   }

   @Override
   public <X extends BaseRecurlyModel> X parse(InputStream is, Class<X> clazz)
         throws ParserConfigurationException, SAXException, IOException {
      return new RecurlyDeserializerImpl().parse(is, clazz);
   }

   @Override
   public <X extends BaseRecurlyModel> X parse(String string, Class<X> clazz)
         throws ParserConfigurationException, SAXException, IOException {
      return new RecurlyDeserializerImpl().parse(string, clazz);
   }

   @Override
   public <X extends BaseRecurlyModel> X parse(File file, Class<X> clazz)
         throws ParserConfigurationException, SAXException, IOException {
      return new RecurlyDeserializerImpl().parse(file, clazz);
   }

}

