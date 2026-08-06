/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal.cdn;

import com.liferay.image.transformation.ImagePreset;
import com.liferay.image.transformation.ImagePresetGroup;
import com.liferay.image.transformation.ImagePresetResolver;
import com.liferay.image.transformation.ImageResource;
import com.liferay.image.transformation.ImageVariant;
import com.liferay.image.transformation.ImageVariantGroup;
import com.liferay.image.transformation.ResponsiveImage;
import com.liferay.image.transformation.ResponsiveImageRequest;
import com.liferay.image.transformation.internal.ImageTransformationFactory;
import com.liferay.image.transformation.internal.configuration.ImageTransformationConfiguration;
import com.liferay.image.transformation.internal.configuration.ImageTransformationConfigurationHelper;
import com.liferay.image.transformation.spi.ImageTransformationProvider;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.url.builder.AbsolutePortalURLBuilder;
import com.liferay.portal.url.builder.AbsolutePortalURLBuilderFactory;
import com.liferay.portal.url.builder.TransformedImageAbsolutePortalURLBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Generates renditions by asking an image optimization provider for arbitrary
 * widths on demand.
 *
 * <p>
 * Provider agnostic on purpose. It owns the variant ladder, the preset, and the
 * markup, and delegates only the URL vocabulary to the registered {@link
 * ImageTransformationURLRenderer}, so supporting another CDN is one small
 * renderer rather than another copy of this class.
 * </p>
 *
 * @author Daniel Sanz
 */
