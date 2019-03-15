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
package com.iris.platform.rule.catalog.serializer;

import org.xml.sax.Attributes;

import com.google.common.base.Preconditions;
import com.iris.platform.rule.catalog.RuleCatalogMetadata;
import com.iris.validators.Validator;

/**
 * 
 */
public class MetadataProcessor extends BaseCatalogProcessor {
   public static final String TAG = "metadata";

   private RuleCatalogMetadata metadata;
   
   protected MetadataProcessor(Validator v) {
      super(v);
   }

   public RuleCatalogMetadata getMetadata() {
      Preconditions.checkState(metadata != null, "metadata tag not fully parsed yet");
      return metadata; 
   }
   
   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#enterTag(java.lang.String, com.iris.capability.attribute.Attributes)
    */
   @Override
   public void enterTag(String qName, Attributes attributes) {
      this.metadata = parseMetadata(attributes);
   }

   private RuleCatalogMetadata parseMetadata(Attributes attributes) {
      RuleCatalogMetadata metadata = new RuleCatalogMetadata();
      metadata.setPublisher(attributes.getValue("publisher"));
      metadata.setVersion(parseDate(attributes.getValue("version")));
      // TODO should this be generated?
      metadata.setHash(attributes.getValue("hash"));
      return metadata;
   }

}

