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

	public ResponsiveImage create(
		ResponsiveImageRequest responsiveImageRequest) {

		if (!_isSupported(responsiveImageRequest.getImageResource())) {
			return null;
		}

		ScopedConfiguration scopedConfiguration =
			_responsiveImageConfigurationRegistry.getScopedConfiguration(
				_responsiveImageConfigurationHelper.getGroupId(
					responsiveImageRequest),
				_responsiveImageConfigurationHelper.getCompanyId(
					responsiveImageRequest));

		ImageTransformationURLRenderer imageTransformationURLRenderer =
			_getImageTransformationURLRenderer(
				scopedConfiguration.getURLRendererName());

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

		if (Validator.isBlank(url) || !url.startsWith(StringPool.SLASH)) {
			return false;
		}

		return true;
	}

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