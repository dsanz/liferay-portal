/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal.adaptive.media;

import com.liferay.adaptive.media.image.configuration.AMImageConfigurationEntry;
import com.liferay.adaptive.media.image.configuration.AMImageConfigurationHelper;
import com.liferay.adaptive.media.image.html.AMImageHTMLTagFactory;
import com.liferay.adaptive.media.image.media.query.Condition;
import com.liferay.adaptive.media.image.media.query.MediaQuery;
import com.liferay.adaptive.media.image.media.query.MediaQueryProvider;
import com.liferay.adaptive.media.image.model.AMImageEntry;
import com.liferay.adaptive.media.image.service.AMImageEntryLocalService;
import com.liferay.adaptive.media.image.url.AMImageURLFactory;
import com.liferay.image.transformation.FileEntryImageResource;
import com.liferay.image.transformation.ImageResource;
import com.liferay.image.transformation.ImageVariant;
import com.liferay.image.transformation.ImageVariantGroup;
import com.liferay.image.transformation.ResponsiveImage;
import com.liferay.image.transformation.ResponsiveImageRequest;
import com.liferay.image.transformation.spi.ImageTransformationProvider;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Adapts Adaptive Media to the transformation SPI.
 *
 * <p>
 * The only class in this bundle that imports Adaptive Media, and the reason its
 * packages are imported optionally: everything else keeps working if Adaptive
 * Media is absent, and retiring it eventually means deleting this class rather
 * than unpicking it from a dozen call sites.
 * </p>
 *
 * <p>
 * Ignores presets. Its renditions were generated in advance at widths fixed by
 * configuration entries, so it cannot honor a requested crop, and its media
 * conditions come from those entries rather than from a layout description.
 * </p>
 *
 * @author Daniel Sanz
 */
