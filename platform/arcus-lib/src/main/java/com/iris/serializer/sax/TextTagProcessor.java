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
package com.iris.serializer.sax;

import com.google.common.base.Preconditions;
import com.iris.platform.rule.catalog.serializer.BaseCatalogProcessor;
import com.iris.validators.Validator;

/**
 * 
 */
public class TextTagProcessor extends BaseCatalogProcessor {
   private StringBuilder buffer = new StringBuilder();
   private String contents;

   public TextTagProcessor(Validator v) {
      super(v);
   }
   
   public String getText() {
      Preconditions.checkState(contents != null, "Can't call getContents until the tag has been completed");
      return contents; 
   }
   

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#onText(char[], int, int)
    */
   @Override
   public void onText(char[] text, int start, int length) {
      this.buffer.append(text, start, length);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#exitTag(java.lang.String)
    */
   @Override
   public void exitTag(String qName) {
      contents = buffer.toString().trim().replaceAll("\\s+", " ");
   }

}

