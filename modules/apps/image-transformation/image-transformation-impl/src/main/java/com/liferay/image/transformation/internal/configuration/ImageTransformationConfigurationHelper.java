/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal.configuration;

import com.liferay.image.transformation.ResponsiveImageRequest;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.module.configuration.ConfigurationException;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.WebKeys;

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
 * Constructed directly by the components that need it, and deliberately not
 * registered as an OSGi component. It implements no interface, so registering
 * it means naming its own class in the <code>service</code> attribute, which
 * reads as redundant and is easy to "simplify" into <code>service = {}</code>.
 * A component in that state is active but registers nothing, leaving every
 * reference to it unsatisfied while the bundle still starts cleanly and the
 * tests still pass. Constructing it from collaborators the consumer already
 * injects removes that failure mode entirely.
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
	 * Returns the site this image is being rendered for, or <code>0</code>.
	 *
	 * <p>
	 * What the caller supplied wins. Otherwise the rendering site is taken from
	 * the theme display, and failing that from the ambient service context.
	 * Both name the site a page is being served for, which is the question this
	 * is asking.
	 * </p>
	 *
	 * <p>
	 * Note what is deliberately absent: the site of the image itself. That is a
	 * different question, and for a file in a shared asset library it is never
	 * the right answer, so falling back to it would read one site's layout
	 * while rendering another's without ever saying so. Better to return
	 * <code>0</code> and let the configuration cascade to the company.
	 * </p>
	 *
	 * @param  responsiveImageRequest the request, or <code>null</code>
	 * @return the site ID, or <code>0</code>
	 */
	public long getGroupId(ResponsiveImageRequest responsiveImageRequest) {
		if (responsiveImageRequest == null) {
			return 0;
		}

		long groupId = responsiveImageRequest.getGroupId();

		if (groupId > 0) {
			return groupId;
		}

		HttpServletRequest httpServletRequest =
			responsiveImageRequest.getHttpServletRequest();

		if (httpServletRequest != null) {
			ThemeDisplay themeDisplay =
				(ThemeDisplay)httpServletRequest.getAttribute(
					WebKeys.THEME_DISPLAY);

			if (themeDisplay != null) {
				groupId = themeDisplay.getScopeGroupId();

				if (groupId > 0) {
					return groupId;
				}
			}
		}

		ServiceContext serviceContext =
			ServiceContextThreadLocal.getServiceContext();

		if (serviceContext != null) {
			groupId = serviceContext.getScopeGroupId();

			if (groupId > 0) {
				return groupId;
			}
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				"Unable to determine a site, falling back to the company " +
					"configuration");
		}

		return 0;
	}

	/**
	 * Returns the configuration for the narrowest scope the caller could name.
	 *
	 * <p>
	 * A site when one is given, otherwise the company, otherwise the system.
	 * Reading the site configuration already falls back to the company and then
	 * to the metatype defaults, so a site that configures nothing is not a
	 * special case here.
	 * </p>
	 *
	 * @param  groupId the site, or <code>0</code>
	 * @param  companyId the company, or <code>0</code>
	 * @return the configuration, or <code>null</code> if it could not be read
	 */
	public ImageTransformationConfiguration getImageTransformationConfiguration(
		long groupId, long companyId) {

		try {
			if (groupId > 0) {
				return _configurationProvider.getGroupConfiguration(
					ImageTransformationConfiguration.class, groupId);
			}

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
					StringBundler.concat(
						"Unable to read configuration for group ", groupId,
						" and company ", companyId),
					configurationException);
			}

			return null;
		}
	}

	/**
	 * Returns a key identifying the scope a configuration was read for, so that
	 * parsed results can be cached per scope.
	 *
	 * <p>
	 * Sites are keyed positive and companies negative, because the two ID
	 * spaces are distinct and a shared map would otherwise let a site borrow a
	 * company's parsed presets.
	 * </p>
	 *
	 * @param  groupId the site, or <code>0</code>
	 * @param  companyId the company, or <code>0</code>
	 * @return the scope key
	 */
	public long getScopeKey(long groupId, long companyId) {
		if (groupId > 0) {
			return groupId;
		}

		return -companyId;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ImageTransformationConfigurationHelper.class);

	private final ConfigurationProvider _configurationProvider;
	private final Portal _portal;

}