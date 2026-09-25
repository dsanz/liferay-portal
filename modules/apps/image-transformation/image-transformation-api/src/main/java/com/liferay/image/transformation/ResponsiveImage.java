/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

import java.util.Collections;
import java.util.List;

/**
 * A resolved image, ready to be rendered or serialized.
 *
 * <p>
 * A model rather than a markup string on purpose. Content rewriting needs HTML,
 * but the page editor needs JSON and patches attributes onto elements that
 * already exist, and both should go through the same provider.
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
 * {@link ResponsiveImageBreakpoint} is a source set together with a source size
 * list; it renders as a <code>&lt;source&gt;</code>, as the
 * <code>&lt;img&gt;</code>, or as both
 * </li>
 * <li>
 * {@link ResponsiveImageBreakpointVariant} is an image candidate string; it
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
 * from it. A preset is one shared object per company, parsed when configuration
 * changes; this is one object per image per request. Adaptive Media also
 * produces these without ever holding a preset, so the result model cannot be
 * the configuration model.
 * </p>
 *
 * @author Daniel Sanz
 */
public final class ResponsiveImage {

	/**
	 * Returns a result carrying only the untransformed URL, used whenever no
	 * provider claimed the resource so that callers always get something
	 * renderable.
	 *
	 * @param  src the untransformed URL
	 * @return the passthrough result
	 */
	public static ResponsiveImage passthrough(String src) {
		return new ResponsiveImage(
			Collections.<ResponsiveImageBreakpoint>emptyList(), src);
	}

	public ResponsiveImage(
		List<ResponsiveImageBreakpoint> responsiveImageBreakpoints,
		String src) {

		_responsiveImageBreakpoints = Collections.unmodifiableList(
			responsiveImageBreakpoints);
		_src = src;
	}

	/**
	 * Returns the breakpoints, or an empty list when no provider could
	 * transform the resource. Empty is not an error: callers render a plain
	 * image tag pointing at {@link #getSrc()}.
	 *
	 * <p>
	 * Order is significant. Source matching is first wins, so a breakpoint with a
	 * narrower media condition must precede a broader one.
	 * </p>
	 *
	 * @return the breakpoints
	 */
	public List<ResponsiveImageBreakpoint> getBreakpoints() {
		return _responsiveImageBreakpoints;
	}

	/**
	 * Returns the URL for the fallback <code>src</code> attribute. Always
	 * usable, whether or not any breakpoint was produced.
	 *
	 * @return the fallback URL
	 */
	public String getSrc() {
		return _src;
	}

	private final List<ResponsiveImageBreakpoint> _responsiveImageBreakpoints;
	private final String _src;

}