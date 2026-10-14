/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.asset.publisher.web.internal.display.context;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.ClassType;
import com.liferay.asset.kernel.model.ClassTypeReader;
import com.liferay.asset.list.model.AssetListEntry;
import com.liferay.asset.publisher.util.AssetPublisherHelper;
import com.liferay.asset.publisher.web.internal.configuration.AssetPublisherPortletInstanceConfiguration;
import com.liferay.asset.publisher.web.internal.model.TestClassType;
import com.liferay.asset.publisher.web.internal.util.DefaultAssetPublisherCustomizer;
import com.liferay.asset.util.AssetHelper;
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.portal.configuration.module.configuration.ConfigurationProviderUtil;
import com.liferay.portal.kernel.dao.search.SearchContainer;
import com.liferay.portal.kernel.module.configuration.ConfigurationException;
import com.liferay.portal.kernel.module.util.SystemBundleUtil;
import com.liferay.portal.kernel.portlet.LiferayPortletRequest;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.upgrade.MockPortletPreferences;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import jakarta.portlet.PortletURL;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * @author Eudaldo Alonso
 */
public class AssetPublisherDisplayContextTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@BeforeClass
	public static void setUpClass() {
		_configurationProviderUtilMockedStatic = Mockito.mockStatic(
			ConfigurationProviderUtil.class);
	}

	@AfterClass
	public static void tearDownClass() {
		_configurationProviderUtilMockedStatic.close();
		_frameworkUtilMockedStatic.close();
	}

	@Before
	public void setUp() throws ConfigurationException {
		_setUpConfigurationProviderUtil();
		_setUpFrameworkUtil();
		_setUpPortalUtil();

		_assetPublisherDisplayContext = new AssetPublisherDisplayContext(
			null, null, null, null, _assetPublisherHelper, null, null, null,
			null, _portal, _getLiferayPortletRequest(), null,
			new MockPortletPreferences(), null, null);
	}

	@Test
	public void testGetAssetLinkBehavior() throws Exception {
		Assert.assertEquals(
			"viewInPortlet",
			_assetPublisherDisplayContext.getAssetLinkBehavior());
	}

	@Test
	public void testGetClassTypes() throws Exception {
		ClassTypeReader classTypeReader = Mockito.mock(ClassTypeReader.class);

		List<ClassType> classTypes = ListUtil.fromArray(
			new TestClassType(RandomTestUtil.randomLong(), "Map"),
			new TestClassType(RandomTestUtil.randomLong(), "Banner"),
			new TestClassType(RandomTestUtil.randomLong(), "Accordion"));

		Mockito.when(
			classTypeReader.getAvailableClassTypes(null, LocaleUtil.US)
		).thenReturn(
			classTypes
		);

		long[] expectedClassTypeIds = TransformUtil.transformToLongArray(
			classTypes, classType -> classType.getClassTypeId());

		ArrayUtil.reverse(expectedClassTypeIds);

		Assert.assertArrayEquals(
			expectedClassTypeIds,
			TransformUtil.transformToLongArray(
				_assetPublisherDisplayContext.getClassTypes(classTypeReader),
				classType -> classType.getClassTypeId()));
	}

	@Test
	public void testGetScopeAssetPublisherAddItemHolders() throws Exception {
		AssetHelper assetHelper = Mockito.mock(AssetHelper.class);

		AssetListEntry assetListEntry = Mockito.mock(AssetListEntry.class);

		Mockito.when(
			assetListEntry.getAssetEntrySubtype()
		).thenReturn(
			"0,3"
		);

		long[] classTypeIds = {1L, 2L};
		long[] expectedClassTypeIds = {1L, 2L, 0L, 3L};

		AssetPublisherDisplayContext assetPublisherDisplayContext =
			new AssetPublisherDisplayContext(
				assetHelper, null, null, null, _assetPublisherHelper, null,
				null, null, null, _portal, _getLiferayPortletRequest(), null,
				new MockPortletPreferences(), null, null) {

				@Override
				public AssetListEntry fetchAssetListEntry() {
					return assetListEntry;
				}

				@Override
				public long[] getAllAssetCategoryIds() {
					return new long[0];
				}

				@Override
				public String[] getAllAssetTagNames() {
					return new String[0];
				}

				@Override
				public long[] getClassNameIds() {
					return new long[0];
				}

				@Override
				public long[] getClassTypeIds() {
					return classTypeIds;
				}

				@Override
				public long[] getGroupIds() {
					return new long[] {RandomTestUtil.randomLong()};
				}

			};

		assetPublisherDisplayContext.getScopeAssetPublisherAddItemHolders(1);

		Mockito.verify(
			assetHelper
		).getAssetPublisherAddItemHolders(
			Mockito.nullable(LiferayPortletRequest.class),
			Mockito.nullable(LiferayPortletResponse.class), Mockito.anyLong(),
			Mockito.any(long[].class),
			Mockito.argThat(
				actualClassTypeIds -> Arrays.equals(
					expectedClassTypeIds, actualClassTypeIds)),
			Mockito.any(long[].class), Mockito.any(String[].class),
			Mockito.nullable(String.class)
		);
	}

	@Test
	public void testGetSearchContainer() throws Exception {
		_testGetSearchContainer(
			5, 0, AssetPublisherDisplayContext.PAGINATION_TYPE_NONE);
		_testGetSearchContainer(
			10, 5, AssetPublisherDisplayContext.PAGINATION_TYPE_REGULAR);
		_testGetSearchContainer(
			10, 5, AssetPublisherDisplayContext.PAGINATION_TYPE_SIMPLE);
	}

	private LiferayPortletRequest _getLiferayPortletRequest() {
		LiferayPortletRequest liferayPortletRequest = Mockito.mock(
			LiferayPortletRequest.class);

		ThemeDisplay themeDisplay = Mockito.mock(ThemeDisplay.class);

		Mockito.when(
			themeDisplay.getLocale()
		).thenReturn(
			LocaleUtil.US
		);

		Mockito.when(
			liferayPortletRequest.getAttribute(WebKeys.THEME_DISPLAY)
		).thenReturn(
			themeDisplay
		);

		return liferayPortletRequest;
	}

	private void _setUpConfigurationProviderUtil() {
		_configurationProviderUtilMockedStatic.when(
			() -> ConfigurationProviderUtil.getPortletInstanceConfiguration(
				Mockito.any(), Mockito.any())
		).thenReturn(
			Mockito.mock(AssetPublisherPortletInstanceConfiguration.class)
		);
	}

	private void _setUpFrameworkUtil() {
		BundleContext bundleContext = SystemBundleUtil.getBundleContext();

		_frameworkUtilMockedStatic.when(
			() -> FrameworkUtil.getBundle(Mockito.any())
		).thenReturn(
			bundleContext.getBundle()
		);
	}

	private void _setUpPortalUtil() {
		PortalUtil portalUtil = new PortalUtil();

		portalUtil.setPortal(_portal);
	}

	private void _testGetSearchContainer(
			int expectedEnd, int expectedStart, String paginationType)
		throws Exception {

		LiferayPortletRequest liferayPortletRequest =
			_getLiferayPortletRequest();

		Mockito.when(
			liferayPortletRequest.getParameter(
				SearchContainer.DEFAULT_CUR_PARAM)
		).thenReturn(
			"2"
		);

		Mockito.when(
			liferayPortletRequest.getParameter(
				SearchContainer.DEFAULT_DELTA_PARAM)
		).thenReturn(
			"20"
		);

		LiferayPortletResponse liferayPortletResponse = Mockito.mock(
			LiferayPortletResponse.class);

		Mockito.when(
			liferayPortletResponse.createRenderURL()
		).thenReturn(
			Mockito.mock(PortletURL.class)
		);

		Mockito.when(
			_portal.getHttpServletRequest(liferayPortletRequest)
		).thenReturn(
			Mockito.mock(HttpServletRequest.class)
		);

		Mockito.when(
			_portal.getLiferayPortletResponse(Mockito.any())
		).thenReturn(
			liferayPortletResponse
		);

		MockPortletPreferences mockPortletPreferences =
			new MockPortletPreferences();

		mockPortletPreferences.setValue("delta", "5");
		mockPortletPreferences.setValue("paginationType", paginationType);

		AssetPublisherDisplayContext assetPublisherDisplayContext =
			new AssetPublisherDisplayContext(
				null, null, null,
				new DefaultAssetPublisherCustomizer(null, null),
				_assetPublisherHelper, null, null, null, null, _portal,
				liferayPortletRequest, null, mockPortletPreferences, null,
				null);

		SearchContainer<AssetEntry> searchContainer =
			assetPublisherDisplayContext.getSearchContainer();

		Assert.assertEquals(5, searchContainer.getDelta());
		Assert.assertEquals(expectedEnd, searchContainer.getEnd());
		Assert.assertEquals(expectedStart, searchContainer.getStart());
	}

	private static MockedStatic<ConfigurationProviderUtil>
		_configurationProviderUtilMockedStatic;
	private static final MockedStatic<FrameworkUtil>
		_frameworkUtilMockedStatic = Mockito.mockStatic(FrameworkUtil.class);

	private AssetPublisherDisplayContext _assetPublisherDisplayContext;
	private final AssetPublisherHelper _assetPublisherHelper = Mockito.mock(
		AssetPublisherHelper.class);
	private final Portal _portal = Mockito.mock(Portal.class);

}