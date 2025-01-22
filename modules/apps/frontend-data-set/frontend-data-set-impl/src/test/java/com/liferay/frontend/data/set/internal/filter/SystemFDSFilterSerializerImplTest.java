/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal.filter;

import com.liferay.frontend.data.set.SystemFDSEntry;
import com.liferay.frontend.data.set.action.FDSItemActionList;
import com.liferay.frontend.data.set.constants.FDSEntityFieldTypes;
import com.liferay.frontend.data.set.filter.BaseDateRangeFDSFilter;
import com.liferay.frontend.data.set.filter.BaseSelectionFDSFilter;
import com.liferay.frontend.data.set.filter.DateFDSFilterItem;
import com.liferay.frontend.data.set.filter.FDSFilter;
import com.liferay.frontend.data.set.filter.FDSFilterContextContributor;
import com.liferay.frontend.data.set.filter.FDSFilterContextContributorRegistry;
import com.liferay.frontend.data.set.filter.FDSFilterRegistry;
import com.liferay.frontend.data.set.filter.SelectionFDSFilterItem;
import com.liferay.frontend.data.set.internal.BaseSystemFDSSerializerTestCase;
import com.liferay.frontend.data.set.internal.filter.SystemFDSFilterSerializerImpl;
import com.liferay.frontend.data.set.model.FDSActionDropdownItem;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerCustomizerFactory;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMapFactory;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.osgi.framework.ServiceRegistration;

import org.osgi.service.component.annotations.Reference;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * @author Daniel Sanz
 */
