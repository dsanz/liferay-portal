/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.workflow.kaleo.internal.upgrade.v1_3_4;

import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.LoggingTimer;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.upgrade.AutoBatchPreparedStatementUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author Inácio Nery
 */
public class UpgradeKaleoDefinitionVersion extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		upgradeKaleoDefinitionVersion();
	}

	protected String getVersion(int version) {
		return version + StringPool.PERIOD + 0;
	}

	protected void upgradeKaleoDefinitionVersion() throws SQLException {
		StringBundler sb = new StringBundler(7);

		sb.append("insert into KaleoDefinitionVersion ");
		sb.append("(kaleoDefinitionVersionId, groupId, companyId, userId, ");
		sb.append("userName, statusByUserId, statusByUserName, statusDate, ");
		sb.append("createDate, kaleoDefinitionId, name, title, description, ");
		sb.append("content, version, active_, startKaleoNodeId, status) ");
		sb.append("values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ");
		sb.append("?, ?)");

		try (LoggingTimer loggingTimer = new LoggingTimer();
			PreparedStatement ps1 = connection.prepareStatement(
				"select * from KaleoDefinition");
			PreparedStatement ps2 =
				AutoBatchPreparedStatementUtil.concurrentAutoBatch(
					connection, sb.toString());
			ResultSet rs = ps1.executeQuery()) {

			while (rs.next()) {
				long kaleoDefinitionId = rs.getLong("kaleoDefinitionId");
				long groupId = rs.getLong("groupId");
				long companyId = rs.getLong("companyId");
				long userId = rs.getLong("userId");
				String userName = rs.getString("userName");
				Timestamp createDate = rs.getTimestamp("createDate");
				Timestamp modifiedDate = rs.getTimestamp("modifiedDate");
				String name = rs.getString("name");
				String title = rs.getString("title");
				String description = rs.getString("description");
				String content = rs.getString("content");
				int version = rs.getInt("version");
				boolean active = rs.getBoolean("active_");
				long startKaleoNodeId = rs.getLong("startKaleoNodeId");

				ps2.setLong(1, increment());
				ps2.setLong(2, groupId);
				ps2.setLong(3, companyId);
				ps2.setLong(4, userId);
				ps2.setString(5, userName);
				ps2.setLong(6, userId);
				ps2.setString(7, userName);
				ps2.setTimestamp(8, modifiedDate);
				ps2.setTimestamp(9, createDate);
				ps2.setLong(10, kaleoDefinitionId);
				ps2.setString(11, name);
				ps2.setString(12, title);
				ps2.setString(13, description);
				ps2.setString(14, content);
				ps2.setString(15, getVersion(version));
				ps2.setBoolean(16, active);
				ps2.setLong(17, startKaleoNodeId);
				ps2.setInt(18, WorkflowConstants.STATUS_APPROVED);

				ps2.addBatch();
			}

			ps2.executeBatch();
		}
	}

}