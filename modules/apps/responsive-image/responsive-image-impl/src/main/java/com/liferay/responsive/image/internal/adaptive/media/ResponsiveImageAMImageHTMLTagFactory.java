/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal.adaptive.media;

import com.liferay.adaptive.media.image.html.AMImageHTMLTagFactory;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.feature.flag.FeatureFlagManagerUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.responsive.image.ImageResourceFactory;
import com.liferay.responsive.image.ResponsiveImageMarkupRenderer;
import com.liferay.responsive.image.ResponsiveImageRequestBuilder;

import jakarta.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * <b>TEMPORARY.</b> Routes existing Adaptive Media tag factory callers through
 * the umbrella until they are migrated, then must be deleted.
 *
 * <p>
 * Registered above Adaptive Media's own implementation so that the ten
 * consumers already calling {@code AMImageHTMLTagFactory} (content
 * transformers, export and import, the taglib, several commerce classes) pick
 * up whichever provider is configured without being modified. It exists only to
 * decouple "prove the CDN path works" from "touch ten modules", and it is not
 * an intended part of the architecture.
 * </p>
 *
 * <p>
 * <b>Removal checklist</b>, to be done in a single commit once a non Adaptive
 * Media provider has been validated end to end:
 * </p>
 *
 * <ol>
 * <li>
 * Migrate the ten call sites from {@code AMImageHTMLTagFactory#create(String,
 * FileEntry)} to {@link ResponsiveImageMarkupRenderer#render(String,
 * com.liferay.responsive.image.ResponsiveImageRequest)}, obtaining the
 * resource from {@link ImageResourceFactory}.
 * </li>
 * <li>Delete this class.</li>
 * <li>
 * Delete the {@code target} filter on {@link AMResponsiveImageProvider}'s
 * {@code AMImageHTMLTagFactory} reference, which exists only to avoid recursing
 * back through this class.
 * </li>
 * </ol>
 *
 * <p>
 * Leaving it in place indefinitely means a permanent service ranking override on
 * a core Adaptive Media interface, which is precisely the kind of invisible
 * indirection that makes image rendering hard to debug later.
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

		// The second cut, so that an instance with the feature off never enters
		// the framework at all. It must hand back Adaptive Media's own markup
		// rather than the untransformed tag, because this factory outranks the
		// real one: returning the tag would silently drop the <picture> that
		// Adaptive Media would have produced.

		if (!FeatureFlagManagerUtil.isEnabled(
				fileEntry.getCompanyId(), "LPD-94784")) {

			return _amImageHTMLTagFactory.create(originalImgTag, fileEntry);
		}

		// Best effort on the request. This interface has no way to carry one,
		// and the content transformer chain that calls it has none either, so
		// most of the time there is nothing to find. Absent it, the company
		// comes from the ambient one and the CDN host is resolved per company.

		return _responsiveImageMarkupRenderer.render(
			originalImgTag,
			ResponsiveImageRequestBuilder.imageResource(
				_imageResourceFactory.fromFileEntry(fileEntry)
			).httpServletRequest(
				_getHttpServletRequest()
			).build());
	}

	private HttpServletRequest _getHttpServletRequest() {
		ServiceContext serviceContext =
			ServiceContextThreadLocal.getServiceContext();

		if (serviceContext == null) {
			return null;
		}

		return serviceContext.getRequest();
	}

	/**
	 * Binds Adaptive Media's own implementation, by excluding this one.
	 *
	 * <p>
	 * Selecting on {@code component.name} keeps the exclusion to the single
	 * component it is about, this one, and needs no marker property on it. The
	 * name is this class, so nothing here depends on how Adaptive Media names
	 * its internals.
	 * </p>
	 */
	@Reference(
		target = "(!(component.name=com.liferay.responsive.image.internal.adaptive.media.ResponsiveImageAMImageHTMLTagFactory))"
	)
	private AMImageHTMLTagFactory _amImageHTMLTagFactory;

	@Reference
	private ImageResourceFactory _imageResourceFactory;

	@Reference
	private ResponsiveImageMarkupRenderer _responsiveImageMarkupRenderer;

}