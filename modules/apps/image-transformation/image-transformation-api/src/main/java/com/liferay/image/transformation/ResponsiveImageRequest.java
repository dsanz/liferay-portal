/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Everything one call needs: which image, where it sits, and the context it is
 * being rendered in.
 *
 * <p>
 * Deliberately a concrete class and not an interface. Nobody implements this;
 * consumers build it and providers read it. As an interface it would have to be
 * a consumer type, making every new field a breaking change for a holder that
 * has exactly one implementation.
 * </p>
 *
 * @author Daniel Sanz
 */
public final class ResponsiveImageRequest {

	public static Builder builder(
		ImageResource imageResource, HttpServletRequest httpServletRequest) {

		return new Builder(imageResource, httpServletRequest);
	}

	/**
	 * Returns a request for the given image with no preset group and lazy
	 * loading enabled, which reproduces current behavior.
	 *
	 * @param  imageResource the image to transform
	 * @param  httpServletRequest the request being served
	 * @return the request
	 */
	public static ResponsiveImageRequest of(
		ImageResource imageResource, HttpServletRequest httpServletRequest) {

		return new Builder(
			imageResource, httpServletRequest
		).build();
	}

	/**
	 * Returns the request being served. Never <code>null</code>.
	 *
	 * <p>
	 * Carries two things providers need: CDN host resolution is request scoped,
	 * and the company whose configuration applies, obtained with {@code
	 * Portal#getCompanyId(HttpServletRequest)}. Supplied by the caller rather
	 * than read from a thread local, because the thread local that would
	 * otherwise carry it is pushed by the service layer and is frequently
	 * absent while rendering.
	 * </p>
	 *
	 * <p>
	 * Required rather than optional. Without it a URL can still be built, but
	 * without the CDN host or the proxy path, so it is wrong for the deployment
	 * in a way nothing downstream can detect. A caller with no request should
	 * not be asking for a transformed image at all.
	 * </p>
	 *
	 * @return the servlet request
	 */
	public HttpServletRequest getHttpServletRequest() {
		return _httpServletRequest;
	}

	/**
	 * Returns the image to transform.
	 *
	 * @return the image
	 */
	public ImageResource getImageResource() {
		return _imageResource;
	}

	/**
	 * Returns the name of the configured preset group describing where this
	 * image sits in the page layout (for example <code>card</code>), or
	 * <code>null</code> to use the default.
	 *
	 * @return the preset group name, or <code>null</code>
	 */
	public String getPresetGroupName() {
		return _presetGroupName;
	}

	/**
	 * Returns <code>true</code> if the rendered image should be lazily loaded.
	 *
	 * <p>
	 * Not merely a rendering detail: <code>sizes="auto"</code> is only valid
	 * together with <code>loading="lazy"</code>, so the framework cannot choose
	 * the automatic sizes strategy without knowing this.
	 * </p>
	 *
	 * @return <code>true</code> if lazily loaded
	 */
	public boolean isLazy() {
		return _lazy;
	}

	public static final class Builder {

		public ResponsiveImageRequest build() {
			return new ResponsiveImageRequest(
				_httpServletRequest, _imageResource, _lazy, _presetGroupName);
		}

		public Builder lazy(boolean lazy) {
			_lazy = lazy;

			return this;
		}

		public Builder presetGroupName(String presetGroupName) {
			_presetGroupName = presetGroupName;

			return this;
		}

		private Builder(
			ImageResource imageResource,
			HttpServletRequest httpServletRequest) {

			if (imageResource == null) {
				throw new IllegalArgumentException("Image resource is null");
			}

			if (httpServletRequest == null) {
				throw new IllegalArgumentException(
					"HTTP servlet request is null");
			}

			_imageResource = imageResource;
			_httpServletRequest = httpServletRequest;
		}

		private final HttpServletRequest _httpServletRequest;
		private final ImageResource _imageResource;
		private boolean _lazy = true;
		private String _presetGroupName;

	}

	private ResponsiveImageRequest(
		HttpServletRequest httpServletRequest, ImageResource imageResource,
		boolean lazy, String presetGroupName) {

		_httpServletRequest = httpServletRequest;
		_imageResource = imageResource;
		_lazy = lazy;
		_presetGroupName = presetGroupName;
	}

	private final HttpServletRequest _httpServletRequest;
	private final ImageResource _imageResource;
	private final boolean _lazy;
	private final String _presetGroupName;

}