/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal;

import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.responsive.image.ImageResource;
import com.liferay.responsive.image.ResponsiveImage;
import com.liferay.responsive.image.ResponsiveImageCandidate;
import com.liferay.responsive.image.ResponsiveImageRequest;
import com.liferay.responsive.image.ResponsiveImageSource;
import com.liferay.responsive.image.ResponsiveImageURLTransformer;
import com.liferay.responsive.image.internal.configuration.PresetDefinition;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationHelper;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationRegistry;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationRegistry.ScopedConfiguration;
import com.liferay.responsive.image.internal.configuration.SourceDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * @author Daniel Sanz
 */
public class ResponsiveImageFactory {

	public ResponsiveImageFactory(
		Portal portal,
		ResponsiveImageConfigurationHelper responsiveImageConfigurationHelper,
		ResponsiveImageConfigurationRegistry
			responsiveImageConfigurationRegistry,
		ServiceTrackerMap<String, ResponsiveImageURLTransformer>
			serviceTrackerMap) {

		_portal = portal;
		_responsiveImageConfigurationHelper =
			responsiveImageConfigurationHelper;
		_responsiveImageConfigurationRegistry =
			responsiveImageConfigurationRegistry;
		_serviceTrackerMap = serviceTrackerMap;
	}

	public ResponsiveImage create(
		ResponsiveImageRequest responsiveImageRequest) {

		ImageResource imageResource = responsiveImageRequest.getImageResource();

		if (!_isSupported(imageResource)) {
			return null;
		}

		long companyId = _responsiveImageConfigurationHelper.getCompanyId(
			responsiveImageRequest);

		ScopedConfiguration scopedConfiguration =
			_responsiveImageConfigurationRegistry.getScopedConfiguration(
				_responsiveImageConfigurationHelper.getGroupId(
					responsiveImageRequest),
				companyId);

		ResponsiveImageURLTransformer responsiveImageURLTransformer =
			_getResponsiveImageURLTransformer(
				scopedConfiguration.getResponsiveImageURLTransformerName());

		if (responsiveImageURLTransformer == null) {
			return null;
		}

		PresetDefinition presetDefinition =
			scopedConfiguration.getPresetDefinition(
				responsiveImageRequest.getPresetName());

		return ResponsiveImage.builder(
		).lazy(
			presetDefinition.isLazy()
		).sources(
			_getResponsiveImageSources(
				presetDefinition, responsiveImageURLTransformer,
				scopedConfiguration,
				_getURL(companyId, scopedConfiguration, imageResource.getURL()))
		).build();
	}

	private List<ResponsiveImageCandidate> _getResponsiveImageCandidates(
		ResponsiveImageURLTransformer responsiveImageURLTransformer,
		ScopedConfiguration scopedConfiguration,
		SourceDefinition sourceDefinition, String url, List<Integer> widths) {

		Map<String, String> imageTransformations = HashMapBuilder.putAll(
			scopedConfiguration.getDefaultTransformations()
		).putAll(
			sourceDefinition.getTransformations()
		).build();

		return TransformUtil.transform(
			widths,
			width -> ResponsiveImageCandidate.builder(
				responsiveImageURLTransformer.transform(
					url,
					HashMapBuilder.putAll(
						imageTransformations
					).put(
						"width", String.valueOf(width)
					).build())
			).width(
				width
			).build());
	}

	private List<ResponsiveImageSource> _getResponsiveImageSources(
		PresetDefinition presetDefinition,
		ResponsiveImageURLTransformer responsiveImageURLTransformer,
		ScopedConfiguration scopedConfiguration, String url) {

		List<SourceDefinition> sourceDefinitions =
			presetDefinition.getSourceDefinitions();

		boolean lazy = presetDefinition.isLazy();

		return TransformUtil.transform(
			sourceDefinitions,
			sourceDefinition -> {
				List<Integer> widths = _getWidths(
					scopedConfiguration, sourceDefinition);

				if (widths.isEmpty()) {
					return null;
				}

				return ResponsiveImageSource.builder(
				).candidates(
					_getResponsiveImageCandidates(
						responsiveImageURLTransformer, scopedConfiguration,
						sourceDefinition, url, widths)
				).mediaQuery(
					sourceDefinition.getMediaQuery()
				).sizes(
					sourceDefinition.getSizes(lazy)
				).build();
			});
	}

	private ResponsiveImageURLTransformer _getResponsiveImageURLTransformer(
		String responsiveImageURLTransformerName) {

		if (Validator.isBlank(responsiveImageURLTransformerName)) {
			return null;
		}

		ResponsiveImageURLTransformer responsiveImageURLTransformer =
			_serviceTrackerMap.getService(responsiveImageURLTransformerName);

		if ((responsiveImageURLTransformer == null) && _log.isDebugEnabled()) {
			_log.debug(
				"No image URL transformer is named " +
					responsiveImageURLTransformerName);
		}

		return responsiveImageURLTransformer;
	}

	/**
	 * Returns the image URL served from the host the transformed images are
	 * delivered by, or the URL unchanged when no host is configured.
	 *
	 * <p>
	 * The configured host wins, so images can be delivered by an image
	 * optimization service that fronts nothing else. Absent it, the company's
	 * own CDN host is used. There is no request to tell whether the connection
	 * is secure, so the HTTPS host is preferred.
	 * </p>
	 */
	private String _getURL(
		long companyId, ScopedConfiguration scopedConfiguration, String url) {

		String host = scopedConfiguration.getCDNHost();

		if (Validator.isBlank(host)) {
			host = _portal.getCDNHostHttps(companyId);
		}

		if (Validator.isBlank(host)) {
			host = _portal.getCDNHostHttp(companyId);
		}

		if (Validator.isBlank(host)) {
			return url;
		}

		if (host.endsWith(StringPool.SLASH)) {
			host = host.substring(0, host.length() - 1);
		}

		return host.concat(url);
	}

	private List<Integer> _getWidths(
		ScopedConfiguration scopedConfiguration,
		SourceDefinition sourceDefinition) {

		TreeSet<Integer> candidateWidths =
			scopedConfiguration.getCandidateWidths();

		List<Integer> widths = new ArrayList<>(candidateWidths.size());

		Integer maxWidth = sourceDefinition.getMaxWidth();

		for (Integer width : candidateWidths) {
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

	private final Portal _portal;
	private final ResponsiveImageConfigurationHelper
		_responsiveImageConfigurationHelper;
	private final ResponsiveImageConfigurationRegistry
		_responsiveImageConfigurationRegistry;
	private final ServiceTrackerMap<String, ResponsiveImageURLTransformer>
		_serviceTrackerMap;

}