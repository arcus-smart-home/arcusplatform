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
package com.iris.platform.rule.catalog.condition;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.iris.common.rule.condition.Condition;
import com.iris.common.rule.filter.MatcherFilter;
import com.iris.common.rule.matcher.ModelPredicateMatcher;
import com.iris.messages.model.Model;
import com.iris.platform.rule.catalog.template.TemplatedValue;

public class ContextQueryTemplate extends FilterTemplate {

	private TemplatedValue<Predicate<Model>> selector = null;
	private TemplatedValue<Predicate<Model>> matcher = null;
	
	
	public TemplatedValue<Predicate<Model>> getSelector() {
		return selector;
	}


	public void setSelector(TemplatedValue<Predicate<Model>> selector) {
		this.selector = selector;
	}


	public TemplatedValue<Predicate<Model>> getMatcher() {
		return matcher;
	}


	public void setMatcher(TemplatedValue<Predicate<Model>> matcher) {
		this.matcher = matcher;
	}

	@Override
	public Condition generate(Map<String, Object> values) {
		Preconditions.checkNotNull(getCondition(), "Must specify a delegate condition");
		Preconditions.checkNotNull(matcher, "Must specify a matcher");
		
		Predicate<Model> selectorPred = Predicates.alwaysTrue();
		if(selector != null) {
			selectorPred = selector.apply(values);
		}
		Predicate<Model> matcherPred = matcher.apply(values);
		Condition condition = getCondition().generate(values);
		
		return new MatcherFilter(condition, new ModelPredicateMatcher(selectorPred, matcherPred));
	}


	@Override
	public String toString() {
		return "ContextQueryTemplate [selector=" + selector + ", matcher="
				+ matcher + "]";
	}	
}

