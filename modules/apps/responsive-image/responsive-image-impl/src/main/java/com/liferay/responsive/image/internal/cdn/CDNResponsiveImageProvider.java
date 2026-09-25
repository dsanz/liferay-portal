/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal.cdn;

import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerList;
import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerListFactory;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.url.builder.AbsolutePortalURLBuilder;
import com.liferay.portal.url.builder.AbsolutePortalURLBuilderFactory;
import com.liferay.portal.url.builder.ImageTransformationURLRenderer;
import com.liferay.portal.url.builder.TransformedImageAbsolutePortalURLBuilder;
import com.liferay.responsive.image.ImageResource;
import com.liferay.responsive.image.ResponsiveImage;
import com.liferay.responsive.image.ResponsiveImageBreakpoint;
import com.liferay.responsive.image.ResponsiveImageBreakpointVariant;
import com.liferay.responsive.image.ResponsiveImageBreakpointVariantBuilder;
import com.liferay.responsive.image.ResponsiveImageProvider;
import com.liferay.responsive.image.ResponsiveImageRequest;
import com.liferay.responsive.image.internal.configuration.BreakpointDefinition;
import com.liferay.responsive.image.internal.configuration.PresetDefinition;
import com.liferay.responsive.image.internal.configuration.PresetDefinitionResolver;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfiguration;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
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
@Component(service = ResponsiveImageProvider.class)
public class CDNResponsiveImageProvider implements ResponsiveImageProvider {

	public static final String NAME = "cdn";

