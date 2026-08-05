/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.url.builder.internal;

import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMapFactory;
import com.liferay.portal.kernel.frontend.hashed.files.HashedFilesRegistry;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.url.builder.AbsolutePortalURLBuilder;
import com.liferay.portal.url.builder.AbsolutePortalURLBuilderFactory;
import com.liferay.portal.url.builder.ImageTransformationURLRenderer;
import com.liferay.portal.url.builder.internal.util.CacheHelper;

import jakarta.servlet.http.HttpServletRequest;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Iván Zaera Avellón
 */
@Component(service = AbsolutePortalURLBuilderFactory.class)
public class AbsolutePortalURLBuilderFactoryImpl
	implements AbsolutePortalURLBuilderFactory {

	@Override
	public AbsolutePortalURLBuilder getAbsolutePortalURLBuilder(
		HttpServletRequest httpServletRequest) {

		return new AbsolutePortalURLBuilderImpl(
			_cacheHelper, _hashedFilesRegistry, _serviceTrackerMap, _portal,
			httpServletRequest);
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_serviceTrackerMap = ServiceTrackerMapFactory.openSingleValueMap(
			bundleContext, ImageTransformationURLRenderer.class,
			ImageTransformationURLRenderer.RENDERER_NAME);
	}

	@Deactivate
	protected void deactivate() {
		_serviceTrackerMap.close();
	}

	@Reference
	private CacheHelper _cacheHelper;

	@Reference
	private HashedFilesRegistry _hashedFilesRegistry;

	@Reference
	private Portal _portal;

	/**
	 * Indexed by name rather than resolved by service ranking: which provider
	 * serves a company's images is a configuration decision, and ranking would
	 * make it a deployment ordering accident. Empty is normal, and means
	 * transformations are dropped rather than that anything is wrong.
	 */
	private ServiceTrackerMap<String, ImageTransformationURLRenderer>
		_serviceTrackerMap;

}