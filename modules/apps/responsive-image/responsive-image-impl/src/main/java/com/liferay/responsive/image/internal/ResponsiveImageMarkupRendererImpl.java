/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.responsive.image.internal;

import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMap;
import com.liferay.osgi.service.tracker.collections.map.ServiceTrackerMapFactory;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.feature.flag.FeatureFlagManagerUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.responsive.image.ImageResource;
import com.liferay.responsive.image.ResponsiveImage;
import com.liferay.responsive.image.ResponsiveImageCandidate;
import com.liferay.responsive.image.ResponsiveImageMarkupRenderer;
import com.liferay.responsive.image.ResponsiveImageRequest;
import com.liferay.responsive.image.ResponsiveImageSource;
import com.liferay.responsive.image.ResponsiveImageURLTransformer;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationHelper;
import com.liferay.responsive.image.internal.configuration.ResponsiveImageConfigurationRegistry;

import java.util.List;
import java.util.Objects;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(service = ResponsiveImageMarkupRenderer.class)
public class ResponsiveImageMarkupRendererImpl
	implements ResponsiveImageMarkupRenderer {

	@Override
	public String render(
			String originalImgTag,
			ResponsiveImageRequest responsiveImageRequest)
		throws PortalException {

		if ((responsiveImageRequest == null) ||
			!FeatureFlagManagerUtil.isEnabled(
				_responsiveImageConfigurationHelper.getCompanyId(
					responsiveImageRequest),
				"LPD-94784")) {

			return null;
		}

		ImageResource imageResource = responsiveImageRequest.getImageResource();

		if (imageResource == null) {
			return null;
		}

		ResponsiveImage responsiveImage = _responsiveImageFactory.create(
			responsiveImageRequest);

		if (responsiveImage == null) {
			return null;
		}

		List<ResponsiveImageSource> responsiveImageSources =
			responsiveImage.getSources();

		if (responsiveImageSources.isEmpty() ||
			_isUntransformed(responsiveImageSources)) {

			return null;
		}

		if (_isPictureElementRequired(responsiveImageSources)) {
			return _renderPicture(
				responsiveImageSources, responsiveImage.isLazy(),
				originalImgTag);
		}

		return _renderImg(
			responsiveImageSources.get(0), responsiveImage.isLazy(),
			originalImgTag);
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_serviceTrackerMap = ServiceTrackerMapFactory.openSingleValueMap(
			bundleContext, ResponsiveImageURLTransformer.class,
			"responsive.image.url.transformer.name");

		_responsiveImageConfigurationHelper =
			new ResponsiveImageConfigurationHelper(
				_configurationProvider, _portal);

		_responsiveImageFactory = new ResponsiveImageFactory(
			_portal,
			new ResponsiveImageConfigurationRegistry(
				_configurationProvider, _portal),
			_responsiveImageConfigurationHelper, _serviceTrackerMap);
	}

	@Deactivate
	protected void deactivate() {
		_serviceTrackerMap.close();
	}

	private String _getSrcSet(
		List<ResponsiveImageCandidate> responsiveImageCandidates) {

		StringBundler sb = new StringBundler(
			responsiveImageCandidates.size() * 4);

		for (ResponsiveImageCandidate responsiveImageCandidate :
				responsiveImageCandidates) {

			if (responsiveImageCandidate.getWidth() == null) {
				continue;
			}

			if (sb.index() > 0) {
				sb.append(StringPool.COMMA_AND_SPACE);
			}

			sb.append(responsiveImageCandidate.getURL());
			sb.append(StringPool.SPACE);
			sb.append(responsiveImageCandidate.getWidth());
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

	private boolean _isPictureElementRequired(
		List<ResponsiveImageSource> responsiveImageSources) {

		for (ResponsiveImageSource responsiveImageSource :
				responsiveImageSources) {

			if (!Validator.isBlank(responsiveImageSource.getMediaQuery())) {
				return true;
			}
		}

		return false;
	}

	private boolean _isUntransformed(
		List<ResponsiveImageSource> responsiveImageSources) {

		for (ResponsiveImageSource responsiveImageSource :
				responsiveImageSources) {

			List<ResponsiveImageCandidate> responsiveImageCandidates =
				responsiveImageSource.getCandidates();

			if (responsiveImageCandidates.size() < 2) {
				continue;
			}

			ResponsiveImageCandidate firstResponsiveImageCandidate =
				responsiveImageCandidates.get(0);
			ResponsiveImageCandidate secondResponsiveImageCandidate =
				responsiveImageCandidates.get(1);

			return Objects.equals(
				firstResponsiveImageCandidate.getURL(),
				secondResponsiveImageCandidate.getURL());
		}

		return false;
	}

	private String _renderImg(
		ResponsiveImageSource responsiveImageSource, boolean lazy,
		String originalImgTag) {

		StringBundler sb = new StringBundler(7);

		sb.append("srcset=\"");
		sb.append(
			HtmlUtil.escapeAttribute(
				_getSrcSet(responsiveImageSource.getCandidates())));
		sb.append("\"");

		String sizes = responsiveImageSource.getSizes();

		if (!Validator.isBlank(sizes)) {
			sb.append(" sizes=\"");
			sb.append(HtmlUtil.escapeAttribute(sizes));
			sb.append("\"");
		}

		// The sizes="auto" value is only honored on a lazily loaded image, so
		// the two attributes have to be emitted together or not at all.

		if (lazy && !originalImgTag.contains("loading=")) {
			sb.append(" loading=\"lazy\"");
		}

		return _injectAttributes(originalImgTag, sb.toString());
	}

	private String _renderPicture(
		List<ResponsiveImageSource> responsiveImageSources, boolean lazy,
		String originalImgTag) {

		StringBundler sb = new StringBundler(
			(responsiveImageSources.size() * 7) + 3);

		sb.append("<picture>");

		for (ResponsiveImageSource responsiveImageSource :
				responsiveImageSources) {

			String mediaQuery = responsiveImageSource.getMediaQuery();

			if (Validator.isBlank(mediaQuery)) {
				continue;
			}

			sb.append("<source media=\"");
			sb.append(HtmlUtil.escapeAttribute(mediaQuery));
			sb.append("\" srcset=\"");
			sb.append(
				HtmlUtil.escapeAttribute(
					_getSrcSet(responsiveImageSource.getCandidates())));
			sb.append("\"");

			String sizes = responsiveImageSource.getSizes();

			if (!Validator.isBlank(sizes)) {
				sb.append(" sizes=\"");
				sb.append(HtmlUtil.escapeAttribute(sizes));
				sb.append("\"");
			}

			sb.append(" />");
		}

		// The last source also feeds the img element, which is both the
		// fallback for browsers without picture support and the final source
		// when no media condition matches.

		sb.append(
			_renderImg(
				responsiveImageSources.get(responsiveImageSources.size() - 1),
				lazy, originalImgTag));

		sb.append("</picture>");

		return sb.toString();
	}

	@Reference
	private ConfigurationProvider _configurationProvider;

	@Reference
	private Portal _portal;

	private ResponsiveImageConfigurationHelper
		_responsiveImageConfigurationHelper;
	private ResponsiveImageFactory _responsiveImageFactory;
	private ServiceTrackerMap<String, ResponsiveImageURLTransformer>
		_serviceTrackerMap;

}