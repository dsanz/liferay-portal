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
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;


/**
 * @author Daniel Sanz
 */
@Component(service = DataSetRegistry.class)
public class DataSetRegistryImpl implements DataSetRegistry {

	@Activate
	protected void activate(BundleContext bundleContext) {
		_serviceTrackerMap = ServiceTrackerMapFactory.openSingleValueMap(
			bundleContext, DataSet.class, "frontend.data.set.name",
			ServiceTrackerCustomizerFactory.<DataSet>serviceWrapper(
				bundleContext));
	}

	@Deactivate
	protected void deactivate() {
		_serviceTrackerMap.close();
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DataSetRegistryImpl.class);

	private ServiceTrackerMap<String, ServiceTrackerCustomizerFactory.ServiceWrapper<DataSet>>
		_serviceTrackerMap;

	@Override
	public DataSet getDataSet(String fdsName) {
		ServiceTrackerCustomizerFactory.ServiceWrapper<DataSet> dataSetServiceWrapper =
			_serviceTrackerMap.getService(fdsName);

		if (dataSetServiceWrapper == null) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"No data set frontend data set filter is associated with " +
						fdsName);
			}

			return null;
		}

		return dataSetServiceWrapper.getService();
	}
}
