/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal;

import com.liferay.document.library.util.DLURLHelper;
import com.liferay.image.transformation.FileEntryImageResource;
import com.liferay.image.transformation.ImageResource;
import com.liferay.image.transformation.ImageResourceFactory;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.theme.ThemeDisplay;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(service = ImageResourceFactory.class)
public class ImageResourceFactoryImpl implements ImageResourceFactory {

	@Override
	public FileEntryImageResource fromFileEntry(FileEntry fileEntry)
		throws PortalException {

		return new FileEntryImageResourceImpl(fileEntry, _getURL(fileEntry));
	}

	@Override
	public ImageResource fromURL(String url, String mimeType) {
		return new ImageResourceImpl(mimeType, url);
	}

	private ThemeDisplay _getThemeDisplay() {
		ServiceContext serviceContext =
			ServiceContextThreadLocal.getServiceContext();

		if (serviceContext == null) {
			return null;
		}

		return serviceContext.getThemeDisplay();
	}

	/**
	 * Returns the URL of the <b>original</b> file, not of a pregenerated
	 * derivative.
	 *
	 * <p>
	 * This matters: an image optimization provider asked to resize an
	 * already-resized image resamples twice, losing quality for no benefit, and
	 * leaves the derivative pipeline doing work nobody consumes.
	 * </p>
	 */
	private String _getURL(FileEntry fileEntry) throws PortalException {
		return _dlURLHelper.getPreviewURL(
			fileEntry, fileEntry.getFileVersion(), _getThemeDisplay(),
			StringPool.BLANK, false, false);
	}

	@Reference
	private DLURLHelper _dlURLHelper;

	private static class FileEntryImageResourceImpl
		extends ImageResourceImpl implements FileEntryImageResource {

		public FileEntryImageResourceImpl(FileEntry fileEntry, String url) {
			super(fileEntry.getMimeType(), url);

			_fileEntry = fileEntry;
		}

		@Override
		public FileEntry getFileEntry() {
			return _fileEntry;
		}

		private final FileEntry _fileEntry;

	}

	private static class ImageResourceImpl implements ImageResource {

		public ImageResourceImpl(String mimeType, String url) {
			_mimeType = mimeType;
			_url = url;
		}

		@Override
		public String getMimeType() {
			return _mimeType;
		}

		@Override
		public String getURL() {
			return _url;
		}

		private final String _mimeType;
		private final String _url;

	}

}