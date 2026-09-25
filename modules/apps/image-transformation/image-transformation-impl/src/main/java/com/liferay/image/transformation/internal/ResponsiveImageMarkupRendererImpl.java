/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.image.transformation.internal;

import com.liferay.image.transformation.ImageResource;
import com.liferay.image.transformation.ResponsiveImage;
import com.liferay.image.transformation.ResponsiveImageBreakpoint;
import com.liferay.image.transformation.ResponsiveImageBreakpointVariant;
import com.liferay.image.transformation.ResponsiveImageMarkupRenderer;
import com.liferay.image.transformation.ResponsiveImageRequest;
import com.liferay.image.transformation.internal.configuration.ImageTransformationConfigurationHelper;
import com.liferay.image.transformation.internal.configuration.ImageTransformationConfigurationValidator;
import com.liferay.image.transformation.internal.configuration.MarkupShape;
import com.liferay.image.transformation.internal.configuration.PresetDefinition;
import com.liferay.image.transformation.internal.configuration.PresetDefinitionResolver;
import com.liferay.image.transformation.spi.ImageTransformationProvider;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;

import java.util.List;
import java.util.Objects;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * Turns the active provider's renditions into an image tag.
 *
 * <p>
 * Selects the provider, asks it for the model, and renders it. A provider that
 * renders its own markup says so by returning something from {@link
 * ImageTransformationProvider#render}; everything else is rendered here, from
 * the model alone, so that adding a provider does not mean writing markup
 * again.
 * </p>
 *
 * @author Daniel Sanz
 */
@Component(service = ResponsiveImageMarkupRenderer.class)
public class ResponsiveImageMarkupRendererImpl
	implements ResponsiveImageMarkupRenderer {

	/**
	 * Renders a single <code>&lt;img&gt;</code> when the preset declares one
	 * breakpoint, and <code>&lt;picture&gt;</code> when it declares several.
	 *
	 * <p>
	 * The shape comes from {@link
	 * ImageTransformationConfigurationValidator#getMarkupShape}, which reads
	 * the configured presets rather than the breakpoints that survived
	 * generation. A preset whose ladder came back empty is skipped, so counting
	 * breakpoints instead would quietly downgrade an art directed placement to
	 * a plain <code>&lt;img&gt;</code> and drop the media conditions that made
	 * it art directed.
	 * </p>
	 *
	 * <p>
	 * Returns the original tag whenever anything is missing or nothing was
	 * actually transformed, so this is always safe to call.
	 * </p>
	 */
	@Override
	public String render(
			String originalImgTag,
			ResponsiveImageRequest responsiveImageRequest)
		throws PortalException {

		if (responsiveImageRequest == null) {
			return originalImgTag;
		}

		ImageResource imageResource = responsiveImageRequest.getImageResource();

		if (imageResource == null) {
			return originalImgTag;
		}

		ImageTransformationProvider imageTransformationProvider =
			_imageTransformationProviderSelector.getImageTransformationProvider(
				responsiveImageRequest);

		if (imageTransformationProvider == null) {
			return originalImgTag;
		}

		String markup = imageTransformationProvider.render(
			originalImgTag, responsiveImageRequest);

		if (markup != null) {
			return markup;
		}

		ResponsiveImage responsiveImage =
			imageTransformationProvider.getResponsiveImage(
				responsiveImageRequest);

		if (responsiveImage == null) {
			return originalImgTag;
		}

		List<ResponsiveImageBreakpoint> responsiveImageBreakpoints =
			responsiveImage.getBreakpoints();

		if (responsiveImageBreakpoints.isEmpty() ||
			!_isTransformed(responsiveImageBreakpoints)) {

			// Nothing was generated, or every width built the same URL because
			// no renderer is bound. Emitting the descriptors anyway would have
			// the browser trust them and render at the wrong density.

			return originalImgTag;
		}

		PresetDefinition presetDefinition = _presetDefinitionResolver.resolve(
			_imageTransformationConfigurationHelper.getGroupId(
				responsiveImageRequest),
			_imageTransformationConfigurationHelper.getCompanyId(
				responsiveImageRequest),
			responsiveImageRequest.getPresetName());

		boolean lazy = presetDefinition.isLazy(
			responsiveImageRequest.getLazy());

		MarkupShape markupShape =
			ImageTransformationConfigurationValidator.getMarkupShape(
				presetDefinition);

		if (markupShape == MarkupShape.IMG) {
			return _renderImg(
				responsiveImageBreakpoints.get(0), lazy, originalImgTag);
		}

		return _renderPicture(responsiveImageBreakpoints, lazy, originalImgTag);
	}

	@Activate
	protected void activate() {
		_imageTransformationConfigurationHelper =
			new ImageTransformationConfigurationHelper(
				_configurationProvider, _portal);

		_presetDefinitionResolver = new PresetDefinitionResolver(
			_configurationProvider, _portal);

		_imageTransformationProviderSelector =
			new ImageTransformationProviderSelector(
				_imageTransformationConfigurationHelper,
				() -> _imageTransformationProviders);
	}

	private String _getSrcSet(
		List<ResponsiveImageBreakpointVariant>
			responsiveImageBreakpointVariants) {

		StringBundler sb = new StringBundler(
			responsiveImageBreakpointVariants.size() * 4);

		for (ResponsiveImageBreakpointVariant responsiveImageBreakpointVariant :
				responsiveImageBreakpointVariants) {

			if (responsiveImageBreakpointVariant.getWidth() == null) {
				continue;
			}

			if (sb.index() > 0) {
				sb.append(StringPool.COMMA_AND_SPACE);
			}

			sb.append(responsiveImageBreakpointVariant.getURL());
			sb.append(StringPool.SPACE);
			sb.append(responsiveImageBreakpointVariant.getWidth());
			sb.append("w");
		}

		return sb.toString();
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
	 * Two variants in one breakpoint differ only by width, so identical URLs mean
	 * the width never reached the URL. Comparing within a breakpoint rather than
	 * against the original is what makes this work regardless of the CDN host
	 * and proxy path the builder prepends.
	 * </p>
	 */
	private boolean _isTransformed(
		List<ResponsiveImageBreakpoint> responsiveImageBreakpoints) {

		for (ResponsiveImageBreakpoint responsiveImageBreakpoint :
				responsiveImageBreakpoints) {

			List<ResponsiveImageBreakpointVariant>
				responsiveImageBreakpointVariants =
					responsiveImageBreakpoint.getVariants();

			if (responsiveImageBreakpointVariants.size() < 2) {
				continue;
			}

			ResponsiveImageBreakpointVariant
				firstResponsiveImageBreakpointVariant =
					responsiveImageBreakpointVariants.get(0);
			ResponsiveImageBreakpointVariant
				secondResponsiveImageBreakpointVariant =
					responsiveImageBreakpointVariants.get(1);

			return !Objects.equals(
				firstResponsiveImageBreakpointVariant.getURL(),
				secondResponsiveImageBreakpointVariant.getURL());
		}

		return true;
	}

	private String _renderImg(
		ResponsiveImageBreakpoint responsiveImageBreakpoint, boolean lazy,
		String originalImgTag) {

		StringBundler sb = new StringBundler(7);

		sb.append("srcset=\"");
		sb.append(
			HtmlUtil.escapeAttribute(
				_getSrcSet(responsiveImageBreakpoint.getVariants())));
		sb.append("\"");

		String sizes = responsiveImageBreakpoint.getSizes();

		if (!Validator.isBlank(sizes)) {
			sb.append(" sizes=\"");
			sb.append(HtmlUtil.escapeAttribute(sizes));
			sb.append("\"");
		}

		// sizes="auto" is only honored on a lazily loaded image, so the two
		// attributes have to be emitted together or not at all.

		if (lazy && !originalImgTag.contains("loading=")) {
			sb.append(" loading=\"lazy\"");
		}

		return _injectAttributes(originalImgTag, sb.toString());
	}

	private String _renderPicture(
		List<ResponsiveImageBreakpoint> responsiveImageBreakpoints,
		boolean lazy, String originalImgTag) {

		StringBundler sb = new StringBundler(
			(responsiveImageBreakpoints.size() * 7) + 3);

		sb.append("<picture>");

		for (ResponsiveImageBreakpoint responsiveImageBreakpoint :
				responsiveImageBreakpoints) {

			String mediaQuery = responsiveImageBreakpoint.getMediaQuery();

			if (Validator.isBlank(mediaQuery)) {
				continue;
			}

			sb.append("<source media=\"");
			sb.append(HtmlUtil.escapeAttribute(mediaQuery));
			sb.append("\" srcset=\"");
			sb.append(
				HtmlUtil.escapeAttribute(
					_getSrcSet(responsiveImageBreakpoint.getVariants())));
			sb.append("\"");

			String sizes = responsiveImageBreakpoint.getSizes();

			if (!Validator.isBlank(sizes)) {
				sb.append(" sizes=\"");
				sb.append(HtmlUtil.escapeAttribute(sizes));
				sb.append("\"");
			}

			sb.append(" />");
		}

		// The last breakpoint also feeds the img element, which is both the
		// fallback for browsers without picture support and the final source
		// when no media condition matches.

		sb.append(
			_renderImg(
				responsiveImageBreakpoints.get(
					responsiveImageBreakpoints.size() - 1),
				lazy, originalImgTag));

		sb.append("</picture>");

		return sb.toString();
	}

	/**
	 * Renders a single <code>&lt;img&gt;</code> when the preset declares one
	 * breakpoint, and <code>&lt;picture&gt;</code> when it declares several.
	 *
	 * <p>
	 * The shape comes from {@link
	 * ImageTransformationConfigurationValidator#getMarkupShape}, which reads
	 * the configured presets rather than the breakpoints that survived
	 * generation. A preset whose ladder came back empty is skipped, so counting
	 * breakpoints instead would quietly downgrade an art directed placement
	 * to a plain <code>&lt;img&gt;</code> and drop the media conditions that
	 * made it art directed.
	 * </p>
	 *
	 * <p>
	 * Wrapping a lone source in <code>&lt;picture&gt;</code> would be pure
	 * overhead, and enumerating breakpoints when only resolution varies would
	 * discard what the browser knows about pixel density, network conditions,
	 * and its own cache.
	 * </p>
	 */
	@Reference
	private ConfigurationProvider _configurationProvider;

	private ImageTransformationConfigurationHelper
		_imageTransformationConfigurationHelper;

	@Reference(
		cardinality = ReferenceCardinality.MULTIPLE,
		policy = ReferencePolicy.DYNAMIC,
		policyOption = ReferencePolicyOption.GREEDY
	)
	private volatile List<ImageTransformationProvider>
		_imageTransformationProviders;

	private ImageTransformationProviderSelector
		_imageTransformationProviderSelector;

	@Reference
	private Portal _portal;

	private PresetDefinitionResolver _presetDefinitionResolver;

}