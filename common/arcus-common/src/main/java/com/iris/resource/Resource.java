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
package com.iris.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import com.iris.util.Subscription;

/**
 * 
 */
public interface Resource {

   public InputStream open() throws IOException;
   
   public boolean exists();
   
   public boolean isReadable();
   
   public boolean isFile();
   
   public boolean isDirectory();
   
   public List<Resource> listResources();

   public String getRepresentation();
   
   public URI getUri();
   
   public File getFile();
   
   public boolean isWatchable();
   
   public Subscription addWatch(ResourceListener listener);
   
   public void dispose();
}

