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
package com.iris.client.event;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class GetAttributesEvent implements ClientEvent {

    @SuppressWarnings("unchecked")
    public static GetAttributesEvent fromResultMap(Map<String,Object> results) {
    	GetAttributesEvent result = new GetAttributesEvent();
        for(Map.Entry<String,Object> entry : results.entrySet()) {
            Map<String,Object> res = (Map<String,Object>) entry.getValue();

            if(!StringUtils.isBlank((String) res.get("error"))) {
                result.errors.put(entry.getKey(), (String) res.get("error"));
            } else if(res.get("value") != null) {
                result.attributes.put(entry.getKey(), res.get("value"));
            }
        }
        return result;
    }

    private final Map<String,Object> attributes = new HashMap<>();
    private final Map<String,String> errors = new HashMap<>();

    public void putAttribute(String name, Object object) {
        attributes.put(name, object);
    }

    public void putError(String name, String error) {
        errors.put(name, error);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return "GetAttributesResult{" +
                "attributes=" + attributes +
                ", errors=" + errors +
                '}';
    }
}

