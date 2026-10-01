/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image;

import java.util.Collections;
import java.util.List;

/**
 * Holds the candidates that apply under one media condition, together with the
 * condition itself.
 *
 * <p>
 * The middle level of the responsive images model, and the one that makes
 * <code>&lt;picture&gt;</code> expressible:
 * </p>
 *
 * <pre>
 * ResponsiveImage  the image, plus a fallback src
 *   ResponsiveImageSource  which candidates apply, and how wide they render
 *     ResponsiveImageCandidate  which one the browser picks
 * </pre>
 *
 * <p>
 * Mirrors a configured source definition on the result side: the definition
 * describes what to generate, and this holds what was generated.
 * </p>
 *
 * @author Daniel Sanz
 */
public final class ResponsiveImageSource {

	/**
	 * Returns a builder for a source.
	 *
	 * @return the builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Returns this source's candidates.
	 *
	 * @return the candidates
	 */
	public List<ResponsiveImageCandidate> getCandidates() {
		return _candidates;
	}

	/**
	 * Returns the media condition under which this source applies, or
	 * <code>null</code> if it applies unconditionally.
	 *
	 * @return the media condition, or <code>null</code>
	 */
	public String getMediaQuery() {
		return _mediaQuery;
	}

	/**
	 * Returns the <code>sizes</code> attribute for this source, or
	 * <code>null</code>.
	 *
	 * <p>
	 * Describes how wide the image renders under this source's media condition,
	 * which is what turns the candidates' width descriptors into a selection.
	 * It is meaningless without them, so a source holding one candidate has
	 * none.
	 * </p>
	 *
	 * @return the sizes attribute value, or <code>null</code>
	 */
	public String getSizes() {
		return _sizes;
	}

	public static class Builder {

		public ResponsiveImageSource build() {
			return new ResponsiveImageSource(_candidates, _mediaQuery, _sizes);
		}

		/**
		 * Sets this source's candidates.
		 *
		 * @param  candidates the candidates
		 * @return this builder
		 */
		public Builder candidates(List<ResponsiveImageCandidate> candidates) {
			_candidates = candidates;

			return this;
		}

		/**
		 * Sets the media condition under which this source applies. Leave it
		 * unset for the unconditional source.
		 *
		 * @param  mediaQuery the media condition
		 * @return this builder
		 */
		public Builder mediaQuery(String mediaQuery) {
			_mediaQuery = mediaQuery;

			return this;
		}

		/**
		 * Sets the <code>sizes</code> attribute. Leave it unset when this
		 * source holds a single candidate and has nothing to disambiguate.
		 *
		 * @param  sizes the sizes attribute value
		 * @return this builder
		 */
		public Builder sizes(String sizes) {
			_sizes = sizes;

			return this;
		}

		private Builder() {
		}

		private List<ResponsiveImageCandidate> _candidates =
			Collections.emptyList();
		private String _mediaQuery;
		private String _sizes;

	}

	private ResponsiveImageSource(
		List<ResponsiveImageCandidate> candidates, String mediaQuery,
		String sizes) {

		_candidates = Collections.unmodifiableList(candidates);
		_mediaQuery = mediaQuery;
		_sizes = sizes;
	}

	private final List<ResponsiveImageCandidate> _candidates;
	private final String _mediaQuery;
	private final String _sizes;

}