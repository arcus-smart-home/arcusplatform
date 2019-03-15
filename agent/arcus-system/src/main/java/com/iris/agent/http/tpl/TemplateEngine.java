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
package com.iris.agent.http.tpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.iris.agent.http.tpl.cmds.RenderContext;
import com.iris.agent.http.tpl.cmds.RenderFor;
import com.iris.agent.http.tpl.cmds.RenderStatic;
import com.iris.agent.http.tpl.cmds.RenderSubTemplate;
import com.iris.agent.http.tpl.cmds.TemplateCommand;

/**
 * Super basic and lightweight template engine to avoid adding dependencies to the hub agent.
 * 
 * When rendering a template the user needs to specify the template to be used (loaded from the 
 * classpath) and pass in a context which is a map of names to String providers. 
 * 
 * There are two kinds of template expressions, meta commands, and renders.
 * 
 * Meta commands must be on the first line(s) and start with '#'. Right now the only meta command
 * is 'extends' which must specify another template and the name of a render. 
 * 
 *  META COMMANDS
 *  extends [template] [render_name] 
 *  
 *  Extends will add the current template as a supplier to the context sent to the parent template
 *  specified as the first argument. The supplier will be given the name specified
 *  as the second argument. The parent can then render the current template like anything else
 *  in the context.
 *  
 *  Example:
 *  
 *  In the current template (1st line)
 *  <pre>
 *  #extends parent_template child_content
 *  </pre>
 *  
 *  In the parent template (anywhere)
 *  <pre>
 *  {%= child_content %}
 *  </pre>
 *  
 *  
 *  RENDERS
 *  Renders follow the same syntax pattern.
 *  {%[render_type] [value] %}
 *  
 *  There are three renders currently defined but additional ones may be created by implementing
 *  the interface 'TemplateCommand' and registering with the this class.
 *  
 *    CONTEXT RENDER
 *    {%= [key] %}
 *    
 *    The render syntax is replaced with the value returned from the String supplier specified
 *    by 'key' in the context.x
 *    <pre>
 *    {%= myvalue %}
 *    </pre>
 *    
 *    STATIC RENDER
 *    {%s [class]#[method] %}
 *    
 *    The render syntax is replaced with the static method call specified. Arguments to the method are
 *    not currently supported, so a call that requires an argument will need to be performed by
 *    a context supplier in a context render.
 *    <pre>
 *    {%s com.iris.agent.hal.IrisHal#getHubId %}
 *    </pre>
 *    
 *    TEMPLATE RENDER
 *    {%t [template] %}
 *    
 *    Renders a template and replaces the render syntax with the result. The template will be 
 *    rendered with the current context. It is also possible to add anonymous context entries by using
 *    the following syntax.
 *    
 *    {%t [template]#[value1],[value2],[value3],... }
 *    
 *    In this case, new entries will be added to the context where a numbered key (like '_1', '_2', etc...) will redirect to
 *    the respective context key. The original context keys will remain valid as well.
 *    
 *    Simple Example:
 *    {%t header %}
 *    
 *    Example with Redirection:
 *    
 *    In the calling template,
 *    {%t section#details_section_title,details_section_footer %}
 *    
 *    In the sub-template,
 *    {%= _1 %}  and {%= _2 %}
 *    
 *    {%= _1 %} in the sub-template will be the same as {%= details_section_title %} 
 *    {%= _2 %} in the sub-template will be the same as {%= details_section_footer %}
 * 
 * @author Erik Larson
 *
 */
public final class TemplateEngine {
   private final static Pattern EXTEND_CMD = Pattern.compile("\\s*(\\#extends)\\s+(\\w+)\\s+(\\w+)");
   private final static Pattern EXPRESSION = Pattern.compile("(\\{\\%([\\w=]+)\\s+(\\S+?)\\s*\\%\\})");

   private final Map<String, TemplateCommand> commands = new HashMap<>();

   private static TemplateEngine instance = null;

   public static TemplateEngine instance() {
      if (instance == null) {
         instance = new TemplateEngine();
      }
      return instance;
   }

   private TemplateEngine() {
      register(new RenderContext());
      register(new RenderStatic());
      register(new RenderSubTemplate());
      register(new RenderFor());
   };

   public String render(String template) {
      return render(template, Collections.emptyMap(), new Options());
   }

