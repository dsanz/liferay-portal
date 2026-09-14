/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

import com.liferay.portal.kernel.exception.PortalException;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Resolves an image resource into a set of variants, using whichever
 * transformation provider is active.
 *
 * <p>
 * The entry point for callers that render their own markup, such as the page
 * editor, which serializes the model to JSON rather than HTML. Callers that
 * want an image tag should use {@link ResponsiveImageMarkupRenderer} instead.
 * </p>
 *
 *
 * <p>
 * Carries the same signature as {@link
 * com.liferay.image.transformation.spi.ImageTransformationProvider#getResponsiveImage},
 * deliberately. This is the facade in front of that extension point, and a
 * facade that reshaped the request or the answer would force every caller to
 * learn two models of the same thing. What differs is the contract, not the
 * shape.
 * </p>
 *
 * <p>
 * Call this one. It resolves the company, selects the provider that company
 * configured by name, and guarantees a renderable result: a provider that
 * returns nothing, returns <code>null</code>, or declines the resource all
 * collapse into a passthrough carrying the original URL. Implement the other
 * one, whose contract is only to produce what it can. Trusting each
 * implementation to construct its own passthrough correctly is how a fragment
 * throws months later.
 * </p>
 *
 * <p>
 * Calling the extension point directly would also bind a caller to one
 * provider, where the point of the framework is that the choice is
 * configuration.
 * </p>
 *
 * <p>
 * The word <em>provider</em> means different things in the two names, the same
 * inversion that makes this API <code>@ProviderType</code> while the extension
 * point is not. Here it is the party providing responsive images to callers;
 * there it is a pluggable source of transformations.
 * </p>
 * @author Daniel Sanz
 */
@ProviderType
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