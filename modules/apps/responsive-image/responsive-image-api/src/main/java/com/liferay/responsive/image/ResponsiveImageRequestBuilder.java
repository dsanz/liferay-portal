/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image;

import jakarta.servlet.http.HttpServletRequest;

/**
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