/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

import com.liferay.portal.kernel.exception.PortalException;

/**
 * Produces responsive image markup, whatever transformation provider is active.
 *
 * <p>
 * The umbrella every consumer should call when it needs an image tag, replacing
 * direct calls to provider specific factories.
 * </p>
 *
 * <p>
 * Holds no markup knowledge itself. It selects the active provider and hands
 * off to that provider's {@link
 * com.liferay.image.transformation.spi.ImageMarkupRenderer}, because what an
 * image tag should look like depends on how the variants were produced.
 * </p>
 *
 * @author Daniel Sanz
 */
public interface ImageHTMLTagFactory {

	/**
	 * Returns responsive markup wrapping the given image tag.
	 *
	 * <p>
	 * Returns <code>originalImgTag</code> unchanged when no provider can
	 * transform the resource, so this is always safe to call.
	 * </p>
	 *
	 * @param  originalImgTag the original image tag, whose attributes are
	 *         preserved
	 * @param  responsiveImageRequest what the caller wants; use {@link
	 *         ResponsiveImageRequest#of(ImageResource)} for current behavior
	 * @return the responsive markup
	 * @throws PortalException if the resource could not be read
	 */
	public String create(
			String originalImgTag,
			ResponsiveImageRequest responsiveImageRequest)
		throws PortalException;

}