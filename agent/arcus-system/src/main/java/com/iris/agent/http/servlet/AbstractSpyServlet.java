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
package com.iris.agent.http.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.iris.agent.http.tpl.TemplateEngine;
import com.iris.agent.spy.SpyPlugIn;
import com.iris.agent.spy.SpyService;

public abstract class AbstractSpyServlet extends HttpServlet {
	private static final long serialVersionUID = 2153565738144215815L;
	private static final String DEFAULT_PAGE = "*";
	
	private final Map<String, Function<HttpServletRequest, Object>> pages = new HashMap<>();
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String pageName = pageName(req);
		resp.setContentType(contentType(pageName));
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.getWriter().println(transform(pageName, loadPage(pageName, req)));
	}
	
	protected void addDefaultPage(Function<HttpServletRequest, Object> pageLoader) {
		pages.put(DEFAULT_PAGE, pageLoader);
	}
	
	protected void addPage(String pageName, Function<HttpServletRequest, Object> pageLoader) {
		pages.put(pageName.toLowerCase(), pageLoader);
	}
	
	protected String contentType(String pageName) {
		return "text/html";
	}
	
	protected String transform(String pageName, Object value) {
		return value != null ? value.toString() : "";
	}
	
	protected String render(String templateName) {
		return TemplateEngine.instance().render(templateName);
	}
	
	protected String render(String templateName, Map<String, Supplier<Object>> context) {
	   return TemplateEngine.instance().render(templateName, context);
	}
	
	private String pageName(HttpServletRequest req) {
		Map<String, String[]> params = req.getParameterMap();
		return params.containsKey("p") 
				? (params.get("p") != null ? params.get("p")[0].toLowerCase() : DEFAULT_PAGE)
				: DEFAULT_PAGE;
	}
	
	private Object loadPage(String pageName, HttpServletRequest req) {
		if (pages.containsKey(pageName)) {
			return pages.get(pageName).apply(req);
		}
		else {
			SpyPlugIn plugin = SpyService.INSTANCE.getPlugInByPage(pageName);
			if (plugin != null) {
			   return plugin.apply(req);
			}
		}
		return "";
	}
	
}

