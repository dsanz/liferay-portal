/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.responsive.image.ImageResource;
import com.liferay.responsive.image.ResponsiveImageProvider;
import com.liferay.responsive.image.ResponsiveImageRequest;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfiguration;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationHelper;

import java.util.List;
import java.util.function.Supplier;

/**
 * Decides which provider handles a given image.
 *
 * <p>
 * Extracted so that model production and markup rendering cannot disagree about
 * who is active. Both paths ask this one object.
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
public class ResponsiveImageProviderSelector {

	public ResponsiveImageProviderSelector(
		ResponsiveImageConfigurationHelper responsiveImageConfigurationHelper,
		Supplier<List<ResponsiveImageProvider>>
			responsiveImageProvidersSupplier) {

		_responsiveImageConfigurationHelper =
			responsiveImageConfigurationHelper;
		_responsiveImageProvidersSupplier = responsiveImageProvidersSupplier;
	}

	public ResponsiveImageProvider getResponsiveImageProvider(
		ResponsiveImageRequest responsiveImageRequest) {

		List<ResponsiveImageProvider> responsiveImageProviders =
			_responsiveImageProvidersSupplier.get();

		if ((responsiveImageProviders == null) ||
			(responsiveImageRequest == null)) {

			return null;
		}

		ImageResource imageResource = responsiveImageRequest.getImageResource();

		if (imageResource == null) {
			return null;
		}

		String providerName = _getProviderName(responsiveImageRequest);

		// Unconfigured must mean Adaptive Media, not "whichever service
		// happens to be first". Deploying this bundle should not silently
		// change how existing images are served.

		if (Validator.isBlank(providerName)) {
			providerName = _PROVIDER_NAME_DEFAULT;
		}

		ResponsiveImageProvider fallbackResponsiveImageProvider = null;

		for (ResponsiveImageProvider responsiveImageProvider :
				responsiveImageProviders) {

			if (!responsiveImageProvider.canTransform(imageResource)) {
				continue;
			}

			if (providerName.equals(responsiveImageProvider.getName())) {
				return responsiveImageProvider;
			}

			if (fallbackResponsiveImageProvider == null) {
				fallbackResponsiveImageProvider = responsiveImageProvider;
			}
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				StringBundler.concat(
					"No provider named ", providerName,
					" was able to transform ", imageResource.getURL()));
		}

		return fallbackResponsiveImageProvider;
	}

	private String _getProviderName(
		ResponsiveImageRequest responsiveImageRequest) {

		ResponsiveImageConfiguration responsiveImageConfiguration =
			_responsiveImageConfigurationHelper.getResponsiveImageConfiguration(
				_responsiveImageConfigurationHelper.getGroupId(
					responsiveImageRequest),
				_responsiveImageConfigurationHelper.getCompanyId(
					responsiveImageRequest));

		if (responsiveImageConfiguration == null) {
			return null;
		}

		return responsiveImageConfiguration.providerName();
	}

	private static final String _PROVIDER_NAME_DEFAULT = "adaptive-media";

	private static final Log _log = LogFactoryUtil.getLog(
		ResponsiveImageProviderSelector.class);

	private final ResponsiveImageConfigurationHelper
		_responsiveImageConfigurationHelper;
	private final Supplier<List<ResponsiveImageProvider>>
		_responsiveImageProvidersSupplier;

}