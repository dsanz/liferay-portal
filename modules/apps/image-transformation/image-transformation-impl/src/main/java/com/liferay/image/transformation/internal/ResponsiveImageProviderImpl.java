/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal;

import com.liferay.image.transformation.ImageResource;
import com.liferay.image.transformation.ImageVariantGroup;
import com.liferay.image.transformation.ResponsiveImage;
import com.liferay.image.transformation.ResponsiveImageProvider;
import com.liferay.image.transformation.ResponsiveImageRequest;
import com.liferay.image.transformation.spi.ImageTransformationProvider;
import com.liferay.portal.kernel.exception.PortalException;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Resolves an image through the active provider, for consumers that want the
 * model rather than markup.
 *
 * @author Daniel Sanz
 */
@Component(service = ResponsiveImageProvider.class)
public class ResponsiveImageProviderImpl implements ResponsiveImageProvider {

	@Override
	public ResponsiveImage getResponsiveImage(
			ResponsiveImageRequest responsiveImageRequest)
		throws PortalException {

		if (responsiveImageRequest == null) {
			return null;
		}

		ImageResource imageResource = responsiveImageRequest.getImageResource();

		ImageTransformationProvider imageTransformationProvider =
			_imageTransformationProviderSelector.getImageTransformationProvider(
				responsiveImageRequest);

		if (imageTransformationProvider == null) {
			return ResponsiveImage.passthrough(imageResource.getURL());
		}

		ResponsiveImage responsiveImage =
			imageTransformationProvider.getResponsiveImage(
				responsiveImageRequest);

		// Normalize here so that consumers are guaranteed something renderable
		// no matter how a provider behaves. Trusting every implementation to
		// construct its own passthrough correctly is how a fragment ends up
		// throwing months later.

		if (responsiveImage == null) {
			return ResponsiveImage.passthrough(imageResource.getURL());
		}

		List<ImageVariantGroup> imageVariantGroups =
			responsiveImage.getVariantGroups();

		if (imageVariantGroups.isEmpty()) {
			return ResponsiveImage.passthrough(imageResource.getURL());
		}

		return responsiveImage;
	}

	@Reference
	private ImageTransformationProviderSelector
		_imageTransformationProviderSelector;

}