/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal.configuration;

import java.util.Collections;
import java.util.Map;

/**
 * Describes how an image should be generated under one media condition.
 *
 * <p>
 * One entry of a {@link PresetDefinition}, and the recipe whose result is an
 * {@link ResponsiveImageSource}: the media condition and sizes pass through
 * unchanged, while the transformations are consumed to produce the candidates.
 * </p>
 *
 * @author Daniel Sanz
 */
public final class SourceDefinition {

	public SourceDefinition(
		boolean autoSizes, String mediaConditionName, Integer maxWidth,
		String mediaQuery, String sizes, Map<String, String> transformations) {

		_autoSizes = autoSizes;
		_mediaConditionName = mediaConditionName;
		_maxWidth = maxWidth;
		_mediaQuery = mediaQuery;
		_sizes = sizes;
		_transformations = Collections.unmodifiableMap(transformations);
	}

	/**
	 * Returns the widest rendition worth generating for this placement, or
	 * <code>null</code> for no limit.
	 *
	 * <p>
	 * A placement that renders small has no use for the widest configured
	 * candidate: a 96 pixel thumbnail advertising a 2560 pixel candidate ships
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
	 * @return the maximum width in pixels, or <code>null</code>
	 */
	public Integer getMaxWidth() {
		return _maxWidth;
	}

	/**
	 * Returns the name this source was declared under in the media conditions
	 * configuration, for diagnostics.
	 *
	 * @return the media condition name
	 */
	public String getMediaConditionName() {
		return _mediaConditionName;
	}

	/**
	 * Returns the media condition this source applies under, resolved from
	 * its declaration, or <code>null</code> for the unconditional source.
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
	 * Returns the <code>sizes</code> attribute for an image loaded the given
	 * way, prefixed with the <code>auto</code> keyword when this source
	 * opts in and the image is lazily loaded.
	 *
	 * @param  lazy whether the image is lazily loaded
	 * @return the sizes attribute value, or <code>null</code>
	 */
	public String getSizes(boolean lazy) {
		if (!lazy || !_autoSizes || (_sizes == null)) {
			return _sizes;
		}

		return "auto, " + _sizes;
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
	 *
	 * @return <code>true</code> if automatic sizing may be used
	 */
	public boolean isAutoSizes() {
		return _autoSizes;
	}

	private final boolean _autoSizes;
	private final Integer _maxWidth;
	private final String _mediaConditionName;
	private final String _mediaQuery;
	private final String _sizes;
	private final Map<String, String> _transformations;

}