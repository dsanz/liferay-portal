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
 * Register with a {@link #RENDERER_NAME} service property naming the provider,
 * which is how callers select one:
 * </p>
 *
 * <pre>
 * &#64;Component(
 *     property = "image.transformation.renderer.name=fastly",
 *     service = ImageTransformationURLRenderer.class
 * )
 * </pre>
 *
 * <p>
 * The property rather than a method, so that several implementations can be
 * deployed at once and indexed without instantiating any of them. Selection is
 * by name rather than by service ranking because it is a per company
 * configuration decision, not a deployment ordering accident.
 * </p>
 *
 * @author Daniel Sanz
 */
public interface ImageTransformationURLRenderer {

	/**
	 * The service property naming the provider this renderer speaks for, and
	 * the value {@link TransformedImageAbsolutePortalURLBuilder#rendererName(
	 * String)} is matched against.
	 */
	public static final String RENDERER_NAME =
		"image.transformation.renderer.name";

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