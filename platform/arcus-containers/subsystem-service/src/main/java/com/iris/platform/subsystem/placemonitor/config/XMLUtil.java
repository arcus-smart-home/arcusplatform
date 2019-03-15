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
package com.iris.platform.subsystem.placemonitor.config;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;

import javax.management.RuntimeErrorException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.output.WriterOutputStream;

import com.google.common.io.Closeables;
import com.iris.resource.Resource;

public class XMLUtil {

   public static <T> T deserializeJAXB(InputStream is, Class<T> clazz) throws Exception {
      JAXBContext jc = JAXBContext.newInstance(clazz);

      StreamSource xml = new StreamSource(is);
      Unmarshaller unmarshaller = jc.createUnmarshaller();
      JAXBElement<T> unmarhsalledObject = unmarshaller.unmarshal(xml, clazz);
      T parsed = unmarhsalledObject.getValue();
      return parsed;
   }

   public static <T> T deserializeJAXB(Resource resource, Class<T> clazz) {
      InputStream is = null;
      try{
         is = resource.open();
         return deserializeJAXB(is, clazz);
      }catch (Exception e){
         throw new RuntimeException("Error parsing XML document", e);
      }finally{
         Closeables.closeQuietly(is);
      }
   }

   public static <T> void serializeJAXB(Object object, OutputStream os) {
      try{
         JAXBContext jc = JAXBContext.newInstance(object.getClass());
         Marshaller marsh = jc.createMarshaller();
         marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
         marsh.marshal(object, os);
      }catch (Exception e){
         throw new RuntimeException("Error Serializing JAXBObject");
      }
   }

   public static <T> String serializeJAXB(Object object) {
      StringWriter sw = new StringWriter();
      WriterOutputStream wos;
      try{
         wos = new WriterOutputStream(sw);
         serializeJAXB(object, wos);
         wos.flush();
         return sw.toString();
      }
      catch(Exception e){
         throw new RuntimeException(e);
      }
   }

}

