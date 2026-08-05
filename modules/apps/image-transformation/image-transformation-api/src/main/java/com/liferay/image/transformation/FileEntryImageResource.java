/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation;

import com.liferay.portal.kernel.repository.model.FileEntry;

/**
 * An {@link ImageResource} backed by a document library file entry.
 *
 * <p>
 * Providers whose model is file entry keyed (Adaptive Media, for one) accept
 * only this narrower type and decline everything else.
 * </p>
 *
 * @author Daniel Sanz
 */
public interface FileEntryImageResource extends ImageResource {

	/**
	 * Returns the file entry backing this resource.
	 *
	 * @return the file entry
	 */
	public FileEntry getFileEntry();

}