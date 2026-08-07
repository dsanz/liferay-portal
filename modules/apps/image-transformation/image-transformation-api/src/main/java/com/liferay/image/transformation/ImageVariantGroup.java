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
 *   ImageVariantGroup    which variants apply, and how wide they render
 *     ImageVariant       which one the browser picks
 * </pre>
 *
 * <p>
 * Mirrors {@link ImagePresetGroup} on the output side: a group of presets
 * describes what to generate per breakpoint, and a group of variants holds what
 * was generated. One is produced from the other by {@link #from}.
 * </p>
 *
 * <p>
 * The level exists because a media condition can never attach to an individual
 * variant; in markup it belongs to a <code>&lt;source&gt;</code>. Both current
 * providers happen to use one axis only (Adaptive Media: many groups of one
 * variant; a CDN: one group of many), but art direction needs both at once.
 * </p>
 *
 * @author Daniel Sanz
 */
public final class ImageVariantGroup {

	/**
	 * Returns a group carrying the layout of the preset it was generated from.
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
	 * @param  imagePreset the preset whose transformations produced the
	 *         variants
	 * @param  variants the generated candidates
	 * @param  lazy whether the image is lazily loaded
	 * @return the group
	 */
	public static ImageVariantGroup from(
		ImagePreset imagePreset, List<ImageVariant> variants, boolean lazy) {

		String sizes = imagePreset.getSizes();

		if (lazy && imagePreset.isAutoSizes() && (sizes != null)) {
			sizes = "auto, " + sizes;
		}

		return new ImageVariantGroup(
			imagePreset.getMediaQuery(), sizes, variants);
	}

	/**
	 * Returns a group the provider determined for itself, having ignored any
	 * preset.
	 *
	 * @param  mediaQuery the media condition, or <code>null</code> for the
	 *         unconditional group
	 * @param  sizes the sizes attribute, or <code>null</code> when this group
	 *         holds a single candidate and has nothing to disambiguate
	 * @param  variants the candidates
	 * @return the group
	 */
	public static ImageVariantGroup of(
		String mediaQuery, String sizes, List<ImageVariant> variants) {

		return new ImageVariantGroup(mediaQuery, sizes, variants);
	}

	/**
	 * Returns the media condition under which this group applies, or
	 * <code>null</code> if it applies unconditionally.
	 *
	 * <p>
	 * A single unconditional group renders as a plain <code>&lt;img&gt;</code>;
	 * several groups render as <code>&lt;picture&gt;</code>. Order is
	 * significant, because source matching is first wins.
	 * </p>
	 *
	 * @return the media condition, or <code>null</code>
	 */
	public String getMediaQuery() {
		return _mediaQuery;
	}

	/**
	 * Returns the <code>sizes</code> attribute for this group, or
	 * <code>null</code>.
	 *
	 * <p>
	 * Describes how wide the image renders under this group's media condition,
	 * which is what turns the candidates' width descriptors into a selection.
	 * Meaningless without them, so a group of one candidate has none.
	 * </p>
	 *
	 * @return the sizes attribute value, or <code>null</code>
	 */
	public String getSizes() {
		return _sizes;
	}

	/**
	 * Returns this group's candidates.
	 *
	 * @return the candidates
	 */
	public List<ImageVariant> getVariants() {
		return _variants;
	}

	private ImageVariantGroup(
		String mediaQuery, String sizes, List<ImageVariant> variants) {

		_mediaQuery = mediaQuery;
		_sizes = sizes;
		_variants = Collections.unmodifiableList(variants);
	}

	private final String _mediaQuery;
	private final String _sizes;
	private final List<ImageVariant> _variants;

}