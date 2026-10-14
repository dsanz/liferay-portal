/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.v7_4_x;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Yuri Monteiro
 */
public class AssetTagGroupRelUpgradeProcess extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		try (PreparedStatement preparedStatement1 = connection.prepareStatement(
				StringBundler.concat(
					"select AssetTag.tagId, AssetTag.companyId from AssetTag ",
					"inner join Group_ on Group_.groupId = AssetTag.groupId ",
					"where AssetTag.ctCollectionId = 0 and ",
					"Group_.ctCollectionId = 0 and Group_.groupKey = ? and ",
					"not exists (select 1 from AssetTagGroupRel where ",
					"AssetTagGroupRel.ctCollectionId = 0 and ",
					"AssetTagGroupRel.tagId = AssetTag.tagId and ",
					"AssetTagGroupRel.depotEntryType = ?)"));
			PreparedStatement preparedStatement2 =
				AutoBatchPreparedStatementUtil.autoBatch(
					connection,
					StringBundler.concat(
						"insert into AssetTagGroupRel (mvccVersion, ",
						"ctCollectionId, uuid_, assetTagGroupRelId, groupId, ",
						"companyId, tagId, depotEntryType) values (0, 0, ?, ",
						"?, ?, ?, ?, ?)"))) {

			preparedStatement1.setString(1, GroupConstants.CMS);
			preparedStatement1.setInt(2, _DEPOT_ENTRY_TYPE_PROJECT);

			try (ResultSet resultSet = preparedStatement1.executeQuery()) {
				while (resultSet.next()) {
					preparedStatement2.setString(1, PortalUUIDUtil.generate());
					preparedStatement2.setLong(2, increment());
					preparedStatement2.setLong(3, GroupConstants.GROUP_ID_ALL);
					preparedStatement2.setLong(
						4, resultSet.getLong("companyId"));
					preparedStatement2.setLong(5, resultSet.getLong("tagId"));
					preparedStatement2.setInt(6, _DEPOT_ENTRY_TYPE_PROJECT);

					preparedStatement2.addBatch();
				}
			}

			preparedStatement2.executeBatch();
		}
	}

	private static final int _DEPOT_ENTRY_TYPE_PROJECT = 2;

}