   public String render(String template, final Map<String, Supplier<Object>> context) {
      return render(template, context, new Options());
   }

   public String render(String template, final Map<String, Supplier<Object>> context, final Options options) {
      final StringBuffer sb = new StringBuffer();
      final State renderState = new State();
      renderState.setCurrentTemplate(template);
      try (BufferedReader br = new BufferedReader(
            new InputStreamReader(TemplateEngine.class.getResourceAsStream("/tpl/" + template + ".tpl")))) {
         String line;
         int lineCount = 0;
         while ((line = br.readLine()) != null) {
            if (lineCount > 0) {
               sb.append("\n");
            }
            String processedLine = processLine(line, context, options, renderState);
            if (processedLine != null && !processedLine.isEmpty()) {
               lineCount++;
               sb.append(processedLine);
            }
            if (renderState.isStopProcessing()) {
               break;
            }
         }
      } catch (IOException e) {

         // Swallow Exception for Now
      }
      return sb.toString();
   }

   public static Map<String, Supplier<String>> createContext() {
      return new HashMap<>();
   }

   public static Map<String, Supplier<String>> createContext(final String key, final Object value) {
      return buildContext().add(key, value).build();
   }

   public static ContextBuilder buildContext() {
      return new ContextBuilder();
   }

   public void register(TemplateCommand cmd) {
      commands.put(cmd.name(), cmd);
   }

   private String processLine(String line, final Map<String, Supplier<Object>> context, final Options options,
         final State renderState) {
      if (renderState.isMetaCommandsActive()) {
         final Matcher matcher = EXTEND_CMD.matcher(line);
         if (matcher.matches()) {
            if (options.isIgnoreMetaCommands()) {
               return "";
            } else {
               String template = matcher.group(2);
               String section = matcher.group(3);
               
               Map<String, Supplier<Object>> newContext = new HashMap<>(context);
               newContext.put(section, () -> TemplateEngine.instance().render(renderState.getCurrentTemplate(), context,
                     optIgnoreMetaCommands()));
               renderState.setStopProcessing(true);
               return TemplateEngine.instance().render(template, newContext, options);
            }
         } else {
            renderState.setMetaCommandsActive(false);
         }
      }
      final Matcher matcher = EXPRESSION.matcher(line);
      while (matcher.find()) {
         String val = matcher.group(1);
         line = line.replace(val, renderCommand(matcher.group(2), matcher.group(3), context));
      }
      return line;
   }

   private String renderCommand(String cmd, String value, Map<String, Supplier<Object>> context) {
      final TemplateCommand templateCommand = commands.get(cmd);
      return templateCommand != null ? templateCommand.evaluate(value, context) : "";
   }

   public static Options optIgnoreMetaCommands() {
      final Options opt = new Options();
      opt.ignoreMetaCommands = true;
      return opt;
   }

   public static class Options {
      private boolean ignoreMetaCommands = false;

      public boolean isIgnoreMetaCommands() {
         return ignoreMetaCommands;
      }

      public void setIgnoreMetaCommands(boolean ignoreMetaCommands) {
         this.ignoreMetaCommands = ignoreMetaCommands;
      }
   }

   public static class ContextBuilder {
      private final Map<String, Supplier<String>> contextMap = new HashMap<>();

      public ContextBuilder add(String key, Supplier<String> supplier) {
         contextMap.put(key, supplier);
         return this;
      }

      public ContextBuilder add(String key, final Object value) {
         contextMap.put(key, () -> value != null ? value.toString() : "");
         return this;
      }

      public Map<String, Supplier<String>> build() {
         return contextMap;
      }

   }

   private static class State {
      private boolean metaCommandsActive = true;
      private boolean stopProcessing = false;
      private String currentTemplate;

      public boolean isMetaCommandsActive() {
         return metaCommandsActive;
      }

      public void setMetaCommandsActive(boolean metaCommandsActive) {
         this.metaCommandsActive = metaCommandsActive;
      }

      public boolean isStopProcessing() {
         return stopProcessing;
      }

      public void setStopProcessing(boolean stopProcessing) {
         this.stopProcessing = stopProcessing;
      }

      public String getCurrentTemplate() {
         return currentTemplate;
      }

      public void setCurrentTemplate(String currentTemplate) {
         this.currentTemplate = currentTemplate;
      }
   }
}

