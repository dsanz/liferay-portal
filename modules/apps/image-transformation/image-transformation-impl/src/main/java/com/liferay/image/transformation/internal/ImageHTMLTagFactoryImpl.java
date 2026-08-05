/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal;

import com.liferay.image.transformation.ImageHTMLTagFactory;
import com.liferay.image.transformation.ResponsiveImageRequest;
import com.liferay.image.transformation.spi.ImageTransformationProvider;
import com.liferay.portal.kernel.exception.PortalException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Dispatches to the active provider.
 *
 * <p>
 * Contains no markup. Each provider decides what its own output should look
 * like, so this class only has to find the right one and get out of the way.
 * </p>
 *
 * @author Daniel Sanz
 */
@Component(service = ImageHTMLTagFactory.class)
public class ImageHTMLTagFactoryImpl implements ImageHTMLTagFactory {

	@Override
	public String create(
			String originalImgTag,
			ResponsiveImageRequest responsiveImageRequest)
		throws PortalException {

		if (responsiveImageRequest == null) {
			return originalImgTag;
		}

		ImageTransformationProvider imageTransformationProvider =
			_imageTransformationProviderSelector.getImageTransformationProvider(
				responsiveImageRequest);

		if (imageTransformationProvider == null) {
			return originalImgTag;
		}

		return imageTransformationProvider.render(
			originalImgTag, responsiveImageRequest);
	}

	@Reference
	private ImageTransformationProviderSelector
		_imageTransformationProviderSelector;

}