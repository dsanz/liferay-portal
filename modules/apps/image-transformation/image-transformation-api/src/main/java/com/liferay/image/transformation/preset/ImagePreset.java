/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.preset;

import java.util.Collections;
import java.util.List;

/**
 * A named placement describing one place an image sits in a layout.
 *
 * <p>
 * What a caller asks for: a fragment says it is rendering a <code>card</code>,
 * and configuration supplies one {@link BreakpointPreset} per breakpoint. Presets
 * exist because <code>sizes</code> and art direction are properties of the
 * placement, not of the image or of the provider serving it, and a hero and a
 * card on the same page need different values.
 * </p>
 *
 * <p>
 * These duplicate the theme's CSS layout and nothing keeps the two in sync,
 * which is the standing hazard of responsive images: a stale preset still
 * renders, just at the wrong size, with no error anywhere.
 * </p>
 *
 * @author Daniel Sanz
 */
public final class ImagePreset {

	public ImagePreset(
		String label, Boolean lazy, String name,
		List<BreakpointPreset> presets) {

		_label = label;
		_lazy = lazy;
		_name = name;

		_breakpointPresets = Collections.unmodifiableList(presets);
	}

	/**
	 * Returns this preset's breakpoints, in the order they must be rendered.
	 *
	 * <p>
	 * Ordering comes from the order breakpoints are declared, not from the
	 * order presets appear, so it is decided once for the whole installation
	 * rather than per preset. Source matching is first wins, and the
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
	public List<BreakpointPreset> getBreakpointPresets() {
		return _breakpointPresets;
	}

	/**
	 * Returns a human readable name for this preset, or <code>null</code>.
	 *
	 * @return the label, or <code>null</code>
	 */
	public String getLabel() {
		return _label;
	}

	/**
	 * Returns whether images in this placement are lazily loaded by default, or
	 * <code>null</code> if the placement does not say.
	 *
	 * <p>
	 * On the preset rather than on a breakpoint, because an image is loaded once:
	 * laziness is a property of the image element, not of a media condition.
	 * A caller that knows better overrides it per instance.
	 * </p>
	 *
	 * <p>
	 * Undeclared means eager. Loading eagerly costs bandwidth; loading the
	 * largest contentful image lazily costs a Core Web Vital, so the safer
	 * default is the one that cannot regress it.
	 * </p>
	 *
	 * @return whether to lazily load, or <code>null</code> if undeclared
	 */
	public Boolean getLazy() {
		return _lazy;
	}

	/**
	 * Returns the name callers use to request this preset.
	 *
	 * @return the name
	 */
	public String getName() {
		return _name;
	}

	private final List<BreakpointPreset> _breakpointPresets;
	private final String _label;
	private final Boolean _lazy;
	private final String _name;

}