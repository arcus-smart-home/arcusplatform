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
package com.iris.prodcat.search;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.store.RAMDirectory;

import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalog;

public class ProductIndex {

	private Directory dir;
	private ProductCatalog prodcat;
	private static final String searchField = "content";
	
	public ProductIndex(ProductCatalog prodcat) throws IOException {
		this.prodcat = prodcat;
		dir = new RAMDirectory(NoLockFactory.INSTANCE);
		Analyzer analyzer = new SimpleAnalyzer();
		
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		IndexWriter iw = new IndexWriter(dir, iwc);
		indexProducts(iw, prodcat);
		iw.close();
	}
	
	
	public List<ProductCatalogEntry> search(String queryString) throws IOException, ParseException {
		List<ProductCatalogEntry> results = new ArrayList<ProductCatalogEntry>();
		
		IndexReader reader = DirectoryReader.open(dir);
		IndexSearcher searcher = new IndexSearcher(reader);
		Analyzer analyzer = new SimpleAnalyzer();
		
		QueryParser parser = new QueryParser(searchField, analyzer);
		Query query = parser.parse(queryString);
		
		TopDocs docs = searcher.search(query, 100);
		ScoreDoc[] hits = docs.scoreDocs;
		
		for (ScoreDoc sd: hits) {
			Document doc = searcher.doc(sd.doc);
			results.add(prodcat.getProductById(doc.get("id")));
		}
		reader.close();
		
		return results;
	}
	
	
	private void indexProducts(IndexWriter iw, ProductCatalog prodcat) throws IOException {
		for (ProductCatalogEntry p : prodcat.getAllProducts()) {
			
			if (p.getCanSearch()) {
				Document doc = new Document();
				
				// add the id as stored but not indexed field
				Field idField = new StringField("id", p.getId(), Field.Store.YES);
				doc.add(idField);
				
				// append all data into a single searchable indexed field
				StringBuilder sb = new StringBuilder();
				sb.append(p.getName()).append(" ");
				sb.append(p.getManufacturer()).append(" ");
				sb.append(p.getVendor()).append(" ");
				sb.append(p.getKeywords()).append(" ");
				sb.append(p.getProtoFamily()).append(" ");
				for (String cat : p.getCategories()) {
					sb.append(cat).append(" ");	
				}
				doc.add(new TextField(searchField, new StringReader(sb.toString())));
				
				iw.addDocument(doc);
			}		
		}
	}
	
}