@Component(service = ImageTransformationProvider.class)
public class CDNImageTransformationProvider
	implements ImageTransformationProvider {

	public static final String NAME = "cdn";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public ResponsiveImage getResponsiveImage(
		ResponsiveImageRequest responsiveImageRequest) {

		long companyId = _imageTransformationConfigurationHelper.getCompanyId(
			responsiveImageRequest);

		ImagePresetGroup imagePresetGroup = _imagePresetResolver.resolve(
			companyId, responsiveImageRequest.getPresetGroupName());

		CompanySettings companySettings = _getCompanySettings(companyId);

		List<ImagePreset> imagePresets = imagePresetGroup.getPresets();

		List<ImageVariantGroup> imageVariantGroups = new ArrayList<>(
			imagePresets.size());

		for (ImagePreset imagePreset : imagePresets) {
			List<Integer> widths = _getWidths(companySettings, imagePreset);

			if (widths.isEmpty()) {
				continue;
			}

			imageVariantGroups.add(
				ImageVariantGroup.from(
					imagePreset,
					_getImageVariants(
						companySettings, imagePreset, responsiveImageRequest,
						widths)));
		}

		if (!_isTransformed(imageVariantGroups)) {

			// No renderer is bound, so every width built the same URL. Emitting
			// them would advertise one image at seven different widths, and the
			// browser would trust the descriptors and render at the wrong
			// density. An untransformed image is the honest answer.

			if (_log.isWarnEnabled()) {
				_log.warn(
					StringBundler.concat(
						"No image transformation URL renderer named \"",
						companySettings._rendererName,
						"\" is deployed, serving untransformed images"));
			}

			return ResponsiveImage.passthrough(
				responsiveImageRequest.getImageResource(
				).getURL());
		}

		// Still the untransformed original. Now that the provider owns this
		// choice it could point at a middle rendition instead, which would be
		// a kinder default for a browser ignoring srcset, but that is a
		// behavior change and not part of moving the method.

		return new ResponsiveImage(
			imageVariantGroups,
			responsiveImageRequest.getImageResource(
			).getURL());
	}

	@Override
	public boolean isTransformable(ImageResource imageResource) {
		if (imageResource == null) {
			return false;
		}

		String mimeType = imageResource.getMimeType();

		if (Validator.isBlank(mimeType) || !mimeType.startsWith("image/") ||
			_excludedMimeTypes.contains(mimeType)) {

			return false;
		}

		String url = imageResource.getURL();

		// An image the CDN does not front never reaches the optimizer, so
		// appending parameters to it would change the URL without changing the
		// response. Decline rather than emit a srcset of identical images.

		if (Validator.isBlank(url) || !url.startsWith(StringPool.SLASH)) {
			return false;
		}

		return true;
	}

	/**
	 * Renders a single <code>&lt;img&gt;</code> when the preset has one group,
	 * and <code>&lt;picture&gt;</code> when it describes art direction.
	 *
	 * <p>
	 * Wrapping a lone source in <code>&lt;picture&gt;</code> would be pure
	 * overhead, and enumerating breakpoints when only resolution varies would
	 * discard what the browser knows about pixel density, network conditions,
	 * and its own cache.
	 * </p>
	 */
	@Override
	public String render(
		String originalImgTag, ResponsiveImageRequest responsiveImageRequest) {

		ResponsiveImage responsiveImage = getResponsiveImage(
			responsiveImageRequest);

		List<ImageVariantGroup> imageVariantGroups =
			responsiveImage.getVariantGroups();

		if (imageVariantGroups.isEmpty()) {
			return originalImgTag;
		}

		if (imageVariantGroups.size() == 1) {
			return _renderImg(
				imageVariantGroups.get(0), originalImgTag,
				responsiveImageRequest);
		}

		return _renderPicture(
			imageVariantGroups, originalImgTag, responsiveImageRequest);
	}

	@Activate
	protected void activate() {
		_imageTransformationConfigurationHelper =
			ImageTransformationFactory.
				createImageTransformationConfigurationHelper(
					_configurationProvider, _portal);
	}

	private String _buildURL(
		CompanySettings companySettings,
		ResponsiveImageRequest responsiveImageRequest, String url,
		Map<String, String> transformations) {

		AbsolutePortalURLBuilder absolutePortalURLBuilder =
			_absolutePortalURLBuilderFactory.getAbsolutePortalURLBuilder(
				responsiveImageRequest.getHttpServletRequest());

		TransformedImageAbsolutePortalURLBuilder
			transformedImageAbsolutePortalURLBuilder =
				absolutePortalURLBuilder.forTransformedImage(
					url
				).rendererName(
					companySettings._rendererName
				);

		for (Map.Entry<String, String> entry : transformations.entrySet()) {
			transformedImageAbsolutePortalURLBuilder.param(
				entry.getKey(), entry.getValue());
		}

		return transformedImageAbsolutePortalURLBuilder.build();
	}

	/**
	 * Returns the parsed settings for a company, reparsing only when the
	 * underlying configuration has actually changed.
	 */
	private CompanySettings _getCompanySettings(long companyId) {
		ImageTransformationConfiguration imageTransformationConfiguration =
			_imageTransformationConfigurationHelper.
				getImageTransformationConfiguration(companyId);

		if (imageTransformationConfiguration == null) {
			return _emptyCompanySettings;
		}

		String[] defaultTransformations =
			imageTransformationConfiguration.defaultTransformations();
		String[] variantWidths =
			imageTransformationConfiguration.variantWidths();

		int contentHash =
			(31 * Arrays.hashCode(defaultTransformations)) +
				Arrays.hashCode(variantWidths);

		CompanySettings companySettings = _companySettings.get(companyId);

		if ((companySettings != null) &&
			(companySettings._contentHash == contentHash)) {

			return companySettings;
		}

		companySettings = new CompanySettings(
			contentHash, _toMap(defaultTransformations),
			imageTransformationConfiguration.rendererName(),
			_toWidths(variantWidths));

		_companySettings.put(companyId, companySettings);

		return companySettings;
	}

	private List<ImageVariant> _getImageVariants(
		CompanySettings companySettings, ImagePreset imagePreset,
		ResponsiveImageRequest responsiveImageRequest, List<Integer> widths) {

		ImageResource imageResource = responsiveImageRequest.getImageResource();

		// Precedence runs from broadest to narrowest: installation wide
		// defaults, then this placement's art direction, then the width.
		// Callers contribute none of it, so every transformation the site
		// issues is visible in configuration.

		Map<String, String> transformations = HashMapBuilder.putAll(
			companySettings._defaultTransformations
		).putAll(
			imagePreset.getTransformations()
		).build();

		List<ImageVariant> imageVariants = new ArrayList<>(widths.size());

		for (Integer width : widths) {
			Map<String, String> widthTransformations = HashMapBuilder.putAll(
				transformations
			).put(
				"width", String.valueOf(width)
			).build();

			imageVariants.add(
				ImageVariant.builder(
					_buildURL(
						companySettings, responsiveImageRequest,
						imageResource.getURL(), widthTransformations)
				).identifier(
					String.valueOf(width)
				).label(
					width + "px"
				).width(
					width
				).build());
		}

		return imageVariants;
	}

	private String _getSrcSet(List<ImageVariant> imageVariants) {
		StringBundler sb = new StringBundler(imageVariants.size() * 4);

		for (ImageVariant imageVariant : imageVariants) {
			if (imageVariant.getWidth() == null) {
				continue;
			}

			if (sb.index() > 0) {
				sb.append(StringPool.COMMA_AND_SPACE);
			}

			sb.append(imageVariant.getURL());
			sb.append(StringPool.SPACE);
			sb.append(imageVariant.getWidth());
			sb.append("w");
		}

		return sb.toString();
	}

	/**
	 * Returns the widths worth generating, bounded by the placement's maximum.
	 *
	 * <p>
	 * A <b>soft</b> bound, and the distinction matters: the smallest width at
	 * or above the maximum is still emitted, because that is the one the
	 * browser needs. Filtering strictly below it would leave a srcset with
	 * nothing usable in it.
	 * </p>
	 *
	 * <p>
	 * Nothing bounds this by the original's width. Doing so would cost several
	 * metadata queries per image and would forfeit resolution rather than
	 * protect it: with upscaling disabled, a variant wider than the original
	 * returns the original, which is the best the source can give.
	 * </p>
	 */
	private List<Integer> _getWidths(
		CompanySettings companySettings, ImagePreset imagePreset) {

		Integer maxWidth = imagePreset.getMaxWidth();

		List<Integer> widths = new ArrayList<>(
			companySettings._variantWidths.size());

		for (Integer width : companySettings._variantWidths) {
			widths.add(width);

			if ((maxWidth != null) && (width >= maxWidth)) {
				break;
			}
		}

		return widths;
	}

	private String _injectAttributes(String imgTag, String attributes) {
		int i = imgTag.indexOf("<img");

		if (i == -1) {
			return imgTag;
		}

		return StringBundler.concat(
			imgTag.substring(0, i + 4), StringPool.SPACE, attributes,
			imgTag.substring(i + 4));
	}

	/**
	 * Returns <code>true</code> if building actually changed anything.
	 *
	 * <p>
	 * Two variants in one group differ only by width, so identical URLs mean
	 * the width never reached the URL. Comparing within a group rather than
	 * against the original is what makes this work regardless of the CDN host
	 * and proxy path the builder prepends.
	 * </p>
	 */
	private boolean _isTransformed(List<ImageVariantGroup> imageVariantGroups) {
		for (ImageVariantGroup imageVariantGroup : imageVariantGroups) {
			List<ImageVariant> imageVariants = imageVariantGroup.getVariants();

			if (imageVariants.size() < 2) {
				continue;
			}

			ImageVariant firstImageVariant = imageVariants.get(0);
			ImageVariant secondImageVariant = imageVariants.get(1);

			return !Objects.equals(
				firstImageVariant.getURL(), secondImageVariant.getURL());
		}

		return true;
	}

	private String _renderImg(
		ImageVariantGroup imageVariantGroup, String originalImgTag,
		ResponsiveImageRequest responsiveImageRequest) {

		StringBundler sb = new StringBundler(7);

		sb.append("srcset=\"");
		sb.append(
			HtmlUtil.escapeAttribute(
				_getSrcSet(imageVariantGroup.getVariants())));
		sb.append("\"");

		String sizes = imageVariantGroup.getSizes();

		if (!Validator.isBlank(sizes)) {
			sb.append(" sizes=\"");
			sb.append(HtmlUtil.escapeAttribute(sizes));
			sb.append("\"");
		}

		// sizes="auto" is only honored on a lazily loaded image, so the two
		// attributes have to be emitted together or not at all.

		if (responsiveImageRequest.isLazy() &&
			!originalImgTag.contains("loading=")) {

			sb.append(" loading=\"lazy\"");
		}

		return _injectAttributes(originalImgTag, sb.toString());
	}

	private String _renderPicture(
		List<ImageVariantGroup> imageVariantGroups, String originalImgTag,
		ResponsiveImageRequest responsiveImageRequest) {

		StringBundler sb = new StringBundler(
			(imageVariantGroups.size() * 7) + 3);

		sb.append("<picture>");

		for (ImageVariantGroup imageVariantGroup : imageVariantGroups) {
			String mediaQuery = imageVariantGroup.getMediaQuery();

			if (Validator.isBlank(mediaQuery)) {
				continue;
			}

			sb.append("<source media=\"");
			sb.append(HtmlUtil.escapeAttribute(mediaQuery));
			sb.append("\" srcset=\"");
			sb.append(
				HtmlUtil.escapeAttribute(
					_getSrcSet(imageVariantGroup.getVariants())));
			sb.append("\"");

			String sizes = imageVariantGroup.getSizes();

			if (!Validator.isBlank(sizes)) {
				sb.append(" sizes=\"");
				sb.append(HtmlUtil.escapeAttribute(sizes));
				sb.append("\"");
			}

			sb.append(" />");
		}

		// The last group also feeds the img element, which is both the fallback
		// for browsers without picture support and the final source when no
		// media condition matches.

		sb.append(
			_renderImg(
				imageVariantGroups.get(imageVariantGroups.size() - 1),
				originalImgTag, responsiveImageRequest));

		sb.append("</picture>");

		return sb.toString();
	}

	private Map<String, String> _toMap(String[] entries) {
		if (entries == null) {
			return Collections.emptyMap();
		}

		Map<String, String> map = new HashMap<>();

		for (String entry : entries) {
			if (Validator.isBlank(entry)) {
				continue;
			}

			int i = entry.indexOf(StringPool.EQUAL);

			if (i <= 0) {
				if (_log.isWarnEnabled()) {
					_log.warn("Ignoring malformed entry " + entry);
				}

				continue;
			}

			map.put(entry.substring(0, i), entry.substring(i + 1));
		}

		return map;
	}

	private TreeSet<Integer> _toWidths(String[] variantWidths) {
		TreeSet<Integer> widths = new TreeSet<>();

		if (variantWidths == null) {
			return widths;
		}

		for (String variantWidth : variantWidths) {
			int width = GetterUtil.getInteger(variantWidth);

			if (width > 0) {
				widths.add(width);
			}
			else if (_log.isWarnEnabled()) {
				_log.warn("Ignoring invalid variant width " + variantWidth);
			}
		}

		return widths;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CDNImageTransformationProvider.class);

	private static final CompanySettings _emptyCompanySettings =
		new CompanySettings(
			0, Collections.<String, String>emptyMap(), null, new TreeSet<>());
	private static final List<String> _excludedMimeTypes = Arrays.asList(
		"image/svg+xml", "image/x-icon");

	@Reference
	private AbsolutePortalURLBuilderFactory _absolutePortalURLBuilderFactory;

	private final Map<Long, CompanySettings> _companySettings =
		new ConcurrentHashMap<>();

	@Reference
	private ConfigurationProvider _configurationProvider;

	@Reference
	private ImagePresetResolver _imagePresetResolver;

	private ImageTransformationConfigurationHelper
		_imageTransformationConfigurationHelper;

	@Reference
	private Portal _portal;

	private static class CompanySettings {

		private CompanySettings(
			int contentHash, Map<String, String> defaultTransformations,
			String rendererName, TreeSet<Integer> variantWidths) {

			_contentHash = contentHash;
			_defaultTransformations = defaultTransformations;
			_rendererName = rendererName;
			_variantWidths = variantWidths;
		}

		private final int _contentHash;
		private final Map<String, String> _defaultTransformations;
		private final String _rendererName;
		private final TreeSet<Integer> _variantWidths;

	}

}