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
 * <p>
 * Immutable, and built through {@link ResponsiveImageRequestBuilder}.
 * </p>
 *
 * @author Daniel Sanz
 */
public final class ResponsiveImageRequest {

	/**
	 * Returns a request for the given image with no preset and lazy
	 * loading enabled, which reproduces current behavior.
	 *
	 * @param  imageResource the image to transform
	 * @return the request
	 */
	public static ResponsiveImageRequest of(ImageResource imageResource) {
		return ResponsiveImageRequestBuilder.imageResource(
			imageResource
		).build();
	}

	/**
	 * Prefer {@link ResponsiveImageRequestBuilder}, which names the optional
	 * arguments. This is public only because Liferay's coding standards have no
	 * way to spell a package private constructor, and the builder has to reach
	 * it from its own compilation unit.
	 *
	 * @param groupId the site being rendered for, or <code>0</code>
	 * @param httpServletRequest the request being served, or <code>null</code>
	 * @param imageResource the image to transform
	 * @param lazy whether to lazily load, or <code>null</code> to defer to the
	 *        preset
	 * @param presetName the preset name, or <code>null</code> for the default
	 */
	public ResponsiveImageRequest(
		long groupId, HttpServletRequest httpServletRequest,
		ImageResource imageResource, Boolean lazy, String presetName) {

		_groupId = groupId;
		_httpServletRequest = httpServletRequest;
		_imageResource = imageResource;
		_lazy = lazy;
		_presetName = presetName;
	}

	/**
	 * Returns the site the caller declared, or <code>0</code> to have one
	 * derived.
	 *
	 * <p>
	 * What was declared, not what will be used. Left at <code>0</code> the
	 * framework takes the rendering site from the theme display of {@link
	 * #getHttpServletRequest}, then from the ambient service context, and
	 * failing both resolves the configuration at company scope. So
	 * <code>0</code> means "work it out", not "no site".
	 * </p>
	 *
	 * <p>
	 * Worth declaring only when there is no request to derive it from, which is
	 * the case for export and import, staging, and scheduled work. A declared
	 * site wins over the derived one, so a caller that sets it is asserting it
	 * knows better than the page being served.
	 * </p>
	 *
	 * <p>
	 * The <em>rendering</em> site, not the site the image is stored in. Presets
	 * describe how a page lays images out, so the same image placed on two
	 * sites must resolve two different sets of presets. Taking it from the
	 * resource instead would read the layout of whichever site happens to own
	 * the file, silently, which for an image in a shared asset library is never
	 * the right answer.
	 * </p>
	 *
	 * <p>
	 * Explicit rather than inferred because most callers cannot know it. The
	 * content transformer chain hands over a string with no context at all, and
	 * there is no ambient group the way there is an ambient company, so a
	 * framework that guessed would be wrong without saying so. Left at
	 * <code>0</code> the configuration falls back to the company, which is what
	 * the group scope cascades to anyway.
	 * </p>
	 *
	 * @return the site ID, or <code>0</code>
	 */
	public long getGroupId() {
		return _groupId;
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
	 * <code>null</code> to use the preset's default.
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
	 *         preset
	 */
	public Boolean getLazy() {
		return _lazy;
	}

	/**
	 * Returns the name of the configured preset describing where this
	 * image sits in the page layout (for example <code>card</code>), or
	 * <code>null</code> to use the default.
	 *
	 * @return the preset name, or <code>null</code>
	 */
	public String getPresetName() {
		return _presetName;
	}

	private final long _groupId;
	private final HttpServletRequest _httpServletRequest;
	private final ImageResource _imageResource;
	private final Boolean _lazy;
	private final String _presetName;

}