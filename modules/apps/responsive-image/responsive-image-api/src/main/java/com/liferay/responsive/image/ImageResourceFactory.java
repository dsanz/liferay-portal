/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.repository.model.FileEntry;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @author Daniel Sanz
 */
@ProviderType
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
	 * @param  url the image URL
	 * @param  mimeType the image mime type, or <code>null</code> if unknown
	 * @return the resource
	 */
	public ImageResource fromURL(String url, String mimeType);

}