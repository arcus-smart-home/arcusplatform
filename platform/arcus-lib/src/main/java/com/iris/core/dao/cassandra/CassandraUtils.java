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
package com.iris.core.dao.cassandra;

import com.netflix.governator.configuration.ConfigurationKey;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.configuration.KeyParser;

public class CassandraUtils
{

    public static <T> T getConfig(ConfigurationProvider config, ConfigurationKey key, Class<T> type, T dflt) {
        if(config.has(key)) {
            if (String.class == type) {
                return (T)config.getStringSupplier(key, (String)dflt).get();
            } else {
                return config.getObjectSupplier(key, dflt, type).get();
            }
        } else {
            return dflt;
        }
    }

    public static ConfigurationKey toKey(String key) {
        return new ConfigurationKey(key, KeyParser.parse(key));
    }

    public static ConfigurationKey toKey(String key, String name) {
        return toKey( String.format(key, name) );
    }

    public static ConfigurationKey toKey(String simpleProp, String namedProp, String name) {
        return (name == null) ? toKey(simpleProp) : toKey(namedProp, name);
    }

    public static String getUsername(ConfigurationProvider config, String name) {
        ConfigurationKey propUser = toKey(CassandraConstants.CASSANDRA_USER_PROP, CassandraConstants.CASSANDRA_X_USER_PROP, name);
        return getConfig(config, propUser, String.class, null);
    }

    public static String getPassword(ConfigurationProvider config, String name) {
        ConfigurationKey propPass = toKey(CassandraConstants.CASSANDRA_PASSWORD_PROP, CassandraConstants.CASSANDRA_X_PASSWORD_PROP, name);
        return getConfig(config, propPass, String.class, null);
    }
}

