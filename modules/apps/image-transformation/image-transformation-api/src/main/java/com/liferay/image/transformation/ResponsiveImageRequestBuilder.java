/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Builds a {@link ResponsiveImageRequest}.
 *
 * <pre>
 * ResponsiveImageRequestBuilder.imageResource(
 *     imageResource
 * ).presetName(
 *     "card"
 * ).build()
 * </pre>
 *
 * <p>
 * Entered through a static method carrying the one required value, as {@link
 * com.liferay.portal.kernel.util.HashMapBuilder} is entered through its first
 * put. A request without an image has nothing to transform, and this way there
 * is no partially built state to reach: the constructor is private, so the
 * image cannot be omitted or supplied twice.
 * </p>
 *
 * <p>
 * Accumulates values and constructs at {@link #build}, rather than mutating a
 * held instance as builders over Service Builder models do, because the request
 * it produces is immutable.
 * </p>
 *
 * @author Daniel Sanz
 */
public class ResponsiveImageRequestBuilder {

	public static ResponsiveImageRequestBuilder imageResource(
		ImageResource imageResource) {

		return new ResponsiveImageRequestBuilder(imageResource);
	}

	public ResponsiveImageRequest build() {
		return new ResponsiveImageRequest(
			_httpServletRequest, _imageResource, _lazy, _presetName);
	}

	public ResponsiveImageRequestBuilder httpServletRequest(
		HttpServletRequest httpServletRequest) {

		_httpServletRequest = httpServletRequest;

		return this;
	}

	public ResponsiveImageRequestBuilder lazy(Boolean lazy) {
		_lazy = lazy;

		return this;
	}

	public ResponsiveImageRequestBuilder presetName(String presetName) {
		_presetName = presetName;

		return this;
	}

	private ResponsiveImageRequestBuilder(ImageResource imageResource) {
		if (imageResource == null) {
			throw new IllegalArgumentException("Image resource is null");
		}

		_imageResource = imageResource;
	}

	private HttpServletRequest _httpServletRequest;
	private final ImageResource _imageResource;
	private Boolean _lazy;
	private String _presetName;

}