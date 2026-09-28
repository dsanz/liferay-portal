/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image;

import java.util.Collections;
import java.util.List;

/**
 * A resolved image, ready to be rendered or serialized in a responsive way.
 *
 * <p>
 * A model rather than a markup string on purpose, because different pieces
 * require different renditions. Content rewriting needs HTML, but the page
 * editor needs JSON and patches attributes onto elements that
 * already exist.
 * </p>
 *
 * <p>
 * The three levels of the result model, and the HTML each renders as:
 * </p>
 *
 * <ul>
 * <li>
 * {@link ResponsiveImage} corresponds to no single term in the HTML
 * specification; it renders as an <code>&lt;img&gt;</code> or a
 * <code>&lt;picture&gt;</code>
 * </li>
 * <li>
 * {@link ResponsiveImageSource} is a source set together with a source size
 * list; it renders as a <code>&lt;source&gt;</code>, as the
 * <code>&lt;img&gt;</code>, or as both
 * </li>
 * <li>
 * {@link ResponsiveImageCandidate} is an image candidate string; it
 * renders as one <code>srcset</code> entry
 * </li>
 * </ul>
 *
 * <p>
 * The names are deliberately not the markup ones. No HTML noun covers "an
 * <code>&lt;img&gt;</code> or a <code>&lt;picture&gt;</code>, whichever this
 * turns out to be", and the middle level renders as a
 * <code>&lt;source&gt;</code>, as the <code>&lt;img&gt;</code>, or as both, so
 * naming it after either element would mislead more often than it helped.
 * Markup vocabulary belongs to the renderer, the layer that knows about tags.
 * </p>
 *
 * <p>
 * Mirrors a configured preset level for level, and deliberately stays separate
 * from it. A preset is one shared object per site, parsed when configuration
 * changes; this is one object per image per request. Adaptive Media also
 * produces these without ever holding a preset, so the result model cannot be
 * the configuration model.
 * </p>
 *
 * @author Daniel Sanz
 */
public final class ResponsiveImage {

	public ResponsiveImage(
		boolean lazy, List<ResponsiveImageSource> responsiveImageSources,
		String src) {

		_lazy = lazy;
		_responsiveImageSources = Collections.unmodifiableList(
			responsiveImageSources);
		_src = src;
	}

	/**
	 * Constructs a result carrying only the untransformed URL, used whenever
	 * resource is not claimed, so that callers always get something renderable.
	 *
	 * @param src the untransformed URL
	 */
	public ResponsiveImage(String src) {
		this(false, Collections.<ResponsiveImageSource>emptyList(), src);
	}

	/**
	 * Returns the sources, or an empty list when no systemr could
	 * support the resource. Empty is not an error: callers render a plain
	 * image tag pointing at {@link #getSrc()}.
	 *
	 * <p>
	 * Order is significant. Source matching is first wins, so a source with a
	 * narrower media condition must precede a broader one.
	 * </p>
	 *
	 * @return the sources
	 */
	public List<ResponsiveImageSource> getSources() {
		return _responsiveImageSources;
	}

	/**
	 * Returns the URL for the fallback <code>src</code> attribute. Always
	 * usable, whether or not any source was produced.
	 *
	 * @return the fallback URL
	 */
	public String getSrc() {
		return _src;
	}

	/**
	 * Returns whether this image should be loaded lazily.
	 *
	 * @return <code>true</code> if this image should be loaded lazily
	 */
	public boolean isLazy() {
		return _lazy;
	}

	private final boolean _lazy;
	private final List<ResponsiveImageSource> _responsiveImageSources;
	private final String _src;

}