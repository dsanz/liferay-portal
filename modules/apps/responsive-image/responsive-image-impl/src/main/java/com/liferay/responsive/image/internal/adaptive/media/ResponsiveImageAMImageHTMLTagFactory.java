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
 * resource from {@link ImageResourceFactory} and rendering the original tag
 * when it answers <code>null</code>.
 * </li>
 * <li>Delete this class.</li>
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
		// rather than the original tag, because this factory outranks the
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

		String markup = _responsiveImageMarkupRenderer.render(
			originalImgTag,
			ResponsiveImageRequestBuilder.imageResource(
				_imageResourceFactory.fromFileEntry(fileEntry)
			).httpServletRequest(
				_getHttpServletRequest()
			).build());

		// The framework declined, so Adaptive Media answers exactly as it did
		// before this existed. Its markup carries a data-fileentryid on the
		// <picture> that its own content transformer and its export and import
		// processor both match on, so handing back the original tag here would
		// break them.

		if (markup == null) {
			return _amImageHTMLTagFactory.create(originalImgTag, fileEntry);
		}

		return _markPicture(markup, fileEntry);
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
	 * Marks a <code>&lt;picture&gt;</code> the way Adaptive Media marks its
	 * own.
	 *
	 * <p>
	 * Two Adaptive Media components read the file entry id from the opening
	 * tag: its content transformer uses it as an idempotency guard, matching
	 * the whole <code>&lt;picture data-fileentryid="N"&gt;</code> literal, and
	 * its export and import processor selects on it to regenerate sources on
	 * import. Markup missing it is wrapped a second time and survives an
	 * import still pointing at the source installation.
	 * </p>
	 *
	 * <p>
	 * Here rather than in the renderer because this is Adaptive Media's
	 * private protocol between three of its own classes. The framework has no
	 * business knowing it, and it should disappear with this class.
	 * </p>
	 */
	private String _markPicture(String markup, FileEntry fileEntry) {
		if (!markup.startsWith(_OPEN_TAG_PICTURE)) {
			return markup;
		}

		return StringBundler.concat(
			_OPEN_TAG_PICTURE, StringPool.SPACE,
			AMImageHTMLConstants.ATTRIBUTE_NAME_FILE_ENTRY_ID, "=\"",
			fileEntry.getFileEntryId(), "\"",
			markup.substring(_OPEN_TAG_PICTURE.length()));
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