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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Document;

import com.google.common.io.Closeables;
import com.iris.io.Deserializer;
import com.iris.io.xml.XMLUtil;
import com.iris.resource.Resource;

public abstract class BaseJAXPDeserializer<T> implements Deserializer<T> {

   @Override
   public T deserialize(byte[] input) throws IllegalArgumentException {
      try{
         return deserialize(new ByteArrayInputStream(input));
      }
      catch(IOException ioe){
         throw new RuntimeException(ioe);
      }
   }

   public abstract T fromJAXP(Document documentRoot);
   public abstract String schemaResource();

   @Override
   public T deserialize(InputStream input) throws IOException, IllegalArgumentException {
      Document document = XMLUtil.parseDocumentDOM(input);
      if(schemaResource()!=null){
         XMLUtil.validate(document, schemaResource());
      }
      return fromJAXP(document);   
   }
   
   public T deserialize(Resource input) throws IllegalArgumentException {
      InputStream is = null;
      try{
         is=input.open();
         return deserialize(is);
      }
      catch(IOException ioe){
         throw new RuntimeException(ioe);
      }
      finally{
         Closeables.closeQuietly(is);
      }
   } 
   
   
}

