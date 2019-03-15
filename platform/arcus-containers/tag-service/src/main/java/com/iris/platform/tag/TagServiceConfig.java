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
package com.iris.platform.tag;

import static com.iris.core.platform.TaggedEventBuilder.KEY_RULE_TEMPLATE_ID;
import static com.iris.messages.service.SessionService.TaggedEvent.ATTR_SERVICELEVEL;
import static com.iris.messages.service.SessionService.TaggedEvent.ATTR_SOURCE;
import static com.iris.messages.service.SessionService.TaggedEvent.ATTR_VERSION;
import static org.apache.commons.lang3.StringUtils.join;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class TagServiceConfig
{
   @Inject(optional = true) @Named("tag.service.threads.max")
   private int threads = 100;

   @Inject(optional = true) @Named("tag.service.threads.keepAliveMs")
   private int keepAliveMs = 10_000;

   @Inject(optional = true) @Named("tag.includes.path")
   private String tagIncludesPath = "conf/tag-include.txt";

   @Inject(optional = true) @Named("tag.context.includes")
   private String contextIncludes =
      join(new String[] { ATTR_SOURCE, ATTR_VERSION, ATTR_SERVICELEVEL, KEY_RULE_TEMPLATE_ID }, ',');

   public int getThreads()
   {
      return threads;
   }

   public void setThreads(int threads)
   {
      this.threads = threads;
   }

   public int getKeepAliveMs()
   {
      return keepAliveMs;
   }

   public void setKeepAliveMs(int keepAliveMs)
   {
      this.keepAliveMs = keepAliveMs;
   }

   public String getTagIncludesPath()
   {
      return tagIncludesPath;
   }

   public void setTagIncludesPath(String tagIncludesPath)
   {
      this.tagIncludesPath = tagIncludesPath;
   }

   public String getContextIncludes()
   {
      return contextIncludes;
   }

   public void setContextIncludes(String contextIncludes)
   {
      this.contextIncludes = contextIncludes;
   }
}

