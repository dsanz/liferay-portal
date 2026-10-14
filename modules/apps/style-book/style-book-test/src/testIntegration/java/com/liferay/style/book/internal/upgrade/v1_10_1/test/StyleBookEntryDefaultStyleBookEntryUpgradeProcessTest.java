/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.style.book.internal.upgrade.v1_10_1.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.dao.orm.EntityCacheUtil;
import com.liferay.portal.kernel.dao.orm.FinderCacheUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.upgrade.registry.UpgradeStepRegistrator;
import com.liferay.portal.upgrade.test.util.UpgradeTestUtil;
import com.liferay.style.book.model.StyleBookEntry;
import com.liferay.style.book.service.StyleBookEntryLocalService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Thiago Buarque
 */
@RunWith(Arquillian.class)
public class StyleBookEntryDefaultStyleBookEntryUpgradeProcessTest {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();

		_serviceContext = ServiceContextTestUtil.getServiceContext(
			_group, TestPropsValues.getUserId());
	}

	@Test
	public void testUpgrade() throws Exception {
		String themeId = RandomTestUtil.randomString();

		StyleBookEntry styleBookEntry1 = _addStyleBookEntry(false, themeId);
		StyleBookEntry styleBookEntry2 = _addStyleBookEntry(true, themeId);

		_updateStyleBookEntry(
			new Date(System.currentTimeMillis() - Time.HOUR),
			new Date(System.currentTimeMillis() + Time.HOUR),
			styleBookEntry1.getStyleBookEntryId());

		Assert.assertEquals(
			styleBookEntry2.getStyleBookEntryId(),
			_getDefaultStyleBookEntryId(themeId));

		_runUpgrade();

		EntityCacheUtil.clearCache();
		FinderCacheUtil.clearCache();

		Assert.assertEquals(
			styleBookEntry2.getStyleBookEntryId(),
			_getDefaultStyleBookEntryId(themeId));

		_assertDefaultStyleBookEntry(
			false, styleBookEntry1.getStyleBookEntryId());
		_assertDefaultStyleBookEntry(
			true, styleBookEntry2.getStyleBookEntryId());
	}

	private StyleBookEntry _addStyleBookEntry(
			boolean defaultStyleBookEntry, String themeId)
		throws Exception {

		return _styleBookEntryLocalService.addStyleBookEntry(
			RandomTestUtil.randomString(), TestPropsValues.getUserId(),
			_group.getGroupId(), defaultStyleBookEntry, null, null,
			RandomTestUtil.randomString(), null, themeId, _serviceContext);
	}

	private void _assertDefaultStyleBookEntry(
			boolean defaultStyleBookEntry, long styleBookEntryId)
		throws Exception {

		StyleBookEntry styleBookEntry =
			_styleBookEntryLocalService.getStyleBookEntry(styleBookEntryId);

		Assert.assertEquals(
			defaultStyleBookEntry, styleBookEntry.isDefaultStyleBookEntry());
	}

	private long _getDefaultStyleBookEntryId(String themeId) {
		StyleBookEntry styleBookEntry =
			_styleBookEntryLocalService.fetchDefaultStyleBookEntry(
				_group.getGroupId(), themeId);

		return styleBookEntry.getStyleBookEntryId();
	}

	private void _runUpgrade() throws Exception {
		UpgradeProcess upgradeProcess = UpgradeTestUtil.getUpgradeStep(
			_upgradeStepRegistrator, _CLASS_NAME);

		upgradeProcess.upgrade();
	}

	private void _updateStyleBookEntry(
			Date createDate, Date modifiedDate, long styleBookEntryId)
		throws Exception {

		try (Connection connection = DataAccess.getConnection();

			PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"update StyleBookEntry set createDate = ?, ",
					"defaultStyleBookEntry = ?, modifiedDate = ? where ",
					"styleBookEntryId = ?"))) {

			preparedStatement.setTimestamp(
				1, new Timestamp(createDate.getTime()));
			preparedStatement.setBoolean(2, true);
			preparedStatement.setTimestamp(
				3, new Timestamp(modifiedDate.getTime()));
			preparedStatement.setLong(4, styleBookEntryId);

			preparedStatement.executeUpdate();
		}

		EntityCacheUtil.clearCache();
		FinderCacheUtil.clearCache();
	}

	private static final String _CLASS_NAME =
		"com.liferay.style.book.internal.upgrade.v1_10_1." +
			"StyleBookEntryDefaultStyleBookEntryUpgradeProcess";

	private Group _group;
	private ServiceContext _serviceContext;

	@Inject
	private StyleBookEntryLocalService _styleBookEntryLocalService;

	@Inject(
		filter = "(&(component.name=com.liferay.style.book.internal.upgrade.registry.StyleBookServiceUpgradeStepRegistrator))"
	)
	private UpgradeStepRegistrator _upgradeStepRegistrator;

}