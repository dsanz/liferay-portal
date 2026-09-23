/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.url.builder;

import com.liferay.portal.url.builder.facet.BuildableAbsolutePortalURLBuilder;
import com.liferay.portal.url.builder.facet.CDNAwareAbsolutePortalURLBuilder;

/**
 * Builds a URL for an image that an external image optimization provider may
 * transform on the fly.
 *
 * <p>
 * Unlike the other builders in this package, the path passed to {@link
 * AbsolutePortalURLBuilder#forTransformedImage(String)} is <b>not</b> resolved
 * against a well known portal root such as {@code Portal#PATH_IMAGE}. Images
 * that can be transformed live wherever the repository put them (for example
 * {@code /documents/...}), so the caller supplies a complete portal relative
 * path.
 * </p>
 *
 * <p>
 * Transformations are collected in a provider neutral form and rendered by the
 * registered {@link ImageTransformationURLRenderer}. When no renderer is
 * available the transformations are dropped and the untransformed URL is
 * returned, so callers always receive a usable URL.
 * </p>
 *
 * @author Daniel Sanz
 */
public interface TransformedImageAbsolutePortalURLBuilder
	extends BuildableAbsolutePortalURLBuilder,
			CDNAwareAbsolutePortalURLBuilder
				<TransformedImageAbsolutePortalURLBuilder> {

	/**
	 * Requests the given output format (for example <code>webp</code>).
	 *
	 * @param  format the output format
	 * @return this builder
	 */
	public TransformedImageAbsolutePortalURLBuilder format(String format);

	/**
	 * Requests the given output height, in pixels.
	 *
	 * @param  height the output height
	 * @return this builder
	 */
	public TransformedImageAbsolutePortalURLBuilder height(int height);

	/**
	 * Requests an arbitrary provider specific transformation. Use this for
	 * options that this builder does not model explicitly.
	 *
	 * @param  name the transformation name
	 * @param  value the transformation value
	 * @return this builder
	 */
	public TransformedImageAbsolutePortalURLBuilder param(
		String name, String value);

	/**
	 * Requests the given output compression quality (0-100).
	 *
	 * @param  quality the output quality
	 * @return this builder
	 */
	public TransformedImageAbsolutePortalURLBuilder quality(int quality);

	/**
	 * Sets the renderer that spells the transformations.
	 *
	 * <p>
	 * Required in order for anything to be transformed. Which provider serves a
	 * given image is a configuration decision, and the caller is the one
	 * holding that configuration, so it supplies the renderer rather than
	 * naming one for this builder to look up. Passing <code>null</code>, which
	 * is what a caller that cannot resolve one should do, drops the
	 * transformations and returns the untransformed URL.
	 * </p>
	 *
	 * @param  imageTransformationURLRenderer the renderer, or <code>null</code>
	 * @return this builder
	 */
	public TransformedImageAbsolutePortalURLBuilder renderer(
		ImageTransformationURLRenderer imageTransformationURLRenderer);

	/**
	 * Requests the given output width, in pixels.
	 *
	 * @param  width the output width
	 * @return this builder
	 */
	public TransformedImageAbsolutePortalURLBuilder width(int width);

}