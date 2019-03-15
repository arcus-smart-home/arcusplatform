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
package com.iris.detector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum ProfanityDetector {

   INSTANCE;

	private final static Logger logger = LoggerFactory.getLogger(ProfanityDetector.class);
	private static final String DEFAULT_WORDLIST = "/profanity.data";
	private static final DetectorResult NOT_FOUND = new DetectorResult(false, null);

	private Map<Character,List<String>> dictionary;

	ProfanityDetector() {
	   try(BufferedReader r = new BufferedReader(new InputStreamReader(ProfanityDetector.class.getResourceAsStream(DEFAULT_WORDLIST)))) {
	      String line = null;
	      String word = null;
	      Map<Character,List<String>> words = new LinkedHashMap<>();
	      while ( (line = r.readLine()) != null) {
	         word = line.trim();
	         if (word.length() > 0 && !(word.charAt(0) == '#')) {
	            word = word.toLowerCase();
	            char first = word.charAt(0);
	            List<String> entries = words.get(first);
	            if (entries == null) {
	               entries = new ArrayList<>();
	               words.put(first, entries);
	            }
	            entries.add(word);
	         }
	      }
	      Comparator<String> alphaComparator = new AlphaComparator();
	      for (Character c: words.keySet()) {
	         words.get(c).sort(alphaComparator);
	      }
	      dictionary = words;
	   } catch(Exception e) {
	      throw new RuntimeException(e);
	   }
	}

	public DetectorResult detect(String input) {
		logger.trace("Detecting profanity in '{}'", input);

		if (input == null || input.length() == 0) {
			return NOT_FOUND;
		}

		input = input.toLowerCase();

		int size = input.length();
		for (int i = 0; i < size; i++) {
			List<String> words = dictionary.get(input.charAt(i));
			if (words != null) {
				for (String word : words) {
					if (containsAt(input, word, i)) {
						logger.trace("Profanity {} detected in {}", word, input);
						return new DetectorResult(true, word);
					}
				}
			}
		}
		return NOT_FOUND;
	}

	private boolean containsAt(String source, String target, int pos) {
		if (source.length() - (pos + target.length()) < 0) return false;
		for (int i = 0; i < target.length(); i++) {
			if (source.charAt(pos + i) != target.charAt(i)) return false;
		}
		return true;
	}


	private class AlphaComparator implements Comparator<String> {
		@Override
		public int compare(String o1, String o2) {
			if (o1 == null || o2 == null) throw new NullPointerException("Comparable values cannot be null");
			return o1.compareTo(o2);
		}

	}


}

