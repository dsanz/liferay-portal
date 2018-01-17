/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.subscription.manager.web.internal.registry;

import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMapFactory;
import com.liferay.subscription.manager.SubscriptionManagerHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

/**
 * @author Roberto Díaz
 */
@Component(service = SubscriptionManagerHandlerRegistry.class)
public class SubscriptionManagerHandlerRegistry {

	public Map<String, SubscriptionManagerHandler>
		getSubscriptionManagerHandlerMap() {

		Map<String, SubscriptionManagerHandler> subscriptionManagerHandlerMap =
			new HashMap<>();

		Set<String> keys = _serviceTrackerMap.keySet();

		for (String key : keys) {
			subscriptionManagerHandlerMap.put(
				key, _serviceTrackerMap.getService(key));
		}

		return subscriptionManagerHandlerMap;
	}

	@Activate
	@Modified
	protected void activate(BundleContext bundleContext) {
		_serviceTrackerMap = ServiceTrackerMapFactory.openSingleValueMap(
			bundleContext, SubscriptionManagerHandler.class,
			"model.class.name");
	}

	@Deactivate
	protected void deactivate() {
		_serviceTrackerMap.close();
	}

	private ServiceTrackerMap<String, SubscriptionManagerHandler>
		_serviceTrackerMap;

}