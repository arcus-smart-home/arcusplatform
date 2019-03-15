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
package com.iris.agent.hal.sound;

import com.iris.agent.hal.Model;
import com.iris.agent.hal.SounderMode;

public class SoundKey {
    private final Model version;
    private final SounderMode mode;
    
    private SoundKey(Model version, SounderMode mode) {
        this.version = version;
        this.mode = mode;
    }
    
    Model getVersion() {
        return version;
    }
    
    SounderMode getMode() {
        return mode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mode == null) ? 0 : mode.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SoundKey other = (SoundKey) obj;
        if (mode != other.mode)
            return false;
        if (version != other.version)
            return false;
        return true;
    }

    private static final Builder BUILDER = new Builder();
    
    public static Builder builder() {
        return BUILDER;
    }
    
    public static class Builder {
        private Model version;
        private SounderMode mode;
        
        public Builder() {
            this.version = Model.IH200;
            this.mode = SounderMode.NO_SOUND;
        }
        
        public Builder mode(SounderMode mode) {
            this.mode = mode;
            return this;
        }
        
        public Builder version(Model version) {
            this.version = version;
            return this;
        }
        
        public SoundKey build() {
            return new SoundKey(version,mode);
        }
        
    }
    
    
}

