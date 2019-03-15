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
package com.iris.bridge.server.http.handlers;

public class SessionActionHelper {
	
	public static String PARAM_TOKEN = "token";
	public static String PARAM_ACTION = "action";
	
	
	static enum Action {
		LOGOUT,
		DISCONNECT
	}
	private static final String NEWLINE = "\r\n";
	
	private static final String SESSION_ACTION_TEMPLATE =
    "<FORM name='%s' action='/adhocsession' method='POST'>" + NEWLINE
 		   + "<a href='javascript:document.%s.submit();'>%s</a>" + NEWLINE 
 		   + "<input type='hidden' name='%s' value='%s' />" + NEWLINE
 		   + "<input type='hidden' name='%s' value='%s' />" + NEWLINE
 		   + "</FORM>"+NEWLINE;
	/*private static final String SESSION_ACTION_TEMPLATE =
	   		"<a href='/adhocsession?sessionId=%s&action=%s'>%s</a>" + NEWLINE; */
	
	
	public static String createHtml(int count, String token, Action action) {
		return String.format(SESSION_ACTION_TEMPLATE, 
				action.name()+count,  //form name
				action.name()+count,  //form name
				action.name(),  //link text
				PARAM_TOKEN,
				token,	//sessionId
				PARAM_ACTION,
				action.name()  //action
				);
	}
}

