/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

/**
 * One generated rendition of an {@link ImageResource}.
 *
 * <p>
 * A candidate, in the responsive images sense: a URL plus enough about the
 * image behind it for a browser to choose, or for an author to choose. It
 * carries no media condition, because a media condition selects a whole {@link
 * ImageBreakpoint} rather than an individual candidate.
 * </p>
 *
 * <p>
 * A concrete class rather than an interface: an immutable output value with no
 * behavior and no plausible second implementation.
 * </p>
 *
 * <p>
 * Immutable, and built through {@link
 * ImageBreakpointVariantBuilder}.
 * </p>
 *
 * @author Daniel Sanz
 */
public final class ImageBreakpointVariant {

	/**
	 * Prefer {@link ImageBreakpointVariantBuilder}, which names the
	 * optional arguments. This is public only because Liferay's coding standards
	 * have no way to spell a package private constructor, and the builder has to
	 * reach it from its own compilation unit.
	 *
	 * @param url the URL this candidate is fetched from
	 * @param width the width in pixels, or <code>null</code>
	 */
	public ImageBreakpointVariant(String url, Integer width) {
		_url = url;
		_width = width;
	}

	/**
	 * Returns this rendition's URL.
	 *
	 * @return the URL
	 */
	public String getURL() {
		return _url;
	}

	/**
	 * Returns this rendition's width in pixels, or <code>null</code> if
	 * unknown.
	 *
	 * <p>
	 * A fact about the rendition, independent of how a browser selects it. When
	 * rendered into a <code>srcset</code> it becomes the <code>w</code>
	 * descriptor, which the browser trusts without verifying: if it does not
	 * match the width the URL actually serves, selection silently picks wrong.
	 * </p>
	 *
	 * @return the width in pixels, or <code>null</code>
	 */
	public Integer getWidth() {
		return _width;
	}

	private final String _url;
	private final Integer _width;

}