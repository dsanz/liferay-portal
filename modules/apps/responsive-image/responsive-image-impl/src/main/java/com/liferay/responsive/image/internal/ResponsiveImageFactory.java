/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.url.builder.AbsolutePortalURLBuilder;
import com.liferay.portal.url.builder.AbsolutePortalURLBuilderFactory;
import com.liferay.portal.url.builder.ImageTransformationURLRenderer;
import com.liferay.portal.url.builder.TransformedImageAbsolutePortalURLBuilder;
import com.liferay.responsive.image.ImageResource;
import com.liferay.responsive.image.ResponsiveImage;
import com.liferay.responsive.image.ResponsiveImageCandidate;
import com.liferay.responsive.image.ResponsiveImageCandidateBuilder;
import com.liferay.responsive.image.ResponsiveImageRequest;
import com.liferay.responsive.image.ResponsiveImageSource;
import com.liferay.responsive.image.internal.configuration.PresetDefinition;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationHelper;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationRegistry;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationRegistry.ScopedConfiguration;
import com.liferay.responsive.image.internal.configuration.SourceDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Builds a {@link ResponsiveImage} by asking an image optimization service for
 * arbitrary widths on demand.
 *
 * <p>
 * Vendor agnostic. It owns the candidate ladder and the preset, and delegates
 * only the URL vocabulary to the configured {@link
 * ImageTransformationURLRenderer}, so supporting another optimizer is one
 * small renderer rather than another copy of this class. That is also why
 * there is no provider abstraction above it: everything except the URL
 * spelling is the same whoever serves the bytes.
 * </p>
 *
 * @author Daniel Sanz
 */
public class ResponsiveImageFactory {

	public ResponsiveImageFactory(
		AbsolutePortalURLBuilderFactory absolutePortalURLBuilderFactory,
		ResponsiveImageConfigurationRegistry
			responsiveImageConfigurationRegistry,
		ResponsiveImageConfigurationHelper responsiveImageConfigurationHelper,
		Supplier<List<ImageTransformationURLRenderer>>
			imageTransformationURLRenderersSupplier) {

		_absolutePortalURLBuilderFactory = absolutePortalURLBuilderFactory;
		_responsiveImageConfigurationRegistry =
			responsiveImageConfigurationRegistry;
		_responsiveImageConfigurationHelper =
			responsiveImageConfigurationHelper;
		_imageTransformationURLRenderersSupplier =
			imageTransformationURLRenderersSupplier;
	}

	/**
	 * Returns the renditions for this request, or <code>null</code> if none
	 * could be produced.
	 *
	 * <p>
	 * Declines generously. An image the optimizer cannot serve, or a format
	 * that must not be resampled, answers <code>null</code> so the caller
	 * hands over rather than emitting a srcset of identical images.
	 * </p>
	 */
	public ResponsiveImage create(
		ResponsiveImageRequest responsiveImageRequest) {

		if (!_isSupported(responsiveImageRequest.getImageResource())) {
			return null;
		}

		// One read of this scope's configuration, so the presets and the
		// ladder cannot come from two different edits.

		ScopedConfiguration scopedConfiguration =
			_responsiveImageConfigurationRegistry.getScopedConfiguration(
				_responsiveImageConfigurationHelper.getGroupId(
					responsiveImageRequest),
				_responsiveImageConfigurationHelper.getCompanyId(
					responsiveImageRequest));

		ImageTransformationURLRenderer imageTransformationURLRenderer =
			_getImageTransformationURLRenderer(
				scopedConfiguration.getURLRendererName());

		// The renderer name is the switch. Unset, or naming one that is not
		// deployed, means decline here rather than build a ladder whose URLs
		// would all come back identical and be thrown away downstream.

		if (imageTransformationURLRenderer == null) {
			return null;
		}

		return _getResponsiveImage(
			imageTransformationURLRenderer, responsiveImageRequest,
			scopedConfiguration);
	}

	private String _buildURL(
		ImageTransformationURLRenderer imageTransformationURLRenderer,
		ResponsiveImageRequest responsiveImageRequest, String url,
		Map<String, String> transformations) {

		AbsolutePortalURLBuilder absolutePortalURLBuilder =
			_absolutePortalURLBuilderFactory.getAbsolutePortalURLBuilder(
				responsiveImageRequest.getHttpServletRequest());

		TransformedImageAbsolutePortalURLBuilder
			transformedImageAbsolutePortalURLBuilder =
				absolutePortalURLBuilder.forTransformedImage(
					url
				).setRenderer(
					imageTransformationURLRenderer
				);

		for (Map.Entry<String, String> entry : transformations.entrySet()) {
			transformedImageAbsolutePortalURLBuilder.addTransformation(
				entry.getKey(), entry.getValue());
		}

		return transformedImageAbsolutePortalURLBuilder.build();
	}

