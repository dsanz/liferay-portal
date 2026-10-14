/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.style.book.internal.verify;

import com.liferay.change.tracking.model.CTCollection;
import com.liferay.change.tracking.service.CTCollectionLocalService;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.change.tracking.CTCollectionThreadLocal;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Release;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.Tuple;
import com.liferay.portal.verify.VerifyProcess;
import com.liferay.style.book.model.StyleBookEntry;
import com.liferay.style.book.service.StyleBookEntryLocalService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Thiago Buarque
 */
@Component(service = VerifyProcess.class)
public class StyleBookServiceVerifyProcess extends VerifyProcess {

	@Override
	protected void doVerify() throws Exception {
		List<Tuple> tuples = new ArrayList<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select distinct ctCollectionId, groupId, themeId from ",
					"StyleBookEntry where ctCollectionId > 0 and head = ? and ",
					"defaultStyleBookEntry = ?"))) {

			preparedStatement.setBoolean(1, true);
			preparedStatement.setBoolean(2, true);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					tuples.add(
						new Tuple(
							resultSet.getLong("ctCollectionId"),
							resultSet.getLong("groupId"),
							resultSet.getString("themeId")));
				}
			}
		}

		for (Tuple tuple : tuples) {
			long ctCollectionId = (Long)tuple.getObject(0);
			long groupId = (Long)tuple.getObject(1);
			String themeId = (String)tuple.getObject(2);

			try {
				_unsetDuplicateDefaultStyleBookEntries(
					ctCollectionId, groupId, themeId);
			}
			catch (Exception exception) {
				_log.error(
					StringBundler.concat(
						"Unable to remove duplicated default style books for ",
						"group ", groupId, " and theme ", themeId,
						" in publication ", ctCollectionId),
					exception);
			}
		}
	}

	private void _unsetDuplicateDefaultStyleBookEntries(
			long ctCollectionId, long groupId, String themeId)
		throws Exception {

		CTCollection ctCollection = _ctCollectionLocalService.fetchCTCollection(
			ctCollectionId);

		if ((ctCollection == null) || ctCollection.isReadOnly()) {
			return;
		}

		try (SafeCloseable safeCloseable =
				CTCollectionThreadLocal.setCTCollectionIdWithSafeCloseable(
					ctCollectionId)) {

			String name = PrincipalThreadLocal.getName();

			PrincipalThreadLocal.setName(ctCollection.getUserId(), false);

			try {
				List<StyleBookEntry> styleBookEntries = ListUtil.filter(
					_styleBookEntryLocalService.getStyleBookEntries(
						groupId, themeId),
					StyleBookEntry::isDefaultStyleBookEntry);

				for (int i = 1; i < styleBookEntries.size(); i++) {
					StyleBookEntry styleBookEntry = styleBookEntries.get(i);

					_styleBookEntryLocalService.updateDefaultStyleBookEntry(
						styleBookEntry.getStyleBookEntryId(), false);
				}
			}
			finally {
				PrincipalThreadLocal.setName(name, false);
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		StyleBookServiceVerifyProcess.class);

	@Reference
	private CTCollectionLocalService _ctCollectionLocalService;

	@Reference(
		target = "(&(release.bundle.symbolic.name=com.liferay.style.book.service)(release.schema.version>=1.10.1))"
	)
	private Release _release;

	@Reference
	private StyleBookEntryLocalService _styleBookEntryLocalService;

}