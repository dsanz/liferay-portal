/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

/**
 * Builds a {@link ResponsiveImageBreakpointVariant}.
 *
 * <pre>
 * ResponsiveImageBreakpointVariantBuilder.url(
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
public class ResponsiveImageBreakpointVariantBuilder {

	public static ResponsiveImageBreakpointVariantBuilder url(String url) {
		return new ResponsiveImageBreakpointVariantBuilder(url);
	}

	public ResponsiveImageBreakpointVariant build() {
		return new ResponsiveImageBreakpointVariant(
			_identifier, _label, _mimeType, _size, _url, _width);
	}

	public ResponsiveImageBreakpointVariantBuilder identifier(
		String identifier) {

		_identifier = identifier;

		return this;
	}

	public ResponsiveImageBreakpointVariantBuilder label(String label) {
		_label = label;

		return this;
	}

	public ResponsiveImageBreakpointVariantBuilder mimeType(String mimeType) {
		_mimeType = mimeType;

		return this;
	}

	public ResponsiveImageBreakpointVariantBuilder size(Long size) {
		_size = size;

		return this;
	}

	public ResponsiveImageBreakpointVariantBuilder width(Integer width) {
		_width = width;

		return this;
	}

	private ResponsiveImageBreakpointVariantBuilder(String url) {
		_url = url;
	}

	private String _identifier;
	private String _label;
	private String _mimeType;
	private Long _size;
	private final String _url;
	private Integer _width;

}