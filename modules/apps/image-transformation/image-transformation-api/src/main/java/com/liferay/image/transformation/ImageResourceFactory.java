/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.repository.model.FileEntry;

/**
 * Creates {@link ImageResource} instances.
 *
 * <p>
 * A service rather than static factory methods because resolving a file entry's
 * URL and intrinsic dimensions needs collaborators that consumers should not
 * have to wire up themselves.
 * </p>
 *
 * @author Daniel Sanz
 */
public interface ImageResourceFactory {

	/**
	 * Returns a resource for a document library image.
	 *
	 * @param  fileEntry the file entry
	 * @return the resource
	 * @throws PortalException if the file entry could not be read
	 */
	public FileEntryImageResource fromFileEntry(FileEntry fileEntry)
		throws PortalException;

	/**
	 * Returns a resource for an image at an arbitrary URL.
	 *
	 * <p>
	 * A URL on a host the CDN does not front cannot be transformed at all, and
	 * providers are expected to decline it.
	 * </p>
	 *
	 * @param  url the image URL
	 * @param  mimeType the image mime type, or <code>null</code> if unknown, in
	 *         which case providers will decline to transform it
	 * @return the resource
	 */
	public ImageResource fromURL(String url, String mimeType);

}