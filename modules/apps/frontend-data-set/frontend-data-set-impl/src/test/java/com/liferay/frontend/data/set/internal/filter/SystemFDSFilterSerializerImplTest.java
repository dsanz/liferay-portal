/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal.filter;

import com.liferay.frontend.data.set.SystemFDSEntry;
import com.liferay.frontend.data.set.action.FDSItemActionList;
import com.liferay.frontend.data.set.filter.FDSFilter;
import com.liferay.frontend.data.set.filter.FDSFilterContextContributorRegistry;
import com.liferay.frontend.data.set.internal.BaseSystemFDSSerializerTestCase;
import com.liferay.frontend.data.set.internal.action.SystemFDSFilterSerializerImpl;
import com.liferay.frontend.data.set.model.FDSActionDropdownItem;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerCustomizerFactory;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMapFactory;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

		_filterServiceTrackerMap =
			ServiceTrackerMapFactory.openMultiValueMap(
						bundleContext, FDSFilter.class, "frontend.data.set.name",
						ServiceTrackerCustomizerFactory.<FDSFilter>serviceWrapper(
							bundleContext));

		ReflectionTestUtil.setFieldValue(
			_fdsFilterRegistryImpl, "_serviceTrackerMap",
			_filterServiceTrackerMap);

		ReflectionTestUtil.setFieldValue(
			_systemFDSFilterSerializerImpl,
			"_fdsFilterRegistry", _fdsFilterRegistryImpl);

		ReflectionTestUtil.setFieldValue(
			_systemFDSFilterSerializerImpl,
			"_fdsFilterContextContributorRegistry", _fdsFilterContextContributorRegistryImpl);
	}

	@After
	public void tearDown() {
		super.tearDown();

		_filterServiceTrackerMap.close();
	}

	@Test
	public void testFDSCreationMenuSerialization() throws Exception {
		ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration =
			registerSystemFDSEntry("fdsName", "/app", "/endpoint", "schema");

		List<FDSActionDropdownItem> dropDownItemList = ListUtil.fromArray(
			new FDSActionDropdownItem(
				null, "trash", "delete", "delete", "delete", "delete",
				"headless"));

		ServiceRegistration<FDSItemActionList>
			itemActionListServiceRegistration = _registerFilter(
				"fdsName", dropDownItemList);

		Assert.assertEquals(
			dropDownItemList,
			_systemFDSFilterSerializerImpl.serialize(
				"fdsName", httpServletRequest));

		itemActionListServiceRegistration.unregister();

		systemFDSEntryServiceRegistration.unregister();
	}

	@Test
	public void testFDSCreationMenuSerializationNoCreationMenu()
		throws Exception {

		ServiceRegistration<SystemFDSEntry> systemFDSEntryServiceRegistration =
			registerSystemFDSEntry("fdsName", "/app", "/endpoint", "schema");

		Assert.assertTrue(
			_systemFDSFilterSerializerImpl.serialize(
				"fdsName", httpServletRequest
			).isEmpty());

		systemFDSEntryServiceRegistration.unregister();
	}

	@Test
	public void testFDSCreationMenuSerializationSeparateCreationMenus()
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
	public void testFDSCreationMenuSerializationSharingCreationMenu()
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
	}

	private ServiceRegistration<FDSFilter> _registerFilter(
		String fdsName, String id, String label, String type, String entityFieldType) {

		return bundleContext.registerService(
			FDSFilter.class,
			new FDSFilter() {

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
				public String getType() {
					return type;
				}
			},
			MapUtil.singletonDictionary("frontend.data.set.name", fdsName));
	}

	private static final FDSFilterRegistryImpl
		_fdsFilterRegistryImpl = new FDSFilterRegistryImpl();

	private FDSFilterContextContributorRegistryImpl
		_fdsFilterContextContributorRegistryImpl = new FDSFilterContextContributorRegistryImpl();

	private static ServiceTrackerMap<String, List<ServiceTrackerCustomizerFactory.ServiceWrapper<FDSFilter>>>
		_filterServiceTrackerMap;
	private static final SystemFDSFilterSerializerImpl
		_systemFDSFilterSerializerImpl =
			new SystemFDSFilterSerializerImpl();

}