	private ImageTransformationURLRenderer _getImageTransformationURLRenderer(
		String urlRendererName) {

		if (Validator.isBlank(urlRendererName)) {
			return null;
		}

		List<ImageTransformationURLRenderer> imageTransformationURLRenderers =
			_imageTransformationURLRenderersSupplier.get();

		if (imageTransformationURLRenderers == null) {
			return null;
		}

		for (ImageTransformationURLRenderer imageTransformationURLRenderer :
				imageTransformationURLRenderers) {

			if (urlRendererName.equals(
					imageTransformationURLRenderer.getName())) {

				return imageTransformationURLRenderer;
			}
		}

		if (_log.isDebugEnabled()) {
			_log.debug("No URL renderer is named " + urlRendererName);
		}

		return null;
	}

	private ResponsiveImage _getResponsiveImage(
		ImageTransformationURLRenderer imageTransformationURLRenderer,
		ResponsiveImageRequest responsiveImageRequest,
		ScopedConfiguration scopedConfiguration) {

		PresetDefinition presetDefinition =
			scopedConfiguration.getPresetDefinition(
				responsiveImageRequest.getPresetName());

		List<SourceDefinition> sourceDefinitions =
			presetDefinition.getSourceDefinitions();

		boolean lazy = presetDefinition.isLazy();

		List<ResponsiveImageSource> responsiveImageSources = new ArrayList<>(
			sourceDefinitions.size());

		for (SourceDefinition sourceDefinition : sourceDefinitions) {
			List<Integer> widths = _getWidths(
				scopedConfiguration, sourceDefinition);

			if (widths.isEmpty()) {
				continue;
			}

			responsiveImageSources.add(
				ResponsiveImageSource.of(
					sourceDefinition.getMediaQuery(),
					sourceDefinition.getSizes(lazy),
					_getResponsiveImageCandidates(
						imageTransformationURLRenderer, scopedConfiguration,
						sourceDefinition, responsiveImageRequest, widths)));
		}

		// Still the original URL. Now that the provider owns this
		// choice it could point at a middle rendition instead, which would be
		// a kinder default for a browser ignoring srcset, but that is a
		// behavior change and not part of moving the method.

		return new ResponsiveImage(
			lazy, responsiveImageSources,
			responsiveImageRequest.getImageResource(
			).getURL());
	}

	private List<ResponsiveImageCandidate> _getResponsiveImageCandidates(
		ImageTransformationURLRenderer imageTransformationURLRenderer,
		ScopedConfiguration scopedConfiguration,
		SourceDefinition sourceDefinition,
		ResponsiveImageRequest responsiveImageRequest, List<Integer> widths) {

		ImageResource imageResource = responsiveImageRequest.getImageResource();

		// Precedence runs from broadest to narrowest: installation wide
		// defaults, then this placement's art direction, then the width.
		// Callers contribute none of it, so every transformation the site
		// issues is visible in configuration.

		Map<String, String> transformations = HashMapBuilder.putAll(
			scopedConfiguration.getDefaultTransformations()
		).putAll(
			sourceDefinition.getTransformations()
		).build();

		List<ResponsiveImageCandidate> responsiveImageCandidates =
			new ArrayList<>(widths.size());

		for (Integer width : widths) {
			Map<String, String> widthTransformations = HashMapBuilder.putAll(
				transformations
			).put(
				"width", String.valueOf(width)
			).build();

			responsiveImageCandidates.add(
				ResponsiveImageCandidateBuilder.url(
					_buildURL(
						imageTransformationURLRenderer, responsiveImageRequest,
						imageResource.getURL(), widthTransformations)
				).width(
					width
				).build());
		}

		return responsiveImageCandidates;
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
	 * protect it: with upscaling disabled, a candidate wider than the original
	 * returns the original, which is the best the source can give.
	 * </p>
	 */
	private List<Integer> _getWidths(
		ScopedConfiguration scopedConfiguration,
		SourceDefinition sourceDefinition) {

		Integer maxWidth = sourceDefinition.getMaxWidth();

		List<Integer> widths = new ArrayList<>(
			scopedConfiguration.getCandidateWidths(
			).size());

		for (Integer width : scopedConfiguration.getCandidateWidths()) {
			widths.add(width);

			if ((maxWidth != null) && (width >= maxWidth)) {
				break;
			}
		}

		return widths;
	}

	private boolean _isSupported(ImageResource imageResource) {
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
	 * Returns the preset this request asks for, resolved against the narrowest
	 * scope the request can name.
	 *
	 * <p>
	 * The scope is derived here rather than passed around, because it is a
	 * property of the request and carrying it alongside would let the two
	 * disagree.
	 * </p>
	 */
	private static final Log _log = LogFactoryUtil.getLog(
		ResponsiveImageFactory.class);

	private static final List<String> _excludedMimeTypes = Arrays.asList(
		"image/svg+xml", "image/x-icon");

	private final AbsolutePortalURLBuilderFactory
		_absolutePortalURLBuilderFactory;
	private final Supplier<List<ImageTransformationURLRenderer>>
		_imageTransformationURLRenderersSupplier;
	private final ResponsiveImageConfigurationHelper
		_responsiveImageConfigurationHelper;
	private final ResponsiveImageConfigurationRegistry
		_responsiveImageConfigurationRegistry;

}