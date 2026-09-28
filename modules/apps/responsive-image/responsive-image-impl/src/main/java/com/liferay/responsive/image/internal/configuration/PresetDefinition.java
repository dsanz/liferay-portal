/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal.configuration;

import java.util.Collections;
import java.util.List;

/**
 * A named placement describing one place an image sits in a layout.
 *
 * <p>
 * What a caller asks for: a fragment says it is rendering a <code>card</code>,
 * and configuration supplies one {@link SourceDefinition} per media condition. Presets
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
public final class PresetDefinition {

	public PresetDefinition(
		String label, Boolean lazy, String name,
		List<SourceDefinition> sourceDefinitions) {

		_label = label;
		_lazy = lazy;
		_name = name;

		_sourceDefinitions = Collections.unmodifiableList(sourceDefinitions);
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
	 * Returns the name callers use to request this preset.
	 *
	 * @return the name
	 */
	public String getName() {
		return _name;
	}

	/**
	 * Returns this preset's source definitions, in the order they must be rendered.
	 *
	 * <p>
	 * Ordering comes from the order media conditions are declared, not from the
	 * order presets appear, so it is decided once for the whole installation
	 * rather than per preset. Source matching is first wins, and the
	 * unconditional media condition always sorts last because it is the catch all.
	 * </p>
	 *
	 * <p>
	 * One source definition is the ordinary case and produces a plain
	 * <code>&lt;img&gt;</code>. Several describe art direction and render as
	 * <code>&lt;picture&gt;</code>, which multiplies the number of distinct
	 * objects held at the edge by the number of candidate widths.
	 * </p>
	 *
	 * @return this preset's source definitions
	 */
	public List<SourceDefinition> getSourceDefinitions() {
		return _sourceDefinitions;
	}

	/**
	 * Returns whether an image in this placement is lazily loaded.
	 *
	 * <p>
	 * On the preset rather than on a source, because an image is loaded
	 * once: laziness is a property of the image element, not of a media
	 * condition.
	 * </p>
	 *
	 * <p>
	 * Undeclared means eager. Loading eagerly costs bandwidth; loading the
	 * largest contentful image lazily costs a Core Web Vital, so the safer
	 * default is the one that cannot regress it.
	 * </p>
	 *
	 * <p>
	 * Here rather than in each consumer because two of them need the same
	 * answer for different reasons. Generating renditions needs it because
	 * <code>sizes="auto"</code> is only honored on a lazily loaded image,
	 * and rendering needs it for the <code>loading</code> attribute.
	 * Deriving it twice would let the markup and the sizes it describes
	 * disagree.
	 * </p>
	 *
	 * @return whether to lazily load
	 */
	public boolean isLazy() {
		if (_lazy != null) {
			return _lazy;
		}

		return false;
	}

	private final String _label;
	private final Boolean _lazy;
	private final String _name;
	private final List<SourceDefinition> _sourceDefinitions;

}