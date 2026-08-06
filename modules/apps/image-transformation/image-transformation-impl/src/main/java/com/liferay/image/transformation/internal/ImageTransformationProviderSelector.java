/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal;

import com.liferay.image.transformation.ImageResource;
import com.liferay.image.transformation.ResponsiveImageRequest;
import com.liferay.image.transformation.internal.configuration.ImageTransformationConfiguration;
import com.liferay.image.transformation.internal.configuration.ImageTransformationConfigurationHelper;
import com.liferay.image.transformation.spi.ImageTransformationProvider;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

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
 * A plain object built by {@link ImageTransformationFactory} rather than an
 * OSGi component; its consumers are services and inject what it needs.
 * </p>
 *
 * @author Daniel Sanz
 */
public class ImageTransformationProviderSelector {

	public ImageTransformationProviderSelector(
		ImageTransformationConfigurationHelper
			imageTransformationConfigurationHelper,
		Supplier<List<ImageTransformationProvider>>
			imageTransformationProvidersSupplier) {

		_imageTransformationConfigurationHelper =
			imageTransformationConfigurationHelper;
		_imageTransformationProvidersSupplier =
			imageTransformationProvidersSupplier;
	}

	public ImageTransformationProvider getImageTransformationProvider(
		ResponsiveImageRequest responsiveImageRequest) {

		List<ImageTransformationProvider> imageTransformationProviders =
			_imageTransformationProvidersSupplier.get();

		if ((imageTransformationProviders == null) ||
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

		ImageTransformationProvider fallbackImageTransformationProvider = null;

		for (ImageTransformationProvider imageTransformationProvider :
				imageTransformationProviders) {

			if (!imageTransformationProvider.isTransformable(imageResource)) {
				continue;
			}

			if (providerName.equals(imageTransformationProvider.getName())) {
				return imageTransformationProvider;
			}

			if (fallbackImageTransformationProvider == null) {
				fallbackImageTransformationProvider =
					imageTransformationProvider;
			}
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				StringBundler.concat(
					"No provider named ", providerName,
					" was able to transform ", imageResource.getURL()));
		}

		return fallbackImageTransformationProvider;
	}

	private String _getProviderName(
		ResponsiveImageRequest responsiveImageRequest) {

		ImageTransformationConfiguration imageTransformationConfiguration =
			_imageTransformationConfigurationHelper.
				getImageTransformationConfiguration(
					_imageTransformationConfigurationHelper.getCompanyId(
						responsiveImageRequest));

		if (imageTransformationConfiguration == null) {
			return null;
		}

		return imageTransformationConfiguration.providerName();
	}

	private static final String _PROVIDER_NAME_DEFAULT = "adaptive-media";

	private static final Log _log = LogFactoryUtil.getLog(
		ImageTransformationProviderSelector.class);

	private final ImageTransformationConfigurationHelper
		_imageTransformationConfigurationHelper;
	private final Supplier<List<ImageTransformationProvider>>
		_imageTransformationProvidersSupplier;

}