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
package com.iris.core.template;

import java.io.IOException;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

/**
 * Handlebars helper that allows for string comparisons in template files.
 * 
 * The helper will evaluate the resolved context variable against the input parameter
 * for String equality. When True it will return the value in the if_equal block, otherwise 
 * it will return the value in the else block.
 * 
 * Sample format:
 * 
 * {{#if_equal someVar \"MYSTRING\"}}The value of someVar is MYSTRING{{else}}The value of someVar is not MYSTRING{/if_equal}}
 * 
 * String context parameters that evaluate to null will always be considered as a false equivalence test.
 * 
 * Do not confuse the String context with the Options#context as they differ.
 * 
 * @author Trip, Terry Trippany
 */
public class IfEqualHelper implements Helper<String> {

	public static final String NAME = "if_equal";
	public static final Helper<String> INSTANCE = new IfEqualHelper();

	@Override
	public CharSequence apply(String context, Options options) throws IOException {
		if (options.params.length != 1 || !(options.params[0] instanceof String)) {
			throw new IllegalArgumentException(
					"#if_equal requires one and only one String as an argument to compare against.\nShould be of the form:\n{{#if_equal someVar \"MYSTRING\"}}The value of someVar is MYSTRING{{else}}The value of someVar is not MYSTRING{/if_equal}}");
		}

		String match = (String) options.params[0];

		if (context == null || !context.equals(match)) {
			return options.inverse(Context.newContext(options.context, context));
		}

		return options.fn(Context.newContext(options.context, context));
	}
}

