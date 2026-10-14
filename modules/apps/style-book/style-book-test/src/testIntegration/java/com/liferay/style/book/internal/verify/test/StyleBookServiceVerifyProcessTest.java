/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.style.book.internal.verify.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.change.tracking.model.CTCollection;
import com.liferay.change.tracking.service.CTCollectionLocalService;
import com.liferay.change.tracking.service.CTEntryLocalService;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.kernel.change.tracking.CTCollectionThreadLocal;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.dao.orm.EntityCacheUtil;
import com.liferay.portal.kernel.dao.orm.FinderCacheUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.verify.VerifyProcess;
import com.liferay.style.book.model.StyleBookEntry;
import com.liferay.style.book.service.StyleBookEntryLocalService;

import java.sql.Connection;
import java.sql.PreparedStatement;

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
public class StyleBookServiceVerifyProcessTest {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_ctCollection = _ctCollectionLocalService.addCTCollection(
			null, TestPropsValues.getCompanyId(), TestPropsValues.getUserId(),
			0, RandomTestUtil.randomString(), RandomTestUtil.randomString());

		_group = GroupTestUtil.addGroup();

		_serviceContext = ServiceContextTestUtil.getServiceContext(
			_group, TestPropsValues.getUserId());
	}

	@Test
	public void testVerify() throws Exception {
		_testVerifyWithDuplicateDefaultStyleBookEntries();
		_testVerifyWithoutDuplicateDefaultStyleBookEntries();

		_testVerifyWithReadOnlyCTCollection();
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
		StyleBookEntry styleBookEntry, String themeId) {

		StyleBookEntry defaultStyleBookEntry =
			_styleBookEntryLocalService.fetchDefaultStyleBookEntry(
				_group.getGroupId(), themeId);

		Assert.assertEquals(
			styleBookEntry.getStyleBookEntryId(),
			defaultStyleBookEntry.getStyleBookEntryId());
	}

	private int _getDefaultStyleBookEntriesCount(String themeId) {
		try (SafeCloseable safeCloseable =
				CTCollectionThreadLocal.setCTCollectionIdWithSafeCloseable(
					_ctCollection.getCtCollectionId())) {

			return ListUtil.count(
				_styleBookEntryLocalService.getStyleBookEntries(
					_group.getGroupId(), themeId),
				StyleBookEntry::isDefaultStyleBookEntry);
		}
	}

	private void _testVerifyWithDuplicateDefaultStyleBookEntries()
		throws Exception {

		String themeId = RandomTestUtil.randomString();

		StyleBookEntry styleBookEntry1 = _addStyleBookEntry(true, themeId);

		StyleBookEntry styleBookEntry2 = _addStyleBookEntry(false, themeId);

		_updateDefaultStyleBookEntry(styleBookEntry2);

		Assert.assertEquals(2, _getDefaultStyleBookEntriesCount(themeId));

		_verifyProcess.verify();

		Assert.assertEquals(1, _getDefaultStyleBookEntriesCount(themeId));

		try (SafeCloseable safeCloseable =
				CTCollectionThreadLocal.setCTCollectionIdWithSafeCloseable(
					_ctCollection.getCtCollectionId())) {

			_assertDefaultStyleBookEntry(styleBookEntry2, themeId);
		}

		_assertDefaultStyleBookEntry(styleBookEntry1, themeId);

		int count = _ctEntryLocalService.getCTCollectionCTEntriesCount(
			_ctCollection.getCtCollectionId());

		_verifyProcess.verify();

		Assert.assertEquals(
			count,
			_ctEntryLocalService.getCTCollectionCTEntriesCount(
				_ctCollection.getCtCollectionId()));
	}

	private void _testVerifyWithReadOnlyCTCollection() throws Exception {
		String themeId = RandomTestUtil.randomString();

		_addStyleBookEntry(true, themeId);

		StyleBookEntry styleBookEntry = _addStyleBookEntry(false, themeId);

		_updateDefaultStyleBookEntry(styleBookEntry);

		_ctCollection.setStatus(WorkflowConstants.STATUS_EXPIRED);

		_ctCollection = _ctCollectionLocalService.updateCTCollection(
			_ctCollection);

		_verifyProcess.verify();

		Assert.assertEquals(2, _getDefaultStyleBookEntriesCount(themeId));
	}

	private void _testVerifyWithoutDuplicateDefaultStyleBookEntries()
		throws Exception {

		String themeId = RandomTestUtil.randomString();

		_addStyleBookEntry(true, themeId);

		StyleBookEntry styleBookEntry = _addStyleBookEntry(false, themeId);

		try (SafeCloseable safeCloseable =
				CTCollectionThreadLocal.setCTCollectionIdWithSafeCloseable(
					_ctCollection.getCtCollectionId())) {

			_styleBookEntryLocalService.updateDefaultStyleBookEntry(
				styleBookEntry.getStyleBookEntryId(), true);
		}

		int count = _ctEntryLocalService.getCTCollectionCTEntriesCount(
			_ctCollection.getCtCollectionId());

		_verifyProcess.verify();

		Assert.assertEquals(
			count,
			_ctEntryLocalService.getCTCollectionCTEntriesCount(
				_ctCollection.getCtCollectionId()));
		Assert.assertEquals(1, _getDefaultStyleBookEntriesCount(themeId));
	}

	private void _updateDefaultStyleBookEntry(StyleBookEntry styleBookEntry)
		throws Exception {

		try (SafeCloseable safeCloseable =
				CTCollectionThreadLocal.setCTCollectionIdWithSafeCloseable(
					_ctCollection.getCtCollectionId())) {

			_styleBookEntryLocalService.updateName(
				styleBookEntry.getStyleBookEntryId(),
				RandomTestUtil.randomString());
		}

		try (Connection connection = DataAccess.getConnection();

			PreparedStatement preparedStatement = connection.prepareStatement(
				"update StyleBookEntry set defaultStyleBookEntry = ? where " +
					"ctCollectionId = ? and styleBookEntryId = ?")) {

			preparedStatement.setBoolean(1, true);
			preparedStatement.setLong(2, _ctCollection.getCtCollectionId());
			preparedStatement.setLong(3, styleBookEntry.getStyleBookEntryId());

			preparedStatement.executeUpdate();
		}

		EntityCacheUtil.clearCache();
		FinderCacheUtil.clearCache();
	}

	private CTCollection _ctCollection;

	@Inject
	private CTCollectionLocalService _ctCollectionLocalService;

	@Inject
	private CTEntryLocalService _ctEntryLocalService;

	private Group _group;
	private ServiceContext _serviceContext;

	@Inject
	private StyleBookEntryLocalService _styleBookEntryLocalService;

	@Inject(
		filter = "component.name=com.liferay.style.book.internal.verify.StyleBookServiceVerifyProcess"
	)
	private VerifyProcess _verifyProcess;

}