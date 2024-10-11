/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal.renderer;

import com.liferay.frontend.data.set.renderer.ReactPropsProvider;
import com.liferay.frontend.data.set.renderer.ReactPropsProviderRegistry;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;

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
@Component(service = ReactPropsProviderRegistry.class)
public class ReactPropsProviderRegistryImpl
	implements ReactPropsProviderRegistry {

	@Override
	public ReactPropsProvider getReactPropsProvider(
		String fdsName, HttpServletRequest httpServletRequest) {

		List<ReactPropsProviderWrapper> reactPropsProviderWrappers =
			_reactPropsProviderWrappers.get();

		if ((reactPropsProviderWrappers == null) ||
			reactPropsProviderWrappers.isEmpty()) {

			if (_log.isDebugEnabled()) {
				_log.debug("No react props provider is registered");
			}

			return null;
		}

		for (ReactPropsProviderWrapper reactPropsProviderWrapper :
				reactPropsProviderWrappers) {

			ReactPropsProvider reactPropsProvider =
				reactPropsProviderWrapper.getReactPropsProvider();

			if (reactPropsProvider.isAvailable(fdsName, httpServletRequest)) {
				return reactPropsProvider;
			}
		}

		return null;
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_bundleContext = bundleContext;

		_serviceTracker = new ServiceTracker<>(
			_bundleContext, ReactPropsProvider.class,
			new ReactPropsProviderServiceTrackerCustomizer());

		_serviceTracker.open();
	}

	@Deactivate
	protected void deactivate() {
		_serviceTracker.close();
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ReactPropsProviderRegistryImpl.class);

	private BundleContext _bundleContext;
	private final AtomicReference<List<ReactPropsProviderWrapper>>
		_reactPropsProviderWrappers = new AtomicReference<>();
	private ServiceTracker<ReactPropsProvider, ReactPropsProvider>
		_serviceTracker;

	private class ReactPropsProviderServiceTrackerCustomizer
		implements ServiceTrackerCustomizer
			<ReactPropsProvider, ReactPropsProvider> {

		@Override
		public ReactPropsProvider addingService(
			ServiceReference<ReactPropsProvider> serviceReference) {

			ReactPropsProvider reactPropsProvider = _bundleContext.getService(
				serviceReference);

			int order = GetterUtil.getInteger(
				serviceReference.getProperty("react.props.provider.order"));

			ReactPropsProviderWrapper reactPropsProviderWrapper =
				new ReactPropsProviderWrapper(reactPropsProvider, order);

			_reactPropsProviderWrappers.updateAndGet(
				reactPropsProviderWrappers -> {
					if (reactPropsProviderWrappers == null) {
						reactPropsProviderWrappers = new ArrayList<>();
					}
					else {
						reactPropsProviderWrappers = new ArrayList<>(
							reactPropsProviderWrappers);
					}

					int index = Collections.binarySearch(
						reactPropsProviderWrappers, reactPropsProviderWrapper,
						Comparator.reverseOrder());

					if (index < 0) {
						index = -index - 1;
					}

					reactPropsProviderWrappers.add(
						index, reactPropsProviderWrapper);

					return reactPropsProviderWrappers;
				});

			return reactPropsProvider;
		}

		@Override
		public void modifiedService(
			ServiceReference<ReactPropsProvider> serviceReference,
			ReactPropsProvider reactPropsProvider) {

			removedService(serviceReference, reactPropsProvider);

			addingService(serviceReference);
		}

		@Override
		public void removedService(
			ServiceReference<ReactPropsProvider> serviceReference,
			ReactPropsProvider reactPropsProvider) {

			_bundleContext.ungetService(serviceReference);

			_reactPropsProviderWrappers.updateAndGet(
				reactPropsProviderWrappers -> {
					reactPropsProviderWrappers = new ArrayList<>(
						reactPropsProviderWrappers);

					reactPropsProviderWrappers.removeIf(
						reactPropsProviderWrapper ->
							reactPropsProviderWrapper._reactPropsProvider ==
								reactPropsProvider);

					if (reactPropsProviderWrappers.isEmpty()) {
						return null;
					}

					return reactPropsProviderWrappers;
				});
		}

	}

	private class ReactPropsProviderWrapper
		implements Comparable<ReactPropsProviderWrapper> {

		@Override
		public int compareTo(
			ReactPropsProviderWrapper reactPropsProviderWrapper) {

			return _order.compareTo(reactPropsProviderWrapper._order);
		}

		public ReactPropsProvider getReactPropsProvider() {
			return _reactPropsProvider;
		}

		private ReactPropsProviderWrapper(
			ReactPropsProvider reactPropsProvider, Integer order) {

			_reactPropsProvider = reactPropsProvider;
			_order = order;
		}

		private final Integer _order;
		private ReactPropsProvider _reactPropsProvider;

	}

}