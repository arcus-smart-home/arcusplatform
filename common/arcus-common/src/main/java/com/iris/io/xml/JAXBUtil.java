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
package com.iris.io.xml;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.Closeables;
import com.iris.resource.Resource;

public class JAXBUtil {

   private static final LoadingCache<String, JAXBContext> jaxbContextsCache = CacheBuilder.newBuilder()
         .build(
                 new CacheLoader<String, JAXBContext>() {
                     @Override
                     public JAXBContext load(String packageName) throws Exception {
                         return JAXBContext.newInstance(packageName);
                     }
                 }
         );
   
   public static <T> T fromXml(InputStream is, Class<T> clazz) throws Exception {
      
      JAXBContext jc = getJAXBContext(clazz);

      StreamSource xml = new StreamSource(is);
      Unmarshaller unmarshaller = jc.createUnmarshaller();
      JAXBElement<T> unmarhsalledObject = unmarshaller.unmarshal(xml, clazz);
      T parsed = unmarhsalledObject.getValue();
      return parsed;
   }

   public static <T> T fromXml(Resource resource, Class<T> clazz) {
      InputStream is = null;
      try{
         is = resource.open();
         return fromXml(is, clazz);
      }catch (Exception e){
         throw new RuntimeException("Error parsing XML document", e);
      }finally{
         Closeables.closeQuietly(is);
      }
   }

   public static <T> void toXml(Object object, OutputStream os) {
      try{
         JAXBContext jc = getJAXBContext(object.getClass());
         Marshaller marsh = jc.createMarshaller();
         marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
         marsh.marshal(object, os);
      }catch (Exception e){
         throw new RuntimeException("Error Serializing JAXBObject");
      }
   }

   public static <T> String toXml(Object object) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try{
         toXml(object, baos);
         baos.flush();
         return new String(baos.toByteArray());
      }
      catch(Exception e){
         throw new RuntimeException(e);
      }
   }
   private static JAXBContext getJAXBContext(Class clazz) {
      String packageName = clazz.getPackage().getName();
      try {
          return jaxbContextsCache.get(packageName);
      } catch (ExecutionException e) {
          throw new RuntimeException("Error when getting JAXBContext from cache", e);
      }
  }

}

