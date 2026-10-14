/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.data.cleanup.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.dao.db.PostgreSQLDB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.AssumeTestRule;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.upgrade.data.cleanup.PostgreSQLRuleDataCleanupPreupgradeProcess;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Mariano Álvaro Sáiz
 */
@RunWith(Arquillian.class)
public class PostgreSQLRuleDataCleanupPreupgradeProcessTest
	extends PostgreSQLRuleDataCleanupPreupgradeProcess {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new AssumeTestRule("assume"), new LiferayIntegrationTestRule());

	public static void assume() {
		Assume.assumeTrue(DBManagerUtil.getDBType() == DBType.POSTGRESQL);
	}

	@Before
	public void setUp() throws Exception {
		runSQL(
			"create table TestTable (id_ bigint not null primary key, data_ " +
				"oid)");
	}

	@After
	public void tearDown() throws Exception {
		runSQL("drop table TestTable");
	}

	@Test
	public void testUpgrade() throws Exception {
		runSQL(
			StringUtil.replace(
				PostgreSQLDB.getCreateRulesSQL("TestTable", "data_"),
				"pg_largeobject_metadata where (oid",
				"pg_largeobject where (loid"));

		upgrade();

		List<String> ruleNames = new ArrayList<>();

		try (Connection connection = DataAccess.getConnection();

			PreparedStatement preparedStatement = connection.prepareStatement(
				"select rulename from pg_catalog.pg_rules where tablename = " +
					"'testtable' and definition like " +
						"'%pg_largeobject_metadata%' order by rulename");

			ResultSet resultSet = preparedStatement.executeQuery()) {

			while (resultSet.next()) {
				ruleNames.add(resultSet.getString("rulename"));
			}
		}

		Assert.assertEquals(
			Arrays.asList("delete_testtable_data_", "update_testtable_data_"),
			ruleNames);
	}

}