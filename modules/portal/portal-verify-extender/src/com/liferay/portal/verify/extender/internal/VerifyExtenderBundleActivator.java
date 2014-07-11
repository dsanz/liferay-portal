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

package com.liferay.portal.verify.extender.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Miguel Pastor
 */
public class VerifyExtenderBundleActivator implements BundleActivator {

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		_verifyProcessTracker = new VerifyProcessTracker(bundleContext);

		_verifyProcessTracker.open();

		_serviceRegistration = _registerServiceRegistration(bundleContext);
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		_serviceRegistration.unregister();

		_serviceRegistration = null;

		_verifyProcessTracker.close();

		_verifyProcessTracker = null;
	}

	private ServiceRegistration<Object> _registerServiceRegistration(
		BundleContext bundleContext) {

		Dictionary<String, Object> properties = new Hashtable<String, Object>();

		properties.put(
			"osgi.command.function",
			new String[] {"execute", "list"});
		properties.put("osgi.command.scope", "verify-extender");

		return bundleContext.registerService(
			Object.class, _verifyProcessTracker, properties);
	}

	private ServiceRegistration<Object> _serviceRegistration;
	private VerifyProcessTracker _verifyProcessTracker;

}