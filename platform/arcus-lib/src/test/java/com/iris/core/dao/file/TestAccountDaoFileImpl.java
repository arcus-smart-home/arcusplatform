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
package com.iris.core.dao.file;

import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.core.dao.AccountDAO;
import com.iris.io.json.gson.GsonModule;
import com.iris.messages.model.Account;

public class TestAccountDaoFileImpl {
	private AccountDAO dao;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {

		Bootstrap bootstrap = Bootstrap
				.builder()
				.withModuleClasses(GsonModule.class, FileDAOModule.class)
				.build();
		ServiceLocator.init(GuiceServiceLocator.create(bootstrap.bootstrap()));

		dao = ServiceLocator.getInstance(AccountDAO.class);
	}

	@After
	public void tearDown() throws Exception {
		ServiceLocator.destroy();
	}

	@Test
	public void testFindById() throws Exception {
		Account account = dao.findById(UUID.fromString("c24b0e18-3394-4f81-b762-274ba3605ccc"));
		Assert.assertEquals("c24b0e18-3394-4f81-b762-274ba3605ccc", account.getId().toString());
		Assert.assertEquals("active", account.getState());
	}
}

