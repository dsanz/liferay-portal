/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image;

/**
 * Represents the original image a provider generates renditions from,
 * independently of
 * where it is stored.
 *
 * <p>
 * This interface is deliberately not tied to {@code FileEntry}, because the
 * images to render also live in
 * OSGi modules, in the legacy portal image path, or on a completely different
 * host.
 * </p>
 *
 * <p>
 * It deliberately carries nothing about cache freshness either. How long a
 * generated rendition may be held is decided by the <code>Cache-Control</code>
 * the origin returns and by the CDN's own invalidation, both of which are
 * configured elsewhere. Encoding a version into the URL here would be cache
 * policy smuggled in as a transformation parameter.
 * </p>
 *
 * <p>
 * It deliberately carries no intrinsic dimensions, as they are not cheaply
 * obtainable in general.
 * </p>
 *
 * @author Daniel Sanz
 */
public interface ImageResource {

	/**
	 * Returns the image's mime type, or <code>null</code> if it is not known.
	 *
	 * @return the mime type, or <code>null</code>
	 */
	public String getMimeType();

	/**
	 * Returns the URL the image is served from today, before any
	 * transformation. May be portal relative or absolute.
	 *
	 * @return the untransformed URL
	 */
	public String getURL();

}