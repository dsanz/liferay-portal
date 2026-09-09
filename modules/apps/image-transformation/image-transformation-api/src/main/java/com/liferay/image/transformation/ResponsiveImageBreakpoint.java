/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

import java.util.Collections;
import java.util.List;

/**
 * The variants that apply under one media condition.
 *
 * <p>
 * The middle level of the responsive images model, and the one that makes
 * <code>&lt;picture&gt;</code> expressible:
 * </p>
 *
 * <pre>
 * ResponsiveImage        the image, plus a fallback src
 *   ResponsiveImageBreakpoint    which variants apply, and how wide they render
 *     ResponsiveImageBreakpointVariant       which one the browser picks
 * </pre>
 *
 * <p>
 * Mirrors {@link ImagePresetBreakpoint} on the result side: the preset
 * breakpoint describes what to generate, and this holds what was generated. One
 * is produced from the other by {@link #from}.
 * </p>
 *
 * <p>
 * The level exists because a media condition can never attach to an individual
 * variant; in markup it belongs to a <code>&lt;source&gt;</code>. Both current
 * providers happen to use one axis only (Adaptive Media: many breakpoints of
 * one variant; a CDN: one breakpoint of many), but art direction needs both at
 * once.
 * </p>
 *
 * @author Daniel Sanz
 */
public final class ResponsiveImageBreakpoint {

	/**
	 * Returns a breakpoint carrying the layout of the preset breakpoint it was
	 * generated from.
	 *
	 * <p>
	 * The single place the media condition and sizes cross over from
	 * configuration into output, so a provider cannot drift from the preset it
	 * was asked to honor.
	 * </p>
	 *
	 * <p>
	 * Automatic sizing is applied here, and only when the image is lazily
	 * loaded, because that is the only case a browser honors it. The declared
	 * sizes is kept behind the keyword so that browsers without support still
	 * receive a real value.
	 * </p>
	 *
	 * @param  imagePresetBreakpoint the preset whose transformations produced the
	 *         variants
	 * @param  variants the generated candidates
	 * @param  lazy whether the image is lazily loaded
	 * @return the breakpoint
	 */
	public static ResponsiveImageBreakpoint from(
		ImagePresetBreakpoint imagePresetBreakpoint,
		List<ResponsiveImageBreakpointVariant> variants, boolean lazy) {

		String sizes = imagePresetBreakpoint.getSizes();

		if (lazy && imagePresetBreakpoint.isAutoSizes() && (sizes != null)) {
			sizes = "auto, " + sizes;
		}

		return new ResponsiveImageBreakpoint(
			imagePresetBreakpoint.getMediaQuery(), sizes, variants);
	}

	/**
	 * Returns a breakpoint the provider determined for itself, having ignored any
	 * preset.
	 *
	 * @param  mediaQuery the media condition, or <code>null</code> for the
	 *         unconditional breakpoint
	 * @param  sizes the sizes attribute, or <code>null</code> when this breakpoint
	 *         holds a single candidate and has nothing to disambiguate
	 * @param  variants the candidates
	 * @return the breakpoint
	 */
	public static ResponsiveImageBreakpoint of(
		String mediaQuery, String sizes,
		List<ResponsiveImageBreakpointVariant> variants) {

		return new ResponsiveImageBreakpoint(mediaQuery, sizes, variants);
	}

	/**
	 * Returns the media condition under which this breakpoint applies, or
	 * <code>null</code> if it applies unconditionally.
	 *
	 * <p>
	 * A single unconditional breakpoint renders as a plain <code>&lt;img&gt;</code>;
	 * several render as <code>&lt;picture&gt;</code>. Order is
	 * significant, because source matching is first wins.
	 * </p>
	 *
	 * @return the media condition, or <code>null</code>
	 */
	public String getMediaQuery() {
		return _mediaQuery;
	}

	/**
	 * Returns the <code>sizes</code> attribute for this breakpoint, or
	 * <code>null</code>.
	 *
	 * <p>
	 * Describes how wide the image renders under this breakpoint's media condition,
	 * which is what turns the candidates' width descriptors into a selection.
	 * Meaningless without them, so a breakpoint holding one candidate has none.
	 * </p>
	 *
	 * @return the sizes attribute value, or <code>null</code>
	 */
	public String getSizes() {
		return _sizes;
	}

	/**
	 * Returns this breakpoint's candidates.
	 *
	 * @return the candidates
	 */
	public List<ResponsiveImageBreakpointVariant> getVariants() {
		return _variants;
	}

	private ResponsiveImageBreakpoint(
		String mediaQuery, String sizes,
		List<ResponsiveImageBreakpointVariant> variants) {

		_mediaQuery = mediaQuery;
		_sizes = sizes;
		_variants = Collections.unmodifiableList(variants);
	}

	private final String _mediaQuery;
	private final String _sizes;
	private final List<ResponsiveImageBreakpointVariant> _variants;

}