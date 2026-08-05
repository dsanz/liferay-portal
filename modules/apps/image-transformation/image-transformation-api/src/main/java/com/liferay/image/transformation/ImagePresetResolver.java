/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

/**
 * Resolves a preset group name against configuration.
 *
 * <p>
 * Preset groups are framework owned rather than provider owned, so that layout
 * is described once regardless of which provider is serving images. Providers
 * consume this; they do not define it.
 * </p>
 *
 * <p>
 * Configuration is instance scoped, so the same group name can resolve
 * differently for two companies in the same JVM.
 * </p>
 *
 * @author Daniel Sanz
 */
public interface ImagePresetResolver {

	/**
	 * Returns the preset group for the given request.
	 *
	 * <p>
	 * Never returns <code>null</code>. An unknown or unnamed group resolves to
	 * a single unconditional preset, which renders as a plain
	 * <code>&lt;img&gt;</code> spanning the viewport, so a typo in
	 * configuration degrades to a working image rather than to none.
	 * </p>
	 *
	 * @param  companyId the company whose configuration applies, or
	 *         <code>0</code> for the system configuration
	 * @param  presetGroupName the group name, or <code>null</code> for the
	 *         default
	 * @return the preset group
	 */
	public ImagePresetGroup resolve(long companyId, String presetGroupName);

}