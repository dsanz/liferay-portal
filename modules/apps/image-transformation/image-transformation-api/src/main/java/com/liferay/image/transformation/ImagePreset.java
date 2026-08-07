/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

import java.util.Collections;
import java.util.Map;

/**
 * How an image should be generated under one media condition.
 *
 * <p>
 * One entry of an {@link ImagePresetGroup}, and the recipe whose result is an
 * {@link ImageVariantGroup}: the media condition and sizes pass through
 * unchanged, while the transformations are consumed to produce the candidates.
 * {@link ImageVariantGroup#from} performs that crossing.
 * </p>
 *
 * <p>
 * A request, not a command. A provider limited to renditions generated in
 * advance cannot honor an arbitrary crop, and determines its own groups
 * instead.
 * </p>
 *
 * @author Daniel Sanz
 */
public final class ImagePreset {

	public ImagePreset(
		boolean autoSizes, String breakpointName, Integer maxWidth,
		String mediaQuery, String sizes, Map<String, String> transformations) {

		_autoSizes = autoSizes;
		_breakpointName = breakpointName;
		_maxWidth = maxWidth;
		_mediaQuery = mediaQuery;
		_sizes = sizes;
		_transformations = Collections.unmodifiableMap(transformations);
	}

	/**
	 * Returns the name of the breakpoint this preset was declared against, for
	 * diagnostics.
	 *
	 * @return the breakpoint name
	 */
	public String getBreakpointName() {
		return _breakpointName;
	}

	/**
	 * Returns the widest rendition worth generating for this placement, or
	 * <code>null</code> for no limit.
	 *
	 * <p>
	 * A placement that renders small has no use for the widest configured
	 * variant: a 96 pixel thumbnail advertising a 2560 pixel candidate ships
	 * seven URLs of markup for an image the browser will never choose.
	 * </p>
	 *
	 * <p>
	 * A <b>soft</b> limit: the smallest candidate at or above this width is
	 * still included, because that is the one the browser needs, and excluding
	 * it would leave nothing usable. Set it to the rendered width multiplied by
	 * the highest pixel density worth serving.
	 * </p>
	 *
	 * <p>
	 * The only bound on a ladder. Nothing truncates by the original image's
	 * width, so this is what stops a small placement advertising candidates it
	 * will never use.
	 * </p>
	 *
	 * @return the maximum width in pixels, or <code>null</code>
	 */
	public Integer getMaxWidth() {
		return _maxWidth;
	}

	/**
	 * Returns the media condition this preset applies under, resolved from the
	 * named breakpoint, or <code>null</code> for the unconditional preset.
	 *
	 * @return the media condition, or <code>null</code>
	 */
	public String getMediaQuery() {
		return _mediaQuery;
	}

	/**
	 * Returns the <code>sizes</code> attribute describing how wide the image
	 * renders under this condition, or <code>null</code>.
	 *
	 * @return the sizes attribute value, or <code>null</code>
	 */
	public String getSizes() {
		return _sizes;
	}

	/**
	 * Returns the transformations to apply to every candidate generated for
	 * this condition, such as a crop that differs between viewports.
	 *
	 * @return the transformations
	 */
	public Map<String, String> getTransformations() {
		return _transformations;
	}

	/**
	 * Returns <code>true</code> if this condition's width is best determined by
	 * layout rather than declared.
	 *
	 * <p>
	 * An opt in, and only an opt in: it adds the <code>auto</code> keyword in
	 * front of {@link #getSizes()}, which stays mandatory and serves both as
	 * the fallback for browsers without automatic sizing and as the value used
	 * when the image is not lazily loaded. Automatic sizing is only honored on
	 * a lazily loaded image, so this alone never produces
	 * <code>sizes="auto"</code>.
	 * </p>
	 *
	 * @return <code>true</code> if automatic sizing may be used
	 */
	public boolean isAutoSizes() {
		return _autoSizes;
	}

	private final boolean _autoSizes;
	private final String _breakpointName;
	private final Integer _maxWidth;
	private final String _mediaQuery;
	private final String _sizes;
	private final Map<String, String> _transformations;

}