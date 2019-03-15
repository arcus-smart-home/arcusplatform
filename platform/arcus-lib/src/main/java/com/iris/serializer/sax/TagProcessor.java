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

import org.xml.sax.Attributes;

import com.iris.validators.Validator;

// TODO should add a context object and move validator there
public interface TagProcessor {
   
   Validator getValidator();

   TagProcessor getHandler(String qName, Attributes attributes);
   
   void enterTag(String qName, Attributes attributes);
   
   void onText(char[] text, int start, int length);
   
   void exitTag(String qName);
   
   void enterChildTag(String qName, TagProcessor handler);
   
   void exitChildTag(String qName, TagProcessor handler);
}

