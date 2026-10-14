/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.v7_4_x.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.model.AssetTagGroupRel;
import com.liferay.asset.kernel.service.AssetTagGroupRelLocalService;
import com.liferay.asset.test.util.AssetTestUtil;
import com.liferay.change.tracking.model.CTCollection;
import com.liferay.change.tracking.service.CTCollectionLocalService;
import com.liferay.depot.constants.DepotConstants;
import com.liferay.depot.model.DepotEntry;
import com.liferay.depot.service.DepotEntryLocalService;
import com.liferay.petra.lang.SafeCloseable;
import com.liferay.portal.kernel.cache.CacheRegistryUtil;
import com.liferay.portal.kernel.change.tracking.CTCollectionThreadLocal;
import com.liferay.portal.kernel.dao.orm.EntityCache;
import com.liferay.portal.kernel.dao.orm.FinderCache;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;
import com.liferay.portal.upgrade.v7_4_x.AssetTagGroupRelUpgradeProcess;
import com.liferay.portlet.asset.model.impl.AssetTagGroupRelImpl;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Yuri Monteiro
 */
@RunWith(Arquillian.class)
public class AssetTagGroupRelUpgradeProcessTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_cmsGroup = _groupLocalService.getGroup(
			TestPropsValues.getCompanyId(), GroupConstants.CMS);

		_cmsAssetTag = AssetTestUtil.addTag(_cmsGroup.getGroupId());
	}

	@Test
	public void testUpgrade() throws Exception {
		Group group = GroupTestUtil.addGroup();

		AssetTag assetTag = AssetTestUtil.addTag(group.getGroupId());

		_runUpgrade();

		_assertAssetTagGroupRel(
			_cmsAssetTag, DepotConstants.TYPE_PROJECT,
			GroupConstants.ANY_PARENT_GROUP_ID);

		Assert.assertEquals(
			Collections.emptyList(),
			_assetTagGroupRelLocalService.
				getAssetTagGroupRelsByTagIdAndDepotEntryType(
					assetTag.getTagId(), DepotConstants.TYPE_PROJECT));

		_groupLocalService.deleteGroup(group);
	}

	@Test
	public void testUpgradeWithProjectAssetTagGroupRel() throws Exception {
		_depotEntry = _addDepotEntry(DepotConstants.TYPE_PROJECT);

		_assetTagGroupRelLocalService.addAssetTagGroupRel(
			_depotEntry.getGroupId(), _cmsAssetTag.getTagId(),
			DepotConstants.TYPE_PROJECT);

		_runUpgrade();

		_assertAssetTagGroupRel(
			_cmsAssetTag, DepotConstants.TYPE_PROJECT,
			_depotEntry.getGroupId());
	}

	@Test
	public void testUpgradeWithSpaceAssetTagGroupRel() throws Exception {
		_assetTagGroupRelLocalService.addAssetTagGroupRel(
			GroupConstants.ANY_PARENT_GROUP_ID, _cmsAssetTag.getTagId(),
			DepotConstants.TYPE_SPACE);

		_runUpgrade();

		_assertAssetTagGroupRel(
			_cmsAssetTag, DepotConstants.TYPE_PROJECT,
			GroupConstants.ANY_PARENT_GROUP_ID);
		_assertAssetTagGroupRel(
			_cmsAssetTag, DepotConstants.TYPE_SPACE,
			GroupConstants.ANY_PARENT_GROUP_ID);
	}

	@Test
	public void testUpgradeWithUnpublishedAssetTagGroupRel() throws Exception {
		_ctCollection = _ctCollectionLocalService.addCTCollection(
			null, TestPropsValues.getCompanyId(), TestPropsValues.getUserId(),
			0, RandomTestUtil.randomString(), RandomTestUtil.randomString());

		_depotEntry = _addDepotEntry(DepotConstants.TYPE_PROJECT);

		AssetTagGroupRel assetTagGroupRel;

		try (SafeCloseable safeCloseable =
				CTCollectionThreadLocal.setCTCollectionIdWithSafeCloseable(
					_ctCollection.getCtCollectionId())) {

			assetTagGroupRel =
				_assetTagGroupRelLocalService.addAssetTagGroupRel(
					_depotEntry.getGroupId(), _cmsAssetTag.getTagId(),
					DepotConstants.TYPE_PROJECT);
		}

		_runUpgrade();

		_assertAssetTagGroupRel(
			_cmsAssetTag, DepotConstants.TYPE_PROJECT,
			GroupConstants.ANY_PARENT_GROUP_ID);

		try (SafeCloseable safeCloseable =
				CTCollectionThreadLocal.setCTCollectionIdWithSafeCloseable(
					_ctCollection.getCtCollectionId())) {

			assetTagGroupRel =
				_assetTagGroupRelLocalService.fetchAssetTagGroupRel(
					assetTagGroupRel.getAssetTagGroupRelId());

			Assert.assertEquals(
				assetTagGroupRel.toString(), _depotEntry.getGroupId(),
				assetTagGroupRel.getGroupId());
		}
	}

	@Test
	public void testUpgradeWithUnpublishedGroup() throws Exception {
		_ctCollection = _ctCollectionLocalService.addCTCollection(
			null, TestPropsValues.getCompanyId(), TestPropsValues.getUserId(),
			0, RandomTestUtil.randomString(), RandomTestUtil.randomString());

		String description = RandomTestUtil.randomString();

		try (SafeCloseable safeCloseable =
				CTCollectionThreadLocal.setCTCollectionIdWithSafeCloseable(
					_ctCollection.getCtCollectionId())) {

			_cmsGroup.setDescription(description);

			_cmsGroup = _groupLocalService.updateGroup(_cmsGroup);
		}

		_runUpgrade();

		_assertAssetTagGroupRel(
			_cmsAssetTag, DepotConstants.TYPE_PROJECT,
			GroupConstants.ANY_PARENT_GROUP_ID);

		try (SafeCloseable safeCloseable =
				CTCollectionThreadLocal.setCTCollectionIdWithSafeCloseable(
					_ctCollection.getCtCollectionId())) {

			Group group = _groupLocalService.getGroup(_cmsGroup.getGroupId());

			Assert.assertEquals(description, group.getDescription());
		}
	}

	private DepotEntry _addDepotEntry(int depotEntryType) throws Exception {
		return _depotEntryLocalService.addDepotEntry(
			RandomTestUtil.randomLocaleStringMap(),
			RandomTestUtil.randomLocaleStringMap(), depotEntryType,
			ServiceContextTestUtil.getServiceContext());
	}

	private void _assertAssetTagGroupRel(
		AssetTag assetTag, int depotEntryType, long expectedGroupId) {

		List<AssetTagGroupRel> assetTagGroupRels =
			_assetTagGroupRelLocalService.
				getAssetTagGroupRelsByTagIdAndDepotEntryType(
					assetTag.getTagId(), depotEntryType);

		Assert.assertEquals(
			assetTagGroupRels.toString(), 1, assetTagGroupRels.size());

		AssetTagGroupRel assetTagGroupRel = assetTagGroupRels.get(0);

		Assert.assertEquals(
			assetTagGroupRel.toString(), expectedGroupId,
			assetTagGroupRel.getGroupId());
	}

	private void _runUpgrade() throws Exception {
		UpgradeProcess upgradeProcess = new AssetTagGroupRelUpgradeProcess();

		upgradeProcess.upgrade();

		CacheRegistryUtil.clear();

		_entityCache.clearCache(AssetTagGroupRelImpl.class);

		_finderCache.clearCache(AssetTagGroupRelImpl.class);
	}

	@Inject
	private AssetTagGroupRelLocalService _assetTagGroupRelLocalService;

	@DeleteAfterTestRun
	private AssetTag _cmsAssetTag;

	private Group _cmsGroup;

	@DeleteAfterTestRun
	private CTCollection _ctCollection;

	@Inject
	private CTCollectionLocalService _ctCollectionLocalService;

	@DeleteAfterTestRun
	private DepotEntry _depotEntry;

	@Inject
	private DepotEntryLocalService _depotEntryLocalService;

	@Inject
	private EntityCache _entityCache;

	@Inject
	private FinderCache _finderCache;

	@Inject
	private GroupLocalService _groupLocalService;

}