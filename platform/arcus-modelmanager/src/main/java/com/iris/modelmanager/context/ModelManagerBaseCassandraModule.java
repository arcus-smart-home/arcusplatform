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
package com.iris.modelmanager.context;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.iris.core.dao.cassandra.BaseCassandraModule;
import com.iris.core.dao.cassandra.CassandraUtils;
import com.netflix.governator.configuration.ConfigurationProvider;

public abstract class ModelManagerBaseCassandraModule extends BaseCassandraModule
{

    public ModelManagerBaseCassandraModule(ConfigurationProvider config, String name){
        super(config, name);
    }

    @Provides @Singleton
    public Profile getProfile() {
        Profile profile = new Profile();
        profile.setKeyspace(getKeyspace());

        if(StringUtils.isNotBlank(getContactPoints())) {
            String[] nodeList = getContactPoints().split(",");
            profile.setNodes(Arrays.asList(nodeList));
        }else{
            profile.setNodes(ImmutableList.<String>of());
        }

        profile.setUsername(CassandraUtils.getUsername(getConfig(), getName()));
        profile.setPassword(CassandraUtils.getPassword(getConfig(), getName()));
        profile.setPort(getPort());
        profile.setConsistencyLevel(getConsistencyLevel());
        return profile;
    }



}

