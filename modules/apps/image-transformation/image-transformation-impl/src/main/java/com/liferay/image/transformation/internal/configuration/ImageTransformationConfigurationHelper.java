/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal.configuration;

import com.liferay.image.transformation.ResponsiveImageRequest;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.module.configuration.ConfigurationException;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.util.Portal;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves which company's configuration applies to a call, and reads it.
 *
 * <p>
 * Configuration is instance scoped, so it cannot be read once at activation and
 * cached in a field: two companies in the same JVM can have different providers
 * and different presets at the same moment.
 * </p>
 *
 * <p>
 * A plain object built by {@link
 * com.liferay.image.transformation.internal.ImageTransformationFactory} rather
 * than an OSGi component, because it implements no interface and would have to
 * be registered under its own class.
 * </p>
 *
 * @author Daniel Sanz
 */
public class ImageTransformationConfigurationHelper {

	public ImageTransformationConfigurationHelper(
		ConfigurationProvider configurationProvider, Portal portal) {

		_configurationProvider = configurationProvider;
		_portal = portal;
	}

	/**
	 * Returns the company this call is being made for, or <code>0</code> if it
	 * could not be determined.
	 *
	 * <p>
	 * Prefers the caller's request, because that is explicit and cannot be
	 * wrong. Falls back to the ambient company, which is set for most portal
	 * work but not all of it, and is exactly the kind of implicit state that
	 * fails quietly during export or a scheduled job.
	 * </p>
	 *
	 * @param  responsiveImageRequest the request, or <code>null</code>
	 * @return the company ID, or <code>0</code>
	 */
	public long getCompanyId(ResponsiveImageRequest responsiveImageRequest) {
		if (responsiveImageRequest != null) {
			HttpServletRequest httpServletRequest =
				responsiveImageRequest.getHttpServletRequest();

			if (httpServletRequest != null) {
				long companyId = _portal.getCompanyId(httpServletRequest);

				if (companyId > 0) {
					return companyId;
				}
			}
		}

		Long companyId = CompanyThreadLocal.getCompanyId();

		if ((companyId != null) && (companyId > 0)) {
			return companyId;
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Unable to determine a company, falling back to system " +
					"configuration");
		}

		return 0;
	}

	/**
	 * Returns the configuration for the given company, or the system
	 * configuration when no company could be determined.
	 *
	 * @param  companyId the company ID, or <code>0</code>
	 * @return the configuration, or <code>null</code> if it could not be read
	 */
	public ImageTransformationConfiguration getImageTransformationConfiguration(
		long companyId) {

		try {
			if (companyId > 0) {
				return _configurationProvider.getCompanyConfiguration(
					ImageTransformationConfiguration.class, companyId);
			}

			return _configurationProvider.getSystemConfiguration(
				ImageTransformationConfiguration.class);
		}
		catch (ConfigurationException configurationException) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to read configuration for company " + companyId,
					configurationException);
			}

			return null;
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ImageTransformationConfigurationHelper.class);

	private final ConfigurationProvider _configurationProvider;
	private final Portal _portal;

}