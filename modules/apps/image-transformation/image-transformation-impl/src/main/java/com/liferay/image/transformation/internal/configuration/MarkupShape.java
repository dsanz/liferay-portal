/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal.configuration;

/**
 * The element a preset group renders as.
 *
 * <p>
 * Decided by how many media conditions a group declares and by nothing else. A
 * transformation belongs to a preset and a preset is a media condition, so art
 * direction can only ever arrive as several presets; reading the
 * transformations to look for a crop would answer the same question less
 * reliably.
 * </p>
 *
 * @author Daniel Sanz
 */
public enum MarkupShape {

	IMG, PICTURE

}