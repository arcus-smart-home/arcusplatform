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
package com.iris.capability.attribute.transform;

import java.util.Map;

public interface BeanAttributesTransformer<B> {
   /** Converts the given bean into an attribute map for all 'base' or 'advanced' attributes */
   Map<String,Object> transform(B bean);
   /** Converts and attribute map into a complete or partially populated bean */
   B transform(Map<String,Object> attributes);
   /** 
    * Merge given attributes into an already defined bean.
    * 
    * @param bean The bean to merge the changes into.
    * @param newAttributes The new attribute values to merge into the bean.
    * @return a string, object map of the old attribute values.
    */
   Map<String,Object> merge(B bean, Map<String,Object> newAttributes);
}

