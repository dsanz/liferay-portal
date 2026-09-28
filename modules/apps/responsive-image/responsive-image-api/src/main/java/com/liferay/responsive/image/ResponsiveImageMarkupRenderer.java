/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image;

import com.liferay.portal.kernel.exception.PortalException;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Produces responsive image markup, whatever url renderer is active.
 *
 * <p>
 * The umbrella every consumer should call when it needs image markup, replacing
 * direct calls to specific factories.
 * </p>
 *
 * @author Daniel Sanz
 */
@ProviderType
public interface ResponsiveImageMarkupRenderer {

	/**
	 * Returns responsive markup wrapping the given image tag, or
	 * <code>null</code> when nothing was produced.
	 *
	 * @param  originalImgTag the original image tag, whose attributes are
	 *         preserved
	 * @param  responsiveImageRequest what the caller wants; use {@link
	 *         ResponsiveImageRequest#of(ImageResource)} for current behavior
	 * @return the responsive markup, or <code>null</code> if the framework
	 *         declined
	 * @throws PortalException if the resource could not be read
	 */
	public String render(
			String originalImgTag,
			ResponsiveImageRequest responsiveImageRequest)
		throws PortalException;

}