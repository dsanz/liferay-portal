/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

import java.util.Collections;
import java.util.List;

/**
 * A named group of presets describing one place an image sits in a layout.
 *
 * <p>
 * What a caller asks for: a fragment says it is rendering a <code>card</code>,
 * and configuration supplies one {@link ImagePreset} per breakpoint. Groups
 * exist because <code>sizes</code> and art direction are properties of the
 * placement, not of the image or of the provider serving it, and a hero and a
 * card on the same page need different values.
 * </p>
 *
 * <p>
 * These duplicate the theme's CSS layout and nothing keeps the two in sync,
 * which is the standing hazard of responsive images: a stale group still
 * renders, just at the wrong size, with no error anywhere.
 * </p>
 *
 * @author Daniel Sanz
 */
public final class ImagePresetGroup {

	public ImagePresetGroup(
		String label, String name, List<ImagePreset> presets) {

		_label = label;
		_name = name;
		_presets = Collections.unmodifiableList(presets);
	}

	/**
	 * Returns a human readable name for this group, or <code>null</code>.
	 *
	 * @return the label, or <code>null</code>
	 */
	public String getLabel() {
		return _label;
	}

	/**
	 * Returns the name callers use to request this group.
	 *
	 * @return the name
	 */
	public String getName() {
		return _name;
	}

	/**
	 * Returns this group's presets, in the order they must be rendered.
	 *
	 * <p>
	 * Ordering comes from the order breakpoints are declared, not from the
	 * order presets appear, so it is decided once for the whole installation
	 * rather than per group. Source matching is first wins, and the
	 * unconditional preset always sorts last because it is the catch all.
	 * </p>
	 *
	 * <p>
	 * One preset is the ordinary case and produces a plain
	 * <code>&lt;img&gt;</code>. Several describe art direction and render as
	 * <code>&lt;picture&gt;</code>, which multiplies the number of distinct
	 * objects held at the edge by the number of variant widths.
	 * </p>
	 *
	 * @return the presets
	 */
	public List<ImagePreset> getPresets() {
		return _presets;
	}

	private final String _label;
	private final String _name;
	private final List<ImagePreset> _presets;

}