/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.constants;

/**
 * Holds the constants the responsive image framework exposes to callers.
 *
 * <p>
 * The <code>PRESET_</code> constants name the presets every installation has,
 * whatever is configured.
 * </p>
 *
 * @author Daniel Sanz
 */
public class ResponsiveImageConstants {

	/**
	 * Names the preset used by asset publisher cards, navigation tiles and
	 * data set cards.
	 */
	public static final String PRESET_CARD = "card";

	/**
	 * Names the preset used by images placed inside web content.
	 */
	public static final String PRESET_CONTENT = "content";

	/**
	 * Names the preset a caller gets when it names none.
	 */
	public static final String PRESET_DEFAULT = "default";

	/**
	 * Names the preset used by full bleed banners.
	 */
	public static final String PRESET_FULL_WIDTH = "fullWidth";

	/**
	 * Names the preset used by the leading image of a content page.
	 */
	public static final String PRESET_HERO = "hero";

	/**
	 * Names the preset used by avatars and list thumbnails.
	 */
	public static final String PRESET_THUMBNAIL = "thumbnail";

}