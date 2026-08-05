/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

import com.liferay.portal.kernel.exception.PortalException;

/**
 * Resolves an image resource into a set of variants, using whichever
 * transformation provider is active.
 *
 * <p>
 * The entry point for callers that render their own markup, such as the page
 * editor, which serializes the model to JSON rather than HTML. Callers that
 * want an image tag should use {@link ImageHTMLTagFactory} instead.
 * </p>
 *
 * @author Daniel Sanz
 */
public interface ResponsiveImageProvider {

	/**
	 * Returns the variants of the given image.
	 *
	 * <p>
	 * Never fails because no provider handled the resource: in that case the
	 * result carries the original URL as {@code src} and no variants.
	 * </p>
	 *
	 * @param  responsiveImageRequest what the caller wants; use {@link
	 *         ResponsiveImageRequest#of(ImageResource)} for current behavior
	 * @return the resolved variants
	 * @throws PortalException if the resource could not be read
	 */
	public ResponsiveImage getResponsiveImage(
			ResponsiveImageRequest responsiveImageRequest)
		throws PortalException;

}