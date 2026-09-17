/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

/**
 * Builds a {@link ImageBreakpointVariant}.
 *
 * <pre>
 * ImageBreakpointVariantBuilder.url(
 *     url
 * ).width(
 *     640
 * ).build()
 * </pre>
 *
 * <p>
 * Entered through a static method carrying the one required value, as {@link
 * com.liferay.portal.kernel.util.HashMapBuilder} is entered through its first
 * put. The URL is the one thing a candidate cannot be without; everything else
 * varies by provider, since a CDN generating widths on demand knows the width
 * but not the byte size, while Adaptive Media knows both and also has a
 * configuration entry UUID to use as the identifier.
 * </p>
 *
 * <p>
 * Accumulates values and constructs at {@link #build}, rather than mutating a
 * held instance as builders over Service Builder models do, because the
 * candidate it produces is immutable.
 * </p>
 *
 * @author Daniel Sanz
 */
public class ImageBreakpointVariantBuilder {

	public static ImageBreakpointVariantBuilder url(String url) {
		return new ImageBreakpointVariantBuilder(url);
	}

	public ImageBreakpointVariant build() {
		return new ImageBreakpointVariant(_url, _width);
	}

	public ImageBreakpointVariantBuilder width(Integer width) {
		_width = width;

		return this;
	}

	private ImageBreakpointVariantBuilder(String url) {
		_url = url;
	}

	private final String _url;
	private Integer _width;

}