/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.style.book.internal.upgrade.v1_10_1;

import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Thiago Buarque
 */
public class StyleBookEntryDefaultStyleBookEntryUpgradeProcess
	extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		Set<String> groupIdThemeIds = new HashSet<>();

		try (PreparedStatement preparedStatement1 = connection.prepareStatement(
				StringBundler.concat(
					"select styleBookEntryId, groupId, themeId from ",
					"StyleBookEntry where ctCollectionId = 0 and head = ? and ",
					"defaultStyleBookEntry = ? order by createDate desc, ",
					"styleBookEntryId desc"));
			PreparedStatement preparedStatement2 =
				AutoBatchPreparedStatementUtil.autoBatch(
					connection,
					"update StyleBookEntry set defaultStyleBookEntry = ? " +
						"where ctCollectionId = 0 and styleBookEntryId = ?")) {

			preparedStatement1.setBoolean(1, true);
			preparedStatement1.setBoolean(2, true);

			try (ResultSet resultSet = preparedStatement1.executeQuery()) {
				while (resultSet.next()) {
					String groupIdThemeId = StringBundler.concat(
						resultSet.getLong("groupId"), StringPool.POUND,
						resultSet.getString("themeId"));

					if (groupIdThemeIds.add(groupIdThemeId)) {
						continue;
					}

					preparedStatement2.setBoolean(1, false);
					preparedStatement2.setLong(
						2, resultSet.getLong("styleBookEntryId"));

					preparedStatement2.addBatch();
				}
			}

			preparedStatement2.executeBatch();
		}
	}

}