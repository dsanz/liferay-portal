/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal.adaptive.media;

import com.liferay.adaptive.media.image.html.AMImageHTMLTagFactory;
import com.liferay.adaptive.media.image.html.constants.AMImageHTMLConstants;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.feature.flag.FeatureFlagManagerUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.responsive.image.ImageResourceFactory;
import com.liferay.responsive.image.ResponsiveImageMarkupRenderer;
import com.liferay.responsive.image.ResponsiveImageRequest;

import jakarta.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Routes existing Adaptive Media tag factory callers through the umbrella
 *
 * <p>
 * Registered above Adaptive Media's own implementation so that the consumers
 * already calling {@code AMImageHTMLTagFactory} (content transformers, export
 * and import, the taglib, several commerce classes) pick up whichever is
 * configured without being modified.
 * </p>
 *
 * @author Daniel Sanz
 */
@Component(
	property = "service.ranking:Integer=100",
	service = AMImageHTMLTagFactory.class
)
public class ResponsiveImageAMImageHTMLTagFactory
	implements AMImageHTMLTagFactory {

	@Override
	public String create(String originalImgTag, FileEntry fileEntry)
		throws PortalException {

		if (!FeatureFlagManagerUtil.isEnabled(
				fileEntry.getCompanyId(), "LPD-94784")) {

			return _amImageHTMLTagFactory.create(originalImgTag, fileEntry);
		}

		String markup = _responsiveImageMarkupRenderer.render(
			originalImgTag,
			ResponsiveImageRequest.builder(
				_imageResourceFactory.fromFileEntry(fileEntry)
			).httpServletRequest(
				_getHttpServletRequest()
			).build());

		if (markup == null) {
			return _amImageHTMLTagFactory.create(originalImgTag, fileEntry);
		}

		return _addFileEntryIdAttribute(markup, fileEntry);
	}

	private String _addFileEntryIdAttribute(
		String markup, FileEntry fileEntry) {

		if (!markup.startsWith(_OPEN_TAG_PICTURE)) {
			return markup;
		}

		return StringBundler.concat(
			_OPEN_TAG_PICTURE, StringPool.SPACE,
			AMImageHTMLConstants.ATTRIBUTE_NAME_FILE_ENTRY_ID, "=\"",
			fileEntry.getFileEntryId(), "\"",
			markup.substring(_OPEN_TAG_PICTURE.length()));
	}

	private HttpServletRequest _getHttpServletRequest() {
		ServiceContext serviceContext =
			ServiceContextThreadLocal.getServiceContext();

		if (serviceContext == null) {
			return null;
		}

		return serviceContext.getRequest();
	}

	private static final String _OPEN_TAG_PICTURE = "<picture";

	@Reference(
		target = "(!(component.name=com.liferay.responsive.image.internal.adaptive.media.ResponsiveImageAMImageHTMLTagFactory))"
	)
	private AMImageHTMLTagFactory _amImageHTMLTagFactory;

	@Reference
	private ImageResourceFactory _imageResourceFactory;

	@Reference
	private ResponsiveImageMarkupRenderer _responsiveImageMarkupRenderer;

}