/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal.renderer;

import com.liferay.frontend.data.set.constants.FDSTypes;
import com.liferay.frontend.data.set.renderer.FDSEntryPropsProvider;
import com.liferay.frontend.data.set.renderer.FDSEntryPropsProviderRegistry;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMapFactory;

import javax.servlet.http.HttpServletRequest;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * @author Daniel Sanz
 */
@Component(service = FDSEntryPropsProviderRegistry.class)
public class FDSEntryPropsProviderRegistryImpl
	implements FDSEntryPropsProviderRegistry {

	@Override
	public FDSEntryPropsProvider getFDSEntryPropsProvider(
		String fdsName, HttpServletRequest httpServletRequest) {

		return _firstAvailable(
			fdsName, httpServletRequest, FDSTypes.CUSTOM, FDSTypes.SYSTEM);
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_bundleContext = bundleContext;

		_serviceTrackerMap = ServiceTrackerMapFactory.openSingleValueMap(
			bundleContext, FDSEntryPropsProvider.class, "dataset.type");
	}

	@Deactivate
	protected void deactivate() {
		_serviceTrackerMap.close();
	}

	private FDSEntryPropsProvider _firstAvailable(
		String fdsName, HttpServletRequest httpServletRequest,
		String... dataSetTypes) {

		for (String dataSetType : dataSetTypes) {
			FDSEntryPropsProvider fdsEntryPropsProvider =
				_serviceTrackerMap.getService(dataSetType);

			if ((fdsEntryPropsProvider != null) &&
				fdsEntryPropsProvider.isAvailable(
					fdsName, httpServletRequest)) {

				return fdsEntryPropsProvider;
			}
		}

		return null;
	}

	private BundleContext _bundleContext;
	private ServiceTrackerMap<String, FDSEntryPropsProvider> _serviceTrackerMap;

}