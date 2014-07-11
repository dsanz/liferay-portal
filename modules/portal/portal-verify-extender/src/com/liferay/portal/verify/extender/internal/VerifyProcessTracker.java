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

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ClassUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.verify.VerifyException;
import com.liferay.portal.verify.VerifyProcess;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Miguel Pastor
 */
public class VerifyProcessTracker
	extends ServiceTracker<VerifyProcess, VerifyProcess> {

	public VerifyProcessTracker(BundleContext bundleContext) {
		super(bundleContext, VerifyProcess.class, null);

		_bundleContext = bundleContext;
	}

	@Override
	public VerifyProcess addingService(
		ServiceReference<VerifyProcess> serviceReference) {

		VerifyProcess verifyProcess = _bundleContext.getService(
			serviceReference);

		String verifyProcessName = getVerifyProcessName(serviceReference);

		if (Validator.isNull(verifyProcessName)) {
			return null;
		}

		_verifyProcesses.put(verifyProcessName, verifyProcess);

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Executing the verify process " +
					ClassUtil.getClassName(verifyProcess.getClass()));
		}

		try {
			verifyProcess.verify();
		}
		catch (VerifyException ve) {
			_log.error(
				"A verify exception was thrown while executing the verify " +
					"process " +
						ClassUtil.getClassName(verifyProcess.getClass()),
				ve);
		}

		return verifyProcess;
	}

	public void execute(String verifyProcessName) throws VerifyException {
		VerifyProcess verifyProcess = _verifyProcesses.get(verifyProcessName);

		if (verifyProcess == null) {
			System.out.println(
				"Unable to find a verify process with the name " +
					verifyProcessName);

			return;
		}

		verifyProcess.verify();
	}

	public String getVerifyProcessName(
		ServiceReference<VerifyProcess> serviceReference) {

		return (String)serviceReference.getProperty("verify.process.name");
	}

	public void list() {
		for (Map.Entry<String, VerifyProcess> entry :
				_verifyProcesses.entrySet()) {

			System.out.println(
				"Verify process " + ClassUtil.getClassName(entry.getValue()) +
					" is registered with the name " + entry.getKey());
		}
	}

	@Override
	public void modifiedService(
		ServiceReference<VerifyProcess> serviceReference,
		VerifyProcess verifyProcess) {

		addingService(serviceReference);
	}

	@Override
	public void removedService(
		ServiceReference<VerifyProcess> serviceReference,
		VerifyProcess verifyProcess) {

		String verifyProcessName = getVerifyProcessName(serviceReference);

		_verifyProcesses.remove(verifyProcessName);
	}

	private static Log _log = LogFactoryUtil.getLog(VerifyProcessTracker.class);

	private BundleContext _bundleContext;
	private Map<String, VerifyProcess> _verifyProcesses =
		new ConcurrentHashMap<String, VerifyProcess>();

}