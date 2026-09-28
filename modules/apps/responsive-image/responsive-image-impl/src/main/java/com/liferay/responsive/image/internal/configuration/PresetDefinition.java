/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal.configuration;

import java.util.Collections;
import java.util.List;

/**
 * A configured, named placement describing one place an image sits in a layout.
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
	 * @return this preset's source definitions
	 */
	public List<SourceDefinition> getSourceDefinitions() {
		return _sourceDefinitions;
	}

	/**
	 * Returns whether an image in this placement is lazily loaded.
	 *
	 * <p>
	 * Undeclared means eager. Loading eagerly costs bandwidth; loading the
	 * largest contentful image lazily costs a Core Web Vital, so the safer
	 * default is the one that cannot regress it.
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