	@Override
	public boolean canTransform(ImageResource imageResource) {
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

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public ResponsiveImage getResponsiveImage(
		ResponsiveImageRequest responsiveImageRequest) {

		return _getResponsiveImage(
			responsiveImageRequest,
			_resolvePresetDefinition(responsiveImageRequest));
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_responsiveImageConfigurationHelper =
			new ResponsiveImageConfigurationHelper(
				_configurationProvider, _portal);

		_presetDefinitionResolver = new PresetDefinitionResolver(
			_configurationProvider, _portal);

		_serviceTrackerList = ServiceTrackerListFactory.open(
			bundleContext, ImageTransformationURLRenderer.class);
	}

	@Deactivate
	protected void deactivate() {
		_serviceTrackerList.close();
	}

	private String _buildURL(
		ScopedSettings scopedSettings,
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
					_getImageTransformationURLRenderer(
						scopedSettings._urlRendererName)
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

		for (ImageTransformationURLRenderer imageTransformationURLRenderer :
				_serviceTrackerList) {

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
		ResponsiveImageRequest responsiveImageRequest,
		PresetDefinition presetDefinition) {

		ScopedSettings scopedSettings = _getScopedSettings(
			responsiveImageRequest);

		List<BreakpointDefinition> breakpointDefinitions =
			presetDefinition.getBreakpointDefinitions();

		boolean lazy = presetDefinition.isLazy(
			responsiveImageRequest.getLazy());

		List<ResponsiveImageBreakpoint> responsiveImageBreakpoints =
			new ArrayList<>(breakpointDefinitions.size());

		for (BreakpointDefinition breakpointDefinition :
				breakpointDefinitions) {

			List<Integer> widths = _getWidths(
				scopedSettings, breakpointDefinition);

			if (widths.isEmpty()) {
				continue;
			}

			responsiveImageBreakpoints.add(
				ResponsiveImageBreakpoint.of(
					breakpointDefinition.getMediaQuery(),
					breakpointDefinition.getSizes(lazy),
					_getResponsiveImageBreakpointVariants(
						scopedSettings, breakpointDefinition,
						responsiveImageRequest, widths)));
		}

		// Still the untransformed original. Now that the provider owns this
		// choice it could point at a middle rendition instead, which would be
		// a kinder default for a browser ignoring srcset, but that is a
		// behavior change and not part of moving the method.

		return new ResponsiveImage(
			responsiveImageBreakpoints,
			responsiveImageRequest.getImageResource(
			).getURL());
	}

	private List<ResponsiveImageBreakpointVariant>
		_getResponsiveImageBreakpointVariants(
			ScopedSettings scopedSettings,
			BreakpointDefinition breakpointDefinition,
			ResponsiveImageRequest responsiveImageRequest,
			List<Integer> widths) {

		ImageResource imageResource = responsiveImageRequest.getImageResource();

		// Precedence runs from broadest to narrowest: installation wide
		// defaults, then this placement's art direction, then the width.
		// Callers contribute none of it, so every transformation the site
		// issues is visible in configuration.

		Map<String, String> transformations = HashMapBuilder.putAll(
			scopedSettings._defaultTransformations
		).putAll(
			breakpointDefinition.getTransformations()
		).build();

		List<ResponsiveImageBreakpointVariant>
			responsiveImageBreakpointVariants = new ArrayList<>(widths.size());

		for (Integer width : widths) {
			Map<String, String> widthTransformations = HashMapBuilder.putAll(
				transformations
			).put(
				"width", String.valueOf(width)
			).build();

			responsiveImageBreakpointVariants.add(
				ResponsiveImageBreakpointVariantBuilder.url(
					_buildURL(
						scopedSettings, responsiveImageRequest,
						imageResource.getURL(), widthTransformations)
				).width(
					width
				).build());
		}

		return responsiveImageBreakpointVariants;
	}

	/**
	 * Returns the parsed settings for a company, reparsing only when the
	 * underlying configuration has actually changed.
	 */
	private ScopedSettings _getScopedSettings(
		ResponsiveImageRequest responsiveImageRequest) {

		long companyId = _responsiveImageConfigurationHelper.getCompanyId(
			responsiveImageRequest);
		long groupId = _responsiveImageConfigurationHelper.getGroupId(
			responsiveImageRequest);

		ResponsiveImageConfiguration responsiveImageConfiguration =
			_responsiveImageConfigurationHelper.getResponsiveImageConfiguration(
				groupId, companyId);

		if (responsiveImageConfiguration == null) {
			return _emptyScopedSettings;
		}

		String[] defaultTransformations =
			responsiveImageConfiguration.defaultTransformations();
		String[] variantWidths = responsiveImageConfiguration.variantWidths();

		int contentHash =
			(31 * Arrays.hashCode(defaultTransformations)) +
				Arrays.hashCode(variantWidths);

		long scopeKey = _responsiveImageConfigurationHelper.getScopeKey(
			groupId, companyId);

		ScopedSettings scopedSettings = _scopedSettings.get(scopeKey);

		if ((scopedSettings != null) &&
			(scopedSettings._contentHash == contentHash)) {

			return scopedSettings;
		}

		scopedSettings = new ScopedSettings(
			contentHash, _toMap(defaultTransformations),
			responsiveImageConfiguration.urlRendererName(),
			_toWidths(variantWidths));

		_scopedSettings.put(scopeKey, scopedSettings);

		return scopedSettings;
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
		ScopedSettings scopedSettings,
		BreakpointDefinition breakpointDefinition) {

		Integer maxWidth = breakpointDefinition.getMaxWidth();

		List<Integer> widths = new ArrayList<>(
			scopedSettings._variantWidths.size());

		for (Integer width : scopedSettings._variantWidths) {
			widths.add(width);

			if ((maxWidth != null) && (width >= maxWidth)) {
				break;
			}
		}

		return widths;
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
	private PresetDefinition _resolvePresetDefinition(
		ResponsiveImageRequest responsiveImageRequest) {

		return _presetDefinitionResolver.resolve(
			_responsiveImageConfigurationHelper.getGroupId(
				responsiveImageRequest),
			_responsiveImageConfigurationHelper.getCompanyId(
				responsiveImageRequest),
			responsiveImageRequest.getPresetName());
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
		CDNResponsiveImageProvider.class);

	private static final ScopedSettings _emptyScopedSettings =
		new ScopedSettings(
			0, Collections.<String, String>emptyMap(), null, new TreeSet<>());
	private static final List<String> _excludedMimeTypes = Arrays.asList(
		"image/svg+xml", "image/x-icon");

	@Reference
	private AbsolutePortalURLBuilderFactory _absolutePortalURLBuilderFactory;

	@Reference
	private ConfigurationProvider _configurationProvider;

	@Reference
	private Portal _portal;

	private PresetDefinitionResolver _presetDefinitionResolver;
	private ResponsiveImageConfigurationHelper
		_responsiveImageConfigurationHelper;
	private final Map<Long, ScopedSettings> _scopedSettings =
		new ConcurrentHashMap<>();
	private ServiceTrackerList<ImageTransformationURLRenderer>
		_serviceTrackerList;

	private static class ScopedSettings {

		private ScopedSettings(
			int contentHash, Map<String, String> defaultTransformations,
			String urlRendererName, TreeSet<Integer> variantWidths) {

			_contentHash = contentHash;
			_defaultTransformations = defaultTransformations;
			_urlRendererName = urlRendererName;
			_variantWidths = variantWidths;
		}

		private final int _contentHash;
		private final Map<String, String> _defaultTransformations;
		private final String _urlRendererName;
		private final TreeSet<Integer> _variantWidths;

	}

}