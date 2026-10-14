/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.data.cleanup;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.dao.db.PostgreSQLDB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.upgrade.data.cleanup.DataCleanupPreupgradeProcess;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Mariano Álvaro Sáiz
 */
public class PostgreSQLRuleDataCleanupPreupgradeProcess
	extends DataCleanupPreupgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		if (DBManagerUtil.getDBType() != DBType.POSTGRESQL) {
			return;
		}

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select distinct tablename, substr(rulename, ",
					"length(tablename) + 9) columnname from pg_catalog.",
					"pg_rules where schemaname = current_schema() and ",
					"rulename ~ ('^(delete|update)_' || tablename || '_') and ",
					"definition ~ 'pg_largeobject\\M'"));

			ResultSet resultSet = preparedStatement.executeQuery()) {

			while (resultSet.next()) {
				String tableName = resultSet.getString("tablename");
				String columnName = resultSet.getString("columnname");

				runSQL(PostgreSQLDB.getCreateRulesSQL(tableName, columnName));

				if (_log.isInfoEnabled()) {
					_log.info(
						StringBundler.concat(
							"Table ", tableName, ", rules for column ",
							columnName, " were recreated because they ",
							"referenced \"pg_catalog.pg_largeobject\""));
				}
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		PostgreSQLRuleDataCleanupPreupgradeProcess.class);

}