@Component(service = ImageTransformationProvider.class)
public class AMImageTransformationProvider
	implements ImageTransformationProvider {

	public static final String NAME = "adaptive-media";

	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * Returns one group per media condition, each holding the single rendition
	 * that condition selects, falling back to the untransformed original.
	 *
	 * <p>
	 * Joins two Adaptive Media sources: {@code MediaQueryProvider} supplies the
	 * conditions and the URLs they select, while the rendition records supply
	 * the width and byte size that an authoring UI needs in order to let
	 * someone choose between them.
	 * </p>
	 */
	@Override
	public ResponsiveImage getResponsiveImage(
			ResponsiveImageRequest responsiveImageRequest)
		throws PortalException {

		ImageResource imageResource = responsiveImageRequest.getImageResource();

		FileEntryImageResource fileEntryImageResource =
			(FileEntryImageResource)imageResource;

		FileEntry fileEntry = fileEntryImageResource.getFileEntry();

		List<MediaQuery> mediaQueries = _mediaQueryProvider.getMediaQueries(
			fileEntry);

		if (ListUtil.isEmpty(mediaQueries)) {
			return ResponsiveImage.passthrough(imageResource.getURL());
		}

		Map<String, ImageVariant> imageVariants = _getImageVariants(fileEntry);

		List<ImageVariantGroup> imageVariantGroups = new ArrayList<>(
			mediaQueries.size());

		for (MediaQuery mediaQuery : mediaQueries) {
			String mediaQueryString = _getMediaQueryString(mediaQuery);

			if (mediaQueryString == null) {
				continue;
			}

			List<ImageVariant> groupImageVariants = new ArrayList<>();

			for (String url :
					StringUtil.split(mediaQuery.getSrc(), StringPool.COMMA)) {

				url = StringUtil.trim(url);

				ImageVariant imageVariant = imageVariants.get(url);

				if (imageVariant == null) {

					// The URL is known but its rendition record is not, so emit
					// what is certain and leave the rest null rather than drop
					// a candidate the browser could have used.

					imageVariant = ImageVariant.builder(
						url
					).build();
				}

				groupImageVariants.add(imageVariant);
			}

			if (groupImageVariants.isEmpty()) {
				continue;
			}

			// No sizes: a group holding one candidate has nothing to
			// disambiguate.

			imageVariantGroups.add(
				ImageVariantGroup.of(
					mediaQueryString, null, groupImageVariants));
		}

		// The untransformed original as fallback: Adaptive Media's renditions
		// are pregenerated at fixed widths, and picking one of them as the
		// default would silently prefer a size nobody asked for.

		return new ResponsiveImage(imageVariantGroups, imageResource.getURL());
	}

	@Override
	public boolean isTransformable(ImageResource imageResource) {
		return imageResource instanceof FileEntryImageResource;
	}

	/**
	 * Delegates to Adaptive Media's own renderer.
	 *
	 * <p>
	 * Reuse rather than reimplementation: Adaptive Media already knows how to
	 * build its <code>&lt;picture&gt;</code> element, and a second copy would
	 * drift from the original the first time either changed.
	 * </p>
	 */
	@Override
	public String render(
			String originalImgTag,
			ResponsiveImageRequest responsiveImageRequest)
		throws PortalException {

		ImageResource imageResource = responsiveImageRequest.getImageResource();

		if (!(imageResource instanceof FileEntryImageResource)) {
			return originalImgTag;
		}

		FileEntryImageResource fileEntryImageResource =
			(FileEntryImageResource)imageResource;

		return _amImageHTMLTagFactory.create(
			originalImgTag, fileEntryImageResource.getFileEntry());
	}

	private Map<String, ImageVariant> _getImageVariants(FileEntry fileEntry)
		throws PortalException {

		Map<String, ImageVariant> imageVariants = new HashMap<>();

		FileVersion fileVersion = fileEntry.getFileVersion();

		List<AMImageEntry> amImageEntries =
			_amImageEntryLocalService.getAMImageEntries(
				fileVersion.getFileVersionId());

		for (AMImageEntry amImageEntry : amImageEntries) {
			AMImageConfigurationEntry amImageConfigurationEntry =
				_amImageConfigurationHelper.getAMImageConfigurationEntry(
					fileEntry.getCompanyId(),
					amImageEntry.getConfigurationUuid());

			if (amImageConfigurationEntry == null) {
				continue;
			}

			String url = String.valueOf(
				_amImageURLFactory.createFileEntryURL(
					fileVersion, amImageConfigurationEntry));

			imageVariants.put(
				url,
				ImageVariant.builder(
					url
				).identifier(
					amImageEntry.getConfigurationUuid()
				).label(
					amImageConfigurationEntry.getName()
				).size(
					amImageEntry.getSize()
				).width(
					amImageEntry.getWidth()
				).build());
		}

		return imageVariants;
	}

	private String _getMediaQueryString(MediaQuery mediaQuery) {
		List<Condition> conditions = mediaQuery.getConditions();

		if (ListUtil.isEmpty(conditions)) {
			return null;
		}

		String[] conditionStrings = new String[conditions.size()];

		for (int i = 0; i < conditionStrings.length; i++) {
			Condition condition = conditions.get(i);

			conditionStrings[i] = StringBundler.concat(
				StringPool.OPEN_PARENTHESIS, condition.getAttribute(),
				StringPool.COLON, condition.getValue(),
				StringPool.CLOSE_PARENTHESIS);
		}

		return StringUtil.merge(conditionStrings, " and ");
	}

	@Reference
	private AMImageConfigurationHelper _amImageConfigurationHelper;

	@Reference
	private AMImageEntryLocalService _amImageEntryLocalService;

	/**
	 * Targets Adaptive Media's own implementation explicitly.
	 *
	 * <p>
	 * {@link AMImageHTMLTagFactoryShim} also implements this interface, at a
	 * higher service ranking, and delegates back into the umbrella. Binding it
	 * here would recurse until the stack ran out. The filter selects any
	 * implementation that is not the shim, without depending on Adaptive
	 * Media's internal component names, and should be deleted along with the
	 * shim once its callers have migrated.
	 * </p>
	 */
	@Reference(
		target = "(!(" + AMImageHTMLTagFactoryShim.PROPERTY_SHIM + "=true))"
	)
	private AMImageHTMLTagFactory _amImageHTMLTagFactory;

	@Reference
	private AMImageURLFactory _amImageURLFactory;

	@Reference
	private MediaQueryProvider _mediaQueryProvider;

}