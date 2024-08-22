/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.admin.model.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.frontend.data.set.admin.model.FDSEntryCreator;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.test.util.CompanyTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Daniel Sanz
 */
@RunWith(Arquillian.class)
public class FDSAdminModelManagerTest {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		Bundle bundle = FrameworkUtil.getBundle(FDSAdminModelManagerTest.class);

		_bundleContext = bundle.getBundleContext();
	}

	@Test
	public void testDataIsCreatedForNewCompanies() throws Exception {
		FDSEntryCreator fdsEntryCreator = new FDSSimpleEntryCreator(
			"SIMPLE_DATASET", _objectEntryLocalService,
			_objectDefinitionLocalService);

		ServiceRegistration<FDSEntryCreator> serviceRegistration =
			_bundleContext.registerService(
				FDSEntryCreator.class, fdsEntryCreator, null);

		Assert.assertTrue(
			"FDS model is created in the default company",
			_mainModelObjectEntryExists(
				PortalUtil.getDefaultCompanyId(),
				fdsEntryCreator.getFDSEntryERC()));

		Company company = CompanyTestUtil.addCompany(true);

		Assert.assertTrue(
			"FDS model is created in the new company",
			_mainModelObjectEntryExists(
				company.getCompanyId(), fdsEntryCreator.getFDSEntryERC()));

		_companyLocalService.deleteCompany(company);

		serviceRegistration.unregister();
	}

	@Test
	public void testDataIsCreatedForTheDefaultCompany() throws Exception {
		FDSEntryCreator fdsEntryCreator = new FDSSimpleEntryCreator(
			"SIMPLE_DATASET", _objectEntryLocalService,
			_objectDefinitionLocalService);

		ServiceRegistration<FDSEntryCreator> serviceRegistration =
			_bundleContext.registerService(
				FDSEntryCreator.class, fdsEntryCreator, null);

		Assert.assertTrue(
			"FDS model is created in the default company",
			_mainModelObjectEntryExists(
				PortalUtil.getDefaultCompanyId(),
				fdsEntryCreator.getFDSEntryERC()));

		serviceRegistration.unregister();
	}

	@Test
	public void testModelIsCreatedForNewCompanies() throws Exception {
		Company company = CompanyTestUtil.addCompany(true);

		Assert.assertNotNull(
			_objectDefinitionLocalService.fetchObjectDefinition(
				company.getCompanyId(), "FDSView"));
		_companyLocalService.deleteCompany(company);
	}

	@Test
	public void testModelIsCreatedForTheDefaultCompany() throws Exception {
		Assert.assertNotNull(
			_objectDefinitionLocalService.fetchObjectDefinition(
				PortalUtil.getDefaultCompanyId(), "FDSView"));
	}

	@Test
	public void testNewDataIsCreatedForAllCompanies() throws Exception {
		List<Company> companies = new ArrayList<>();

		Map<String, FDSEntryCreator> fdsEntryCreators =
			HashMapBuilder.<String, FDSEntryCreator>put(
				"FDS_1",
				new FDSSimpleEntryCreator(
					"FDS_1", _objectEntryLocalService,
					_objectDefinitionLocalService)
			).put(
				"FDS_2",
				new FDSSimpleEntryCreator(
					"FDS_2", _objectEntryLocalService,
					_objectDefinitionLocalService)
			).build();

		List<ServiceRegistration<FDSEntryCreator>> serviceRegistrations =
			new ArrayList<>();

		companies.add(CompanyTestUtil.addCompany(true));

		serviceRegistrations.add(
			_bundleContext.registerService(
				FDSEntryCreator.class, fdsEntryCreators.get("FDS_1"), null));

		companies.add(CompanyTestUtil.addCompany(true));

		serviceRegistrations.add(
			_bundleContext.registerService(
				FDSEntryCreator.class, fdsEntryCreators.get("FDS_2"), null));

		companies.add(CompanyTestUtil.addCompany(true));

		_companyLocalService.forEachCompany(
			company -> {
				for (FDSEntryCreator fdsEntryCreator :
						fdsEntryCreators.values()) {

					Assert.assertTrue(
						StringBundler.concat(
							"FDS entry ", fdsEntryCreator.getFDSEntryERC(),
							" is created in company ", company.getCompanyId()),
						_mainModelObjectEntryExists(
							company.getCompanyId(),
							fdsEntryCreator.getFDSEntryERC()));
				}
			});

		_companyLocalService.forEachCompany(
			company -> {
				if (companies.contains(company)) {
					_companyLocalService.deleteCompany(company);
				}
			});

		for (ServiceRegistration<FDSEntryCreator> serviceRegistration :
				serviceRegistrations) {

			serviceRegistration.unregister();
		}
	}

	private boolean _mainModelObjectEntryExists(
		long companyId, String externalReferenceCode) {

		ObjectDefinition objectDefinition =
			_objectDefinitionLocalService.fetchObjectDefinition(
				companyId, "FDSView");

		ObjectEntry objectEntry = _objectEntryLocalService.fetchObjectEntry(
			externalReferenceCode, objectDefinition.getObjectDefinitionId());

		if (objectEntry != null) {
			return true;
		}

		return false;
	}

	private static BundleContext _bundleContext;

	@Inject
	private CompanyLocalService _companyLocalService;

	@Inject
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Inject
	private ObjectEntryLocalService _objectEntryLocalService;

	private class FDSSimpleEntryCreator implements FDSEntryCreator {

		public FDSSimpleEntryCreator(
			String fdsEntryERC, ObjectEntryLocalService objectEntryLocalService,
			ObjectDefinitionLocalService objectDefinitionLocalService) {

			_fdsEntryERC = fdsEntryERC;
			_objectEntryLocalService = objectEntryLocalService;
			_objectDefinitionLocalService = objectDefinitionLocalService;
		}

		@Override
		public void create(long companyId, long userId) {
			ObjectDefinition objectDefinition =
				_objectDefinitionLocalService.fetchObjectDefinition(
					companyId, "FDSView");

			try {
				_objectEntryLocalService.addObjectEntry(
					getFDSEntryERC(), userId, objectDefinition);
			}
			catch (PortalException portalException) {
				ReflectionUtil.throwException(portalException);
			}
		}

		@Override
		public String getFDSEntryERC() {
			return _fdsEntryERC;
		}

		private final String _fdsEntryERC;
		private final ObjectDefinitionLocalService
			_objectDefinitionLocalService;
		private final ObjectEntryLocalService _objectEntryLocalService;

	}

}