/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image;

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
 * put. A request without an image has nothing to render, and this way there
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
			_groupId, _httpServletRequest, _imageResource, _presetName);
	}

	/**
	 * Declares the site this image is rendered for, overriding the site the
	 * framework would otherwise derive from the request.
	 *
	 * <p>
	 * Needed only where there is no request to derive from, such as export and
	 * import, which knows its site from the portlet data context. Callers that
	 * pass a request should leave this alone and let the theme display answer,
	 * since setting both makes this one win and a stale value would then read
	 * the wrong site's layout without failing.
	 * </p>
	 */
	public ResponsiveImageRequestBuilder groupId(long groupId) {
		_groupId = groupId;

		return this;
	}

	public ResponsiveImageRequestBuilder httpServletRequest(
		HttpServletRequest httpServletRequest) {

		_httpServletRequest = httpServletRequest;

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

	private long _groupId;
	private HttpServletRequest _httpServletRequest;
	private final ImageResource _imageResource;
	private String _presetName;

}