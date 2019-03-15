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

public class SoundFile {
    private final Model version;            // Hardware Version that the sound file was introduced in
    private final SounderMode mode;             // Mode for playing the sound file.
    private final String info;                  // This is either the tones to play for play_tone, or the URL for player, depending on the version.
    private final String info2;                  // This is either the tones to play for play_tone, or the URL for player, depending on the version.
    
    private SoundFile(Model version, SounderMode mode, String info, String info2) {
        this.version = version;
        this.mode = mode;
        this.info = info;
        this.info2 = info2;
    }
    
    Model getVersion() {
        return version;
    }
    SounderMode getMode() {
        return mode;
    }
    String getInfo() {
        return info;
    }
    String getInfo2() {
        return info2;
    }
    
    @Override
	public String toString() {
		return "SoundFile [version=" + version + ", mode=" + mode + ", info=" + info + ", info2=" + info2 + "]";
	}

	private static final Builder BUILDER = new Builder();
    
    public static Builder builder() {
        return BUILDER;
    }
    public static class Builder {
        private Model version;            // Hardware Version that the sound file was introduced in
        private SounderMode mode;           // Mode for playing the sound file.
        private String info;                // First item to play 
        private String info2;				// Second item to play
        
        public Builder() {
            version = Model.IH200;
            mode = SounderMode.NO_SOUND;
            info = "nosound.txt";
            info2 = null;
        }
        
        public Builder version(Model version) {
            this.version = version;
            return this;
        }
        
        public Builder v2() {
            version = Model.IH200;
            return this;
        }
        
        public Builder v3() {
            version = Model.IH300;
            return this;
        }
        
        public Builder mode(SounderMode mode) {
            this.mode = mode;
            return this;
        }
        
        public Builder info(String info) {
            this.info = info;
            return this;
        }

        public Builder info2(String info2) {
            this.info2 = info2;
            return this;
        }
        
        public SoundFile build() {
            return new SoundFile(version,mode,info,info2);
        }
        
    }
    
};

