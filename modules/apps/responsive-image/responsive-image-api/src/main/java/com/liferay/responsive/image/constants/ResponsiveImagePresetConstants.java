/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.constants;

/**
 * Lists the responsive image presets every installation has, whatever is
 * configured.
 *
 *
 * @author Daniel Sanz
 */
public class ResponsiveImagePresetConstants {

	/**
	 * Names the preset used by asset publisher cards, navigation tiles and
	 * data set cards.
	 */
	public static final String CARD = "card";

	/**
	 * Names the preset used by images placed inside web content.
	 */
	public static final String CONTENT = "content";

	/**
	 * Names the preset a caller gets when it names none.
	 */
	public static final String DEFAULT = "default";

	/**
	 * Names the preset used by full bleed banners.
	 */
	public static final String FULL_WIDTH = "fullWidth";

	/**
	 * Names the preset used by the leading image of a content page.
	 */
	public static final String HERO = "hero";

	/**
	 * Names the preset used by avatars and list thumbnails.
	 */
	public static final String THUMBNAIL = "thumbnail";

}