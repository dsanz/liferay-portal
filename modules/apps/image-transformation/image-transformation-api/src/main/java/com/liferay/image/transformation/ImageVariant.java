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
 * ImageVariantGroup} rather than an individual candidate.
 * </p>
 *
 * <p>
 * A concrete class rather than an interface: an immutable output value with no
 * behavior and no plausible second implementation.
 * </p>
 *
 * @author Daniel Sanz
 */
public final class ImageVariant {

	public static Builder builder(String url) {
		return new Builder(url);
	}

	/**
	 * Returns a stable identifier for this rendition, or <code>null</code>.
	 *
	 * <p>
	 * Adaptive Media uses its configuration entry UUID; a provider generating
	 * widths on demand can use the width. Authoring UIs need something to store
	 * when the author picks a rendition.
	 * </p>
	 *
	 * @return the identifier, or <code>null</code>
	 */
	public String getIdentifier() {
		return _identifier;
	}

	/**
	 * Returns a human readable name for this rendition, or <code>null</code>.
	 *
	 * @return the label, or <code>null</code>
	 */
	public String getLabel() {
		return _label;
	}

	/**
	 * Returns this rendition's mime type, or <code>null</code> if it is the
	 * same as the original.
	 *
	 * @return the mime type, or <code>null</code>
	 */
	public String getMimeType() {
		return _mimeType;
	}

	/**
	 * Returns this rendition's size in bytes, or <code>null</code> if unknown.
	 *
	 * <p>
	 * Unknown is the normal case for a provider that generates renditions on
	 * demand: nothing has been produced yet, and the eventual size depends on
	 * format negotiation. Only providers backed by pregenerated files can
	 * answer this, so any UI that displays it must tolerate its absence rather
	 * than assume a finite catalogue of renditions exists.
	 * </p>
	 *
	 * @return the size in bytes, or <code>null</code>
	 */
	public Long getSize() {
		return _size;
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

	public static final class Builder {

		public ImageVariant build() {
			return new ImageVariant(
				_identifier, _label, _mimeType, _size, _url, _width);
		}

		public Builder identifier(String identifier) {
			_identifier = identifier;

			return this;
		}

		public Builder label(String label) {
			_label = label;

			return this;
		}

		public Builder mimeType(String mimeType) {
			_mimeType = mimeType;

			return this;
		}

		public Builder size(Long size) {
			_size = size;

			return this;
		}

		public Builder width(Integer width) {
			_width = width;

			return this;
		}

		private Builder(String url) {
			_url = url;
		}

		private String _identifier;
		private String _label;
		private String _mimeType;
		private Long _size;
		private final String _url;
		private Integer _width;

	}

	private ImageVariant(
		String identifier, String label, String mimeType, Long size, String url,
		Integer width) {

		_identifier = identifier;
		_label = label;
		_mimeType = mimeType;
		_size = size;
		_url = url;
		_width = width;
	}

	private final String _identifier;
	private final String _label;
	private final String _mimeType;
	private final Long _size;
	private final String _url;
	private final Integer _width;

}