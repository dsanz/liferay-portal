/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image;

import java.util.Collections;
import java.util.List;

/**
 * The candidates that apply under one media condition, and the condition itself.
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
	 * Returns a source, having ignored any preset.
	 *
	 * @param  mediaQuery the media condition, or <code>null</code> for the
	 *         unconditional source
	 * @param  sizes the sizes attribute, or <code>null</code> when this source
	 *         holds a single candidate and has nothing to disambiguate
	 * @param  candidates the candidates
	 * @return the source
	 */
	public static ResponsiveImageSource of(
		String mediaQuery, String sizes,
		List<ResponsiveImageCandidate> candidates) {

		return new ResponsiveImageSource(mediaQuery, sizes, candidates);
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
	 * Meaningless without them, so a source holding one candidate has none.
	 * </p>
	 *
	 * @return the sizes attribute value, or <code>null</code>
	 */
	public String getSizes() {
		return _sizes;
	}

	private ResponsiveImageSource(
		String mediaQuery, String sizes,
		List<ResponsiveImageCandidate> candidates) {

		_mediaQuery = mediaQuery;
		_sizes = sizes;
		_candidates = Collections.unmodifiableList(candidates);
	}

	private final List<ResponsiveImageCandidate> _candidates;
	private final String _mediaQuery;
	private final String _sizes;

}