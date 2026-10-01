/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image;

/**
 * One generated rendition of an {@link ImageResource}.
 *
 * <p>
 * A candidate, in the responsive images sense: a URL plus enough about the
 * image behind it for a browser to choose, or for an author to choose. It
 * carries no media condition, because a media condition selects a whole {@link
 * ResponsiveImageSource} rather than an individual candidate.
 * </p>
 *
 * @author Daniel Sanz
 */
public final class ResponsiveImageCandidate {

	/**
	 * Returns a builder for a candidate at the given URL.
	 *
	 * @param  url the rendition's URL
	 * @return the builder
	 */
	public static Builder builder(String url) {
		return new Builder(url);
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
	 * @return the width in pixels, or <code>null</code>
	 */
	public Integer getWidth() {
		return _width;
	}

	public static class Builder {

		public ResponsiveImageCandidate build() {
			return new ResponsiveImageCandidate(_url, _width);
		}

		public Builder width(Integer width) {
			_width = width;

			return this;
		}

		private Builder(String url) {
			_url = url;
		}

		private final String _url;
		private Integer _width;

	}

	private ResponsiveImageCandidate(String url, Integer width) {
		_url = url;
		_width = width;
	}

	private final String _url;
	private final Integer _width;

}