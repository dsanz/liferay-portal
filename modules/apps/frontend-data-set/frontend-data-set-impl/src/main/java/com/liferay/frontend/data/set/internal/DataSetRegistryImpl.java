/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal;

import com.liferay.frontend.data.set.DataSet;
import com.liferay.frontend.data.set.DataSetRegistry;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerCustomizerFactory;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMapFactory;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Daniel Sanz
 */
@Component(service = DataSetRegistry.class)
public class DataSetRegistryImpl implements DataSetRegistry {

	@Override
	public DataSet getDataSet(String fdsName) {
		ServiceTrackerCustomizerFactory.ServiceWrapper<DataSet> serviceWrapper =
			_serviceTrackerMap.getService(fdsName);

		if (serviceWrapper == null) {
			if (_log.isDebugEnabled()) {
				_log.debug("No data set is associated with " + fdsName);
			}

			return null;
		}

		return serviceWrapper.getService();
	}

	@Override
	public Map<String, DataSet> getDataSets() {
		return _dataSets.get();
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_bundleContext = bundleContext;

		_serviceTracker = new ServiceTracker<>(
			_bundleContext, DataSet.class,
			new DataSetServiceTrackerCustomizer());

		_serviceTracker.open();

		_serviceTrackerMap = ServiceTrackerMapFactory.openSingleValueMap(
			bundleContext, DataSet.class, "frontend.data.set.name",
			ServiceTrackerCustomizerFactory.<DataSet>serviceWrapper(
				bundleContext));
	}

	@Deactivate
	protected void deactivate() {
		_serviceTracker.close();

		_serviceTrackerMap.close();
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DataSetRegistryImpl.class);

	private BundleContext _bundleContext;
	private final AtomicReference<Map<String, DataSet>> _dataSets =
		new AtomicReference<>();
	private ServiceTracker<DataSet, DataSet> _serviceTracker;
	private ServiceTrackerMap
		<String, ServiceTrackerCustomizerFactory.ServiceWrapper<DataSet>>
			_serviceTrackerMap;

	private class DataSetServiceTrackerCustomizer
		implements ServiceTrackerCustomizer<DataSet, DataSet> {

		@Override
		public DataSet addingService(
			ServiceReference<DataSet> serviceReference) {

			DataSet dataSet = _bundleContext.getService(serviceReference);

			String fdsName = GetterUtil.getString(
				serviceReference.getProperty("frontend.data.set.name"));

			_dataSets.updateAndGet(
				dataSets -> {
					if (dataSets == null) {
						dataSets = new HashMap<>();
					}
					else {
						dataSets = new HashMap<>(dataSets);
					}

					dataSets.put(fdsName, dataSet);

					return dataSets;
				});

			return dataSet;
		}

		@Override
		public void modifiedService(
			ServiceReference<DataSet> serviceReference, DataSet dataSet) {

			removedService(serviceReference, dataSet);

			addingService(serviceReference);
		}

		@Override
		public void removedService(
			ServiceReference<DataSet> serviceReference, DataSet dataSet) {

			String fdsName = GetterUtil.getString(
				serviceReference.getProperty("frontend.data.set.name"));

			_bundleContext.ungetService(serviceReference);

			_dataSets.updateAndGet(
				dataSets -> {
					dataSets = new HashMap<>(dataSets);

					dataSets.remove(fdsName);

					if (dataSets.isEmpty()) {
						return null;
					}

					return dataSets;
				});
		}

	}

}