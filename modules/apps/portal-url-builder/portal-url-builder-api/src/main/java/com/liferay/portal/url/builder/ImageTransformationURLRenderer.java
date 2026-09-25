/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.url.builder;

import java.util.Map;

/**
 * Renders image transformation instructions using the URL vocabulary of a
 * concrete image optimization provider (typically a CDN).
 *
 * <p>
 * The {@link TransformedImageAbsolutePortalURLBuilder} collects transformations
 * in a provider neutral way (width, height, quality, format, and arbitrary
 * named parameters). This interface performs the last step: turning those
 * transformations into whatever query string, path segment, or header contract
 * the provider understands.
 * </p>
 *
 * <p>
 * Implementations must be deterministic. Two calls with equal transformations
 * must produce byte identical URLs, otherwise every render creates a new CDN
 * cache object for the same image.
 * </p>
 *
 * <p>
 * Register an implementation as an OSGi service. Several can be deployed at
 * once; a caller picks the one its configuration asks for by matching {@link
 * #getName()}, rather than by service ranking, because which vocabulary a
 * company's images are spelled in is a configuration decision and not a
 * deployment ordering accident.
 * </p>
 *
 * @author Daniel Sanz
 */
public interface ImageTransformationURLRenderer {

	/**
	 * Returns the name of the provider this renderer speaks for, for example
	 * <code>fastly</code>.
	 *
	 * <p>
	 * Resolving a name to a renderer is the caller's job, not this package's:
	 * the name comes from the caller's own configuration, so it matches the
	 * registered renderers itself and hands the chosen one to {@link
	 * TransformedImageAbsolutePortalURLBuilder#setRenderer(
	 * ImageTransformationURLRenderer)}.
	 * </p>
	 *
	 * @return the provider name
	 */
	public String getName();

	/**
	 * Returns the URL with the given transformations applied.
	 *
	 * @param  url the image URL to transform
	 * @param  transformations the transformations to apply, keyed by the
	 *         provider neutral names used by {@link
	 *         TransformedImageAbsolutePortalURLBuilder}
	 * @return the transformed URL, or <code>url</code> unchanged if the
	 *         provider cannot apply any of the transformations
	 */
	public String render(String url, Map<String, String> transformations);

}