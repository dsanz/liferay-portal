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

	public static Builder builder(ImageResource imageResource) {
		return new Builder(imageResource);
	}

	/**
	 * Returns a request for the given image with no preset group and lazy
	 * loading enabled, which reproduces current behavior.
	 *
	 * @param  imageResource the image to transform
	 * @return the request
	 */
	public static ResponsiveImageRequest of(ImageResource imageResource) {
		return new Builder(
			imageResource
		).build();
	}

	/**
	 * Returns the request being served, or <code>null</code> if the caller has
	 * none.
	 *
	 * <p>
	 * Worth supplying when available: it resolves the CDN host exactly, knows
	 * whether the connection is secure, and identifies the company. Callers
	 * that have one should pass it.
	 * </p>
	 *
	 * <p>
	 * Optional rather than required, because several rendering paths genuinely
	 * have none to give. Content transformers and template transformer
	 * listeners take a string and return a string, and there is no portal wide
	 * thread local carrying the current request. Without it, the company comes
	 * from the ambient one and the CDN host is resolved per company rather than
	 * per request.
	 * </p>
	 *
	 * @return the servlet request, or <code>null</code>
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
	 * Returns whether the rendered image should be lazily loaded, or
	 * <code>null</code> to use the preset group's default.
	 *
	 * <p>
	 * Deliberately per call rather than per placement. Whether an image is
	 * lazily loaded depends on where <em>this</em> instance sits on the page,
	 * not on what kind of slot it occupies: the same card is above the fold in
	 * the first row and below it further down, and lazily loading the largest
	 * contentful image is a measurable regression.
	 * </p>
	 *
	 * <p>
	 * Also decides whether automatic sizing may be used at all, since
	 * <code>sizes="auto"</code> is only honored on a lazily loaded image.
	 * </p>
	 *
	 * @return whether to lazily load, or <code>null</code> to defer to the
	 *         preset group
	 */
	public Boolean getLazy() {
		return _lazy;
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

	public static final class Builder {

		public ResponsiveImageRequest build() {
			return new ResponsiveImageRequest(
				_httpServletRequest, _imageResource, _lazy, _presetGroupName);
		}

		public Builder httpServletRequest(
			HttpServletRequest httpServletRequest) {

			_httpServletRequest = httpServletRequest;

			return this;
		}

		public Builder lazy(Boolean lazy) {
			_lazy = lazy;

			return this;
		}

		public Builder presetGroupName(String presetGroupName) {
			_presetGroupName = presetGroupName;

			return this;
		}

		private Builder(ImageResource imageResource) {
			if (imageResource == null) {
				throw new IllegalArgumentException("Image resource is null");
			}

			_imageResource = imageResource;
		}

		private HttpServletRequest _httpServletRequest;
		private final ImageResource _imageResource;
		private Boolean _lazy;
		private String _presetGroupName;

	}

	private ResponsiveImageRequest(
		HttpServletRequest httpServletRequest, ImageResource imageResource,
		Boolean lazy, String presetGroupName) {

		_httpServletRequest = httpServletRequest;
		_imageResource = imageResource;
		_lazy = lazy;
		_presetGroupName = presetGroupName;
	}

	private final HttpServletRequest _httpServletRequest;
	private final ImageResource _imageResource;
	private final Boolean _lazy;
	private final String _presetGroupName;

}