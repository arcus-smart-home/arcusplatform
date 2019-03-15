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
/**
 * 
 */
package com.iris.oculus.util;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.commons.io.IOUtils;

import com.iris.resource.Resource;
import com.iris.resource.Resources;

/**
 * 
 */
public class Icons {
   // TODO add a default icon

   public static Icon load(String resource) throws IOException {
      return load(Resources.getResource(resource));
   }
   
   public static Icon load(Resource resource) throws IOException {
      try {
         return new ImageIcon(resource.getUri().toURL());
      }
      catch(MalformedURLException e) {
         return new ImageIcon(IOUtils.toByteArray(resource.open()));
      }
   }
}

