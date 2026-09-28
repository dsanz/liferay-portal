/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.constants;

/**
 * The presets every installation has, whatever is configured.
 *
 * <p>
 * A component naming one of these can assume it resolves. They are seeded
 * before the configured presets are read, and a list of <code>key=value</code>
 * entries has no way to spell a removal, so configuration can change what they
 * render but cannot take them away.
 * </p>
 *
 * <p>
 * Name a constant rather than the string it holds. The point of publishing them
 * is that a rendering surface asking for a preset and the registry seeding it
 * cannot drift apart.
 * </p>
 *
 * @author Daniel Sanz
 */
public class ResponsiveImagePresetConstants {

	/**
	 * Asset publisher cards, navigation tiles, data set cards.
	 */
	public static final String CARD = "card";

	/**
	 * Images placed inside web content.
	 */
	public static final String CONTENT = "content";

	/**
	 * What a caller that names no preset gets.
	 */
	public static final String DEFAULT = "default";

	/**
	 * Full bleed banners.
	 */
	public static final String FULL_WIDTH = "fullWidth";

	/**
	 * The leading image of a content page.
	 */
	public static final String HERO = "hero";

	/**
	 * Avatars and list thumbnails.
	 */
	public static final String THUMBNAIL = "thumbnail";

}