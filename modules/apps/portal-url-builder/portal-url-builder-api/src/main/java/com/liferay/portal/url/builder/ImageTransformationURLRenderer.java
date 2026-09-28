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
 * in a provider neutral way. This interface performs the last step: turning
 * those transformations into whatever query string or path segment contract
 * the provider understands.
 * </p>
 *
 * <p>
 * Implementations must be deterministic. Two calls with equal transformations
 * must produce byte identical URLs, otherwise every render creates a new CDN
 * cache object for the same image.
 * </p>
 *
 * @author Daniel Sanz
 */
public interface ImageTransformationURLRenderer {

	/**
	 * Returns the name of the provider this renderer speaks for, for example
	 * <code>fastly</code>.
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