public class SystemFDSFilterSerializerImplTest
	extends BaseSystemFDSSerializerTestCase {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		_filterServiceTrackerMap = ServiceTrackerMapFactory.openMultiValueMap(
			bundleContext, FDSFilter.class, "frontend.data.set.name",
			ServiceTrackerCustomizerFactory.<FDSFilter>serviceWrapper(
				bundleContext));

		// registries

		ReflectionTestUtil.setFieldValue(
			_fdsFilterRegistryImpl, "_serviceTrackerMap",
			_filterServiceTrackerMap);

		ReflectionTestUtil.setFieldValue(
			_fdsFilterContextContributorRegistryImpl, "_serviceTrackerMap",
			_filterContextContributorServiceTrackerMap);




		ReflectionTestUtil.setFieldValue(
			_systemFDSFilterSerializerImpl, "_fdsFilterRegistry",
			_fdsFilterRegistryImpl);

		ReflectionTestUtil.setFieldValue(
			_systemFDSFilterSerializerImpl, "_jsonFactory",
			_jsonFactory);

		ReflectionTestUtil.setFieldValue(
		_systemFDSFilterSerializerImpl, "_language",
			_language);

		ReflectionTestUtil.setFieldValue(
		_systemFDSFilterSerializerImpl, "_portal",
			_portal);

		ReflectionTestUtil.setFieldValue(
			_systemFDSFilterSerializerImpl,
			"_fdsFilterContextContributorRegistry",
			_fdsFilterContextContributorRegistryImpl);


		ReflectionTestUtil.setFieldValue(
			_dateRangeFDSFilterContextContributor, "_jsonFactory",
			_jsonFactory);

		ReflectionTestUtil.setFieldValue(
			_selectionFDSFilterContextContributor, "_jsonFactory",
			_jsonFactory);

		ReflectionTestUtil.setFieldValue(
					_selectionFDSFilterContextContributor, "_language",
					_language);

		_dateRangeFDSFilterContextContributorServiceRegistration =
			bundleContext.registerService(
				FDSFilterContextContributor.class,
				_dateRangeFDSFilterContextContributor,
				MapUtil.singletonDictionary(
					"frontend.data.set.filter.type", "dateRange"));

		_selectionFDSFilterContextContributorServiceRegistration =
			bundleContext.registerService(
				FDSFilterContextContributor.class,
				_selectionFDSFilterContextContributor,
				MapUtil.singletonDictionary(
					"frontend.data.set.filter.type", "selection"));
	}

	@After
	public void tearDown() {
		super.tearDown();

		_selectionFDSFilterContextContributorServiceRegistration.unregister();
		_dateRangeFDSFilterContextContributorServiceRegistration.unregister();

		_filterServiceTrackerMap.close();
	}

	/*
	@Test
	public void testFDSFilterSerializationNoCreationMenu()
		throws Exception {

		ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration =
			registerSystemFDSEntry("fdsName", "/app", "/endpoint", "schema");

		Assert.assertTrue(
			_systemFDSFilterSerializerImpl.serialize(
				"fdsName", httpServletRequest
			).isNull(
				1
			));

		systemFDSEntryServiceRegistration.unregister();
	}

	@Test
	public void testFDSFilterSerializationSeparateFilters()
		throws Exception {

		ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration1 =
			registerSystemFDSEntry("fdsName1", "/app", "/endpoint", "schema");

		ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration2 =
			registerSystemFDSEntry("fdsName2", "/app", "/endpoint", "schema");

		List<FDSActionDropdownItem> dropDownItemList1 = ListUtil.fromArray(
			new FDSActionDropdownItem(
				null, "trash", "delete", "delete", "delete", "delete",
				"headless"));

		ServiceRegistration<FDSItemActionList>
			itemActionListServiceRegistration1 = _registerFilter(
				"fdsName1", dropDownItemList1);

		List<FDSActionDropdownItem> dropDownItemList2 = ListUtil.fromArray(
			new FDSActionDropdownItem(
				null, "cog", "permissions", "permissions", "get", "permissions",
				"modal-permissions"));

		ServiceRegistration<FDSItemActionList>
			itemActionListServiceRegistration2 = _registerFilter(
				"fdsName2", dropDownItemList2);

		Assert.assertNotEquals(
			_systemFDSFilterSerializerImpl.serialize(
				"fdsName1", httpServletRequest),
			_systemFDSFilterSerializerImpl.serialize(
				"fdsName2", httpServletRequest));

		Assert.assertEquals(
			dropDownItemList1,
			_systemFDSFilterSerializerImpl.serialize(
				"fdsName1", httpServletRequest));

		Assert.assertEquals(
			dropDownItemList2,
			_systemFDSFilterSerializerImpl.serialize(
				"fdsName2", httpServletRequest));

		itemActionListServiceRegistration1.unregister();

		itemActionListServiceRegistration2.unregister();

		systemFDSEntryServiceRegistration1.unregister();

		systemFDSEntryServiceRegistration2.unregister();
	}

	@Test
	public void testFDSFilterSerializationSharingFilter()
		throws Exception {

		ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration1 =
			registerSystemFDSEntry("fdsName1", "/app", "/endpoint", "schema");

		ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration2 =
			registerSystemFDSEntry("fdsName2", "/app", "/endpoint", "schema");

		List<FDSActionDropdownItem> dropDownItemList = ListUtil.fromArray(
			new FDSActionDropdownItem(
				null, "trash", "delete", "delete", "delete", "delete",
				"headless"));

		ServiceRegistration<FDSItemActionList>
			itemActionListServiceRegistration1 = _registerFilter(
				"fdsName1", dropDownItemList);

		ServiceRegistration<FDSItemActionList>
			itemActionListServiceRegistration2 = _registerFilter(
				"fdsName2", dropDownItemList);

		Assert.assertEquals(
			_systemFDSFilterSerializerImpl.serialize(
				"fdsName1", httpServletRequest),
			_systemFDSFilterSerializerImpl.serialize(
				"fdsName2", httpServletRequest));

		itemActionListServiceRegistration1.unregister();

		itemActionListServiceRegistration2.unregister();

		systemFDSEntryServiceRegistration1.unregister();

		systemFDSEntryServiceRegistration2.unregister();
	} */

	@Test
	public void testFDSDateRangeFilterSerialization() throws Exception {
		ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration =
			registerSystemFDSEntry("fdsName", "/app", "/endpoint", "schema");

		ServiceRegistration<FDSFilter> dateRangeFilterServiceRegistration =
			_registerDateRangeFilter(
				"fdsName", "createDate", "By Creation Date",
				FDSEntityFieldTypes.DATE, null, new DateFDSFilterItem(0, 0, 0),
				new DateFDSFilterItem(16, 3, 1977));

		JSONAssert.assertEquals(
			JSONUtil.putAll(
			).toString(),
			_systemFDSFilterSerializerImpl.serialize(
				"fdsName", httpServletRequest
			).toString(),
			JSONCompareMode.LENIENT);

		dateRangeFilterServiceRegistration.unregister();

		systemFDSEntryServiceRegistration.unregister();
	}

	private ServiceRegistration<FDSFilter> _registerDateRangeFilter(
		String fdsName, String id, String label, String entityFieldType,
		Map<String, Object> preloadedData, DateFDSFilterItem min,
		DateFDSFilterItem max) {

		return _registerFilter(
			fdsName,
			new BaseDateRangeFDSFilter() {

				@Override
				public String getEntityFieldType() {
					return entityFieldType;
				}

				@Override
				public String getId() {
					return id;
				}

				@Override
				public String getLabel() {
					return label;
				}

				@Override
				public DateFDSFilterItem getMaxDateFDSFilterItem() {
					return max;
				}

				@Override
				public DateFDSFilterItem getMinDateFDSFilterItem() {
					return min;
				}

				@Override
				public Map<String, Object> getPreloadedData() {
					return preloadedData;
				}

			});
	}

	private ServiceRegistration<FDSFilter> _registerFilter(
		String fdsName, FDSFilter fdsFilter) {

		return bundleContext.registerService(
			FDSFilter.class, fdsFilter,
			MapUtil.singletonDictionary("frontend.data.set.name", fdsName));
	}

	private ServiceRegistration<FDSFilter> _registerSelectionFilter(
		String fdsName, String id, String label, String entityFieldType,
		Map<String, Object> preloadedData,
		List<SelectionFDSFilterItem> selectionFDSFilterItems, String itemKey,
		String itemLabel, boolean autocompleteEnabled, boolean multiple) {

		return _registerFilter(
			fdsName,
			new BaseSelectionFDSFilter() {

				@Override
				public String getEntityFieldType() {
					return entityFieldType;
				}

				@Override
				public String getId() {
					return id;
				}

				@Override
				public String getItemKey() {
					return itemKey;
				}

				@Override
				public String getItemLabel() {
					return itemLabel;
				}

				@Override
				public String getLabel() {
					return label;
				}

				@Override
				public Map<String, Object> getPreloadedData() {
					return preloadedData;
				}

				@Override
				public List<SelectionFDSFilterItem> getSelectionFDSFilterItems(
					Locale locale) {

					return selectionFDSFilterItems;
				}

				@Override
				public boolean isAutocompleteEnabled() {
					return autocompleteEnabled;
				}

				@Override
				public boolean isMultiple() {
					return multiple;
				}

			});
	}

	private ServiceRegistration<FDSFilter> _registerSelectionFilter(
		String fdsName, String id, String label, String entityFieldType,
		Map<String, Object> preloadedData, String apiURL, String itemKey,
		String itemLabel, boolean autocompleteEnabled, boolean multiple) {

		return _registerFilter(
			fdsName,
			new BaseSelectionFDSFilter() {

				@Override
				public String getAPIURL() {
					return apiURL;
				}

				@Override
				public String getEntityFieldType() {
					return entityFieldType;
				}

				@Override
				public String getId() {
					return id;
				}

				@Override
				public String getItemKey() {
					return itemKey;
				}

				@Override
				public String getItemLabel() {
					return itemLabel;
				}

				@Override
				public String getLabel() {
					return label;
				}

				@Override
				public Map<String, Object> getPreloadedData() {
					return preloadedData;
				}

				@Override
				public boolean isAutocompleteEnabled() {
					return autocompleteEnabled;
				}

				@Override
				public boolean isMultiple() {
					return multiple;
				}

			});
	}

	private static final DateRangeFDSFilterContextContributor
		_dateRangeFDSFilterContextContributor =
			new DateRangeFDSFilterContextContributor();
	private static final SelectionFDSFilterContextContributor
		_selectionFDSFilterContextContributor =
			new SelectionFDSFilterContextContributor();

	private static final FDSFilterRegistryImpl _fdsFilterRegistryImpl =
		new FDSFilterRegistryImpl();
	private static ServiceTrackerMap
		<String,
		 List<ServiceTrackerCustomizerFactory.ServiceWrapper<FDSFilter>>>
			_filterServiceTrackerMap;
	private static final SystemFDSFilterSerializerImpl
		_systemFDSFilterSerializerImpl = new SystemFDSFilterSerializerImpl();

	private ServiceRegistration<FDSFilterContextContributor>
		_dateRangeFDSFilterContextContributorServiceRegistration;
	private ServiceRegistration<FDSFilterContextContributor>
		_selectionFDSFilterContextContributorServiceRegistration;

	private final FDSFilterContextContributorRegistryImpl
		_fdsFilterContextContributorRegistryImpl =
			new FDSFilterContextContributorRegistryImpl();
	private ServiceTrackerMap
		<String,
		 List
			 <ServiceTrackerCustomizerFactory.ServiceWrapper
				 <FDSFilterContextContributor>>>
					_filterContextContributorServiceTrackerMap;

	@Inject
	private JSONFactory _jsonFactory;

	@Inject
	private Language _language;

	@Inject
	private Portal _portal;

}